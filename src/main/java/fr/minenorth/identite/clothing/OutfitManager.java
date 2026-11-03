package fr.minenorth.identite.clothing;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceData;
import fr.minenorth.identite.character.CharacterManager;
import fr.minenorth.identite.document.IdentityData;
import fr.minenorth.identite.item.ClothingItem;
import fr.minenorth.identite.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Porter et retirer un vêtement côté serveur : applique {@link OutfitRules}, enregistre et prévient les clients. */
public final class OutfitManager {
    private OutfitManager() {}

    /** Le joueur porte le vêtement qu'il tient en main (remplace, et rend, celui qui occupait l'emplacement). */
    public static void wear(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof ClothingItem item)) return;
        Appearance current = ready(player);
        if (current == null) return;
        OutfitRules.Change change = OutfitRules.wear(gender(player), current.outfit(), item.clothing());
        if (change.error().isPresent()) {
            player.displayClientMessage(Component.literal("§c" + change.error().get()), true);
            return;
        }
        apply(player, current, change.outfit());
        stack.shrink(1);
        change.returned().ifPresent(old -> give(player, old));
        player.displayClientMessage(Component.literal("§aVous portez : " + item.clothing().label() + "."), true);
    }

    /** Le joueur retire le vêtement de cet emplacement ; l'item revient dans son inventaire. */
    public static void remove(ServerPlayer player, ClothingSlot slot) {
        Appearance current = ready(player);
        if (current == null) return;
        OutfitRules.Change change = OutfitRules.remove(current.outfit(), slot);
        if (change.error().isPresent()) {
            player.displayClientMessage(Component.literal("§c" + change.error().get()), true);
            return;
        }
        apply(player, current, change.outfit());
        change.returned().ifPresent(old -> give(player, old));
        player.displayClientMessage(Component.literal("§aVous avez retiré : " + slot.label() + "."), true);
    }

    /** Fiche du joueur s'il a un personnage et n'est pas en pleine création ; sinon null. */
    private static Appearance ready(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (CharacterManager.isCreating(player.getUUID()) || !CharacterManager.hasCharacter(level, player.getUUID())) {
            player.displayClientMessage(Component.literal("§cVous devez d'abord créer votre personnage."), true);
            return null;
        }
        return AppearanceData.get(level).get(player.getUUID());
    }

    private static fr.minenorth.identite.document.Gender gender(ServerPlayer player) {
        IdentityData.Profile profile = IdentityData.get(player.serverLevel()).get(player.getUUID());
        return profile.gender();
    }

    private static void apply(ServerPlayer player, Appearance current, Outfit outfit) {
        CharacterManager.updateAppearance(player.server, player.getUUID(), gender(player), current.withOutfit(outfit));
    }

    private static void give(ServerPlayer player, Clothing clothing) {
        ItemStack stack = ModItems.stackFor(clothing);
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }
}
