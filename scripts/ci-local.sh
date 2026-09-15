```bash
#!/bin/bash

set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo "=== Running Maven tests ==="

for dir in auth-service billing-service catalog-service gateway report-service sales-service store-service; do
    echo ""
    echo "===== $dir ====="

    (
        cd "$PROJECT_ROOT/services/$dir"
        ./mvnw test
    )
done

echo ""
echo "=== Building Docker images ==="

cd "$PROJECT_ROOT"

docker compose build

echo ""
echo "=== CI checks passed ==="
```
