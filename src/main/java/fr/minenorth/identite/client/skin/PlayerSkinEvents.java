package fr.minenorth.identite.client.skin;

import fr.minenorth.identite.MineNorthIdentite;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Substitue le rendu d'un joueur (et de sa main en vue subjective) par celui qui porte le skin créé. Le sexe
 * choisit le modèle : bras fins pour Femme, classiques pour Homme. Sans mixin.
 */
public final class PlayerSkinEvents {
    private PlayerSkinEvents() {}

    private static PlayerRenderer defaultRenderer, slimRenderer;
    private static boolean rendering, renderingArm;
    private static boolean layersSynced;

    /** Recopie sur nos rendus les calques ajoutés par les autres mods au rendu vanilla (une fois tous les mods chargés). */
    private static void syncLayers() {
        layersSynced = true;
        var skins = Minecraft.getInstance().getEntityRenderDispatcher().getSkinMap();
        ExtraLayers.copy(skins.get("default") instanceof PlayerRenderer p ? p : null, defaultRenderer);
        ExtraLayers.copy(skins.get("slim") instanceof PlayerRenderer p ? p : null, slimRenderer);
        if (skins.get("default") instanceof PlayerRenderer p && defaultRenderer instanceof SkinPlayerRenderer s) s.useModelOf(p);
        if (skins.get("slim") instanceof PlayerRenderer p && slimRenderer instanceof SkinPlayerRenderer s) s.useModelOf(p);
    }

    @Mod.EventBusSubscriber(modid = MineNorthIdentite.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        // Chaque mod a son propre bus : la priorité ne garantit pas de passer après les autres mods. On crée donc
        // nos rendus ici, et on leur recopie les calques des autres mods plus tard (premier affichage, voir syncLayers).
        @SubscribeEvent
        public static void onAddLayers(EntityRenderersEvent.AddLayers e) {
            defaultRenderer = new SkinPlayerRenderer(e.getContext(), false);
            slimRenderer = new SkinPlayerRenderer(e.getContext(), true);
            layersSynced = false;
        }
    }

    @Mod.EventBusSubscriber(modid = MineNorthIdentite.MOD_ID, value = Dist.CLIENT)
    public static final class ForgeBus {
        @SubscribeEvent
        public static void onRenderPlayer(RenderPlayerEvent.Pre e) {
            if (rendering || !(e.getEntity() instanceof AbstractClientPlayer p)) return;
            PlayerRenderer r = rendererFor(p);
            if (r == null) return;
            if (!layersSynced) syncLayers();
            e.setCanceled(true);
            rendering = true;   // notre propre rendu renvoie un RenderPlayerEvent.Pre
            try {
                r.render(p, p.getYRot(), e.getPartialTick(), e.getPoseStack(), e.getMultiBufferSource(), e.getPackedLight());
            } finally {
                rendering = false;
            }
        }

        @SubscribeEvent
        public static void onRenderArm(RenderArmEvent e) {
            if (renderingArm) return;
            PlayerRenderer r = rendererFor(e.getPlayer());
            if (r == null) return;
            e.setCanceled(true);
            renderingArm = true;   // renderRightHand/LeftHand renvoient eux-mêmes un RenderArmEvent
            try {
                if (e.getArm() == HumanoidArm.RIGHT) {
                    r.renderRightHand(e.getPoseStack(), e.getMultiBufferSource(), e.getPackedLight(), e.getPlayer());
                } else {
                    r.renderLeftHand(e.getPoseStack(), e.getMultiBufferSource(), e.getPackedLight(), e.getPlayer());
                }
            } finally {
                renderingArm = false;
            }
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
            ClientSkins.clear();
        }
    }

    /** Bras fins du personnage dessiné, null s'il n'a pas de skin créé. Lu par le mod Vêtements (réflexion) : manches et montres suivent le vrai bras. */
    public static Boolean slimArms(java.util.UUID player) {
        return ClientSkins.active(player).map(entry -> entry.gender().slimArms()).orElse(null);
    }

    static PlayerRenderer rendererFor(AbstractClientPlayer player) {
        return ClientSkins.active(player.getUUID())
                .map(entry -> entry.gender().slimArms() ? slimRenderer : defaultRenderer)
                .orElse(null);
    }
}
