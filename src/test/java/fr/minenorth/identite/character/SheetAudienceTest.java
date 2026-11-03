package fr.minenorth.identite.character;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SheetAudienceTest {
    @Test
    void onlyOnlinePlayersSheetsAreSent() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID offline = UUID.randomUUID();
        assertEquals(Set.of(a, b), CharacterManager.sheetsToSend(Set.of(a, b), Set.of(a, b, offline)));
    }

    @Test
    void onlinePlayerWithoutSheetIsSkipped() {
        UUID a = UUID.randomUUID();
        UUID noSheet = UUID.randomUUID();
        assertEquals(Set.of(a), CharacterManager.sheetsToSend(Set.of(a, noSheet), Set.of(a)));
    }

    @Test
    void nobodyOnlineMeansNothingToSend() {
        assertTrue(CharacterManager.sheetsToSend(Set.of(), Set.of(UUID.randomUUID())).isEmpty());
    }

    @Test
    void creationScreenIsResentOnlyOnTheInterval() {
        assertTrue(CharacterManager.shouldResendCreation(200));
        assertTrue(CharacterManager.shouldResendCreation(400));
        assertFalse(CharacterManager.shouldResendCreation(0));
        assertFalse(CharacterManager.shouldResendCreation(199));
        assertFalse(CharacterManager.shouldResendCreation(201));
    }
}
