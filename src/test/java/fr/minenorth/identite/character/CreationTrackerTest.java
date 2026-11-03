package fr.minenorth.identite.character;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CreationTrackerTest {
    @Test
    void beginThenFinish() {
        CreationTracker t = new CreationTracker();
        UUID id = UUID.randomUUID();
        assertFalse(t.isCreating(id));
        t.begin(id);
        assertTrue(t.isCreating(id));
        t.finish(id);
        assertFalse(t.isCreating(id));
    }

    @Test
    void clearOnDisconnectForgetsPlayer() {
        CreationTracker t = new CreationTracker();
        UUID id = UUID.randomUUID();
        t.begin(id);
        t.clear(id);
        assertFalse(t.isCreating(id));
    }

    @Test
    void playersAreIndependent() {
        CreationTracker t = new CreationTracker();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        t.begin(a);
        assertTrue(t.isCreating(a));
        assertFalse(t.isCreating(b));
        t.finish(b);
        assertTrue(t.isCreating(a));
    }

    @Test
    void beginTwiceStaysCreating() {
        CreationTracker t = new CreationTracker();
        UUID id = UUID.randomUUID();
        t.begin(id);
        t.begin(id);
        assertTrue(t.isCreating(id));
        t.finish(id);
        assertFalse(t.isCreating(id));
    }
}
