package fr.minenorth.identite.skin;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.document.Gender;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Piercings : oreilles, nez, sourcil, lèvre, en argent, or, noir, or rose ou acier. */
class PiercingTest {
    private static final int W = SkinComposer.SIZE;
    private static final int EARS = 0, NOSE = 1, BROW = 2, LIP = 3, METAL = 4;

    private static Appearance bare(Gender g) {
        return Catalog.defaults(g).with(AppearanceSection.HAIR, new int[]{0, 1});
    }

    private static int[] pierced(Gender g, int slot, int value, int metal) {
        int[] v = new int[5];
        v[slot] = value;
        v[METAL] = metal;
        return SkinComposer.compose(g, bare(g).with(AppearanceSection.PIERCINGS, v));
    }

    private static int[] none(Gender g) { return SkinComposer.compose(g, bare(g)); }

    private static int changed(int[] a, int[] b, int x0, int y0, int w, int h) {
        int n = 0;
        for (int y = y0; y < y0 + h; y++) for (int x = x0; x < x0 + w; x++) if (a[y * W + x] != b[y * W + x]) n++;
        return n;
    }

    private static int total(int[] a, int[] b) { return changed(a, b, 0, 0, W, W); }

    private static int optionCount(int slot) { return Catalog.slots(AppearanceSection.PIERCINGS).get(slot).options(); }

    @Test
    void theSectionExistsWithTheFiveSlots() {
        assertEquals("Piercings", AppearanceSection.PIERCINGS.label());
        assertSame(AppearanceSection.PIERCINGS, AppearanceSection.fromName("PIERCINGS"));
        assertEquals(5, Catalog.slots(AppearanceSection.PIERCINGS).size());
        assertEquals(5, optionCount(EARS));
        assertEquals(4, optionCount(NOSE));
        assertEquals(3, optionCount(BROW));
        assertEquals(4, optionCount(LIP));
        assertEquals(5, optionCount(METAL));
        for (int v : Catalog.defaults(Gender.FEMALE).get(AppearanceSection.PIERCINGS)) assertEquals(0, v);
    }

    @Test
    void everyPiercingShowsAndDiffersFromTheOthers() {
        for (Gender g : Gender.values()) {
            int[] base = none(g);
            for (int slot : new int[]{EARS, NOSE, BROW, LIP}) {
                List<int[]> images = new ArrayList<>();
                for (int v = 1; v < optionCount(slot); v++) {
                    int[] p = pierced(g, slot, v, 0);
                    assertTrue(total(base, p) >= 1, g + " slot " + slot + " option " + v + " invisible");
                    images.add(p);
                }
                for (int i = 0; i < images.size(); i++) {
                    for (int j = i + 1; j < images.size(); j++) {
                        assertTrue(total(images.get(i), images.get(j)) > 0, "slot " + slot + " : options " + (i + 1) + " et " + (j + 1) + " identiques");
                    }
                }
            }
        }
    }

    @Test
    void theMetalChangesTheColor() {
        for (int metal = 1; metal < optionCount(METAL); metal++) {
            assertTrue(total(pierced(Gender.MALE, EARS, 1, 0), pierced(Gender.MALE, EARS, 1, metal)) > 0, "métal " + metal);
        }
    }

    @Test
    void metalAloneWithoutAPiercingChangesNothing() {
        int[] v = new int[5];
        v[METAL] = 3;
        assertArrayEquals(none(Gender.MALE), SkinComposer.compose(Gender.MALE, bare(Gender.MALE).with(AppearanceSection.PIERCINGS, v)));
    }

    @Test
    void earringsAreSymmetricOnBothSidesOfTheHead() {
        for (int v = 1; v < optionCount(EARS); v++) {
            int[] base = none(Gender.FEMALE);
            int[] p = pierced(Gender.FEMALE, EARS, v, 1);
            int right = changed(base, p, 0, 8, 8, 8);
            int left = changed(base, p, 16, 8, 8, 8);
            assertTrue(right >= 1, "oreille droite, option " + v);
            assertEquals(right, left, "option " + v);
            assertEquals(changed(base, p, 32, 8, 8, 8), changed(base, p, 48, 8, 8, 8), "couche extérieure, option " + v);
            assertTrue(changed(base, p, 32, 8, 8, 8) >= 1, "bijou en relief, option " + v);
        }
    }

    @Test
    void facePiercingsAreAlsoDrawnOnTheOuterLayerForRelief() {
        for (int slot : new int[]{NOSE, BROW, LIP}) {
            for (int v = 1; v < optionCount(slot); v++) {
                int[] base = none(Gender.MALE);
                int[] p = pierced(Gender.MALE, slot, v, 1);
                assertTrue(changed(base, p, 40, 8, 8, 8) >= 1, "slot " + slot + " option " + v);
            }
        }
    }

    @Test
    void facePiercingsNeverCoverTheEyes() {
        for (int slot : new int[]{NOSE, BROW, LIP}) {
            for (int v = 1; v < optionCount(slot); v++) {
                int[] base = none(Gender.MALE);
                int[] p = pierced(Gender.MALE, slot, v, 1);
                for (int dx : new int[]{1, 2, 5, 6}) {
                    assertEquals(base[(8 + 4) * W + 8 + dx], p[(8 + 4) * W + 8 + dx], "slot " + slot + " option " + v + " sur l'œil " + dx);
                }
            }
        }
    }

    @Test
    void piercingsStayOnTheHead() {
        int[] base = none(Gender.FEMALE);
        for (int slot : new int[]{EARS, NOSE, BROW, LIP}) {
            for (int v = 1; v < optionCount(slot); v++) {
                int[] p = pierced(Gender.FEMALE, slot, v, 2);
                assertEquals(0, changed(base, p, 0, 16, W, W - 16), "slot " + slot + " option " + v + " touche le corps");
                assertEquals(0, changed(base, p, 0, 0, W, 8), "slot " + slot + " option " + v + " touche le dessus de la tête");
            }
        }
    }

    // ---- fiches enregistrées avant l'arrivée des piercings

    @Test
    void oldSheetsWithoutPiercingsLoadWithNone() {
        CompoundTag tag = Catalog.defaults(Gender.MALE).toTag();
        tag.remove(AppearanceSection.PIERCINGS.name());
        Appearance a = Appearance.fromTag(tag);
        for (int v : a.get(AppearanceSection.PIERCINGS)) assertEquals(0, v);
    }

    @Test
    void randomAppearancesNowIncludeValidPiercings() {
        java.util.Random r = new java.util.Random(8);
        for (Gender g : Gender.values()) {
            for (int i = 0; i < 100; i++) assertTrue(Catalog.validate(g, Catalog.random(g, r)).isEmpty());
        }
    }
}
