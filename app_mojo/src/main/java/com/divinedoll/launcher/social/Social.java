package com.divinedoll.launcher.social;

import android.content.Context;

import com.divinedoll.launcher.prefs.LauncherPreferences;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Friends and chat backend. Anonymous Firebase account + a unique username.
 * Data model (see firebase/firestore.rules):
 *   users/{uid}, usernames/{lowerName}, requests/{from_to}, friends/{uid}/list/{friendUid},
 *   chats/{uidA_uidB}/messages/{id}, reports/{id}
 */
public final class Social {
    public static final String PREF_NAME = "social_name";

    public interface Done {
        void ok();
        void fail(String message);
    }

    private Social() {}

    /** True when app_mojo/google-services.json was present at build time. */
    public static boolean configured(Context c) {
        try {
            return !FirebaseApp.getApps(c).isEmpty();
        } catch (Throwable t) {
            return false;
        }
    }

    public static FirebaseFirestore db() { return FirebaseFirestore.getInstance(); }
    public static FirebaseAuth auth() { return FirebaseAuth.getInstance(); }

    public static String uid() {
        FirebaseUser u = auth().getCurrentUser();
        return u == null ? null : u.getUid();
    }

    public static String myName() {
        return LauncherPreferences.DEFAULT_PREF.getString(PREF_NAME, null);
    }

    private static String msg(Exception e) {
        return e == null || e.getMessage() == null ? "error" : e.getMessage();
    }

    /** Lowercase letters, digits and underscore, 3 to 16 chars. */
    public static String sanitize(String raw) {
        if (raw == null) return "";
        String s = raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "");
        return s.length() > 16 ? s.substring(0, 16) : s;
    }

    public static boolean validName(String s) {
        return s != null && s.matches("[a-z0-9_]{3,16}");
    }

    public static String chatId(String a, String b) {
        return a.compareTo(b) < 0 ? a + "_" + b : b + "_" + a;
    }

    public static void signIn(Done d) {
        if (auth().getCurrentUser() != null) { d.ok(); return; }
        auth().signInAnonymously()
                .addOnSuccessListener(r -> d.ok())
                .addOnFailureListener(e -> d.fail(msg(e)));
    }

    /** Reserves a username for this account (fails if somebody has it). */
    public static void claimName(String name, Done d) {
        final String uid = uid();
        if (uid == null) { d.fail("not signed in"); return; }
        final String lower = sanitize(name);
        if (!validName(lower)) { d.fail("invalid"); return; }
        final DocumentReference nameRef = db().collection("usernames").document(lower);
        final DocumentReference userRef = db().collection("users").document(uid);
        db().runTransaction(tx -> {
            if (tx.get(nameRef).exists()) throw new IllegalStateException("taken");
            Map<String, Object> n = new HashMap<>();
            n.put("uid", uid);
            tx.set(nameRef, n);
            Map<String, Object> u = new HashMap<>();
            u.put("name", lower);
            u.put("nameLower", lower);
            u.put("createdAt", FieldValue.serverTimestamp());
            tx.set(userRef, u);
            return null;
        }).addOnSuccessListener(r -> {
            LauncherPreferences.DEFAULT_PREF.edit().putString(PREF_NAME, lower).apply();
            d.ok();
        }).addOnFailureListener(e -> d.fail(e instanceof IllegalStateException || String.valueOf(e.getMessage()).contains("taken") ? "taken" : msg(e)));
    }

    public static void sendRequest(String name, Done d) {
        final String me = uid();
        final String myName = myName();
        if (me == null || myName == null) { d.fail("not ready"); return; }
        final String lower = sanitize(name);
        if (lower.equals(myName)) { d.fail("self"); return; }
        db().collection("usernames").document(lower).get().addOnSuccessListener(snap -> {
            if (!snap.exists()) { d.fail("notfound"); return; }
            String other = snap.getString("uid");
            if (other == null) { d.fail("notfound"); return; }
            Map<String, Object> req = new HashMap<>();
            req.put("from", me);
            req.put("to", other);
            req.put("fromName", myName);
            req.put("ts", FieldValue.serverTimestamp());
            db().collection("requests").document(me + "_" + other).set(req)
                    .addOnSuccessListener(r -> d.ok())
                    .addOnFailureListener(e -> d.fail(msg(e)));
        }).addOnFailureListener(e -> d.fail(msg(e)));
    }

    public static void accept(String fromUid, String fromName, Done d) {
        final String me = uid();
        final String myName = myName();
        if (me == null || myName == null) { d.fail("not ready"); return; }
        WriteBatch b = db().batch();
        Map<String, Object> mine = new HashMap<>();
        mine.put("name", fromName);
        mine.put("since", FieldValue.serverTimestamp());
        b.set(db().collection("friends").document(me).collection("list").document(fromUid), mine);
        Map<String, Object> theirs = new HashMap<>();
        theirs.put("name", myName);
        theirs.put("since", FieldValue.serverTimestamp());
        b.set(db().collection("friends").document(fromUid).collection("list").document(me), theirs);
        b.delete(db().collection("requests").document(fromUid + "_" + me));
        b.commit().addOnSuccessListener(r -> d.ok()).addOnFailureListener(e -> d.fail(msg(e)));
    }

    public static void declineOrCancel(String requestId, Done d) {
        db().collection("requests").document(requestId).delete()
                .addOnSuccessListener(r -> d.ok()).addOnFailureListener(e -> d.fail(msg(e)));
    }

    public static void removeFriend(String friendUid, Done d) {
        final String me = uid();
        if (me == null) { d.fail("not ready"); return; }
        WriteBatch b = db().batch();
        b.delete(db().collection("friends").document(me).collection("list").document(friendUid));
        b.delete(db().collection("friends").document(friendUid).collection("list").document(me));
        b.commit().addOnSuccessListener(r -> d.ok()).addOnFailureListener(e -> d.fail(msg(e)));
    }

    public static void send(String friendUid, String text, Done d) {
        final String me = uid();
        if (me == null) { d.fail("not ready"); return; }
        Map<String, Object> m = new HashMap<>();
        m.put("from", me);
        m.put("text", text);
        m.put("ts", FieldValue.serverTimestamp());
        db().collection("chats").document(chatId(me, friendUid)).collection("messages").add(m)
                .addOnSuccessListener(r -> d.ok()).addOnFailureListener(e -> d.fail(msg(e)));
    }

    public static void report(String friendUid, String text, Done d) {
        final String me = uid();
        if (me == null) { d.fail("not ready"); return; }
        Map<String, Object> m = new HashMap<>();
        m.put("reporter", me);
        m.put("reported", friendUid);
        m.put("chatId", chatId(me, friendUid));
        m.put("text", text);
        m.put("ts", FieldValue.serverTimestamp());
        db().collection("reports").add(m)
                .addOnSuccessListener(r -> d.ok()).addOnFailureListener(e -> d.fail(msg(e)));
    }

    /** Removes the username and profile, signs out and forgets the name. Friends' copies disappear when they remove you. */
    public static void deleteAccount(Done d) {
        final String me = uid();
        final String name = myName();
        if (me == null) { d.fail("not ready"); return; }
        WriteBatch b = db().batch();
        if (name != null) b.delete(db().collection("usernames").document(name));
        b.delete(db().collection("users").document(me));
        b.commit().addOnSuccessListener(r -> {
            LauncherPreferences.DEFAULT_PREF.edit().remove(PREF_NAME).apply();
            FirebaseUser u = auth().getCurrentUser();
            if (u != null) u.delete();
            auth().signOut();
            d.ok();
        }).addOnFailureListener(e -> d.fail(msg(e)));
    }
}
