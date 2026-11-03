package fr.minenorth.identite.character;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.document.Gender;
import fr.minenorth.identite.drawing.DrawingKind;
import fr.minenorth.identite.drawing.Drawings;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DrawingEditRulesTest {
    private static Map<DrawingKind, byte[]> one(DrawingKind k, int value) {
        byte[] g = new byte[k.width() * k.height()];
        java.util.Arrays.fill(g, (byte) value);
        Map<DrawingKind, byte[]> m = new EnumMap<>(DrawingKind.class);
        m.put(k, g);
        return m;
    }

    private static final Set<AppearanceSection> FACE_ONLY = EnumSet.of(AppearanceSection.FACE);
    private static final Map<AppearanceSection, int[]> NONE = Map.of();

    @Test
    void aDrawingOfAnOpenedSectionIsAccepted() {
        assertTrue(CharacterManager.validateEdit(Gender.MALE, FACE_ONLY, NONE, one(DrawingKind.FACE, 3)).isEmpty());
    }

    @Test
    void aDrawingOfAnotherSectionIsRefused() {
        assertTrue(CharacterManager.validateEdit(Gender.MALE, FACE_ONLY, NONE, one(DrawingKind.TATTOO_TORSO, 3)).isPresent());
        assertTrue(CharacterManager.validateEdit(Gender.MALE, FACE_ONLY, NONE, one(DrawingKind.BEARD, 3)).isPresent());
    }

    @Test
    void aDrawingWithAColorOutsideThePaletteIsRefused() {
        assertTrue(CharacterManager.validateEdit(Gender.MALE, FACE_ONLY, NONE, one(DrawingKind.FACE, DrawingKind.FACE.paletteSize() + 1)).isPresent());
        assertTrue(CharacterManager.validateEdit(Gender.MALE, FACE_ONLY, NONE, one(DrawingKind.FACE, -5)).isPresent());
    }

    @Test
    void aDrawingOfTheWrongSizeIsRefused() {
        Map<DrawingKind, byte[]> wrong = new EnumMap<>(DrawingKind.class);
        wrong.put(DrawingKind.FACE, new byte[5]);
        assertTrue(CharacterManager.validateEdit(Gender.MALE, FACE_ONLY, NONE, wrong).isPresent());
    }

    @Test
    void catalogEditsAndDrawingsCanComeTogether() {
        Map<AppearanceSection, int[]> edits = Map.of(AppearanceSection.FACE, new int[]{0, 0, 0, 0, 0});
        assertTrue(CharacterManager.validateEdit(Gender.MALE, FACE_ONLY, edits, one(DrawingKind.FACE, 2)).isEmpty());
    }

    @Test
    void nothingAtAllIsStillRefused() {
        assertTrue(CharacterManager.validateEdit(Gender.MALE, FACE_ONLY, NONE, Map.of()).isPresent());
    }

    @Test
    void noSessionMeansNoDrawingEither() {
        assertTrue(CharacterManager.validateEdit(Gender.MALE, Set.of(), NONE, one(DrawingKind.FACE, 2)).isPresent());
    }

    @Test
    void applyingDrawingsKeepsEverythingElse() {
        Appearance before = Catalog.defaults(Gender.MALE);
        Optional<Appearance> after = CharacterManager.applyEdit(before, NONE, one(DrawingKind.FACE, 4));
        assertTrue(after.isPresent());
        assertEquals(4, after.get().drawings().get(DrawingKind.FACE)[0]);
        assertArrayEquals(before.get(AppearanceSection.BODY), after.get().get(AppearanceSection.BODY));
        assertEquals(before.customSkin(), after.get().customSkin());
    }

    @Test
    void anAllTransparentDrawingClearsTheExistingOne() {
        Appearance drawn = Catalog.defaults(Gender.MALE).withDrawings(Drawings.EMPTY.with(DrawingKind.FACE, one(DrawingKind.FACE, 4).get(DrawingKind.FACE)));
        Appearance cleared = CharacterManager.applyEdit(drawn, NONE, one(DrawingKind.FACE, 0)).orElseThrow();
        assertTrue(cleared.drawings().isEmpty());
    }
}
