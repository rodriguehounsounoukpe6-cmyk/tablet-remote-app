# Application de telecommande directe (sans ADB, sans cable)

Cette version installe une vraie application sur la tablette. Elle recoit les
commandes directement par le reseau WiFi local — plus besoin d'ADB, d'USB ou
d'ordinateur.

**Rappel important** : ceci resout le probleme "ADB/USB". Le probleme de reseau
(isolation eventuelle du WiFi de la maison) reste le meme — il faudra toujours
que telephone et tablette communiquent sur un reseau qui ne les isole pas
(hotspot du telephone, ou un petit routeur neutre).

---

## Etape 1 — Creer un compte GitHub (si tu n'en as pas)

Va sur https://github.com/join depuis le navigateur du telephone, cree un compte gratuit.

## Etape 2 — Creer un nouveau depot (repository)

1. Une fois connecte, tape sur le **+** en haut a droite → **New repository**
2. Nom : `tablet-remote-app`
3. Laisse "Public", ne coche aucune case additionnelle
4. Cree le depot

## Etape 3 — Creer un token d'acces (pour pouvoir envoyer le code depuis Termux)

1. Va sur https://github.com/settings/tokens
2. **Generate new token** → **Generate new token (classic)**
3. Nom : `termux`, duree : 90 jours (ou plus)
4. Coche la case **repo**
5. Genere le token, **copie-le immediatement** (il ne sera plus jamais affiche) —
   colle-le temporairement dans une note

## Etape 4 — Envoyer le code depuis Termux

Dans Termux :

```bash
pkg install git -y
cd ~
cp -r ~/storage/downloads/tablet-remote-app ~/tablet-remote-app 2>/dev/null
cd ~/tablet-remote-app
git init
git add .
git commit -m "premiere version"
git branch -M main
git remote add origin https://TON_NOM_UTILISATEUR:TON_TOKEN@github.com/TON_NOM_UTILISATEUR/tablet-remote-app.git
git push -u origin main
```

Remplace `TON_NOM_UTILISATEUR` et `TON_TOKEN` par tes vraies valeurs.

(Il faudra d'abord télécharger tous les fichiers du projet depuis ce chat vers
`~/storage/downloads/tablet-remote-app/`, en respectant les memes sous-dossiers
que ceux que je t'ai donnes : `app/`, `.github/workflows/`, etc.)

## Etape 5 — Attendre la compilation automatique

1. Va sur `https://github.com/TON_NOM_UTILISATEUR/tablet-remote-app/actions`
2. Tu verras un workflow "Build APK" qui tourne (cercle orange qui tourne)
3. Attends qu'il devienne vert (coche) — ca prend 3-5 minutes

## Etape 6 — Telecharger l'APK

1. Tape sur le workflow termine (coche verte)
2. En bas de la page, section **Artifacts** → tape sur `telecommande-tablette-apk`
3. Ca telecharge un fichier `.zip` contenant `app-debug.apk`
4. Dezippe-le (gestionnaire de fichiers Android, ou dans Termux : `unzip fichier.zip`)

## Etape 7 — Installer l'APK sur la TABLETTE

1. Transfere le fichier `app-debug.apk` sur la tablette (Bluetooth, cle USB, WhatsApp a toi-meme, etc.)
2. Sur la tablette, ouvre le fichier → autorise "installer des applis inconnues" si demande
3. Installe l'application "Telecommande Tablette"

## Etape 8 — Activer le service d'accessibilite sur la tablette

1. Ouvre l'app "Telecommande Tablette" sur la tablette
2. Note l'adresse IP affichee (ex: `192.168.88.64`)
3. Appuie sur **"Activer le service d'accessibilite"**
4. Dans les parametres qui s'ouvrent, trouve "Telecommande Tablette" dans la liste, active-le
5. Accepte l'avertissement de securite (normal, c'est le fonctionnement standard de cette API Android)
6. Reviens dans l'app : le statut doit passer a "ACTIF"

## Etape 9 — Sur le telephone : utiliser la nouvelle interface

Dans Termux :
```bash
cd ~/remote-tablet
python server_v2.py
```

Ouvre `http://localhost:8000` dans le navigateur du telephone, mets l'IP de
la tablette (etape 8), clique **Tester la connexion**.

Si le point passe vert, les boutons sont operationnels.

---

## Limitations honnetes

- Les fleches directionnelles simulent des glissements (swipe) plutot qu'une
  vraie navigation D-pad — les tablettes tactiles n'ont pas d'equivalent
  officiel a la navigation flèches d'une télécommande TV. C'est une
  approximation qui peut bien fonctionner pour parcourir des photos, une
  liste, une page, mais ne "naviguera" pas toujours comme un vrai bouton.
- Le probleme d'isolation reseau (rencontre avec le WiFi de la maison) n'est
  pas resolu par cette app — utilise le hotspot du telephone (en le laissant
  actif) comme reseau de liaison.
