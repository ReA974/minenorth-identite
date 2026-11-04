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
    public static final RegistryObject<Item> TATTOO_CHAIR = block("tattoo_chair", fr.minenorth.identite.salon.ModBlocks.TATTOO_CHAIR);
    public static final RegistryObject<Item> TATTOO_TABLE = block("tattoo_table", fr.minenorth.identite.salon.ModBlocks.TATTOO_TABLE);
    public static final RegistryObject<Item> BARBER_CHAIR = block("barber_chair", fr.minenorth.identite.salon.ModBlocks.BARBER_CHAIR);
    public static final RegistryObject<Item> BARBER_TABLE = block("barber_table", fr.minenorth.identite.salon.ModBlocks.BARBER_TABLE);
    public static final RegistryObject<Item> BARBER_MIRROR = block("barber_mirror", fr.minenorth.identite.salon.ModBlocks.BARBER_MIRROR);

    private static RegistryObject<Item> block(String id, RegistryObject<net.minecraft.world.level.block.Block> block) {
        return ITEMS.register(id, () -> new net.minecraft.world.item.BlockItem(block.get(), new Item.Properties()));
    }

    public static final RegistryObject<Item> UNDERWEAR_FEMALE = ITEMS.register(Clothing.UNDERWEAR_FEMALE.id(), () -> new ClothingItem(Clothing.UNDERWEAR_FEMALE));

    /** L'item qui correspond à un vêtement (pour le rendre au joueur quand il l'enlève). */
    public static ItemStack stackFor(Clothing clothing) {
        return new ItemStack(switch (clothing) {
            case UNDERWEAR_MALE -> UNDERWEAR_MALE.get();
            case UNDERWEAR_FEMALE -> UNDERWEAR_FEMALE.get();
        });
    }
}
