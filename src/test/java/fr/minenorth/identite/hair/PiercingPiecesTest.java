package fr.minenorth.identite.hair;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.document.Gender;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Piercings en 3D : nez, sourcil, lèvre et nombril. Plus de piercings d'oreille. */
class PiercingPiecesTest {
    private static final int EARS = 0, NOSE = 1, BROW = 2, LIP = 3, METAL = 4, HELIX = 5, NAVEL = 6;

    private static int optionCount(int slot) { return Catalog.slots(AppearanceSection.PIERCINGS).get(slot).options(); }

    private static List<HairPieces.Piece> pieces(int slot, int value) {
        int[] p = new int[7];
        p[slot] = value;
        return PiercingPieces.of(p);
    }

    @Test
    void theSectionKeepsSevenSlotsButNoEars() {
        assertEquals(7, Catalog.slots(AppearanceSection.PIERCINGS).size());
        assertEquals(1, optionCount(EARS));
        assertEquals(1, optionCount(HELIX));
        assertEquals(6, optionCount(NOSE));
        assertEquals(5, optionCount(BROW));
        assertEquals(6, optionCount(LIP));
        assertEquals(5, optionCount(NAVEL));
        assertEquals(5, optionCount(METAL));
    }

    @Test
    void nothingChosenMeansNoPieces() {
        assertTrue(PiercingPieces.of(new int[7]).isEmpty());
    }

    @Test
    void everyOptionHasPiecesAndDiffersFromTheOthers() {
        for (int slot : new int[]{NOSE, BROW, LIP, NAVEL}) {
            List<List<HairPieces.Piece>> all = new ArrayList<>();
            for (int v = 1; v < optionCount(slot); v++) {
                List<HairPieces.Piece> pieces = pieces(slot, v);
                assertFalse(pieces.isEmpty(), "slot " + slot + " option " + v);
                all.add(pieces);
            }
            for (int i = 0; i < all.size(); i++) {
                for (int j = i + 1; j < all.size(); j++) assertNotEquals(all.get(i), all.get(j), "slot " + slot + " options " + (i + 1) + " et " + (j + 1));
            }
        }
    }

    @Test
    void facePiecesSitInFrontOfTheFaceAndNeverCoverTheEyes() {
        for (int slot : new int[]{NOSE, BROW, LIP}) {
            for (int v = 1; v < optionCount(slot); v++) {
                for (HairPieces.Piece p : pieces(slot, v)) {
                    assertEquals(HairPieces.Part.HEAD, p.part());
                    assertTrue(p.minZ() < -4.0f && p.maxZ() < -3.8f, "devant le visage, slot " + slot + " option " + v);
                    assertTrue(p.minX() > -4.2f && p.maxX() < 4.2f, "sur le visage");
                    boolean overEyeColumns = p.maxX() > -3f && p.minX() < -1f || p.maxX() > 1f && p.minX() < 3f;
                    boolean overEyeRow = p.maxY() > -4f && p.minY() < -3f;
                    assertFalse(overEyeColumns && overEyeRow, "couvre un œil, slot " + slot + " option " + v);
                }
            }
        }
    }

    @Test
    void navelPiecesAreOnTheFrontOfTheTorse() {
        for (int v = 1; v < optionCount(NAVEL); v++) {
            for (HairPieces.Piece p : pieces(NAVEL, v)) {
                assertEquals(HairPieces.Part.BODY, p.part());
                assertTrue(p.minY() > 5.5f && p.maxY() < 10.5f, "au niveau du nombril");
                assertTrue(p.maxZ() < -2.0f, "devant le torse");
            }
        }
    }

    @Test
    void jewelsAreSmall() {
        for (int slot : new int[]{NOSE, BROW, LIP, NAVEL}) {
            for (int v = 1; v < optionCount(slot); v++) {
                for (HairPieces.Piece p : pieces(slot, v)) assertTrue(p.w() <= 3.5f && p.h() <= 3.5f && p.d() <= 1.0f, "slot " + slot + " option " + v);
            }
        }
    }

    @Test
    void theMetalTextureHasThreeTones() {
        for (int metal = 0; metal < optionCount(METAL); metal++) {
            int[] px = HairTexture.metalPixels(fr.minenorth.identite.skin.Palette.metal(metal));
            assertEquals(HairTexture.WIDTH * HairTexture.HEIGHT, px.length);
            int a = px[0], b = px[HairTexture.BAND * HairTexture.WIDTH], c = px[2 * HairTexture.BAND * HairTexture.WIDTH];
            assertNotEquals(a, b);
            assertNotEquals(a, c);
        }
    }

    // ---- fiches enregistrées avant

    @Test
    void oldSheetsWithoutPiercingsLoadWithNone() {
        CompoundTag tag = Catalog.defaults(Gender.MALE).toTag();
        tag.remove(AppearanceSection.PIERCINGS.name());
        for (int v : Appearance.fromTag(tag).get(AppearanceSection.PIERCINGS)) assertEquals(0, v);
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

    @Test
    void randomNeverAddsPiercings() {
        java.util.Random r = new java.util.Random(3);
        for (Gender g : Gender.values()) {
            for (int i = 0; i < 50; i++) {
                for (int v : Catalog.random(g, r).get(AppearanceSection.PIERCINGS)) assertEquals(0, v);
            }
        }
    }
}
