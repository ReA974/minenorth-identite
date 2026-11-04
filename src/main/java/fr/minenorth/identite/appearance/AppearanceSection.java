package fr.minenorth.identite.appearance;

/** Sections de l'apparence, chacune modifiable séparément (le sexe, lui, appartient à l'identité). */
public enum AppearanceSection {
    BODY("Corps"),
    FACE("Visage"),
    SKIN("Peau"),
    HAIR("Cheveux"),
    FACIAL_HAIR("Pilosité"),
    MAKEUP("Maquillage"),
    TATTOOS("Tatouages"),
    PIERCINGS("Piercings");

    private final String label;

    AppearanceSection(String label) { this.label = label; }

    public String label() { return label; }

    /** Retrouve une section par son nom d'enum ; null si inconnu. */
    public static AppearanceSection fromName(String name) {
        if (name == null) return null;
        for (AppearanceSection s : values()) if (s.name().equals(name)) return s;
        return null;
    }
}
