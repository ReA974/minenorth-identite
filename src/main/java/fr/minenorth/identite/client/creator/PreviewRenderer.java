package fr.minenorth.identite.client.creator;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.client.skin.ClientSkins;
import fr.minenorth.identite.document.Gender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Aperçu 3D du personnage : un modèle de joueur rendu directement avec la texture générée (aucune entité). */
@OnlyIn(Dist.CLIENT)
public final class PreviewRenderer {
    private PreviewRenderer() {}

    /** Caméra : libellé, rotation, zoom, hauteur visée (en blocs au-dessus des pieds). */
    public record Cam(String label, float yaw, float zoom, float focus) {}

    public static final Cam[] CAMS = {
            new Cam("Corps", 0, 1.0f, 1.0f), new Cam("Buste", 0, 1.9f, 1.4f), new Cam("Visage", 0, 3.6f, 1.75f),
            new Cam("Dos", 180, 1.0f, 1.0f), new Cam("Br. G", -65, 2.0f, 1.25f), new Cam("Br. D", 65, 2.0f, 1.25f),
            new Cam("Jambes", 0, 2.0f, 0.5f)};

    private static PlayerModel<?> classic, slim;

    private static PlayerModel<?> model(boolean slimArms) {
        if (slimArms) {
            if (slim == null) slim = new PlayerModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
            return slim;
        }
        if (classic == null) classic = new PlayerModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        return classic;
    }

    /** Dessine le modèle dans le rectangle donné ; texture déjà à jour (ClientSkins.preview). */
    public static void render(GuiGraphics g, int x, int y, int w, int h, Gender gender, ResourceLocation texture,
                              float yaw, float zoom, float focus, Appearance appearance) {
        float scale = h / 2.3f * zoom;
        float cx = x + w / 2f;
        float feetY = y + h / 2f + focus * scale;
        g.enableScissor(x, y, x + w, y + h);
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(cx, feetY, 100.0);
        ps.scale(scale, scale, -scale);
        ps.mulPose(Axis.ZP.rotationDegrees(180f));
        ps.mulPose(Axis.YP.rotationDegrees(yaw));
        ps.scale(-1f, -1f, 1f);
        ps.translate(0f, -1.501f, 0f);
        Lighting.setupForEntityInInventory();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        PlayerModel<?> m = model(gender.slimArms());
        m.setAllVisible(true);
        m.young = false;
        MultiBufferSource.BufferSource buffers = g.bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(texture));
        m.renderToBuffer(ps, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
        fr.minenorth.identite.client.skin.HairRenderer.drawAll(ps, buffers, LightTexture.FULL_BRIGHT, m, appearance, false);
        g.flush();
        ps.popPose();
        Lighting.setupFor3DItems();
        g.disableScissor();
    }

    /** Régénère la texture d'aperçu puis dessine. */
    public static void render(GuiGraphics g, int x, int y, int w, int h, Gender gender, Appearance appearance,
                              float yaw, float zoom, float focus) {
        render(g, x, y, w, h, gender, ClientSkins.preview(gender, appearance), yaw, zoom, focus, appearance);
    }
}
