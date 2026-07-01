CREATE TABLE rooms (
    id   BIGSERIAL    PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE users (
    id   BIGSERIAL    PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE bookings (
    id         BIGSERIAL NOT NULL PRIMARY KEY,
    room_id    BIGINT    NOT NULL REFERENCES rooms(id),
    user_id    BIGINT    NOT NULL REFERENCES users(id),
    start_time TIMESTAMP NOT NULL,
    end_time   TIMESTAMP NOT NULL
);
