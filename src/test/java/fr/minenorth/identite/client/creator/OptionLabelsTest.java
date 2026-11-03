package fr.minenorth.identite.client.creator;

import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OptionLabelsTest {
    @Test
    void buildLabelsMatchSpec() {
        assertEquals("Normale", OptionLabels.option(AppearanceSection.BODY, "build", 0));
        assertEquals("Athlétique", OptionLabels.option(AppearanceSection.BODY, "build", 1));
        assertEquals("Ronde", OptionLabels.option(AppearanceSection.BODY, "build", 2));
    }

    @Test
    void everyOptionAndSlotHasALabel() {
        for (AppearanceSection s : AppearanceSection.values()) {
            for (Catalog.Slot slot : Catalog.slots(s)) {
                assertFalse(OptionLabels.slot(s, slot.key()).isBlank(), s + "/" + slot.key());
                for (int v = 0; v < slot.options(); v++) {
                    assertFalse(OptionLabels.option(s, slot.key(), v).isBlank(), s + "/" + slot.key() + "/" + v);
                }
            }
        }
    }

    @Test
    void colorSlotsAreDetected() {
        assertTrue(OptionLabels.isColor("color"));
        assertTrue(OptionLabels.isColor("tone"));
        assertTrue(OptionLabels.isColor("eyeColor"));
        assertTrue(OptionLabels.isColor("lipColor"));
        assertFalse(OptionLabels.isColor("style"));
        assertFalse(OptionLabels.isColor("nose"));
    }
}
