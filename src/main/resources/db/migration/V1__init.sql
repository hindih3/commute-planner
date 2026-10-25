CREATE TABLE stop (
    id   SERIAL PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE segment (
    id               SERIAL  PRIMARY KEY,
    parent_id        INTEGER REFERENCES segment(id),
    mode             TEXT    NOT NULL CHECK (mode IN ('WALK', 'BUS', 'TRAIN')),
    description      TEXT    NOT NULL,
    minutes          INTEGER NOT NULL CHECK (minutes >= 0),
    line             TEXT,
    boarding_stop_id INTEGER REFERENCES stop(id)
);

CREATE TABLE departure (
    id         SERIAL  PRIMARY KEY,
    line       TEXT    NOT NULL,
    stop_id    INTEGER NOT NULL REFERENCES stop(id),
    day_type   TEXT    NOT NULL CHECK (day_type IN ('WEEKDAY', 'SATURDAY', 'SUNDAY')),
    departs_at TIME    NOT NULL
);

INSERT INTO stop (name) VALUES
    ('Clinton (157)'), ('Halsted & Jackson'), ('Clinton Blue Line'), ('UIC-Halsted');

INSERT INTO segment (parent_id, mode, description, minutes, line, boarding_stop_id) VALUES
    (NULL, 'WALK',  'Exit Union Station',          3, NULL,   NULL),
    (1,    'WALK',  'Walk to 157 stop',            2, NULL,   NULL),
    (2,    'BUS',   '157 to building',             8, '157',  1),
    (1,    'WALK',  'Walk to Halsted & Jackson',   9, NULL,   NULL),
    (4,    'BUS',   '8 to Halsted & Taylor',       4, '8',    2),
    (5,    'WALK',  'Walk to door',                1, NULL,   NULL),
    (1,    'WALK',  'Walk to Clinton Blue Line',   4, NULL,   NULL),
    (7,    'TRAIN', 'Blue Line to UIC-Halsted',    3, 'Blue', 3),
    (8,    'WALK',  'Walk to campus',             10, NULL,   NULL),
    (8,    'BUS',   '8 to Halsted & Taylor',       3, '8',    4),
    (10,   'WALK',  'Walk to door',                1, NULL,   NULL),
    (1,    'WALK',  'Walk the whole way',         25, NULL,   NULL);

INSERT INTO departure (line, stop_id, day_type, departs_at) VALUES
    ('157',  1, 'WEEKDAY', '07:40'), ('157',  1, 'WEEKDAY', '08:00'), ('157',  1, 'WEEKDAY', '08:20'),
    ('8',    2, 'WEEKDAY', '07:50'), ('8',    2, 'WEEKDAY', '08:02'), ('8',    2, 'WEEKDAY', '08:14'),
    ('Blue', 3, 'WEEKDAY', '07:48'), ('Blue', 3, 'WEEKDAY', '07:56'), ('Blue', 3, 'WEEKDAY', '08:04'),
    ('8',    4, 'WEEKDAY', '07:55'), ('8',    4, 'WEEKDAY', '08:07'), ('8',    4, 'WEEKDAY', '08:19');
