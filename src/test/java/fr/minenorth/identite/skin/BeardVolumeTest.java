package fr.minenorth.identite.skin;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.document.Gender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Volume des barbes et moustaches : les poils sont aussi sur la couche extérieure, au-dessus de la couche sombre. */
class BeardVolumeTest {
    private static final int W = SkinComposer.SIZE;

    private static int[] withBeard(int style) {
        Appearance a = Catalog.defaults(Gender.MALE)
                .with(AppearanceSection.HAIR, new int[]{0, 1})
                .with(AppearanceSection.FACIAL_HAIR, new int[]{style, 2});
        return SkinComposer.compose(Gender.MALE, a);
    }

    private static int alpha(int[] p, int x, int y) { return p[y * W + x] >>> 24; }

    private static int outer(int style, int x0, int y0, int w, int h, boolean opaqueOnly) {
        int[] p = withBeard(style);
        int n = 0;
        for (int y = y0; y < y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) {
                int a = alpha(p, x, y);
                if (opaqueOnly ? a == 255 : a > 0 && a < 255) n++;
            }
        }
        return n;
    }

    @Test
    void solidBeardsAndMustachesHaveAnOpaqueOuterLayerOnTheFront() {
        for (int style = 3; style < 12; style++) {
            assertTrue(outer(style, 40, 8, 8, 8, true) >= 2, "style " + style);
        }
    }

    @Test
    void stubbleHasASoftSemiTransparentOuterLayer() {
        for (int style : new int[]{1, 2}) {
            assertTrue(outer(style, 40, 8, 8, 8, false) >= 3, "style " + style);
            assertEquals(0, outer(style, 40, 8, 8, 8, true), "pas de poil opaque pour la barbe de quelques jours");
        }
    }

    @Test
    void theOuterLayerAlsoReachesTheSidesForJawStyles() {
        for (int style : new int[]{7, 8, 9, 10}) {
            assertTrue(outer(style, 32, 8, 8, 8, true) >= 3, "style " + style + " côté droit");
            assertTrue(outer(style, 48, 8, 8, 8, true) >= 3, "style " + style + " côté gauche");
        }
    }

    @Test
    void theOuterLayerOnlyCoversWhereTheBaseHasHair() {
        int[] none = withBeard(0);
        for (int style = 1; style < 12; style++) {
            int[] p = withBeard(style);
            for (int y = 8; y < 16; y++) {
                for (int x = 32; x < 56; x++) {
                    if (alpha(p, x, y) == 0) continue;
                    int baseX = x - 32;
                    if (x >= 48) baseX = x - 32;   // couche extérieure à +32 de la couche de base (gauche : 16 -> 48)
                    assertNotEquals(none[y * W + baseX], p[y * W + baseX], "style " + style + " poil flottant en " + x + "," + y);
                }
            }
        }
    }

    @Test
    void theBeardNeverTouchesTheTopOfTheOuterHeadLayer() {
        int[] none = withBeard(0);
        for (int style = 1; style < 12; style++) {
            int[] p = withBeard(style);
            for (int y = 0; y < 8; y++) for (int x = 32; x < 64; x++) assertEquals(none[y * W + x], p[y * W + x], "style " + style);
        }
    }

    @Test
    void noBeardLeavesTheOuterHeadLayerEmpty() {
        int[] p = withBeard(0);
        for (int y = 0; y < 16; y++) for (int x = 32; x < 64; x++) assertEquals(0, alpha(p, x, y));
    }
}
