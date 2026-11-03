package fr.minenorth.identite.document;

import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Mc2Discord ignore les messages de chat annulés (cf. rpChat). On lui renvoie donc le message nous-mêmes,
 * par réflexion : sans Mc2Discord (ou si son API change), il ne se passe rien.
 */
final class Mc2DiscordRelay {
    private Mc2DiscordRelay() {}

    static void chat(ServerPlayer player, String rpName, String message) {
        try {
            Class<?> entity = Class.forName("fr.denisd3d.mc2discord.core.entities.PlayerEntity");
            Object who = entity.getConstructor(String.class, String.class, UUID.class)
                    .newInstance(player.getGameProfile().getName(), rpName, player.getUUID());
            Method send = Class.forName("fr.denisd3d.mc2discord.core.events.MinecraftEvents")
                    .getMethod("onMinecraftChatMessageEvent", String.class, entity);
            send.invoke(null, message, who);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Mc2Discord absent ou incompatible
        }
    }
}
