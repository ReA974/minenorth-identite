package fr.minenorth.identite.clothing;

import fr.minenorth.identite.document.Gender;

/** Vêtements connus : chacun a un emplacement, un sexe et l'identifiant de son item. */
public enum Clothing {
    UNDERWEAR_MALE("underwear_male", ClothingSlot.UNDERWEAR, Gender.MALE, "Caleçon"),
    UNDERWEAR_FEMALE("underwear_female", ClothingSlot.UNDERWEAR, Gender.FEMALE, "Sous-vêtements");

    private final String id;
    private final ClothingSlot slot;
    private final Gender gender;
    private final String label;

    Clothing(String id, ClothingSlot slot, Gender gender, String label) {
        this.id = id;
        this.slot = slot;
        this.gender = gender;
        this.label = label;
    }

    /** Identifiant de l'item (sans le nom du mod) et clé de sauvegarde. */
    public String id() { return id; }
    public ClothingSlot slot() { return slot; }
    public Gender gender() { return gender; }
    public String label() { return label; }

    /** Retrouve un vêtement par son identifiant ; null si inconnu. */
    public static Clothing fromId(String id) {
        if (id == null) return null;
        for (Clothing c : values()) if (c.id.equals(id)) return c;
        return null;
    }
}
