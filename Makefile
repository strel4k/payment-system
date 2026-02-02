DOCKER_COMPOSE = docker compose

GRAFANA_URL     = http://localhost:3000/api/health
LOKI_URL        = http://localhost:3100/ready
LOKI_QUERY_URL  = http://localhost:3100/loki/api/v1/query_range
PROM_URL        = http://localhost:9090/-/healthy
KEYCLOAK_URL    = http://localhost:8080/health/ready
NEXUS_URL       = http://localhost:8091/service/rest/v1/status
TEMPO_URL       = http://localhost:3200/status

INDIVIDUALS_API_URL = http://localhost:8081/actuator/health
PERSON_SERVICE_URL  = http://localhost:8082/actuator/health

INFRA_SERVICES ?= person-postgres keycloak-postgres individuals-keycloak nexus loki prometheus grafana promtail tempo

.PHONY: all up start stop clean logs rebuild infra infra-logs infra-stop health loki-test test test-coverage nexus-publish nexus-password

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
	@echo "Waiting for Nexus..."
	@$(call WAIT_HTTP,$(NEXUS_URL))
	@echo "Waiting for Tempo..."
	@$(call WAIT_HTTP,$(TEMPO_URL))
	@echo "Waiting for Person Service..."
	@$(call WAIT_HTTP,$(PERSON_SERVICE_URL))
	@echo "Waiting for Individuals API..."
	@$(call WAIT_HTTP,$(INDIVIDUALS_API_URL))

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
	@echo "Waiting for Nexus..."
	@$(call WAIT_HTTP,$(NEXUS_URL))
	@echo "Waiting for Tempo..."
	@$(call WAIT_HTTP,$(TEMPO_URL))
	@echo "Infrastructure is ready."

infra-logs:
	$(DOCKER_COMPOSE) logs -f --tail=200 $(INFRA_SERVICES)

infra-stop:
	$(DOCKER_COMPOSE) stop $(INFRA_SERVICES)

health:
	@echo "=== Infrastructure Health ==="
	@echo "Grafana:"; curl -sS $(GRAFANA_URL) | jq
	@echo "Loki:"; curl -sS $(LOKI_URL); echo
	@echo "Prometheus:"; curl -sS $(PROM_URL); echo
	@echo "Tempo:"; curl -sS $(TEMPO_URL) | jq
	@echo "Nexus:"; curl -sS $(NEXUS_URL) | jq
	@echo "Keycloak:"; curl -sS $(KEYCLOAK_URL) | jq
	@echo ""
	@echo "=== Application Services ==="
	@echo "Person Service:"; curl -sS $(PERSON_SERVICE_URL) | jq
	@echo "Individuals API:"; curl -sS $(INDIVIDUALS_API_URL) | jq
	@echo ""
	@echo "=== Prometheus Targets ==="
	@curl -sS http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | {job:.labels.job, health:.health, lastError:.lastError}'

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
	@echo "Running person-service tests..."
	cd person-service && ./gradlew test
	@echo "Running individuals-api tests..."
	cd individuals-api && ./gradlew test

test-coverage:
	@echo "Generating coverage reports..."
	./gradlew jacocoTestReport
	@echo "Coverage reports generated:"
	@echo "  person-service:   person-service/build/reports/jacoco/test/html/index.html"
	@echo "  individuals-api:  individuals-api/build/reports/jacoco/test/html/index.html"

nexus-publish:
	@echo "Publishing person-service-client to Nexus..."
	./gradlew :common:publish -PnexusUsername=admin -PnexusPassword=admin123
	@echo "Verifying artifact in Nexus..."
	@curl -u admin:admin123 'http://localhost:8091/service/rest/v1/components?repository=maven-releases' | jq '.items[] | {name, group, version}'

nexus-password:
	@echo "Nexus admin password:"
	@docker exec nexus cat /nexus-data/admin.password

rebuild: clean all