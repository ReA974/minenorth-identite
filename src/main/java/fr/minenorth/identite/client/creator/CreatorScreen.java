package fr.minenorth.identite.client.creator;

import com.mojang.blaze3d.systems.RenderSystem;
import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.client.MineNorthStyle;
import fr.minenorth.identite.client.skin.ClientSkins;
import fr.minenorth.identite.document.Gender;
import fr.minenorth.identite.network.ModNetwork;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Écran de création de personnage (et éditeur limité à certaines sections pour les autres mods). Onglets à gauche,
 * aperçu 3D au centre, options à droite. L'état est dans {@link CreatorState}.
 */
@OnlyIn(Dist.CLIENT)
public final class CreatorScreen extends Screen {
    private enum Kind { HOME, SECTION, IDENTITY, RECAP }

    private record Tab(String label, Kind kind, AppearanceSection section) {}

    /** Zone cliquable de la colonne d'options, recalculée à chaque image. */
    private record Hit(int x, int y, int w, int h, Runnable action) {
        boolean contains(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private final CreatorState state;
    private final boolean creation;
    private final Consumer<CreatorState> onSave;
    private final List<Tab> tabs = new ArrayList<>();
    private final Set<Integer> visited = new java.util.HashSet<>();
    private final Random random = new Random();

    private int tab, camera, scroll, contentHeight;
    private float yaw, zoom = 1f;
    private long lastClick;
    private Gender previewGender;
    private Appearance previewAppearance;
    private EditBox lastNameBox, firstNameBox, placeBox, nationBox, dayBox, monthBox, yearBox;

    // mise en page, recalculée dans init()
    private int px, py, pw, ph, bodyTop, bodyBottom, tabsX, tabsW, viewX, viewW, optX, optW, optTop, optBottom;

    private CreatorScreen(CreatorState state, Set<AppearanceSection> editorSections, Consumer<CreatorState> onSave) {
        super(Component.literal("Création du personnage"));
        this.state = state;
        this.creation = editorSections == null;
        this.onSave = onSave;
        if (creation) {
            tabs.add(new Tab("Accueil", Kind.HOME, null));
            for (AppearanceSection s : AppearanceSection.values()) tabs.add(new Tab(s.label(), Kind.SECTION, s));
            tabs.add(new Tab("Identité", Kind.IDENTITY, null));
            tabs.add(new Tab("Récapitulatif", Kind.RECAP, null));
        } else {
            for (AppearanceSection s : AppearanceSection.values()) {
                if (editorSections.contains(s)) tabs.add(new Tab(s.label(), Kind.SECTION, s));
            }
        }
        visited.add(0);
    }

    public static CreatorScreen creation() {
        return new CreatorScreen(CreatorState.forCreation(), null, null);
    }

    public static CreatorScreen editor(Gender gender, Appearance current, Set<AppearanceSection> sections, Consumer<CreatorState> onSave) {
        return new CreatorScreen(CreatorState.forEditor(gender, current), EnumSet.copyOf(sections), onSave);
    }

    // ------------------------------------------------------------------ mise en page

    @Override protected void init() {
        clearWidgets();
        pw = Math.min(width - 8, 520);
        ph = Math.min(height - 8, 290);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        bodyTop = py + 38;
        bodyBottom = py + ph - 28;
        tabsX = px + 6;
        tabsW = Math.min(84, pw / 5);
        viewX = tabsX + tabsW + 6;
        viewW = Math.min(130, (pw - tabsW) / 3);
        optX = viewX + viewW + 6;
        optW = px + pw - 6 - optX;
        optTop = bodyTop;
        optBottom = bodyBottom;
        if (tabs.get(tab).kind == Kind.IDENTITY) buildIdentityBoxes();
    }

    private void buildIdentityBoxes() {
        int x = optX + 6 + labelWidth(), w = optW - 12 - labelWidth(), y = identityRowY(0);
        firstNameBox = box(x, y, w, "Prénom", state.firstName(), 32, null);
        lastNameBox = box(x, identityRowY(1), w, "Nom", state.lastName(), 32, null);
        placeBox = box(x, identityRowY(2), w, "Ville", state.birthPlace(), 32, null);
        nationBox = box(x, identityRowY(3), w, "Nationalité", state.nationality(), 32, null);
        int dw = (w - 8) / 3;
        int dy = identityRowY(4);
        dayBox = box(x, dy, dw, "JJ", state.day(), 2, s -> s.matches("\\d*"));
        monthBox = box(x + dw + 4, dy, dw, "MM", state.month(), 2, s -> s.matches("\\d*"));
        yearBox = box(x + 2 * (dw + 4), dy, dw, "AAAA", state.year(), 4, s -> s.matches("\\d*"));
    }

    private static final String[] IDENTITY_LABELS = {"Prénom", "Nom", "Né(e) à", "Nationalité", "Né(e) le"};

    /** Largeur de la colonne des libellés : les champs sont à droite de leur libellé, sur la même ligne. */
    private int labelWidth() {
        int w = 0;
        for (String s : IDENTITY_LABELS) w = Math.max(w, font.width(s));
        return w + 8;
    }

    private int identityRowY(int row) { return optTop + 22 + row * 22; }

    private EditBox box(int x, int y, int w, String hint, String value, int max, java.util.function.Predicate<String> filter) {
        EditBox b = new EditBox(font, x, y, w, 16, Component.literal(hint));
        b.setMaxLength(max);
        b.setHint(Component.literal(hint));
        if (filter != null) b.setFilter(filter);
        b.setValue(value);
        b.setResponder(s -> syncIdentity());
        addRenderableWidget(b);
        return b;
    }

    private void syncIdentity() {
        if (lastNameBox == null) return;
        state.setNames(lastNameBox.getValue(), firstNameBox.getValue(), placeBox.getValue(), nationBox.getValue());
        state.setBirth(dayBox.getValue(), monthBox.getValue(), yearBox.getValue());
    }

    private void selectTab(int index) {
        if (index < 0 || index >= tabs.size() || index == tab) return;
        syncIdentity();
        tab = index;
        scroll = 0;
        visited.add(index);
        lastNameBox = firstNameBox = placeBox = nationBox = dayBox = monthBox = yearBox = null;
        rebuildWidgets();
    }

    private AppearanceSection currentSection() { return tabs.get(tab).section; }

    // ------------------------------------------------------------------ rendu

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        drawFrame(g);
        drawTabs(g, mx, my);
        drawPreview(g, mx, my);
        drawOptions(g, mx, my);
        drawFooter(g, mx, my);
        super.render(g, mx, my, pt);
    }

    private void drawFrame(GuiGraphics g) {
        g.fill(px - 3, py - 3, px + pw + 3, py + ph + 3, MineNorthStyle.FRAME);
        g.fill(px, py, px + pw, py + ph, MineNorthStyle.PANEL);
        RenderSystem.enableBlend();
        g.blit(MineNorthStyle.LOGO, px + 8, py + 6, 24, 24, 0, 0, 256, 256, 256, 256);
        MineNorthStyle.scaled(g, MineNorthStyle.bold("MINENORTHRP · BUREAU D'ENREGISTREMENT"), px + 38, py + 8, 1.15f, MineNorthStyle.WHITE);
        g.drawString(font, creation ? "Terminez votre dossier pour entrer à MineNorthRP." : "Modification de l'apparence.", px + 38, py + 22, MineNorthStyle.MUTED, false);
        g.fill(px + 8, py + 34, px + pw - 8, py + 35, MineNorthStyle.HOVER);
        for (int i = 0; i < tabs.size(); i++) {
            int sx = px + pw - 12 - (tabs.size() - i) * 9;
            g.fill(sx, py + 8, sx + 6, py + 14, i == tab ? MineNorthStyle.PINK : visited.contains(i) ? MineNorthStyle.CYAN : MineNorthStyle.DISABLED);
        }
    }

    private int tabHeight() { return Math.min(17, (bodyBottom - bodyTop) / tabs.size()); }

    private void drawTabs(GuiGraphics g, int mx, int my) {
        int h = tabHeight();
        for (int i = 0; i < tabs.size(); i++) {
            int y = bodyTop + i * h;
            boolean hover = mx >= tabsX && mx < tabsX + tabsW && my >= y && my < y + h - 1;
            g.fill(tabsX, y, tabsX + tabsW, y + h - 1, i == tab ? MineNorthStyle.HOVER : hover ? MineNorthStyle.LIST : MineNorthStyle.PANEL);
            if (i == tab) g.fill(tabsX, y, tabsX + 2, y + h - 1, MineNorthStyle.PINK);
            g.drawString(font, tabs.get(i).label, tabsX + 6, y + (h - 9) / 2, i == tab ? MineNorthStyle.WHITE : MineNorthStyle.TEXT, false);
        }
    }

    private int previewH() { return bodyBottom - bodyTop - 14; }

    private void drawPreview(GuiGraphics g, int mx, int my) {
        int h = previewH();
        g.fillGradient(viewX, bodyTop, viewX + viewW, bodyTop + h, 0xFFFF9F6B, 0xFFFFD9A0);
        if (previewAppearance == null || !state.appearance().equals(previewAppearance) || state.gender() != previewGender) {
            previewAppearance = state.appearance();
            previewGender = state.gender();
            ClientSkins.preview(previewGender, previewAppearance);
        }
        PreviewRenderer.Cam cam = PreviewRenderer.CAMS[camera];
        PreviewRenderer.render(g, viewX, bodyTop, viewW, h, state.gender(), ClientSkins.previewLocation(),
                cam.yaw() + yaw, cam.zoom() * zoom, cam.focus(),
                state.appearance().get(fr.minenorth.identite.appearance.AppearanceSection.HAIR)[0],
                state.appearance().get(fr.minenorth.identite.appearance.AppearanceSection.HAIR)[1]);
        int bw = viewW / PreviewRenderer.CAMS.length;
        for (int i = 0; i < PreviewRenderer.CAMS.length; i++) {
            int x = viewX + i * bw;
            boolean hover = mx >= x && mx < x + bw && my >= bodyTop + h + 2 && my < bodyBottom;
            g.fill(x, bodyTop + h + 2, x + bw - 1, bodyBottom, i == camera ? MineNorthStyle.CYAN : hover ? MineNorthStyle.HOVER : MineNorthStyle.LIST);
            String label = PreviewRenderer.CAMS[i].label();
            g.drawString(font, font.plainSubstrByWidth(label, bw - 2), x + 1, bodyTop + h + 4, MineNorthStyle.WHITE, false);
        }
    }

    private void drawFooter(GuiGraphics g, int mx, int my) {
        int y = bodyBottom + 5;
        int gap = 4;
        int n = creation ? 5 : 4;
        int w = (pw - 12 - (n - 1) * gap) / n;
        for (int i = 0; i < n; i++) {
            int x = px + 6 + i * (w + gap);
            footerButton(g, i, x, y, w, 18, mx, my);
        }
    }

    private String footerLabel(int i) {
        if (creation) return new String[]{"‹ Précédent", "Aléatoire", "Annuler", "Rétablir", "Suivant ›"}[i];
        return new String[]{"Aléatoire", "Annuler", "Rétablir", "Valider"}[i];
    }

    private boolean footerActive(int i) {
        String label = footerLabel(i);
        return switch (label) {
            case "‹ Précédent" -> tab > 0;
            case "Annuler" -> state.canUndo();
            case "Rétablir" -> state.canRedo();
            case "Suivant ›" -> tab < tabs.size() - 1;
            default -> true;
        };
    }

    private void footerButton(GuiGraphics g, int i, int x, int y, int w, int h, int mx, int my) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
        int color = footerLabel(i).equals("Suivant ›") || footerLabel(i).equals("Valider") ? MineNorthStyle.CYAN : MineNorthStyle.DARK;
        MineNorthStyle.button(g, x, y, w, h, footerLabel(i), color, hover, footerActive(i));
    }

    private void footerClick(int i) {
        if (!footerActive(i)) return;
        switch (footerLabel(i)) {
            case "‹ Précédent" -> selectTab(tab - 1);
            case "Suivant ›" -> selectTab(tab + 1);
            case "Aléatoire" -> randomize();
            case "Annuler" -> state.undo();
            case "Rétablir" -> state.redo();
            case "Valider" -> { if (onSave != null) onSave.accept(state); onClose(); }
            default -> { }
        }
    }

    private void randomize() {
        AppearanceSection s = currentSection();
        if (s != null) {
            state.randomizeSection(s, random);
        } else {
            for (AppearanceSection sec : AppearanceSection.values()) state.randomizeSection(sec, random);
        }
    }

    // ------------------------------------------------------------------ colonne d'options

    private void drawOptions(GuiGraphics g, int mx, int my) {
        g.fill(optX, optTop, optX + optW, optBottom, MineNorthStyle.LIST);
        Tab t = tabs.get(tab);
        g.enableScissor(optX, optTop, optX + optW, optBottom);
        List<Hit> hits = new ArrayList<>();
        int y = optTop + 6 - scroll;
        switch (t.kind) {
            case HOME -> y = drawHome(g, hits, y);
            case SECTION -> y = drawSection(g, hits, y, t.section, mx, my);
            case IDENTITY -> y = drawIdentity(g, y);
            case RECAP -> y = drawRecap(g, hits, y, mx, my);
        }
        contentHeight = y + scroll - optTop;
        g.disableScissor();
        drawHits(g, hits, mx, my);
        this.hits = hits;
    }

    private List<Hit> hits = List.of();

    private void drawHits(GuiGraphics g, List<Hit> list, int mx, int my) {
        // les zones sont déjà dessinées par les méthodes ci-dessus ; seule la surbrillance est ajoutée ici
        for (Hit h : list) {
            if (h.contains(mx, my) && my >= optTop && my < optBottom) g.fill(h.x, h.y + h.h - 1, h.x + h.w, h.y + h.h, MineNorthStyle.WHITE);
        }
    }

    private int drawHome(GuiGraphics g, List<Hit> hits, int y) {
        int x = optX + 8, w = optW - 16;
        g.drawString(font, MineNorthStyle.bold("BUREAU D'ENREGISTREMENT"), x, y, MineNorthStyle.BLUE, false);
        y += 14;
        String[] lines = {
                "Composez votre personnage puis votre identité.",
                "Votre sexe et votre identité seront définitifs.",
                "Votre apparence pourra être modifiée plus tard chez les professionnels (coiffeur, tatoueur…).",
                "Durée estimée : 5 à 8 minutes. Rien n'est enregistré avant la validation finale."};
        for (String line : lines) {
            for (var part : font.split(Component.literal(line), w)) {
                g.drawString(font, part, x, y, MineNorthStyle.TEXT, false);
                y += 10;
            }
            y += 4;
        }
        y += 6;
        hits.add(button(g, x, y, w, 20, "Commencer", MineNorthStyle.CYAN, () -> selectTab(1)));
        return y + 24;
    }

    private Hit button(GuiGraphics g, int x, int y, int w, int h, String label, int color, Runnable action) {
        MineNorthStyle.button(g, x, y, w, h, label, color, false, true);
        return new Hit(x, y, w, h, action);
    }

    private int drawSection(GuiGraphics g, List<Hit> hits, int y, AppearanceSection section, int mx, int my) {
        int x = optX + 6, right = optX + optW - 6;
        g.drawString(font, MineNorthStyle.bold(section.label().toUpperCase(java.util.Locale.FRENCH)), x, y, MineNorthStyle.BLUE, false);
        String reset = "Réinitialiser";
        int rw = font.width(reset);
        g.drawString(font, reset, right - rw, y, MineNorthStyle.MUTED, false);
        hits.add(new Hit(right - rw, y - 1, rw, 10, () -> state.resetSection(section)));
        y += 14;
        if (section == AppearanceSection.BODY) y = drawGender(g, hits, y);
        int[] values = state.appearance().get(section);
        List<Catalog.Slot> slots = Catalog.slots(section);
        for (int i = 0; i < slots.size(); i++) {
            Catalog.Slot slot = slots.get(i);
            g.drawString(font, OptionLabels.slot(section, slot.key()), x, y, MineNorthStyle.TEXT, false);
            y += 12;
            y = OptionLabels.isColor(slot.key())
                    ? drawSwatches(g, hits, section, slot, i, values, x, y, right)
                    : drawChips(g, hits, section, slot, i, values, x, y, right);
            y += 6;
        }
        return drawingButtons(g, hits, section, y, x, right - x);
    }

    /** Boutons « Dessiner » des dessins de cette section (visage, barbe, tatouages), avec une pastille si déjà dessiné. */
    private int drawingButtons(GuiGraphics g, List<Hit> hits, AppearanceSection section, int y, int x, int w) {
        boolean any = false;
        for (fr.minenorth.identite.drawing.DrawingKind kind : fr.minenorth.identite.drawing.DrawingKind.values()) {
            if (kind.section() != section) continue;
            if (!any) {
                g.drawString(font, "Dessiner moi-même", x, y, MineNorthStyle.BLUE, false);
                y += 12;
                any = true;
            }
            boolean drawn = !isBlank(state.appearance().drawings().get(kind));
            hits.add(button(g, x, y, w, 16, kind.label() + (drawn ? "  ●" : ""), drawn ? MineNorthStyle.GREEN : MineNorthStyle.CYAN, () -> openDraw(kind)));
            y += 19;
        }
        return y + 4;
    }

    private static boolean isBlank(byte[] grid) {
        for (byte b : grid) if (b != 0) return false;
        return true;
    }

    /** Ouvre l'éditeur de pixels sur ce dessin ; la grille montre ce que le catalogue dessine déjà à cet endroit. */
    private void openDraw(fr.minenorth.identite.drawing.DrawingKind kind) {
        Appearance a = state.appearance();
        Appearance withoutThis = a.withDrawings(a.drawings().with(kind, new byte[kind.width() * kind.height()]));
        int[] underlay = fr.minenorth.identite.drawing.DrawingRegions.read(
                fr.minenorth.identite.skin.SkinComposer.compose(state.gender(), withoutThis), kind, state.gender().slimArms());
        minecraft.setScreen(new DrawScreen(this, kind, a.drawings().get(kind), underlay, grid -> state.setDrawing(kind, grid)));
    }

    private int drawGender(GuiGraphics g, List<Hit> hits, int y) {
        int x = optX + 6;
        g.drawString(font, "Sexe (définitif)", x, y, MineNorthStyle.TEXT, false);
        y += 12;
        for (Gender gender : Gender.values()) {
            String label = gender.label();
            int w = font.width(label) + 14;
            boolean selected = state.gender() == gender;
            boolean enabled = !state.genderLocked();
            g.fill(x, y, x + w, y + 14, selected ? MineNorthStyle.CYAN : enabled ? MineNorthStyle.DARK : MineNorthStyle.DISABLED);
            g.drawString(font, label, x + 7, y + 3, selected || enabled ? MineNorthStyle.WHITE : MineNorthStyle.MUTED, false);
            if (enabled) hits.add(new Hit(x, y, w, 14, () -> state.setGender(gender)));
            x += w + 4;
        }
        if (state.genderLocked()) g.drawString(font, "Non modifiable", x + 4, y + 3, MineNorthStyle.MUTED, false);
        return y + 22;
    }

    private int drawChips(GuiGraphics g, List<Hit> hits, AppearanceSection section, Catalog.Slot slot, int slotIndex,
                          int[] values, int x0, int y, int right) {
        int x = x0;
        for (int v = 0; v < slot.options(); v++) {
            String label = OptionLabels.option(section, slot.key(), v);
            int w = font.width(label) + 10;
            if (x + w > right) { x = x0; y += 16; }
            boolean allowed = slot.allows(state.gender(), v);
            boolean selected = values[slotIndex] == v;
            g.fill(x, y, x + w, y + 14, selected ? MineNorthStyle.CYAN : allowed ? MineNorthStyle.DARK : MineNorthStyle.DISABLED);
            g.drawString(font, label, x + 5, y + 3, allowed ? MineNorthStyle.WHITE : MineNorthStyle.MUTED, false);
            if (allowed) {
                final int value = v;
                hits.add(new Hit(x, y, w, 14, () -> choose(section, slotIndex, value)));
            }
            x += w + 3;
        }
        return y + 16;
    }

    private int drawSwatches(GuiGraphics g, List<Hit> hits, AppearanceSection section, Catalog.Slot slot, int slotIndex,
                             int[] values, int x0, int y, int right) {
        int x = x0;
        for (int v = 0; v < slot.options(); v++) {
            if (x + 16 > right) { x = x0; y += 19; }
            int color = swatchColor(slot.key(), v);
            boolean selected = values[slotIndex] == v;
            g.fill(x - 1, y - 1, x + 17, y + 17, selected ? MineNorthStyle.WHITE : MineNorthStyle.FRAME);
            g.fill(x, y, x + 16, y + 16, color);
            final int value = v;
            hits.add(new Hit(x, y, 16, 16, () -> choose(section, slotIndex, value)));
            x += 20;
        }
        return y + 19;
    }

    private static int swatchColor(String key, int v) {
        if (key.equals("blushColor")) return fr.minenorth.identite.skin.Palette.lipColor(v);
        if (key.equals("metal")) return fr.minenorth.identite.skin.Palette.metal(v);
        if (key.equals("tone")) return fr.minenorth.identite.skin.Palette.skinTone(v);
        if (key.equals("lipColor")) return fr.minenorth.identite.skin.Palette.lipColor(v);
        if (key.equals("eyeColor")) return fr.minenorth.identite.skin.Palette.eyeColor(v);
        return fr.minenorth.identite.skin.Palette.hairColor(v);
    }

    private void choose(AppearanceSection section, int slotIndex, int value) {
        int[] values = state.appearance().get(section);
        values[slotIndex] = value;
        state.set(section, values);
    }

    private int drawIdentity(GuiGraphics g, int y) {
        int x = optX + 6;
        g.drawString(font, MineNorthStyle.bold("IDENTITÉ"), x, y, MineNorthStyle.BLUE, false);
        g.drawString(font, "Définitive", optX + optW - 6 - font.width("Définitive"), y, MineNorthStyle.WARN, false);
        for (int i = 0; i < IDENTITY_LABELS.length; i++) {
            g.drawString(font, IDENTITY_LABELS[i], x, identityRowY(i) + 4, MineNorthStyle.MUTED, false);
        }
        LocalDate today = LocalDate.now();
        int line = identityRowY(4) + 24;
        state.age(today).ifPresent(age -> g.drawString(font, age + " ans · sexe : " + state.gender().label(), x, line, MineNorthStyle.OK, false));
        state.identityError(today).filter(e -> !state.lastName().isBlank() || !state.day().isBlank())
                .ifPresent(e -> g.drawString(font, font.plainSubstrByWidth(e, optW - 12), x, line + 11, MineNorthStyle.ALERT, false));
        return line + 24;
    }

    private int drawRecap(GuiGraphics g, List<Hit> hits, int y, int mx, int my) {
        int x = optX + 6, right = optX + optW - 6;
        g.drawString(font, MineNorthStyle.bold("RÉCAPITULATIF"), x, y, MineNorthStyle.BLUE, false);
        y += 14;
        for (AppearanceSection s : AppearanceSection.values()) {
            g.drawString(font, s.label(), x, y, MineNorthStyle.TEXT, false);
            g.drawString(font, "OK", right - font.width("OK"), y, MineNorthStyle.OK, false);
            y += 11;
        }
        LocalDate today = LocalDate.now();
        boolean identityOk = state.identityError(today).isEmpty();
        String id = identityOk ? state.firstName() + " " + state.lastName().toUpperCase(java.util.Locale.FRENCH) + " · " + state.gender().label()
                + " · " + state.age(today).orElse(0) + " ans" : "Identité incomplète";
        g.drawString(font, "Identité", x, y, MineNorthStyle.TEXT, false);
        g.drawString(font, font.plainSubstrByWidth(id, optW - 70), right - font.width(font.plainSubstrByWidth(id, optW - 70)), y,
                identityOk ? MineNorthStyle.OK : MineNorthStyle.ALERT, false);
        y += 15;
        int cb = y;
        g.fill(x, cb, x + 10, cb + 10, MineNorthStyle.FRAME);
        if (state.rulesAccepted()) g.fill(x + 2, cb + 2, x + 8, cb + 8, MineNorthStyle.OK);
        int tw = optW - 30;
        int ty = y;
        for (var part : font.split(Component.literal("Je confirme que mon nom respecte le règlement RP (pas de célébrité, pas de jeu de mots)."), tw)) {
            g.drawString(font, part, x + 16, ty, MineNorthStyle.TEXT, false);
            ty += 10;
        }
        hits.add(new Hit(x, cb, optW - 12, Math.max(12, ty - cb), () -> state.setRulesAccepted(!state.rulesAccepted())));
        y = ty + 6;
        boolean ready = identityOk && state.rulesAccepted();
        int w = optW - 12;
        if (!identityOk) {
            g.drawString(font, "Complétez l'onglet Identité.", x, y, MineNorthStyle.ALERT, false);
            y += 12;
        }
        MineNorthStyle.button(g, x, y, w, 20, "Choisir mon skin", MineNorthStyle.GREEN, false, ready);
        if (ready) hits.add(new Hit(x, y, w, 20, () -> submit(true)));
        y += 24;
        return y;
    }

    private void submit(boolean customSkin) {
        syncIdentity();
        state.setCustomSkin(customSkin);
        ModNetwork.CHANNEL.sendToServer(new fr.minenorth.identite.network.CreateCharacterPacket(
                state.lastName(), state.firstName(), state.birthDate(), state.birthPlace(), state.nationality(),
                state.gender(), state.appearance(), state.rulesAccepted()));
    }

    // ------------------------------------------------------------------ entrées

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0) return false;
        int h = tabHeight();
        if (mx >= tabsX && mx < tabsX + tabsW && my >= bodyTop && my < bodyTop + tabs.size() * h) {
            selectTab((int) ((my - bodyTop) / h));
            return true;
        }
        int ph2 = previewH();
        if (mx >= viewX && mx < viewX + viewW && my >= bodyTop + ph2 + 2 && my < bodyBottom) {
            camera = Math.min(PreviewRenderer.CAMS.length - 1, (int) ((mx - viewX) / (viewW / PreviewRenderer.CAMS.length)));
            yaw = 0;
            zoom = 1f;
            return true;
        }
        if (mx >= viewX && mx < viewX + viewW && my >= bodyTop && my < bodyTop + ph2) {
            long now = System.currentTimeMillis();
            if (now - lastClick < 300) { yaw = 0; zoom = 1f; }
            lastClick = now;
            return true;
        }
        if (mx >= optX && mx < optX + optW && my >= optTop && my < optBottom) {
            for (Hit hit : hits) {
                if (hit.contains(mx, my)) { hit.action.run(); return true; }
            }
        }
        int n = creation ? 5 : 4;
        int gap = 4, w = (pw - 12 - (n - 1) * gap) / n, y = bodyBottom + 5;
        for (int i = 0; i < n; i++) {
            int x = px + 6 + i * (w + gap);
            if (mx >= x && mx < x + w && my >= y && my < y + 18) { footerClick(i); return true; }
        }
        return false;
    }

    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 0 && mx >= viewX && mx < viewX + viewW && my >= bodyTop && my < bodyTop + previewH()) {
            yaw += (float) dx * 1.5f;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx >= viewX && mx < viewX + viewW && my >= bodyTop && my < bodyTop + previewH()) {
            zoom = Math.max(0.6f, Math.min(3f, zoom + (float) delta * 0.1f));
            return true;
        }
        if (mx >= optX && mx < optX + optW) {
            int max = Math.max(0, contentHeight - (optBottom - optTop) + 6);
            scroll = Math.max(0, Math.min(max, scroll - (int) (delta * 14)));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override public boolean keyPressed(int key, int scan, int mods) {
        boolean typing = getFocused() instanceof EditBox eb && eb.isFocused();
        if (!typing) {
            if (hasControlDown() && key == GLFW.GLFW_KEY_Z) { state.undo(); return true; }
            if (hasControlDown() && key == GLFW.GLFW_KEY_Y) { state.redo(); return true; }
            if (key == GLFW.GLFW_KEY_R) { randomize(); return true; }
            if (key == GLFW.GLFW_KEY_TAB) { selectTab(Math.floorMod(tab + (hasShiftDown() ? -1 : 1), tabs.size())); return true; }
            if (key == GLFW.GLFW_KEY_DOWN) { selectTab(tab + 1); return true; }
            if (key == GLFW.GLFW_KEY_UP) { selectTab(tab - 1); return true; }
            if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_7) { camera = key - GLFW.GLFW_KEY_1; yaw = 0; zoom = 1f; return true; }
        }
        return super.keyPressed(key, scan, mods);
    }

    public boolean isCreation() { return creation; }

    @Override public boolean shouldCloseOnEsc() { return !creation; }
    @Override public boolean isPauseScreen() { return false; }

    @Override public void onClose() {
        if (previewAppearance != null) previewAppearance = null;
        super.onClose();
    }
}
