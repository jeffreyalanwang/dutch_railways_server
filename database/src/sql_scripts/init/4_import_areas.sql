--Area with its parent class' attributes.
CREATE OR REPLACE VIEW AreaFull AS (
    SELECT Id, Name, Geom
    FROM Area NATURAL JOIN Place
);

CREATE OR REPLACE FUNCTION AreaFull_insert_func() RETURNS trigger AS
$$
DECLARE
    place_id INT;
BEGIN
    IF NEW.Name IS NULL OR NEW.Geom IS NULL THEN
        RAISE EXCEPTION 'Incorrect trigger configuration';
    END IF;

    INSERT INTO Place (Subclass, Name)
        VALUES ('Area', NEW.Name)
        RETURNING Id INTO place_id;
    INSERT INTO Area (Id, Geom)
        VALUES (place_id, NEW.Geom);

    RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER AreaFull_insert
    INSTEAD OF INSERT ON AreaFull
    FOR EACH ROW EXECUTE FUNCTION AreaFull_insert_func();

CREATE OR REPLACE RULE AreaFull_update AS ON UPDATE TO AreaFull
    DO INSTEAD (
        UPDATE Area
            SET Geom = NEW.Geom
            WHERE Id = OLD.Id;
        UPDATE Place
            SET Id = NEW.Id, Name = NEW.Name
            WHERE Id = OLD.Id;
    );

CREATE OR REPLACE RULE AreaFull_del AS ON DELETE TO AreaFull
    DO INSTEAD
        DELETE FROM Place
        WHERE Id = OLD.Id;

-- The below must have been run before this point
-- &'C:\Program Files\QGIS 4.0.1\bin\ogr2ogr.exe' "PG:dbname=dutch_railways user=postgres password=****" .\data_src\BestuurlijkeGebieden_2026.gpkg landgebied -nln src_landgebied -nlt PROMOTE_TO_MULTI -lco GEOMETRY_NAME=geom -lco FID=gid
-- &'C:\Program Files\QGIS 4.0.1\bin\ogr2ogr.exe' "PG:dbname=dutch_railways user=postgres password=****" .\data_src\BestuurlijkeGebieden_2026.gpkg provinciegebied -nln src_provinciegebied -nlt PROMOTE_TO_MULTI -lco GEOMETRY_NAME=geom -lco FID=gid
-- &'C:\Program Files\QGIS 4.0.1\bin\ogr2ogr.exe' "PG:dbname=dutch_railways user=postgres password=****" .\data_src\BestuurlijkeGebieden_2026.gpkg gemeentegebied -nln src_gemeentegebied -nlt PROMOTE_TO_MULTI -lco GEOMETRY_NAME=geom -lco FID=gid

INSERT INTO AreaFull (Name, Geom)
    SELECT naam as Name, geom as Geom FROM src_landgebied;
INSERT INTO AreaFull (Name, Geom)
    SELECT naam as Name, geom as Geom FROM src_provinciegebied;
INSERT INTO AreaFull (Name, Geom)
    SELECT naam as Name, geom as Geom FROM src_gemeentegebied;

DROP TABLE src_landgebied;
DROP TABLE src_provinciegebied;
DROP TABLE src_gemeentegebied;

--check SRID for an Area
SELECT ST_srid((SELECT Geom FROM AreaFull LIMIT 1));
