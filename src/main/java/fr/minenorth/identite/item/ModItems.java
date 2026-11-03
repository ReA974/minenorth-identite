package fr.minenorth.identite.item;
import fr.minenorth.identite.MineNorthIdentite;
import fr.minenorth.identite.clothing.Clothing;
import fr.minenorth.identite.document.DocumentItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
public final class ModItems {
    private ModItems() {}
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MineNorthIdentite.MOD_ID);
    public static final RegistryObject<Item> IDENTITY_CARD = ITEMS.register("identity_card", () -> new DocumentItem("identity"));
    public static final RegistryObject<Item> UNDERWEAR_MALE = ITEMS.register(Clothing.UNDERWEAR_MALE.id(), () -> new ClothingItem(Clothing.UNDERWEAR_MALE));
    public static final RegistryObject<Item> UNDERWEAR_FEMALE = ITEMS.register(Clothing.UNDERWEAR_FEMALE.id(), () -> new ClothingItem(Clothing.UNDERWEAR_FEMALE));

    /** L'item qui correspond à un vêtement (pour le rendre au joueur quand il l'enlève). */
    public static ItemStack stackFor(Clothing clothing) {
        return new ItemStack(switch (clothing) {
            case UNDERWEAR_MALE -> UNDERWEAR_MALE.get();
            case UNDERWEAR_FEMALE -> UNDERWEAR_FEMALE.get();
        });
    }
}
