package fr.minenorth.identite.skin;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.document.Gender;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Maquillage et particularités de la peau en haute définition : une image 32 × 32 devant le visage. */
class HiResFaceTest {
    private static Appearance base() { return Catalog.defaults(Gender.FEMALE).with(AppearanceSection.HAIR, new int[]{0, 1}); }

    private static Appearance makeup(int lips, int shadow, int blush) {
        return base().with(AppearanceSection.MAKEUP, new int[]{lips, 1, shadow, 2, blush, 1});
    }

    private static Appearance marks(int style) {
        return base().with(AppearanceSection.SKIN, new int[]{3, style});
    }

    private static int opaque(int[] px) {
        int n = 0;
        for (int v : px) if ((v >>> 24) > 0) n++;
        return n;
    }

    @Test
    void nothingChosenMeansNothingToDraw() {
        assertFalse(HiResFace.hasAny(base()));
        assertEquals(0, opaque(HiResFace.image(base())));
    }

    @Test
    void everyMakeupOptionDrawsSomethingAndDiffers() {
        for (int slot = 0; slot < 3; slot++) {
            int options = new int[]{6, 6, 5}[slot];
            List<int[]> images = new ArrayList<>();
            for (int v = 1; v < options; v++) {
                Appearance a = slot == 0 ? makeup(v, 0, 0) : slot == 1 ? makeup(0, v, 0) : makeup(0, 0, v);
                assertTrue(HiResFace.hasAny(a));
                int[] image = HiResFace.image(a);
                assertTrue(opaque(image) > 0, "slot " + slot + " option " + v + " vide");
                images.add(image);
            }
            for (int i = 0; i < images.size(); i++) {
                for (int j = i + 1; j < images.size(); j++) {
                    assertFalse(java.util.Arrays.equals(images.get(i), images.get(j)), "slot " + slot + " : " + (i + 1) + " et " + (j + 1));
                }
            }
        }
    }

    @Test
    void everySkinMarkDrawsSomethingAndDiffers() {
        List<int[]> images = new ArrayList<>();
        for (int v = 1; v < Catalog.slots(AppearanceSection.SKIN).get(1).options(); v++) {
            assertTrue(HiResFace.hasAny(marks(v)));
            int[] image = HiResFace.image(marks(v));
            assertTrue(opaque(image) > 0, "particularité " + v);
            images.add(image);
        }
        for (int i = 0; i < images.size(); i++) {
            for (int j = i + 1; j < images.size(); j++) assertFalse(java.util.Arrays.equals(images.get(i), images.get(j)));
        }
    }

    @Test
    void edgesAreSoft() {
        int[] image = HiResFace.image(makeup(0, 0, 3));
        boolean soft = false;
        for (int v : image) {
            int a = v >>> 24;
            if (a > 5 && a < 0x60) soft = true;
        }
        assertTrue(soft, "le dégradé des joues doit avoir des bords doux");
    }

    @Test
    void lipsStayOnTheMouthAndCheeksNeverOnTheEyes() {
        // la bouche : colonnes 10 à 22, lignes 23 à 31 ; les joues : lignes 16 à 28
        int[] lips = HiResFace.image(makeup(2, 0, 0));
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                if ((lips[y * 32 + x] >>> 24) > 0) assertTrue(x >= 9 && x <= 23 && y >= 22 && y <= 31, "lèvres hors de la bouche : " + x + "," + y);
            }
        }
        int[] blush = HiResFace.image(makeup(0, 0, 4));
        for (int y = 0; y < 14; y++) for (int x = 0; x < 32; x++) assertEquals(0, blush[y * 32 + x] >>> 24, "joues trop hautes : " + x + "," + y);
    }

    @Test
    void theMarksFollowTheSkinTone() {
        int[] light = HiResFace.image(base().with(AppearanceSection.SKIN, new int[]{0, 1}));
        int[] dark = HiResFace.image(base().with(AppearanceSection.SKIN, new int[]{9, 1}));
        assertFalse(java.util.Arrays.equals(light, dark));
    }

    @Test
    void isDeterministic() {
        assertArrayEquals(HiResFace.image(marks(2)), HiResFace.image(marks(2)));
    }

    @Test
    void theFaceLivesInItsOwnRegionOfTheAtlas() {
        int[] atlas = HiResTattoo.atlas(makeup(2, 3, 2));
        int[] o = TattooRegions.FACE_ORIGIN;
        int inside = 0, total = 0;
        for (int y = 0; y < TattooRegions.ATLAS_H; y++) {
            for (int x = 0; x < TattooRegions.ATLAS_W; x++) {
                if ((atlas[y * TattooRegions.ATLAS_W + x] >>> 24) == 0) continue;
                total++;
                if (x >= o[0] && x < o[0] + 32 && y >= o[1] && y < o[1] + 32) inside++;
            }
        }
        assertTrue(total > 0);
        assertEquals(total, inside);
        assertTrue(HiResTattoo.hasAny(makeup(2, 0, 0)));
    }

    @Test
    void theLayeredSkinNoLongerPaintsMakeupOrMarksButTheOldOneStill() {
        Appearance a = makeup(2, 3, 2).with(AppearanceSection.SKIN, new int[]{3, 2});
        assertArrayEquals(SkinComposer.compose(Gender.FEMALE, base(), true), SkinComposer.compose(Gender.FEMALE, a, true));
        assertFalse(java.util.Arrays.equals(SkinComposer.compose(Gender.FEMALE, base(), false), SkinComposer.compose(Gender.FEMALE, a, false)));
    }
}
