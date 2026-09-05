# W2Full — Project Specification

> **Fonte di verità corrente.** Tutti i requisiti, decisioni, vincoli di firma, migrazioni e verifiche documentati nella cronologia Git fino a `main` `ae1904c2889ba4d5ec35d542c1889458feeeff20` restano normativi, salvo quanto esplicitamente sostituito qui. M6 resta non iniziata. M7.4 resta l'unico checkpoint attivo.

## Stato

- M0–M5: **chiuse**.
- Distribuzione GitHub Releases: **chiusa**.
- M6 — notifiche soglia: **non iniziata**.
- M7.1: **chiusa e verificata su Galaxy S25**.
- M7.2 RC1/RC2: **FAIL UX**, sostituita da M7.4.
- M7.4 RC1 `v0.5.3-m7.4-rc1`: tecnicamente verde ma **FAIL UX su Galaxy S25** il 5 settembre 2026.
- M7.4 RC2: **checkpoint attivo**.

## Evidenza RC1 M7.4

- sorgente esatta: `ae1904c2889ba4d5ec35d542c1889458feeeff20`;
- Release run `33914981865`, job `101159953591`: **SUCCESS**;
- APK `w2full-v0.5.3-m7.4-rc1-debug.apk`;
- SHA-256 APK `9a0985f92aa1ff77a8325cb32d5c4bc400a0ab48fc6e5dbf66d220fbd38e7885`;
- certificato persistente SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`;
- public key SHA-256 `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`.

La prova reale ha confermato presenza delle funzioni ma ha respinto l'UX: schermate troppo scrollabili, filtri e impostazioni ridondanti, Storico troppo occupato dai controlli, grafico con un solo punto perché lo storico deduplica per `communicated_at`, e target della stella preferita troppo piccolo. RC1 non chiude M7.4.

# M7.4 RC2 — contratto UX correttivo

Branch: `m7.4-ux-rc2`, derivato da `main` `ae1904c2889ba4d5ec35d542c1889458feeeff20`.

## 1. Regola generale: niente page-scroll nelle tab principali

- `Stazioni`, `Storico` e home `Impostazioni` **non devono essere pagine verticalmente scrollabili**.
- Header, stato sintetico, filtri/azioni principali e bottom navigation restano fissi e sempre raggiungibili.
- In `Stazioni` **solo la lista risultati** può scorrere verticalmente.
- Liste dedicate aperte da Impostazioni, come l'elenco completo preferite, possono avere una propria area-lista scrollabile; non devono trasformare la home Impostazioni in una lunga pagina.
- La tabella completa dello Storico non allunga la schermata principale: si apre in una vista dedicata/lista dati.
- Nessun controllo principale deve richiedere scroll per essere raggiunto su Galaxy S25.

## 2. Target touch grandi

- La stella preferita sulla card stazione è un controllo esplicito con area touch **minimo 48×48 dp**; icona visiva almeno 28 dp circa.
- `☆` aggiunge e `★` rimuove con un singolo tap.
- Anche refresh, filtri rapidi e azioni primarie rispettano target touch ampi; evitare chip minuscoli o controlli compressi.

## 3. Stazioni — layout compatto e user-centred

Area fissa superiore:
1. titolo `W2Full` / `Stazioni Eni vicine`;
2. riga compatta `Aggiornato …` con azione refresh grande;
3. indicazione posizione solo se utile/errore, non in una card alta;
4. una riga di controlli compatti che riassume almeno: raggio, ordinamento, scope `Tutte/Preferite`, carburante usato per il prezzo.

I controlli dettagliati si aprono tramite dialog/bottom-sheet o contenitore temporaneo; non restano espansi occupando la schermata.

Sotto l'area fissa parte una **LazyColumn risultati** che occupa tutto lo spazio restante.

Regole lista invarianti:
- un solo elenco operativo;
- preferita/non preferita non altera l'ordinamento;
- `Distanza`, `Prezzo Self`, `Prezzo Servito` ordinano globalmente tutte le stazioni del filtro corrente;
- raggio applicato prima dell'ordinamento;
- `Tutte/Preferite` filtra senza pinnare preferite sopra;
- carburante di riferimento = carburante veicolo, chiaramente indicato;
- stella grande direttamente nella card.

## 4. Storico — informazione prima, configurazione dopo

La schermata principale **non scrolla** e deve privilegiare il grafico.

Layout principale:
1. stazione selezionata + grande azione `Cambia`;
2. sintesi compatta del prezzo corrente e, quando disponibile, confronto con osservazione precedente / min / max del periodo;
3. grafico che usa la parte centrale disponibile;
4. periodo rapido `7g`, `30g`, `3m`, `1a`, `Tutto`;
5. azioni grandi `Confronta` e `Mostra dati`.

Scelta stazione:
- `Cambia` apre selettore con gruppi/filtri `Preferite` e `Altre`;
- liste ordinate per distanza quando posizione disponibile, altrimenti nome/ID stabile;
- gestione stella resta in `Stazioni` e `Impostazioni > Stazioni preferite`.

Grafico:
- Serie A sempre disponibile;
- Serie B opzionale e configurabile tramite `Confronta`;
- per ogni serie carburante e servizio indipendenti;
- casi obbligatori: Benzina vs Gasolio, Benzina Self vs Benzina Servito, Benzina Servito vs Gasolio Servito;
- legenda chiara;
- 0/1 punto non mostra un grande grafico vuoto: stato `Storico in costruzione` con spazio proporzionato;
- nessuna interpolazione inventata.

`Mostra dati` apre una vista dedicata con lista/tabella delle osservazioni; non rende scrollabile la schermata Storico principale.

## 5. Storico giornaliero reale

Il modello M5 basato sulla chiave `stationId + fuel + isSelf + communicatedAt` viene corretto perché due estrazioni giornaliere possono riportare lo stesso prezzo e la stessa data comunicazione.

Nuovo concetto: **osservazione giornaliera MIMIT**.

Ogni refresh MIMIT riuscito registra per ogni prezzo valido Eni:
- stationId;
- fuelDescription;
- isSelf;
- prezzo;
- `observedOn` = data di estrazione prezzi MIMIT quando disponibile; fallback alla data locale del refresh solo se il dataset non espone la data;
- `communicatedAt` mantenuto come metadato;
- importedAtEpochMillis.

Unicità logica: `stationId + fuelDescription + isSelf + observedOn`.

Conseguenze:
- due refresh della **stessa estrazione** non duplicano punti;
- l'estrazione del giorno successivo crea un nuovo punto anche se prezzo e `communicatedAt` non sono cambiati;
- giorni non realmente osservati restano mancanti; nessun backfill/interpolazione inventata.

Room passa da schema 3 a 4 con migrazione esplicita e senza destructive migration. I punti M5 già presenti vengono preservati come osservazioni legacy usando una data derivata dal timestamp import quando non esiste un `observedOn` originale.

## 6. Impostazioni — eliminare ridondanza

Home `Impostazioni` non scrollabile e ridotta a sole destinazioni globali:
- `Veicolo`;
- `Stazioni preferite`;
- `Informazioni`.

Rimuovere dalla home le sezioni duplicate `Stazioni` e `Storico`: raggio, ordinamento, periodo e confronto si impostano nella schermata dove vengono usati e l'app ricorda l'ultima scelta automaticamente.

`Stazioni preferite` apre pagina dedicata/lista con target di rimozione grandi e tutte le preferite indipendentemente dal raggio.

## 7. Persistenza e compatibilità

- Preferiti e preferenze esistenti restano compatibili.
- Le scelte operative Stazioni/Storico persistono localmente, ma non vengono duplicate come pagine impostazioni.
- Nessun account/cloud/servizio a pagamento.
- Firma debug persistente invariata.

## 8. Versione RC2

- `versionCode = 12`;
- `versionName = 0.5.3-m7.4-rc2`.

## 9. Test obbligatori RC2

Stazioni:
- header/controlli fuori dalla LazyColumn risultati;
- solo lista risultati scrollabile;
- ordinamento globale invariato con preferite miste;
- filtro Tutte/Preferite;
- star add/remove + persistenza;
- target stella almeno 48 dp verificato per contratto UI;
- regressioni raggio/prezzo/distanza.

Storico:
- schermata principale senza verticalScroll/LazyColumn;
- selettore stazione separato;
- Serie A/B configurabili;
- periodi 7g/30g/3m/1a/tutto;
- 0/1 punto gestito in modo compatto;
- vista dati separata;
- confronto Benzina/Gasolio e Self/Servito.

Storico dati:
- migrazione Room 3→4 preserva righe esistenti;
- stessa extraction date non duplica;
- extraction date successiva crea nuovo punto anche con prezzo/communicatedAt invariati;
- query cronologica usa `observedOn`.

Impostazioni:
- home contiene solo Veicolo / Stazioni preferite / Informazioni;
- nessuna duplicazione Stazioni/Storico;
- home non scrollabile.

Gate:
- regressioni M3–M7 verdi;
- CI branch reale test/build/firma/artifact SUCCESS;
- PR limitato a M7.4 RC2;
- CI `main` SUCCESS;
- Release `v0.5.3-m7.4-rc2` dall'esatto SHA finale `main` con firma/hash/tag verificati;
- M7.4 si chiude solo dopo PASS reale Galaxy S25.

## Fuori scope

- M6 notifiche soglia;
- issue #1 `Indicazioni`;
- manutenzione CI non necessaria;
- nuove funzioni non approvate.
