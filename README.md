# MineNorth Identité

Mod Forge 1.20.1 indépendant pour le système d'identité MineNorth RP.

## Commandes
- `/cidmenu <joueur>` : OP uniquement, ouvre la consultation de l'identité d'un joueur qui a un personnage (carte perdue).
- `/cidreset <joueur>` : OP uniquement, supprime le personnage (identité et apparence) ; le joueur doit en recréer un (l'écran se rouvre tout de suite s'il est en ligne, sinon à sa prochaine connexion).

## Fonctionnalités
- création de personnage obligatoire à la première connexion : identité (nom, prénom, sexe, date et lieu de naissance, nationalité) puis apparence (corps, visage, peau, cheveux, pilosité, maquillage, tatouages) avec aperçu 3D ;
- le joueur est immobile et ne peut ni chatter ni lancer de commande (sauf OP) tant que la création n'est pas terminée ;
- « Choisir mon skin » : le skin créé s'applique au joueur et est visible par les autres (fiche de choix validée par le serveur, texture générée par chaque client) ; « Garder mon skin Minecraft » conserve le skin Mojang ;
- identité et sexe définitifs (seul `/cidreset` les change) ; apparence modifiable plus tard par d'autres mods via l'API d'apparence de `minenorth-api` (`MineNorth.appearance()`), section par section, jamais le sexe ;
- numéro de carte, carte d'identité physique (avec le sexe), présentation par sneak + clic droit sur un autre joueur, lecture par clic droit, réédition en cas de perte ;
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
