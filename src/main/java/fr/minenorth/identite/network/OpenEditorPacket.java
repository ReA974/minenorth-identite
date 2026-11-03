package fr.minenorth.identite.network;

import fr.minenorth.identite.appearance.Appearance;
import fr.minenorth.identite.appearance.AppearanceSection;
import fr.minenorth.identite.document.Gender;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;

/** Serveur vers client : ouvre l'éditeur d'apparence limité à certaines sections (demandé par un autre mod). */
public record OpenEditorPacket(Set<AppearanceSection> sections, Gender gender, Appearance appearance) {
    static void encode(OpenEditorPacket p, FriendlyByteBuf b) {
        b.writeVarInt(p.sections.size());
        for (AppearanceSection s : p.sections) b.writeUtf(s.name(), 32);
        b.writeUtf(p.gender.name(), 16);
        p.appearance.write(b);
    }

    static OpenEditorPacket decode(FriendlyByteBuf b) {
        int n = b.readVarInt();
        if (n < 0 || n > AppearanceSection.values().length) throw new IllegalArgumentException("sections");
        Set<AppearanceSection> sections = EnumSet.noneOf(AppearanceSection.class);
        for (int i = 0; i < n; i++) {
            AppearanceSection s = AppearanceSection.fromName(b.readUtf(32));
            if (s != null) sections.add(s);
        }
        Gender gender = Gender.fromName(b.readUtf(16));
        return new OpenEditorPacket(sections, gender, Appearance.read(b));
    }

    static void handle(OpenEditorPacket p, Supplier<NetworkEvent.Context> c) {
        c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> fr.minenorth.identite.client.ClientNetworkHandler.openEditor(p)));
        c.get().setPacketHandled(true);
    }
}
