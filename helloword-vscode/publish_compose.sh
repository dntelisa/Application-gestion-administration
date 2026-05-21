#!/bin/bash
docker compose -f docker-compose.prod.yml config > docker-compose-prod-rendered.yml
docker push dntelisa/asso:1.0.0
