package fr.minenorth.identite.client.skin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Le rendu du skin créé est un second PlayerRenderer : les calques ajoutés par d'autres mods au rendu vanilla
 * (vêtements, accessoires…) n'y sont pas. On les recrée ici, un par un, sur notre rendu.
 * Seuls les calques à constructeur {@code (RenderLayerParent)} sont repris ; les autres sont signalés dans le journal.
 */
final class ExtraLayers {
    private static final Logger LOG = LoggerFactory.getLogger("minenorthidentite");

    private ExtraLayers() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void copy(PlayerRenderer from, PlayerRenderer to) {
        if (from == null || to == null) return;
        java.util.List<String> found = new java.util.ArrayList<>(), copied = new java.util.ArrayList<>();
        try {
            java.util.Set<Class<?>> already = new java.util.HashSet<>();
            for (RenderLayer<?, ?> l : layersOf(to)) already.add(l.getClass());
            for (RenderLayer<?, ?> layer : layersOf(from)) {
                Class<?> type = layer.getClass();
                String pkg = type.getPackageName();
                if (pkg.startsWith("net.minecraft.") || pkg.startsWith("net.minecraftforge.")) continue;
                found.add(type.getSimpleName());
                if (already.contains(type)) continue;
                try {
                    RenderLayer copy = (RenderLayer) type.getConstructor(RenderLayerParent.class).newInstance(to);
                    to.addLayer(copy);
                    copied.add(type.getSimpleName());
                } catch (ReflectiveOperationException | RuntimeException e) {
                    LOG.warn("Calque {} non repris sur le skin du personnage : {}", type.getName(), e.toString());
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.warn("Calques des autres mods non repris sur le skin du personnage", e);
        }
        LOG.info("Calques d'autres mods sur le rendu vanilla : {} | copies sur le skin du personnage : {}", found, copied);
    }

    /** La liste des calques est protégée ; on la retrouve par son type, quel que soit le nom des champs. */
    @SuppressWarnings("unchecked")
    private static List<RenderLayer<?, ?>> layersOf(LivingEntityRenderer<?, ?> renderer) throws ReflectiveOperationException {
        for (Field f : LivingEntityRenderer.class.getDeclaredFields()) {
            if (List.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                return (List<RenderLayer<?, ?>>) f.get(renderer);
            }
        }
        throw new NoSuchFieldException("liste des calques");
    }

}
