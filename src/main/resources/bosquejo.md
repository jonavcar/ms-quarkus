### Client Contracts

**openapi-product-v1.yaml**

Headers: `Authorization`, `request-id`, `session-uuid`

**Search Products:**
- Request: Search by name or code
- Response: [`code`, `name`, `price`, `discount`, `stock`]

**openapi-client-v1.yaml**

Headers: `Authorization`, `request-id`, `session-uuid`, `caller-name`

**Search Client:**
- Request: Search by names or DNI
- Response: `names`, `dni`, `address`, `phone`

***

### Server Contract

**openapi.yaml**

Headers: `Authorization`, `request-id`, `session-uuid`, `app-code`

**Search Products:**
- Request: Search by name or code
- Response: [`code`, `name`, `price`, `status`]

**Search Client:**
- Request: Search by DNI or names
- Response: `names`, `dni`, `address`, `phone`

**Search Sales:**
- Request: Search by date or client (DNI) or opnCode
- Response: `saleId`, `opnCode`, `date`, `dni`, `names`, `products` [`code`, `name`, `quantity`, `amount`], `discount`, `subtotal`, `total`

**Create Sale:**
- Request: `dni`, `names`, `products` [`code`, `quantity`, `amount`], `discount`, `subtotal`, `total`
- Response: `saleId`, `opnCode`, `date`

This structure follows REST API naming conventions with camelCase for field names and kebab-case for HTTP headers.