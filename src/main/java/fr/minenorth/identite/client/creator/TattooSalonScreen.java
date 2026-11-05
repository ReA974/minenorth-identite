package fr.minenorth.identite.client.creator;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.TattooZone;
import fr.minenorth.identite.client.MineNorthStyle;
import fr.minenorth.identite.document.Gender;
import fr.minenorth.identite.skin.SkinComposer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Salon de tatouage : un carnet de motifs à gauche, le personnage au centre (qui suit la zone choisie), et à droite les
 * réglages de la zone (taille, encre, face et hauteur sur un bras). Les choix s'affichent en direct ; « Tatouer »
 * enregistre.
 */
@OnlyIn(Dist.CLIENT)
public final class TattooSalonScreen extends Screen {
    private record Hit(int x, int y, int w, int h, Runnable action) {}

    private static final TattooZone[] ZONES = {TattooZone.FACE_NECK, TattooZone.TORSO, TattooZone.BACK,
            TattooZone.RIGHT_ARM, TattooZone.LEFT_ARM, TattooZone.RIGHT_LEG, TattooZone.LEFT_LEG};
    private static final String[] ZONE_NAMES = {"Visage", "Torse", "Dos", "Bras droit", "Bras gauche", "Jambe droite", "Jambe gauche"};
    /** Caméra de l'aperçu pour chaque zone (indices de PreviewRenderer.CAMS). */
    private static final int[] ZONE_CAMS = {2, 1, 3, 5, 4, 6, 6};
    private static final String[] VIEW_NAMES = {"Face", "Dos", "Bras D", "Bras G"};
    private static final int[] VIEW_CAMS = {0, 3, 5, 4};
    private static final String[] SIZES = {"Petit", "Moyen", "Grand"};
    private static final String[] INKS = {"Couleurs", "Noir", "Gris"};
    private static final String[] FACES = {"Avant", "Ext.", "Int.", "Arrière"};
    private static final String[] HEIGHTS = {"Centré", "Épaule", "Bras", "Avant-bras", "Poignet"};

    private final CreatorState state;
    private final Consumer<CreatorState> onSave;
    private final Appearance original;
    private final List<Hit> hits = new ArrayList<>();
    private int zone = 1;
    private int camera = 1;
    private float yaw;
    private Appearance previewAppearance;
    private Gender previewGender;

    private int padX, padY;
    private int px, py, pw, ph, bodyTop, bodyBottom, gridX, viewX, viewW, optX, optW;

    private TattooSalonScreen(CreatorState state, Consumer<CreatorState> onSave) {
        super(Component.literal("Salon de tatouage"));
        this.state = state;
        this.onSave = onSave;
        this.original = state.appearance();
    }

    public static TattooSalonScreen open(Gender gender, Appearance current, Consumer<CreatorState> onSave) {
        return new TattooSalonScreen(CreatorState.forEditor(gender, current), onSave);
    }

    @Override protected void init() {
        pw = Math.min(width - 8, 520);
        ph = Math.min(height - 8, 290);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        bodyTop = py + 38;
        bodyBottom = py + ph - 30;
        gridX = px + 8;
        viewX = gridX + 5 * 28 + 10;
        optW = Math.min(176, pw / 3);
        optX = px + pw - 8 - optW;
        viewW = optX - 8 - viewX;
    }

    // ------------------------------------------------------------------ données

    private int[] tattoos() { return state.appearance().get(AppearanceSection.TATTOOS); }

    private void setSlot(int slot, int value) {
        int[] t = tattoos();
        t[slot] = value;
        state.set(AppearanceSection.TATTOOS, t);
    }

    /** Le dessin libre de la zone (le visage n'en a pas : seulement de petites marques). */
    private fr.minenorth.identite.drawing.DrawingKind drawingKind() {
        return switch (zone()) {
            case FACE_NECK -> null;
            case TORSO -> fr.minenorth.identite.drawing.DrawingKind.TATTOO_TORSO;
            case BACK -> fr.minenorth.identite.drawing.DrawingKind.TATTOO_BACK;
            case RIGHT_ARM -> fr.minenorth.identite.drawing.DrawingKind.TATTOO_RIGHT_ARM;
            case LEFT_ARM -> fr.minenorth.identite.drawing.DrawingKind.TATTOO_LEFT_ARM;
            case RIGHT_LEG -> fr.minenorth.identite.drawing.DrawingKind.TATTOO_LEGS;
            case LEFT_LEG -> fr.minenorth.identite.drawing.DrawingKind.TATTOO_LEFT_LEG;
        };
    }

    private static boolean isBlank(byte[] grid) {
        for (byte b : grid) if (b != 0) return false;
        return true;
    }

    /** Ouvre l'Ã©diteur de pixels sur le dessin de la zone ; la grille montre ce que le catalogue dessine dÃ©jÃ  Ã  cet endroit. */
    private void openDraw(fr.minenorth.identite.drawing.DrawingKind kind) {
        Appearance a = state.appearance();
        Appearance without = a.withDrawings(a.drawings().with(kind, new byte[kind.width() * kind.height()]));
        int[] underlay = fr.minenorth.identite.drawing.DrawingRegions.read(
                SkinComposer.compose(state.gender(), without), kind, state.gender().slimArms());
        minecraft.setScreen(new DrawScreen(this, kind, a.drawings().get(kind), underlay, grid -> state.setDrawing(kind, grid)));
    }

    private TattooZone zone() { return ZONES[zone]; }

    /** Torse et dos : le tatouage se place oÃ¹ on veut. */
    private boolean isPlaceable() { return zone() == TattooZone.TORSO || zone() == TattooZone.BACK; }

    private int xSlot() { return zone() == TattooZone.TORSO ? TattooZone.TORSO_X_SLOT : TattooZone.BACK_X_SLOT; }

    private int ySlot() { return zone() == TattooZone.TORSO ? TattooZone.TORSO_Y_SLOT : TattooZone.BACK_Y_SLOT; }

    /** Cases occupÃ©es par le tatouage (taille petit, moyen, grand) : 4, 6 ou 8. */
    private int span() { return 4 + 2 * tattoos()[zone().sizeSlot()]; }

    private int ox() { int v = tattoos()[xSlot()]; return v > 0 ? Math.min(v - 1, 8 - span()) : (8 - span()) / 2; }

    private int oy() {
        int v = tattoos()[ySlot()];
        int size = tattoos()[zone().sizeSlot()];
        return v > 0 ? Math.min(v - 1, 12 - span()) : (size == 2 ? 2 : 3);
    }

    private boolean overPad(double mx, double my) { return isPlaceable() && over((int) mx, (int) my, padX, padY, 48, 72); }

    /** Place le centre du tatouage sur la case cliquÃ©e du schÃ©ma. */
    private void placeAt(double mx, double my) {
        int cx = (int) ((mx - padX) / 6), cy = (int) ((my - padY) / 6);
        int x = Math.max(0, Math.min(8 - span(), cx - span() / 2));
        int y = Math.max(0, Math.min(12 - span(), cy - span() / 2));
        int[] t = tattoos();
        if (t[xSlot()] == x + 1 && t[ySlot()] == y + 1) return;
        t[xSlot()] = x + 1;
        t[ySlot()] = y + 1;
        state.set(AppearanceSection.TATTOOS, t);
    }

    /** SchÃ©ma du torse (8 Ã— 12 cases) : le carrÃ© clair montre oÃ¹ sera le tatouage ; on clique pour le dÃ©placer. */
    private void drawPad(GuiGraphics g, int mx, int my, int x, int y) {
        g.drawString(font, "Position (cliquez)", x, y, MineNorthStyle.MUTED, false);
        padX = x;
        padY = y + 10;
        g.fill(padX - 1, padY - 1, padX + 49, padY + 73, MineNorthStyle.CYAN);
        g.fill(padX, padY, padX + 48, padY + 72, 0xFF6B4A38);
        int s = span(), ox = ox(), oy = oy();
        g.fill(padX + ox * 6, padY + oy * 6, padX + (ox + s) * 6, padY + (oy + s) * 6, 0xCCFFE066);
        g.drawString(font, "Cliquez ou glissez", padX + 56, padY + 4, MineNorthStyle.MUTED, false);
        g.drawString(font, "pour placer le", padX + 56, padY + 14, MineNorthStyle.MUTED, false);
        g.drawString(font, "tatouage.", padX + 56, padY + 24, MineNorthStyle.MUTED, false);
    }

    private boolean isArm() { return zone() == TattooZone.RIGHT_ARM || zone() == TattooZone.LEFT_ARM; }

    private int faceSlot() { return zone() == TattooZone.RIGHT_ARM ? TattooZone.RIGHT_ARM_FACE_SLOT : TattooZone.LEFT_ARM_FACE_SLOT; }

    private int heightSlot() { return zone() == TattooZone.RIGHT_ARM ? TattooZone.RIGHT_ARM_HEIGHT_SLOT : TattooZone.LEFT_ARM_HEIGHT_SLOT; }

    private void chooseZone(int index) {
        zone = index;
        camera = ZONE_CAMS[index];
        yaw = 0;
    }

    // ------------------------------------------------------------------ dessin

    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        hits.clear();
        MineNorthStyle.panel(g, px, py, pw, ph, "MINENORTHRP - SALON DE TATOUAGE", "Choisissez un motif, une zone, puis tatouez. Paiement par carte.");
        drawGrid(g, mx, my);
        drawPreview(g, mx, my);
        drawOptions(g, mx, my);
        drawFooter(g, mx, my);
        super.render(g, mx, my, partial);
    }

    private boolean over(int mx, int my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    private void drawGrid(GuiGraphics g, int mx, int my) {
        g.drawString(font, "Motifs", gridX, bodyTop - 2, MineNorthStyle.CYAN, false);
        int top = bodyTop + 10;
        int current = tattoos()[zone().motifSlot()];
        if (zone() == TattooZone.FACE_NECK) {
            int y = top;
            for (int m = 0; m <= 12; m++) {
                int x = gridX + (m % 2) * 70;
                if (m % 2 == 0 && m > 0) y += 18;
                final int value = m;
                chip(g, mx, my, x, y, 66, OptionLabels.option(AppearanceSection.TATTOOS, "faceNeck", m), current == m, () -> setSlot(zone().motifSlot(), value));
            }
            return;
        }
        int ink = tattoos()[TattooZone.INK_SLOT];
        int rowsVisible = gridRows(), totalRows = (TattooZone.MOTIFS + 5) / 5;
        scroll = Math.max(0, Math.min(scroll, totalRows - rowsVisible));
        if (totalRows > rowsVisible) {
            int barH = rowsVisible * 28;
            g.fill(gridX + 5 * 28, top, gridX + 5 * 28 + 3, top + barH, MineNorthStyle.LIST);
            int thumb = Math.max(10, barH * rowsVisible / totalRows);
            int thumbY = top + (barH - thumb) * scroll / (totalRows - rowsVisible);
            g.fill(gridX + 5 * 28, thumbY, gridX + 5 * 28 + 3, thumbY + thumb, MineNorthStyle.CYAN);
        }
        for (int m = 0; m <= TattooZone.MOTIFS; m++) {
            int row = m / 5;
            if (row < scroll || row >= scroll + rowsVisible) continue;
            int x = gridX + (m % 5) * 28, y = top + (row - scroll) * 28;
            boolean on = current == m, hover = over(mx, my, x, y, 26, 26);
            g.fill(x, y, x + 26, y + 26, on ? MineNorthStyle.CYAN : hover ? MineNorthStyle.HOVER : MineNorthStyle.LIST);
            g.fill(x + 1, y + 1, x + 25, y + 25, on ? 0xFF16106A : MineNorthStyle.LIST);
            if (m == 0) {
                g.drawString(font, "X", x + 10, y + 9, MineNorthStyle.MUTED, false);
            } else {
                int[] px8 = SkinComposer.designPixels(m, ink);
                for (int i = 0; i < 64; i++) {
                    if (px8[i] == 0) continue;
                    int cx = x + 1 + (i % 8) * 3, cy = y + 1 + (i / 8) * 3;
                    g.fill(cx, cy, cx + 3, cy + 3, px8[i] | 0xFF000000);
                }
            }
            final int value = m;
            hits.add(new Hit(x, y, 26, 26, () -> setSlot(zone().motifSlot(), value)));
        }
    }

    private int scroll;

    /** Lignes de vignettes visibles : le carnet défile à la molette. */
    private int gridRows() { return Math.max(3, (bodyBottom - (bodyTop + 10)) / 28); }

    @Override public boolean mouseScrolled(double mx, double my, double delta) {
        if (zone() != TattooZone.FACE_NECK && over((int) mx, (int) my, gridX, bodyTop + 10, 5 * 28 + 4, gridRows() * 28)) {
            scroll = Math.max(0, scroll - (int) Math.signum(delta));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    private void drawPreview(GuiGraphics g, int mx, int my) {
        int h = bodyBottom - bodyTop - 22;
        g.fillGradient(viewX, bodyTop, viewX + viewW, bodyTop + h, 0xFFFF9F6B, 0xFFFFD9A0);
        if (previewAppearance == null || !state.appearance().equals(previewAppearance) || state.gender() != previewGender) {
            previewAppearance = state.appearance();
            previewGender = state.gender();
            fr.minenorth.identite.client.skin.ClientSkins.preview(previewGender, previewAppearance);
        }
        PreviewRenderer.Cam cam = PreviewRenderer.CAMS[camera];
        int[] hair = state.appearance().get(AppearanceSection.HAIR);
        PreviewRenderer.render(g, viewX, bodyTop, viewW, h, state.gender(), fr.minenorth.identite.client.skin.ClientSkins.previewLocation(),
                cam.yaw() + yaw, cam.zoom(), cam.focus(), hair[0], hair[1]);
        int bw = viewW / VIEW_NAMES.length;
        for (int i = 0; i < VIEW_NAMES.length; i++) {
            final int cam2 = VIEW_CAMS[i];
            chip(g, mx, my, viewX + i * bw, bodyTop + h + 4, bw - 3, VIEW_NAMES[i], camera == cam2, () -> { camera = cam2; yaw = 0; });
        }
    }

    private void drawOptions(GuiGraphics g, int mx, int my) {
        int[] t = tattoos();
        int x = optX, y = bodyTop - 2;
        g.drawString(font, "Zone : " + ZONE_NAMES[zone], x, y, MineNorthStyle.CYAN, false);
        y += 12;
        for (int i = 0; i < ZONES.length; i++) {
            final int index = i;
            chip(g, mx, my, x + (i % 3) * (optW / 3), y + (i / 3) * 18, optW / 3 - 3, ZONE_NAMES[i], zone == i, () -> chooseZone(index));
        }
        y += 58;
        if (zone().hasSize()) {
            y = row(g, mx, my, x, y, "Taille", SIZES, t[zone().sizeSlot()], zone().sizeSlot(), 3);
        }
        y = row(g, mx, my, x, y, "Encre", INKS, t[TattooZone.INK_SLOT], TattooZone.INK_SLOT, 3);
        if (isPlaceable()) drawPad(g, mx, my, x, y);
        if (isArm()) {
            y = row(g, mx, my, x, y, "Face du bras", FACES, t[faceSlot()], faceSlot(), 4);
            row(g, mx, my, x, y, "Hauteur", HEIGHTS, t[heightSlot()], heightSlot(), 3);
        }
    }

    /** Une ligne de choix : libellé puis puces sur {@code perRow} colonnes ; renvoie le y suivant. */
    private int row(GuiGraphics g, int mx, int my, int x, int y, String label, String[] names, int selected, int slot, int perRow) {
        g.drawString(font, label, x, y, MineNorthStyle.MUTED, false);
        y += 10;
        int w = optW / perRow;
        for (int i = 0; i < names.length; i++) {
            final int value = i;
            chip(g, mx, my, x + (i % perRow) * w, y + (i / perRow) * 18, w - 3, names[i], selected == i, () -> setSlot(slot, value));
        }
        return y + ((names.length + perRow - 1) / perRow) * 18 + 6;
    }

    private void chip(GuiGraphics g, int mx, int my, int x, int y, int w, String label, boolean on, Runnable action) {
        boolean hover = over(mx, my, x, y, w, 16);
        g.fill(x, y, x + w, y + 16, on ? MineNorthStyle.CYAN : hover ? MineNorthStyle.HOVER : MineNorthStyle.DARK);
        String text = font.plainSubstrByWidth(label, w - 4);
        g.drawString(font, text, x + (w - font.width(text)) / 2, y + 4, on ? 0xFF06103A : MineNorthStyle.TEXT, false);
        hits.add(new Hit(x, y, w, 16, action));
    }

    private void drawFooter(GuiGraphics g, int mx, int my) {
        int y = py + ph - 24, w = 90, sw = 150;
        long price = fr.minenorth.identite.salon.SalonPrices.quote(fr.minenorth.identite.salon.SalonKind.TATTOO, original, state.appearance());
        int saveX = px + pw - 8 - sw, cancelX = saveX - 6 - w;
        MineNorthStyle.button(g, cancelX, y, w, 20, "Annuler", MineNorthStyle.DARK, over(mx, my, cancelX, y, w, 20), true);
        hits.add(new Hit(cancelX, y, w, 20, this::onClose));
        MineNorthStyle.button(g, saveX, y, sw, 20, price > 0 ? "Tatouer · " + MineNorthStyle.euros(price) : "Tatouer", MineNorthStyle.GREEN, over(mx, my, saveX, y, sw, 20), true);
        hits.add(new Hit(saveX, y, sw, 20, () -> { onSave.accept(state); onClose(); }));
        MineNorthStyle.button(g, px + 8, y, 80, 20, "Défaire", MineNorthStyle.DARK, over(mx, my, px + 8, y, 80, 20), state.canUndo());
        if (state.canUndo()) hits.add(new Hit(px + 8, y, 80, 20, state::undo));
        fr.minenorth.identite.drawing.DrawingKind kind = drawingKind();
        if (kind != null) {
            boolean has = !isBlank(state.appearance().drawings().get(kind));
            String label = (has ? "Mon dessin : " : "Dessiner : ") + ZONE_NAMES[zone];
            int dx = px + 92, dw = Math.min(170, font.width(label) + 16);
            MineNorthStyle.button(g, dx, y, dw, 20, label, MineNorthStyle.PINK, over(mx, my, dx, y, dw, 20), true);
            hits.add(new Hit(dx, y, dw, 20, () -> openDraw(kind)));
        }
    }

    // ------------------------------------------------------------------ entrées

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            if (overPad(mx, my)) {
                placeAt(mx, my);
                return true;
            }
            for (Hit h : hits) {
                if (over((int) mx, (int) my, h.x, h.y, h.w, h.h)) {
                    h.action.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 0 && overPad(mx, my)) {
            placeAt(mx, my);
            return true;
        }
        if (button == 0 && over((int) mx, (int) my, viewX, bodyTop, viewW, bodyBottom - bodyTop - 22)) {
            yaw += (float) dx * 1.5f;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override public boolean isPauseScreen() { return false; }
}
