package fr.minenorth.identite.client.skin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.minenorth.identite.appearance.TattooZone;
import fr.minenorth.identite.skin.TattooRegions;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * La couche de tatouages haute définition : de fines faces posées entre la peau et la couche extérieure du skin sur le
 * torse, les bras et les jambes, avec une image quatre fois plus fine. Les vêtements dessinés sur la couche extérieure
 * (sous-vêtements) passent donc par-dessus les tatouages, comme dans la réalité.
 *
 * <p>Orientation : vu de face, la gauche de l'image est la gauche de l'écran (x négatif du modèle) ; vu de dos, c'est
 * l'inverse ; sur un côté, l'image va de l'arrière vers l'avant pour une face tournée vers x négatif, et de l'avant vers
 * l'arrière pour une face tournée vers x positif.
 */
@OnlyIn(Dist.CLIENT)
final class TattooLayerRenderer {
    private TattooLayerRenderer() {}

    /** Les faces sont un peu au-dessus de la peau (demi-épaisseur 2) et sous la couche extérieure (2,25). */
    private static final float OFFSET = 0.12f;

    static void draw(PoseStack ps, MultiBufferSource buffers, int light, PlayerModel<?> model, ResourceLocation texture, boolean slim) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(texture));
        part(ps, vc, light, model.body, TattooZone.TORSO, TattooZone.BACK, -4, 4, 0, 12, false);
        face(ps, vc, light, model.head);
        float armRight0 = -3, armRight1 = 1, armLeft0 = -1, armLeft1 = 3;
        if (slim) {
            armRight0 = -2;
            armLeft1 = 2;
        }
        limb(ps, vc, light, model.rightArm, TattooZone.RIGHT_ARM, armRight0, armRight1, -2, 10, true, slim);
        limb(ps, vc, light, model.leftArm, TattooZone.LEFT_ARM, armLeft0, armLeft1, -2, 10, false, slim);
        limb(ps, vc, light, model.rightLeg, TattooZone.RIGHT_LEG, -2, 2, 0, 12, true, false);
        limb(ps, vc, light, model.leftLeg, TattooZone.LEFT_LEG, -2, 2, 0, 12, false, false);
    }

    /** Le visage : maquillage et particularités de la peau, juste devant la peau de la tête (8 × 8, image de 32 × 32). */
    private static void face(PoseStack ps, VertexConsumer vc, int light, ModelPart head) {
        ps.pushPose();
        head.translateAndRotate(ps);
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        face(m, n, vc, light, -4, -8, -4 - OFFSET, 4, -8, -4 - OFFSET, 4, 0, -4 - OFFSET, -4, 0, -4 - OFFSET,
                TattooRegions.FACE_ORIGIN, 32, 32, 1f, 0, 0, -1);
        ps.popPose();
    }

    /** Le torse : devant et dos. */
    private static void part(PoseStack ps, VertexConsumer vc, int light, ModelPart part, TattooZone front, TattooZone back,
                             float xa, float xb, float ya, float yb, boolean unused) {
        ps.pushPose();
        part.translateAndRotate(ps);
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        int[] f = TattooRegions.origin(front, 0), b = TattooRegions.origin(back, 0);
        float w = TattooRegions.width(front);
        face(m, n, vc, light, xa, ya, -2 - OFFSET, xb, ya, -2 - OFFSET, xb, yb, -2 - OFFSET, xa, yb, -2 - OFFSET, f, w, TattooRegions.HEIGHT, 1f, 0, 0, -1);
        face(m, n, vc, light, xb, ya, 2 + OFFSET, xa, ya, 2 + OFFSET, xa, yb, 2 + OFFSET, xb, yb, 2 + OFFSET, b, w, TattooRegions.HEIGHT, 1f, 0, 0, 1);
        ps.popPose();
    }

    /** Un bras ou une jambe : avant, arrière et les deux côtés. */
    private static void limb(PoseStack ps, VertexConsumer vc, int light, ModelPart part, TattooZone zone, float xa, float xb,
                             float ya, float yb, boolean rightSide, boolean slim) {
        ps.pushPose();
        part.translateAndRotate(ps);
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        float w = TattooRegions.width(zone);
        float frontShare = slim && (zone == TattooZone.RIGHT_ARM || zone == TattooZone.LEFT_ARM) ? 0.75f : 1f;   // un bras fin n'a que 3 colonnes
        int[] front = TattooRegions.origin(zone, 0), outer = TattooRegions.origin(zone, 1), inner = TattooRegions.origin(zone, 2), back = TattooRegions.origin(zone, 3);
        face(m, n, vc, light, xa, ya, -2 - OFFSET, xb, ya, -2 - OFFSET, xb, yb, -2 - OFFSET, xa, yb, -2 - OFFSET, front, w, TattooRegions.HEIGHT, frontShare, 0, 0, -1);
        face(m, n, vc, light, xb, ya, 2 + OFFSET, xa, ya, 2 + OFFSET, xa, yb, 2 + OFFSET, xb, yb, 2 + OFFSET, back, w, TattooRegions.HEIGHT, frontShare, 0, 0, 1);
        // côté tourné vers x négatif : l'image va de l'arrière (z positif) vers l'avant ; côté x positif : l'inverse
        int[] minusSide = rightSide ? outer : inner, plusSide = rightSide ? inner : outer;
        face(m, n, vc, light, xa - OFFSET, ya, 2, xa - OFFSET, ya, -2, xa - OFFSET, yb, -2, xa - OFFSET, yb, 2, minusSide, w, TattooRegions.HEIGHT, 1f, -1, 0, 0);
        face(m, n, vc, light, xb + OFFSET, ya, -2, xb + OFFSET, ya, 2, xb + OFFSET, yb, 2, xb + OFFSET, yb, -2, plusSide, w, TattooRegions.HEIGHT, 1f, 1, 0, 0);
        ps.popPose();
    }

    /**
     * Une face : les quatre coins vus de l'extérieur (haut gauche, haut droit, bas droit, bas gauche) et la zone de l'image
     * (origine, largeur de 16 ou 32, hauteur 48 ; {@code share} : part de la largeur utilisée). La face et son envers sont
     * émis, le rendu translucide écarte celui qui tourne le dos.
     */
    private static void face(Matrix4f m, Matrix3f n, VertexConsumer vc, int light,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             int[] origin, float width, float height, float share, float nx, float ny, float nz) {
        float u0 = (origin[0] + 0.1f) / TattooRegions.ATLAS_W, u1 = (origin[0] + width * share - 0.1f) / TattooRegions.ATLAS_W;
        float v0 = (origin[1] + 0.1f) / TattooRegions.ATLAS_H, v1 = (origin[1] + height - 0.1f) / TattooRegions.ATLAS_H;
        vertex(m, n, vc, light, ax, ay, az, u0, v0, nx, ny, nz);
        vertex(m, n, vc, light, bx, by, bz, u1, v0, nx, ny, nz);
        vertex(m, n, vc, light, cx, cy, cz, u1, v1, nx, ny, nz);
        vertex(m, n, vc, light, dx, dy, dz, u0, v1, nx, ny, nz);
        vertex(m, n, vc, light, dx, dy, dz, u0, v1, -nx, -ny, -nz);
        vertex(m, n, vc, light, cx, cy, cz, u1, v1, -nx, -ny, -nz);
        vertex(m, n, vc, light, bx, by, bz, u1, v0, -nx, -ny, -nz);
        vertex(m, n, vc, light, ax, ay, az, u0, v0, -nx, -ny, -nz);
    }

    private static void vertex(Matrix4f m, Matrix3f n, VertexConsumer vc, int light, float x, float y, float z,
                               float u, float v, float nx, float ny, float nz) {
        vc.vertex(m, x / 16f, y / 16f, z / 16f).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(n, nx, ny, nz).endVertex();
    }
}
