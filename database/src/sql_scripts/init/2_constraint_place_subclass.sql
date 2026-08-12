--These triggers occur on INSERT, UPDATE ON Id OR Subclass, or DELETE.
--They modify `Subclass`. (Foreign Key constraints already present may modify child-class `Id`.)

--	Check Place Subclass
--		Raises exception when `Place.Subclass` does not correspond with
--		a subclass table referencing the triggering row.
--		NOTE: Assumes `Place.Subclass` cannot be NULL (i.e. every row belongs to a subclass).
--	Trigger from:
--		Table: `Place`
--		AFTER FOR EACH ROW
--		DEFERRABLE (to allow subclass row creation afterwards)
--		INSERT OR UPDATE OF Subclass
CREATE OR REPLACE FUNCTION check_place_subclass() RETURNS trigger
AS $$ DECLARE
	ExistsInSubclass BOOLEAN;
BEGIN
	IF NEW.Id IS NULL OR NEW.Subclass IS NULL THEN
		RAISE EXCEPTION 'Incorrect trigger configuration';
	END IF;
	
	--where subclass matches Area or Station, Id must be in that table

	EXECUTE 'SELECT CASE WHEN EXISTS ('
            || format('SELECT * FROM %s WHERE Id = %s', NEW.Subclass, NEW.Id)
            || ') THEN 1 ELSE 0 END'
	INTO ExistsInSubclass;
	
	IF NOT ExistsInSubclass THEN
		RAISE EXCEPTION 'Row does not exist in subclass % with Id %', NEW.Subclass, NEW.Id;
	END IF;

	RETURN NEW;
END; $$ LANGUAGE plpgsql;
--	Check Deleted Subclass
--		Raises exception when `Place.Subclass` does not correspond with
--		a subclass table referencing the triggering row.
--		NOTE: Assumes `Place.Subclass` cannot be NULL (i.e. every row belongs to a subclass).
--	Trigger from:
--		Table: `Place`
--		AFTER FOR EACH ROW
--		DEFERRABLE (to allow subclass row creation afterwards)
--		INSERT OR UPDATE OF Subclass
CREATE OR REPLACE FUNCTION check_deleted_subclass() RETURNS trigger
AS $$ BEGIN
	IF OLD.Id IS NULL THEN
		RAISE EXCEPTION 'Incorrect trigger configuration';
	END IF;

	--if Subclass is NULL, parent table row has been dissociated
	--if query otherwise returns NULL, parent table row has been removed
	--if query returns a different Subclass,
	--	it will be tested through the trigger on Place.Subclass modification
	IF
        LOWER((SELECT Subclass FROM Place WHERE Id = OLD.Id)::TEXT) = TG_TABLE_NAME --TODO1 can enum manage to match TG_TABLE_NAME
	THEN
		RAISE EXCEPTION 'Subclass row deleted, but parent Place still has its type';
	END IF;

	RETURN NEW;
END; $$ LANGUAGE plpgsql;
--	Deny Subclass Update Id
--		Denies direct modifications in the `Id` column of a subclass table,
--		ensuring it is only modified by cascade from the parent class table.
CREATE OR REPLACE FUNCTION deny_subclass_update_id() RETURNS trigger
	IMMUTABLE
AS $$ BEGIN
	RAISE EXCEPTION 'Cannot modify `Id` values from subclass table. Modify parent table instead.';
END; $$ LANGUAGE plpgsql;

-- Trigger definitions

CREATE CONSTRAINT TRIGGER PlaceSubclasses
	AFTER INSERT OR UPDATE OF Subclass ON Place --on DELETE on Place, subclass cascade deletes anyways
	DEFERRABLE INITIALLY DEFERRED
	FOR EACH ROW EXECUTE FUNCTION check_place_subclass();

CREATE CONSTRAINT TRIGGER PlaceSubclassDel
	AFTER DELETE ON Area
	DEFERRABLE INITIALLY DEFERRED
	FOR EACH ROW EXECUTE FUNCTION check_deleted_subclass();

CREATE CONSTRAINT TRIGGER PlaceSubclassDel
	AFTER DELETE ON Station
	DEFERRABLE INITIALLY DEFERRED
	FOR EACH ROW EXECUTE FUNCTION check_deleted_subclass();

CREATE OR REPLACE TRIGGER SubclassAreaId
	BEFORE UPDATE OF Id ON Area
	FOR EACH STATEMENT
	WHEN (pg_trigger_depth() < 1) --allow if this is cascade from parent table
	EXECUTE FUNCTION deny_subclass_update_id();

CREATE OR REPLACE TRIGGER SubclassStationId
	BEFORE UPDATE OF Id ON Station
	FOR EACH STATEMENT
	WHEN (pg_trigger_depth() < 1) --allow if this is cascade from parent table
	EXECUTE FUNCTION deny_subclass_update_id();
