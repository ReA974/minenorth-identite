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
    private static final String PROTOCOL = "2";   // 2 : champs limités à 64 caractères
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MineNorthIdentite.MOD_ID, "network"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static int id = 0;

    public static void register() {
        CHANNEL.registerMessage(id++, IdentityViewPacket.class, IdentityViewPacket::encode, IdentityViewPacket::decode, IdentityViewPacket::handle);
        CHANNEL.registerMessage(id++, IdentitySavePacket.class, IdentitySavePacket::encode, IdentitySavePacket::decode, IdentitySavePacket::handle);
        CHANNEL.registerMessage(id++, IdentityLostPacket.class, IdentityLostPacket::encode, IdentityLostPacket::decode, IdentityLostPacket::handle);
    }

    public static void sendIdentityCreation(ServerPlayer player, UUID subjectUuid) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new IdentityViewPacket(false,true,false,true,subjectUuid,"","","","","","",""));
    }
    public static void sendIdentityAlreadyExists(ServerPlayer player, IdentityData.Profile profile, UUID subjectUuid) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new IdentityViewPacket(true,false,true,true,subjectUuid,"",profile.lastName(),profile.firstName(),profile.birthDate(),profile.birthPlace(),profile.nationality(),profile.cardNumber()));
    }
    public static void sendIdentityView(ServerPlayer player, IdentityData.Profile profile, UUID subjectUuid, String presenter, boolean staff, boolean creation) {
        if (profile == null) { sendIdentityCreation(player, subjectUuid); return; }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new IdentityViewPacket(true,false,staff,creation,subjectUuid,presenter,profile.lastName(),profile.firstName(),profile.birthDate(),profile.birthPlace(),profile.nationality(),profile.cardNumber()));
    }

    public record IdentityViewPacket(boolean exists, boolean editable, boolean staff, boolean creation, UUID subjectUuid, String presenter, String lastName, String firstName, String birthDate, String birthPlace, String nationality, String cardNumber) {
        static void encode(IdentityViewPacket p, FriendlyByteBuf b) { b.writeBoolean(p.exists); b.writeBoolean(p.editable); b.writeBoolean(p.staff); b.writeBoolean(p.creation); b.writeUUID(p.subjectUuid); b.writeUtf(p.presenter); b.writeUtf(p.lastName); b.writeUtf(p.firstName); b.writeUtf(p.birthDate); b.writeUtf(p.birthPlace); b.writeUtf(p.nationality); b.writeUtf(p.cardNumber); }
        static IdentityViewPacket decode(FriendlyByteBuf b) { return new IdentityViewPacket(b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readUUID(),b.readUtf(),b.readUtf(),b.readUtf(),b.readUtf(),b.readUtf(),b.readUtf(),b.readUtf()); }
        static void handle(IdentityViewPacket p, Supplier<NetworkEvent.Context> c) { c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> fr.minenorth.identite.client.ClientNetworkHandler.openIdentity(p))); c.get().setPacketHandled(true); }
    }
    public record IdentitySavePacket(String lastName,String firstName,String birthDate,String birthPlace,String nationality) {
        static void encode(IdentitySavePacket p,FriendlyByteBuf b){b.writeUtf(p.lastName);b.writeUtf(p.firstName);b.writeUtf(p.birthDate);b.writeUtf(p.birthPlace);b.writeUtf(p.nationality);}
        static IdentitySavePacket decode(FriendlyByteBuf b){return new IdentitySavePacket(b.readUtf(64),b.readUtf(64),b.readUtf(64),b.readUtf(64),b.readUtf(64));}
        static void handle(IdentitySavePacket p,Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->{ServerPlayer sp=c.get().getSender();if(sp!=null)IdentityManager.save(sp,p.lastName(),p.firstName(),p.birthDate(),p.birthPlace(),p.nationality());});c.get().setPacketHandled(true);}
    }
    public record IdentityLostPacket() {
        static void encode(IdentityLostPacket p,FriendlyByteBuf b){}
        static IdentityLostPacket decode(FriendlyByteBuf b){return new IdentityLostPacket();}
        static void handle(IdentityLostPacket p,Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->{ServerPlayer sp=c.get().getSender();if(sp!=null)IdentityManager.reissueLostCard(sp);});c.get().setPacketHandled(true);}
    }
}
