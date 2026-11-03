package fr.minenorth.identite.drawing;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.clothing.Outfit;
import fr.minenorth.identite.document.Gender;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DrawingsTest {
    private static FriendlyByteBuf buf() { return new FriendlyByteBuf(Unpooled.buffer()); }

    private static byte[] filled(DrawingKind k, int value) {
        byte[] b = new byte[k.width() * k.height()];
        java.util.Arrays.fill(b, (byte) value);
        return b;
    }

    @Test
    void kindsHaveTheSpecifiedSizesAndSections() {
        assertEquals(8, DrawingKind.FACE.width());
        assertEquals(8, DrawingKind.FACE.height());
        assertEquals(AppearanceSection.FACE, DrawingKind.FACE.section());
        assertEquals(AppearanceSection.FACIAL_HAIR, DrawingKind.BEARD.section());
        assertEquals(8 * 12, DrawingKind.TATTOO_TORSO.width() * DrawingKind.TATTOO_TORSO.height());
        assertEquals(8 * 12, DrawingKind.TATTOO_BACK.width() * DrawingKind.TATTOO_BACK.height());
        for (DrawingKind k : new DrawingKind[]{DrawingKind.TATTOO_RIGHT_ARM, DrawingKind.TATTOO_LEFT_ARM, DrawingKind.TATTOO_LEGS}) {
            assertEquals(4, k.width());
            assertEquals(12, k.height());
        }
        for (DrawingKind k : DrawingKind.values()) if (k.name().startsWith("TATTOO")) assertEquals(AppearanceSection.TATTOOS, k.section());
    }

    @Test
    void palettesAreLimitedOpaqueAndDistinct() {
        for (DrawingKind k : DrawingKind.values()) {
            int n = k.paletteSize();
            assertTrue(n >= 12 && n <= 32, k + " : " + n + " couleurs");
            java.util.Set<Integer> seen = new java.util.HashSet<>();
            for (int i = 1; i <= n; i++) {
                int c = k.color(i);
                assertEquals(255, c >>> 24, k + " couleur " + i + " non opaque");
                assertTrue(seen.add(c), k + " couleur " + i + " en double");
            }
        }
    }

    @Test
    void emptyDrawingsAreAllTransparent() {
        assertTrue(Drawings.EMPTY.isEmpty());
        for (DrawingKind k : DrawingKind.values()) {
            byte[] b = Drawings.EMPTY.get(k);
            assertEquals(k.width() * k.height(), b.length);
            for (byte v : b) assertEquals(0, v);
        }
    }

    @Test
    void withIsImmutableAndGetIsACopy() {
        byte[] data = filled(DrawingKind.FACE, 3);
        Drawings d = Drawings.EMPTY.with(DrawingKind.FACE, data);
        assertTrue(Drawings.EMPTY.isEmpty());
        assertFalse(d.isEmpty());
        data[0] = 9;
        assertEquals(3, d.get(DrawingKind.FACE)[0]);
        d.get(DrawingKind.FACE)[1] = 7;
        assertEquals(3, d.get(DrawingKind.FACE)[1]);
    }

    @Test
    void withRejectsWrongLengthAndOutOfPaletteValues() {
        assertThrows(IllegalArgumentException.class, () -> Drawings.EMPTY.with(DrawingKind.FACE, new byte[3]));
        assertThrows(IllegalArgumentException.class, () -> Drawings.EMPTY.with(DrawingKind.FACE, filled(DrawingKind.FACE, DrawingKind.FACE.paletteSize() + 1)));
        assertThrows(IllegalArgumentException.class, () -> Drawings.EMPTY.with(DrawingKind.FACE, filled(DrawingKind.FACE, -1)));
    }

    @Test
    void anAllZeroDrawingCountsAsEmpty() {
        assertTrue(Drawings.EMPTY.with(DrawingKind.BEARD, filled(DrawingKind.BEARD, 0)).isEmpty());
    }

    @Test
    void equalsFollowsContent() {
        Drawings a = Drawings.EMPTY.with(DrawingKind.FACE, filled(DrawingKind.FACE, 2));
        Drawings b = Drawings.EMPTY.with(DrawingKind.FACE, filled(DrawingKind.FACE, 2));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, Drawings.EMPTY.with(DrawingKind.FACE, filled(DrawingKind.FACE, 3)));
    }

    @Test
    void tagRoundTrip() {
        Drawings d = Drawings.EMPTY.with(DrawingKind.FACE, filled(DrawingKind.FACE, 4)).with(DrawingKind.TATTOO_LEGS, filled(DrawingKind.TATTOO_LEGS, 9));
        assertEquals(d, Drawings.fromTag(d.toTag()));
    }

    @Test
    void invalidSavedDrawingIsIgnoredNotFatal() {
        CompoundTag tag = new CompoundTag();
        tag.putByteArray(DrawingKind.FACE.name(), new byte[]{1, 2});
        tag.putByteArray(DrawingKind.BEARD.name(), filled(DrawingKind.BEARD, 100));
        assertTrue(Drawings.fromTag(tag).isEmpty());
    }

    @Test
    void bufferRoundTrip() {
        Drawings d = Drawings.EMPTY.with(DrawingKind.BEARD, filled(DrawingKind.BEARD, 5)).with(DrawingKind.TATTOO_TORSO, filled(DrawingKind.TATTOO_TORSO, 16));
        FriendlyByteBuf b = buf();
        d.write(b);
        assertEquals(d, Drawings.read(b));
    }

    @Test
    void bufferReadRejectsOutOfPaletteValues() {
        FriendlyByteBuf b = buf();
        Drawings.EMPTY.write(b);
        byte[] raw = new byte[b.readableBytes()];
        b.getBytes(0, raw);
        raw[0] = 127;
        FriendlyByteBuf forged = new FriendlyByteBuf(Unpooled.wrappedBuffer(raw));
        assertThrows(IllegalArgumentException.class, () -> Drawings.read(forged));
    }

    @Test
    void bufferReadRejectsTruncatedData() {
        FriendlyByteBuf b = buf();
        b.writeBytes(new byte[10]);
        assertThrows(RuntimeException.class, () -> Drawings.read(b));
    }

    // ---- intégration dans la fiche

    @Test
    void appearanceKeepsDrawingsThroughEveryOperation() {
        Drawings d = Drawings.EMPTY.with(DrawingKind.FACE, filled(DrawingKind.FACE, 2));
        Appearance a = Catalog.defaults(Gender.MALE).withDrawings(d);
        assertEquals(d, a.withCustomSkin(true).drawings());
        assertEquals(d, a.withOutfit(Outfit.starter(Gender.MALE)).drawings());
        assertEquals(d, a.with(AppearanceSection.BODY, new int[]{1}).drawings());
    }

    @Test
    void appearanceDrawingsSurviveTagAndBuffer() {
        Drawings d = Drawings.EMPTY.with(DrawingKind.TATTOO_BACK, filled(DrawingKind.TATTOO_BACK, 7));
        Appearance a = Catalog.defaults(Gender.FEMALE).withDrawings(d).withCustomSkin(true);
        assertEquals(a, Appearance.fromTag(a.toTag()));
        FriendlyByteBuf b = buf();
        a.write(b);
        assertEquals(a, Appearance.read(b));
    }

    @Test
    void oldSavedAppearanceWithoutDrawingsLoadsWithNone() {
        CompoundTag tag = Catalog.defaults(Gender.MALE).toTag();
        tag.remove("Drawings");
        assertTrue(Appearance.fromTag(tag).drawings().isEmpty());
    }

    @Test
    void differentDrawingsMakeDifferentAppearances() {
        Appearance plain = Catalog.defaults(Gender.MALE);
        assertNotEquals(plain, plain.withDrawings(Drawings.EMPTY.with(DrawingKind.FACE, filled(DrawingKind.FACE, 1))));
    }
}
