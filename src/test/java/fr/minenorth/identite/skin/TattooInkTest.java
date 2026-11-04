package fr.minenorth.identite.skin;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.appearance.TattooZone;
import fr.minenorth.identite.document.Gender;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Encre des tatouages : couleurs du motif, noir ou gris ombré ; 30 figures. */
class TattooInkTest {
    private static int[] torso(int motif, int ink) {
        Appearance base = Catalog.defaults(Gender.MALE).with(AppearanceSection.HAIR, new int[]{0, 1});
        int[] t = new int[TattooZone.TOTAL_SLOTS];
        t[TattooZone.TORSO.motifSlot()] = motif;
        t[TattooZone.TORSO.sizeSlot()] = 2;
        t[TattooZone.INK_SLOT] = ink;
        return SkinComposer.compose(Gender.MALE, base.with(AppearanceSection.TATTOOS, t));
    }

    @Test
    void thereAreThirtyFigures() {
        assertEquals(30, TattooZone.MOTIFS);
        for (TattooZone z : TattooZone.values()) {
            if (z != TattooZone.FACE_NECK) {
                assertEquals(31, Catalog.slots(AppearanceSection.TATTOOS).get(z.motifSlot()).options());
            }
        }
    }

    @Test
    void inkIsTheLastSlotWithThreeChoices() {
        assertEquals(TattooZone.TOTAL_SLOTS, Catalog.slots(AppearanceSection.TATTOOS).size());
        assertEquals(3, Catalog.slots(AppearanceSection.TATTOOS).get(TattooZone.INK_SLOT).options());
    }

    @Test
    void blackAndGreyInksChangeTheLookOfEveryFigure() {
        for (int m = 1; m <= TattooZone.MOTIFS; m++) {
            int[] color = torso(m, 0);
            assertFalse(java.util.Arrays.equals(color, torso(m, 1)), "noir, motif " + m);
            assertFalse(java.util.Arrays.equals(color, torso(m, 2)), "gris, motif " + m);
            assertFalse(java.util.Arrays.equals(torso(m, 1), torso(m, 2)), "noir et gris, motif " + m);
        }
    }

    @Test
    void greyInkHasNoColorTint() {
        int[] none = torso(0, 0);
        for (int m = 1; m <= TattooZone.MOTIFS; m++) {
            int[] p = torso(m, 2);
            for (int i = 0; i < p.length; i++) {
                if (p[i] == none[i]) continue;
                int r = (p[i] >> 16) & 0xFF, g = (p[i] >> 8) & 0xFF, b = p[i] & 0xFF;
                int skin = (none[i] >> 16) & 0xFF;
                // le pixel est un mélange de peau et d'encre grise : jamais plus coloré que la peau d'origine
                assertTrue(Math.abs(r - g) <= Math.abs(skin - ((none[i] >> 8) & 0xFF)) + 4, "motif " + m + " pixel " + i);
                assertTrue(b >= 0);
            }
        }
    }

    @Test
    void oldSheetsWithoutAnInkChoiceKeepTheColors() {
        CompoundTag tag = Catalog.defaults(Gender.MALE).toTag();
        tag.putIntArray(AppearanceSection.TATTOOS.name(), new int[11]);
        int[] t = Appearance.fromTag(tag).get(AppearanceSection.TATTOOS);
        assertEquals(TattooZone.TOTAL_SLOTS, t.length);
        assertEquals(0, t[TattooZone.INK_SLOT]);
    }
}
