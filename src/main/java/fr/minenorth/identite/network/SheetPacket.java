package fr.minenorth.identite.network;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.document.Gender;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Serveur vers client : fiche d'apparence d'un joueur, ou « aucune fiche » (present = faux) après un reset. */
public record SheetPacket(UUID subject, boolean present, Gender gender, Appearance appearance) {
    static void encode(SheetPacket p, FriendlyByteBuf b) {
        b.writeUUID(p.subject);
        b.writeBoolean(p.present);
        if (p.present) {
            b.writeUtf(p.gender.name());
            p.appearance.write(b);
        }
    }

    static SheetPacket decode(FriendlyByteBuf b) {
        UUID subject = b.readUUID();
        if (!b.readBoolean()) return new SheetPacket(subject, false, null, null);
        Gender gender = Gender.fromName(b.readUtf(16));
        return new SheetPacket(subject, true, gender, Appearance.read(b));
    }

    static void handle(SheetPacket p, Supplier<NetworkEvent.Context> c) {
        c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> fr.minenorth.identite.client.ClientNetworkHandler.onSheet(p)));
        c.get().setPacketHandled(true);
    }
}
