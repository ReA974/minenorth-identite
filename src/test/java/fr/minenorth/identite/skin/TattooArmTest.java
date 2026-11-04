package fr.minenorth.identite.skin;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.appearance.TattooZone;
import fr.minenorth.identite.document.Gender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tatouages de bras : on choisit la face (avant, exterieur, interieur, arriere) et la hauteur. */
class TattooArmTest {
    private static final int W = SkinComposer.SIZE;

    private static int[] arm(Gender g, int face, int height, int size, boolean right) {
        Appearance base = Catalog.defaults(g).with(AppearanceSection.HAIR, new int[]{0, 1});
        int[] t = new int[TattooZone.TOTAL_SLOTS];
        TattooZone z = right ? TattooZone.RIGHT_ARM : TattooZone.LEFT_ARM;
        t[z.motifSlot()] = 5;
        t[z.sizeSlot()] = size;
        t[right ? TattooZone.RIGHT_ARM_FACE_SLOT : TattooZone.LEFT_ARM_FACE_SLOT] = face;
        t[right ? TattooZone.RIGHT_ARM_HEIGHT_SLOT : TattooZone.LEFT_ARM_HEIGHT_SLOT] = height;
        return SkinComposer.compose(g, base.with(AppearanceSection.TATTOOS, t));
    }

    private static int changed(int[] a, int[] b, int x0, int y0, int w, int h) {
        int n = 0;
        for (int y = y0; y < y0 + h; y++) for (int x = x0; x < x0 + w; x++) if (a[y * W + x] != b[y * W + x]) n++;
        return n;
    }

    @Test
    void eachFaceOfTheRightArmIsUsedAlone() {
        int[] none = SkinComposer.compose(Gender.MALE, Catalog.defaults(Gender.MALE).with(AppearanceSection.HAIR, new int[]{0, 1}));
        // bras droit : exterieur 40-43, avant 44-47, interieur 48-51, arriere 52-55 (lignes 20 a 31)
        int[][] cols = {{44, 4}, {40, 4}, {48, 4}, {52, 4}};
        for (int face = 0; face < 4; face++) {
            int[] p = arm(Gender.MALE, face, 0, 2, true);
            for (int other = 0; other < 4; other++) {
                int n = changed(none, p, cols[other][0], 20, cols[other][1], 12);
                if (other == face) assertTrue(n > 0, "face " + face + " vide");
                else assertEquals(0, n, "face " + face + " deborde sur la face " + other);
            }
        }
    }

    @Test
    void heightMovesTheTattooDownTheArm() {
        int[] none = SkinComposer.compose(Gender.MALE, Catalog.defaults(Gender.MALE).with(AppearanceSection.HAIR, new int[]{0, 1}));
        int[] shoulder = arm(Gender.MALE, 0, 1, 0, true);
        int[] wrist = arm(Gender.MALE, 0, 4, 0, true);
        assertTrue(changed(none, shoulder, 44, 20, 4, 4) > 0);
        assertEquals(0, changed(none, shoulder, 44, 28, 4, 4));
        assertTrue(changed(none, wrist, 44, 28, 4, 4) > 0);
        assertEquals(0, changed(none, wrist, 44, 20, 4, 4));
    }

    @Test
    void heightNeverLeavesTheArm() {
        int[] none = SkinComposer.compose(Gender.FEMALE, Catalog.defaults(Gender.FEMALE).with(AppearanceSection.HAIR, new int[]{0, 1}));
        for (int face = 0; face < 4; face++) {
            for (int height = 0; height < 5; height++) {
                for (int size = 0; size < 3; size++) {
                    for (boolean right : new boolean[]{true, false}) {
                        int[] p = arm(Gender.FEMALE, face, height, size, right);
                        int inArm = right ? changed(none, p, 40, 20, 16, 12) : changed(none, p, 32, 52, 16, 12);
                        assertEquals(changed(none, p, 0, 0, W, W), inArm, "face " + face + " hauteur " + height + " taille " + size);
                    }
                }
            }
        }
    }

    @Test
    void leftArmOutsideIsTheLeftSideOfTheBody() {
        int[] none = SkinComposer.compose(Gender.MALE, Catalog.defaults(Gender.MALE).with(AppearanceSection.HAIR, new int[]{0, 1}));
        // bras gauche : interieur 32-35, avant 36-39, exterieur 40-43, arriere 44-47 (lignes 52 a 63)
        assertTrue(changed(none, arm(Gender.MALE, 1, 0, 2, false), 40, 52, 4, 12) > 0);
        assertTrue(changed(none, arm(Gender.MALE, 2, 0, 2, false), 32, 52, 4, 12) > 0);
    }
}
