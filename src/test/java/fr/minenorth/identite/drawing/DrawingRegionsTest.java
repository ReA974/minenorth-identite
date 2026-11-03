package fr.minenorth.identite.drawing;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.document.Gender;
import fr.minenorth.identite.skin.SkinComposer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DrawingRegionsTest {
    private static Appearance bare(Gender g) {
        return Catalog.defaults(g).with(AppearanceSection.HAIR, new int[]{0, 1});
    }

    private static Appearance withCell(Gender g, DrawingKind kind, int x, int y) {
        byte[] grid = new byte[kind.width() * kind.height()];
        grid[y * kind.width() + x] = 1;
        return bare(g).withDrawings(Drawings.EMPTY.with(kind, grid));
    }

    @Test
    void theRegionIsReadAtTheSamePlaceTheComposerDraws() {
        for (Gender g : Gender.values()) {
            for (DrawingKind kind : DrawingKind.values()) {
                int x = 1, y = 2;
                int[] naked = DrawingRegions.read(SkinComposer.compose(g, bare(g)), kind, g.slimArms());
                int[] drawn = DrawingRegions.read(SkinComposer.compose(g, withCell(g, kind, x, y)), kind, g.slimArms());
                assertEquals(kind.width() * kind.height(), drawn.length);
                int changed = 0;
                for (int i = 0; i < drawn.length; i++) {
                    if (drawn[i] != naked[i]) {
                        changed++;
                        assertEquals(y * kind.width() + x, i, g + " " + kind + " : mauvaise case");
                    }
                }
                assertEquals(1, changed, g + " " + kind);
            }
        }
    }

    @Test
    void slimArmsHaveNoFourthColumn() {
        int[] region = DrawingRegions.read(SkinComposer.compose(Gender.FEMALE, bare(Gender.FEMALE)), DrawingKind.TATTOO_RIGHT_ARM, true);
        for (int y = 0; y < 12; y++) {
            assertEquals(0, region[y * 4 + 3], "colonne 4 hors du bras fin, ligne " + y);
            assertNotEquals(0, region[y * 4 + 2]);
        }
        int[] male = DrawingRegions.read(SkinComposer.compose(Gender.MALE, bare(Gender.MALE)), DrawingKind.TATTOO_RIGHT_ARM, false);
        for (int y = 0; y < 12; y++) assertNotEquals(0, male[y * 4 + 3]);
    }

    @Test
    void wrongSkinSizeIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> DrawingRegions.read(new int[10], DrawingKind.FACE, false));
    }
}
