package fr.minenorth.identite;

import fr.minenorth.api.PlayerWipeEvent;
import fr.minenorth.identite.document.IdentityManager;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Suppression d'un joueur depuis le panneau admin : sa carte d'identité est effacée. */
@Mod.EventBusSubscriber(modid = MineNorthIdentite.MOD_ID)
public final class IdentityWipe {
    private IdentityWipe() {}

    @SubscribeEvent
    public static void onWipe(PlayerWipeEvent e) {
        if (IdentityManager.reset(e.server(), e.player())) e.cleaned("identité");
    }
}
