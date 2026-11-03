package fr.minenorth.identite.network;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.character.CharacterManager;
import fr.minenorth.identite.document.Gender;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client vers serveur : tout ce qu'il faut pour terminer la création. Une apparence illisible est décodée en
 * {@code null} (la demande est alors refusée par le serveur) : un paquet falsifié ne coupe pas la connexion.
 */
public record CreateCharacterPacket(String lastName, String firstName, String birthDate, String birthPlace,
                                    String nationality, Gender gender, Appearance appearance, boolean rulesAccepted) {
    static void encode(CreateCharacterPacket p, FriendlyByteBuf b) {
        b.writeUtf(p.lastName, 64);
        b.writeUtf(p.firstName, 64);
        b.writeUtf(p.birthDate, 64);
        b.writeUtf(p.birthPlace, 64);
        b.writeUtf(p.nationality, 64);
        b.writeUtf(p.gender == null ? "" : p.gender.name(), 16);
        p.appearance.write(b);
        b.writeBoolean(p.rulesAccepted);
    }

    static CreateCharacterPacket decode(FriendlyByteBuf b) {
        String last = b.readUtf(64);
        String first = b.readUtf(64);
        String birthDate = b.readUtf(64);
        String birthPlace = b.readUtf(64);
        String nationality = b.readUtf(64);
        Gender gender = Gender.fromName(b.readUtf(16));
        Appearance appearance;
        boolean rules;
        try {
            appearance = Appearance.read(b);
            rules = b.readBoolean();
        } catch (RuntimeException e) {
            appearance = null;
            rules = false;
        }
        return new CreateCharacterPacket(last, first, birthDate, birthPlace, nationality, gender, appearance, rules);
    }

    static void handle(CreateCharacterPacket p, Supplier<NetworkEvent.Context> c) {
        c.get().enqueueWork(() -> {
            ServerPlayer sender = c.get().getSender();
            if (sender != null) {
                CharacterManager.create(sender, new CharacterManager.CreateRequest(p.lastName, p.firstName,
                        p.birthDate, p.birthPlace, p.nationality, p.gender, p.appearance, p.rulesAccepted));
            }
        });
        c.get().setPacketHandled(true);
    }
}
