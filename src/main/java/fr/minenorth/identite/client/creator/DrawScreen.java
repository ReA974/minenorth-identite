package fr.minenorth.identite.client.creator;

import fr.minenorth.identite.client.MineNorthStyle;
import fr.minenorth.identite.drawing.DrawingEditor;
import fr.minenorth.identite.drawing.DrawingKind;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Éditeur de pixels pour un dessin (visage, barbe, tatouage) : palette limitée, crayon, gomme, pipette, remplissage,
 * annuler / rétablir. Sous la grille, on voit ce que le catalogue a déjà dessiné à cet endroit.
 */
@OnlyIn(Dist.CLIENT)
public final class DrawScreen extends Screen {
    private enum Tool {
        PENCIL("Crayon"), ERASER("Gomme"), PICKER("Pipette"), FILL("Remplir");
        final String label;
        Tool(String label) { this.label = label; }
    }

    private record Hit(int x, int y, int w, int h, Runnable action) {
        boolean contains(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private final Screen parent;
    private final DrawingKind kind;
    private final DrawingEditor editor;
    private final int[] underlay;
    private final Consumer<byte[]> onDone;

    private Tool tool = Tool.PENCIL;
    private int color = 1;
    private String message = "";
    private boolean painting;
    private List<Hit> hits = List.of();

    private int px, py, pw, ph, cell, gridX, gridY, rightX;

    /**
     * @param underlay pixels ARGB déjà présents à cet endroit du skin (largeur × hauteur du dessin) ; peut contenir des 0
     * @param onDone reçoit la grille quand le joueur valide
     */
    public DrawScreen(Screen parent, DrawingKind kind, byte[] initial, int[] underlay, Consumer<byte[]> onDone) {
        super(Component.literal("Dessiner"));
        this.parent = parent;
        this.kind = kind;
        this.editor = new DrawingEditor(kind, initial);
        this.underlay = underlay;
        this.onDone = onDone;
    }

    @Override protected void init() {
        pw = Math.min(width - 8, 440);
        ph = Math.min(height - 8, 280);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        cell = Math.max(6, Math.min(Math.min((ph - 84) / kind.height(), (pw / 2 - 24) / kind.width()), 30));
        gridX = px + 14;
        gridY = py + 48;
        rightX = gridX + kind.width() * cell + 22;
    }

    // ------------------------------------------------------------------ rendu

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        MineNorthStyle.panel(g, px, py, pw, ph, "DESSINER", kind.label());
        List<Hit> list = new ArrayList<>();
        drawGrid(g, mx, my);
        int y = py + 48;
        y = drawTools(g, list, y);
        y = drawPalette(g, list, y + 6, mx, my);
        drawActions(g, list, y + 6);
        hits = list;
        if (!message.isEmpty()) g.drawString(font, font.plainSubstrByWidth(message, pw - 28), px + 14, py + ph - 14, MineNorthStyle.WARN, false);
        super.render(g, mx, my, pt);
    }

    private void drawGrid(GuiGraphics g, int mx, int my) {
        int w = kind.width(), h = kind.height();
        g.fill(gridX - 2, gridY - 2, gridX + w * cell + 2, gridY + h * cell + 2, MineNorthStyle.FRAME);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int cx = gridX + x * cell, cy = gridY + y * cell;
                g.fill(cx, cy, cx + cell, cy + cell, ((x + y) & 1) == 0 ? 0xFF3A3A4E : 0xFF2E2E40);
                int under = underlay[y * w + x];
                if ((under >>> 24) != 0) g.fill(cx, cy, cx + cell, cy + cell, under);
                int index = editor.get(x, y);
                if (index != 0) g.fill(cx, cy, cx + cell, cy + cell, kind.color(index));
                g.renderOutline(cx, cy, cell, cell, 0x22000000);
            }
        }
        int hx = (mx - gridX) / cell, hy = (my - gridY) / cell;
        if (mx >= gridX && my >= gridY && hx < w && hy < h) {
            g.renderOutline(gridX + hx * cell, gridY + hy * cell, cell, cell, MineNorthStyle.WHITE);
        }
    }

    private int drawTools(GuiGraphics g, List<Hit> list, int y) {
        g.drawString(font, "Outil", rightX, y, MineNorthStyle.BLUE, false);
        y += 12;
        int x = rightX;
        for (Tool t : Tool.values()) {
            int w = font.width(t.label) + 12;
            g.fill(x, y, x + w, y + 14, t == tool ? MineNorthStyle.CYAN : MineNorthStyle.DARK);
            g.drawString(font, t.label, x + 6, y + 3, MineNorthStyle.WHITE, false);
            list.add(new Hit(x, y, w, 14, () -> tool = t));
            x += w + 3;
        }
        return y + 20;
    }

    private int drawPalette(GuiGraphics g, List<Hit> list, int y, int mx, int my) {
        g.drawString(font, "Couleurs", rightX, y, MineNorthStyle.BLUE, false);
        y += 12;
        int size = 14, gap = 3;
        int perRow = Math.max(4, (px + pw - 14 - rightX) / (size + gap));
        for (int i = 1; i <= kind.paletteSize(); i++) {
            int x = rightX + ((i - 1) % perRow) * (size + gap);
            int cy = y + ((i - 1) / perRow) * (size + gap);
            g.fill(x - 1, cy - 1, x + size + 1, cy + size + 1, i == color ? MineNorthStyle.WHITE : MineNorthStyle.FRAME);
            g.fill(x, cy, x + size, cy + size, kind.color(i));
            final int index = i;
            list.add(new Hit(x, cy, size, size, () -> {
                color = index;
                if (tool == Tool.ERASER || tool == Tool.PICKER) tool = Tool.PENCIL;
            }));
        }
        int rows = (kind.paletteSize() + perRow - 1) / perRow;
        return y + rows * (size + gap);
    }

    private void drawActions(GuiGraphics g, List<Hit> list, int y) {
        int x = rightX, w = (px + pw - 14 - rightX - 4) / 2;
        action(g, list, x, y, w, "Annuler", editor.canUndo(), MineNorthStyle.DARK, editor::undo);
        action(g, list, x + w + 4, y, w, "Rétablir", editor.canRedo(), MineNorthStyle.DARK, editor::redo);
        y += 20;
        action(g, list, x, y, w, "Tout effacer", true, MineNorthStyle.PINK, editor::clear);
        if (kind == DrawingKind.FACE) {
            action(g, list, x + w + 4, y, w, "Ma tête", true, MineNorthStyle.CYAN, this::importHead);
        }
        y += 20;
        action(g, list, x, y, w, "Valider", true, MineNorthStyle.GREEN, this::done);
        action(g, list, x + w + 4, y, w, "Fermer", true, MineNorthStyle.DARK, this::close);
    }

    private void action(GuiGraphics g, List<Hit> list, int x, int y, int w, String label, boolean active, int colorValue, Runnable run) {
        MineNorthStyle.button(g, x, y, w, 16, label, colorValue, false, active);
        if (active) list.add(new Hit(x, y, w, 16, run));
    }

    // ------------------------------------------------------------------ actions

    private void importHead() {
        message = "Lecture de votre skin Minecraft… (rien ne se passe si votre compte n'a pas de skin)";
        MojangHead.fetch(grid -> {
            editor.replace(grid);
            message = "Tête importée : retouchez-la, puis validez.";
        }, error -> message = error);
    }

    private void done() {
        onDone.accept(editor.grid());
        close();
    }

    private void close() {
        minecraft.setScreen(parent);
    }

    // ------------------------------------------------------------------ entrées

    private void applyAt(double mx, double my, boolean erase) {
        int x = (int) Math.floor((mx - gridX) / cell), y = (int) Math.floor((my - gridY) / cell);
        if (mx < gridX || my < gridY || x >= kind.width() || y >= kind.height()) return;
        Tool t = erase ? Tool.ERASER : tool;
        switch (t) {
            case PENCIL -> editor.paint(x, y, color);
            case ERASER -> editor.erase(x, y);
            case PICKER -> {
                int picked = editor.pick(x, y);
                if (picked != 0) {
                    color = picked;
                    tool = Tool.PENCIL;
                }
            }
            case FILL -> editor.fill(x, y, color);
        }
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        for (Hit h : hits) {
            if (h.contains(mx, my)) { h.action.run(); return true; }
        }
        if (mx >= gridX && my >= gridY && mx < gridX + kind.width() * cell && my < gridY + kind.height() * cell) {
            painting = true;
            editor.beginStroke();
            applyAt(mx, my, button == 1);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (painting && (tool == Tool.PENCIL || tool == Tool.ERASER || button == 1)) {
            applyAt(mx, my, button == 1);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override public boolean mouseReleased(double mx, double my, int button) {
        if (painting) {
            painting = false;
            editor.endStroke();
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override public boolean keyPressed(int key, int scan, int mods) {
        if (hasControlDown() && key == GLFW.GLFW_KEY_Z) { editor.undo(); return true; }
        if (hasControlDown() && key == GLFW.GLFW_KEY_Y) { editor.redo(); return true; }
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_4) { tool = Tool.values()[key - GLFW.GLFW_KEY_1]; return true; }
        return super.keyPressed(key, scan, mods);
    }

    /** Vrai si cet écran a été ouvert depuis la création de personnage (pas depuis l'éditeur d'un autre mod). */
    public boolean belongsToCreation() {
        return parent instanceof CreatorScreen c ? c.isCreation() : parent instanceof TattooSalonScreen t && t.belongsToCreation();
    }

    @Override public void onClose() { close(); }

    @Override public boolean isPauseScreen() { return false; }
}
