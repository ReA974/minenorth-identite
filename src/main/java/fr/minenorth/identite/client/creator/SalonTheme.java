package fr.minenorth.identite.client.creator;

import fr.minenorth.identite.client.MineNorthStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Habillage d'un salon : couleurs et petite icône à la place du logo. Le salon de tatouage est charbon et rouge encre
 * (avec une goutte d'encre), le salon de coiffure est bleu-vert et or (avec des ciseaux).
 */
@OnlyIn(Dist.CLIENT)
public final class SalonTheme {
    public final int frame, panel, list, hover, accent, onAccent, chip, text, muted, confirm, draw, line;
    private final String[] icon;
    private final int[] palette;   // couleur des lettres a, b, c, d de l'icône

    private SalonTheme(int frame, int panel, int list, int hover, int accent, int onAccent, int chip, int text, int muted,
                       int confirm, int draw, int line, String[] icon, int[] palette) {
        this.frame = frame;
        this.panel = panel;
        this.list = list;
        this.hover = hover;
        this.accent = accent;
        this.onAccent = onAccent;
        this.chip = chip;
        this.text = text;
        this.muted = muted;
        this.confirm = confirm;
        this.draw = draw;
        this.line = line;
        this.icon = icon;
        this.palette = palette;
    }

    /** Goutte d'encre : a = encre, b = reflet, c = contour. */
    private static final String[] DROP = {
            ".....cc.....",
            "....caac....",
            "....caac....",
            "...caaaac...",
            "..caaaaaac..",
            ".caabaaaaac.",
            ".cabbaaaaac.",
            ".caaaaaaaac.",
            ".caaaaaaaac.",
            "..caaaaaac..",
            "...cccccc...",
            "............"};

    /** Ciseaux : a = lames, b = anneaux. */
    private static final String[] SCISSORS = {
            "aa........aa",
            ".aa......aa.",
            "..aa....aa..",
            "...aa..aa...",
            "....aaaa....",
            "....aaaa....",
            "...aa..aa...",
            "..aa....aa..",
            ".bbb....bbb.",
            "b..b....b..b",
            "b..b....b..b",
            ".bb......bb."};

    public static final SalonTheme TATTOO = new SalonTheme(
            0xFF0A0506, 0xFF241619, 0xFF150D0F, 0xFF4E2029, 0xFFE0465E, 0xFFFFFFFF, 0xFF43262C, 0xFFF4E6E6, 0xFFC9A2A8,
            0xFFB02A44, 0xFF7A3A8A, 0xFF8A2E3C, DROP, new int[]{0xFFE0465E, 0xFFFFC0C8, 0xFF14090B});

    public static final SalonTheme HAIR = new SalonTheme(
            0xFF050F10, 0xFF11292F, 0xFF0A1A1F, 0xFF1F5A66, 0xFFE8B84A, 0xFF21180A, 0xFF265560, 0xFFEAF6F2, 0xFF9CC9C2,
            0xFF1E9E7A, 0xFFB07A2A, 0xFF3C8A8A, SCISSORS, new int[]{0xFFE6EAF2, 0xFFE8B84A, 0xFF14090B});

    /** Fond, cadre, icône à la place du logo, titre et filet, comme les autres écrans MineNorth. */
    public void panel(GuiGraphics g, int left, int top, int w, int h, String title, String subtitle) {
        Font font = Minecraft.getInstance().font;
        g.fill(left - 3, top - 3, left + w + 3, top + h + 3, frame);
        g.fill(left, top, left + w, top + h, panel);
        g.fill(left + 8, top + 5, left + 36, top + 33, list);
        drawIcon(g, left + 10, top + 7, 2);
        MineNorthStyle.scaled(g, MineNorthStyle.bold(title), left + 42, top + 9, 1.6f, MineNorthStyle.WHITE);
        if (subtitle != null && !subtitle.isBlank()) g.drawString(font, subtitle, left + 42, top + 26, muted, false);
        g.fill(left + 10, top + 40, left + w - 10, top + 41, line);
    }

    private void drawIcon(GuiGraphics g, int x, int y, int scale) {
        for (int row = 0; row < icon.length; row++) {
            for (int col = 0; col < icon[row].length(); col++) {
                char ch = icon[row].charAt(col);
                if (ch == '.') continue;
                int color = palette[ch - 'a'];
                g.fill(x + col * scale, y + row * scale, x + (col + 1) * scale, y + (row + 1) * scale, color);
            }
        }
    }
}
