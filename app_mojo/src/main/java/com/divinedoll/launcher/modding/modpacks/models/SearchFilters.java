package com.divinedoll.launcher.modding.modpacks.models;

import org.jetbrains.annotations.Nullable;

/**
 * Search filters, passed to APIs
 */
public class SearchFilters {
    public boolean isModpack;
    /** mod, modpack, shader or resourcepack. null means: decided by isModpack. */
    @Nullable public String projectType;

    public String type() {
        if(projectType != null) return projectType;
        return isModpack ? "modpack" : "mod";
    }
    public String name;
    @Nullable public String mcVersion;

}
