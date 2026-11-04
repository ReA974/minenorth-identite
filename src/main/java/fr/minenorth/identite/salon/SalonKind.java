package fr.minenorth.identite.salon;

import fr.minenorth.identite.appearance.AppearanceSection;

import java.util.EnumSet;
import java.util.Set;

/** Les deux salons : tatouage et coiffeur. Chaque fauteuil permet de modifier une partie de l'apparence. */
public enum SalonKind {
    TATTOO(EnumSet.of(AppearanceSection.TATTOOS, AppearanceSection.PIERCINGS)),
    HAIR(EnumSet.of(AppearanceSection.HAIR, AppearanceSection.FACIAL_HAIR));

    private final Set<AppearanceSection> sections;

    SalonKind(Set<AppearanceSection> sections) { this.sections = sections; }

    /** Ce que le fauteuil permet de modifier. */
    public Set<AppearanceSection> sections() { return EnumSet.copyOf(sections); }
}
