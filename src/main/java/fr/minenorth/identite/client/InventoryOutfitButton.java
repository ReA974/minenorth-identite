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
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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
        if (event.getScreen() instanceof InventoryScreen screen) {
            // à droite du livre de recettes (qui est en 104,61 dans la fenêtre de l'inventaire)
            event.addListener(new Toggle(screen.getGuiLeft() + 128, screen.getGuiTop() + 61, false));
        } else if (event.getScreen() instanceof AbstractContainerScreen<?> screen
                && screen.getClass().getName().equals(MINENORTH_INVENTORY)) {
            // inventaire du mod minenorth-inventaire (sans dépendance directe) : colonne de droite du panneau
            // Équipement, sous l'emplacement de la main gauche (InvLayout.HAND_X / HAND_Y + 24)
            event.addListener(new Toggle(screen.getGuiLeft() + 304, screen.getGuiTop() + 78, true));
        }
    }

    private static final String MINENORTH_INVENTORY = "fr.minenorth.inventaire.client.InvScreen";

    private static final class Toggle extends AbstractButton {
        private final boolean styled;

        Toggle(int x, int y, boolean styled) {
            super(x, y, styled ? 22 : 20, styled ? 22 : 18, Component.literal("Sous-vêtements"));
            this.styled = styled;
        }

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
            if (styled) {
                // style de l'inventaire MineNorth : fond bleu nuit, cadre cyan (rose au survol), plein quand porté
                int edge = isHoveredOrFocused() ? 0xFFFF4FA8 : on ? 0xFF2BD0E8 : 0xFF1A4560;
                g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF0B2033);
                g.renderOutline(getX(), getY(), width, height, edge);
                if (on) g.renderOutline(getX() + 1, getY() + 1, width - 2, height - 2, 0x552BD0E8);
            } else {
                g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF000000);
                g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1,
                        isHoveredOrFocused() ? 0xFF6B6B8C : 0xFF3A3A4E);
            }
            var player = Minecraft.getInstance().player;
            var gender = player == null ? null : ClientSkins.entry(player.getUUID()).map(ClientSkins.Entry::gender).orElse(null);
            Clothing icon = gender == fr.minenorth.identite.document.Gender.FEMALE ? Clothing.UNDERWEAR_FEMALE : Clothing.UNDERWEAR_MALE;
            ItemStack stack = ModItems.stackFor(icon);
            g.renderItem(stack, getX() + (width - 16) / 2, getY() + (height - 16) / 2);
            if (!on) g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0x99081018);
        }
    }
}
