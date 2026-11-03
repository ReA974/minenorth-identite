package fr.minenorth.identite.client.creator;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import fr.minenorth.identite.drawing.HeadImport;
import fr.minenorth.identite.skin.Palette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

/**
 * Lit le visage du skin Minecraft du joueur (celui de son compte, tel que le serveur le connaît) et le ramène à la
 * palette limitée. Sans skin (serveur hors-ligne, compte sans skin), rien ne revient : l'écran le dit.
 */
@OnlyIn(Dist.CLIENT)
final class MojangHead {
    private MojangHead() {}

    static void fetch(Consumer<byte[]> onFace, Consumer<String> onError) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            onError.accept("Vous n'êtes pas connecté.");
            return;
        }
        PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        if (info == null) {
            onError.accept("Votre profil est introuvable.");
            return;
        }
        GameProfile profile = info.getProfile();
        mc.getSkinManager().registerSkins(profile, (type, location, texture) -> {
            if (type == MinecraftProfileTexture.Type.SKIN) mc.execute(() -> read(location, onFace, onError));
        }, true);
    }

    private static void read(ResourceLocation location, Consumer<byte[]> onFace, Consumer<String> onError) {
        Minecraft mc = Minecraft.getInstance();
        AbstractTexture texture = mc.getTextureManager().getTexture(location);
        try (NativeImage image = new NativeImage(64, 64, false)) {
            RenderSystem.bindTexture(texture.getId());
            image.downloadTexture(0, false);
            int[] argb = new int[64 * 64];
            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 64; x++) argb[y * 64 + x] = Palette.argbToAbgr(image.getPixelRGBA(x, y));
            }
            onFace.accept(HeadImport.faceFromSkin(argb, 64, 64));
        } catch (RuntimeException e) {
            onError.accept("Impossible de lire votre skin Minecraft.");
        }
    }
}
