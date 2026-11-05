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
 * Salon de coiffure : un grand aperçu du personnage, comme dans un miroir, et à droite la couleur (toujours visible) puis la
 * liste des coupes ou des barbes, qui défile à la molette. Les choix s'affichent en direct ; « Valider » enregistre et paie.
 */
@OnlyIn(Dist.CLIENT)
public final class HairSalonScreen extends Screen {
    private static final SalonTheme T = SalonTheme.HAIR;

    private record Hit(int x, int y, int w, int h, Runnable action) {}

    /** Vues du miroir : {libellé, rotation, zoom, hauteur visée}. On voit toute la tête et les épaules, pas seulement le visage. */
    private static final String[] VIEW_NAMES = {"Face", "Profil", "Dos", "Corps"};
    private static final float[][] VIEWS = {{0, 2.0f, 1.45f}, {70, 2.0f, 1.45f}, {180, 2.0f, 1.45f}, {0, 1.0f, 1.0f}};
    private static final AppearanceSection[] TABS = {AppearanceSection.HAIR, AppearanceSection.FACIAL_HAIR};
    private static final String[] TAB_NAMES = {"Cheveux", "Barbe"};

    private final CreatorState state;
    private final Consumer<CreatorState> onSave;
    private final Appearance original;
    private final Screen parent;
    private final boolean free;
    private final List<Hit> hits = new ArrayList<>();
    private int tab;
    private int view;
    private int scroll;
    private float yaw;
    private Appearance previewAppearance;
    private Gender previewGender;

    private int px, py, pw, ph, bodyTop, bodyBottom, viewX, viewW, optX, optW, listTop;

    private HairSalonScreen(CreatorState state, Consumer<CreatorState> onSave, Screen parent, boolean free, int tab) {
        super(Component.literal("Salon de coiffure"));
        this.parent = parent;
        this.free = free;
        this.tab = tab;
        this.state = state;
        this.onSave = onSave;
        this.original = state.appearance();
    }

    public static HairSalonScreen open(Gender gender, Appearance current, Consumer<CreatorState> onSave) {
        return new HairSalonScreen(CreatorState.forEditor(gender, current), onSave, null, false, 0);
    }

    /** Le même miroir, ouvert depuis la création du personnage ou /cidedit : gratuit, et on revient à l'écran d'origine. */
    public static HairSalonScreen forCreator(Gender gender, Appearance current, Screen parent, int tab, Consumer<CreatorState> onSave) {
        return new HairSalonScreen(CreatorState.forEditor(gender, current), onSave, parent, true, tab);
    }

    public boolean belongsToCreation() { return parent instanceof CreatorScreen c && c.isCreation(); }

    @Override public void onClose() {
        if (parent != null) minecraft.setScreen(parent);
        else super.onClose();
    }

    @Override protected void init() {
        pw = Math.min(width - 8, 540);
        ph = Math.min(height - 8, 300);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        bodyTop = py + 50;
        bodyBottom = py + ph - 30;
        viewX = px + 8;
        viewW = Math.min(210, pw * 40 / 100);
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
        T.panel(g, px, py, pw, ph, "MINENORTHRP - SALON DE COIFFURE", free ? "Choisissez une coupe, une couleur et une barbe." : "Choisissez une coupe, une couleur et une barbe. Paiement par carte.");
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
        float[] v = VIEWS[view];
        int[] hair = state.appearance().get(AppearanceSection.HAIR);
        PreviewRenderer.render(g, viewX, bodyTop, viewW, h, state.gender(), ClientSkins.previewLocation(),
                v[0] + yaw, v[1], v[2], hair[0], hair[1]);
        int bw = viewW / VIEW_NAMES.length;
        for (int i = 0; i < VIEW_NAMES.length; i++) {
            final int index = i;
            chip(g, mx, my, viewX + i * bw, bodyTop + h + 6, bw - 3, VIEW_NAMES[i], view == i, () -> { view = index; yaw = 0; });
        }
    }

    private void drawOptions(GuiGraphics g, int mx, int my) {
        int tw = optW / 2;
        for (int i = 0; i < TABS.length; i++) {
            final int index = i;
            chip(g, mx, my, optX + i * tw, py + 46, tw - 3, TAB_NAMES[i], tab == i, () -> { tab = index; scroll = 0; });
        }
        int[] v = values();
        // la couleur d'abord : toujours visible, jamais sous les boutons
        int y = py + 68;
        g.drawString(font, "Couleur", optX, y, T.accent, false);
        y += 11;
        int colors = Catalog.slots(section()).get(1).options();
        int sw = Math.min(22, optW / colors);
        for (int i = 0; i < colors; i++) {
            int x = optX + i * sw;
            boolean on = v[1] == i;
            g.fill(x, y, x + sw - 2, y + 18, on ? MineNorthStyle.WHITE : over(mx, my, x, y, sw - 2, 18) ? T.accent : T.frame);
            g.fill(x + 1, y + 1, x + sw - 3, y + 17, Palette.hairColor(i) | 0xFF000000);
            final int value = i;
            hits.add(new Hit(x, y, sw - 2, 18, () -> set(1, value)));
        }
        y += 26;
        int titleY = y;
        y += 11;
        listTop = y;
        Catalog.Slot style = Catalog.slots(section()).get(0);
        List<Integer> allowed = new ArrayList<>();
        for (int i = 0; i < style.options(); i++) if (style.allows(state.gender(), i)) allowed.add(i);
        int cols = 4, w = (optW - 8) / cols, th = 68;
        int rowsVisible = Math.max(1, (bodyBottom - listTop) / (th + 3)), totalRows = (allowed.size() + cols - 1) / cols;
        scroll = Math.max(0, Math.min(scroll, totalRows - rowsVisible));
        String hover = null;
        for (int n = 0; n < allowed.size(); n++) {
            int row = n / cols;
            if (row < scroll || row >= scroll + rowsVisible) continue;
            final int value = allowed.get(n);
            int tx = optX + (n % cols) * w, ty = listTop + (row - scroll) * (th + 3);
            boolean selected = v[0] == value, hovered = over(mx, my, tx, ty, w - 3, th);
            g.fill(tx, ty, tx + w - 3, ty + th, selected ? T.accent : hovered ? T.hover : T.chip);
            int[] vals = values();
            vals[0] = value;
            Appearance shown = state.appearance().with(section(), vals);
            var texture = ClientSkins.tile(state.gender().name() + shown.toTag(), state.gender(), shown);
            int[] hairNow = shown.get(AppearanceSection.HAIR);
            g.fillGradient(tx + 2, ty + 2, tx + w - 5, ty + th - 13, 0xFFFFC38A, 0xFFFFE6B8);
            PreviewRenderer.render(g, tx + 2, ty + 2, w - 7, th - 15, state.gender(), texture, 0, 2.5f, 1.55f, hairNow[0], hairNow[1]);
            String name = OptionLabels.option(section(), "style", value);
            String text = font.plainSubstrByWidth(name, w - 9);
            g.drawString(font, text, tx + 3, ty + th - 11, selected ? T.onAccent : T.text, false);
            if (hovered) hover = name;
            hits.add(new Hit(tx, ty, w - 3, th, () -> set(0, value)));
        }
        String current = hover != null ? hover : OptionLabels.option(section(), "style", v[0]);
        g.drawString(font, (tab == 0 ? "Coupe" : "Style de barbe") + " : " + current, optX, titleY, T.accent, false);
        if (totalRows > rowsVisible) {
            int barH = rowsVisible * (th + 3) - 3, bx = optX + optW - 3;
            g.fill(bx, listTop, bx + 3, listTop + barH, T.list);
            int thumb = Math.max(10, barH * rowsVisible / totalRows);
            int thumbY = listTop + (barH - thumb) * scroll / (totalRows - rowsVisible);
            g.fill(bx, thumbY, bx + 3, thumbY + thumb, T.accent);
        }
    }

    @Override public boolean mouseScrolled(double mx, double my, double delta) {
        if (over((int) mx, (int) my, optX, listTop, optW, bodyBottom - listTop)) {
            scroll = Math.max(0, scroll - (int) Math.signum(delta));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    private void chip(GuiGraphics g, int mx, int my, int x, int y, int w, String label, boolean on, Runnable action) {
        boolean hover = over(mx, my, x, y, w, 16);
        g.fill(x, y, x + w, y + 16, on ? T.accent : hover ? T.hover : T.chip);
        String text = font.plainSubstrByWidth(label, w - 4);
        g.drawString(font, text, x + (w - font.width(text)) / 2, y + 4, on ? T.onAccent : T.text, false);
        hits.add(new Hit(x, y, w, 16, action));
    }

    private void drawFooter(GuiGraphics g, int mx, int my) {
        int y = py + ph - 24, w = 170;
        long price = free ? 0 : fr.minenorth.identite.salon.SalonPrices.quote(fr.minenorth.identite.salon.SalonKind.HAIR, original, state.appearance());
        int saveX = px + pw - 8 - w, cancelX = saveX - 6 - 90;
        MineNorthStyle.button(g, cancelX, y, 90, 20, "Annuler", T.chip, over(mx, my, cancelX, y, 90, 20), true);
        hits.add(new Hit(cancelX, y, 90, 20, this::onClose));
        MineNorthStyle.button(g, saveX, y, w, 20, price > 0 ? "Valider · " + MineNorthStyle.euros(price) : free ? "Valider" : "Valider la coupe", T.confirm, over(mx, my, saveX, y, w, 20), true);
        hits.add(new Hit(saveX, y, w, 20, () -> { onSave.accept(state); onClose(); }));
        MineNorthStyle.button(g, px + 8, y, 70, 20, "Défaire", T.chip, over(mx, my, px + 8, y, 70, 20), state.canUndo());
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

    @Override public void removed() {
        ClientSkins.releaseTiles();
        super.removed();
    }

    @Override public boolean isPauseScreen() { return false; }
}
