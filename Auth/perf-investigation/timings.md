# Auth Service Timings from Container Logs

Source container: `perf-auth-service`  
Log window in container output: 2026-02-14T01:02:46Z to 2026-02-14T01:07:50Z  
Generated at: 2026-02-14 03:09:48 +02:00

## Endpoint Request Durations (Controller)

```text
+-----------+---------+--------+--------+--------+--------+
| Operation | Samples | Min ms | Avg ms | P95 ms | Max ms |
+-----------+---------+--------+--------+--------+--------+
| Login     |     249 |     68 | 100.59 |    151 |    632 |
| Register  |     249 |     70 |  95.29 |    137 |    814 |
| Validate  |    1936 |      1 |   1.46 |      2 |     29 |
+-----------+---------+--------+--------+--------+--------+
```

## Service Total Durations (AuthService)

```text
+-----------+---------+--------+--------+--------+--------+
| Operation | Samples | Min ms | Avg ms | P95 ms | Max ms |
+-----------+---------+--------+--------+--------+--------+
| Login     |     249 |     68 | 100.46 |    151 |    631 |
| Register  |     249 |     70 |  95.14 |    137 |    812 |
| Validate  |    1936 |      1 |   1.33 |      2 |     28 |
+-----------+---------+--------+--------+--------+--------+
```

## Internal Operation Durations (AuthService)

```text
+--------------------------------------------------+---------+--------+--------+--------+--------+
| Internal Operation                               | Samples | Min ms | Avg ms | P95 ms | Max ms |
+--------------------------------------------------+---------+--------+--------+--------+--------+
| Login -> db.findByNormalizedUsername             |     249 |      1 |   3.04 |     13 |     53 |
| Login -> password verification                   |     249 |     61 |  89.90 |    125 |    594 |
| Login -> token generation                        |     249 |      0 |   0.00 |      0 |      0 |
| Login -> db.save(session)                        |     249 |      3 |   5.83 |     13 |     34 |
| Register -> db.existsByNormalizedUsername        |     249 |      0 |   1.84 |      3 |    131 |
| Register -> password hashing                     |     249 |     65 |  87.17 |    121 |    594 |
| Register -> db.save(user)                        |     249 |      3 |   4.73 |      7 |     86 |
| Validate -> db.findValidationProjectionByTokenHash |    1936 |      0 |   1.16 |      2 |     25 |
| Shared -> Token hashing                          |    2185 |      0 |   0.00 |      0 |      1 |
+--------------------------------------------------+---------+--------+--------+--------+--------+
```

## Notes

- Metrics are parsed from `docker logs perf-auth-service`.
- Values are calculated only for the 5-minute smoke run log window above.
