package fr.minenorth.identite.drawing;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * Logique de l'éditeur de pixels (sans rendu) : crayon, gomme, pipette, remplissage, effacer tout, annuler / rétablir.
 * Un trait de souris (beginStroke … endStroke) compte pour une seule étape d'annulation.
 */
public final class DrawingEditor {
    private final DrawingKind kind;
    private final byte[] initial;
    private byte[] cells;
    private final Deque<byte[]> undo = new ArrayDeque<>();
    private final Deque<byte[]> redo = new ArrayDeque<>();
    private boolean stroking, strokeRecorded;

    /** Lève IllegalArgumentException si le dessin de départ n'a pas la taille ou la palette de ce type. */
    public DrawingEditor(DrawingKind kind, byte[] initial) {
        Drawings.EMPTY.with(kind, initial);
        this.kind = kind;
        this.initial = initial.clone();
        this.cells = initial.clone();
    }

    public DrawingKind kind() { return kind; }

    /** Copie de la grille (numéros de couleur, ligne par ligne). */
    public byte[] grid() { return cells.clone(); }

    private boolean inside(int x, int y) { return x >= 0 && y >= 0 && x < kind.width() && y < kind.height(); }

    /** Numéro de couleur de la case ; 0 hors de la grille. */
    public int get(int x, int y) { return inside(x, y) ? cells[y * kind.width() + x] : 0; }

    public int pick(int x, int y) { return get(x, y); }

    /** Pose une couleur (1 à N) ; 0 efface. Lève IllegalArgumentException si la couleur sort de la palette. */
    public void paint(int x, int y, int color) {
        if (color < 0 || color > kind.paletteSize()) throw new IllegalArgumentException("couleur hors palette : " + color);
        if (!inside(x, y) || get(x, y) == color) return;
        record();
        cells[y * kind.width() + x] = (byte) color;
    }

    public void erase(int x, int y) { paint(x, y, 0); }

    /** Remplit la zone de même couleur qui touche cette case (voisines en haut, bas, gauche, droite). */
    public void fill(int x, int y, int color) {
        if (color < 0 || color > kind.paletteSize()) throw new IllegalArgumentException("couleur hors palette : " + color);
        if (!inside(x, y)) return;
        int target = get(x, y);
        if (target == color) return;
        record();
        Deque<int[]> todo = new ArrayDeque<>();
        todo.push(new int[]{x, y});
        while (!todo.isEmpty()) {
            int[] p = todo.pop();
            if (!inside(p[0], p[1]) || get(p[0], p[1]) != target) continue;
            cells[p[1] * kind.width() + p[0]] = (byte) color;
            todo.push(new int[]{p[0] + 1, p[1]});
            todo.push(new int[]{p[0] - 1, p[1]});
            todo.push(new int[]{p[0], p[1] + 1});
            todo.push(new int[]{p[0], p[1] - 1});
        }
    }

    /** Remplace toute la grille (import de la tête) ; une seule étape d'annulation. Lève IllegalArgumentException si invalide. */
    public void replace(byte[] grid) {
        Drawings.EMPTY.with(kind, grid);
        if (Arrays.equals(cells, grid)) return;
        record();
        cells = grid.clone();
    }

    public void clear() {
        boolean blank = true;
        for (byte b : cells) if (b != 0) { blank = false; break; }
        if (blank) return;
        record();
        Arrays.fill(cells, (byte) 0);
    }

    public void beginStroke() {
        stroking = true;
        strokeRecorded = false;
    }

    public void endStroke() {
        stroking = false;
        strokeRecorded = false;
    }

    /** Garde l'état d'avant la modification, une seule fois par trait. */
    private void record() {
        if (stroking && strokeRecorded) return;
        undo.push(cells.clone());
        redo.clear();
        if (stroking) strokeRecorded = true;
    }

    public boolean canUndo() { return !undo.isEmpty(); }
    public boolean canRedo() { return !redo.isEmpty(); }

    public void undo() {
        if (undo.isEmpty()) return;
        redo.push(cells);
        cells = undo.pop();
    }

    public void redo() {
        if (redo.isEmpty()) return;
        undo.push(cells);
        cells = redo.pop();
    }

    /** Vrai si la grille diffère du dessin de départ. */
    public boolean isModified() { return !Arrays.equals(cells, initial); }
}
