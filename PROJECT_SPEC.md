# W2Full — Project Specification

> **Fonte di verità corrente.** Restano normativi tutti i requisiti e le decisioni già verificati nella cronologia Git fino a `main` `57dbb9f00498ffdd97ba9e8fd78b1bb5333e0dea`, salvo quanto esplicitamente sostituito qui. M6 resta non iniziata. M7.4 è l'unico checkpoint attivo.

## Stato progetto

- M0–M5: **chiuse**.
- Distribuzione GitHub Releases: **chiusa**.
- M6 — notifiche soglia: **non iniziata**.
- M7.1 — filtri Stazioni: **chiusa e verificata su Galaxy S25**.
- M7.2 RC1/RC2: **FAIL UX**, sostituita da M7.4.
- M7.4 RC1 `v0.5.3-m7.4-rc1`: tecnicamente verde, **FAIL UX su Galaxy S25**.
- M7.4 RC2 `v0.5.3-m7.4-rc2`: tecnicamente verde, **PASS UX parziale / FAIL funzionale** su posizione Stazioni e grafico incompleto.
- M7.4 RC3: **in validazione**.

## Baseline M7.4 RC2

La RC2 resta la baseline UX approvata per RC3:
- Stazioni senza page-scroll; solo la lista risultati è scrollabile;
- header e filtri compatti sempre raggiungibili;
- un solo elenco, con ordinamento globale indipendente dai preferiti;
- stella preferita direttamente sulla card con target touch 56×56 dp;
- Storico principale non scrollabile, con `Cambia`, periodo, `Confronta`, `Mostra dati`;
- Serie A/B indipendenti per carburante e Self/Servito;
- Impostazioni ridotta a `Veicolo`, `Stazioni preferite`, `Informazioni`;
- storico giornaliero Room schema 4 basato su `observedOn`, senza interpolazioni inventate.

Release RC2 verificata:
- sorgente `57dbb9f00498ffdd97ba9e8fd78b1bb5333e0dea`;
- Android CI `33954009523`, job `101273898387`: **SUCCESS**;
- Release run `33954142263`, job `101274257018`: **SUCCESS**;
- tag `v0.5.3-m7.4-rc2` direttamente sullo SHA sorgente;
- APK `w2full-v0.5.3-m7.4-rc2-debug.apk`, SHA-256 `bf5a57937978ca9517e61926774bb1a09d3192a77978df3991e578a031ace488`;
- firma persistente verificata: certificato SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`, public key SHA-256 `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`.

### Esito reale RC2 su Galaxy S25 — 5 settembre 2026

Confermato dall'utente:
- layout Stazioni compatto e lista come unica area scrollabile: migliorato e coerente col mockup;
- stella grande: coerente;
- Impostazioni ridotta: coerente;
- Storico non scrollabile e confronto Benzina/Gasolio: presenti.

Problemi che impediscono la chiusura M7.4:
1. Stazioni può mostrare `Posizione non disponibile` e stazioni remote con `Distanza non disponibile` mentre Storico, nella stessa sessione, dispone di distanze corrette.
2. Manca una posizione attuale leggibile nella UI Stazioni.
3. Il grafico manca di scala prezzo asse Y e date asse X.
4. Non è possibile selezionare un giorno e leggere i prezzi delle serie per quella data.
5. Il grafico non supporta zoom/pan/reset.

# M7.4 RC3 — contratto

Branch `m7.4-ux-rc3`, derivato dall'esatto `main` `57dbb9f00498ffdd97ba9e8fd78b1bb5333e0dea`.

Commit spec-first: `e5d198fdda50092d12b4e271be488b064ea6205d`.

## 1. Posizione Stazioni

- `Riprova` richiede una **nuova localizzazione reale** e ricalcola le distanze sulle stazioni già in cache, senza download MIMIT obbligatorio.
- All'ingresso successivo nella tab Stazioni viene ritentata la localizzazione senza perdere il dataset in cache.
- Con posizione disponibile, raggio e ordinamento distanza usano immediatamente le nuove distanze.
- Con raggio 25 km attivo e posizione disponibile, risultati senza distanza o fuori raggio non devono restare visibili.
- Un risultato location temporaneamente indisponibile proveniente dallo snapshot non deve sovrascrivere una posizione valida appena ottenuta nella stessa sessione.
- La UI mostra una riga fissa `📍 <posizione attuale>`.
- Preferenza label: `Comune (Provincia)` tramite reverse geocoding Android di sistema; fallback `📍 Posizione rilevata` se il nome non è risolvibile.
- Mai coordinate grezze nella schermata principale.
- Permission denied/unavailable mantiene azione `Consenti`/`Riprova` appropriata.
- Il reverse geocoding non introduce account, cloud dell'app o servizi a pagamento e il suo fallimento non blocca raggio/distanze.

## 2. Grafico Storico — assi

- Asse Y visibile a sinistra, unità `€/L`, con 3–5 tick leggibili.
- Scala Y calcolata sui punti **attualmente visibili**, con margine sopra/sotto e range non nullo anche a prezzo costante.
- Asse X visibile con date compatte italiane.
- I giorni realmente mancanti restano mancanti: nessuna interpolazione artificiale.
- Segmenti di linea non devono attraversare automaticamente buchi di più giorni.

## 3. Grafico Storico — selezione giorno

- Tap sul plot seleziona l'osservazione/giorno più vicino.
- Quando non zoomato, trascinamento orizzontale permette di scorrere la selezione giorno per giorno.
- La selezione mostra un cursore verticale.
- Il dettaglio mostra data completa + valore Serie A + valore Serie B se attiva.
- Se una serie non ha un'osservazione in quel giorno mostra `n.d.` e non inventa un valore.

## 4. Grafico Storico — zoom e pan

- Pinch-to-zoom orizzontale sulle date.
- Quando zoomato, drag orizzontale effettua il pan della finestra temporale.
- Il pan resta nei limiti dei dati disponibili.
- La scala Y si ricalcola sulla viewport corrente.
- Doppio tap resetta zoom, pan e selezione.
- Cambio periodo (`7g`, `30g`, `3m`, `1a`, `Tutto`) resetta la viewport.
- Zoom massimo limitato; con meno di due giorni utili il grafico resta nello stato compatto `Storico in costruzione`.

## 5. Layout invariato

RC3 **non ridisegna** le schermate già approvate:
- Stazioni resta fixed-screen con sola lista risultati scrollabile;
- Storico principale resta non scrollabile;
- Impostazioni resta con sole tre destinazioni globali;
- target touch grandi invariati.

## 6. Versione

- `versionCode = 13`;
- `versionName = 0.5.3-m7.4-rc3`.

## 7. Test obbligatori

Posizione:
- retry effettua una nuova risoluzione;
- cached stations vengono ricalcolate senza refresh dataset;
- unavailable → available aggiorna stato, distanze, ranking e raggio;
- label leggibile quando risolta e fallback `Posizione rilevata` quando non risolta;
- regressioni permission denied/unavailable.

Grafico:
- Y range/tick con prezzi uguali e differenti;
- Y range usa soltanto dati della viewport;
- X tick restano nella viewport;
- selezione nearest-day;
- Serie B mancante nello stesso giorno restituisce `n.d.`/null;
- zoom clamp minimo/massimo;
- pan clamp ai limiti;
- reset viewport;
- regressioni Serie A/B, periodi e storico giornaliero.

Gate:
- Android CI reale branch: test/build/firma/artifact **SUCCESS**;
- PR limitata a M7.4 RC3;
- Android CI reale su `main`: **SUCCESS**;
- Release reale `v0.5.3-m7.4-rc3` dall'esatto SHA finale `main` con tag, asset, firma e SHA-256 verificati;
- M7.4 si chiude solo dopo nuovo PASS reale Galaxy S25.

## 8. Implementazione RC3 candidata

Implementato sul branch:
- resolver Android Geocoder per label località con fallback;
- `FusedUserLocationProvider` usa nuova richiesta high-accuracy e fallback a last location;
- `NearbyStationsViewModel` ricalcola le stazioni in cache da una fresh location e preserva una posizione valida da snapshot transitoriamente unavailable;
- Stazioni mostra `📍 Comune (Provincia)` o `📍 Posizione rilevata`;
- nuovo modello matematico puro per viewport, asse Y, tick X, selezione nearest-day, zoom e pan;
- nuovo grafico Compose interattivo con cursore, dettaglio A/B, pinch zoom, pan e doppio-tap reset;
- linee interrotte sui gap >1 giorno;
- test dedicati per location retry/radius e grafico;
- versione `0.5.3-m7.4-rc3`, versionCode 13.

### Gate branch

Primo HEAD CI `843906e02f9fda400e91e947c84ccdfcc1492d9b`:
- run `33959603212`, job `101289046922`: **FAIL** in `compileDebugKotlin`;
- causa: due import espliciti `androidx.compose.foundation.layout.weight` risolvevano a un simbolo interno Compose;
- build/APK non eseguiti; nessun merge;
- correzione limitata alla rimozione degli import, mantenendo `weight` nei relativi `ColumnScope`/`RowScope`.

Candidato funzionale validato `5ac2f970494972d48af7c09e3517f3cc9c2ad983`:
- Android CI run `33959732692`, job `101289390232`: **SUCCESS completa**;
- JVM tests: **SUCCESS**;
- `assembleDebug`: **SUCCESS**;
- APK: `Verifies`, APK Signature Scheme v2, un signer;
- certificato SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`;
- public key SHA-256 `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`;
- artifact `w2full-debug-apk`, ID `9967565312`;
- artifact ZIP SHA-256 `fbfaa04038d97318ade88cc62d21e85d8c1b31634f3e3e0d51a23d7f57520453`, size `14549369` byte.

Il presente commit documentale deve superare una nuova CI completa prima dell'apertura PR.

## Fuori scope

- M6 notifiche soglia;
- issue #1 `Indicazioni`;
- backfill storico MIMIT antecedente alle osservazioni locali;
- manutenzione CI non necessaria;
- nuove funzioni non approvate.