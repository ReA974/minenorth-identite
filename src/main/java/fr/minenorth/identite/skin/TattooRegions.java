package fr.minenorth.identite.skin;

import fr.minenorth.identite.appearance.TattooZone;
import fr.minenorth.identite.drawing.DrawingKind;

/**
 * Où chaque face du corps se trouve dans l'image de la couche de tatouages haute définition (128 × 144) : le torse
 * (devant, dos) en 32 × 48, et pour chaque bras et jambe quatre faces de 16 × 48 dans l'ordre avant, extérieur, intérieur,
 * arrière (le même que le choix de face du salon).
 */
public final class TattooRegions {
    private TattooRegions() {}

    public static final int ATLAS_W = 128, ATLAS_H = 176;

    /** Le coin haut gauche de l'image 32 × 32 du maquillage et des particularités de la peau ({@link HiResFace}). */
    public static final int[] FACE_ORIGIN = {0, 144};

    /** Les zones qui ont une couche haute définition (le visage garde ses petites marques peintes). */
    public static final TattooZone[] ZONES = {TattooZone.TORSO, TattooZone.BACK, TattooZone.RIGHT_ARM, TattooZone.LEFT_ARM,
            TattooZone.RIGHT_LEG, TattooZone.LEFT_LEG};

    public static boolean isLimb(TattooZone zone) { return zone != TattooZone.TORSO && zone != TattooZone.BACK && zone != TattooZone.FACE_NECK; }

    /** Largeur d'une face dans l'image : 32 pour le torse, 16 pour un membre. */
    public static int width(TattooZone zone) { return isLimb(zone) ? 16 : 32; }

    public static final int HEIGHT = 48;

    /** Coin haut gauche de la face dans l'image ({@code face} : 0 avant, 1 extérieur, 2 intérieur, 3 arrière ; ignoré pour le torse). */
    public static int[] origin(TattooZone zone, int face) {
        return switch (zone) {
            case TORSO -> new int[]{0, 0};
            case BACK -> new int[]{32, 0};
            case RIGHT_ARM -> new int[]{64 + 16 * face, 0};
            case LEFT_ARM -> new int[]{16 * face, 48};
            case RIGHT_LEG -> new int[]{64 + 16 * face, 48};
            case LEFT_LEG -> new int[]{16 * face, 96};
            default -> new int[]{0, 0};
        };
    }

    /** Le réglage « face » d'une zone de membre (0 si la zone n'a pas de choix de face). */
    public static int faceSlot(TattooZone zone) {
        return switch (zone) {
            case RIGHT_ARM -> TattooZone.RIGHT_ARM_FACE_SLOT;
            case LEFT_ARM -> TattooZone.LEFT_ARM_FACE_SLOT;
            case RIGHT_LEG -> TattooZone.RIGHT_LEG_FACE_SLOT;
            case LEFT_LEG -> TattooZone.LEFT_LEG_FACE_SLOT;
            default -> -1;
        };
    }

    /** Le dessin libre d'une zone ; null pour le visage. */
    public static DrawingKind drawing(TattooZone zone) {
        return switch (zone) {
            case TORSO -> DrawingKind.TATTOO_TORSO;
            case BACK -> DrawingKind.TATTOO_BACK;
            case RIGHT_ARM -> DrawingKind.TATTOO_RIGHT_ARM;
            case LEFT_ARM -> DrawingKind.TATTOO_LEFT_ARM;
            case RIGHT_LEG -> DrawingKind.TATTOO_LEGS;
            case LEFT_LEG -> DrawingKind.TATTOO_LEFT_LEG;
            default -> null;
        };
    }
}
