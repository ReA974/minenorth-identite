package fr.minenorth.identite.appearance;

import fr.minenorth.identite.document.Gender;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TattooZoneTest {
    @Test
    void everyZoneHasAMotifSlotWithAllTheMotifs() {
        for (TattooZone z : TattooZone.values()) {
            Catalog.Slot slot = Catalog.slots(AppearanceSection.TATTOOS).get(z.motifSlot());
            assertEquals(z == TattooZone.FACE_NECK ? 13 : TattooZone.MOTIFS + 1, slot.options(), z.toString());
        }
    }

    @Test
    void zonesWithASizeHaveAThreeOptionSizeSlotRightAfterTheirMotif() {
        for (TattooZone z : TattooZone.values()) {
            if (z == TattooZone.FACE_NECK) {
                assertFalse(z.hasSize());
                continue;
            }
            assertTrue(z.hasSize());
            assertEquals(z.motifSlot() + 1, z.sizeSlot());
            assertEquals(3, Catalog.slots(AppearanceSection.TATTOOS).get(z.sizeSlot()).options(), z.toString());
        }
    }

    @Test
    void slotsAreUsedExactlyOnce() {
        boolean[] used = new boolean[Catalog.slots(AppearanceSection.TATTOOS).size()];
        for (TattooZone z : TattooZone.values()) {
            assertFalse(used[z.motifSlot()]);
            used[z.motifSlot()] = true;
            if (z.hasSize()) {
                assertFalse(used[z.sizeSlot()]);
                used[z.sizeSlot()] = true;
            }
        }
        for (int i = TattooZone.INK_SLOT; i < used.length; i++) used[i] = true;
        for (boolean u : used) assertTrue(u);
    }

    @Test
    void newTattoosStartSmallAndEmpty() {
        int[] t = Catalog.defaults(Gender.MALE).get(AppearanceSection.TATTOOS);
        for (int v : t) assertEquals(0, v);
    }

    // ---- anciennes fiches (6 zones, sans taille) : les motifs sont gardés, en grand comme avant

    @Test
    void oldSheetsKeepTheirTattoosAtTheirOldSize() {
        CompoundTag tag = Catalog.defaults(Gender.MALE).toTag();
        tag.putIntArray(AppearanceSection.TATTOOS.name(), new int[]{3, 5, 7, 9, 11, 12});
        int[] t = Appearance.fromTag(tag).get(AppearanceSection.TATTOOS);
        assertEquals(16, t.length);
        assertEquals(3, t[TattooZone.FACE_NECK.motifSlot()]);
        assertEquals(5, t[TattooZone.TORSO.motifSlot()]);
        assertEquals(2, t[TattooZone.TORSO.sizeSlot()]);
        assertEquals(7, t[TattooZone.BACK.motifSlot()]);
        assertEquals(9, t[TattooZone.RIGHT_ARM.motifSlot()]);
        assertEquals(11, t[TattooZone.LEFT_ARM.motifSlot()]);
        assertEquals(12, t[TattooZone.LEGS.motifSlot()]);
        assertEquals(2, t[TattooZone.LEGS.sizeSlot()]);
    }

    @Test
    void anOldSheetWithAnInvalidMotifIsStillRefused() {
        CompoundTag tag = Catalog.defaults(Gender.MALE).toTag();
        tag.putIntArray(AppearanceSection.TATTOOS.name(), new int[]{3, 99, 0, 0, 0, 0});
        assertThrows(IllegalArgumentException.class, () -> Appearance.fromTag(tag));
    }

    @Test
    void anAppearanceWithNoLegacyTattooLoadsAsIs() {
        Appearance a = Catalog.defaults(Gender.FEMALE);
        assertEquals(a, Appearance.fromTag(a.toTag()));
    }
}
