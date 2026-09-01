# W2Full — Project Specification

> **Fonte di verità del progetto.** `PROJECT_SPEC.md` va aggiornato **prima di ogni modifica al codice o agli asset di progetto**. Flusso: spec → implementazione/asset e test pertinenti → verifica reale → chiusura spec → cleanup branch temporanei.

## 1. Obiettivo del progetto

W2Full è un'app Android nativa, gratuita e locale per gestire rifornimenti e consumi di un singolo veicolo e consultare i prezzi carburante delle stazioni vicine usando gli open data ufficiali MIMIT.

Obiettivi: registro rifornimenti; consumo medio, costo/km e autonomia residua; import dati MIMIT; stazioni vicine con focus iniziale Eni; storico prezzi; soglie/notifiche; esportazione CSV.

## 2. Vincoli

- 100% gratuita, nessuna carta di credito o servizio a pagamento.
- Nessun account, backend o cloud per i dati utente.
- Distribuzione solo APK / GitHub Releases, non Play Store.
- Kotlin + Jetpack Compose + Material 3, dark mode.
- `applicationId` e namespace: `com.archimede.w2full`.
- Target principale Samsung Galaxy S25.
- SDK M2: `minSdk 26`, `targetSdk 37`, `compileSdk 37`.
- `minSdk 26` mantiene compatibilità da Android 8.0 senza introdurre supporto legacy non necessario per le API previste; il Galaxy S25 resta ampiamente coperto.
- `targetSdk 37` adotta i comportamenti correnti di Android 17; `compileSdk 37` è richiesto dalle release Compose 1.12.x correnti.
- Toolchain M2: Android Gradle Plugin 9.3.0, Gradle 9.5.0, JDK 17, Kotlin/Compose compiler 2.3.21, Compose BOM 2026.08.00.
- Un solo veicolo in V1, modello estendibile.
- Filtro iniziale Eni, modello impianto generico.
- Rete solo per CSV MIMIT via OkHttp.
- Room locale; WorkManager per sync; FusedLocationProviderClient + Haversine; NotificationCompat.
- Debug APK aggiornabile sopra il precedente tramite keystore persistente conservato come secret, mai nel repository.
- Quando esiste codice Android, la CI deve produrre un APK reale.

## 3. Architettura

- UI: Jetpack Compose + Material 3.
- Pattern: MVVM + Repository, stato con StateFlow.
- Persistenza: Room.
- Rete: OkHttp per CSV MIMIT.
- Sync: WorkManager giornaliero + refresh manuale.
- Posizione: FusedLocationProviderClient + Haversine.
- Notifiche: WorkManager + NotificationCompat.
- Grafici: libreria Compose-native gratuita e leggera; candidato Vico da verificare in M5.
- Test: JUnit su consumo medio, costo/km, autonomia e casi limite.

## 4. Modello dati

### Veicolo
`id`, nome, carburante predefinito, capacità serbatoio, riferimento odometrico e metadati futuri. V1: un solo veicolo attivo.

### Rifornimento
`id`, `veicoloId`, data, km attuali, litri, costo totale, tipo carburante. Estensioni future: prezzo/litro, pieno, note, impianto.

### Impianto
ID MIMIT, gestore, bandiera, tipo, nome, indirizzo, comune, provincia, latitudine, longitudine e metadati import. Eni è solo un filtro iniziale.

### StoricoPrezzo
Impianto, carburante, prezzo, self/servito, data comunicazione MIMIT, data estrazione/import; deduplicazione logica obbligatoria.

### SogliaAvviso
Carburante, prezzo massimo, modalità servizio, brand opzionale, eventuale raggio, stato attivo e metadati anti-spam.

## 5. Funzionalità

Legenda: `[ ]` da fare · `[~]` in corso · `[x]` fatto.

### Fondazioni
- [x] Repo pubblica e documentazione M0.
- [~] Scheletro Android Compose.
- [~] CI con test/build e APK reale.
- [~] Firma debug persistente da secrets.
- [ ] GitHub Releases per APK debug.

### Design
- [x] Direzione visiva approvata: **Petrol Night** come tema default dei mockup.
- [x] Concept icona approvato: gauge con lancetta arancione e identità W2Full.
- [x] Sorgente icona finale raster PNG 1024×1024 in `design/final/icon-source.png`, senza wordmark esterno.
- [x] Lockup raster approvato in `design/final/logo-lockup.png`, derivato dall'immagine originale approvata e non ridisegnato con font di sistema.
- [x] Spostamento dei 3 concept logo scartati in `design/archive/`.
- [x] Home: 2 varianti statiche.
- [x] Registro rifornimenti: 2 varianti statiche.
- [x] Storico prezzi: 2 varianti statiche.

### Registro e calcoli
- [ ] CRUD rifornimenti + Room.
- [ ] Consumo medio, costo/km, autonomia residua.
- [ ] JUnit e casi limite.

### Dati MIMIT
- [ ] Download/parsing/import.
- [ ] Filtro Eni, posizione, Haversine, stazioni vicine.
- [ ] Refresh manuale + WorkManager giornaliero.

### Storico/notifiche/rifiniture
- [ ] Storico prezzi + grafico.
- [ ] Soglia prezzo + notifiche anti-spam.
- [ ] Esportazione CSV, impostazioni, tema, errori/empty states.

## 6. Roadmap a milestone

### M0 — Repo + spec
Stato: **[x] fatto**

### M1 — Design: logo e mockup
Stato: **[x] fatto**

Deliverable:
- [x] concept logo/icona W2Full esplorati;
- [x] scelta finale del concept gauge;
- [x] `design/final/icon-source.png` — PNG raster 1024×1024, solo icona;
- [x] `design/final/logo-lockup.png` — lockup raster approvato, derivato direttamente dall'immagine originale approvata;
- [x] 3 concept scartati archiviati in `design/archive/`;
- [x] 2 mockup statici Home in `design/mockup/`;
- [x] 2 mockup statici Registro rifornimenti in `design/mockup/`;
- [x] 2 mockup statici Storico prezzi in `design/mockup/`;
- [x] **Petrol Night** scelto come tema default dei mockup;
- [x] nessun codice app introdotto.

L'icona finale è intenzionalmente un asset **raster PNG**, non il corrispondente SVG semplificato: gradienti, riflessi, ombre e profondità 3D del concept approvato non sono replicabili fedelmente con l'SVG scritto a mano usato per le bozze iniziali.

Il lockup finale è anch'esso un asset raster derivato dall'immagine originale approvata con wordmark `W2Full`; non deve essere sostituito da testo ridisegnato con font di sistema.

In **M2**, durante lo scaffold Android, questa sorgente dovrà essere trasformata in una **Adaptive Icon** Android con foreground/background separati e safe-zone verificata, mantenendo la resa approvata.

### M2 — Scheletro Android + CI con APK installabile
Stato: **[~] in corso**

Decisioni tecniche fissate prima del codice:
- `namespace` / `applicationId`: `com.archimede.w2full`;
- `minSdk 26` (Android 8.0), compromesso tra compatibilità e assenza di workaround legacy non necessari;
- `targetSdk 37` e `compileSdk 37` per Android 17 e compatibilità con Compose 1.12;
- Android Gradle Plugin `9.3.0`, Gradle `9.5.0`, JDK `17`;
- Kotlin/Compose compiler `2.3.21` con Kotlin integrato di AGP 9;
- Compose BOM `2026.08.00`, Material 3;
- AndroidX Activity Compose `1.13.0`;
- `versionCode 1`, `versionName 0.1.0-m2` per il primo APK installabile;
- Gradle 9.5.0 viene installato in CI tramite `gradle/actions/setup-gradle@v4` con versione esplicitamente fissata; M2 non dipende da un wrapper binario committato;
- adaptive icon derivata da `design/final/icon-source.png`, preservando la sorgente M1 e usando un foreground con safe-zone e background Petrol Night;
- test JVM minimo obbligatorio in CI prima di `assembleDebug`;
- APK CI caricato come artifact GitHub Actions e verificato come file `.apk` non vuoto;
- firma debug persistente ricostruita solo nel runner da GitHub Actions Secrets, senza file di chiave nel repository.

Secrets M2:
- `W2FULL_DEBUG_KEYSTORE_BASE64` — keystore binario codificato Base64;
- `W2FULL_DEBUG_KEYSTORE_PASSWORD` — password del keystore;
- `W2FULL_DEBUG_KEY_ALIAS` — alias della chiave;
- `W2FULL_DEBUG_KEY_PASSWORD` — password della chiave.

I quattro repository secret sono stati configurati il **1 settembre 2026**. La configurazione resta da validare tramite due run GitHub Actions indipendenti: entrambi devono eseguire `Restore persistent debug keystore` con esito `success` e produrre APK con lo stesso SHA-256 del certificato di firma.

Deliverable:
- [~] scheletro Kotlin + Compose/Material 3 con tema Petrol Night;
- [~] package/applicationId `com.archimede.w2full`;
- [~] Adaptive Icon Android derivata dalla sorgente approvata;
- [~] test JVM minimo;
- [~] workflow GitHub Actions con JDK 17, Gradle 9.5.0 pinning, test, `assembleDebug`, verifica e upload APK;
- [~] firma debug persistente tramite i quattro secret definiti sopra;
- [ ] verifica reale di due workflow indipendenti riusciti con APK installabili e identico SHA-256 del certificato persistente.

### M3 — Registro rifornimenti + calcoli
Stato: **[ ] da fare**
Deliverable: Room, CRUD, singolo veicolo, consumo/costo/autonomia e JUnit.

### M4 — Integrazione dati MIMIT
Stato: **[ ] da fare**
Deliverable: OkHttp, parsing formato corrente, import impianti/prezzi, Eni, WorkManager, posizione/Haversine, stazioni vicine.

### M5 — Storico prezzi + grafico
Stato: **[ ] da fare**

### M6 — Notifiche soglia prezzo
Stato: **[ ] da fare**

### M7 — Rifiniture
Stato: **[ ] da fare**
Deliverable: CSV, impostazioni, tema, UX errori/permessi/stati vuoti.

## 7. Design

M1 produce riferimenti **statici**, non componenti Compose funzionanti.

### Tema default — Petrol Night
Background `#101418`; Surface `#182028`; Primary `#33C3A5`; Secondary `#7FD1FF`; Accent `#FFB84D`; Alert `#FF6B6B`; testo `#F5F7FA` / `#A9B4C2`.

Direzione: scura, tecnica, automotive e data-centric. **Petrol Night è la variante predefinita approvata per i mockup e fungerà da riferimento iniziale per la UI Compose in M2.**

### Variante alternativa — Road Light
Background `#F6F7F9`; Surface `#FFFFFF`; Primary `#1C6DD0`; Secondary `#25A18E`; Accent `#F59E0B`; Alert `#E45757`; testo `#1F2937` / `#6B7280`.

Direzione: chiara, leggibile, editoriale. Resta come variante/secondo riferimento, non come default.

### Tipografia
- Titoli e metriche: Roboto Bold/Medium.
- UI: Roboto Regular/Medium.
- Metriche tabellari: Roboto Mono Medium.

### Icona e lockup finali
- `design/final/icon-source.png` — sorgente ufficiale icona, **PNG raster 1024×1024**, senza wordmark esterno.
- `design/final/logo-lockup.png` — lockup raster derivato direttamente dall'immagine originale approvata con wordmark W2Full, destinato a README/materiale di presentazione/store.

La sorgente ufficiale è raster perché il livello di dettaglio approvato (gradienti, ombre, riflessi, effetti metallici/3D) è sostanzialmente superiore agli SVG flat scritti a mano; usare lo SVG semplificato come master visivo produrrebbe una resa diversa da quella approvata.

Nota M2: creare risorse **Adaptive Icon** Android separate (foreground/background), senza modificare l'identità visiva approvata e rispettando le safe-zone delle maschere adattive.

### Concept SVG
- `design/logo/w2full-logo-concept-01.svg` — gauge, bozza vettoriale semplificata del concept selezionato; non è la sorgente finale dell'icona.
- `design/archive/w2full-logo-concept-02.svg` — monogramma W2F + goccia, scartato.
- `design/archive/w2full-logo-concept-03.svg` — pompa carburante + wordmark, scartato.
- `design/archive/w2full-logo-concept-04.svg` — strada + goccia in badge circolare, scartato.

### Mockup
- `design/mockup/home-theme-petrol-night.svg` — **default**.
- `design/mockup/home-theme-road-light.svg`
- `design/mockup/rifornimenti-theme-petrol-night.svg` — **default**.
- `design/mockup/rifornimenti-theme-road-light.svg`
- `design/mockup/storico-theme-petrol-night.svg` — **default**.
- `design/mockup/storico-theme-road-light.svg`

I mockup SVG restano volutamente wireframe semplici; la qualità visuale definitiva verrà verificata nell'app Compose reale.

## 8. Fonte dati esterna

Dataset MIMIT: **Carburanti - Prezzi praticati e anagrafica degli impianti**.

Pagina: `https://www.mimit.gov.it/it/open-data/elenco-dataset/carburanti-prezzi-praticati-e-anagrafica-degli-impianti`

URL verificati al 31 agosto 2026:
- `https://www.mimit.gov.it/images/exportCSV/prezzo_alle_8.csv`
- `https://www.mimit.gov.it/images/exportCSV/anagrafica_impianti_attivi.csv`

Dal **10 febbraio 2026** il separatore per “Anagrafica alle 8” e “Prezzi alle 8” è `|`. In M4 vanno ricontrollati URL, header e formato effettivo prima del parser. Licenza dati: IODL 2.0 secondo la pagina ufficiale.

## 9. CI/CD

Da M2: GitHub Actions con checkout, JDK 17, Gradle 9.5.0 installato e pinning tramite `gradle/actions/setup-gradle@v4`, test JVM, build `assembleDebug`, verifica dell'APK e upload artifact. Non viene committato un Gradle Wrapper binario in M2: il runner usa la distribuzione Gradle fissata dalla pipeline, evitando un JAR wrapper generato o trasferito fuori dal normale flusso sorgente.

Toolchain M2: AGP 9.3.0 + Gradle 9.5.0 + `compileSdk/targetSdk 37`. Non si fissa manualmente `buildToolsVersion`: viene usata la versione predefinita compatibile con AGP.

La firma debug persistente usa un keystore generato una sola volta e conservato esclusivamente come Base64 in `W2FULL_DEBUG_KEYSTORE_BASE64`; le password e l'alias sono separati nei secret `W2FULL_DEBUG_KEYSTORE_PASSWORD`, `W2FULL_DEBUG_KEY_ALIAS`, `W2FULL_DEBUG_KEY_PASSWORD`. Il workflow ricostruisce il file sotto `$RUNNER_TEMP`, lo usa per la `signingConfig` debug e lo elimina a fine job. Nessun contenuto dei secret va stampato nei log o committato.

Dal **1 settembre 2026** i quattro secret risultano configurati dal proprietario del repository. Prima di chiudere M2 la CI deve confermare in due run indipendenti che il ripristino del keystore avviene realmente e che lo SHA-256 del certificato di firma resta identico tra i due APK.

## 10. Changelog

### 2026-09-01 — M2: secrets configurati, verifica firma avviata
- Registrata la configurazione dei quattro GitHub Actions Secrets per il keystore debug persistente.
- Avviata la verifica obbligatoria su due run indipendenti: `Restore persistent debug keystore` deve risultare `success` in entrambi.
- La chiusura M2 richiede inoltre che `apksigner` riporti lo stesso SHA-256 del certificato nei due APK.

### 2026-09-01 — M2 avviata: toolchain e firma definite
- Confermato `applicationId`/namespace `com.archimede.w2full`.
- Fissati `minSdk 26`, `targetSdk 37`, `compileSdk 37`.
- Scelti AGP 9.3.0, Gradle 9.5.0, JDK 17, Kotlin/Compose compiler 2.3.21 e Compose BOM 2026.08.00.
- Fissata AndroidX Activity Compose 1.13.0 stabile.
- Motivato `minSdk 26` come floor compatibile senza oneri legacy superflui; API 37 segue Android 17 e il requisito delle release Compose 1.12 correnti.
- Definiti i quattro GitHub Actions Secrets per il keystore debug persistente.
- Chiarito che M2 usa Gradle 9.5.0 pinning tramite `setup-gradle`, senza committare un wrapper JAR binario.
- M2 marcata in corso prima di introdurre codice Android.

### 2026-09-01 — M1 Design completata
- Spostati i master raster approvati nei percorsi finali `design/final/icon-source.png` e `design/final/logo-lockup.png`, preservando i blob originali.
- Mantenuto `design/logo/w2full-logo-concept-01.svg` come bozza vettoriale del concept gauge selezionato.
- Archiviati i concept scartati 02, 03 e 04 in `design/archive/`.
- Verificati i 6 mockup statici in `design/mockup/`, con Petrol Night come riferimento default.
- Verificato che `main` non contiene `.m1-tmp/` né `.github/workflows/m1-assemble-assets.yml`; il materiale di assemblaggio temporaneo è rimasto confinato al branch temporaneo destinato al cleanup post-chiusura.
- M1 chiusa senza introdurre codice Android; M2 resta da avviare solo dopo conferma utente.

### 2026-09-01 — M1 Design: sistemazione finale asset in corso
- Confermato `icon-source.png` come master raster PNG dell'icona approvata.
- Confermato `logo-lockup.png` come ritaglio raster dell'immagine originale approvata, senza ridisegno del wordmark.
- Previsto spostamento dei tre concept scartati in `design/archive/`.
- Previsto cleanup completo del materiale temporaneo di assemblaggio prima della chiusura M1.

### 2026-09-01 — M1 Design: aggiustamento finale avviato
- Approvato il concept gauge con lancetta arancione come icona finale.
- Approvato Petrol Night come tema default dei mockup.
- Specificato master raster PNG 1024×1024 per preservare gradienti ed effetti 3D.
- Specificato lockup separato per README/store.
- Pianificato archivio dei tre concept SVG scartati.
- Annotata la necessità di derivare Adaptive Icon foreground/background in M2.

### 2026-08-31 — M1 Design: asset pronti per review
- Committati 4 concept logo/icona SVG 1024×1024.
- Committati 6 mockup statici: Home, Registro rifornimenti e Storico prezzi in Petrol Night e Road Light.
- M1 resta in corso fino alla scelta umana della direzione visuale.
- Nessun codice applicativo introdotto.

### 2026-08-31 — M1 Design avviata
- Inserita M1 Design prima dello scheletro Android e rinumerate le milestone fino a M7.
- Aggiornati i riferimenti MIMIT/CI/grafici alla nuova numerazione.
- Aggiunta sezione Design con palette, tipografia e percorsi asset.

### 2026-08-31 — M0 completata
- Repository pubblica e scrivibile.
- Aggiunti spec, README, MIT license e gitignore Android.
- Definiti nome, vincoli, architettura, modello dati e roadmap iniziale.
- Verificata fonte MIMIT, URL correnti e separatore `|`.
- Nessun codice Android, workflow CI o branch temporaneo introdotto.

## 11. Decisioni aperte

- versionamento/naming GitHub Releases e trigger Release;
- semantica autonomia e unità consumo;
- rifornimenti parziali;
- raggio e ordinamento stazioni;
- permessi posizione minimali;
- retention storico prezzi;
- libreria grafici definitiva in M5;
- schema CSV in M7;
