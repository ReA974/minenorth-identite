package fr.minenorth.identite.client.skin;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FieldsByTypeTest {
    @SuppressWarnings("unused")
    private static final class Dummy {
        private final Map<String, String> textures = new java.util.HashMap<>();
        private String model;
        private String other;
        private boolean pending;
        private int count;
    }

    @Test
    void findsTheUniqueFieldOfAType() {
        assertEquals("textures", FieldsByType.unique(Dummy.class, Map.class).orElseThrow().getName());
        assertEquals("pending", FieldsByType.unique(Dummy.class, boolean.class).orElseThrow().getName());
    }

    @Test
    void refusesToGuessWhenSeveralFieldsHaveTheType() {
        assertTrue(FieldsByType.unique(Dummy.class, String.class).isEmpty());
    }

    @Test
    void findsNothingWhenNoFieldHasTheType() {
        Optional<java.lang.reflect.Field> none = FieldsByType.unique(Dummy.class, java.util.List.class);
        assertTrue(none.isEmpty());
    }

    @Test
    void foundFieldsAreAccessible() throws Exception {
        Dummy d = new Dummy();
        var f = FieldsByType.unique(Dummy.class, boolean.class).orElseThrow();
        f.setBoolean(d, true);
        assertTrue(d.pending);
    }
}
