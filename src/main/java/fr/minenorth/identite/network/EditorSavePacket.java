package fr.minenorth.identite.network;

import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.character.CharacterManager;
import fr.minenorth.identite.drawing.DrawingKind;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Client vers serveur : sections modifiées dans l'éditeur, et dessins des sections ouvertes. Un paquet illisible ou
 * falsifié (section ou dessin inconnu, tailles démesurées, octets en moins) est décodé en modification vide, que le
 * serveur refuse : pas de déconnexion. Les numéros de couleur des dessins sont vérifiés par le serveur (palette).
 */
public record EditorSavePacket(Map<AppearanceSection, int[]> edits, Map<DrawingKind, byte[]> drawings) {
    private static final int MAX_VALUES = 32;

    /** Modification du catalogue seulement. */
    public EditorSavePacket(Map<AppearanceSection, int[]> edits) { this(edits, Map.of()); }

    static void encode(EditorSavePacket p, FriendlyByteBuf b) {
        b.writeVarInt(p.edits.size());
        for (Map.Entry<AppearanceSection, int[]> e : p.edits.entrySet()) {
            b.writeUtf(e.getKey().name(), 32);
            b.writeVarInt(e.getValue().length);
            for (int v : e.getValue()) b.writeVarInt(v);
        }
        b.writeVarInt(p.drawings.size());
        for (Map.Entry<DrawingKind, byte[]> e : p.drawings.entrySet()) {
            b.writeUtf(e.getKey().name(), 32);
            b.writeBytes(e.getValue());
        }
    }

    private static EditorSavePacket nothing() { return new EditorSavePacket(Map.of(), Map.of()); }

    static EditorSavePacket decode(FriendlyByteBuf b) {
        Map<AppearanceSection, int[]> edits = new EnumMap<>(AppearanceSection.class);
        Map<DrawingKind, byte[]> drawings = new EnumMap<>(DrawingKind.class);
        try {
            int n = b.readVarInt();
            if (n < 0 || n > AppearanceSection.values().length) return nothing();
            for (int i = 0; i < n; i++) {
                AppearanceSection s = AppearanceSection.fromName(b.readUtf(32));
                int len = b.readVarInt();
                if (s == null || len < 0 || len > MAX_VALUES) return nothing();
                int[] values = new int[len];
                for (int j = 0; j < len; j++) values[j] = b.readVarInt();
                edits.put(s, values);
            }
            int d = b.readVarInt();
            if (d < 0 || d > DrawingKind.values().length) return nothing();
            for (int i = 0; i < d; i++) {
                DrawingKind kind = DrawingKind.fromName(b.readUtf(32));
                if (kind == null || b.readableBytes() < kind.width() * kind.height()) return nothing();
                byte[] grid = new byte[kind.width() * kind.height()];
                b.readBytes(grid);
                drawings.put(kind, grid);
            }
        } catch (RuntimeException e) {
            return nothing();
        }
        return new EditorSavePacket(edits, drawings);
    }

    static void handle(EditorSavePacket p, Supplier<NetworkEvent.Context> c) {
        c.get().enqueueWork(() -> {
            ServerPlayer sender = c.get().getSender();
            if (sender != null) CharacterManager.applyClientEdit(sender, p.edits, p.drawings);
        });
        c.get().setPacketHandled(true);
    }
}
