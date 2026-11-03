package fr.minenorth.identite;

import fr.minenorth.api.AppearanceService;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.character.CharacterManager;
import net.minecraft.server.MinecraftServer;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Apparence des personnages pour les autres mods (coiffeur, tatoueur…) via MineNorth API. Jamais le sexe. */
final class AppearanceProvider implements AppearanceService {
    @Override
    public Optional<Map<String, int[]>> get(MinecraftServer s, UUID player) {
        if (s == null || player == null) return Optional.empty();
        return CharacterManager.appearanceOf(s, player).map(a -> {
            Map<String, int[]> out = new HashMap<>();
            for (AppearanceSection section : AppearanceSection.values()) out.put(section.name(), a.get(section));
            return out;
        });
    }

    @Override
    public boolean set(MinecraftServer s, UUID player, String section, int[] values) {
        if (s == null || player == null || values == null) return false;
        return CharacterManager.setSection(s, player, AppearanceSection.fromName(section), values.clone());
    }

    @Override
    public boolean openEditor(MinecraftServer s, UUID player, Set<String> sections) {
        if (s == null || player == null || sections == null) return false;
        Set<AppearanceSection> parsed = EnumSet.noneOf(AppearanceSection.class);
        for (String name : sections) {
            AppearanceSection section = AppearanceSection.fromName(name);
            if (section == null) return false;
            parsed.add(section);
        }
        return CharacterManager.openEditor(s, player, parsed);
    }
}
