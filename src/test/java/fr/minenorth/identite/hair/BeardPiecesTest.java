package fr.minenorth.identite.hair;

import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Barbes et moustaches en volume 3D. */
class BeardPiecesTest {
    /** Les styles en volume : tout sauf aucune barbe, la barbe de trois jours et l'ombre légère (peintes). */
    private static final Set<Integer> VOLUME = Set.of(3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16);

    @Test
    void everyVolumeStyleHasPiecesAndTheOthersNone() {
        int styles = Catalog.slots(AppearanceSection.FACIAL_HAIR).get(0).options();
        for (int s = 0; s < styles; s++) assertEquals(VOLUME.contains(s), !BeardPieces.forStyle(s).isEmpty(), "style " + s);
    }

    @Test
    void piecesNeverCoverTheEyes() {
        for (int s : VOLUME) {
            for (HairPieces.Piece p : BeardPieces.forStyle(s)) {
                boolean overEyeColumns = p.maxX() > -3f && p.minX() < -1f || p.maxX() > 1f && p.minX() < 3f;
                boolean aboveTheEyesBottom = p.minY() < BeardPieces.EYES_BOTTOM && p.maxY() > -4f;
                assertFalse(overEyeColumns && aboveTheEyesBottom, "style " + s + " couvre un œil");
            }
        }
    }

    @Test
    void piecesStayOnTheFaceAndInFrontOfIt() {
        for (int s : VOLUME) {
            for (HairPieces.Piece p : BeardPieces.forStyle(s)) {
                assertEquals(HairPieces.Part.HEAD, p.part());
                assertTrue(p.w() > 0 && p.h() > 0 && p.d() > 0);
                assertTrue(p.minX() > -6f && p.maxX() < 6f, "style " + s + " trop large");
                assertTrue(p.minY() > -7f && p.maxY() < 8f, "style " + s + " trop haut ou trop bas");
                assertTrue(p.minZ() > -6f && p.minZ() < -1f, "style " + s + " hors du visage");
            }
        }
    }

    @Test
    void theMouthStaysFreeOnTheFullBeards() {
        // la bouche (colonnes -1 à 1, lignes -2 à -1) reste visible : aucune pièce ne la recouvre sur toute sa largeur de devant
        for (int s : new int[]{8, 9, 13}) {
            List<HairPieces.Piece> pieces = BeardPieces.forStyle(s);
            boolean covered = pieces.stream().anyMatch(p -> p.minX() <= -1f && p.maxX() >= 1f && p.minY() <= -1.9f && p.maxY() >= -1.1f
                    && p.minZ() <= -4.6f);
            assertFalse(covered, "style " + s);
        }
    }

    @Test
    void beardStylesAreAllDifferent() {
        for (int a : VOLUME) {
            for (int b : VOLUME) if (a < b) assertNotEquals(BeardPieces.forStyle(a), BeardPieces.forStyle(b), a + " et " + b);
        }
    }
}
