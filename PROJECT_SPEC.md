# W2Full — Project Specification

> **Fonte di verità del progetto.** Per M0–M5 e per il checkpoint GitHub Releases restano normativi tutti i requisiti, decisioni ed evidenze presenti in `PROJECT_SPEC.md` al commit `1eb6f5ab3b4a248e30e7a693ac0bc62ac311bffa`, salvo quanto esplicitamente modificato qui. I requisiti M0–M3 storici restano inoltre tracciati dal riferimento normativo già presente in quella spec al commit `749f9e44646113fb0c115c9a6685c73beee00b77`.

## Stato

- M0–M5: **chiuse**.
- Distribuzione GitHub Releases: **chiusa**.
- M6 — notifiche soglia: **non iniziata**.
- M7 — rifiniture: **in corso**.
- M7.1 — filtri/ordinamento Stazioni: **[x] chiusa e verificata sul Galaxy S25**.
- M7.2 — preferiti stazioni: **[~] RC1 FAIL; correzione integrata su `main` con CI verde, RC2 reale da pubblicare/provare**.
- M7.3 — grafico Storico configurabile multi-serie: **richiesta esplicita e contratto funzionale fissato; codice non iniziato finché M7.2 corretta non è chiusa/provata**.

## Evidenza reale M5 su dispositivo

- Release `v0.5.0-m5-rc1`, tag diretto al commit finale M5 `1eb6f5ab3b4a248e30e7a693ac0bc62ac311bffa`;
- Release run `33890959597`, job `101082219282`: **SUCCESS**;
- APK `w2full-v0.5.0-m5-rc1-debug.apk`, SHA-256 `4768319aecf910875cdabc5f020595434aeebb4bb09c32529902f4754a685806`;
- certificato persistente SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`;
- public key SHA-256 `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`;
- verifica reale Samsung Galaxy S25 confermata dall'utente il 4 settembre 2026: aggiornamento installato, schermata Stazioni e schermata Storico funzionanti **PASS**.

## M7.1 — filtri e ordinamento schermata Stazioni — chiusa

### Contratto implementato

- branch `m7-station-filters`, derivato dall'HEAD `main` `1eb6f5ab3b4a248e30e7a693ac0bc62ac311bffa`;
- contratto spec-first: commit `ed8e3969d758612d6bb9394a88c42ac418e15dda`;
- card `Filtri` nella schermata Stazioni;
- filtro raggio opzionale, default `Nessun limite`, raggio `1..200` km, valore iniziale `20` km;
- raggio applicato solo con posizione disponibile; preferenza conservata ma non applicata se posizione negata/non disponibile;
- ordinamento `Distanza`, `Prezzo Self`, `Prezzo Servito` sul carburante selezionato, valori mancanti in fondo e tie-break distanza/ID;
- filtro raggio prima dell'ordinamento;
- preferenze locali persistenti in storage privato Android, nessun account/cloud;
- conteggio risultati mostrati/totali;
- nessuna modifica a refresh MIMIT, cache Room, storico M5, filtro Eni o `defaultFuelType`;
- `versionCode = 8`, `versionName = 0.5.1-m7.1`.

### Evidenze tecniche M7.1

- commit funzionale `b22466587e9ca960a175f3196c0bd5f09f795af3`;
- fix compilazione `0e015eda3b58a2ed0797dd560a3d411743d306c6`;
- fix regression label M4 `c0b57c7962b17f57784a0642efc02310b7f6a805`;
- candidato documentato `d00401d1d83c4054b094c8ffdb7be2bdd8f8fde5`;
- run `33897001425`, job `101101872680`: **FAIL** compilazione `Modifier.weight`, corretto prima del merge;
- run `33897332878`, job `101102942309`: **FAIL** su un regression test testo M4, corretto prima del merge;
- run `33897609814`, job `101103839492`: **SUCCESS** completa;
- run sul vero HEAD documentato `33897902917`, job `101104806879`: **SUCCESS** completa, 77 test;
- PR `#7` — `feat(m7.1): add station radius and price sorting`, merge squash;
- commit integrato `1c97cc65a570f6c9220005ffa9541687b8e86386`;
- CI `main` run `33898139949`, job `101105588935`: **SUCCESS**;
- commit pre-Release `316eec016a5dd7d309a07d68c819b17a8e2fbe70`, CI run `33898389712`, job `101106388820`: **SUCCESS**;
- Release `v0.5.1-m7.1-rc1`, run `33898639201`, job `101107200806`: **SUCCESS**;
- tag lightweight `v0.5.1-m7.1-rc1` → commit `316eec016a5dd7d309a07d68c819b17a8e2fbe70`;
- APK `w2full-v0.5.1-m7.1-rc1-debug.apk`, SHA-256 `3f5c8e138b7c3158d52026e3e135be15314d65fc3385a1dd932311cd36ba9a08`;
- certificato SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265` e public key SHA-256 `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313` verificati;
- commit evidenze Release `67ad7fd46c2a83d74e56d85358599d994187bbe1`, CI run `33899223729`, job `101109087355`: **SUCCESS**;
- branch bridge `m7.1-release-rc1` riallineato a `67ad7fd46c2a83d74e56d85358599d994187bbe1`; workflow nuovamente identico al permanente tag-only.

### Verifica reale M7.1

Il 4 settembre 2026 l'utente ha installato/provato la RC sul Samsung Galaxy S25 e ha confermato esplicitamente: **“I filtri funzionano.”** M7.1 è quindi **chiusa**.

## M7.2 — stazioni preferite

Stato: **[~] RC1 FAIL; correzione integrata su `main` con CI verde, RC2 reale da pubblicare/provare**.

### Implementazione RC1 non accettata

La prima interpretazione M7.2 aveva collocato la gestione dei preferiti direttamente nello Storico. È stata implementata, integrata e distribuita tecnicamente con successo, ma la prova reale sul Galaxy S25 ha mostrato che il comportamento non corrisponde al requisito utente.

- branch originario `m7-history-favorites`, derivato dall'HEAD `main` `67ad7fd46c2a83d74e56d85358599d994187bbe1`;
- contratto originario spec-first commit `19a6f7ecad648f4da841fb9dda780bfbffa0a1f1`;
- commit funzionale `992b42f1b7a74b8e47d0e2f82ea2e18c8702baae`;
- candidato documentato `2c274458519392baeb6efc34b2ac3949409d276e`;
- CI branch run `33903014671`, job `101121279791`: **SUCCESS**;
- PR `#8`, merge squash;
- commit integrato `5a9bc5c717b71d8bae8994f3484ab928836104f5`;
- CI integrazione run `33903257045`, job `101122079944`: **SUCCESS**;
- commit pre-Release `c6498f633f558969e4e08f1b4aa47e7cfecc0d0a`, CI `33903530588`, job `101122969895`: **SUCCESS**;
- Release RC1 `v0.5.2-m7.2-rc1`, ID `382925789`, run `33903851611`, job `101124006633`: **SUCCESS**;
- tag RC1 → `c6498f633f558969e4e08f1b4aa47e7cfecc0d0a`;
- APK RC1 `w2full-v0.5.2-m7.2-rc1-debug.apk`, SHA-256 `bc18518971f955b0e75233e86df496d280f6ec9d5f8ca380c81576f4cc8fa16a`;
- firma persistente verificata: certificato SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`, public key SHA-256 `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`;
- commit evidenze RC1 `d65ba4ddce9432caa0afc640c62d24b9843c043e`, CI `33904324496`, job `101125545786`: **SUCCESS**;
- branch `m7-history-favorites` e `m7.2-release-rc1` riallineati a `d65ba4ddce9432caa0afc640c62d24b9843c043e`.

### Verifica reale RC1 — FAIL funzionale

Il 4 settembre 2026 l'utente ha provato `v0.5.2-m7.2-rc1` sul Samsung Galaxy S25 e ha segnalato esplicitamente:
- la gestione preferiti nello Storico non è il comportamento desiderato;
- una stazione aggiunta per errore ai preferiti non risultava facilmente rimovibile nel flusso desiderato;
- nello Storico continuavano a comparire molte stazioni molto lontane dalla posizione corrente;
- i preferiti devono essere gestiti dalla schermata posizione/Stazioni (oppure impostazioni), con una stella che al tap aggiunge o rimuove il preferito;
- lo Storico deve invece concentrarsi sulla configurazione del grafico.

Pertanto RC1 è **FAIL funzionale su dispositivo** e M7.2 non è chiusa.

### Contratto correttivo M7.2 — fonte di verità

- branch correttivo `m7.2-favorites-correction`, derivato da `main` `d65ba4ddce9432caa0afc640c62d24b9843c043e`;
- contratto correttivo spec-first commit `1138d5a76d9556843d3ba4113d16de7aae1c1097`;
- la gestione preferiti viene spostata nella schermata `Stazioni`, non nello `Storico`;
- ogni card stazione nella schermata `Stazioni` deve avere un controllo stella direttamente visibile e accessibile: stato non preferito `☆`, stato preferito `★`;
- un singolo tap sulla stella deve aggiungere/rimuovere immediatamente la stazione dai preferiti e persistere la scelta localmente;
- i preferiti restano identificati esclusivamente da `stationId` MIMIT e salvati solo in storage privato locale Android; nessun account/cloud e nessuna migrazione Room;
- il nuovo codice deve riutilizzare/migrare trasparentemente i dati già salvati dalla RC1, così la stazione aggiunta per errore può essere rimossa dalla schermata `Stazioni` senza perdita o reset manuale;
- nella schermata `Stazioni` le preferite devono essere sempre raggiungibili per poterle rimuovere anche se sono fuori dal raggio corrente: se necessario vengono mostrate in una sezione `Preferite` separata sopra l'elenco filtrato, senza duplicati nell'elenco normale;
- i filtri/ordinamenti M7.1 continuano ad applicarsi all'elenco normale delle stazioni; la sezione preferite serve come gestione esplicita e non altera il calcolo della distanza;
- nello `Storico` vengono rimossi il pulsante `☆ Aggiungi ai preferiti` / `★ Preferita` e il raggruppamento `Preferite` / `Altre stazioni` introdotti dalla RC1;
- nello `Storico` il selettore stazione deve proporre **solo le stazioni preferite che possiedono dati storici**, evitando l'elenco indiscriminato di stazioni lontane;
- se non esiste alcuna preferita con storico, mostrare uno stato vuoto esplicito che indirizza l'utente alla schermata `Stazioni` per aggiungere una preferita, invece di mostrare tutte le stazioni disponibili;
- se una preferita viene rimossa mentre era selezionata nello Storico, la selezione passa alla prima altra preferita con storico disponibile; se non ne restano, compare lo stato vuoto;
- nessuna modifica alla logica del grafico multi-serie durante questo checkpoint correttivo;
- `versionCode = 10`, `versionName = 0.5.2-m7.2-rc2`;
- M7.3, M6 e pulsante `Indicazioni` restano fuori scope del codice M7.2 correttivo.

### Test obbligatori correzione M7.2

- store preferiti: lettura compatibile dei dati RC1, add, remove e persistenza;
- toggle stella in `Stazioni`: primo tap aggiunge, secondo tap rimuove lo stesso `stationId`;
- sezione `Preferite` in `Stazioni`: include anche una preferita fuori raggio, nessun duplicato nell'elenco normale;
- filtri/ordinamento M7.1 dell'elenco normale restano invariati;
- Storico: nessun controllo stella e nessun gruppo `Altre stazioni`;
- Storico: elenco stazioni = intersezione tra preferite e stazioni con storico;
- Storico: nessuna preferita → stato vuoto esplicito;
- rimozione della preferita selezionata → fallback corretto oppure stato vuoto;
- regression test M3–M7.1 e M5 storico tutti verdi;
- CI branch reale con `testDebugUnitTest`, `assembleDebug`, verifica firma persistente e artifact **SUCCESS**;
- prima della nuova prova Galaxy S25: PR/integration su `main`, CI `main` **SUCCESS**, Release RC2 reale con firma/hash verificati.

### Implementazione correttiva M7.2

- storage RC1 mantenuto compatibile usando lo stesso `SharedPreferences`/key per gli ID stazione;
- `NearbyStationsViewModel` carica e persiste gli ID preferiti e separa la presentazione in `Preferite` e lista normale filtrata, senza duplicati;
- una preferita fuori dal raggio corrente resta visibile nella sezione `Preferite` per consentirne la rimozione;
- `NearbyStationsScreen` espone il controllo `☆ Aggiungi ai preferiti` / `★ Preferita` direttamente sulla card;
- `PriceHistoryViewModel` limita il selettore alle sole stazioni preferite che possiedono storico;
- `PriceHistoryScreen` non contiene più toggle preferiti né gruppo `Altre stazioni`; in assenza di preferite con storico mostra uno stato vuoto esplicito;
- test dedicati aggiunti per toggle add/remove, preferita fuori raggio senza duplicati, intersezione preferite/storico e fallback della selezione;
- CI ordinaria modificata esclusivamente per aggiungere `m7.2-favorites-correction` ai branch trigger; nessuna versione/action CI aggiornata.

### Gate CI branch correzione M7.2

- HEAD primo candidato `61b9ced82e84ff92de51c31d12af940a0fcacf2c` — `ci(m7.2): validate favorites correction branch`;
- Android CI run `33905916130`, job `101130684120`: **SUCCESS** completa;
- `testDebugUnitTest`: **SUCCESS**;
- `assembleDebug`: **SUCCESS**;
- `apksigner`: **Verifies**, firma v2, un signer;
- certificato SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`;
- public key SHA-256 `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`;
- artifact `w2full-debug-apk`, ID `9949506252`, size archivio `14291773` byte, digest ZIP SHA-256 `05640019d0d5f9f5a5c73c399d9ab73e6fe2b7b47f23b4078784181363bccd1e`;
- vero HEAD documentato branch `33cb521d58120ce425a4aea06d49fb4e9eddb2d4`;
- Android CI run `33906280194`, job `101131852025`: **SUCCESS** completa.

### Integrazione `main` correzione M7.2

- PR `#9` — `fix(m7.2): move favorites to station cards`;
- PR verificato mergeable, 10 file modificati e HEAD bloccato a `33cb521d58120ce425a4aea06d49fb4e9eddb2d4`;
- merge **squash**;
- commit integrato `a0646b4e86f18d8c03ca2e15d96893178b105212`;
- Android CI reale di integrazione run `33906548734`, job `101132707262`: **SUCCESS** completa;
- test JVM, build debug APK, verifica firma persistente e upload artifact: tutti **SUCCESS**.

### Piano Release RC2 autorizzato

Obiettivo: produrre una vera GitHub prerelease `v0.5.2-m7.2-rc2`, installabile sopra la RC1 sul Galaxy S25 e costruita dall'esatto HEAD finale di `main` che contiene questo piano dopo relativa CI verde.

Il connettore GitHub disponibile non espone una mutazione diretta per creare un tag. È quindi autorizzato **solo per RC2** un bridge temporaneo sul branch dedicato `m7.2-release-rc2`:
- il branch nasce dall'esatto HEAD `main` pre-Release dopo CI verde;
- `main` e il suo workflow permanente `.github/workflows/android-release.yml` non vengono modificati dal bridge;
- sul solo branch temporaneo il workflow Release può ricevere un unico trigger push del branch oltre al comportamento tag;
- il bridge deve forzare checkout e `target_commitish` all'esatto SHA `main` pre-Release e verificare con `git rev-parse HEAD` lo SHA sorgente;
- il job deve eseguire `testDebugUnitTest`, `assembleDebug`, verifica certificato/public key persistenti, creare il tag `v0.5.2-m7.2-rc2` e pubblicare l'APK reale in GitHub Releases;
- il tag creato deve puntare direttamente all'esatto SHA `main` pre-Release, non al commit temporaneo del bridge;
- Release/asset/tag/firma/SHA-256 devono essere verificati prima di considerare RC2 pronta;
- dopo SUCCESS il branch bridge viene riallineato all'HEAD finale `main`, così il trigger temporaneo scompare; non viene dichiarato cancellato;
- nessuna modifica temporanea del bridge entra in `main`;
- il workflow Release permanente di `main` resta esclusivamente tag `v*`.

M7.2 resta aperta finché RC2 non è realmente pubblicata/verificata e non riceve una nuova prova reale sul Galaxy S25.

## M7.3 — grafico Storico configurabile dall'utente

Stato: **richiesta esplicita; contratto funzionale fissato, codice non iniziato finché M7.2 corretta non è chiusa/provata**.

### Obiettivo funzionale

Il grafico dello Storico non deve essere limitato a un solo carburante/servizio scelto globalmente. L'utente deve poter decidere quali andamenti confrontare nello stesso grafico.

### Contratto minimo M7.3

- supportare una o due serie visibili nello stesso grafico;
- ogni serie deve essere configurabile indipendentemente scegliendo almeno `Carburante` e `Servizio`;
- devono essere possibili almeno questi confronti sulla stessa stazione selezionata:
  - `Benzina Self` vs `Gasolio Self`;
  - `Benzina Servito` vs `Gasolio Servito`;
  - `Benzina Self` vs `Benzina Servito`;
- l'utente deve poter usare anche una sola serie;
- i controlli devono rendere evidente quale configurazione appartiene alla Serie A e quale alla Serie B;
- il grafico deve mostrare una legenda/identificazione chiara delle serie;
- le serie condividono lo stesso asse temporale della stazione selezionata;
- dati mancanti per una combinazione carburante/servizio non devono essere inventati o interpolati come prezzi reali;
- nessuna modifica alla selezione preferiti durante M7.3;
- contratto tecnico dettagliato/query/rendering/test verrà completato in spec prima del relativo codice, dopo PASS reale della M7.2 corretta.

## Requisito futuro già approvato

M7: pulsante `Indicazioni` su ogni stazione tramite intent verso Google Maps/app mappe compatibile, destinazione stazione e partenza dalla posizione corrente; preferire coordinate MIMIT valide con fallback indirizzo. Issue #1.

## Regola di avanzamento

Un checkpoint alla volta. M7.2 correttiva è l'unico checkpoint attivo. M7.3 parte solo dopo chiusura/prova M7.2 corretta. M6 resta non iniziata finché non viene esplicitamente autorizzata come obiettivo attivo.