package fr.minenorth.identite.client.skin;

import com.mojang.blaze3d.platform.NativeImage;
import fr.minenorth.identite.MineNorthIdentite;
import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.document.Gender;
import fr.minenorth.identite.skin.Palette;
import fr.minenorth.identite.skin.SkinComposer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Fiches d'apparence reçues du serveur et textures de skin générées à partir d'elles. Tout se passe sur le thread
 * de rendu (les paquets sont traités par enqueueWork).
 */
@OnlyIn(Dist.CLIENT)
public final class ClientSkins {
    private ClientSkins() {}

    public record Entry(Gender gender, Appearance appearance, ResourceLocation texture) {}

    private static final Map<UUID, Entry> ENTRIES = new HashMap<>();
    private static DynamicTexture previewTexture;
    private static final ResourceLocation PREVIEW = new ResourceLocation(MineNorthIdentite.MOD_ID, "preview");

    /** Enregistre (ou remplace) la fiche d'un joueur et régénère son skin. */
    public static void put(UUID player, Gender gender, Appearance appearance) {
        remove(player);
        ResourceLocation location = new ResourceLocation(MineNorthIdentite.MOD_ID, "skin/" + player);
        Minecraft.getInstance().getTextureManager().register(location, texture(gender, appearance));
        ENTRIES.put(player, new Entry(gender, appearance, location));
    }

    /** Retire la fiche d'un joueur (reset) : son skin Minecraft reprend. */
    public static void remove(UUID player) {
        Entry old = ENTRIES.remove(player);
        if (old != null) Minecraft.getInstance().getTextureManager().release(old.texture());
    }

    public static void clear() {
        for (UUID id : java.util.List.copyOf(ENTRIES.keySet())) remove(id);
    }

    /** La fiche du joueur, seulement s'il a choisi « Choisir mon skin ». */
    /** La fiche du joueur, qu'il ait choisi son skin ou non. */
    public static Optional<Entry> entry(UUID player) { return Optional.ofNullable(ENTRIES.get(player)); }

    public static Optional<Entry> active(UUID player) {
        Entry e = ENTRIES.get(player);
        return e != null && e.appearance().customSkin() ? Optional.of(e) : Optional.empty();
    }

    public static ResourceLocation previewLocation() { return PREVIEW; }

    /** Texture d'aperçu de l'écran de création, régénérée à chaque appel. */
    public static ResourceLocation preview(Gender gender, Appearance appearance) {
        if (previewTexture == null) {
            previewTexture = texture(gender, appearance);
            Minecraft.getInstance().getTextureManager().register(PREVIEW, previewTexture);
        } else {
            fill(previewTexture.getPixels(), SkinComposer.compose(gender, appearance));
            previewTexture.upload();
        }
        return PREVIEW;
    }

    private static DynamicTexture texture(Gender gender, Appearance appearance) {
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, SkinComposer.SIZE, SkinComposer.SIZE, true);
        fill(image, SkinComposer.compose(gender, appearance));
        return new DynamicTexture(image);
    }

    private static void fill(NativeImage image, int[] argb) {
        for (int y = 0; y < SkinComposer.SIZE; y++) {
            for (int x = 0; x < SkinComposer.SIZE; x++) {
                image.setPixelRGBA(x, y, Palette.argbToAbgr(argb[y * SkinComposer.SIZE + x]));
            }
        }
    }
}
