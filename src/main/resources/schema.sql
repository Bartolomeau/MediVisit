-- =====================================================================
-- MediVisit - system rejestracji wizyt w przychodni
-- Struktura bazy danych (SQLite)
-- =====================================================================

CREATE TABLE IF NOT EXISTS users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    first_name    TEXT    NOT NULL,
    last_name     TEXT    NOT NULL,
    email         TEXT    NOT NULL UNIQUE,
    phone         TEXT,
    password_hash TEXT    NOT NULL,
    role          TEXT    NOT NULL CHECK (role IN ('PATIENT', 'DOCTOR', 'ADMIN')),
    created_at    TEXT    NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE TABLE IF NOT EXISTS specializations (
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT    NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS doctors (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id           INTEGER NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    specialization_id INTEGER NOT NULL REFERENCES specializations (id),
    room              TEXT,
    work_start        TEXT    NOT NULL DEFAULT '08:00',
    work_end          TEXT    NOT NULL DEFAULT '16:00',
    slot_minutes      INTEGER NOT NULL DEFAULT 30
);

CREATE TABLE IF NOT EXISTS appointments (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    patient_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    doctor_id  INTEGER NOT NULL REFERENCES doctors (id) ON DELETE CASCADE,
    date       TEXT    NOT NULL,              -- format: YYYY-MM-DD
    time       TEXT    NOT NULL,              -- format: HH:MM
    status     TEXT    NOT NULL DEFAULT 'ZAPLANOWANA'
                       CHECK (status IN ('ZAPLANOWANA', 'ODBYTA', 'ANULOWANA')),
    reason     TEXT,                          -- powod wizyty podany przez pacjenta
    notes      TEXT,                          -- zalecenia lekarza po wizycie
    created_at TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
    UNIQUE (doctor_id, date, time)
);

CREATE INDEX IF NOT EXISTS idx_appointments_patient ON appointments (patient_id);
CREATE INDEX IF NOT EXISTS idx_appointments_doctor  ON appointments (doctor_id, date);
