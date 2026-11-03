package fr.minenorth.identite.skin;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.clothing.Outfit;
import fr.minenorth.identite.document.Gender;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SkinOutfitTest {
    private static final int W = SkinComposer.SIZE;

    private static Appearance bare(Gender g) {
        return Catalog.defaults(g).with(AppearanceSection.HAIR, new int[]{0, 1});
    }

    private static Appearance dressed(Gender g) { return bare(g).withOutfit(Outfit.starter(g)); }

    private static int px(int[] p, int x, int y) { return p[y * W + x]; }

    private static boolean opaque(int[] p, int x, int y) { return (px(p, x, y) >>> 24) == 255; }

    private static boolean rowOpaque(int[] p, int x0, int x1, int y) {
        for (int x = x0; x <= x1; x++) if (!opaque(p, x, y)) return false;
        return true;
    }

    private static boolean rowTransparent(int[] p, int x0, int x1, int y) {
        for (int x = x0; x <= x1; x++) if (opaque(p, x, y)) return false;
        return true;
    }

    @Test
    void underwearIsDrawnOnTheOuterLayerNotTheBaseSkin() {
        for (Gender g : Gender.values()) {
            int[] naked = SkinComposer.compose(g, bare(g));
            int[] worn = SkinComposer.compose(g, dressed(g));
            for (int y = 0; y < W; y++) {
                for (int x = 0; x < W; x++) {
                    boolean baseLayer = (y < 16 && x < 32) || (y >= 16 && y < 32 && x < 56) || (y >= 48 && x >= 16 && x < 48);
                    if (baseLayer) assertEquals(px(naked, x, y), px(worn, x, y), g + " la peau change en " + x + "," + y);
                }
            }
        }
    }

    @Test
    void maleBoxersCoverHipsAndTopOfLegsOnly() {
        int[] p = SkinComposer.compose(Gender.MALE, dressed(Gender.MALE));
        for (int y = 45; y <= 47; y++) assertTrue(rowOpaque(p, 20, 27, y), "caleçon devant, ligne " + y);
        for (int y = 45; y <= 47; y++) assertTrue(rowOpaque(p, 32, 39, y), "caleçon dos, ligne " + y);
        for (int y = 36; y <= 38; y++) assertTrue(rowOpaque(p, 4, 7, y), "jambe droite haut, ligne " + y);
        for (int y = 52; y <= 54; y++) assertTrue(rowOpaque(p, 4, 7, y), "jambe gauche haut, ligne " + y);
        assertTrue(rowTransparent(p, 20, 27, 39), "pas de haut pour l'homme");
        assertTrue(rowTransparent(p, 4, 7, 44), "bas de jambe nu");
        assertTrue(rowTransparent(p, 4, 7, 60), "bas de jambe nu");
    }

    @Test
    void femaleSetHasTopAndBriefs() {
        int[] p = SkinComposer.compose(Gender.FEMALE, dressed(Gender.FEMALE));
        for (int y = 39; y <= 41; y++) assertTrue(rowOpaque(p, 20, 27, y), "haut devant, ligne " + y);
        for (int y = 39; y <= 41; y++) assertTrue(rowOpaque(p, 32, 39, y), "haut dos, ligne " + y);
        for (int y = 46; y <= 47; y++) assertTrue(rowOpaque(p, 20, 27, y), "culotte devant, ligne " + y);
        assertTrue(rowTransparent(p, 20, 27, 43), "ventre nu entre les deux");
        assertTrue(rowTransparent(p, 4, 7, 44), "bas de jambe nu");
    }

    @Test
    void garmentsAreNotFlatBlocks() {
        for (Gender g : Gender.values()) {
            int[] p = SkinComposer.compose(g, dressed(g));
            Set<Integer> colors = new HashSet<>();
            for (int y = 32; y < 48; y++) for (int x = 16; x < 40; x++) if (opaque(p, x, y)) colors.add(px(p, x, y));
            assertTrue(colors.size() >= 2, g + " couleurs : " + colors.size());
        }
    }

    @Test
    void takingTheOutfitOffGivesTheNakedSkinBack() {
        for (Gender g : Gender.values()) {
            assertArrayEquals(SkinComposer.compose(g, bare(g)), SkinComposer.compose(g, dressed(g).withOutfit(Outfit.EMPTY)));
        }
    }

    @Test
    void outfitNeverTouchesTheHeadOverlay() {
        for (Gender g : Gender.values()) {
            int[] naked = SkinComposer.compose(g, bare(g));
            int[] worn = SkinComposer.compose(g, dressed(g));
            for (int y = 0; y < 16; y++) for (int x = 32; x < 64; x++) assertEquals(px(naked, x, y), px(worn, x, y));
        }
    }

    @Test
    void womenAndMenWearDifferentGarments() {
        int[] m = SkinComposer.compose(Gender.MALE, dressed(Gender.MALE));
        int[] f = SkinComposer.compose(Gender.FEMALE, dressed(Gender.FEMALE));
        int n = 0;
        for (int y = 32; y < 48; y++) for (int x = 0; x < 40; x++) if (px(m, x, y) != px(f, x, y)) n++;
        assertTrue(n >= 20);
    }
}
