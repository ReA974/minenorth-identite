package fr.minenorth.identite.appearance;

import fr.minenorth.identite.document.Gender;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class CatalogTest {
    @Test
    void everySectionHasSlots() {
        for (AppearanceSection s : AppearanceSection.values()) {
            assertFalse(Catalog.slots(s).isEmpty(), s.name());
        }
    }

    @Test
    void defaultsAreValidForBothGenders() {
        for (Gender g : Gender.values()) {
            assertTrue(Catalog.validate(g, Catalog.defaults(g)).isEmpty(), g.name());
        }
    }

    @Test
    void randomIsAlwaysValid() {
        Random r = new Random(42);
        for (Gender g : Gender.values()) {
            for (int i = 0; i < 500; i++) {
                assertTrue(Catalog.validate(g, Catalog.random(g, r)).isEmpty(), g.name() + " #" + i);
            }
        }
    }

    @Test
    void femaleCannotHaveFacialHair() {
        Appearance a = Catalog.defaults(Gender.FEMALE).with(AppearanceSection.FACIAL_HAIR, new int[]{1, 0});
        assertTrue(Catalog.validate(Gender.FEMALE, a).isPresent());
        assertTrue(Catalog.validate(Gender.MALE, a).isEmpty());
    }

    @Test
    void maleOnlyHairRejectedForFemale() {
        Catalog.Slot style = Catalog.slots(AppearanceSection.HAIR).get(0);
        int maleOnly = style.maleOnly().iterator().next();
        int[] hair = {maleOnly, 0};
        assertTrue(Catalog.validate(Gender.FEMALE, AppearanceSection.HAIR, hair).isPresent());
        assertTrue(Catalog.validate(Gender.MALE, AppearanceSection.HAIR, hair).isEmpty());
    }

    @Test
    void eachGenderHasAtLeastThreeExclusiveHairStyles() {
        Catalog.Slot style = Catalog.slots(AppearanceSection.HAIR).get(0);
        assertTrue(style.maleOnly().size() >= 3);
        assertTrue(style.femaleOnly().size() >= 3);
    }

    @Test
    void outOfRangeRejected() {
        int n = Catalog.slots(AppearanceSection.BODY).get(0).options();
        assertTrue(Catalog.validate(Gender.MALE, AppearanceSection.BODY, new int[]{-1}).isPresent());
        assertTrue(Catalog.validate(Gender.MALE, AppearanceSection.BODY, new int[]{n}).isPresent());
        assertTrue(Catalog.validate(Gender.MALE, AppearanceSection.BODY, new int[]{n - 1}).isEmpty());
    }

    @Test
    void wrongLengthRejected() {
        assertTrue(Catalog.validate(Gender.MALE, AppearanceSection.BODY, new int[]{0, 0}).isPresent());
        assertTrue(Catalog.validate(Gender.MALE, AppearanceSection.BODY, new int[]{}).isPresent());
    }

    @Test
    void catalogSizesMatchSpec() {
        assertEquals(1, Catalog.slots(AppearanceSection.BODY).size());
        assertEquals(6, Catalog.slots(AppearanceSection.BODY).get(0).options());
        assertEquals(5, Catalog.slots(AppearanceSection.FACE).size());
        assertEquals(2, Catalog.slots(AppearanceSection.SKIN).size());
        assertEquals(40, Catalog.slots(AppearanceSection.HAIR).get(0).options());
        assertEquals(12, Catalog.slots(AppearanceSection.FACIAL_HAIR).get(0).options());
        assertEquals(6, Catalog.slots(AppearanceSection.MAKEUP).size());
        assertEquals(22, Catalog.slots(AppearanceSection.TATTOOS).size());
        assertEquals(13, Catalog.slots(AppearanceSection.TATTOOS).get(0).options());
    }
}
