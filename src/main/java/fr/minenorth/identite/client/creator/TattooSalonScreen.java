package fr.minenorth.identite.client.creator;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.TattooZone;
import fr.minenorth.identite.client.MineNorthStyle;
import fr.minenorth.identite.client.skin.ClientSkins;
import fr.minenorth.identite.document.Gender;
import fr.minenorth.identite.drawing.DrawingKind;
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
 * Salon de tatouage. À gauche le carnet de motifs, au centre le personnage (qui suit la zone choisie), à droite une carte
 * du corps cliquable qui montre les tatouages déjà posés et la zone choisie, puis les réglages de cette zone (taille,
 * encre, face et hauteur sur un bras, position sur le torse et le dos). Les choix s'affichent en direct ; « Tatouer »
 * enregistre et paie.
 */
@OnlyIn(Dist.CLIENT)
public final class TattooSalonScreen extends Screen {
    private static final SalonTheme T = SalonTheme.TATTOO;

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
    private static final String[] LEG_HEIGHTS = {"Centré", "Haut", "Cuisse", "Mollet", "Cheville"};
    private static final int[] ARM_START = {-1, 0, 3, 6, 8};
    private static final int SKIN = 0xFF9A6C50, SKIN_DARK = 0xFF6E4A36;

    private final CreatorState state;
    private final Consumer<CreatorState> onSave;
    private final Appearance original;
    private final Screen parent;
    private final boolean free;
    private final List<Hit> hits = new ArrayList<>();
    private int zone = 1;
    private int camera = 1;
    private float yaw;
    private int scroll;
    private boolean mapBack;
    private boolean underwear = true;
    private Appearance previewAppearance;
    private Gender previewGender;

    private int padX, padY;
    private int px, py, pw, ph, bodyTop, bodyBottom, gridX, viewX, viewW, optX, optW;

    private TattooSalonScreen(CreatorState state, Consumer<CreatorState> onSave, Screen parent, boolean free) {
        super(Component.literal("Salon de tatouage"));
        this.parent = parent;
        this.free = free;
        this.state = state;
        this.onSave = onSave;
        this.original = state.appearance();
    }

    public static TattooSalonScreen open(Gender gender, Appearance current, Consumer<CreatorState> onSave) {
        return new TattooSalonScreen(CreatorState.forEditor(gender, current), onSave, null, false);
    }

    /** Le même carnet, ouvert depuis la création du personnage ou /cidedit : gratuit, et on revient à l'écran d'origine. */
    public static TattooSalonScreen forCreator(Gender gender, Appearance current, Screen parent, Consumer<CreatorState> onSave) {
        return new TattooSalonScreen(CreatorState.forEditor(gender, current), onSave, parent, true);
    }

    /** Vrai si ce carnet a été ouvert depuis la création du personnage (le serveur ne doit pas la rouvrir par-dessus). */
    public boolean belongsToCreation() { return parent instanceof CreatorScreen c && c.isCreation(); }

    @Override public void onClose() {
        if (parent != null) minecraft.setScreen(parent);
        else super.onClose();
    }

    @Override protected void init() {
        pw = Math.min(width - 8, 560);
        ph = Math.min(height - 8, 300);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        bodyTop = py + 58;
        bodyBottom = py + ph - 30;
        gridX = px + 8;
        optW = pw >= 540 ? 254 : Math.max(180, pw * 42 / 100);
        optX = px + pw - 8 - optW;
        viewX = gridX + 5 * 28 + 12;
        viewW = Math.max(100, optX - 8 - viewX);
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

    /** Bras et jambes : face et hauteur au choix. */
    private boolean isLimb() { return isArm() || zone() == TattooZone.RIGHT_LEG || zone() == TattooZone.LEFT_LEG; }

    private boolean isPlaceable() { return zone() == TattooZone.TORSO || zone() == TattooZone.BACK; }

    private int faceSlot() { return faceSlotOf(zone()); }

    private static int faceSlotOf(TattooZone z) {
        return switch (z) {
            case RIGHT_ARM -> TattooZone.RIGHT_ARM_FACE_SLOT;
            case LEFT_ARM -> TattooZone.LEFT_ARM_FACE_SLOT;
            case RIGHT_LEG -> TattooZone.RIGHT_LEG_FACE_SLOT;
            default -> TattooZone.LEFT_LEG_FACE_SLOT;
        };
    }

    private static int heightSlotOf(TattooZone z) {
        return switch (z) {
            case RIGHT_ARM -> TattooZone.RIGHT_ARM_HEIGHT_SLOT;
            case LEFT_ARM -> TattooZone.LEFT_ARM_HEIGHT_SLOT;
            case RIGHT_LEG -> TattooZone.RIGHT_LEG_HEIGHT_SLOT;
            default -> TattooZone.LEFT_LEG_HEIGHT_SLOT;
        };
    }

    private int heightSlot() { return heightSlotOf(zone()); }

    private int xSlot() { return zone() == TattooZone.TORSO ? TattooZone.TORSO_X_SLOT : TattooZone.BACK_X_SLOT; }

    private int ySlot() { return zone() == TattooZone.TORSO ? TattooZone.TORSO_Y_SLOT : TattooZone.BACK_Y_SLOT; }

    private void chooseZone(int index) {
        zone = index;
        camera = ZONE_CAMS[index];
        yaw = 0;
        if (ZONES[index] == TattooZone.BACK) mapBack = true;
        else if (ZONES[index] == TattooZone.TORSO) mapBack = false;
    }

    /** Placement d'un tatouage sur sa zone, comme le calcule le skin : {colonne, ligne, largeur, hauteur} en pixels. */
    private static int[] placement(TattooZone z, int[] t) {
        int size = z.hasSize() ? t[z.sizeSlot()] : 0;
        boolean wide = z == TattooZone.TORSO || z == TattooZone.BACK;
        int w = wide ? (size == 0 ? 4 : size == 1 ? 6 : 8) : 4;
        int h = size == 0 ? 4 : size == 1 ? 6 : 8;
        int ox = wide ? (8 - w) / 2 : 0;
        int oy = size == 0 ? (wide ? 3 : 4) : size == 1 ? 3 : 2;
        if (wide) {
            int xs = t[z == TattooZone.TORSO ? TattooZone.TORSO_X_SLOT : TattooZone.BACK_X_SLOT];
            int ys = t[z == TattooZone.TORSO ? TattooZone.TORSO_Y_SLOT : TattooZone.BACK_Y_SLOT];
            if (xs > 0) ox = Math.min(xs - 1, 8 - w);
            if (ys > 0) oy = Math.min(ys - 1, 12 - h);
        } else if (z != TattooZone.FACE_NECK) {
            int hs = t[heightSlotOf(z)];
            if (hs > 0) oy = Math.min(ARM_START[hs], 12 - h);
        }
        return new int[]{ox, oy, w, h};
    }

    private int span() { return 4 + 2 * tattoos()[zone().sizeSlot()]; }

    private int ox() { return placement(zone(), tattoos())[0]; }

    private int oy() { return placement(zone(), tattoos())[1]; }

    private boolean overPad(double mx, double my) { return isPlaceable() && over((int) mx, (int) my, padX, padY, 48, 72); }

    /** Place le centre du tatouage sur la case cliquée du schéma. */
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

    /** Le dessin libre d'une zone (le visage n'en a pas : seulement de petites marques). */
    private static DrawingKind drawingKind(TattooZone z) {
        return switch (z) {
            case FACE_NECK -> null;
            case TORSO -> DrawingKind.TATTOO_TORSO;
            case BACK -> DrawingKind.TATTOO_BACK;
            case RIGHT_ARM -> DrawingKind.TATTOO_RIGHT_ARM;
            case LEFT_ARM -> DrawingKind.TATTOO_LEFT_ARM;
            case RIGHT_LEG -> DrawingKind.TATTOO_LEGS;
            case LEFT_LEG -> DrawingKind.TATTOO_LEFT_LEG;
        };
    }

    private static boolean isBlank(byte[] grid) {
        for (byte b : grid) if (b != 0) return false;
        return true;
    }

    /** Ouvre l'éditeur de pixels sur le dessin de la zone ; la grille montre ce que le catalogue dessine déjà à cet endroit. */
    private void openDraw(DrawingKind kind) {
        Appearance a = state.appearance();
        Appearance without = a.withDrawings(a.drawings().with(kind, new byte[kind.width() * kind.height()]));
        int[] underlay = fr.minenorth.identite.drawing.DrawingRegions.read(
                SkinComposer.compose(state.gender(), without), kind, state.gender().slimArms());
        minecraft.setScreen(new DrawScreen(this, kind, a.drawings().get(kind), underlay, grid -> state.setDrawing(kind, grid)));
    }

    // ------------------------------------------------------------------ dessin

    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        hits.clear();
        T.panel(g, px, py, pw, ph, "MINENORTHRP - SALON DE TATOUAGE", free ? "Choisissez un motif, une zone, puis validez." : "Choisissez un motif, une zone, puis tatouez. Paiement par carte.");
        drawGrid(g, mx, my);
        drawPreview(g, mx, my);
        drawOptions(g, mx, my);
        drawFooter(g, mx, my);
        super.render(g, mx, my, partial);
    }

    private boolean over(int mx, int my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    private int gridRows() { return Math.max(3, (bodyBottom - bodyTop) / 28); }

    @Override public boolean mouseScrolled(double mx, double my, double delta) {
        if (zone() != TattooZone.FACE_NECK && over((int) mx, (int) my, gridX, bodyTop, 5 * 28 + 6, gridRows() * 28)) {
            scroll = Math.max(0, scroll - (int) Math.signum(delta));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    private void drawGrid(GuiGraphics g, int mx, int my) {
        g.drawString(font, "Motifs", gridX, py + 46, T.accent, false);
        int top = bodyTop;
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
            g.fill(gridX + 5 * 28 + 2, top, gridX + 5 * 28 + 5, top + barH, T.list);
            int thumb = Math.max(10, barH * rowsVisible / totalRows);
            int thumbY = top + (barH - thumb) * scroll / (totalRows - rowsVisible);
            g.fill(gridX + 5 * 28 + 2, thumbY, gridX + 5 * 28 + 5, thumbY + thumb, T.accent);
        }
        for (int m = 0; m <= TattooZone.MOTIFS; m++) {
            int row = m / 5;
            if (row < scroll || row >= scroll + rowsVisible) continue;
            int x = gridX + (m % 5) * 28, y = top + (row - scroll) * 28;
            boolean on = current == m, hover = over(mx, my, x, y, 26, 26);
            g.fill(x, y, x + 26, y + 26, on ? T.accent : hover ? T.hover : T.list);
            g.fill(x + 1, y + 1, x + 25, y + 25, on ? T.panel : T.list);
            if (m == 0) {
                g.drawString(font, "X", x + 10, y + 9, T.muted, false);
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
        // l'aperçu peut se faire sans sous-vêtements pour bien voir les tatouages (la fiche n'est pas modifiée)
        Appearance shown = underwear ? state.appearance() : state.appearance().withOutfit(fr.minenorth.identite.clothing.Outfit.EMPTY);
        if (previewAppearance == null || !shown.equals(previewAppearance) || state.gender() != previewGender) {
            previewAppearance = shown;
            previewGender = state.gender();
            ClientSkins.preview(previewGender, previewAppearance);
        }
        PreviewRenderer.Cam cam = PreviewRenderer.CAMS[camera];
        int[] hair = state.appearance().get(AppearanceSection.HAIR);
        PreviewRenderer.render(g, viewX, bodyTop, viewW, h, state.gender(), ClientSkins.previewLocation(),
                cam.yaw() + yaw, cam.zoom(), cam.focus(), hair[0], hair[1]);
        chip(g, mx, my, viewX + 2, bodyTop + 2, Math.min(96, viewW - 4), underwear ? "Sans sous-vêt." : "Avec sous-vêt.", !underwear, () -> underwear = !underwear);
        int bw = viewW / VIEW_NAMES.length;
        for (int i = 0; i < VIEW_NAMES.length; i++) {
            final int cam2 = VIEW_CAMS[i];
            chip(g, mx, my, viewX + i * bw, bodyTop + h + 4, bw - 3, VIEW_NAMES[i], camera == cam2, () -> { camera = cam2; yaw = 0; });
        }
    }

    // ------------------------------------------------------------------ carte du corps

    /** Zones de la carte (échelle 3) : {indice de zone, x, y, largeur, hauteur}. Vue de dos, la droite est à droite de l'image. */
    private int[][] mapZones() {
        int torso = mapBack ? 2 : 1;
        int[][] zones = {
                {0, 12, 0, 24, 24},
                {torso, 12, 24, 24, 36},
                {3, 0, 24, 12, 36},
                {4, 36, 24, 12, 36},
                {5, 12, 60, 12, 36},
                {6, 24, 60, 12, 36}};
        if (mapBack) {
            for (int[] z : zones) z[1] = 48 - z[1] - z[3];
        }
        return zones;
    }

    private void drawMap(GuiGraphics g, int mx, int my, int x0, int y0) {
        int[] t = tattoos();
        for (int[] z : mapZones()) {
            int index = z[0], x = x0 + z[1], y = y0 + z[2], w = z[3], h = z[4];
            TattooZone tz = ZONES[index];
            boolean selected = zone == index, hover = over(mx, my, x, y, w, h);
            g.fill(x, y, x + w, y + h, selected ? T.accent : T.frame);
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, hover ? 0xFFB07F60 : SKIN);
            if (tz == TattooZone.FACE_NECK) {
                if (!mapBack) {
                    g.fill(x + 6, y + 9, x + 9, y + 12, SKIN_DARK);
                    g.fill(x + 15, y + 9, x + 18, y + 12, SKIN_DARK);
                }
                if (t[tz.motifSlot()] > 0) g.fill(x + 15, y + 15, x + 18, y + 18, 0xFF2A2A3A);
            } else if (t[tz.motifSlot()] > 0) {
                int[] p = placement(tz, t);
                int[] art = SkinComposer.designPixels(t[tz.motifSlot()], t[TattooZone.INK_SLOT], p[2], p[3]);
                for (int i = 0; i < art.length; i++) {
                    if (art[i] == 0) continue;
                    int cx = x + (p[0] + i % p[2]) * 3, cy = y + (p[1] + i / p[2]) * 3;
                    g.fill(Math.max(cx, x + 1), cy, Math.min(cx + 3, x + w - 1), Math.min(cy + 3, y + h - 1), art[i] | 0xFF000000);
                }
            }
            DrawingKind kind = drawingKind(tz);
            if (kind != null && !isBlank(state.appearance().drawings().get(kind))) {
                g.fill(x + 2, y + h - 5, x + 5, y + h - 2, T.draw);   // un point violet : il y a un dessin libre
            }
            final int target = index;
            hits.add(new Hit(x, y, w, h, () -> chooseZone(target)));
        }
    }

    private void drawOptions(GuiGraphics g, int mx, int my) {
        int[] t = tattoos();
        g.drawString(font, "Zone : " + ZONE_NAMES[zone], optX, py + 46, T.accent, false);
        drawMap(g, mx, my, optX, bodyTop);
        int colX = optX + 58, colW = optW - 58;
        int y = bodyTop;
        if (zone().hasSize()) {
            y = row(g, mx, my, colX, y, colW, "Taille", SIZES, t[zone().sizeSlot()], zone().sizeSlot(), 3);
            y = row(g, mx, my, colX, y, colW, "Encre", INKS, t[TattooZone.INK_SLOT], TattooZone.INK_SLOT, 3);
        } else {
            g.drawString(font, "Petites marques discrètes,", colX, y, T.muted, false);
            g.drawString(font, "choisies dans le carnet.", colX, y + 10, T.muted, false);
        }
        if (isLimb()) {
            y = row(g, mx, my, colX, y, colW, isArm() ? "Face du bras" : "Face de la jambe", FACES, t[faceSlot()], faceSlot(), 4);
            row(g, mx, my, colX, y, colW, "Hauteur", isArm() ? HEIGHTS : LEG_HEIGHTS, t[heightSlot()], heightSlot(), 3);
        }
        int below = bodyTop + 102;
        chip(g, mx, my, optX, below, 52, mapBack ? "Devant" : "Dos", false, () -> {
            mapBack = !mapBack;
            if (zone() == TattooZone.TORSO && mapBack) zone = 2;
            else if (zone() == TattooZone.BACK && !mapBack) zone = 1;
        });
        if (isPlaceable()) drawPad(g, optX, below + 20);
    }

    /** Schéma du torse (8 × 12 cases) : le carré clair montre où sera le tatouage ; on clique pour le déplacer. */
    private void drawPad(GuiGraphics g, int x, int y) {
        padX = x;
        padY = y;
        g.fill(padX - 1, padY - 1, padX + 49, padY + 73, T.accent);
        g.fill(padX, padY, padX + 48, padY + 72, SKIN);
        int s = span(), ox = ox(), oy = oy();
        g.fill(padX + ox * 6, padY + oy * 6, padX + (ox + s) * 6, padY + (oy + s) * 6, 0xCCFFE066);
        g.drawString(font, "Cliquez ou glissez", padX + 56, padY + 4, T.muted, false);
        g.drawString(font, "pour placer le", padX + 56, padY + 14, T.muted, false);
        g.drawString(font, "tatouage.", padX + 56, padY + 24, T.muted, false);
    }

    /** Une ligne de choix : libellé puis puces sur {@code perRow} colonnes ; renvoie le y suivant. */
    private int row(GuiGraphics g, int mx, int my, int x, int y, int width, String label, String[] names, int selected, int slot, int perRow) {
        g.drawString(font, label, x, y, T.muted, false);
        y += 10;
        int w = width / perRow;
        for (int i = 0; i < names.length; i++) {
            final int value = i;
            chip(g, mx, my, x + (i % perRow) * w, y + (i / perRow) * 18, w - 3, names[i], selected == i, () -> setSlot(slot, value));
        }
        return y + ((names.length + perRow - 1) / perRow) * 18 + 6;
    }

    private void chip(GuiGraphics g, int mx, int my, int x, int y, int w, String label, boolean on, Runnable action) {
        boolean hover = over(mx, my, x, y, w, 16);
        g.fill(x, y, x + w, y + 16, on ? T.accent : hover ? T.hover : T.chip);
        String text = font.plainSubstrByWidth(label, w - 4);
        g.drawString(font, text, x + (w - font.width(text)) / 2, y + 4, on ? T.onAccent : T.text, false);
        hits.add(new Hit(x, y, w, 16, action));
    }

    private void drawFooter(GuiGraphics g, int mx, int my) {
        int y = py + ph - 24, w = 90, sw = 150;
        long price = free ? 0 : fr.minenorth.identite.salon.SalonPrices.quote(fr.minenorth.identite.salon.SalonKind.TATTOO, original, state.appearance());
        int saveX = px + pw - 8 - sw, cancelX = saveX - 6 - w;
        MineNorthStyle.button(g, cancelX, y, w, 20, "Annuler", T.chip, over(mx, my, cancelX, y, w, 20), true);
        hits.add(new Hit(cancelX, y, w, 20, this::onClose));
        MineNorthStyle.button(g, saveX, y, sw, 20, price > 0 ? "Tatouer · " + MineNorthStyle.euros(price) : free ? "Valider" : "Tatouer", T.confirm, over(mx, my, saveX, y, sw, 20), true);
        hits.add(new Hit(saveX, y, sw, 20, () -> { onSave.accept(state); onClose(); }));
        MineNorthStyle.button(g, px + 8, y, 70, 20, "Défaire", T.chip, over(mx, my, px + 8, y, 70, 20), state.canUndo());
        if (state.canUndo()) hits.add(new Hit(px + 8, y, 70, 20, state::undo));
        DrawingKind kind = drawingKind(zone());
        if (kind != null) {
            boolean has = !isBlank(state.appearance().drawings().get(kind));
            String label = (has ? "Mon dessin : " : "Dessiner : ") + ZONE_NAMES[zone];
            int dx = px + 84, dw = Math.min(180, font.width(label) + 16);
            MineNorthStyle.button(g, dx, y, dw, 20, label, T.draw, over(mx, my, dx, y, dw, 20), true);
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
