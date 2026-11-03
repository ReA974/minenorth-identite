package fr.minenorth.identite.character;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.document.Gender;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EditSessionsTest {
    @Test
    void onlyOpenedSectionsAllowed() {
        EditSessions s = new EditSessions();
        UUID id = UUID.randomUUID();
        s.open(id, EnumSet.of(AppearanceSection.HAIR), 1000);
        assertEquals(Set.of(AppearanceSection.HAIR), s.allowed(id, 500));
    }

    @Test
    void expiredSessionAllowsNothing() {
        EditSessions s = new EditSessions();
        UUID id = UUID.randomUUID();
        s.open(id, EnumSet.of(AppearanceSection.HAIR), 1000);
        assertTrue(s.allowed(id, 1001).isEmpty());
    }

    @Test
    void closeRemovesSession() {
        EditSessions s = new EditSessions();
        UUID id = UUID.randomUUID();
        s.open(id, EnumSet.of(AppearanceSection.HAIR), 1000);
        s.close(id);
        assertTrue(s.allowed(id, 1).isEmpty());
    }

    @Test
    void sessionsAreIndependentPerPlayer() {
        EditSessions s = new EditSessions();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        s.open(a, EnumSet.of(AppearanceSection.HAIR), 1000);
        assertTrue(s.allowed(b, 1).isEmpty());
    }

    // ---- validation d'une modification envoyée par un client

    private static final Set<AppearanceSection> HAIR_ONLY = EnumSet.of(AppearanceSection.HAIR);

    @Test
    void editOfAllowedSectionPasses() {
        Map<AppearanceSection, int[]> edit = Map.of(AppearanceSection.HAIR, new int[]{5, 2});
        assertTrue(CharacterManager.validateEdit(Gender.MALE, HAIR_ONLY, edit).isEmpty());
    }

    @Test
    void editOfOtherSectionRejected() {
        Map<AppearanceSection, int[]> edit = Map.of(
                AppearanceSection.HAIR, new int[]{5, 2},
                AppearanceSection.BODY, new int[]{1});
        assertTrue(CharacterManager.validateEdit(Gender.MALE, HAIR_ONLY, edit).isPresent());
    }

    @Test
    void invalidValuesForGenderRejected() {
        Map<AppearanceSection, int[]> edit = Map.of(AppearanceSection.HAIR, new int[]{1, 0});   // coupe d'homme
        assertTrue(CharacterManager.validateEdit(Gender.FEMALE, HAIR_ONLY, edit).isPresent());
    }

    @Test
    void emptyEditRejected() {
        assertTrue(CharacterManager.validateEdit(Gender.MALE, HAIR_ONLY, Map.of()).isPresent());
    }

    @Test
    void noSessionMeansNothingAllowed() {
        Map<AppearanceSection, int[]> edit = Map.of(AppearanceSection.HAIR, new int[]{5, 2});
        assertTrue(CharacterManager.validateEdit(Gender.MALE, Set.of(), edit).isPresent());
    }

    @Test
    void editApplyKeepsOtherSections() {
        Appearance before = Catalog.defaults(Gender.MALE);
        Optional<Appearance> after = CharacterManager.applyEdit(before, Map.of(AppearanceSection.HAIR, new int[]{6, 3}));
        assertTrue(after.isPresent());
        assertArrayEquals(new int[]{6, 3}, after.get().get(AppearanceSection.HAIR));
        assertArrayEquals(before.get(AppearanceSection.BODY), after.get().get(AppearanceSection.BODY));
        assertEquals(before.customSkin(), after.get().customSkin());
    }
}
