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
- Target principale Samsung Galaxy S25.
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
- [ ] Scheletro Android Compose.
- [ ] CI con test/build e APK reale.
- [ ] Firma debug persistente da secrets.
- [ ] GitHub Releases per APK debug.

### Design
- [~] Direzione visiva iniziale: **in review**.
- [x] 4 concept logo/icona 1024×1024 in `design/logo/`.
- [x] Home: 2 varianti statiche.
- [x] Registro rifornimenti: 2 varianti statiche.
- [x] Storico prezzi: 2 varianti statiche.
- [ ] Scelta finale della direzione visiva prima di M2.

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
Stato: **[~] in corso — asset pronti, scelta visuale in attesa**

Deliverable:
- [x] 4 concept logo/icona W2Full con canvas **1024×1024**, in `design/logo/`;
- [x] 2 mockup statici Home in `design/mockup/`;
- [x] 2 mockup statici Registro rifornimenti in `design/mockup/`;
- [x] 2 mockup statici Storico prezzi in `design/mockup/`;
- [x] nessun codice app introdotto;
- [x] sezione Design con palette, tipografia e percorsi;
- [ ] review umana e scelta direzione prima di M2.

Gli asset sono SVG statici/vettoriali; i logo dichiarano `width="1024" height="1024" viewBox="0 0 1024 1024"`.

### M2 — Scheletro Android + CI con APK installabile
Stato: **[ ] da fare**
Deliverable: Kotlin, Compose/Material 3, package/applicationId definito prima del codice, target S25, test minimo, CI reale, APK debug e firma stabile.

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

### Variante A — Petrol Night
Background `#101418`; Surface `#182028`; Primary `#33C3A5`; Secondary `#7FD1FF`; Accent `#FFB84D`; Alert `#FF6B6B`; testo `#F5F7FA` / `#A9B4C2`.

Direzione: scura, tecnica, automotive e data-centric.

### Variante B — Road Light
Background `#F6F7F9`; Surface `#FFFFFF`; Primary `#1C6DD0`; Secondary `#25A18E`; Accent `#F59E0B`; Alert `#E45757`; testo `#1F2937` / `#6B7280`.

Direzione: chiara, leggibile, editoriale.

### Tipografia
- Titoli e metriche: Roboto Bold/Medium.
- UI: Roboto Regular/Medium.
- Metriche tabellari: Roboto Mono Medium.

### Percorsi asset
Logo:
- `design/logo/w2full-logo-concept-01.svg` — gauge / indicatore carburante.
- `design/logo/w2full-logo-concept-02.svg` — monogramma W2F + goccia.
- `design/logo/w2full-logo-concept-03.svg` — pompa carburante + wordmark.
- `design/logo/w2full-logo-concept-04.svg` — strada + goccia in badge circolare.

Mockup:
- `design/mockup/home-theme-petrol-night.svg`
- `design/mockup/home-theme-road-light.svg`
- `design/mockup/rifornimenti-theme-petrol-night.svg`
- `design/mockup/rifornimenti-theme-road-light.svg`
- `design/mockup/storico-theme-petrol-night.svg`
- `design/mockup/storico-theme-road-light.svg`

## 8. Fonte dati esterna

Dataset MIMIT: **Carburanti - Prezzi praticati e anagrafica degli impianti**.

Pagina: `https://www.mimit.gov.it/it/open-data/elenco-dataset/carburanti-prezzi-praticati-e-anagrafica-degli-impianti`

URL verificati al 31 agosto 2026:
- `https://www.mimit.gov.it/images/exportCSV/prezzo_alle_8.csv`
- `https://www.mimit.gov.it/images/exportCSV/anagrafica_impianti_attivi.csv`

Dal **10 febbraio 2026** il separatore per “Anagrafica alle 8” e “Prezzi alle 8” è `|`. In M4 vanno ricontrollati URL, header e formato effettivo prima del parser. Licenza dati: IODL 2.0 secondo la pagina ufficiale.

## 9. CI/CD

Da M2: GitHub Actions con checkout, toolchain, test, build `assembleDebug` o equivalente, verifica APK e upload artifact. Keystore debug generato una volta, codificato in secret, ricostruito nel runner e mai stampato nei log.

## 10. Changelog

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

- concept logo e palette da scegliere prima di M2;
- package/applicationId e SDK definitivi in M2;
- versionamento/naming APK e trigger Release;
- secrets keystore;
- semantica autonomia e unità consumo;
- rifornimenti parziali;
- raggio e ordinamento stazioni;
- permessi posizione minimali;
- retention storico prezzi;
- libreria grafici definitiva in M5;
- schema CSV in M7.
