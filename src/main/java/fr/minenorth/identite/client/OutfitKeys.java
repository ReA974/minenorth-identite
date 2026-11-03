package fr.minenorth.identite.client;

import com.mojang.blaze3d.platform.InputConstants;
import fr.minenorth.identite.MineNorthIdentite;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Touche « Ma tenue » (K par défaut, modifiable dans les options) : ouvre l'écran pour retirer ses vêtements. */
public final class OutfitKeys {
    private OutfitKeys() {}

    static final KeyMapping OUTFIT = new KeyMapping("key.minenorthidentite.outfit",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.minenorthidentite");

    @Mod.EventBusSubscriber(modid = MineNorthIdentite.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent e) { e.register(OUTFIT); }
    }

    @Mod.EventBusSubscriber(modid = MineNorthIdentite.MOD_ID, value = Dist.CLIENT)
    public static final class ForgeBus {
        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            while (OUTFIT.consumeClick()) {
                if (mc.player != null && mc.screen == null) mc.setScreen(new OutfitScreen());
            }
        }
    }
}
