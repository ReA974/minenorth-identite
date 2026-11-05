package fr.minenorth.identite.salon;

import fr.minenorth.identite.appearance.AppearanceSection;

import java.util.EnumSet;
import java.util.Set;

/** Les trois salons : tatouage, coiffeur et maquillage. Chaque fauteuil permet de modifier une partie de l'apparence, contre paiement. */
public enum SalonKind {
    TATTOO("salon:tatouage", EnumSet.of(AppearanceSection.TATTOOS, AppearanceSection.PIERCINGS)),
    HAIR("salon:coiffeur", EnumSet.of(AppearanceSection.HAIR, AppearanceSection.FACIAL_HAIR)),
    /** Maquillage, particularités de la peau (taches, grains de beauté, cicatrices) et teint. */
    MAKEUP("salon:maquillage", EnumSet.of(AppearanceSection.MAKEUP, AppearanceSection.SKIN));

    private final String source;
    private final Set<AppearanceSection> sections;

    SalonKind(String source, Set<AppearanceSection> sections) {
        this.source = source;
        this.sections = sections;
    }

    /** Ce que le fauteuil permet de modifier. */
    public Set<AppearanceSection> sections() { return EnumSet.copyOf(sections); }

    /** Source notée dans l'historique de la banque et le trésor. */
    public String source() { return source; }

    /** Le salon qui correspond exactement à ces sections ouvertes ; null pour tout autre éditeur (commande, autre mod). */
    public static SalonKind forSections(Set<AppearanceSection> opened) {
        for (SalonKind k : values()) if (k.sections.equals(opened)) return k;
        return null;
    }
}
