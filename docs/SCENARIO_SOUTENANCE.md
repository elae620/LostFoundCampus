# Scénario d'exécution et questions de cours

Dossiers abrégés dans ce document :

- **J** = `app/src/main/java/com/example/lostfoundcampus/`
- **R** = `app/src/main/res/`
- **SQL** = `supabase/lostfound_schema.sql`

## 1. Scénario pas à pas

### Étape 1 : lancer l'application

**À l'écran :** toucher l'icône. La connexion s'affiche, ou l'accueil si une session existe.

| Fichier | Rôle |
|---|---|
| `app/src/main/AndroidManifest.xml` | `intent-filter` MAIN + LAUNCHER : désigne `MainActivity` |
| J `LostFoundApp.java` | S'exécute en premier, initialise Supabase |
| J `MainActivity.java` | `onCreate()` charge `activity_main` |
| R `layout/activity_main.xml` | Zone des fragments + barre basse |
| R `navigation/nav_graph.xml` | Premier écran : `loginFragment` |
| J `data/remote/SessionStore.java` | Cherche une session enregistrée |

**Cours :** *Layout* : ConstraintLayout. *Intent* : le lanceur Android démarre l'activité. *View* : FragmentContainerView, BottomNavigationView.

**À dire :** « Nous avons une seule activité. Les écrans sont des fragments. »

### Étape 2 : se connecter

**À l'écran :** saisir l'e-mail et le mot de passe, puis Se connecter.

| Fichier | Rôle |
|---|---|
| R `layout/fragment_login.xml` | NestedScrollView contenant des LinearLayout, champs et boutons |
| J `ui/auth/LoginFragment.java` | `attemptLogin()` lit et vérifie les champs |
| J `util/Validators.java` | Vérifie `@universite.tn` |
| J `data/AuthRepository.java` | `signIn()` |
| J `data/remote/AuthApi.java` | `POST token?grant_type=password` |
| J `data/remote/SessionStore.java` | `save()` garde les jetons |
| J `MainActivity.java` | `showMain()` affiche l'accueil |

**BDD :** `auth.users` (gérée par Supabase). À l'**inscription**, le trigger `on_auth_user_created` crée la ligne de `profiles`.

**Cours :** *View* : TextInputEditText, MaterialButton, `setOnClickListener`.

### Étape 3 : consulter l'accueil

**À l'écran :** défiler, choisir Perdus, filtrer Téléphones, rechercher.

| Fichier | Rôle |
|---|---|
| R `layout/fragment_home.xml` | CoordinatorLayout, AppBarLayout, RecyclerView |
| R `layout/item_announcement.xml` | Une carte : MaterialCardView + ConstraintLayout |
| J `ui/home/HomeFragment.java` | Filtres, recherche, pagination |
| J `ui/home/AnnouncementAdapter.java` | Remplit chaque carte |
| J `ui/common/ImageLoader.java` | Photos avec Glide |
| J `data/AnnouncementRepository.java` | `feed()` construit la requête |
| J `data/remote/RestApi.java` | `GET announcements` |
| J `data/Mappers.java` | JSON (DTO) vers modèle |

**BDD :** lecture de `announcements`, politique `announcements_select`, index `announcements_feed_idx`.

Équivalent SQL **simplifié** :

```sql
select * from announcements
where status = 'ACTIVE' and type = 'LOST' and category = 'PHONES'
order by created_at desc, id desc limit 20 offset 0;
```

**Cours :** *View* : RecyclerView, TabLayout, ChipGroup.

### Étape 4 : ouvrir le détail

**À l'écran :** toucher une annonce, faire glisser les photos.

| Fichier | Rôle |
|---|---|
| J `ui/home/HomeFragment.java` | `openDetail()` transmet l'identifiant |
| J `ui/common/Nav.java` | `go()` lance la navigation |
| J `ui/detail/AnnouncementDetailFragment.java` | Lit l'identifiant, `load()`, `bind()` |
| J `ui/detail/PhotoPagerAdapter.java` | Galerie |
| R `layout/fragment_announcement_detail.xml` | ConstraintLayout, NestedScrollView, ViewPager2 |
| R `layout/view_info_row.xml`, `view_mini_map.xml` | Layouts réutilisés avec `<include>` |

**BDD :** `announcements` et `profiles`.

**Cours :** **question piège.** Ce passage d'écran n'utilise **pas d'Intent**, mais le Navigation Component et un `Bundle` :

```java
Bundle args = new Bundle();
args.putString("announcement_id", announcementId);
```

### Étape 5 : prendre ou choisir une photo

**À l'écran :** Publier > Ajouter une photo > appareil photo ou galerie.

| Fichier | Rôle |
|---|---|
| R `layout/fragment_publish.xml` | Formulaire |
| J `ui/publish/PublishFragment.java` | `launchCamera()`, `launchGallery()` |
| J `ui/publish/CameraFiles.java` | Crée le fichier et son URI |
| `AndroidManifest.xml` | Déclare le `FileProvider` |
| R `xml/file_paths.xml` | Dossier partagé |
| J `ui/publish/PhotoThumbAdapter.java` | Miniatures horizontales |

**Cours :** *Intents implicites*, l'étape la plus importante pour ce thème (partie 5).

### Étape 6 : cadrer et analyser

| Fichier | Rôle |
|---|---|
| R `layout/fragment_photo_preview.xml`, `fragment_crop.xml`, `fragment_ai_analysis.xml` | FrameLayout et LinearLayout |
| J `ui/publish/CropFragment.java`, `CropView.java` | Cadre, **vue personnalisée** |
| J `ui/publish/AiAnalysisFragment.java`, `AiScanView.java` | Animation, **vue personnalisée** |
| J `ui/publish/PhotoAnalyzer.java` | `analyze()` |
| J `ui/publish/ObjectVision.java`, `VisionLabels.java` | Modèles MediaPipe |
| J `ui/publish/ObjectClassifier.java` | Choix de la catégorie |
| J `ui/publish/ColorMatcher.java` | Couleurs |
| J `ui/publish/PublishViewModel.java` | Garde le brouillon |

**Cours :** *View* : `CropView` et `AiScanView` héritent de `View` et redéfinissent `onDraw()`. `CropView` redéfinit aussi `onTouchEvent()`.

### Étape 7 : remplir et publier

| Fichier | Rôle |
|---|---|
| J `ui/publish/PublishFragment.java` | `setUpFields()` (catégories), `openDatePicker()`, `submit()` |
| J `ui/map/LocationPickerFragment.java`, `LocationHelper.java` | Carte et GPS |
| R `layout/fragment_location_picker.xml` | Carte |
| J `ui/publish/AnnouncementPublisher.java` | `uploadPhoto()`, `saveRow()`, `saveAnalyses()` |
| J `util/PhotoCompressor.java` | Compression |
| J `data/StorageRepository.java`, `data/remote/StorageApi.java` | Envoi des photos |
| J `data/remote/RestApi.java` | `POST announcements` |

**BDD :** bucket `announcement-photos`, puis `announcements`, puis `photo_analyses`. Contrôles : contraintes `check`, trigger `announcements_guard`, politique `announcements_insert_own`.

**Cours :** *View* : MaterialAutoCompleteTextView (à la place d'un Spinner), MaterialDatePicker, Snackbar.

### Étape 8 : voir les correspondances

**À l'écran :** avec Amine : Profil > iPhone 13 mini bleu > Voir les correspondances.

| Fichier | Rôle |
|---|---|
| J `ui/profile/ProfileFragment.java`, `MyAnnouncementAdapter.java` | Mes annonces |
| J `ui/matches/MatchesFragment.java`, `MatchAdapter.java` | Résultats |
| R `layout/fragment_matches.xml`, `item_match.xml` | Interface, barre de score |
| J `data/AnnouncementRepository.java` | `matches()` puis `byIds()` |
| SQL `match_candidates` | Calcul sur 100 |

**BDD :** appel RPC d'une fonction SQL.

### Étape 9 : contacter

| Fichier | Rôle |
|---|---|
| J `ui/messages/ChatFragment.java` | `openWithAuthor()`, `send()`, `poll()` |
| J `ui/messages/MessageAdapter.java` | Bulles envoyées et reçues |
| J `ui/messages/IntroMessageTemplate.java` | Brouillon |
| J `util/Poller.java` | Interrogation toutes les 4 s |
| J `data/MessageRepository.java` | Requêtes |
| R `layout/fragment_chat.xml`, `item_message_sent.xml`, `item_message_received.xml` | Interface |
| SQL `open_conversation`, `mark_conversation_read` | Fonctions |

**BDD :** `conversations`, `messages`, trigger `messages_after_insert`.

### Étape 10 : marquer restitué et se déconnecter

| Fichier | Rôle |
|---|---|
| J `ui/profile/ProfileFragment.java` | `confirmReturned()`, `confirmLogout()` |
| J `data/AnnouncementRepository.java` | `markReturned()` : `PATCH`, statut RETURNED |
| J `data/AuthRepository.java` | `signOut()` |
| J `MainActivity.java` | `showAuth()` |

**BDD :** politique `announcements_update_own`. Une annonce rendue sort du fil, qui demande `status = ACTIVE`.

> Ne valide pas la restitution de l'iPhone avant la fin : il disparaîtrait de la démo des correspondances.

## 2. Thème : les layouts

Un **layout** (`ViewGroup`) est un conteneur qui organise des vues.

| Layout | Principe | Exemple dans le projet |
|---|---|---|
| `ConstraintLayout` | Positions par contraintes | `activity_main`, `fragment_chat`, `item_announcement` |
| `LinearLayout` | En ligne ou en colonne ; partage avec `layout_weight` | `fragment_login`, `fragment_messages` |
| `FrameLayout` | Superposition | `fragment_ai_analysis` (photo et animation) |
| `CoordinatorLayout` | Coordonne les comportements au défilement | `fragment_home` |
| `NestedScrollView` | Défilement vertical, **un seul** enfant direct | `fragment_profile` |
| `HorizontalScrollView` | Défilement horizontal | Filtres de `fragment_home` |
| `MaterialCardView` | Carte arrondie (hérite de FrameLayout) | `item_announcement` |

**Non utilisés :** RelativeLayout, TableLayout et GridLayout.

| Question | Réponse |
|---|---|
| `match_parent` / `wrap_content` / `0dp` ? | Taille du parent / du contenu / définie par les contraintes (ou le poids dans un LinearLayout) |
| `padding` / `margin` ? | Espace intérieur / extérieur |
| `dp` / `sp` ? | Dimensions indépendantes de la densité / textes, suit la taille choisie par l'utilisateur |
| `gone` / `invisible` ? | Cachée sans occuper de place / cachée en gardant sa place |
| `gravity` / `layout_gravity` ? | Position du contenu dans la vue / de la vue dans son parent |
| Pourquoi ConstraintLayout ? | Moins d'imbrication, s'adapte aux écrans |
| À quoi sert `<include>` ? | Réutiliser un layout : `layout_screen_header`, `view_empty_state` |
| Comment le XML devient une vue ? | Par l'inflation : `FragmentLoginBinding.inflate(...)` |

## 3. Thème : les views

Une **View** est un élément visible. Un **ViewGroup** est une View qui contient d'autres views.

| View | Utilisation |
|---|---|
| `TextView` | Titres, messages |
| `TextInputLayout` + `TextInputEditText` | Saisie, erreur, aide |
| `MaterialButton` | Boutons |
| `ImageView`, `ShapeableImageView` | Photos |
| `Chip` / `ChipGroup` | Filtres, couleurs |
| `RecyclerView` | **6 listes** |
| `ViewPager2` | Galerie |
| `TabLayout`, `BottomNavigationView` | Navigation |
| `MaterialAutoCompleteTextView` | Liste déroulante des catégories |
| `ExtendedFloatingActionButton` | Publier une annonce |
| `CropView`, `AiScanView` | Vues personnalisées |

**ListView et Spinner : non utilisés.**

| Question | Réponse |
|---|---|
| Comment accédez-vous aux vues ? | ViewBinding : `b.btnLogin`. Seule exception : un `findViewById` dans `HomeFragment` pour retrouver la puce cochée |
| Pourquoi `b = null` dans `onDestroyView` ? | La vue du fragment est détruite ; évite une fuite mémoire |
| Comment gérez-vous un clic ? | `b.btnLogin.setOnClickListener(v -> attemptLogin());` |
| Rôle de l'adaptateur ? | Relie les données aux vues de la liste |
| Rôle du ViewHolder ? | Garde les vues d'un élément pour les réutiliser |
| RecyclerView ou ListView ? | Recyclage imposé, dispositions variées, mises à jour ciblées |
| Pourquoi pas Spinner ? | La liste Material s'intègre au formulaire (libellé, erreur) |
| Deux types de messages ? | `getItemViewType()` dans `MessageAdapter` |
| Comment créer une vue personnalisée ? | Hériter de `View` et redéfinir `onDraw()` |

## 4. Thème : la base de données

**Supabase = PostgreSQL en ligne.** Pas de SQLite, Room ni Firebase.

| Opération | HTTP | Exemple |
|---|---|---|
| Create | `POST` | Publier (`insertAnnouncement`) |
| Read | `GET` | Accueil (`announcements`) |
| Update | `PATCH` | Modifier, restituer (`updateAnnouncement`) |
| Delete | `DELETE` | Analyses remplacées, photos du Storage |
| Fonction SQL | `POST rpc/...` | Correspondances, conversations |

> L'app ne propose **pas** de supprimer une annonce : elle la marque restituée.

| Fichier | Rôle |
|---|---|
| `local.properties` | URL et clé publique |
| `app/build.gradle.kts` | Les met dans `BuildConfig` |
| J `data/remote/SupabaseClient.java` | Connexion |
| J `data/remote/RestApi.java` | Routes |
| J `data/*Repository.java` | Opérations |
| J `data/remote/dto/` | Format JSON |
| SQL | Tables, triggers, fonctions, sécurité |

Tables : `profiles`, `announcements`, `photo_analyses`, `conversations`, `messages`, plus `auth.users` gérée par Supabase.

| Question | Réponse |
|---|---|
| Clé primaire ? | Identifie une ligne : `announcements.id` (UUID) |
| Clé étrangère ? | Relie deux tables : `announcements.author_id` vers `profiles.id` |
| Relation profil / annonce ? | Un profil a plusieurs annonces (1 à n) |
| Pourquoi pas SQLite ? | Les données doivent être partagées entre téléphones |
| Qu'est-ce que la RLS ? | Règles par ligne : seul l'auteur modifie son annonce |
| Qu'est-ce qu'un trigger ? | Code exécuté automatiquement : création du profil |
| À quoi sert un index ? | Accélérer les recherches |
| Comment éviter l'injection SQL ? | L'app n'écrit pas de SQL ; l'API reçoit des filtres, et `sanitize()` retire les caractères spéciaux de la recherche |
| Que reste-t-il sur le téléphone ? | Les jetons en SharedPreferences et des caches (images, carte) |
| Et sans Internet ? | Les requêtes échouent avec un message |

## 5. Thème : les intents

| | Explicite | Implicite |
|---|---|---|
| On précise | La **classe** à ouvrir | L'**action** à faire |
| Qui répond | Le composant nommé | Une application capable de le faire |
| Exemple | `new Intent(this, DetailActivity.class)` | Prendre une photo |

### Intents explicites dans notre projet

**Aucun Intent explicite n'est écrit dans notre code.** Il n'y a qu'une activité ; les écrans sont des fragments ouverts par le Navigation Component.

| Avec plusieurs activités | Notre équivalent |
|---|---|
| `new Intent(A.this, B.class)` | `Nav.go(this, R.id.announcementDetailFragment, args)` |
| `putExtra("id", id)` | `args.putString(...)` dans un `Bundle` |
| `getIntent().getStringExtra("id")` | `getArguments().getString(...)` |
| `finish()` | `Nav.back(this)` |
| `startActivityForResult` (obsolète) | `registerForActivityResult`, ou `SavedStateHandle` entre fragments |

Si on demande de l'écrire (exemple de cours, **absent du projet**) :

```java
Intent intent = new Intent(this, DetailActivity.class);
intent.putExtra("announcement_id", id);
startActivity(intent);
```

### Intents implicites dans notre projet

Nous n'écrivons pas `new Intent(...)` : les **Activity Result Contracts** d'AndroidX créent l'intent.

| Action | Fichier | Contrat | Intent créé |
|---|---|---|---|
| Prendre une photo | `PublishFragment`, `PhotoPreviewFragment` | `TakePicture` | `ACTION_IMAGE_CAPTURE` + URI de sortie |
| Choisir plusieurs photos | `PublishFragment` | `PickMultipleVisualMedia` | Sélecteur de photos Android, ou sélecteur de documents sinon |
| Choisir un avatar | `EditProfileFragment` | `PickVisualMedia` | Idem |

Les permissions (`RequestMultiplePermissions`) passent par le même mécanisme, mais ce n'est pas un bon exemple d'intent implicite.

Extrait du projet :

```java
private final ActivityResultLauncher<Uri> takePicture =
        registerForActivityResult(new ActivityResultContracts.TakePicture(), this::onPhotoTaken);
```

```java
vm.cameraTarget = CameraFiles.newTarget(requireContext());
takePicture.launch(vm.cameraTarget);
```

Forme classique équivalente (exemple de cours) :

```java
Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
startActivity(intent);
```

### Intent-filter

Dans `AndroidManifest.xml` :

```xml
<intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.LAUNCHER" />
</intent-filter>
```

`MAIN` : point d'entrée. `LAUNCHER` : icône dans le lanceur.

| Question | Réponse |
|---|---|
| Qu'est-ce qu'un Intent ? | Un message qui demande une action à un composant Android |
| Pourquoi aucun intent explicite ? | Une seule activité ; navigation entre fragments |
| Comment passez-vous des données ? | Un `Bundle` avec l'identifiant ; le détail relit la base |
| Pourquoi FileProvider ? | Partager un fichier par une URI `content://` ; une URI `file://` est refusée depuis Android 7 |
| Si aucune app photo n'existe ? | `ActivityNotFoundException` est capturée et un message s'affiche |
| Comment récupérez-vous la photo ? | `onPhotoTaken(Boolean saved)` ; l'image est dans l'URI préparée |
| Faut-il la permission caméra ? | Non : c'est l'application photo qui prend la photo, et nous ne déclarons pas `CAMERA` |

## 6. Cycle de vie (souvent lié)

| Méthode | Utilisation |
|---|---|
| `onCreate` | Préparer ViewModel, Poller |
| `onCreateView` | Créer la vue |
| `onViewCreated` | Clics, adaptateurs |
| `onResume` | Recharger, démarrer le polling |
| `onPause` | Arrêter le polling et la carte |
| `onSaveInstanceState` | Garder l'onglet, la catégorie |
| `onDestroyView` | Libérer la vue |

## 7. Fiche à relire juste avant

1. **1 activité, 15 fragments.**
2. **6 RecyclerView**, 0 ListView, 0 Spinner.
3. **ConstraintLayout, LinearLayout, FrameLayout, CoordinatorLayout, NestedScrollView.**
4. **PostgreSQL via Supabase**, 5 tables, RLS, Retrofit.
5. **Intents explicites : aucun dans le code.**
6. **Intents implicites : caméra et photos, via Activity Result Contracts.**
7. **Données entre écrans : Bundle.**
8. **ViewBinding** (un seul `findViewById`, dans `HomeFragment`).
