# Auth Service Timings from Container Logs

Source container: `perf-auth-service`  
Log window in container output: 2026-02-14T11:32:05Z to 2026-02-14T11:37:10Z  
Generated at: 2026-02-14 13:45:29 +02:00

## Endpoint Request Durations (Controller)

```text
+-----------+---------+--------+--------+--------+--------+
| Operation | Samples | Min ms | Avg ms | P95 ms | Max ms |
+-----------+---------+--------+--------+--------+--------+
| Login     |    4682 |     71 | 825.91 |   1617 |   3178 |
| Register  |    4682 |     64 | 568.13 |   1132 |   2256 |
| Validate  |   37400 |      0 |  23.74 |    107 |   1841 |
+-----------+---------+--------+--------+--------+--------+
```

## Service Total Durations (AuthService)

```text
+-----------+---------+--------+--------+--------+--------+
| Operation | Samples | Min ms | Avg ms | P95 ms | Max ms |
+-----------+---------+--------+--------+--------+--------+
| Login     |    4682 |     71 | 821.05 |   1614 |   3178 |
| Register  |    4682 |     64 | 563.48 |   1129 |   2256 |
| Validate  |   37400 |      0 |  21.03 |     99 |   1841 |
+-----------+---------+--------+--------+--------+--------+
```

## Internal Operation Durations (AuthService)

```text
+--------------------------------------------------+---------+--------+--------+--------+--------+
| Internal Operation                               | Samples | Min ms | Avg ms | P95 ms | Max ms |
+--------------------------------------------------+---------+--------+--------+--------+--------+
| Login -> db.findByNormalizedUsername             |    4682 |      0 | 216.37 |    840 |   1919 |
| Login -> password verification                   |    4682 |     66 | 240.69 |    392 |   1077 |
| Login -> token generation                        |    4682 |      0 |   0.12 |      0 |    218 |
| Login -> db.save(session)                        |    4682 |      2 | 195.03 |    560 |   1536 |
| Register -> db.existsByNormalizedUsername        |    4682 |      0 | 119.34 |    469 |   1375 |
| Register -> password hashing                     |    4682 |     60 | 239.45 |    376 |    985 |
| Register -> db.save(user)                        |    4682 |      2 |  93.27 |    308 |   1073 |
| Validate -> db.findValidationProjectionByTokenHash |   37400 |      0 |  12.30 |     56 |   1020 |
| Shared -> Token hashing                          |   42082 |      0 |   0.01 |      0 |     47 |
+--------------------------------------------------+---------+--------+--------+--------+--------+
```

## Notes

- Metrics are parsed from `docker logs perf-auth-service`.
- Values are calculated only for the 5-minute smoke run log window above.
