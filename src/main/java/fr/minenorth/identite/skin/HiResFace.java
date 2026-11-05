package fr.minenorth.identite.skin;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;

/**
 * Maquillage et particularités de la peau (taches de rousseur, grains de beauté, cicatrices) en haute définition : une
 * image 32 × 32 posée juste devant le visage, quatre fois plus fine que les 8 × 8 pixels du skin. Les lèvres ont une vraie
 * forme, les joues un dégradé doux, l'eye-liner un trait fin, les grains de beauté sont ronds. Code pur ; l'image est
 * rangée dans {@link TattooRegions#FACE_ORIGIN} de la couche de tatouages. Coordonnées : 1 pixel du visage = 4 ici,
 * la colonne 0 est à gauche de l'écran.
 */
public final class HiResFace {
    private HiResFace() {}

    public static final int SIZE = 32;

    /** Vrai s'il y a du maquillage ou une particularité de la peau à dessiner. */
    public static boolean hasAny(Appearance a) {
        int[] makeup = a.get(AppearanceSection.MAKEUP);
        return makeup[0] > 0 || makeup[2] > 0 || makeup[4] > 0 || a.get(AppearanceSection.SKIN)[1] > 0;
    }

    /** L'image 32 × 32 ARGB (transparente là où il n'y a rien). */
    public static int[] image(Appearance a) {
        Layer layer = new Layer();
        int[] makeup = a.get(AppearanceSection.MAKEUP);
        int[] skin = a.get(AppearanceSection.SKIN);
        int tone = Palette.skinTone(skin[0]);
        marks(layer, skin[1], tone);
        blush(layer, makeup[4], Palette.lipColor(makeup[5]));
        shadow(layer, makeup[2], Palette.eyeColor(makeup[3]));
        lips(layer, makeup[0], Palette.lipColor(makeup[1]));
        return layer.px;
    }

    // ------------------------------------------------------------------ maquillage

    private static void blush(Layer l, int style, int color) {
        if (style <= 0) return;
        double alpha = new double[]{0, 0.2, 0.3, 0.38, 0.48}[style];
        double radius = new double[]{0, 4.0, 5.0, 5.8, 6.4}[style];
        l.soft(6.5, 22.5, radius, radius * 0.75, color, alpha);
        l.soft(25.5, 22.5, radius, radius * 0.75, color, alpha);
    }

    private static void shadow(Layer l, int style, int color) {
        if (style <= 0) return;
        for (int side = 0; side < 2; side++) {
            boolean left = side == 0;
            double cx = left ? 8 : 24, dir = left ? -1 : 1;   // dir : vers l'extérieur du visage
            switch (style) {
                case 1 -> l.soft(cx, 13.2, 4.8, 2.6, color, 0.38);
                case 2 -> {
                    l.soft(cx, 13.0, 5.4, 3.6, color, 0.5);
                    l.soft(cx + dir * 1.2, 12.0, 3.4, 2.2, Palette.shade(color, 0.7), 0.45);
                }
                case 3 -> l.line(cx - 4, 15.6, cx + 4, 15.6, 1.2, Palette.shade(color, 0.3), 0.9);
                case 4 -> {
                    l.line(cx - 4, 15.6, cx + 4, 15.6, 1.2, Palette.shade(color, 0.3), 0.9);
                    l.line(cx + dir * 4, 15.6, cx + dir * 6.6, 13.4, 1.1, Palette.shade(color, 0.3), 0.9);   // la pointe vers l'extérieur
                }
                default -> {   // scintillant : un voile et un reflet clair au centre de la paupière
                    l.soft(cx, 13.2, 4.8, 2.8, color, 0.42);
                    l.soft(cx, 13.4, 1.8, 1.0, Palette.mix(color, 0xFFFFFFFF, 0.65), 0.7);
                }
            }
        }
    }

    private static void lips(Layer l, int style, int color) {
        if (style <= 0) return;
        double grow = style == 2 ? 1.0 : 0.0;
        double alpha = style == 1 ? 0.6 : style == 5 ? 0.88 : style == 4 ? 0.95 : 0.8;
        int tint = style == 5 ? Palette.shade(color, 0.65) : color;
        // lèvre supérieure en deux lobes (arc de Cupidon), lèvre inférieure plus pleine
        l.soft(13.8, 26.2, 3.0 + grow, 1.5 + grow * 0.3, tint, alpha, 3.2);
        l.soft(18.2, 26.2, 3.0 + grow, 1.5 + grow * 0.3, tint, alpha, 3.2);
        l.soft(16.0, 28.4, 5.0 + grow, 1.9 + grow * 0.4, tint, alpha, 3.2);
        if (style == 4) l.line(11.8, 27.3, 20.2, 27.3, 0.7, Palette.shade(color, 0.5), 0.8);
        if (style == 3) l.soft(16.0, 28.6, 2.4, 0.8, 0xFFFFFFFF, 0.5, 2.2);   // brillant
    }

    // ------------------------------------------------------------------ particularités de la peau

    private static void marks(Layer l, int style, int tone) {
        int dark = Palette.shade(tone, 0.5), freckle = Palette.shade(tone, 0.64);
        switch (style) {
            case 1 -> l.soft(26.0, 22.0, 1.5, 1.5, dark, 0.95, 3.4);
            case 2 -> {
                for (int i = 0; i < 18; i++) {
                    int h1 = hash(i, 1), h2 = hash(i, 2), h3 = hash(i, 3);
                    double zone = (h1 & 0xFF) / 255.0;
                    double x, y;
                    if (zone < 0.38) { x = 4.5 + (h2 & 0xFF) / 255.0 * 8; y = 19 + (h3 & 0xFF) / 255.0 * 6; }
                    else if (zone < 0.76) { x = 19.5 + (h2 & 0xFF) / 255.0 * 8; y = 19 + (h3 & 0xFF) / 255.0 * 6; }
                    else { x = 13 + (h2 & 0xFF) / 255.0 * 6; y = 18.5 + (h3 & 0xFF) / 255.0 * 3.5; }
                    l.soft(x, y, 0.75 + (h1 >>> 8 & 3) * 0.12, 0.75 + (h1 >>> 8 & 3) * 0.12, freckle, 0.55 + (h3 >>> 8 & 7) * 0.05, 3.4);
                }
            }
            case 3 -> {   // cicatrice en diagonale sur la joue, avec ses points de suture
                int scar = Palette.mix(tone, 0xFFE89A9A, 0.6);
                l.line(5.5, 21.0, 9.0, 29.0, 1.0, scar, 0.9);
                l.line(5.5, 21.0, 9.0, 29.0, 0.4, Palette.mix(scar, 0xFFFFFFFF, 0.5), 0.8);
                for (int i = 0; i < 4; i++) {
                    double t = 0.15 + i * 0.22, px = 5.5 + 3.5 * t, py = 21 + 8 * t;
                    l.line(px - 1.3, py - 0.5, px + 1.3, py + 0.5, 0.5, Palette.shade(scar, 0.8), 0.8);
                }
            }
            case 4 -> l.soft(22.0, 30.0, 1.4, 1.4, dark, 0.95, 3.4);
            case 5 -> {   // cicatrice qui coupe le sourcil
                int scar = Palette.mix(tone, 0xFFFFFFFF, 0.5);
                l.line(21.0, 8.0, 25.5, 14.5, 1.1, scar, 0.85);
                l.line(21.0, 8.0, 25.5, 14.5, 0.4, Palette.shade(scar, 1.1), 0.7);
            }
            case 6 -> {
                l.soft(5.0, 21.0, 1.2, 1.2, dark, 0.9, 3.4);
                l.soft(26.5, 19.0, 1.0, 1.0, dark, 0.9, 3.4);
                l.soft(18.0, 30.5, 1.1, 1.1, dark, 0.9, 3.4);
                l.soft(9.0, 30.0, 0.9, 0.9, dark, 0.85, 3.4);
            }
            default -> { }
        }
    }

    private static int hash(int i, int salt) {
        int h = i * 73856093 ^ salt * 19349663;
        return (h ^ (h >>> 13)) * 1274126177 >>> 4;
    }

    // ------------------------------------------------------------------ un calque 32 × 32

    /** Une image sur laquelle on pose des taches douces et des traits fins (mélange alpha). */
    private static final class Layer {
        final int[] px = new int[SIZE * SIZE];

        /** Une tache douce : ellipse dont le bord s'estompe ; {@code sharp} grand = bord plus net. */
        void soft(double cx, double cy, double rx, double ry, int color, double alpha) { soft(cx, cy, rx, ry, color, alpha, 1.8); }

        void soft(double cx, double cy, double rx, double ry, int color, double alpha, double sharp) {
            int x0 = (int) Math.floor(cx - rx - 1), x1 = (int) Math.ceil(cx + rx + 1);
            int y0 = (int) Math.floor(cy - ry - 1), y1 = (int) Math.ceil(cy + ry + 1);
            for (int y = y0; y <= y1; y++) {
                for (int x = x0; x <= x1; x++) {
                    double dx = (x + 0.5 - cx) / rx, dy = (y + 0.5 - cy) / ry;
                    double d2 = dx * dx + dy * dy;
                    if (d2 >= 1.0) continue;
                    // le centre garde toute sa force, le bord s'efface
                    double a = alpha * Math.min(1.0, (1 - d2) * 1.7) * (0.55 + 0.45 * Math.exp(-d2 * sharp));
                    set(x, y, color, a);
                }
            }
        }

        /** Un trait fin d'une largeur donnée (en pixels de l'image). */
        void line(double x0, double y0, double x1, double y1, double width, int color, double alpha) {
            double minX = Math.min(x0, x1) - width - 1, maxX = Math.max(x0, x1) + width + 1;
            double minY = Math.min(y0, y1) - width - 1, maxY = Math.max(y0, y1) + width + 1;
            double vx = x1 - x0, vy = y1 - y0, len2 = vx * vx + vy * vy;
            for (int y = (int) Math.floor(minY); y <= maxY; y++) {
                for (int x = (int) Math.floor(minX); x <= maxX; x++) {
                    double px = x + 0.5 - x0, py = y + 0.5 - y0;
                    double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, (px * vx + py * vy) / len2));
                    double dx = px - t * vx, dy = py - t * vy;
                    double d = Math.sqrt(dx * dx + dy * dy);
                    double edge = width / 2 + 0.5 - d;   // 1 au cœur du trait, 0 à un demi-pixel du bord
                    if (edge <= 0) continue;
                    set(x, y, color, alpha * Math.min(1, edge));
                }
            }
        }

        void set(int x, int y, int color, double alpha) {
            if (x < 0 || y < 0 || x >= SIZE || y >= SIZE || alpha <= 0.01) return;
            int a = (int) Math.round(Math.min(1, alpha) * 255);
            px[y * SIZE + x] = HiResTattoo.over(px[y * SIZE + x], (a << 24) | (color & 0x00FFFFFF));
        }
    }
}
