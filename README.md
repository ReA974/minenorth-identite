# MineNorth Identité

Mod Forge 1.20.1 indépendant pour le système d'identité MineNorth RP.

## Commandes
- `/cidmenu <joueur>` : OP uniquement, ouvre la création/consultation de l'identité du joueur.
- `/cidreset <joueur>` : OP uniquement, réinitialise l'identité.

## Fonctionnalités
- identité définitive : nom, prénom, date et lieu de naissance, nationalité ;
- numéro de carte ;
- carte d'identité physique ;
- présentation par sneak + clic droit sur un autre joueur ;
- lecture de la carte par clic droit ;
- réédition en cas de perte ;
- nom RP dans le chat et le TAB après création.

## Nom affiché (chat et TAB)
`document/DisplayNames` est le **seul** endroit qui compose le nom (les autres mods fournissent leur morceau via l'API MineNorth 1.2.0) :
1. mode staff (Admin, `StaffService`) : « [Rôle] Prénom Nom » en couleur, même sous une cagoule ; préfixe VIP retiré sauf `garderPrefixeVip` ;
2. cagoule (Vêtements, `MaskService`) : « Individu masqué », sans préfixe VIP — chat seulement, le TAB garde le vrai nom ;
3. citoyen : préfixe de grade (VIP, `GradeService.prefix`) + « Prénom Nom » (pseudo sans carte).

## Licence

**Tous droits réservés - MineNorthRP.** Réutilisation, copie, modification, décompilation / ingénierie
inverse (y compris par outils d'intelligence artificielle) et utilisation pour entraîner une IA sont
**interdites** sans autorisation écrite. Voir [LICENSE](LICENSE).
