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
    LEGS(9, 10);

    /** Nombre total d'emplacements de la section TATTOOS. */
    public static final int SLOT_COUNT = 11;

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
        int[] out = new int[SLOT_COUNT];
        TattooZone[] zones = values();
        for (int i = 0; i < zones.length && i < old.length; i++) {
            out[zones[i].motifSlot] = old[i];
            if (zones[i].hasSize()) out[zones[i].sizeSlot] = 2;
        }
        return out;
    }
}
