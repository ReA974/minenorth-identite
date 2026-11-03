package fr.minenorth.identite.client.creator;

import fr.minenorth.identite.document.Gender;
import fr.minenorth.identite.drawing.DrawingKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CreatorStateDrawingTest {
    private static byte[] face(int value) {
        byte[] g = new byte[64];
        g[10] = (byte) value;
        return g;
    }

    @Test
    void settingADrawingChangesTheAppearanceAndCanBeUndone() {
        CreatorState s = CreatorState.forCreation();
        s.setDrawing(DrawingKind.FACE, face(5));
        assertEquals(5, s.appearance().drawings().get(DrawingKind.FACE)[10]);
        assertTrue(s.canUndo());
        s.undo();
        assertTrue(s.appearance().drawings().isEmpty());
        s.redo();
        assertEquals(5, s.appearance().drawings().get(DrawingKind.FACE)[10]);
    }

    @Test
    void settingTheSameDrawingDoesNotPolluteTheHistory() {
        CreatorState s = CreatorState.forCreation();
        s.setDrawing(DrawingKind.FACE, new byte[64]);
        assertFalse(s.canUndo());
        s.setDrawing(DrawingKind.FACE, face(2));
        s.setDrawing(DrawingKind.FACE, face(2));
        s.undo();
        assertFalse(s.canUndo());
    }

    @Test
    void drawingsSurviveAGenderChange() {
        CreatorState s = CreatorState.forCreation();
        s.setDrawing(DrawingKind.TATTOO_BACK, new byte[96]);
        byte[] back = new byte[96];
        back[0] = 3;
        s.setDrawing(DrawingKind.TATTOO_BACK, back);
        s.setGender(Gender.FEMALE);
        assertEquals(3, s.appearance().drawings().get(DrawingKind.TATTOO_BACK)[0]);
    }

    @Test
    void anInvalidDrawingIsRefusedAndLeavesTheStateAlone() {
        CreatorState s = CreatorState.forCreation();
        assertThrows(IllegalArgumentException.class, () -> s.setDrawing(DrawingKind.FACE, new byte[3]));
        assertThrows(IllegalArgumentException.class, () -> s.setDrawing(DrawingKind.FACE, face(100)));
        assertTrue(s.appearance().drawings().isEmpty());
        assertFalse(s.canUndo());
    }

    @Test
    void randomizingASectionKeepsTheDrawings() {
        CreatorState s = CreatorState.forCreation();
        s.setDrawing(DrawingKind.BEARD, face(4));
        s.randomizeSection(fr.minenorth.identite.appearance.AppearanceSection.FACIAL_HAIR, new java.util.Random(1));
        assertEquals(4, s.appearance().drawings().get(DrawingKind.BEARD)[10]);
    }
}
