package fr.minenorth.identite.skin;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.document.Gender;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class SkinComposerTest {
    private static final int W = SkinComposer.SIZE;

    /** Fiche épurée : pas de cheveux, de pilosité, de maquillage, de marque, de tatouage. */
    private static Appearance bare(Gender g) {
        return Catalog.defaults(g)
                .with(AppearanceSection.HAIR, new int[]{0, 1})
                .with(AppearanceSection.FACIAL_HAIR, new int[]{0, 0})
                .with(AppearanceSection.SKIN, new int[]{3, 0});
    }

    private static int px(int[] p, int x, int y) { return p[y * W + x]; }
    private static int alpha(int argb) { return argb >>> 24; }

    @Test
    void sizeIs4096() {
        assertEquals(4096, SkinComposer.compose(Gender.MALE, bare(Gender.MALE)).length);
    }

    @Test
    void skinToneFillsHeadFront() {
        int[] p = SkinComposer.compose(Gender.MALE, bare(Gender.MALE));
        assertEquals(Palette.skinTone(3), px(p, 12, 12));
    }

    @Test
    void differentTonesDiffer() {
        int[] a = SkinComposer.compose(Gender.MALE, bare(Gender.MALE).with(AppearanceSection.SKIN, new int[]{1, 0}));
        int[] b = SkinComposer.compose(Gender.MALE, bare(Gender.MALE).with(AppearanceSection.SKIN, new int[]{8, 0}));
        assertNotEquals(px(a, 12, 12), px(b, 12, 12));
    }

    @Test
    void noHairLeavesHeadOverlayTransparent() {
        int[] p = SkinComposer.compose(Gender.MALE, bare(Gender.MALE));
        assertEquals(0, alpha(px(p, 44, 3)));
    }

    @Test
    void hairStyleChangesTopOfHead() {
        int[] p = SkinComposer.compose(Gender.MALE, bare(Gender.MALE).with(AppearanceSection.HAIR, new int[]{9, 1}));
        assertEquals(255, alpha(px(p, 44, 3)));
    }

    @Test
    void hairColorApplied() {
        int[] a = SkinComposer.compose(Gender.MALE, bare(Gender.MALE).with(AppearanceSection.HAIR, new int[]{9, 0}));
        int[] b = SkinComposer.compose(Gender.MALE, bare(Gender.MALE).with(AppearanceSection.HAIR, new int[]{9, 3}));
        assertNotEquals(px(a, 44, 3), px(b, 44, 3));
    }

    @Test
    void slimArmsLeaveLastColumnTransparentForFemale() {
        int[] f = SkinComposer.compose(Gender.FEMALE, bare(Gender.FEMALE));
        assertEquals(0, alpha(px(f, 55, 25)));
        assertEquals(0, alpha(px(f, 47, 55)));
        assertEquals(255, alpha(px(f, 45, 25)));
    }

    @Test
    void classicArmsFilledForMale() {
        int[] m = SkinComposer.compose(Gender.MALE, bare(Gender.MALE));
        assertEquals(255, alpha(px(m, 55, 25)));
        assertEquals(255, alpha(px(m, 47, 55)));
    }

    @Test
    void buildChangesShadingOnly() {
        int[][] skins = new int[3][];
        for (int b = 0; b < 3; b++) {
            skins[b] = SkinComposer.compose(Gender.MALE, bare(Gender.MALE).with(AppearanceSection.BODY, new int[]{b}));
        }
        for (int i = 0; i < W * W; i++) {
            assertEquals(alpha(skins[0][i]), alpha(skins[1][i]));
            assertEquals(alpha(skins[0][i]), alpha(skins[2][i]));
        }
        assertFalse(Arrays.equals(skins[0], skins[1]));
        assertFalse(Arrays.equals(skins[0], skins[2]));
    }

    @Test
    void femaleHasChestShadingMaleDoesNot() {
        int[] m = SkinComposer.compose(Gender.MALE, bare(Gender.MALE));
        int[] f = SkinComposer.compose(Gender.FEMALE, bare(Gender.FEMALE));
        boolean differs = false;
        for (int y = 20; y <= 31; y++) for (int x = 20; x <= 27; x++) differs |= px(m, x, y) != px(f, x, y);
        assertTrue(differs);
    }

    @Test
    void underwearCoversLowerTorso() {
        int[] p = SkinComposer.compose(Gender.MALE, bare(Gender.MALE));
        assertNotEquals(Palette.skinTone(3), px(p, 22, 30));
    }

    @Test
    void tattooAppearsOnlyOnItsZone() {
        Appearance without = bare(Gender.MALE);
        Appearance with = without.with(AppearanceSection.TATTOOS, new int[]{0, 0, 0, 1, 0, 0});
        int[] a = SkinComposer.compose(Gender.MALE, without);
        int[] b = SkinComposer.compose(Gender.MALE, with);
        int changed = 0;
        for (int y = 0; y < W; y++) {
            for (int x = 0; x < W; x++) {
                if (px(a, x, y) != px(b, x, y)) {
                    changed++;
                    assertTrue(x >= 40 && x <= 55 && y >= 16 && y <= 47, "pixel hors du bras droit : " + x + "," + y);
                }
            }
        }
        assertTrue(changed > 0);
    }

    @Test
    void deterministic() {
        Appearance a = Catalog.random(Gender.FEMALE, new java.util.Random(5));
        assertArrayEquals(SkinComposer.compose(Gender.FEMALE, a), SkinComposer.compose(Gender.FEMALE, a));
    }

    @Test
    void customSkinFlagIgnored() {
        Appearance a = Catalog.random(Gender.MALE, new java.util.Random(9));
        assertArrayEquals(SkinComposer.compose(Gender.MALE, a), SkinComposer.compose(Gender.MALE, a.withCustomSkin(true)));
    }

    @Test
    void everyRandomAppearanceComposes() {
        java.util.Random r = new java.util.Random(11);
        for (Gender g : Gender.values()) {
            for (int i = 0; i < 100; i++) assertEquals(4096, SkinComposer.compose(g, Catalog.random(g, r)).length);
        }
    }
}
