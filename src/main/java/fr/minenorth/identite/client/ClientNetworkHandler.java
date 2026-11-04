package fr.minenorth.identite.client;
import fr.minenorth.identite.network.ModNetwork;
import fr.minenorth.identite.network.SheetPacket;
import net.minecraft.client.Minecraft;
public final class ClientNetworkHandler {
    private ClientNetworkHandler() {}
    public static void openIdentity(ModNetwork.IdentityViewPacket p) { Minecraft.getInstance().setScreen(new IdentityScreen(p)); }
    /** Ouverture de l'écran de création de personnage (envoyée par le serveur à la connexion ou après un reset). */
    public static void openCreation() {
        // Le serveur redemande l'écran périodiquement : ne rien faire si la création (ou un dessin qui en dépend) est à l'écran.
        net.minecraft.client.gui.screens.Screen current = Minecraft.getInstance().screen;
        boolean creator = current instanceof fr.minenorth.identite.client.creator.CreatorScreen c && c.isCreation();
        boolean drawing = current instanceof fr.minenorth.identite.client.creator.DrawScreen d && d.belongsToCreation();
        if (!fr.minenorth.identite.client.creator.CreationScreens.shouldOpen(creator, drawing)) return;
        Minecraft.getInstance().setScreen(fr.minenorth.identite.client.creator.CreatorScreen.creation());
    }
    /** Réception d'une fiche d'apparence : régénère le skin du joueur, ou le retire (reset). */
    public static void onSheet(SheetPacket p) {
        if (p.present() && p.gender() != null) fr.minenorth.identite.client.skin.ClientSkins.put(p.subject(), p.gender(), p.appearance());
        else fr.minenorth.identite.client.skin.ClientSkins.remove(p.subject());
    }
    /** Éditeur d'apparence demandé par un autre mod : seules les sections ouvertes sont modifiables. */
    public static void openEditor(fr.minenorth.identite.network.OpenEditorPacket p) {
        java.util.function.Consumer<fr.minenorth.identite.client.creator.CreatorState> save = state -> sendEdit(p.sections(), state);
        if (p.sections().equals(fr.minenorth.identite.salon.SalonKind.TATTOO.sections())) {
            Minecraft.getInstance().setScreen(fr.minenorth.identite.client.creator.TattooSalonScreen.open(p.gender(), p.appearance(), save));
            return;
        }
        if (p.sections().equals(fr.minenorth.identite.salon.SalonKind.HAIR.sections())) {
            Minecraft.getInstance().setScreen(fr.minenorth.identite.client.creator.HairSalonScreen.open(p.gender(), p.appearance(), save));
            return;
        }
        Minecraft.getInstance().setScreen(fr.minenorth.identite.client.creator.CreatorScreen.editor(p.gender(), p.appearance(), p.sections(), save));
    }

    private static void sendEdit(java.util.Set<fr.minenorth.identite.appearance.AppearanceSection> sections, fr.minenorth.identite.client.creator.CreatorState state) {
        java.util.Map<fr.minenorth.identite.appearance.AppearanceSection, int[]> edits = new java.util.EnumMap<>(fr.minenorth.identite.appearance.AppearanceSection.class);
        for (fr.minenorth.identite.appearance.AppearanceSection s : sections) edits.put(s, state.appearance().get(s));
        java.util.Map<fr.minenorth.identite.drawing.DrawingKind, byte[]> drawings = new java.util.EnumMap<>(fr.minenorth.identite.drawing.DrawingKind.class);
        for (fr.minenorth.identite.drawing.DrawingKind k : fr.minenorth.identite.drawing.DrawingKind.values()) {
            if (sections.contains(k.section())) drawings.put(k, state.appearance().drawings().get(k));
        }
        ModNetwork.CHANNEL.sendToServer(new fr.minenorth.identite.network.EditorSavePacket(edits, drawings));
    }
}
