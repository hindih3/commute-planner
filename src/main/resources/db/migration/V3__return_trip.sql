-- The trip home: campus -> Union Station.
--
-- Every segment now belongs to one commute. The existing rows are the trip to
-- campus and get that value from the column default. Planner needs exactly one
-- root per tree, so SegmentRepository loads one commute at a time.
ALTER TABLE segment
    ADD COLUMN commute TEXT NOT NULL DEFAULT 'TO_CAMPUS'
    CHECK (commute IN ('TO_CAMPUS', 'TO_UNION'));

-- Walking the whole way, measured over three days: 18, 16 and 14 minutes door
-- to door, including the 3-minute exit from Union Station. Use the slowest.
UPDATE segment SET minutes = 15 WHERE id = 12;

-- Boarding stops for the trip home (the other side of each street or platform).
INSERT INTO stop (id, name, cta_stop_id) VALUES
    (5, 'Taylor & Halsted (157 eastbound)',        '6701'),
    (6, 'Halsted & Taylor (8 northbound)',         '14487'),
    (7, 'UIC-Halsted Blue Line (toward O''Hare)',  '30068');

-- The trip to campus flipped. One shared root, since Planner needs a single
-- starting point. Ride minutes are the scheduled median plus about a minute,
-- like the trip in. The last walk of each path includes the 3 minutes of
-- getting into Union Station and to the train.
INSERT INTO segment (id, parent_id, mode, description, minutes, line, boarding_stop_id, commute) VALUES
    (13, NULL, 'WALK',  'Leave class',                 0, NULL,   NULL, 'TO_UNION'),
    (14, 13,   'WALK',  'Walk to 157 stop',            1, NULL,   NULL, 'TO_UNION'),
    (15, 14,   'BUS',   '157 to Jefferson & Jackson',  8, '157',  5,    'TO_UNION'),
    (16, 15,   'WALK',  'Walk into Union Station',     7, NULL,   NULL, 'TO_UNION'),
    (17, 13,   'WALK',  'Walk to Halsted & Taylor',    1, NULL,   NULL, 'TO_UNION'),
    (18, 17,   'BUS',   '8 to Halsted & Jackson',      6, '8',    6,    'TO_UNION'),
    (19, 18,   'WALK',  'Walk into Union Station',    12, NULL,   NULL, 'TO_UNION'),
    (20, 17,   'BUS',   '8 to UIC-Halsted',            4, '8',    6,    'TO_UNION'),
    (21, 20,   'TRAIN', 'Blue Line to Clinton',        3, 'Blue', 7,    'TO_UNION'),
    (22, 21,   'WALK',  'Walk into Union Station',     7, NULL,   NULL, 'TO_UNION'),
    (23, 13,   'WALK',  'Walk to UIC-Halsted',        10, NULL,   NULL, 'TO_UNION'),
    (24, 23,   'TRAIN', 'Blue Line to Clinton',        3, 'Blue', 7,    'TO_UNION'),
    (25, 24,   'WALK',  'Walk into Union Station',     7, NULL,   NULL, 'TO_UNION'),
    (26, 13,   'WALK',  'Walk the whole way',         18, NULL,   NULL, 'TO_UNION');

-- The rows above have explicit ids, so move each SERIAL counter past them.
-- Otherwise the next insert without an id would reuse 5 or 13 and fail.
SELECT setval('stop_id_seq', (SELECT max(id) FROM stop));
SELECT setval('segment_id_seq', (SELECT max(id) FROM segment));

-- Weekday departures at the new stops, from the same CTA GTFS feed as V2
-- (published 2026-09-14, Tuesday-Friday timetable).
INSERT INTO departure (line, stop_id, day_type, departs_at) VALUES
    -- 157 eastbound at Taylor & Halsted (CTA stop 6701): 65 departures
    ('157', 5, 'WEEKDAY', '05:43'), ('157', 5, 'WEEKDAY', '05:59'), ('157', 5, 'WEEKDAY', '06:15'), ('157', 5, 'WEEKDAY', '06:32'), ('157', 5, 'WEEKDAY', '06:48'), ('157', 5, 'WEEKDAY', '07:05'),
    ('157', 5, 'WEEKDAY', '07:21'), ('157', 5, 'WEEKDAY', '07:37'), ('157', 5, 'WEEKDAY', '07:40'), ('157', 5, 'WEEKDAY', '07:53'), ('157', 5, 'WEEKDAY', '08:09'), ('157', 5, 'WEEKDAY', '08:22'),
    ('157', 5, 'WEEKDAY', '08:35'), ('157', 5, 'WEEKDAY', '08:44'), ('157', 5, 'WEEKDAY', '08:47'), ('157', 5, 'WEEKDAY', '09:00'), ('157', 5, 'WEEKDAY', '09:12'), ('157', 5, 'WEEKDAY', '09:24'),
    ('157', 5, 'WEEKDAY', '09:36'), ('157', 5, 'WEEKDAY', '09:44'), ('157', 5, 'WEEKDAY', '09:48'), ('157', 5, 'WEEKDAY', '10:00'), ('157', 5, 'WEEKDAY', '10:16'), ('157', 5, 'WEEKDAY', '10:32'),
    ('157', 5, 'WEEKDAY', '10:44'), ('157', 5, 'WEEKDAY', '10:50'), ('157', 5, 'WEEKDAY', '11:08'), ('157', 5, 'WEEKDAY', '11:25'), ('157', 5, 'WEEKDAY', '11:43'), ('157', 5, 'WEEKDAY', '12:01'),
    ('157', 5, 'WEEKDAY', '12:19'), ('157', 5, 'WEEKDAY', '12:36'), ('157', 5, 'WEEKDAY', '12:53'), ('157', 5, 'WEEKDAY', '13:10'), ('157', 5, 'WEEKDAY', '13:27'), ('157', 5, 'WEEKDAY', '13:44'),
    ('157', 5, 'WEEKDAY', '14:01'), ('157', 5, 'WEEKDAY', '14:15'), ('157', 5, 'WEEKDAY', '14:28'), ('157', 5, 'WEEKDAY', '14:41'), ('157', 5, 'WEEKDAY', '14:54'), ('157', 5, 'WEEKDAY', '15:06'),
    ('157', 5, 'WEEKDAY', '15:17'), ('157', 5, 'WEEKDAY', '15:19'), ('157', 5, 'WEEKDAY', '15:31'), ('157', 5, 'WEEKDAY', '15:43'), ('157', 5, 'WEEKDAY', '15:55'), ('157', 5, 'WEEKDAY', '16:07'),
    ('157', 5, 'WEEKDAY', '16:16'), ('157', 5, 'WEEKDAY', '16:27'), ('157', 5, 'WEEKDAY', '16:39'), ('157', 5, 'WEEKDAY', '16:52'), ('157', 5, 'WEEKDAY', '17:05'), ('157', 5, 'WEEKDAY', '17:18'),
    ('157', 5, 'WEEKDAY', '17:32'), ('157', 5, 'WEEKDAY', '17:52'), ('157', 5, 'WEEKDAY', '18:11'), ('157', 5, 'WEEKDAY', '18:30'), ('157', 5, 'WEEKDAY', '18:49'), ('157', 5, 'WEEKDAY', '19:09'),
    ('157', 5, 'WEEKDAY', '19:28'), ('157', 5, 'WEEKDAY', '19:48'), ('157', 5, 'WEEKDAY', '20:08'), ('157', 5, 'WEEKDAY', '20:28'), ('157', 5, 'WEEKDAY', '20:48'),
    -- 8 northbound at Halsted & Taylor (CTA stop 14487): 123 departures
    ('8', 6, 'WEEKDAY', '00:00'), ('8', 6, 'WEEKDAY', '00:21'), ('8', 6, 'WEEKDAY', '00:45'), ('8', 6, 'WEEKDAY', '01:10'), ('8', 6, 'WEEKDAY', '04:45'), ('8', 6, 'WEEKDAY', '05:04'),
    ('8', 6, 'WEEKDAY', '05:22'), ('8', 6, 'WEEKDAY', '05:39'), ('8', 6, 'WEEKDAY', '05:56'), ('8', 6, 'WEEKDAY', '06:11'), ('8', 6, 'WEEKDAY', '06:25'), ('8', 6, 'WEEKDAY', '06:40'),
    ('8', 6, 'WEEKDAY', '06:52'), ('8', 6, 'WEEKDAY', '06:59'), ('8', 6, 'WEEKDAY', '07:06'), ('8', 6, 'WEEKDAY', '07:12'), ('8', 6, 'WEEKDAY', '07:18'), ('8', 6, 'WEEKDAY', '07:24'),
    ('8', 6, 'WEEKDAY', '07:29'), ('8', 6, 'WEEKDAY', '07:35'), ('8', 6, 'WEEKDAY', '07:40'), ('8', 6, 'WEEKDAY', '07:45'), ('8', 6, 'WEEKDAY', '07:50'), ('8', 6, 'WEEKDAY', '07:55'),
    ('8', 6, 'WEEKDAY', '08:00'), ('8', 6, 'WEEKDAY', '08:06'), ('8', 6, 'WEEKDAY', '08:11'), ('8', 6, 'WEEKDAY', '08:16'), ('8', 6, 'WEEKDAY', '08:21'), ('8', 6, 'WEEKDAY', '08:26'),
    ('8', 6, 'WEEKDAY', '08:31'), ('8', 6, 'WEEKDAY', '08:35'), ('8', 6, 'WEEKDAY', '08:40'), ('8', 6, 'WEEKDAY', '08:45'), ('8', 6, 'WEEKDAY', '08:50'), ('8', 6, 'WEEKDAY', '08:55'),
    ('8', 6, 'WEEKDAY', '09:00'), ('8', 6, 'WEEKDAY', '09:06'), ('8', 6, 'WEEKDAY', '09:12'), ('8', 6, 'WEEKDAY', '09:19'), ('8', 6, 'WEEKDAY', '09:26'), ('8', 6, 'WEEKDAY', '09:33'),
    ('8', 6, 'WEEKDAY', '09:42'), ('8', 6, 'WEEKDAY', '09:48'), ('8', 6, 'WEEKDAY', '09:54'), ('8', 6, 'WEEKDAY', '10:00'), ('8', 6, 'WEEKDAY', '10:07'), ('8', 6, 'WEEKDAY', '10:13'),
    ('8', 6, 'WEEKDAY', '10:19'), ('8', 6, 'WEEKDAY', '10:26'), ('8', 6, 'WEEKDAY', '10:33'), ('8', 6, 'WEEKDAY', '10:39'), ('8', 6, 'WEEKDAY', '10:47'), ('8', 6, 'WEEKDAY', '10:57'),
    ('8', 6, 'WEEKDAY', '11:07'), ('8', 6, 'WEEKDAY', '11:17'), ('8', 6, 'WEEKDAY', '11:27'), ('8', 6, 'WEEKDAY', '11:37'), ('8', 6, 'WEEKDAY', '11:48'), ('8', 6, 'WEEKDAY', '11:58'),
    ('8', 6, 'WEEKDAY', '12:08'), ('8', 6, 'WEEKDAY', '12:18'), ('8', 6, 'WEEKDAY', '12:28'), ('8', 6, 'WEEKDAY', '12:38'), ('8', 6, 'WEEKDAY', '12:48'), ('8', 6, 'WEEKDAY', '12:58'),
    ('8', 6, 'WEEKDAY', '13:08'), ('8', 6, 'WEEKDAY', '13:18'), ('8', 6, 'WEEKDAY', '13:28'), ('8', 6, 'WEEKDAY', '13:38'), ('8', 6, 'WEEKDAY', '13:48'), ('8', 6, 'WEEKDAY', '13:58'),
    ('8', 6, 'WEEKDAY', '14:07'), ('8', 6, 'WEEKDAY', '14:17'), ('8', 6, 'WEEKDAY', '14:26'), ('8', 6, 'WEEKDAY', '14:36'), ('8', 6, 'WEEKDAY', '14:45'), ('8', 6, 'WEEKDAY', '14:54'),
    ('8', 6, 'WEEKDAY', '15:04'), ('8', 6, 'WEEKDAY', '15:14'), ('8', 6, 'WEEKDAY', '15:23'), ('8', 6, 'WEEKDAY', '15:32'), ('8', 6, 'WEEKDAY', '15:41'), ('8', 6, 'WEEKDAY', '15:50'),
    ('8', 6, 'WEEKDAY', '15:59'), ('8', 6, 'WEEKDAY', '16:07'), ('8', 6, 'WEEKDAY', '16:15'), ('8', 6, 'WEEKDAY', '16:23'), ('8', 6, 'WEEKDAY', '16:31'), ('8', 6, 'WEEKDAY', '16:39'),
    ('8', 6, 'WEEKDAY', '16:46'), ('8', 6, 'WEEKDAY', '16:53'), ('8', 6, 'WEEKDAY', '17:01'), ('8', 6, 'WEEKDAY', '17:07'), ('8', 6, 'WEEKDAY', '17:15'), ('8', 6, 'WEEKDAY', '17:22'),
    ('8', 6, 'WEEKDAY', '17:29'), ('8', 6, 'WEEKDAY', '17:37'), ('8', 6, 'WEEKDAY', '17:44'), ('8', 6, 'WEEKDAY', '17:53'), ('8', 6, 'WEEKDAY', '18:02'), ('8', 6, 'WEEKDAY', '18:12'),
    ('8', 6, 'WEEKDAY', '18:23'), ('8', 6, 'WEEKDAY', '18:33'), ('8', 6, 'WEEKDAY', '18:45'), ('8', 6, 'WEEKDAY', '18:56'), ('8', 6, 'WEEKDAY', '19:10'), ('8', 6, 'WEEKDAY', '19:24'),
    ('8', 6, 'WEEKDAY', '19:38'), ('8', 6, 'WEEKDAY', '19:52'), ('8', 6, 'WEEKDAY', '20:05'), ('8', 6, 'WEEKDAY', '20:21'), ('8', 6, 'WEEKDAY', '20:38'), ('8', 6, 'WEEKDAY', '20:55'),
    ('8', 6, 'WEEKDAY', '21:12'), ('8', 6, 'WEEKDAY', '21:30'), ('8', 6, 'WEEKDAY', '21:48'), ('8', 6, 'WEEKDAY', '22:07'), ('8', 6, 'WEEKDAY', '22:25'), ('8', 6, 'WEEKDAY', '22:44'),
    ('8', 6, 'WEEKDAY', '23:03'), ('8', 6, 'WEEKDAY', '23:22'), ('8', 6, 'WEEKDAY', '23:41'),
    -- Blue Line toward O'Hare at UIC-Halsted (CTA stop 30068): 178 departures
    ('Blue', 7, 'WEEKDAY', '00:03'), ('Blue', 7, 'WEEKDAY', '00:13'), ('Blue', 7, 'WEEKDAY', '00:23'), ('Blue', 7, 'WEEKDAY', '00:33'), ('Blue', 7, 'WEEKDAY', '00:43'), ('Blue', 7, 'WEEKDAY', '00:53'),
    ('Blue', 7, 'WEEKDAY', '01:03'), ('Blue', 7, 'WEEKDAY', '01:18'), ('Blue', 7, 'WEEKDAY', '01:33'), ('Blue', 7, 'WEEKDAY', '01:48'), ('Blue', 7, 'WEEKDAY', '02:03'), ('Blue', 7, 'WEEKDAY', '02:23'),
    ('Blue', 7, 'WEEKDAY', '02:43'), ('Blue', 7, 'WEEKDAY', '03:03'), ('Blue', 7, 'WEEKDAY', '03:23'), ('Blue', 7, 'WEEKDAY', '03:43'), ('Blue', 7, 'WEEKDAY', '04:03'), ('Blue', 7, 'WEEKDAY', '04:18'),
    ('Blue', 7, 'WEEKDAY', '04:33'), ('Blue', 7, 'WEEKDAY', '04:48'), ('Blue', 7, 'WEEKDAY', '05:03'), ('Blue', 7, 'WEEKDAY', '05:18'), ('Blue', 7, 'WEEKDAY', '05:33'), ('Blue', 7, 'WEEKDAY', '05:43'),
    ('Blue', 7, 'WEEKDAY', '05:53'), ('Blue', 7, 'WEEKDAY', '06:03'), ('Blue', 7, 'WEEKDAY', '06:13'), ('Blue', 7, 'WEEKDAY', '06:23'), ('Blue', 7, 'WEEKDAY', '06:31'), ('Blue', 7, 'WEEKDAY', '06:38'),
    ('Blue', 7, 'WEEKDAY', '06:46'), ('Blue', 7, 'WEEKDAY', '06:48'), ('Blue', 7, 'WEEKDAY', '06:53'), ('Blue', 7, 'WEEKDAY', '07:01'), ('Blue', 7, 'WEEKDAY', '07:08'), ('Blue', 7, 'WEEKDAY', '07:11'),
    ('Blue', 7, 'WEEKDAY', '07:16'), ('Blue', 7, 'WEEKDAY', '07:23'), ('Blue', 7, 'WEEKDAY', '07:31'), ('Blue', 7, 'WEEKDAY', '07:38'), ('Blue', 7, 'WEEKDAY', '07:46'), ('Blue', 7, 'WEEKDAY', '07:53'),
    ('Blue', 7, 'WEEKDAY', '08:01'), ('Blue', 7, 'WEEKDAY', '08:08'), ('Blue', 7, 'WEEKDAY', '08:16'), ('Blue', 7, 'WEEKDAY', '08:23'), ('Blue', 7, 'WEEKDAY', '08:30'), ('Blue', 7, 'WEEKDAY', '08:37'),
    ('Blue', 7, 'WEEKDAY', '08:44'), ('Blue', 7, 'WEEKDAY', '08:51'), ('Blue', 7, 'WEEKDAY', '08:58'), ('Blue', 7, 'WEEKDAY', '09:05'), ('Blue', 7, 'WEEKDAY', '09:12'), ('Blue', 7, 'WEEKDAY', '09:19'),
    ('Blue', 7, 'WEEKDAY', '09:26'), ('Blue', 7, 'WEEKDAY', '09:33'), ('Blue', 7, 'WEEKDAY', '09:40'), ('Blue', 7, 'WEEKDAY', '09:47'), ('Blue', 7, 'WEEKDAY', '09:54'), ('Blue', 7, 'WEEKDAY', '10:01'),
    ('Blue', 7, 'WEEKDAY', '10:08'), ('Blue', 7, 'WEEKDAY', '10:15'), ('Blue', 7, 'WEEKDAY', '10:22'), ('Blue', 7, 'WEEKDAY', '10:30'), ('Blue', 7, 'WEEKDAY', '10:38'), ('Blue', 7, 'WEEKDAY', '10:46'),
    ('Blue', 7, 'WEEKDAY', '10:54'), ('Blue', 7, 'WEEKDAY', '11:03'), ('Blue', 7, 'WEEKDAY', '11:13'), ('Blue', 7, 'WEEKDAY', '11:23'), ('Blue', 7, 'WEEKDAY', '11:33'), ('Blue', 7, 'WEEKDAY', '11:43'),
    ('Blue', 7, 'WEEKDAY', '11:53'), ('Blue', 7, 'WEEKDAY', '12:03'), ('Blue', 7, 'WEEKDAY', '12:13'), ('Blue', 7, 'WEEKDAY', '12:23'), ('Blue', 7, 'WEEKDAY', '12:33'), ('Blue', 7, 'WEEKDAY', '12:43'),
    ('Blue', 7, 'WEEKDAY', '12:53'), ('Blue', 7, 'WEEKDAY', '13:03'), ('Blue', 7, 'WEEKDAY', '13:13'), ('Blue', 7, 'WEEKDAY', '13:23'), ('Blue', 7, 'WEEKDAY', '13:33'), ('Blue', 7, 'WEEKDAY', '13:43'),
    ('Blue', 7, 'WEEKDAY', '13:53'), ('Blue', 7, 'WEEKDAY', '14:02'), ('Blue', 7, 'WEEKDAY', '14:10'), ('Blue', 7, 'WEEKDAY', '14:18'), ('Blue', 7, 'WEEKDAY', '14:25'), ('Blue', 7, 'WEEKDAY', '14:32'),
    ('Blue', 7, 'WEEKDAY', '14:39'), ('Blue', 7, 'WEEKDAY', '14:46'), ('Blue', 7, 'WEEKDAY', '14:53'), ('Blue', 7, 'WEEKDAY', '15:00'), ('Blue', 7, 'WEEKDAY', '15:07'), ('Blue', 7, 'WEEKDAY', '15:14'),
    ('Blue', 7, 'WEEKDAY', '15:20'), ('Blue', 7, 'WEEKDAY', '15:26'), ('Blue', 7, 'WEEKDAY', '15:32'), ('Blue', 7, 'WEEKDAY', '15:38'), ('Blue', 7, 'WEEKDAY', '15:44'), ('Blue', 7, 'WEEKDAY', '15:50'),
    ('Blue', 7, 'WEEKDAY', '15:55'), ('Blue', 7, 'WEEKDAY', '16:00'), ('Blue', 7, 'WEEKDAY', '16:05'), ('Blue', 7, 'WEEKDAY', '16:10'), ('Blue', 7, 'WEEKDAY', '16:15'), ('Blue', 7, 'WEEKDAY', '16:20'),
    ('Blue', 7, 'WEEKDAY', '16:25'), ('Blue', 7, 'WEEKDAY', '16:29'), ('Blue', 7, 'WEEKDAY', '16:33'), ('Blue', 7, 'WEEKDAY', '16:37'), ('Blue', 7, 'WEEKDAY', '16:41'), ('Blue', 7, 'WEEKDAY', '16:45'),
    ('Blue', 7, 'WEEKDAY', '16:49'), ('Blue', 7, 'WEEKDAY', '16:53'), ('Blue', 7, 'WEEKDAY', '16:57'), ('Blue', 7, 'WEEKDAY', '17:00'), ('Blue', 7, 'WEEKDAY', '17:04'), ('Blue', 7, 'WEEKDAY', '17:07'),
    ('Blue', 7, 'WEEKDAY', '17:11'), ('Blue', 7, 'WEEKDAY', '17:14'), ('Blue', 7, 'WEEKDAY', '17:18'), ('Blue', 7, 'WEEKDAY', '17:21'), ('Blue', 7, 'WEEKDAY', '17:25'), ('Blue', 7, 'WEEKDAY', '17:30'),
    ('Blue', 7, 'WEEKDAY', '17:35'), ('Blue', 7, 'WEEKDAY', '17:40'), ('Blue', 7, 'WEEKDAY', '17:45'), ('Blue', 7, 'WEEKDAY', '17:50'), ('Blue', 7, 'WEEKDAY', '17:55'), ('Blue', 7, 'WEEKDAY', '18:01'),
    ('Blue', 7, 'WEEKDAY', '18:07'), ('Blue', 7, 'WEEKDAY', '18:14'), ('Blue', 7, 'WEEKDAY', '18:22'), ('Blue', 7, 'WEEKDAY', '18:29'), ('Blue', 7, 'WEEKDAY', '18:37'), ('Blue', 7, 'WEEKDAY', '18:44'),
    ('Blue', 7, 'WEEKDAY', '18:52'), ('Blue', 7, 'WEEKDAY', '18:59'), ('Blue', 7, 'WEEKDAY', '19:07'), ('Blue', 7, 'WEEKDAY', '19:14'), ('Blue', 7, 'WEEKDAY', '19:22'), ('Blue', 7, 'WEEKDAY', '19:29'),
    ('Blue', 7, 'WEEKDAY', '19:37'), ('Blue', 7, 'WEEKDAY', '19:44'), ('Blue', 7, 'WEEKDAY', '19:52'), ('Blue', 7, 'WEEKDAY', '19:59'), ('Blue', 7, 'WEEKDAY', '20:07'), ('Blue', 7, 'WEEKDAY', '20:14'),
    ('Blue', 7, 'WEEKDAY', '20:22'), ('Blue', 7, 'WEEKDAY', '20:29'), ('Blue', 7, 'WEEKDAY', '20:37'), ('Blue', 7, 'WEEKDAY', '20:44'), ('Blue', 7, 'WEEKDAY', '20:52'), ('Blue', 7, 'WEEKDAY', '20:59'),
    ('Blue', 7, 'WEEKDAY', '21:07'), ('Blue', 7, 'WEEKDAY', '21:14'), ('Blue', 7, 'WEEKDAY', '21:22'), ('Blue', 7, 'WEEKDAY', '21:29'), ('Blue', 7, 'WEEKDAY', '21:37'), ('Blue', 7, 'WEEKDAY', '21:44'),
    ('Blue', 7, 'WEEKDAY', '21:52'), ('Blue', 7, 'WEEKDAY', '21:59'), ('Blue', 7, 'WEEKDAY', '22:07'), ('Blue', 7, 'WEEKDAY', '22:14'), ('Blue', 7, 'WEEKDAY', '22:22'), ('Blue', 7, 'WEEKDAY', '22:29'),
    ('Blue', 7, 'WEEKDAY', '22:37'), ('Blue', 7, 'WEEKDAY', '22:44'), ('Blue', 7, 'WEEKDAY', '22:52'), ('Blue', 7, 'WEEKDAY', '22:59'), ('Blue', 7, 'WEEKDAY', '23:07'), ('Blue', 7, 'WEEKDAY', '23:14'),
    ('Blue', 7, 'WEEKDAY', '23:23'), ('Blue', 7, 'WEEKDAY', '23:33'), ('Blue', 7, 'WEEKDAY', '23:43'), ('Blue', 7, 'WEEKDAY', '23:53');
