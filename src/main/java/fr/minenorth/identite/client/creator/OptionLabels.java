package fr.minenorth.identite.client.creator;

import fr.minenorth.identite.appearance.AppearanceSection;

import java.util.Map;

/** Libellés français des emplacements et des options du catalogue (repli « Option n »). */
public final class OptionLabels {
    private OptionLabels() {}

    private static final Map<String, String> SLOTS = Map.ofEntries(
            Map.entry("BODY/build", "Corpulence"),
            Map.entry("FACE/eyes", "Yeux"), Map.entry("FACE/eyeColor", "Couleur des yeux"),
            Map.entry("FACE/brows", "Sourcils"), Map.entry("FACE/nose", "Nez"), Map.entry("FACE/mouth", "Bouche"),
            Map.entry("SKIN/tone", "Teint"), Map.entry("SKIN/marks", "Particularités"),
            Map.entry("HAIR/style", "Coupe"), Map.entry("HAIR/color", "Couleur"),
            Map.entry("FACIAL_HAIR/style", "Barbe et moustache"), Map.entry("FACIAL_HAIR/color", "Couleur"),
            Map.entry("MAKEUP/lips", "Lèvres"), Map.entry("MAKEUP/lipColor", "Couleur des lèvres"),
            Map.entry("MAKEUP/eyes", "Yeux"), Map.entry("MAKEUP/eyeColor", "Couleur du fard"),
            Map.entry("MAKEUP/blush", "Blush"), Map.entry("MAKEUP/blushColor", "Couleur du blush"),
            Map.entry("TATTOOS/faceNeck", "Visage et cou"), Map.entry("TATTOOS/torso", "Torse"),
            Map.entry("TATTOOS/back", "Dos"), Map.entry("TATTOOS/rightArm", "Bras droit"),
            Map.entry("TATTOOS/leftArm", "Bras gauche"), Map.entry("TATTOOS/legs", "Jambes"),
            Map.entry("TATTOOS/torsoSize", "Taille du torse"), Map.entry("TATTOOS/backSize", "Taille du dos"),
            Map.entry("TATTOOS/rightArmSize", "Taille du bras droit"), Map.entry("TATTOOS/leftArmSize", "Taille du bras gauche"),
            Map.entry("TATTOOS/legsSize", "Taille des jambes"),
            Map.entry("PIERCINGS/ears", "Oreilles"), Map.entry("PIERCINGS/nose", "Nez"), Map.entry("PIERCINGS/brow", "Sourcil"),
            Map.entry("PIERCINGS/lip", "Lèvre"), Map.entry("PIERCINGS/metal", "Métal"),
            Map.entry("PIERCINGS/helix", "Haut de l'oreille"), Map.entry("PIERCINGS/navel", "Nombril"));

    private static final Map<String, String[]> OPTIONS = Map.ofEntries(
            Map.entry("BODY/build", new String[]{"Normale", "Athlétique", "Ronde", "Mince", "Sablier", "Costaud"}),
            Map.entry("FACE/eyes", new String[]{"Standard", "Étroits", "Grands", "Cils discrets", "Écartés", "Rêveurs",
                    "Cils longs", "Félins", "Doux", "Brillants"}),
            Map.entry("FACE/brows", new String[]{"Fins", "Épais", "Arqués", "Droits", "Froncés", "Aucun"}),
            Map.entry("FACE/nose", new String[]{"Fin", "Moyen", "Large", "Retroussé", "Aquilin"}),
            Map.entry("FACE/mouth", new String[]{"Neutre", "Sourire", "Pulpeuse", "Fine", "Boudeuse", "Large"}),
            Map.entry("SKIN/marks", new String[]{"Aucune", "Grain de beauté", "Taches de rousseur", "Cicatrice",
                    "Grain près des lèvres", "Cicatrice au sourcil", "Grains multiples"}),
            Map.entry("HAIR/style", new String[]{"Aucune", "Rasée", "Brosse", "Court classique", "Dégradé", "Crête",
                    "Gominé", "Frange latérale", "Banane", "Désordonnée", "Mi-longue", "Bouclée courte", "Afro",
                    "Queue de cheval basse", "Longs lisses", "Carré", "Carré plongeant", "Longs raides",
                    "Longs ondulés", "Queue de cheval haute", "Chignon", "Couettes", "Tresse", "Frange droite",
                    "Pixie", "Mi-longs dégradés", "Boucles longues", "Bob à frange"}),
            Map.entry("FACIAL_HAIR/style", new String[]{"Aucune", "Barbe de trois jours", "Ombre légère", "Moustache",
                    "Moustache en croc", "Bouc", "Mouche", "Collier", "Barbe courte", "Barbe pleine", "Favoris", "Van Dyke"}),
            Map.entry("MAKEUP/lips", new String[]{"Aucun", "Fin", "Plein", "Brillant", "Dégradé", "Mat sombre"}),
            Map.entry("MAKEUP/eyes", new String[]{"Aucun", "Léger", "Smoky", "Eye-liner", "Eye-liner ailé", "Pailleté"}),
            Map.entry("MAKEUP/blush", new String[]{"Aucun", "Léger", "Pommettes", "Soleil", "Intense"}),
            Map.entry("PIERCINGS/ears", new String[]{"Aucun", "Clou", "Anneau", "Pendant", "Grand anneau"}),
            Map.entry("PIERCINGS/nose", new String[]{"Aucun", "Clou", "Anneau", "Septum"}),
            Map.entry("PIERCINGS/brow", new String[]{"Aucun", "Anneau", "Barre"}),
            Map.entry("PIERCINGS/lip", new String[]{"Aucun", "Labret", "Anneau", "Double"}),
            Map.entry("PIERCINGS/metal", new String[]{"Argent", "Or", "Noir", "Or rose", "Acier"}),
            Map.entry("PIERCINGS/helix", new String[]{"Aucun", "Clou", "Anneau"}),
            Map.entry("PIERCINGS/navel", new String[]{"Aucun", "Clou", "Anneau", "Pendant"}));

private static final String[] TATTOO_MOTIFS = {"Aucun", "Étoile", "Cœur", "Croix", "Ancre", "Rose", "Crâne",            "Ailes", "Flamme", "Éclair", "Soleil", "Couronne", "Tribal"};

    private static final String[] TATTOO_SIZES = {"Petit", "Moyen", "Grand"};
    private static final String[] FACE_MARKS = {"Aucune", "Croix", "X", "Trait", "Barres", "Zigzag", "Triangle", "Cœur",
            "Larme", "Cercle", "Diagonale", "Points", "Éclair"};

    /** Libellés d'un emplacement de tatouage : tailles, petites marques du visage, ou figures des autres zones. */
    private static String[] tattooNames(String key) {
        if (key.endsWith("Size")) return TATTOO_SIZES;
        return key.equals("faceNeck") ? FACE_MARKS : TATTOO_MOTIFS;
    }

    public static boolean isColor(String key) {
        return key.equals("color") || key.equals("tone") || key.equals("metal") || key.endsWith("Color");
    }

    public static String slot(AppearanceSection section, String key) {
        return SLOTS.getOrDefault(section.name() + "/" + key, key);
    }

    public static String option(AppearanceSection section, String key, int value) {
        String[] names = section == AppearanceSection.TATTOOS ? tattooNames(key) : OPTIONS.get(section.name() + "/" + key);
        if (names != null && value >= 0 && value < names.length) return names[value];
        return "Option " + (value + 1);
    }
}
