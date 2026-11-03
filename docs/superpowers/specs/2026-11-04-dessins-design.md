# Dessins du personnage (visage, barbe, tatouages) — design

Complète `2026-11-03-creation-personnage-design.md`. Mod `minenorth-identite`.

## Intention

Le joueur peut dessiner lui-même son visage, sa barbe et ses tatouages, ou reprendre la tête de son skin Minecraft,
avec une **palette de couleurs limitée** (décision de l'utilisateur : plus propre, et moins d'abus).

## Décisions

- **Un dessin** = une grille de numéros de couleur (0 = transparent, 1 à N = couleur de la palette du dessin). Taille fixe.
- **Types de dessin** (`DrawingKind`) :
  - `FACE` : 8×8, devant du visage. Palette « visage » (teintes de peau, ombres, yeux, lèvres, noir, blanc).
  - `BEARD` : 8×8, devant du visage. Palette « cheveux » (les 12 couleurs de cheveux).
  - `TATTOO_TORSO` 8×12, `TATTOO_BACK` 8×12, `TATTOO_RIGHT_ARM` 4×12, `TATTOO_LEFT_ARM` 4×12, `TATTOO_LEGS` 4×12 :
    devant de la zone (jambes : les deux). Palette « encres » (16 couleurs).
- Un dessin se pose **par-dessus** ce que le catalogue a déjà dessiné (visage, pilosité, tatouages) : seules les cases
  non transparentes remplacent ou teintent le skin. Les tatouages se mélangent à la peau (comme ceux du catalogue).
- **Import de la tête Minecraft** : le client lit le visage (8×8, plus la couche de chapeau) du skin Mojang du joueur, le
  ramène à la couleur la plus proche de la palette « visage » et le met dans le dessin `FACE`.
- **Stockage** : les dessins font partie de la fiche d'apparence (`Appearance.drawings()`), donc enregistrés, envoyés aux
  clients et modifiables par section par les autres mods, comme le reste. Tailles et numéros de couleur sont vérifiés à
  la lecture (valeur hors palette = fiche refusée) : le serveur ne reçoit jamais de couleur libre.
- **Modération** : le staff garde `/cidreset`. Aucune couleur hors palette n'est possible.
- Un dessin appartient à une section (`FACE`, `FACIAL_HAIR`, `TATTOOS`) : l'éditeur ouvert par un autre mod sur cette
  section permet de le modifier, pas les autres.
- Barbes et moustaches : en plus des dessins libres, les 11 styles du catalogue gagnent du détail (poils irréguliers,
  favoris, mâchoire).

## Hors périmètre

- Pas de couleur libre, pas d'image importée à part la tête du skin Minecraft.
- Les parties intimes ne sont pas dessinées (refusé par le concepteur).
