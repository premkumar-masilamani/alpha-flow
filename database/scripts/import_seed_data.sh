#!/bin/bash

export PGPASSWORD=${POSTGRES_PASSWORD}

echo "Importing seed data (idempotent)..."
psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} -h ${POSTGRES_DB} -f ${DOCKER_SEED_DATA_VOLUME}/seed_data.sql
echo "Importing seed data is complete."

