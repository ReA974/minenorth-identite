package fr.minenorth.identite.clothing;

import fr.minenorth.identite.document.Gender;

import java.util.Optional;

/** Règles pour porter ou retirer un vêtement (pures : le serveur applique ensuite le résultat). */
public final class OutfitRules {
    private OutfitRules() {}

    /** Nouvelle tenue, vêtement à rendre au joueur (celui qu'on remplace ou qu'on retire) et erreur éventuelle. */
    public record Change(Outfit outfit, Optional<Clothing> returned, Optional<String> error) {}

    public static Change wear(Gender gender, Outfit current, Clothing item) {
        if (item.gender() != gender) {
            return new Change(current, Optional.empty(), Optional.of("Ce vêtement n'est pas fait pour vous."));
        }
        Clothing old = current.get(item.slot());
        if (old == item) {
            return new Change(current, Optional.empty(), Optional.of("Vous portez déjà ce vêtement."));
        }
        return new Change(current.with(item), Optional.ofNullable(old), Optional.empty());
    }

    public static Change remove(Outfit current, ClothingSlot slot) {
        Clothing old = current.get(slot);
        if (old == null) return new Change(current, Optional.empty(), Optional.of("Vous ne portez rien à cet endroit."));
        return new Change(current.without(slot), Optional.of(old), Optional.empty());
    }
}
