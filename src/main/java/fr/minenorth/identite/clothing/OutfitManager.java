package fr.minenorth.identite.clothing;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceData;
import fr.minenorth.identite.character.CharacterManager;
import fr.minenorth.identite.document.Gender;
import fr.minenorth.identite.document.IdentityData;
import fr.minenorth.identite.item.ClothingItem;
import fr.minenorth.identite.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/** Porter et retirer un vêtement côté serveur : applique {@link OutfitRules}, enregistre et prévient les clients. */
public final class OutfitManager {
    private OutfitManager() {}

    /** Le joueur porte le vêtement qu'il tient en main (remplace, et rend, celui qui occupait l'emplacement). */
    public static void wear(ServerPlayer player, InteractionHand hand) {
        wearStack(player, player.getItemInHand(hand));
    }

    /**
     * Bouton de l'inventaire : retire le vêtement de cet emplacement s'il est porté, sinon porte le premier vêtement
     * qui convient parmi les objets du joueur.
     */
    public static void toggle(ServerPlayer player, ClothingSlot slot) {
        Appearance current = ready(player);
        if (current == null) return;
        if (current.outfit().get(slot) != null) {
            remove(player, slot);
            return;
        }
        List<Clothing> clothes = new ArrayList<>();
        List<ItemStack> stacks = new ArrayList<>();
        List<ItemStack> all = new ArrayList<>(player.getInventory().items);
        all.addAll(player.getInventory().offhand);
        for (ItemStack s : all) {
            if (s.getItem() instanceof ClothingItem item) {
                clothes.add(item.clothing());
                stacks.add(s);
            }
        }
        OptionalInt pick = OutfitRules.pickFromInventory(gender(player), slot, clothes);
        if (pick.isEmpty()) {
            player.displayClientMessage(Component.literal("§cVous n'avez pas de " + slot.label().toLowerCase(java.util.Locale.FRENCH) + " sur vous."), true);
            return;
        }
        wearStack(player, stacks.get(pick.getAsInt()));
    }

    private static void wearStack(ServerPlayer player, ItemStack stack) {
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
        player.displayClientMessage(Component.literal("§aVous avez retiré : " + slot.label().toLowerCase(java.util.Locale.FRENCH) + "."), true);
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

    private static Gender gender(ServerPlayer player) {
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
