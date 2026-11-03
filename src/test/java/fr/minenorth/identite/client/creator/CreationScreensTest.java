package fr.minenorth.identite.client.creator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CreationScreensTest {
    @Test
    void opensWhenNothingOfTheCreationIsOnScreen() {
        assertTrue(CreationScreens.shouldOpen(false, false));
    }

    @Test
    void doesNotReopenOverTheCreationScreen() {
        assertFalse(CreationScreens.shouldOpen(true, false));
    }

    @Test
    void doesNotReopenOverADrawingOpenedFromTheCreation() {
        assertFalse(CreationScreens.shouldOpen(false, true));
    }

    @Test
    void aDrawingOpenedFromAnEditorDoesNotBlockTheCreationScreen() {
        // un dessin ouvert depuis l'éditeur d'un autre mod n'appartient pas à la création
        assertTrue(CreationScreens.shouldOpen(false, false));
    }
}
