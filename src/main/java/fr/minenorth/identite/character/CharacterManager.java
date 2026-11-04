package fr.minenorth.identite.character;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceData;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.appearance.Catalog;
import fr.minenorth.identite.document.CharacterValidator;
import fr.minenorth.identite.drawing.DrawingKind;
import fr.minenorth.identite.drawing.Drawings;
import fr.minenorth.identite.document.Gender;
import fr.minenorth.identite.document.IdentityData;
import fr.minenorth.identite.document.IdentityManager;
import fr.minenorth.identite.network.ModNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cycle de vie du personnage : création obligatoire à la première connexion, reset complet, gel du joueur
 * pendant la création et diffusion des fiches d'apparence aux clients.
 */
@Mod.EventBusSubscriber
public final class CharacterManager {
    private CharacterManager() {}

    private static final CreationTracker CREATING = new CreationTracker();
    /** Position où un joueur en création est retenu. */
    private static final Map<UUID, Vec3> FROZEN_AT = new ConcurrentHashMap<>();

    /** Tout ce que le client envoie pour terminer la création. */
    public record CreateRequest(String lastName, String firstName, String birthDate, String birthPlace,
                                String nationality, Gender gender, Appearance appearance, boolean rulesAccepted) {}

    // ------------------------------------------------------------------ règles

    /** Message d'erreur (français) ou vide si la demande est acceptable. */
    public static Optional<String> check(CreateRequest r, LocalDate today) {
        if (!r.rulesAccepted()) return Optional.of("Vous devez confirmer que votre nom respecte le règlement RP.");
        if (r.gender() == null) return Optional.of("Choisissez un sexe.");
        if (r.appearance() == null) return Optional.of("Apparence manquante.");
        Optional<String> identity = CharacterValidator.validateIdentity(
                r.lastName(), r.firstName(), r.birthDate(), r.birthPlace(), r.nationality(), today);
        if (identity.isPresent()) return identity;
        return Catalog.validate(r.gender(), r.appearance());
    }

    /** Un personnage = identité avec sexe ET apparence. Une ancienne identité sans sexe n'en est pas un. */
    public static boolean hasCharacter(ServerLevel level, UUID player) {
        IdentityData.Profile profile = IdentityData.get(level).get(player);
        return profile != null && !profile.isLegacy() && AppearanceData.get(level).get(player) != null;
    }

    public static boolean isCreating(UUID player) { return CREATING.isCreating(player); }

    /** Fiches à envoyer à un joueur qui se connecte : seulement celles des joueurs en ligne qui en ont une. */
    public static Set<UUID> sheetsToSend(Set<UUID> online, Set<UUID> withSheet) {
        Set<UUID> out = new java.util.HashSet<>(online);
        out.retainAll(withSheet);
        return out;
    }

    /** Intervalle (en ticks) où le serveur redemande l'écran de création à un joueur retenu, si un autre écran l'a remplacé. */
    public static final int RESEND_TICKS = 200;

    public static boolean shouldResendCreation(int tickCount) {
        return tickCount > 0 && tickCount % RESEND_TICKS == 0;
    }

    // ------------------------------------------------------------------ création

    /** Démarre (ou relance) la création : le joueur est retenu sur place et l'écran s'ouvre chez lui. */
    public static void begin(ServerPlayer player) {
        CREATING.begin(player.getUUID());
        FROZEN_AT.put(player.getUUID(), player.position());
        ModNetwork.sendOpenCreation(player);
    }

    public static void create(ServerPlayer player, CreateRequest r) {
        if (!CREATING.isCreating(player.getUUID())) {
            player.displayClientMessage(Component.literal("§cAucune création de personnage en cours."), true);
            return;
        }
        Optional<String> error = check(r, LocalDate.now());
        if (error.isPresent()) {
            player.displayClientMessage(Component.literal("§c" + error.get()), true);
            return;
        }
        String nationality = CharacterValidator.cleanName(r.nationality());
        if (nationality.isBlank()) nationality = "Française";

        ServerLevel level = player.serverLevel();
        IdentityData data = IdentityData.get(level);
        IdentityData.Profile profile = new IdentityData.Profile(
                player.getUUID(),
                CharacterValidator.cleanName(r.lastName()),
                CharacterValidator.cleanName(r.firstName()),
                CharacterValidator.clean(r.birthDate()),
                CharacterValidator.cleanName(r.birthPlace()),
                nationality,
                IdentityManager.generateCardNumber(data),
                r.gender());
        data.set(profile);
        // le personnage arrive en sous-vêtements : la tenue de départ est posée par le serveur, jamais par le client
        Appearance sheet = r.appearance().withCustomSkin(true)
                .withOutfit(fr.minenorth.identite.clothing.Outfit.starter(r.gender()));
        AppearanceData.get(level).set(player.getUUID(), sheet);

        CREATING.finish(player.getUUID());
        FROZEN_AT.remove(player.getUUID());
        player.refreshTabListName();
        player.refreshDisplayName();
        IdentityManager.giveCard(player, profile);
        ModNetwork.broadcastSheet(player.server, player.getUUID(), r.gender(), sheet);
        ModNetwork.sendIdentityView(player, profile, player.getUUID(), "", false, false);
        player.displayClientMessage(Component.literal("§aVotre personnage a été créé définitivement."), true);
    }

    // ------------------------------------------------------------------ reset

    /**
     * Efface identité et apparence (joueur connecté ou non), retire son skin chez les autres joueurs et, s'il est
     * en ligne, relance la création. Renvoie faux s'il n'y avait rien à effacer.
     */
    public static boolean reset(MinecraftServer server, UUID target) {
        ServerLevel level = server.overworld();
        IdentityData identities = IdentityData.get(level);
        AppearanceData sheets = AppearanceData.get(level);
        ServerPlayer online = server.getPlayerList().getPlayer(target);
        boolean existed = identities.exists(target) || sheets.get(target) != null
                || (online != null && CREATING.isCreating(target));
        identities.reset(target);
        EDITS.close(target);
        sheets.remove(target);
        ModNetwork.broadcastSheet(server, target, null, null);
        if (online != null) {
            online.refreshTabListName();
            online.refreshDisplayName();
            online.displayClientMessage(Component.literal(
                    "§eVotre personnage a été réinitialisé par le staff. Vous devez en créer un nouveau."), false);
            begin(online);
        } else {
            CREATING.clear(target);
            FROZEN_AT.remove(target);
        }
        return existed;
    }

    // ------------------------------------------------------------------ modification par d'autres mods

    private static final EditSessions EDITS = new EditSessions();
    private static final long EDIT_MS = 10 * 60_000L;

    /** Message d'erreur ou vide : seules les sections ouvertes, valides pour le sexe de l'identité. */
    public static Optional<String> validateEdit(Gender gender, Set<AppearanceSection> allowed, Map<AppearanceSection, int[]> edits) {
        return validateEdit(gender, allowed, edits, Map.of());
    }

    /**
     * Idem avec des dessins : chacun doit appartenir à une section ouverte, avoir la bonne taille et ne contenir que des
     * numéros de la palette de son type.
     */
    public static Optional<String> validateEdit(Gender gender, Set<AppearanceSection> allowed,
                                                Map<AppearanceSection, int[]> edits, Map<DrawingKind, byte[]> drawings) {
        if (edits.isEmpty() && drawings.isEmpty()) return Optional.of("Aucune modification.");
        for (Map.Entry<AppearanceSection, int[]> e : edits.entrySet()) {
            if (!allowed.contains(e.getKey())) return Optional.of(e.getKey().label() + " : modification non autorisée.");
            Optional<String> error = Catalog.validate(gender, e.getKey(), e.getValue());
            if (error.isPresent()) return error;
        }
        for (Map.Entry<DrawingKind, byte[]> e : drawings.entrySet()) {
            DrawingKind kind = e.getKey();
            if (!allowed.contains(kind.section())) return Optional.of(kind.label() + " : modification non autorisée.");
            try {
                Drawings.EMPTY.with(kind, e.getValue());
            } catch (IllegalArgumentException ex) {
                return Optional.of(kind.label() + " : dessin invalide.");
            }
        }
        return Optional.empty();
    }

    /** Nouvelle fiche avec les sections remplacées ; le choix « mon skin » est conservé. */
    public static Optional<Appearance> applyEdit(Appearance current, Map<AppearanceSection, int[]> edits) {
        return applyEdit(current, edits, Map.of());
    }

    /** Idem avec des dessins (déjà validés) ; un dessin entièrement transparent efface l'ancien. */
    public static Optional<Appearance> applyEdit(Appearance current, Map<AppearanceSection, int[]> edits,
                                                 Map<DrawingKind, byte[]> drawings) {
        Appearance next = current;
        for (Map.Entry<AppearanceSection, int[]> e : edits.entrySet()) next = next.with(e.getKey(), e.getValue());
        Drawings merged = next.drawings();
        for (Map.Entry<DrawingKind, byte[]> e : drawings.entrySet()) merged = merged.with(e.getKey(), e.getValue());
        return Optional.of(next.withDrawings(merged));
    }

    /** Toutes les sections de l'apparence : ce que rouvre {@code /cidedit}. L'identité et le sexe n'en font jamais partie. */
    public static Set<AppearanceSection> fullEditorSections() { return java.util.EnumSet.allOf(AppearanceSection.class); }

    /** Rouvre l'éditeur d'apparence complet d'un joueur en ligne (staff) ; l'identité, le sexe et la tenue ne changent pas. */
    public static boolean openFullEditor(MinecraftServer server, UUID target) {
        return openEditor(server, target, fullEditorSections());
    }

    /** Ouvre l'éditeur limité à ces sections chez un joueur en ligne qui a un personnage. */
    public static boolean openEditor(MinecraftServer server, UUID target, Set<AppearanceSection> sections) {
        ServerPlayer player = server.getPlayerList().getPlayer(target);
        if (player == null || sections.isEmpty() || !hasCharacter(server.overworld(), target)) return false;
        IdentityData.Profile profile = IdentityData.get(server.overworld()).get(target);
        Appearance current = AppearanceData.get(server.overworld()).get(target);
        EDITS.open(target, sections, System.currentTimeMillis() + EDIT_MS);
        ModNetwork.sendOpenEditor(player, sections, profile.gender(), current);
        return true;
    }

    /** Enregistre la modification envoyée par le client de l'éditeur (une seule fois par session). */
    public static void applyClientEdit(ServerPlayer player, Map<AppearanceSection, int[]> edits, Map<DrawingKind, byte[]> drawings) {
        ServerLevel level = player.serverLevel();
        Set<AppearanceSection> allowed = EDITS.allowed(player.getUUID(), System.currentTimeMillis());
        IdentityData.Profile profile = IdentityData.get(level).get(player.getUUID());
        Appearance current = AppearanceData.get(level).get(player.getUUID());
        if (allowed.isEmpty() || profile == null || profile.gender() == null || current == null) {
            player.displayClientMessage(Component.literal("§cAucune modification d'apparence n'est ouverte."), true);
            return;
        }
        Optional<String> error = validateEdit(profile.gender(), allowed, edits, drawings);
        if (error.isPresent()) {
            player.displayClientMessage(Component.literal("§c" + error.get()), true);
            return;
        }
        Appearance next = applyEdit(current, edits, drawings).orElse(current);
        // salon de tatouage ou de coiffeur : le prix est calculé ici, jamais par le client, et payé par carte avant d'appliquer
        fr.minenorth.identite.salon.SalonKind salon = fr.minenorth.identite.salon.SalonKind.forSections(allowed);
        long price = fr.minenorth.identite.salon.SalonPrices.quote(salon, current, next);
        long paid = 0;
        if (price > 0 && fr.minenorth.api.MineNorth.bank() != fr.minenorth.api.BankService.NONE) {
            paid = fr.minenorth.api.MineNorth.discounted(player.server, player.getUUID(), price);
            fr.minenorth.api.PayResult result = fr.minenorth.api.MineNorth.bank().charge(player, paid, salon.source());
            if (!result.ok()) {
                player.displayClientMessage(Component.literal("§c" + result.message() + " (" + euros(paid) + ")"), true);
                return;
            }
        }
        EDITS.close(player.getUUID());
        updateAppearance(player.server, player.getUUID(), profile.gender(), next);
        player.displayClientMessage(Component.literal(paid > 0
                ? "§aApparence mise à jour. Payé par carte : " + euros(paid) + "."
                : "§aApparence mise à jour."), true);
    }

    /** Montant en centimes, pour les messages : 25 € ou 12,50 €. */
    public static String euros(long cents) {
        return (cents / 100) + (cents % 100 == 0 ? "" : "," + String.format("%02d", cents % 100)) + " €";
    }

    /** Fiche d'un joueur (en ligne ou non) ; vide s'il n'a pas de personnage. */
    public static Optional<Appearance> appearanceOf(MinecraftServer server, UUID target) {
        if (!hasCharacter(server.overworld(), target)) return Optional.empty();
        return Optional.ofNullable(AppearanceData.get(server.overworld()).get(target));
    }

    /** Remplace une section (API des autres mods) : le sexe n'est jamais touché. Faux si invalide. */
    public static boolean setSection(MinecraftServer server, UUID target, AppearanceSection section, int[] values) {
        if (section == null || !hasCharacter(server.overworld(), target)) return false;
        IdentityData.Profile profile = IdentityData.get(server.overworld()).get(target);
        Appearance current = AppearanceData.get(server.overworld()).get(target);
        if (Catalog.validate(profile.gender(), section, values).isPresent()) return false;
        updateAppearance(server, target, profile.gender(), current.with(section, values));
        return true;
    }

    /** Enregistre la fiche d'un joueur et la diffuse à tous les clients. */
    public static void updateAppearance(MinecraftServer server, UUID target, Gender gender, Appearance appearance) {
        AppearanceData.get(server.overworld()).set(target, appearance);
        ModNetwork.broadcastSheet(server, target, gender, appearance);
    }

    // ------------------------------------------------------------------ connexion

    @SubscribeEvent
    public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        AppearanceData sheets = AppearanceData.get(level);
        IdentityData identities = IdentityData.get(level);
        java.util.Set<UUID> online = new java.util.HashSet<>();
        for (ServerPlayer p : player.server.getPlayerList().getPlayers()) online.add(p.getUUID());
        for (UUID id : sheetsToSend(online, sheets.uuids())) {
            IdentityData.Profile profile = identities.get(id);
            if (profile != null && profile.gender() != null) {
                ModNetwork.sendSheet(player, id, profile.gender(), sheets.get(id));
            }
        }
        if (hasCharacter(level, player.getUUID())) {
            ModNetwork.broadcastSheet(player.server, player.getUUID(),
                    identities.get(player.getUUID()).gender(), sheets.get(player.getUUID()));
        } else {
            begin(player);
        }
    }

    @SubscribeEvent
    public static void onLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        CREATING.clear(event.getEntity().getUUID());
        EDITS.close(event.getEntity().getUUID());
        FROZEN_AT.remove(event.getEntity().getUUID());
    }

    // ------------------------------------------------------------------ gel pendant la création

    private static boolean frozen(net.minecraft.world.entity.Entity entity) {
        return entity instanceof ServerPlayer p && CREATING.isCreating(p.getUUID());
    }

    @SubscribeEvent
    public static void hold(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        Vec3 at = FROZEN_AT.get(player.getUUID());
        if (at == null || !CREATING.isCreating(player.getUUID())) return;
        if (player.position().distanceToSqr(at) > 0.01) {
            player.connection.teleport(at.x, at.y, at.z, player.getYRot(), player.getXRot());
        }
        player.setDeltaMovement(Vec3.ZERO);
        if (shouldResendCreation(player.tickCount)) ModNetwork.sendOpenCreation(player);
    }

    @SubscribeEvent
    public static void noInteract(PlayerInteractEvent event) {
        if (event.isCancelable() && frozen(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void noAttack(AttackEntityEvent event) {
        if (frozen(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void noDamage(LivingAttackEvent event) {
        if (frozen(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void noChat(ServerChatEvent event) {
        if (frozen(event.getPlayer())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void noCommand(CommandEvent event) {
        var source = event.getParseResults().getContext().getSource();
        if (source.getEntity() instanceof ServerPlayer p && CREATING.isCreating(p.getUUID()) && !source.hasPermission(2)) {
            event.setCanceled(true);
        }
    }
}
