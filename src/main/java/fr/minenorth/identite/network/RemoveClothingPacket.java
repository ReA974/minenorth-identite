package fr.minenorth.identite.network;

import fr.minenorth.identite.clothing.ClothingSlot;
import fr.minenorth.identite.clothing.OutfitManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client vers serveur : le joueur retire le vêtement de cet emplacement. Un nom inconnu est ignoré sans couper la connexion. */
public record RemoveClothingPacket(ClothingSlot slot) {
    static void encode(RemoveClothingPacket p, FriendlyByteBuf b) { b.writeUtf(p.slot.name(), 32); }

    static RemoveClothingPacket decode(FriendlyByteBuf b) { return new RemoveClothingPacket(ClothingSlot.fromName(b.readUtf(32))); }

    static void handle(RemoveClothingPacket p, Supplier<NetworkEvent.Context> c) {
        c.get().enqueueWork(() -> {
            ServerPlayer sender = c.get().getSender();
            if (sender != null && p.slot != null) OutfitManager.remove(sender, p.slot);
        });
        c.get().setPacketHandled(true);
    }
}
