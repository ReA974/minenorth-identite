package fr.minenorth.identite.client.creator;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.client.MineNorthStyle;
import fr.minenorth.identite.client.skin.ClientSkins;
import fr.minenorth.identite.document.Gender;
import fr.minenorth.identite.skin.Palette;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Salon de coiffure : un grand aperçu du personnage, comme dans un miroir, et à droite la coupe et la barbe avec leur
 * couleur. Les choix s'affichent en direct ; « Valider la coupe » enregistre.
 */
@OnlyIn(Dist.CLIENT)
public final class HairSalonScreen extends Screen {
    private record Hit(int x, int y, int w, int h, Runnable action) {}

    private static final String[] VIEW_NAMES = {"Face", "Profil", "Dos", "Corps"};
    private static final int[] VIEW_CAMS = {2, 4, 3, 0};
    private static final AppearanceSection[] TABS = {AppearanceSection.HAIR, AppearanceSection.FACIAL_HAIR};
    private static final String[] TAB_NAMES = {"Cheveux", "Barbe"};

    private final CreatorState state;
    private final Consumer<CreatorState> onSave;
    private final List<Hit> hits = new ArrayList<>();
    private int tab;
    private int camera = 2;
    private float yaw;
    private Appearance previewAppearance;
    private Gender previewGender;

    private int px, py, pw, ph, bodyTop, bodyBottom, viewX, viewW, optX, optW;

    private HairSalonScreen(CreatorState state, Consumer<CreatorState> onSave) {
        super(Component.literal("Salon de coiffure"));
        this.state = state;
        this.onSave = onSave;
    }

    public static HairSalonScreen open(Gender gender, Appearance current, Consumer<CreatorState> onSave) {
        return new HairSalonScreen(CreatorState.forEditor(gender, current), onSave);
    }

    @Override protected void init() {
        pw = Math.min(width - 8, 520);
        ph = Math.min(height - 8, 290);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        bodyTop = py + 38;
        bodyBottom = py + ph - 30;
        viewX = px + 8;
        viewW = Math.min(190, pw / 2 - 20);
        optX = viewX + viewW + 10;
        optW = px + pw - 8 - optX;
    }

    private AppearanceSection section() { return TABS[tab]; }

    private int[] values() { return state.appearance().get(section()); }

    private void set(int slot, int value) {
        int[] v = values();
        v[slot] = value;
        state.set(section(), v);
    }

    private boolean over(int mx, int my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        hits.clear();
        MineNorthStyle.panel(g, px, py, pw, ph, "MINENORTHRP - SALON DE COIFFURE", "Choisissez une coupe, une couleur et une barbe.");
        drawMirror(g, mx, my);
        drawOptions(g, mx, my);
        drawFooter(g, mx, my);
        super.render(g, mx, my, partial);
    }

    private void drawMirror(GuiGraphics g, int mx, int my) {
        int h = bodyBottom - bodyTop - 22;
        g.fill(viewX - 2, bodyTop - 2, viewX + viewW + 2, bodyTop + h + 2, 0xFFB08A40);
        g.fillGradient(viewX, bodyTop, viewX + viewW, bodyTop + h, 0xFFFFC38A, 0xFFFFE6B8);
        if (previewAppearance == null || !state.appearance().equals(previewAppearance) || state.gender() != previewGender) {
            previewAppearance = state.appearance();
            previewGender = state.gender();
            ClientSkins.preview(previewGender, previewAppearance);
        }
        PreviewRenderer.Cam cam = PreviewRenderer.CAMS[camera];
        int[] hair = state.appearance().get(AppearanceSection.HAIR);
        PreviewRenderer.render(g, viewX, bodyTop, viewW, h, state.gender(), ClientSkins.previewLocation(),
                cam.yaw() + yaw, cam.zoom(), cam.focus(), hair[0], hair[1]);
        int bw = viewW / VIEW_NAMES.length;
        for (int i = 0; i < VIEW_NAMES.length; i++) {
            final int c = VIEW_CAMS[i];
            chip(g, mx, my, viewX + i * bw, bodyTop + h + 6, bw - 3, VIEW_NAMES[i], camera == c, () -> { camera = c; yaw = 0; });
        }
    }

    private void drawOptions(GuiGraphics g, int mx, int my) {
        int tw = optW / 2;
        for (int i = 0; i < TABS.length; i++) {
            final int index = i;
            chip(g, mx, my, optX + i * tw, bodyTop - 2, tw - 3, TAB_NAMES[i], tab == i, () -> tab = index);
        }
        int y = bodyTop + 22;
        g.drawString(font, tab == 0 ? "Coupe" : "Style de barbe", optX, y, MineNorthStyle.CYAN, false);
        y += 11;
        Catalog.Slot style = Catalog.slots(section()).get(0);
        int[] v = values();
        int cols = 3, w = optW / cols, n = 0;
        for (int i = 0; i < style.options(); i++) {
            if (!style.allows(state.gender(), i)) continue;
            final int value = i;
            chip(g, mx, my, optX + (n % cols) * w, y + (n / cols) * 18, w - 3,
                    OptionLabels.option(section(), "style", i), v[0] == i, () -> set(0, value));
            n++;
        }
        y += ((n + cols - 1) / cols) * 18 + 6;
        g.drawString(font, "Couleur", optX, y, MineNorthStyle.CYAN, false);
        y += 11;
        int colors = Catalog.slots(section()).get(1).options();
        int sw = Math.min(22, optW / colors);
        for (int i = 0; i < colors; i++) {
            int x = optX + i * sw;
            boolean on = v[1] == i;
            g.fill(x, y, x + sw - 2, y + 18, on ? MineNorthStyle.WHITE : over(mx, my, x, y, sw - 2, 18) ? MineNorthStyle.CYAN : MineNorthStyle.FRAME);
            g.fill(x + 1, y + 1, x + sw - 3, y + 17, Palette.hairColor(i) | 0xFF000000);
            final int value = i;
            hits.add(new Hit(x, y, sw - 2, 18, () -> set(1, value)));
        }
    }

    private void chip(GuiGraphics g, int mx, int my, int x, int y, int w, String label, boolean on, Runnable action) {
        boolean hover = over(mx, my, x, y, w, 16);
        g.fill(x, y, x + w, y + 16, on ? MineNorthStyle.CYAN : hover ? MineNorthStyle.HOVER : MineNorthStyle.DARK);
        String text = font.plainSubstrByWidth(label, w - 4);
        g.drawString(font, text, x + (w - font.width(text)) / 2, y + 4, on ? 0xFF06103A : MineNorthStyle.TEXT, false);
        hits.add(new Hit(x, y, w, 16, action));
    }

    private void drawFooter(GuiGraphics g, int mx, int my) {
        int y = py + ph - 24, w = 110;
        int cancelX = px + pw - 8 - w - 6 - 90, saveX = px + pw - 8 - w;
        MineNorthStyle.button(g, cancelX, y, 90, 20, "Annuler", MineNorthStyle.DARK, over(mx, my, cancelX, y, 90, 20), true);
        hits.add(new Hit(cancelX, y, 90, 20, this::onClose));
        MineNorthStyle.button(g, saveX, y, w, 20, "Valider la coupe", MineNorthStyle.GREEN, over(mx, my, saveX, y, w, 20), true);
        hits.add(new Hit(saveX, y, w, 20, () -> { onSave.accept(state); onClose(); }));
        MineNorthStyle.button(g, px + 8, y, 70, 20, "Défaire", MineNorthStyle.DARK, over(mx, my, px + 8, y, 70, 20), state.canUndo());
        if (state.canUndo()) hits.add(new Hit(px + 8, y, 70, 20, state::undo));
    }

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
