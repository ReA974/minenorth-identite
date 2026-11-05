package fr.minenorth.identite.client.creator;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.client.MineNorthStyle;
import fr.minenorth.identite.client.skin.ClientSkins;
import fr.minenorth.identite.document.Gender;
import fr.minenorth.identite.salon.SalonKind;
import fr.minenorth.identite.salon.SalonPrices;
import fr.minenorth.identite.skin.OptionPreview;
import fr.minenorth.identite.skin.Palette;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Salon de maquillage : un grand miroir à gauche, et à droite quatre onglets (lèvres, yeux, joues, peau) avec la couleur
 * puis les styles montrés tels qu'ils seront sur le visage, en haute définition. Les choix s'affichent en direct ;
 * « Valider » enregistre et paie.
 */
@OnlyIn(Dist.CLIENT)
public final class MakeupSalonScreen extends Screen {
    private static final SalonTheme T = SalonTheme.MAKEUP;

    private record Hit(int x, int y, int w, int h, Runnable action) {}

    /** Un onglet : titre, section, réglage du style, réglage de la couleur (-1 si aucune), clé de la couleur, libellé de la couleur. */
    private record Category(String title, AppearanceSection section, int styleSlot, int colorSlot, String colorKey, String colorLabel) {}

    private static final Category[] CATEGORIES = {
            new Category("Lèvres", AppearanceSection.MAKEUP, 0, 1, "lipColor", "Couleur des lèvres"),
            new Category("Yeux", AppearanceSection.MAKEUP, 2, 3, "eyeColor", "Couleur du fard"),
            new Category("Joues", AppearanceSection.MAKEUP, 4, 5, "blushColor", "Couleur du blush"),
            new Category("Peau", AppearanceSection.SKIN, 1, 0, "tone", "Teint")};

    private static final String[] VIEW_NAMES = {"Face", "Gros plan", "Profil"};
    private static final float[][] VIEWS = {{0, 3.0f, 1.7f}, {0, 5.2f, 1.74f}, {65, 3.0f, 1.7f}};

    private final CreatorState state;
    private final Consumer<CreatorState> onSave;
    private final Appearance original;
    private final List<Hit> hits = new ArrayList<>();
    private final Map<String, OptionPreview.Tile> tiles = new HashMap<>();
    private Appearance tilesFor;
    private Gender tilesGender;
    private int category;
    private int view;
    private int scroll;
    private float yaw;
    private Appearance previewAppearance;
    private Gender previewGender;

    private int px, py, pw, ph, bodyTop, bodyBottom, viewX, viewW, optX, optW, listTop;

    private MakeupSalonScreen(CreatorState state, Consumer<CreatorState> onSave) {
        super(Component.literal("Salon de maquillage"));
        this.state = state;
        this.onSave = onSave;
        this.original = state.appearance();
    }

    public static MakeupSalonScreen open(Gender gender, Appearance current, Consumer<CreatorState> onSave) {
        return new MakeupSalonScreen(CreatorState.forEditor(gender, current), onSave);
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

    private Category cat() { return CATEGORIES[category]; }

    private int[] values() { return state.appearance().get(cat().section()); }

    private void set(int slot, int value) {
        int[] v = values();
        v[slot] = value;
        state.set(cat().section(), v);
    }

    private boolean over(int mx, int my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        hits.clear();
        T.panel(g, px, py, pw, ph, "MINENORTHRP - SALON DE MAQUILLAGE", "Choisissez un maquillage, une couleur ou un teint. Paiement par carte.");
        drawMirror(g, mx, my);
        drawOptions(g, mx, my);
        drawFooter(g, mx, my);
        super.render(g, mx, my, partial);
    }

    private void drawMirror(GuiGraphics g, int mx, int my) {
        int h = bodyBottom - bodyTop - 22;
        g.fill(viewX - 2, bodyTop - 2, viewX + viewW + 2, bodyTop + h + 2, 0xFFD8A24A);
        g.fillGradient(viewX, bodyTop, viewX + viewW, bodyTop + h, 0xFFFFC9A0, 0xFFFFE8CC);
        if (previewAppearance == null || !state.appearance().equals(previewAppearance) || state.gender() != previewGender) {
            previewAppearance = state.appearance();
            previewGender = state.gender();
            ClientSkins.preview(previewGender, previewAppearance);
        }
        float[] v = VIEWS[view];
        PreviewRenderer.render(g, viewX, bodyTop, viewW, h, state.gender(), ClientSkins.previewLocation(), v[0] + yaw, v[1], v[2], state.appearance());
        int bw = viewW / VIEW_NAMES.length;
        for (int i = 0; i < VIEW_NAMES.length; i++) {
            final int index = i;
            chip(g, mx, my, viewX + i * bw, bodyTop + h + 6, bw - 3, VIEW_NAMES[i], view == i, () -> { view = index; yaw = 0; });
        }
    }

    private void drawOptions(GuiGraphics g, int mx, int my) {
        int tw = optW / CATEGORIES.length;
        for (int i = 0; i < CATEGORIES.length; i++) {
            final int index = i;
            chip(g, mx, my, optX + i * tw, py + 46, tw - 3, CATEGORIES[i].title(), category == i, () -> { category = index; scroll = 0; });
        }
        Category c = cat();
        int[] v = values();
        Catalog.Slot colorSlot = Catalog.slots(c.section()).get(c.colorSlot());
        // la couleur d'abord : toujours visible
        int y = py + 68;
        g.drawString(font, c.colorLabel(), optX, y, T.accent, false);
        y += 11;
        int colors = colorSlot.options();
        int sw = Math.min(24, (optW - 4) / colors);
        for (int i = 0; i < colors; i++) {
            int x = optX + i * sw;
            boolean on = v[c.colorSlot()] == i;
            g.fill(x, y, x + sw - 2, y + 18, on ? MineNorthStyle.WHITE : over(mx, my, x, y, sw - 2, 18) ? T.accent : T.frame);
            g.fill(x + 1, y + 1, x + sw - 3, y + 17, swatch(c.colorKey(), i) | 0xFF000000);
            final int value = i;
            hits.add(new Hit(x, y, sw - 2, 18, () -> set(c.colorSlot(), value)));
        }
        y += 26;
        int titleY = y;
        y += 11;
        listTop = y;
        Catalog.Slot style = Catalog.slots(c.section()).get(c.styleSlot());
        String key = style.key();
        List<Integer> allowed = new ArrayList<>();
        for (int i = 0; i < style.options(); i++) if (style.allows(state.gender(), i)) allowed.add(i);
        refreshTiles();
        int cols = 4, w = (optW - 8) / cols, th = 52;
        int rowsVisible = Math.max(1, (bodyBottom - listTop) / (th + 3)), totalRows = (allowed.size() + cols - 1) / cols;
        scroll = Math.max(0, Math.min(scroll, totalRows - rowsVisible));
        String hover = null;
        for (int n = 0; n < allowed.size(); n++) {
            int row = n / cols;
            if (row < scroll || row >= scroll + rowsVisible) continue;
            final int value = allowed.get(n);
            int tx = optX + (n % cols) * w, ty = listTop + (row - scroll) * (th + 3);
            boolean selected = v[c.styleSlot()] == value, hovered = over(mx, my, tx, ty, w - 3, th);
            g.fill(tx, ty, tx + w - 3, ty + th, selected ? T.accent : hovered ? T.hover : T.chip);
            g.fill(tx + 1, ty + 1, tx + w - 4, ty + th - 1, 0xFF1A0C18);
            var tile = tiles.computeIfAbsent(c.section().name() + c.styleSlot() + ":" + value,
                    k -> OptionPreview.of(state.gender(), state.appearance(), c.section(), c.styleSlot(), key, value));
            float pixel = Math.min(w - 9, th - 6) / (float) tile.w();
            int ox = tx + (w - 3 - Math.round(pixel * tile.w())) / 2, oy = ty + 3;
            for (int j = 0; j < tile.h(); j++) {
                for (int i = 0; i < tile.w(); i++) {
                    int argb = tile.argb()[j * tile.w() + i];
                    if (argb != 0) g.fill(ox + Math.round(i * pixel), oy + Math.round(j * pixel), ox + Math.round((i + 1) * pixel), oy + Math.round((j + 1) * pixel), argb);
                }
            }
            if (hovered) hover = OptionLabels.option(c.section(), key, value);
            hits.add(new Hit(tx, ty, w - 3, th, () -> set(c.styleSlot(), value)));
        }
        String current = hover != null ? hover : OptionLabels.option(c.section(), key, v[c.styleSlot()]);
        g.drawString(font, OptionLabels.slot(c.section(), key) + " : " + current, optX, titleY, T.accent, false);
        if (totalRows > rowsVisible) {
            int barH = rowsVisible * (th + 3) - 3, bx = optX + optW - 3;
            g.fill(bx, listTop, bx + 3, listTop + barH, T.list);
            int thumb = Math.max(10, barH * rowsVisible / totalRows);
            int thumbY = listTop + (barH - thumb) * scroll / (totalRows - rowsVisible);
            g.fill(bx, thumbY, bx + 3, thumbY + thumb, T.accent);
        }
    }

    private void refreshTiles() {
        if (tilesFor == null || !state.appearance().equals(tilesFor) || tilesGender != state.gender()) {
            tiles.clear();
            tilesFor = state.appearance();
            tilesGender = state.gender();
        }
    }

    private static int swatch(String key, int i) {
        return switch (key) {
            case "eyeColor" -> Palette.eyeColor(i);
            case "tone" -> Palette.skinTone(i);
            default -> Palette.lipColor(i);
        };
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
        long price = SalonPrices.quote(SalonKind.MAKEUP, original, state.appearance());
        int saveX = px + pw - 8 - w, cancelX = saveX - 6 - 90;
        MineNorthStyle.button(g, cancelX, y, 90, 20, "Annuler", T.chip, over(mx, my, cancelX, y, 90, 20), true);
        hits.add(new Hit(cancelX, y, 90, 20, this::onClose));
        MineNorthStyle.button(g, saveX, y, w, 20, price > 0 ? "Valider · " + MineNorthStyle.euros(price) : "Valider", T.confirm, over(mx, my, saveX, y, w, 20), true);
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

    @Override public boolean isPauseScreen() { return false; }
}
