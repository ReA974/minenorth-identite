package fr.minenorth.identite.client.creator;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.document.CharacterValidator;
import fr.minenorth.identite.document.Gender;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.Random;

/** Choix en cours dans l'écran de création (pur Java, sans rendu) : apparence, sexe, identité et historique. */
public final class CreatorState {
    private record Snapshot(Gender gender, Appearance appearance) {}

    private Gender gender;
    private Appearance appearance;
    private final boolean genderLocked;
    private final Deque<Snapshot> undo = new ArrayDeque<>();
    private final Deque<Snapshot> redo = new ArrayDeque<>();

    private String lastName = "", firstName = "", birthPlace = "", nationality = "Française";
    private String day = "", month = "", year = "";
    private boolean rulesAccepted;

    private CreatorState(Gender gender, Appearance appearance, boolean genderLocked) {
        this.gender = gender;
        this.appearance = appearance;
        this.genderLocked = genderLocked;
    }

    /** Création : sexe Homme par défaut, modifiable ; apparence de départ du catalogue. */
    public static CreatorState forCreation() {
        return new CreatorState(Gender.MALE, Catalog.defaults(Gender.MALE).withOutfit(fr.minenorth.identite.clothing.Outfit.starter(Gender.MALE)), false);
    }

    /** Éditeur ouvert par un autre mod : le sexe de l'identité est affiché mais ne change pas. */
    public static CreatorState forEditor(Gender gender, Appearance current) {
        return new CreatorState(gender, current, true);
    }

    // ------------------------------------------------------------------ apparence

    public Gender gender() { return gender; }
    public boolean genderLocked() { return genderLocked; }
    public Appearance appearance() { return appearance; }

    public boolean customSkin() { return appearance.customSkin(); }

    public void setCustomSkin(boolean value) { appearance = appearance.withCustomSkin(value); }

    public void set(AppearanceSection section, int[] values) {
        apply(new Snapshot(gender, appearance.with(section, values.clone())));
    }

    /** Change le sexe (création seulement) et remet à zéro les choix qui n'existent pas pour l'autre sexe. */
    public void setGender(Gender newGender) {
        if (genderLocked || newGender == gender) return;
        Appearance next = appearance;
        for (AppearanceSection s : AppearanceSection.values()) {
            if (Catalog.validate(newGender, s, next.get(s)).isPresent()) {
                next = next.with(s, Catalog.defaults(newGender).get(s));
            }
        }
        // la tenue de départ suit le sexe : l'aperçu montre ce que le joueur portera à son arrivée
        apply(new Snapshot(newGender, next.withOutfit(fr.minenorth.identite.clothing.Outfit.starter(newGender))));
    }

    /** Remplace un dessin (palette limitée vérifiée) ; annulable comme les autres choix. */
    public void setDrawing(fr.minenorth.identite.drawing.DrawingKind kind, byte[] grid) {
        apply(new Snapshot(gender, appearance.withDrawings(appearance.drawings().with(kind, grid))));
    }

    public void randomizeSection(AppearanceSection section, Random random) {
        set(section, Catalog.randomSection(gender, section, random));
    }

    public void resetSection(AppearanceSection section) {
        set(section, Catalog.defaults(gender).get(section));
    }

    private void apply(Snapshot next) {
        if (next.gender == gender && next.appearance.equals(appearance)) return;
        undo.push(new Snapshot(gender, appearance));
        redo.clear();
        gender = next.gender;
        appearance = next.appearance;
    }

    public boolean canUndo() { return !undo.isEmpty(); }
    public boolean canRedo() { return !redo.isEmpty(); }

    public void undo() {
        if (undo.isEmpty()) return;
        redo.push(new Snapshot(gender, appearance));
        Snapshot s = undo.pop();
        gender = s.gender;
        appearance = s.appearance;
    }

    public void redo() {
        if (redo.isEmpty()) return;
        undo.push(new Snapshot(gender, appearance));
        Snapshot s = redo.pop();
        gender = s.gender;
        appearance = s.appearance;
    }

    // ------------------------------------------------------------------ identité

    public String lastName() { return lastName; }
    public String firstName() { return firstName; }
    public String birthPlace() { return birthPlace; }
    public String nationality() { return nationality; }
    public String day() { return day; }
    public String month() { return month; }
    public String year() { return year; }
    public boolean rulesAccepted() { return rulesAccepted; }

    public void setNames(String last, String first, String place, String nation) {
        lastName = last;
        firstName = first;
        birthPlace = place;
        nationality = nation;
    }

    public void setBirth(String d, String m, String y) {
        day = d;
        month = m;
        year = y;
    }

    public void setRulesAccepted(boolean value) { rulesAccepted = value; }

    /** « JJ/MM/AAAA » si les trois parties sont numériques, sinon vide. */
    public String birthDate() {
        try {
            return String.format("%02d/%02d/%04d", Integer.parseInt(day.trim()), Integer.parseInt(month.trim()), Integer.parseInt(year.trim()));
        } catch (NumberFormatException e) {
            return "";
        }
    }

    public Optional<Integer> age(LocalDate today) { return CharacterValidator.age(birthDate(), today); }

    public Optional<String> identityError(LocalDate today) {
        return CharacterValidator.validateIdentity(lastName, firstName, birthDate(), birthPlace, nationality, today);
    }
}
