# Amigos y chat: configuración de Firebase (gratis)

DivineLauncher usa Firebase (Authentication anónima + Firestore) para amigos y chat.
Sin el archivo `google-services.json` el launcher compila igual, pero la pantalla **Amigos** muestra esta guía.

## 1. Crear el proyecto
1. Entra a https://console.firebase.google.com con tu cuenta de Google.
2. **Agregar proyecto** → nombre `DivineLauncher` → desactiva Google Analytics → **Crear**.

## 2. Activar el inicio de sesión anónimo
**Compilación → Authentication → Comenzar → Método de acceso → Anónimo → Habilitar → Guardar.**

## 3. Crear la base de datos
**Compilación → Firestore Database → Crear base de datos** → elige una región cercana → **modo de producción** → Habilitar.

## 4. Pegar las reglas de seguridad
En Firestore → pestaña **Reglas**, borra todo, pega el contenido de `firebase/firestore.rules` y toca **Publicar**.
Las reglas son lo que protege los datos: solo tú escribes tu perfil, solo dos amigos leen su chat, mensajes de 1 a 500 caracteres.

## 5. Registrar las apps Android
**Configuración del proyecto (engranaje) → Tus apps → Android.** Registra **las dos**:
- `com.divinedoll.launcher`
- `com.divinedoll.launcher.debug` (el APK debug que compila Actions usa este nombre)

Después descarga **`google-services.json`** (sirve para las dos).

## 6. Ponerlo en el repo
En Termux:
```
cp /sdcard/Download/google-services.json ~/DivineLauncher/app_mojo/
cd ~/DivineLauncher && git add -f app_mojo/google-services.json && git commit -m "Add Firebase config" && git push divine HEAD:main
```
Vuelve a compilar. Abre **Amigos** en el menú lateral, elige un usuario y añade a un amigo por su nombre.

## Notas
- Ese archivo contiene una clave de API pública por diseño de Firebase; la seguridad real son las reglas del paso 4. Aun así puedes restringir la clave en Google Cloud Console → Credenciales.
- La cuenta es anónima: si desinstalas la app o borras sus datos pierdes tu usuario y tus amigos.
- Los mensajes se guardan en servidores de Google. El botón **Borrar mi cuenta de amigos** elimina tu usuario y perfil.
- Los reportes (mantén pulsado un mensaje) llegan a la colección `reports` de Firestore; revísala en la consola.
- Si tu comunidad incluye menores, revisa las leyes de tu país sobre datos y chat de menores antes de abrirlo al público.
