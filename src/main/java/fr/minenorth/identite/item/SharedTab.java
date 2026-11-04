package fr.minenorth.identite.item;

import fr.minenorth.identite.MineNorthIdentite;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Les vêtements vont dans l'onglet créatif commun « MineNorth » (fourni par minenorth_api). */
@Mod.EventBusSubscriber(modid = MineNorthIdentite.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SharedTab {
    private SharedTab() {}

    @SubscribeEvent
    public static void fill(BuildCreativeModeTabContentsEvent e) {
        if (!e.getTabKey().location().equals(new ResourceLocation("minenorthapi", "minenorth"))) return;
        e.accept(ModItems.UNDERWEAR_MALE.get());
        e.accept(ModItems.UNDERWEAR_FEMALE.get());
        e.accept(ModItems.TATTOO_CHAIR.get());
        e.accept(ModItems.TATTOO_TABLE.get());
        e.accept(ModItems.BARBER_CHAIR.get());
        e.accept(ModItems.BARBER_TABLE.get());
        e.accept(ModItems.BARBER_MIRROR.get());
    }
}
