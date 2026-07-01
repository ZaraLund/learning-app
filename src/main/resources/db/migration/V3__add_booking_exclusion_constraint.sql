
-- btree_gist krävs för att kombinera en vanlig kolumn (room_id =)
-- med ett GiST-index (tsrange &&) i samma exclusion constraint.
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Hindrar överlappande bokningar för samma rum.
-- tsrange(start_time, end_time, '[)') betyder:
--   [ = inklusivt start  (10:00 ingår i bokningen)
--   ) = exklusivt slut   (11:00 ingår INTE – dvs. 10-11 och 11-12 kan samexistera)
--
-- && = "överlappar" – detta är vad exclusion constraint testar mot varje befintlig rad.
ALTER TABLE bookings
    ADD CONSTRAINT no_overlapping_bookings
    EXCLUDE USING gist (
        room_id  WITH =,
        tsrange(start_time, end_time, '[)') WITH &&
    );
