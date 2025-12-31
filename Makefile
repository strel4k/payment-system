DOCKER_COMPOSE = docker compose

GRAFANA_URL     = http://localhost:3000/api/health
LOKI_URL        = http://localhost:3100/ready
LOKI_QUERY_URL = http://localhost:3100/loki/api/v1/query_range
PROM_URL        = http://localhost:9090/-/healthy
KEYCLOAK_URL    = http://localhost:9000/health/ready
APP_HEALTH_URL  = http://localhost:8081/actuator/health

INFRA_SERVICES ?= keycloak-postgres keycloak loki prometheus grafana promtail

.PHONY: all up start stop clean logs rebuild infra infra-logs infra-stop health loki-test test

all: infra start health

ifeq ($(OS),Windows_NT)
WAIT_HTTP = powershell -Command "while ($$true) { \
		try { Invoke-WebRequest -UseBasicParsing -Uri $(1) -ErrorAction Stop | Out-Null; break } \
		catch { Write-Host 'Not ready: $(1)'; Start-Sleep -Seconds 3 } \
	}"
else
WAIT_HTTP = until curl -sf $(1) > /dev/null; do echo 'Not ready: $(1)'; sleep 3; done
endif

up: start

start:
	$(DOCKER_COMPOSE) up -d
	@$(MAKE) wait

wait:
	@echo "Waiting for Loki..."
	@$(call WAIT_HTTP,$(LOKI_URL))
	@echo "Waiting for Prometheus..."
	@$(call WAIT_HTTP,$(PROM_URL))
	@echo "Waiting for Grafana..."
	@$(call WAIT_HTTP,$(GRAFANA_URL))
	@echo "Waiting for Keycloak..."
	@$(call WAIT_HTTP,$(KEYCLOAK_URL))
	@echo "Waiting for Individuals API..."
	@$(call WAIT_HTTP,$(APP_HEALTH_URL))

stop:
	$(DOCKER_COMPOSE) down

clean: stop
	$(DOCKER_COMPOSE) rm -f
	docker volume rm $$(docker volume ls -qf dangling=true) 2>/dev/null || true

logs:
	$(DOCKER_COMPOSE) logs -f --tail=200

infra:
	@echo "Starting infrastructure services: $(INFRA_SERVICES)"
	$(DOCKER_COMPOSE) up -d $(INFRA_SERVICES)
	@echo "Waiting for Loki..."
	@$(call WAIT_HTTP,$(LOKI_URL))
	@echo "Waiting for Prometheus..."
	@$(call WAIT_HTTP,$(PROM_URL))
	@echo "Waiting for Grafana..."
	@$(call WAIT_HTTP,$(GRAFANA_URL))
	@echo "Waiting for Keycloak..."
	@$(call WAIT_HTTP,$(KEYCLOAK_URL))
	@echo "Infrastructure is ready."

infra-logs:
	$(DOCKER_COMPOSE) logs -f --tail=200 $(INFRA_SERVICES)

infra-stop:
	$(DOCKER_COMPOSE) stop $(INFRA_SERVICES)

health:
	@echo "Grafana:"; curl -sS $(GRAFANA_URL) | jq
	@echo "Loki:"; curl -sS $(LOKI_URL); echo
	@echo "Prometheus:"; curl -sS $(PROM_URL); echo
	@echo "Keycloak:"; curl -sS $(KEYCLOAK_URL) | jq
	@echo "Individuals API:"; curl -sS $(APP_HEALTH_URL) | jq
	@echo "Prometheus targets:"; curl -sS http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | {job:.labels.job, health:.health, lastError:.lastError}'

loki-test:
	@echo "Triggering a log event (restart individuals-api)..."
	@$(DOCKER_COMPOSE) restart individuals-api >/dev/null
	@sleep 3
	@echo "Querying Loki for individuals-api logs (last 5 minutes)..."
	@curl -G -sS "$(LOKI_QUERY_URL)" \
	  --data-urlencode 'query={job="docker",service="individuals-api"}' \
	  --data-urlencode 'limit=20' \
	  --data-urlencode "start=$$(date -u -v-5M +%s)000000000" \
	| jq '.data.result | length'

test:
	cd individuals-api && ./gradlew test

rebuild: clean all