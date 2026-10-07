package fr.minenorth.identite;
import fr.minenorth.identite.item.ModItems;
import fr.minenorth.identite.network.ModNetwork;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
@Mod(MineNorthIdentite.MOD_ID)
public class MineNorthIdentite {
    public static final String MOD_ID="minenorthidentite";
    public MineNorthIdentite(){
        IEventBus bus=FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(bus);
        ModNetwork.register();
        fr.minenorth.api.MineNorth.provide(fr.minenorth.api.IdentityService.class, new IdentityProvider());
    }
}
