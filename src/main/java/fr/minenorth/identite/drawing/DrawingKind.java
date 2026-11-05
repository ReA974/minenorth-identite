package fr.minenorth.identite.drawing;

import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.skin.Palette;

/**
 * Les dessins que le joueur peut faire : une grille de numéros de couleur (0 = transparent, 1 à N = couleur de la
 * palette du dessin). Taille et palette sont fixes : le serveur ne reçoit jamais de couleur libre.
 */
public enum DrawingKind {
    FACE("Visage", AppearanceSection.FACE, 8, 8, facePalette()),
    BEARD("Barbe et moustache", AppearanceSection.FACIAL_HAIR, 8, 8, hairPalette()),
    TATTOO_TORSO("Tatouage du torse", AppearanceSection.TATTOOS, 8, 12, inkPalette()),
    TATTOO_BACK("Tatouage du dos", AppearanceSection.TATTOOS, 8, 12, inkPalette()),
    TATTOO_RIGHT_ARM("Tatouage du bras droit", AppearanceSection.TATTOOS, 4, 12, inkPalette()),
    TATTOO_LEFT_ARM("Tatouage du bras gauche", AppearanceSection.TATTOOS, 4, 12, inkPalette()),
    /** La jambe droite (le nom date d'avant la séparation des jambes ; il reste pour garder les dessins enregistrés). */
    TATTOO_LEGS("Tatouage de la jambe droite", AppearanceSection.TATTOOS, 4, 12, inkPalette()),
    TATTOO_LEFT_LEG("Tatouage de la jambe gauche", AppearanceSection.TATTOOS, 4, 12, inkPalette());

    private final String label;
    private final AppearanceSection section;
    private final int width, height;
    private final int[] palette;

    DrawingKind(String label, AppearanceSection section, int width, int height, int[] palette) {
        this.label = label;
        this.section = section;
        this.width = width;
        this.height = height;
        this.palette = palette;
    }

    public String label() { return label; }
    /** Section de l'apparence à laquelle appartient ce dessin (les droits de modification suivent la section). */
    public AppearanceSection section() { return section; }
    public int width() { return width; }
    public int height() { return height; }
    public int paletteSize() { return palette.length; }

    /** Retrouve un type de dessin par son nom d'enum ; null si inconnu. */
    public static DrawingKind fromName(String name) {
        if (name == null) return null;
        for (DrawingKind k : values()) if (k.name().equals(name)) return k;
        return null;
    }

    /** Couleur ARGB du numéro donné, de 1 à {@link #paletteSize()}. */
    public int color(int index) { return palette[index - 1]; }

    /** Numéro de la couleur de la palette la plus proche de ce pixel ; 0 (transparent) si le pixel est presque transparent. */
    public int nearest(int argb) {
        if ((argb >>> 24) < 128) return 0;
        int best = 1;
        long bestDistance = Long.MAX_VALUE;
        for (int i = 1; i <= palette.length; i++) {
            long d = distance(argb, palette[i - 1]);
            if (d < bestDistance) {
                bestDistance = d;
                best = i;
            }
        }
        return best;
    }

    private static long distance(int a, int b) {
        long dr = ((a >> 16) & 0xFF) - ((b >> 16) & 0xFF);
        long dg = ((a >> 8) & 0xFF) - ((b >> 8) & 0xFF);
        long db = (a & 0xFF) - (b & 0xFF);
        return dr * dr + dg * dg + db * db;
    }

    /** Teintes de peau, ombres, yeux, lèvres, noir et blanc : de quoi redessiner un visage. */
    private static int[] facePalette() {
        int[] p = new int[24];
        for (int i = 0; i < 10; i++) p[i] = Palette.skinTone(i);
        int[] others = {
                0xFF5A3A28, 0xFF2A1A12, 0xFF101010, 0xFFF2F2F2,          // bruns, noir, blanc
                0xFF6B4423, 0xFF3A6AB8, 0xFF3E8A3E, 0xFF7A8EA0,          // yeux : marron, bleu, vert, gris
                0xFF8A7A30, 0xFFB8862A,                                  // yeux : noisette, ambre
                0xFFB05050, 0xFFD06080, 0xFF8A2A4A, 0xFFE0A090};         // lèvres et joues
        System.arraycopy(others, 0, p, 10, others.length);
        return p;
    }

    private static int[] hairPalette() {
        int[] p = new int[12];
        for (int i = 0; i < 12; i++) p[i] = Palette.hairColor(i);
        return p;
    }

    private static int[] inkPalette() {
        return new int[]{
                0xFF14141E, 0xFF1A2A5A, 0xFF2A5AA0, 0xFF4A8AD0, 0xFF2A8A8A, 0xFF2A6A3A, 0xFF4AAA4A, 0xFFE0C040,
                0xFFE08030, 0xFFA02828, 0xFFD03030, 0xFFD06090, 0xFF6A3A9A, 0xFF6B4423, 0xFF5A5A66, 0xFFE8E8F0};
    }
}
