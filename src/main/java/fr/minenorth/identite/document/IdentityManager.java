package fr.minenorth.identite.document;

import fr.minenorth.identite.network.ModNetwork;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Mod.EventBusSubscriber
public final class IdentityManager {
    private IdentityManager() {}

    public static final String CARD_TAG = "MineNorthIdentityOwner";
    public static final String CARD_NUMBER_TAG = "MineNorthIdentityNumber";

    /**
     * Utilise le prénom + nom RP dans le chat sans modifier le nom au-dessus de la tête.
     */
    @SubscribeEvent
    public static void nameFormat(PlayerEvent.NameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        IdentityData.Profile profile = IdentityData.get(player.serverLevel()).get(player.getUUID());
        if (profile != null) {
            event.setDisplayname(Component.literal(profile.firstName() + " " + profile.lastName()));
        }
    }

    /**
     * Utilise le prénom + nom RP dans le TAB.
     */
    @SubscribeEvent
    public static void tabListNameFormat(PlayerEvent.TabListNameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        IdentityData.Profile profile = IdentityData.get(player.serverLevel()).get(player.getUUID());
        if (profile != null) {
            event.setDisplayName(Component.literal(profile.firstName() + " " + profile.lastName()));
        }
    }

    /**
     * Force le format du chat côté serveur pour les serveurs/mods qui ignorent
     * le PlayerEvent.NameFormat. Le pseudo Minecraft n'est jamais modifié.
     */
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void rpChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        IdentityData.Profile profile = IdentityData.get(player.serverLevel()).get(player.getUUID());
        if (profile == null) return;

        net.minecraft.network.chat.MutableComponent formatted = Component.literal(profile.firstName() + " " + profile.lastName());
        formatted.append(Component.literal(" » "));
        formatted.append(event.getMessage());

        event.setCanceled(true);
        player.server.getPlayerList().broadcastSystemMessage(formatted, false);
    }

    @SubscribeEvent
    public static void join(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // Recalcule explicitement le nom TAB à la connexion.
        player.refreshTabListName();
    }

    /** Tick serveur de la dernière présentation de carte, par joueur (anti double ouverture). */
    private static final java.util.Map<UUID, Long> LAST_PRESENT = new java.util.HashMap<>();

    private static boolean holdsIdentityCard(Player player, net.minecraft.world.InteractionHand hand) {
        return player.getItemInHand(hand).getItem() instanceof DocumentItem document && "identity".equals(document.type());
    }

    /**
     * Sneak + clic droit sur un joueur avec sa carte en main : la carte s'ouvre
     * UNIQUEMENT chez le joueur visé, jamais chez celui qui la présente.
     */
    @SubscribeEvent
    public static void rightClickPlayer(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        Player presenter = event.getEntity();
        if (!(event.getTarget() instanceof Player)) return;
        if (!presenter.isShiftKeyDown() || !holdsIdentityCard(presenter, event.getHand())) return;

        // Côté client : on consomme le clic, sinon le jeu enchaîne sur "utiliser l'objet"
        // et la carte s'ouvre aussi chez celui qui la présente.
        if (event.getLevel().isClientSide) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        if (!(presenter instanceof ServerPlayer viewer) || !(event.getTarget() instanceof ServerPlayer target)) return;
        event.setCanceled(true);
        LAST_PRESENT.put(viewer.getUUID(), viewer.serverLevel().getGameTime());

        IdentityData.Profile profile = IdentityData.get(viewer.serverLevel()).get(viewer.getUUID());
        if (profile == null) {
            viewer.displayClientMessage(Component.literal("§cVous ne possédez pas encore de carte d'identité."), true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        showTo(viewer, target, profile);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void rightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = player.getItemInHand(event.getHand());
        if (!(stack.getItem() instanceof DocumentItem document) || !"identity".equals(document.type())) return;

        // Sécurité serveur : si la carte vient d'être présentée à un joueur, on ne l'ouvre pas chez le présentateur.
        Long presented = LAST_PRESENT.get(player.getUUID());
        if (presented != null && Math.abs(player.serverLevel().getGameTime() - presented) <= 5) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        UUID owner = stack.getTag() != null && stack.getTag().hasUUID(CARD_TAG)
                ? stack.getTag().getUUID(CARD_TAG) : player.getUUID();
        IdentityData.Profile profile = IdentityData.get(player.serverLevel()).get(owner);
        String cardNumber = stack.getTag() != null ? stack.getTag().getString(CARD_NUMBER_TAG) : "";
        if (profile == null || !profile.cardNumber().equals(cardNumber)) {
            player.displayClientMessage(Component.literal("§cCette carte d'identité n'est plus valide."), true);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        // Clic droit sur la carte : lecture de la carte, quel que soit son propriétaire.
        ModNetwork.sendIdentityView(player, profile, owner, "", false, false);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    public static void showTo(ServerPlayer from, ServerPlayer to, IdentityData.Profile profile) {
        ModNetwork.sendIdentityView(to, profile, from.getUUID(), from.getGameProfile().getName(), false, false);
        from.displayClientMessage(Component.literal("§aVotre carte d'identité a été présentée à " + to.getGameProfile().getName() + "."), true);
    }

    // ------------------------------------------------------------------ sessions /cidmenu
    /**
     * Joueurs à qui le staff a ouvert l'écran (/cidmenu) : seuls eux peuvent créer leur identité ou demander
     * une nouvelle carte. Sans ça, un client modifié pourrait envoyer les paquets à tout moment.
     */
    private static final java.util.Map<UUID, Long> SESSIONS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long SESSION_MS = 10 * 60_000L;

    private static boolean consumeSession(ServerPlayer p) {
        Long until = SESSIONS.remove(p.getUUID());
        return until != null && until > System.currentTimeMillis();
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
        LAST_PRESENT.remove(event.getEntity().getUUID());
    }

    /** Opens the /cidmenu management screen. Only a command source with permission can call this. */
    public static void open(ServerPlayer viewer, UUID target, boolean staff, boolean forceCreation) {
        SESSIONS.put(viewer.getUUID(), System.currentTimeMillis() + SESSION_MS);
        IdentityData.Profile profile = IdentityData.get(viewer.serverLevel()).get(target);
        if (profile == null) ModNetwork.sendIdentityCreation(viewer, target);
        else ModNetwork.sendIdentityAlreadyExists(viewer, profile, target);
    }

    /** Creates an identity exactly once. Players cannot overwrite their identity. */
    public static void save(ServerPlayer actor, String lastName, String firstName, String birthDate,
                            String birthPlace, String nationality) {
        IdentityData data = IdentityData.get(actor.serverLevel());
        if (data.exists(actor.getUUID())) {
            SESSIONS.remove(actor.getUUID());
            actor.displayClientMessage(Component.literal("§cVotre carte d'identité existe déjà. Elle ne peut pas être modifiée."), true);
            return;
        }
        Long until = SESSIONS.get(actor.getUUID());
        if (until == null || until < System.currentTimeMillis()) {
            actor.displayClientMessage(Component.literal("§cAdressez-vous au staff (mairie) pour créer votre carte d'identité."), true);
            return;
        }

        lastName = cleanName(lastName);
        firstName = cleanName(firstName);
        birthDate = clean(birthDate);
        birthPlace = cleanName(birthPlace);
        nationality = cleanName(nationality);

        if (lastName.isBlank() || firstName.isBlank() || birthDate.isBlank() || birthPlace.isBlank()) {
            actor.displayClientMessage(Component.literal("§cTous les champs doivent être remplis (lettres, espaces, - et ' uniquement)."), true);
            return;
        }
        if (!validDate(birthDate)) {
            actor.displayClientMessage(Component.literal("§cDate de naissance invalide : format JJ/MM/AAAA."), true);
            return;
        }
        if (nationality.isBlank()) nationality = "Française";
        SESSIONS.remove(actor.getUUID());

        String cardNumber = generateCardNumber(data);
        IdentityData.Profile profile = new IdentityData.Profile(
                actor.getUUID(), lastName, firstName, birthDate, birthPlace, nationality, cardNumber
        );
        data.set(profile);
        refreshTabName(actor);
        giveCard(actor, profile);

        ModNetwork.sendIdentityView(actor, profile, actor.getUUID(), "", false, false);
        actor.displayClientMessage(Component.literal("§aVotre carte d'identité a été créée définitivement."), true);
    }

    /** Force le serveur et les clients à recalculer le nom affiché dans le TAB. */
    private static void refreshTabName(ServerPlayer player) {
        player.refreshTabListName();
    }

    public static void giveCard(ServerPlayer player, IdentityData.Profile profile) {
        ItemStack card = new ItemStack(fr.minenorth.identite.item.ModItems.IDENTITY_CARD.get());
        card.getOrCreateTag().putUUID(CARD_TAG, profile.uuid());
        card.getOrCreateTag().putString(CARD_NUMBER_TAG, profile.cardNumber());
        if (!player.getInventory().add(card)) player.drop(card, false);
    }

    /**
     * Carte perdue : uniquement depuis l'écran ouvert par le staff. La nouvelle carte reçoit un NOUVEAU numéro,
     * ce qui annule l'ancienne (« Cette carte d'identité n'est plus valide ») : on ne peut pas multiplier les cartes.
     */
    public static void reissueLostCard(ServerPlayer player) {
        IdentityData data = IdentityData.get(player.serverLevel());
        IdentityData.Profile profile = data.get(player.getUUID());
        if (profile == null) {
            player.displayClientMessage(Component.literal("§cVous n'avez pas encore de carte d'identité."), true);
            return;
        }
        if (!consumeSession(player)) {
            player.displayClientMessage(Component.literal("§cAdressez-vous au staff (mairie) pour refaire votre carte."), true);
            return;
        }
        IdentityData.Profile renewed = new IdentityData.Profile(profile.uuid(), profile.lastName(), profile.firstName(),
                profile.birthDate(), profile.birthPlace(), profile.nationality(), generateCardNumber(data));
        data.set(renewed);
        giveCard(player, renewed);
        player.displayClientMessage(Component.literal("§aNouvelle carte remise (n° " + renewed.cardNumber() + "). L'ancienne n'est plus valide."), true);
    }

    private static String generateCardNumber(IdentityData data) {
        String number;
        do {
            long value = ThreadLocalRandom.current().nextLong(10_000_000L, 100_000_000L);
            number = "FR-" + value;
        } while (data.profilesContainsCard(number));
        return number;
    }

    public static final int MAX_FIELD = 32;

    /** Retire les codes couleur (§), les caractères de contrôle et les espaces en trop ; 32 caractères max. */
    private static String clean(String value) {
        if (value == null) return "";
        String v = value.replaceAll("[\\p{Cntrl}§]", "").trim().replaceAll("\\s+", " ");
        return v.length() > MAX_FIELD ? v.substring(0, MAX_FIELD).trim() : v;
    }

    /** Nom, prénom, lieu, nationalité : lettres (accents compris), espaces, tirets et apostrophes. Sinon vide. */
    private static String cleanName(String value) {
        String v = clean(value);
        return v.matches("[\\p{L}][\\p{L} '\\-]*") ? v : "";
    }

    private static boolean validDate(String v) {
        if (!v.matches("\\d{2}/\\d{2}/\\d{4}")) return false;
        try {
            java.time.LocalDate.parse(v, java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu")
                    .withResolverStyle(java.time.format.ResolverStyle.STRICT));
            return true;
        } catch (java.time.format.DateTimeParseException e) {
            return false;
        }
    }

    /** Réinitialise l'identité (joueur connecté ou non). Renvoie faux s'il n'en avait pas. */
    public static boolean reset(net.minecraft.server.MinecraftServer server, UUID target) {
        IdentityData data = IdentityData.get(server.overworld());
        if (!data.exists(target)) return false;
        data.reset(target);
        ServerPlayer targetPlayer = server.getPlayerList().getPlayer(target);
        if (targetPlayer != null) {
            targetPlayer.refreshTabListName();
            targetPlayer.displayClientMessage(Component.literal("§eVotre carte d'identité a été réinitialisée par le staff. Vous devez en créer une nouvelle."), false);
        }
        return true;
    }
}
