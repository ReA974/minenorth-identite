package fr.minenorth.identite.client;
import fr.minenorth.identite.network.ModNetwork;
import fr.minenorth.identite.network.SheetPacket;
import net.minecraft.client.Minecraft;
public final class ClientNetworkHandler {
    private ClientNetworkHandler() {}
    public static void openIdentity(ModNetwork.IdentityViewPacket p) { Minecraft.getInstance().setScreen(new IdentityScreen(p)); }
    /** Ouverture de l'écran de création de personnage (envoyée par le serveur à la connexion ou après un reset). */
    public static void openCreation() { Minecraft.getInstance().setScreen(fr.minenorth.identite.client.creator.CreatorScreen.creation()); }
    /** Réception d'une fiche d'apparence : régénère le skin du joueur, ou le retire (reset). */
    public static void onSheet(SheetPacket p) {
        if (p.present() && p.gender() != null) fr.minenorth.identite.client.skin.ClientSkins.put(p.subject(), p.gender(), p.appearance());
        else fr.minenorth.identite.client.skin.ClientSkins.remove(p.subject());
    }
}
