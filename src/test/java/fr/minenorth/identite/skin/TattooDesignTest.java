package fr.minenorth.identite.skin;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.document.Gender;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Les tatouages du catalogue sont de vrais petits dessins (contour et couleurs), pas un motif répété. */
class TattooDesignTest {
    private static final int W = SkinComposer.SIZE;

    private static Appearance bare(Gender g) {
        return Catalog.defaults(g).with(AppearanceSection.HAIR, new int[]{0, 1});
    }

    private static int[] naked(Gender g) { return SkinComposer.compose(g, bare(g)); }

    private static int[] tattoo(Gender g, int zone, int motif) {
        int[] t = new int[6];
        t[zone] = motif;
        return SkinComposer.compose(g, bare(g).with(AppearanceSection.TATTOOS, t));
    }

    private static int changed(int[] a, int[] b, int x0, int y0, int w, int h) {
        int n = 0;
        for (int y = y0; y < y0 + h; y++) for (int x = x0; x < x0 + w; x++) if (a[y * W + x] != b[y * W + x]) n++;
        return n;
    }

    private static int changedTotal(int[] a, int[] b) { return changed(a, b, 0, 0, W, W); }

    @Test
    void everyMotifIsAFigureNotAWallpaper() {
        int[] base = naked(Gender.MALE);
        for (int m = 1; m <= 12; m++) {
            int n = changedTotal(base, tattoo(Gender.MALE, 1, m));
            assertTrue(n >= 18 && n <= 50, "motif " + m + " : " + n + " pixels sur le torse");
        }
    }

    @Test
    void theTorsoFigureStaysInTheMiddleOfTheChest() {
        int[] base = naked(Gender.MALE);
        for (int m = 1; m <= 12; m++) {
            int[] p = tattoo(Gender.MALE, 1, m);
            assertEquals(changedTotal(base, p), changed(base, p, 20, 22, 8, 8), "motif " + m + " déborde de la zone");
        }
    }

    @Test
    void theBackFigureStaysInTheMiddleOfTheBack() {
        int[] base = naked(Gender.FEMALE);
        for (int m = 1; m <= 12; m++) {
            int[] p = tattoo(Gender.FEMALE, 2, m);
            assertEquals(changedTotal(base, p), changed(base, p, 32, 22, 8, 8), "motif " + m + " déborde de la zone");
        }
    }

    @Test
    void aLimbTattooIsOneFigureOnTheFrontFaceOnly() {
        int[] base = naked(Gender.MALE);
        for (int m = 1; m <= 12; m++) {
            int[] right = tattoo(Gender.MALE, 3, m);
            assertEquals(changedTotal(base, right), changed(base, right, 44, 22, 4, 8), "bras droit, motif " + m);
            int[] legs = tattoo(Gender.MALE, 5, m);
            assertEquals(changedTotal(base, legs), changed(base, legs, 4, 22, 4, 8) + changed(base, legs, 20, 54, 4, 8), "jambes, motif " + m);
        }
    }

    @Test
    void theFaceTattooSitsOnTheCheek() {
        int[] base = naked(Gender.MALE);
        for (int m = 1; m <= 12; m++) {
            int[] p = tattoo(Gender.MALE, 0, m);
            assertEquals(changedTotal(base, p), changed(base, p, 8, 12, 4, 4), "motif " + m);
        }
    }

    @Test
    void figuresUseAnOutlineAndFillColors() {
        int[] base = naked(Gender.MALE);
        for (int m = 1; m <= 12; m++) {
            int[] p = tattoo(Gender.MALE, 1, m);
            Set<Integer> colors = new HashSet<>();
            for (int i = 0; i < p.length; i++) if (p[i] != base[i]) colors.add(p[i]);
            assertTrue(colors.size() >= 2, "motif " + m + " : " + colors.size() + " couleurs");
        }
    }

    @Test
    void eachMotifHasItsOwnLook() {
        int[] base = naked(Gender.MALE);
        Set<Integer> signatures = new HashSet<>();
        for (int m = 1; m <= 12; m++) signatures.add(java.util.Arrays.hashCode(tattoo(Gender.MALE, 1, m)));
        assertEquals(12, signatures.size());
        assertNotEquals(java.util.Arrays.hashCode(base), signatures.iterator().next());
    }

    @Test
    void slimArmsStillGetTheirTattooInsideTheArm() {
        int[] base = naked(Gender.FEMALE);
        for (int m = 1; m <= 12; m++) {
            int[] p = tattoo(Gender.FEMALE, 3, m);
            int n = changedTotal(base, p);
            assertTrue(n >= 6, "motif " + m + " : " + n + " pixels");
            assertEquals(n, changed(base, p, 44, 22, 3, 8), "bras fin, motif " + m + " : rien hors des 3 colonnes");
        }
    }
}
