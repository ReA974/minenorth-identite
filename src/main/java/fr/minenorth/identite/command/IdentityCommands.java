package fr.minenorth.identite.command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.minenorth.identite.document.IdentityManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber
public final class IdentityCommands {
    private IdentityCommands() {}
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d=event.getDispatcher();
        d.register(Commands.literal("cidmenu").requires(s->s.hasPermission(2))
            .executes(c->{ ServerPlayer self=c.getSource().getPlayerOrException(); return open(c.getSource(), self); })
            .then(Commands.argument("joueur", EntityArgument.player()).executes(c->open(c.getSource(),EntityArgument.getPlayer(c,"joueur")))));
        // Rouvre l'éditeur d'apparence d'un joueur en ligne (il a oublié quelque chose sur son skin) : identité et sexe intacts.
        d.register(Commands.literal("cidedit").requires(s->s.hasPermission(2))
            .then(Commands.argument("joueur", EntityArgument.player()).executes(c->{
                ServerPlayer target=EntityArgument.getPlayer(c,"joueur");
                if(fr.minenorth.identite.character.CharacterManager.openFullEditor(c.getSource().getServer(),target.getUUID())){
                    c.getSource().sendSystemMessage(Component.literal("§aÉditeur d'apparence ouvert pour "+target.getGameProfile().getName()+" (identité et sexe inchangés)."));
                    return 1;
                }
                c.getSource().sendFailure(Component.literal(target.getGameProfile().getName()+" n'a pas encore de personnage complet."));
                return 0;
            })));
        // Fonctionne aussi pour un joueur hors ligne (déjà venu sur le serveur) et depuis la console.
        d.register(Commands.literal("cidreset").requires(s->s.hasPermission(2)).then(Commands.argument("joueur", GameProfileArgument.gameProfile()).executes(c->{
            int n=0;
            for(com.mojang.authlib.GameProfile gp:GameProfileArgument.getGameProfiles(c,"joueur")){
                boolean done=fr.minenorth.identite.character.CharacterManager.reset(c.getSource().getServer(),gp.getId());
                String name=gp.getName();
                if(done){ n++; c.getSource().sendSystemMessage(Component.literal("§aPersonnage de "+name+" réinitialisé (identité et apparence).")); }
                else c.getSource().sendFailure(Component.literal(name+" n'a pas de personnage."));
            }
            return n;
        })));
    }
    private static int open(CommandSourceStack source, ServerPlayer target){ IdentityManager.open(target,target.getUUID(),true,false); source.sendSystemMessage(Component.literal("§aInterface d'identité ouverte pour "+fr.minenorth.api.MineNorth.displayName(target)+".")); return 1; }
}
