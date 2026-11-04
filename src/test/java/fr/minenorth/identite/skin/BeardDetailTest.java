package fr.minenorth.identite.skin;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.document.Gender;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Les barbes ne se limitent plus au devant du visage : favoris et mâchoire montent sur les côtés de la tête. */
class BeardDetailTest {
    private static final int W = SkinComposer.SIZE;

    private static int[] withBeard(int style) {
        Appearance a = Catalog.defaults(Gender.MALE)
                .with(AppearanceSection.HAIR, new int[]{0, 1})
                .with(AppearanceSection.FACIAL_HAIR, new int[]{style, 2});
        return SkinComposer.compose(Gender.MALE, a);
    }

    /** Pixels qui changent sur une zone du skin (x0, y0, largeur, hauteur) par rapport à un visage sans barbe. */
    private static int changed(int style, int x0, int y0, int w, int h) {
        int[] none = withBeard(0);
        int[] beard = withBeard(style);
        int n = 0;
        for (int y = y0; y < y0 + h; y++) for (int x = x0; x < x0 + w; x++) if (none[y * W + x] != beard[y * W + x]) n++;
        return n;
    }

    private static int onRightSide(int style) { return changed(style, 0, 8, 8, 8); }
    private static int onLeftSide(int style) { return changed(style, 16, 8, 8, 8); }
    private static int onFront(int style) { return changed(style, 8, 8, 8, 8); }

    @Test
    void sideburnsAndJawStylesReachBothSidesOfTheHead() {
        for (int style : new int[]{1, 2, 7, 8, 9, 10}) {
            assertTrue(onRightSide(style) >= 3, "style " + style + " côté droit : " + onRightSide(style));
            assertTrue(onLeftSide(style) >= 3, "style " + style + " côté gauche : " + onLeftSide(style));
        }
    }

    @Test
    void bothSidesAreSymmetric() {
        for (int style = 1; style < 12; style++) assertEquals(onRightSide(style), onLeftSide(style), "style " + style);
    }

    @Test
    void mustachesAndGoateesStayOnTheFront() {
        for (int style : new int[]{3, 4, 5, 6, 11}) {
            assertEquals(0, onRightSide(style), "style " + style);
            assertEquals(0, onLeftSide(style), "style " + style);
            assertTrue(onFront(style) >= 2, "style " + style);
        }
    }

    @Test
    void fullBeardCoversMoreThanShortBeardWhichCoversMoreThanStubbleLight() {
        int full = onFront(9) + onRightSide(9) + onLeftSide(9);
        int shortBeard = onFront(8) + onRightSide(8) + onLeftSide(8);
        int chinstrap = onFront(7) + onRightSide(7) + onLeftSide(7);
        assertTrue(full > shortBeard, "pleine " + full + " vs courte " + shortBeard);
        assertTrue(full > chinstrap);
    }

    @Test
    void beardNeverTouchesTheHairOverlayOrTheBody() {
        int[] none = withBeard(0);
        for (int style = 1; style < 12; style++) {
            int[] beard = withBeard(style);
            for (int y = 0; y < W; y++) {
                for (int x = 0; x < W; x++) {
                    boolean head = (y < 16 && x < 32) || (y >= 8 && y < 16 && x >= 32 && x < 56);   // tête + volume sur le devant et les côtés
                    if (!head) assertEquals(none[y * W + x], beard[y * W + x], "style " + style + " touche " + x + "," + y);
                }
            }
        }
    }

    @Test
    void solidBeardsHaveHairTextureNotAFlatColor() {
        int[] p = withBeard(9);
        Set<Integer> colors = new HashSet<>();
        for (int y = 8; y < 16; y++) for (int x = 0; x < 24; x++) colors.add(p[y * W + x]);
        int[] none = withBeard(0);
        Set<Integer> base = new HashSet<>();
        for (int y = 8; y < 16; y++) for (int x = 0; x < 24; x++) base.add(none[y * W + x]);
        colors.removeAll(base);
        assertTrue(colors.size() >= 4, "nuances de poil : " + colors.size());
    }

    @Test
    void mustacheIsLighterOnTopAndDarkerBelow() {
        int[] p = withBeard(3);
        int top = luminance(p[(8 + 5) * W + 8 + 2]);
        int below = luminance(p[(8 + 6) * W + 8 + 2]);
        assertTrue(top > below, "haut " + top + " bas " + below);
    }

    private static int luminance(int argb) {
        return ((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF);
    }
}
