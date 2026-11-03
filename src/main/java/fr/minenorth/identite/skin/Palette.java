package fr.minenorth.identite.skin;

/** Couleurs ARGB des options du catalogue. Les indices correspondent aux emplacements de {@code Catalog}. */
public final class Palette {
    private Palette() {}

    private static final int[] SKIN = {
            0xFFF6DDC8, 0xFFF0CBA8, 0xFFE5B88F, 0xFFD9A577, 0xFFC68E5F,
            0xFFB07A55, 0xFF9A6644, 0xFF7E4E32, 0xFF63392A, 0xFF4A2A1E};
    private static final int[] HAIR = {
            0xFF1A1008, 0xFF2E1B10, 0xFF4A2C1A, 0xFF6B4423, 0xFF8A5A30, 0xFFB07A3C,
            0xFFD8B060, 0xFFEEDDA0, 0xFFA04020, 0xFF7A7A7A, 0xFFE8E8E8, 0xFF2A4A7A};
    private static final int[] EYE = {
            0xFF3A2410, 0xFF6B4423, 0xFF8A7A30, 0xFFB8862A, 0xFF3E8A3E, 0xFF3A6AB8, 0xFF7A8EA0, 0xFF8A8A8A};
    private static final int[] LIP = {
            0xFFB05050, 0xFFC03030, 0xFFD06080, 0xFF8A2A4A, 0xFFD08060, 0xFF602030, 0xFF903090, 0xFF202020};
    private static final int[] INK = {
            0xFF000000, 0xFF202030, 0xFF1A2A5A, 0xFFA02828, 0xFF2A6A3A, 0xFF2A5AA0, 0xFF5A5A66};

    public static final int WHITE = 0xFFF2F2F2;
    public static final int UNDERWEAR = 0xFF1A1A22;
    public static final int TRANSPARENT = 0;

    public static int skinTone(int i) { return SKIN[i]; }
    public static int hairColor(int i) { return HAIR[i]; }
    public static int eyeColor(int i) { return EYE[i]; }
    public static int lipColor(int i) { return LIP[i]; }
    /** Encre d'un motif de tatouage (1 à 6). */
    public static int ink(int motif) { return INK[motif]; }

    /** Multiplie les canaux RVB (facteur &lt; 1 assombrit, &gt; 1 éclaircit). */
    public static int shade(int argb, double factor) {
        int a = argb >>> 24;
        int r = clamp(((argb >> 16) & 0xFF) * factor);
        int g = clamp(((argb >> 8) & 0xFF) * factor);
        int b = clamp((argb & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Mélange t (0 à 1) de b dans a, alpha de a conservé. */
    public static int mix(int a, int b, double t) {
        int r = clamp(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = clamp(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = clamp((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (a & 0xFF000000) | (r << 16) | (g << 8) | bl;
    }

    private static int clamp(double v) { return (int) Math.max(0, Math.min(255, Math.round(v))); }
}
