package fr.minenorth.identite.skin;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.appearance.TattooZone;
import fr.minenorth.identite.document.Gender;
import fr.minenorth.identite.drawing.DrawingKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tatouages en haute définition du torse et du dos : figures lissées, couche de 64 × 64, torse à gauche et dos à droite. */
class HiResTattooTest {
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
    void thereIsMoreDetailThanInTheSkin() {
        // à 32 × 32, un motif a bien plus de couleurs différentes qu'à 8 × 8 (relief, grain)
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
        assertEquals(HiResTattoo.ATLAS * HiResTattoo.ATLAS, torso.length);
        int tl = 0, tr = 0, bl = 0, br = 0;
        for (int y = 0; y < HiResTattoo.ATLAS; y++) {
            for (int x = 0; x < HiResTattoo.ATLAS; x++) {
                if ((torso[y * 64 + x] >>> 24) > 0) { if (x < 32) tl++; else tr++; }
                if ((back[y * 64 + x] >>> 24) > 0) { if (x < 32) bl++; else br++; }
            }
        }
        assertTrue(tl > 0 && tr == 0, "torse à gauche seulement");
        assertTrue(br > 0 && bl == 0, "dos à droite seulement");
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
                    for (int j = 0; j < 64; j++) {
                        for (int i = 0; i < 64; i++) {
                            if ((atlas[j * 64 + i] >>> 24) == 0) continue;
                            assertTrue(i < HiResTattoo.REGION_W && j < HiResTattoo.REGION_H, "pixel hors du torse : " + i + "," + j);
                        }
                    }
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
        for (int j = 0; j < 4; j++) for (int i = 0; i < 4; i++) assertTrue((atlas[(4 + j) * 64 + 8 + i] >>> 24) > 0xC0, "bloc " + i + "," + j);
        assertEquals(16, opaque(atlas));
    }

    @Test
    void hasAnyOnlyForTorsoAndBack() {
        assertFalse(HiResTattoo.hasAny(base()));
        assertTrue(HiResTattoo.hasAny(tattoo(TattooZone.TORSO, 2, 0, 0)));
        assertTrue(HiResTattoo.hasAny(tattoo(TattooZone.BACK, 2, 0, 0)));
        assertFalse(HiResTattoo.hasAny(tattoo(TattooZone.RIGHT_ARM, 2, 0, 0)), "les bras restent peints sur le skin dans ce pilote");
    }

    @Test
    void theLayeredSkinSkipsTorsoAndBackTattoosButKeepsTheOthers() {
        Appearance torso = tattoo(TattooZone.TORSO, 3, 2, 0);
        assertFalse(java.util.Arrays.equals(SkinComposer.compose(Gender.MALE, torso, false), SkinComposer.compose(Gender.MALE, base(), false)));
        assertArrayEquals(SkinComposer.compose(Gender.MALE, base(), true), SkinComposer.compose(Gender.MALE, torso, true), "plus de tatouage de torse peint");
        Appearance arm = tattoo(TattooZone.RIGHT_ARM, 3, 2, 0);
        assertFalse(java.util.Arrays.equals(SkinComposer.compose(Gender.MALE, base(), true), SkinComposer.compose(Gender.MALE, arm, true)), "le bras reste peint");
    }
}
