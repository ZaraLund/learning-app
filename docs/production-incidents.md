# Production Incidents

---

## INC-001 · Dubbelbokningar av konferensrum

**Status:** Åtgärdat  
**Datum:** 2026-07-01  
**Allvarlighetsgrad:** Hög

### Symptom

Två användare kunde boka samma konferensrum på exakt samma tid. Båda fick bekräftelse på sin bokning, men bara en av dem kunde faktiskt använda rummet.

### Rotorsak

`BookingService.create()` saknade `@Transactional` och innehöll ingen kontroll för överlappande bokningar. Varje databasanrop körde i sin egen transaktion. Under hög belastning kunde två trådar passera en hypotetisk overlap-kontroll innan någon av dem hann spara sin bokning — ett klassiskt TOCTOU-problem (time-of-check-time-of-use).

Utan ett databassidigt skydd var det omöjligt att garantera atomicitet oavsett applikationslogik.

### Åtgärd

1. `@Transactional` lades till på `BookingService.create()` för att säkerställa att read + write är en atomär operation.
2. En PostgreSQL exclusion constraint lades till via Flyway-migration `V3`:

```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE bookings
    ADD CONSTRAINT no_overlapping_bookings
    EXCLUDE USING gist (
        room_id  WITH =,
        tsrange(start_time, end_time, '[)') WITH &&
    );
```

Constraint:en hanterar det som applikationskod aldrig kan garantera under concurrency: databasen avvisar en andra insättning om den överlappar med en redan committad bokning för samma rum.

### Lärdom

Applikationsnivå-kontroller räcker inte under concurrency. En databaskonstrant är den enda tillförlitliga sista försvarslinjen mot race conditions som involverar överlappande data.

---

## INC-002 · 500 Internal Server Error vid belastad bokning

**Status:** Åtgärdat  
**Datum:** 2026-07-01  
**Allvarlighetsgrad:** Medium

### Symptom

När två användare försökte boka samma rum samtidigt fick en av dem ett `500 Internal Server Error` istället för ett meningsfullt felmeddelande. Klienten hade ingen möjlighet att förstå vad som gått fel eller försöka igen med ett annat rum.

### Rotorsak

Exclusion constraint (se INC-001) gör att PostgreSQL kastar ett `PSQLException` vid kollision. Spring omvandlar detta till `DataIntegrityViolationException`. Utan en registrerad exception handler lät Spring Boot felet bubbla upp till en generisk 500-respons.

### Åtgärd

`GlobalExceptionHandler` skapades med en `@ExceptionHandler` för `DataIntegrityViolationException`:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body("Rummet är redan bokat för det angivna tidsintervallet.");
    }
}
```

Klienten får nu `409 Conflict` med ett läsbart felmeddelande.

### Lärdom

Databasfält är implementationsdetaljer — klienten ska aldrig exponeras för råa `PSQLException`-meddelanden eller generiska 500-svar. Exception handlers på rätt abstraktionsnivå är en del av API-kontraktet.
