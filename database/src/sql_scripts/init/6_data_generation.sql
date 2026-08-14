--	Duplicate daily PassServices
--		Copy pass services departing their first stop in the
--	    earliest 24 hours known to the database to cover the
--	    full next 24 hours.
--      This rule thus prevents overlap in services that
--      span multiple days.
CREATE OR REPLACE PROCEDURE duplicate_daily_pass_services()
BEGIN ATOMIC
    -- Source stops, shifted in time and
    -- with would-be duplicates filtered out.
    WITH ShiftedStop AS (
        WITH UpdateRule AS (
            -- For each potential source departure:
            -- the amount we would shift it by
            WITH FirstDepart AS (
                SELECT DISTINCT ON (Service)
                    Service,
                    DepartTime as Time
                FROM Stop
                ORDER BY Service, DepartTime NULLS LAST
            )
            SELECT
                Service,
                date_trunc('day', now() AT TIME ZONE 'UTC' - Time) + INTERVAL '1 day' AS Delta
            FROM FirstDepart
            WHERE Time < (
                SELECT min(DepartTime) + INTERVAL '24 hours'
                FROM Stop
            )
        ), ShiftedBaseStop AS (
            SELECT
               Service,
               (ArriveTime + Delta) as ArriveTime, -- Delta is in days, so midnight is preserved
               (DepartTime + Delta) as DepartTime, -- Delta is in days, so midnight is preserved
               Station
            FROM Stop INNER JOIN UpdateRule USING (Service)
        ), WouldDuplicate AS (
            -- Each generated service (ShiftedService)
            -- where, for *any* existing service (ExistingService),
            -- the two services' set of stops are identical
            SELECT DISTINCT ShiftedService.Id
            FROM (
                (SELECT Service as Id FROM UpdateRule) as ShiftedService
                CROSS JOIN
                (SELECT Id FROM PassService) as ExistingService
            )
            WHERE (
                -- They have identical sets of stops if:
                -- there is not a stop unique to one from the other
                WITH ShiftedServiceStop AS (
                    SELECT DepartTime, ArriveTime, Station FROM ShiftedBaseStop WHERE Service = ShiftedService.Id
                ), ExistingServiceStop AS (
                    SELECT DepartTime, ArriveTime, Station FROM Stop WHERE Service = ExistingService.Id
                )
                SELECT NOT EXISTS (
                    -- symmetric difference
                    ((SELECT * FROM ShiftedServiceStop) EXCEPT (SELECT * FROM ExistingServiceStop))
                    UNION
                    ((SELECT * FROM ExistingServiceStop) EXCEPT (SELECT * FROM ShiftedServiceStop))
                )
            )
        )
        SELECT * FROM ShiftedBaseStop WHERE Service NOT IN (SELECT Id FROM WouldDuplicate)
    ), Key AS (
        -- Duplicate services for which we shifted stops
        MERGE INTO PassService USING ( -- actually an insert
            SELECT
                Id,
                CONCAT(
                    Name,
                    ' departing on ',
                    TO_CHAR(now() AT TIME ZONE 'UTC', 'FMMonth DD, YYYY "(UTC)"')
                ) as Name,
                Consist
            FROM (SELECT DISTINCT Service FROM ShiftedStop) as _
            INNER JOIN PassService ON Service = PassService.Id
        ) as Incoming
        ON FALSE
        WHEN NOT MATCHED THEN
            INSERT (name, consist)
            VALUES (Incoming.name, Incoming.consist)
        RETURNING PassService.Id AS NewId, Incoming.Id AS SourceId
    )
    INSERT INTO Stop
        SELECT Key.NewId as Service, ArriveTime, DepartTime, Station
        FROM ShiftedStop INNER JOIN Key
        ON ShiftedStop.Service = Key.SourceId;
END;

CALL duplicate_daily_pass_services();
