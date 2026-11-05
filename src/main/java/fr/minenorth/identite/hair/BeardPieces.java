package fr.minenorth.identite.hair;

import java.util.ArrayList;
import java.util.List;

/**
 * Barbes et moustaches en volume 3D : des blocs de poils posés sur le menton, les joues et au-dessus de la bouche, comme
 * les coiffures. Mêmes unités que {@link HairPieces}. Les yeux (y de -4 à -3, colonnes x -3 à -1 et 1 à 3) ne sont jamais
 * couverts. Les barbes de trois jours et l'ombre légère (1 et 2) restent peintes sur le skin.
 */
public final class BeardPieces {
    private BeardPieces() {}

    /** Zone des yeux à ne jamais couvrir : bas de la ligne des yeux. */
    public static final float EYES_BOTTOM = -3.0f;

    public static List<HairPieces.Piece> forStyle(int style) {
        List<HairPieces.Piece> out = new ArrayList<>();
        switch (style) {
            case 3 -> moustache(out, 2.8f, 1.0f);
            case 4 -> {
                moustache(out, 2.6f, 0.9f);
                face(out, -3.7f, -3.0f, -4.9f, 0.9f, 0.7f, 0.7f, 0);   // pointes relevées
                face(out, 2.8f, -3.0f, -4.9f, 0.9f, 0.7f, 0.7f, 0);
            }
            case 5 -> goatee(out);
            case 6 -> face(out, -0.7f, -0.9f, -5.0f, 1.4f, 1.1f, 0.8f, 0);   // mouche
            case 7 -> {   // collier : le tour de la mâchoire
                face(out, -3.6f, -0.4f, -4.9f, 7.2f, 1.0f, 0.7f, 0);
                face(out, -4.7f, -2.6f, -4.6f, 0.9f, 3.0f, 3.2f, 0);
                face(out, 3.8f, -2.6f, -4.6f, 0.9f, 3.0f, 3.2f, 0);
            }
            case 8 -> fullBeard(out, 0.9f, 0.9f);
            case 9 -> fullBeard(out, 1.3f, 1.8f);
            case 10 -> {   // favoris
                face(out, -4.9f, -6.0f, -4.6f, 1.0f, 5.2f, 3.6f, 0);
                face(out, 3.9f, -6.0f, -4.6f, 1.0f, 5.2f, 3.6f, 0);
            }
            case 11 -> {   // Van Dyke : moustache et bouc
                moustache(out, 2.4f, 0.9f);
                goatee(out);
            }
            default -> { }
        }
        return out;
    }

    // ------------------------------------------------------------------ briques

    private static void face(List<HairPieces.Piece> out, float x, float y, float z, float w, float h, float d, int tone) {
        out.add(new HairPieces.Piece(HairPieces.Part.HEAD, 0, 0, 0, 0, x, y, z, w, h, d, tone));
    }

    /** Moustache : une barre au-dessus de la bouche, avec les deux bouts qui retombent. */
    private static void moustache(List<HairPieces.Piece> out, float half, float depth) {
        face(out, -half, -2.7f, -4.4f - depth, 2 * half, 0.8f, depth, 0);
        face(out, -half - 0.1f, -1.9f, -4.4f - depth, 0.8f, 1.1f, depth * 0.8f, 2);
        face(out, half - 0.7f, -1.9f, -4.4f - depth, 0.8f, 1.1f, depth * 0.8f, 2);
    }

    /** Bouc : une touffe sous la lèvre qui dépasse du menton. */
    private static void goatee(List<HairPieces.Piece> out) {
        face(out, -1.2f, -0.9f, -5.0f, 2.4f, 1.6f, 0.9f, 0);
        face(out, -0.8f, 0.7f, -4.9f, 1.6f, 1.1f, 0.8f, 2);
    }

    /** Barbe : les joues et le menton, avec la bouche laissée libre ; plus ou moins épaisse et longue. */
    private static void fullBeard(List<HairPieces.Piece> out, float depth, float length) {
        moustache(out, 2.3f, depth * 0.9f);
        face(out, -4.5f, -3.0f, -4.4f - depth, 2.5f, 3.4f + length * 0.2f, depth, 0);   // joue droite
        face(out, 2.0f, -3.0f, -4.4f - depth, 2.5f, 3.4f + length * 0.2f, depth, 0);    // joue gauche
        face(out, -2.4f, -0.5f, -4.4f - depth, 4.8f, 0.5f + length, depth, 0);          // menton
        face(out, -2.0f, -0.5f + length, -4.4f - depth * 0.8f, 4.0f, 0.7f, depth * 0.8f, 2);
        face(out, -4.9f, -3.2f, -4.6f, 0.9f, 3.0f, 3.4f, 2);                            // pattes
        face(out, 4.0f, -3.2f, -4.6f, 0.9f, 3.0f, 3.4f, 2);
    }
}
