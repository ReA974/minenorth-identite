package fr.minenorth.identite.salon;

import fr.minenorth.identite.appearance.AppearanceSection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SalonKindTest {
    @Test
    void theTattooChairEditsTattoosAndPiercingsOnly() {
        assertEquals(Set.of(AppearanceSection.TATTOOS, AppearanceSection.PIERCINGS), SalonKind.TATTOO.sections());
    }

    @Test
    void theBarberChairEditsHairAndBeardOnly() {
        assertEquals(Set.of(AppearanceSection.HAIR, AppearanceSection.FACIAL_HAIR), SalonKind.HAIR.sections());
    }

    @Test
    void aClientNeedsAProfessionalNearby() {
        assertFalse(SalonKind.TATTOO.allowed(false, Set.of(), List.of()));
        assertFalse(SalonKind.TATTOO.allowed(false, Set.of(), List.of(Set.of("coiffeur"), Set.of())));
        assertTrue(SalonKind.TATTOO.allowed(false, Set.of(), List.of(Set.of("tatoueur"))));
        assertTrue(SalonKind.HAIR.allowed(false, Set.of(), List.of(Set.of("coiffeur"))));
    }

    @Test
    void aProfessionalOrAnOperatorNeedsNoOne() {
        assertTrue(SalonKind.TATTOO.allowed(false, Set.of("tatoueur"), List.of()));
        assertTrue(SalonKind.HAIR.allowed(true, Set.of(), List.of()));
        assertFalse(SalonKind.HAIR.allowed(false, Set.of("tatoueur"), List.of()));
    }

    @Test
    void everyKindHasAFrenchMessageAndATag() {
        for (SalonKind k : SalonKind.values()) {
            assertFalse(k.tag().isBlank());
            assertTrue(k.missingMessage().endsWith("."));
        }
    }
}
