package fr.minenorth.identite.hair;

import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.skin.Palette;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Coiffures en volume : blocs 3D de cheveux, jamais sur le visage. */
class HairPiecesTest {
    private static final Set<Integer> VOLUME = Set.of(5, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27);

    @Test
    void everyVolumeStyleHasPiecesAndOthersNone() {
        int styles = Catalog.slots(AppearanceSection.HAIR).get(0).options();
        for (int s = 0; s < styles; s++) {
            assertEquals(VOLUME.contains(s), !HairPieces.forStyle(s).isEmpty(), "coupe " + s);
        }
    }

    @Test
    void everyFemaleOnlyStyleHasVolume() {
        for (int s : Catalog.slots(AppearanceSection.HAIR).get(0).femaleOnly()) {
            assertFalse(HairPieces.forStyle(s).isEmpty(), "coupe femme " + s);
        }
    }

    @Test
    void piecesHavePositiveSizeAndStayCloseToTheBody() {
        for (int s : VOLUME) {
            for (HairPieces.Piece p : HairPieces.forStyle(s)) {
                assertTrue(p.w() > 0 && p.h() > 0 && p.d() > 0, "coupe " + s);
                assertTrue(Math.abs(p.minX()) < 12 && Math.abs(p.maxX()) < 12, "coupe " + s + " trop large");
                assertTrue(p.minY() > -16 && p.maxY() < 26, "coupe " + s + " trop haute ou trop basse");
                assertTrue(p.minZ() > -8 && p.maxZ() < 12, "coupe " + s + " trop en avant ou en arrière");
            }
        }
    }

    @Test
    void hairNeverCoversTheFaceOnTheHead() {
        for (int s : VOLUME) {
            for (HairPieces.Piece p : HairPieces.forStyle(s)) {
                if (p.part() != HairPieces.Part.HEAD || p.rx() != 0) continue;
                boolean inFront = p.minZ() < HairPieces.FRONT;
                boolean belowForehead = p.maxY() > HairPieces.FACE_TOP;
                boolean overFace = p.maxX() > -HairPieces.FACE_X && p.minX() < HairPieces.FACE_X;
                assertFalse(inFront && belowForehead && overFace, "coupe " + s + " masque le visage");
            }
        }
    }

    @Test
    void ponytailsAndBraidsHangAtTheBack() {
        for (int s : new int[]{13, 19, 22}) {
            assertTrue(HairPieces.forStyle(s).stream().anyMatch(p -> p.rx() != 0), "coupe " + s);
        }
    }

    @Test
    void pigtailsAreOnBothSides() {
        List<HairPieces.Piece> pieces = HairPieces.forStyle(21);
        assertTrue(pieces.stream().anyMatch(p -> p.px() > 0));
        assertTrue(pieces.stream().anyMatch(p -> p.px() < 0));
    }

    @Test
    void longHairHangsDownTheBack() {
        for (int s : new int[]{14, 17, 18, 26}) {
            assertTrue(HairPieces.forStyle(s).stream().anyMatch(p -> p.part() == HairPieces.Part.BODY), "coupe " + s);
        }
    }

    @Test
    void textureHasThreeDistinctTones() {
        for (int color = 0; color < 12; color++) {
            int[] px = HairTexture.pixels(Palette.hairColor(color));
            assertEquals(HairTexture.WIDTH * HairTexture.HEIGHT, px.length);
            for (int v : px) assertEquals(0xFF, v >>> 24);
            int hair = px[0], tie = px[HairTexture.BAND * HairTexture.WIDTH], shadow = px[2 * HairTexture.BAND * HairTexture.WIDTH];
            assertNotEquals(hair, tie);
            assertNotEquals(hair, shadow);
        }
    }
}
