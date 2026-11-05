package fr.minenorth.identite.skin;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.document.Gender;
import fr.minenorth.identite.drawing.DrawingKind;

/**
 * Vignette d'une option du catalogue : le skin est composé avec cette option choisie (cheveux, barbe et dessins du
 * visage mis de côté pour qu'on voie bien l'option), puis on en découpe la partie qui compte : le visage, le ventre ou
 * le corps entier de face. Code pur, utilisé par l'écran de création.
 */
public final class OptionPreview {
    private OptionPreview() {}

    /** Image ARGB, ligne par ligne ; 0 = transparent. */
    public record Tile(int w, int h, int[] argb) {}

    private static final int SIZE = SkinComposer.SIZE;

    /** Ce que montre la vignette d'un réglage : la tête, le ventre, ou tout le corps de face. */
    public enum Region { HEAD, TORSO, FIGURE }

    public static Region regionFor(AppearanceSection section, String key) {
        if (section == AppearanceSection.BODY) return Region.FIGURE;
        if (section == AppearanceSection.PIERCINGS && key.equals("navel")) return Region.TORSO;
        return Region.HEAD;
    }

    public static Tile of(Gender gender, Appearance current, AppearanceSection section, int slot, String key, int value) {
        int[] values = current.get(section);
        values[slot] = value;
        Appearance a = current.with(section, values);
        // on retire ce qui cacherait l'option : cheveux, barbe, tête importée
        int[] hair = a.get(AppearanceSection.HAIR);
        a = a.with(AppearanceSection.HAIR, new int[]{0, hair[1]});
        if (section != AppearanceSection.FACIAL_HAIR) {
            int[] beard = a.get(AppearanceSection.FACIAL_HAIR);
            a = a.with(AppearanceSection.FACIAL_HAIR, new int[]{0, beard[1]});
        }
        if (section != AppearanceSection.TATTOOS) {
            a = a.withDrawings(a.drawings()
                    .with(DrawingKind.FACE, new byte[DrawingKind.FACE.width() * DrawingKind.FACE.height()])
                    .with(DrawingKind.BEARD, new byte[DrawingKind.BEARD.width() * DrawingKind.BEARD.height()]));
        }
        int[] skin = SkinComposer.compose(gender, a);
        return switch (regionFor(section, key)) {
            case HEAD -> region(skin, 8, 8, 40, 8, 8, 8);
            case TORSO -> region(skin, 20, 20, 20, 36, 8, 12);
            case FIGURE -> figure(skin, gender.slimArms());
        };
    }

    /** Une zone de la couche de base, avec la couche extérieure par-dessus. */
    private static Tile region(int[] skin, int x, int y, int ox, int oy, int w, int h) {
        Tile t = new Tile(w, h, new int[w * h]);
        paste(t, skin, x, y, ox, oy, w, h, 0, 0);
        return t;
    }

    private static void paste(Tile t, int[] skin, int x, int y, int ox, int oy, int w, int h, int dx, int dy) {
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int base = skin[(y + j) * SIZE + x + i];
                int outer = skin[(oy + j) * SIZE + ox + i];
                int color = (outer >>> 24) >= 128 ? outer : base;
                if ((color >>> 24) < 128) continue;
                t.argb[(dy + j) * t.w + dx + i] = color | 0xFF000000;
            }
        }
    }

    /** Le corps de face, 16 × 32 : tête, torse, bras et jambes assemblés comme sur le skin. */
    private static Tile figure(int[] skin, boolean slim) {
        Tile t = new Tile(16, 32, new int[16 * 32]);
        int arm = slim ? 3 : 4;
        paste(t, skin, 8, 8, 40, 8, 8, 8, 4, 0);                       // tête
        paste(t, skin, 20, 20, 20, 36, 8, 12, 4, 8);                   // torse
        paste(t, skin, 44, 20, 44, 36, arm, 12, 4 - arm, 8);           // bras droit (à gauche de l'image)
        paste(t, skin, 36, 52, 52, 52, arm, 12, 12, 8);                // bras gauche
        paste(t, skin, 4, 20, 4, 36, 4, 12, 4, 20);                    // jambe droite
        paste(t, skin, 20, 52, 4, 52, 4, 12, 8, 20);                   // jambe gauche
        return t;
    }
}
