package fr.minenorth.identite.client.creator;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.document.Gender;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class CreatorStateTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 3);

    @Test
    void changingGenderResetsInvalidHair() {
        CreatorState s = CreatorState.forCreation();
        s.set(AppearanceSection.HAIR, new int[]{1, 2});          // coupe réservée aux hommes
        s.set(AppearanceSection.FACIAL_HAIR, new int[]{2, 0});
        s.setGender(Gender.FEMALE);
        assertTrue(Catalog.validate(Gender.FEMALE, s.appearance()).isEmpty());
        assertEquals(Catalog.defaults(Gender.FEMALE).get(AppearanceSection.HAIR)[0], s.appearance().get(AppearanceSection.HAIR)[0]);
        assertEquals(0, s.appearance().get(AppearanceSection.FACIAL_HAIR)[0]);
    }

    @Test
    void changingGenderKeepsStillValidChoices() {
        CreatorState s = CreatorState.forCreation();
        s.set(AppearanceSection.HAIR, new int[]{9, 7});          // coupe commune
        s.setGender(Gender.FEMALE);
        assertArrayEquals(new int[]{9, 7}, s.appearance().get(AppearanceSection.HAIR));
    }

    @Test
    void undoRedoRestoresPreviousChoice() {
        CreatorState s = CreatorState.forCreation();
        int[] before = s.appearance().get(AppearanceSection.BODY);
        s.set(AppearanceSection.BODY, new int[]{2});
        assertTrue(s.canUndo());
        s.undo();
        assertArrayEquals(before, s.appearance().get(AppearanceSection.BODY));
        assertTrue(s.canRedo());
        s.redo();
        assertArrayEquals(new int[]{2}, s.appearance().get(AppearanceSection.BODY));
    }

    @Test
    void undoOnEmptyHistoryIsNoOp() {
        CreatorState s = CreatorState.forCreation();
        Appearance a = s.appearance();
        s.undo();
        s.redo();
        assertEquals(a, s.appearance());
        assertFalse(s.canUndo());
        assertFalse(s.canRedo());
    }

    @Test
    void newChoiceClearsRedo() {
        CreatorState s = CreatorState.forCreation();
        s.set(AppearanceSection.BODY, new int[]{1});
        s.undo();
        s.set(AppearanceSection.BODY, new int[]{2});
        assertFalse(s.canRedo());
    }

    @Test
    void settingSameValueDoesNotPolluteHistory() {
        CreatorState s = CreatorState.forCreation();
        s.set(AppearanceSection.BODY, s.appearance().get(AppearanceSection.BODY));
        assertFalse(s.canUndo());
    }

    @Test
    void randomizeSectionStaysValid() {
        CreatorState s = CreatorState.forCreation();
        s.setGender(Gender.FEMALE);
        Random r = new Random(1);
        for (int i = 0; i < 200; i++) {
            for (AppearanceSection sec : AppearanceSection.values()) s.randomizeSection(sec, r);
            assertTrue(Catalog.validate(Gender.FEMALE, s.appearance()).isEmpty());
        }
    }

    @Test
    void resetSectionRestoresDefaults() {
        CreatorState s = CreatorState.forCreation();
        s.set(AppearanceSection.SKIN, new int[]{9, 3});
        s.resetSection(AppearanceSection.SKIN);
        assertArrayEquals(Catalog.defaults(Gender.MALE).get(AppearanceSection.SKIN), s.appearance().get(AppearanceSection.SKIN));
    }

    @Test
    void birthDateBuiltFromParts() {
        CreatorState s = CreatorState.forCreation();
        s.setBirth("4", "9", "1999");
        assertEquals("04/09/1999", s.birthDate());
        s.setBirth("x", "9", "1999");
        assertEquals("", s.birthDate());
    }

    @Test
    void identityErrorUntilFilled() {
        CreatorState s = CreatorState.forCreation();
        assertTrue(s.identityError(TODAY).isPresent());
        s.setNames("Dupont", "Jean", "Paris", "Française");
        s.setBirth("14", "10", "1996");
        assertTrue(s.identityError(TODAY).isEmpty());
        assertEquals(30, s.age(TODAY).orElseThrow());
    }

    @Test
    void editorModeKeepsFixedGender() {
        CreatorState s = CreatorState.forEditor(Gender.FEMALE, Catalog.defaults(Gender.FEMALE));
        assertEquals(Gender.FEMALE, s.gender());
        assertTrue(s.genderLocked());
        s.setGender(Gender.MALE);
        assertEquals(Gender.FEMALE, s.gender());
    }

    @Test
    void creationPreviewWearsTheStarterUnderwearOfTheChosenGender() {
        CreatorState s = CreatorState.forCreation();
        assertEquals(fr.minenorth.identite.clothing.Outfit.starter(Gender.MALE), s.appearance().outfit());
        s.setGender(Gender.FEMALE);
        assertEquals(fr.minenorth.identite.clothing.Outfit.starter(Gender.FEMALE), s.appearance().outfit());
        s.undo();
        assertEquals(fr.minenorth.identite.clothing.Outfit.starter(Gender.MALE), s.appearance().outfit());
    }

    @Test
    void editorKeepsTheOutfitItWasGiven() {
        Appearance current = Catalog.defaults(Gender.FEMALE);
        CreatorState s = CreatorState.forEditor(Gender.FEMALE, current);
        assertTrue(s.appearance().outfit().isEmpty());
    }
}
