#!/bin/bash

export PGPASSWORD=${POSTGRES_PASSWORD}

echo "Checking if 'tickers' table has data..."

COUNT=$(psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} -h ${POSTGRES_DB} -tAc "SELECT count(*) FROM tickers" 2>/dev/null)

if [ -z "$COUNT" ] || [ "$COUNT" -eq 0 ]; then
  echo "Importing seed data..."
  psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} -h ${POSTGRES_DB} -f ${DOCKER_SEED_DATA_VOLUME}/seed_data.sql
  echo "Importing seed data is complete."
else
  echo "Tables already has data. Skipping import."
fi
