# W2Full — Project Specification

> **Fonte di verità corrente.** Restano normativi tutti i requisiti, le decisioni, i vincoli di firma e le evidenze presenti nella cronologia Git fino a `main` `7c4f6b88730ed69230bfd6517752d4bf5cda83f5`, che chiude M7.4 dopo PASS reale Galaxy S25. Quanto segue definisce M6 prima di qualsiasi modifica al codice/config.

## Stato progetto

- M0–M5: **chiuse**.
- Distribuzione GitHub Releases: **chiusa**.
- M6 — notifiche soglia prezzo: **[~] checkpoint attivo**.
- M7.1 e M7.4: **chiuse e verificate su Galaxy S25**.
- Rifinitura finale interfaccia + issue #1 `Indicazioni`: **differite alla fine del piano funzionale**.

# M6 — Notifiche soglia prezzo

Branch: `m6-price-alerts`, derivato dall'esatto `main` verde `7c4f6b88730ed69230bfd6517752d4bf5cda83f5`.

Commit spec-first: `f947c7e2f1307cdaad71af82513724ed836fb027`.

## 1. Obiettivo

Permettere all'utente di configurare un avviso locale quando il prezzo Eni del carburante scelto scende sotto una soglia, sfruttando il refresh MIMIT già esistente e notifiche Android locali, senza account, backend, cloud o servizi a pagamento.

M6 V1 usa **una sola regola di avviso globale** coerente con il singolo veicolo V1. Il modello resta estendibile a più regole in futuro.

## 2. Regola di avviso V1

La regola persistente contiene:
- `id = 1` fisso;
- `fuelDescription`: carburante monitorato; default al carburante del veicolo quando la regola viene creata;
- `maxPriceMilliEuroPerUnit`: prezzo massimo accettato, memorizzato come intero in millesimi di euro/unità;
- `isSelf`: `true` Self, `false` Servito;
- `brand`: `Eni` in M6 V1; il campo resta esplicitamente modellato per estensioni future;
- `radiusKm`: opzionale; `null` = nessun limite, altrimenti intero `1..200` km;
- `isActive`;
- `lastNotifiedFingerprint`: fingerprint anti-spam nullable;
- `lastNotifiedAtEpochMillis`: nullable;
- `updatedAtEpochMillis`.

Validazione soglia UI/repository: prezzo `0,500..5,000 €/L`; valori fuori range o non numerici non vengono salvati.

## 3. Persistenza

Room passa da schema `4` a `5` con migrazione esplicita `4→5`, senza destructive migration.

Nuova tabella `price_alert_rule` con una sola riga V1 (`id = 1`). Nessuna modifica distruttiva alle tabelle M3–M7.4; rifornimenti, veicolo, cache MIMIT, storico giornaliero e preferiti restano invariati.

La migrazione deve essere coperta dai test esistenti e da un test specifico 4→5.

## 4. Posizione per il raggio senza background-location permission

M6 **non richiede** `ACCESS_BACKGROUND_LOCATION`.

Quando l'app ottiene una posizione valida in foreground, salva localmente l'ultima posizione (`lat`, `lon`, timestamp) in storage privato.

Per una regola con `radiusKm`:
- l'evaluazione background usa soltanto l'ultima posizione foreground salvata;
- la posizione è considerata utilizzabile se ha meno di 24 ore;
- se manca o è più vecchia di 24 ore, l'evaluazione con raggio viene **saltata senza notificare e senza alterare lo stato anti-spam**;
- la UI Avviso prezzo deve indicare in modo compatto che il raggio usa l'ultima posizione rilevata nell'app;
- nessuna coordinata grezza viene mostrata all'utente.

Senza raggio (`radiusKm = null`) la regola può essere valutata senza posizione.

## 5. Valutazione della soglia

L'evaluatore usa esclusivamente la cache MIMIT corrente validata.

Una stazione qualifica se:
- è Eni, come già garantito dalla cache M4;
- dispone del carburante `fuelDescription` richiesto;
- la modalità `isSelf` coincide;
- `priceMilliEuroPerUnit <= maxPriceMilliEuroPerUnit`;
- se il raggio è attivo, la stazione ha coordinate valide e distanza `<= radiusKm` dalla posizione foreground recente.

Le stazioni qualificanti vengono ordinate per prezzo crescente, poi distanza quando disponibile, poi ID stabile.

La notifica riassume il risultato:
- una stazione: nome, carburante/modalità, prezzo e distanza se disponibile;
- più stazioni: numero di stazioni e migliore prezzo/stazione.

Nessun prezzo viene interpolato o dedotto da dati mancanti.

## 6. Anti-spam

La fingerprint è calcolata in modo deterministico sull'insieme ordinato degli ID delle stazioni attualmente sotto soglia.

Regole:
- primo insieme non vuoto dopo attivazione/reset → una notifica;
- stesso insieme qualificante delle valutazioni successive → nessuna nuova notifica, anche se passa un altro refresh giornaliero;
- insieme che cambia → nuova notifica;
- nessuna stazione qualificante → `lastNotifiedFingerprint` viene azzerata, così un futuro nuovo ingresso sotto soglia può notificare;
- modifica di carburante, modalità, soglia o raggio → reset fingerprint;
- se Android impedisce la notifica per permesso mancante, la fingerprint **non viene avanzata**, così il prossimo ciclo può riprovare dopo concessione del permesso.

L'obiettivo è notificare cambiamenti utili, non ripetere ogni giorno lo stesso avviso.

## 7. Trigger di valutazione

La soglia viene valutata:
1. dopo un refresh MIMIT background `WorkManager` riuscito;
2. dopo un refresh manuale Stazioni riuscito;
3. una volta subito dopo salvataggio/attivazione della regola, usando la cache disponibile.

Un refresh MIMIT fallito non genera notifiche e non modifica la fingerprint.

Il WorkManager giornaliero M4 resta la sorgente periodica; non vengono aggiunti scheduler più frequenti né polling aggressivo.

## 8. Notifica Android

- Notification channel stabile: `w2full_price_alerts` / `Avvisi prezzo`;
- `NotificationCompat`, notifica locale;
- su Android 13+ dichiarare e richiedere `POST_NOTIFICATIONS` solo quando l'utente prova ad attivare l'avviso;
- se il permesso viene negato, la regola può essere salvata ma resta `isActive = false` e la UI segnala che serve il permesso;
- su versioni precedenti nessun runtime permission aggiuntivo;
- tap sulla notifica apre W2Full; non è richiesto un deep-link complesso in M6;
- nessuna notifica persistente/foreground service.

## 9. UI minima funzionale

Per non anticipare il polish finale:
- la home Impostazioni resta non scrollabile;
- viene aggiunta una quarta destinazione globale `Avviso prezzo`, necessaria a M6;
- la pagina dedicata deve restare compatta e non richiedere page-scroll sul Galaxy S25;
- controlli: attivo, carburante, Self/Servito, soglia €/L, raggio opzionale, salva/aggiorna, stato permesso notifiche;
- target touch >= 48 dp;
- nessun ridisegno delle altre tab.

La precedente regola M7.4 delle sole tre destinazioni Impostazioni è sostituita **solo** per consentire questa nuova funzione globale M6.

## 10. Versione

- `versionCode = 14`;
- `versionName = 0.6.0-m6`.

## 11. Test obbligatori

Persistenza:
- migrazione Room 4→5 preserva tutte le tabelle esistenti;
- default/lettura/salvataggio regola id=1;
- validazione soglia e raggio;
- modifica configurazione resetta fingerprint.

Valutatore:
- filtro carburante e Self/Servito;
- `<=` soglia incluso;
- sopra soglia escluso;
- raggio incluso/escluso con Haversine;
- posizione mancante/scaduta con raggio → `SKIPPED`, nessuna mutazione anti-spam;
- senza raggio non richiede posizione;
- ordinamento migliore prezzo;
- fingerprint deterministica;
- stesso insieme → no notify;
- insieme cambiato → notify;
- insieme vuoto → reset;
- permesso/notifier failure → fingerprint non avanzata.

Trigger:
- worker e refresh manuale condividono lo stesso `RoomNearbyStationsRepository.refresh()`;
- callback di valutazione eseguita solo dopo transazione refresh MIMIT riuscita;
- refresh fallito non raggiunge la callback;
- salvataggio/attivazione valuta cache una volta.

UI/permessi:
- `POST_NOTIFICATIONS` presente nel Manifest;
- `ACCESS_BACKGROUND_LOCATION` assente;
- negazione permesso non lascia regola attiva;
- pagina M6 raggiungibile da Impostazioni senza rendere scrollabile la home.

Regressioni:
- test M3–M7.4 verdi;
- firma persistente invariata.

## 12. Implementazione candidata ed evidenze branch

Implementazione funzionale commit `83a317375db0260c832b3f989219a5e718f09be5`:
- Room schema 5 e migrazione esplicita 4→5 con tabella singleton `price_alert_rule`;
- repository regola con default carburante veicolo, validazione soglia/raggio e reset anti-spam su modifica;
- ultima posizione foreground salvata localmente e scadenza 24 ore per valutazioni con raggio;
- selettore candidati su cache MIMIT con carburante/modalità/soglia/raggio e Haversine;
- fingerprint SHA-256 deterministica sugli ID stazione sotto soglia;
- evaluator con stati inattivo, posizione mancante, nessun match, invariato, notificato e permesso bloccato;
- canale locale `w2full_price_alerts`, NotificationCompat e tap che apre W2Full;
- `POST_NOTIFICATIONS` richiesto on-demand su Android 13+; nessuna background-location permission;
- valutazione dopo refresh MIMIT riuscito tramite callback comune a manuale/WorkManager e dopo salvataggio attivo;
- Impostazioni con quarta voce `Avviso prezzo` e pagina dedicata fixed-screen;
- versione `0.6.0-m6`, versionCode 14;
- test M6 per migrazione, repository, policy/evaluator e Manifest.

Trigger CI temporaneo branch: commit `d162afd4ae64779c98b5adf6fe75269eebd24f1e`.

Gate branch reale:
- Android CI run `33967278629`, job `101309501352`: **SUCCESS completa**;
- JVM tests: **SUCCESS**;
- `assembleDebug`: **SUCCESS**;
- APK: **Verifies**, APK Signature Scheme v2, un signer;
- certificato SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`;
- public key SHA-256 `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`;
- artifact `w2full-debug-apk`, ID `9969854108`, size ZIP `14612262` byte;
- artifact ZIP SHA-256 `334f5fe5750741911cd80f73a94ea9e535c9a6089b9ffea1bbf75c5825725072`.

Il presente commit documentale deve superare una seconda Android CI completa prima del PR.

## 13. Gate M6

Ordine obbligatorio restante:
1. Android CI reale sul presente HEAD documentato: **pending**;
2. PR limitata a M6;
3. integrazione su `main` con expected head SHA;
4. Android CI reale `main` **SUCCESS**;
5. cleanup del solo trigger CI temporaneo M6;
6. Release reale `v0.6.0-m6-rc1` dall'esatto SHA finale `main`, con tag/asset/firma/SHA-256 verificati;
7. prova reale Galaxy S25;
8. chiusura M6 solo dopo PASS dispositivo.

# Rifinitura finale differita

Resta fuori scope M6:
- ridisegno generale UI;
- issue #1 `Indicazioni`;
- export CSV e altre rifiniture finali;
- backfill storico MIMIT;
- manutenzione CI non necessaria.

# Vincoli invarianti

- app gratuita e locale;
- nessun account/backend/cloud proprietario/servizio a pagamento;
- Kotlin + Jetpack Compose + Material 3;
- target Samsung Galaxy S25;
- APK solo GitHub Releases;
- firma debug persistente invariata;
- certificato SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`;
- public key SHA-256 `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`;
- un obiettivo alla volta;
- nessun build/test/commit simulato.
