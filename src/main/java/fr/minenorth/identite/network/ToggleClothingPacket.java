package fr.minenorth.identite.network;

import fr.minenorth.identite.clothing.ClothingSlot;
import fr.minenorth.identite.clothing.OutfitManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client vers serveur : le joueur retire ou remet le vêtement de cet emplacement (bouton de l'inventaire). Un nom
 * inconnu est ignoré sans couper la connexion.
 */
public record ToggleClothingPacket(ClothingSlot slot) {
    static void encode(ToggleClothingPacket p, FriendlyByteBuf b) { b.writeUtf(p.slot.name(), 32); }

    static ToggleClothingPacket decode(FriendlyByteBuf b) { return new ToggleClothingPacket(ClothingSlot.fromName(b.readUtf(32))); }

    static void handle(ToggleClothingPacket p, Supplier<NetworkEvent.Context> c) {
        c.get().enqueueWork(() -> {
            ServerPlayer sender = c.get().getSender();
            if (sender != null && p.slot != null) OutfitManager.toggle(sender, p.slot);
        });
        c.get().setPacketHandled(true);
    }
}
