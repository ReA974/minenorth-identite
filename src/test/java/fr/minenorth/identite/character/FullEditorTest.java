package fr.minenorth.identite.character;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.document.Gender;
import fr.minenorth.identite.drawing.DrawingKind;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** /cidedit : l'éditeur complet rouvre toute l'apparence, mais jamais l'identité ni le sexe. */
class FullEditorTest {
    @Test
    void theFullEditorOpensEveryAppearanceSection() {
        assertEquals(EnumSet.allOf(AppearanceSection.class), CharacterManager.fullEditorSections());
    }

    @Test
    void everySectionAndEveryDrawingIsAcceptedInAFullEditSession() {
        Appearance a = Catalog.defaults(Gender.FEMALE);
        Map<AppearanceSection, int[]> edits = new EnumMap<>(AppearanceSection.class);
        for (AppearanceSection s : AppearanceSection.values()) edits.put(s, a.get(s));
        Map<DrawingKind, byte[]> drawings = new EnumMap<>(DrawingKind.class);
        for (DrawingKind k : DrawingKind.values()) drawings.put(k, new byte[k.width() * k.height()]);
        assertTrue(CharacterManager.validateEdit(Gender.FEMALE, CharacterManager.fullEditorSections(), edits, drawings).isEmpty());
    }

    @Test
    void aFullEditSessionStillRefusesValuesTheSexDoesNotAllow() {
        Map<AppearanceSection, int[]> edits = Map.of(AppearanceSection.FACIAL_HAIR, new int[]{3, 0});
        assertTrue(CharacterManager.validateEdit(Gender.FEMALE, CharacterManager.fullEditorSections(), edits).isPresent());
    }

    @Test
    void theEditCannotChangeTheSkinChoiceNorTheOutfit() {
        Appearance before = Catalog.defaults(Gender.MALE).withCustomSkin(true).withOutfit(fr.minenorth.identite.clothing.Outfit.starter(Gender.MALE));
        Map<AppearanceSection, int[]> edits = Map.of(AppearanceSection.HAIR, new int[]{9, 3});
        Optional<Appearance> after = CharacterManager.applyEdit(before, edits);
        assertTrue(after.orElseThrow().customSkin());
        assertEquals(before.outfit(), after.orElseThrow().outfit());
    }
}
