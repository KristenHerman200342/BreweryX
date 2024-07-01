package com.dre.brewery.utility;

import org.bukkit.Bukkit;

/**
 * Enum for major Minecraft versions where Brewery needs
 * to handle things differently.
 */
public enum MinecraftVersion {

    V1_7("1.7"),
    V1_8("1.8"),
    V1_9("1.9"),
    V1_10("1.10"),
    V1_11("1.11"),
    V1_12("1.12"),
    V1_13("1.13"),
    V1_14("1.14"),
    V1_15("1.15"),
    V1_16("1.16"),
    V1_17("1.17"),
    V1_18("1.18"),
    V1_19("1.19"),
    V1_20("1.20"),
    V1_21("1.21"),
    UNKNOWN("Unknown");

    final String version;

    MinecraftVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public static MinecraftVersion get(String version) {
        for (MinecraftVersion v : values()) {
            if (v.version.equals(version)) {
                return v;
            }
        }
        return UNKNOWN;
    }

    public static MinecraftVersion getIt() {
        String rawVersion = Bukkit.getVersion();
        String rawVersionParsed = rawVersion.substring(rawVersion.indexOf("(MC: ") + 5, rawVersion.indexOf(")"));

        String[] versionSplit = rawVersionParsed.split("\\.");
        return get(versionSplit[0] + "." + versionSplit[1]);
    }

    public boolean isOrLater(MinecraftVersion version) {
        return this.ordinal() >= version.ordinal();
    }

    public boolean isOrEarlier(MinecraftVersion version) {
        return this.ordinal() <= version.ordinal();
    }

}
