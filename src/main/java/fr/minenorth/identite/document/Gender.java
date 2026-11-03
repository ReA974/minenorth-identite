package fr.minenorth.identite.document;

/** Sexe du personnage : défini à la création, jamais modifiable ensuite (sauf reset staff). */
public enum Gender {
    MALE("Homme", false),
    FEMALE("Femme", true);

    private final String label;
    private final boolean slimArms;

    Gender(String label, boolean slimArms) {
        this.label = label;
        this.slimArms = slimArms;
    }

    public String label() { return label; }

    /** Bras fins (modèle « slim ») pour Femme, classiques pour Homme. */
    public boolean slimArms() { return slimArms; }

    /** Retrouve un sexe par son nom d'enum ; null si inconnu ou vide. */
    public static Gender fromName(String name) {
        if (name == null) return null;
        for (Gender g : values()) if (g.name().equals(name)) return g;
        return null;
    }
}
