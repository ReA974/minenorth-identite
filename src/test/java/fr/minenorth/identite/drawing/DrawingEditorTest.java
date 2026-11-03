package fr.minenorth.identite.drawing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DrawingEditorTest {
    private static DrawingEditor editor(DrawingKind kind) { return new DrawingEditor(kind, new byte[kind.width() * kind.height()]); }

    @Test
    void paintSetsOneCell() {
        DrawingEditor e = editor(DrawingKind.FACE);
        e.paint(2, 3, 5);
        assertEquals(5, e.get(2, 3));
        assertEquals(0, e.get(3, 3));
        assertTrue(e.isModified());
    }

    @Test
    void paintOutsideTheGridDoesNothing() {
        DrawingEditor e = editor(DrawingKind.TATTOO_LEGS);
        e.paint(-1, 0, 1);
        e.paint(4, 0, 1);
        e.paint(0, 12, 1);
        assertFalse(e.isModified());
        assertFalse(e.canUndo());
    }

    @Test
    void paintRejectsColorsOutsideThePalette() {
        DrawingEditor e = editor(DrawingKind.FACE);
        assertThrows(IllegalArgumentException.class, () -> e.paint(0, 0, DrawingKind.FACE.paletteSize() + 1));
        assertThrows(IllegalArgumentException.class, () -> e.paint(0, 0, -1));
    }

    @Test
    void eraseMakesACellTransparentAgain() {
        DrawingEditor e = editor(DrawingKind.FACE);
        e.paint(1, 1, 4);
        e.erase(1, 1);
        assertEquals(0, e.get(1, 1));
    }

    @Test
    void pickReadsTheColorOfACell() {
        DrawingEditor e = editor(DrawingKind.BEARD);
        e.paint(7, 7, 9);
        assertEquals(9, e.pick(7, 7));
        assertEquals(0, e.pick(0, 0));
        assertEquals(0, e.pick(99, 99));
    }

    @Test
    void fillColorsTheWholeConnectedArea() {
        DrawingEditor e = editor(DrawingKind.TATTOO_RIGHT_ARM);
        e.fill(0, 0, 3);
        for (int y = 0; y < 12; y++) for (int x = 0; x < 4; x++) assertEquals(3, e.get(x, y));
    }

    @Test
    void fillStopsAtOtherColors() {
        DrawingEditor e = editor(DrawingKind.TATTOO_RIGHT_ARM);
        for (int x = 0; x < 4; x++) e.paint(x, 5, 1);
        e.fill(0, 0, 2);
        assertEquals(2, e.get(3, 4));
        assertEquals(1, e.get(2, 5));
        assertEquals(0, e.get(0, 6));
    }

    @Test
    void fillWithTheSameColorChangesNothing() {
        DrawingEditor e = editor(DrawingKind.FACE);
        e.paint(0, 0, 2);
        e.fill(0, 0, 2);
        e.undo();
        assertEquals(0, e.get(0, 0));
        assertFalse(e.canUndo());
    }

    @Test
    void clearEmptiesTheGridAndCanBeUndone() {
        DrawingEditor e = editor(DrawingKind.FACE);
        e.paint(0, 0, 2);
        e.clear();
        assertEquals(0, e.get(0, 0));
        e.undo();
        assertEquals(2, e.get(0, 0));
    }

    @Test
    void undoAndRedoStepOneAction() {
        DrawingEditor e = editor(DrawingKind.FACE);
        e.paint(0, 0, 1);
        e.paint(1, 0, 2);
        e.undo();
        assertEquals(0, e.get(1, 0));
        assertEquals(1, e.get(0, 0));
        e.redo();
        assertEquals(2, e.get(1, 0));
    }

    @Test
    void aStrokeIsOneUndoStep() {
        DrawingEditor e = editor(DrawingKind.FACE);
        e.beginStroke();
        e.paint(0, 0, 1);
        e.paint(1, 0, 1);
        e.paint(2, 0, 1);
        e.endStroke();
        e.undo();
        for (int x = 0; x < 3; x++) assertEquals(0, e.get(x, 0));
        assertFalse(e.canUndo());
    }

    @Test
    void aStrokeThatChangedNothingLeavesNoUndoStep() {
        DrawingEditor e = editor(DrawingKind.FACE);
        e.beginStroke();
        e.erase(0, 0);
        e.endStroke();
        assertFalse(e.canUndo());
    }

    @Test
    void newActionClearsRedo() {
        DrawingEditor e = editor(DrawingKind.FACE);
        e.paint(0, 0, 1);
        e.undo();
        e.paint(1, 1, 2);
        assertFalse(e.canRedo());
    }

    @Test
    void undoOnFreshEditorIsANoOp() {
        DrawingEditor e = editor(DrawingKind.FACE);
        e.undo();
        e.redo();
        assertFalse(e.isModified());
    }

    @Test
    void gridReturnsACopyAndStartsFromTheInitialDrawing() {
        byte[] start = new byte[64];
        start[9] = 4;
        DrawingEditor e = new DrawingEditor(DrawingKind.FACE, start);
        assertEquals(4, e.get(1, 1));
        e.grid()[9] = 0;
        assertEquals(4, e.get(1, 1));
        start[9] = 0;
        assertEquals(4, e.get(1, 1));
        assertFalse(e.isModified());
    }

    @Test
    void initialDrawingMustMatchTheKind() {
        assertThrows(IllegalArgumentException.class, () -> new DrawingEditor(DrawingKind.FACE, new byte[3]));
    }

    @Test
    void replaceSetsTheWholeGridAndCanBeUndone() {
        DrawingEditor e = editor(DrawingKind.FACE);
        e.paint(0, 0, 2);
        byte[] other = new byte[64];
        other[63] = 5;
        e.replace(other);
        assertEquals(0, e.get(0, 0));
        assertEquals(5, e.get(7, 7));
        e.undo();
        assertEquals(2, e.get(0, 0));
        assertEquals(0, e.get(7, 7));
    }

    @Test
    void replaceRejectsAnInvalidGridAndKeepsTheOldOne() {
        DrawingEditor e = editor(DrawingKind.FACE);
        e.paint(1, 1, 3);
        assertThrows(IllegalArgumentException.class, () -> e.replace(new byte[3]));
        byte[] bad = new byte[64];
        bad[0] = 99;
        assertThrows(IllegalArgumentException.class, () -> e.replace(bad));
        assertEquals(3, e.get(1, 1));
    }

    @Test
    void replacingWithTheSameGridChangesNothing() {
        DrawingEditor e = editor(DrawingKind.FACE);
        e.replace(new byte[64]);
        assertFalse(e.canUndo());
    }
}
