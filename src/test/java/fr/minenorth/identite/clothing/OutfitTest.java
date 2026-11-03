package fr.minenorth.identite.clothing;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.document.Gender;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OutfitTest {
    private static FriendlyByteBuf buf() { return new FriendlyByteBuf(Unpooled.buffer()); }

    @Test
    void emptyOutfitWearsNothing() {
        assertTrue(Outfit.EMPTY.isEmpty());
        assertNull(Outfit.EMPTY.get(ClothingSlot.UNDERWEAR));
    }

    @Test
    void starterOutfitMatchesGender() {
        assertEquals(Clothing.UNDERWEAR_MALE, Outfit.starter(Gender.MALE).get(ClothingSlot.UNDERWEAR));
        assertEquals(Clothing.UNDERWEAR_FEMALE, Outfit.starter(Gender.FEMALE).get(ClothingSlot.UNDERWEAR));
    }

    @Test
    void clothesKnowTheirIdSlotAndGender() {
        assertEquals("underwear_male", Clothing.UNDERWEAR_MALE.id());
        assertEquals(ClothingSlot.UNDERWEAR, Clothing.UNDERWEAR_FEMALE.slot());
        assertEquals(Gender.FEMALE, Clothing.UNDERWEAR_FEMALE.gender());
        assertEquals(Clothing.UNDERWEAR_FEMALE, Clothing.fromId("underwear_female"));
        assertNull(Clothing.fromId("nope"));
    }

    @Test
    void withAndWithoutAreImmutable() {
        Outfit worn = Outfit.EMPTY.with(Clothing.UNDERWEAR_MALE);
        assertTrue(Outfit.EMPTY.isEmpty());
        assertEquals(Clothing.UNDERWEAR_MALE, worn.get(ClothingSlot.UNDERWEAR));
        assertTrue(worn.without(ClothingSlot.UNDERWEAR).isEmpty());
        assertEquals(Clothing.UNDERWEAR_MALE, worn.get(ClothingSlot.UNDERWEAR));
    }

    @Test
    void tagRoundTrip() {
        Outfit o = Outfit.starter(Gender.FEMALE);
        assertEquals(o, Outfit.fromTag(o.toTag()));
        assertEquals(Outfit.EMPTY, Outfit.fromTag(Outfit.EMPTY.toTag()));
    }

    @Test
    void unknownClothingInSavedTagIsIgnored() {
        CompoundTag tag = new CompoundTag();
        tag.putString(ClothingSlot.UNDERWEAR.name(), "removed_item");
        assertTrue(Outfit.fromTag(tag).isEmpty());
    }

    @Test
    void bufferRoundTrip() {
        FriendlyByteBuf b = buf();
        Outfit.starter(Gender.MALE).write(b);
        assertEquals(Outfit.starter(Gender.MALE), Outfit.read(b));
    }

    @Test
    void readRejectsUnknownClothingIndex() {
        FriendlyByteBuf b = buf();
        b.writeVarInt(99);
        assertThrows(IllegalArgumentException.class, () -> Outfit.read(b));
    }

    // ---- intégration dans la fiche d'apparence

    @Test
    void appearanceKeepsItsOutfitThroughEveryOperation() {
        Appearance a = Catalog.defaults(Gender.FEMALE).withOutfit(Outfit.starter(Gender.FEMALE));
        assertEquals(Outfit.starter(Gender.FEMALE), a.withCustomSkin(true).outfit());
        assertEquals(Outfit.starter(Gender.FEMALE),
                a.with(fr.minenorth.identite.appearance.AppearanceSection.BODY, new int[]{1}).outfit());
    }

    @Test
    void appearanceOutfitSurvivesTagAndBuffer() {
        Appearance a = Catalog.defaults(Gender.MALE).withOutfit(Outfit.starter(Gender.MALE)).withCustomSkin(true);
        assertEquals(a, Appearance.fromTag(a.toTag()));
        FriendlyByteBuf b = buf();
        a.write(b);
        assertEquals(a, Appearance.read(b));
    }

    @Test
    void oldSavedAppearanceWithoutOutfitLoadsAsNaked() {
        CompoundTag tag = Catalog.defaults(Gender.MALE).toTag();
        tag.remove("Outfit");
        assertTrue(Appearance.fromTag(tag).outfit().isEmpty());
    }

    @Test
    void differentOutfitsMakeDifferentAppearances() {
        Appearance naked = Catalog.defaults(Gender.MALE);
        assertNotEquals(naked, naked.withOutfit(Outfit.starter(Gender.MALE)));
    }

    @Test
    void catalogRejectsClothesForTheOtherGender() {
        Appearance wrong = Catalog.defaults(Gender.MALE).withOutfit(Outfit.starter(Gender.FEMALE));
        assertTrue(Catalog.validate(Gender.MALE, wrong).isPresent());
        assertTrue(Catalog.validate(Gender.FEMALE, wrong).isEmpty());
        assertTrue(Catalog.validate(Gender.MALE, Catalog.defaults(Gender.MALE).withOutfit(Outfit.starter(Gender.MALE))).isEmpty());
    }
}
