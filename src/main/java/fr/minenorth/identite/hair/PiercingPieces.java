package fr.minenorth.identite.hair;

import java.util.ArrayList;
import java.util.List;

/**
 * Piercings en 3D : de petits bijoux (clous, anneaux, barres) posés devant le visage ou le ventre. Mêmes unités que
 * {@link HairPieces} (1 = 1 pixel de skin, y vers le bas, le devant est du côté z négatif). Teintes : 0 métal, 1 reflet,
 * 2 ombre. Le métal lui-même (argent, or…) est la texture choisie au rendu.
 */
public final class PiercingPieces {
    private PiercingPieces() {}

    /** Ordre des réglages de la section PIERCINGS : oreilles (retirées), nez, sourcil, lèvre, métal, haut de l'oreille (retiré), nombril. */
    public static final int NOSE = 1, BROW = 2, LIP = 3, METAL = 4, NAVEL = 6;

    public static List<HairPieces.Piece> of(int[] p) {
        List<HairPieces.Piece> out = new ArrayList<>();
        nose(out, p[NOSE]);
        brow(out, p[BROW]);
        lip(out, p[LIP]);
        navel(out, p[NAVEL]);
        return out;
    }

    // ------------------------------------------------------------------ visage (devant la tête, z de -5,1 à -4,2)

    private static void nose(List<HairPieces.Piece> out, int style) {
        switch (style) {
            case 1 -> stud(out, -1.5f, -2.5f);
            case 2 -> stud(out, 1.5f, -2.5f);
            case 3 -> {
                stud(out, -1.5f, -2.5f);
                stud(out, 1.5f, -2.5f);
            }
            case 4 -> ring(out, -1.55f, -1.95f, 0.75f);
            case 5 -> {   // septum : un fer à cheval sous le nez
                head(out, -1.0f, -2.0f, -5.0f, 2.0f, 0.4f, 0.45f, 0);
                head(out, -1.0f, -2.6f, -4.95f, 0.4f, 0.7f, 0.4f, 0);
                head(out, 0.6f, -2.6f, -4.95f, 0.4f, 0.7f, 0.4f, 0);
                head(out, -0.25f, -1.75f, -5.1f, 0.5f, 0.5f, 0.5f, 1);
            }
            default -> { }
        }
    }

    private static void brow(List<HairPieces.Piece> out, int style) {
        switch (style) {
            case 1 -> ring(out, -2.6f, -4.95f, 0.75f);
            case 2 -> barbell(out, -3.2f, -2.0f, -4.95f);
            case 3 -> {
                ring(out, -2.6f, -4.95f, 0.75f);
                ring(out, 2.6f, -4.95f, 0.75f);
            }
            case 4 -> {
                barbell(out, -3.2f, -2.0f, -4.95f);
                barbell(out, 2.0f, 3.2f, -4.95f);
            }
            default -> { }
        }
    }

    private static void lip(List<HairPieces.Piece> out, int style) {
        switch (style) {
            case 1 -> stud(out, 0f, -0.45f);
            case 2 -> ring(out, 0f, -0.95f, 0.8f);
            case 3 -> {   // snake bites : deux clous de chaque côté de la lèvre
                stud(out, -1.6f, -0.65f);
                stud(out, 1.6f, -0.65f);
            }
            case 4 -> stud(out, 1.7f, -1.75f);   // monroe
            case 5 -> stud(out, 0f, -2.05f);     // medusa
            default -> { }
        }
    }

    // ------------------------------------------------------------------ nombril (devant le torse, z de -3,2 à -2,3)

    private static void navel(List<HairPieces.Piece> out, int style) {
        switch (style) {
            case 1 -> studBody(out, -0.5f, 7.5f);
            case 2 -> {
                body(out, -1.4f, 7.1f, -3.0f, 1.8f, 0.35f, 0.4f, 0);
                body(out, -1.4f, 8.45f, -3.0f, 1.8f, 0.35f, 0.4f, 0);
                body(out, -1.4f, 7.1f, -3.0f, 0.35f, 1.7f, 0.4f, 0);
                body(out, 0.05f, 7.1f, -3.0f, 0.35f, 1.7f, 0.4f, 0);
            }
            case 3 -> {   // pendant : clou, petite chaîne et pierre
                studBody(out, -0.5f, 7.3f);
                body(out, -0.7f, 7.8f, -3.0f, 0.4f, 1.0f, 0.4f, 2);
                body(out, -1.0f, 8.7f, -3.1f, 1.0f, 1.0f, 0.6f, 1);
            }
            case 4 -> {   // barre verticale
                body(out, -0.7f, 6.9f, -3.0f, 0.4f, 1.8f, 0.5f, 0);
                studBody(out, -0.5f, 6.9f);
                studBody(out, -0.5f, 8.7f);
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------ briques

    private static void head(List<HairPieces.Piece> out, float x, float y, float z, float w, float h, float d, int tone) {
        out.add(new HairPieces.Piece(HairPieces.Part.HEAD, 0, 0, 0, 0, x, y, z, w, h, d, tone));
    }

    private static void body(List<HairPieces.Piece> out, float x, float y, float z, float w, float h, float d, int tone) {
        out.add(new HairPieces.Piece(HairPieces.Part.BODY, 0, 0, 0, 0, x, y, z, w, h, d, tone));
    }

    /** Un clou : une petite bille brillante. */
    private static void stud(List<HairPieces.Piece> out, float cx, float cy) {
        head(out, cx - 0.45f, cy - 0.45f, -5.1f, 0.9f, 0.9f, 0.9f, 1);
    }

    private static void studBody(List<HairPieces.Piece> out, float cx, float cy) {
        body(out, cx - 0.45f, cy - 0.45f, -3.2f, 0.9f, 0.9f, 0.9f, 1);
    }

    /** Un anneau de face : quatre fines barres qui laissent voir la peau au milieu. */
    private static void ring(List<HairPieces.Piece> out, float cx, float cy, float r) {
        head(out, cx - r, cy - r, -5.0f, 2 * r, 0.35f, 0.4f, 1);
        head(out, cx - r, cy + r - 0.35f, -5.0f, 2 * r, 0.35f, 0.4f, 2);
        head(out, cx - r, cy - r, -5.0f, 0.35f, 2 * r, 0.4f, 0);
        head(out, cx + r - 0.35f, cy - r, -5.0f, 0.35f, 2 * r, 0.4f, 0);
    }

    /** Une barre à billes, couchée le long du sourcil. */
    private static void barbell(List<HairPieces.Piece> out, float xa, float xb, float cy) {
        head(out, xa, cy - 0.2f, -4.9f, xb - xa, 0.4f, 0.5f, 0);
        head(out, xa - 0.3f, cy - 0.4f, -5.1f, 0.8f, 0.8f, 0.8f, 1);
        head(out, xb - 0.5f, cy - 0.4f, -5.1f, 0.8f, 0.8f, 0.8f, 1);
    }
}
