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
        // une vignette montre seulement l'option : ni maquillage, ni particularités, ni marque du visage, ni piercing d'une autre section
        if (section != AppearanceSection.MAKEUP) {
            int[] makeup = a.get(AppearanceSection.MAKEUP);
            a = a.with(AppearanceSection.MAKEUP, new int[makeup.length]);
        }
        if (section != AppearanceSection.SKIN) {
            int[] skin = a.get(AppearanceSection.SKIN);
            a = a.with(AppearanceSection.SKIN, new int[]{skin[0], 0});
        }
        if (section != AppearanceSection.TATTOOS) {
            int[] tattoos = a.get(AppearanceSection.TATTOOS);
            tattoos[0] = 0;
            a = a.with(AppearanceSection.TATTOOS, tattoos);
        }
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
        if (regionFor(section, key) == Region.HEAD && (section == AppearanceSection.SKIN || section == AppearanceSection.MAKEUP)) {
            return highDefinitionHead(SkinComposer.compose(gender, a, true), a);   // la même image que celle du jeu
        }
        int[] skin = SkinComposer.compose(gender, a);
        return switch (regionFor(section, key)) {
            case HEAD -> enhance(region(skin, 8, 8, 40, 8, 8, 8), 1.7);
            case TORSO -> enhance(region(skin, 20, 20, 20, 36, 8, 12), 2.2);
            case FIGURE -> enhance(figure(skin, gender.slimArms()), 2.6);
        };
    }

    /** Le visage en 32 × 32 : la tête peinte agrandie, avec par-dessus le maquillage et les particularités en haute définition. */
    private static Tile highDefinitionHead(int[] skin, Appearance a) {
        Tile small = region(skin, 8, 8, 40, 8, 8, 8);
        int[] out = new int[HiResFace.SIZE * HiResFace.SIZE];
        for (int y = 0; y < HiResFace.SIZE; y++) for (int x = 0; x < HiResFace.SIZE; x++) out[y * HiResFace.SIZE + x] = small.argb[(y / 4) * 8 + x / 4];
        int[] face = HiResFace.image(a);
        for (int i = 0; i < out.length; i++) out[i] = HiResTattoo.over(out[i], face[i]);
        return new Tile(HiResFace.SIZE, HiResFace.SIZE, out);
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

    /**
     * Le contraste est renforcé autour de la couleur moyenne de la vignette : les différences de modelé (corpulence, nez,
     * grains de beauté) sont de petites variations de ton, presque invisibles à cette taille sans ça.
     */
    private static Tile enhance(Tile t, double factor) {
        double[] sum = new double[3];
        int n = 0;
        for (int c : t.argb) {
            if (c == 0) continue;
            sum[0] += (c >> 16) & 0xFF;
            sum[1] += (c >> 8) & 0xFF;
            sum[2] += c & 0xFF;
            n++;
        }
        if (n == 0) return t;
        int[] out = new int[t.argb.length];
        for (int i = 0; i < out.length; i++) {
            int c = t.argb[i];
            if (c == 0) continue;
            int r = clamp(sum[0] / n + (((c >> 16) & 0xFF) - sum[0] / n) * factor);
            int g = clamp(sum[1] / n + (((c >> 8) & 0xFF) - sum[1] / n) * factor);
            int b = clamp(sum[2] / n + ((c & 0xFF) - sum[2] / n) * factor);
            out[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
        return new Tile(t.w, t.h, out);
    }

    private static int clamp(double v) { return (int) Math.max(0, Math.min(255, Math.round(v))); }

    /** Le haut du corps de face, 16 × 20 : tête, torse et bras assemblés comme sur le skin (les jambes ne changent pas avec la corpulence). */
    private static Tile figure(int[] skin, boolean slim) {
        Tile t = new Tile(16, 20, new int[16 * 20]);
        int arm = slim ? 3 : 4;
        paste(t, skin, 8, 8, 40, 8, 8, 8, 4, 0);                       // tête
        paste(t, skin, 20, 20, 20, 36, 8, 12, 4, 8);                   // torse
        paste(t, skin, 44, 20, 44, 36, arm, 12, 4 - arm, 8);           // bras droit (à gauche de l'image)
        paste(t, skin, 36, 52, 52, 52, arm, 12, 12, 8);                // bras gauche
        return t;
    }
}
