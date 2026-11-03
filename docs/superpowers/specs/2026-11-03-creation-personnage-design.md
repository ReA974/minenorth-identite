# Création de personnage MineNorthRP — design

Mod : `minenorth-identite` (Forge 1.20.1). Touche aussi `minenorth-api` (dépôt voisin).

## Intention

Aujourd'hui l'identité est créée par un écran que le staff ouvre (`/cidmenu`). On veut que tout nouveau joueur
crée son personnage lui-même dès sa première connexion : identité, puis apparence, avec un écran de création
dans le style de la capture de référence (onglets à gauche, aperçu 3D au centre, options à droite). Le skin
créé peut ensuite s'appliquer au joueur. L'apparence doit pouvoir être modifiée plus tard par d'autres mods
(coiffeur, tatoueur, chirurgien…).

## Objectifs

- Sexe ajouté à l'identité et imprimé sur la carte d'identité (« Homme » / « Femme »).
- Création obligatoire à la première connexion (plus de session ouverte par le staff pour créer).
- Éditeur d'apparence en onglets : Accueil, Corps, Visage, Peau, Cheveux, Pilosité, Maquillage, Tatouages,
  Identité, Récapitulatif. Aperçu 3D rotatif, boutons Précédent / Aléatoire / Annuler / Rétablir / Suivant.
- Fin de parcours : case « mon nom respecte le règlement RP », puis « Choisir mon skin » (le skin créé
  s'applique) ou « Garder mon skin Minecraft ».
- `/cidreset` supprime identité ET apparence ; le joueur doit tout recréer (écran rouvert tout de suite s'il
  est en ligne, sinon à sa prochaine connexion). Même chose pour `PlayerWipeEvent`.
- Textures des options (coupes, yeux, nez, tatouages…) générées par code, pas de fichiers dessinés.
- Tous les textes en français (`en_us` = `fr_fr`, comme aujourd'hui).

## Hors périmètre (non-objectifs)

- Les boutiques coiffeur / tatoueur elles-mêmes : c'est un autre mod. Ici on fournit seulement l'API.
- Vêtements (le magasin de vêtements de la capture est à venir et hors de ce chantier). Seuls les sous-vêtements
  de base sont affichés.
- Les cadenas / pastilles « modifiable chez… » de la capture : supprimés.

## Anti-objectifs

- Un client modifié ne doit jamais pouvoir imposer une image ou des valeurs hors catalogue.
- Un autre mod ne doit jamais pouvoir changer le sexe ou l'identité via l'API d'apparence.
- Un reset ne doit jamais laisser un joueur jouable sans personnage.

## Règles de modification

| Donnée | Qui peut la changer |
|---|---|
| Identité (nom, prénom, date et lieu de naissance, nationalité, **sexe**) | Personne, sauf reset par le staff |
| Corps (corpulence), Visage, Peau, Cheveux, Pilosité, Maquillage, Tatouages | Modifiables plus tard par d'autres mods via l'API |

Le sexe se choisit dans l'onglet Corps pendant la création (il détermine le gabarit des bras et le catalogue de
coupes), mais il est enregistré dans l'identité. Quand un autre mod rouvre l'éditeur, il est affiché en lecture
seule. L'âge n'est pas saisi : il se calcule depuis la date de naissance et s'affiche sur l'écran Identité.

## Approches envisagées

- **A. Le client fabrique l'image du skin, le serveur la stocke et la diffuse.** Simple, mais le serveur reçoit un
  fichier qu'un client modifié peut truquer, et un mod coiffeur devrait refaire l'image entière.
- **B. Le serveur ne stocke que la fiche de choix ; chaque client fabrique l'image.** Réseau minuscule, validation
  serveur contre le catalogue, modification par section naturelle. **Choisie** par l'utilisateur.

## Conception retenue (approche B)

### Données
- `Profile` (identité) reçoit un champ `gender` (`MALE` / `FEMALE`).
- Nouvelle fiche `Appearance` par joueur, sauvegardée comme l'identité (SavedData serveur) : choix par section
  (id de catalogue + couleurs). Sections indépendantes : `body`, `face`, `skin`, `hair`, `facialHair`,
  `makeup`, `tattoos`.
- Un catalogue unique (code partagé client/serveur) liste pour chaque section les options valides, leurs couleurs
  et, pour les coupes, le sexe auquel elles s'adressent. Le serveur rejette tout ce qui n'y figure pas.

### Skin
- Chaque client assemble un skin 64×64 à partir d'une fiche : calques générés par code (peau, yeux, nez, bouche,
  cheveux, pilosité, maquillage, tatouages, sous-vêtements), cache en mémoire, texture dynamique.
- Le serveur envoie la fiche (quelques dizaines d'octets) à la connexion d'un joueur, à l'arrivée d'un autre
  joueur et à chaque modification. À l'affichage, le client remplace le skin des joueurs ayant choisi « Choisir
  mon skin ». Le gabarit des bras (classique / fin) suit le sexe.
- « Garder mon skin Minecraft » : la fiche reste enregistrée (elle sert à l'aperçu et peut resservir) mais aucun
  remplacement n'est fait.

### Parcours de première connexion
- À la connexion, si le joueur n'a pas de personnage, le serveur ouvre l'écran de création. Le joueur est figé
  (pas de déplacement, pas de dégâts, pas de commandes) tant que la création n'est pas terminée, et l'écran ne se
  ferme pas avec Échap.
- Si le joueur se déconnecte en cours de route, rien n'est enregistré ; il recommence à la reconnexion.
- La validation finale est entièrement faite côté serveur (champs d'identité comme aujourd'hui + catalogue
  d'apparence + case règlement RP). La carte d'identité physique est remise à la fin comme aujourd'hui.
- `/cidmenu` reste pour le staff (consultation, carte perdue) ; l'ancien mécanisme de session qui autorisait la
  création sur ouverture staff est remplacé par un état « création en cours » côté serveur.

### Écran
- Style MineNorth existant (`MineNorthStyle`), titre « MineNorthRP · Bureau de l'immigration », maquette
  validée avec l'utilisateur (onglets à gauche, aperçu 3D au centre avec présélections de caméra, options à
  droite). Navigation clavier comme sur la capture.
- Annuler / Rétablir : historique local des choix de l'onglet courant.

### API pour les autres mods (`minenorth-api`)
- `Identity` reçoit `gender`. `IdentityService` reste compatible.
- Nouveau service d'apparence : lire la fiche d'un joueur, remplacer une section (validée contre le catalogue,
  sans toucher à l'identité), ouvrir l'éditeur limité à une ou plusieurs sections pour un joueur. Le sexe n'est
  jamais modifiable par ce service.

### Protocole réseau
- Le protocole du canal passe à `3`. Les clients sans le mod à jour sont refusés (déjà le cas pour toute
  incompatibilité de version).

## Migration

Décision de l'utilisateur (option A) : **tous les joueurs qui ont déjà une identité refont tout depuis zéro**,
identité comprise. Concrètement :
- « Le joueur a un personnage » signifie « identité ET apparence présentes ». Un joueur avec une ancienne
  identité (sans sexe ni apparence) est donc traité comme un joueur sans personnage.
- À sa première connexion après la mise à jour, l'écran de création complet s'ouvre. À la validation, la nouvelle
  identité remplace l'ancienne : nouveau numéro de carte, l'ancienne carte physique devient invalide
  (« Cette carte d'identité n'est plus valide »).
- `Profile` doit quand même se charger sans `gender` (anciennes sauvegardes) sans erreur ; ces profils restent en
  base jusqu'au remplacement, mais ne comptent pas comme personnage valide (pas de nom RP au chat tant que la
  création n'est pas faite).
- Aucun mode « compléter la partie manquante » n'est nécessaire.

## Choix faits par le concepteur (à relire)

- **Dépendance `minenorth-api`** : c'est un jar voisin (`flatDir ../minenorth-api/build/libs`), à construire avant
  ce mod. On ajoute `gender` à `Identity` en gardant l'ancien constructeur pour ne pas casser les mods existants.
- **État de création** : le serveur garde un état « création en cours » par joueur (remplace `SESSIONS`).
  Un seul paquet « terminer la création » transporte identité + apparence + case RP ; pendant la création, le
  catalogue de coupes suit le sexe envoyé dans ce paquet, ensuite celui de l'identité enregistrée. Les
  modifications ultérieures passent uniquement par l'API, section par section.
- **Corpulence** : une peau 64×64 ne peut pas changer de silhouette. Normale / Athlétique / Ronde sont donc des
  variantes d'ombrage et de détails sur la texture (muscles, ventre), pas un changement de géométrie. Seule la
  finesse des bras change, et elle suit le sexe (Homme : classiques, Femme : fins, comme sur la capture).
- **Remplacement du skin** : sans mixin, par transformateur d'accès (`PlayerInfo`) ; le choix « mon skin /
  skin Minecraft » est un drapeau stocké dans `Appearance`, diffusé avec la fiche, modifiable seulement par le
  staff (reset) ou l'API.
- **Reset / wipe** : le serveur diffuse « aucune fiche » pour ce joueur afin que les clients retirent le skin
  remplacé, que le joueur soit en ligne, en création ou hors ligne. Un reset pendant une création relance la
  création à zéro.
- **Gel pendant la création** : pas de déplacement, de dégâts, d'interaction ni de commandes (sauf opérateurs) ;
  le chat est bloqué. Un opérateur peut débloquer un joueur coincé avec `/cidreset`.
- **Règle RP** : côté serveur, la case cochée plus les filtres de noms déjà présents (`cleanName`). Pas de
  filtre de célébrités.

## Laissé au constructeur

Format exact des textures procédurales et du catalogue (nombre de coupes, couleurs), disposition fine de
l'écran, rendu 3D de l'aperçu (entité factice dans l'écran), format de sauvegarde, tests.
