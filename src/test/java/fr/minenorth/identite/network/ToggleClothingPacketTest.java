package fr.minenorth.identite.network;

import fr.minenorth.identite.clothing.ClothingSlot;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToggleClothingPacketTest {
    private static FriendlyByteBuf buf() { return new FriendlyByteBuf(Unpooled.buffer()); }

    @Test
    void roundTrip() {
        FriendlyByteBuf b = buf();
        ToggleClothingPacket.encode(new ToggleClothingPacket(ClothingSlot.UNDERWEAR), b);
        assertEquals(ClothingSlot.UNDERWEAR, ToggleClothingPacket.decode(b).slot());
    }

    @Test
    void forgedSlotNameDecodesToNullInsteadOfThrowing() {
        FriendlyByteBuf b = buf();
        b.writeUtf("HELMET");
        assertNull(ToggleClothingPacket.decode(b).slot());
    }

    @Test
    void slotsAreFoundByName() {
        assertEquals(ClothingSlot.UNDERWEAR, ClothingSlot.fromName("UNDERWEAR"));
        assertNull(ClothingSlot.fromName("nope"));
        assertNull(ClothingSlot.fromName(null));
    }
}
