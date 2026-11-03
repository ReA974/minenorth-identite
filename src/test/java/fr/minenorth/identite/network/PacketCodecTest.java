package fr.minenorth.identite.network;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.document.Gender;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PacketCodecTest {
    private static FriendlyByteBuf buf() { return new FriendlyByteBuf(Unpooled.buffer()); }

    @Test
    void sheetPacketPresentRoundTrip() {
        UUID id = UUID.randomUUID();
        Appearance a = Catalog.random(Gender.FEMALE, new Random(3)).withCustomSkin(true);
        FriendlyByteBuf b = buf();
        SheetPacket.encode(new SheetPacket(id, true, Gender.FEMALE, a), b);
        SheetPacket p = SheetPacket.decode(b);
        assertEquals(id, p.subject());
        assertTrue(p.present());
        assertEquals(Gender.FEMALE, p.gender());
        assertEquals(a, p.appearance());
    }

    @Test
    void sheetPacketAbsentRoundTrip() {
        UUID id = UUID.randomUUID();
        FriendlyByteBuf b = buf();
        SheetPacket.encode(new SheetPacket(id, false, null, null), b);
        SheetPacket p = SheetPacket.decode(b);
        assertEquals(id, p.subject());
        assertFalse(p.present());
        assertNull(p.appearance());
    }

    @Test
    void createPacketRoundTrip() {
        Appearance a = Catalog.defaults(Gender.MALE).withCustomSkin(true);
        CreateCharacterPacket in = new CreateCharacterPacket("Dupont", "Jean", "14/10/1996", "Paris", "Française", Gender.MALE, a, true);
        FriendlyByteBuf b = buf();
        CreateCharacterPacket.encode(in, b);
        assertEquals(in, CreateCharacterPacket.decode(b));
    }

    @Test
    void invalidAppearanceBytesDoNotThrowOutOfDecode() {
        FriendlyByteBuf b = buf();
        b.writeUtf("Dupont");
        b.writeUtf("Jean");
        b.writeUtf("14/10/1996");
        b.writeUtf("Paris");
        b.writeUtf("Française");
        b.writeUtf("MALE");
        for (int i = 0; i < 40; i++) b.writeVarInt(999);
        CreateCharacterPacket p = CreateCharacterPacket.decode(b);
        assertNull(p.appearance());
        assertFalse(p.rulesAccepted());
    }

    @Test
    void unknownGenderDecodesAsNull() {
        FriendlyByteBuf b = buf();
        CreateCharacterPacket.encode(new CreateCharacterPacket("A", "B", "01/01/2000", "C", "D", Gender.MALE, Catalog.defaults(Gender.MALE), true), b);
        CreateCharacterPacket good = CreateCharacterPacket.decode(b);
        assertEquals(Gender.MALE, good.gender());
        assertNull(Gender.fromName("ALIEN"));
    }

    @Test
    void oversizedStringIsRejected() {
        FriendlyByteBuf b = buf();
        b.writeUtf("x".repeat(200));
        assertThrows(RuntimeException.class, () -> b.readUtf(64));
    }
}
