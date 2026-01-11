# API Design

## Design Principles

- **Versioning:** Path-based `/api/v1/resource`
- **Error format:** RFC 9457 Problem Details
- **Response format:** JSON with consistent structure
- **Propery names** Use camelCase for property names

## Error Handling

Use RFC 9457 Problem Details format:

```json
{
  "type": "https://api.onlineshop.com/errors/item-not-found",
  "title": "Item Not Found",
  "status": 404,
  "detail": "Item with ID 123 does not exist",
  "instance": "/api/v1/items/123"
}
```

## Observability and Metrics

### Metrics Standards

All services should expose metrics via Spring Boot Actuator at `/actuator/prometheus`.

### Metric Naming Conventions

Follow Micrometer/Prometheus best practices:

- **Use tags (dimensions) instead of hierarchical names**
- **Metric names**: lowercase with dots (`service.operation.type`)
- **Suffix for type**: `.total` for counters, `.seconds` for timers
- **Tags**: Use for dimensions like `service`, `layer`, `result`, `status`

### Examples

✅ **Good** - Tag-based dimensional metrics:
```
service.cache.operations.total{layer="l1", service="auth", result="hit"}
service.api.requests.total{endpoint="/items", method="GET", status="200"}
```

❌ **Bad** - Hierarchical naming:
```
service.cache.l1.auth.hits
service.api.items.get.200
```

### Standard Tags

Common tags across services:

- `service`: Service name (e.g., "auth", "items", "gateway")
- `layer`: Cache or component layer (e.g., "l1", "l2", "database")
- `result`: Operation outcome (e.g., "hit", "miss", "success", "failure")
- `status`: HTTP status code (e.g., "200", "404", "500")
- `method`: HTTP method (e.g., "GET", "POST")
- `endpoint`: API endpoint path

### Benefits of Tag-Based Metrics

1. **Future-proof**: Adding new dimensions doesn't require new metrics
2. **Flexible querying**: Filter and aggregate by any dimension
3. **Better dashboards**: Single metric can be visualized multiple ways
4. **Industry standard**: Works seamlessly with Prometheus, Grafana, Datadog, etc.

### Example Prometheus Queries

```promql
# Total requests per service
sum by (service) (rate(service_api_requests_total[5m]))

# Error rate for specific endpoint
sum(rate(service_api_requests_total{endpoint="/items",status=~"5.."}[5m])) /
sum(rate(service_api_requests_total{endpoint="/items"}[5m]))

# Cache hit rate
sum(rate(service_cache_operations_total{result="hit"}[5m])) /
sum(rate(service_cache_operations_total[5m]))
```

### References

- [Micrometer Naming Conventions](https://docs.micrometer.io/micrometer/reference/concepts/naming.html)
- [Prometheus Metric Naming Best Practices](https://prometheus.io/docs/practices/naming/)
- [API Gateway Metrics Documentation](../api-gateway/CLAUDE.md#metrics-and-observability)