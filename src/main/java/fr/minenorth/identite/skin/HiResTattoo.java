package fr.minenorth.identite.skin;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.TattooZone;
import fr.minenorth.identite.drawing.DrawingKind;

/**
 * Tatouages en haute définition. Le skin ne peut pas faire mieux que 8 × 12 pixels par face ; ici le tatouage est dessiné
 * sur une fine couche posée sur la peau (torse, dos, bras et jambes), quatre fois plus fine : les figures du catalogue
 * sont lissées (bords adoucis), éclairées (relief) et légèrement granuleuses comme de l'encre. Code pur : le rendu en jeu
 * ne fait qu'afficher cette image ({@link TattooRegions} dit où chaque face se trouve).
 */
public final class HiResTattoo {
    private HiResTattoo() {}

    /** Pixels de la couche pour un pixel de skin. */
    public static final int SCALE = 4;

    /** Vrai s'il y a un tatouage (catalogue ou dessin) sur le corps, ou du maquillage ou une particularité de la peau : sinon la couche n'est pas dessinée. */
    public static boolean hasAny(Appearance a) {
        int[] t = a.get(AppearanceSection.TATTOOS);
        for (TattooZone zone : TattooRegions.ZONES) {
            if (t[zone.motifSlot()] > 0) return true;
            if (!blank(a.drawings().get(TattooRegions.drawing(zone)))) return true;
        }
        return HiResFace.hasAny(a);
    }

    private static boolean blank(byte[] grid) {
        for (byte b : grid) if (b != 0) return false;
        return true;
    }

    /** L'image ARGB de la couche, {@link TattooRegions#ATLAS_W} × {@link TattooRegions#ATLAS_H} (transparente là où il n'y a rien). */
    public static int[] atlas(Appearance a) {
        int[] out = new int[TattooRegions.ATLAS_W * TattooRegions.ATLAS_H];
        int[] t = a.get(AppearanceSection.TATTOOS);
        for (TattooZone zone : TattooRegions.ZONES) {
            int motif = t[zone.motifSlot()];
            if (motif > 0) {
                int face = TattooRegions.isLimb(zone) ? t[TattooRegions.faceSlot(zone)] : 0;
                int[] origin = TattooRegions.origin(zone, face);
                int[] p = TattooPlacement.of(zone, t);
                int[] art = motif(motif, t[TattooZone.INK_SLOT], p[2] * SCALE, p[3] * SCALE);
                blit(out, origin[0] + p[0] * SCALE, origin[1] + p[1] * SCALE, art, p[2] * SCALE, p[3] * SCALE, origin, TattooRegions.width(zone));
            }
            paintDrawing(out, zone, a.drawings().get(TattooRegions.drawing(zone)));
        }
        int[] face = HiResFace.image(a);
        for (int y = 0; y < HiResFace.SIZE; y++) {
            for (int x = 0; x < HiResFace.SIZE; x++) {
                out[(TattooRegions.FACE_ORIGIN[1] + y) * TattooRegions.ATLAS_W + TattooRegions.FACE_ORIGIN[0] + x] = face[y * HiResFace.SIZE + x];
            }
        }
        return out;
    }

    /** Le dessin libre : chaque case de la grille devient un carré net de 4 × 4, sur la face avant. */
    private static void paintDrawing(int[] atlas, TattooZone zone, byte[] grid) {
        DrawingKind kind = TattooRegions.drawing(zone);
        int[] origin = TattooRegions.origin(zone, 0);
        for (int y = 0; y < kind.height(); y++) {
            for (int x = 0; x < kind.width(); x++) {
                int index = grid[y * kind.width() + x];
                if (index == 0) continue;
                int color = (kind.color(index) & 0x00FFFFFF) | 0xE6000000;
                for (int j = 0; j < SCALE; j++) {
                    for (int i = 0; i < SCALE; i++) {
                        int px = origin[0] + x * SCALE + i, py = origin[1] + y * SCALE + j;
                        atlas[py * TattooRegions.ATLAS_W + px] = over(atlas[py * TattooRegions.ATLAS_W + px], color);
                    }
                }
            }
        }
    }

    /** Pose le motif ; ce qui sortirait de la face (largeur {@code faceWidth}, 48 de haut) est coupé. */
    private static void blit(int[] atlas, int x0, int y0, int[] art, int w, int h, int[] origin, int faceWidth) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int px = x0 + x, py = y0 + y;
                if (px < origin[0] || px >= origin[0] + faceWidth || py < origin[1] || py >= origin[1] + TattooRegions.HEIGHT) continue;
                if (art[y * w + x] == 0) continue;
                atlas[py * TattooRegions.ATLAS_W + px] = over(atlas[py * TattooRegions.ATLAS_W + px], art[y * w + x]);
            }
        }
    }

    /** {@code src} posé sur {@code dst} (alpha non prémultiplié). */
    static int over(int dst, int src) {
        double sa = (src >>> 24) / 255.0, da = (dst >>> 24) / 255.0;
        double oa = sa + da * (1 - sa);
        if (oa <= 0) return 0;
        int r = (int) Math.round((((src >> 16) & 0xFF) * sa + ((dst >> 16) & 0xFF) * da * (1 - sa)) / oa);
        int g = (int) Math.round((((src >> 8) & 0xFF) * sa + ((dst >> 8) & 0xFF) * da * (1 - sa)) / oa);
        int b = (int) Math.round(((src & 0xFF) * sa + (dst & 0xFF) * da * (1 - sa)) / oa);
        return ((int) Math.round(oa * 255) << 24) | (r << 16) | (g << 8) | b;
    }

    // ------------------------------------------------------------------ un motif lissé

    /**
     * Le motif (1 à {@link TattooZone#MOTIFS}) en largeur × hauteur pixels ARGB. Le dessin 8 × 8 d'origine sert de guide :
     * contour, couleur principale et secondaire sont lissés séparément, le contour est un peu renforcé, les bords ont une
     * transparence progressive centrée sur le bord d'origine (le motif ne grossit pas), un relief éclaire les bords haut et
     * gauche, et un grain léger imite l'encre.
     */
    public static int[] motif(int motif, int ink, int w, int h) {
        int[] out = new int[w * h];
        if (motif < 1 || motif > TattooZone.MOTIFS) return out;
        String[] art = SkinComposer.DESIGNS[motif - 1];
        int[] colors = SkinComposer.DESIGN_COLORS[motif - 1];
        double[][] outline = new double[8][8], main = new double[8][8], second = new double[8][8];
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                switch (art[y].charAt(x)) {
                    case '#' -> outline[y][x] = 1;
                    case 'o' -> main[y][x] = 1;
                    case 'a' -> second[y][x] = 1;
                    default -> { }
                }
            }
        }
        int outlineColor = SkinComposer.inked(SkinComposer.OUTLINE, '#', ink);
        int mainColor = SkinComposer.inked(colors[0], 'o', ink);
        int secondColor = SkinComposer.inked(colors[1], 'a', ink);

        double[] cover = new double[w * h];
        double[] wo = new double[w * h], wm = new double[w * h], ws = new double[w * h];
        for (int ty = 0; ty < h; ty++) {
            for (int tx = 0; tx < w; tx++) {
                double sx = (tx + 0.5) * 8.0 / w - 0.5, sy = (ty + 0.5) * 8.0 / h - 0.5;
                double o = sample(outline, sx, sy), m = sample(main, sx, sy), s = sample(second, sx, sy);
                int i = ty * w + tx;
                cover[i] = smooth(0.40, 0.60, Math.min(1, o + m + s));
                wo[i] = o * 1.6;
                wm[i] = m;
                ws[i] = s;
            }
        }
        for (int ty = 0; ty < h; ty++) {
            for (int tx = 0; tx < w; tx++) {
                int i = ty * w + tx;
                if (cover[i] < 0.02) continue;
                double sum = wo[i] + wm[i] + ws[i];
                int color = sum <= 0 ? mainColor : blend(outlineColor, wo[i] / sum, mainColor, wm[i] / sum, secondColor, ws[i] / sum);
                // relief : les bords tournés vers le haut et la gauche sont éclairés, les autres assombris (sauf le contour)
                double g = at(cover, w, h, tx + 1, ty) + at(cover, w, h, tx, ty + 1) - at(cover, w, h, tx - 1, ty) - at(cover, w, h, tx, ty - 1);
                double outlineShare = wo[i] / Math.max(sum, 1e-6);
                double light = 1 + g * 0.16 * (1 - outlineShare);
                // grain : une variation douce et fixe (la même sur tous les clients)
                int hash = tx * 73856093 ^ ty * 19349663 ^ motif * 83492791;
                hash = (hash ^ (hash >>> 13)) * 1274126177;
                double grain = 0.965 + 0.07 * ((hash >>> 8 & 0xFF) / 255.0);
                int shaded = Palette.shade(color | 0xFF000000, Math.max(0.55, Math.min(1.35, light * grain)));
                int alpha = (int) Math.round(cover[i] * 0xF2);
                out[i] = (alpha << 24) | (shaded & 0x00FFFFFF);
            }
        }
        return out;
    }

    private static double at(double[] field, int w, int h, int x, int y) {
        if (x < 0 || y < 0 || x >= w || y >= h) return 0;
        return field[y * w + x];
    }

    /** Valeur d'un champ 8 × 8 à une position fractionnaire (interpolation bilinéaire, bords prolongés). */
    private static double sample(double[][] f, double x, double y) {
        double cx = Math.max(0, Math.min(7, x)), cy = Math.max(0, Math.min(7, y));
        int x0 = (int) Math.floor(cx), y0 = (int) Math.floor(cy);
        int x1 = Math.min(7, x0 + 1), y1 = Math.min(7, y0 + 1);
        double fx = cx - x0, fy = cy - y0;
        double top = f[y0][x0] * (1 - fx) + f[y0][x1] * fx;
        double bottom = f[y1][x0] * (1 - fx) + f[y1][x1] * fx;
        return top * (1 - fy) + bottom * fy;
    }

    private static double smooth(double edge0, double edge1, double v) {
        double t = Math.max(0, Math.min(1, (v - edge0) / (edge1 - edge0)));
        return t * t * (3 - 2 * t);
    }

    private static int blend(int c1, double w1, int c2, double w2, int c3, double w3) {
        int r = (int) Math.round(((c1 >> 16) & 0xFF) * w1 + ((c2 >> 16) & 0xFF) * w2 + ((c3 >> 16) & 0xFF) * w3);
        int g = (int) Math.round(((c1 >> 8) & 0xFF) * w1 + ((c2 >> 8) & 0xFF) * w2 + ((c3 >> 8) & 0xFF) * w3);
        int b = (int) Math.round((c1 & 0xFF) * w1 + (c2 & 0xFF) * w2 + (c3 & 0xFF) * w3);
        return 0xFF000000 | (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
    }
}
