package fr.minenorth.identite.skin;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.document.Gender;

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

    /** Fiche vide : tout est transparent. */
    private static final class Canvas {
        final int[] px = new int[SIZE * SIZE];

        void set(int x, int y, int argb) {
            if (x >= 0 && y >= 0 && x < SIZE && y < SIZE) px[y * SIZE + x] = argb;
        }

        int get(int x, int y) { return px[y * SIZE + x]; }

        void fill(int[] r, int argb) {
            for (int y = r[1]; y < r[1] + r[3]; y++) for (int x = r[0]; x < r[0] + r[2]; x++) set(x, y, argb);
        }

        /** Pixel relatif à un rectangle de face, ignoré s'il sort du rectangle. */
        void rel(int[] r, int dx, int dy, int argb) {
            if (dx >= 0 && dy >= 0 && dx < r[2] && dy < r[3]) set(r[0] + dx, r[1] + dy, argb);
        }

        int relGet(int[] r, int dx, int dy) { return get(r[0] + dx, r[1] + dy); }

        void shadeRel(int[] r, int dx, int dy, double factor) {
            if (dx >= 0 && dy >= 0 && dx < r[2] && dy < r[3]) set(r[0] + dx, r[1] + dy, Palette.shade(relGet(r, dx, dy), factor));
        }
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

        paintBuild(c, body[0], tone);
        paintMarks(c, skin[1], tone);
        paintFace(c, face, makeup, tone);
        paintMakeup(c, makeup);
        paintFacialHair(c, beard, tone);
        paintTattoos(c, tattoos, rArm, lArm);
        paintUnderwear(c, gender, rArm, lArm);
        if (gender == Gender.FEMALE) paintChest(c);
        paintHair(c, hair);
        return c.px;
    }

    // ------------------------------------------------------------------ corps

    private static void paintBuild(Canvas c, int build, int tone) {
        int[] t = BODY.front();
        if (build == 1) {
            for (int dy = 4; dy <= 8; dy++) { c.shadeRel(t, 3, dy, 0.9); c.shadeRel(t, 4, dy, 0.9); }
            for (int dx : new int[]{1, 2, 5, 6}) c.shadeRel(t, dx, 6, 0.9);
        } else if (build == 2) {
            for (int dx = 1; dx <= 6; dx++) { c.shadeRel(t, dx, 7, 0.93); c.shadeRel(t, dx, 8, 0.93); }
            for (int dx = 2; dx <= 5; dx++) c.shadeRel(t, dx, 6, 1.04);
        }
    }

    private static void paintChest(Canvas c) {
        int[] t = BODY.front();
        for (int dx : new int[]{1, 2, 5, 6}) {
            c.shadeRel(t, dx, 2, 1.06);
            c.shadeRel(t, dx, 6, 0.88);
        }
    }

    private static void paintMarks(Canvas c, int marks, int tone) {
        int[] f = HEAD.front();
        int dark = Palette.shade(tone, 0.7);
        switch (marks) {
            case 1 -> c.rel(f, 6, 5, dark);
            case 2 -> {
                int freckle = Palette.shade(tone, 0.85);
                for (int[] p : new int[][]{{2, 5}, {5, 5}, {1, 6}, {6, 6}}) c.rel(f, p[0], p[1], freckle);
            }
            case 3 -> {
                int scar = Palette.shade(tone, 0.78);
                for (int[] p : new int[][]{{1, 5}, {1, 6}, {2, 7}}) c.rel(f, p[0], p[1], scar);
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------ visage

    private static final String[][] EYES = {
            {"WI"}, {"II"}, {"WI", "WI"}, {"LL", "WI"}, {"IW"}, {" I", "WI"}};

    private static void paintFace(Canvas c, int[] face, int[] makeup, int tone) {
        int[] f = HEAD.front();
        int iris = Palette.eyeColor(face[1]);
        String[] rows = EYES[face[0]];
        for (int r = 0; r < rows.length; r++) {
            int dy = 4 - (rows.length - 1 - r);
            for (int i = 0; i < 2; i++) {
                int color = switch (rows[r].charAt(i)) {
                    case 'W' -> Palette.WHITE;
                    case 'I' -> iris;
                    case 'L' -> Palette.shade(tone, 0.45);
                    default -> -1;
                };
                if (color == -1) continue;
                c.rel(f, 1 + i, dy, color);
                c.rel(f, 6 - i, dy, color);
            }
        }
        int nose = Palette.shade(tone, 0.88);
        switch (face[2]) {
            case 0 -> c.rel(f, 4, 5, nose);
            case 1 -> { c.rel(f, 3, 5, nose); c.rel(f, 4, 5, nose); }
            default -> { c.rel(f, 3, 5, nose); c.rel(f, 4, 5, nose); c.rel(f, 3, 6, nose); }
        }
        int mouth = Palette.shade(tone, 0.78);
        for (int[] p : mouthPixels(face[3])) c.rel(f, p[0], p[1], mouth);
    }

    private static int[][] mouthPixels(int style) {
        return switch (style) {
            case 1 -> new int[][]{{2, 6}, {3, 7}, {4, 7}, {5, 6}};
            case 2 -> new int[][]{{3, 6}, {4, 6}, {3, 7}, {4, 7}};
            case 3 -> new int[][]{{3, 6}};
            default -> new int[][]{{3, 6}, {4, 6}};
        };
    }

    private static void paintMakeup(Canvas c, int[] makeup) {
        int[] f = HEAD.front();
        int lips = makeup[0];
        if (lips > 0) {
            int color = Palette.lipColor(makeup[1]);
            int[][] pixels = lips == 2 ? mouthPixels(2) : mouthPixels(0);
            for (int[] p : pixels) c.rel(f, p[0], p[1], color);
            if (lips == 3) c.rel(f, 3, 6, Palette.shade(color, 1.35));
        }
        int shadow = makeup[2];
        if (shadow > 0) {
            int color = Palette.eyeColor(makeup[3]);
            int[] xs = {1, 2, 5, 6};
            for (int dx : xs) {
                c.rel(f, dx, 3, shadow == 3 ? Palette.shade(color, 0.5) : color);
                if (shadow == 2) c.rel(f, dx, 2, Palette.mix(c.relGet(f, dx, 2), color, 0.5));
            }
        }
    }

    private static void paintFacialHair(Canvas c, int[] beard, int tone) {
        int style = beard[0];
        if (style == 0) return;
        int[] f = HEAD.front();
        int hair = Palette.hairColor(beard[1]);
        int stubble = Palette.mix(tone, hair, 0.5);
        switch (style) {
            case 1 -> {
                for (int dx = 2; dx <= 5; dx++) c.rel(f, dx, 7, stubble);
                c.rel(f, 1, 6, stubble);
                c.rel(f, 6, 6, stubble);
            }
            case 2 -> {
                for (int[] p : new int[][]{{2, 5}, {5, 5}, {1, 6}, {2, 6}, {5, 6}, {6, 6}}) c.rel(f, p[0], p[1], hair);
            }
            case 3 -> {
                c.rel(f, 3, 7, hair);
                c.rel(f, 4, 7, hair);
            }
            default -> {
                for (int dx = 1; dx <= 6; dx++) c.rel(f, dx, 7, hair);
                for (int[] p : new int[][]{{1, 6}, {2, 6}, {5, 6}, {6, 6}, {0, 5}, {7, 5}, {0, 6}, {7, 6}, {0, 7}, {7, 7}}) {
                    c.rel(f, p[0], p[1], hair);
                }
            }
        }
    }

    // ------------------------------------------------------------------ cheveux (couche extérieure)

    /** Par coupe : rangées de frange, de côtés, de nuque, et de dos prolongé sur le torse. */
    private static final int[][] HAIR_STYLES = {
            {0, 0, 0, 0},
            {1, 2, 2, 0}, {2, 3, 4, 0}, {2, 4, 6, 0}, {3, 5, 8, 0}, {2, 4, 5, 0}, {3, 6, 8, 0},
            {3, 8, 8, 4}, {4, 8, 8, 8}, {3, 8, 8, 3}, {2, 5, 6, 2}, {3, 8, 8, 6}};

    private static void paintHair(Canvas c, int[] hair) {
        if (hair[0] == 0) return;
        int[] s = HAIR_STYLES[hair[0]];
        int color = Palette.hairColor(hair[1]);
        paintHairRect(c, HEAD_O.top(), 8, color);
        paintHairRect(c, HEAD_O.front(), s[0], color);
        paintHairRect(c, HEAD_O.right(), s[1], color);
        paintHairRect(c, HEAD_O.left(), s[1], color);
        paintHairRect(c, HEAD_O.back(), s[2], color);
        if (s[3] > 0) paintHairRect(c, BODY_O.back(), s[3], color);
    }

    private static void paintHairRect(Canvas c, int[] r, int rows, int color) {
        for (int dy = 0; dy < Math.min(rows, r[3]); dy++) {
            for (int dx = 0; dx < r[2]; dx++) {
                c.rel(r, dx, dy, ((dx + dy) & 1) == 0 ? color : Palette.shade(color, 0.94));
            }
        }
    }

    // ------------------------------------------------------------------ tatouages

    private static final String[][] MOTIFS = {
            null,
            {"..#.", ".###", "..#.", "...."},
            {"####", "....", "####", "...."},
            {".#..", "#.#.", ".#..", "...."},
            {"#.#.", "....", "#.#.", "...."},
            {"#...", "#...", "###.", "...."},
            {"#...", ".#..", "..#.", "...#"}};

    private static void paintTattoos(Canvas c, int[] t, Box rArm, Box lArm) {
        tattoo(c, HEAD.front(), t[0], 0, 4);
        tattoo(c, BODY.front(), t[1], 2, 2);
        tattoo(c, BODY.back(), t[2], 2, 2);
        tattoo(c, rArm.front(), t[3], 0, 3);
        tattoo(c, lArm.front(), t[4], 0, 3);
        tattoo(c, R_LEG.front(), t[5], 0, 2);
        tattoo(c, L_LEG.front(), t[5], 0, 2);
    }

    private static void tattoo(Canvas c, int[] rect, int motif, int ax, int ay) {
        if (motif <= 0) return;
        String[] rows = MOTIFS[motif];
        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                if (rows[y].charAt(x) == '#') c.rel(rect, ax + x, ay + y, Palette.ink(motif));
            }
        }
    }

    // ------------------------------------------------------------------ sous-vêtements

    private static void paintUnderwear(Canvas c, Gender gender, Box rArm, Box lArm) {
        for (int[] side : BODY.sides()) rows(c, side, 9, 11, Palette.UNDERWEAR);
        for (Box leg : new Box[]{R_LEG, L_LEG}) {
            for (int[] side : leg.sides()) rows(c, side, 0, 2, Palette.UNDERWEAR);
        }
        if (gender == Gender.FEMALE) {
            for (int[] side : BODY.sides()) rows(c, side, 3, 5, Palette.UNDERWEAR);
        }
    }

    private static void rows(Canvas c, int[] rect, int from, int to, int argb) {
        for (int dy = from; dy <= to; dy++) for (int dx = 0; dx < rect[2]; dx++) c.rel(rect, dx, dy, argb);
    }
}
