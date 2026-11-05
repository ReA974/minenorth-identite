package fr.minenorth.identite.skin;

import fr.minenorth.identite.appearance.TattooZone;

/** Où tombe un tatouage du catalogue sur sa zone, en pixels de skin ; identique pour le skin peint et la couche haute définition. */
public final class TattooPlacement {
    private TattooPlacement() {}

    /** Hauteur de départ sur un bras ou une jambe : 0 centré (comme avant), puis haut, milieu, bas, extrémité. */
    private static final int[] LIMB_START = {-1, 0, 3, 6, 8};

    /** {colonne, ligne, largeur, hauteur} du motif dans sa zone ; le torse et le dos font 8 × 12, un bras ou une jambe 4 × 12. */
    public static int[] of(TattooZone zone, int[] t) {
        int size = zone.hasSize() ? t[zone.sizeSlot()] : 0;
        boolean wide = zone == TattooZone.TORSO || zone == TattooZone.BACK;
        int w = wide ? (size == 0 ? 4 : size == 1 ? 6 : 8) : 4;
        int h = size == 0 ? 4 : size == 1 ? 6 : 8;
        int ox = wide ? (8 - w) / 2 : 0;
        int oy = size == 0 ? (wide ? 3 : 4) : size == 1 ? 3 : 2;
        if (wide) {
            int xs = t[zone == TattooZone.TORSO ? TattooZone.TORSO_X_SLOT : TattooZone.BACK_X_SLOT];
            int ys = t[zone == TattooZone.TORSO ? TattooZone.TORSO_Y_SLOT : TattooZone.BACK_Y_SLOT];
            if (xs > 0) ox = Math.min(xs - 1, 8 - w);
            if (ys > 0) oy = Math.min(ys - 1, 12 - h);
        } else if (zone != TattooZone.FACE_NECK) {
            int hs = t[heightSlot(zone)];
            if (hs > 0) oy = Math.min(LIMB_START[hs], 12 - h);
        }
        return new int[]{ox, oy, w, h};
    }

    private static int heightSlot(TattooZone z) {
        return switch (z) {
            case RIGHT_ARM -> TattooZone.RIGHT_ARM_HEIGHT_SLOT;
            case LEFT_ARM -> TattooZone.LEFT_ARM_HEIGHT_SLOT;
            case RIGHT_LEG -> TattooZone.RIGHT_LEG_HEIGHT_SLOT;
            default -> TattooZone.LEFT_LEG_HEIGHT_SLOT;
        };
    }
}
