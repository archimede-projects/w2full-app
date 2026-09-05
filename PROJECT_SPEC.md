# W2Full — Project Specification

> **Fonte di verità corrente.** Restano normativi tutti i requisiti, le decisioni, i vincoli di firma e le verifiche presenti nella cronologia Git fino al sorgente Release RC3 `bcb02e9a1e1c1476a26873bf209356a4a721b677`, salvo quanto esplicitamente sostituito qui. M6 resta non iniziata. M7.4 è l'unico checkpoint attivo e non è chiuso finché manca il PASS reale su Galaxy S25.

## Stato progetto

- M0–M5: **chiuse**.
- Distribuzione GitHub Releases: **chiusa**.
- M6 — notifiche soglia: **non iniziata**.
- M7.1 — filtri Stazioni: **chiusa e verificata su Galaxy S25**.
- M7.2 RC1/RC2: **FAIL UX**, sostituita da M7.4.
- M7.4 RC1 `v0.5.3-m7.4-rc1`: tecnicamente verde, **FAIL UX su Galaxy S25**.
- M7.4 RC2 `v0.5.3-m7.4-rc2`: tecnicamente verde, **PASS UX parziale / FAIL funzionale** su posizione Stazioni e grafico incompleto.
- M7.4 RC3 `v0.5.3-m7.4-rc3`: **Release reale verificata; prova Galaxy S25 pendente**.

# M7.4 RC3 — contratto implementato

RC3 mantiene il layout fixed-screen approvato in RC2 e corregge esclusivamente posizione e grafico.

## Posizione Stazioni

- `Riprova` richiede una nuova localizzazione reale e ricalcola le distanze sulle stazioni già in cache senza obbligare un nuovo download MIMIT.
- Una posizione valida appena ottenuta non viene sovrascritta da uno snapshot transitoriamente unavailable nella stessa sessione.
- Raggio e ordinamento distanza usano immediatamente le distanze ricalcolate.
- Con raggio attivo e posizione disponibile, risultati fuori raggio o senza distanza vengono esclusi.
- La UI mostra `📍 Comune (Provincia)` tramite reverse geocoding Android di sistema; fallback `📍 Posizione rilevata` se il nome non è disponibile.
- Nessuna coordinata grezza nella schermata principale.
- Permission denied/unavailable mantiene azione `Consenti`/`Riprova` appropriata.

## Grafico Storico

- asse Y a sinistra in `€/L` con tick leggibili;
- scala Y calcolata sui dati della viewport visibile con margine e range non nullo;
- asse X con date compatte italiane;
- nessuna interpolazione artificiale dei giorni mancanti e linee interrotte sui gap reali;
- tap seleziona il giorno/osservazione più vicino;
- trascinamento non zoomato permette lo scrub della selezione;
- cursore verticale e dettaglio con data completa + Serie A/B;
- serie assente nello stesso giorno = `n.d.`, senza valore inventato;
- pinch-to-zoom orizzontale;
- pan orizzontale quando zoomato, clampato ai limiti dati;
- scala Y ricalcolata durante zoom/pan;
- doppio tap resetta zoom, pan e selezione;
- cambio periodo resetta viewport;
- con meno di due giorni utili resta lo stato compatto `Storico in costruzione`.

## Layout e compatibilità invariati

- Stazioni: niente page-scroll, solo lista risultati scrollabile.
- Storico principale: non scrollabile.
- Impostazioni: sole destinazioni `Veicolo`, `Stazioni preferite`, `Informazioni`.
- target touch grandi invariati.
- Room schema 4 e storico giornaliero `observedOn` invariati.
- preferiti e preferenze locali invariati.
- nessun account/cloud proprietario/servizio a pagamento.
- firma debug persistente invariata.

## Versione

- `versionCode = 13`;
- `versionName = 0.5.3-m7.4-rc3`.

# Evidenze tecniche RC3

## Spec-first e branch

- branch `m7.4-ux-rc3` derivato dall'esatto `main` `57dbb9f00498ffdd97ba9e8fd78b1bb5333e0dea`;
- commit spec-first `e5d198fdda50092d12b4e271be488b064ea6205d`.

## Implementazione e gate branch

Implementato:
- fresh location request e ricalcolo cache in `NearbyStationsViewModel`;
- `LocationLabelResolver` con Android Geocoder e fallback;
- grafico interattivo Compose + modello matematico puro viewport/tick/selezione/zoom/pan;
- test dedicati posizione e chart math.

Primo gate:
- HEAD `843906e02f9fda400e91e947c84ccdfcc1492d9b`;
- Android CI run `33959603212`, job `101289046922`: **FAIL** in `compileDebugKotlin` per due import espliciti Compose `weight` che risolvevano a simbolo interno;
- nessun merge eseguito.

Correzione limitata agli import.

Candidato funzionale:
- HEAD `5ac2f970494972d48af7c09e3517f3cc9c2ad983`;
- Android CI run `33959732692`, job `101289390232`: **SUCCESS completa**;
- JVM tests, `assembleDebug`, firma e artifact: **SUCCESS**;
- artifact `w2full-debug-apk`, ID `9967565312`;
- artifact ZIP SHA-256 `fbfaa04038d97318ade88cc62d21e85d8c1b31634f3e3e0d51a23d7f57520453`.

HEAD documentale finale branch:
- `463b47cfed1a8a37748ed168a5772bb3fe62f64a`;
- Android CI run `33959910103`, job `101289874608`: **SUCCESS completa**.

## Integrazione su main

- PR #12 `fix(m7.4): current location and interactive history chart RC3`;
- base `57dbb9f00498ffdd97ba9e8fd78b1bb5333e0dea`;
- head validato `463b47cfed1a8a37748ed168a5772bb3fe62f64a`;
- squash merge eseguito bloccando l'expected head SHA;
- commit integrazione `main`: `40b9a0f2d6b169e2c05a6ab5f11c7ab7fb7883c9`;
- Android CI post-merge run `33960093309`, job `101290364358`: **SUCCESS completa**.

## Cleanup CI finale sorgente Release

- rimosso esclusivamente il trigger temporaneo `m7.4-ux-rc3` da `.github/workflows/android-ci.yml`;
- commit sorgente finale `main`: `bcb02e9a1e1c1476a26873bf209356a4a721b677`;
- Android CI run `33961085355`, job `101292966146`: **SUCCESS completa**;
- test JVM, build APK, verifica firma e upload artifact: **SUCCESS**.

# Release reale RC3

Tag: `v0.5.3-m7.4-rc3`.

Poiché il connettore GitHub disponibile non espone una mutazione diretta di tag, è stato usato lo stesso bridge one-shot già adottato per le RC precedenti:
- branch temporaneo operativo `m7.4-release-rc3` creato dall'esatto sorgente `bcb02e9a1e1c1476a26873bf209356a4a721b677`;
- solo la copia branch di `android-release.yml` ha aggiunto il trigger branch e hardcodato tag + sorgente;
- bridge commit `a9a7f650dbdcb1c161264680a6f65fe52d2cf429`;
- checkout/build/test/firma effettuati sull'esatto sorgente `bcb02e9a1e1c1476a26873bf209356a4a721b677`, non sul bridge commit;
- workflow permanente su `main` è rimasto tag-only;
- dopo la Release il branch `m7.4-release-rc3` è stato forzatamente riallineato al sorgente `bcb02e9a1e1c1476a26873bf209356a4a721b677`, quindi il trigger temporaneo non resta nello stato finale del branch.

Evidenza Release:
- Android Release run `33961205244`, job `101293280752`: **SUCCESS completa**;
- checkout confermato: `bcb02e9a1e1c1476a26873bf209356a4a721b677`;
- JVM tests: **SUCCESS**;
- `assembleDebug`: **SUCCESS**;
- APK `Verifies`;
- APK Signature Scheme v2: `true`;
- numero signer: `1`;
- certificato SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`;
- public key SHA-256 `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`;
- APK SHA-256 `53ff5d981ee752930f65edf197e460623c7c16e417914725404cadc08fa6f752`;
- Release ID `383216022`, prerelease `true`, draft `false`;
- asset ID `545706233`;
- asset `w2full-v0.5.3-m7.4-rc3-debug.apk`;
- asset size `15142766` byte;
- digest asset GitHub `sha256:53ff5d981ee752930f65edf197e460623c7c16e417914725404cadc08fa6f752`;
- tag lightweight `refs/tags/v0.5.3-m7.4-rc3` punta direttamente a `bcb02e9a1e1c1476a26873bf209356a4a721b677`.

## Gate dispositivo ancora aperto

M7.4 **NON è chiusa**. Serve prova reale su Samsung Galaxy S25 della Release `v0.5.3-m7.4-rc3`.

Verifica richiesta:
1. aggiornamento/installazione sopra RC2 senza perdita dati;
2. Stazioni: posizione leggibile e coerente, `Riprova` effettivo, distanze/raggio corretti;
3. Storico: assi X/Y leggibili;
4. tap/scrub giorno con valori A/B corretti e `n.d.` dove manca una serie;
5. pinch zoom, pan e doppio-tap reset;
6. nessuna regressione su layout fixed-screen, preferiti, Impostazioni e storico giornaliero.

# Fuori scope

- M6 notifiche soglia;
- issue #1 `Indicazioni`;
- backfill storico MIMIT antecedente alle osservazioni locali;
- manutenzione CI non necessaria;
- nuove funzioni non approvate.
