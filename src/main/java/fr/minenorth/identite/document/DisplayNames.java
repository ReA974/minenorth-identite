package fr.minenorth.identite.document;

import fr.minenorth.api.MineNorth;
import fr.minenorth.api.StaffService;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * Seul endroit qui compose le nom affiché d'un joueur (chat RP, messages de mort, /msg... et liste des joueurs TAB).
 * Ordre, du plus fort au plus faible :
 *  1. mode staff (mod Admin) : « [Rôle] Prénom Nom », même sous une cagoule ; le préfixe VIP n'est gardé que si l'Admin le règle ;
 *  2. cagoule (mod Vêtements) : « Individu masqué », sans préfixe VIP (chat seulement : le TAB garde le vrai nom) ;
 *  3. citoyen : préfixe de grade (mod VIP) + « Prénom Nom » (pseudo Minecraft sans carte d'identité).
 * Les autres mods ne touchent plus au nom : ils fournissent leur morceau via l'API MineNorth.
 */
public final class DisplayNames {
    private DisplayNames() {}

    /** @param fallback nom à garder sans carte d'identité (pseudo) ; @param chat faux pour le TAB (la cagoule ne s'y applique pas). */
    public static Component compose(ServerPlayer p, Component fallback, boolean chat) {
        IdentityData.Profile profile = IdentityData.get(p.serverLevel()).get(p.getUUID());
        // seul un personnage complet (identité avec sexe + apparence) donne un nom RP ; une ancienne identité reste au pseudo
        boolean character = profile != null && fr.minenorth.identite.character.CharacterManager.hasCharacter(p.serverLevel(), p.getUUID());
        Component base = character ? Component.literal(profile.firstName() + " " + profile.lastName()) : fallback;

        StaffService staff = MineNorth.staff();
        Component tag = staff.dutyTag(p);
        if (tag != null) {
            MutableComponent out = Component.empty().append(tag).append(Component.literal(" "));
            if (staff.keepGradePrefix()) out.append(Component.literal(MineNorth.grades().prefix(p.server, p.getUUID())));
            return out.append(base);
        }
        if (chat && MineNorth.masks().masked(p)) return MineNorth.masks().maskedName();
        String prefix = MineNorth.grades().prefix(p.server, p.getUUID());
        return prefix.isEmpty() ? base : Component.literal(prefix).append(base);
    }
}
