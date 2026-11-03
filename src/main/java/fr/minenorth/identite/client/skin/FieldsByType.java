package fr.minenorth.identite.client.skin;

import java.lang.reflect.Field;
import java.util.Optional;

/**
 * Retrouve un champ privé par son type plutôt que par son nom : les noms changent entre le jeu de développement et le
 * jeu officiel (noms obfusqués), pas les types. S'il y a zéro ou plusieurs champs de ce type, on ne devine pas.
 */
final class FieldsByType {
    private FieldsByType() {}

    static Optional<Field> unique(Class<?> owner, Class<?> type) {
        Field found = null;
        for (Field f : owner.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) || f.getType() != type) continue;
            if (found != null) return Optional.empty();
            found = f;
        }
        if (found == null) return Optional.empty();
        try {
            found.setAccessible(true);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
        return Optional.of(found);
    }
}
