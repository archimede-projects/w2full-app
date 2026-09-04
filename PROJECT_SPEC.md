# W2Full — Project Specification

> **Fonte di verità del progetto.** Questa versione chiude M0–M5 e il checkpoint infrastrutturale di distribuzione GitHub Releases e apre il checkpoint M7.1 richiesto dall'utente. Restano normativi tutti i requisiti M0–M3 presenti nel precedente `PROJECT_SPEC.md` al commit `749f9e44646113fb0c115c9a6685c73beee00b77` e tutti i contratti/evidenze M4 documentati nel candidato verificato al commit `69b4259e855ea35eb9dac1ab5112290837a45933`, salvo quanto esplicitamente modificato qui.

## Stato

- M0–M5: **chiuse**.
- Distribuzione GitHub Releases: **chiusa**.
- M6 — notifiche soglia: **non iniziata**.
- M7 — rifiniture: **in corso; M7.1 filtri/ordinamento Stazioni autorizzata**.
- M7.2 — preferiti nello Storico: **richiesta e accodata; non iniziata finché M7.1 non è chiusa**.

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
- `.github/workflows/android-ci.yml`;
- `.github/workflows/android-release.yml` come unico workflow di distribuzione.

Contratto permanente verificato:
- trigger Release esclusivamente su `push` di tag `v*`;
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

- commit di validazione `c3567b4b7afb7f37651784842460a41a94bc8efc` — `chore(release): validate distribution workflow`;
- run `33884215157`, job `101059934737`: **SUCCESS**;
- certificato SHA-256 verificato: `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`;
- public key SHA-256 verificata: `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`;
- Release reale `v0.4.0-rc1-distcheck1`, prerelease, target commit `c3567b4b7afb7f37651784842460a41a94bc8efc`;
- asset `w2full-v0.4.0-rc1-distcheck1-debug.apk`;
- APK SHA-256 `616ea026b3d3e80ab7e7e865df624a7df088479f9ecc4838015d2b14f8d846ef`.

### Integrazione finale

- PR `#5` — `ci: finalize GitHub Releases distribution`, merge squash;
- commit integrato su `main`: `99803fd1ac1af01622d39ab406833a81bebc3a1b`;
- CI di integrazione run `33884796836`, job `101061834450`: **SUCCESS**;
- commit documentale finale precedente a M5: `200b32d708cc1850564e5d08496a3234e1c9ea79`, CI run `33885088932`, job `101062798425`: **SUCCESS**;
- `release-distribution` e `release-distribution-clean` riallineati a `200b32d708cc1850564e5d08496a3234e1c9ea79` prima dell'avvio M5.

## M5 — storico prezzi + grafico — chiusa

Stato: **[x] chiusa**.

### Contratto implementato

- branch di lavoro `m5-price-history`, derivato dall'HEAD `main` `200b32d708cc1850564e5d08496a3234e1c9ea79`;
- Room schema `3` con migrazione esplicita `2→3`, senza migrazione distruttiva;
- `mimit_prices` resta la cache corrente M4 e continua a essere sostituita atomicamente a ogni refresh;
- nuova tabella append-only `mimit_price_history`, senza foreign key verso la cache corrente;
- chiave primaria/logica storico: `station_id`, `fuel_description`, `is_self`, `communicated_at`;
- ogni osservazione conserva prezzo in milli-euro/unità e `imported_at_epoch_millis`;
- refresh riuscito inserisce lo storico con `INSERT OR IGNORE` nella stessa transazione della cache M4: refresh identici non duplicano, una nuova data comunicazione aggiunge un nuovo punto;
- query per stazione/carburante/modalità ordinate cronologicamente;
- nuova destinazione bottom-nav `Storico`;
- selezione stazione, carburante e modalità Self/Servito con fallback al `defaultFuelType` del veicolo e preferenza Self quando disponibile;
- grafico lineare nativo con `androidx.compose.foundation.Canvas`, senza nuova dipendenza chart;
- gestione esplicita di serie vuota, punto singolo, serie multipla e serie piatta;
- elenco rilevazioni sotto il grafico in ordine cronologico inverso con data comunicazione e prezzo €/l;
- `versionCode = 7`, `versionName = 0.5.0-m5`;
- M6/M7 non incluse.

### Evidenze branch M5

- contratto fissato prima del codice: commit `095c9659d57be37b103ce9d66afa7102f0e6930a`;
- candidato funzionale verificato `b09cd84bf39b0b40fcd830a50ad45f5d8922a397`;
- prima CI branch run `33886894102`, job `101068776817`: **SUCCESS**;
- artifact `w2full-debug-apk`, ID `9942280190`, digest archivio SHA-256 `e65bc393b76bb7353eaafed2307de13cc878ea713f10f7808ae4d0c625d7304d`;
- HEAD finale del PR `8c3ecaf70a57bc82fdc5239ea57229bbcaf6bf3e`;
- CI sul vero HEAD del PR run `33887224084`, job `101069876584`: **SUCCESS**;
- test JVM, `assembleDebug`, `apksigner` e upload artifact: tutti **SUCCESS**.

Test coperti:
- migrazione Room `1→3` e `2→3`, con preservazione dati precedenti;
- deduplica DAO e query filtrate/ordinate;
- refresh: primo import aggiunge storico, import identico non duplica, nuova comunicazione aggiunge punti;
- regressioni M3/M4;
- normalizzazione grafico per empty/single/multiple/flat series.

### Integrazione finale M5

- PR `#6` — `feat(m5): add persistent price history and chart`;
- merge eseguito con **squash**;
- commit M5 integrato su `main`: `71e89190d653e666f4bc73a56a46c7e1eabc7ec6` — `feat(m5): add persistent price history and chart`;
- parent diretto: `200b32d708cc1850564e5d08496a3234e1c9ea79`;
- CI reale su `main` run `33887446442`, job `101070611480`: **SUCCESS**;
- `testDebugUnitTest`, `assembleDebug`, verifica APK/firma e upload artifact: tutti **SUCCESS**;
- firma persistente su APK integrato: certificato SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`, public key SHA-256 `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`;
- artifact CI main `w2full-debug-apk`, ID `9942483998`, digest archivio SHA-256 `887925f389c310b000514cc8e59d2a4009c5019361a340b38b6e64fa5cb9d27f`;
- commit documentale finale `1eb6f5ab3b4a248e30e7a693ac0bc62ac311bffa`, CI run `33887752052`, job `101071620632`: **SUCCESS**;
- Release reale di prova `v0.5.0-m5-rc1`, target/tag diretto `1eb6f5ab3b4a248e30e7a693ac0bc62ac311bffa`, Release run `33890959597`, job `101082219282`: **SUCCESS**;
- APK `w2full-v0.5.0-m5-rc1-debug.apk`, SHA-256 `4768319aecf910875cdabc5f020595434aeebb4bb09c32529902f4754a685806`;
- verifica reale su Samsung Galaxy S25 confermata dall'utente il 4 settembre 2026: installazione aggiornamento, schermata Stazioni e nuova schermata Storico **PASS**.

## M7.1 — filtri e ordinamento schermata Stazioni

Stato: **[~] autorizzata; spec fissata prima del codice**.

### Obiettivo

Permettere all'utente di limitare la lista delle stazioni Eni a un raggio massimo configurabile e di scegliere l'ordinamento per distanza oppure per prezzo del carburante correntemente selezionato dall'impostazione Veicolo, mantenendo invariati download MIMIT, cache, storico e calcolo distanze M4/M5.

### Contratto UI e comportamento

- branch di lavoro `m7-station-filters`, derivato dall'HEAD `main` `1eb6f5ab3b4a248e30e7a693ac0bc62ac311bffa`;
- nuova card `Filtri` nella schermata Stazioni, dopo lo stato posizione e prima dell'elenco;
- filtro raggio opzionale: comportamento predefinito `Nessun limite`, così l'aggiornamento non nasconde stazioni finché l'utente non abilita il filtro;
- quando abilitato, l'utente imposta un raggio intero da `1` a `200` km; il valore iniziale proposto è `20` km;
- il raggio viene applicato solo quando la posizione è disponibile; con posizione negata/non disponibile la preferenza resta salvata ma il filtro raggio non viene applicato e la UI lo segnala;
- con posizione disponibile e filtro attivo, stazioni con distanza nulla o distanza maggiore del limite vengono escluse perché non è possibile garantire che siano entro il raggio;
- ordinamento selezionabile con tre modalità: `Distanza`, `Prezzo Self`, `Prezzo Servito`;
- `Distanza` resta il default e conserva la semantica M4: distanza crescente; distanze mancanti in fondo; in assenza posizione fallback alfabetico già esistente;
- `Prezzo Self`: prezzo Self crescente per il carburante correntemente selezionato; prezzi Self mancanti in fondo; a parità di prezzo tie-break per distanza e poi identificativo stazione;
- `Prezzo Servito`: stessa regola usando il prezzo Servito;
- il filtro raggio viene applicato prima dell'ordinamento;
- le preferenze raggio/ordinamento sono esclusivamente locali e persistono tra riavvii dell'app tramite storage privato Android; nessun account/cloud;
- la card mostra un riepilogo del numero di stazioni visualizzate rispetto al totale disponibile;
- nessuna modifica al refresh MIMIT, alla cache Room, allo storico M5, al filtro bandiera Eni o al `defaultFuelType` del veicolo;
- `versionCode = 8`, `versionName = 0.5.1-m7.1`;
- M7.2 preferiti Storico, M6 notifiche e pulsante Indicazioni restano fuori scope di questo checkpoint.

### Test obbligatori M7.1

- funzione pura filtro/ordinamento: nessun limite + distanza conserva l'ordine atteso;
- raggio attivo con posizione disponibile esclude fuori-raggio e distanza nulla;
- raggio attivo con posizione non disponibile non filtra la lista;
- ordinamento prezzo Self crescente, prezzo mancante in fondo e tie-break distanza;
- ordinamento prezzo Servito equivalente;
- validazione raggio accetta solo `1..200` e conserva un valore valido precedente in caso di input non valido;
- regression test M3–M5 tutti verdi;
- CI reale sul branch con `testDebugUnitTest`, `assembleDebug`, verifica firma persistente e artifact **SUCCESS**;
- prima di chiudere M7.1: PR/integration su `main`, CI reale main **SUCCESS**, aggiornamento evidenze spec e APK Release reale da provare sul Galaxy S25.

## M7.2 — preferiti nello Storico — richiesta

Richiesta già autorizzata dall'utente, ma non viene implementata finché M7.1 non è chiusa. Obiettivo preliminare: poter marcare stazioni come preferite e trovarle/monitorarle prioritariamente nella schermata Storico, mantenendo comunque accessibili le altre stazioni. Il contratto tecnico dettagliato verrà fissato in `PROJECT_SPEC.md` prima del relativo codice.

## Requisito futuro già approvato

M7: pulsante `Indicazioni` su ogni stazione, tramite intent verso Google Maps/app mappe compatibile, destinazione stazione e partenza dalla posizione corrente; preferire coordinate MIMIT valide con fallback indirizzo. Issue #1.

## Regola di avanzamento

Un checkpoint alla volta. M7.1 è autorizzata; M7.2 è già richiesta ma parte solo dopo chiusura M7.1. M6 notifiche soglia resta non iniziata finché non viene esplicitamente autorizzata come obiettivo attivo.
