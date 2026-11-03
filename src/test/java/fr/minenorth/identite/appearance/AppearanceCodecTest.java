package fr.minenorth.identite.appearance;

import fr.minenorth.identite.document.Gender;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class AppearanceCodecTest {
    private static Appearance sample() {
        return Catalog.random(Gender.MALE, new Random(7)).withCustomSkin(true);
    }

    @Test
    void tagRoundTrip() {
        Appearance a = sample();
        assertEquals(a, Appearance.fromTag(a.toTag()));
    }

    @Test
    void bufferRoundTrip() {
        Appearance a = sample();
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        a.write(buf);
        assertEquals(a, Appearance.read(buf));
    }

    @Test
    void customSkinFlagSurvives() {
        Appearance on = Catalog.defaults(Gender.FEMALE).withCustomSkin(true);
        Appearance off = on.withCustomSkin(false);
        assertTrue(Appearance.fromTag(on.toTag()).customSkin());
        assertFalse(Appearance.fromTag(off.toTag()).customSkin());
        assertNotEquals(on, off);
    }

    @Test
    void readRejectsWrongLength() {
        CompoundTag tag = Catalog.defaults(Gender.MALE).toTag();
        tag.putIntArray(AppearanceSection.BODY.name(), new int[]{0, 0, 0});
        assertThrows(IllegalArgumentException.class, () -> Appearance.fromTag(tag));
    }

    @Test
    void readRejectsOutOfRangeValue() {
        CompoundTag tag = Catalog.defaults(Gender.MALE).toTag();
        tag.putIntArray(AppearanceSection.BODY.name(), new int[]{99});
        assertThrows(IllegalArgumentException.class, () -> Appearance.fromTag(tag));
    }

    @Test
    void readRejectsTruncatedBuffer() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(0);
        assertThrows(RuntimeException.class, () -> Appearance.read(buf));
    }

    @Test
    void getReturnsDefensiveCopy() {
        Appearance a = Catalog.defaults(Gender.MALE);
        int[] body = a.get(AppearanceSection.BODY);
        body[0] = 2;
        assertEquals(0, a.get(AppearanceSection.BODY)[0]);
    }
}
