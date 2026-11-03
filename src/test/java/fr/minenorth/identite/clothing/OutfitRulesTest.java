package fr.minenorth.identite.clothing;

import fr.minenorth.identite.document.Gender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OutfitRulesTest {
    @Test
    void wearingIntoAnEmptySlotWorks() {
        OutfitRules.Change c = OutfitRules.wear(Gender.MALE, Outfit.EMPTY, Clothing.UNDERWEAR_MALE);
        assertTrue(c.error().isEmpty());
        assertEquals(Clothing.UNDERWEAR_MALE, c.outfit().get(ClothingSlot.UNDERWEAR));
        assertTrue(c.returned().isEmpty());
    }

    @Test
    void clothesOfTheOtherGenderAreRefused() {
        OutfitRules.Change c = OutfitRules.wear(Gender.MALE, Outfit.EMPTY, Clothing.UNDERWEAR_FEMALE);
        assertTrue(c.error().isPresent());
        assertTrue(c.outfit().isEmpty());
    }

    @Test
    void wearingTheSameClothingAgainChangesNothing() {
        Outfit worn = Outfit.starter(Gender.FEMALE);
        OutfitRules.Change c = OutfitRules.wear(Gender.FEMALE, worn, Clothing.UNDERWEAR_FEMALE);
        assertTrue(c.error().isPresent());
        assertEquals(worn, c.outfit());
        assertTrue(c.returned().isEmpty());
    }

    @Test
    void removingGivesTheClothingBack() {
        OutfitRules.Change c = OutfitRules.remove(Outfit.starter(Gender.MALE), ClothingSlot.UNDERWEAR);
        assertTrue(c.error().isEmpty());
        assertTrue(c.outfit().isEmpty());
        assertEquals(Clothing.UNDERWEAR_MALE, c.returned().orElseThrow());
    }

    @Test
    void removingFromAnEmptySlotIsRefused() {
        OutfitRules.Change c = OutfitRules.remove(Outfit.EMPTY, ClothingSlot.UNDERWEAR);
        assertTrue(c.error().isPresent());
        assertTrue(c.returned().isEmpty());
    }

    @Test
    void pickFromInventoryTakesTheFirstClothingFitForTheSlotAndGender() {
        java.util.List<Clothing> inventory = java.util.List.of(Clothing.UNDERWEAR_FEMALE, Clothing.UNDERWEAR_MALE, Clothing.UNDERWEAR_MALE);
        assertEquals(1, OutfitRules.pickFromInventory(Gender.MALE, ClothingSlot.UNDERWEAR, inventory).orElseThrow());
        assertEquals(0, OutfitRules.pickFromInventory(Gender.FEMALE, ClothingSlot.UNDERWEAR, inventory).orElseThrow());
    }

    @Test
    void pickFromInventoryFindsNothingWhenNoneFits() {
        assertTrue(OutfitRules.pickFromInventory(Gender.MALE, ClothingSlot.UNDERWEAR, java.util.List.of()).isEmpty());
        assertTrue(OutfitRules.pickFromInventory(Gender.MALE, ClothingSlot.UNDERWEAR, java.util.List.of(Clothing.UNDERWEAR_FEMALE)).isEmpty());
    }
}
