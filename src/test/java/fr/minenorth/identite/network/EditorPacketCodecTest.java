package fr.minenorth.identite.network;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.document.Gender;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EditorPacketCodecTest {
    private static FriendlyByteBuf buf() { return new FriendlyByteBuf(Unpooled.buffer()); }

    @Test
    void openEditorRoundTrip() {
        Appearance a = Catalog.defaults(Gender.FEMALE);
        FriendlyByteBuf b = buf();
        OpenEditorPacket.encode(new OpenEditorPacket(EnumSet.of(AppearanceSection.HAIR, AppearanceSection.TATTOOS), Gender.FEMALE, a), b);
        OpenEditorPacket p = OpenEditorPacket.decode(b);
        assertEquals(EnumSet.of(AppearanceSection.HAIR, AppearanceSection.TATTOOS), p.sections());
        assertEquals(Gender.FEMALE, p.gender());
        assertEquals(a, p.appearance());
    }

    @Test
    void editorSaveRoundTrip() {
        FriendlyByteBuf b = buf();
        EditorSavePacket.encode(new EditorSavePacket(Map.of(AppearanceSection.HAIR, new int[]{5, 2})), b);
        EditorSavePacket p = EditorSavePacket.decode(b);
        assertEquals(1, p.edits().size());
        assertArrayEquals(new int[]{5, 2}, p.edits().get(AppearanceSection.HAIR));
    }

    @Test
    void forgedSectionNameDecodesToEmptyEdit() {
        FriendlyByteBuf b = buf();
        b.writeVarInt(1);
        b.writeUtf("GENDER");
        b.writeVarInt(1);
        b.writeVarInt(0);
        assertTrue(EditorSavePacket.decode(b).edits().isEmpty());
    }

    @Test
    void hugeCountDecodesToEmptyEdit() {
        FriendlyByteBuf b = buf();
        b.writeVarInt(100000);
        assertTrue(EditorSavePacket.decode(b).edits().isEmpty());
    }

    @Test
    void hugeArrayLengthDecodesToEmptyEdit() {
        FriendlyByteBuf b = buf();
        b.writeVarInt(1);
        b.writeUtf("HAIR");
        b.writeVarInt(1_000_000);
        assertTrue(EditorSavePacket.decode(b).edits().isEmpty());
    }
}
