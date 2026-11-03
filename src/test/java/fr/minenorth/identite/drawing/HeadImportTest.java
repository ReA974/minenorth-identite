package fr.minenorth.identite.drawing;

import fr.minenorth.identite.skin.Palette;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HeadImportTest {
    private static int[] skin(int h) { return new int[64 * h]; }

    private static void fillRect(int[] px, int x0, int y0, int w, int h, int argb) {
        for (int y = y0; y < y0 + h; y++) for (int x = x0; x < x0 + w; x++) px[y * 64 + x] = argb;
    }

    @Test
    void nearestFindsTheExactPaletteColorAndIgnoresTransparency() {
        for (DrawingKind k : DrawingKind.values()) {
            for (int i = 1; i <= k.paletteSize(); i++) assertEquals(i, k.nearest(k.color(i)), k + " " + i);
            assertEquals(0, k.nearest(0x00FFFFFF));
            assertEquals(0, k.nearest(0x7FFFFFFF));
        }
    }

    @Test
    void nearestPicksTheClosestColor() {
        assertEquals(DrawingKind.FACE.nearest(0xFF101010), DrawingKind.FACE.nearest(0xFF0B0B0B));
    }

    @Test
    void theFaceOfTheSkinBecomesTheDrawing() {
        int[] px = skin(64);
        fillRect(px, 8, 8, 8, 8, Palette.skinTone(3));
        byte[] face = HeadImport.faceFromSkin(px, 64, 64);
        assertEquals(64, face.length);
        int expected = DrawingKind.FACE.nearest(Palette.skinTone(3));
        for (byte b : face) assertEquals(expected, b);
    }

    @Test
    void pixelsAreReadAtTheirPlace() {
        int[] px = skin(64);
        fillRect(px, 8, 8, 8, 8, Palette.skinTone(3));
        px[(8 + 4) * 64 + (8 + 2)] = 0xFF101010;
        byte[] face = HeadImport.faceFromSkin(px, 64, 64);
        assertEquals(DrawingKind.FACE.nearest(0xFF101010), face[4 * 8 + 2]);
        assertEquals(DrawingKind.FACE.nearest(Palette.skinTone(3)), face[4 * 8 + 3]);
    }

    @Test
    void opaqueHatPixelsCoverTheFace() {
        int[] px = skin(64);
        fillRect(px, 8, 8, 8, 8, Palette.skinTone(3));
        px[8 * 64 + 40] = 0xFF101010;
        byte[] face = HeadImport.faceFromSkin(px, 64, 64);
        assertEquals(DrawingKind.FACE.nearest(0xFF101010), face[0]);
        assertEquals(DrawingKind.FACE.nearest(Palette.skinTone(3)), face[1]);
    }

    @Test
    void oldSkinsOf32RowsStillWork() {
        int[] px = skin(32);
        fillRect(px, 8, 8, 8, 8, Palette.skinTone(5));
        byte[] face = HeadImport.faceFromSkin(px, 64, 32);
        assertEquals(DrawingKind.FACE.nearest(Palette.skinTone(5)), face[10]);
    }

    @Test
    void anEmptySkinGivesATransparentDrawing() {
        byte[] face = HeadImport.faceFromSkin(skin(64), 64, 64);
        for (byte b : face) assertEquals(0, b);
    }

    @Test
    void resultIsAlwaysAValidDrawing() {
        int[] px = skin(64);
        java.util.Random r = new java.util.Random(4);
        for (int i = 0; i < px.length; i++) px[i] = r.nextInt();
        Drawings.EMPTY.with(DrawingKind.FACE, HeadImport.faceFromSkin(px, 64, 64));
    }

    @Test
    void wrongSizesAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> HeadImport.faceFromSkin(new int[16 * 16], 16, 16));
        assertThrows(IllegalArgumentException.class, () -> HeadImport.faceFromSkin(new int[10], 64, 64));
    }
}
