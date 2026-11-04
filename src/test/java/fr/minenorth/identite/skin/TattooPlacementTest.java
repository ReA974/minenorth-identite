package fr.minenorth.identite.skin;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.appearance.TattooZone;
import fr.minenorth.identite.document.Gender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Jambe tatouee (droite, gauche ou les deux) et position libre du tatouage sur le torse et le dos. */
class TattooPlacementTest {
    private static final int W = SkinComposer.SIZE;

    private static int[] none() {
        return SkinComposer.compose(Gender.MALE, Catalog.defaults(Gender.MALE).with(AppearanceSection.HAIR, new int[]{0, 1}));
    }

    private static int[] with(int[] t) {
        Appearance a = Catalog.defaults(Gender.MALE).with(AppearanceSection.HAIR, new int[]{0, 1}).with(AppearanceSection.TATTOOS, t);
        return SkinComposer.compose(Gender.MALE, a);
    }

    private static int changed(int[] a, int[] b, int x0, int y0, int w, int h) {
        int n = 0;
        for (int y = y0; y < y0 + h; y++) for (int x = x0; x < x0 + w; x++) if (a[y * W + x] != b[y * W + x]) n++;
        return n;
    }

    private static int[] legs(int side) {
        int[] t = new int[TattooZone.TOTAL_SLOTS];
        t[TattooZone.LEGS.motifSlot()] = 5;
        t[TattooZone.LEGS.sizeSlot()] = 2;
        t[TattooZone.LEG_SIDE_SLOT] = side;
        return t;
    }

    @Test
    void bothLegsByDefault() {
        int[] base = none(), p = with(legs(0));
        assertTrue(changed(base, p, 4, 20, 4, 12) > 0, "jambe droite");
        assertTrue(changed(base, p, 20, 52, 4, 12) > 0, "jambe gauche");
    }

    @Test
    void rightLegOnly() {
        int[] base = none(), p = with(legs(1));
        assertTrue(changed(base, p, 4, 20, 4, 12) > 0);
        assertEquals(0, changed(base, p, 20, 52, 4, 12));
    }

    @Test
    void leftLegOnly() {
        int[] base = none(), p = with(legs(2));
        assertEquals(0, changed(base, p, 4, 20, 4, 12));
        assertTrue(changed(base, p, 20, 52, 4, 12) > 0);
    }

    private static int[] torso(int size, int x, int y) {
        int[] t = new int[TattooZone.TOTAL_SLOTS];
        t[TattooZone.TORSO.motifSlot()] = 6;
        t[TattooZone.TORSO.sizeSlot()] = size;
        t[TattooZone.TORSO_X_SLOT] = x;
        t[TattooZone.TORSO_Y_SLOT] = y;
        return t;
    }

    @Test
    void thePositionMovesTheTorsoTattoo() {
        int[] base = none();
        // petit (4x4) tout en haut a gauche, puis tout en bas a droite
        int[] topLeft = with(torso(0, 1, 1)), bottomRight = with(torso(0, 8, 12));
        assertTrue(changed(base, topLeft, 20, 20, 4, 4) > 0);
        assertEquals(0, changed(base, topLeft, 24, 24, 4, 8));
        assertTrue(changed(base, bottomRight, 24, 28, 4, 4) > 0);
        assertEquals(0, changed(base, bottomRight, 20, 20, 4, 8));
    }

    @Test
    void thePositionNeverLeavesTheTorso() {
        int[] base = none();
        for (int size = 0; size < 3; size++) {
            for (int x : new int[]{0, 1, 5, 8}) {
                for (int y : new int[]{0, 1, 7, 12}) {
                    int[] p = with(torso(size, x, y));
                    int total = changed(base, p, 0, 0, W, W);
                    assertEquals(total, changed(base, p, 20, 20, 8, 12), "taille " + size + " x " + x + " y " + y);
                }
            }
        }
    }

    @Test
    void positionZeroKeepsTheOldCenteredPlacement() {
        assertArrayEquals(with(torso(1, 0, 0)), with(torso(1, 0, 0)));
        int[] t = torso(2, 0, 0);
        int[] centered = with(t);
        assertNotEquals(0, changed(none(), centered, 20, 20, 8, 12));
    }
}
