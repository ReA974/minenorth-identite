package fr.minenorth.identite.skin;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.document.Gender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Queues de cheval, couettes, tresse et chignon : plus de volume, avec des élastiques et un chignon en relief. */
class HairUpdoTest {
    private static final int W = SkinComposer.SIZE;

    private static int[] hair(int style) {
        Appearance a = Catalog.defaults(Gender.FEMALE)
                .with(AppearanceSection.HAIR, new int[]{style, 3})
                .with(AppearanceSection.FACIAL_HAIR, new int[]{0, 0});
        return SkinComposer.compose(Gender.FEMALE, a);
    }

    private static int px(int[] p, int x, int y) { return p[y * W + x]; }

    private static boolean opaque(int[] p, int x, int y) { return (px(p, x, y) >>> 24) == 255; }

    /** Pixels opaques d'une colonne du dos du torse (couche extérieure : x 32 à 39, y 36 à 47 pour BODY_O.back). */
    private static int backColumn(int[] p, int dx) {
        int n = 0;
        for (int dy = 0; dy < 12; dy++) if (opaque(p, 32 + dx, 36 + dy)) n++;
        return n;
    }

    private static int backTotal(int[] p) {
        int n = 0;
        for (int dx = 0; dx < 8; dx++) n += backColumn(p, dx);
        return n;
    }

    private static int distance(int a, int b) {
        return Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF)) + Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF))
                + Math.abs((a & 0xFF) - (b & 0xFF));
    }

    private static int lum(int argb) { return ((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF); }

    @Test
    void ponytailsAreThickAndTaperedNotASingleStrand() {
        int[] low = hair(13);
        int[] high = hair(19);
        assertTrue(backTotal(low) >= 14, "queue basse : " + backTotal(low));
        assertTrue(backTotal(high) >= 20, "queue haute : " + backTotal(high));
        assertTrue(backColumn(high, 3) > backColumn(high, 2), "le centre est plus long que les bords");
        assertTrue(backColumn(low, 3) > backColumn(low, 2));
    }

    @Test
    void ponytailsHaveAContrastingHairTie() {
        for (int style : new int[]{13, 19}) {
            int[] p = hair(style);
            assertTrue(distance(px(p, 32 + 3, 36), px(p, 32 + 3, 37)) >= 90, "queue " + style + " : élastique");
            assertTrue(distance(px(p, 32 + 4, 36), px(p, 32 + 4, 37)) >= 90, "queue " + style + " : élastique");
        }
    }

    @Test
    void pigtailsAreTiedAtTheTopOnBothSides() {
        int[] p = hair(21);
        for (int dx : new int[]{0, 1, 6, 7}) {
            assertTrue(opaque(p, 32 + dx, 36), "couette colonne " + dx);
            assertTrue(distance(px(p, 32 + dx, 36), px(p, 32 + dx, 37)) >= 90, "élastique colonne " + dx);
        }
        assertTrue(backTotal(p) >= 28, "couettes : " + backTotal(p));
    }

    @Test
    void theBraidIsTiedAtTheEndAndIsThickerAtTheTop() {
        int[] p = hair(22);
        assertTrue(backColumn(p, 3) >= 9);
        assertTrue(backColumn(p, 2) >= 3, "tresse épaisse en haut");
        assertTrue(distance(px(p, 32 + 3, 36 + 9), px(p, 32 + 3, 36 + 8)) >= 90, "élastique en bas de la tresse");
    }

    @Test
    void theBunIsARoundBumpBrighterInTheMiddleThanOnItsRim() {
        int[] p = hair(20);
        // dos de la tête (couche extérieure : x 56 à 63, y 8 à 15) : le chignon occupe les colonnes 2 à 5, lignes 0 à 3
        int centre = lum(px(p, 56 + 3, 8 + 1));
        int rim = lum(px(p, 56 + 2, 8 + 1));
        assertTrue(centre > rim + 30, "centre " + centre + " vs bord " + rim);
    }

    @Test
    void shortHairHasNoTiesNorBun() {
        int[] p = hair(3);
        assertFalse(opaque(p, 32 + 3, 36));
    }
}
