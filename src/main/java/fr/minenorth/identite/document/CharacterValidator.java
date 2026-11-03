package fr.minenorth.identite.document;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Optional;

/** Nettoyage et validation de l'identité saisie à la création. Aucune dépendance Minecraft. */
public final class CharacterValidator {
    private CharacterValidator() {}

    public static final int MAX_FIELD = 32;
    public static final int MAX_AGE = 110;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    /** Retire les codes couleur (§), les caractères de contrôle et les espaces en trop ; 32 caractères max. */
    public static String clean(String value) {
        if (value == null) return "";
        String v = value.replaceAll("[\\p{Cntrl}§]", "").trim().replaceAll("\\s+", " ");
        return v.length() > MAX_FIELD ? v.substring(0, MAX_FIELD).trim() : v;
    }

    /** Nom, prénom, lieu, nationalité : lettres (accents compris), espaces, tirets et apostrophes. Sinon vide. */
    public static String cleanName(String value) {
        String v = clean(value);
        return v.matches("[\\p{L}][\\p{L} '\\-]*") ? v : "";
    }

    public static boolean validDate(String v) {
        return parse(v).isPresent();
    }

    /** Âge en années à la date donnée ; vide si la date de naissance est invalide. */
    public static Optional<Integer> age(String birthDate, LocalDate today) {
        return parse(birthDate).map(b -> java.time.Period.between(b, today).getYears());
    }

    /** Message d'erreur (français) ou vide si l'identité est valide. Les valeurs doivent déjà être nettoyées. */
    public static Optional<String> validateIdentity(String lastName, String firstName, String birthDate,
                                                    String birthPlace, String nationality, LocalDate today) {
        if (cleanName(lastName).isBlank() || cleanName(firstName).isBlank() || cleanName(birthPlace).isBlank()
                || clean(birthDate).isBlank()) {
            return Optional.of("Tous les champs doivent être remplis (lettres, espaces, - et ' uniquement).");
        }
        Optional<LocalDate> birth = parse(birthDate);
        if (birth.isEmpty()) return Optional.of("Date de naissance invalide : format JJ/MM/AAAA.");
        if (birth.get().isAfter(today)) return Optional.of("La date de naissance ne peut pas être dans le futur.");
        if (java.time.Period.between(birth.get(), today).getYears() > MAX_AGE) {
            return Optional.of("Date de naissance trop ancienne.");
        }
        return Optional.empty();
    }

    private static Optional<LocalDate> parse(String v) {
        if (v == null || !v.matches("\\d{2}/\\d{2}/\\d{4}")) return Optional.empty();
        try {
            return Optional.of(LocalDate.parse(v, DATE));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }
}
