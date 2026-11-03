package fr.minenorth.identite.client.creator;

/**
 * Le serveur redemande l'écran de création toutes les 10 secondes à un joueur retenu (au cas où un autre écran l'aurait
 * remplacé). Il ne faut pas le rouvrir tant que la création est déjà à l'écran, sous une forme ou une autre : ce serait
 * fermer l'éditeur de dessin et perdre les choix en cours.
 */
public final class CreationScreens {
    private CreationScreens() {}

    public static boolean shouldOpen(boolean creationScreenOnTop, boolean drawingFromCreationOnTop) {
        return !creationScreenOnTop && !drawingFromCreationOnTop;
    }
}
