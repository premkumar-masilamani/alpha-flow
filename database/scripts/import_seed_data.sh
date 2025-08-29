#!/bin/bash

export PGPASSWORD=${POSTGRES_PASSWORD}

echo "Checking if 'zones' table has data..."

COUNT=$(psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} -h ${POSTGRES_DB} -tAc "SELECT count(*) FROM zones" 2>/dev/null)

if [ -z "$COUNT" ] || [ "$COUNT" -eq 0 ]; then
  echo "Importing seed data..."
  psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} -h ${POSTGRES_DB} -f ${DOCKER_SEED_DATA_VOLUME}/seed_data.sql
  echo "Importing seed data is complete."
else
  echo "Tables already has data. Skipping import."
fi

echo "Checking if 'component_works' table has data..."

COUNT=$(psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} -h ${POSTGRES_DB} -tAc "SELECT count(*) FROM component_works" 2>/dev/null)

if [ -z "$COUNT" ] || [ "$COUNT" -eq 0 ]; then
  echo "Importing random data into 'component_works'..."
  psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} -h ${POSTGRES_DB} -f ${DOCKER_SEED_DATA_VOLUME}/random_data.sql
  COUNT=$(psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} -h ${POSTGRES_DB} -tAc "SELECT count(*) FROM component_works" 2>/dev/null)
  echo "Scheme tasks import complete. Total rows: $COUNT"
else
  echo "Scheme tasks table already has $COUNT rows. Skipping import."
fi
