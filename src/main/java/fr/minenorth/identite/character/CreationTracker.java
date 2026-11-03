package fr.minenorth.identite.character;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Joueurs en train de créer leur personnage (en mémoire : une déconnexion remet tout à zéro). */
public final class CreationTracker {
    private final Set<UUID> creating = ConcurrentHashMap.newKeySet();

    public void begin(UUID player) { creating.add(player); }

    public boolean isCreating(UUID player) { return creating.contains(player); }

    public void finish(UUID player) { creating.remove(player); }

    /** À la déconnexion : rien n'a été enregistré, le joueur recommencera à sa prochaine connexion. */
    public void clear(UUID player) { creating.remove(player); }
}
