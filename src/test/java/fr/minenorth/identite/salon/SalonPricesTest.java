package fr.minenorth.identite.salon;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.appearance.TattooZone;
import fr.minenorth.identite.document.Gender;
import fr.minenorth.identite.drawing.DrawingKind;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SalonPricesTest {
    private static final Appearance BASE = Catalog.defaults(Gender.MALE);

    private static Appearance tattoo(Appearance a, TattooZone zone, int motif, int size, int ink) {
        int[] t = a.get(AppearanceSection.TATTOOS);
        t[zone.motifSlot()] = motif;
        if (zone.hasSize()) t[zone.sizeSlot()] = size;
        t[TattooZone.INK_SLOT] = ink;
        return a.with(AppearanceSection.TATTOOS, t);
    }

    private static long tattooPrice(Appearance before, Appearance after) { return SalonPrices.quote(SalonKind.TATTOO, before, after); }

    @Test
    void nothingChangedCostsNothing() {
        assertEquals(0, tattooPrice(BASE, BASE));
        assertEquals(0, SalonPrices.quote(SalonKind.HAIR, BASE, BASE));
    }

    @Test
    void aCatalogTattooCostsMoreWhenBigger() {
        assertEquals(SalonPrices.TATTOO_SMALL, tattooPrice(BASE, tattoo(BASE, TattooZone.TORSO, 3, 0, 1)));
        assertEquals(SalonPrices.TATTOO_MEDIUM, tattooPrice(BASE, tattoo(BASE, TattooZone.TORSO, 3, 1, 1)));
        assertEquals(SalonPrices.TATTOO_LARGE, tattooPrice(BASE, tattoo(BASE, TattooZone.TORSO, 3, 2, 1)));
    }

    @Test
    void colorInkCostsExtra() {
        assertEquals(SalonPrices.TATTOO_SMALL + SalonPrices.COLOR_INK, tattooPrice(BASE, tattoo(BASE, TattooZone.BACK, 4, 0, 0)));
    }

    @Test
    void aFaceMarkIsTheCheapest() {
        assertEquals(SalonPrices.FACE_MARK, tattooPrice(BASE, tattoo(BASE, TattooZone.FACE_NECK, 2, 0, 1)));
        assertTrue(SalonPrices.FACE_MARK < SalonPrices.TATTOO_SMALL);
    }

    @Test
    void anExistingTattooLeftAloneIsNotPaidAgain() {
        Appearance has = tattoo(BASE, TattooZone.TORSO, 3, 1, 1);
        assertEquals(0, tattooPrice(has, has));
        Appearance plusArm = tattoo(has, TattooZone.LEFT_ARM, 5, 0, 1);
        assertEquals(SalonPrices.TATTOO_SMALL, tattooPrice(has, plusArm));
    }

    @Test
    void movingAnExistingTattooIsPaidAsANewOne() {
        Appearance has = tattoo(BASE, TattooZone.TORSO, 3, 0, 1);
        int[] t = has.get(AppearanceSection.TATTOOS);
        t[TattooZone.TORSO_X_SLOT] = 2;
        assertEquals(SalonPrices.TATTOO_SMALL, tattooPrice(has, has.with(AppearanceSection.TATTOOS, t)));
    }

    @Test
    void removingATattooCostsALaserSession() {
        Appearance has = tattoo(BASE, TattooZone.RIGHT_LEG, 3, 0, 1);
        assertEquals(SalonPrices.REMOVAL, tattooPrice(has, tattoo(has, TattooZone.RIGHT_LEG, 0, 0, 1)));
    }

    @Test
    void aDrawnTattooCostsMuchMoreThanACatalogOne() {
        byte[] grid = new byte[DrawingKind.TATTOO_TORSO.width() * DrawingKind.TATTOO_TORSO.height()];
        grid[5] = 1;
        long drawn = tattooPrice(BASE, BASE.withDrawings(BASE.drawings().with(DrawingKind.TATTOO_TORSO, grid)));
        assertEquals(SalonPrices.DRAWN_BODY, drawn);
        assertTrue(drawn > SalonPrices.TATTOO_LARGE + SalonPrices.COLOR_INK);
        byte[] arm = new byte[DrawingKind.TATTOO_RIGHT_ARM.width() * DrawingKind.TATTOO_RIGHT_ARM.height()];
        arm[3] = 1;
        long limb = tattooPrice(BASE, BASE.withDrawings(BASE.drawings().with(DrawingKind.TATTOO_RIGHT_ARM, arm)));
        assertEquals(SalonPrices.DRAWN_LIMB, limb);
        assertTrue(limb > SalonPrices.TATTOO_LARGE + SalonPrices.COLOR_INK);
    }

    @Test
    void theBarberChargesPerChange() {
        Appearance cut = BASE.with(AppearanceSection.HAIR, new int[]{20, BASE.get(AppearanceSection.HAIR)[1]});
        assertEquals(SalonPrices.HAIR_CUT, SalonPrices.quote(SalonKind.HAIR, BASE, cut));
        Appearance dye = cut.with(AppearanceSection.HAIR, new int[]{20, 5});
        assertEquals(SalonPrices.HAIR_COLOR, SalonPrices.quote(SalonKind.HAIR, cut, dye));
        assertEquals(SalonPrices.HAIR_CUT + SalonPrices.HAIR_COLOR, SalonPrices.quote(SalonKind.HAIR, BASE, dye));
        Appearance beard = BASE.with(AppearanceSection.FACIAL_HAIR, new int[]{3, 2});
        assertEquals(SalonPrices.BEARD_STYLE + SalonPrices.BEARD_COLOR, SalonPrices.quote(SalonKind.HAIR, BASE, beard));
    }

    @Test
    void dyeingBaldHeadsOrNoBeardIsFree() {
        Appearance bald = BASE.with(AppearanceSection.HAIR, new int[]{0, 1});
        assertEquals(0, SalonPrices.quote(SalonKind.HAIR, bald, bald.with(AppearanceSection.HAIR, new int[]{0, 7})));
        assertEquals(0, SalonPrices.quote(SalonKind.HAIR, BASE, BASE.with(AppearanceSection.FACIAL_HAIR, new int[]{0, 7})));
    }

    @Test
    void otherEditorsAreFree() {
        assertEquals(0, SalonPrices.quote(null, BASE, tattoo(BASE, TattooZone.TORSO, 3, 2, 0)));
        assertNull(SalonKind.forSections(Set.of(AppearanceSection.HAIR)));
        assertEquals(SalonKind.HAIR, SalonKind.forSections(SalonKind.HAIR.sections()));
        assertEquals(SalonKind.TATTOO, SalonKind.forSections(SalonKind.TATTOO.sections()));
    }

    private static Appearance pierced(Appearance a, int slot, int value, int metal) {
        int[] p = a.get(AppearanceSection.PIERCINGS);
        p[slot] = value;
        p[4] = metal;
        return a.with(AppearanceSection.PIERCINGS, p);
    }

    @Test
    void piercingsArePaidAtTheTattooSalon() {
        assertEquals(SalonPrices.PIERCING_NOSE[1], tattooPrice(BASE, pierced(BASE, 1, 1, 0)));
        assertEquals(SalonPrices.PIERCING_NAVEL[3], tattooPrice(BASE, pierced(BASE, 6, 3, 2)));
        assertTrue(SalonPrices.PIERCING_LIP[3] > SalonPrices.PIERCING_LIP[1], "snake bites : deux bijoux");
    }

    @Test
    void goldAndRoseGoldCostExtraPerPiercing() {
        long silver = tattooPrice(BASE, pierced(BASE, 2, 1, 0));
        assertEquals(silver + SalonPrices.PRECIOUS_METAL, tattooPrice(BASE, pierced(BASE, 2, 1, 1)));
        assertEquals(silver + SalonPrices.PRECIOUS_METAL, tattooPrice(BASE, pierced(BASE, 2, 1, 3)));
        assertEquals(silver, tattooPrice(BASE, pierced(BASE, 2, 1, 4)));
    }

    @Test
    void anExistingPiercingKeptAsIsIsNotPaidAgain() {
        Appearance has = pierced(BASE, 1, 4, 0);
        assertEquals(0, tattooPrice(has, has));
        assertEquals(SalonPrices.PIERCING_REMOVAL, tattooPrice(has, pierced(has, 1, 0, 0)));
        assertEquals(SalonPrices.PIERCING_NOSE[4] + SalonPrices.PRECIOUS_METAL, tattooPrice(has, pierced(has, 1, 4, 1)), "changer de métal = refaire le piercing");
    }

    @Test
    void tattoosAndPiercingsAddUp() {
        Appearance both = pierced(tattoo(BASE, TattooZone.TORSO, 3, 0, 1), 6, 1, 0);
        assertEquals(SalonPrices.TATTOO_SMALL + SalonPrices.PIERCING_NAVEL[1], tattooPrice(BASE, both));
    }

    private static Appearance makeup(Appearance a, int lips, int lipColor, int eyes, int blush) {
        int[] m = a.get(AppearanceSection.MAKEUP);
        m[0] = lips;
        m[1] = lipColor;
        m[2] = eyes;
        m[4] = blush;
        return a.with(AppearanceSection.MAKEUP, m);
    }

    @Test
    void makeupIsPaidAtTheMakeupSalon() {
        assertEquals(0, SalonPrices.quote(SalonKind.MAKEUP, BASE, BASE));
        assertEquals(SalonPrices.LIPS, SalonPrices.quote(SalonKind.MAKEUP, BASE, makeup(BASE, 2, 0, 0, 0)));
        assertEquals(SalonPrices.LIPS + SalonPrices.EYES + SalonPrices.BLUSH, SalonPrices.quote(SalonKind.MAKEUP, BASE, makeup(BASE, 2, 0, 3, 1)));
    }

    @Test
    void onlyChangingTheColorCostsLess() {
        Appearance has = makeup(BASE, 2, 0, 0, 0);
        assertEquals(SalonPrices.MAKEUP_COLOR, SalonPrices.quote(SalonKind.MAKEUP, has, makeup(has, 2, 3, 0, 0)));
        assertTrue(SalonPrices.MAKEUP_COLOR < SalonPrices.LIPS);
        assertEquals(0, SalonPrices.quote(SalonKind.MAKEUP, has, makeup(has, 0, 0, 0, 0)), "retirer un maquillage est gratuit");
    }

    @Test
    void marksAndSkinToneArePaid() {
        Appearance mark = BASE.with(AppearanceSection.SKIN, new int[]{BASE.get(AppearanceSection.SKIN)[0], 2});
        assertEquals(SalonPrices.SKIN_MARK, SalonPrices.quote(SalonKind.MAKEUP, BASE, mark));
        Appearance tan = BASE.with(AppearanceSection.SKIN, new int[]{6, 0});
        assertEquals(SalonPrices.SKIN_TONE, SalonPrices.quote(SalonKind.MAKEUP, BASE, tan));
    }
}
