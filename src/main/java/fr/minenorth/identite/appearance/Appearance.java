package fr.minenorth.identite.appearance;

import fr.minenorth.identite.clothing.Outfit;
import fr.minenorth.identite.drawing.Drawings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Fiche d'un personnage : entiers de catalogue par section, choix du skin, tenue portée et dessins (numéros de couleur
 * d'une palette limitée), jamais d'image libre. Immuable.
 */
public final class Appearance {
    private final Map<AppearanceSection, int[]> values;
    private final boolean customSkin;
    private final Outfit outfit;
    private final Drawings drawings;

    /** Copie défensive ; toutes les sections doivent être présentes avec la bonne longueur. Tenue et dessins vides. */
    public Appearance(Map<AppearanceSection, int[]> values, boolean customSkin) {
        this(values, customSkin, Outfit.EMPTY, Drawings.EMPTY);
    }

    public Appearance(Map<AppearanceSection, int[]> values, boolean customSkin, Outfit outfit) {
        this(values, customSkin, outfit, Drawings.EMPTY);
    }

    public Appearance(Map<AppearanceSection, int[]> values, boolean customSkin, Outfit outfit, Drawings drawings) {
        Map<AppearanceSection, int[]> copy = new EnumMap<>(AppearanceSection.class);
        for (AppearanceSection s : AppearanceSection.values()) {
            int[] v = values.get(s);
            if (v == null || v.length != Catalog.slots(s).size()) {
                throw new IllegalArgumentException("Section " + s + " invalide");
            }
            copy.put(s, v.clone());
        }
        this.values = copy;
        this.customSkin = customSkin;
        this.outfit = outfit;
        this.drawings = drawings;
    }

    public int[] get(AppearanceSection section) { return values.get(section).clone(); }

    public Appearance with(AppearanceSection section, int[] newValues) {
        Map<AppearanceSection, int[]> copy = new EnumMap<>(values);
        copy.put(section, newValues);
        return new Appearance(copy, customSkin, outfit, drawings);
    }

    /** Vrai si le joueur a choisi « Choisir mon skin » (le skin créé remplace son skin Minecraft). */
    public boolean customSkin() { return customSkin; }

    public Appearance withCustomSkin(boolean value) { return new Appearance(values, value, outfit, drawings); }

    /** Vêtements portés (dessinés sur la couche extérieure du skin). */
    public Outfit outfit() { return outfit; }

    public Appearance withOutfit(Outfit value) { return new Appearance(values, customSkin, value, drawings); }

    /** Dessins du joueur (visage, barbe, tatouages) : palette limitée, posés par-dessus le catalogue. */
    public Drawings drawings() { return drawings; }

    public Appearance withDrawings(Drawings value) { return new Appearance(values, customSkin, outfit, value); }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        for (AppearanceSection s : AppearanceSection.values()) tag.putIntArray(s.name(), values.get(s));
        tag.putBoolean("CustomSkin", customSkin);
        tag.put("Outfit", outfit.toTag());
        tag.put("Drawings", drawings.toTag());
        return tag;
    }

    /** Lève IllegalArgumentException si une section manque, a la mauvaise longueur ou une valeur hors plage. */
    public static Appearance fromTag(CompoundTag tag) {
        Map<AppearanceSection, int[]> values = new EnumMap<>(AppearanceSection.class);
        for (AppearanceSection s : AppearanceSection.values()) {
            int[] v = tag.getIntArray(s.name());
            // fiche d'avant les tailles de tatouage : on garde les motifs, en grand comme avant
            // fiche d'avant les piercings : aucun piercing
            // fiche d'avant le choix de l'encre : les couleurs du motif
            if (s == AppearanceSection.TATTOOS && (v.length == 6 || (v.length >= TattooZone.INK_SLOT && v.length < TattooZone.TOTAL_SLOTS))) v = TattooZone.upgrade(v);
            // les piercings d'oreille (lobe et haut de l'oreille) n'existent plus : ils sont retirés des anciennes fiches
            if (s == AppearanceSection.PIERCINGS && v.length > 0) {
                v = v.clone();
                v[0] = 0;
                if (v.length > 5) v[5] = 0;
            }
            if (s == AppearanceSection.PIERCINGS && v.length < Catalog.slots(s).size()) {
                v = java.util.Arrays.copyOf(v, Catalog.slots(s).size());
            }
            checkRange(s, v);
            values.put(s, v);
        }
        return new Appearance(values, tag.getBoolean("CustomSkin"), Outfit.fromTag(tag.getCompound("Outfit")),
                Drawings.fromTag(tag.getCompound("Drawings")));
    }

    public void write(FriendlyByteBuf buf) {
        for (AppearanceSection s : AppearanceSection.values()) {
            for (int v : values.get(s)) buf.writeVarInt(v);
        }
        buf.writeBoolean(customSkin);
        outfit.write(buf);
        drawings.write(buf);
    }

    /** Lève IllegalArgumentException (ou une exception de lecture) si les données sont invalides. */
    public static Appearance read(FriendlyByteBuf buf) {
        Map<AppearanceSection, int[]> values = new EnumMap<>(AppearanceSection.class);
        for (AppearanceSection s : AppearanceSection.values()) {
            int[] v = new int[Catalog.slots(s).size()];
            for (int i = 0; i < v.length; i++) v[i] = buf.readVarInt();
            checkRange(s, v);
            values.put(s, v);
        }
        boolean customSkin = buf.readBoolean();
        Outfit outfit = Outfit.read(buf);
        return new Appearance(values, customSkin, outfit, Drawings.read(buf));
    }

    private static void checkRange(AppearanceSection s, int[] v) {
        List<Catalog.Slot> slots = Catalog.slots(s);
        if (v.length != slots.size()) throw new IllegalArgumentException("Section " + s + " : longueur " + v.length);
        for (int i = 0; i < v.length; i++) {
            if (v[i] < 0 || v[i] >= slots.get(i).options()) {
                throw new IllegalArgumentException("Section " + s + " : valeur hors plage");
            }
        }
    }

    @Override public boolean equals(Object o) {
        if (!(o instanceof Appearance a) || a.customSkin != customSkin || !a.outfit.equals(outfit)
                || !a.drawings.equals(drawings)) return false;
        for (AppearanceSection s : AppearanceSection.values()) if (!Arrays.equals(values.get(s), a.values.get(s))) return false;
        return true;
    }

    @Override public int hashCode() {
        int h = (customSkin ? 1 : 0) * 31 + outfit.hashCode();
        h = 31 * h + drawings.hashCode();
        for (AppearanceSection s : AppearanceSection.values()) h = 31 * h + Arrays.hashCode(values.get(s));
        return h;
    }
}
