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
            TattooZone.RIGHT_ARM, TattooZone.LEFT_ARM, TattooZone.LEGS};
    private static final String[] ZONE_NAMES = {"Visage", "Torse", "Dos", "Bras droit", "Bras gauche", "Jambes"};
    /** Caméra de l'aperçu pour chaque zone (indices de PreviewRenderer.CAMS). */
    private static final int[] ZONE_CAMS = {2, 1, 3, 5, 4, 6};
    private static final String[] VIEW_NAMES = {"Face", "Dos", "Bras D", "Bras G"};
    private static final int[] VIEW_CAMS = {0, 3, 5, 4};
    private static final String[] SIZES = {"Petit", "Moyen", "Grand"};
    private static final String[] INKS = {"Couleurs", "Noir", "Gris"};
    private static final String[] FACES = {"Avant", "Ext.", "Int.", "Arrière"};
    private static final String[] HEIGHTS = {"Centré", "Épaule", "Bras", "Avant-bras", "Poignet"};

    private final CreatorState state;
    private final Consumer<CreatorState> onSave;
    private final List<Hit> hits = new ArrayList<>();
    private int zone = 1;
    private int camera = 1;
    private float yaw;
    private Appearance previewAppearance;
    private Gender previewGender;

    private int px, py, pw, ph, bodyTop, bodyBottom, gridX, viewX, viewW, optX, optW;

    private TattooSalonScreen(CreatorState state, Consumer<CreatorState> onSave) {
        super(Component.literal("Salon de tatouage"));
        this.state = state;
        this.onSave = onSave;
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

    private TattooZone zone() { return ZONES[zone]; }

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
        MineNorthStyle.panel(g, px, py, pw, ph, "MINENORTHRP - SALON DE TATOUAGE", "Choisissez un motif, une zone, puis tatouez.");
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
        for (int m = 0; m <= TattooZone.MOTIFS; m++) {
            int x = gridX + (m % 5) * 28, y = top + (m / 5) * 28;
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
        y += 40;
        if (zone().hasSize()) {
            y = row(g, mx, my, x, y, "Taille", SIZES, t[zone().sizeSlot()], zone().sizeSlot(), 3);
        }
        y = row(g, mx, my, x, y, "Encre", INKS, t[TattooZone.INK_SLOT], TattooZone.INK_SLOT, 3);
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
        int y = py + ph - 24, w = 90;
        int cancelX = px + pw - 8 - 2 * w - 6, saveX = px + pw - 8 - w;
        MineNorthStyle.button(g, cancelX, y, w, 20, "Annuler", MineNorthStyle.DARK, over(mx, my, cancelX, y, w, 20), true);
        hits.add(new Hit(cancelX, y, w, 20, this::onClose));
        MineNorthStyle.button(g, saveX, y, w, 20, "Tatouer", MineNorthStyle.GREEN, over(mx, my, saveX, y, w, 20), true);
        hits.add(new Hit(saveX, y, w, 20, () -> { onSave.accept(state); onClose(); }));
        MineNorthStyle.button(g, px + 8, y, 80, 20, "Défaire", MineNorthStyle.DARK, over(mx, my, px + 8, y, 80, 20), state.canUndo());
        if (state.canUndo()) hits.add(new Hit(px + 8, y, 80, 20, state::undo));
    }

    // ------------------------------------------------------------------ entrées

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
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
        if (button == 0 && over((int) mx, (int) my, viewX, bodyTop, viewW, bodyBottom - bodyTop - 22)) {
            yaw += (float) dx * 1.5f;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override public boolean isPauseScreen() { return false; }
}
