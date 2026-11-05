package fr.minenorth.identite.client.skin;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import fr.minenorth.identite.MineNorthIdentite;
import fr.minenorth.identite.hair.HairPieces;
import fr.minenorth.identite.hair.HairTexture;
import fr.minenorth.identite.skin.Palette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/** Dessine les blocs de cheveux en 3D ({@link HairPieces}) sur un modèle de joueur déjà positionné. */
@OnlyIn(Dist.CLIENT)
public final class HairRenderer {
    private HairRenderer() {}

    private static final Map<Integer, ResourceLocation> TEXTURES = new HashMap<>();

    /** Une texture par couleur de cheveux, créée à la demande. */
    private static ResourceLocation texture(int colorIndex) {
        return TEXTURES.computeIfAbsent(colorIndex, i -> {
            NativeImage image = new NativeImage(NativeImage.Format.RGBA, HairTexture.WIDTH, HairTexture.HEIGHT, true);
            int[] argb = HairTexture.pixels(Palette.hairColor(i));
            for (int y = 0; y < HairTexture.HEIGHT; y++) {
                for (int x = 0; x < HairTexture.WIDTH; x++) {
                    image.setPixelRGBA(x, y, Palette.argbToAbgr(argb[y * HairTexture.WIDTH + x]));
                }
            }
            ResourceLocation location = new ResourceLocation(MineNorthIdentite.MOD_ID, "hair/" + i);
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
            return location;
        });
    }

    private static final Map<Integer, ResourceLocation> METALS = new HashMap<>();

    /** Une texture par métal de piercing. */
    private static ResourceLocation metalTexture(int index) {
        return METALS.computeIfAbsent(index, i -> {
            NativeImage image = new NativeImage(NativeImage.Format.RGBA, HairTexture.WIDTH, HairTexture.HEIGHT, true);
            int[] argb = HairTexture.metalPixels(Palette.metal(i));
            for (int y = 0; y < HairTexture.HEIGHT; y++) {
                for (int x = 0; x < HairTexture.WIDTH; x++) {
                    image.setPixelRGBA(x, y, Palette.argbToAbgr(argb[y * HairTexture.WIDTH + x]));
                }
            }
            ResourceLocation location = new ResourceLocation(MineNorthIdentite.MOD_ID, "metal/" + i);
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
            return location;
        });
    }

    /** Cheveux, barbe et piercings en volume 3D sur un modèle de joueur déjà positionné. */
    public static void drawAll(PoseStack ps, MultiBufferSource buffers, int light, PlayerModel<?> model,
                               fr.minenorth.identite.appearance.Appearance a, boolean headCovered, ResourceLocation tattoo) {
        if (tattoo != null) TattooLayerRenderer.draw(ps, buffers, light, model, tattoo);
        int[] hair = a.get(fr.minenorth.identite.appearance.AppearanceSection.HAIR);
        int[] beard = a.get(fr.minenorth.identite.appearance.AppearanceSection.FACIAL_HAIR);
        int[] piercings = a.get(fr.minenorth.identite.appearance.AppearanceSection.PIERCINGS);
        drawPieces(ps, buffers, light, model, HairPieces.forStyle(hair[0]), texture(hair[1]), headCovered);
        drawPieces(ps, buffers, light, model, fr.minenorth.identite.hair.BeardPieces.forStyle(beard[0]), texture(beard[1]), headCovered);
        drawPieces(ps, buffers, light, model, fr.minenorth.identite.hair.PiercingPieces.of(piercings),
                metalTexture(piercings[fr.minenorth.identite.hair.PiercingPieces.METAL]), headCovered);
    }

    private static void drawPieces(PoseStack ps, MultiBufferSource buffers, int light, PlayerModel<?> model,
                                   java.util.List<HairPieces.Piece> pieces, ResourceLocation texture, boolean headCovered) {
        if (pieces.isEmpty()) return;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        for (HairPieces.Piece p : pieces) {
            if (headCovered && p.part() == HairPieces.Part.HEAD) continue;
            ModelPart part = p.part() == HairPieces.Part.HEAD ? model.head : model.body;
            ps.pushPose();
            part.translateAndRotate(ps);
            ps.translate(p.px() / 16f, p.py() / 16f, p.pz() / 16f);
            if (p.rx() != 0f) ps.mulPose(Axis.XP.rotationDegrees(p.rx()));
            box(ps.last().pose(), ps.last().normal(), vc, light, p);
            ps.popPose();
        }
    }

    private static void box(Matrix4f m, Matrix3f n, VertexConsumer vc, int light, HairPieces.Piece p) {
        float x0 = p.x() / 16f, y0 = p.y() / 16f, z0 = p.z() / 16f;
        float x1 = x0 + p.w() / 16f, y1 = y0 + p.h() / 16f, z1 = z0 + p.d() / 16f;
        float v0 = p.tone() / 3f + 0.01f, v1 = (p.tone() + 1) / 3f - 0.01f;
        // haut (y petit), bas, devant (z petit), derrière, côté gauche (x petit), côté droit
        quad(m, n, vc, light, v0, v1, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        quad(m, n, vc, light, v0, v1, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
        quad(m, n, vc, light, v0, v1, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
        quad(m, n, vc, light, v0, v1, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        quad(m, n, vc, light, v0, v1, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        quad(m, n, vc, light, v0, v1, 1, 0, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
    }

    private static void quad(Matrix4f m, Matrix3f n, VertexConsumer vc, int light, float v0, float v1,
                             float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        vertex(m, n, vc, light, ax, ay, az, 0.02f, v0, nx, ny, nz);
        vertex(m, n, vc, light, bx, by, bz, 0.98f, v0, nx, ny, nz);
        vertex(m, n, vc, light, cx, cy, cz, 0.98f, v1, nx, ny, nz);
        vertex(m, n, vc, light, dx, dy, dz, 0.02f, v1, nx, ny, nz);
    }

    private static void vertex(Matrix4f m, Matrix3f n, VertexConsumer vc, int light, float x, float y, float z,
                               float u, float v, float nx, float ny, float nz) {
        vc.vertex(m, x, y, z).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(n, nx, ny, nz).endVertex();
    }
}
