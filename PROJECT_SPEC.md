# W2Full — Project Specification

> **Fonte di verità corrente.** Restano normativi tutti i requisiti e le decisioni presenti nella cronologia Git fino al candidato M6 `ce752eeb11a4a3eeb047fb7bc1e6493b30368518`, ora integrato su `main`, oltre ai vincoli storici M0–M7.4. Questo documento compatta lo stato corrente senza sostituire i dettagli tecnici già fissati nel commit spec-first M6 `f947c7e2f1307cdaad71af82513724ed836fb027`.

## Stato progetto

- M0–M5: **chiuse**.
- Distribuzione GitHub Releases: **chiusa**.
- M7.1 e M7.4: **chiuse e verificate su Samsung Galaxy S25**.
- M6 — notifiche soglia prezzo: **[~] implementata e integrata; Release/device gate pendenti**.
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

Primo gate branch:
- Android CI `33967278629`, job `101309501352`: **SUCCESS completa**;
- JVM tests, `assembleDebug`, firma e artifact: **SUCCESS**;
- artifact ID `9969854108`;
- artifact ZIP SHA-256 `334f5fe5750741911cd80f73a94ea9e535c9a6089b9ffea1bbf75c5825725072`.

Gate sul vero HEAD documentato:
- Android CI `33967446980`: **SUCCESS completa**.

## Integrazione reale su main

- PR #13 `feat(m6): add local price threshold alerts`;
- base `7c4f6b88730ed69230bfd6517752d4bf5cda83f5`;
- expected head bloccato `ce752eeb11a4a3eeb047fb7bc1e6493b30368518`;
- squash merge riuscito;
- commit integrazione `main`: `9071082f7963b207dd83e42a171a7c7fd785003b`;
- Android CI post-merge run `33968610131`, job `101313029178`: **SUCCESS completa**;
- JVM tests, build APK, verifica firma e upload artifact: **SUCCESS**.

Firma persistente invariata:
- certificato SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`;
- public key SHA-256 `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`.

## Gate M6 ancora aperti

1. rimuovere esclusivamente il trigger CI temporaneo `m6-price-alerts`;
2. Android CI reale sul commit finale sorgente Release: deve essere **SUCCESS**;
3. pubblicare Release reale `v0.6.0-m6-rc1` dall'esatto SHA finale `main`;
4. verificare tag, asset APK, SHA-256 e firma persistente;
5. prova reale Samsung Galaxy S25;
6. chiudere M6 solo dopo PASS dispositivo.

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
