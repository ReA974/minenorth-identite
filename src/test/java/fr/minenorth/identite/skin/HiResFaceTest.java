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
        Appearance a = makeup(0, 3, 2).with(AppearanceSection.SKIN, new int[]{3, 2});
        assertArrayEquals(SkinComposer.compose(Gender.FEMALE, base(), true), SkinComposer.compose(Gender.FEMALE, a, true));
        assertFalse(java.util.Arrays.equals(SkinComposer.compose(Gender.FEMALE, base(), false), SkinComposer.compose(Gender.FEMALE, a, false)));
    }

    @Test
    void freckleAndMoleStayBelowTheEyes() {
        // les yeux occupent les lignes 16 à 20 de l'image : aucune particularité n'y touche (sauf la cicatrice du sourcil, au-dessus)
        for (int style : new int[]{1, 2, 3, 4, 6}) {
            int[] image = HiResFace.image(marks(style));
            for (int y = 15; y <= 20; y++) {
                for (int x = 0; x < 32; x++) {
                    if (x >= 12 && x <= 20) continue;   // l'arête du nez, entre les yeux
                    assertEquals(0, image[y * 32 + x] >>> 24, "particularité " + style + " sur l'œil : " + x + "," + y);
                }
            }
        }
    }

    @Test
    void theHighDefinitionLipsHideThePaintedMouth() {
        Appearance smile = makeup(2, 0, 0).with(AppearanceSection.FACE, new int[]{0, 0, 0, 0, 1});
        Appearance pout = makeup(2, 0, 0).with(AppearanceSection.FACE, new int[]{0, 0, 0, 0, 3});
        // la bouche peinte (lignes 14 et 15 de la tête) ne dépend plus de la forme choisie : les lèvres la remplacent
        int[] a = SkinComposer.compose(Gender.FEMALE, smile, true), b = SkinComposer.compose(Gender.FEMALE, pout, true);
        for (int x = 8; x < 16; x++) for (int y = 13; y < 16; y++) assertEquals(a[y * 64 + x], b[y * 64 + x], "bouche peinte visible : " + x + "," + y);
        // sans lèvres, la bouche peinte reste
        int[] c = SkinComposer.compose(Gender.FEMALE, base().with(AppearanceSection.FACE, new int[]{0, 0, 0, 0, 1}), true);
        int[] d = SkinComposer.compose(Gender.FEMALE, base().with(AppearanceSection.FACE, new int[]{0, 0, 0, 0, 3}), true);
        assertFalse(java.util.Arrays.equals(c, d));
    }

    @Test
    void thePreviewTileOfAMakeupOptionShowsTheHighDefinitionLips() {
        var plain = OptionPreview.of(Gender.FEMALE, base(), AppearanceSection.MAKEUP, 0, "lips", 0);
        var lips = OptionPreview.of(Gender.FEMALE, base(), AppearanceSection.MAKEUP, 0, "lips", 2);
        assertEquals(32, lips.w());
        int[] hd = HiResFace.image(makeup(2, 0, 0));
        int changed = 0;
        for (int j = 0; j < hd.length; j++) if ((hd[j] >>> 24) > 0x90 && plain.argb()[j] != lips.argb()[j]) changed++;
        assertTrue(changed > 10, "les lÃ¨vres doivent se voir sur la vignette : " + changed);
    }

    @Test
    void eyeMakeupFollowsTheShapeOfTheEyes() {
        for (int shadow = 1; shadow <= 5; shadow++) {
            Appearance oneRow = makeup(0, shadow, 0).with(AppearanceSection.FACE, new int[]{0, 0, 0, 0, 0});
            Appearance twoRows = makeup(0, shadow, 0).with(AppearanceSection.FACE, new int[]{2, 0, 0, 0, 0});
            assertFalse(java.util.Arrays.equals(HiResFace.image(oneRow), HiResFace.image(twoRows)), "fard " + shadow + " identique pour deux formes d'yeux");
        }
    }

    @Test
    void theLinerSitsOnTopOfTheEyeWhateverItsHeight() {
        for (int eye = 0; eye < 10; eye++) {
            int[] face = new int[]{eye, 0, 0, 0, 0};
            int top = 4 - (SkinComposer.EYES[eye].length - 1);
            int[] image = HiResFace.image(makeup(0, 3, 0).with(AppearanceSection.FACE, face));
            boolean onTheLid = false;
            for (int x = 0; x < 32; x++) for (int y = top * 4; y < top * 4 + 3; y++) if ((image[y * 32 + x] >>> 24) > 0x80) onTheLid = true;
            assertTrue(onTheLid, "forme d'yeux " + eye);
        }
    }
}
