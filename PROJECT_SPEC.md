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
In M3 viene introdotta una configurazione Room minima per il singolo veicolo V1, tabella `vehicles`: `id: Long` chiave primaria non autogenerata (V1 usa sempre `1`), `name: String`, `defaultFuelType: String`, `tankCapacityMilliliters: Long?`. La capacità può restare `null` finché non viene configurata; in tal caso l'autonomia residua è intenzionalmente non disponibile. Nessun CRUD multi-veicolo in M3.

### Rifornimento — schema Room M3 definitivo
Tabella: `refuel_entries`.

Campi e mapping SQLite/Room:
- `id: Long` → `INTEGER`, `@PrimaryKey(autoGenerate = true)`;
- `vehicleId: Long` → colonna `vehicle_id INTEGER NOT NULL`, foreign key verso `vehicles.id`, `ON DELETE CASCADE`;
- `timestampEpochMillis: Long` → colonna `timestamp_epoch_millis INTEGER NOT NULL`, istante del rifornimento in epoch milliseconds UTC;
- `odometerKm: Long` → colonna `odometer_km INTEGER NOT NULL`, chilometri totali indicati dal contachilometri;
- `litersMilliliters: Long` → colonna `liters_milliliters INTEGER NOT NULL`, quantità acquistata espressa in millilitri per evitare errori binari di persistenza;
- `totalCostCents: Long` → colonna `total_cost_cents INTEGER NOT NULL`, costo totale in centesimi di euro;
- `fuelType: String` → colonna `fuel_type TEXT NOT NULL`, etichetta carburante normalizzata lato dominio;
- `isFullTank: Boolean` → colonna `is_full_tank INTEGER NOT NULL`, `true` se il serbatoio è stato riportato a pieno dopo quel rifornimento.

Indici non univoci:
- `idx_refuel_vehicle_odometer` su (`vehicle_id`, `odometer_km`);
- `idx_refuel_vehicle_timestamp` su (`vehicle_id`, `timestamp_epoch_millis`).

Vincoli applicativi nel Repository prima di insert/update: `vehicleId > 0`, `timestampEpochMillis > 0`, `odometerKm >= 0`, `litersMilliliters > 0`, `totalCostCents > 0`, `fuelType` non vuoto; ordinando i record dello stesso veicolo per data, l'odometro non può diminuire. Odometraggi uguali restano ammessi. In M3 non vengono memorizzati `pricePerLiter`, note o impianto: il prezzo/litro è derivato da costo/litri; note e collegamento impianto restano estensioni future.

`isFullTank` viene anticipato rispetto alla bozza M0 perché è necessario per calcoli corretti anche in presenza di rifornimenti parziali. Un rifornimento parziale non chiude una finestra di consumo; i suoi litri/costi confluiscono nella successiva finestra pieno→pieno.

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
- [x] Scheletro Android Compose.
- [x] CI con test/build e APK reale.
- [x] Firma debug persistente da secrets.
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
- [x] CRUD rifornimenti + Room.
- [x] Consumo medio, costo/km, autonomia residua.
- [x] JUnit e casi limite.

### Dati MIMIT
- [~] Download + parsing dei due CSV MIMIT con fixture statiche e nessuna dipendenza dalla rete reale nei test CI.
- [ ] Import locale impianti/prezzi.
- [ ] Filtro bandiera Eni.
- [ ] Posizione utente + Haversine.
- [ ] UI stazioni vicine.
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
Stato: **[x] fatto**

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

I quattro repository secret sono stati configurati il **1 settembre 2026** e verificati in due run GitHub Actions indipendenti. I run `33501937187` e `33502077657`, eseguiti rispettivamente sui runner `1000000224` e `1000000225`, hanno entrambi ripristinato il keystore persistente con esito `success`, superato test/build/verifica APK e prodotto lo stesso certificato SHA-256: `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`.

Deliverable:
- [x] scheletro Kotlin + Compose/Material 3 con tema Petrol Night;
- [x] package/applicationId `com.archimede.w2full`;
- [x] Adaptive Icon Android derivata dalla sorgente approvata;
- [x] test JVM minimo;
- [x] workflow GitHub Actions con JDK 17, Gradle 9.5.0 pinning, test, `assembleDebug`, verifica e upload APK;
- [x] firma debug persistente tramite i quattro secret definiti sopra;
- [x] verifica reale di due workflow indipendenti riusciti con APK installabili e identico SHA-256 del certificato persistente.

### M3 — Registro rifornimenti + calcoli
Stato: **[x] fatto**

Decisioni tecniche fissate prima del codice:
- Room `2.8.4` (`androidx.room`) con KSP `2.3.11`; si resta sulla linea Room 2.x stabile per questa milestone Android-only invece di introdurre contemporaneamente la migrazione a Room 3;
- Lifecycle `2.11.0` per ViewModel/Compose;
- database `W2FullDatabase`, versione schema `1`;
- `versionCode 2`, `versionName 0.2.0-m3`;
- singleton veicolo V1 con `vehicleId = 1`;
- denaro e carburante persistiti come interi (`cent`, `millilitri`), conversione in `Double` solo nel dominio/UI;
- CRUD DAO asincrono con `suspend` e lista osservabile con `Flow`;
- Repository responsabile della validazione dei record e dell'inizializzazione del veicolo singleton;
- test JVM dei calcoli puri e test CRUD Room su database in-memory tramite Robolectric, senza introdurre test device/emulatore in M3.

Formule M3. I record sono ordinati in modo deterministico per `odometerKm`, poi `timestampEpochMillis`, poi `id`.

**Prezzo unitario derivato** per un rifornimento:
`pricePerLiterEuro = (totalCostCents / 100.0) / (litersMilliliters / 1000.0)`.

**Finestra valida consumo/costo**: servono almeno due rifornimenti con `isFullTank = true`. Sia `F0` il primo pieno e `Fn` l'ultimo pieno della finestra. Tutti i rifornimenti dopo `F0` e fino a `Fn` compreso — inclusi eventuali parziali — rappresentano il carburante reintegrato/consumato nella distanza chiusa. Il pieno iniziale `F0` è escluso dalla somma dei litri/costi perché rappresenta l'inventario di partenza.

`distanceKm = Fn.odometerKm - F0.odometerKm`

`consumedLiters = sum(litersMilliliters dei record (F0, Fn]) / 1000.0`

`averageConsumptionLPer100Km = consumedLiters / distanceKm * 100.0`

`consumedCostEuro = sum(totalCostCents dei record (F0, Fn]) / 100.0`

`costPerKmEuro = consumedCostEuro / distanceKm`

Se ci sono meno di due pieni, `distanceKm <= 0` o litri validi assenti, consumo medio e costo/km sono `null`/non disponibili anziché inventare valori.

**Autonomia residua stimata**: è riferita all'ultimo odometro noto nel registro, non a chilometri percorsi dopo l'ultimo record. Richiede: capacità serbatoio configurata `tankCapacityLiters > 0`, consumo medio valido e almeno un pieno che faccia da ancora. Sia `F` l'ultimo record con `isFullTank = true`; siano `P` i soli rifornimenti parziali successivi a `F`; sia `K` l'odometro massimo dei record da `F` in poi.

`distanceSinceFullKm = K - F.odometerKm`

`estimatedConsumedSinceFullLiters = distanceSinceFullKm * averageConsumptionLPer100Km / 100.0`

`partialAddedLiters = sum(P.litersMilliliters) / 1000.0`

`estimatedRemainingLiters = clamp(tankCapacityLiters - estimatedConsumedSinceFullLiters + partialAddedLiters, 0.0, tankCapacityLiters)`

`estimatedRangeKm = estimatedRemainingLiters / averageConsumptionLPer100Km * 100.0`

Se capacità, consumo medio o pieno-ancora mancano, autonomia e litri residui sono `null`. Il `clamp` impedisce stime fisicamente superiori alla capacità o negative.

Sotto-passaggi M3:
1. **M3.1 — nucleo dati/calcoli**: entità Room, DAO, database, Repository, motore formule e test JVM/Room; nessuna UI CRUD nuova finché questo blocco non è verde in CI.
2. **M3.2 — registro Compose**: una sola schermata Registro con riepilogo metriche + lista; inserimento/modifica tramite dialog, eliminazione con conferma e configurazione capacità serbatoio tramite dialog. Non si introduce Navigation né una seconda schermata in M3.
3. **M3.3 — verifica/chiusura**: build reale su `main`, APK firmato con keystore persistente, aggiornamento finale della spec e cleanup branch temporaneo.

Deliverable:
- [x] Room schema v1 con `VehicleEntity` e `RifornimentoEntity` secondo lo schema sopra;
- [x] DAO/Repository CRUD e validazioni;
- [x] consumo medio, costo/km e autonomia residua con rifornimenti parziali gestiti pieno→pieno;
- [x] test JVM dei casi normali e limite + CRUD Room in-memory;
- [x] schermata Compose Registro con add/edit/delete e capacità serbatoio;
- [x] CI reale e APK M3 aggiornabile con la chiave persistente già verificata in M2.

Verifica finale M3 su `main`: il run GitHub Actions `33527223004`, head SHA `14df6c15d25cec8d7f5fcdfb0b24e2daac4dda50`, ha completato con successo ripristino del keystore persistente, `testDebugUnitTest`, `assembleDebug`, verifica `apksigner` e upload dell'artifact `w2full-debug-apk` ID `9808283878` (12.507.371 byte, digest ZIP `sha256:ed7a8bbfdbf60572a92988b8222721a965a916a6acaed6082417d911b622b550`). Il certificato APK SHA-256 è `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`, identico a M2 e ai build verificati sul branch M3.

### M4 — Integrazione dati MIMIT
Stato: **[~] in corso**

Verifica esterna obbligatoria rifatta il **1 settembre 2026**, prima di qualunque codice M4:
- la pagina ufficiale MIMIT continua a esporre i download `https://www.mimit.gov.it/images/exportCSV/prezzo_alle_8.csv` e `https://www.mimit.gov.it/images/exportCSV/anagrafica_impianti_attivi.csv`; i link raggiungono risorse `text/csv`;
- il metadato ufficiale attualmente collegato è `Metadati_prezzi_carburanti_20260128.pdf`, indicato dalla pagina come versione in vigore dal 10 febbraio 2026;
- il delimitatore ufficiale resta `|` e i numeri usano il punto come separatore decimale;
- colonne prezzi secondo il metadato corrente: `idimpianto`, `descCarburante`, `prezzo`, `isSelf`, `dtComu`;
- colonne anagrafica secondo il metadato corrente: `idimpianto`, `Gestore`, `Bandiera`, `Tipo Impianto`, `Nome Impianto`, `Indirizzo`, `Comune`, `Provincia`, `Latitudine`, `Longitudine`; l'asterisco mostrato nel PDF accanto a Latitudine/Longitudine è un richiamo alla nota sulle coordinate volontarie, non viene trattato come parte del nome logico della colonna;
- non sono emerse differenze sostanziali rispetto agli URL/formato già annotati: la differenza documentale rilevante è che il metadato corrente scrive `idimpianto` tutto minuscolo, mentre dataset/esempi storici mostrano anche `idImpianto`; il parser M4 deve quindi validare i nomi colonna senza distinzione di maiuscole/minuscole, mantenendo invece rigorosi numero e significato delle colonne.

Decisioni tecniche M4.1 — download/parsing, da implementare prima di Eni/posizione/UI:
- OkHttp `5.5.0` per il download HTTPS; nessun download MIMIT reale viene eseguito nei test CI;
- `versionCode 3`, `versionName 0.3.0-m4`;
- endpoint centralizzati in una classe/oggetto MIMIT dedicato, senza URL sparsi nella UI;
- parser pipe-delimited separato dal client HTTP e testabile da `Reader`/testo statico;
- supporto al preambolo di estrazione (`Estrazione del ...`) e individuazione/validazione dell'header atteso;
- campioni statici ridotti, sintetici e aderenti al contratto corrente sotto `app/src/test/resources/mimit/`; i campioni non devono essere copie massive del dataset reale;
- parser con supporto a campi racchiusi tra doppi apici e escaping `""`, così separatori o virgolette nei valori non rompono la riga;
- `idimpianto` → `Long`; `prezzo` → millesimi di euro per unità (`Long`) per preservare le 3 cifre decimali; `isSelf` → `Boolean`; `dtComu` → `LocalDateTime` nel formato `dd/MM/yyyy HH:mm:ss`;
- coordinate anagrafica nullable: i metadati dichiarano che sono volontarie e non sempre verificate; valori vuoti restano `null` senza scartare l'impianto;
- i test del download usano MockWebServer `5.5.0` alimentato dalle fixture statiche locali, così verificano HTTP + parsing senza dipendere da MIMIT o Internet.

Sotto-passaggi M4, ciascuno con CI reale sul proprio branch prima di integrazione:
1. **M4.1 — download/parsing CSV**: OkHttp, parser dei due formati, DTO MIMIT, fixture statiche e test JVM/MockWebServer. Fermarsi dopo il verde e attendere conferma utente.
2. **M4.2 — filtro bandiera Eni**: normalizzazione/filtro bandiera e test dedicati.
3. **M4.3 — posizione e distanza**: permessi minimali, posizione utente e Haversine, con test della formula indipendenti dalla posizione reale.
4. **M4.4 — UI stazioni vicine**: stato ViewModel/Repository e schermata Compose; nessun ampliamento a storico/notifiche.
5. **M4.5 — import/sync**: persistenza dei dati necessari, refresh manuale e WorkManager giornaliero, solo dopo conferma dei passaggi precedenti.

Deliverable M4.1:
- [ ] client HTTPS per i due endpoint MIMIT;
- [ ] parser anagrafica e prezzi secondo il contratto sopra;
- [ ] DTO indipendenti da Room/UI;
- [ ] fixture statiche sotto `app/src/test/resources/mimit/`;
- [ ] test JVM parser + MockWebServer senza Internet;
- [ ] CI branch reale con test, APK e firma persistente verdi.

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

Direzione: scura, tecnica, automotive e data-centric. **Petrol Night è la variante predefinita approvata per i mockup e fungerà da riferimento iniziale per la UI Compose.**

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

URL ricontrollati dal vivo il **1 settembre 2026** sulla pagina ufficiale:
- `https://www.mimit.gov.it/images/exportCSV/prezzo_alle_8.csv`
- `https://www.mimit.gov.it/images/exportCSV/anagrafica_impianti_attivi.csv`

I link correnti raggiungono risorse `text/csv`. Il metadato ufficiale collegato dalla stessa pagina è `https://www.mimit.gov.it/images/stories/documenti/Metadati_prezzi_carburanti_20260128.pdf`, versione indicata come in vigore dal 10 febbraio 2026.

Dal **10 febbraio 2026** il separatore per “Anagrafica alle 8” e “Prezzi alle 8” è `|`. Il metadato corrente conferma numeri in formato internazionale con `.` decimale. Header logici attesi: prezzi `idimpianto|descCarburante|prezzo|isSelf|dtComu`; anagrafica `idimpianto|Gestore|Bandiera|Tipo Impianto|Nome Impianto|Indirizzo|Comune|Provincia|Latitudine|Longitudine`. La validazione dell'header M4 è case-insensitive per assorbire la variante storica `idImpianto` senza accettare colonne mancanti o semanticamente diverse. Licenza dati: IODL 2.0 secondo la pagina ufficiale.

## 9. CI/CD

Da M2: GitHub Actions con checkout, JDK 17, Gradle 9.5.0 installato e pinning tramite `gradle/actions/setup-gradle@v4`, test JVM, build `assembleDebug`, verifica dell'APK e upload artifact. Non viene committato un Gradle Wrapper binario in M2: il runner usa la distribuzione Gradle fissata dalla pipeline, evitando un JAR wrapper generato o trasferito fuori dal normale flusso sorgente.

Toolchain M2/M3/M4: AGP 9.3.0 + Gradle 9.5.0 + `compileSdk/targetSdk 37`. Non si fissa manualmente `buildToolsVersion`: viene usata la versione predefinita compatibile con AGP.

La firma debug persistente usa un keystore generato una sola volta e conservato esclusivamente come Base64 in `W2FULL_DEBUG_KEYSTORE_BASE64`; le password e l'alias sono separati nei secret `W2FULL_DEBUG_KEYSTORE_PASSWORD`, `W2FULL_DEBUG_KEY_ALIAS`, `W2FULL_DEBUG_KEY_PASSWORD`. Il workflow ricostruisce il file sotto `$RUNNER_TEMP`, lo usa per la `signingConfig` debug e lo elimina a fine job. Nessun contenuto dei secret va stampato nei log o committato.

La firma persistente è stata verificata il **1 settembre 2026** su due runner GitHub Actions distinti. I run `33501937187` e `33502077657` hanno entrambi eseguito con successo il ripristino del keystore, `testDebugUnitTest`, `assembleDebug`, `apksigner verify` e upload dell'artifact `w2full-debug-apk`. Entrambi gli APK riportano certificato `CN=W2Full Debug, OU=Personal, O=Archimede Projects, C=IT` con SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`, confermando che gli APK futuri firmati con questi secret sono aggiornabili senza cambio chiave.

M3 ha ripetuto la verifica sul codice completo direttamente su `main` nel run `33527223004`: test JVM/Room/parser, build debug, `apksigner` e artifact sono tutti riusciti con lo stesso certificato persistente SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`.

Per M4 ogni sotto-passaggio usa un branch dedicato e deve completare la stessa pipeline reale prima di qualsiasi integrazione. I test MIMIT non devono effettuare richieste alla rete pubblica: usano fixture statiche e, quando serve verificare il client HTTP, un server locale di test.

## 10. Changelog

### 2026-09-01 — M4 avviata: contratto CSV MIMIT riverificato
- Prima del codice M4 sono stati ricontrollati dal vivo pagina dataset, link CSV e metadato MIMIT corrente: gli URL restano invariati e puntano a risorse `text/csv`.
- Confermati separatore `|`, formato numerico internazionale e colonne documentate dal metadato in vigore dal 10 febbraio 2026.
- Esplicitata la normalizzazione case-insensitive dell'header per la sola variante `idimpianto`/`idImpianto`, senza rilassare il controllo sulle colonne richieste.
- M4 suddivisa in M4.1 download/parsing, M4.2 filtro Eni, M4.3 posizione/Haversine, M4.4 UI stazioni vicine e M4.5 import/sync.
- Per M4.1 scelti OkHttp/MockWebServer 5.5.0 e fixture statiche locali; i test CI non contatteranno MIMIT.
- M4 marcata in corso; autorizzato esclusivamente M4.1 fino a nuova conferma utente.

### 2026-09-01 — M3 completata
- Integrato su `main` con fast-forward puro il codice M3 già verificato sul branch `m3-refueling-register`; commit applicativo finale prima della chiusura spec: `14df6c15d25cec8d7f5fcdfb0b24e2daac4dda50`.
- Completati Room schema v1, CRUD Repository, formule di consumo/costo/autonomia, gestione pieno→pieno con rifornimenti parziali, test JVM/Room e schermata Compose Registro a singola schermata con dialog add/edit/delete e capacità serbatoio.
- Run finale diretto su `main`: `33527223004`, job `99921047840`, tutti gli step obbligatori `success`.
- Artifact finale `w2full-debug-apk`: ID `9808283878`, dimensione `12507371` byte, digest ZIP `sha256:ed7a8bbfdbf60572a92988b8222721a965a916a6acaed6082417d911b622b550`.
- `apksigner` ha verificato una firma v2 con un solo signer e certificato SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`, identico alla chiave persistente M2.
- M3 chiusa; M4 resta esplicitamente da fare e non è stata avviata.

### 2026-09-01 — M3 avviata: schema Room e formule fissati
- M3 marcata in corso prima di modificare codice/configurazione Android.
- Fissato lo schema Room v1 di `vehicles` e `refuel_entries`, con unità persistenti intere per millilitri e centesimi.
- Promosso `isFullTank` a campo M3 per supportare correttamente rifornimenti parziali e finestre pieno→pieno.
- Fissate le formule per consumo medio, costo/km, carburante residuo stimato e autonomia all'ultimo odometro noto.
- Scelti Room 2.8.4, KSP 2.3.11 e Lifecycle 2.11.0; previsto CRUD Room verificato anche con test JVM in-memory.
- M3 suddivisa in nucleo dati/calcoli, UI Registro singola schermata e verifica/chiusura.

### 2026-09-01 — M2 completata
- Completato lo scheletro Android nativo Kotlin + Jetpack Compose + Material 3 con `applicationId` `com.archimede.w2full` e tema Petrol Night.
- Integrata l'Adaptive Icon derivata dal master M1 e aggiunto il test JVM minimo.
- Pipeline Android CI verificata con build reale, `apksigner` e artifact APK.
- Configurato e verificato il keystore debug persistente tramite i quattro GitHub Actions Secrets, senza chiavi nel repository.
- Run indipendenti `33501937187` e `33502077657` riusciti su runner distinti con identico certificato SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`.
- Artifact `w2full-debug-apk` prodotti correttamente; M2 chiusa senza avviare M3.

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
- raggio e ordinamento stazioni;
- permessi posizione minimali;
- retention storico prezzi;
- libreria grafici definitiva in M5;
- schema CSV in M7;