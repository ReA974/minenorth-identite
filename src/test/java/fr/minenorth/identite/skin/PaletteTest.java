package fr.minenorth.identite.skin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaletteTest {
    @Test
    void argbToAbgrSwapsRedAndBlue() {
        assertEquals(0xFF332211, Palette.argbToAbgr(0xFF112233));
        assertEquals(0x00000000, Palette.argbToAbgr(0));
        assertEquals(0x80FF0000, Palette.argbToAbgr(0x800000FF));
    }

    @Test
    void shadeKeepsAlphaAndClamps() {
        assertEquals(0xFF000000, Palette.shade(0xFF000000, 2.0));
        assertEquals(0xFFFFFFFF, Palette.shade(0xFFFFFFFF, 2.0));
        assertEquals(0x80808080 >>> 24, Palette.shade(0x80808080, 0.5) >>> 24);
    }
}
