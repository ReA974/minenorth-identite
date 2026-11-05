package fr.minenorth.identite.skin;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.document.Gender;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Vignettes des options de la création : le visage, le ventre ou le corps de face avec l'option choisie. */
class OptionPreviewTest {
    private static Appearance base(Gender g) { return Catalog.defaults(g); }

    private static List<int[]> tiles(Gender g, AppearanceSection section, int slot, String key) {
        List<int[]> out = new ArrayList<>();
        for (int v = 0; v < Catalog.slots(section).get(slot).options(); v++) {
            if (!Catalog.slots(section).get(slot).allows(g, v)) continue;
            out.add(OptionPreview.of(g, base(g), section, slot, key, v).argb());
        }
        return out;
    }

    @Test
    void sizesFollowTheRegion() {
        assertEquals(8, OptionPreview.of(Gender.MALE, base(Gender.MALE), AppearanceSection.FACE, 0, "eyes", 0).w());
        assertEquals(8, OptionPreview.of(Gender.MALE, base(Gender.MALE), AppearanceSection.FACE, 0, "eyes", 0).h());
        assertEquals(12, OptionPreview.of(Gender.MALE, base(Gender.MALE), AppearanceSection.PIERCINGS, 6, "navel", 1).h());
        assertEquals(32, OptionPreview.of(Gender.FEMALE, base(Gender.FEMALE), AppearanceSection.BODY, 0, "build", 0).h());
        assertEquals(OptionPreview.Region.FIGURE, OptionPreview.regionFor(AppearanceSection.BODY, "build"));
        assertEquals(OptionPreview.Region.HEAD, OptionPreview.regionFor(AppearanceSection.MAKEUP, "lips"));
    }

    @Test
    void everyEyeShapeLooksDifferent() {
        for (Gender g : Gender.values()) {
            List<int[]> images = tiles(g, AppearanceSection.FACE, 0, "eyes");
            for (int i = 0; i < images.size(); i++) {
                for (int j = i + 1; j < images.size(); j++) assertFalse(java.util.Arrays.equals(images.get(i), images.get(j)), g + " yeux " + i + " et " + j);
            }
        }
    }

    @Test
    void browsNoseAndMouthShowTheirDifferences() {
        for (String[] s : new String[][]{{"2", "brows"}, {"3", "nose"}, {"4", "mouth"}}) {
            int slot = Integer.parseInt(s[0]);
            List<int[]> images = tiles(Gender.MALE, AppearanceSection.FACE, slot, s[1]);
            long distinct = images.stream().map(java.util.Arrays::hashCode).distinct().count();
            assertEquals(images.size(), distinct, s[1]);
        }
    }

    @Test
    void skinMarksAndMakeupAreVisible() {
        long marks = tiles(Gender.FEMALE, AppearanceSection.SKIN, 1, "marks").stream().map(java.util.Arrays::hashCode).distinct().count();
        assertEquals(Catalog.slots(AppearanceSection.SKIN).get(1).options(), marks);
        for (int slot : new int[]{0, 2, 4}) {
            String key = Catalog.slots(AppearanceSection.MAKEUP).get(slot).key();
            List<int[]> images = tiles(Gender.FEMALE, AppearanceSection.MAKEUP, slot, key);
            assertEquals(images.size(), images.stream().map(java.util.Arrays::hashCode).distinct().count(), key);
        }
    }

    @Test
    void piercingsAreVisibleOnTheirTile() {
        for (int slot : new int[]{1, 2, 3, 6}) {
            String key = Catalog.slots(AppearanceSection.PIERCINGS).get(slot).key();
            List<int[]> images = tiles(Gender.MALE, AppearanceSection.PIERCINGS, slot, key);
            assertEquals(images.size(), images.stream().map(java.util.Arrays::hashCode).distinct().count(), key);
        }
    }

    @Test
    void anImportedHeadOrHairNeverHidesTheOption() {
        Appearance hairy = base(Gender.MALE).with(AppearanceSection.HAIR, new int[]{12, 3});
        int[] a = OptionPreview.of(Gender.MALE, hairy, AppearanceSection.FACE, 2, "brows", 1).argb();
        int[] b = OptionPreview.of(Gender.MALE, hairy, AppearanceSection.FACE, 2, "brows", 5).argb();
        assertFalse(java.util.Arrays.equals(a, b));
    }

    @Test
    void buildsDifferOnTheFigure() {
        List<int[]> images = tiles(Gender.FEMALE, AppearanceSection.BODY, 0, "build");
        assertEquals(images.size(), images.stream().map(java.util.Arrays::hashCode).distinct().count());
    }
}
