package fr.minenorth.identite.skin;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.document.Gender;
import fr.minenorth.identite.drawing.DrawingKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Une tête importée (dessin du visage) ne cache plus sourcils, taches, maquillage, barbe ni marques du visage. */
class ImportedHeadTest {
    private static final int W = SkinComposer.SIZE;

    private static Appearance bare(Gender g) { return Catalog.defaults(g).with(AppearanceSection.HAIR, new int[]{0, 1}); }

    private static Appearance withImportedHead(Appearance a) {
        byte[] grid = new byte[DrawingKind.FACE.width() * DrawingKind.FACE.height()];
        java.util.Arrays.fill(grid, (byte) 5);   // un visage entier, tout opaque
        return a.withDrawings(a.drawings().with(DrawingKind.FACE, grid));
    }

    private static int total(int[] a, int[] b) {
        int n = 0;
        for (int i = 0; i < W * W; i++) if (a[i] != b[i]) n++;
        return n;
    }

    private static int[] head(Gender g, Appearance a) { return SkinComposer.compose(g, a); }

    @Test
    void anImportedHeadStillShowsFreckles() {
        Appearance base = withImportedHead(bare(Gender.FEMALE));
        Appearance freckles = base.with(AppearanceSection.SKIN, new int[]{base.get(AppearanceSection.SKIN)[0], 2});
        assertTrue(total(head(Gender.FEMALE, base), head(Gender.FEMALE, freckles)) > 0, "taches de rousseur invisibles sur la tête importée");
    }

    @Test
    void anImportedHeadStillShowsBrowsAndMakeup() {
        Appearance base = withImportedHead(bare(Gender.FEMALE));
        int[] face = base.get(AppearanceSection.FACE);
        int[] thick = face.clone();
        thick[2] = 1;
        int[] thin = face.clone();
        thin[2] = 5;
        assertTrue(total(head(Gender.FEMALE, base.with(AppearanceSection.FACE, thick)), head(Gender.FEMALE, base.with(AppearanceSection.FACE, thin))) > 0, "sourcils");
        int[] makeup = base.get(AppearanceSection.MAKEUP).clone();
        makeup[4] = 4;
        assertTrue(total(head(Gender.FEMALE, base), head(Gender.FEMALE, base.with(AppearanceSection.MAKEUP, makeup))) > 0, "maquillage");
    }

    @Test
    void anImportedHeadStillShowsBeardAndFaceMarks() {
        Appearance base = withImportedHead(bare(Gender.MALE));
        assertTrue(total(head(Gender.MALE, base), head(Gender.MALE, base.with(AppearanceSection.FACIAL_HAIR, new int[]{9, 1}))) > 0, "barbe");
        int[] tattoos = base.get(AppearanceSection.TATTOOS).clone();
        tattoos[0] = 3;
        assertTrue(total(head(Gender.MALE, base), head(Gender.MALE, base.with(AppearanceSection.TATTOOS, tattoos))) > 0, "marque du visage");
    }

    @Test
    void theCatalogFeaturesAreNotAppliedTwiceWithAnEmptyImportedHead() {
        Appearance plain = bare(Gender.FEMALE).with(AppearanceSection.SKIN, new int[]{3, 2});
        byte[] empty = new byte[DrawingKind.FACE.width() * DrawingKind.FACE.height()];
        assertArrayEquals(head(Gender.FEMALE, plain), head(Gender.FEMALE, plain.withDrawings(plain.drawings().with(DrawingKind.FACE, empty))));
    }
}
