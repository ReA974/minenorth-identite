package fr.minenorth.identite;

import net.minecraftforge.common.ForgeConfigSpec;

/** Fichier config/minenorth_identite.toml : messages de connexion / déconnexion avec le nom RP. */
public final class IdentityConfig {
    private IdentityConfig() {}

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue JOIN_MESSAGES;
    public static final ForgeConfigSpec.ConfigValue<String> JOIN_MESSAGE;
    public static final ForgeConfigSpec.ConfigValue<String> QUIT_MESSAGE;
    public static final ForgeConfigSpec.BooleanValue ANNOUNCE_WITHOUT_IDENTITY;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        JOIN_MESSAGES = b.comment("Envoie un message à tout le serveur quand un joueur se connecte / se déconnecte, avec son nom RP.",
                        "Mets aussi custom-join-message et custom-quit-message à \"\" dans EssentialsX pour ne pas avoir de doublon.")
                .define("join_messages", true);
        JOIN_MESSAGE = b.comment("Message de connexion. &a = couleurs, {nom} = Prénom Nom RP, {pseudo} = pseudo Minecraft.")
                .define("join_message", "&a+ &f{nom} &7a rejoint la ville");
        QUIT_MESSAGE = b.comment("Message de déconnexion. Mêmes variables.")
                .define("quit_message", "&c- &f{nom} &7a quitté la ville");
        ANNOUNCE_WITHOUT_IDENTITY = b.comment("Annoncer aussi les joueurs sans identité RP ? Si false, aucun message (leur pseudo n'est jamais révélé).")
                .define("announce_without_identity", false);
        SPEC = b.build();
    }
}
