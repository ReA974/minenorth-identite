package fr.minenorth.identite.client.skin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Rendu de joueur vanilla, mais avec le skin créé quand le joueur l'a choisi. */
@OnlyIn(Dist.CLIENT)
public class SkinPlayerRenderer extends PlayerRenderer {
    public SkinPlayerRenderer(EntityRendererProvider.Context ctx, boolean slim) {
        super(ctx, slim);
        addLayer(new HairLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractClientPlayer player) {
        return ClientSkins.active(player.getUUID()).map(ClientSkins.Entry::texture).orElseGet(() -> super.getTextureLocation(player));
    }

    // En vue subjective, vanilla dessine la main avec player.getSkinTextureLocation() et non getTextureLocation() :
    // on la redessine avec notre texture.

    @Override
    public void renderRightHand(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer player) {
        hand(ps, buf, light, player, getModel().rightArm, getModel().rightSleeve);
    }

    @Override
    public void renderLeftHand(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer player) {
        hand(ps, buf, light, player, getModel().leftArm, getModel().leftSleeve);
    }

    private void hand(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer player, ModelPart arm, ModelPart sleeve) {
        PlayerModel<AbstractClientPlayer> model = getModel();
        model.setAllVisible(true);
        model.leftSleeve.visible = player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
        model.rightSleeve.visible = player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
        model.attackTime = 0.0F;
        model.crouching = false;
        model.swimAmount = 0.0F;
        model.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        ResourceLocation texture = getTextureLocation(player);
        arm.xRot = 0.0F;
        arm.render(ps, buf.getBuffer(RenderType.entitySolid(texture)), light, OverlayTexture.NO_OVERLAY);
        sleeve.xRot = 0.0F;
        sleeve.render(ps, buf.getBuffer(RenderType.entityTranslucent(texture)), light, OverlayTexture.NO_OVERLAY);
    }
}
