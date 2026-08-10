--Station with its parent class' attributes.
CREATE OR REPLACE VIEW StationFull AS (
    SELECT Id, Name, Address, Geom
    FROM Station NATURAL JOIN Place
);

CREATE OR REPLACE FUNCTION StationFull_insert_func() RETURNS trigger AS
$$
DECLARE
    place_id INT;
BEGIN
    IF NEW.Name IS NULL OR NEW.Geom IS NULL THEN
        RAISE EXCEPTION 'Incorrect trigger configuration';
    END IF;

    INSERT INTO Place (Subclass, Name)
        VALUES ('Station', NEW.Name)
        RETURNING Id INTO place_id;
    INSERT INTO Station (Id, Address, Geom)
        VALUES (place_id, NEW.Address, NEW.Geom);
    NEW.Id = place_id;

    RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER StationFull_insert
    INSTEAD OF INSERT ON StationFull
    FOR EACH ROW EXECUTE FUNCTION StationFull_insert_func();

CREATE OR REPLACE RULE StationFull_update AS ON UPDATE TO StationFull
    DO INSTEAD (
        UPDATE Station
            SET Address = NEW.Address, Geom = NEW.Geom
            WHERE Id = OLD.Id;
        UPDATE Place
            SET Id = NEW.Id, Name = NEW.Name
            WHERE Id = OLD.Id;
    );

CREATE OR REPLACE RULE StationFull_del AS ON DELETE TO StationFull
    DO INSTEAD
        DELETE FROM Place
        WHERE Id = OLD.Id;

--Modify `PassService` table and track Ids.
CREATE TEMP TABLE tmp_translateKey_PassService(csv_num INT, pg_id INT)
    ON COMMIT DROP;
CREATE TEMP VIEW tmp_dummyInsert_PassService(csv_num, pg_id, Name, Consist) AS (
    SELECT csv_num, pg_id, Name, Consist
    FROM tmp_translateKey_PassService, PassService
    WHERE tmp_translateKey_PassService.pg_id = PassService.Id
);
CREATE OR REPLACE FUNCTION trigger_fn_dummyInsert_PassService() RETURNS trigger AS
$$
DECLARE
    generated_id INT;
BEGIN
    IF NEW.Name IS NULL OR NEW.csv_num IS NULL THEN
        RAISE EXCEPTION 'Incorrect trigger configuration';
    END IF;

    INSERT INTO PassService (Name, Consist)
        VALUES (NEW.Name, NEW.Consist)
        RETURNING Id INTO generated_id;
    INSERT INTO tmp_translateKey_PassService (csv_num, pg_id)
        VALUES (NEW.csv_num, generated_id);

    RETURN NEW;
END; $$ LANGUAGE plpgsql;
CREATE TRIGGER trigger_dummyInsert_PassService
    INSTEAD OF INSERT ON tmp_dummyInsert_PassService
    FOR EACH ROW EXECUTE FUNCTION trigger_fn_dummyInsert_PassService();

--Modify `StationFull` table and track Ids.
CREATE TEMP TABLE tmp_translateKey_Station(csv_uic INT, pg_id INT)
    ON COMMIT DROP;
CREATE TEMP VIEW tmp_dummyInsert_Station(csv_uic, pg_id, Name, Address, Geom) AS (
    SELECT csv_uic, pg_id, Name, Address, Geom
    FROM tmp_translateKey_Station, StationFull
    WHERE tmp_translateKey_Station.pg_id = StationFull.Id
);
CREATE OR REPLACE FUNCTION trigger_fn_dummyInsert_Station() RETURNS trigger AS
$$
DECLARE
    generated_id INT;
BEGIN
    IF NEW.Name IS NULL OR NEW.csv_uic IS NULL OR NEW.Geom IS NULL THEN
        RAISE EXCEPTION 'Incorrect trigger configuration';
    END IF;

    INSERT INTO StationFull (Name, Address, Geom)
        VALUES (NEW.Name, NEW.Address, NEW.Geom)
        RETURNING Id INTO generated_id;
    INSERT INTO tmp_translateKey_Station (csv_uic, pg_id)
        VALUES (NEW.csv_uic, generated_id);

    RETURN NEW;
END; $$ LANGUAGE plpgsql;
CREATE TRIGGER trigger_dummyInsert_Station
    INSTEAD OF INSERT ON tmp_dummyInsert_Station
    FOR EACH ROW EXECUTE FUNCTION trigger_fn_dummyInsert_Station();

--Copy data in from CSV files.

CREATE TEMP TABLE tmp_TrainsetAmenities (Id INT, TrainsetType VARCHAR, Amenity VARCHAR)
    ON COMMIT DROP;
COPY tmp_TrainsetAmenities
    FROM 'C:\Users\jeffw\Code\pgadmin\Dutch Railways\extra\ns_results\trainsetamenities.csv'
    WITH CSV HEADER QUOTE '"';
INSERT INTO TrainsetType(name)
    SELECT DISTINCT TrainsetType FROM tmp_TrainsetAmenities;
INSERT INTO Amenity(description)
    SELECT DISTINCT Amenity FROM tmp_TrainsetAmenities;
INSERT INTO TrainsetAmenities(TrainsetType, Amenity)
    SELECT TrainsetType, Amenity.Id
    FROM tmp_TrainsetAmenities
        INNER JOIN Amenity ON tmp_TrainsetAmenities.Amenity = Amenity.description;

CREATE TEMP TABLE tmp_PassService (Id INT, Num INT, Name VARCHAR, Trainset VARCHAR)
    ON COMMIT DROP;
COPY tmp_PassService
    FROM 'C:\Users\jeffw\Code\pgadmin\Dutch Railways\extra\ns_results\passservice.csv'
    WITH CSV HEADER QUOTE '"';
INSERT INTO tmp_dummyInsert_PassService(csv_num, Name, Consist)
    SELECT Num, Name, Trainset AS Consist
    FROM tmp_PassService;

CREATE TEMP TABLE tmp_Station (Id INT, Uic INT, Name VARCHAR, Lat Numeric, Lng Numeric, Address VARCHAR)
    ON COMMIT DROP;
COPY tmp_Station
    FROM 'C:\Users\jeffw\Code\pgadmin\Dutch Railways\extra\ns_results\stations.csv'
    WITH CSV HEADER QUOTE '"';
INSERT INTO tmp_dummyInsert_Station(csv_uic, Name, Address, Geom)
    SELECT Uic, Name, Address, ST_Transform(ST_Point(Lng, Lat, 4326), 28992) AS Geom
    FROM tmp_Station;

CREATE TEMP TABLE tmp_Stop (Id INT, PassService_num INT, Arrival TIMESTAMP, Departure TIMESTAMP, Stations_uic INT)
    ON COMMIT DROP;
COPY tmp_Stop
    FROM 'C:\Users\jeffw\Code\pgadmin\Dutch Railways\extra\ns_results\stop.csv'
    WITH CSV HEADER QUOTE '"';
INSERT INTO Stop(Service, ArriveTime, DepartTime, Station)
    SELECT
        tmp_translateKey_PassService.pg_id AS Service,
        Arrival AS ArriveTime,
        Departure AS DepartTime,
        tmp_translateKey_Station.pg_id AS Station
    FROM tmp_Stop, tmp_translateKey_Station, tmp_translateKey_PassService
    WHERE tmp_Stop.PassService_num = tmp_translateKey_PassService.csv_num
      AND tmp_Stop.Stations_uic = tmp_translateKey_Station.csv_uic;
