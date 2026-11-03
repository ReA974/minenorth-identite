package fr.minenorth.identite.client;
import fr.minenorth.identite.network.ModNetwork;
import fr.minenorth.identite.network.SheetPacket;
import net.minecraft.client.Minecraft;
public final class ClientNetworkHandler {
    private ClientNetworkHandler() {}
    public static void openIdentity(ModNetwork.IdentityViewPacket p) { Minecraft.getInstance().setScreen(new IdentityScreen(p)); }
    /** Ouverture de l'écran de création : branchée à la tâche 8. */
    public static void openCreation() {}
    /** Réception d'une fiche d'apparence : branchée à la tâche 7. */
    public static void onSheet(SheetPacket p) {}
}
