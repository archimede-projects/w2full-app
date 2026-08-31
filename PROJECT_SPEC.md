# W2Full — Project Specification

> **Fonte di verità del progetto.** Questo file deve essere aggiornato **prima di ogni modifica al codice**. Ogni milestone viene sviluppata un obiettivo alla volta: aggiornamento spec → implementazione e test → build reale su GitHub Actions con generazione APK → commit finale di chiusura spec → cleanup dei branch temporanei.

## 1. Obiettivo del progetto

W2Full è un'app Android nativa, gratuita e locale per gestire i rifornimenti e i consumi di un singolo veicolo e per consultare i prezzi carburante delle stazioni vicine usando i dataset open data ufficiali del Ministero delle Imprese e del Made in Italy (MIMIT).

Obiettivi principali:

- registrare manualmente i rifornimenti;
- calcolare consumo medio, costo per km e stima dell'autonomia residua;
- scaricare e importare i dati ufficiali MIMIT relativi agli impianti e ai prezzi carburante;
- mostrare le stazioni vicine alla posizione dell'utente, con focus iniziale sulla bandiera **Eni**;
- conservare uno storico locale dei prezzi e visualizzarne l'andamento;
- notificare l'utente quando un prezzo scende sotto una soglia configurata;
- consentire il backup manuale del registro rifornimenti tramite esportazione CSV.

L'architettura deve restare estendibile a più veicoli e più brand in futuro, pur mantenendo la prima versione volutamente mono-veicolo e con filtro Eni.

## 2. Vincoli

- App **100% gratuita**.
- Nessuna carta di credito richiesta in sviluppo o utilizzo.
- Nessun servizio a pagamento.
- Nessun account utente.
- Nessun backend applicativo.
- Nessun cloud per i dati dell'utente: persistenza esclusivamente locale sul dispositivo.
- Distribuzione esclusivamente tramite **APK / GitHub Releases**; nessuna pubblicazione su Google Play Store.
- Sviluppo Android nativo in **Kotlin + Jetpack Compose**.
- Dispositivo target principale: **Samsung Galaxy S25**.
- Dark mode supportato.
- Un solo veicolo gestito nella prima versione.
- Modello dati generico per futura estensione a più veicoli e brand.
- Filtro iniziale delle stazioni sulla sola bandiera **Eni**.
- I dati di rete provengono esclusivamente dai CSV MIMIT; nessuna REST API applicativa.
- I rifornimenti devono poter essere esportati in CSV per backup manuale.
- Gli aggiornamenti APK di debug devono poter essere installati sopra la versione precedente senza disinstallazione.
- Il debug keystore deve essere persistente e generato una sola volta; non deve essere committato nel repository.
- Ogni milestone deve produrre una build reale di verifica su GitHub Actions quando esiste codice Android, con generazione di un APK vero e non sola compilazione.
- Metodo di lavoro obbligatorio: aggiornare prima `PROJECT_SPEC.md`, poi modificare il codice e i test, poi verificare con CI, poi chiudere la milestone nella spec, infine rimuovere i branch temporanei.

## 3. Architettura

### UI

- Jetpack Compose.
- Material 3.
- Supporto tema chiaro/scuro con dark mode.

### Pattern applicativo

- MVVM.
- Repository pattern.
- Stato UI esposto tramite `StateFlow`.

### Persistenza locale

- Room.
- Database locale come unica fonte persistente dei dati utente.
- Nessun account, backend o sincronizzazione cloud.

### Rete

- OkHttp.
- Uso limitato al download dei file CSV ufficiali MIMIT.
- Nessuna REST API applicativa.

### Sincronizzazione dati carburante

- WorkManager.
- Job giornaliero per aggiornare i dataset/prezzi.
- Refresh manuale disponibile dall'interfaccia.
- Import robusto rispetto a cambi di formato compatibili e con validazione esplicita dell'header/separatore.

### Posizione e distanza

- `FusedLocationProviderClient` per ottenere la posizione del dispositivo.
- Calcolo della distanza tra utente e impianti tramite formula di Haversine.
- Nessun invio della posizione a backend W2Full, poiché non esiste un backend.

### Notifiche

- WorkManager per la verifica periodica delle soglie.
- NotificationCompat per notificare il superamento verso il basso di una soglia prezzo configurata.

### Grafici

- Libreria Compose-native, leggera e gratuita.
- Candidato iniziale: **Vico**.
- Versione, licenza e compatibilità Compose da verificare al momento di M4 prima di introdurre la dipendenza.

### Test

- JUnit per la logica di dominio e, in particolare, per:
  - consumo medio;
  - costo/km;
  - autonomia residua;
  - casi limite e dati insufficienti.

## 4. Modello dati

Il modello seguente è concettuale; tipi Kotlin, indici Room e relazioni definitive saranno specificati prima delle rispettive implementazioni.

### Veicolo

Rappresenta il veicolo gestito dall'app.

Campi previsti:

- `id` — identificativo locale;
- `nome` — nome descrittivo opzionale;
- `tipoCarburantePredefinito`;
- `capacitaSerbatoioLitri` — necessaria per la stima autonomia quando configurata;
- `kmIniziali` / riferimento odometrico iniziale, se utile ai calcoli;
- metadati futuri compatibili con estensione multi-veicolo.

Vincolo V1: un solo record veicolo attivo.

### Rifornimento

Rappresenta un rifornimento inserito manualmente.

Campi minimi:

- `id`;
- `veicoloId`;
- `data`;
- `kmAttuali`;
- `litri`;
- `costoTotale`;
- `tipoCarburante`.

Campi eventualmente aggiungibili in milestone successive, previa modifica della spec: prezzo/litro calcolato o inserito, pieno sì/no, note, impianto associato.

### Impianto

Rappresenta un distributore proveniente dall'anagrafica MIMIT.

Campi concettuali:

- `idImpiantoMimit`;
- `gestore`;
- `bandiera`;
- `tipoImpianto`;
- `nomeImpianto` se disponibile;
- `indirizzo`;
- `comune`;
- `provincia`;
- `latitudine`;
- `longitudine`;
- metadati di import/aggiornamento locale.

Il modello non deve codificare Eni come unica possibilità: il filtro Eni è una regola funzionale iniziale, non un vincolo strutturale.

### StoricoPrezzo

Rappresenta una rilevazione di prezzo associata a un impianto e a un carburante.

Campi concettuali:

- `id` locale;
- `idImpiantoMimit`;
- `descrizioneCarburante`;
- `prezzo`;
- `isSelf`;
- `dataOraComunicazione` MIMIT;
- `dataEstrazione` / data del dataset;
- `dataImportLocale`.

La conservazione dello storico deve evitare duplicati logici dello stesso prezzo/rilevazione.

### SogliaAvviso

Rappresenta una regola locale di notifica prezzo.

Campi concettuali:

- `id`;
- `tipoCarburante`;
- `prezzoMassimo`;
- `soloSelf` o modalità di servizio, se prevista;
- `bandiera` opzionale, inizialmente Eni;
- eventuale raggio massimo dalla posizione;
- `attiva`;
- metadati per evitare notifiche duplicate eccessive.

## 5. Funzionalità

Legenda: `[ ]` da fare · `[~]` in corso · `[x]` fatto.

### Fondazioni progetto

- [x] Repository pubblica `w2full-app` e documentazione iniziale M0.
- [ ] Scheletro Android Kotlin + Jetpack Compose + Material 3.
- [ ] Pipeline GitHub Actions che esegue test/build e produce un APK reale.
- [ ] Firma debug con keystore persistente ricostruito da GitHub Actions secrets.
- [ ] Pubblicazione APK debug tramite GitHub Releases.

### Registro rifornimenti

- [ ] Inserimento rifornimento manuale: data, km attuali, litri, costo, tipo carburante.
- [ ] Elenco rifornimenti.
- [ ] Modifica rifornimento.
- [ ] Eliminazione rifornimento.
- [ ] Persistenza Room.
- [ ] Validazione dei dati inseriti.

### Calcoli automatici

- [ ] Consumo medio.
- [ ] Costo medio per km.
- [ ] Stima autonomia residua.
- [ ] Test JUnit dei calcoli e casi limite.

### Dati MIMIT e stazioni vicine

- [ ] Download manuale dei CSV MIMIT.
- [ ] Download giornaliero tramite WorkManager.
- [ ] Parsing robusto del CSV con separatore verificato al runtime/import.
- [ ] Import anagrafica impianti.
- [ ] Import prezzi alle 8.
- [ ] Filtro iniziale bandiera Eni.
- [ ] Posizione tramite FusedLocationProviderClient.
- [ ] Calcolo Haversine.
- [ ] Elenco stazioni vicine ordinabile per distanza/prezzo secondo UX da definire.

### Storico e grafici

- [ ] Persistenza storico prezzi.
- [ ] Vista andamento prezzi nel tempo.
- [ ] Grafico Compose-native con libreria gratuita verificata.

### Soglie e notifiche

- [ ] Configurazione soglia prezzo.
- [ ] Verifica periodica tramite WorkManager.
- [ ] Notifica locale tramite NotificationCompat.
- [ ] Controllo duplicati/spam notifiche.

### Rifiniture e backup

- [ ] Esportazione rifornimenti in CSV.
- [ ] Impostazioni essenziali.
- [ ] Rifinitura tema Material 3 e dark mode.
- [ ] Gestione errori/retry/empty states.

## 6. Roadmap a milestone

La roadmap proposta viene mantenuta perché separa bene le dipendenze: prima la capacità di produrre e distribuire un APK, poi i dati locali e la logica di dominio, quindi la rete/MIMIT, infine storico, notifiche e rifiniture.

### M0 — Repo + spec

Stato: **[x] fatto**

Deliverable:

- repository GitHub pubblica `w2full-app`;
- `PROJECT_SPEC.md`;
- `LICENSE` MIT;
- `.gitignore` Android;
- `README.md` minimale che rimanda alla spec;
- nessun codice applicativo.

Criterio di chiusura: repository pubblica e scrivibile con i quattro file iniziali committati.

### M1 — Scheletro Android + CI con APK installabile

Stato: **[ ] da fare**

Deliverable:

- progetto Android nativo Kotlin;
- Jetpack Compose + Material 3;
- package/applicationId da definire prima del codice;
- configurazione target coerente con Samsung Galaxy S25;
- test minimo di verifica progetto;
- GitHub Actions con build reale;
- APK debug come artifact e/o GitHub Release secondo il flusso definito;
- debug keystore persistente ricostruito da secrets.

Criterio di chiusura: APK generato in CI e installabile, con firma stabile tra build successive.

### M2 — Registro rifornimenti + calcoli

Stato: **[ ] da fare**

Deliverable:

- Room;
- CRUD rifornimenti;
- gestione singolo veicolo;
- consumo medio;
- costo/km;
- stima autonomia residua;
- test JUnit della logica di calcolo.

### M3 — Integrazione dati MIMIT

Stato: **[ ] da fare**

Deliverable:

- download CSV via OkHttp;
- parsing formato MIMIT corrente;
- import anagrafica impianti e prezzi;
- filtro Eni;
- refresh manuale;
- WorkManager giornaliero;
- posizione dispositivo;
- distanza Haversine;
- stazioni vicine.

### M4 — Storico prezzi + grafico

Stato: **[ ] da fare**

Deliverable:

- storico locale dei prezzi;
- deduplicazione rilevazioni;
- andamento temporale;
- grafico Compose-native leggero e gratuito, con Vico come candidato da verificare.

### M5 — Notifiche soglia prezzo

Stato: **[ ] da fare**

Deliverable:

- configurazione soglia;
- controllo periodico;
- notifiche locali;
- protezione da notifiche duplicate/eccessive.

### M6 — Rifiniture

Stato: **[ ] da fare**

Deliverable:

- esportazione CSV dei rifornimenti;
- impostazioni;
- rifinitura tema/dark mode;
- UX per errori, permessi, stati vuoti e aggiornamento dati;
- revisione generale prima della prima release stabile utilizzabile.

## 7. Fonte dati esterna

### Dataset ufficiale

Ministero delle Imprese e del Made in Italy — **Carburanti - Prezzi praticati e anagrafica degli impianti**.

Pagina dataset:

- `https://www.mimit.gov.it/it/open-data/elenco-dataset/carburanti-prezzi-praticati-e-anagrafica-degli-impianti`

La pagina MIMIT dichiara aggiornamento quotidiano e dati relativi alle informazioni in vigore alle ore 8 del giorno precedente alla pubblicazione.

### URL diretti verificati al 31 agosto 2026

- Prezzi alle 8: `https://www.mimit.gov.it/images/exportCSV/prezzo_alle_8.csv`
- Anagrafica impianti attivi: `https://www.mimit.gov.it/images/exportCSV/anagrafica_impianti_attivi.csv`

Questi URL devono comunque essere considerati configurabili/centralizzati nel codice e ricontrollati al momento dell'implementazione M3, perché un endpoint pubblico può cambiare senza controllo da parte dell'app.

### Formato e separatore

- File testuali `.csv`.
- **Dal 10 febbraio 2026 il MIMIT ha modificato il separatore di campo per “Anagrafica alle 8” e “Prezzi alle 8”: il separatore corrente è `|` (pipe).**
- Il parser non deve assumere in modo cieco la virgola solo perché l'estensione è `.csv`.
- In M3 si dovrà validare l'header effettivo e fallire in modo leggibile se il formato atteso cambia.
- La pagina MIMIT e i metadati ufficiali restano la fonte primaria per verificare schema e semantica delle colonne prima dell'implementazione.

Per il file prezzi, i metadati MIMIT aggiornati indicano almeno le colonne concettuali `idimpianto`, `descCarburante`, `prezzo`, `isSelf`, `dtComu`; la data di estrazione è parte del contesto del dataset.

### Licenza dati

- Dataset MIMIT pubblicato con licenza **IODL 2.0** secondo la pagina ufficiale.
- W2Full deve mantenere un riferimento chiaro alla fonte MIMIT nella documentazione e, se opportuno, nell'interfaccia informativa dell'app.

## 8. CI/CD

### Principi

- GitHub Actions è il sistema CI/CD.
- Nessuna dipendenza da servizi a pagamento.
- Le pipeline devono essere compatibili con una repository pubblica e con i limiti gratuiti applicabili.
- La CI non deve limitarsi a compilare classi: deve generare un **APK reale** nelle milestone con progetto Android.

### Pipeline prevista da M1

A ogni push/PR rilevante:

1. checkout repository;
2. setup JDK/Android toolchain richiesti dal progetto;
3. cache Gradle dove utile;
4. esecuzione test unitari;
5. build debug reale (`assembleDebug` o task equivalente definito dal progetto);
6. verifica presenza dell'APK;
7. upload dell'APK come artifact di workflow.

Per i flussi di release/debug release definiti in M1/M6:

- pubblicazione dell'APK anche tramite GitHub Releases;
- naming chiaro con versione/build/commit quando utile.

### Debug keystore persistente

Obiettivo: permettere l'installazione di un nuovo APK debug sopra il precedente senza disinstallazione, mantenendo la stessa firma.

Strategia prevista:

- generare il debug keystore una sola volta;
- **non** committare il file `.jks`/`.keystore` nel repository;
- codificare il keystore (es. Base64) e salvarlo in un GitHub Actions secret;
- salvare password, alias e password chiave in secrets separati quando necessario;
- ricostruire il file keystore temporaneamente nel runner CI;
- configurare Gradle/signing tramite variabili d'ambiente/secrets;
- cancellazione implicita a fine job con il runner effimero;
- non stampare mai segreti nei log.

I nomi definitivi dei secrets e il flusso esatto verranno documentati in `PROJECT_SPEC.md` **prima** dell'implementazione M1.

## 9. Changelog

### 2026-08-31 — M0 completata

- Repository `archimede-projects/w2full-app` verificata pubblica e scrivibile.
- Aggiunti `PROJECT_SPEC.md`, `README.md`, `LICENSE` MIT e `.gitignore` Android.
- Definito nome progetto/app: **W2Full** e repository `w2full-app`.
- Definiti vincoli di gratuità, assenza account/cloud/backend e distribuzione via APK/GitHub Releases.
- Definita architettura: Compose/Material 3, MVVM + Repository, StateFlow, Room, OkHttp, WorkManager, FusedLocationProviderClient, Haversine, NotificationCompat.
- Definito modello dati concettuale iniziale.
- Definita roadmap M0–M6.
- Verificata nuovamente la fonte ufficiale MIMIT, inclusi gli URL correnti dei CSV e il separatore `|` in vigore dal 10 febbraio 2026 per anagrafica/prezzi alle 8.
- Nessun codice Android, workflow CI o branch temporaneo introdotto in M0.

## 10. Decisioni aperte

Decisioni da prendere solo nella milestone in cui diventano necessarie, aggiornando prima questa spec:

- package name / `applicationId` definitivo di W2Full;
- `minSdk`, `targetSdk` e `compileSdk` definitivi in M1, scegliendo versioni correnti e compatibili con Samsung Galaxy S25;
- strategia esatta di versionamento APK e naming GitHub Releases;
- trigger esatto di pubblicazione Release (tag manuale, workflow_dispatch o altra soluzione gratuita);
- nomi definitivi dei GitHub Actions secrets per il keystore persistente;
- semantica precisa della stima autonomia residua (dipende dai dati disponibili sul livello carburante/serbatoio e dall'UX scelta);
- unità e regole di calcolo del consumo mostrate all'utente (es. km/l e/o l/100 km);
- comportamento dei rifornimenti parziali rispetto ai calcoli medi;
- raggio predefinito per “stazioni vicine” e ordinamento UX;
- gestione permessi posizione: solo durante l'uso vs eventuali esigenze di background, evitando permessi più invasivi se non necessari;
- politica di conservazione dello storico prezzi per contenere dimensione del database;
- scelta definitiva della libreria grafici in M4 dopo verifica di licenza, manutenzione e compatibilità Compose;
- schema dettagliato di esportazione CSV dei rifornimenti in M6.
