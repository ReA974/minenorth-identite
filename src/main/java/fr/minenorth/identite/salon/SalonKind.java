package fr.minenorth.identite.salon;

import fr.minenorth.identite.appearance.AppearanceSection;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * Les deux salons : tatouage et coiffeur. Un client ne s'assoit au fauteuil que si un professionnel est dans le salon
 * (joueur portant l'étiquette « tatoueur » ou « coiffeur », donnée avec /tag), ou s'il l'est lui-même, ou s'il est OP.
 */
public enum SalonKind {
    TATTOO("tatoueur", "Aucun tatoueur n'est présent dans le salon.",
            EnumSet.of(AppearanceSection.TATTOOS, AppearanceSection.PIERCINGS)),
    HAIR("coiffeur", "Aucun coiffeur n'est présent dans le salon.",
            EnumSet.of(AppearanceSection.HAIR, AppearanceSection.FACIAL_HAIR));

    /** Distance (en blocs) à laquelle un professionnel compte comme présent dans le salon. */
    public static final double RADIUS = 8.0;

    private final String tag;
    private final String missing;
    private final Set<AppearanceSection> sections;

    SalonKind(String tag, String missing, Set<AppearanceSection> sections) {
        this.tag = tag;
        this.missing = missing;
        this.sections = sections;
    }

    public String tag() { return tag; }

    public String missingMessage() { return missing; }

    /** Ce que le fauteuil permet de modifier. */
    public Set<AppearanceSection> sections() { return EnumSet.copyOf(sections); }

    /** Vrai si le client peut s'asseoir : OP, professionnel lui-même, ou professionnel à proximité. */
    public boolean allowed(boolean operator, Set<String> ownTags, Collection<? extends Set<String>> nearbyTags) {
        if (operator || ownTags.contains(tag)) return true;
        for (Set<String> tags : nearbyTags) if (tags.contains(tag)) return true;
        return false;
    }
}
