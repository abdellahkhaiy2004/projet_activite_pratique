#!/usr/bin/env bash
# End-to-end smoke test of the running stack, through the gateway.
# Usage: scripts/smoke-test.sh [--with-bot]    (--with-bot also asks the chatbot, which calls OpenAI)
GATEWAY=${GATEWAY:-http://localhost:9999}
EUREKA=${EUREKA:-http://localhost:8761}
pass=0; fail=0

check() { # name, condition result
  if [ "$2" = "0" ]; then echo "  PASS  $1"; pass=$((pass+1)); else echo "  FAIL  $1"; fail=$((fail+1)); fi
}
status() { curl -s -o /dev/null -w "%{http_code}" "$@"; }

echo "Eureka registry"
apps=$(curl -s "$EUREKA/eureka/apps")
for app in CUSTOMER-SERVICE EBANK-SERVICE GATEWAY-SERVICE EBANK-BOT; do
  echo "$apps" | grep -q "<app>$app</app>"; check "$app registered" $?
done

echo "Gateway routes"
[ "$(status $GATEWAY/customer-service/customers)" = "200" ]; check "GET customers" $?
curl -s $GATEWAY/customer-service/customers/1 | grep -q Mohammed; check "GET customer 1 is Mohammed" $?
[ "$(status $GATEWAY/customer-service/customers/9999)" = "404" ]; check "unknown customer -> 404" $?
[ "$(status $GATEWAY/ebank-service/accounts)" = "200" ]; check "GET accounts" $?
n=$(curl -s $GATEWAY/ebank-service/accounts/customer/1 | grep -o '"customerId":1' | wc -l)
[ "$n" -ge 5 ]; check "customer 1 has at least 5 accounts" $?
curl -s $GATEWAY/ebank-service/accounts/customer/1 | grep -q '"name":"Mohammed"'; check "account enriched with customer (Feign)" $?

echo "Account creation rules"
body() { echo "{\"balance\":100,\"type\":\"CURRENT_ACCOUNT\",\"customerId\":$1}"; }
[ "$(status -X POST -H 'Content-Type: application/json' -d "$(body 1)" $GATEWAY/ebank-service/accounts)" = "201" ]; check "valid customer -> 201" $?
[ "$(status -X POST -H 'Content-Type: application/json' -d "$(body 999)" $GATEWAY/ebank-service/accounts)" = "400" ]; check "unknown customer -> 400" $?

echo "Browser access and docs"
curl -s -i -H "Origin: http://localhost:4200" $GATEWAY/customer-service/customers | grep -qi "access-control-allow-origin: http://localhost:4200"; check "CORS for Angular" $?
for p in 8056 8057 8058; do [ "$(status http://localhost:$p/swagger-ui/index.html)" = "200" ]; check "Swagger UI on $p" $?; done

echo "MCP servers"
for p in 8056 8057; do
  curl -s -X POST -H 'Content-Type: application/json' -H 'Accept: application/json,text/event-stream' \
    -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"smoke","version":"1"}}}' \
    http://localhost:$p/mcp | grep -q serverInfo; check "MCP initialize on $p" $?
done

if [ "$1" = "--with-bot" ]; then
  echo "Chatbot (uses OpenAI)"
  curl -s -G "$GATEWAY/ebank-bot/chat" --data-urlencode "query=List all customers" -m 90 | grep -qi "mohammed"; check "chatbot lists customers" $?
fi

echo; echo "passed: $pass  failed: $fail"
[ "$fail" -eq 0 ]
