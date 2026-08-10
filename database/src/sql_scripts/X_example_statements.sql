-- Two (2) SELECT queries demonstrating different ways of retrieving data

    -- Select all areas that a station stands within
    WITH RECURSIVE PlaceHierarchy(Parent, Child) AS (
        (
            SELECT LocatedIn AS Parent, Id AS Child
            FROM Place
            WHERE LocatedIn IS NOT NULL
        ) UNION (
            --each row of this subquery takes one anc-desc relationship
            --and extends it by one (i.e. `Child` is one generation lower)
            SELECT	P.Parent AS Parent	, C.Id AS Child
            FROM	PlaceHierarchy P	, Place C
            WHERE	P.Child				= C.LocatedIn
        )
    ), Target AS (
        SELECT Id
        FROM Place
        WHERE LOWER(Subclass::TEXT) = LOWER('Station')
          AND Name = 'Amsterdam Centraal'
    )
    SELECT Parent
    FROM PlaceHierarchy, Target
    WHERE Child = Target.Id;

    -- Select all train services that go between two specified stations
    WITH Origin AS (
        SELECT Id
        FROM Place
        WHERE LOWER(Subclass::TEXT) = LOWER('Station')
          AND Name = 'Amsterdam Centraal'
    ), Destination AS (
        SELECT Id
        FROM Place
        WHERE LOWER(Subclass::TEXT) = LOWER('Station')
          AND Name = 'Rotterdam Centraal'
    ), OriginStops AS (
        SELECT Service, DepartTime
        FROM Stop, Origin
        WHERE Stop.Station = Origin.Id
    ), DestinationStops AS (
        SELECT Service, ArriveTime
        FROM Stop, Destination
        WHERE Stop.Station = Destination.Id
    ), MatchingServiceIds AS (
        SELECT OriginStops.Service
        FROM OriginStops, DestinationStops
        WHERE OriginStops.Service = DestinationStops.Service
          AND OriginStops.DepartTime < DestinationStops.ArriveTime
    )
    SELECT *
    FROM PassService
        INNER JOIN MatchingServiceIds
        ON PassService.Id = MatchingServiceIds.Service;

PREPARE SampleServiceSegment AS (
    SELECT ArriveTime, DepartTime, Place.Name
    FROM Stop
        INNER JOIN Place
        ON Stop.Station = Place.Id
    WHERE Service = 583
      AND ArriveTime > '2026-05-02 19:10:00'::TIMESTAMP
      AND ArriveTime < '2026-05-02 19:25:00'::TIMESTAMP
    ORDER BY ArriveTime
);

EXECUTE SampleServiceSegment;

-- One (1) INSERT query to add new data

    -- Insert a new stop on a specific train route

    INSERT INTO Stop (Service, ArriveTime, DepartTime, Station)
    VALUES (
        583,
        '2026-05-02 19:18:00'::TIMESTAMP,
        '2026-05-02 19:20:00'::TIMESTAMP,
        (SELECT Id FROM StationFull WHERE Name = 'De Vink')
    );

    EXECUTE SampleServiceSegment;

-- One (1) UPDATE query to modify existing data

    -- Update the time of a specific stop
    UPDATE Stop
    SET ArriveTime = ArriveTime + INTERVAL '2 minutes',
        DepartTime = DepartTime + INTERVAL '2 minutes'
    WHERE Service = 583
      AND Station = (SELECT Id FROM StationFull WHERE Name = 'De Vink');

    EXECUTE SampleServiceSegment;

-- One (1) DELETE query to remove data

    -- Delete a stop on a specific train route
    DELETE FROM Stop
    WHERE Service = 583
      AND Station = (SELECT Id FROM StationFull WHERE Name = 'De Vink');

    EXECUTE SampleServiceSegment;