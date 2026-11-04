package fr.minenorth.identite.client.skin;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import fr.minenorth.identite.MineNorthIdentite;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Fait connaître le skin du personnage à tout le jeu : l'inventaire, la liste des joueurs, les plaques de nom… lisent
 * le skin enregistré dans le {@link PlayerInfo} du joueur (celui de son compte Mojang). On y met la texture générée, et
 * on remet celui de Mojang quand le personnage n'a plus de skin créé (reset). Sans mixin : les champs sont retrouvés
 * par leur type. Le chargement du skin Mojang est asynchrone et peut repasser par-dessus : on réaffirme à chaque tick.
 */
public final class PlayerInfoSkin {
    private static final Logger LOG = LoggerFactory.getLogger("minenorthidentite");

    private PlayerInfoSkin() {}

    private static Field textures, model, pending;
    private static boolean ready, failed;
    private static final Set<UUID> overridden = new HashSet<>();

    /** Vrai si le skin créé est bien posé dans le PlayerInfo : tout le jeu (dont la main en vue subjective) le lit alors tout seul. */
    static boolean works() { return init(); }

    private static boolean init() {
        if (ready) return true;
        if (failed) return false;
        Optional<Field> t = FieldsByType.unique(PlayerInfo.class, Map.class);
        Optional<Field> m = FieldsByType.unique(PlayerInfo.class, String.class);
        Optional<Field> p = FieldsByType.unique(PlayerInfo.class, boolean.class);
        if (t.isEmpty() || m.isEmpty() || p.isEmpty()) {
            failed = true;
            LOG.warn("Skin du personnage : champs de PlayerInfo introuvables, seul le rendu du joueur utilisera le skin créé");
            return false;
        }
        textures = t.get();
        model = m.get();
        pending = p.get();
        ready = true;
        return true;
    }

    /** Met à jour tous les joueurs connus du client. */
    @SuppressWarnings("unchecked")
    static void sync(Minecraft mc) {
        if (mc.getConnection() == null || !init()) return;
        try {
            for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
                UUID id = info.getProfile().getId();
                Optional<ClientSkins.Entry> active = ClientSkins.active(id);
                Map<Object, Object> map = (Map<Object, Object>) textures.get(info);
                if (active.isPresent()) {
                    ResourceLocation ours = active.get().texture();
                    if (!ours.equals(map.get(MinecraftProfileTexture.Type.SKIN))) map.put(MinecraftProfileTexture.Type.SKIN, ours);
                    String wanted = active.get().gender().slimArms() ? "slim" : "default";
                    if (!wanted.equals(model.get(info))) model.set(info, wanted);
                    overridden.add(id);
                } else if (overridden.remove(id)) {
                    // retour au skin Mojang : on vide ce qu'on avait posé et on laisse le jeu le recharger
                    map.remove(MinecraftProfileTexture.Type.SKIN);
                    model.set(info, null);
                    pending.setBoolean(info, false);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed = true;
            LOG.warn("Skin du personnage : mise à jour du skin des joueurs abandonnée", e);
        }
    }

    @Mod.EventBusSubscriber(modid = MineNorthIdentite.MOD_ID, value = Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent
        public static void onTick(TickEvent.ClientTickEvent e) {
            if (e.phase == TickEvent.Phase.END) sync(Minecraft.getInstance());
        }
    }
}
