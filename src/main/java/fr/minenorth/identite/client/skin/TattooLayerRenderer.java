package fr.minenorth.identite.client.skin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * La couche de tatouages haute définition : deux fines faces (devant et dos du torse) posées entre la peau et la couche
 * extérieure du skin, avec une image quatre fois plus fine. Les vêtements dessinés sur la couche extérieure (sous-vêtements)
 * passent donc par-dessus les tatouages, comme dans la réalité.
 */
@OnlyIn(Dist.CLIENT)
final class TattooLayerRenderer {
    private TattooLayerRenderer() {}

    /** Le torse mesure 8 × 12 ; la face est un peu devant la peau (z = 2) et derrière la couche extérieure (z = 2,25). */
    private static final float HALF = 4f / 16f, HEIGHT = 12f / 16f, Z = 2.12f / 16f;

    static void draw(PoseStack ps, MultiBufferSource buffers, int light, PlayerModel<?> model, ResourceLocation texture) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(texture));
        ps.pushPose();
        model.body.translateAndRotate(ps);
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        // devant : l'image va de la droite du personnage (x = +4) à sa gauche ; derrière : de x = -4 à +4
        quad(m, n, vc, light, -1, 0.002f, 0.498f,
                HALF, 0, -Z, -HALF, 0, -Z, -HALF, HEIGHT, -Z, HALF, HEIGHT, -Z);
        quad(m, n, vc, light, 1, 0.502f, 0.998f,
                -HALF, 0, Z, HALF, 0, Z, HALF, HEIGHT, Z, -HALF, HEIGHT, Z);
        ps.popPose();
    }

    /** Une face et son envers (le rendu translucide écarte l'une des deux selon le sens), v de 0 à 0,75 (48 sur 64). */
    private static void quad(Matrix4f m, Matrix3f n, VertexConsumer vc, int light, float nz, float u0, float u1,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        float v0 = 0.002f, v1 = 0.748f;
        vertex(m, n, vc, light, ax, ay, az, u0, v0, nz);
        vertex(m, n, vc, light, bx, by, bz, u1, v0, nz);
        vertex(m, n, vc, light, cx, cy, cz, u1, v1, nz);
        vertex(m, n, vc, light, dx, dy, dz, u0, v1, nz);
        vertex(m, n, vc, light, dx, dy, dz, u0, v1, -nz);
        vertex(m, n, vc, light, cx, cy, cz, u1, v1, -nz);
        vertex(m, n, vc, light, bx, by, bz, u1, v0, -nz);
        vertex(m, n, vc, light, ax, ay, az, u0, v0, -nz);
    }

    private static void vertex(Matrix4f m, Matrix3f n, VertexConsumer vc, int light, float x, float y, float z, float u, float v, float nz) {
        vc.vertex(m, x, y, z).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(n, 0, 0, nz).endVertex();
    }
}
