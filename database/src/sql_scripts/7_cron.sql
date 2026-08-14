-- Prerequisites:
-- shared_preload_libraries = 'pg_cron'
-- cron.database_name = 'dutch_railways'
-- (the above are automatically configured in Kartoza Docker image)

CREATE EXTENSION IF NOT EXISTS pg_cron;

GRANT USAGE ON SCHEMA cron TO postgres;

SELECT cron.schedule(
   'pass-service-generate-daily',
   '0 1 * * *',
   'CALL duplicate_daily_pass_services();'
);
