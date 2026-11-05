# MineNorth Identité

Mod Forge 1.20.1 indépendant pour le système d'identité MineNorth RP.

## Commandes
- `/cidmenu <joueur>` : OP uniquement, ouvre la consultation de l'identité d'un joueur qui a un personnage (carte perdue).
- `/cidedit <joueur>` : OP uniquement, rouvre chez un joueur en ligne l'éditeur d'apparence complet (corps, visage, peau, cheveux, pilosité, maquillage, tatouages, piercings, dessins) s'il a oublié quelque chose sur son skin ; l'identité, le sexe et la tenue ne changent pas. La modification est valable 10 minutes et une seule fois.
- `/cidreset <joueur>` : OP uniquement, supprime le personnage (identité et apparence) ; le joueur doit en recréer un (l'écran se rouvre tout de suite s'il est en ligne, sinon à sa prochaine connexion).

## Fonctionnalités
- création de personnage obligatoire à la première connexion : identité (nom, prénom, sexe, date et lieu de naissance, nationalité) puis apparence (corps, visage, peau, cheveux, pilosité, maquillage, tatouages, piercings) avec aperçu 3D ;
- le joueur est immobile et ne peut ni chatter ni lancer de commande (sauf OP) tant que la création n'est pas terminée ;
- « Choisir mon skin » : le skin créé s'applique au joueur et est visible par les autres (fiche de choix validée par le serveur, texture générée par chaque client) ; « Garder mon skin Minecraft » conserve le skin Mojang ;
- identité et sexe définitifs (seul `/cidreset` les change) ; apparence modifiable plus tard par d'autres mods via l'API d'apparence de `minenorth-api` (`MineNorth.appearance()`), section par section, jamais le sexe ;
- numéro de carte, carte d'identité physique (avec le sexe), présentation par sneak + clic droit sur un autre joueur, lecture par clic droit, réédition en cas de perte ;
- tatouages du catalogue : 30 figures, dont étoile, cœur, croix, ancre, rose, crâne, ailes, flamme, éclair, soleil, couronne, tribal, lune, papillon, serpent, poignard et patte (étoile, cœur, croix, ancre, rose, crâne, ailes, flamme, éclair, soleil, couronne, tribal) avec une taille au choix par zone, petit par défaut, et une encre (couleurs, noir ou gris ombré) ; le visage a 12 petites marques discrètes de 3×3 pixels ; les anciennes fiches gardent leurs tatouages en grand ;
- salons : interface dédiée pour chaque salon (carnet de motifs avec position libre, une jambe droite et une jambe gauche indépendantes, pour le tatouage ; miroir pour le coiffeur) ; fauteuil de tatouage, desserte de tatoueur, fauteuil de coiffeur, desserte de coiffeur et miroir de coiffeur (onglet créatif MineNorth). Clic droit sur un fauteuil : l'éditeur s'ouvre chez le joueur, limité à ce que le salon permet (tatouages et piercings, ou cheveux et pilosité) ;
- 12 coiffures de plus, toutes en volume 3D : natte sur le côté, demi-queue, macarons, chignon bas, lob, frange rideau (femmes) ; mulet, banane haute (hommes) ; dreadlocks, chignon haut, coupe au bol, frange emo (tous) ;
- salons payants : le prix est calculé par le serveur et prélevé par carte via EuroBank (`MineNorth.bank().charge`, source `salon:tatouage` ou `salon:coiffeur`, remise de grade appliquée) ; on ne paie que ce qui change. Tatouage : petit 15 €, moyen 30 €, grand 50 €, marque du visage 10 €, encre de couleur +10 €, retrait 20 €, tatouage dessiné 120 € (torse, dos) ou 80 € (bras, jambes) ; chaque jambe a son propre tatouage. Coiffeur : coupe 25 €, couleur 15 €, style de barbe 10 €, couleur de barbe 8 €. Sans EuroBank, les salons sont gratuits ;
- piercings : oreilles (clou, anneau, pendant, grand anneau), haut de l'oreille, nez, sourcil, lèvre et nombril, dans cinq métaux au choix ;
- plus de « garder mon skin Minecraft » : tout le monde prend le skin créé, mais chacun peut importer la tête de son skin Minecraft (bouton « Ma tête » du dessin du visage) et la retoucher ;
- dessiner soi-même (palette limitée, éditeur de pixels : crayon, gomme, pipette, remplissage, annuler / rétablir) : le visage (avec import de la tête du skin Minecraft), la barbe et la moustache, et les tatouages (torse, dos, bras, jambes), par-dessus les choix du catalogue ; les barbes ont des favoris et une mâchoire détaillés ;
- sous-vêtements (items « Caleçon » et « Sous-vêtements (femme) », onglet créatif MineNorth) : le personnage les porte dès sa création ; clic droit sur l'item pour le porter, bouton à droite du livre de recettes dans l'inventaire pour les retirer ou les remettre, l'item revient dans l'inventaire ; ils se dessinent sur la couche extérieure du skin (visibles seulement avec « Choisir mon skin ») ;
- nom RP dans le chat et le TAB une fois le personnage créé ;
- migration : les joueurs qui avaient une identité d'avant cette version refont tout (nouvelle carte, l'ancienne n'est plus valide).

## Nom affiché (chat et TAB)
`document/DisplayNames` est le **seul** endroit qui compose le nom (les autres mods fournissent leur morceau via l'API MineNorth 1.2.0) :
1. mode staff (Admin, `StaffService`) : « [Rôle] Prénom Nom » en couleur, même sous une cagoule ; préfixe VIP retiré sauf `garderPrefixeVip` ;
2. cagoule (Vêtements, `MaskService`) : « Individu masqué », sans préfixe VIP — chat seulement, le TAB garde le vrai nom ;
3. citoyen : préfixe de grade (VIP, `GradeService.prefix`) + « Prénom Nom » (pseudo sans carte).

## Licence

**Tous droits réservés - MineNorthRP.** Réutilisation, copie, modification, décompilation / ingénierie
inverse (y compris par outils d'intelligence artificielle) et utilisation pour entraîner une IA sont
**interdites** sans autorisation écrite. Voir [LICENSE](LICENSE).
