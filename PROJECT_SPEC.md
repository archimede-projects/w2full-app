# W2Full — Project Specification

> **Fonte di verità del progetto.** Questa versione chiude M4 e il checkpoint infrastrutturale di distribuzione GitHub Releases e apre M5. Restano normativi tutti i requisiti M0–M3 presenti nel precedente `PROJECT_SPEC.md` al commit `749f9e44646113fb0c115c9a6685c73beee00b77` e tutti i contratti/evidenze M4 documentati nel candidato verificato al commit `69b4259e855ea35eb9dac1ab5112290837a45933`, salvo quanto esplicitamente modificato qui.

## Stato

- M0–M4: **chiuse**.
- Distribuzione GitHub Releases: **chiusa**.
- M5 — storico prezzi + grafico: **in corso**.
- M6 — notifiche soglia: **non iniziata**.
- M7 — rifiniture: **non iniziata**.

## M4 — integrazione MIMIT chiusa

M4 è stata integrata su `main` il 3 settembre 2026 tramite integrazione pulita, senza incorporare i file o la storia del checkpoint distribuzione.

Commit funzionale integrato:
`343425cb3e87b140ed7c32f145de83f3ca399183` — `feat(m4): integrate verified MIMIT milestone`.

Contenuto M4 su `main`:
- client/parser MIMIT e contratti CSV;
- correzione mirata dell'artefatto reale `| gestori.prezzibenzina.it` mantenendo validazione stretta;
- filtro Eni exact-match su `eni` e `agip eni`;
- posizione foreground, Haversine, fallback e timeout applicativo di 12 secondi;
- schermata Stazioni con ranking, indirizzo, distanza, prezzi Self/Servito e timestamp cache;
- refresh atomico/resiliente, logger `W2Full-MIMIT`, Room v2 e migrazione 1→2;
- WorkManager sync con stessa semantica del refresh manuale;
- schermata Veicolo con `defaultFuelType` configurabile e prezzi aggiornati dalla cache senza nuovo download;
- regression test M3/M4;
- `versionCode = 6`, `versionName = 0.4.0-rc1`, corrispondenti al contenuto funzionale realmente provato.

## Evidenze finali M4

RC realmente provata:
- Release `v0.4.0-rc1`;
- tag annotato `5c1b2f7bba64e205bb3b1d84be9db7cbf1140602` → commit `69b4259e855ea35eb9dac1ab5112290837a45933`;
- Release run `33716404065`, job `100526422722`: **SUCCESS**;
- APK SHA-256 `616ea026b3d3e80ab7e7e865df624a7df088479f9ecc4838015d2b14f8d846ef`;
- verifica reale Galaxy S25: **PASS** il 3 settembre 2026.

Integrazione pulita:
- branch `m4-integration-clean` derivata direttamente dal vecchio `main` `749f9e44646113fb0c115c9a6685c73beee00b77`;
- CI branch run `33718156047`, job `100531588029`: **SUCCESS** con test JVM, build, verifica firma e artifact;
- confronto col vecchio main conferma assenza di `.github/workflows/android-release.yml` e `.github/workflows/bootstrap-preview-release.yml`;
- `main` avanzato a `343425cb3e87b140ed7c32f145de83f3ca399183`;
- CI reale su main run `33718342202`, job `100532144968`: **SUCCESS** con test JVM, build, verifica firma e artifact.

Certificato persistente atteso e verificato lungo M4: SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`; public key SHA-256 `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`.

## Distribuzione GitHub Releases — chiusa

Questo intervento è stato esclusivamente infrastrutturale. Non ha introdotto funzionalità M5 e non ha modificato comportamento, dati o UI dell'app.

### Workflow permanente

Il tree finale di `main` contiene:
- `.github/workflows/android-ci.yml` invariato rispetto al checkpoint precedente;
- `.github/workflows/android-release.yml` come unico workflow di distribuzione.

Contratto permanente verificato:
- trigger esclusivamente su `push` di tag `v*`;
- nessun `workflow_call`;
- nessun `workflow_dispatch` per pubblicare APK;
- nessun `.github/workflows/bootstrap-preview-release.yml` o altro bootstrap Release;
- checkout del commit realmente taggato;
- test JVM `testDebugUnitTest` prima della build;
- build `assembleDebug`;
- uso obbligatorio del keystore debug persistente via secrets: se un secret di firma manca, la Release fallisce;
- verifica con `apksigner` del certificato SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265` e della public key SHA-256 `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`;
- APK versionato come `w2full-<tag>-debug.apk`;
- calcolo SHA-256 dell'APK e pubblicazione del digest nel corpo della GitHub Release;
- APK distribuito esclusivamente come asset di una vera GitHub Release.

### Validazione Release reale

Validazione eseguita prima dell'integrazione:
- commit di validazione `c3567b4b7afb7f37651784842460a41a94bc8efc` — `chore(release): validate distribution workflow`;
- run `33884215157`, job `101059934737`: **SUCCESS**;
- test JVM: **SUCCESS**;
- `assembleDebug`: **SUCCESS**;
- certificato SHA-256 verificato: `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`;
- public key SHA-256 verificata: `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`;
- Release reale `v0.4.0-rc1-distcheck1`, prerelease, target commit `c3567b4b7afb7f37651784842460a41a94bc8efc`;
- asset `w2full-v0.4.0-rc1-distcheck1-debug.apk`;
- APK SHA-256 `616ea026b3d3e80ab7e7e865df624a7df088479f9ecc4838015d2b14f8d846ef`;
- cleanup del trigger/logica temporanea eseguito nel branch prima dell'integrazione.

### Integrazione finale

- PR `#5` — `ci: finalize GitHub Releases distribution`;
- merge eseguito con **squash**, così la storia temporanea di validazione non entra nella storia lineare di `main`;
- commit integrato su `main`: `99803fd1ac1af01622d39ab406833a81bebc3a1b` — `ci: finalize GitHub Releases distribution`;
- parent diretto: `3abd465cd9f049bc45b552a68d174f3d83b75191`;
- CI reale di integrazione su `main`: run `33884796836`, job `101061834450`: **SUCCESS**;
- nella CI di integrazione: test JVM, build APK, verifica firma persistente e upload artifact tutti **SUCCESS**;
- log CI integrazione: certificato SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`, public key SHA-256 `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`;
- commit documentale finale `200b32d708cc1850564e5d08496a3234e1c9ea79`, con CI run `33885088932`, job `101062798425`: **SUCCESS**;
- `release-distribution` e `release-distribution-clean` riallineati a `200b32d708cc1850564e5d08496a3234e1c9ea79`.

## M5 — storico prezzi + grafico

Stato: **[~] in corso**.

### Obiettivo

Conservare localmente, tra refresh MIMIT successivi, le osservazioni di prezzo delle stazioni Eni e permettere di consultare l'andamento cronologico di una stazione/carburante/modalità di servizio. M5 non introduce notifiche, soglie o nuovi download esterni rispetto al flusso MIMIT già esistente.

### Decisioni tecniche fissate prima del codice

- branch di lavoro: `m5-price-history`, derivato dall'HEAD `main` `200b32d708cc1850564e5d08496a3234e1c9ea79`;
- Room passa da schema `2` a `3` con migrazione esplicita `2→3` e senza migrazione distruttiva;
- la tabella `mimit_prices` resta la cache corrente usata da M4: continua a essere sostituita atomicamente a ogni refresh;
- nuova tabella append-only `mimit_price_history`, indipendente dalla cancellazione della cache corrente;
- chiave logica/primaria dello storico: `station_id`, `fuel_description`, `is_self`, `communicated_at`; il medesimo record MIMIT importato più volte non crea duplicati;
- lo storico conserva inoltre `price_milli_euro_per_unit` e `imported_at_epoch_millis`;
- durante ogni refresh riuscito, le righe prezzo Eni validate vengono inserite nello storico con `INSERT OR IGNORE` nella stessa transazione che aggiorna la cache corrente;
- nessuna foreign key dallo storico verso `mimit_stations`, così una stazione non presente nella cache corrente non perde la propria serie storica;
- query storico ordinate cronologicamente per `communicated_at`, con filtro esatto per stazione/carburante/modalità;
- UI: nuova destinazione bottom-nav `Storico`;
- selezione iniziale: prima stazione Eni disponibile; carburante iniziale uguale al `defaultFuelType` del veicolo quando presente nello storico, altrimenti primo carburante disponibile; modalità iniziale `Self` quando disponibile, altrimenti `Servito`;
- grafico lineare realizzato con `androidx.compose.foundation.Canvas`, senza dipendenze chart aggiuntive. Vico è stato rivalutato in M5 ma non introdotto: la linea 3.x corrente usa l'asse Compose Multiplatform/JetBrains, mentre W2Full resta sulla toolchain AndroidX Compose già verificata;
- il grafico deve gestire 0, 1 o più punti senza crash: empty state esplicito, punto singolo visibile, più punti collegati in ordine cronologico;
- sotto il grafico viene mostrato l'elenco cronologico inverso con data comunicazione e prezzo €/l;
- `versionCode = 7`, `versionName = 0.5.0-m5`;
- M6/M7 restano fuori scope.

### Test obbligatori

- migrazione Room `2→3` preserva tabelle/dati M3-M4 e crea lo storico;
- DAO: inserimento storico deduplicato e query filtrata/ordinata;
- repository refresh: un refresh aggiunge storico, refresh identico non duplica, refresh successivo con nuova comunicazione aggiunge un nuovo punto;
- regression test M3/M4 esistenti tutti verdi;
- test puri della trasformazione serie grafico/empty-single-multiple;
- CI branch reale con test JVM, `assembleDebug`, verifica firma persistente e artifact **SUCCESS**;
- prima di chiudere M5: integrazione su `main`, CI reale su `main` **SUCCESS**, aggiornamento delle evidenze in questa spec e riallineamento branch M5 al commit finale.

## Requisito futuro già approvato

M7: pulsante `Indicazioni` su ogni stazione, tramite intent verso Google Maps/app mappe compatibile, destinazione stazione e partenza dalla posizione corrente; preferire coordinate MIMIT valide con fallback indirizzo. Issue #1.

## Regola di avanzamento

M6 e milestone successive non partono senza nuova autorizzazione esplicita dell'utente.
