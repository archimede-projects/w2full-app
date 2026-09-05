# W2Full — Project Specification

> **Fonte di verità corrente.** Restano normativi tutti i requisiti e le decisioni presenti nella cronologia Git fino al candidato M6 `ce752eeb11a4a3eeb047fb7bc1e6493b30368518`, ora integrato su `main`, oltre ai vincoli storici M0–M7.4. I dettagli tecnici M6 restano fissati dal commit spec-first `f947c7e2f1307cdaad71af82513724ed836fb027`.

## Stato progetto

- M0–M5: **chiuse**.
- Distribuzione GitHub Releases: **chiusa**.
- M7.1 e M7.4: **chiuse e verificate su Samsung Galaxy S25**.
- M6 — notifiche soglia prezzo: **[~] implementata, integrata e pubblicata come RC1; PASS Galaxy S25 pendente**.
- Rifinitura finale interfaccia + issue #1 `Indicazioni`: **differite alla fine del piano funzionale**.

# M6 — Notifiche soglia prezzo

## Contratto normativo

M6 V1 usa una sola regola globale locale per il singolo veicolo:
- carburante monitorato;
- prezzo massimo in millesimi di euro/unità, validazione `0,500..5,000 €/L`;
- Self/Servito;
- brand Eni;
- raggio opzionale `1..200 km`;
- stato attivo;
- fingerprint anti-spam e timestamp ultima notifica.

Persistenza:
- Room schema `5`;
- migrazione esplicita `4→5`, senza destructive migration;
- tabella singleton `price_alert_rule`, `id = 1`;
- rifornimenti, veicolo, cache MIMIT, storico giornaliero e preferiti preservati.

Posizione:
- nessun `ACCESS_BACKGROUND_LOCATION`;
- ultima posizione ottenuta in foreground salvata localmente con timestamp;
- con raggio, posizione utilizzabile solo se più recente di 24 ore;
- posizione assente/scaduta => valutazione saltata senza notifica né mutazione anti-spam;
- senza raggio la posizione non è richiesta.

Valutazione:
- esclusivamente sulla cache MIMIT corrente validata;
- carburante e modalità devono coincidere;
- `price <= threshold` incluso;
- con raggio, coordinate valide e distanza Haversine entro soglia;
- risultati ordinati per prezzo, poi distanza, poi ID;
- nessun dato inventato/interpolato.

Anti-spam:
- fingerprint deterministica sull'insieme ordinato degli ID stazione qualificanti;
- primo insieme non vuoto => notifica;
- insieme invariato => nessuna nuova notifica;
- insieme cambiato => nuova notifica;
- insieme vuoto => reset fingerprint;
- modifica configurazione => reset fingerprint;
- notifica bloccata/permesso mancante => fingerprint non avanzata.

Trigger:
- refresh MIMIT manuale e WorkManager condividono la stessa callback post-refresh riuscito;
- refresh fallito non valuta;
- salvataggio/attivazione della regola valuta una volta la cache disponibile.

Notifiche Android:
- NotificationCompat;
- channel `w2full_price_alerts` / `Avvisi prezzo`;
- `POST_NOTIFICATIONS` dichiarato e richiesto on-demand su Android 13+;
- permesso negato durante l'attivazione => regola salvabile ma inattiva;
- tap notifica apre W2Full;
- nessun foreground service.

UI funzionale M6:
- quarta destinazione `Avviso prezzo` in Impostazioni;
- home Impostazioni non scrollabile;
- pagina dedicata compact/fixed-screen su Galaxy S25;
- controlli: attivo, carburante, Self/Servito, soglia, raggio opzionale, salva e stato permesso;
- touch target >= 48 dp;
- nessun ridisegno delle altre schermate.

Versione:
- `versionCode = 14`;
- `versionName = 0.6.0-m6`.

## Implementazione ed evidenze branch

Branch `m6-price-alerts` derivato dall'esatto `main` verde `7c4f6b88730ed69230bfd6517752d4bf5cda83f5`.

- spec-first: `f947c7e2f1307cdaad71af82513724ed836fb027`;
- candidato funzionale: `83a317375db0260c832b3f989219a5e718f09be5`;
- trigger CI temporaneo branch: `d162afd4ae64779c98b5adf6fe75269eebd24f1e`;
- HEAD documentato branch: `ce752eeb11a4a3eeb047fb7bc1e6493b30368518`.

Gate branch:
- Android CI `33967278629`, job `101309501352`: **SUCCESS completa**;
- Android CI sul vero HEAD documentato `33967446980`: **SUCCESS completa**;
- artifact candidato ID `9969854108`, ZIP SHA-256 `334f5fe5750741911cd80f73a94ea9e535c9a6089b9ffea1bbf75c5825725072`.

## Integrazione reale su main

- PR #13 `feat(m6): add local price threshold alerts`;
- expected head bloccato `ce752eeb11a4a3eeb047fb7bc1e6493b30368518`;
- squash merge riuscito;
- commit integrazione `9071082f7963b207dd83e42a171a7c7fd785003b`;
- Android CI post-merge `33968610131`, job `101313029178`: **SUCCESS completa**;
- commit documentale integrazione `78a5eeba0e93df59304aa40f25285da0105ed1b0`, CI `33968785072`, job `101313485420`: **SUCCESS completa**;
- rimosso esclusivamente il trigger temporaneo `m6-price-alerts`;
- sorgente Release finale `8889257fcce33c5b2f0c3339e3562990ec6f472d`;
- Android CI sorgente Release `33968903903`, job `101313798127`: **SUCCESS completa**.

Firma persistente invariata:
- certificato SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`;
- public key SHA-256 `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`.

## Release reale M6 RC1

Release `v0.6.0-m6-rc1` costruita dall'esatto sorgente `8889257fcce33c5b2f0c3339e3562990ec6f472d`.

Poiché il connettore GitHub non espone la creazione diretta di tag, è stato usato il bridge one-shot già adottato per le RC precedenti:
- branch `m6-release-rc1` creato dall'esatto sorgente finale;
- bridge commit `295efedf726a44bbb4c3fdefaa52f62ac5a1a547` modifica solo la copia branch del workflow Release;
- il workflow ha hardcodato tag e sorgente e ha effettuato checkout/test/build/firma sull'esatto commit `8889257fcce33c5b2f0c3339e3562990ec6f472d`, non sul bridge;
- dopo il successo `m6-release-rc1` è stato riallineato al sorgente finale;
- anche `m6-price-alerts` è stato riallineato al sorgente finale;
- il workflow Release permanente su `main` resta tag-only `v*`.

Evidenze:
- Android Release run `33969077794`, job `101314275101`: **SUCCESS completa**;
- tag lightweight `v0.6.0-m6-rc1` punta direttamente a `8889257fcce33c5b2f0c3339e3562990ec6f472d`;
- Release ID `383256019`, prerelease `true`, draft `false`;
- asset ID `545865110`;
- asset `w2full-v0.6.0-m6-rc1-debug.apk`;
- asset size `15208958` byte;
- APK SHA-256 `01a57437901559d0f166a7e5e8f93141e120f469fb32e1d3425f626d9a710c45`;
- APK `Verifies`, schema firma v2, un signer;
- certificato SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`;
- public key SHA-256 `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`.

## Gate dispositivo M6 ancora aperto

M6 **NON è chiusa** finché manca il PASS reale Samsung Galaxy S25 della Release `v0.6.0-m6-rc1`.

Verifica richiesta:
1. installazione sopra RC3 senza perdita dei dati locali;
2. `Impostazioni → Avviso prezzo` raggiungibile e utilizzabile senza page-scroll bloccante;
3. primo tentativo di attivazione richiede `POST_NOTIFICATIONS` sul Galaxy S25;
4. negando il permesso, la regola non resta attiva;
5. concedendo il permesso, soglia/carburante/modalità/raggio vengono salvati e la regola si attiva;
6. con soglia volutamente sopra un prezzo corrente qualificante, arriva una notifica locale e il tap apre W2Full;
7. refresh successivo con stesso insieme di stazioni qualificanti non genera spam duplicato;
8. raggio opzionale usa l'ultima posizione foreground senza richiesta di background location.

# Rifinitura finale differita

Resta fuori scope M6:
- ridisegno generale UI;
- issue #1 `Indicazioni`;
- export CSV e altre rifiniture finali;
- backfill storico MIMIT;
- manutenzione CI non necessaria.

Principio approvato fino al polish finale: privilegiare funzione/affidabilità/dati/test; evitare page-scroll nelle schermate principali quando possibile, e in Stazioni lasciare scrollabile soltanto la lista risultati.

# Vincoli invarianti

- app gratuita e locale;
- nessun account/backend/cloud proprietario/servizio a pagamento;
- Kotlin + Jetpack Compose + Material 3;
- target Samsung Galaxy S25;
- APK solo tramite GitHub Releases;
- firma debug persistente aggiornabile invariata;
- `PROJECT_SPEC.md` prima del codice/config per requisiti nuovi;
- un obiettivo alla volta;
- nessun test/build/commit simulato.
