package fr.minenorth.identite.item;

import fr.minenorth.identite.clothing.Clothing;
import fr.minenorth.identite.clothing.OutfitManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Un vêtement : clic droit pour le porter (il apparaît alors sur le skin du joueur). */
public class ClothingItem extends Item {
    private final Clothing clothing;

    public ClothingItem(Clothing clothing) {
        super(new Item.Properties().stacksTo(1));
        this.clothing = clothing;
    }

    public Clothing clothing() { return clothing; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) OutfitManager.wear(sp, hand);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
