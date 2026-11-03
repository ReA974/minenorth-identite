package fr.minenorth.identite.appearance;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Fiche d'apparence de chaque joueur (le sexe est dans l'identité, pas ici). */
public class AppearanceData extends SavedData {
    private static final String NAME = "minenorthidentite_appearance";
    private final Map<UUID, Appearance> sheets = new HashMap<>();

    public static AppearanceData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(AppearanceData::load, AppearanceData::new, NAME);
    }

    /** Copie des joueurs qui ont une fiche. */
    public Set<UUID> uuids() { return new HashSet<>(sheets.keySet()); }

    /** Fiche du joueur, ou null s'il n'en a pas. */
    public Appearance get(UUID uuid) { return sheets.get(uuid); }

    public void set(UUID uuid, Appearance appearance) {
        sheets.put(uuid, appearance);
        setDirty();
    }

    public void remove(UUID uuid) {
        if (sheets.remove(uuid) != null) setDirty();
    }

    static AppearanceData load(CompoundTag tag) {
        AppearanceData data = new AppearanceData();
        ListTag list = tag.getList("Sheets", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            try {
                data.sheets.put(UUID.fromString(t.getString("UUID")), Appearance.fromTag(t.getCompound("Appearance")));
            } catch (Exception ignored) {
                // fiche illisible : le joueur sera invité à recréer son personnage
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Appearance> e : sheets.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putString("UUID", e.getKey().toString());
            t.put("Appearance", e.getValue().toTag());
            list.add(t);
        }
        tag.put("Sheets", list);
        return tag;
    }
}
