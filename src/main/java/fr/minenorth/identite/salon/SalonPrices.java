package fr.minenorth.identite.salon;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.TattooZone;
import fr.minenorth.identite.drawing.DrawingKind;

/**
 * Tarifs des salons, en centimes. Le prix est toujours calculé par le serveur à partir de la fiche avant et après ; le
 * client ne fait que l'afficher. On ne paie que ce qui change.
 */
public final class SalonPrices {
    private SalonPrices() {}

    /** Tatouage du catalogue selon sa taille, petite marque du visage, supplément pour l'encre de couleur. */
    public static final long TATTOO_SMALL = 1_500, TATTOO_MEDIUM = 3_000, TATTOO_LARGE = 5_000, FACE_MARK = 1_000, COLOR_INK = 1_000;
    /** Retirer un tatouage (laser). */
    public static final long REMOVAL = 2_000;
    /** Tatouage dessiné par le joueur : bien plus cher qu'un motif du catalogue. */
    public static final long DRAWN_BODY = 12_000, DRAWN_LIMB = 8_000;
    /** Coiffeur : coupe, couleur des cheveux, style de barbe, couleur de barbe. */
    public static final long HAIR_CUT = 2_500, HAIR_COLOR = 1_500, BEARD_STYLE = 1_000, BEARD_COLOR = 800;

    public static long quote(SalonKind kind, Appearance before, Appearance after) {
        if (kind == null) return 0;
        return kind == SalonKind.TATTOO ? tattoos(before, after) : hair(before, after);
    }

    // ------------------------------------------------------------------ tatouages

    /** Les emplacements propres à chaque zone : motif, taille, et ce qui règle son placement. */
    private static int[] slots(TattooZone zone) {
        return switch (zone) {
            case FACE_NECK -> new int[]{0};
            case TORSO -> new int[]{1, 2, TattooZone.TORSO_X_SLOT, TattooZone.TORSO_Y_SLOT};
            case BACK -> new int[]{3, 4, TattooZone.BACK_X_SLOT, TattooZone.BACK_Y_SLOT};
            case RIGHT_ARM -> new int[]{5, 6, TattooZone.RIGHT_ARM_FACE_SLOT, TattooZone.RIGHT_ARM_HEIGHT_SLOT};
            case LEFT_ARM -> new int[]{7, 8, TattooZone.LEFT_ARM_FACE_SLOT, TattooZone.LEFT_ARM_HEIGHT_SLOT};
            case RIGHT_LEG -> new int[]{9, 10};
            case LEFT_LEG -> new int[]{20, 21};
        };
    }

    private static long tattoos(Appearance before, Appearance after) {
        int[] b = before.get(AppearanceSection.TATTOOS), a = after.get(AppearanceSection.TATTOOS);
        long total = 0;
        boolean inkChanged = b[TattooZone.INK_SLOT] != a[TattooZone.INK_SLOT];
        for (TattooZone zone : TattooZone.values()) {
            int motifBefore = b[zone.motifSlot()], motifAfter = a[zone.motifSlot()];
            boolean changed = false;
            for (int slot : slots(zone)) changed |= b[slot] != a[slot];
            if (motifAfter == 0) {
                if (motifBefore != 0) total += REMOVAL;
                continue;
            }
            if (!changed && !(inkChanged && zone != TattooZone.FACE_NECK)) continue;
            if (zone == TattooZone.FACE_NECK) {
                total += FACE_MARK;
            } else {
                total += switch (a[zone.sizeSlot()]) {
                    case 0 -> TATTOO_SMALL;
                    case 1 -> TATTOO_MEDIUM;
                    default -> TATTOO_LARGE;
                };
                if (a[TattooZone.INK_SLOT] == 0) total += COLOR_INK;
            }
        }
        for (DrawingKind kind : DrawingKind.values()) {
            if (kind.section() != AppearanceSection.TATTOOS) continue;
            byte[] gridBefore = before.drawings().get(kind), gridAfter = after.drawings().get(kind);
            if (java.util.Arrays.equals(gridBefore, gridAfter)) continue;
            if (isBlank(gridAfter)) {
                if (!isBlank(gridBefore)) total += REMOVAL;
            } else {
                total += (kind == DrawingKind.TATTOO_TORSO || kind == DrawingKind.TATTOO_BACK) ? DRAWN_BODY : DRAWN_LIMB;
            }
        }
        return total;
    }

    private static boolean isBlank(byte[] grid) {
        for (byte v : grid) if (v != 0) return false;
        return true;
    }

    // ------------------------------------------------------------------ coiffeur

    private static long hair(Appearance before, Appearance after) {
        int[] hb = before.get(AppearanceSection.HAIR), ha = after.get(AppearanceSection.HAIR);
        int[] bb = before.get(AppearanceSection.FACIAL_HAIR), ba = after.get(AppearanceSection.FACIAL_HAIR);
        long total = 0;
        if (hb[0] != ha[0]) total += HAIR_CUT;
        if (hb[1] != ha[1] && ha[0] != 0) total += HAIR_COLOR;
        if (bb[0] != ba[0]) total += BEARD_STYLE;
        if (bb[1] != ba[1] && ba[0] != 0) total += BEARD_COLOR;
        return total;
    }
}
