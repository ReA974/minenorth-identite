package fr.minenorth.identite.hair;

import fr.minenorth.identite.skin.Palette;

/** Texture des blocs de cheveux : trois bandes de 16 × 16 (cheveux, élastique, mèche sombre), mèches verticales. */
public final class HairTexture {
    private HairTexture() {}

    public static final int WIDTH = 16, BAND = 16, HEIGHT = BAND * 3;
    private static final int TIE_COLOR = 0xFFD04060;

    /** Pixels ARGB, ligne par ligne, pour une couleur de cheveux donnée. */
    public static int[] pixels(int hairColor) {
        int[] out = new int[WIDTH * HEIGHT];
        for (int tone = 0; tone < 3; tone++) {
            for (int y = 0; y < BAND; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    out[(tone * BAND + y) * WIDTH + x] = tonePixel(hairColor, tone, x, y);
                }
            }
        }
        return out;
    }

    /** Texture d'un métal de piercing : métal, reflet brillant, ombre (mêmes bandes que les cheveux). */
    public static int[] metalPixels(int metal) {
        int[] out = new int[WIDTH * HEIGHT];
        double[] factors = {1.0, 1.45, 0.58};
        for (int tone = 0; tone < 3; tone++) {
            for (int y = 0; y < BAND; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    double sheen = 0.94 + 0.12 * (((x * 7 + y * 13) % 5) / 4.0);
                    out[(tone * BAND + y) * WIDTH + x] = Palette.shade(metal | 0xFF000000, factors[tone] * sheen);
                }
            }
        }
        return out;
    }

    private static int tonePixel(int base, int tone, int x, int y) {
        if (tone == HairPieces.TIE) return Palette.shade(Palette.mix(base, TIE_COLOR, 0.78), y % 4 == 0 ? 0.9 : 1.0);
        int h = x * 374761393 + 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        double strand = 0.84 + ((h >>> 8) & 0xFF) / 255.0 * 0.28;      // une mèche par colonne
        strand *= (y % 5 == 2) ? 1.06 : 1.0;
        double f = tone == HairPieces.SHADOW ? 0.76 : 1.0;
        return Palette.shade(base | 0xFF000000, strand * f);
    }
}
