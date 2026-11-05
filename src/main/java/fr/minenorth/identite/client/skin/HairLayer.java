package fr.minenorth.identite.client.skin;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.minenorth.identite.appearance.AppearanceSection;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Calque du rendu de joueur : ajoute les cheveux en volume (chignon, queues, cheveux longs…) au personnage créé. */
@OnlyIn(Dist.CLIENT)
public class HairLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public HairLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (player.isInvisible()) return;
        var entry = ClientSkins.active(player.getUUID());
        if (entry.isEmpty()) return;
        boolean helmet = !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
        HairRenderer.drawAll(ps, buffers, light, getParentModel(), entry.get().appearance(), helmet);
    }
}
