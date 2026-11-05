package fr.minenorth.identite.client.skin;

import com.mojang.blaze3d.platform.NativeImage;
import fr.minenorth.identite.MineNorthIdentite;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Commande cliente /diagbras : montre, pour les joueurs proches, ce qui décide vraiment de la largeur des bras (genre, modèle du
 * profil, rendu utilisé, largeur du cube, pixels opaques du skin, avis du mod Vêtements). Résultat dans le chat et dans le journal.
 */
@Mod.EventBusSubscriber(modid = MineNorthIdentite.MOD_ID, value = Dist.CLIENT)
public final class ArmDiagnostic {
    private static final Logger LOG = LoggerFactory.getLogger("minenorthidentite");
    private static final double RANGE = 32.0;

    private ArmDiagnostic() {}

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("diagbras").executes(c -> {
            run();
            return 1;
        }));
    }

    private static void run() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        for (Player p : mc.level.players()) {
            if (p.distanceTo(mc.player) > RANGE || !(p instanceof AbstractClientPlayer c)) continue;
            for (String line : report(mc, c)) {
                LOG.info("[diagbras] {}", line);
                mc.player.displayClientMessage(Component.literal(line), false);
            }
        }
    }

    private static List<String> report(Minecraft mc, AbstractClientPlayer p) {
        List<String> out = new ArrayList<>();
        Optional<ClientSkins.Entry> entry = ClientSkins.active(p.getUUID());
        out.add("§e" + p.getGameProfile().getName() + "§r : genre " + entry.map(x -> x.gender().name()).orElse("aucun skin créé")
                + ", modèle du profil " + p.getModelName() + ", couche tatouage " + entry.map(x -> x.tattoo() != null ? "oui" : "non").orElse("-"));
        EntityRenderer<?> vanilla = mc.getEntityRenderDispatcher().getRenderer(p);
        out.add("  rendu vanilla : " + describe(vanilla));
        PlayerRenderer ours = PlayerSkinEvents.rendererFor(p);
        out.add("  rendu identité : " + (ours == null ? "aucun" : describe(ours)));
        entry.ifPresent(x -> out.add("  skin, colonnes opaques du bras droit : " + opaqueColumns(mc, x)));
        out.add("  vêtements, bras fins : " + clothesSlim(p));
        return out;
    }

    private static String describe(EntityRenderer<?> r) {
        String name = r.getClass().getName();
        if (!(r instanceof PlayerRenderer pr)) return name;
        PlayerModel<AbstractClientPlayer> m = pr.getModel();
        return name + ", bras droit " + width(m.rightArm) + ", gauche " + width(m.leftArm) + ", manche " + width(m.rightSleeve)
                + ", échelle x " + m.rightArm.xScale;
    }

    private static String width(ModelPart part) {
        try {
            ModelPart.Cube cube = part.getRandomCube(RandomSource.create());
            return String.valueOf(cube.maxX - cube.minX);
        } catch (RuntimeException e) {
            return "?";
        }
    }

    /** Ligne du milieu de la face avant du bras droit (x 44 à 47, y 26) : 4 colonnes opaques pour un bras large. */
    private static String opaqueColumns(Minecraft mc, ClientSkins.Entry entry) {
        AbstractTexture t = mc.getTextureManager().getTexture(entry.texture());
        if (!(t instanceof DynamicTexture d) || d.getPixels() == null) return "?";
        NativeImage image = d.getPixels();
        int n = 0;
        for (int x = 44; x < 48; x++) if (((image.getPixelRGBA(x, 26) >>> 24) & 0xFF) != 0) n++;
        return n + " / 4";
    }

    private static String clothesSlim(AbstractClientPlayer p) {
        try {
            Object slim = Class.forName("com.minenorth_vetements.client.SlimArmor")
                    .getMethod("slim", net.minecraft.world.entity.LivingEntity.class).invoke(null, p);
            return String.valueOf(slim);
        } catch (ReflectiveOperationException | LinkageError e) {
            return "?";
        }
    }
}
