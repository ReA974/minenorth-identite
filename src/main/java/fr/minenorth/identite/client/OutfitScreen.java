package fr.minenorth.identite.client;

import fr.minenorth.identite.clothing.Clothing;
import fr.minenorth.identite.clothing.ClothingSlot;
import fr.minenorth.identite.client.skin.ClientSkins;
import fr.minenorth.identite.network.ModNetwork;
import fr.minenorth.identite.network.RemoveClothingPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** « Ma tenue » : liste ce que le joueur porte et lui permet de le retirer (l'item revient dans son inventaire). */
@OnlyIn(Dist.CLIENT)
public final class OutfitScreen extends Screen {
    private static final int W = 240, ROW = 26;

    public OutfitScreen() { super(Component.literal("Ma tenue")); }

    private int left() { return (width - W) / 2; }
    private int top() { return Math.max(8, (height - (60 + ClothingSlot.values().length * ROW)) / 2); }

    private Clothing worn(ClothingSlot slot) {
        var player = Minecraft.getInstance().player;
        if (player == null) return null;
        return ClientSkins.entry(player.getUUID()).map(e -> e.appearance().outfit().get(slot)).orElse(null);
    }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int l = left(), t = top(), h = 56 + ClothingSlot.values().length * ROW;
        MineNorthStyle.panel(g, l, t, W, h, "MA TENUE", "Vêtements portés");
        int y = t + 50;
        for (ClothingSlot slot : ClothingSlot.values()) {
            Clothing c = worn(slot);
            g.drawString(font, slot.label(), l + 12, y + 3, MineNorthStyle.BLUE, false);
            g.drawString(font, c == null ? "Rien" : c.label(), l + 12, y + 14, c == null ? MineNorthStyle.MUTED : MineNorthStyle.TEXT, false);
            boolean hover = mx >= l + W - 90 && mx < l + W - 10 && my >= y && my < y + 20;
            MineNorthStyle.button(g, l + W - 90, y, 80, 20, "Retirer", MineNorthStyle.PINK, hover, c != null);
            y += ROW;
        }
        g.drawString(font, "Clic droit sur l'item pour le remettre.", l + 12, t + h - 14, MineNorthStyle.MUTED, false);
        super.render(g, mx, my, pt);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        int l = left(), y = top() + 50;
        for (ClothingSlot slot : ClothingSlot.values()) {
            if (worn(slot) != null && mx >= l + W - 90 && mx < l + W - 10 && my >= y && my < y + 20) {
                ModNetwork.CHANNEL.sendToServer(new RemoveClothingPacket(slot));
                return true;
            }
            y += ROW;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override public boolean isPauseScreen() { return false; }
}
