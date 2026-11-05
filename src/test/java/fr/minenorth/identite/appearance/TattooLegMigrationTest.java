package fr.minenorth.identite.appearance;

import fr.minenorth.identite.document.Gender;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Les jambes ont chacune leur tatouage : les fiches d'avant gardent ce qu'elles avaient. */
class TattooLegMigrationTest {
    private static int[] load(int[] saved) {
        CompoundTag tag = Catalog.defaults(Gender.MALE).toTag();
        tag.putIntArray(AppearanceSection.TATTOOS.name(), saved);
        return Appearance.fromTag(tag).get(AppearanceSection.TATTOOS);
    }

    /** Fiche à 21 emplacements : zones, encre, bras, position, puis le choix du côté (0 les deux, 1 droite, 2 gauche). */
    private static int[] withSide(int side) {
        int[] t = new int[21];
        t[9] = 7;
        t[10] = 1;
        t[20] = side;
        return t;
    }

    @Test
    void bothLegsBecomeTwoTattoos() {
        int[] t = load(withSide(0));
        assertEquals(26, t.length);
        assertEquals(7, t[TattooZone.RIGHT_LEG.motifSlot()]);
        assertEquals(7, t[TattooZone.LEFT_LEG.motifSlot()]);
        assertEquals(1, t[TattooZone.LEFT_LEG.sizeSlot()]);
    }

    @Test
    void rightOnlyStaysOnTheRightLeg() {
        int[] t = load(withSide(1));
        assertEquals(7, t[TattooZone.RIGHT_LEG.motifSlot()]);
        assertEquals(0, t[TattooZone.LEFT_LEG.motifSlot()]);
    }

    @Test
    void leftOnlyMovesToTheLeftLeg() {
        int[] t = load(withSide(2));
        assertEquals(0, t[TattooZone.RIGHT_LEG.motifSlot()]);
        assertEquals(7, t[TattooZone.LEFT_LEG.motifSlot()]);
        assertEquals(1, t[TattooZone.LEFT_LEG.sizeSlot()]);
    }

    @Test
    void sheetsFromBeforeTheSideChoiceAreCopiedOnBothLegs() {
        int[] old = new int[16];
        old[9] = 4;
        old[10] = 2;
        int[] t = load(old);
        assertEquals(26, t.length);
        assertEquals(4, t[TattooZone.RIGHT_LEG.motifSlot()]);
        assertEquals(4, t[TattooZone.LEFT_LEG.motifSlot()]);
        assertEquals(2, t[TattooZone.LEFT_LEG.sizeSlot()]);
    }

    @Test
    void currentSheetsAreLeftAlone() {
        int[] t = new int[26];
        t[TattooZone.RIGHT_LEG.motifSlot()] = 3;
        t[TattooZone.LEFT_LEG.motifSlot()] = 9;
        int[] back = load(t);
        assertEquals(3, back[TattooZone.RIGHT_LEG.motifSlot()]);
        assertEquals(9, back[TattooZone.LEFT_LEG.motifSlot()]);
    }
}
