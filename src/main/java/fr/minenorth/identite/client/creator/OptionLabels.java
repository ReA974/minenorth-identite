package fr.minenorth.identite.client.creator;

import fr.minenorth.identite.appearance.AppearanceSection;

import java.util.Map;

/** Libellés français des emplacements et des options du catalogue (repli « Option n »). */
public final class OptionLabels {
    private OptionLabels() {}

    private static final Map<String, String> SLOTS = Map.ofEntries(
            Map.entry("BODY/build", "Corpulence"),
            Map.entry("FACE/eyes", "Yeux"), Map.entry("FACE/eyeColor", "Couleur des yeux"),
            Map.entry("FACE/nose", "Nez"), Map.entry("FACE/mouth", "Bouche"),
            Map.entry("SKIN/tone", "Teint"), Map.entry("SKIN/marks", "Particularités"),
            Map.entry("HAIR/style", "Coupe"), Map.entry("HAIR/color", "Couleur"),
            Map.entry("FACIAL_HAIR/style", "Barbe et moustache"), Map.entry("FACIAL_HAIR/color", "Couleur"),
            Map.entry("MAKEUP/lips", "Lèvres"), Map.entry("MAKEUP/lipColor", "Couleur des lèvres"),
            Map.entry("MAKEUP/eyes", "Yeux"), Map.entry("MAKEUP/eyeColor", "Couleur du fard"),
            Map.entry("TATTOOS/faceNeck", "Visage et cou"), Map.entry("TATTOOS/torso", "Torse"),
            Map.entry("TATTOOS/back", "Dos"), Map.entry("TATTOOS/rightArm", "Bras droit"),
            Map.entry("TATTOOS/leftArm", "Bras gauche"), Map.entry("TATTOOS/legs", "Jambes"));

    private static final Map<String, String[]> OPTIONS = Map.ofEntries(
            Map.entry("BODY/build", new String[]{"Normale", "Athlétique", "Ronde"}),
            Map.entry("FACE/eyes", new String[]{"Standard", "Étroits", "Grands", "Cils", "Écartés", "Rêveurs"}),
            Map.entry("FACE/nose", new String[]{"Fin", "Moyen", "Large"}),
            Map.entry("FACE/mouth", new String[]{"Neutre", "Sourire", "Pulpeuse", "Fine"}),
            Map.entry("SKIN/marks", new String[]{"Aucune", "Grain de beauté", "Taches de rousseur", "Cicatrice"}),
            Map.entry("HAIR/style", new String[]{"Aucune", "Rasée", "Courte", "Mi-courte", "Longue homme", "Désordonnée",
                    "Mi-longue", "Carré", "Longs", "Queue de cheval", "Dégradé", "Très longs"}),
            Map.entry("FACIAL_HAIR/style", new String[]{"Aucune", "Barbe de trois jours", "Moustache", "Bouc", "Barbe pleine"}),
            Map.entry("MAKEUP/lips", new String[]{"Aucun", "Fin", "Plein", "Brillant"}),
            Map.entry("MAKEUP/eyes", new String[]{"Aucun", "Léger", "Smoky", "Eye-liner"}));

    private static final Map<String, String[]> TATTOO_MOTIFS = Map.of(
            "default", new String[]{"Aucun", "Croix", "Lignes", "Losange", "Points", "Angle", "Zigzag"});

    public static boolean isColor(String key) {
        return key.equals("color") || key.equals("tone") || key.endsWith("Color");
    }

    public static String slot(AppearanceSection section, String key) {
        return SLOTS.getOrDefault(section.name() + "/" + key, key);
    }

    public static String option(AppearanceSection section, String key, int value) {
        String[] names = section == AppearanceSection.TATTOOS ? TATTOO_MOTIFS.get("default") : OPTIONS.get(section.name() + "/" + key);
        if (names != null && value >= 0 && value < names.length) return names[value];
        return "Option " + (value + 1);
    }
}
