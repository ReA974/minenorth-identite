package fr.minenorth.identite.skin;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.document.Gender;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Richesse des options : chaque choix du catalogue doit se voir, et se distinguer des autres. */
class SkinDetailTest {
    private static final int W = SkinComposer.SIZE;

    private static Appearance bare(Gender g) {
        Appearance a = Catalog.defaults(g)
                .with(AppearanceSection.HAIR, new int[]{0, 3})
                .with(AppearanceSection.FACIAL_HAIR, new int[]{0, 3});
        int[] face = a.get(AppearanceSection.FACE);
        return a.with(AppearanceSection.FACE, face);
    }

    private static int[] px(Gender g, Appearance a) { return SkinComposer.compose(g, a); }

    private static List<Integer> allowed(Gender g, AppearanceSection s, int slot) {
        Catalog.Slot sl = Catalog.slots(s).get(slot);
        List<Integer> out = new ArrayList<>();
        for (int v = 0; v < sl.options(); v++) if (sl.allows(g, v)) out.add(v);
        return out;
    }

    private static Appearance with(Appearance a, AppearanceSection s, int slot, int value) {
        int[] v = a.get(s);
        v[slot] = value;
        return a.with(s, v);
    }

    private static int diff(int[] a, int[] b) {
        int n = 0;
        for (int i = 0; i < a.length; i++) if (a[i] != b[i]) n++;
        return n;
    }

    private static void assertAllDistinct(List<int[]> images, String what) {
        for (int i = 0; i < images.size(); i++) {
            for (int j = i + 1; j < images.size(); j++) {
                assertTrue(diff(images.get(i), images.get(j)) > 0, what + " : options " + i + " et " + j + " identiques");
            }
        }
    }

    private static List<int[]> sweep(Gender g, AppearanceSection s, int slot, Appearance base) {
        List<int[]> out = new ArrayList<>();
        for (int v : allowed(g, s, slot)) out.add(px(g, with(base, s, slot, v)));
        return out;
    }

    // ---------------------------------------------------------------- cheveux

    @Test
    void everyHairStyleDrawsOnTheHeadOverlay() {
        for (Gender g : Gender.values()) {
            for (int style : allowed(g, AppearanceSection.HAIR, 0)) {
                if (style == 0) continue;
                int[] p = px(g, with(bare(g), AppearanceSection.HAIR, 0, style));
                int count = 0;
                for (int y = 0; y < 16; y++) for (int x = 32; x < 64; x++) if ((p[y * W + x] >>> 24) != 0) count++;
                assertTrue(count >= 10, g + " coupe " + style + " : " + count + " pixels");
            }
        }
    }

    @Test
    void hairStylesAreAllDistinctForEachGender() {
        for (Gender g : Gender.values()) assertAllDistinct(sweep(g, AppearanceSection.HAIR, 0, bare(g)), "coupes " + g);
    }

    @Test
    void hairHasStrandDetailNotAFlatColor() {
        int[] p = px(Gender.MALE, with(bare(Gender.MALE), AppearanceSection.HAIR, 0, 9));
        Set<Integer> colors = new HashSet<>();
        for (int y = 0; y < 8; y++) for (int x = 40; x < 48; x++) colors.add(p[y * W + x]);
        assertTrue(colors.size() >= 3, "couleurs distinctes : " + colors.size());
    }

    @Test
    void womenHaveMoreHairStylesAndMenKeepTheirs() {
        assertTrue(allowed(Gender.FEMALE, AppearanceSection.HAIR, 0).size() >= 20);
        assertTrue(allowed(Gender.MALE, AppearanceSection.HAIR, 0).size() >= 15);
    }

    // ---------------------------------------------------------------- pilosité

    @Test
    void facialHairStylesAreAllDistinct() {
        assertTrue(allowed(Gender.MALE, AppearanceSection.FACIAL_HAIR, 0).size() >= 12);
        assertAllDistinct(sweep(Gender.MALE, AppearanceSection.FACIAL_HAIR, 0, bare(Gender.MALE)), "pilosité");
    }

    @Test
    void womenCannotHaveFacialHair() {
        assertEquals(List.of(0), allowed(Gender.FEMALE, AppearanceSection.FACIAL_HAIR, 0));
    }

    // ---------------------------------------------------------------- tatouages

    @Test
    void everyTattooMotifShowsInEveryZone() {
        Catalog.Slot motifs = Catalog.slots(AppearanceSection.TATTOOS).get(0);
        assertTrue(motifs.options() >= 13, "au moins 12 motifs");
        int[] none = px(Gender.MALE, bare(Gender.MALE));
        for (int zone = 0; zone < 6; zone++) {
            for (int m = 1; m < motifs.options(); m++) {
                int[] t = new int[6];
                t[zone] = m;
                int n = diff(none, px(Gender.MALE, bare(Gender.MALE).with(AppearanceSection.TATTOOS, t)));
                assertTrue(n >= 3, "zone " + zone + " motif " + m + " : " + n + " pixels");
            }
        }
    }

    @Test
    void tattooMotifsAreDistinctInEveryZone() {
        Catalog.Slot motifs = Catalog.slots(AppearanceSection.TATTOOS).get(0);
        for (int zone = 0; zone < 6; zone++) {
            List<int[]> images = new ArrayList<>();
            for (int m = 1; m < motifs.options(); m++) {
                int[] t = new int[6];
                t[zone] = m;
                images.add(px(Gender.FEMALE, bare(Gender.FEMALE).with(AppearanceSection.TATTOOS, t)));
            }
            assertAllDistinct(images, "tatouages zone " + zone);
        }
    }

    @Test
    void tattoosUseSeveralColors() {
        int[] none = px(Gender.MALE, bare(Gender.MALE));
        int[] t = new int[6];
        t[1] = 8;
        int[] p = px(Gender.MALE, bare(Gender.MALE).with(AppearanceSection.TATTOOS, t));
        Set<Integer> colors = new HashSet<>();
        for (int i = 0; i < p.length; i++) if (p[i] != none[i]) colors.add(p[i]);
        assertTrue(colors.size() >= 3, "couleurs : " + colors.size());
    }

    // ---------------------------------------------------------------- visage, maquillage, corps

    @Test
    void faceOptionsAreDistinct() {
        for (Gender g : Gender.values()) {
            for (int slot : new int[]{0, 2, 3, 4}) assertAllDistinct(sweep(g, AppearanceSection.FACE, slot, bare(g)), "visage " + g + " slot " + slot);
        }
        assertTrue(allowed(Gender.FEMALE, AppearanceSection.FACE, 0).size() > allowed(Gender.MALE, AppearanceSection.FACE, 0).size());
    }

    @Test
    void makeupOptionsAreDistinctAndVisible() {
        Appearance base = bare(Gender.FEMALE);
        int[] none = px(Gender.FEMALE, base);
        for (int slot : new int[]{0, 2, 4}) {
            List<int[]> images = sweep(Gender.FEMALE, AppearanceSection.MAKEUP, slot, base);
            assertAllDistinct(images, "maquillage slot " + slot);
            for (int i = 1; i < images.size(); i++) assertTrue(diff(none, images.get(i)) > 0);
        }
    }

    @Test
    void skinMarksAreDistinct() {
        assertAllDistinct(sweep(Gender.MALE, AppearanceSection.SKIN, 1, bare(Gender.MALE)), "particularités");
        assertTrue(Catalog.slots(AppearanceSection.SKIN).get(1).options() >= 7);
    }

    @Test
    void buildsAreDistinctAndWomenGetTheirOwn() {
        for (Gender g : Gender.values()) assertAllDistinct(sweep(g, AppearanceSection.BODY, 0, bare(g)), "corpulence " + g);
        assertTrue(allowed(Gender.FEMALE, AppearanceSection.BODY, 0).size() >= 5);
    }

    @Test
    void everyOptionOfEveryGenderStaysTransparentWhereItShould() {
        for (Gender g : Gender.values()) {
            int[] base = px(g, bare(g));
            for (AppearanceSection s : AppearanceSection.values()) {
                for (int slot = 0; slot < Catalog.slots(s).size(); slot++) {
                    for (int v : allowed(g, s, slot)) {
                        int[] p = px(g, with(bare(g), s, slot, v));
                        // la forme du skin (alpha de la couche de base) ne change jamais : seule la couche extérieure varie
                        for (int y = 0; y < W; y++) {
                            for (int x = 0; x < W; x++) {
                                boolean baseLayer = (y < 16 && x < 32) || (y >= 16 && y < 32 && x < 56) || (y >= 48 && x < 48 && x >= 16);
                                if (baseLayer) {
                                    assertEquals(base[y * W + x] >>> 24, p[y * W + x] >>> 24, s + "/" + slot + "=" + v + " altère la forme en " + x + "," + y);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
