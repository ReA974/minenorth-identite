package fr.minenorth.identite.skin;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.document.Gender;
import fr.minenorth.identite.drawing.DrawingKind;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Piercings : nez, sourcil, lèvre et nombril, en argent, or, noir, or rose ou acier. Plus de piercings d'oreille. */
class PiercingTest {
    private static final int W = SkinComposer.SIZE;
    private static final int EARS = 0, NOSE = 1, BROW = 2, LIP = 3, METAL = 4, HELIX = 5, NAVEL = 6;

    private static Appearance bare(Gender g) {
        return Catalog.defaults(g).with(AppearanceSection.HAIR, new int[]{0, 1});
    }

    private static int[] pierced(Gender g, int slot, int value, int metal) {
        int[] v = new int[7];
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
    void theSectionExistsWithTheSevenSlotsAndNoEarOptions() {
        assertEquals("Piercings", AppearanceSection.PIERCINGS.label());
        assertSame(AppearanceSection.PIERCINGS, AppearanceSection.fromName("PIERCINGS"));
        assertEquals(7, Catalog.slots(AppearanceSection.PIERCINGS).size());
        assertEquals(1, optionCount(EARS));
        assertEquals(1, optionCount(HELIX));
        assertEquals(6, optionCount(NOSE));
        assertEquals(5, optionCount(BROW));
        assertEquals(6, optionCount(LIP));
        assertEquals(5, optionCount(METAL));
        assertEquals(5, optionCount(NAVEL));
        for (int v : Catalog.defaults(Gender.FEMALE).get(AppearanceSection.PIERCINGS)) assertEquals(0, v);
    }

    @Test
    void everyPiercingShowsAndDiffersFromTheOthers() {
        for (Gender g : Gender.values()) {
            int[] base = none(g);
            for (int slot : new int[]{NOSE, BROW, LIP, NAVEL}) {
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
            assertTrue(total(pierced(Gender.MALE, NOSE, 5, 0), pierced(Gender.MALE, NOSE, 5, metal)) > 0, "métal " + metal);
        }
    }

    @Test
    void metalAloneWithoutAPiercingChangesNothing() {
        int[] v = new int[7];
        v[METAL] = 3;
        assertArrayEquals(none(Gender.MALE), SkinComposer.compose(Gender.MALE, bare(Gender.MALE).with(AppearanceSection.PIERCINGS, v)));
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
    void facePiercingsStayOnTheFrontOfTheHead() {
        int[] base = none(Gender.FEMALE);
        for (int slot : new int[]{NOSE, BROW, LIP}) {
            for (int v = 1; v < optionCount(slot); v++) {
                int[] p = pierced(Gender.FEMALE, slot, v, 2);
                assertEquals(0, changed(base, p, 0, 16, W, W - 16), "slot " + slot + " option " + v + " touche le corps");
                assertEquals(0, changed(base, p, 0, 0, W, 8), "slot " + slot + " option " + v + " touche le dessus de la tête");
                assertEquals(0, changed(base, p, 0, 8, 8, 8), "slot " + slot + " option " + v + " touche le côté de la tête");
            }
        }
    }

    @Test
    void navelPiercingsStayOnTheTorsoFront() {
        for (Gender g : Gender.values()) {
            int[] base = none(g);
            for (int v = 1; v < optionCount(NAVEL); v++) {
                int[] p = pierced(g, NAVEL, v, 1);
                assertTrue(changed(base, p, 20, 20, 8, 12) >= 1, "nombril " + v);
                assertTrue(changed(base, p, 20, 36, 8, 12) >= 1, "nombril en relief " + v);
                assertEquals(0, changed(base, p, 0, 0, W, 16), "la tête ne bouge pas");
            }
        }
    }

    @Test
    void randomNeverAddsTattoosOrPiercings() {
        java.util.Random r = new java.util.Random(3);
        for (Gender g : Gender.values()) {
            for (int i = 0; i < 50; i++) {
                Appearance a = Catalog.random(g, r);
                for (int v : a.get(AppearanceSection.TATTOOS)) assertEquals(0, v);
                for (int v : a.get(AppearanceSection.PIERCINGS)) assertEquals(0, v);
            }
        }
    }

    // ---- fiches enregistrées avant

    @Test
    void oldSheetsWithoutPiercingsLoadWithNone() {
        CompoundTag tag = Catalog.defaults(Gender.MALE).toTag();
        tag.remove(AppearanceSection.PIERCINGS.name());
        Appearance a = Appearance.fromTag(tag);
        for (int v : a.get(AppearanceSection.PIERCINGS)) assertEquals(0, v);
    }

    @Test
    void earPiercingsOfOldSheetsAreRemovedAndTheOthersKept() {
        CompoundTag tag = Catalog.defaults(Gender.MALE).toTag();
        tag.putIntArray(AppearanceSection.PIERCINGS.name(), new int[]{3, 1, 2, 1, 4, 2, 3});
        int[] v = Appearance.fromTag(tag).get(AppearanceSection.PIERCINGS);
        assertEquals(0, v[EARS]);
        assertEquals(0, v[HELIX]);
        assertEquals(1, v[NOSE]);
        assertEquals(2, v[BROW]);
        assertEquals(1, v[LIP]);
        assertEquals(4, v[METAL]);
        assertEquals(3, v[NAVEL]);
    }

    @Test
    void sheetsWithTheFirstFivePiercingSlotsGetTheNewOnesAtZero() {
        CompoundTag tag = Catalog.defaults(Gender.MALE).toTag();
        tag.putIntArray(AppearanceSection.PIERCINGS.name(), new int[]{2, 0, 0, 0, 1});
        int[] v = Appearance.fromTag(tag).get(AppearanceSection.PIERCINGS);
        assertEquals(7, v.length);
        assertEquals(0, v[EARS]);
        assertEquals(1, v[METAL]);
        assertEquals(0, v[NAVEL]);
    }

    // ---- une tête importée ne cache plus les options du catalogue

    private static Appearance withImportedHead(Appearance a) {
        byte[] grid = new byte[DrawingKind.FACE.width() * DrawingKind.FACE.height()];
        java.util.Arrays.fill(grid, (byte) 5);   // un visage entier, tout opaque
        return a.withDrawings(a.drawings().with(DrawingKind.FACE, grid));
    }

    private static int[] head(Appearance a) { return SkinComposer.compose(Gender.FEMALE, a); }

    @Test
    void anImportedHeadStillShowsFreckles() {
        Appearance base = withImportedHead(bare(Gender.FEMALE));
        Appearance freckles = base.with(AppearanceSection.SKIN, new int[]{base.get(AppearanceSection.SKIN)[0], 2});
        assertTrue(total(head(base), head(freckles)) > 0, "taches de rousseur invisibles sur la tête importée");
    }

    @Test
    void anImportedHeadStillShowsBrowsAndMakeup() {
        Appearance base = withImportedHead(bare(Gender.FEMALE));
        int[] face = base.get(AppearanceSection.FACE);
        int[] thick = face.clone();
        thick[2] = 1;
        int[] thin = face.clone();
        thin[2] = 5;
        assertTrue(total(head(base.with(AppearanceSection.FACE, thick)), head(base.with(AppearanceSection.FACE, thin))) > 0, "sourcils");
        int[] makeup = base.get(AppearanceSection.MAKEUP).clone();
        makeup[4] = 4;
        assertTrue(total(head(base), head(base.with(AppearanceSection.MAKEUP, makeup))) > 0, "maquillage");
    }

    @Test
    void anImportedHeadStillShowsBeardAndFaceMarks() {
        Appearance base = withImportedHead(bare(Gender.MALE));
        int[] beard = new int[]{9, 1};
        assertTrue(total(SkinComposer.compose(Gender.MALE, base), SkinComposer.compose(Gender.MALE, base.with(AppearanceSection.FACIAL_HAIR, beard))) > 0, "barbe");
        int[] tattoos = base.get(AppearanceSection.TATTOOS).clone();
        tattoos[0] = 3;
        assertTrue(total(SkinComposer.compose(Gender.MALE, base), SkinComposer.compose(Gender.MALE, base.with(AppearanceSection.TATTOOS, tattoos))) > 0, "marque du visage");
    }

    @Test
    void theCatalogFeaturesAreNotAppliedTwiceOnAnImportedHead() {
        // un visage importé transparent partout ne doit rien changer à ce que le catalogue dessine
        Appearance plain = bare(Gender.FEMALE).with(AppearanceSection.SKIN, new int[]{3, 2});
        byte[] empty = new byte[DrawingKind.FACE.width() * DrawingKind.FACE.height()];
        assertArrayEquals(head(plain), head(plain.withDrawings(plain.drawings().with(DrawingKind.FACE, empty))));
    }
}
