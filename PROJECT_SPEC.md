# W2Full — Project Specification

> **Fonte di verità del progetto.** Per M0–M5 e per il checkpoint GitHub Releases restano normativi tutti i requisiti, decisioni ed evidenze presenti in `PROJECT_SPEC.md` al commit `1eb6f5ab3b4a248e30e7a693ac0bc62ac311bffa`, salvo quanto esplicitamente modificato qui. I requisiti M0–M3 storici restano inoltre tracciati dal riferimento normativo già presente in quella spec al commit `749f9e44646113fb0c115c9a6685c73beee00b77`.

## Stato

- M0–M5: **chiuse**.
- Distribuzione GitHub Releases: **chiusa**.
- M6 — notifiche soglia: **non iniziata**.
- M7 — rifiniture: **in corso**.
- M7.1 — filtri/ordinamento Stazioni: **candidato branch verificato; integrazione su `main` pendente**.
- M7.2 — preferiti nello Storico: **richiesta e accodata; non iniziata finché M7.1 non è chiusa**.

## Evidenza reale M5 su dispositivo

- Release `v0.5.0-m5-rc1`, tag diretto al commit finale M5 `1eb6f5ab3b4a248e30e7a693ac0bc62ac311bffa`;
- Release run `33890959597`, job `101082219282`: **SUCCESS**;
- APK `w2full-v0.5.0-m5-rc1-debug.apk`, SHA-256 `4768319aecf910875cdabc5f020595434aeebb4bb09c32529902f4754a685806`;
- certificato persistente SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`;
- public key SHA-256 `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`;
- verifica reale Samsung Galaxy S25 confermata dall'utente il 4 settembre 2026: aggiornamento installato, schermata Stazioni e schermata Storico funzionanti **PASS**.

## M7.1 — filtri e ordinamento schermata Stazioni

### Obiettivo

Permettere all'utente di limitare la lista delle stazioni Eni a un raggio massimo configurabile e di scegliere l'ordinamento per distanza oppure per prezzo del carburante correntemente selezionato dall'impostazione Veicolo, mantenendo invariati download MIMIT, cache, storico e calcolo distanze M4/M5.

### Contratto fissato prima del codice

- branch `m7-station-filters`, derivato dall'HEAD `main` `1eb6f5ab3b4a248e30e7a693ac0bc62ac311bffa`;
- contratto spec-first: commit `ed8e3969d758612d6bb9394a88c42ac418e15dda`;
- nuova card `Filtri` nella schermata Stazioni, dopo lo stato posizione e prima dell'elenco;
- filtro raggio opzionale, default `Nessun limite`;
- quando abilitato, raggio intero configurabile `1..200` km; valore iniziale proposto `20` km;
- il raggio si applica solo con posizione disponibile; in caso di posizione negata/non disponibile la preferenza resta salvata, il filtro non viene applicato e la card lo segnala;
- con filtro attivo e posizione disponibile, distanza nulla o superiore al limite comporta esclusione dalla lista;
- ordinamento selezionabile: `Distanza`, `Prezzo Self`, `Prezzo Servito`;
- `Distanza` è il default e conserva l'ordine/ranking M4;
- `Prezzo Self`: prezzo Self crescente del carburante selezionato, prezzo mancante in fondo, tie-break distanza e poi ID stazione;
- `Prezzo Servito`: stessa regola sul prezzo Servito;
- filtro raggio prima dell'ordinamento;
- preferenze esclusivamente locali e persistenti in storage privato Android, senza account/cloud;
- riepilogo del numero di stazioni mostrate rispetto al totale;
- nessuna modifica a refresh MIMIT, cache Room, storico M5, filtro Eni o `defaultFuelType`;
- `versionCode = 8`, `versionName = 0.5.1-m7.1`;
- M7.2 preferiti Storico, M6 notifiche e pulsante `Indicazioni` fuori scope M7.1.

### Implementazione candidato

- commit funzionale `b22466587e9ca960a175f3196c0bd5f09f795af3` — `feat(m7.1): add station radius and price sorting`;
- fix compilazione Compose mirato `0e015eda3b58a2ed0797dd560a3d411743d306c6`;
- fix regression label M4 mirato `c0b57c7962b17f57784a0642efc02310b7f6a805`;
- `StationListPreferencesStore` locale via `SharedPreferences` private;
- funzione pura di filtro/ordinamento separata dalla sorgente MIMIT;
- ViewModel conserva sorgente non filtrata e ricalcola la lista visibile al cambio preferenze/posizione/snapshot;
- card Compose `Filtri` con switch raggio, input numerico, chip ordinamento e conteggio risultati;
- la CI ordinaria è stata modificata esclusivamente per aggiungere `m7-station-filters` ai branch trigger; nessuna versione/action CI è stata aggiornata.

### Test ed evidenze branch

Sono coperti e verdi:
- nessun limite + distanza conserva l'ordine M4;
- raggio attivo con posizione disponibile esclude fuori-raggio e distanza nulla;
- raggio attivo senza posizione non filtra;
- ordinamento Self crescente con mancanti in fondo e tie-break distanza;
- ordinamento Servito equivalente;
- validazione raggio `1..200` con conservazione dell'ultimo valore valido;
- persistenza locale raggio/ordinamento;
- regression test M3–M5.

Cronologia CI reale:
- run `33897001425`, job `101101872680`: **FAIL** in compilazione per uso di `Modifier.weight`; nessun merge eseguito;
- run `33897332878`, job `101102942309`: compilazione superata, **FAIL** su un solo regression test di testo M4; nessun merge eseguito;
- run `33897609814`, job `101103839492`: **SUCCESS** completa;
- `testDebugUnitTest`: **SUCCESS** (77 test);
- `assembleDebug`: **SUCCESS**;
- APK: `apksigner` **Verifies**, schema v2, un signer;
- certificato SHA-256 verificato: `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`;
- public key SHA-256 verificata: `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`;
- artifact CI `w2full-debug-apk`, ID `9946406207`, non scaduto al momento della verifica;
- digest SHA-256 dell'archivio artifact: `5fa254d687d9b860b1844df28bd792bb05fd2f3a85265cb9399e76ec5dd5cd7f`.

M7.1 resta aperta finché il vero HEAD documentato del branch non ha CI verde, il PR non è integrato con CI `main` verde, le evidenze finali non sono registrate e non esiste una Release APK reale da provare sul Galaxy S25.

## M7.2 — preferiti nello Storico — richiesta

Richiesta già autorizzata dall'utente ma non implementata durante M7.1. Obiettivo preliminare: marcare stazioni come preferite e trovarle/monitorarle prioritariamente nello Storico, mantenendo comunque accessibili le altre stazioni. Il contratto tecnico dettagliato verrà fissato qui **prima** del relativo codice.

## Requisito futuro già approvato

M7: pulsante `Indicazioni` su ogni stazione tramite intent verso Google Maps/app mappe compatibile, destinazione stazione e partenza dalla posizione corrente; preferire coordinate MIMIT valide con fallback indirizzo. Issue #1.

## Regola di avanzamento

Un checkpoint alla volta. M7.1 è l'unico checkpoint attivo. M7.2 parte solo dopo chiusura e prova M7.1. M6 notifiche soglia resta non iniziata finché non viene esplicitamente autorizzata come obiettivo attivo.
