# W2Full — Project Specification

> **Fonte di verità del progetto.** `PROJECT_SPEC.md` deve essere aggiornato **prima di ogni modifica al codice o agli asset di progetto**. Ogni milestone procede un obiettivo alla volta: aggiornamento spec → implementazione/asset e test pertinenti → verifica reale → commit finale di chiusura spec → cleanup dei branch temporanei.

## 1. Obiettivo del progetto

W2Full è un'app Android nativa, gratuita e locale per gestire i rifornimenti e i consumi di un singolo veicolo e consultare i prezzi carburante delle stazioni vicine usando gli open data ufficiali MIMIT.

Obiettivi principali:

- registro manuale dei rifornimenti;
- consumo medio, costo/km e stima autonomia residua;
- download e import dei dataset MIMIT relativi a impianti e prezzi;
- stazioni vicine alla posizione dell'utente, con focus iniziale sulla bandiera **Eni**;
- storico locale dei prezzi e grafico dell'andamento;
- avviso quando un prezzo scende sotto una soglia configurata;
- esportazione CSV dei rifornimenti per backup manuale.

La V1 gestisce un solo veicolo e filtra inizialmente Eni, mantenendo però un modello dati estendibile a più veicoli e brand.

## 2. Vincoli

- App **100% gratuita**: nessuna carta di credito e nessun servizio a pagamento.
- Nessun account, backend applicativo o cloud per i dati utente.
- Persistenza utente esclusivamente locale.
- Pubblicazione esclusivamente tramite **APK / GitHub Releases**, non Play Store.
- Android nativo in **Kotlin + Jetpack Compose**.
- Dispositivo target principale: **Samsung Galaxy S25**.
- Material 3 e dark mode.
- Un solo veicolo nella prima versione.
- Filtro iniziale Eni, modello dati generico.
- Rete usata soltanto per scaricare CSV MIMIT; nessuna REST API applicativa.
- Esportazione rifornimenti in CSV.
- APK debug aggiornabili sopra la versione precedente grazie a una firma debug persistente.
- Il debug keystore viene generato una sola volta e conservato come secret, mai committato.
- Quando esiste codice Android, ogni milestone deve verificare una **build reale che produca un APK**, non sola compilazione.
- Metodo di lavoro: spec prima delle modifiche → codice/asset e test → verifica → chiusura spec → cleanup branch.

## 3. Architettura

### UI

- Jetpack Compose + Material 3.
- Tema chiaro/scuro.
- UI coerente con la direzione visuale scelta in M1.

### Pattern applicativo

- MVVM + Repository.
- Stato UI tramite `StateFlow`.

### Persistenza locale

- Room come persistenza dei dati utente.
- Nessun account, backend o sync cloud.

### Rete e sincronizzazione

- OkHttp per il download dei CSV MIMIT.
- WorkManager: job giornaliero + refresh manuale.
- Parser con validazione esplicita di header e separatore.

### Posizione

- `FusedLocationProviderClient`.
- Distanza impianti tramite formula Haversine.
- Nessun invio della posizione a un backend W2Full.

### Notifiche

- WorkManager per le verifiche periodiche.
- NotificationCompat per gli avvisi di soglia prezzo.

### Grafici

- Libreria Compose-native, leggera e gratuita.
- Candidato iniziale: **Vico**; versione, licenza e compatibilità da verificare in M5 prima di aggiungere la dipendenza.

### Test

- JUnit per consumo medio, costo/km, autonomia residua e casi limite.

## 4. Modello dati

Il modello è concettuale; tipi Kotlin, indici Room e relazioni definitive verranno specificati prima dell'implementazione relativa.

### Veicolo

Campi previsti: `id`, `nome`, `tipoCarburantePredefinito`, `capacitaSerbatoioLitri`, eventuale riferimento odometrico iniziale e metadati futuri. In V1 esiste un solo veicolo attivo.

### Rifornimento

Campi minimi: `id`, `veicoloId`, `data`, `kmAttuali`, `litri`, `costoTotale`, `tipoCarburante`. Possibili estensioni future: prezzo/litro, pieno sì/no, note, impianto associato.

### Impianto

Campi concettuali: `idImpiantoMimit`, `gestore`, `bandiera`, `tipoImpianto`, `nomeImpianto`, `indirizzo`, `comune`, `provincia`, `latitudine`, `longitudine`, metadati di import. Eni è un filtro funzionale iniziale, non un vincolo strutturale.

### StoricoPrezzo

Campi concettuali: `id`, `idImpiantoMimit`, `descrizioneCarburante`, `prezzo`, `isSelf`, `dataOraComunicazione`, `dataEstrazione`, `dataImportLocale`. Lo storico deve evitare duplicati logici.

### SogliaAvviso

Campi concettuali: `id`, `tipoCarburante`, `prezzoMassimo`, modalità self/servito, `bandiera` opzionale, eventuale raggio massimo, `attiva`, metadati anti-duplicazione notifiche.

## 5. Funzionalità

Legenda: `[ ]` da fare · `[~]` in corso · `[x]` fatto.

### Fondazioni progetto

- [x] Repository pubblica `w2full-app` e documentazione M0.
- [ ] Scheletro Android Kotlin + Compose + Material 3.
- [ ] Pipeline GitHub Actions con test/build e APK reale.
- [ ] Firma debug con keystore persistente da secrets.
- [ ] Pubblicazione APK debug tramite GitHub Releases.

### Design

- [~] Definizione direzione visiva iniziale.
- [~] 4 concept di logo/icona 1024x1024 in `design/logo/`.
- [~] Mockup statici Home in due varianti di tema.
- [~] Mockup statici Registro rifornimenti in due varianti di tema.
- [~] Mockup statici Storico prezzi in due varianti di tema.
- [ ] Selezione finale della direzione visiva prima di M2.

### Registro rifornimenti

- [ ] Inserimento, elenco, modifica ed eliminazione rifornimenti.
- [ ] Persistenza Room e validazione input.

### Calcoli automatici

- [ ] Consumo medio.
- [ ] Costo medio/km.
- [ ] Stima autonomia residua.
- [ ] Test JUnit e casi limite.

### Dati MIMIT e stazioni vicine

- [ ] Download manuale e giornaliero dei CSV.
- [ ] Parsing robusto e import anagrafica/prezzi.
- [ ] Filtro Eni.
- [ ] Posizione dispositivo e calcolo Haversine.
- [ ] Elenco stazioni vicine.

### Storico e grafici

- [ ] Persistenza e deduplicazione storico prezzi.
- [ ] Vista andamento temporale e grafico Compose-native.

### Soglie e notifiche

- [ ] Configurazione soglia.
- [ ] Verifica periodica e notifiche locali.
- [ ] Protezione da notifiche duplicate/eccessive.

### Rifiniture e backup

- [ ] Esportazione rifornimenti in CSV.
- [ ] Impostazioni, tema e dark mode.
- [ ] Errori/retry/empty states.

## 6. Roadmap a milestone

La roadmap inserisce il design prima dello scheletro Android, così logo, palette e layout delle schermate principali possono essere scelti prima dell'implementazione UI. Le milestone tecniche successive vengono rinumerate senza cambiare la loro dipendenza logica.

### M0 — Repo + spec

Stato: **[x] fatto**

Deliverable: repository pubblica; `PROJECT_SPEC.md`; `README.md`; `LICENSE` MIT; `.gitignore` Android; nessun codice applicativo.

### M1 — Design: logo e mockup

Stato: **[~] in corso**

Deliverable:

- 3-4 concept di logo/icona W2Full, **1024x1024**, in `design/logo/` e committati;
- mockup **statici** di Home, Registro rifornimenti e Storico prezzi, con **2 varianti di palette/tema per schermata**, in `design/mockup/` e committati;
- nessun codice dell'app;
- sezione `Design` con palette, tipografia e percorsi asset;
- review e scelta della direzione visiva prima di M2.

Criterio di chiusura: asset committati e direzione visuale approvata. Fino all'approvazione M1 resta in corso.

### M2 — Scheletro Android + CI con APK installabile

Stato: **[ ] da fare**

Deliverable: progetto Android Kotlin; Compose + Material 3; package/applicationId definito prima del codice; configurazione target Samsung Galaxy S25; test minimo; GitHub Actions con build reale; APK debug; firma persistente ricostruita da secrets.

Criterio di chiusura: APK generato in CI, installabile e con firma stabile tra build successive.

### M3 — Registro rifornimenti + calcoli

Stato: **[ ] da fare**

Deliverable: Room; CRUD rifornimenti; singolo veicolo; consumo medio; costo/km; autonomia residua; test JUnit.

### M4 — Integrazione dati MIMIT

Stato: **[ ] da fare**

Deliverable: download CSV via OkHttp; parsing formato corrente; import impianti/prezzi; filtro Eni; refresh manuale; WorkManager giornaliero; posizione; Haversine; stazioni vicine.

### M5 — Storico prezzi + grafico

Stato: **[ ] da fare**

Deliverable: storico locale, deduplicazione, andamento temporale e grafico Compose-native gratuito.

### M6 — Notifiche soglia prezzo

Stato: **[ ] da fare**

Deliverable: soglia configurabile, controllo periodico, notifiche locali e protezione anti-spam.

### M7 — Rifiniture

Stato: **[ ] da fare**

Deliverable: esportazione CSV, impostazioni, rifinitura tema/dark mode, UX errori/permessi/stati vuoti, revisione generale.

## 7. Design

M1 produce riferimenti grafici **statici**, non componenti funzionanti e non codice Compose.

### Variante A — Petrol Night

- Background `#101418`
- Surface `#182028`
- Primary `#33C3A5`
- Secondary `#7FD1FF`
- Accent `#FFB84D`
- Error/Price Alert `#FF6B6B`
- Testo principale `#F5F7FA`
- Testo secondario `#A9B4C2`

Direzione: dashboard scura, tecnica e automotive, con enfasi su dati e contrasto.

### Variante B — Road Light

- Background `#F6F7F9`
- Surface `#FFFFFF`
- Primary `#1C6DD0`
- Secondary `#25A18E`
- Accent `#F59E0B`
- Error/Price Alert `#E45757`
- Testo principale `#1F2937`
- Testo secondario `#6B7280`

Direzione: interfaccia chiara e luminosa, orientata a leggibilità e consultazione rapida.

### Tipografia di riferimento

- Titoli e metriche chiave: **Roboto Bold/Medium**.
- Testo UI: **Roboto Regular/Medium**.
- Valori tabellari/metriche: **Roboto Mono Medium**.

La scelta definitiva potrà essere affinata in M2 mantenendo font gratuiti e compatibili con Android/Material 3.

### Percorsi asset M1

Logo/icona:

- `design/logo/w2full-logo-concept-01.jpg`
- `design/logo/w2full-logo-concept-02.jpg`
- `design/logo/w2full-logo-concept-03.jpg`
- `design/logo/w2full-logo-concept-04.jpg`

Mockup:

- `design/mockup/home-theme-petrol-night.jpg`
- `design/mockup/home-theme-road-light.jpg`
- `design/mockup/rifornimenti-theme-petrol-night.jpg`
- `design/mockup/rifornimenti-theme-road-light.jpg`
- `design/mockup/storico-theme-petrol-night.jpg`
- `design/mockup/storico-theme-road-light.jpg`

## 8. Fonte dati esterna

Dataset ufficiale: MIMIT — **Carburanti - Prezzi praticati e anagrafica degli impianti**.

Pagina dataset:
`https://www.mimit.gov.it/it/open-data/elenco-dataset/carburanti-prezzi-praticati-e-anagrafica-degli-impianti`

URL diretti verificati al 31 agosto 2026:

- Prezzi alle 8: `https://www.mimit.gov.it/images/exportCSV/prezzo_alle_8.csv`
- Anagrafica impianti attivi: `https://www.mimit.gov.it/images/exportCSV/anagrafica_impianti_attivi.csv`

Note:

- aggiornamento quotidiano;
- dal **10 febbraio 2026** il separatore corrente per “Anagrafica alle 8” e “Prezzi alle 8” è `|` (pipe);
- il parser non deve assumere la virgola soltanto perché l'estensione è `.csv`;
- in M4 si devono ricontrollare URL, header e formato effettivo prima di implementare il parser;
- per i prezzi i metadati indicano almeno `idimpianto`, `descCarburante`, `prezzo`, `isSelf`, `dtComu`;
- licenza dataset: **IODL 2.0** secondo la pagina ufficiale.

## 9. CI/CD

- GitHub Actions, senza servizi a pagamento.
- Da M2, pipeline con checkout, toolchain, cache Gradle, test, `assembleDebug` (o equivalente), verifica APK e upload artifact.
- Per i flussi di release previsti fra M2 e M7, pubblicazione anche via GitHub Releases secondo strategia definita nella spec prima dell'implementazione.

### Debug keystore persistente

- generato una sola volta;
- mai committato;
- keystore codificato (es. Base64) in GitHub Actions secret;
- password/alias in secrets separati se necessario;
- ricostruito temporaneamente nel runner;
- configurazione signing tramite secrets/variabili d'ambiente;
- nessun segreto nei log.

## 10. Changelog

### 2026-08-31 — M1 Design avviata

- Inserita M1 “Design: logo e mockup” prima dello scheletro Android.
- Rinumerate le milestone tecniche fino a M7 e aggiornati i riferimenti MIMIT/CI/grafici.
- Aggiunta la sezione `Design` con due palette candidate, tipografia e percorsi asset.
- Previsti quattro concept logo e sei mockup statici, senza codice applicativo.

### 2026-08-31 — M0 completata

- Repository `archimede-projects/w2full-app` verificata pubblica e scrivibile.
- Aggiunti `PROJECT_SPEC.md`, `README.md`, `LICENSE` MIT e `.gitignore` Android.
- Definiti nome W2Full, vincoli, architettura, modello dati e roadmap iniziale.
- Verificata la fonte MIMIT, inclusi URL correnti e separatore `|` in vigore dal 10 febbraio 2026 per i file alle 8.
- Nessun codice Android, workflow CI o branch temporaneo introdotto in M0.

## 11. Decisioni aperte

- selezione del concept logo/icona e della palette da portare in M2;
- package name / `applicationId` definitivo;
- `minSdk`, `targetSdk`, `compileSdk` definitivi in M2;
- versionamento APK e naming GitHub Releases;
- trigger di pubblicazione Release;
- nomi definitivi dei secrets per il keystore;
- semantica dell'autonomia residua;
- unità consumo mostrate (km/l e/o l/100 km);
- comportamento dei rifornimenti parziali;
- raggio/ordinamento delle stazioni vicine;
- strategia permessi posizione evitando permessi più invasivi del necessario;
- politica di conservazione dello storico prezzi;
- libreria grafici definitiva in M5;
- schema esportazione CSV in M7.
