package fr.minenorth.identite.drawing;

/**
 * Où chaque dessin tombe sur le skin 64×64 : sert à montrer, sous la grille de dessin, ce que le catalogue a déjà
 * dessiné à cet endroit. Doit rester d'accord avec {@code SkinComposer} (un test le vérifie).
 */
public final class DrawingRegions {
    private DrawingRegions() {}

    private static int originX(DrawingKind kind) {
        return switch (kind) {
            case FACE, BEARD -> 8;
            case TATTOO_TORSO -> 20;
            case TATTOO_BACK -> 32;
            case TATTOO_RIGHT_ARM -> 44;
            case TATTOO_LEFT_ARM -> 36;
            case TATTOO_LEGS -> 4;
            case TATTOO_LEFT_LEG -> 20;
        };
    }

    private static int originY(DrawingKind kind) {
        return switch (kind) {
            case FACE, BEARD -> 8;
            case TATTOO_TORSO, TATTOO_BACK, TATTOO_RIGHT_ARM, TATTOO_LEGS -> 20;
            case TATTOO_LEFT_ARM, TATTOO_LEFT_LEG -> 52;
        };
    }

    private static boolean isArm(DrawingKind kind) {
        return kind == DrawingKind.TATTOO_RIGHT_ARM || kind == DrawingKind.TATTOO_LEFT_ARM;
    }

    /**
     * Pixels ARGB du skin à l'endroit du dessin (largeur × hauteur du dessin, ligne par ligne). Avec des bras fins, la
     * quatrième colonne n'existe pas sur le bras : elle est transparente.
     */
    public static int[] read(int[] skin, DrawingKind kind, boolean slimArms) {
        if (skin == null || skin.length != 64 * 64) throw new IllegalArgumentException("skin 64x64 attendu");
        int[] out = new int[kind.width() * kind.height()];
        for (int y = 0; y < kind.height(); y++) {
            for (int x = 0; x < kind.width(); x++) {
                if (slimArms && isArm(kind) && x >= 3) continue;
                out[y * kind.width() + x] = skin[(originY(kind) + y) * 64 + originX(kind) + x];
            }
        }
        return out;
    }
}
