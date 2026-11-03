package fr.minenorth.identite.network;

import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.character.CharacterManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Client vers serveur : sections modifiées dans l'éditeur. Un paquet illisible ou falsifié (section inconnue,
 * tailles démesurées) est décodé en modification vide, que le serveur refuse : pas de déconnexion.
 */
public record EditorSavePacket(Map<AppearanceSection, int[]> edits) {
    private static final int MAX_VALUES = 16;

    static void encode(EditorSavePacket p, FriendlyByteBuf b) {
        b.writeVarInt(p.edits.size());
        for (Map.Entry<AppearanceSection, int[]> e : p.edits.entrySet()) {
            b.writeUtf(e.getKey().name(), 32);
            b.writeVarInt(e.getValue().length);
            for (int v : e.getValue()) b.writeVarInt(v);
        }
    }

    static EditorSavePacket decode(FriendlyByteBuf b) {
        Map<AppearanceSection, int[]> edits = new EnumMap<>(AppearanceSection.class);
        try {
            int n = b.readVarInt();
            if (n < 0 || n > AppearanceSection.values().length) return new EditorSavePacket(Map.of());
            for (int i = 0; i < n; i++) {
                AppearanceSection s = AppearanceSection.fromName(b.readUtf(32));
                int len = b.readVarInt();
                if (s == null || len < 0 || len > MAX_VALUES) return new EditorSavePacket(Map.of());
                int[] values = new int[len];
                for (int j = 0; j < len; j++) values[j] = b.readVarInt();
                edits.put(s, values);
            }
        } catch (RuntimeException e) {
            return new EditorSavePacket(Map.of());
        }
        return new EditorSavePacket(edits);
    }

    static void handle(EditorSavePacket p, Supplier<NetworkEvent.Context> c) {
        c.get().enqueueWork(() -> {
            ServerPlayer sender = c.get().getSender();
            if (sender != null) CharacterManager.applyClientEdit(sender, p.edits);
        });
        c.get().setPacketHandled(true);
    }
}
