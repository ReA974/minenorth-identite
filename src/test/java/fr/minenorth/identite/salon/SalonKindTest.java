package fr.minenorth.identite.salon;

import fr.minenorth.identite.appearance.AppearanceSection;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SalonKindTest {
    @Test
    void theTattooChairEditsTattoosAndPiercingsOnly() {
        assertEquals(Set.of(AppearanceSection.TATTOOS, AppearanceSection.PIERCINGS), SalonKind.TATTOO.sections());
    }

    @Test
    void theBarberChairEditsHairAndBeardOnly() {
        assertEquals(Set.of(AppearanceSection.HAIR, AppearanceSection.FACIAL_HAIR), SalonKind.HAIR.sections());
    }
}
