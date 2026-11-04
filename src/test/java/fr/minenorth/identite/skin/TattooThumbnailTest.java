package fr.minenorth.identite.skin;

import fr.minenorth.identite.appearance.TattooZone;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Vignettes du salon de tatouage : le dessin de chaque motif en 8x8 pixels. */
class TattooThumbnailTest {
    @Test
    void everyMotifHasADrawingInEveryInk() {
        for (int m = 1; m <= TattooZone.MOTIFS; m++) {
            for (int ink = 0; ink < 3; ink++) {
                int[] px = SkinComposer.designPixels(m, ink);
                assertEquals(64, px.length);
                long filled = java.util.Arrays.stream(px).filter(v -> v != 0).count();
                assertTrue(filled >= 12, "motif " + m + " encre " + ink + " : " + filled);
            }
        }
    }

    @Test
    void noMotifReturnsAnEmptyDrawing() {
        for (int v : SkinComposer.designPixels(0, 0)) assertEquals(0, v);
        for (int v : SkinComposer.designPixels(TattooZone.MOTIFS + 1, 0)) assertEquals(0, v);
    }

    @Test
    void inkChangesTheColorsButNotTheShape() {
        for (int m = 1; m <= TattooZone.MOTIFS; m++) {
            int[] color = SkinComposer.designPixels(m, 0), black = SkinComposer.designPixels(m, 1), grey = SkinComposer.designPixels(m, 2);
            for (int i = 0; i < 64; i++) {
                assertEquals(color[i] == 0, black[i] == 0, "motif " + m);
                assertEquals(color[i] == 0, grey[i] == 0, "motif " + m);
            }
            assertFalse(java.util.Arrays.equals(color, black), "motif " + m);
        }
    }
}
