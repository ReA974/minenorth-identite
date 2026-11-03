package fr.minenorth.identite.document;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IdentityDataTest {
    private static CompoundTag legacyTag(UUID id, String genderOrNull) {
        CompoundTag p = new CompoundTag();
        p.putString("UUID", id.toString());
        p.putString("LastName", "Dupont");
        p.putString("FirstName", "Jean");
        p.putString("BirthDate", "01/02/2000");
        p.putString("BirthPlace", "Paris");
        p.putString("Nationality", "Française");
        p.putString("Card", "FR-12345678");
        if (genderOrNull != null) p.putString("Gender", genderOrNull);
        ListTag list = new ListTag();
        list.add(p);
        CompoundTag root = new CompoundTag();
        root.put("Profiles", list);
        return root;
    }

    @Test
    void legacyProfileLoadsWithNullGender() {
        UUID id = UUID.randomUUID();
        IdentityData data = IdentityData.load(legacyTag(id, null));
        assertNull(data.get(id).gender());
        assertTrue(data.get(id).isLegacy());
    }

    @Test
    void genderRoundTrips() {
        UUID id = UUID.randomUUID();
        IdentityData data = new IdentityData();
        data.set(new IdentityData.Profile(id, "Martin", "Alice", "03/04/1999", "Lyon", "Française", "FR-87654321", Gender.FEMALE));
        IdentityData reloaded = IdentityData.load(data.save(new CompoundTag()));
        assertEquals(Gender.FEMALE, reloaded.get(id).gender());
        assertFalse(reloaded.get(id).isLegacy());
    }

    @Test
    void unknownGenderStringLoadsAsNull() {
        UUID id = UUID.randomUUID();
        IdentityData data = IdentityData.load(legacyTag(id, "ALIEN"));
        assertNull(data.get(id).gender());
    }

    @Test
    void genderLabelsAndArms() {
        assertEquals("Homme", Gender.MALE.label());
        assertEquals("Femme", Gender.FEMALE.label());
        assertFalse(Gender.MALE.slimArms());
        assertTrue(Gender.FEMALE.slimArms());
        assertNull(Gender.fromName("nope"));
        assertEquals(Gender.MALE, Gender.fromName("MALE"));
    }
}
