package fr.minenorth.identite.hair;

import java.util.ArrayList;
import java.util.List;

/**
 * Volume 3D des coiffures : de petits blocs de cheveux posés sur la tête ou le dos du joueur, par-dessus le skin peint.
 * Code pur (aucun rendu), les unités sont celles du modèle de joueur : 1 = 1 pixel de skin, y vers le bas, le devant
 * est du côté z négatif. La tête occupe x -4..4, y -8..0, z -4..4 (couche extérieure : 0,5 de plus) et le torse
 * x -4..4, y 0..12, z -2..2.
 */
public final class HairPieces {
    private HairPieces() {}

    public enum Part { HEAD, BODY }

    /** Teinte : cheveux, élastique (rouge), mèche plus sombre. */
    public static final int HAIR = 0, TIE = 1, SHADOW = 2;

    /**
     * Un bloc : pivot (px, py, pz) dans la partie, rotation autour de l'axe x en degrés (positive = vers l'arrière),
     * puis boîte (x, y, z) de taille (w, h, d) relative au pivot.
     */
    public record Piece(Part part, float px, float py, float pz, float rx,
                        float x, float y, float z, float w, float h, float d, int tone) {
        /** Boîte alignée, rotation ignorée (suffisant pour vérifier qu'on ne masque pas le visage). */
        public float minX() { return px + x; }
        public float maxX() { return px + x + w; }
        public float minY() { return py + y; }
        public float maxY() { return py + y + h; }
        public float minZ() { return pz + z; }
        public float maxZ() { return pz + z + d; }
    }

    /** Zone du visage qui doit rester dégagée (face avant de la tête, sous le front). */
    public static final float FACE_TOP = -6.4f, FACE_X = 4.0f, FRONT = -4.0f;

    /** Les cheveux collent à la tête (4,0) et la couche extérieure peinte (4,5) reste dessous. */
    private static final float H = 4.25f, TOP = -8.25f;

    /** Les coiffures qui ont du volume (les autres restent peintes sur le skin). */
    public static List<Piece> forStyle(int style) {
        List<Piece> out = new ArrayList<>();
        switch (style) {
            case 5 -> head(out, -1.1f, -12.4f, -4.2f, 2.2f, 3.9f, 8.4f, HAIR);
            case 8 -> {
                head(out, -3.6f, -11.0f, -5.8f, 7.2f, 2.5f, 4.8f, HAIR);
                cap(out, 0.8f);
            }
            case 9 -> {
                cap(out, 1.3f);
                head(out, -3.0f, -10.8f, -3.5f, 2.2f, 1.6f, 2.0f, HAIR);
                head(out, 1.0f, -10.6f, 0.5f, 2.4f, 1.5f, 2.4f, HAIR);
                head(out, -2.0f, -10.4f, 2.5f, 2.4f, 1.4f, 2.2f, SHADOW);
            }
            case 10 -> {
                cap(out, 1.0f);
                sides(out, 1.0f, -2.0f, -3.5f);
                back(out, 1.0f, -1.5f);
            }
            case 11 -> {
                cap(out, 1.8f);
                sides(out, 1.6f, -5.4f, -3.8f);
                back(out, 1.6f, -4.0f);
            }
            case 12 -> {
                cap(out, 3.0f);
                sides(out, 2.6f, -3.5f, -4.4f);
                back(out, 2.6f, -1.5f);
            }
            case 13 -> {
                cap(out, 0.8f);
                back(out, 0.8f, -3.0f);
                tail(out, 0f, -4.4f, 4.25f, -10f, 3.6f, 10f, 3.0f);
            }
            case 14 -> longHair(out, 1.2f, 9f, 1.0f);
            case 15 -> {
                cap(out, 1.1f);
                sides(out, 1.2f, -2.5f, -3.8f);
                back(out, 1.2f, -2.0f);
            }
            case 16 -> {
                cap(out, 1.1f);
                // plus long devant que derrière
                head(out, H, TOP, -4.4f, 1.2f, 7.7f, 4.0f, HAIR);
                head(out, -H - 1.2f, TOP, -4.4f, 1.2f, 7.7f, 4.0f, HAIR);
                head(out, H, TOP, -0.4f, 1.2f, 6.2f, 5.6f, HAIR);
                head(out, -H - 1.2f, TOP, -0.4f, 1.2f, 6.2f, 5.6f, HAIR);
                back(out, 1.2f, -2.0f);
            }
            case 17 -> longHair(out, 1.0f, 12f, 0.9f);
            case 18 -> {
                cap(out, 1.4f);
                sides(out, 1.8f, -2.0f, -3.6f);
                back(out, 1.6f, 0.0f);
                for (int i = 0; i < 4; i++) {
                    float w = (i % 2 == 0) ? 8.8f : 9.8f, t = (i % 2 == 0) ? 1.2f : 1.9f;
                    body(out, -w / 2f, 2.5f * i, 2.1f, w, 2.5f, t, i % 2 == 0 ? HAIR : SHADOW);
                }
            }
            case 19 -> {
                cap(out, 0.8f);
                head(out, -1.4f, -9.3f, 3.2f, 2.8f, 1.0f, 2.8f, TIE);
                out.add(new Piece(Part.HEAD, 0f, -9.0f, 3.8f, 35f, -1.9f, 0f, -1.5f, 3.8f, 4.5f, 3.2f, HAIR));
                out.add(new Piece(Part.HEAD, 0f, -5.4f, 6.4f, -14f, -1.8f, -0.4f, -1.5f, 3.6f, 11.0f, 3.0f, HAIR));
            }
            case 20 -> {
                cap(out, 0.8f);
                head(out, -2.7f, -12.8f, 0.8f, 5.4f, 4.4f, 5.4f, HAIR);
                head(out, -1.8f, -13.8f, 1.6f, 3.6f, 1.2f, 3.6f, SHADOW);
                head(out, -2.9f, -9.0f, 1.4f, 5.8f, 0.9f, 4.4f, TIE);
            }
            case 21 -> {
                cap(out, 0.8f);
                sides(out, 0.8f, -5.0f, -3.0f);
                for (int s : new int[]{-1, 1}) {
                    float px = s * 5.0f;
                    out.add(new Piece(Part.HEAD, px, -5.5f, 1.0f, 0f, -1.4f, -0.6f, -1.4f, 2.8f, 1.0f, 2.8f, TIE));
                    out.add(new Piece(Part.HEAD, px, -5.5f, 1.0f, 0f, -1.7f, 0.4f, -1.7f, 3.4f, 7.5f, 3.4f, HAIR));
                }
            }
            case 22 -> {
                cap(out, 0.8f);
                back(out, 0.8f, -3.0f);
                for (int i = 0; i < 5; i++) {
                    out.add(new Piece(Part.HEAD, 0f, -3.5f, 4.25f, -9f, -1.7f + (i % 2 == 0 ? 0f : 0.3f), 2.6f * i,
                            -0.2f, 3.4f, 2.8f, 2.6f, i % 2 == 0 ? HAIR : SHADOW));
                }
                out.add(new Piece(Part.HEAD, 0f, -3.5f, 4.25f, -9f, -1.5f, 13.0f, -0.2f, 3.0f, 1.0f, 2.6f, TIE));
            }
            case 23 -> {
                cap(out, 1.0f);
                sides(out, 1.0f, -3.5f, -3.6f);
                back(out, 1.0f, -2.0f);
                bangs(out);
            }
            case 24 -> {
                cap(out, 1.2f);
                sides(out, 0.8f, -5.5f, -3.0f);
                head(out, -4.5f, -9.4f, -5.3f, 9.0f, 1.9f, 0.8f, HAIR);
            }
            case 25 -> {
                cap(out, 1.2f);
                sides(out, 1.4f, -3.0f, -3.6f);
                back(out, 1.4f, -1.0f);
                body(out, -4.0f, 0f, 2.1f, 8.0f, 2.2f, 1.0f, SHADOW);
            }
            case 26 -> {
                cap(out, 2.0f);
                sides(out, 2.2f, -2.0f, -3.8f);
                back(out, 2.2f, 0.0f);
                for (int i = 0; i < 4; i++) {
                    float w = (i % 2 == 0) ? 9.4f : 10.6f, t = (i % 2 == 0) ? 1.8f : 2.6f;
                    body(out, -w / 2f, 2.6f * i, 2.1f, w, 2.6f, t, i % 2 == 0 ? HAIR : SHADOW);
                }
                body(out, -5.4f, -0.5f, -2.9f, 1.6f, 3.2f, 5.8f, HAIR);
                body(out, 3.8f, -0.5f, -2.9f, 1.6f, 3.2f, 5.8f, HAIR);
            }
            case 27 -> {
                cap(out, 1.1f);
                sides(out, 1.2f, -2.5f, -3.8f);
                back(out, 1.2f, -2.0f);
                bangs(out);
            }
            case 28 -> {
                cap(out, 0.8f);
                sides(out, 0.8f, -3.0f, -3.4f);
                back(out, 0.8f, -3.0f);
                for (int i = 0; i < 5; i++) {
                    body(out, 1.7f + (i % 2) * 0.2f, -1.0f + 2.6f * i, -4.6f, 3.0f, 2.8f, 2.6f, i % 2 == 0 ? HAIR : SHADOW);
                }
                body(out, 1.8f, 12.0f, -4.6f, 2.8f, 1.0f, 2.6f, TIE);
            }
            case 29 -> {
                cap(out, 1.0f);
                sides(out, 1.2f, -2.0f, -3.8f);
                back(out, 1.2f, 0.0f);
                body(out, -4.2f, 0f, 2.1f, 8.4f, 8f, 1.0f, HAIR);
                head(out, -2.0f, -11.4f, 1.0f, 4.0f, 3.2f, 4.0f, HAIR);
                head(out, -2.3f, -8.9f, 0.8f, 4.6f, 0.9f, 4.6f, TIE);
            }
            case 30 -> {
                cap(out, 0.8f);
                sides(out, 0.8f, -5.0f, -3.0f);
                back(out, 0.8f, -4.0f);
                for (int s : new int[]{-1, 1}) {
                    float cx = s * 3.8f;
                    head(out, cx - 2.2f, -12.4f, -1.4f, 4.4f, 3.9f, 4.4f, HAIR);
                    head(out, cx - 1.4f, -13.4f, -0.6f, 2.8f, 1.0f, 2.8f, SHADOW);
                    head(out, cx - 1.2f, -8.9f, -0.2f, 2.4f, 0.8f, 2.4f, TIE);
                }
            }
            case 31 -> {
                cap(out, 0.8f);
                sides(out, 0.8f, -4.0f, -3.4f);
                back(out, 0.8f, -1.5f);
                head(out, -2.6f, -3.2f, 4.25f, 5.2f, 5.0f, 4.4f, HAIR);
                head(out, -2.0f, -2.6f, 8.2f, 4.0f, 3.6f, 0.8f, SHADOW);
                head(out, -2.5f, -3.8f, 4.25f, 5.0f, 0.8f, 3.8f, TIE);
            }
            case 32 -> {
                cap(out, 1.2f);
                sides(out, 1.6f, -0.5f, -3.6f);
                back(out, 1.6f, 0.0f);
                body(out, -4.2f, 0f, 2.1f, 8.4f, 3.2f, 1.2f, HAIR);
                body(out, -4.0f, 3.0f, 2.1f, 8.0f, 1.4f, 0.8f, SHADOW);
            }
            case 33 -> {
                cap(out, 1.0f);
                sides(out, 1.0f, -1.5f, -3.8f);
                back(out, 1.0f, -2.0f);
                head(out, -4.7f, -9.4f, -5.0f, 9.4f, 1.8f, 1.0f, HAIR);
                head(out, -4.8f, -9.4f, -5.0f, 0.8f, 7.4f, 1.0f, HAIR);
                head(out, 4.0f, -9.4f, -5.0f, 0.8f, 7.4f, 1.0f, HAIR);
            }
            case 34 -> {
                cap(out, 1.0f);
                sides(out, 0.9f, -4.5f, -3.8f);
                back(out, 1.4f, -1.0f);
                body(out, -3.0f, 0f, 2.1f, 6.0f, 5.0f, 1.4f, HAIR);
                body(out, -2.6f, 5.0f, 2.1f, 5.2f, 1.6f, 1.0f, SHADOW);
            }
            case 35 -> {
                head(out, -3.8f, -13.4f, -5.2f, 7.6f, 5.0f, 6.8f, HAIR);
                head(out, -3.0f, -14.2f, -3.6f, 6.0f, 1.0f, 4.6f, SHADOW);
                cap(out, 0.8f);
                sides(out, 0.8f, -5.0f, -3.0f);
                back(out, 0.8f, -5.0f);
            }
            case 36 -> {
                cap(out, 1.0f);
                sides(out, 1.0f, -3.5f, -3.4f);
                back(out, 1.0f, -3.0f);
                float[] xs = {-3.6f, -1.8f, 0.0f, 1.8f, 3.6f};
                float[] lengths = {11f, 9f, 12f, 10f, 12f};
                for (int i = 0; i < xs.length; i++) {
                    head(out, xs[i] - 0.7f, -4.0f, 5.2f, 1.4f, lengths[i], 1.4f, i % 2 == 0 ? HAIR : SHADOW);
                }
                for (int s : new int[]{-1, 1}) {
                    head(out, s * 5.0f - 0.7f, -6.0f, -1.5f, 1.4f, 4.0f, 1.4f, HAIR);
                    head(out, s * 5.0f - 0.7f, -6.0f, 2.0f, 1.4f, 4.0f, 1.4f, SHADOW);
                }
            }
            case 37 -> {
                cap(out, 0.8f);
                sides(out, 0.8f, -5.5f, -3.0f);
                back(out, 0.8f, -5.0f);
                head(out, -2.4f, -12.0f, 0.5f, 4.8f, 4.0f, 4.8f, HAIR);
                head(out, -1.6f, -12.8f, 1.3f, 3.2f, 0.9f, 3.2f, SHADOW);
                head(out, -2.6f, -8.9f, 0.4f, 5.2f, 0.9f, 5.2f, TIE);
            }
            case 38 -> {
                cap(out, 1.6f);
                sides(out, 1.6f, -5.5f, -4.4f);
                back(out, 1.6f, -5.5f);
                head(out, -4.9f, -9.8f, -5.4f, 9.8f, 3.4f, 1.2f, HAIR);
            }
            case 39 -> {
                cap(out, 1.0f);
                sides(out, 1.2f, -1.0f, -3.8f);
                back(out, 1.2f, -3.0f);
                head(out, -4.7f, -9.6f, -5.2f, 9.4f, 3.2f, 1.0f, HAIR);
                head(out, -4.8f, -9.6f, -5.2f, 0.8f, 8.6f, 1.0f, SHADOW);
                head(out, 4.0f, -9.6f, -5.2f, 0.8f, 8.6f, 1.0f, SHADOW);
            }
            default -> { }
        }
        return out;
    }

    // ------------------------------------------------------------------ briques communes

    private static void head(List<Piece> out, float x, float y, float z, float w, float h, float d, int tone) {
        out.add(new Piece(Part.HEAD, 0, 0, 0, 0, x, y, z, w, h, d, tone));
    }

    private static void body(List<Piece> out, float x, float y, float z, float w, float h, float d, int tone) {
        out.add(new Piece(Part.BODY, 0, 0, 0, 0, x, y, z, w, h, d, tone));
    }

    /** Calotte au-dessus de la tête. */
    private static void cap(List<Piece> out, float t) {
        head(out, -H, TOP - t, -H, 2 * H, t, 2 * H, HAIR);
    }

    /** Mèches de chaque côté de la tête, de la hauteur de la calotte jusqu'à {@code bottom}. */
    private static void sides(List<Piece> out, float t, float bottom, float front) {
        head(out, H, TOP - t, front, t, bottom - TOP + t, H + t - front, HAIR);
        head(out, -H - t, TOP - t, front, t, bottom - TOP + t, H + t - front, HAIR);
    }

    /** Derrière de la tête, jusqu'à {@code bottom}. */
    private static void back(List<Piece> out, float t, float bottom) {
        head(out, -H, TOP - t, H, 2 * H, bottom - TOP + t, t, HAIR);
    }

    /** Frange droite au-dessus des sourcils. */
    private static void bangs(List<Piece> out) {
        head(out, -4.7f, -9.4f, -5.0f, 9.4f, 2.8f, 1.0f, HAIR);
    }

    /** Cheveux longs : tête, puis rideau dans le dos jusqu'à {@code length} sous le cou. */
    private static void longHair(List<Piece> out, float t, float length, float backT) {
        cap(out, t);
        sides(out, t, -2.2f, -3.8f);
        back(out, t, 0.0f);
        body(out, -4.2f, 0f, 2.1f, 8.4f, length, backT, HAIR);
    }

    /** Queue qui pend : un élastique puis une mèche inclinée vers le dos. */
    private static void tail(List<Piece> out, float px, float py, float pz, float rx, float w, float length, float d) {
        out.add(new Piece(Part.HEAD, px, py, pz, 0f, -w / 2f + 0.3f, -0.6f, -0.2f, w - 0.6f, 1.0f, d - 0.4f, TIE));
        out.add(new Piece(Part.HEAD, px, py, pz, rx, -w / 2f, 0.4f, 0f, w, length, d, HAIR));
    }
}
