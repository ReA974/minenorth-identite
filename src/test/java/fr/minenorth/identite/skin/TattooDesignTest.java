package fr.minenorth.identite.skin;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.appearance.TattooZone;
import fr.minenorth.identite.document.Gender;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Les tatouages du catalogue sont de petits dessins discrets (contour et couleurs), dont on règle la taille. */
class TattooDesignTest {
    private static final int W = SkinComposer.SIZE;

    private static Appearance bare(Gender g) {
        return Catalog.defaults(g).with(AppearanceSection.HAIR, new int[]{0, 1});
    }

    private static int[] naked(Gender g) { return SkinComposer.compose(g, bare(g)); }

    private static int[] tattoo(Gender g, TattooZone zone, int motif, int size) {
        int[] t = Catalog.defaults(g).get(AppearanceSection.TATTOOS);
        t[zone.motifSlot()] = motif;
        if (zone.hasSize()) t[zone.sizeSlot()] = size;
        return SkinComposer.compose(g, bare(g).with(AppearanceSection.TATTOOS, t));
    }

    private static int changed(int[] a, int[] b, int x0, int y0, int w, int h) {
        int n = 0;
        for (int y = y0; y < y0 + h; y++) for (int x = x0; x < x0 + w; x++) if (a[y * W + x] != b[y * W + x]) n++;
        return n;
    }

    private static int total(int[] a, int[] b) { return changed(a, b, 0, 0, W, W); }

    /** Zone du skin couverte par un tatouage de cette taille : {x, y, largeur, hauteur} (le bras droit, le torse… une seule jambe). */
    private static int[] region(TattooZone zone, int size) {
        return switch (zone) {
            case FACE_NECK -> new int[]{8, 13, 3, 3};
            case TORSO -> sized(20, 20, size, true);
            case BACK -> sized(32, 20, size, true);
            case RIGHT_ARM -> sized(44, 20, size, false);
            case LEFT_ARM -> sized(36, 52, size, false);
            case LEGS -> sized(4, 20, size, false);
        };
    }

    private static int[] sized(int x, int y, int size, boolean wide) {
        return switch (size) {
            case 0 -> wide ? new int[]{x + 2, y + 3, 4, 4} : new int[]{x, y + 4, 4, 4};
            case 1 -> wide ? new int[]{x + 1, y + 3, 6, 6} : new int[]{x, y + 3, 4, 6};
            default -> wide ? new int[]{x, y + 2, 8, 8} : new int[]{x, y + 2, 4, 8};
        };
    }

    private static int inside(int[] a, int[] b, int[] r) { return changed(a, b, r[0], r[1], r[2], r[3]); }

    @Test
    void largeTorsoFiguresAreFiguresNotWallpaper() {
        int[] base = naked(Gender.MALE);
        for (int m = 1; m <= 12; m++) {
            int n = total(base, tattoo(Gender.MALE, TattooZone.TORSO, m, 2));
            assertTrue(n >= 18 && n <= 50, "motif " + m + " : " + n + " pixels");
        }
    }

    @Test
    void everyFigureStaysInsideItsRegionAtEverySize() {
        for (Gender g : Gender.values()) {
            int[] base = naked(g);
            for (TattooZone zone : TattooZone.values()) {
                for (int size = 0; size < (zone.hasSize() ? 3 : 1); size++) {
                    for (int m = 1; m <= 12; m++) {
                        int[] p = tattoo(g, zone, m, size);
                        int[] r = region(zone, size);
                        int in = inside(base, p, r);
                        if (zone == TattooZone.LEGS) in += inside(base, p, new int[]{20, 52 + (r[1] - 20), r[2], r[3]});
                        assertEquals(total(base, p), in, g + " " + zone + " taille " + size + " motif " + m + " déborde de sa zone");
                    }
                }
            }
        }
    }

    @Test
    void theDefaultSizeIsDiscreet() {
        int[] base = naked(Gender.MALE);
        for (TattooZone zone : new TattooZone[]{TattooZone.TORSO, TattooZone.BACK, TattooZone.RIGHT_ARM, TattooZone.LEFT_ARM}) {
            for (int m = 1; m <= 12; m++) {
                int n = total(base, tattoo(Gender.MALE, zone, m, 0));
                assertTrue(n <= 16, zone + " motif " + m + " : " + n + " pixels en petit");
            }
        }
    }

    @Test
    void mediumFiguresAreInBetween() {
        int[] base = naked(Gender.MALE);
        for (int m = 1; m <= 12; m++) {
            int n = total(base, tattoo(Gender.MALE, TattooZone.TORSO, m, 1));
            assertTrue(n <= 36 && n >= 8, "motif " + m + " : " + n + " pixels en moyen");
        }
    }

    @Test
    void faceMarksAreTinyAndDiffer() {
        int[] base = naked(Gender.MALE);
        Set<Integer> looks = new HashSet<>();
        for (int m = 1; m <= 12; m++) {
            int[] p = tattoo(Gender.MALE, TattooZone.FACE_NECK, m, 0);
            int n = total(base, p);
            assertTrue(n >= 3 && n <= 8, "motif visage " + m + " : " + n + " pixels");
            looks.add(java.util.Arrays.hashCode(p));
        }
        assertEquals(12, looks.size());
    }

    @Test
    void faceMarksNeverCoverTheEyesOrTheMouth() {
        int[] base = naked(Gender.MALE);
        for (int m = 1; m <= 12; m++) {
            int[] p = tattoo(Gender.MALE, TattooZone.FACE_NECK, m, 0);
            assertEquals(0, changed(base, p, 8, 8, 8, 5), "motif " + m + " touche le haut du visage et les yeux");
            assertEquals(0, changed(base, p, 11, 8, 5, 8), "motif " + m + " touche le nez et la bouche");
        }
    }

    @Test
    void figuresUseAnOutlineAndFillColorsWhenLarge() {
        int[] base = naked(Gender.MALE);
        for (int m = 1; m <= 12; m++) {
            int[] p = tattoo(Gender.MALE, TattooZone.TORSO, m, 2);
            Set<Integer> colors = new HashSet<>();
            for (int i = 0; i < p.length; i++) if (p[i] != base[i]) colors.add(p[i]);
            assertTrue(colors.size() >= 2, "motif " + m + " : " + colors.size() + " couleurs");
        }
    }

    @Test
    void eachMotifHasItsOwnLookAtEverySize() {
        for (int size = 0; size < 3; size++) {
            Set<Integer> signatures = new HashSet<>();
            for (int m = 1; m <= 12; m++) signatures.add(java.util.Arrays.hashCode(tattoo(Gender.MALE, TattooZone.TORSO, m, size)));
            assertEquals(12, signatures.size(), "taille " + size);
        }
    }

    @Test
    void slimArmsStillGetTheirTattooInsideTheArm() {
        int[] base = naked(Gender.FEMALE);
        for (int size = 0; size < 3; size++) {
            for (int m = 1; m <= 12; m++) {
                int[] p = tattoo(Gender.FEMALE, TattooZone.RIGHT_ARM, m, size);
                int n = total(base, p);
                assertTrue(n >= 3, "motif " + m + " taille " + size + " : " + n + " pixels");
                int[] r = region(TattooZone.RIGHT_ARM, size);
                assertEquals(n, changed(base, p, r[0], r[1], 3, r[3]), "bras fin : rien hors des 3 colonnes");
            }
        }
    }
}
