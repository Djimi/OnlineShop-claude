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