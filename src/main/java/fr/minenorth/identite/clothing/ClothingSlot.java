package fr.minenorth.identite.clothing;

/** Emplacement de la tenue : un seul vêtement par emplacement. D'autres (haut, bas…) viendront avec les mods de vêtements. */
public enum ClothingSlot {
    UNDERWEAR("Sous-vêtements");

    private final String label;

    ClothingSlot(String label) { this.label = label; }

    public String label() { return label; }
}
