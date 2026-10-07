package fr.minenorth.identite.client;
import fr.minenorth.identite.network.ModNetwork;
import net.minecraft.client.Minecraft;
public final class ClientNetworkHandler {
    private ClientNetworkHandler() {}
    public static void openIdentity(ModNetwork.IdentityViewPacket p) { Minecraft.getInstance().setScreen(new IdentityScreen(p)); }
}
