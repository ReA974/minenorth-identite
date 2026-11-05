package fr.minenorth.identite.salon;

import fr.minenorth.identite.MineNorthIdentite;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Blocs des salons de tatouage et de coiffeur. */
public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MineNorthIdentite.MOD_ID);

    private static BlockBehaviour.Properties props(SoundType sound) {
        return BlockBehaviour.Properties.of().strength(2.0f).sound(sound).noOcclusion();
    }

    public static final RegistryObject<Block> TATTOO_CHAIR = BLOCKS.register("tattoo_chair",
            () -> new ChairBlock(SalonKind.TATTOO, props(SoundType.WOOL)));
    public static final RegistryObject<Block> TATTOO_TABLE = BLOCKS.register("tattoo_table",
            () -> new DecorBlock(false, props(SoundType.METAL)));
    public static final RegistryObject<Block> BARBER_CHAIR = BLOCKS.register("barber_chair",
            () -> new ChairBlock(SalonKind.HAIR, props(SoundType.WOOL)));
    public static final RegistryObject<Block> BARBER_TABLE = BLOCKS.register("barber_table",
            () -> new DecorBlock(false, props(SoundType.METAL)));
    public static final RegistryObject<Block> MAKEUP_CHAIR = BLOCKS.register("makeup_chair",
            () -> new ChairBlock(SalonKind.MAKEUP, props(SoundType.WOOL)));
    public static final RegistryObject<Block> MAKEUP_TABLE = BLOCKS.register("makeup_table",
            () -> new DecorBlock(false, props(SoundType.METAL)));
    public static final RegistryObject<Block> BARBER_MIRROR = BLOCKS.register("barber_mirror",
            () -> new DecorBlock(true, props(SoundType.GLASS)));
}
