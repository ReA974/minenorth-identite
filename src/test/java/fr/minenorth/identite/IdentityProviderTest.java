package fr.minenorth.identite;

import fr.minenorth.identite.document.Gender;
import fr.minenorth.identite.document.IdentityData;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IdentityProviderTest {
    private static IdentityData.Profile profile(Gender g) {
        return new IdentityData.Profile(UUID.randomUUID(), "Dupont", "Jean", "01/01/2000", "Paris", "Française", "FR-12345678", g);
    }

    @Test
    void legacyProfilesAreNotExposedToOtherMods() {
        assertFalse(IdentityProvider.visible(profile(null)));
    }

    @Test
    void profilesWithGenderAreExposed() {
        assertTrue(IdentityProvider.visible(profile(Gender.MALE)));
        assertTrue(IdentityProvider.visible(profile(Gender.FEMALE)));
    }

    @Test
    void missingProfileIsNotVisible() {
        assertFalse(IdentityProvider.visible(null));
    }
}
