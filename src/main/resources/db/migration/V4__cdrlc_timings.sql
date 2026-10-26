-- Timings checked against the actual destination, CDRLC at 900 W Taylor, using
-- CTA stop coordinates and an ordinary walking pace.

-- Getting off the 8 at Halsted & Taylor, CDRLC is ~150 m away: 2 minutes, not 1.
-- Segments 6 and 11 are the two "Walk to door" steps after the 8.
UPDATE segment SET minutes = 2 WHERE id IN (6, 11);

-- Going home, the 8's northbound stop is on the far (northeast) corner of
-- Halsted & Taylor, ~200 m plus crossing Halsted: 3 minutes, not 1.
UPDATE segment SET minutes = 3 WHERE id = 17;

-- The 157 eastbound stops at 900 W Taylor (CTA 6700), right outside CDRLC,
-- before it reaches Taylor & Halsted (6701). Board there instead: same buses,
-- 0-1 minutes earlier, and only a street to cross. Its ride to Jefferson &
-- Jackson is 7.3 minutes scheduled, so segment 15 stays at 8.
UPDATE stop SET name = '900 W Taylor (157 eastbound)', cta_stop_id = '6700' WHERE id = 5;

DELETE FROM departure WHERE stop_id = 5;

-- Weekday departures at 900 W Taylor eastbound, from the same CTA GTFS feed as
-- V2 and V3 (published 2026-09-14, Tuesday-Friday timetable).
INSERT INTO departure (line, stop_id, day_type, departs_at) VALUES
    -- 157 eastbound at 900 W Taylor (CTA stop 6700): 65 departures
    ('157', 5, 'WEEKDAY', '05:43'), ('157', 5, 'WEEKDAY', '05:59'), ('157', 5, 'WEEKDAY', '06:15'), ('157', 5, 'WEEKDAY', '06:31'), ('157', 5, 'WEEKDAY', '06:48'), ('157', 5, 'WEEKDAY', '07:04'),
    ('157', 5, 'WEEKDAY', '07:20'), ('157', 5, 'WEEKDAY', '07:37'), ('157', 5, 'WEEKDAY', '07:40'), ('157', 5, 'WEEKDAY', '07:53'), ('157', 5, 'WEEKDAY', '08:08'), ('157', 5, 'WEEKDAY', '08:21'),
    ('157', 5, 'WEEKDAY', '08:34'), ('157', 5, 'WEEKDAY', '08:43'), ('157', 5, 'WEEKDAY', '08:47'), ('157', 5, 'WEEKDAY', '08:59'), ('157', 5, 'WEEKDAY', '09:12'), ('157', 5, 'WEEKDAY', '09:24'),
    ('157', 5, 'WEEKDAY', '09:36'), ('157', 5, 'WEEKDAY', '09:43'), ('157', 5, 'WEEKDAY', '09:47'), ('157', 5, 'WEEKDAY', '10:00'), ('157', 5, 'WEEKDAY', '10:15'), ('157', 5, 'WEEKDAY', '10:31'),
    ('157', 5, 'WEEKDAY', '10:43'), ('157', 5, 'WEEKDAY', '10:49'), ('157', 5, 'WEEKDAY', '11:07'), ('157', 5, 'WEEKDAY', '11:25'), ('157', 5, 'WEEKDAY', '11:43'), ('157', 5, 'WEEKDAY', '12:01'),
    ('157', 5, 'WEEKDAY', '12:18'), ('157', 5, 'WEEKDAY', '12:36'), ('157', 5, 'WEEKDAY', '12:53'), ('157', 5, 'WEEKDAY', '13:10'), ('157', 5, 'WEEKDAY', '13:27'), ('157', 5, 'WEEKDAY', '13:44'),
    ('157', 5, 'WEEKDAY', '14:01'), ('157', 5, 'WEEKDAY', '14:14'), ('157', 5, 'WEEKDAY', '14:27'), ('157', 5, 'WEEKDAY', '14:40'), ('157', 5, 'WEEKDAY', '14:53'), ('157', 5, 'WEEKDAY', '15:06'),
    ('157', 5, 'WEEKDAY', '15:16'), ('157', 5, 'WEEKDAY', '15:18'), ('157', 5, 'WEEKDAY', '15:30'), ('157', 5, 'WEEKDAY', '15:42'), ('157', 5, 'WEEKDAY', '15:55'), ('157', 5, 'WEEKDAY', '16:07'),
    ('157', 5, 'WEEKDAY', '16:16'), ('157', 5, 'WEEKDAY', '16:26'), ('157', 5, 'WEEKDAY', '16:39'), ('157', 5, 'WEEKDAY', '16:51'), ('157', 5, 'WEEKDAY', '17:04'), ('157', 5, 'WEEKDAY', '17:17'),
    ('157', 5, 'WEEKDAY', '17:32'), ('157', 5, 'WEEKDAY', '17:51'), ('157', 5, 'WEEKDAY', '18:10'), ('157', 5, 'WEEKDAY', '18:29'), ('157', 5, 'WEEKDAY', '18:49'), ('157', 5, 'WEEKDAY', '19:08'),
    ('157', 5, 'WEEKDAY', '19:28'), ('157', 5, 'WEEKDAY', '19:48'), ('157', 5, 'WEEKDAY', '20:08'), ('157', 5, 'WEEKDAY', '20:28'), ('157', 5, 'WEEKDAY', '20:48');
