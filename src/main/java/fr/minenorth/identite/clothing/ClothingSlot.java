package fr.minenorth.identite.clothing;

/** Emplacement de la tenue : un seul vêtement par emplacement. D'autres (haut, bas…) viendront avec les mods de vêtements. */
public enum ClothingSlot {
    UNDERWEAR("Sous-vêtements");

    private final String label;

    ClothingSlot(String label) { this.label = label; }

    public String label() { return label; }

    /** Retrouve un emplacement par son nom d'enum ; null si inconnu. */
    public static ClothingSlot fromName(String name) {
        if (name == null) return null;
        for (ClothingSlot s : values()) if (s.name().equals(name)) return s;
        return null;
    }
}
