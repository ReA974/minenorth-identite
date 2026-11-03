package fr.minenorth.identite.network;

import fr.minenorth.identite.MineNorthIdentite;
import fr.minenorth.identite.document.IdentityData;
import fr.minenorth.identite.document.IdentityManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.fml.DistExecutor;
import java.util.UUID;
import java.util.function.Supplier;

public final class ModNetwork {
    private ModNetwork() {}
    private static final String PROTOCOL = "4";   // 4 : tenue portée dans la fiche d'apparence
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MineNorthIdentite.MOD_ID, "network"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static int id = 0;

    public static void register() {
        CHANNEL.registerMessage(id++, IdentityViewPacket.class, IdentityViewPacket::encode, IdentityViewPacket::decode, IdentityViewPacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, IdentityLostPacket.class, IdentityLostPacket::encode, IdentityLostPacket::decode, IdentityLostPacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, OpenCreationPacket.class, OpenCreationPacket::encode, OpenCreationPacket::decode, OpenCreationPacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, SheetPacket.class, SheetPacket::encode, SheetPacket::decode, SheetPacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, CreateCharacterPacket.class, CreateCharacterPacket::encode, CreateCharacterPacket::decode, CreateCharacterPacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, OpenEditorPacket.class, OpenEditorPacket::encode, OpenEditorPacket::decode, OpenEditorPacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, EditorSavePacket.class, EditorSavePacket::encode, EditorSavePacket::decode, EditorSavePacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, RemoveClothingPacket.class, RemoveClothingPacket::encode, RemoveClothingPacket::decode, RemoveClothingPacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));
    }

    public static void sendIdentityAlreadyExists(ServerPlayer player, IdentityData.Profile profile, UUID subjectUuid) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new IdentityViewPacket(true,false,true,true,subjectUuid,"",profile.lastName(),profile.firstName(),profile.birthDate(),profile.birthPlace(),profile.nationality(),profile.cardNumber(),profile.gender()==null?"":profile.gender().label()));
    }
    public static void sendIdentityView(ServerPlayer player, IdentityData.Profile profile, UUID subjectUuid, String presenter, boolean staff, boolean creation) {
        if (profile == null) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new IdentityViewPacket(true,false,staff,creation,subjectUuid,presenter,profile.lastName(),profile.firstName(),profile.birthDate(),profile.birthPlace(),profile.nationality(),profile.cardNumber(),profile.gender()==null?"":profile.gender().label()));
    }

    public static void sendOpenCreation(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenCreationPacket());
    }

    /** Envoie la fiche de `subject` à un joueur ; `appearance` null = aucune fiche (le client retire le skin). */
    public static void sendSheet(ServerPlayer target, UUID subject, fr.minenorth.identite.document.Gender gender, fr.minenorth.identite.appearance.Appearance appearance) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), new SheetPacket(subject, appearance != null, gender, appearance));
    }

    public static void sendOpenEditor(ServerPlayer player, java.util.Set<fr.minenorth.identite.appearance.AppearanceSection> sections, fr.minenorth.identite.document.Gender gender, fr.minenorth.identite.appearance.Appearance appearance) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenEditorPacket(sections, gender, appearance));
    }

    public static void broadcastSheet(net.minecraft.server.MinecraftServer server, UUID subject, fr.minenorth.identite.document.Gender gender, fr.minenorth.identite.appearance.Appearance appearance) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), new SheetPacket(subject, appearance != null, gender, appearance));
    }

    public record IdentityViewPacket(boolean exists, boolean editable, boolean staff, boolean creation, UUID subjectUuid, String presenter, String lastName, String firstName, String birthDate, String birthPlace, String nationality, String cardNumber, String gender) {
        static void encode(IdentityViewPacket p, FriendlyByteBuf b) { b.writeBoolean(p.exists); b.writeBoolean(p.editable); b.writeBoolean(p.staff); b.writeBoolean(p.creation); b.writeUUID(p.subjectUuid); b.writeUtf(p.presenter); b.writeUtf(p.lastName); b.writeUtf(p.firstName); b.writeUtf(p.birthDate); b.writeUtf(p.birthPlace); b.writeUtf(p.nationality); b.writeUtf(p.cardNumber); b.writeUtf(p.gender); }
        static IdentityViewPacket decode(FriendlyByteBuf b) { return new IdentityViewPacket(b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readUUID(),b.readUtf(),b.readUtf(),b.readUtf(),b.readUtf(),b.readUtf(),b.readUtf(),b.readUtf(),b.readUtf()); }
        static void handle(IdentityViewPacket p, Supplier<NetworkEvent.Context> c) { c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> fr.minenorth.identite.client.ClientNetworkHandler.openIdentity(p))); c.get().setPacketHandled(true); }
    }
    public record IdentityLostPacket() {
        static void encode(IdentityLostPacket p,FriendlyByteBuf b){}
        static IdentityLostPacket decode(FriendlyByteBuf b){return new IdentityLostPacket();}
        static void handle(IdentityLostPacket p,Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->{ServerPlayer sp=c.get().getSender();if(sp!=null)IdentityManager.reissueLostCard(sp);});c.get().setPacketHandled(true);}
    }
}
