package fr.minenorth.identite.client.skin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
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

    /**
     * Reprend le modèle du rendu vanilla. Le modèle construit ici sortait avec des bras de 3 de large, même pour un homme (vu avec
     * /diagbras), alors que le skin est peint sur 4 : tatouages décollés, bras fins dans les manches, montre trop grosse.
     */
    void useModelOf(PlayerRenderer vanilla) {
        model = vanilla.getModel();
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractClientPlayer player) {
        return ClientSkins.active(player.getUUID()).map(ClientSkins.Entry::texture).orElseGet(() -> super.getTextureLocation(player));
    }

    // En vue subjective, vanilla dessine la main avec player.getSkinTextureLocation(). PlayerInfoSkin y met déjà le skin créé :
    // on laisse donc vanilla dessiner le bras (pose calculée d'après l'objet tenu, et les mods d'armes comme TACZ qui
    // repositionnent le bras s'y attendent). Redessiner nous-mêmes reprenait la pose du dernier rendu à la 3e personne
    // (bras tendu pour viser) : un bras énorme et étiré traversait l'écran avec une arme en main.
    // Repli seulement si PlayerInfoSkin n'a pas pu poser la texture.

    @Override
    public void renderRightHand(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer player) {
        if (PlayerInfoSkin.works() || ClientSkins.active(player.getUUID()).isEmpty()) super.renderRightHand(ps, buf, light, player);
        else hand(ps, buf, light, player, getModel().rightArm, getModel().rightSleeve);
    }

    @Override
    public void renderLeftHand(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer player) {
        if (PlayerInfoSkin.works() || ClientSkins.active(player.getUUID()).isEmpty()) super.renderLeftHand(ps, buf, light, player);
        else hand(ps, buf, light, player, getModel().leftArm, getModel().leftSleeve);
    }

    private void hand(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer player, ModelPart arm, ModelPart sleeve) {
        PlayerModel<AbstractClientPlayer> model = getModel();
        model.setAllVisible(false);
        arm.visible = true;
        sleeve.visible = arm == model.leftArm ? player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE) : player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
        // pas de pose héritée du rendu à la 3e personne (arme épaulée, arc…) ni d'échelle laissée par un calque
        model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        for (ModelPart p : new ModelPart[]{arm, sleeve}) { p.resetPose(); p.xScale = 1f; p.yScale = 1f; p.zScale = 1f; }
        model.attackTime = 0.0F;
        model.crouching = false;
        model.swimAmount = 0.0F;
        model.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        ResourceLocation texture = getTextureLocation(player);
        arm.xRot = 0.0F;
        arm.render(ps, buf.getBuffer(RenderType.entitySolid(texture)), light, OverlayTexture.NO_OVERLAY);
        sleeve.xRot = 0.0F;
        sleeve.render(ps, buf.getBuffer(RenderType.entityTranslucent(texture)), light, OverlayTexture.NO_OVERLAY);
        model.setAllVisible(true);
    }
}
