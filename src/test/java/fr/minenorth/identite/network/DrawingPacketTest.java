package fr.minenorth.identite.network;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.document.Gender;
import fr.minenorth.identite.drawing.DrawingKind;
import fr.minenorth.identite.drawing.Drawings;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DrawingPacketTest {
    private static FriendlyByteBuf buf() { return new FriendlyByteBuf(Unpooled.buffer()); }

    private static byte[] filled(DrawingKind k, int v) {
        byte[] b = new byte[k.width() * k.height()];
        java.util.Arrays.fill(b, (byte) v);
        return b;
    }

    @Test
    void editorSaveCarriesDrawings() {
        Map<DrawingKind, byte[]> drawings = new EnumMap<>(DrawingKind.class);
        drawings.put(DrawingKind.FACE, filled(DrawingKind.FACE, 3));
        drawings.put(DrawingKind.TATTOO_LEGS, filled(DrawingKind.TATTOO_LEGS, 9));
        FriendlyByteBuf b = buf();
        EditorSavePacket.encode(new EditorSavePacket(Map.of(AppearanceSection.FACE, new int[]{0, 0, 0, 0, 0}), drawings), b);
        EditorSavePacket p = EditorSavePacket.decode(b);
        assertEquals(1, p.edits().size());
        assertEquals(2, p.drawings().size());
        assertArrayEquals(filled(DrawingKind.FACE, 3), p.drawings().get(DrawingKind.FACE));
        assertArrayEquals(filled(DrawingKind.TATTOO_LEGS, 9), p.drawings().get(DrawingKind.TATTOO_LEGS));
    }

    @Test
    void editorSaveWithoutDrawingsStillWorks() {
        FriendlyByteBuf b = buf();
        EditorSavePacket.encode(new EditorSavePacket(Map.of(AppearanceSection.HAIR, new int[]{9, 2})), b);
        EditorSavePacket p = EditorSavePacket.decode(b);
        assertEquals(1, p.edits().size());
        assertTrue(p.drawings().isEmpty());
    }

    @Test
    void forgedDrawingKindDecodesToNothing() {
        FriendlyByteBuf b = buf();
        b.writeVarInt(0);
        b.writeVarInt(1);
        b.writeUtf("EVIL");
        b.writeBytes(new byte[64]);
        EditorSavePacket p = EditorSavePacket.decode(b);
        assertTrue(p.edits().isEmpty());
        assertTrue(p.drawings().isEmpty());
    }

    @Test
    void forgedDrawingCountDecodesToNothing() {
        FriendlyByteBuf b = buf();
        b.writeVarInt(0);
        b.writeVarInt(100000);
        assertTrue(EditorSavePacket.decode(b).drawings().isEmpty());
    }

    @Test
    void truncatedDrawingBytesDecodeToNothing() {
        FriendlyByteBuf b = buf();
        b.writeVarInt(0);
        b.writeVarInt(1);
        b.writeUtf("FACE");
        b.writeBytes(new byte[3]);
        EditorSavePacket p = EditorSavePacket.decode(b);
        assertTrue(p.drawings().isEmpty());
    }

    @Test
    void sheetAndCreationPacketsCarryTheDrawings() {
        Appearance a = Catalog.defaults(Gender.MALE).withDrawings(Drawings.EMPTY.with(DrawingKind.BEARD, filled(DrawingKind.BEARD, 4)));
        FriendlyByteBuf b = buf();
        SheetPacket.encode(new SheetPacket(UUID.randomUUID(), true, Gender.MALE, a), b);
        assertEquals(a, SheetPacket.decode(b).appearance());

        FriendlyByteBuf c = buf();
        CreateCharacterPacket.encode(new CreateCharacterPacket("A", "B", "01/01/2000", "C", "D", Gender.MALE, a, true), c);
        assertEquals(a, CreateCharacterPacket.decode(c).appearance());
    }

    @Test
    void forgedDrawingBytesInACreationPacketDecodeToANullAppearance() {
        Appearance a = Catalog.defaults(Gender.MALE);
        FriendlyByteBuf b = buf();
        b.writeUtf("A");
        b.writeUtf("B");
        b.writeUtf("01/01/2000");
        b.writeUtf("C");
        b.writeUtf("D");
        b.writeUtf("MALE");
        a.write(b);
        byte[] raw = new byte[b.readableBytes()];
        b.getBytes(b.readerIndex(), raw);
        raw[raw.length - 1 - 1] = 120;
        FriendlyByteBuf forged = new FriendlyByteBuf(Unpooled.wrappedBuffer(raw));
        assertNull(CreateCharacterPacket.decode(forged).appearance());
    }
}
