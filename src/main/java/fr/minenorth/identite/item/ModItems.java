package fr.minenorth.identite.item;
import fr.minenorth.identite.MineNorthIdentite;
import fr.minenorth.identite.document.DocumentItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
public final class ModItems {
    private ModItems() {}
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MineNorthIdentite.MOD_ID);
    public static final RegistryObject<Item> IDENTITY_CARD = ITEMS.register("identity_card", () -> new DocumentItem("identity"));
}
