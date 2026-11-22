package io.github.blaublue1303.brassanchor;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-Konfiguration (liegt pro Welt unter serverconfig/brassanchor-server.toml). */
public final class AnchorConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue MAX_RADIUS;
    public static final ModConfigSpec.IntValue DEFAULT_RADIUS;
    public static final ModConfigSpec.BooleanValue CONTRAPTIONS;
    public static final ModConfigSpec.BooleanValue SABLE;
    public static final ModConfigSpec.IntValue MAX_ANCHORS_PER_DIMENSION;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        MAX_RADIUS = b.comment("Groesster einstellbarer Radius in Chunks. 0 = nur der eigene Chunk, 2 = 5x5 Chunks.")
                .defineInRange("maxRadius", 2, 0, 8);
        DEFAULT_RADIUS = b.comment("Radius, mit dem ein frisch platzierter Anker startet.")
                .defineInRange("defaultRadius", 1, 0, 8);
        CONTRAPTIONS = b.comment("Anker auf Create-Contraptions und Zuegen laden die Chunks um ihre aktuelle Position.")
                .define("loadOnContraptions", true);
        SABLE = b.comment("Anker auf Sable-Schiffen halten das Schiff geladen und laden die Chunks um seine Position.")
                .define("loadOnSableShips", true);
        MAX_ANCHORS_PER_DIMENSION = b.comment("Hoechstzahl aktiver Anker pro Dimension (0 = unbegrenzt).")
                .defineInRange("maxAnchorsPerDimension", 0, 0, 100000);
        SPEC = b.build();
    }

    /** Liest einen Wert, auch wenn die Server-Config (z. B. auf dem Client) noch nicht geladen ist. */
    public static int maxRadius() {
        return SPEC.isLoaded() ? MAX_RADIUS.get() : MAX_RADIUS.getDefault();
    }

    public static int defaultRadius() {
        int d = SPEC.isLoaded() ? DEFAULT_RADIUS.get() : DEFAULT_RADIUS.getDefault();
        return Math.min(d, maxRadius());
    }

    public static boolean contraptions() {
        return SPEC.isLoaded() ? CONTRAPTIONS.get() : CONTRAPTIONS.getDefault();
    }

    public static boolean sable() {
        return SPEC.isLoaded() ? SABLE.get() : SABLE.getDefault();
    }

    public static int maxAnchorsPerDimension() {
        return SPEC.isLoaded() ? MAX_ANCHORS_PER_DIMENSION.get() : 0;
    }

    private AnchorConfig() {}
}
