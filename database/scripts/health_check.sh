#!/bin/bash

psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -c "select 1" > /dev/null 2>&1
