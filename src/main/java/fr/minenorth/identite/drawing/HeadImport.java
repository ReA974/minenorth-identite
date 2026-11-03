package fr.minenorth.identite.drawing;

/** Reprend le visage d'un skin Minecraft comme dessin de visage, en le ramenant à la palette limitée. */
public final class HeadImport {
    private HeadImport() {}

    /**
     * @param skin pixels ARGB du skin, ligne par ligne ; 64 de large, 32 ou 64 de haut
     * @return grille 8×8 pour {@link DrawingKind#FACE} : le devant de la tête, avec la couche de chapeau par-dessus
     *         là où elle est opaque, chaque pixel ramené à la couleur de palette la plus proche (0 = transparent)
     */
    public static byte[] faceFromSkin(int[] skin, int width, int height) {
        if (width != 64 || (height != 32 && height != 64) || skin == null || skin.length < width * height) {
            throw new IllegalArgumentException("skin de taille inattendue : " + width + "x" + height);
        }
        byte[] face = new byte[64];
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int base = skin[(8 + y) * width + 8 + x];
                int hat = skin[(8 + y) * width + 40 + x];
                int pixel = (hat >>> 24) >= 128 ? hat : base;
                face[y * 8 + x] = (byte) DrawingKind.FACE.nearest(pixel);
            }
        }
        return face;
    }
}
