# Création de personnage MineNorthRP — plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tout joueur sans personnage crée son identité (avec sexe) puis son apparence à la première connexion ; le skin créé s'applique via une fiche de choix validée par le serveur et assemblée par chaque client ; l'apparence est modifiable plus tard par d'autres mods via `minenorth-api`.

**Architecture:** Logique pure (catalogue, validation, assemblage du skin) en classes Java testables sans Minecraft ; stockage serveur en `SavedData` ; le serveur ne diffuse que la fiche (entiers), chaque client génère la texture 64×64 et la substitue au skin Mojang par transformateur d'accès (sans mixin). Écran de création dans le style `MineNorthStyle`, aperçu 3D rendu avec un `PlayerModel` baké directement.

**Tech Stack:** Forge 1.20.1 (47.4.10), Java 17, mappings officiels, SimpleChannel, JUnit 5 (à ajouter), jar voisin `minenorth-api`.

**Spec:** `docs/superpowers/specs/2026-11-03-creation-personnage-design.md`

## Global Constraints

- Paquets Java sous `fr.minenorth.identite` ; mod id `minenorthidentite`.
- Protocole du canal : `"3"` (aujourd'hui `"2"`).
- Tous les textes visibles en français ; titre d'écran « MineNorthRP · Bureau d'enregistrement » ; aucune mention de Vice City.
- Identité (nom, prénom, date et lieu de naissance, nationalité, sexe) : jamais modifiable, sauf reset staff. Le sexe n'est jamais modifiable par l'API d'apparence.
- Apparence (corps, visage, peau, cheveux, pilosité, maquillage, tatouages) : modifiable plus tard par l'API, section par section, validée contre le catalogue.
- Le serveur ne reçoit et ne stocke que des entiers de catalogue, jamais d'image.
- Nom, prénom, lieu, nationalité : 32 caractères max, lettres/espaces/`-`/`'` (règles de `cleanName` actuelles) ; date au format `JJ/MM/AAAA`.
- Joueur « avec personnage » = identité avec sexe ET apparence présentes. Une ancienne identité sans sexe n'en est pas un.
- Migration : les joueurs existants refont tout ; leur ancienne carte devient invalide (nouveau numéro).
- Skin 64×64 ; bras fins si Femme, classiques si Homme ; corpulence = variantes d'ombrage uniquement.
- Gel pendant la création : pas de déplacement, dégâts, interaction, chat ni commandes (sauf opérateurs) ; Échap ne ferme pas l'écran.
- Licence : tous droits réservés, ne rien ajouter qui la contredise.

## Review Focus

Entrées et cas que la spec sous-entend sans que les tâches les prouvent ailleurs, les plus probables en premier :

1. Paquet falsifié : sexe Femme avec une coupe réservée aux hommes, pilosité faciale pour une Femme, entier hors plage, tableau de mauvaise longueur, case RP décochée → refusé, rien enregistré (Task 3, Task 5).
2. Date de naissance dans le futur, 29/02 d'une année non bissextile, âge absurde (> 110 ans) → refusés (Task 2).
3. Déconnexion en pleine création, puis reconnexion → création repart de zéro, aucun profil partiel (Task 5).
4. Reset d'un joueur en création, hors ligne ou en ligne → plus aucune fiche, les autres clients retirent le skin remplacé, la création se relance (Task 5, Task 8).
5. Ancienne sauvegarde sans sexe → se charge sans erreur, n'est pas un personnage valide, la nouvelle création remplace l'ancienne carte (Task 1, Task 5).
6. Éditeur ouvert par l'API pour la section CHEVEUX, client qui renvoie aussi CORPS ou un changement de sexe → seule CHEVEUX est acceptée (Task 9).

---

## File Structure

| Fichier | Rôle |
|---|---|
| `document/Gender.java` (créer) | Enum `MALE`/`FEMALE`, libellé, bras fins |
| `document/IdentityData.java` (modifier) | `Profile.gender` nullable, sauvegarde/lecture tolérantes |
| `document/CharacterValidator.java` (créer) | Nettoyage et validation de l'identité (extrait d'`IdentityManager`), calcul d'âge |
| `appearance/AppearanceSection.java`, `Appearance.java`, `Catalog.java` (créer) | Fiche de choix, catalogue, validation, aléatoire |
| `appearance/AppearanceData.java` (créer) | `SavedData` fiche par joueur |
| `character/CreationTracker.java` (créer) | Ensemble des joueurs en création (pur) |
| `character/CharacterManager.java` (créer) | Création, reset, gel, diffusion des fiches, gating du nom RP |
| `skin/SkinComposer.java`, `skin/Palette.java` (créer) | Assemblage pur du skin 64×64 en `int[]` ARGB |
| `client/skin/ClientSkins.java` (créer) | Textures dynamiques, cache, substitution du skin |
| `client/creator/*` (créer) | `CreatorScreen`, onglets, widgets, `PreviewRenderer` |
| `network/ModNetwork.java` (modifier) | Protocole 3 et nouveaux paquets |
| `document/IdentityManager.java` (modifier) | Retirer la création par session staff ; garder cartes et présentation |
| `client/IdentityScreen.java` (modifier) | Champ sexe, tête avec skin créé |
| `command/IdentityCommands.java`, `IdentityWipe.java` (modifier) | Reset complet |
| `IdentityProvider.java` + `minenorth-api` (modifier) | `Identity.gender`, `AppearanceService` |
| `META-INF/accesstransformer.cfg` (créer), `build.gradle` (modifier) | Transformateur d'accès, JUnit |

Tests sous `src/test/java/fr/minenorth/identite/...`, miroir des paquets.

---

### Task 1: Socle de test, sexe dans le profil, API

**Files:**
- Modify: `build.gradle` (JUnit 5, `test { useJUnitPlatform() }`)
- Create: `src/main/java/fr/minenorth/identite/document/Gender.java`
- Modify: `src/main/java/fr/minenorth/identite/document/IdentityData.java`
- Modify: `../minenorth-api` (`Identity` record : ajouter `String gender`, garder l'ancien constructeur), puis reconstruire le jar
- Modify: `IdentityProvider.java:30` (`toApi` passe le sexe)
- Test: `src/test/java/fr/minenorth/identite/document/IdentityDataTest.java`

**Interfaces:**
- Produces: `enum Gender { MALE, FEMALE; String label() /* "Homme"/"Femme" */; boolean slimArms(); static Gender fromName(String n) /* null si inconnu */ }` ; `Profile(UUID uuid, String lastName, String firstName, String birthDate, String birthPlace, String nationality, String cardNumber, Gender gender)` avec `gender` nullable et `boolean isLegacy()` ; `IdentityData.load(CompoundTag)` et `save(CompoundTag)` accessibles au test (package-private).

- [ ] **Step 1: Vérifier que le build existe** — Run: `./gradlew build` (le `flatDir ../minenorth-api/build/libs` est relatif au dépôt : si le worktree ne trouve pas le jar, construire `minenorth-api` d'abord ou pointer `flatDir` vers le chemin absolu du dépôt principal sans commiter ce changement local). Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 2: Ajouter JUnit** dans `build.gradle` : `testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'` et `test { useJUnitPlatform() }`.
- [ ] **Step 3: Écrire le test** `IdentityDataTest` : `legacyProfileLoadsWithNullGender` (tag sans clé `Gender` → `get(uuid).gender()==null`, `isLegacy()` vrai) ; `genderRoundTrips` (save puis load d'un profil `FEMALE` → `FEMALE`) ; `unknownGenderStringLoadsAsNull`.
- [ ] **Step 4: Run** `./gradlew test --tests '*IdentityDataTest'` — Expected: FAIL (types absents).
- [ ] **Step 5: Implémenter** `Gender`, le champ `gender` dans `Profile`, la clé NBT `"Gender"` (nom de l'enum) en lecture tolérante et écriture seulement si non null. Mettre à jour les appels existants de `new Profile(...)` (dans `IdentityManager`) en passant `null` provisoirement.
- [ ] **Step 6: API** : dans `minenorth-api`, ajouter `gender` (String `"MALE"`/`"FEMALE"`/`null`) à `Identity` avec un constructeur à 7 arguments conservé ; `IdentityProvider.toApi` passe `p.gender()==null?null:p.gender().name()`. Reconstruire le jar de l'API.
- [ ] **Step 7: Run** `./gradlew test build` — Expected: PASS.
- [ ] **Step 8: Commit** `feat: sexe dans le profil et l'API d'identité`.

---

### Task 2: Validation de l'identité (pure)

**Files:**
- Create: `src/main/java/fr/minenorth/identite/document/CharacterValidator.java`
- Test: `src/test/java/fr/minenorth/identite/document/CharacterValidatorTest.java`

**Interfaces:**
- Produces: `static String clean(String)`, `static String cleanName(String)`, `static boolean validDate(String)`, `static java.util.Optional<Integer> age(String birthDate, java.time.LocalDate today)` (vide si date invalide), `static java.util.Optional<String> validateIdentity(String lastName, String firstName, String birthDate, String birthPlace, String nationality, java.time.LocalDate today)` (message d'erreur en français, vide si valide). Les valeurs retournées par `clean`/`cleanName` sont celles à enregistrer.

- [ ] **Step 1: Tests** : `cleanNameRejectsSectionSignAndDigits` (`"§cJean"`→vide, `"Jean2"`→vide, `"Jean-Luc"`→`"Jean-Luc"`, `"  Éloïse   Anne "`→`"Éloïse Anne"`) ; `dateRules` (`29/02/2023` invalide, `29/02/2024` valide, `31/04/2000` invalide) ; `ageComputedFromBirthDate` (`14/10/1996` au `2026-11-03` → 30 ; la veille de l'anniversaire → 29) ; `futureDateRejected` ; `ageOver110Rejected` ; `blankFieldsRejected`.
- [ ] **Step 2: Run** — Expected: FAIL.
- [ ] **Step 3: Implémenter** en déplaçant `clean`, `cleanName`, `validDate`, `MAX_FIELD` depuis `IdentityManager.java:228-251` (les rendre publics ici, supprimer les copies, mettre à jour les appels). Règles ajoutées : date future refusée, âge > 110 refusé.
- [ ] **Step 4: Run** `./gradlew test` — Expected: PASS.
- [ ] **Step 5: Commit** `refactor: extraire la validation d'identité, âge et bornes de date`.

---

### Task 3: Fiche d'apparence et catalogue (purs)

**Files:**
- Create: `appearance/AppearanceSection.java`, `appearance/Appearance.java`, `appearance/Catalog.java`
- Test: `src/test/java/fr/minenorth/identite/appearance/CatalogTest.java`, `AppearanceCodecTest.java`

**Interfaces:**
- Produces:
  - `enum AppearanceSection { BODY, FACE, SKIN, HAIR, FACIAL_HAIR, MAKEUP, TATTOOS }`
  - `final class Appearance` (immuable ; `equals`/`hashCode` par contenu) : `int[] get(AppearanceSection)` (copie), `Appearance with(AppearanceSection, int[])`, `boolean customSkin()`, `Appearance withCustomSkin(boolean)`, `CompoundTag toTag()`, `static Appearance fromTag(CompoundTag)` (valeurs hors plage → `IllegalArgumentException`), `void write(FriendlyByteBuf)`, `static Appearance read(FriendlyByteBuf)` (longueurs ≠ nombre d'emplacements → `IllegalArgumentException`).
  - `Catalog` : `record Slot(String key, int options, java.util.Set<Integer> maleOnly, java.util.Set<Integer> femaleOnly)` avec `boolean allows(Gender g, int v)` ; `static java.util.List<Slot> slots(AppearanceSection)` ; `static Appearance defaults(Gender)` ; `static Appearance random(Gender, java.util.Random)` ; `static java.util.Optional<String> validate(Gender, AppearanceSection, int[])` ; `static java.util.Optional<String> validate(Gender, Appearance)` (message d'erreur ou vide).
- Valeurs du catalogue figées par la spec : BODY = 1 emplacement `build` (3 : Normale, Athlétique, Ronde) ; FACE = `eyes` (6), `eyeColor` (8), `nose` (3), `mouth` (4) ; SKIN = `tone` (10), `marks` (4) ; HAIR = `style` (12, 0 = aucun, au moins 3 coupes réservées à chaque sexe) et `color` (12) ; FACIAL_HAIR = `style` (5, seule la valeur 0 permise pour Femme) et `color` (12) ; MAKEUP = `lips` (4), `lipColor` (8), `eyes` (4), `eyeColor` (8) ; TATTOOS = 6 emplacements (visage-cou, torse, dos, bras droit, bras gauche, jambes) de 7 valeurs (0 = aucun, 6 motifs).

- [ ] **Step 1: Tests Catalog** : `defaultsAreValidForBothGenders` ; `randomIsAlwaysValid` (500 tirages par sexe, graine fixe) ; `femaleCannotHaveFacialHair` ; `maleOnlyHairRejectedForFemale` ; `outOfRangeRejected` (valeur −1 et `options`) ; `wrongLengthRejected` ; `everySectionHasSlots`.
- [ ] **Step 2: Tests codec** : `tagRoundTrip` ; `bufferRoundTrip` (`Unpooled.buffer()` enveloppé dans `FriendlyByteBuf`) ; `readRejectsWrongLength` ; `customSkinFlagSurvives`.
- [ ] **Step 3: Run** — Expected: FAIL.
- [ ] **Step 4: Implémenter** les trois classes. `Appearance` stocke une `EnumMap<AppearanceSection,int[]>` ; `fromTag`/`read` valident seulement la forme (longueurs, plage) ; la règle « selon le sexe » est dans `Catalog.validate(Gender, …)`.
- [ ] **Step 5: Run** `./gradlew test` — Expected: PASS.
- [ ] **Step 6: Commit** `feat: fiche d'apparence et catalogue validé`.

---

### Task 4: Assemblage du skin (pur)

**Files:**
- Create: `skin/Palette.java`, `skin/SkinComposer.java`
- Test: `src/test/java/fr/minenorth/identite/skin/SkinComposerTest.java`

**Interfaces:**
- Consumes: `Gender`, `Appearance`, `Catalog` (Task 3).
- Produces: `SkinComposer.compose(Gender, Appearance) -> int[]` (4096 pixels ARGB, ligne par ligne, 64×64, couche de base + couche extérieure) ; `SkinComposer.SIZE = 64` ; `Palette.skinTone(int)`, `Palette.hairColor(int)`, `Palette.eyeColor(int)`, `Palette.lipColor(int)` → ARGB.

Disposition du skin : format standard 1.8+ (tête 8,8 face, bras gauche/droit en 32,48 et 40,16 ; pour des bras fins la colonne la plus à droite de chaque face de bras reste transparente).

- [ ] **Step 1: Tests** : `sizeIs4096` ; `skinToneFillsHeadFront` (pixel (12,12) de la face = `Palette.skinTone(tone)` pour une peau sans marques, cheveux 0, pilosité 0) ; `differentTonesDiffer` ; `hairStyleChangesTopOfHead` (coupe 0 vs une coupe non nulle → pixels du dessus de la tête différents) ; `hairColorApplied` ; `slimArmsLeaveLastColumnTransparentForFemale` ; `classicArmsFilledForMale` ; `buildChangesShadingOnly` (les trois corpulences donnent le même alpha partout, au moins un pixel de torse différent) ; `tattooAppearsOnlyOnItsZone` (motif sur « bras droit » ne change aucun pixel de tête) ; `deterministic` (deux appels identiques) ; `customSkinFlagIgnored`.
- [ ] **Step 2: Run** — Expected: FAIL.
- [ ] **Step 3: Implémenter** `compose` par peinture de calques successifs : peau + corpulence + marques → yeux/nez/bouche → maquillage → pilosité → cheveux (couche extérieure) → tatouages → sous-vêtements de base. Chaque calque est une méthode privée qui écrit dans le tableau ; motifs dessinés par code (rectangles de pixels), pas de fichier image.
- [ ] **Step 4: Run** `./gradlew test` — Expected: PASS.
- [ ] **Step 5: Commit** `feat: assemblage procédural du skin`.

---

### Task 5: Stockage serveur, création, reset, gel

**Files:**
- Create: `appearance/AppearanceData.java`, `character/CreationTracker.java`, `character/CharacterManager.java`
- Modify: `document/IdentityManager.java` (retirer `SESSIONS`, `save`, la création par `open` ; `nameFormat`, `tabListNameFormat`, `rpChat` : ne s'appliquent que si `CharacterManager.hasCharacter`), `command/IdentityCommands.java`, `IdentityWipe.java`
- Test: `character/CreationTrackerTest.java`, `character/CharacterRequestTest.java`

**Interfaces:**
- Consumes: Tasks 1–3 ; `CharacterValidator.validateIdentity`, `Catalog.validate`.
- Produces:
  - `AppearanceData` (`SavedData`, nom `"minenorthidentite_appearance"`) : `static get(ServerLevel)`, `Appearance get(UUID)` (null si absente), `void set(UUID, Appearance)`, `void remove(UUID)`, `Set<UUID> uuids()`.
  - `CreationTracker` : `begin(UUID)`, `boolean isCreating(UUID)`, `finish(UUID)`, `clear(UUID)`.
  - `CharacterManager.CreateRequest(String lastName, String firstName, String birthDate, String birthPlace, String nationality, Gender gender, Appearance appearance, boolean rulesAccepted)` ; `static Optional<String> check(CreateRequest r, LocalDate today)` (pure : identité + `Catalog.validate` + règle RP ; message ou vide) ; `static boolean hasCharacter(MinecraftServer, UUID)` ; `static void create(ServerPlayer, CreateRequest)` ; `static boolean reset(MinecraftServer, UUID)` (efface profil + fiche, diffuse « aucune fiche », relance la création si en ligne, `true` si quelque chose existait) ; `static void onJoin(ServerPlayer)`.

- [ ] **Step 1: Tests** `CreationTrackerTest` (`begin/isCreating/finish`, `clear` à la déconnexion, deux joueurs indépendants). `CharacterRequestTest` sur `check` : `validRequestPasses` ; `rulesNotAcceptedRejected` ; `femaleWithMaleHairRejected` ; `badNameRejected` ; `futureBirthRejected`.
- [ ] **Step 2: Run** — Expected: FAIL.
- [ ] **Step 3: Implémenter** `AppearanceData`, `CreationTracker`, `CharacterManager.check`.
- [ ] **Step 4: `create`** : refuser si pas « en création » (état posé par `onJoin`/`reset`/`/cidmenu` ancien profil) ; valider via `check` ; enregistrer le nouveau `Profile` (nouveau numéro de carte, remplace une ancienne identité) et la `Appearance` ; `tracker.finish` ; `giveCard` ; `player.refreshTabListName()` ; diffuser la fiche ; retirer le gel.
- [ ] **Step 5: `onJoin`** (`PlayerLoggedInEvent`) : si pas de personnage → `begin` + envoyer l'ouverture de création ; envoyer au joueur les fiches existantes et sa fiche aux autres. `PlayerLoggedOutEvent` → `clear` (rien n'est enregistré en cours de route).
- [ ] **Step 6: Gel** tant que `isCreating` (pour les non-opérateurs pour les commandes) : annuler `PlayerInteractEvent`, `LivingAttackEvent`/`LivingHurtEvent` sur le joueur, `ServerChatEvent`, `CommandEvent` ; remettre le joueur à sa position chaque tick (`TickEvent.PlayerTickEvent`, phase `END`).
- [ ] **Step 7: `reset`** : remplace `IdentityManager.reset` (le supprimer et rediriger `IdentityCommands` et `IdentityWipe` ; message « Aucun personnage » si rien) ; hors ligne : retirer profil et fiche, la création se fera à la prochaine connexion ; en ligne : `begin` + ouverture + message.
- [ ] **Step 8: Run** `./gradlew test build` — Expected: PASS ; la compilation prouve que les paquets de la Task 6 manquent encore → laisser les envois derrière des méthodes `ModNetwork.sendOpenCreation(ServerPlayer)` et `ModNetwork.sendSheet(ServerPlayer target, UUID subject, Gender, Appearance /* null = aucune */)` créées en squelette ici et complétées en Task 6.
- [ ] **Step 9: Commit** `feat: création serveur, reset complet et gel des joueurs en création`.

---

### Task 6: Réseau (protocole 3)

**Files:**
- Modify: `network/ModNetwork.java`
- Test: `network/PacketCodecTest.java`

**Interfaces:**
- Produces (S→C sauf mention) : `OpenCreationPacket()` ; `SheetPacket(UUID subject, boolean present, Gender gender, Appearance appearance)` (si `!present` : gender/appearance null, le client supprime la fiche) ; `CreateCharacterPacket` (C→S) portant les champs de `CharacterManager.CreateRequest` ; `IdentityViewPacket` gagne `String gender` (« Homme »/« Femme »/« ») ; `IdentitySavePacket` supprimé. Chaînes limitées à `readUtf(64)` ; `Appearance.read` sur entrée invalide → le handler ignore le paquet et ne répond rien.
- Méthodes d'envoi : `sendOpenCreation(ServerPlayer)`, `sendSheet(ServerPlayer target, UUID subject, Gender, Appearance)`, `broadcastSheet(MinecraftServer, UUID subject, Gender, Appearance)`.

- [ ] **Step 1: Test** `PacketCodecTest` : encode/decode de `SheetPacket` (présent et absent), de `CreateCharacterPacket`, et `invalidAppearanceBytesDoNotThrowOutOfDecode` (le décodage échoue proprement avec `IllegalArgumentException`).
- [ ] **Step 2: Run** — Expected: FAIL.
- [ ] **Step 3: Implémenter** les paquets, passer `PROTOCOL` à `"3"`, enregistrer les messages, et le handler de `CreateCharacterPacket` appelle `CharacterManager.create`.
- [ ] **Step 4: Run** `./gradlew test build` — Expected: PASS.
- [ ] **Step 5: Commit** `feat: protocole 3, paquets de création et de diffusion des fiches`.

---

### Task 7: Skin côté client (textures et substitution)

**Files:**
- Create: `client/skin/ClientSkins.java`, `src/main/resources/META-INF/accesstransformer.cfg`
- Modify: `build.gradle` (`accessTransformer = file('src/main/resources/META-INF/accesstransformer.cfg')` dans `minecraft { }`), `client/ClientNetworkHandler.java`

**Interfaces:**
- Consumes: `SkinComposer.compose`, `SheetPacket`.
- Produces: `ClientSkins.put(UUID, Gender, Appearance)`, `ClientSkins.remove(UUID)`, `ResourceLocation ClientSkins.textureFor(UUID, Gender, Appearance)` (cache, `DynamicTexture` à partir de `int[]`), `Optional<ResourceLocation> ClientSkins.skinOf(UUID)`.

- [ ] **Step 1: Transformateur d'accès** : rendre accessibles `PlayerInfo.textureLocations` et `PlayerInfo.skinModel` (vérifier les noms exacts dans les sources déobfusquées 1.20.1) ; ajouter la ligne dans `accesstransformer.cfg` et déclarer le fichier dans `build.gradle`.
- [ ] **Step 2: `ClientSkins`** : stocke les fiches reçues (`Map<UUID,…>`), génère une `DynamicTexture` par fiche, libère l'ancienne à la mise à jour ; si `appearance.customSkin()` est faux, ne remplace rien.
- [ ] **Step 3: Substitution** : sur `RenderPlayerEvent.Pre` (et à chaque réception de fiche), pour un joueur ayant une fiche avec `customSkin`, mettre dans son `PlayerInfo` l'entrée `SKIN` = notre texture et `skinModel` = `"slim"` si `Gender.slimArms()` sinon `"default"`. Sans fiche ou `customSkin` faux : ne rien toucher. Une fiche supprimée (`present=false`) restaure en laissant Minecraft recharger le `PlayerInfo` (retirer l'entrée dans `ClientSkins`).
- [ ] **Step 4: Vérification manuelle** : `./gradlew runClient`, se connecter avec un personnage de test (le seul chemin de test avant la Task 8 : commande de debug locale **non commitée** qui appelle `ClientSkins.put` avec `Catalog.random`) ; Expected: le skin généré s'affiche sur le joueur en vue F5 et bras fins pour Femme.
- [ ] **Step 5: Commit** `feat: textures de skin générées et substitution côté client`.

---

### Task 8: Écran de création

**Files:**
- Create: `client/creator/CreatorScreen.java`, `CreatorTab.java`, `OptionGrid.java`, `SwatchRow.java`, `PreviewRenderer.java`, `IdentityTab.java`, `RecapTab.java`
- Modify: `client/ClientNetworkHandler.java` (ouverture sur `OpenCreationPacket` et réception de `SheetPacket` → `ClientSkins`), `client/MineNorthStyle.java` (si besoin de petits helpers)
- Test: `client/creator/CreatorStateTest.java` (état pur extrait de l'écran)

**Interfaces:**
- Consumes: `Catalog`, `Appearance`, `SkinComposer`, `ClientSkins`, `CreateCharacterPacket`, `CharacterValidator`.
- Produces: `CreatorState` (pur) — `Gender gender()`, `Appearance appearance()`, `void set(AppearanceSection, int[])`, `void setGender(Gender)` (réinitialise la coupe et la pilosité si devenues invalides), `void undo()`, `void redo()`, `void randomizeSection(AppearanceSection, Random)`, `void resetSection(AppearanceSection)`, `boolean customSkin()`, `void setCustomSkin(boolean)` ; `CreatorScreen(CreatorMode mode)` avec `CreatorMode.creation()` ou `CreatorMode.editor(Set<AppearanceSection> allowed, Gender fixedGender, Appearance current)`.

- [ ] **Step 1: Tests `CreatorStateTest`** : `changingGenderResetsInvalidHair` ; `undoRedoRestoresPreviousChoice` ; `randomizeSectionStaysValid` ; `resetSectionRestoresDefaults` ; `undoOnEmptyHistoryIsNoOp`.
- [ ] **Step 2: Run** — Expected: FAIL.
- [ ] **Step 3: Implémenter `CreatorState`** (pur).
- [ ] **Step 4: Écran** (titre « MineNorthRP · Bureau d'enregistrement », mise en page de la maquette validée) : colonne d'onglets Accueil, Corps, Visage, Peau, Cheveux, Pilosité, Maquillage, Tatouages, Identité, Récapitulatif ; aperçu au centre ; options à droite (`OptionGrid` pour les styles, `SwatchRow` pour les couleurs, générés depuis `Catalog.slots`) ; boutons Précédent / Aléatoire / Annuler / Rétablir / Suivant ; raccourcis de la capture (Tab, flèches, Espace, R, Ctrl+Z/Ctrl+Y). Échap ne ferme pas en mode création. Le sexe se choisit dans l'onglet Corps ; en mode éditeur il est affiché en lecture seule.
- [ ] **Step 5: `PreviewRenderer`** : bake `PlayerModel` avec `ModelLayers.PLAYER` ou `PLAYER_SLIM` selon `Gender.slimArms()`, texture = `ClientSkins.textureFor`, rendu `RenderType.entityTranslucent` dans un `PoseStack` ; glisser = tourner, molette = zoom, double-clic = de face, présélections de caméra Corps/Buste/Visage/Dos/Br.G/Br.D/Jambes. Texture régénérée à chaque changement.
- [ ] **Step 6: `IdentityTab`** : champs prénom, nom, lieu de naissance, nationalité (défaut « Française »), date en trois sélecteurs JJ/MM/AAAA avec âge calculé (`CharacterValidator.age`), bouton « Supprimer un nom » absent, sexe rappelé en lecture seule. **`RecapTab`** : liste des sections avec « OK », case « Je confirme que mon nom respecte le règlement RP (pas de célébrité, pas de jeu de mots). », puis deux boutons « Choisir mon skin » (customSkin = vrai) et « Garder mon skin Minecraft » (customSkin = faux) qui envoient `CreateCharacterPacket` ; boutons désactivés tant que la case est décochée ou que l'identité est invalide (message d'erreur d'après `CharacterValidator`).
- [ ] **Step 7: Run** `./gradlew test build` — Expected: PASS.
- [ ] **Step 8: Vérification manuelle** (`runClient`) : parcourir tous les onglets, tourner l'aperçu, annuler/rétablir, aléatoire, valider ; Expected: écran stable en 854×480 et en plein écran.
- [ ] **Step 9: Commit** `feat: écran de création de personnage avec aperçu 3D`.

---

### Task 9: Carte d'identité, API d'apparence et éditeur par section

**Files:**
- Modify: `client/IdentityScreen.java` (carte : champ « 6. SEXE », tête avec `ClientSkins.skinOf` sinon skin Mojang), `network/ModNetwork.java` (paquets `OpenEditorPacket` S→C, `EditorSavePacket` C→S), `character/CharacterManager.java` (sessions d'édition), `IdentityProvider.java`, `minenorth-api` (`AppearanceService`)
- Create: `character/EditSessions.java`
- Test: `character/EditSessionsTest.java`

**Interfaces:**
- Produces: `EditSessions` (pur) : `open(UUID, Set<AppearanceSection>, long untilMillis)`, `Set<AppearanceSection> allowed(UUID, long nowMillis)` (vide si expirée), `close(UUID)` ; `CharacterManager.applyEdit(ServerPlayer, Map<AppearanceSection,int[]>)` (refuse tout ce qui n'est pas dans `allowed` ou invalide pour le sexe de l'identité, accepte rien d'autre, diffuse la fiche) ; `minenorth-api` : `interface AppearanceService { Optional<java.util.Map<String,int[]>> get(MinecraftServer, UUID); boolean set(MinecraftServer, UUID, String section, int[] values); boolean openEditor(MinecraftServer, UUID, java.util.Set<String> sections); }` — `set` renvoie faux pour une section inconnue, des valeurs invalides ou si un sexe serait modifié ; le fournisseur est publié comme `IdentityService` dans `MineNorthIdentite`.

- [ ] **Step 1: Tests `EditSessionsTest`** : `onlyOpenedSectionsAllowed` ; `expiredSessionAllowsNothing` ; `closeRemovesSession` ; `sessionsAreIndependentPerPlayer`.
- [ ] **Step 2: Run** — Expected: FAIL.
- [ ] **Step 3: Implémenter** `EditSessions`, `applyEdit` (rejet total si une clé n'est pas autorisée), les deux paquets, le fournisseur `AppearanceService` côté identité, et l'interface côté API (reconstruire le jar).
- [ ] **Step 4: Éditeur** : `OpenEditorPacket(Set<AppearanceSection> allowed, Gender gender, Appearance current)` ouvre `CreatorScreen` en `CreatorMode.editor`, seuls les onglets autorisés sont actifs, Identité et sexe en lecture seule, bouton « Valider » envoie `EditorSavePacket`.
- [ ] **Step 5: Carte** : ajouter le champ « 6. SEXE » (Homme/Femme) dans `renderCard` ; ajuster la mise en page des champs sans déborder du gabarit 280×138 ; ancienne carte sans sexe → champ vide.
- [ ] **Step 6: Run** `./gradlew test build` — Expected: PASS.
- [ ] **Step 7: Vérification manuelle** : lire la carte (sexe visible, tête du personnage) ; depuis un client de test, appeler `openEditor(..., Set.of("HAIR"))` et vérifier que seul l'onglet Cheveux est actif et que le sexe ne change pas.
- [ ] **Step 8: Commit** `feat: sexe sur la carte, API d'apparence et éditeur par section`.

---

### Task 10: Reset complet, documentation et recette

**Files:**
- Modify: `command/IdentityCommands.java` (messages), `README.md`, `src/main/resources/assets/minenorthidentite/lang/fr_fr.json` et `en_us.json` (identiques, textes en français)
- Test: aucune nouvelle logique ; recette manuelle

- [ ] **Step 1: README** : décrire création obligatoire, sexe, apparence, `/cidreset` (efface le personnage, relance la création), API d'apparence, migration (les anciens joueurs refont tout).
- [ ] **Step 2: Lang** : vérifier que `en_us.json` = `fr_fr.json`.
- [ ] **Step 3: Recette** sur `runClient` + `runServer` à deux clients : (1) nouveau joueur → création forcée, gelé, ne peut pas fermer, chat/commandes bloqués ; (2) création complète → carte avec sexe, nom RP au chat et au TAB, skin visible par le second client ; (3) « Garder mon skin Minecraft » → skin Mojang conservé ; (4) déconnexion en pleine création → recommence ; (5) `/cidreset` en ligne → écran rouvert, skin retiré chez l'autre client ; `/cidreset` hors ligne → création à la prochaine connexion ; (6) ancien monde avec identité sans sexe → création complète, ancienne carte « n'est plus valide » ; (7) `/cidmenu` fonctionne toujours pour le staff.
- [ ] **Step 4: Run** `./gradlew test build` — Expected: PASS, jar produit.
- [ ] **Step 5: Commit** `docs: README, textes et recette de la création de personnage`.

---

## Self-review

- **Couverture de la spec** : sexe sur carte (T1, T9) ; création forcée (T5, T8) ; éditeur à onglets et aperçu 3D (T8) ; choix de skin (T8, T7) ; reset complet (T5, T10) ; textures générées (T4) ; apparence modifiable par API et sexe figé (T3, T9) ; migration A (T1, T5) ; protocole 3 (T6) ; gel (T5) ; aucun upload d'image (T3, T6). 
- **Cohérence des types** : `Gender`, `Appearance`, `AppearanceSection`, `Catalog.validate`, `CharacterManager.CreateRequest`, `SheetPacket` utilisés avec les mêmes noms partout.
- **Risque connu** : les noms exacts des champs de `PlayerInfo` pour le transformateur d'accès (T7) et le chemin `flatDir` du worktree (T1) doivent être vérifiés par l'implémenteur.
