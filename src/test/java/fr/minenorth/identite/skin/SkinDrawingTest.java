package fr.minenorth.identite.skin;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.document.Gender;
import fr.minenorth.identite.drawing.DrawingKind;
import fr.minenorth.identite.drawing.Drawings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkinDrawingTest {
    private static final int W = SkinComposer.SIZE;

    private static Appearance bare(Gender g) {
        return Catalog.defaults(g).with(AppearanceSection.HAIR, new int[]{0, 1});
    }

    private static Appearance drawn(Gender g, DrawingKind kind, int x, int y, int color) {
        byte[] grid = new byte[kind.width() * kind.height()];
        grid[y * kind.width() + x] = (byte) color;
        return bare(g).withDrawings(Drawings.EMPTY.with(kind, grid));
    }

    private static int px(int[] p, int x, int y) { return p[y * W + x]; }

    private static int diffCount(int[] a, int[] b) {
        int n = 0;
        for (int i = 0; i < a.length; i++) if (a[i] != b[i]) n++;
        return n;
    }

    private static int luminance(int argb) {
        return ((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF);
    }

    @Test
    void facePixelReplacesTheCatalogFaceExactlyAtItsPlace() {
        int[] naked = SkinComposer.compose(Gender.MALE, bare(Gender.MALE));
        int[] p = SkinComposer.compose(Gender.MALE, drawn(Gender.MALE, DrawingKind.FACE, 2, 5, 14));
        assertEquals(DrawingKind.FACE.color(14), px(p, 8 + 2, 8 + 5));
        assertEquals(1, diffCount(naked, p));
    }

    @Test
    void transparentCellsLeaveTheCatalogFaceUntouched() {
        int[] naked = SkinComposer.compose(Gender.FEMALE, bare(Gender.FEMALE));
        Appearance blank = bare(Gender.FEMALE).withDrawings(Drawings.EMPTY.with(DrawingKind.FACE, new byte[64]));
        assertArrayEquals(naked, SkinComposer.compose(Gender.FEMALE, blank));
    }

    @Test
    void beardPixelUsesTheHairPaletteOnTheFace() {
        int[] naked = SkinComposer.compose(Gender.MALE, bare(Gender.MALE));
        int[] p = SkinComposer.compose(Gender.MALE, drawn(Gender.MALE, DrawingKind.BEARD, 4, 7, 3));
        assertEquals(DrawingKind.BEARD.color(3), px(p, 8 + 4, 8 + 7));
        assertEquals(2, diffCount(naked, p));   // le poil sur la tête et son volume sur la couche extérieure
        assertEquals(255, px(p, 40 + 4, 8 + 7) >>> 24);
    }

    @Test
    void beardIsDrawnOverTheFaceDrawing() {
        byte[] face = new byte[64];
        face[6 * 8 + 3] = 5;
        byte[] beard = new byte[64];
        beard[6 * 8 + 3] = 2;
        Appearance a = bare(Gender.MALE).withDrawings(Drawings.EMPTY.with(DrawingKind.FACE, face).with(DrawingKind.BEARD, beard));
        assertEquals(DrawingKind.BEARD.color(2), px(SkinComposer.compose(Gender.MALE, a), 8 + 3, 8 + 6));
    }

    @Test
    void torsoTattooBlendsWithTheSkinAtTheRightPlace() {
        int[] naked = SkinComposer.compose(Gender.MALE, bare(Gender.MALE));
        int[] p = SkinComposer.compose(Gender.MALE, drawn(Gender.MALE, DrawingKind.TATTOO_TORSO, 3, 4, 1));
        assertEquals(1, diffCount(naked, p));
        assertNotEquals(px(naked, 20 + 3, 20 + 4), px(p, 20 + 3, 20 + 4));
        assertTrue(luminance(px(p, 23, 24)) < luminance(px(naked, 23, 24)), "encre noire plus sombre que la peau");
    }

    @Test
    void backTattooGoesOnTheBackOfTheTorso() {
        int[] naked = SkinComposer.compose(Gender.MALE, bare(Gender.MALE));
        int[] p = SkinComposer.compose(Gender.MALE, drawn(Gender.MALE, DrawingKind.TATTOO_BACK, 0, 0, 1));
        assertNotEquals(px(naked, 32, 20), px(p, 32, 20));
        assertEquals(1, diffCount(naked, p));
    }

    @Test
    void armTattoosGoOnTheFrontOfEachArm() {
        int[] naked = SkinComposer.compose(Gender.MALE, bare(Gender.MALE));
        int[] right = SkinComposer.compose(Gender.MALE, drawn(Gender.MALE, DrawingKind.TATTOO_RIGHT_ARM, 1, 2, 1));
        int[] left = SkinComposer.compose(Gender.MALE, drawn(Gender.MALE, DrawingKind.TATTOO_LEFT_ARM, 1, 2, 1));
        assertNotEquals(px(naked, 44 + 1, 20 + 2), px(right, 44 + 1, 20 + 2));
        assertNotEquals(px(naked, 36 + 1, 52 + 2), px(left, 36 + 1, 52 + 2));
        assertEquals(1, diffCount(naked, right));
        assertEquals(1, diffCount(naked, left));
    }

    @Test
    void slimArmsClipTheFourthColumnInsteadOfPaintingTheNeighbouringFace() {
        int[] naked = SkinComposer.compose(Gender.FEMALE, bare(Gender.FEMALE));
        int[] right = SkinComposer.compose(Gender.FEMALE, drawn(Gender.FEMALE, DrawingKind.TATTOO_RIGHT_ARM, 3, 2, 1));
        int[] left = SkinComposer.compose(Gender.FEMALE, drawn(Gender.FEMALE, DrawingKind.TATTOO_LEFT_ARM, 3, 2, 1));
        assertArrayEquals(naked, right);
        assertArrayEquals(naked, left);
        int[] male = SkinComposer.compose(Gender.MALE, drawn(Gender.MALE, DrawingKind.TATTOO_RIGHT_ARM, 3, 2, 1));
        assertNotEquals(px(SkinComposer.compose(Gender.MALE, bare(Gender.MALE)), 47, 22), px(male, 47, 22));
    }

    @Test
    void legTattooAppearsOnBothLegs() {
        int[] naked = SkinComposer.compose(Gender.MALE, bare(Gender.MALE));
        int[] p = SkinComposer.compose(Gender.MALE, drawn(Gender.MALE, DrawingKind.TATTOO_LEGS, 2, 5, 1));
        assertNotEquals(px(naked, 4 + 2, 20 + 5), px(p, 4 + 2, 20 + 5));
        assertNotEquals(px(naked, 20 + 2, 52 + 5), px(p, 20 + 2, 52 + 5));
        assertEquals(2, diffCount(naked, p));
    }

    @Test
    void tattooColorsFollowTheInkPalette() {
        int[] a = SkinComposer.compose(Gender.MALE, drawn(Gender.MALE, DrawingKind.TATTOO_TORSO, 1, 1, 10));
        int[] b = SkinComposer.compose(Gender.MALE, drawn(Gender.MALE, DrawingKind.TATTOO_TORSO, 1, 1, 11));
        assertNotEquals(px(a, 21, 21), px(b, 21, 21));
    }

    @Test
    void drawingsAreIgnoredByTheOuterLayer() {
        Drawings every = Drawings.EMPTY;
        for (DrawingKind k : DrawingKind.values()) {
            byte[] g = new byte[k.width() * k.height()];
            java.util.Arrays.fill(g, (byte) 1);
            every = every.with(k, g);
        }
        int[] p = SkinComposer.compose(Gender.MALE, bare(Gender.MALE).withDrawings(every));
        for (int y = 32; y < 48; y++) for (int x = 0; x < 40; x++) assertEquals(0, p[y * W + x] >>> 24);
    }
}
