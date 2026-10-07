package fr.minenorth.identite.client;

import fr.minenorth.identite.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class IdentityScreen extends Screen {
    private final ModNetwork.IdentityViewPacket data;
    private EditBox lastName, firstName, birthDate, birthPlace, nationality;

    public IdentityScreen(ModNetwork.IdentityViewPacket data) { super(Component.literal("Carte d'identité")); this.data=data; }

    private static final int PANEL_H=300, FIELD_TOP=83, FIELD_STEP=36;
    private int panelY(){return Math.max(8,(height-PANEL_H)/2);}

    private MineNorthButton btn(int x,int y,int w,int h,String label,int color,Runnable r){return addRenderableWidget(new MineNorthButton(x,y,w,h,Component.literal(label),color,r));}
    private EditBox field(int x,int y,int w,String hint,String value){EditBox b=new EditBox(font,x,y,w,18,Component.literal(hint));b.setMaxLength(32);b.setHint(Component.literal(hint));b.setValue(value);addRenderableWidget(b);return b;}

    @Override protected void init(){
        clearWidgets();
        if(data.creation()) {
            int w=Math.min(560,width-30), x=(width-w)/2, y=panelY();
            btn(x+w-100,y+10,86,16,"Fermer",MineNorthButton.GHOST,this::onClose);
            if(data.exists()){
                btn(x+24,y+210,w-48,22,"CARTE PERDUE — RÉÉDITER",MineNorthStyle.CYAN,()->{ModNetwork.CHANNEL.sendToServer(new ModNetwork.IdentityLostPacket());onClose();});
                btn(x+24,y+238,w-48,20,"FERMER",MineNorthStyle.PINK,this::onClose);
            } else {
                // Chaque ligne : libellé à FIELD_TOP+i*FIELD_STEP, champ 11 px plus bas.
                lastName=field(x+24,y+FIELD_TOP+11,w-48,"Ex : Dupont",""); firstName=field(x+24,y+FIELD_TOP+FIELD_STEP+11,w-48,"Ex : Jean","");
                birthDate=field(x+24,y+FIELD_TOP+2*FIELD_STEP+11,w-48,"JJ/MM/AAAA",""); birthPlace=field(x+24,y+FIELD_TOP+3*FIELD_STEP+11,w-48,"Ex : Paris","");
                nationality=field(x+24,y+FIELD_TOP+4*FIELD_STEP+11,w-48,"Ex : Française","Française");
                btn(x+24,y+266,w-48,22,"CRÉER LA CARTE D'IDENTITÉ",MineNorthStyle.GREEN,this::save);
            }
        } else btn(width/2-60,Math.max(24,(height-CARD_H)/2-10)+CARD_H+12,120,18,"FERMER",MineNorthStyle.DARK,this::onClose);
    }
    private void save(){ModNetwork.CHANNEL.sendToServer(new ModNetwork.IdentitySavePacket(lastName.getValue(),firstName.getValue(),birthDate.getValue(),birthPlace.getValue(),nationality.getValue()));}
    @Override public void render(GuiGraphics g,int mx,int my,float pt){
        renderBackground(g);
        if(data.creation()) renderCreation(g); else renderCard(g);
        super.render(g,mx,my,pt);
    }
    private void renderCreation(GuiGraphics g){
        int w=Math.min(560,width-30),x=(width-w)/2,y=panelY();
        MineNorthStyle.panel(g,x,y,w,PANEL_H,data.exists()?"IDENTITÉ EXISTANTE":"CRÉATION DE L'IDENTITÉ","MINE NORTH RP • DOCUMENT PERSONNEL");
        if(data.exists()){
            g.drawString(font,"Cette identité est déjà enregistrée.",x+24,y+78,MineNorthStyle.TEXT,false);
            g.drawString(font,"Elle est définitive et ne peut pas être modifiée par le joueur.",x+24,y+98,MineNorthStyle.MUTED,false);
            g.drawString(font,"Numéro : "+data.cardNumber(),x+24,y+135,MineNorthStyle.BLUE,false);
            g.drawString(font,"Vous pouvez uniquement demander une nouvelle carte si elle est perdue.",x+24,y+158,MineNorthStyle.TEXT,false);
        } else {
            g.drawString(font,"Les informations seront enregistrées définitivement.",x+24,y+66,MineNorthStyle.TEXT,false);
            String[] labels={"Nom","Prénom","Date de naissance","Lieu de naissance","Nationalité"};
            for(int i=0;i<labels.length;i++) g.drawString(font,labels[i],x+24,y+FIELD_TOP+i*FIELD_STEP,MineNorthStyle.BLUE,false);
        }
    }
    // Carte d'identité : même gabarit que la carte de permis (bandeau, photo, champs numérotés).
    private static final int CARD_W=280, CARD_H=138, CARD_BASE=0xFFC9DDF7, CARD_BAND=0xFF1F3C9C, CARD_DARK=0xFF1A1A2A, CARD_LABEL=0xFF55557A;
    private void renderCard(GuiGraphics g){
        int l=(width-CARD_W)/2, t=Math.max(24,(height-CARD_H)/2-10);
        if(!data.presenter().isBlank()) g.drawCenteredString(font,data.presenter()+" te présente sa carte",width/2,t-16,MineNorthStyle.WARN);

        g.fill(l+2,t,l+CARD_W-2,t+CARD_H,CARD_BASE); g.fill(l,t+2,l+CARD_W,t+CARD_H-2,CARD_BASE);
        g.fill(l+2,t,l+CARD_W-2,t+30,CARD_BAND); g.fill(l,t+2,l+CARD_W,t+30,CARD_BAND);
        RenderSystem.enableBlend();
        g.blit(MineNorthStyle.LOGO,l+8,t+4,22,22,0,0,256,256,256,256);
        g.drawString(font,MineNorthStyle.bold("MINENORTH RP"),l+36,t+6,0xFFFFFFFF,false);
        g.drawString(font,"CARTE NATIONALE D'IDENTITÉ",l+36,t+17,0xFFDDE8FF,false);

        // photo
        int px=l+12, py=t+40;
        g.fill(px-2,py-2,px+58,py+58,0xFF3A3A48);
        PlayerFaceRenderer.draw(g,findSkin(),px,py,56);
        g.drawString(font,"N° DE CARTE",l+12,t+104,CARD_LABEL,false);
        g.drawString(font,data.cardNumber(),l+12,t+114,CARD_DARK,false);

        int x=l+82, x2=l+182, y=t+40;
        cardField(g,"1. NOM",data.lastName(),x,y,94);
        cardField(g,"2. PRÉNOM",data.firstName(),x2,y,CARD_W-12-(x2-l));
        cardField(g,"3. NÉ(E) LE",data.birthDate(),x,y+28,94);
        cardField(g,"4. À",data.birthPlace(),x2,y+28,CARD_W-12-(x2-l));
        cardField(g,"5. NATIONALITÉ",data.nationality(),x,y+56,CARD_W-12-(x-l));
        g.fill(l+2,t+CARD_H-6,l+CARD_W-2,t+CARD_H-4,CARD_BAND);
    }
    private void cardField(GuiGraphics g,String label,String value,int x,int y,int w){
        g.drawString(font,label,x,y,CARD_LABEL,false);
        g.drawString(font,MineNorthStyle.bold(font.plainSubstrByWidth(value==null?"":value,w-6)),x,y+10,CARD_DARK,false);
    }
    private ResourceLocation findSkin(){Minecraft mc=Minecraft.getInstance();if(mc.getConnection()!=null){PlayerInfo i=mc.getConnection().getPlayerInfo(data.subjectUuid());if(i!=null)return i.getSkinLocation();}return DefaultPlayerSkin.getDefaultSkin(data.subjectUuid());}
    @Override public boolean isPauseScreen(){return false;}
}
