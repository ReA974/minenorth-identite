package fr.minenorth.identite.document;

import fr.minenorth.identite.IdentityConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Messages de connexion / déconnexion affichant le nom RP (le message d'EssentialsX affiche le pseudo). */
@Mod.EventBusSubscriber
public final class JoinMessages {
    private JoinMessages() {}

    @SubscribeEvent
    public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        announce(event.getEntity(), IdentityConfig.JOIN_MESSAGE.get());
    }

    @SubscribeEvent
    public static void onQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        announce(event.getEntity(), IdentityConfig.QUIT_MESSAGE.get());
    }

    private static void announce(net.minecraft.world.entity.player.Player entity, String format) {
        if (!(entity instanceof ServerPlayer player) || !IdentityConfig.JOIN_MESSAGES.get() || format.isBlank()) return;
        IdentityData.Profile profile = IdentityData.get(player.serverLevel()).get(player.getUUID());
        // seul un personnage complet a un nom RP : une ancienne identité ou une création en cours reste au pseudo
        boolean character = profile != null && fr.minenorth.identite.character.CharacterManager.hasCharacter(player.serverLevel(), player.getUUID());
        if (!character && !IdentityConfig.ANNOUNCE_WITHOUT_IDENTITY.get()) return;

        String pseudo = player.getGameProfile().getName();
        String nom = !character ? pseudo : profile.firstName() + " " + profile.lastName();
        String text = format.replace("{nom}", nom).replace("{pseudo}", pseudo).replace('&', '§');
        player.server.getPlayerList().broadcastSystemMessage(Component.literal(text), false);
    }
}
