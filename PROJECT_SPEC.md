# W2Full — Project Specification

> **Fonte di verità del progetto.** Tutti i requisiti, decisioni ed evidenze fino al commit `8b4a12ed08700b622f711d485181aef0e1b7a4b2` di `main` restano normativi, salvo quanto esplicitamente sostituito da questo documento. La cronologia precedente resta disponibile in Git e non viene eliminata.

## Stato

- M0–M5: **chiuse**.
- Distribuzione GitHub Releases: **chiusa**.
- M6 — notifiche soglia: **non iniziata**.
- M7 — rifiniture: **in corso**.
- M7.1 — filtri/ordinamento Stazioni: **chiusa e verificata su Galaxy S25**.
- M7.2 — preferiti stazioni RC1/RC2: **FAIL funzionale su Galaxy S25; comportamento sostituito dal contratto M7.4**.
- M7.3 — grafico Storico configurabile multi-serie: **requisito assorbito nel contratto M7.4**.
- M7.4 — redesign UX Stazioni + Storico + Impostazioni: **checkpoint attivo**.

## Evidenza RC2 non accettata

Release tecnica `v0.5.2-m7.2-rc2`:
- sorgente `main` `8b4a12ed08700b622f711d485181aef0e1b7a4b2`;
- Release run `33907099356`, job `101134491125`: **SUCCESS**;
- APK `w2full-v0.5.2-m7.2-rc2-debug.apk`;
- SHA-256 APK `26553fb1e957b7ff67056423d50d71e32cece0f69c9e10bab33b2772735f0476`;
- certificato persistente SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`;
- public key SHA-256 `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`.

Prova reale Galaxy S25 del 4 settembre 2026: **FAIL funzionale UX**. Le preferite venivano mostrate in una sezione separata sopra l'elenco e quindi l'ordinamento globale per distanza/prezzo non risultava più rispettato. L'utente ha approvato un nuovo mockup che sostituisce questo comportamento.

Il branch bridge `m7.2-release-rc2` è stato riallineato a `8b4a12ed08700b622f711d485181aef0e1b7a4b2`; il workflow permanente di `main` resta tag-only.

# M7.4 — redesign UX Stazioni + Storico + Impostazioni

Branch: `m7-stations-history-ux`, derivato da `main` `8b4a12ed08700b622f711d485181aef0e1b7a4b2`.

Il mockup UX mostrato all'utente il 4 settembre 2026 è stato approvato esplicitamente con “Mi piace procedi ad implementarlo”. Subito dopo l'utente ha richiesto anche che la tab `Veicolo` diventi `Impostazioni` con icona ingranaggio e raccolga tutte le configurazioni. Questo documento fissa il contratto **prima del codice**.

## 1. Schermata Stazioni

### Elenco unico e ordinamento

- Deve esistere **un solo elenco operativo di stazioni**: le preferite non vengono più pinnate in una sezione separata sopra la lista.
- Lo stato preferito è una proprietà della card e **non modifica la posizione** della stazione nell'elenco.
- Il filtro raggio M7.1 continua a essere applicato prima dell'ordinamento.
- Ordinamenti disponibili e persistenti:
  - `Distanza`;
  - `Prezzo Self`;
  - `Prezzo Servito`.
- Quando si ordina per prezzo, tutte le stazioni visibili — preferite e non — rispettano lo stesso ordinamento; valori mancanti in fondo, tie-break coerente con M7.1.
- Il carburante di riferimento per l'ordinamento resta quello configurato nel veicolo; l'interfaccia deve indicarlo chiaramente (es. `Benzina`).

### Filtro preferite

- Aggiungere filtro rapido `Tutte | Preferite` nella card Filtri.
- `Tutte`: mostra tutte le stazioni che rispettano raggio e filtri.
- `Preferite`: mostra solo le preferite che rispettano raggio e filtri.
- In entrambi i casi l'ordinamento selezionato deve essere rispettato integralmente.

### Stella sulla card

- Ogni card stazione mostra una stella direttamente accessibile:
  - `☆` non preferita;
  - `★` preferita.
- Un singolo tap aggiunge/rimuove immediatamente la stessa `stationId` dai preferiti.
- La scelta resta persistente localmente usando lo storage già esistente, senza account/cloud e senza migrazione Room.
- I dati salvati dalle RC precedenti devono restare compatibili.

## 2. Schermata Storico — scelta stazione

- Lo Storico non gestisce la stella; la gestione preferiti vive in `Stazioni` e in `Impostazioni > Stazioni preferite`.
- In alto aggiungere selettore `Preferite | Altre`.
- `Preferite`: mostra solo stazioni preferite che hanno dati storici.
- `Altre`: mostra stazioni non preferite che hanno dati storici.
- Con posizione disponibile, entrambe le liste stazioni sono ordinate per distanza; senza posizione usare un ordinamento stabile per nome/ID.
- La selezione stazione deve sopravvivere alle ricomposizioni quando ancora valida; altrimenti fallback alla prima stazione del gruppo selezionato.
- Se il gruppo non contiene stazioni con storico, mostrare stato vuoto esplicito.

## 3. Storico — grafico configurabile dall'utente

### Serie

- Il grafico supporta una o due serie indipendenti.
- `Serie A` è sempre disponibile.
- `Serie B` può essere abilitata/disabilitata dall'utente.
- Per ogni serie l'utente sceglie indipendentemente:
  - `Carburante`;
  - `Servizio` (`Self` / `Servito`).
- Devono essere possibili almeno:
  - `Benzina Self` vs `Gasolio Self`;
  - `Benzina Servito` vs `Gasolio Servito`;
  - `Benzina Self` vs `Benzina Servito`;
  - una sola serie.
- `Blue Diesel` resta disponibile quando presente nei dati.

### Rendering

- Le serie condividono lo stesso asse temporale della stazione selezionata.
- Ogni serie usa esclusivamente punti reali presenti nello storico; nessun prezzo inventato/interpolato come dato reale.
- Il grafico mostra legenda chiara con nome delle serie.
- Il range verticale deve considerare entrambe le serie visibili.
- Il caso 0/1 punto resta gestito senza crash.

### Periodo e tabella dati

Come nel mockup approvato, aggiungere filtro periodo:
- `1 mese`;
- `3 mesi`;
- `6 mesi`;
- `1 anno`;
- `Tutto`.

Aggiungere anche la possibilità di visualizzare una **tabella dati** sotto il grafico con:
- data;
- valore Serie A se presente;
- valore Serie B se abilitata/presente.

Valori assenti restano vuoti/non disponibili, mai inventati.

## 4. Tab Impostazioni

La tab bottom navigation `Veicolo` viene sostituita da:
- etichetta `Impostazioni`;
- icona ingranaggio Material.

Bottom navigation finale:
- `Registro`;
- `Stazioni`;
- `Storico`;
- `Impostazioni`.

La schermata Impostazioni diventa il contenitore delle configurazioni e include almeno:

### Veicolo
- apre/contiene tutte le impostazioni veicolo esistenti;
- nessuna regressione ai dati già salvati;
- carburante predefinito, capacità serbatoio e parametri già presenti restano modificabili.

### Stazioni preferite
- mostra tutte le stationId preferite indipendentemente dal raggio corrente;
- mostra quando disponibili nome, indirizzo e distanza;
- consente di rimuovere una preferita con un'azione esplicita;
- serve anche a gestire preferite lontane/non visibili nella schermata Stazioni filtrata.

### Stazioni
- espone i valori persistenti di default già usati nella schermata Stazioni, almeno raggio e ordinamento;
- i controlli rapidi restano comunque disponibili nella schermata Stazioni.

### Storico
- può mostrare/modificare i default persistenti per Serie A/Serie B e periodo;
- la configurazione resta sempre modificabile direttamente dalla schermata Storico.

### Informazioni
- versione app corrente e informazioni essenziali locali; nessun account/cloud.

## 5. Persistenza

- Riutilizzare `SharedPreferences`/storage privato già usato per preferiti e filtri dove appropriato.
- Nessuna migrazione Room richiesta salvo necessità tecnica documentata **prima** della modifica schema; default previsto: Room invariata.
- Preferenze nuove da persistere almeno:
  - filtro `Tutte/Preferite` Stazioni;
  - gruppo `Preferite/Altre` Storico;
  - Serie A fuel/service;
  - Serie B enabled/fuel/service;
  - periodo Storico;
  - eventuale toggle tabella dati.

## 6. Versione

- `versionCode = 11`;
- `versionName = 0.5.3-m7.4`.

## 7. Test obbligatori

### Stazioni
- preferita e non preferita rimangono nello stesso elenco ordinato;
- `Prezzo Self` ordina globalmente preferite e non preferite;
- `Prezzo Servito` idem;
- `Distanza` idem;
- `Tutte/Preferite` filtra senza alterare l'ordinamento;
- star toggle add/remove e persistenza;
- regression M7.1 raggio/ordinamento.

### Storico
- `Preferite` e `Altre` producono insiemi disgiunti corretti;
- ordinamento per distanza con posizione e stabile senza posizione;
- fallback selezione corretto;
- Serie A singola;
- Serie A + Serie B;
- confronti Benzina/Gasolio e Self/Servito;
- serie senza dati non inventa punti;
- periodo 1m/3m/6m/1y/tutto;
- tabella dati allinea le date senza inventare valori;
- casi grafico 0/1/multi punto.

### Impostazioni
- bottom nav mostra `Impostazioni` al posto di `Veicolo`;
- accesso alle impostazioni veicolo esistenti senza perdita dati;
- elenco completo preferite e rimozione;
- default Stazioni/Storico persistenti.

### Gate
- tutti i regression test M3–M7.1 e storico M5 verdi;
- CI branch reale: `testDebugUnitTest`, `assembleDebug`, verifica firma persistente, artifact **SUCCESS**;
- PR con diff limitato a M7.4;
- CI `main` **SUCCESS** dopo integrazione;
- Release RC reale costruita dall'esatto SHA finale `main`, firma/hash/tag verificati;
- checkpoint chiuso solo dopo prova reale Galaxy S25 dell'utente.

## 8. Fuori scope

- M6 notifiche soglia;
- pulsante `Indicazioni` issue #1, che resta requisito futuro già approvato;
- modifiche non necessarie alle versioni/actions CI esistenti;
- account, cloud o servizi a pagamento.

## Regola di avanzamento

M7.4 è l'unico checkpoint attivo. Nessun altro checkpoint viene avviato finché M7.4 non arriva a Release installabile e prova reale sul Galaxy S25.