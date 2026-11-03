package fr.minenorth.identite;

import fr.minenorth.api.Identity;
import fr.minenorth.api.IdentityService;
import fr.minenorth.identite.document.IdentityData;
import net.minecraft.server.MinecraftServer;

import java.util.Optional;
import java.util.UUID;

/** Fournit les cartes d'identité aux autres mods via MineNorth API (remplace les lectures par réflexion). */
final class IdentityProvider implements IdentityService {
    @Override
    public Optional<Identity> get(MinecraftServer s, UUID player) {
        if (s == null || player == null) return Optional.empty();
        return Optional.ofNullable(IdentityData.get(s.overworld()).get(player)).filter(IdentityProvider::visible).map(IdentityProvider::toApi);
    }

    @Override
    public Optional<Identity> byCard(MinecraftServer s, String cardNumber) {
        if (s == null || cardNumber == null || cardNumber.isBlank()) return Optional.empty();
        return IdentityData.get(s.overworld()).byCard(cardNumber).filter(IdentityProvider::visible).map(IdentityProvider::toApi);
    }

    @Override
    public java.util.Set<UUID> known(MinecraftServer s) {
        if (s == null) return java.util.Set.of();
        IdentityData data = IdentityData.get(s.overworld());
        java.util.Set<UUID> out = new java.util.HashSet<>();
        for (UUID id : data.uuids()) if (visible(data.get(id))) out.add(id);
        return out;
    }

    /** Une identité d'avant la mise à jour (sans sexe) n'est pas un personnage : les autres mods ne la voient pas. */
    static boolean visible(IdentityData.Profile p) {
        return p != null && !p.isLegacy();
    }

    static Identity toApi(IdentityData.Profile p) {
        return new Identity(p.uuid(), p.lastName(), p.firstName(), p.birthDate(), p.birthPlace(), p.nationality(), p.cardNumber(), p.gender() == null ? null : p.gender().name());
    }
}
