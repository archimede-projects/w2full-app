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
- [x] Download + parsing dei due CSV MIMIT con fixture statiche e nessuna dipendenza dalla rete reale nei test CI.
- [~] Import locale impianti/prezzi.
- [x] Filtro bandiera Eni.
- [x] Posizione utente + Haversine.
- [x] UI stazioni vicine.
- [~] Refresh manuale + WorkManager giornaliero.

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

Decisioni tecniche M4.1 — download/parsing:
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

Checkpoint M4.1 confermato il **2 settembre 2026**: commit `256060bef4e6b2af43c274bf810d6f7d4513add2` sul branch `m4-mimit-csv`; run GitHub Actions `33530349742`, job `99931636090`, tutti gli step obbligatori `success`; artifact `w2full-debug-apk` ID `9809511985`, dimensione `13218479` byte, digest ZIP `sha256:6f6bb2f72895cc9e9fc2f55d84a8de19c2edd3fa9bf67e3d8a31152cc0ea258a`; certificato persistente SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`. Il formato live MIMIT è stato inoltre ricontrollato dall'utente il 2 settembre 2026 ed è risultato coerente con header e formato implementati in M4.1.

Decisioni tecniche M4.2 — filtro bandiera Eni:
- il filtro opera esclusivamente sui `MimitStation` già scaricati e parsati da M4.1; nessuna rete, Room, cache, Repository applicativo, posizione o UI viene introdotta in M4.2;
- la bandiera viene normalizzata con `trim`, compressione degli spazi interni e confronto case-insensitive via `Locale.ROOT`;
- è considerata Eni soltanto la bandiera normalizzata esattamente uguale a `eni`; non si usa `contains`, così valori diversi che contengono la sottostringa `eni` non vengono classificati per errore;
- il filtro preserva l'ordine originale dei record e, quando applicato a `MimitDataset<MimitStation>`, preserva anche `extractionDate`;
- test dedicati devono coprire `Eni`, varianti di maiuscole/minuscole e spazi, insieme a non-match come `Q8`, `Pompe Bianche`, stringa vuota e valori contenenti ma non uguali a `Eni`.

Verifica M4.2 sul branch `m4-eni-filter`: commit applicativo `997b9ccca6b1be962751fdb3b8ece048983438b4`; run GitHub Actions `33596403891`, job `100140620391`, tutti gli step obbligatori `success`; `testDebugUnitTest` e `assembleDebug` entrambi `BUILD SUCCESSFUL`; artifact `w2full-debug-apk` ID `9833570963`, dimensione `13219188` byte, digest ZIP `sha256:8963c06b15cf9b97b1606e2d9dbde50aeb816c424e9bc4ffd75378cdc1bd5549`; firma APK v2 con un signer e certificato SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`, identico a M2/M3/M4.1. Il checkpoint M4.2 è stato confermato dall'utente il **2 settembre 2026** dopo verifica anche del confronto esatto della bandiera `eni`.

Decisioni tecniche M4.3 — posizione utente e distanza:
- dipendenza Google Play services Location `21.4.0`, versione stabile corrente verificata sulla documentazione ufficiale il 2 settembre 2026; accesso tramite `FusedLocationProviderClient`, senza introdurre servizi a pagamento o backend;
- il manifest dichiara `ACCESS_COARSE_LOCATION` e `ACCESS_FINE_LOCATION`; M4.3 non introduce ancora UI né dialog di richiesta permesso: il flusso di richiesta esplicita resta a M4.4, mentre il provider M4.3 deve funzionare correttamente quando uno dei due permessi è già concesso;
- il provider restituisce un risultato tipizzato: `Available(GeoPoint)`, `PermissionDenied` oppure `Unavailable`; permesso negato/revocato, location `null`, Google Play services non disponibile o errore del provider non devono causare crash;
- la posizione viene richiesta con `FusedLocationProviderClient.getCurrentLocation` a priorità bilanciata; la cancellazione coroutine deve cancellare anche la richiesta sottostante e non viene trasformata in errore applicativo;
- `GeoPoint` valida coordinate finite con latitudine `[-90, 90]` e longitudine `[-180, 180]`;
- distanza geodetica calcolata con Haversine usando raggio terrestre medio `6371.0088 km`: `a = sin²(Δφ/2) + cos φ1 * cos φ2 * sin²(Δλ/2)`, `c = 2 * atan2(√a, √(1-a))`, `distanceKm = R * c`;
- il ranking opera sulle sole stazioni Eni ottenute tramite `MimitStationFilter`; con posizione disponibile, stazioni con coordinate valide sono ordinate per distanza crescente, poi deterministicamente per nome/comune/indirizzo/id; stazioni con coordinate mancanti o invalide hanno `distanceKm = null` e vengono poste dopo quelle localizzabili;
- senza posizione (`PermissionDenied` o `Unavailable`) nessuna distanza viene inventata: tutte le distanze restano `null` e le stazioni Eni vengono ordinate deterministicamente in modo alfabetico per nome (con fallback indirizzo/comune/id);
- il livello di servizio M4.3 deve intercettare errori non di cancellazione provenienti dal provider e degradare a `Unavailable`, così un problema di localizzazione non interrompe l'app;
- test JVM obbligatori: Haversine stesso punto/rotta nota/simmetria; coordinate station mancanti/invalide; ranking per distanza; tie/fallback alfabetico; permesso negato; posizione non disponibile; provider che fallisce; esclusione delle stazioni non Eni.

Verifica M4.3 sul branch `m4-location-distance`: HEAD applicativo/CI `872fc1b0f180a77838c7ec4a5ac75b817aefa7a6`; run GitHub Actions `33597805710`, job `100144721666`, tutti gli step obbligatori `success`; `testDebugUnitTest` e `assembleDebug` entrambi `BUILD SUCCESSFUL`; artifact `w2full-debug-apk` ID `9834047769`, dimensione `13799932` byte, digest ZIP `sha256:864421ad44b5379d6c897dd33dac9a509c8a9db77965f4f382c085bac22a80a4`; firma APK v2 con un signer e certificato SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`, identico alle milestone precedenti. Il codice M4.3 resta confinato al branch e non è integrato su `main`.

Il checkpoint M4.3 è stato confermato dall'utente il **2 settembre 2026** dopo verifica del codice, del manifest con soli permessi foreground e ricalcolo indipendente Roma–Milano pari a circa `476,9 km`.

Decisioni tecniche M4.4 — UI stazioni vicine:
- viene introdotta una navigazione Compose minima a due destinazioni, `Registro` e `Stazioni`, senza aggiungere una libreria Navigation: lo stato di destinazione resta locale all'app shell e M4.4 non modifica il flusso CRUD M3;
- la schermata `Stazioni` usa un `NearbyStationsViewModel` e un repository temporaneo **non persistente**: in produzione scarica l'anagrafica MIMIT via `MimitCsvClient` su dispatcher IO e passa i record al ranking M4.3; non scrive Room, file o cache e non introduce WorkManager;
- i test M4.4 non effettuano download MIMIT reali: ViewModel/repository sono testati con sorgenti/fake in memoria o fixture statiche già presenti;
- la route Compose richiede runtime `ACCESS_COARSE_LOCATION` e `ACCESS_FINE_LOCATION` tramite Activity Result API quando nessuno dei due è già concesso; dopo il risultato, anche un rifiuto avvia comunque il caricamento delle stazioni, così `PermissionDenied` usa il fallback alfabetico M4.3 invece di bloccare la lista;
- se la posizione è disponibile, la UI mostra stato `Posizione disponibile` e la lista Eni ordinata per distanza crescente; se il permesso è negato mostra stato `Permesso posizione negato` e lista alfabetica; se il provider restituisce `Unavailable` mostra `Posizione non disponibile` e lista alfabetica. Nessuno dei tre stati deve causare crash;
- quando il permesso è negato, la schermata espone un'azione esplicita per richiedere nuovamente il permesso; non esiste loop automatico di richieste dopo un rifiuto;
- ogni riga stazione mostra almeno nome (o fallback leggibile), indirizzo/comune/provincia e distanza formattata quando presente; con `distanceKm = null` mostra `Distanza non disponibile`;
- lo stato UI include già `lastSuccessfulUpdateEpochMillis: Long?` per il requisito M4.5. In M4.4 resta `null` perché non esiste ancora cache persistente e la schermata riserva comunque lo spazio con testo placeholder `Ultimo aggiornamento: non ancora disponibile`; M4.5 sostituirà il placeholder con l'età derivata dal timestamp persistito;
- errori di download/parsing nella sessione M4.4 diventano uno stato UI non tecnico (`Impossibile caricare le stazioni al momento`) e non provocano crash; la resilienza con cache valida resta responsabilità M4.5;
- M4.4 non introduce prezzi persistiti, import locale, schema Room, cache, `lastSuccessfulUpdateEpochMillis` persistito, refresh schedulato o WorkManager.

Verifica M4.4 sul branch `m4-nearby-stations-ui`: HEAD applicativo/CI `66acaf957c15782b0251d2ab2455788dcb56a5a0`; run GitHub Actions `33601057414`, job `100154609063`, tutti gli step obbligatori `success`; `testDebugUnitTest` = `BUILD SUCCESSFUL in 57s`, `assembleDebug` = `BUILD SUCCESSFUL in 29s`; artifact `w2full-debug-apk` ID `9835232992`, dimensione `13847203` byte, digest ZIP `sha256:e55e6d03e7c4ac5df84404d49811f739e922455abfbfe19bcb7232e0219246c4`; firma APK v2 con un signer e certificato SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`, identico alle milestone precedenti. Il primo run M4.4 `33600888882` aveva correttamente bloccato il checkpoint in compilazione per l'uso dell'import Compose `weight`; il fix `66acaf957c15782b0251d2ab2455788dcb56a5a0` ha eliminato tale API e la CI successiva è interamente verde. Nessun dato MIMIT è persistito in M4.4.

Il checkpoint M4.4 è stato confermato dall'utente il **2 settembre 2026** dopo verifica del codice reale, inclusa la navigazione locale `Registro` / `Stazioni` senza libreria Navigation e la gestione trasparente del primo run fallito/corretto.

Decisioni tecniche M4.5 — cache/import/sync resiliente:
- Room passa da schema `1` a schema `2` con migrazione esplicita `MIGRATION_1_2`; la migrazione crea soltanto le nuove tabelle MIMIT e non ricrea né modifica `vehicles` o `refuel_entries`, così i dati M3 devono restare integralmente preservati;
- nuove tabelle cache: `mimit_stations` per le stazioni Eni filtrate, `mimit_prices` per le sole righe prezzo riferite agli ID Eni cached, e singleton `mimit_sync_state` (`id = 1`) con date estrazione anagrafica/prezzi e `last_successful_update_epoch_millis`;
- la cache M4.5 è uno snapshot locale dell'ultimo import valido, non lo storico prezzi M5: `mimit_prices` rappresenta il CSV corrente associato alle stazioni Eni, mentre la retention storica resta una decisione separata di M5;
- ogni refresh usa la stessa pipeline per UI manuale e background: download anagrafica → parsing → filtro Eni → download prezzi → parsing → filtro prezzi per ID Eni → validazione completa → preparazione entità → singola transazione Room di sostituzione;
- prima di aprire la transazione devono essere completati entrambi i download e parsing. La validazione rifiuta dataset anagrafica/prezzi vuoti, insieme Eni vuoto, prezzi Eni vuoti e ID stazione Eni duplicati; un input non valido è trattato come failure e non tocca la cache precedente;
- la sostituzione è atomica con `withTransaction`: delete delle sole tabelle cache MIMIT, insert del nuovo snapshot e upsert di `mimit_sync_state` avvengono nella stessa transazione. `lastSuccessfulUpdateEpochMillis` viene scritto come ultimo stato logico del commit e solo dopo che l'intero snapshot è pronto;
- qualunque errore di rete/HTTP, `MimitCsvFormatException`, header cambiato, parsing, validazione o persistenza viene intercettato al confine repository; `CancellationException` resta propagata. Su failure non viene eseguita alcuna sostituzione e cache/timestamp precedenti restano invariati;
- il repository restituisce un outcome tipizzato `Success` / `Failure`; il dettaglio tecnico dell'errore viene inviato a un logger iniettabile. In produzione il logger usa `android.util.Log` con tag fisso `W2Full-MIMIT`, messaggio tecnico e throwable/stack trace; la UI riceve soltanto il messaggio generico `Impossibile aggiornare i prezzi al momento`;
- la UI osserva la cache Room: dati cached già validi restano visibili durante un refresh e dopo un refresh fallito. Senza cache valida viene mostrato uno stato vuoto/errore comprensibile, senza crash;
- `lastSuccessfulUpdateEpochMillis` viene esposto alla UI dalla cache Room. Quando presente, la schermata mostra sempre età relativa più timestamp assoluto locale: meno di 60 minuti → `Aggiornato pochi minuti fa`; da 1 a 23 ore → `Aggiornato X ore fa`; da 24 ore in poi → `Aggiornato X giorni fa`; seguito da data/ora assoluta `dd/MM/yyyy HH:mm`. Se non esiste ancora alcun import valido resta il placeholder `Ultimo aggiornamento: non ancora disponibile`;
- il pulsante `Aggiorna` della schermata invoca lo stesso `refresh()` atomico usato dal worker; non esiste un percorso manuale che scriva direttamente la cache;
- WorkManager `2.11.2`: `CoroutineWorker` dedicato e unique periodic work giornaliero con `NetworkType.CONNECTED`, policy `KEEP`; il worker usa lo stesso repository `refresh()`. Un errore handled non cancella né svuota la cache; gli errori di rete possono richiedere retry con backoff, mentre errori di formato restano diagnosticati e verranno ritentati al ciclo periodico successivo;
- lo scheduling periodico viene inizializzato dall'Application senza richiedere permessi di posizione/background location; la posizione resta foreground M4.3/M4.4 e il worker importa/cache-a soltanto i dataset MIMIT;
- tutti i test di rete continuano a usare MockWebServer/fixture locali: nessuna richiesta MIMIT reale in CI.

Requisiti vincolanti M4.5 — resilienza import/cache/sync:
- ogni refresh è **atomico**: i dati cached vengono sostituiti soltanto dopo download, parsing, validazione e preparazione dell'intero aggiornamento completati con successo; un fallimento parziale non deve mai cancellare o corrompere l'ultima cache valida;
- errori HTTP/rete, `MimitCsvFormatException`, colonne mancanti/impreviste o qualunque formato MIMIT inatteso devono essere intercettati al confine Repository/sync: l'app non deve crashare e deve continuare a usare gli ultimi dati validi disponibili;
- in caso di errore con cache esistente, la UI mostra un messaggio non bloccante e non tecnico, `Impossibile aggiornare i prezzi al momento`, continuando a visualizzare i dati cached; senza cache valida viene mostrato uno stato vuoto/errore comprensibile, sempre senza crash;
- la persistenza include `lastSuccessfulUpdateEpochMillis`, aggiornato **solo** dopo un import completamente riuscito; un tentativo fallito non modifica questo timestamp;
- la UI stazioni mostra sempre l'età dell'ultimo aggiornamento riuscito quando esiste una cache e rende disponibile anche data/ora assoluta;
- l'errore tecnico specifico viene registrato localmente in Logcat con tag `W2Full-MIMIT`, includendo exception/stack trace quando disponibile, ma dettagli tecnici e stack trace non vengono esposti all'utente;
- test M4.5 obbligatori: CSV valido → cache aggiornata; header cambiato → errore intercettato; parse fallito → cache invariata; download fallito → cache invariata; refresh fallito → timestamp invariato; refresh riuscito → timestamp aggiornato; dati cached + errore → UI continua a mostrare i dati; UI espone sempre l'età dell'ultimo aggiornamento quando il timestamp esiste; errore tecnico raggiunge il logger ma non il messaggio utente; migrazione Room 1→2 preserva i dati M3; worker/manual refresh condividono la stessa semantica repository.

Sotto-passaggi M4, ciascuno con CI reale sul proprio branch prima di integrazione:
1. **M4.1 — download/parsing CSV**: OkHttp, parser dei due formati, DTO MIMIT, fixture statiche e test JVM/MockWebServer. **[x] checkpoint confermato**.
2. **M4.2 — filtro bandiera Eni**: normalizzazione/filtro bandiera e test dedicati. **[x] checkpoint confermato**.
3. **M4.3 — posizione e distanza**: provider posizione resiliente, Haversine e ordinamento delle sole stazioni Eni; nessuna UI/cache/import. **[x] checkpoint confermato**.
4. **M4.4 — UI stazioni vicine**: richiesta permesso runtime, stato ViewModel/repository non persistente, lista Compose Eni con ranking M4.3 e spazio `ultimo aggiornamento`; nessuna cache/Room/WorkManager. **[x] checkpoint confermato**.
5. **M4.5 — import/sync resiliente**: persistenza, cache atomica, `lastSuccessfulUpdateEpochMillis`, logging `W2Full-MIMIT`, refresh manuale e WorkManager giornaliero secondo i requisiti vincolanti sopra. **[~] autorizzato**.

Deliverable M4.1:
- [x] client HTTPS per i due endpoint MIMIT;
- [x] parser anagrafica e prezzi secondo il contratto sopra;
- [x] DTO indipendenti da Room/UI;
- [x] fixture statiche sotto `app/src/test/resources/mimit/`;
- [x] test JVM parser + MockWebServer senza Internet;
- [x] CI branch reale con test, APK e firma persistente verdi.

Deliverable M4.2:
- [x] filtro puro sui `MimitStation` parsati;
- [x] normalizzazione robusta ma confronto semanticamente esatto con `Eni`;
- [x] preservazione ordine e `extractionDate`;
- [x] test JVM positivi/negativi e casi di normalizzazione;
- [x] CI reale sul branch M4.2 con test, APK e firma persistente verdi;
- [x] nessuna modifica a cache/persistenza/UI/posizione.

Deliverable M4.3:
- [x] `GeoPoint` + Haversine puro e validato;
- [x] provider `FusedLocationProviderClient` con esiti `Available` / `PermissionDenied` / `Unavailable` e nessun crash su errore;
- [x] ranking delle sole stazioni Eni per distanza, con coordinate mancanti/invalide gestite come distanza non disponibile;
- [x] fallback alfabetico deterministico quando la posizione non è disponibile o il permesso è negato;
- [x] test JVM dei calcoli, ranking e fallback senza dipendenza da posizione reale;
- [x] CI reale sul branch M4.3 con test, APK e firma persistente verdi;
- [x] nessuna modifica a UI, cache/Room, import o WorkManager.

Deliverable M4.4:
- [x] navigazione minima `Registro` / `Stazioni` senza dipendenza Navigation aggiuntiva;
- [x] richiesta runtime COARSE/FINE con gestione esplicita disponibile/negato/non disponibile e possibilità di riprovare dopo rifiuto;
- [x] `NearbyStationsViewModel` + repository sessione non persistente, senza Room/cache/WorkManager;
- [x] lista Eni coerente con ranking M4.3: distanza crescente quando disponibile, fallback alfabetico altrimenti;
- [x] stato/lista resilienti a permesso negato, provider unavailable e download fallito, senza crash;
- [x] spazio `Ultimo aggiornamento` già presente con `lastSuccessfulUpdateEpochMillis` nullable e placeholder finché M4.5 non persiste il dato;
- [x] test JVM pertinenti senza rete pubblica;
- [x] CI reale sul branch M4.4 con test, APK e firma persistente verdi;
- [x] nessuna modifica a schema Room/cache/import persistente/WorkManager.

Deliverable M4.5:
- [ ] schema Room v2 con cache stazioni/prezzi MIMIT + sync state e migrazione 1→2 che preserva i dati M3;
- [ ] import in memoria dei due CSV, filtro Eni/prezzi associati, validazione e sostituzione cache in singola transazione atomica;
- [ ] failure rete/formato/parsing/validazione/persistenza non distruttiva, senza crash e con cache/timestamp precedenti invariati;
- [ ] `lastSuccessfulUpdateEpochMillis` persistito e aggiornato esclusivamente su import riuscito;
- [ ] logger locale `W2Full-MIMIT` con causa tecnica/throwable separato dal messaggio utente;
- [ ] UI cache-first con messaggio refresh non bloccante, pulsante manuale `Aggiorna`, dati cached preservati su errore e ultimo aggiornamento relativo + assoluto;
- [ ] WorkManager 2.11.2 con unique periodic work giornaliero e vincolo rete, usando lo stesso `refresh()` del repository;
- [ ] test obbligatori di atomicità/cache/timestamp/header/parse/download/UI/logger/migrazione/worker senza Internet reale;
- [ ] CI reale sul branch M4.5 con test, APK e firma persistente verdi;
- [ ] nessuna integrazione su `main` prima della conferma esplicita dell'utente.

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

La firma debug persistente usa un keystore generato una sola volta e conservato esclusivamente come Base64 in `W2FULL_DEBUG_KEYSTORE_BASE64`; le password e l'alias sono separati nei secret `W2FULL_DEBUG_KEYSTORE_PASSWORD`, `W2FULL_DEBUG_KEY_ALIAS`, `W2FULL_DEBUG_KEY_PASSWORD`. Il workflow ricostruisce il file sotto `$RUNNER_TEMP`, lo usa per la `signingConfig` debug e lo elimina a fine job. Nessun contenuto dei secret va stampato o committato.

La firma persistente è stata verificata il **1 settembre 2026** su due runner GitHub Actions distinti. I run `33501937187` e `33502077657` hanno entrambi eseguito con successo il ripristino del keystore, `testDebugUnitTest`, `assembleDebug`, `apksigner verify` e upload dell'artifact `w2full-debug-apk`. Entrambi gli APK riportano certificato `CN=W2Full Debug, OU=Personal, O=Archimede Projects, C=IT` con SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`, confermando che gli APK futuri firmati con questi secret sono aggiornabili senza cambio chiave.

M3 ha ripetuto la verifica sul codice completo direttamente su `main` nel run `33527223004`: test JVM/Room/parser, build debug, `apksigner` e artifact sono tutti riusciti con lo stesso certificato persistente SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`.

Per M4 ogni sotto-passaggio usa un branch dedicato e deve completare la stessa pipeline reale prima di qualsiasi integrazione. I test MIMIT non devono effettuare richieste alla rete pubblica: usano fixture statiche e, quando serve verificare il client HTTP, un server locale di test.

## 10. Changelog

### 2026-09-02 — M4.4 confermata, M4.5 avviata
- M4.4 confermata dall'utente dopo verifica del codice reale, della navigazione locale `Registro` / `Stazioni` senza libreria Navigation e del run fallito/corretto.
- M4.5 autorizzata come checkpoint conclusivo di M4, ma senza integrazione su `main` prima di una nuova conferma esplicita.
- Fissato schema Room v2 con migrazione 1→2 non distruttiva, cache snapshot Eni/prezzi/sync state, refresh atomico condiviso tra UI e worker, timestamp ultimo successo, logger `W2Full-MIMIT` e WorkManager 2.11.2 giornaliero.
- I test M4.5 devono coprire tutti i failure mode richiesti e restano completamente offline rispetto a MIMIT reale.

### 2026-09-02 — M4.4 implementata e verificata sul branch
- Aggiunta app shell Compose minima `Registro` / `Stazioni`, mantenendo invariato il CRUD M3 e senza introdurre la libreria Navigation.
- Aggiunta schermata Stazioni con richiesta runtime COARSE/FINE, stati `Posizione disponibile`, `Permesso posizione negato`, `Posizione non disponibile`, azioni di retry e lista Eni ordinata dal servizio M4.3; un rifiuto del permesso non blocca il caricamento e usa il fallback alfabetico.
- Aggiunti `MimitStationsDataSource`, repository di sessione non persistente e `NearbyStationsViewModel`; download/parsing restano in memoria e gli errori sono esposti come messaggio non tecnico senza crash.
- Predisposto `lastSuccessfulUpdateEpochMillis: Long?` nello stato e placeholder `Ultimo aggiornamento: non ancora disponibile`, senza anticipare la persistenza M4.5.
- I test M4.4 usano esclusivamente fake/fixture locali; nessuna rete pubblica in CI. Il primo run `33600888882` ha rilevato una incompatibilità di compilazione con l'import Compose `weight`; corretto con commit `66acaf957c15782b0251d2ab2455788dcb56a5a0` senza cambiare il comportamento.
- Run verde `33601057414`, job `100154609063`; artifact `9835232992`, dimensione `13847203` byte, digest `sha256:e55e6d03e7c4ac5df84404d49811f739e922455abfbfe19bcb7232e0219246c4`; certificato persistente invariato `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`.
- M4.4 resta sul branch `m4-nearby-stations-ui`; M4.5 non è stata avviata.

### 2026-09-02 — M4.3 confermata, M4.4 avviata
- M4.3 confermata dall'utente dopo verifica del codice, dei soli permessi foreground nel manifest e ricalcolo indipendente Roma–Milano (`476,9 km`).
- M4.4 autorizzata: schermata Stazioni, richiesta runtime posizione, lista Eni ordinata secondo M4.3 e spazio `Ultimo aggiornamento` già predisposto.
- M4.4 usa soltanto dati di sessione: nessuna cache/Room/WorkManager o persistenza MIMIT; `lastSuccessfulUpdateEpochMillis` resta nullable/placeholder fino a M4.5.

### 2026-09-02 — M4.3 implementata e verificata sul branch
- Aggiunti `GeoPoint`, Haversine e provider `FusedLocationProviderClient` resiliente con esiti `Available`, `PermissionDenied` e `Unavailable`; solo permessi foreground `ACCESS_COARSE_LOCATION`/`ACCESS_FINE_LOCATION`, nessun background location.
- Aggiunto ranking delle sole stazioni Eni già filtrate da M4.2: distanza crescente quando la posizione è disponibile; coordinate assenti/invalide in coda; senza posizione tutte le distanze restano `null` e viene usato fallback alfabetico deterministico.
- Test JVM coprono rotta Haversine nota, simmetria, coordinate invalide, esclusione non-Eni, ordinamento per distanza, permesso negato, posizione unavailable, failure provider e propagazione della cancellazione.
- Run branch `33597805710`, job `100144721666`, completamente verde; artifact `9834047769`, dimensione `13799932` byte, digest `sha256:864421ad44b5379d6c897dd33dac9a509c8a9db77965f4f382c085bac22a80a4`; certificato persistente invariato `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`.
- M4.3 resta sul branch `m4-location-distance`; nessuna UI, cache/Room, import o WorkManager è stata introdotta e M4.4/M4.5 non sono stati avviati.

### 2026-09-02 — M4.2 confermata, M4.3 avviata
- M4.2 confermata dall'utente dopo verifica del codice reale e del confronto esatto `eni`.
- M4.3 autorizzata con scope stretto: posizione utente, Haversine e ordinamento delle sole stazioni Eni; UI, cache/persistenza, import e WorkManager restano esclusi.
- Scelto Google Play services Location 21.4.0; provider resiliente con stati `Available`, `PermissionDenied`, `Unavailable`.
- In assenza di posizione non viene inventata alcuna distanza: fallback alfabetico deterministico e nessun crash.

### 2026-09-02 — M4.2 implementata e verificata sul branch
- Aggiunto `MimitStationFilter` come filtro puro dei `MimitStation` già parsati da M4.1: match esatto della bandiera normalizzata `Eni`, preservazione ordine e `extractionDate`, nessuna dipendenza da rete/cache/UI/posizione.
- Aggiunti test JVM per varianti case/whitespace, non-match e preservazione dell'ordine/dataset.
- Run branch `33596403891`, job `100140620391`, completamente verde; artifact `9833570963`, digest `sha256:8963c06b15cf9b97b1606e2d9dbde50aeb816c424e9bc4ffd75378cdc1bd5549`; certificato persistente invariato `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`.
- M4.2 resta sul branch `m4-eni-filter` e non viene integrata su `main` prima della conferma utente.

### 2026-09-02 — M4.1 confermata, M4.2 avviata e resilienza M4.5 vincolata
- M4.1 confermata dopo verifica del commit, della CI e del formato CSV live; resta sul lignaggio M4 e non è stata integrata su `main`.
- Registrati come vincolanti per M4.5 aggiornamento cache atomico, preservazione degli ultimi dati validi su errore, `lastSuccessfulUpdateEpochMillis` aggiornato solo su successo, messaggio utente non tecnico e logging locale `W2Full-MIMIT` dell'errore specifico.
- M4.2 autorizzata con scope esclusivo sul filtro puro della bandiera `Eni` nei `MimitStation` già parsati; nessuna cache/persistenza/UI/posizione è ammessa in questo sotto-passaggio.

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
- raggio massimo stazioni;
- retention storico prezzi;
- libreria grafici definitiva in M5;
- schema CSV in M7;