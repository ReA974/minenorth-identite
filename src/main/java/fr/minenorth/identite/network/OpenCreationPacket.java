package fr.minenorth.identite.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Serveur vers client : ouvre l'écran de création de personnage. */
public record OpenCreationPacket() {
    static void encode(OpenCreationPacket p, FriendlyByteBuf b) {}

    static OpenCreationPacket decode(FriendlyByteBuf b) { return new OpenCreationPacket(); }

    static void handle(OpenCreationPacket p, Supplier<NetworkEvent.Context> c) {
        c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> fr.minenorth.identite.client.ClientNetworkHandler::openCreation));
        c.get().setPacketHandled(true);
    }
}
