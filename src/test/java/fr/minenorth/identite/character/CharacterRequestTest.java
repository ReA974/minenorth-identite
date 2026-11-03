package fr.minenorth.identite.character;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.document.Gender;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CharacterRequestTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 3);

    private static CharacterManager.CreateRequest request(Gender g, Appearance a, String last, String birth, boolean rules) {
        return new CharacterManager.CreateRequest(last, "Jean", birth, "Paris", "Française", g, a, rules);
    }

    private static CharacterManager.CreateRequest valid() {
        return request(Gender.MALE, Catalog.defaults(Gender.MALE), "Dupont", "14/10/1996", true);
    }

    @Test
    void validRequestPasses() {
        assertTrue(CharacterManager.check(valid(), TODAY).isEmpty());
    }

    @Test
    void rulesNotAcceptedRejected() {
        assertTrue(CharacterManager.check(request(Gender.MALE, Catalog.defaults(Gender.MALE), "Dupont", "14/10/1996", false), TODAY).isPresent());
    }

    @Test
    void femaleWithMaleHairRejected() {
        Appearance a = Catalog.defaults(Gender.FEMALE).with(AppearanceSection.HAIR, new int[]{1, 0});
        assertTrue(CharacterManager.check(request(Gender.FEMALE, a, "Dupont", "14/10/1996", true), TODAY).isPresent());
    }

    @Test
    void femaleWithFacialHairRejected() {
        Appearance a = Catalog.defaults(Gender.FEMALE).with(AppearanceSection.FACIAL_HAIR, new int[]{2, 0});
        assertTrue(CharacterManager.check(request(Gender.FEMALE, a, "Dupont", "14/10/1996", true), TODAY).isPresent());
    }

    @Test
    void badNameRejected() {
        assertTrue(CharacterManager.check(request(Gender.MALE, Catalog.defaults(Gender.MALE), "Dup0nt", "14/10/1996", true), TODAY).isPresent());
    }

    @Test
    void futureBirthRejected() {
        assertTrue(CharacterManager.check(request(Gender.MALE, Catalog.defaults(Gender.MALE), "Dupont", "04/11/2026", true), TODAY).isPresent());
    }

    @Test
    void missingGenderRejected() {
        assertTrue(CharacterManager.check(request(null, Catalog.defaults(Gender.MALE), "Dupont", "14/10/1996", true), TODAY).isPresent());
    }

    @Test
    void missingAppearanceRejected() {
        assertTrue(CharacterManager.check(request(Gender.MALE, null, "Dupont", "14/10/1996", true), TODAY).isPresent());
    }

    @Test
    void keepingMinecraftSkinIsStaffOnly() {
        assertTrue(CharacterManager.skinPolicyError(false, false).isPresent());
    }

    @Test
    void staffMayKeepMinecraftSkin() {
        assertTrue(CharacterManager.skinPolicyError(false, true).isEmpty());
    }

    @Test
    void everyoneMayChooseTheCreatedSkin() {
        assertTrue(CharacterManager.skinPolicyError(true, false).isEmpty());
        assertTrue(CharacterManager.skinPolicyError(true, true).isEmpty());
    }
}
