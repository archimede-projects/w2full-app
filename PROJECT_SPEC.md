# W2Full — Project Specification

> **Fonte di verità del progetto.** Per M0–M5 e per il checkpoint GitHub Releases restano normativi tutti i requisiti, decisioni ed evidenze presenti in `PROJECT_SPEC.md` al commit `1eb6f5ab3b4a248e30e7a693ac0bc62ac311bffa`, salvo quanto esplicitamente modificato qui. I requisiti M0–M3 storici restano inoltre tracciati dal riferimento normativo già presente in quella spec al commit `749f9e44646113fb0c115c9a6685c73beee00b77`.

## Stato

- M0–M5: **chiuse**.
- Distribuzione GitHub Releases: **chiusa**.
- M6 — notifiche soglia: **non iniziata**.
- M7 — rifiniture: **in corso**.
- M7.1 — filtri/ordinamento Stazioni: **integrata e Release RC verificata; prova Galaxy S25 pendente**.
- M7.2 — preferiti nello Storico: **richiesta e accodata; non iniziata finché M7.1 non è chiusa/provata**.

## Evidenza reale M5 su dispositivo

- Release `v0.5.0-m5-rc1`, tag diretto al commit finale M5 `1eb6f5ab3b4a248e30e7a693ac0bc62ac311bffa`;
- Release run `33890959597`, job `101082219282`: **SUCCESS**;
- APK `w2full-v0.5.0-m5-rc1-debug.apk`, SHA-256 `4768319aecf910875cdabc5f020595434aeebb4bb09c32529902f4754a685806`;
- certificato persistente SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`;
- public key SHA-256 `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`;
- verifica reale Samsung Galaxy S25 confermata dall'utente il 4 settembre 2026: aggiornamento installato, schermata Stazioni e schermata Storico funzionanti **PASS**.

## M7.1 — filtri e ordinamento schermata Stazioni

### Contratto fissato prima del codice

- branch `m7-station-filters`, derivato dall'HEAD `main` `1eb6f5ab3b4a248e30e7a693ac0bc62ac311bffa`;
- contratto spec-first: commit `ed8e3969d758612d6bb9394a88c42ac418e15dda`;
- nuova card `Filtri` nella schermata Stazioni;
- filtro raggio opzionale, default `Nessun limite`;
- raggio configurabile `1..200` km, valore iniziale `20` km;
- raggio applicato solo con posizione disponibile; preferenza conservata ma non applicata se posizione negata/non disponibile;
- con filtro attivo, distanza nulla o superiore al limite comporta esclusione;
- ordinamento: `Distanza`, `Prezzo Self`, `Prezzo Servito`;
- `Distanza` conserva il ranking M4;
- prezzo crescente sul carburante selezionato, valori mancanti in fondo, tie-break distanza e poi ID stazione;
- filtro raggio prima dell'ordinamento;
- preferenze locali persistenti in storage privato Android, nessun account/cloud;
- conteggio risultati mostrati/totali;
- nessuna modifica a refresh MIMIT, cache Room, storico M5, filtro Eni o `defaultFuelType`;
- `versionCode = 8`, `versionName = 0.5.1-m7.1`;
- M7.2 preferiti Storico, M6 notifiche e pulsante `Indicazioni` fuori scope M7.1.

### Implementazione e branch CI

- commit funzionale `b22466587e9ca960a175f3196c0bd5f09f795af3`;
- fix compilazione `0e015eda3b58a2ed0797dd560a3d411743d306c6`;
- fix regression label M4 `c0b57c7962b17f57784a0642efc02310b7f6a805`;
- candidato documentato `d00401d1d83c4054b094c8ffdb7be2bdd8f8fde5`;
- run `33897001425`, job `101101872680`: **FAIL** compilazione `Modifier.weight`; corretto prima di qualsiasi merge;
- run `33897332878`, job `101102942309`: **FAIL** su un regression test testo M4; corretto prima di qualsiasi merge;
- run `33897609814`, job `101103839492`: **SUCCESS** completa;
- run sul vero HEAD documentato `33897902917`, job `101104806879`: **SUCCESS** completa;
- test JVM: 77 test **SUCCESS**;
- `assembleDebug`, `apksigner`, upload artifact: **SUCCESS**;
- certificato SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`;
- public key SHA-256 `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`;
- artifact candidato `w2full-debug-apk`, ID `9946406207`, digest ZIP SHA-256 `5fa254d687d9b860b1844df28bd792bb05fd2f3a85265cb9399e76ec5dd5cd7f`.

### Integrazione `main`

- PR `#7` — `feat(m7.1): add station radius and price sorting`;
- merge **squash** con HEAD atteso `d00401d1d83c4054b094c8ffdb7be2bdd8f8fde5`;
- commit integrato `1c97cc65a570f6c9220005ffa9541687b8e86386`;
- parent diretto `1eb6f5ab3b4a248e30e7a693ac0bc62ac311bffa`;
- Android CI reale su `main`: run `33898139949`, job `101105588935`: **SUCCESS**;
- test, build debug APK, verifica firma persistente e upload artifact: tutti **SUCCESS**;
- commit documentale pre-Release `316eec016a5dd7d309a07d68c819b17a8e2fbe70`;
- CI reale pre-Release run `33898389712`, job `101106388820`: **SUCCESS** completa.

### Release RC M7.1 verificata

Il connettore GitHub disponibile non espone una mutazione diretta per creare tag. Per questa sola RC è stato usato il bridge temporaneo autorizzato sul branch `m7.1-release-rc1`; nessuna modifica del bridge è entrata in `main`.

Evidenze reali:
- commit bridge `a040bc8aa1fbedb7ed6223d76a7b65e8a72c21c4` — `chore(release): publish M7.1 RC1 from final main`;
- Android Release run `33898639201`, job `101107200806`: **SUCCESS**;
- il workflow ha forzato checkout e `target_commitish` a `316eec016a5dd7d309a07d68c819b17a8e2fbe70` e ha verificato l'HEAD reale prima di test/build;
- `testDebugUnitTest`: **SUCCESS**;
- `assembleDebug`: **SUCCESS**;
- `apksigner`: **Verifies**, firma v2, un signer;
- certificato SHA-256 verificato: `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`;
- public key SHA-256 verificata: `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`;
- Release reale prerelease `v0.5.1-m7.1-rc1`, Release ID `382893738`;
- tag lightweight `v0.5.1-m7.1-rc1` → commit diretto `316eec016a5dd7d309a07d68c819b17a8e2fbe70`;
- asset `w2full-v0.5.1-m7.1-rc1-debug.apk`, asset ID `544644724`, size `14864022` byte;
- APK SHA-256 `3f5c8e138b7c3158d52026e3e135be15314d65fc3385a1dd932311cd36ba9a08`;
- Release non draft, prerelease, pubblicata da `github-actions[bot]`.

Dopo la CI verde di questo aggiornamento documentale, il branch temporaneo `m7.1-release-rc1` deve essere riallineato al nuovo HEAD `main`; in tal modo il suo workflow torna identico al permanente tag-only di `main` e non rimane alcun trigger branch temporaneo.

M7.1 resta aperta solo per la verifica reale sul Samsung Galaxy S25 della RC `v0.5.1-m7.1-rc1`. Dopo PASS utente verranno registrate la prova device e la chiusura M7.1; solo allora può iniziare M7.2.

## M7.2 — preferiti nello Storico — richiesta

Richiesta già autorizzata dall'utente ma non implementata durante M7.1. Obiettivo preliminare: marcare stazioni come preferite e trovarle/monitorarle prioritariamente nello Storico, mantenendo comunque accessibili le altre stazioni. Il contratto tecnico dettagliato verrà fissato qui **prima** del relativo codice.

## Requisito futuro già approvato

M7: pulsante `Indicazioni` su ogni stazione tramite intent verso Google Maps/app mappe compatibile, destinazione stazione e partenza dalla posizione corrente; preferire coordinate MIMIT valide con fallback indirizzo. Issue #1.

## Regola di avanzamento

Un checkpoint alla volta. M7.1 è l'unico checkpoint attivo e attende solo la prova RC sul Galaxy S25. M7.2 parte dopo chiusura/prova M7.1. M6 resta non iniziata finché non viene esplicitamente autorizzata come obiettivo attivo.