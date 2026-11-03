package fr.minenorth.identite.character;

import fr.minenorth.identite.appearance.AppearanceSection;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Éditions d'apparence ouvertes par un autre mod (coiffeur, tatoueur…) : pour chaque joueur, les sections qu'il peut
 * modifier et jusqu'à quand. Sans session, un client modifié ne peut rien changer.
 */
public final class EditSessions {
    private record Session(Set<AppearanceSection> sections, long until) {}

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public void open(UUID player, Set<AppearanceSection> sections, long untilMillis) {
        sessions.put(player, new Session(EnumSet.copyOf(sections), untilMillis));
    }

    /** Sections modifiables à cet instant ; vide s'il n'y a pas de session ou si elle a expiré. */
    public Set<AppearanceSection> allowed(UUID player, long nowMillis) {
        Session s = sessions.get(player);
        if (s == null || s.until < nowMillis) return Set.of();
        return EnumSet.copyOf(s.sections);
    }

    public void close(UUID player) { sessions.remove(player); }
}
