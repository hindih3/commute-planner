# commute-planner

I made this app to plan trips between Chicago's Union Station and UIC's CDRLC building 
in either direction using live CTA bus and train predictions. Given a departure time and 
an arrival deadline, it returns every feasible route, sorted by arrival time, including 
when to leave each stop and how long you'll wait between connections.

I built this to solve two problems I ran into regularly:
- **Getting to class on time**: After getting off the Metra train, I'd sometimes
  take a convoluted route to campus and arrive late
- **Catching the train home**: After my last class, I'd sometimes miss the nearest
  Metra train and end up waiting 30 minutes to *two hours* for the next one, which
  was not a fun experience.

Both problems came down to the same thing: spending my precious time checking Google Maps,
comparing routes, and calculating travel-time in my head. I wanted a minimal app that
could do all that work for me, account for different routes and departure times, and lay 
out my options upfront.

Ideally, my only job would be to leave the building, open the app, and ask: "What's 
the best way to get to my train?"
```
GET /plans?start=07:30&deadline=08:30

1. arrive 07:48  walk
2. arrive 07:51  157 bus            (wait 8 min at Clinton & Jackson)
3. arrive 07:52  8 bus
4. arrive 07:53  Blue Line + 8 bus
5. arrive 07:54  Blue Line + walk
```

I should clarify that the stack is pretty overkill for a project of this size.
The app definitely doesn't require Spring + Postgres with Flyway migration,
but this was more of a learning project for me. All of those tools are industry
standards I need to get used to, especially when applying for backend roles.

## Stack

- Java 25, Spring Boot 4.1 (Spring MVC), Maven wrapper
- PostgreSQL 17 through plain JDBC (`JdbcClient`, no JPA), schema managed by Flyway
- Spring `RestClient` for CTA's Bus Tracker and Train Tracker APIs
- Spring Boot Actuator for health checks

## How it works

```
GET /plans ─▶ PlanController ─▶ SegmentRepository ─────────────▶ Postgres: segment tree
                    └─▶ Planner ─▶ LiveFirstDepartureSource
                                     ├─ departure in the next ~45 min ─▶ CtaClient ─▶ CTA live predictions
                                     └─ otherwise, or no prediction  ─▶ JdbcDepartureSource ─▶ Postgres: CTA schedule
```

- **Routes are a tree of segments.** Each row in `segment` is one step (a walk,
  a bus ride or a train ride) and points to the step before it. Every path from
  the root to a leaf is one way to make the trip. There's one tree per
  direction (`TO_CAMPUS`, `TO_UNION`).
- **`Planner`** walks every path depth-first. For each bus or train step it asks
  a `DepartureSource` for the next departure at least
  `planner.boarding-buffer-minutes` after you reach the stop. It drops paths
  that can't make the deadline and returns the rest sorted by arrival.
- **`DepartureSource`** is a one-method interface, so the planner doesn't know
  where times come from. `LiveFirstDepartureSource` (the `@Primary` bean) asks
  CTA for predictions when you're planning for right now. It falls back to
  `JdbcDepartureSource`, CTA's published weekday schedule stored in Postgres,
  for later trips, when there's no prediction, or when CTA is unreachable.

## Running it

### 1. PostgreSQL

```bash
docker run -d --name commute-db --restart unless-stopped \
  -e POSTGRES_DB=commute -e POSTGRES_PASSWORD=dev -p 5432:5432 postgres:17
```

Flyway creates the schema and loads the data on the app's first start.

### 2. CTA API keys (optional)

Live predictions need keys for
[Bus Tracker](https://www.transitchicago.com/developers/bustracker/) and
[Train Tracker](https://www.transitchicago.com/developers/traintracker/). Put
them in a `.env` file in the project root (it's git-ignored, duh):

```
CTA_BUS_TRACKER_API_KEY=...
CTA_TRAIN_TRACKER_API_KEY=...
```

Without them, every plan comes from the schedule.

### 3. The app

Needs JDK 25 (`JAVA_HOME` pointing at it).

```bash
./mvnw spring-boot:run
```

It listens on `http://localhost:8080`. `GET /actuator/health` reports `UP`
once it can reach the database.

## API

### `GET /plans`

| Parameter  | Required | Example     | Meaning |
|------------|----------|-------------|---------|
| `start`    | yes      | `07:30`     | When you set off: exiting Union Station, or leaving class |
| `deadline` | yes      | `08:30`     | When you need to be there |
| `commute`  | no       | `TO_UNION`  | `TO_CAMPUS` (default) or `TO_UNION` |

Returns a JSON array of plans, earliest arrival first. A missing or unreadable
parameter is a `400`. An empty array means nothing makes the deadline.

```json
[
  {
    "legs": [
      {
        "segment": { "id": 1, "parentId": null, "mode": "WALK", "description": "Exit Union Station",
                     "minutes": 3, "line": null, "boardingStopId": null, "transit": false },
        "departs": "07:30:00",
        "arrives": "07:33:00"
      },
      {
        "segment": { "id": 12, "parentId": 1, "mode": "WALK", "description": "Walk the whole way",
                     "minutes": 15, "line": null, "boardingStopId": null, "transit": false },
        "departs": "07:33:00",
        "arrives": "07:48:00"
      }
    ],
    "arrival": "07:48:00"
  }
]
```

For a bus or train leg, `departs` is when the vehicle leaves. A gap between one
leg's `arrives` and the next leg's `departs` is time spent waiting at the stop.

## Configuration

Set in `src/main/resources/application.properties`:

| Property | Default | Meaning |
|----------|---------|---------|
| `planner.boarding-buffer-minutes` | `2` | How early you need to be at a stop for a departure to count |
| `cta.live.enabled` | `true` | `false` plans from the schedule only |
| `spring.datasource.password` | `${DB_PASSWORD:dev}` | Database password, from `DB_PASSWORD` if set |

## Data

The schema and data are Flyway migrations in `src/main/resources/db/migration/`:

| Migration | Contents |
|-----------|----------|
| `V1__init.sql` | Tables `stop`, `segment`, `departure`, and the route tree to campus |
| `V2__cta_weekday_schedule.sql` | CTA's weekday schedule at the four stops toward campus, plus each stop's `cta_stop_id` |
| `V3__return_trip.sql` | The `commute` column, the route tree home, its three stops and their schedule |
| `V4__cdrlc_timings.sql` | Walk times checked against the destination, CDRLC at 900 W Taylor, and the 157 home boarding right outside it |

The schedule comes from CTA's GTFS feed published 2026-09-14 (valid through
2026-11-30), using the Tuesday-Friday weekday timetable. Applied migrations
are never edited, so changes go in a new `V<n>__...sql`.

## Tests

```bash
./mvnw test
```

The tests need the Postgres container running. None of them call CTA:

- `PlannerTest` and `LiveFirstDepartureSourceTest` are plain unit tests, with a
  lambda or Mockito standing in for the departure source and CTA.
- `CtaClientTest` uses a fake CTA server (`MockRestServiceServer`) with trimmed
  real responses.
- The repository and `PlanControllerTest` tests start the whole app against
  Postgres, with live data turned off.

## Project layout

```
src/main/java/io/github/hindih3/commuteplanner/
├── CommutePlannerApplication.java    entry point, Clock bean
└── planner/
    ├── Planner.java                  depth-first search over the segment tree
    ├── Segment, Leg, Plan, Mode, Commute    records and enums
    ├── DepartureSource.java          "when's the next departure?" interface
    ├── LiveFirstDepartureSource.java live predictions first, schedule fallback (@Primary)
    ├── JdbcDepartureSource.java      the schedule, from Postgres
    ├── CtaClient.java                Bus Tracker and Train Tracker calls
    ├── SegmentRepository.java        loads one commute's tree
    ├── StopRepository.java           our stop id -> CTA stop id
    └── PlanController.java           GET /plans
```

## Known limitations

- Weekday schedule only. On a day a route doesn't run (the 157 on Sundays),
  the schedule fallback still offers its weekday times. This isn't a problem
  for me since I only have classes on weekdays anyway
- Times are compared within one day, so trips that cross midnight are wrong.
  This is also not a big deal for me because I don't have classes at midnight 
  (thankfully)
- The schedule currently expires 2026-11-30 and is refreshed by hand.
- Plans don't say which departures are live and which come from the schedule.
- The database-backed tests expect today's CTA timetable.

## Roadmap

- A scheduled importer that refreshes the timetable from CTA's GTFS feed
- Tests on their own data instead of the real timetable
- A `live` flag on each leg
- Deadlines without typing them: the next Metra train you can make (going
  home) or the next class (going to campus)

## Related

- **commute-bot**: a Discord bot that calls `GET /plans`.
  `/commute deadline: 9:00 [direction: home]` replies with the best routes
  leaving right now.

---

Transit data from the Chicago Transit Authority (GTFS feed, Bus Tracker and
Train Tracker APIs). This project isn't affiliated with or endorsed by CTA.
