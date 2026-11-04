package fr.minenorth.identite.appearance;

import fr.minenorth.identite.document.Gender;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/** Options valides de chaque section. Code partagé client/serveur ; le serveur rejette tout ce qui n'y figure pas. */
public final class Catalog {
    private Catalog() {}

    /**
     * Un choix dans une section (ex. « coupe »). {@code options} valeurs possibles, de 0 à options-1 ;
     * certaines peuvent être réservées à un sexe.
     */
    public record Slot(String key, int options, Set<Integer> maleOnly, Set<Integer> femaleOnly) {
        public Slot(String key, int options) { this(key, options, Set.of(), Set.of()); }

        public boolean allows(Gender gender, int value) {
            if (value < 0 || value >= options) return false;
            if (gender == Gender.FEMALE && maleOnly.contains(value)) return false;
            if (gender == Gender.MALE && femaleOnly.contains(value)) return false;
            return true;
        }
    }

    private static final Map<AppearanceSection, List<Slot>> SLOTS = new EnumMap<>(AppearanceSection.class);

    static {
        // corpulence : 0 normale, 1 athlétique, 2 ronde, 3 mince (F), 4 sablier (F), 5 costaud (H)
        SLOTS.put(AppearanceSection.BODY, List.of(new Slot("build", 6, Set.of(5), Set.of(3, 4))));
        // yeux : 6 à 9 réservés aux femmes (cils) ; bouche 4 (boudeuse) réservée aux femmes
        SLOTS.put(AppearanceSection.FACE, List.of(
                new Slot("eyes", 10, Set.of(), Set.of(6, 7, 8, 9)), new Slot("eyeColor", 8), new Slot("brows", 6),
                new Slot("nose", 5), new Slot("mouth", 6, Set.of(), Set.of(4))));
        SLOTS.put(AppearanceSection.SKIN, List.of(new Slot("tone", 10), new Slot("marks", 7)));
        // coupes : 0 aucune, 1 à 8 hommes, 9 à 14 communes, 15 à 27 femmes
        SLOTS.put(AppearanceSection.HAIR, List.of(
                new Slot("style", 28, Set.of(1, 2, 3, 4, 5, 6, 7, 8),
                        Set.of(15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27)),
                new Slot("color", 12)));
        // pilosité : 1 à 11 réservés aux hommes
        SLOTS.put(AppearanceSection.FACIAL_HAIR, List.of(
                new Slot("style", 12, Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), Set.of()), new Slot("color", 12)));
        SLOTS.put(AppearanceSection.MAKEUP, List.of(
                new Slot("lips", 6), new Slot("lipColor", 8), new Slot("eyes", 6), new Slot("eyeColor", 8),
                new Slot("blush", 5), new Slot("blushColor", 6)));
        // piercings : oreilles, nez, sourcil, lèvre, le métal (argent, or, noir, or rose, acier), haut de l oreille, nombril
        SLOTS.put(AppearanceSection.PIERCINGS, List.of(
                new Slot("ears", 5), new Slot("nose", 4), new Slot("brow", 3), new Slot("lip", 4), new Slot("metal", 5),
                new Slot("helix", 3), new Slot("navel", 4)));
        // tatouages : un motif par zone (0 = aucun, 12 motifs) et, sauf au visage, une taille (petit, moyen, grand)
        SLOTS.put(AppearanceSection.TATTOOS, List.of(
                new Slot("faceNeck", 13),
                new Slot("torso", 31), new Slot("torsoSize", 3),
                new Slot("back", 31), new Slot("backSize", 3),
                new Slot("rightArm", 31), new Slot("rightArmSize", 3),
                new Slot("leftArm", 31), new Slot("leftArmSize", 3),
                new Slot("legs", 31), new Slot("legsSize", 3),
                new Slot("ink", 3),
                new Slot("rightArmFace", 4), new Slot("rightArmHeight", 5), new Slot("leftArmFace", 4), new Slot("leftArmHeight", 5)));
    }

    public static List<Slot> slots(AppearanceSection section) { return SLOTS.get(section); }

    /** Fiche de départ valide pour les deux sexes : tout à 0 sauf peau, coupe commune et couleur de cheveux. */
    public static Appearance defaults(Gender gender) {
        Map<AppearanceSection, int[]> values = new EnumMap<>(AppearanceSection.class);
        for (AppearanceSection s : AppearanceSection.values()) values.put(s, new int[slots(s).size()]);
        values.get(AppearanceSection.SKIN)[0] = 3;
        values.get(AppearanceSection.HAIR)[0] = 9;
        values.get(AppearanceSection.HAIR)[1] = 1;
        return new Appearance(values, false);
    }

    public static Appearance random(Gender gender, Random random) {
        Map<AppearanceSection, int[]> values = new EnumMap<>(AppearanceSection.class);
        for (AppearanceSection s : AppearanceSection.values()) values.put(s, randomSection(gender, s, random));
        return new Appearance(values, false);
    }

    public static int[] randomSection(Gender gender, AppearanceSection section, Random random) {
        // tatouages et piercings se choisissent a la main : le hasard n en pose jamais
        if (section == AppearanceSection.TATTOOS || section == AppearanceSection.PIERCINGS) return new int[slots(section).size()];
        List<Slot> slots = slots(section);
        int[] out = new int[slots.size()];
        for (int i = 0; i < out.length; i++) {
            Slot slot = slots.get(i);
            List<Integer> allowed = new ArrayList<>();
            for (int v = 0; v < slot.options(); v++) if (slot.allows(gender, v)) allowed.add(v);
            out[i] = allowed.get(random.nextInt(allowed.size()));
        }
        return out;
    }

    /** Message d'erreur (français) ou vide si les valeurs sont valides pour ce sexe. */
    public static Optional<String> validate(Gender gender, AppearanceSection section, int[] values) {
        List<Slot> slots = slots(section);
        if (gender == null) return Optional.of("Sexe manquant.");
        if (values == null || values.length != slots.size()) {
            return Optional.of(section.label() + " : nombre de choix incorrect.");
        }
        for (int i = 0; i < slots.size(); i++) {
            if (!slots.get(i).allows(gender, values[i])) {
                return Optional.of(section.label() + " : choix invalide (" + slots.get(i).key() + ").");
            }
        }
        return Optional.empty();
    }

    public static Optional<String> validate(Gender gender, Appearance appearance) {
        for (AppearanceSection s : AppearanceSection.values()) {
            Optional<String> error = validate(gender, s, appearance.get(s));
            if (error.isPresent()) return error;
        }
        for (fr.minenorth.identite.clothing.ClothingSlot slot : fr.minenorth.identite.clothing.ClothingSlot.values()) {
            fr.minenorth.identite.clothing.Clothing worn = appearance.outfit().get(slot);
            if (worn != null && worn.gender() != gender) return Optional.of("Vêtement non adapté à votre sexe.");
        }
        return Optional.empty();
    }
}
