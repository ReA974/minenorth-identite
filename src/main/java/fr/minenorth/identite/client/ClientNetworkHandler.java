package fr.minenorth.identite.client;
import fr.minenorth.identite.network.ModNetwork;
import fr.minenorth.identite.network.SheetPacket;
import net.minecraft.client.Minecraft;
public final class ClientNetworkHandler {
    private ClientNetworkHandler() {}
    public static void openIdentity(ModNetwork.IdentityViewPacket p) { Minecraft.getInstance().setScreen(new IdentityScreen(p)); }
    /** Ouverture de l'écran de création de personnage (envoyée par le serveur à la connexion ou après un reset). */
    public static void openCreation() {
        // Le serveur redemande l'écran périodiquement : ne pas écraser les choix d'un écran déjà ouvert.
        if (Minecraft.getInstance().screen instanceof fr.minenorth.identite.client.creator.CreatorScreen c && c.isCreation()) return;
        Minecraft.getInstance().setScreen(fr.minenorth.identite.client.creator.CreatorScreen.creation());
    }
    /** Réception d'une fiche d'apparence : régénère le skin du joueur, ou le retire (reset). */
    public static void onSheet(SheetPacket p) {
        if (p.present() && p.gender() != null) fr.minenorth.identite.client.skin.ClientSkins.put(p.subject(), p.gender(), p.appearance());
        else fr.minenorth.identite.client.skin.ClientSkins.remove(p.subject());
    }
    /** Éditeur d'apparence demandé par un autre mod : seules les sections ouvertes sont modifiables. */
    public static void openEditor(fr.minenorth.identite.network.OpenEditorPacket p) {
        Minecraft.getInstance().setScreen(fr.minenorth.identite.client.creator.CreatorScreen.editor(p.gender(), p.appearance(), p.sections(), state -> {
            java.util.Map<fr.minenorth.identite.appearance.AppearanceSection, int[]> edits = new java.util.EnumMap<>(fr.minenorth.identite.appearance.AppearanceSection.class);
            for (fr.minenorth.identite.appearance.AppearanceSection s : p.sections()) edits.put(s, state.appearance().get(s));
            ModNetwork.CHANNEL.sendToServer(new fr.minenorth.identite.network.EditorSavePacket(edits));
        }));
    }
}
