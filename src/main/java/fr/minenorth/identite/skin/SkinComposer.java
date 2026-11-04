package fr.minenorth.identite.skin;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.TattooZone;
import fr.minenorth.identite.clothing.Clothing;
import fr.minenorth.identite.clothing.ClothingSlot;
import fr.minenorth.identite.clothing.Outfit;
import fr.minenorth.identite.document.Gender;
import fr.minenorth.identite.drawing.DrawingKind;
import fr.minenorth.identite.drawing.Drawings;

/**
 * Assemble le skin 64x64 (ARGB, ligne par ligne) à partir d'une fiche de choix. Pur Java, déterministe :
 * deux clients qui reçoivent la même fiche obtiennent exactement la même image.
 */
public final class SkinComposer {
    private SkinComposer() {}

    public static final int SIZE = 64;

    /** Boîte du format de skin 1.8+ : coordonnées de dépliage (u, v) et dimensions largeur/hauteur/profondeur. */
    private record Box(int u, int v, int w, int h, int d) {
        int[] top() { return new int[]{u + d, v, w, d}; }
        int[] bottom() { return new int[]{u + d + w, v, w, d}; }
        int[] right() { return new int[]{u, v + d, d, h}; }
        int[] front() { return new int[]{u + d, v + d, w, h}; }
        int[] left() { return new int[]{u + d + w, v + d, d, h}; }
        int[] back() { return new int[]{u + 2 * d + w, v + d, w, h}; }
        int[][] faces() { return new int[][]{top(), bottom(), right(), front(), left(), back()}; }
        int[][] sides() { return new int[][]{right(), front(), left(), back()}; }
    }

    private static final Box HEAD = new Box(0, 0, 8, 8, 8);
    private static final Box HEAD_O = new Box(32, 0, 8, 8, 8);
    private static final Box BODY = new Box(16, 16, 8, 12, 4);
    private static final Box BODY_O = new Box(16, 32, 8, 12, 4);
    private static final Box R_LEG = new Box(0, 16, 4, 12, 4);
    private static final Box L_LEG = new Box(16, 48, 4, 12, 4);

    /** Toile vide : tout est transparent. */
    private static final class Canvas {
        final int[] px = new int[SIZE * SIZE];

        void set(int x, int y, int argb) {
            if (x >= 0 && y >= 0 && x < SIZE && y < SIZE) px[y * SIZE + x] = argb;
        }

        int get(int x, int y) { return px[y * SIZE + x]; }

        void fill(int[] r, int argb) {
            for (int y = r[1]; y < r[1] + r[3]; y++) for (int x = r[0]; x < r[0] + r[2]; x++) set(x, y, argb);
        }

        boolean inside(int[] r, int dx, int dy) { return dx >= 0 && dy >= 0 && dx < r[2] && dy < r[3]; }

        /** Pixel relatif à un rectangle de face, ignoré s'il sort du rectangle. */
        void rel(int[] r, int dx, int dy, int argb) {
            if (inside(r, dx, dy)) set(r[0] + dx, r[1] + dy, argb);
        }

        int relGet(int[] r, int dx, int dy) { return get(r[0] + dx, r[1] + dy); }

        void shadeRel(int[] r, int dx, int dy, double factor) {
            if (inside(r, dx, dy)) set(r[0] + dx, r[1] + dy, Palette.shade(relGet(r, dx, dy), factor));
        }

        void mixRel(int[] r, int dx, int dy, int color, double t) {
            if (inside(r, dx, dy)) set(r[0] + dx, r[1] + dy, Palette.mix(relGet(r, dx, dy), color, t));
        }
    }

    /** Bruit déterministe entre 0 et 1 (même résultat sur tous les clients). */
    private static double noise(int x, int y, int salt) {
        int h = x * 73856093 ^ y * 19349663 ^ salt * 83492791;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFF) / 65535.0;
    }

    public static int[] compose(Gender gender, Appearance a) {
        Canvas c = new Canvas();
        boolean slim = gender.slimArms();
        int armW = slim ? 3 : 4;
        Box rArm = new Box(40, 16, armW, 12, 4);
        Box lArm = new Box(32, 48, armW, 12, 4);

        int[] body = a.get(AppearanceSection.BODY);
        int[] face = a.get(AppearanceSection.FACE);
        int[] skin = a.get(AppearanceSection.SKIN);
        int[] hair = a.get(AppearanceSection.HAIR);
        int[] beard = a.get(AppearanceSection.FACIAL_HAIR);
        int[] makeup = a.get(AppearanceSection.MAKEUP);
        int[] tattoos = a.get(AppearanceSection.TATTOOS);

        int tone = Palette.skinTone(skin[0]);
        for (Box b : new Box[]{HEAD, BODY, rArm, lArm, R_LEG, L_LEG}) {
            for (int[] f : b.faces()) c.fill(f, tone);
        }

        paintBuild(c, body[0], rArm, lArm);
        if (gender == Gender.FEMALE) paintChest(c, body[0]);
        paintLowerBody(c, gender);
        paintMarks(c, skin[1], tone);
        paintFace(c, face, Palette.hairColor(hair[1]), tone);
        paintMakeup(c, makeup, tone);
        paintFacialHair(c, beard, tone);
        paintTattoos(c, tattoos, rArm, lArm);
        paintDrawings(c, a.drawings(), rArm, lArm);
        paintPiercings(c, a.get(AppearanceSection.PIERCINGS));
        paintOutfit(c, a.outfit());
        paintHair(c, hair);
        return c.px;
    }

    // ------------------------------------------------------------------ corps

    private static void paintBuild(Canvas c, int build, Box rArm, Box lArm) {
        int[] t = BODY.front();
        switch (build) {
            case 1 -> {   // athlétique : abdominaux et biceps
                for (int dy = 7; dy <= 9; dy++) { c.shadeRel(t, 3, dy, 0.9); c.shadeRel(t, 4, dy, 0.9); }
                for (int dy = 7; dy <= 9; dy += 2) { c.shadeRel(t, 2, dy, 0.94); c.shadeRel(t, 5, dy, 0.94); }
                for (Box arm : new Box[]{rArm, lArm}) { c.shadeRel(arm.front(), 1, 3, 0.9); c.shadeRel(arm.front(), 1, 4, 0.9); }
            }
            case 2 -> {   // ronde
                for (int dx = 1; dx <= 6; dx++) { c.shadeRel(t, dx, 7, 0.93); c.shadeRel(t, dx, 8, 0.93); }
                c.shadeRel(t, 0, 7, 0.9);
                c.shadeRel(t, 7, 7, 0.9);
            }
            case 3 -> {   // mince : silhouette plus étroite
                for (int dy = 2; dy <= 10; dy++) { c.shadeRel(t, 0, dy, 0.9); c.shadeRel(t, 7, dy, 0.9); }
            }
            case 4 -> {   // sablier : taille marquée, hanches pleines
                for (int dy = 6; dy <= 8; dy++) { c.shadeRel(t, 0, dy, 0.86); c.shadeRel(t, 7, dy, 0.86); c.shadeRel(t, 1, dy, 0.94); c.shadeRel(t, 6, dy, 0.94); }
                for (int dy = 9; dy <= 10; dy++) { c.shadeRel(t, 0, dy, 1.06); c.shadeRel(t, 7, dy, 1.06); }
            }
            case 5 -> {   // costaud : épaules et bras marqués
                for (int dy = 0; dy <= 3; dy++) { c.shadeRel(t, 0, dy, 1.07); c.shadeRel(t, 7, dy, 1.07); }
                for (Box arm : new Box[]{rArm, lArm}) for (int dy = 1; dy <= 5; dy++) c.shadeRel(arm.front(), 1, dy, 0.92);
            }
            default -> { }
        }
    }

    /**
     * Modelé doux de la poitrine pour Femme (reflet en haut, ombre dessous), sans détail anatomique : le corps reste une
     * peau unie pour que les futurs calques de vêtements puissent épouser la forme. Plus marqué avec Ronde et Sablier.
     */
    private static void paintChest(Canvas c, int build) {
        int[] t = BODY.front();
        boolean small = build == 3;
        boolean full = build == 2 || build == 4;
        for (int dx : new int[]{1, 2, 5, 6}) {
            c.shadeRel(t, dx, 2, 1.05);
            if (!small) c.shadeRel(t, dx, 3, 1.03);
            c.shadeRel(t, dx, small ? 4 : 5, 0.9);
            if (full) c.shadeRel(t, dx, 6, 0.95);
        }
        for (int dy = 4; dy <= 5; dy++) { c.shadeRel(t, 3, dy, 0.95); c.shadeRel(t, 4, dy, 0.95); }
    }

    /**
     * Modelé discret du bas du corps (hanches, nombril, genoux, mollets, chevilles, pieds, dos des jambes), différent
     * pour Homme et Femme. Uniquement des variations de la couleur de peau : aucun sous-vêtement, pour que les futurs
     * calques de vêtements restent propres.
     */
    private static void paintLowerBody(Canvas c, Gender gender) {
        boolean female = gender == Gender.FEMALE;
        int[] t = BODY.front();
        c.shadeRel(t, 3, 7, 0.82);
        for (int dy = 8; dy <= 11; dy++) {
            c.shadeRel(t, 0, dy, female ? 1.04 : 0.95);
            c.shadeRel(t, 7, dy, female ? 1.04 : 0.95);
        }
        for (int dx = 1; dx <= 6; dx++) c.shadeRel(t, dx, 11, 0.96);
        c.shadeRel(t, 1, 10, 0.94);
        c.shadeRel(t, 2, 11, 0.93);
        c.shadeRel(t, 5, 11, 0.93);
        c.shadeRel(t, 6, 10, 0.94);
        int[] back = BODY.back();
        c.shadeRel(back, 2, 8, 0.95);
        c.shadeRel(back, 5, 8, 0.95);
        legDetail(c, R_LEG, female, true);
        legDetail(c, L_LEG, female, false);
    }

    private static void legDetail(Canvas c, Box leg, boolean female, boolean right) {
        int[] f = leg.front();
        int[] back = leg.back();
        int inner = right ? 3 : 0;
        int outer = 3 - inner;
        int glow = right ? 2 : 1;
        for (int dy = 1; dy <= 5; dy++) {
            c.shadeRel(f, glow, dy, female ? 1.05 : 1.03);
            c.shadeRel(f, outer, dy, female ? 0.97 : 0.93);
        }
        for (int dx = 0; dx < 4; dx++) {
            c.shadeRel(f, dx, 6, 0.93);
            c.shadeRel(f, dx, 7, 0.97);
        }
        c.shadeRel(f, 1, 6, 0.9);
        c.shadeRel(f, 2, 6, 0.9);
        for (int dy = 8; dy <= 9; dy++) {
            c.shadeRel(f, outer, dy, female ? 0.97 : 0.92);
            c.shadeRel(f, inner, dy, 0.98);
        }
        c.shadeRel(f, 0, 10, female ? 0.9 : 0.94);
        c.shadeRel(f, 3, 10, female ? 0.9 : 0.94);
        for (int dx = 0; dx < 4; dx++) c.shadeRel(f, dx, 11, dx % 2 == 0 ? 0.9 : 0.84);
        for (int dx = 0; dx < 4; dx++) {
            c.shadeRel(back, dx, 1, 0.92);
            c.shadeRel(back, dx, 6, 0.9);
            c.shadeRel(back, dx, 7, 0.96);
        }
        c.shadeRel(back, 1, 4, female ? 1.04 : 1.02);
        c.shadeRel(back, 2, 4, female ? 1.04 : 1.02);
        for (int dy = 8; dy <= 9; dy++) c.shadeRel(back, female ? 1 : 2, dy, female ? 1.02 : 1.04);
        for (int[] side : new int[][]{leg.left(), leg.right()}) {
            for (int dx = 0; dx < side[2]; dx++) {
                c.shadeRel(side, dx, 6, 0.93);
                c.shadeRel(side, dx, 10, 0.94);
                c.shadeRel(side, dx, 11, 0.9);
            }
        }
    }

    private static void paintMarks(Canvas c, int marks, int tone) {
        int[] f = HEAD.front();
        int dark = Palette.shade(tone, 0.7);
        switch (marks) {
            case 1 -> c.rel(f, 6, 5, dark);
            case 2 -> {
                int freckle = Palette.shade(tone, 0.85);
                for (int[] p : new int[][]{{2, 5}, {5, 5}, {1, 6}, {6, 6}, {2, 6}, {5, 4}}) c.rel(f, p[0], p[1], freckle);
            }
            case 3 -> {
                int scar = Palette.shade(tone, 0.78);
                for (int[] p : new int[][]{{1, 5}, {1, 6}, {2, 7}}) c.rel(f, p[0], p[1], scar);
            }
            case 4 -> c.rel(f, 5, 7, dark);
            case 5 -> {
                int scar = Palette.shade(tone, 0.8);
                for (int[] p : new int[][]{{5, 2}, {5, 3}, {6, 3}}) c.rel(f, p[0], p[1], scar);
            }
            case 6 -> {
                for (int[] p : new int[][]{{1, 5}, {6, 4}, {4, 7}, {2, 7}}) c.rel(f, p[0], p[1], Palette.shade(tone, 0.72));
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------ visage

    /** Lignes de haut en bas se terminant sur la ligne des yeux ; colonne 0 = coin extérieur. */
    private static final String[][] EYES = {
            {"WI"}, {"II"}, {"WI", "WI"}, {"LL", "WI"}, {"IW"}, {" I", "WI"},
            {"L ", "WI"}, {"LL", "IW"}, {"L ", "II"}, {" L", "WI"}};

    private static void paintFace(Canvas c, int[] face, int hairColor, int tone) {
        int[] f = HEAD.front();
        int brow = Palette.mix(Palette.shade(hairColor, 0.85), tone, 0.15);
        switch (face[2]) {
            case 0 -> { for (int dx : new int[]{1, 2, 5, 6}) c.rel(f, dx, 3, brow); }
            case 1 -> {
                for (int dx : new int[]{1, 2, 5, 6}) { c.rel(f, dx, 3, brow); c.rel(f, dx, 2, Palette.mix(tone, brow, 0.7)); }
            }
            case 2 -> { c.rel(f, 1, 3, brow); c.rel(f, 2, 2, brow); c.rel(f, 5, 2, brow); c.rel(f, 6, 3, brow); }
            case 3 -> { for (int dx : new int[]{1, 2, 5, 6}) c.rel(f, dx, 3, Palette.mix(tone, brow, 0.75)); }
            case 4 -> { c.rel(f, 1, 2, brow); c.rel(f, 2, 3, brow); c.rel(f, 5, 3, brow); c.rel(f, 6, 2, brow); }
            default -> { }
        }
        int iris = Palette.eyeColor(face[1]);
        String[] rows = EYES[face[0]];
        for (int r = 0; r < rows.length; r++) {
            int dy = 4 - (rows.length - 1 - r);
            for (int i = 0; i < 2; i++) {
                int color = switch (rows[r].charAt(i)) {
                    case 'W' -> Palette.WHITE;
                    case 'I' -> iris;
                    case 'L' -> Palette.shade(tone, 0.4);
                    default -> -1;
                };
                if (color == -1) continue;
                c.rel(f, 1 + i, dy, color);
                c.rel(f, 6 - i, dy, color);
            }
        }
        int nose = Palette.shade(tone, 0.88);
        switch (face[3]) {
            case 0 -> c.rel(f, 4, 5, nose);
            case 1 -> { c.rel(f, 3, 5, nose); c.rel(f, 4, 5, nose); }
            case 2 -> { for (int dx = 2; dx <= 5; dx++) c.rel(f, dx, 5, nose); }
            case 3 -> { c.rel(f, 3, 5, Palette.shade(tone, 0.8)); c.rel(f, 4, 5, Palette.shade(tone, 1.08)); }
            default -> { c.rel(f, 4, 4, Palette.shade(tone, 0.93)); c.rel(f, 4, 5, nose); c.rel(f, 3, 5, nose); }
        }
        int mouth = Palette.shade(tone, 0.78);
        for (int[] p : mouthPixels(face[4])) c.rel(f, p[0], p[1], mouth);
    }

    private static int[][] mouthPixels(int style) {
        return switch (style) {
            case 1 -> new int[][]{{2, 6}, {3, 7}, {4, 7}, {5, 6}};
            case 2 -> new int[][]{{3, 6}, {4, 6}, {3, 7}, {4, 7}};
            case 3 -> new int[][]{{3, 6}};
            case 4 -> new int[][]{{3, 6}, {4, 6}, {3, 7}};
            case 5 -> new int[][]{{2, 6}, {3, 6}, {4, 6}, {5, 6}};
            default -> new int[][]{{3, 6}, {4, 6}};
        };
    }

    private static void paintMakeup(Canvas c, int[] makeup, int tone) {
        int[] f = HEAD.front();
        int blush = makeup[4];
        if (blush > 0) {
            int color = Palette.lipColor(makeup[5]);
            double t = new double[]{0, 0.2, 0.28, 0.34, 0.45}[blush];
            int[][] cheeks = switch (blush) {
                case 1 -> new int[][]{{1, 5}, {6, 5}};
                case 2 -> new int[][]{{1, 5}, {2, 5}, {5, 5}, {6, 5}};
                case 3 -> new int[][]{{1, 5}, {2, 5}, {5, 5}, {6, 5}, {1, 6}, {6, 6}};
                default -> new int[][]{{1, 5}, {2, 5}, {5, 5}, {6, 5}, {1, 6}, {2, 6}, {5, 6}, {6, 6}};
            };
            for (int[] p : cheeks) c.mixRel(f, p[0], p[1], color, t);
        }
        int shadow = makeup[2];
        if (shadow > 0) {
            int color = Palette.eyeColor(makeup[3]);
            for (int dx : new int[]{1, 2, 5, 6}) {
                switch (shadow) {
                    case 1 -> c.mixRel(f, dx, 3, color, 0.45);
                    case 2 -> { c.mixRel(f, dx, 3, color, 0.8); c.mixRel(f, dx, 2, color, 0.4); }
                    case 3 -> c.rel(f, dx, 3, Palette.shade(color, 0.45));
                    case 4 -> c.rel(f, dx, 3, Palette.shade(color, 0.45));
                    default -> c.mixRel(f, dx, 3, color, 0.7);
                }
            }
            if (shadow == 4) { c.rel(f, 0, 3, Palette.shade(color, 0.45)); c.rel(f, 7, 3, Palette.shade(color, 0.45)); }
            if (shadow == 5) { c.rel(f, 2, 3, Palette.shade(color, 1.6)); c.rel(f, 5, 3, Palette.shade(color, 1.6)); }
        }
        int lips = makeup[0];
        if (lips > 0) {
            int color = Palette.lipColor(makeup[1]);
            int[][] pixels = lips == 2 || lips == 4 ? mouthPixels(2) : mouthPixels(0);
            for (int[] p : pixels) c.rel(f, p[0], p[1], lips == 5 ? Palette.shade(color, 0.6) : color);
            if (lips == 3) c.rel(f, 3, 6, Palette.shade(color, 1.35));
            if (lips == 4) { c.rel(f, 3, 7, Palette.mix(color, Palette.WHITE, 0.3)); c.rel(f, 4, 7, Palette.mix(color, Palette.WHITE, 0.3)); }
        }
    }

    // ------------------------------------------------------------------ pilosité

    private static final int[][][] BEARDS = {
            null,
            {{1, 7}, {2, 7}, {3, 7}, {4, 7}, {5, 7}, {6, 7}, {1, 6}, {6, 6}, {0, 5}, {7, 5}},            // 1 trois jours
            {{0, 7}, {1, 7}, {2, 7}, {3, 7}, {4, 7}, {5, 7}, {6, 7}, {7, 7}, {0, 6}, {1, 6}, {2, 6}, {5, 6}, {6, 6}, {7, 6}, {0, 5}, {1, 5}, {6, 5}, {7, 5}},   // 2 ombre
            {{2, 5}, {5, 5}, {1, 6}, {2, 6}, {5, 6}, {6, 6}},                                           // 3 moustache
            {{2, 5}, {5, 5}, {1, 6}, {2, 6}, {5, 6}, {6, 6}, {0, 7}, {7, 7}, {1, 7}, {6, 7}},          // 4 en croc
            {{2, 6}, {5, 6}, {3, 7}, {4, 7}, {3, 6}, {4, 6}},                                           // 5 bouc
            {{3, 7}, {4, 7}},                                                                           // 6 mouche
            {{0, 5}, {0, 6}, {0, 7}, {1, 7}, {2, 7}, {3, 7}, {4, 7}, {5, 7}, {6, 7}, {7, 7}, {7, 6}, {7, 5}},   // 7 collier
            {{1, 7}, {2, 7}, {3, 7}, {4, 7}, {5, 7}, {6, 7}, {1, 6}, {2, 6}, {5, 6}, {6, 6}, {0, 5}, {1, 5}, {6, 5}, {7, 5}},   // 8 courte
            {{0, 7}, {1, 7}, {2, 7}, {3, 7}, {4, 7}, {5, 7}, {6, 7}, {7, 7}, {0, 6}, {1, 6}, {2, 6}, {5, 6}, {6, 6}, {7, 6},
                    {0, 5}, {1, 5}, {6, 5}, {7, 5}, {0, 4}, {7, 4}, {2, 5}, {5, 5}},                   // 9 pleine
            {{0, 3}, {0, 4}, {0, 5}, {0, 6}, {0, 7}, {7, 3}, {7, 4}, {7, 5}, {7, 6}, {7, 7}, {1, 6}, {1, 7}, {6, 6}, {6, 7}},   // 10 favoris
            {{2, 5}, {5, 5}, {1, 6}, {2, 6}, {5, 6}, {6, 6}, {3, 7}, {4, 7}, {3, 6}, {4, 6}}};        // 11 Van Dyke

    /** Favoris et mâchoire sur les côtés de la tête (coordonnées du côté droit : colonne 7 touche le devant du visage). */
    private static int[][] beardSides(int style) {
        return switch (style) {
            case 1 -> rect(4, 5, 7, 7, new int[0][]);
            case 2 -> rect(5, 5, 7, 7, new int[][]{{4, 7}});
            case 7 -> rect(4, 5, 7, 7, new int[0][]);
            case 8 -> rect(5, 5, 7, 7, new int[][]{{4, 7}, {3, 7}});
            case 9 -> rect(2, 4, 7, 7, new int[][]{{6, 3}, {7, 3}});
            case 10 -> rect(6, 3, 7, 7, new int[][]{{4, 6}, {5, 6}, {4, 7}, {5, 7}});
            default -> new int[0][];
        };
    }

    private static int[][] rect(int x0, int y0, int x1, int y1, int[][] extra) {
        java.util.List<int[]> out = new java.util.ArrayList<>();
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) out.add(new int[]{x, y});
        out.addAll(java.util.Arrays.asList(extra));
        return out.toArray(new int[0][]);
    }

    private static void paintFacialHair(Canvas c, int[] beard, int tone) {
        int style = beard[0];
        if (style == 0) return;
        int[] f = HEAD.front();
        int hair = Palette.hairColor(beard[1]);
        for (int[] p : BEARDS[style]) {
            beardPixel(c, f, p[0], p[1], p[0], style, hair, 31 + style);
            beardOuter(c, HEAD_O.front(), p[0], p[1], p[0], style, hair, 31 + style);
        }
        int[][] sides = beardSides(style);
        int minX = 8;
        for (int[] p : sides) minX = Math.min(minX, p[0]);
        for (int[] p : sides) {
            // la limite arrière de la barbe est irrégulière : quelques poils manquent
            if (p[0] == minX && noise(p[0], p[1], 53) > 0.55) continue;
            beardPixel(c, HEAD.right(), p[0], p[1], p[0], style, hair, 61 + style);
            beardPixel(c, HEAD.left(), 7 - p[0], p[1], p[0], style, hair, 61 + style);
            beardOuter(c, HEAD_O.right(), p[0], p[1], p[0], style, hair, 61 + style);
            beardOuter(c, HEAD_O.left(), 7 - p[0], p[1], p[0], style, hair, 61 + style);
        }
    }

    /**
     * Un poil sur la tête : barbe de quelques jours = peau teintée par endroits ; sinon poil plein, plus clair en haut,
     * plus sombre en bas, et assombri pour servir de couche de fond sous le volume de la couche extérieure.
     */
    private static void beardPixel(Canvas c, int[] rect, int dx, int dy, int noiseX, int style, int hair, int salt) {
        double n = noise(noiseX, dy, salt);   // même bruit des deux côtés : la barbe est symétrique
        if (style == 1) {
            if (n > 0.3) c.mixRel(rect, dx, dy, hair, 0.5);
        } else if (style == 2) {
            c.mixRel(rect, dx, dy, hair, 0.25 + 0.15 * n);
        } else {
            double light = dy <= 5 ? 1.06 : 0.94;
            c.rel(rect, dx, dy, Palette.shade(hair, (0.85 + 0.25 * n) * light * 0.78));
        }
    }

    /**
     * Le volume : le même poil sur la couche extérieure de la tête, qui flotte un peu au-dessus. Plein pour une vraie barbe,
     * transparent pour une barbe de quelques jours (duvet). Même placement que la couche de fond.
     */
    private static void beardOuter(Canvas c, int[] rect, int dx, int dy, int noiseX, int style, int hair, int salt) {
        double n = noise(noiseX, dy, salt);
        if (style == 1 && n <= 0.3) return;
        int alpha = style == 1 ? 0xA0 : style == 2 ? 0x70 : 0xFF;
        double light = dy <= 5 ? 1.08 : 0.96;
        int color = Palette.shade(hair, (0.97 + 0.2 * noise(noiseX, dy, salt + 100)) * light);
        c.rel(rect, dx, dy, (color & 0x00FFFFFF) | (alpha << 24));
    }

    // ------------------------------------------------------------------ cheveux (couche extérieure)

    /**
     * Une coupe : colonnes de la face du dessus couvertes (masque), et pour chaque colonne la hauteur couverte sur le
     * front, les côtés (colonne 0 = nuque, 7 = tempe), la nuque et le dos prolongé sur le torse.
     */
    private record Hair(int topMask, int[] front, int[] side, int[] back, int[] extra) {}

    private static int[] fill(int n) { return new int[]{n, n, n, n, n, n, n, n}; }

    private static Hair hair(int mask, int[] f, int[] s, int[] b, int[] e) { return new Hair(mask, f, s, b, e); }

    private static final Hair[] HAIRS = {
            hair(0, fill(0), fill(0), fill(0), fill(0)),
            hair(255, fill(1), new int[]{2, 2, 2, 1, 1, 0, 0, 0}, fill(2), fill(0)),                                          // 1 rasée
            hair(255, fill(2), new int[]{4, 4, 3, 3, 2, 1, 1, 0}, fill(4), fill(0)),                                          // 2 brosse
            hair(255, new int[]{2, 3, 3, 3, 3, 3, 2, 2}, new int[]{4, 4, 4, 3, 2, 2, 3, 3}, fill(5), fill(0)),               // 3 court classique
            hair(255, new int[]{1, 2, 2, 2, 2, 2, 2, 1}, new int[]{3, 2, 2, 1, 1, 0, 0, 0}, new int[]{3, 2, 2, 2, 2, 2, 2, 3}, fill(0)),   // 4 dégradé
            hair(0b00011000, new int[]{0, 0, 0, 2, 2, 0, 0, 0}, fill(0), new int[]{0, 0, 0, 5, 5, 0, 0, 0}, fill(0)),        // 5 crête
            hair(255, new int[]{1, 1, 2, 2, 2, 2, 1, 1}, new int[]{4, 4, 4, 3, 3, 2, 1, 1}, fill(6), fill(0)),               // 6 gominé
            hair(255, new int[]{4, 4, 3, 3, 2, 2, 1, 1}, new int[]{5, 5, 4, 4, 3, 3, 2, 1}, fill(5), fill(0)),               // 7 frange latérale
            hair(255, new int[]{1, 2, 3, 3, 3, 3, 2, 1}, new int[]{3, 2, 2, 1, 1, 0, 0, 0}, fill(3), fill(0)),               // 8 banane
            hair(255, new int[]{2, 3, 2, 3, 2, 3, 2, 3}, new int[]{4, 5, 4, 5, 3, 3, 2, 2}, new int[]{5, 6, 5, 6, 5, 6, 5, 6}, fill(0)),    // 9 désordonnée
            hair(255, fill(3), new int[]{8, 8, 8, 7, 6, 5, 4, 3}, fill(8), fill(2)),                                          // 10 mi-longue
            hair(255, new int[]{3, 2, 3, 2, 3, 2, 3, 2}, new int[]{5, 5, 5, 4, 3, 3, 2, 2}, fill(6), fill(0)),               // 11 bouclée courte
            hair(255, fill(4), new int[]{8, 8, 8, 8, 7, 6, 5, 4}, fill(8), fill(1)),                                          // 12 afro
            hair(255, fill(2), new int[]{5, 5, 4, 3, 2, 1, 1, 0}, fill(6), new int[]{0, 0, 4, 6, 6, 4, 0, 0}),                // 13 queue basse (épaisse, effilée)
            hair(255, new int[]{3, 3, 3, 2, 2, 3, 3, 3}, new int[]{8, 8, 8, 8, 8, 7, 6, 5}, fill(8), new int[]{8, 9, 9, 10, 10, 9, 9, 8}),   // 14 longs lisses
            hair(255, fill(3), new int[]{7, 7, 7, 7, 6, 5, 4, 3}, fill(8), fill(0)),                                          // 15 carré
            hair(255, fill(3), new int[]{8, 8, 8, 7, 7, 6, 6, 5}, fill(8), fill(1)),                                          // 16 carré plongeant
            hair(255, new int[]{4, 3, 3, 2, 2, 3, 3, 4}, fill(8), fill(8), fill(11)),                                         // 17 longs raides
            hair(255, new int[]{3, 4, 3, 4, 3, 4, 3, 4}, fill(8), fill(8), new int[]{9, 11, 9, 11, 9, 11, 9, 11}),             // 18 longs ondulés
            hair(255, fill(2), new int[]{4, 4, 3, 2, 2, 1, 1, 0}, fill(5), new int[]{0, 0, 6, 8, 8, 6, 0, 0}),                // 19 queue haute (épaisse, effilée)
            hair(255, fill(2), new int[]{4, 4, 3, 2, 2, 1, 1, 0}, new int[]{5, 6, 7, 8, 8, 7, 6, 5}, fill(0)),                // 20 chignon
            hair(255, fill(2), fill(5), fill(6), new int[]{8, 7, 0, 0, 0, 0, 7, 8}),                                          // 21 couettes
            hair(255, new int[]{3, 3, 3, 2, 2, 3, 3, 3}, fill(6), fill(7), new int[]{0, 0, 5, 10, 10, 5, 0, 0}),              // 22 tresse (plus épaisse en haut)
            hair(255, fill(4), fill(8), fill(8), fill(9)),                                                                    // 23 frange droite
            hair(255, new int[]{3, 2, 2, 3, 3, 2, 2, 3}, new int[]{3, 3, 3, 2, 2, 2, 1, 1}, fill(3), fill(0)),                // 24 pixie
            hair(255, fill(3), new int[]{8, 8, 7, 7, 6, 5, 4, 3}, fill(8), new int[]{3, 4, 4, 5, 5, 4, 4, 3}),                // 25 mi-longs dégradés
            hair(255, new int[]{3, 4, 3, 4, 3, 4, 3, 4}, fill(8), fill(8), new int[]{10, 11, 10, 12, 12, 10, 11, 10}),         // 26 boucles longues
            hair(255, fill(4), new int[]{7, 7, 7, 7, 6, 5, 4, 3}, fill(7), fill(0))};                                         // 27 bob à frange

    /** Texture des mèches par coupe : 0 droites, 1 ondulées, 2 bouclées (par paquets), 3 lisses et brillantes. */
    private static final int[] HAIR_TEXTURE = {0,
            3, 0, 0, 3, 0, 3, 1, 3,                          // 1 à 8 : rasée, brosse, court, dégradé, crête, gominé, frange, banane
            1, 1, 2, 2, 0, 0,                                // 9 à 14 : désordonnée, mi-longue, bouclée, afro, queue basse, longs lisses
            0, 0, 0, 1, 0, 1, 0, 1, 0, 1, 1, 2, 3};          // 15 à 27 : carré … bob à frange

    private static void paintHair(Canvas c, int[] hair) {
        if (hair[0] == 0) return;
        int style = hair[0];
        Hair h = HAIRS[style];
        int base = Palette.hairColor(hair[1]);
        int[] top = HEAD_O.top();
        for (int dy = 0; dy < 8; dy++) {
            for (int dx = 0; dx < 8; dx++) {
                if (((h.topMask >> dx) & 1) == 0) continue;
                strand(c, top, dx, dy, base, style, true, 8);
                underlay(c, HEAD.top(), dx, dy, base);
            }
        }
        int[] front = HEAD_O.front();
        int[] right = HEAD_O.right();
        int[] left = HEAD_O.left();
        int[] back = HEAD_O.back();
        int longest = 0;
        for (int dx = 0; dx < 8; dx++) {
            column(c, front, null, dx, h.front[dx], base, style);                 // devant : jamais de couche dessous (le visage)
            column(c, right, HEAD.right(), dx, h.side[dx], base, style);
            column(c, left, HEAD.left(), dx, h.side[7 - dx], base, style);
            column(c, back, HEAD.back(), dx, h.back[dx], base, style);
            column(c, BODY_O.back(), null, dx, h.extra[dx], base, style);
            longest = Math.max(longest, h.extra[dx]);
        }
        if (longest >= 9) {
            // cheveux longs : des mèches retombent aussi devant les épaules
            for (int dx : new int[]{0, 1, 6, 7}) column(c, BODY_O.front(), null, dx, longest / 3, base, style);
        }
        accessories(c, style, base);
    }

    /**
     * Accessoires des coiffures attachées : élastiques de couleur (queues, couettes, tresse) et chignon rond en relief
     * (centre plus clair, bord plus sombre, une volute) sur l'arrière de la tête.
     */
    private static void accessories(Canvas c, int style, int base) {
        int tie = Palette.mix(base, 0xFFD04060, 0.78);
        int[] back = BODY_O.back();
        switch (style) {
            case 13, 19 -> {
                c.rel(back, 3, 0, tie);
                c.rel(back, 4, 0, Palette.shade(tie, 1.15));
                if (style == 19) {
                    c.rel(HEAD_O.back(), 3, 1, tie);
                    c.rel(HEAD_O.back(), 4, 1, Palette.shade(tie, 1.15));
                }
            }
            case 21 -> { for (int dx : new int[]{0, 1, 6, 7}) c.rel(back, dx, 0, dx % 2 == 0 ? tie : Palette.shade(tie, 1.15)); }
            case 22 -> {
                c.rel(back, 3, 9, tie);
                c.rel(back, 4, 9, Palette.shade(tie, 1.15));
            }
            case 20 -> bun(c, base);
            default -> { }
        }
    }

    /** Chignon : un rond de 4×4 sur l'arrière de la tête, coins arrondis, avec un reflet au centre et une volute. */
    private static void bun(Canvas c, int base) {
        int[] back = HEAD_O.back();
        int[][] shape = {{3, 0}, {4, 0}, {2, 1}, {3, 1}, {4, 1}, {5, 1}, {2, 2}, {3, 2}, {4, 2}, {5, 2}, {3, 3}, {4, 3}};
        for (int[] p : shape) {
            boolean centre = (p[0] == 3 || p[0] == 4) && (p[1] == 1 || p[1] == 2);
            c.rel(back, p[0], p[1], Palette.shade(base, centre ? 1.2 : 0.74));
        }
        c.rel(back, 4, 2, Palette.shade(base, 0.7));     // la volute du chignon
        int[] top = HEAD_O.top();
        c.rel(top, 3, 6, Palette.shade(base, 1.18));
        c.rel(top, 4, 6, Palette.shade(base, 1.18));
        c.rel(top, 3, 7, Palette.shade(base, 0.78));
        c.rel(top, 4, 7, Palette.shade(base, 0.78));
    }

    /** Couche sombre sous la couche extérieure : la tête ne laisse pas voir la peau à travers les cheveux, et ça crée du relief. */
    private static void underlay(Canvas c, int[] baseRect, int dx, int dy, int base) {
        c.rel(baseRect, dx, dy, Palette.shade(base, 0.5 + 0.12 * noise(baseRect[0] + dx, baseRect[1] + dy, 5)));
    }

    private static void column(Canvas c, int[] rect, int[] under, int dx, int rows, int base, int style) {
        for (int dy = 0; dy < Math.min(rows, rect[3]); dy++) {
            // pointe irrégulière : certaines mèches s'arrêtent un cran plus haut
            if (dy == rows - 1 && rows > 1 && noise(rect[0] + dx, rect[1] + dy, 77) > 0.7) continue;
            strand(c, rect, dx, dy, base, style, false, rows);
            if (under != null) underlay(c, under, dx, dy, base);
        }
    }

    /**
     * Un pixel de cheveu : grain, mèches selon la texture de la coupe, dôme et reflet sur le dessus, dessous des mèches
     * plus sombre.
     */
    private static void strand(Canvas c, int[] rect, int dx, int dy, int base, int style, boolean topFace, int rows) {
        int texture = HAIR_TEXTURE[style];
        double f = 0.86 + 0.24 * noise(rect[0] + dx, rect[1] + dy, 13 + style);
        switch (texture) {
            case 0 -> { if (dx % 3 == 0) f *= 0.92; }
            case 1 -> f *= ((dx + dy / 2) % 4) < 2 ? 0.9 : 1.07;
            case 2 -> f = 0.76 + 0.4 * noise(rect[0] + dx / 2, rect[1] + dy / 2, 91 + style);
            default -> {
                f = 0.93 + 0.12 * noise(rect[0] + dx, rect[1] + dy, 13 + style);
                if (dy == 1 || dy == 2) f *= 1.1;
            }
        }
        if (topFace) {
            f *= 1.1 - 0.035 * Math.hypot(dx - 3.5, dy - 3.5);
            if (texture != 3 && dy >= 2 && dy <= 3) f *= 1.06;
        } else if (dy == rows - 1) {
            f *= 0.8;
        }
        if (style == 22 && rect == BODY_O.back()) f *= (dy % 2 == 0) ? 1.0 : 0.82;
        c.rel(rect, dx, dy, Palette.shade(base, f));
    }

    // ------------------------------------------------------------------ dessins du joueur

    /**
     * Pose les dessins par-dessus ce que le catalogue a déjà dessiné : visage et barbe remplacent le pixel, les tatouages
     * se mélangent à la peau. Une case transparente ne change rien ; ce qui dépasse la zone (bras fins) est coupé.
     */
    private static void paintDrawings(Canvas c, Drawings drawings, Box rArm, Box lArm) {
        drawGrid(c, drawings, DrawingKind.FACE, HEAD.front(), false);
        drawGrid(c, drawings, DrawingKind.BEARD, HEAD.front(), false);
        drawBeardOuter(c, drawings);
        drawGrid(c, drawings, DrawingKind.TATTOO_TORSO, BODY.front(), true);
        drawGrid(c, drawings, DrawingKind.TATTOO_BACK, BODY.back(), true);
        drawGrid(c, drawings, DrawingKind.TATTOO_RIGHT_ARM, rArm.front(), true);
        drawGrid(c, drawings, DrawingKind.TATTOO_LEFT_ARM, lArm.front(), true);
        drawGrid(c, drawings, DrawingKind.TATTOO_LEGS, R_LEG.front(), true);
        drawGrid(c, drawings, DrawingKind.TATTOO_LEGS, L_LEG.front(), true);
    }

    /** Le volume d'une barbe dessinée : même case sur la couche extérieure de la tête, un peu éclaircie. */
    private static void drawBeardOuter(Canvas c, Drawings drawings) {
        byte[] grid = drawings.get(DrawingKind.BEARD);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int index = grid[y * 8 + x];
                if (index != 0) c.rel(HEAD_O.front(), x, y, Palette.shade(DrawingKind.BEARD.color(index), 1.04));
            }
        }
    }

    private static void drawGrid(Canvas c, Drawings drawings, DrawingKind kind, int[] rect, boolean blend) {
        byte[] grid = drawings.get(kind);
        for (int y = 0; y < kind.height(); y++) {
            for (int x = 0; x < kind.width(); x++) {
                int index = grid[y * kind.width() + x];
                if (index == 0 || !c.inside(rect, x, y)) continue;
                int color = kind.color(index);
                c.rel(rect, x, y, blend ? Palette.mix(c.relGet(rect, x, y), color, 0.88) : color);
            }
        }
    }

    // ------------------------------------------------------------------ piercings

    /**
     * Piercings : oreilles (des deux côtés de la tête), nez, sourcil et lèvre. Chaque bijou est posé sur la tête (plus
     * sombre) et sur la couche extérieure (métal clair), pour le relief. Ne touche jamais les yeux.
     */
    private static void paintPiercings(Canvas c, int[] p) {
        int metal = Palette.metal(p[4]);
        int[][] tones = {{metal}, {Palette.shade(metal, 1.25)}, {Palette.shade(metal, 0.6)}};
        for (int[] px : earPixels(p[0])) {
            jewel(c, HEAD.right(), HEAD_O.right(), px[0], px[1], tones[px[2]][0]);
            jewel(c, HEAD.left(), HEAD_O.left(), 7 - px[0], px[1], tones[px[2]][0]);
        }
        for (int[] px : nosePixels(p[1])) jewel(c, HEAD.front(), HEAD_O.front(), px[0], px[1], tones[px[2]][0]);
        for (int[] px : browPixels(p[2])) jewel(c, HEAD.front(), HEAD_O.front(), px[0], px[1], tones[px[2]][0]);
        for (int[] px : lipPixels(p[3])) jewel(c, HEAD.front(), HEAD_O.front(), px[0], px[1], tones[px[2]][0]);
    }

    private static void jewel(Canvas c, int[] baseRect, int[] outerRect, int dx, int dy, int color) {
        c.rel(baseRect, dx, dy, Palette.shade(color, 0.8));
        c.rel(outerRect, dx, dy, color);
    }

    /** Pixels {colonne, ligne, ton} : ton 0 = métal, 1 = reflet, 2 = ombre. Oreilles : coordonnées de la face droite. */
    private static int[][] earPixels(int style) {
        return switch (style) {
            case 1 -> new int[][]{{3, 5, 1}};
            case 2 -> new int[][]{{3, 5, 0}, {2, 6, 0}, {4, 6, 0}, {3, 7, 0}};
            case 3 -> new int[][]{{3, 5, 1}, {3, 6, 2}, {3, 7, 1}};
            case 4 -> new int[][]{{3, 5, 0}, {2, 6, 0}, {4, 6, 0}, {2, 7, 0}, {4, 7, 0}, {3, 7, 0}};
            default -> new int[0][];
        };
    }

    private static int[][] nosePixels(int style) {
        return switch (style) {
            case 1 -> new int[][]{{3, 5, 1}};
            case 2 -> new int[][]{{3, 5, 0}, {3, 6, 0}};
            case 3 -> new int[][]{{3, 5, 0}, {4, 5, 0}};
            default -> new int[0][];
        };
    }

    private static int[][] browPixels(int style) {
        return switch (style) {
            case 1 -> new int[][]{{2, 2, 0}, {2, 3, 2}};
            case 2 -> new int[][]{{1, 2, 1}, {2, 2, 1}};
            default -> new int[0][];
        };
    }

    private static int[][] lipPixels(int style) {
        return switch (style) {
            case 1 -> new int[][]{{3, 7, 1}};
            case 2 -> new int[][]{{3, 7, 0}, {4, 7, 2}};
            case 3 -> new int[][]{{2, 7, 1}, {5, 7, 1}};
            default -> new int[0][];
        };
    }

    // ------------------------------------------------------------------ vêtements (couche extérieure)

    private static final Box R_LEG_O = new Box(0, 32, 4, 12, 4);
    private static final Box L_LEG_O = new Box(0, 48, 4, 12, 4);

    private static void paintOutfit(Canvas c, Outfit outfit) {
        for (ClothingSlot slot : ClothingSlot.values()) {
            Clothing worn = outfit.get(slot);
            if (worn == null) continue;
            switch (worn) {
                case UNDERWEAR_MALE -> paintBoxers(c);
                case UNDERWEAR_FEMALE -> paintLingerie(c);
            }
        }
    }

    /** Caleçon : ceinture claire, tissu rayé en finesse, bas de jambe ourlé. */
    private static void paintBoxers(Canvas c) {
        int cloth = 0xFF222B45;
        int band = 0xFFC9CFE0;
        for (int[] side : BODY_O.sides()) {
            garmentRows(c, side, 9, 11, cloth, band, 9);
        }
        for (Box leg : new Box[]{R_LEG_O, L_LEG_O}) {
            for (int[] side : leg.sides()) {
                garmentRows(c, side, 0, 2, cloth, Palette.shade(cloth, 0.8), 2);
            }
        }
    }

    /** Haut et culotte : dentelle claire en bordure, tissu prune. */
    private static void paintLingerie(Canvas c) {
        int cloth = 0xFF3A3048;
        int lace = 0xFFC8B4D8;
        for (int[] side : BODY_O.sides()) {
            garmentRows(c, side, 3, 5, cloth, lace, 3);
            garmentRows(c, side, 10, 11, cloth, lace, 10);
        }
        for (Box leg : new Box[]{R_LEG_O, L_LEG_O}) {
            for (int[] side : leg.sides()) {
                garmentRows(c, side, 0, 1, cloth, Palette.shade(cloth, 0.8), 1);
            }
        }
    }

    /** Remplit les lignes de from à to ; la ligne trimRow reçoit la couleur de bordure, les autres un léger rayage. */
    private static void garmentRows(Canvas c, int[] rect, int from, int to, int cloth, int trim, int trimRow) {
        for (int dy = from; dy <= to; dy++) {
            for (int dx = 0; dx < rect[2]; dx++) {
                int color = dy == trimRow ? trim : (dx % 2 == 0 ? cloth : Palette.shade(cloth, 0.93));
                c.rel(rect, dx, dy, color);
            }
        }
    }

    // ------------------------------------------------------------------ tatouages

    /**
     * Les 12 motifs, dessinés en 8×8 : « # » contour, « o » couleur principale, « a » couleur secondaire, « . » rien.
     * Sur le torse et le dos le dessin est posé tel quel ; sur un bras, une jambe ou la joue il est réduit en gardant
     * les colonnes et lignes 1, 3, 4 et 6 (la réduction reste symétrique).
     */
    private static final String[][] DESIGNS = {
            {"...##...", "..#oo#..", "###oo###", "#oooooo#", ".#oooo#.", "..#oo#..", ".#o##o#.", ".##..##."},   // 1 étoile
            {".##..##.", "#oo##oo#", "#aooooo#", "#oooooo#", ".#oooo#.", "..#oo#..", "...##...", "........"},   // 2 cœur
            {"...##...", "..#oo#..", "###oo###", "#oooooo#", "###oo###", "..#oo#..", "..#oo#..", "..####.."},   // 3 croix
            {"..####..", "..#oo#..", "...oo...", ".oooooo.", "...oo...", "o..oo..o", "oo.oo.oo", ".oooooo."},   // 4 ancre
            {"..####..", ".#oooo#.", "#oo##oo#", "#o#oo#o#", ".#oooo#.", "..#aa#..", ".#a##a#.", "..#..#.."},   // 5 rose
            {"..####..", ".#oooo#.", "#oooooo#", "#o#oo#o#", "#oooooo#", ".##oo##.", "..#aa#..", "...##..."},   // 6 crâne
            {"#......#", "##....##", "#o#..#o#", "#oo##oo#", "#oooooo#", ".#oooo#.", "..#oo#..", "...##..."},   // 7 ailes
            {"...##...", "..#oo#..", ".#ooo#..", ".#oaoo#.", "#oaaaoo#", "#oaaaoo#", ".#oaoo#.", "..####.."},   // 8 flamme
            {"....##..", "...#oo#.", "..#oo#..", ".#oo#...", "#oooooo#", "...#oo#.", "..#oo#..", ".##o#..."},   // 9 éclair
            {"#..##..#", ".#.oo.#.", "..oooo..", "#oaaaao#", "#oaaaao#", "..oooo..", ".#.oo.#.", "#..##..#"},   // 10 soleil
            {"#..##..#", "##.##.##", "#oo##oo#", "#oooooo#", "#oaooao#", "#oooooo#", "########", "........"},   // 11 couronne
            {"#......#", "##....##", "#oo..oo#", ".#oooo#.", "..#oo#..", ".#oooo#.", "#oo..oo#", "##....##"}};  // 12 tribal

    /** Couleur principale et couleur secondaire de chaque motif (le contour est toujours un noir bleuté). */
    private static final int[][] DESIGN_COLORS = {
            {0xFF4A8AD0, 0xFF1A2A5A}, {0xFFD03030, 0xFFE87070}, {0xFF5A5A66, 0xFFE8E8F0}, {0xFF2A5AA0, 0xFF4A8AD0},
            {0xFFD03030, 0xFF2A6A3A}, {0xFFE8E8F0, 0xFF5A5A66}, {0xFF5A5A66, 0xFFE8E8F0}, {0xFFE08030, 0xFFE0C040},
            {0xFFE0C040, 0xFFE08030}, {0xFFE0C040, 0xFFE08030}, {0xFFE0C040, 0xFFD03030}, {0xFF1A2A5A, 0xFF14141E}};

    private static final int OUTLINE = 0xFF14141E;
    private static final int[] REDUCED = {1, 3, 4, 6};
    private static final int[] FULL = {0, 1, 2, 3, 4, 5, 6, 7};

    private static final int[] SIX = {0, 2, 3, 4, 5, 7};

    /** Petites marques du visage, 3×3 (« o »), posées sur la joue gauche : très discrètes, jamais sur les yeux ni la bouche. */
    private static final String[][] FACE_MARKS = {
            {".o.", "ooo", ".o."}, {"o.o", ".o.", "o.o"}, {"ooo", "...", "..."}, {"o.o", "o.o", "o.o"},
            {"oo.", ".oo", "..."}, {".o.", "ooo", "..."}, {"o.o", "ooo", ".o."}, {".o.", ".o.", "oo."},
            {"ooo", "o.o", "ooo"}, {"o..", ".o.", "..o"}, {"o.o", "...", "o.o"}, {"..o", ".oo", "oo."}};

    private static final int FACE_INK = 0xFF2A2A3A;

    /** Chaque zone a un motif et une taille : petit (4 cases), moyen (6) ou grand (8). Petit par défaut : très discret. */
    private static void paintTattoos(Canvas c, int[] t, Box rArm, Box lArm) {
        faceMark(c, t[TattooZone.FACE_NECK.motifSlot()]);
        sized(c, BODY.front(), t, TattooZone.TORSO, true);
        sized(c, BODY.back(), t, TattooZone.BACK, true);
        sized(c, rArm.front(), t, TattooZone.RIGHT_ARM, false);
        sized(c, lArm.front(), t, TattooZone.LEFT_ARM, false);
        sized(c, R_LEG.front(), t, TattooZone.LEGS, false);
        sized(c, L_LEG.front(), t, TattooZone.LEGS, false);
    }

    /** Torse et dos : figure réduite à 4×4 ou 6×6 au centre de la poitrine ; bras et jambes : 4 de large, 4, 6 ou 8 de haut. */
    private static void sized(Canvas c, int[] rect, int[] t, TattooZone zone, boolean wide) {
        int motif = t[zone.motifSlot()];
        if (motif <= 0) return;
        int size = t[zone.sizeSlot()];
        int[] cols = wide ? (size == 0 ? REDUCED : size == 1 ? SIX : FULL) : REDUCED;
        int[] rows = size == 0 ? REDUCED : size == 1 ? SIX : FULL;
        int ox = wide ? (8 - cols.length) / 2 : 0;
        int oy = size == 0 ? (wide ? 3 : 4) : size == 1 ? 3 : 2;
        design(c, rect, ox, oy, motif, cols, rows);
    }

    private static void faceMark(Canvas c, int motif) {
        if (motif <= 0) return;
        int[] face = HEAD.front();
        String[] art = FACE_MARKS[motif - 1];
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                if (art[y].charAt(x) == 'o') c.mixRel(face, x, 5 + y, FACE_INK, 0.7);
            }
        }
    }

    /** Pose le motif dans la zone ; colonnes et lignes du dessin d'origine à garder (réduction). Mélangé à la peau comme de l'encre. */
    private static void design(Canvas c, int[] rect, int ox, int oy, int motif, int[] cols, int[] rows) {
        if (motif <= 0) return;
        String[] art = DESIGNS[motif - 1];
        int[] colors = DESIGN_COLORS[motif - 1];
        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < cols.length; x++) {
                char ch = art[rows[y]].charAt(cols[x]);
                if (ch == '.' || !c.inside(rect, ox + x, oy + y)) continue;
                int color = ch == '#' ? OUTLINE : ch == 'o' ? colors[0] : colors[1];
                c.set(rect[0] + ox + x, rect[1] + oy + y, Palette.mix(c.relGet(rect, ox + x, oy + y), color, 0.9));
            }
        }
    }
}
