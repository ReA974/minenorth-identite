package fr.minenorth.identite.client;

import fr.minenorth.identite.MineNorthIdentite;
import fr.minenorth.identite.client.skin.ClientSkins;
import fr.minenorth.identite.clothing.Clothing;
import fr.minenorth.identite.clothing.ClothingSlot;
import fr.minenorth.identite.item.ModItems;
import fr.minenorth.identite.network.ModNetwork;
import fr.minenorth.identite.network.ToggleClothingPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Bouton « sous-vêtements » de l'inventaire, à côté du livre de recettes : retire les sous-vêtements s'ils sont portés,
 * sinon les remet depuis l'inventaire. Visible seulement pour un joueur qui a un personnage.
 */
@Mod.EventBusSubscriber(modid = MineNorthIdentite.MOD_ID, value = Dist.CLIENT)
public final class InventoryOutfitButton {
    private InventoryOutfitButton() {}

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;
        // à droite du livre de recettes (qui est en 104,61 dans la fenêtre de l'inventaire)
        event.addListener(new Toggle(screen.getGuiLeft() + 128, screen.getGuiTop() + 61));
    }

    private static final class Toggle extends AbstractButton {
        Toggle(int x, int y) { super(x, y, 20, 18, Component.literal("Sous-vêtements")); }

        private Clothing worn() {
            var player = Minecraft.getInstance().player;
            if (player == null) return null;
            return ClientSkins.entry(player.getUUID()).map(e -> e.appearance().outfit().get(ClothingSlot.UNDERWEAR)).orElse(null);
        }

        private boolean hasCharacter() {
            var player = Minecraft.getInstance().player;
            return player != null && ClientSkins.entry(player.getUUID()).isPresent();
        }

        @Override public void onPress() { ModNetwork.CHANNEL.sendToServer(new ToggleClothingPacket(ClothingSlot.UNDERWEAR)); }

        @Override protected void updateWidgetNarration(NarrationElementOutput out) { defaultButtonNarrationText(out); }

        @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            visible = hasCharacter();
            if (!visible) return;
            boolean on = worn() != null;
            setTooltip(Tooltip.create(Component.literal(on ? "Sous-vêtements : cliquer pour les retirer"
                    : "Sous-vêtements : cliquer pour les remettre")));
            g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF000000);
            g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1,
                    isHoveredOrFocused() ? 0xFF6B6B8C : 0xFF3A3A4E);
            var player = Minecraft.getInstance().player;
            var gender = player == null ? null : ClientSkins.entry(player.getUUID()).map(ClientSkins.Entry::gender).orElse(null);
            Clothing icon = gender == fr.minenorth.identite.document.Gender.FEMALE ? Clothing.UNDERWEAR_FEMALE : Clothing.UNDERWEAR_MALE;
            ItemStack stack = ModItems.stackFor(icon);
            g.renderItem(stack, getX() + 2, getY() + 1);
            if (!on) g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0x99101018);
        }
    }
}
