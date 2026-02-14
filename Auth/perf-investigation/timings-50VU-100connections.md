# Auth Service Timings from Container Logs

Source container: `perf-auth-service`  
Log window in container output: 2026-02-14T12:33:43Z to 2026-02-14T12:38:47Z  
Generated at: 2026-02-14 14:45:33 +02:00

## Endpoint Request Durations (Controller)

```text
+-----------+---------+--------+--------+--------+--------+
| Operation | Samples | Min ms | Avg ms | P95 ms | Max ms |
+-----------+---------+--------+--------+--------+--------+
| Login     |    5117 |     72 | 734.91 |   1405 |   2309 |
| Register  |    5117 |     68 | 493.42 |    974 |   1944 |
| Validate  |   40880 |      0 |  19.39 |     87 |    992 |
+-----------+---------+--------+--------+--------+--------+
```

## Service Total Durations (AuthService)

```text
+-----------+---------+--------+--------+--------+--------+
| Operation | Samples | Min ms | Avg ms | P95 ms | Max ms |
+-----------+---------+--------+--------+--------+--------+
| Login     |    5117 |     71 | 731.01 |   1399 |   2280 |
| Register  |    5117 |     68 | 488.04 |    971 |   1944 |
| Validate  |   40880 |      0 |  16.44 |     81 |    992 |
+-----------+---------+--------+--------+--------+--------+
```

## Internal Operation Durations (AuthService)

```text
+--------------------------------------------------+---------+--------+--------+--------+--------+
| Internal Operation                               | Samples | Min ms | Avg ms | P95 ms | Max ms |
+--------------------------------------------------+---------+--------+--------+--------+--------+
| Login -> db.findByNormalizedUsername             |    5117 |      0 | 235.57 |    796 |   1847 |
| Login -> password verification                   |    5117 |     67 | 209.95 |    310 |    636 |
| Login -> token generation                        |    5117 |      0 |   0.08 |      0 |    353 |
| Login -> db.save(session)                        |    5117 |      2 | 194.54 |    586 |   1532 |
| Register -> db.existsByNormalizedUsername        |    5117 |      0 | 134.50 |    495 |   1176 |
| Register -> password hashing                     |    5117 |     63 | 199.42 |    299 |    732 |
| Register -> db.save(user)                        |    5117 |      2 |  83.62 |    288 |   1244 |
| Validate -> db.findValidationProjectionByTokenHash |   40880 |      0 |   9.86 |     48 |    992 |
| Shared -> Token hashing                          |   45997 |      0 |   0.01 |      0 |     47 |
+--------------------------------------------------+---------+--------+--------+--------+--------+
```

## Notes

- Metrics are parsed from `docker logs perf-auth-service`.
- Values are calculated only for the 5-minute smoke run log window above.
