package fr.minenorth.identite.appearance;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Fiche de choix d'un personnage : entiers de catalogue par section, jamais d'image. Immuable. */
public final class Appearance {
    private final Map<AppearanceSection, int[]> values;
    private final boolean customSkin;

    /** Copie défensive ; toutes les sections doivent être présentes avec la bonne longueur. */
    public Appearance(Map<AppearanceSection, int[]> values, boolean customSkin) {
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
    }

    public int[] get(AppearanceSection section) { return values.get(section).clone(); }

    public Appearance with(AppearanceSection section, int[] newValues) {
        Map<AppearanceSection, int[]> copy = new EnumMap<>(values);
        copy.put(section, newValues);
        return new Appearance(copy, customSkin);
    }

    /** Vrai si le joueur a choisi « Choisir mon skin » (le skin créé remplace son skin Minecraft). */
    public boolean customSkin() { return customSkin; }

    public Appearance withCustomSkin(boolean value) { return new Appearance(values, value); }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        for (AppearanceSection s : AppearanceSection.values()) tag.putIntArray(s.name(), values.get(s));
        tag.putBoolean("CustomSkin", customSkin);
        return tag;
    }

    /** Lève IllegalArgumentException si une section manque, a la mauvaise longueur ou une valeur hors plage. */
    public static Appearance fromTag(CompoundTag tag) {
        Map<AppearanceSection, int[]> values = new EnumMap<>(AppearanceSection.class);
        for (AppearanceSection s : AppearanceSection.values()) {
            int[] v = tag.getIntArray(s.name());
            checkRange(s, v);
            values.put(s, v);
        }
        return new Appearance(values, tag.getBoolean("CustomSkin"));
    }

    public void write(FriendlyByteBuf buf) {
        for (AppearanceSection s : AppearanceSection.values()) {
            for (int v : values.get(s)) buf.writeVarInt(v);
        }
        buf.writeBoolean(customSkin);
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
        return new Appearance(values, buf.readBoolean());
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
        if (!(o instanceof Appearance a) || a.customSkin != customSkin) return false;
        for (AppearanceSection s : AppearanceSection.values()) if (!Arrays.equals(values.get(s), a.values.get(s))) return false;
        return true;
    }

    @Override public int hashCode() {
        int h = customSkin ? 1 : 0;
        for (AppearanceSection s : AppearanceSection.values()) h = 31 * h + Arrays.hashCode(values.get(s));
        return h;
    }
}
