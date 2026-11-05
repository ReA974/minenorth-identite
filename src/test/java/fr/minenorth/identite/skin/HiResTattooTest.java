package fr.minenorth.identite.skin;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.appearance.TattooZone;
import fr.minenorth.identite.document.Gender;
import fr.minenorth.identite.drawing.DrawingKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tatouages en haute définition : figures lissées, une image 128 × 144 avec une zone par face du corps. */
class HiResTattooTest {
    private static final int W = TattooRegions.ATLAS_W;

    private static Appearance base() { return Catalog.defaults(Gender.MALE).with(AppearanceSection.HAIR, new int[]{0, 1}); }

    private static Appearance tattoo(TattooZone zone, int motif, int size, int ink) {
        int[] t = new int[TattooZone.TOTAL_SLOTS];
        t[zone.motifSlot()] = motif;
        t[zone.sizeSlot()] = size;
        t[TattooZone.INK_SLOT] = ink;
        return base().with(AppearanceSection.TATTOOS, t);
    }

    private static int opaque(int[] px) {
        int n = 0;
        for (int v : px) if ((v >>> 24) > 0) n++;
        return n;
    }

    /** Nombre de pixels opaques de l'image dans le rectangle donné. */
    private static int inside(int[] atlas, int x0, int y0, int w, int h) {
        int n = 0;
        for (int y = y0; y < y0 + h; y++) for (int x = x0; x < x0 + w; x++) if ((atlas[y * W + x] >>> 24) > 0) n++;
        return n;
    }

    @Test
    void everyMotifIsSmoothedAtEverySize() {
        for (int m = 1; m <= TattooZone.MOTIFS; m++) {
            for (int size : new int[]{16, 24, 32}) {
                int[] px = HiResTattoo.motif(m, 0, size, size);
                assertEquals(size * size, px.length);
                assertTrue(opaque(px) >= size * size / 6, "motif " + m + " : presque vide à " + size);
                boolean soft = false;
                for (int v : px) {
                    int a = v >>> 24;
                    if (a > 8 && a < 0xE0) soft = true;
                }
                assertTrue(soft, "motif " + m + " : pas de bord adouci à " + size);
            }
        }
    }

    @Test
    void theShapeDoesNotGrowBeyondTheOriginalDrawing() {
        // un motif lissé couvre à peu près la même surface que le dessin d'origine (pas de bavure)
        for (int m = 1; m <= TattooZone.MOTIFS; m++) {
            int original = (int) java.util.Arrays.stream(SkinComposer.designPixels(m, 0)).filter(v -> v != 0).count();
            int[] px = HiResTattoo.motif(m, 0, 32, 32);
            double solid = 0;
            for (int v : px) solid += (v >>> 24) / 255.0;
            double ratio = solid / 16.0 / original;
            assertTrue(ratio > 0.55 && ratio < 1.25, "motif " + m + " : " + ratio + " de la surface d'origine");
        }
    }

    @Test
    void thereIsMoreDetailThanInTheSkin() {
        for (int m : new int[]{1, 6, 20, 31}) {
            long fine = java.util.Arrays.stream(HiResTattoo.motif(m, 0, 32, 32)).filter(v -> v != 0).distinct().count();
            long coarse = java.util.Arrays.stream(SkinComposer.designPixels(m, 0)).filter(v -> v != 0).distinct().count();
            assertTrue(fine > coarse * 3, "motif " + m + " : " + fine + " couleurs contre " + coarse);
        }
    }

    @Test
    void inkChangesTheLookButNotTheShape() {
        for (int m = 1; m <= TattooZone.MOTIFS; m++) {
            int[] color = HiResTattoo.motif(m, 0, 32, 32), black = HiResTattoo.motif(m, 1, 32, 32), grey = HiResTattoo.motif(m, 2, 32, 32);
            assertFalse(java.util.Arrays.equals(color, black), "noir " + m);
            assertFalse(java.util.Arrays.equals(color, grey), "gris " + m);
            for (int i = 0; i < color.length; i++) assertEquals(color[i] >>> 24, black[i] >>> 24, "forme motif " + m);
        }
    }

    @Test
    void isDeterministic() {
        assertArrayEquals(HiResTattoo.motif(5, 0, 32, 32), HiResTattoo.motif(5, 0, 32, 32));
    }

    @Test
    void noMotifOrOutOfRangeIsTransparent() {
        assertEquals(0, opaque(HiResTattoo.motif(0, 0, 32, 32)));
        assertEquals(0, opaque(HiResTattoo.motif(TattooZone.MOTIFS + 1, 0, 32, 32)));
    }

    @Test
    void theAtlasHoldsTheTorsoOnTheLeftAndTheBackOnTheRight() {
        int[] torso = HiResTattoo.atlas(tattoo(TattooZone.TORSO, 3, 2, 0));
        int[] back = HiResTattoo.atlas(tattoo(TattooZone.BACK, 3, 2, 0));
        assertEquals(W * TattooRegions.ATLAS_H, torso.length);
        assertTrue(inside(torso, 0, 0, 32, 48) > 0 && inside(torso, 32, 0, 32, 48) == 0, "torse à gauche seulement");
        assertTrue(inside(back, 32, 0, 32, 48) > 0 && inside(back, 0, 0, 32, 48) == 0, "dos à droite seulement");
    }

    @Test
    void everyLimbHasItsOwnPlaceInTheAtlas() {
        for (TattooZone zone : new TattooZone[]{TattooZone.RIGHT_ARM, TattooZone.LEFT_ARM, TattooZone.RIGHT_LEG, TattooZone.LEFT_LEG}) {
            for (int face = 0; face < 4; face++) {
                int[] t = new int[TattooZone.TOTAL_SLOTS];
                t[zone.motifSlot()] = 7;
                t[zone.sizeSlot()] = 2;
                t[TattooRegions.faceSlot(zone)] = face;
                int[] atlas = HiResTattoo.atlas(base().with(AppearanceSection.TATTOOS, t));
                int[] o = TattooRegions.origin(zone, face);
                int here = inside(atlas, o[0], o[1], 16, 48);
                assertTrue(here > 0, zone + " face " + face + " vide");
                assertEquals(opaque(atlas), here, zone + " face " + face + " déborde de sa zone");
            }
        }
    }

    @Test
    void theAtlasNeverLeavesTheRegions() {
        for (int size = 0; size < 3; size++) {
            for (int x : new int[]{0, 1, 8}) {
                for (int y : new int[]{0, 1, 12}) {
                    int[] t = new int[TattooZone.TOTAL_SLOTS];
                    t[TattooZone.TORSO.motifSlot()] = 6;
                    t[TattooZone.TORSO.sizeSlot()] = size;
                    t[TattooZone.TORSO_X_SLOT] = x;
                    t[TattooZone.TORSO_Y_SLOT] = y;
                    int[] atlas = HiResTattoo.atlas(base().with(AppearanceSection.TATTOOS, t));
                    assertEquals(opaque(atlas), inside(atlas, 0, 0, 32, 48), "taille " + size + " x " + x + " y " + y);
                }
            }
        }
    }

    @Test
    void aFreeHandDrawingAppearsAsCrispBlocks() {
        byte[] grid = new byte[DrawingKind.TATTOO_TORSO.width() * DrawingKind.TATTOO_TORSO.height()];
        grid[1 * 8 + 2] = 5;
        Appearance a = base().withDrawings(base().drawings().with(DrawingKind.TATTOO_TORSO, grid));
        assertTrue(HiResTattoo.hasAny(a));
        int[] atlas = HiResTattoo.atlas(a);
        for (int j = 0; j < 4; j++) for (int i = 0; i < 4; i++) assertTrue((atlas[(4 + j) * W + 8 + i] >>> 24) > 0xC0, "bloc " + i + "," + j);
        assertEquals(16, opaque(atlas));
    }

    @Test
    void aLimbDrawingIsOnTheFrontFace() {
        byte[] grid = new byte[DrawingKind.TATTOO_LEFT_LEG.width() * DrawingKind.TATTOO_LEFT_LEG.height()];
        grid[0] = 3;
        Appearance a = base().withDrawings(base().drawings().with(DrawingKind.TATTOO_LEFT_LEG, grid));
        int[] o = TattooRegions.origin(TattooZone.LEFT_LEG, 0);
        assertEquals(16, inside(HiResTattoo.atlas(a), o[0], o[1], 4, 4));
    }

    @Test
    void hasAnyForEveryZoneExceptTheFace() {
        assertFalse(HiResTattoo.hasAny(base()));
        for (TattooZone zone : TattooRegions.ZONES) assertTrue(HiResTattoo.hasAny(tattoo(zone, 2, 0, 0)), zone.name());
        int[] t = new int[TattooZone.TOTAL_SLOTS];
        t[TattooZone.FACE_NECK.motifSlot()] = 2;
        assertFalse(HiResTattoo.hasAny(base().with(AppearanceSection.TATTOOS, t)), "les marques du visage restent peintes");
    }

    @Test
    void theLayeredSkinHasNoBodyTattooButKeepsFaceMarks() {
        for (TattooZone zone : TattooRegions.ZONES) {
            Appearance a = tattoo(zone, 3, 2, 0);
            assertFalse(java.util.Arrays.equals(SkinComposer.compose(Gender.MALE, base(), false), SkinComposer.compose(Gender.MALE, a, false)), "peint : " + zone);
            assertArrayEquals(SkinComposer.compose(Gender.MALE, base(), true), SkinComposer.compose(Gender.MALE, a, true), "plus peint : " + zone);
        }
        int[] t = new int[TattooZone.TOTAL_SLOTS];
        t[TattooZone.FACE_NECK.motifSlot()] = 3;
        assertFalse(java.util.Arrays.equals(SkinComposer.compose(Gender.MALE, base(), true), SkinComposer.compose(Gender.MALE, base().with(AppearanceSection.TATTOOS, t), true)));
    }
}
