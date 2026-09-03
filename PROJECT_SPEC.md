# W2Full — Project Specification

> **Fonte di verità del progetto.** `PROJECT_SPEC.md` va aggiornato **prima di ogni modifica al codice, agli asset o alla configurazione di progetto**. Flusso: spec → implementazione/asset e test pertinenti → verifica reale → chiusura spec → cleanup branch temporanei.
>
> **Baseline normativa incorporata per riferimento.** Tutti i requisiti, le decisioni tecniche, le formule, gli schemi Room, le evidenze CI e il changelog presenti nel precedente `PROJECT_SPEC.md` al commit immutabile `e93536ece10758d4d2d5381af7dc3da2ede5469d` restano normativi salvo dove questa versione li modifica esplicitamente. Permalink baseline: `https://github.com/archimede-projects/w2full-app/blob/e93536ece10758d4d2d5381af7dc3da2ede5469d/PROJECT_SPEC.md`. Questa forma overlay evita di perdere la storia verificata del progetto e mantiene questo file come punto autoritativo per le decisioni correnti.

## 1. Obiettivo

W2Full è un'app Android nativa, gratuita e locale per un singolo veicolo V1: registro rifornimenti, consumi/costo-km/autonomia, prezzi carburante MIMIT, stazioni vicine con focus iniziale Eni, storico prezzi, soglie/notifiche ed esportazione CSV.

## 2. Vincoli permanenti

- 100% gratuita; nessuna carta di credito, servizio a pagamento, account, backend o cloud per i dati utente.
- Kotlin + Jetpack Compose + Material 3; tema di riferimento Petrol Night.
- `applicationId` / namespace `com.archimede.w2full`.
- Target principale Samsung Galaxy S25; `minSdk 26`, `targetSdk 37`, `compileSdk 37`.
- Toolchain corrente M2–M4: AGP 9.3.0, Gradle 9.5.0, JDK 17, Kotlin/Compose compiler 2.3.21, Compose BOM 2026.08.00.
- Persistenza Room locale; rete solo per CSV MIMIT via OkHttp; sync con WorkManager; posizione foreground con FusedLocationProviderClient + Haversine.
- Nessun Play Store. Il solo canale installabile è l'APK allegato a una GitHub Release taggata; gli artifact GitHub Actions restano evidenza tecnica interna.
- Debug APK sempre aggiornabile sopra il precedente tramite keystore persistente nei GitHub Actions Secrets. Certificato atteso SHA-256: `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`; public key SHA-256: `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`.
- Nessuna milestone/sottotappa successiva parte senza autorizzazione esplicita dell'utente.
- M4 e i suoi hotfix restano fuori da `main` fino a conferma esplicita dell'utente dopo verifica reale.

## 3. Stato milestone

- M0 — repo/spec: **[x]**
- M1 — design: **[x]**
- M2 — scaffold Android/CI/firma: **[x]**
- M3 — registro rifornimenti/calcoli: **[x]**
- M4 — integrazione MIMIT: **[~] integration candidate autorizzato; M4.1–M4.5, hotfix device/prezzi e substep carburante veicolo verificati; nulla integrato su `main`**
- M5 — storico prezzi + grafico: **[ ]**
- M6 — notifiche soglia: **[ ]**
- M7 — rifiniture: **[ ]**
- Checkpoint distribuzione GitHub Releases: **[~] in corso**; Release reali già verificate, cleanup infrastrutturale permanente ancora separato dall'integrazione M4.

`main` deve restare invariato durante i substep M4. Prima di ogni dichiarazione di chiusura va ricontrollato lo stato reale del repository.

## 4. Contratto MIMIT corrente

Endpoint ufficiali:
- prezzi: `https://www.mimit.gov.it/images/exportCSV/prezzo_alle_8.csv`;
- anagrafica: `https://www.mimit.gov.it/images/exportCSV/anagrafica_impianti_attivi.csv`.

Metadato corrente già verificato: `Metadati_prezzi_carburanti_20260128.pdf`, in vigore dal 10 febbraio 2026. Separatore `|`, decimali `.`, header logici anagrafica a 10 colonne e prezzi a 5 colonne come documentato nella baseline. Header `idimpianto`/`idImpianto` validato case-insensitive; numero e significato delle colonne restano rigorosi.

### Correzioni post-preview già implementate e verificate realmente

La prima Release `v0.4.5-preview` ha mostrato sul Galaxy S25 due blocchi non emersi dalle fixture CI. Diagnosi e correzioni applicate sul branch `hotfix-device-validation`:

1. **Artefatto CSV anagrafica reale.** Il dataset 2026-09-01 contiene 112 record con `| gestori.prezzibenzina.it` non quotato dentro un campo testuale; il parser ricompone esclusivamente questo pattern noto e continua a rifiutare ogni altra sovrannumerazione di colonne.
2. **Bandiera Eni reale.** Whitelist esatta dopo trim/compressione spazi/case-folding `Locale.ROOT`: `eni` e `agip eni`; vietato `contains`.
3. **Posizione indipendente dalla cache.** Lo stato posizione si risolve anche con cache vuota o refresh MIMIT fallito; `Verifica della posizione in corso` rappresenta solo una richiesta realmente pendente.
4. **Timeout Fused Location.** `getCurrentLocation()` ha timeout applicativo di 12 secondi; allo scadere cancella la richiesta sottostante e degrada a `Unavailable`; la cancellazione esterna della coroutine continua a propagarsi.
5. **Resilienza M4.5 invariata.** Refresh atomico, cache/timestamp precedenti preservati su failure, logger tecnico `W2Full-MIMIT`, messaggio utente generico e stessa semantica manuale/worker.

La Release `v0.4.5-preview.2` è stata verificata realmente dall'utente il **2 settembre 2026** sul Galaxy S25: refresh MIMIT riuscito, `Agip Eni` visibili, timestamp ultimo aggiornamento valorizzato, posizione non più bloccata indefinitamente e ranking per distanza funzionante.

## 5. Hotfix prezzi nelle card Stazioni

Stato: **[x] implementato, CI verificato e confermato su Galaxy S25**. Issue di tracciamento: `#2 M4 hotfix: mostrare prezzi carburante nelle card stazione`.

### 5.1 Carburante selezionato

- Ogni snapshot/lista Stazioni usa il `defaultFuelType` del veicolo singleton V1 (`vehicleId = 1`).
- Se il veicolo non è ancora disponibile o il valore è blank, fallback deterministico `Benzina`.
- Il confronto tra `defaultFuelType` e `MIMIT descCarburante/fuelDescription` applica trim, compressione degli spazi interni e confronto case-insensitive via `Locale.ROOT`, ma resta semanticamente esatto: niente `contains`, prefissi o suffissi permissivi.

### 5.2 Selezione del prezzo

- Nessun nuovo download e nessuna nuova tabella: usare esclusivamente i `mimit_prices` già appartenenti allo snapshot atomico M4.5.
- Per ciascuna stazione, carburante selezionato e modalità (`isSelf = true/false`), scegliere la comunicazione MIMIT con `communicatedAt` più recente.
- Se sono disponibili entrambe le modalità, mostrarle entrambe come `Self` e `Servito`; se ne esiste una sola, mostrare solo quella.
- Il prezzo persistito in millesimi di euro è mostrato con tre cifre decimali.
- Unità: carburante normalizzato esattamente `Metano` → `€/kg`; tutti gli altri carburanti correnti → `€/L`.
- Se la stazione non possiede alcuna riga compatibile col carburante del veicolo, mostrare `Prezzo non disponibile`; non sostituire con il prezzo di un carburante diverso.
- La selezione prezzi è presentazione dello snapshot corrente e non anticipa lo storico M5.

### 5.3 UI

Ogni card stazione mantiene nome, indirizzo/comune/provincia e distanza e aggiunge carburante selezionato e prezzi, per esempio:

`Benzina`
`Self 1,759 €/L · Servito 1,899 €/L`

oppure:

`Benzina`
`Prezzo non disponibile`

### 5.4 Evidenze automatiche e Release

- Implementazione: `96753c425572004584d24cafcc48de71dead44a4`.
- Correzione test tipologica: `b86b202c4704b925ebca6a316d29110914ce99a2`.
- CI branch finale prima della Release: run `33649350040`, job `100312085667`, **SUCCESS**.
- Test JVM: **SUCCESS**; `assembleDebug`: **SUCCESS**.
- Firma persistente v2 verificata con certificato SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`.
- Release `v0.4.5-preview.3` pubblicata dal commit `904dba42c7d7838aecc6c27a9aea6a580c44fc80`.
- Run Release `33649683672`; job `100313300180`: **SUCCESS**.
- Asset `w2full-v0.4.5-preview.3-debug.apk`, SHA-256 `6a9293db29a3f83b1752d62604ff9a92ef73ad55d5838c9dc54435bfa3daf881`.

### 5.5 Verifica reale Galaxy S25

Confermata dall'utente il **2 settembre 2026** su `v0.4.5-preview.3`:
- posizione disponibile e lista ordinata per distanza;
- refresh MIMIT riuscito;
- carburante `Benzina` mostrato nelle card;
- Self e Servito distinti;
- prezzi a tre decimali con `€/L`;
- fallback `Prezzo non disponibile` osservato su una stazione senza prezzo Benzina compatibile;
- distanza e indirizzo restano visibili.

L'hotfix prezzi è chiuso. Commit di chiusura: `5a512fc5392835ea484b3c483df2cb10166a56e1`; CI di chiusura run `33665212254`, job `100365142376`, **SUCCESS**. Issue #2 chiusa come completata.

## 6. Substep M4 — carburante predefinito del veicolo

Stato: **[x] implementato, CI verificato e confermato su Galaxy S25**. Branch dedicato `m4-vehicle-fuel-setting`. Issue di tracciamento: `#3 M4: configurare il carburante predefinito del veicolo`.

### 6.1 Scopo

Rendere modificabile dall'interfaccia il campo già esistente `VehicleEntity.defaultFuelType`, che la schermata Stazioni usa per scegliere i prezzi MIMIT. Non introdurre un nuovo modello veicolo, non cambiare lo schema Room e non alterare i rifornimenti storici.

### 6.2 UI e navigazione

- Aggiungere una terza destinazione locale nella bottom navigation: **`Veicolo`**.
- Nessuna libreria Navigation nuova: mantenere la shell Compose locale già usata per `Registro` e `Stazioni`.
- La schermata Veicolo mostra almeno il nome V1 e il carburante predefinito corrente.
- La selezione deve essere chiara e touch-friendly su Galaxy S25; il salvataggio è immediato alla scelta, senza pulsante obbligatorio `Salva`.
- Mostrare un feedback non invasivo dello stato corrente; errori locali di persistenza non devono crashare l'app.

### 6.3 Opzioni carburante

- Lista base sempre disponibile: `Benzina`, `Gasolio`, `GPL`, `Metano`.
- Se esiste cache MIMIT, aggiungere tutte le `fuel_description` distinte presenti in `mimit_prices`.
- Deduplicare con la stessa normalizzazione trim/compressione spazi/case-insensitive già usata nel matching prezzi, preservando una label canonica leggibile.
- Ordinare alfabeticamente le opzioni, ma mantenere sempre selezionabile il valore attualmente salvato anche se non è nella cache né nella lista base.
- Persistire esattamente la label scelta; nessun mapping permissivo `contains`.

### 6.4 Persistenza e aggiornamento Stazioni

- Aggiungere a `VehicleDao` un update mirato di `default_fuel_type` per `vehicleId = 1`.
- Nessuna migrazione DB: il campo esiste già in schema v1/v2.
- Nessuna rete necessaria per cambiare carburante.
- Dopo il cambio, tornando a `Stazioni`, lo snapshot deve leggere il nuovo `defaultFuelType` e mostrare i prezzi corrispondenti dalla cache MIMIT esistente, senza richiedere `Aggiorna`.
- Il cambio del carburante predefinito non modifica `RifornimentoEntity`, calcoli storici o valori già registrati.

### 6.5 Test obbligatori offline

Risultati verdi prima della preview:
- lista base disponibile con cache vuota;
- opzioni MIMIT distinte aggiunte e deduplicate case/whitespace-insensitive;
- valore corrente non presente nella cache resta selezionabile;
- persistenza `defaultFuelType` su V1;
- errore/update a zero righe gestito senza crash;
- cambio da `Benzina` a un altro carburante riflesso nello snapshot Stazioni usando la stessa cache prezzi, senza refresh rete;
- nessuna modifica ai record rifornimento;
- regressione M3/M4 precedente verde.

### 6.6 Implementazione, CI e Release

- Spec preventiva: `00860e6b1f0bd04e194c91152e91b320a09e52ac`.
- Implementazione: `1b865d7109c3e4bbae89539b68fcd5ceba4add33`.
- Abilitazione del solo branch corrente nella whitelist CI: `26f3be8404354c02d6e4c61f917c6a6f99b8fe16`.
- Versione applicativa: `versionCode = 5`, `versionName = 0.3.2-m4-fuel-setting`.
- CI branch: run `33666445946`, job `100369275395`, **SUCCESS**.
- `testDebugUnitTest`: **SUCCESS**; `assembleDebug`: **SUCCESS**.
- Firma APK v2 verificata con certificato SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265` e public key SHA-256 `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`.
- Artifact CI interno `9860822663`, ZIP SHA-256 `0f423a359bad8f65e4d587920fe288f3f6fe0b728a4e25708c6bf3d11b05c813`.
- Release bootstrap commit `3d077d90c71eb42b362ed146d6eed3dfa9bd0eb0`.
- Tag annotato `v0.4.5-preview.4` punta esattamente al commit `3d077d90c71eb42b362ed146d6eed3dfa9bd0eb0`.
- Run Release `33666875139`; job tag `100370699858`: **SUCCESS**; job release `100370756041`: **SUCCESS**.
- Asset Release `w2full-v0.4.5-preview.4-debug.apk`, SHA-256 `c4aa826ca43db344be862f6509d6664d657223dde386ac3bb3518189b49b6c7d`.

### 6.7 Verifica reale Galaxy S25

Confermata dall'utente il **3 settembre 2026** su `v0.4.5-preview.4`; l'intero checklist richiesto è passato:
- upgrade installato sopra `v0.4.5-preview.3` senza disinstallazione;
- bottom navigation `Registro | Stazioni | Veicolo` disponibile;
- schermata `Veicolo` mostra carburante corrente e opzioni previste;
- cambio a `Gasolio` persistito con feedback;
- tornando a `Stazioni` senza refresh MIMIT, il carburante e i prezzi riflettono immediatamente `Gasolio` usando la cache esistente;
- ritorno a `Benzina` riflesso allo stesso modo senza refresh;
- rifornimenti storici nel `Registro` rimasti invariati.

Commit di chiusura funzionale: `cecb1c51b78554447b3c3a2348287ce596902be2`. CI di chiusura: run `33705034791`, job `100492229718`, **SUCCESS** con test JVM, build APK, verifica firma e upload artifact tutti riusciti.

Il substep carburante veicolo è quindi chiuso. M4 complessiva resta **in corso** e non viene integrata su `main` in questo passaggio.

## 7. Requisito UX futuro già approvato — Indicazioni

Stato: **[ ] pianificato per M7**. Issue di tracciamento: `#1 UX: pulsante Indicazioni verso la stazione`.

- ogni card stazione dovrà offrire un pulsante **`Indicazioni`**;
- il pulsante apre Google Maps o un gestore mappe compatibile tramite intent, impostando la stazione come destinazione e lasciando all'app mappe la partenza dalla posizione attuale dell'utente;
- usare preferibilmente coordinate MIMIT valide, con fallback all'indirizzo quando opportuno;
- nessuna mappa embedded in W2Full e nessun SDK Maps a pagamento richiesto.

## 8. Distribuzione APK

Il workflow permanente `.github/workflows/android-release.yml` deve creare Release esclusivamente da tag `v*`, ripetendo test/build e verifica firma sul commit taggato. I quattro secret del keystore sono obbligatori; nessun fallback a firma effimera è ammesso nel workflow Release. L'asset usa `w2full-<tag>-debug.apk`; le Release preview/rc sono prerelease.

Il bootstrap temporaneo usato per creare le prime preview resta debito infrastrutturale isolato e deve essere rimosso nel checkpoint distribuzione; non va confuso con i substep M4 né con l'integrazione M4.

## 9. Decisioni aperte

- raggio massimo stazioni;
- retention storico prezzi M5;
- libreria grafici definitiva M5;
- schema CSV M7.

## 10. Changelog corrente

### 2026-09-03 — substep carburante veicolo verificato e chiuso
- `v0.4.5-preview.4` verificata realmente sul Galaxy S25: intero checklist del selettore carburante passato.
- Confermati cambio `Benzina` ↔ `Gasolio`, aggiornamento immediato dei prezzi Stazioni dalla cache senza refresh e invariabilità dei rifornimenti storici.
- Evidenze automatiche: CI branch `33666445946` / `100369275395`, Release `33666875139` / `100370756041` e CI di chiusura `33705034791` / `100492229718`, tutte **SUCCESS**.
- Substep carburante veicolo dichiarato chiuso; issue #3 può essere chiusa come completata.
- `main` resta invariato; M4 complessiva non è chiusa né integrata.
- Cleanup distribuzione resta un checkpoint separato e non viene avviato qui.

### 2026-09-02 — substep carburante veicolo autorizzato
- `v0.4.5-preview.3` verificata sul Galaxy S25 e hotfix prezzi chiuso formalmente.
- Creato branch `m4-vehicle-fuel-setting` dal commit di chiusura `5a512fc5392835ea484b3c483df2cb10166a56e1`.
- Autorizzata una schermata `Veicolo` per modificare `defaultFuelType` senza rete e senza migrazione DB.
- Opzioni base + descrizioni carburante distinte dalla cache MIMIT; cambio riflesso in Stazioni usando la cache esistente.
- Issue #3 aperta per il substep.
- `main` resta invariato; distribuzione cleanup e integrazione M4 restano separati.

## 11. M4 integration candidate — autorizzato 2026-09-03

Stato: **[~] in preparazione sul branch `m4-integration-candidate`**. Issue di tracciamento: `#4 M4: integration candidate e release RC`.

- Base esatta: closure head `48d2305fcd9393fedcf047decae96441759a8655`; confronto con `main` (`749f9e44646113fb0c115c9a6685c73beee00b77`) = **ahead di 75 commit, behind 0**, merge-base esattamente `main`.
- Nessuna nuova funzionalità deve essere introdotta: il candidato consolida esclusivamente quanto già implementato e verificato in M4.1–M4.5, hotfix device/prezzi e selettore carburante.
- `main` resta invariato fino al test reale del candidato RC sul Galaxy S25.
- Il cleanup del bootstrap/pipeline distribuzione resta separato e non fa parte del candidato M4.
- Versioning candidato: incrementare a `versionCode = 6`, `versionName = 0.4.0-rc1`.
- Abilitare esclusivamente `m4-integration-candidate` nella whitelist del workflow Android CI, senza aggiornare action/versioni o warning di manutenzione.
- Verifica obbligatoria: intera suite `testDebugUnitTest`, `assembleDebug`, `apksigner` con certificato persistente atteso e artifact tecnico interno.
- Se CI verde, pubblicare una Release prerelease taggata `v0.4.0-rc1`, installabile sopra `v0.4.5-preview.4`; il workflow Release deve ripetere test/build/firma sul commit taggato.
- Dopo pubblicazione, fermarsi per test reale sul Galaxy S25. Solo dopo conferma utente potrà essere autorizzato il fast-forward di `main` e la chiusura complessiva di M4.
