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

    /** Piercings (au salon de tatouage), par réglage : nez, sourcil, lèvre, nombril ; une ligne par option (0 = aucun). */
    public static final long[] PIERCING_NOSE = {0, 2_000, 2_000, 3_500, 2_500, 3_000};
    public static final long[] PIERCING_BROW = {0, 2_500, 3_000, 4_500, 5_500};
    public static final long[] PIERCING_LIP = {0, 3_000, 3_000, 5_000, 3_000, 3_000};
    public static final long[] PIERCING_NAVEL = {0, 3_500, 4_000, 5_000, 4_500};
    /** Supplément pour l'or et l'or rose, et retrait d'un piercing. */
    public static final long PRECIOUS_METAL = 1_000, PIERCING_REMOVAL = 1_000;

    public static long quote(SalonKind kind, Appearance before, Appearance after) {
        if (kind == null) return 0;
        return switch (kind) {
            case TATTOO -> tattoos(before, after) + piercings(before, after);
            case HAIR -> hair(before, after);
            case MAKEUP -> makeup(before, after);
        };
    }

    /** Maquillage : lèvres, yeux, joues (la couleur seule se paie moins cher), particularité de la peau, teint. */
    public static final long LIPS = 1_500, EYES = 2_000, BLUSH = 1_000, MAKEUP_COLOR = 500, SKIN_MARK = 2_500, SKIN_TONE = 4_000;

    private static long makeup(Appearance before, Appearance after) {
        int[] b = before.get(AppearanceSection.MAKEUP), a = after.get(AppearanceSection.MAKEUP);
        long total = part(b[0], a[0], b[1], a[1], LIPS) + part(b[2], a[2], b[3], a[3], EYES) + part(b[4], a[4], b[5], a[5], BLUSH);
        int[] sb = before.get(AppearanceSection.SKIN), sa = after.get(AppearanceSection.SKIN);
        if (sb[1] != sa[1] && sa[1] != 0) total += SKIN_MARK;
        if (sb[0] != sa[0]) total += SKIN_TONE;
        return total;
    }

    /** Un maquillage : rien si on l'enlève ; le prix plein si le style change ; un petit prix si seule la couleur change. */
    private static long part(int styleBefore, int styleAfter, int colorBefore, int colorAfter, long price) {
        if (styleAfter == 0) return 0;
        if (styleAfter != styleBefore) return price;
        return colorAfter != colorBefore ? MAKEUP_COLOR : 0;
    }

    /** Piercings : ce qui est posé ou changé se paie (et le métal s'il est précieux) ; en retirer un coûte un petit forfait. */
    private static long piercings(Appearance before, Appearance after) {
        int[] b = before.get(AppearanceSection.PIERCINGS), a = after.get(AppearanceSection.PIERCINGS);
        long[][] tables = {PIERCING_NOSE, PIERCING_BROW, PIERCING_LIP, PIERCING_NAVEL};
        int[] slots = {1, 2, 3, 6};
        boolean precious = a[4] == 1 || a[4] == 3;
        boolean metalChanged = a[4] != b[4];
        long total = 0;
        for (int i = 0; i < slots.length; i++) {
            int slot = slots[i];
            if (a[slot] == 0) {
                if (b[slot] != 0) total += PIERCING_REMOVAL;
                continue;
            }
            if (a[slot] == b[slot] && !metalChanged) continue;
            total += tables[i][Math.min(a[slot], tables[i].length - 1)] + (precious ? PRECIOUS_METAL : 0);
        }
        return total;
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
            case RIGHT_LEG -> new int[]{9, 10, TattooZone.RIGHT_LEG_FACE_SLOT, TattooZone.RIGHT_LEG_HEIGHT_SLOT};
            case LEFT_LEG -> new int[]{20, 21, TattooZone.LEFT_LEG_FACE_SLOT, TattooZone.LEFT_LEG_HEIGHT_SLOT};
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
