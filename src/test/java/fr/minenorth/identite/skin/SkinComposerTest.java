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
    void maleUpperBodyIsPlainSkin() {
        int tone = Palette.skinTone(3);
        int[] p = SkinComposer.compose(Gender.MALE, bare(Gender.MALE));
        for (int y = 20; y <= 26; y++) for (int x = 16; x <= 39; x++) assertEquals(tone, px(p, x, y), "haut du torse " + x + "," + y);
    }

    @Test
    void femaleHasSoftBustShadingOnlyOnTheFrontOfTheTorso() {
        int tone = Palette.skinTone(3);
        int[] p = SkinComposer.compose(Gender.FEMALE, bare(Gender.FEMALE));
        int shaded = 0;
        for (int y = 20; y <= 26; y++) {
            for (int x = 16; x <= 39; x++) {
                boolean bustArea = x >= 21 && x <= 26 && y >= 22 && y <= 27;
                if (bustArea) { if (px(p, x, y) != tone) shaded++; }
                else assertEquals(tone, px(p, x, y), "hors poitrine " + x + "," + y);
            }
        }
        assertTrue(shaded >= 6, "pixels modelés : " + shaded);
    }

    @Test
    void bustShadingIsSubtleNeverDrasticallyDifferentFromSkin() {
        int tone = Palette.skinTone(3);
        int[] p = SkinComposer.compose(Gender.FEMALE, bare(Gender.FEMALE));
        for (int y = 22; y <= 27; y++) {
            for (int x = 21; x <= 26; x++) {
                int c = px(p, x, y);
                for (int shift : new int[]{16, 8, 0}) {
                    int d = Math.abs(((c >> shift) & 0xFF) - ((tone >> shift) & 0xFF));
                    assertTrue(d <= 40, "contraste trop fort en " + x + "," + y);
                }
            }
        }
    }

    @Test
    void lowerBodyHasSubtleDetailButNoUnderwear() {
        int tone = Palette.skinTone(3);
        for (Gender g : Gender.values()) {
            int[] p = SkinComposer.compose(g, bare(g));
            int detailed = 0;
            for (int y = 20; y <= 63; y++) {
                for (int x = 0; x <= 39; x++) {
                    boolean rightLeg = y <= 31 && x <= 15;
                    boolean leftLeg = y >= 52 && x >= 16 && x <= 31;
                    boolean hips = y >= 27 && y <= 31 && x >= 16 && x <= 39;
                    if (!(rightLeg || leftLeg || hips)) continue;
                    int c = px(p, x, y);
                    assertEquals(255, c >>> 24, g + " transparent en " + x + "," + y);
                    if (c != tone) detailed++;
                    for (int shift : new int[]{16, 8, 0}) {
                        int d = Math.abs(((c >> shift) & 0xFF) - ((tone >> shift) & 0xFF));
                        assertTrue(d <= 45, g + " contraste trop fort (sous-vêtement ?) en " + x + "," + y);
                    }
                }
            }
            assertTrue(detailed >= 60, g + " bas du corps trop vide : " + detailed + " pixels modelés");
        }
    }

    @Test
    void lowerBodyDiffersBetweenMenAndWomen() {
        int[] m = SkinComposer.compose(Gender.MALE, bare(Gender.MALE));
        int[] f = SkinComposer.compose(Gender.FEMALE, bare(Gender.FEMALE));
        int n = 0;
        for (int y = 27; y <= 31; y++) for (int x = 16; x <= 39; x++) if (px(m, x, y) != px(f, x, y)) n++;
        for (int y = 20; y <= 31; y++) for (int x = 0; x <= 15; x++) if (px(m, x, y) != px(f, x, y)) n++;
        assertTrue(n >= 10, "différences bas du corps : " + n);
    }

    @Test
    void bodyOverlayLayerIsEmptyForClothesToUseLater() {
        for (Gender g : Gender.values()) {
            int[] p = SkinComposer.compose(g, bare(g));
            for (int y = 32; y <= 47; y++) for (int x = 0; x <= 39; x++) assertEquals(0, p[y * W + x] >>> 24, g + " calque " + x + "," + y);
        }
    }

    @Test
    void tattooAppearsOnlyOnItsZone() {
        Appearance without = bare(Gender.MALE);
        int[] tattoos = new int[11];
        tattoos[5] = 1;   // bras droit
        Appearance with = without.with(AppearanceSection.TATTOOS, tattoos);
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
