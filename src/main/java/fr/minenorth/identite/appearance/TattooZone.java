package fr.minenorth.identite.appearance;

/**
 * Les zones du corps qui reçoivent un tatouage du catalogue. Chaque zone a un emplacement « motif » ; toutes sauf le
 * visage ont aussi, juste après, un emplacement « taille » (0 petit, 1 moyen, 2 grand). Le visage n'a que de petites
 * marques discrètes.
 */
public enum TattooZone {
    FACE_NECK(0, -1),
    TORSO(1, 2),
    BACK(3, 4),
    RIGHT_ARM(5, 6),
    LEFT_ARM(7, 8),
    RIGHT_LEG(9, 10),
    LEFT_LEG(20, 21);

    /** Nombre total d'emplacements de la section TATTOOS. */
    public static final int SLOT_COUNT = 11;

    /** Emplacement de l'encre (couleurs, noir ou gris ombré), commun à tous les tatouages du catalogue. */
    public static final int INK_SLOT = 11;

    /** Nombre d'emplacements de la section TATTOOS : les zones puis l'encre. */
    public static final int TOTAL_SLOTS = 26;

    /** Position libre du tatouage sur le torse et le dos : colonne et ligne (0 = centré comme avant, puis 1 à 8 et 1 à 12). */
    public static final int TORSO_X_SLOT = 16, TORSO_Y_SLOT = 17, BACK_X_SLOT = 18, BACK_Y_SLOT = 19;

    /** Face (avant, extérieur, intérieur, arrière) et hauteur (centré, épaule, bras, avant-bras, poignet) des tatouages de bras. */
    public static final int RIGHT_ARM_FACE_SLOT = 12, RIGHT_ARM_HEIGHT_SLOT = 13, LEFT_ARM_FACE_SLOT = 14, LEFT_ARM_HEIGHT_SLOT = 15;

    /**
     * Fiche d'une version précédente vers la fiche actuelle. Avant, les deux jambes partageaient un seul tatouage (puis,
     * pendant quelques versions, avec un choix de côté en dernière position) : il est gardé sur la jambe qu'il couvrait, ou
     * sur les deux. Les valeurs ne sont pas vérifiées ici.
     */
    public static int[] upgrade(int[] old) {
        if (old.length == TOTAL_SLOTS) return old;
        if (old.length >= 22) return java.util.Arrays.copyOf(old, TOTAL_SLOTS);   // sans face ni hauteur de jambe
        if (old.length == 6) old = migrateLegacy(old);
        int side = old.length == 21 ? old[20] : 0;   // 0 les deux, 1 droite, 2 gauche
        int[] out = java.util.Arrays.copyOf(old, TOTAL_SLOTS);
        out[20] = 0;
        out[21] = 0;
        if (side != 1) {
            out[20] = old[9];
            out[21] = old[10];
        }
        if (side == 2) {
            out[9] = 0;
            out[10] = 0;
        }
        return out;
    }

    /** Face (avant, extérieur, intérieur, arrière) et hauteur (centré, haut, cuisse, mollet, cheville) des tatouages de jambe. */
    public static final int RIGHT_LEG_FACE_SLOT = 22, RIGHT_LEG_HEIGHT_SLOT = 23, LEFT_LEG_FACE_SLOT = 24, LEFT_LEG_HEIGHT_SLOT = 25;

    /** Nombre de figures du catalogue. */
    public static final int MOTIFS = 50;

    private final int motifSlot;
    private final int sizeSlot;

    TattooZone(int motifSlot, int sizeSlot) {
        this.motifSlot = motifSlot;
        this.sizeSlot = sizeSlot;
    }

    public int motifSlot() { return motifSlot; }

    public boolean hasSize() { return sizeSlot >= 0; }

    /** Indice de l'emplacement « taille » ; -1 pour le visage, qui n'a pas de taille. */
    public int sizeSlot() { return sizeSlot; }

    /**
     * Ancienne fiche (6 zones, sans taille) vers la fiche actuelle : les motifs sont gardés et, comme avant, en grand.
     * Les valeurs ne sont pas vérifiées ici.
     */
    public static int[] migrateLegacy(int[] old) {
        int[] out = new int[SLOT_COUNT + 1];
        TattooZone[] zones = values();
        for (int i = 0; i < zones.length && i < old.length; i++) {
            out[zones[i].motifSlot] = old[i];
            if (zones[i].hasSize()) out[zones[i].sizeSlot] = 2;
        }
        return out;
    }
}
