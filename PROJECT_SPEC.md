# W2Full — Project Specification

> **Fonte di verità del progetto.** Questa versione è l'overlay di integrazione M4. Restano normativi tutti i requisiti M0–M3 presenti nel precedente `PROJECT_SPEC.md` al commit `749f9e44646113fb0c115c9a6685c73beee00b77` e tutti i contratti/evidenze M4 documentati nel candidato verificato al commit `69b4259e855ea35eb9dac1ab5112290837a45933`, salvo quanto esplicitamente modificato qui.

## Stato

- M0–M3: **chiuse**.
- M4 — integrazione MIMIT: **candidato RC1 verificato realmente sul Galaxy S25; integrazione pulita su `main` in corso**.
- M5–M7: non iniziate.
- Distribuzione GitHub Releases: intervento infrastrutturale **separato da M4**; il relativo cleanup non fa parte di questa integrazione.

## Contratto di integrazione M4 pulita

Autorizzazione utente: 3 settembre 2026, dopo esito `pass` su `v0.4.0-rc1`.

La branch `m4-integration-clean` parte esattamente da `main` (`749f9e44646113fb0c115c9a6685c73beee00b77`) e deve portare su main esclusivamente lo stato funzionale M4 già verificato. Non deve incorporare la storia o i file del checkpoint distribuzione.

Incluso:
- client/parser MIMIT e contratti CSV;
- filtro Eni/Agip Eni;
- posizione foreground, Haversine e timeout;
- schermata Stazioni con distanza, indirizzo, prezzi Self/Servito e cache;
- Room v2 + migrazione 1→2 e refresh atomico/resiliente;
- WorkManager sync;
- schermata Veicolo e `defaultFuelType` configurabile;
- test M4 e regressioni M3;
- dipendenze/config Android richieste da M4;
- `versionCode = 6`, `versionName = 0.4.0-rc1` come contenuto già verificato.

Escluso esplicitamente:
- `.github/workflows/android-release.yml`;
- `.github/workflows/bootstrap-preview-release.yml`;
- qualsiasi cleanup o modifica ulteriore della pipeline Release;
- nuove funzionalità oltre M4.

La CI di integrazione deve eseguire `testDebugUnitTest`, `assembleDebug`, verifica `apksigner` e certificato persistente. Solo dopo CI verde il ref `main` può avanzare al commit di chiusura M4. Dopo l'avanzamento va eseguita e verificata anche la CI reale su `main`.

## Evidenza RC1 già verificata

Release `v0.4.0-rc1`, tag annotato `5c1b2f7bba64e205bb3b1d84be9db7cbf1140602` → commit `69b4259e855ea35eb9dac1ab5112290837a45933`.

Run Release `33716404065`; job `100526422722`: **SUCCESS**. APK SHA-256 `616ea026b3d3e80ab7e7e865df624a7df088479f9ecc4838015d2b14f8d846ef`. Firma persistente attesa SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`.

Verifica reale Galaxy S25: **PASS** il 3 settembre 2026 su upgrade dalla preview.4, con Registro persistente, cache/prezzi, refresh MIMIT, posizione/ranking, cambio carburante e persistenza al riavvio senza regressioni segnalate.

## Requisito futuro già approvato

M7: pulsante `Indicazioni` su ogni stazione, tramite intent verso Google Maps/app mappe compatibile, destinazione stazione e partenza dalla posizione corrente; preferire coordinate MIMIT valide con fallback indirizzo. Issue #1.

## Chiusura M4

M4 sarà marcata **chiusa** soltanto quando:
1. il contenuto M4 pulito è su `main` senza i file distribuzione esclusi;
2. la CI reale di `main` è verde;
3. il repository è ricontrollato e lo stato finale è documentato.
