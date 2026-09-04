# W2Full — Project Specification

> **Fonte di verità del progetto.** Questa versione chiude M4 e apre esclusivamente il checkpoint infrastrutturale di distribuzione GitHub Releases, separato da M5. Restano normativi tutti i requisiti M0–M3 presenti nel precedente `PROJECT_SPEC.md` al commit `749f9e44646113fb0c115c9a6685c73beee00b77` e tutti i contratti/evidenze M4 documentati nel candidato verificato al commit `69b4259e855ea35eb9dac1ab5112290837a45933`, salvo quanto esplicitamente modificato qui.

## Stato

- M0–M4: **chiuse**.
- M5 — storico prezzi + grafico: **non iniziata**.
- M6 — notifiche soglia: **non iniziata**.
- M7 — rifiniture: **non iniziata**.
- Distribuzione GitHub Releases: checkpoint infrastrutturale **in corso**, separato da M4/M5.

## M4 — integrazione MIMIT chiusa

M4 è stata integrata su `main` il 3 settembre 2026 tramite integrazione pulita, senza incorporare i file o la storia del checkpoint distribuzione.

Commit funzionale integrato:
`343425cb3e87b140ed7c32f145de83f3ca399183` — `feat(m4): integrate verified MIMIT milestone`.

Contenuto M4 su `main`:
- client/parser MIMIT e contratti CSV;
- correzione mirata dell'artefatto reale `| gestori.prezzibenzina.it` mantenendo validazione stretta;
- filtro Eni exact-match su `eni` e `agip eni`;
- posizione foreground, Haversine, fallback e timeout applicativo di 12 secondi;
- schermata Stazioni con ranking, indirizzo, distanza, prezzi Self/Servito e timestamp cache;
- refresh atomico/resiliente, logger `W2Full-MIMIT`, Room v2 e migrazione 1→2;
- WorkManager sync con stessa semantica del refresh manuale;
- schermata Veicolo con `defaultFuelType` configurabile e prezzi aggiornati dalla cache senza nuovo download;
- regression test M3/M4;
- `versionCode = 6`, `versionName = 0.4.0-rc1`, corrispondenti al contenuto funzionale realmente provato.

## Evidenze finali M4

RC realmente provata:
- Release `v0.4.0-rc1`;
- tag annotato `5c1b2f7bba64e205bb3b1d84be9db7cbf1140602` → commit `69b4259e855ea35eb9dac1ab5112290837a45933`;
- Release run `33716404065`, job `100526422722`: **SUCCESS**;
- APK SHA-256 `616ea026b3d3e80ab7e7e865df624a7df088479f9ecc4838015d2b14f8d846ef`;
- verifica reale Galaxy S25: **PASS** il 3 settembre 2026.

Integrazione pulita:
- branch `m4-integration-clean` derivata direttamente dal vecchio `main` `749f9e44646113fb0c115c9a6685c73beee00b77`;
- CI branch run `33718156047`, job `100531588029`: **SUCCESS** con test JVM, build, verifica firma e artifact;
- confronto col vecchio main conferma assenza di `.github/workflows/android-release.yml` e `.github/workflows/bootstrap-preview-release.yml`;
- `main` avanzato a `343425cb3e87b140ed7c32f145de83f3ca399183`;
- CI reale su main run `33718342202`, job `100532144968`: **SUCCESS** con test JVM, build, verifica firma e artifact.

Certificato persistente atteso e verificato lungo M4: SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`; public key SHA-256 `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`.

## Checkpoint distribuzione GitHub Releases — contratto di chiusura

Questo intervento è esclusivamente infrastrutturale. Non introduce funzionalità M5 e non modifica comportamento, dati o UI dell'app.

Base di lavoro:
- branch pulito `release-distribution-clean` creato dall'HEAD di `main` `3abd465cd9f049bc45b552a68d174f3d83b75191`;
- il vecchio branch divergente `release-distribution` non deve essere mergeato in `main`;
- a chiusura, il checkpoint legacy deve essere riallineato al commit finale verificato, senza trascinare la sua vecchia storia nel merge funzionale.

Workflow permanente richiesto:
- unico file permanente da integrare: `.github/workflows/android-release.yml`;
- trigger esclusivamente su `push` di tag `v*`;
- nessun `workflow_call` permanente;
- nessun `workflow_dispatch` per pubblicare APK;
- checkout del commit realmente taggato;
- test JVM `testDebugUnitTest` prima della build;
- build `assembleDebug`;
- uso obbligatorio del keystore debug persistente via secrets: se un secret di firma manca, la Release deve fallire;
- verifica con `apksigner` del certificato SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265` e della public key SHA-256 `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`;
- APK versionato come `w2full-<tag>-debug.apk`;
- calcolo SHA-256 dell'APK e pubblicazione del digest nel corpo della GitHub Release;
- APK distribuito esclusivamente come asset di una vera GitHub Release.

Validazione obbligatoria prima dell'integrazione:
- è ammesso un solo helper temporaneo sul branch di lavoro per creare automaticamente un tag di prova `v*`, necessario esclusivamente a validare il vero evento `push` del tag con gli strumenti disponibili;
- l'helper non deve chiamare il workflow permanente tramite `workflow_call`: deve soltanto creare e pushare il tag;
- la Release di prova deve essere generata dal workflow permanente tramite il vero trigger `push.tags: v*`;
- test, build, firma, certificato/public key, APK versionato, SHA-256 e pubblicazione Release devono risultare tutti verdi;
- l'helper temporaneo deve essere rimosso dal branch prima dell'integrazione;
- `.github/workflows/bootstrap-preview-release.yml` non deve risultare presente nel tree finale di `main`;
- dopo l'integrazione su `main`, deve risultare verde anche la CI Android ordinaria sul commit integrato;
- il checkpoint non è chiuso finché `PROJECT_SPEC.md` non registra commit, run/job e SHA-256 finali realmente verificati.

## Isolamento distribuzione

Fino alla chiusura del checkpoint, `main` resta privo del workflow Release permanente. M5 resta esplicitamente bloccata e non deve essere iniziata durante questo intervento.

## Requisito futuro già approvato

M7: pulsante `Indicazioni` su ogni stazione, tramite intent verso Google Maps/app mappe compatibile, destinazione stazione e partenza dalla posizione corrente; preferire coordinate MIMIT valide con fallback indirizzo. Issue #1.

## Regola di avanzamento

Nessuna M5 o milestone successiva parte senza nuova autorizzazione esplicita dell'utente.
