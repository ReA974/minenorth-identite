package fr.minenorth.identite.skin;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.document.Gender;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Volume et relief des cheveux : couche sombre dessous, reflets, dessous des mèches plus sombre, mèches sur les épaules. */
class HairVolumeTest {
    private static final int W = SkinComposer.SIZE;

    private static int[] hair(Gender g, int style) {
        Appearance a = Catalog.defaults(g)
                .with(AppearanceSection.HAIR, new int[]{style, 6})
                .with(AppearanceSection.FACIAL_HAIR, new int[]{0, 0});
        return SkinComposer.compose(g, a);
    }

    private static int[] bald(Gender g) { return hair(g, 0); }

    private static int lum(int argb) { return ((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF); }

    private static int px(int[] p, int x, int y) { return p[y * W + x]; }

    private static double avg(int[] p, int x0, int y0, int w, int h) {
        double sum = 0;
        for (int y = y0; y < y0 + h; y++) for (int x = x0; x < x0 + w; x++) sum += lum(px(p, x, y));
        return sum / (w * h);
    }

    @Test
    void hairIsAlsoPaintedUnderTheOuterLayerOnTopSidesAndBack() {
        int[] none = bald(Gender.MALE);
        int[] p = hair(Gender.MALE, 9);
        assertNotEquals(px(none, 11, 3), px(p, 11, 3), "dessous de la tête (dessus)");
        assertNotEquals(px(none, 3, 11), px(p, 3, 11), "dessous, côté droit");
        assertNotEquals(px(none, 27, 12), px(p, 27, 12), "dessous, nuque");
        assertTrue(lum(px(p, 11, 3)) < lum(px(p, 43, 3)) + 150, "la couche du dessous reste une teinte sombre des cheveux");
    }

    @Test
    void theFaceItselfIsNeverCoveredByTheUnderLayer() {
        int[] none = bald(Gender.MALE);
        int[] p = hair(Gender.MALE, 9);
        for (int y = 8; y < 16; y++) for (int x = 8; x < 16; x++) assertEquals(px(none, x, y), px(p, x, y), x + "," + y);
    }

    @Test
    void hairHasManyShadesAcrossTheHead() {
        int[] p = hair(Gender.MALE, 9);
        Set<Integer> colors = new HashSet<>();
        for (int y = 0; y < 16; y++) for (int x = 32; x < 64; x++) if ((px(p, x, y) >>> 24) != 0) colors.add(px(p, x, y));
        assertTrue(colors.size() >= 10, "nuances : " + colors.size());
    }

    @Test
    void theTopOfTheHeadIsAModestDomeBrighterAtTheCentre() {
        int[] p = hair(Gender.MALE, 3);
        double centre = avg(p, 40 + 2, 2, 4, 4);
        double wholeTop = avg(p, 40, 0, 8, 8);
        assertTrue(centre > wholeTop, "centre " + centre + " vs dessus entier " + wholeTop);
    }

    @Test
    void theBottomRowOfAStrandIsDarkerThanTheOneAbove() {
        int[] p = hair(Gender.FEMALE, 17);
        // nuque (couche extérieure) : 8 lignes de cheveux, la dernière est le dessous des mèches
        double last = avg(p, 56, 8 + 7, 8, 1);
        double above = avg(p, 56, 8 + 6, 8, 1);
        assertTrue(last < above, "dernière ligne " + last + " vs avant-dernière " + above);
    }

    @Test
    void longHairFramesTheShouldersInFront() {
        int[] long17 = hair(Gender.FEMALE, 17);
        int[] none = bald(Gender.FEMALE);
        for (int dx : new int[]{0, 1, 6, 7}) {
            assertNotEquals(px(none, 20 + dx, 36), px(long17, 20 + dx, 36), "mèche devant, colonne " + dx);
        }
        for (int dx : new int[]{2, 3, 4, 5}) {
            assertEquals(px(none, 20 + dx, 36), px(long17, 20 + dx, 36), "le centre de la poitrine reste libre, colonne " + dx);
        }
    }

    @Test
    void shortHairLeavesTheBodyAlone() {
        int[] short3 = hair(Gender.MALE, 3);
        int[] none = bald(Gender.MALE);
        for (int y = 16; y < W; y++) for (int x = 0; x < W; x++) assertEquals(px(none, x, y), px(short3, x, y), x + "," + y);
    }

    @Test
    void curlyAndStraightCutsDoNotShareTheSamePattern() {
        int[] curly = hair(Gender.MALE, 12);
        int[] straight = hair(Gender.MALE, 3);
        int differing = 0;
        for (int y = 0; y < 8; y++) for (int x = 40; x < 48; x++) if (px(curly, x, y) != px(straight, x, y)) differing++;
        assertTrue(differing >= 20, "pixels différents : " + differing);
    }
}
