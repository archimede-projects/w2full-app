# W2Full — Project Specification

> **Fonte di verità del progetto.** Per M0–M5 e per il checkpoint GitHub Releases restano normativi tutti i requisiti, decisioni ed evidenze presenti in `PROJECT_SPEC.md` al commit `1eb6f5ab3b4a248e30e7a693ac0bc62ac311bffa`, salvo quanto esplicitamente modificato qui. I requisiti M0–M3 storici restano inoltre tracciati dal riferimento normativo già presente in quella spec al commit `749f9e44646113fb0c115c9a6685c73beee00b77`.

## Stato

- M0–M5: **chiuse**.
- Distribuzione GitHub Releases: **chiusa**.
- M6 — notifiche soglia: **non iniziata**.
- M7 — rifiniture: **in corso**.
- M7.1 — filtri/ordinamento Stazioni: **[x] chiusa e verificata sul Galaxy S25**.
- M7.2 — preferiti nello Storico: **[~] integrata su `main`, CI verde; Release RC e prova Galaxy S25 pendenti**.
- M7.3 — confronto configurabile serie nello Storico: **richiesta e accodata; non iniziata finché M7.2 non è chiusa/provata**.

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

## M7.2 — stazioni preferite nello Storico

Stato: **[~] integrata su `main`, CI verde; Release RC e prova Galaxy S25 pendenti**.

### Obiettivo

Rendere lo Storico utile anche quando contiene molte stazioni lontane: l'utente deve poter marcare come preferite le stazioni che vuole seguire e ritrovarle subito, mantenendo comunque accessibili tutte le altre stazioni con storico.

### Contratto UI e comportamento

- branch di lavoro `m7-history-favorites`, derivato dall'HEAD `main` `67ad7fd46c2a83d74e56d85358599d994187bbe1`;
- contratto spec-first commit `19a6f7ecad648f4da841fb9dda780bfbffa0a1f1`;
- i preferiti sono identificati esclusivamente da `stationId` MIMIT;
- storage esclusivamente locale tramite `SharedPreferences` private Android; nessun account/cloud e nessuna migrazione Room;
- nella schermata `Storico` la stazione selezionata espone un controllo esplicito `☆ Aggiungi ai preferiti` / `★ Preferita`;
- il toggle è immediato, persistente e non modifica cache/storico MIMIT;
- il selettore stazioni distingue chiaramente `Preferite` e `Altre stazioni` quando esiste almeno un preferito;
- i chip delle preferite mostrano il marker `★` e restano selezionabili come gli altri;
- le stazioni non preferite restano sempre accessibili;
- al primo ingresso/riavvio, se la selezione corrente non è più valida e ci sono preferiti, viene selezionata per prima una stazione preferita; in assenza preferiti resta il fallback M5 alla prima stazione disponibile;
- aggiungere/rimuovere un preferito non azzera la selezione corrente né cambia carburante/servizio se la stessa stazione resta selezionata;
- se una stazione preferita non è più presente tra quelle con storico, il suo ID resta memorizzabile ma non viene mostrato finché non ricompare;
- `versionCode = 9`, `versionName = 0.5.2-m7.2`;
- nessuna modifica al grafico/serie M5 durante M7.2;
- M7.3, M6 e pulsante `Indicazioni` restano fuori scope di questo checkpoint.

### Implementazione e test M7.2

- commit funzionale `992b42f1b7a74b8e47d0e2f82ea2e18c8702baae` — `feat(m7.2): add favorite history stations`;
- nuovo `HistoryFavoriteStationsStore` con implementazione `SharedPreferences` privata;
- funzioni pure per raggruppamento preferite/altre, risoluzione selezione e toggle set preferiti;
- `PriceHistoryViewModel` carica i preferiti all'avvio, conserva la selezione valida e persiste il toggle;
- `PriceHistoryScreen` mostra sezione `Preferite`, `Altre stazioni`, marker `★` e controllo `☆/★` sulla stazione selezionata;
- `W2FullApplication` espone lo store locale senza modificare Room;
- CI ordinaria modificata esclusivamente per aggiungere `m7-history-favorites` ai branch trigger; nessuna versione/action CI aggiornata;
- test dedicati coprono store vuoto, persistenza multipla/rimozione, separazione senza perdita stazioni, fallback preferita, conservazione selezione e toggle della stazione selezionata;
- regression test esistenti M3–M7.1 restano inclusi.

### Evidenze branch M7.2

- primo candidato funzionale run `33902789841`, job `101120543115`: **SUCCESS** completa;
- candidato documentato `2c274458519392baeb6efc34b2ac3949409d276e`;
- CI sul vero HEAD documentato run `33903014671`, job `101121279791`: **SUCCESS** completa;
- `testDebugUnitTest`, `assembleDebug`, verifica APK/firma e upload artifact: tutti **SUCCESS**;
- certificato SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`;
- public key SHA-256 `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`;
- artifact del primo candidato `w2full-debug-apk`, ID `9948318368`, size archivio `14287141` byte, digest ZIP SHA-256 `840e058c34db05d7709e634712a6212469b1c199e25324e98fc274ae0e29e6c8`.

### Integrazione `main` M7.2

- PR `#8` — `feat(m7.2): add favorite history stations`;
- merge **squash** con HEAD atteso `2c274458519392baeb6efc34b2ac3949409d276e`;
- commit integrato su `main` `5a9bc5c717b71d8bae8994f3484ab928836104f5`;
- Android CI reale di integrazione run `33903257045`, job `101122079944`: **SUCCESS** completa;
- test JVM, build debug APK, verifica firma persistente e upload artifact: tutti **SUCCESS**.

### Piano Release RC M7.2 autorizzato

Obiettivo: produrre un APK reale installabile sul Galaxy S25, tag `v0.5.2-m7.2-rc1`, costruito dall'esatto commit finale di `main` dopo questo commit documentale pre-Release e dopo relativa CI verde.

Il connettore GitHub disponibile non espone una mutazione diretta per creare tag. È quindi autorizzato **solo per questa Release RC** un bridge temporaneo sul branch dedicato `m7.2-release-rc1`:
- il branch nasce dall'HEAD `main` da rilasciare;
- `android-release.yml` sul branch temporaneo riceve un unico trigger push del branch oltre al comportamento tag e forza checkout/`target_commitish` all'esatto SHA finale `main`;
- il job deve confermare con `git rev-parse HEAD` di stare costruendo lo SHA target;
- la build deve eseguire test JVM, `assembleDebug`, verifica certificato/public key persistenti e pubblicare una vera GitHub prerelease `v0.5.2-m7.2-rc1`;
- il tag creato dalla Release deve puntare direttamente allo SHA finale `main`;
- dopo SUCCESS e verifica Release/tag/asset, il branch temporaneo viene riallineato all'HEAD finale `main`, rimuovendo ogni trigger temporaneo;
- nessuna modifica temporanea del bridge entra in `main`;
- il workflow permanente di `main` resta esclusivamente tag `v*`.

M7.2 resta aperta finché la Release reale non esiste, asset/firma/hash/tag non sono verificati, le evidenze finali non sono registrate e il Galaxy S25 non ha una build concreta da provare.

## M7.3 — confronto configurabile serie nello Storico — richiesta

Richiesta già autorizzata dall'utente ma non implementata finché M7.2 non è chiusa/provata. Obiettivo funzionale: consentire di visualizzare nello stesso grafico almeno due serie configurabili della stazione selezionata, scegliendo in modo indipendente carburante e modalità di servizio per ciascuna serie. Deve coprire almeno i casi `Benzina Self vs Benzina Servito` e `Benzina Servito vs Gasolio/Diesel Servito`, mantenendo possibile anche la visualizzazione di una sola serie. Il contratto tecnico dettagliato verrà fissato qui **prima** del relativo codice.

## Requisito futuro già approvato

M7: pulsante `Indicazioni` su ogni stazione tramite intent verso Google Maps/app mappe compatibile, destinazione stazione e partenza dalla posizione corrente; preferire coordinate MIMIT valide con fallback indirizzo. Issue #1.

## Regola di avanzamento

Un checkpoint alla volta. M7.2 è l'unico checkpoint attivo. M7.3 parte solo dopo chiusura/prova M7.2. M6 resta non iniziata finché non viene esplicitamente autorizzata come obiettivo attivo.
