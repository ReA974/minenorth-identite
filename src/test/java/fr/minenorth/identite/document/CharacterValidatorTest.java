package fr.minenorth.identite.document;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CharacterValidatorTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 3);

    @Test
    void cleanNameStripsSectionSignAndRejectsDigits() {
        assertEquals("cJean", CharacterValidator.cleanName("§cJean"));
        assertEquals("", CharacterValidator.cleanName("Jean2"));
        assertEquals("Jean-Luc", CharacterValidator.cleanName("Jean-Luc"));
        assertEquals("Éloïse Anne", CharacterValidator.cleanName("  Éloïse   Anne "));
    }

    @Test
    void dateRules() {
        assertFalse(CharacterValidator.validDate("29/02/2023"));
        assertTrue(CharacterValidator.validDate("29/02/2024"));
        assertFalse(CharacterValidator.validDate("31/04/2000"));
        assertFalse(CharacterValidator.validDate("1/1/2000"));
    }

    @Test
    void ageComputedFromBirthDate() {
        assertEquals(30, CharacterValidator.age("14/10/1996", TODAY).orElseThrow());
        assertEquals(29, CharacterValidator.age("04/11/1996", TODAY).orElseThrow());
        assertEquals(30, CharacterValidator.age("03/11/1996", TODAY).orElseThrow());
        assertTrue(CharacterValidator.age("garbage", TODAY).isEmpty());
    }

    @Test
    void futureDateRejected() {
        assertTrue(CharacterValidator.validateIdentity("Dupont", "Jean", "04/11/2026", "Paris", "Française", TODAY).isPresent());
    }

    @Test
    void ageOver110Rejected() {
        assertTrue(CharacterValidator.validateIdentity("Dupont", "Jean", "01/01/1900", "Paris", "Française", TODAY).isPresent());
    }

    @Test
    void blankFieldsRejected() {
        assertTrue(CharacterValidator.validateIdentity("", "Jean", "01/01/2000", "Paris", "Française", TODAY).isPresent());
        assertTrue(CharacterValidator.validateIdentity("Dupont", "", "01/01/2000", "Paris", "Française", TODAY).isPresent());
        assertTrue(CharacterValidator.validateIdentity("Dupont", "Jean", "01/01/2000", "", "Française", TODAY).isPresent());
    }

    @Test
    void validIdentityPasses() {
        assertTrue(CharacterValidator.validateIdentity("Dupont", "Jean", "14/10/1996", "Paris", "Française", TODAY).isEmpty());
    }
}
