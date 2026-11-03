package fr.minenorth.identite.clothing;

import fr.minenorth.identite.document.Gender;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Tenue portée : un vêtement au plus par emplacement. Immuable. */
public final class Outfit {
    public static final Outfit EMPTY = new Outfit(new EnumMap<>(ClothingSlot.class));

    private final Map<ClothingSlot, Clothing> worn;

    private Outfit(Map<ClothingSlot, Clothing> worn) { this.worn = worn; }

    /** Tenue remise à la création du personnage : les sous-vêtements de son sexe. */
    public static Outfit starter(Gender gender) {
        return EMPTY.with(gender == Gender.FEMALE ? Clothing.UNDERWEAR_FEMALE : Clothing.UNDERWEAR_MALE);
    }

    public boolean isEmpty() { return worn.isEmpty(); }

    /** Vêtement porté à cet emplacement, ou null. */
    public Clothing get(ClothingSlot slot) { return worn.get(slot); }

    public Outfit with(Clothing clothing) {
        Map<ClothingSlot, Clothing> copy = new EnumMap<>(ClothingSlot.class);
        copy.putAll(worn);
        copy.put(clothing.slot(), clothing);
        return new Outfit(copy);
    }

    public Outfit without(ClothingSlot slot) {
        Map<ClothingSlot, Clothing> copy = new EnumMap<>(ClothingSlot.class);
        copy.putAll(worn);
        copy.remove(slot);
        return new Outfit(copy);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<ClothingSlot, Clothing> e : worn.entrySet()) tag.putString(e.getKey().name(), e.getValue().id());
        return tag;
    }

    /** Lecture tolérante d'une sauvegarde : un vêtement inconnu ou mal rangé est simplement ignoré. */
    public static Outfit fromTag(CompoundTag tag) {
        Outfit outfit = EMPTY;
        for (ClothingSlot slot : ClothingSlot.values()) {
            Clothing c = Clothing.fromId(tag.getString(slot.name()));
            if (c != null && c.slot() == slot) outfit = outfit.with(c);
        }
        return outfit;
    }

    public void write(FriendlyByteBuf buf) {
        for (ClothingSlot slot : ClothingSlot.values()) {
            Clothing c = worn.get(slot);
            buf.writeVarInt(c == null ? 0 : c.ordinal() + 1);
        }
    }

    /** Lève IllegalArgumentException si un indice est inconnu ou rangé dans le mauvais emplacement. */
    public static Outfit read(FriendlyByteBuf buf) {
        Outfit outfit = EMPTY;
        for (ClothingSlot slot : ClothingSlot.values()) {
            int index = buf.readVarInt();
            if (index == 0) continue;
            if (index < 0 || index > Clothing.values().length) throw new IllegalArgumentException("vêtement inconnu");
            Clothing c = Clothing.values()[index - 1];
            if (c.slot() != slot) throw new IllegalArgumentException("vêtement au mauvais emplacement");
            outfit = outfit.with(c);
        }
        return outfit;
    }

    @Override public boolean equals(Object o) { return o instanceof Outfit other && worn.equals(other.worn); }

    @Override public int hashCode() { return Objects.hash(worn); }
}
