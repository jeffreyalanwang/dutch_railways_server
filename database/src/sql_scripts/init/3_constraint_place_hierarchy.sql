--These triggers occur on INSERT, UPDATE ON LocatedIN OR Geom, or DELETE.
--They modify `LocatedIn` or `Geom`.

--	Deny Update `Place.LocatedIn`
--		If the row belongs to a subclass with geometry,
--		  the function raises an exception
--		  (to deny direct modifications to the `LocatedIn` column),
--		  ensuring it is only modified by trigger from subclass geometries.
--		Note: Assumes all children have automatic geometry attribute.
--	Trigger from:
--		Table: `Place`
--		BEFORE OR AFTER (rejects anyways)
--		INSERT OR UPDATE OF Geom
--		Performed by user (or by a trigger from any other file, but this cannot be checked).
CREATE OR REPLACE FUNCTION deny_place_update_locatedin() RETURNS trigger
AS $$ BEGIN
	IF OLD.Subclass IS NOT NULL THEN
		RAISE EXCEPTION 'Cannot modify `LocatedIn` values if child has Geometry. Modify/set child table instead.';
	END IF;

	RETURN NEW;
END; $$ LANGUAGE plpgsql;

--	Check Area
--		Ensures no `Area`s duplicate or overlap each other (borders OK).
--		Raises exception if they do.
--		`Area.Geom` should have an index supporting Geometry overlap.
--	Trigger from:
--		Table: `Area`
--		AFTER
--		DEFERRABLE (to allow mutually exclusive areas to be moved together)
--		INSERT OR UPDATE OF Geom
--		FOR EACH ROW
CREATE OR REPLACE FUNCTION check_area_geometry() RETURNS trigger
AS $$ DECLARE
	Confl AreaGeomReason := test_area_geometry(NEW.Geom, NEW.Id);
BEGIN
	IF NEW.Id IS NULL OR NEW.Geom IS NULL THEN
		RAISE EXCEPTION 'Incorrect trigger configuration';
	END IF;

	IF Confl IS NULL THEN
		RETURN NULL; --in an AFTER trigger, any return value allows continue
	ELSIF Confl = 'DUPLICATE' THEN
		RAISE EXCEPTION 'Cannot add an area if it duplicates another';
	ELSIF Confl = 'CROSSES' THEN
		RAISE EXCEPTION 'Cannot add an area if it partially overlaps another';
	ELSE
		RAISE EXCEPTION 'Unrecognized enum value %', Confl;
	END IF;

	RETURN NEW;
END; $$ LANGUAGE plpgsql;

--	Update Area Geometry
--		Handles the creation or update of an `Area`
--		by moving it into a parent (or NULL if none exists),
--		moving any applicable children into it,
--		and moving its old children to their new parents (or NULL).
--	Trigger from:
--		Table: `Area`
--		BEFORE or AFTER (assuming, in any case, that Area's `Id` created already in `Place`)
--		INSERT OR UPDATE OF Geom
--		FOR EACH ROW
CREATE OR REPLACE FUNCTION update_area_hierarchy() RETURNS trigger
AS $$ DECLARE
	NewParent Area.Id%TYPE := find_parent_area_direct(NEW.Geom, NEW.Id); --TODO2 move this into function body like a normal person
BEGIN
	IF NEW.Id IS NULL OR NEW.Geom IS NULL THEN
		RAISE EXCEPTION 'Incorrect trigger configuration';
	END IF;

	--1. move old children out, if this is an UPDATE
	IF OLD IS NOT NULL THEN
		PERFORM _give_children_to_parent(OLD.Id);
	END IF;

	--2. set parent of this area
	UPDATE Place SET LocatedIn = NewParent
	WHERE Id = NEW.Id;

	--3. find new children, move them in
	WITH Children(Id) AS (
	    SELECT *
	    FROM _find_area_children_direct(a=>NEW.Geom, within_=>NewParent)
    )
	UPDATE Place
	SET LocatedIn = NEW.Id
	FROM Children
	WHERE Place.Id <> NEW.Id
	  AND Place.Id = Children.Id;

	RETURN NEW;
END; $$ LANGUAGE plpgsql;

--	Delete Area Geometry
--		Handles the deletion of an `Area` by moving `LocatedIn` for its child `Place`s to this `Area`'s parent.
--		If this `Area` does not have a parent, its children are similarly given NULL values.
--	Trigger from:
--		Table: `Area`
--		BEFORE or AFTER
--		DELETE
--		FOR EACH ROW
CREATE OR REPLACE FUNCTION delete_area_geometry() RETURNS trigger
AS $$ BEGIN
	IF OLD.Id IS NULL THEN
		RAISE EXCEPTION 'Incorrect trigger configuration';
	END IF;
	IF NEW IS NOT NULL THEN
		RAISE EXCEPTION 'Incorrect trigger configuration: not triggered by a DELETE';
	END IF;

	PERFORM _give_children_to_parent(OLD.Id);
	
	RETURN OLD; --allows DELETE to continue
END; $$ LANGUAGE plpgsql;

--	Update Geometry Hierarchy
--		Updates a tuple's value in `Place.LocatedIn` to the smallest (most directly) enclosing `Area`.
--		To be used when a subclass table receives information on its location.
--		Uses data from `Area.Geom`. Thus, multiple `Area`s must not include the modified tuple's location.
--	Trigger from:
--		Subtable of `Place`: columns `Id` (Foreign Key of `Place`) and `Geom` (`Geometry`).
--		BEFORE or AFTER
--		INSERT OR UPDATE OF Geom
--		FOR EACH ROW
CREATE OR REPLACE FUNCTION update_geom_hierarchy() RETURNS trigger
AS $$ BEGIN
	--confirms we have INSERT or UPDATE
	--confirms we have columns called Id, Geom
	--confirms we have FOR EACH ROW
	IF NEW.Id IS NULL OR NEW.Geom IS NULL THEN
		RAISE EXCEPTION 'Incorrect trigger configuration';
	END IF;
	
	--does Geom have parent area? set/clear it.
	--confirms Geom column has type Geometry
	UPDATE Place
	SET LocatedIn = find_parent_area_direct(NEW.Geom) --may set NULL
	WHERE Id = NEW.Id;
	
	RETURN NEW;
END; $$ LANGUAGE plpgsql;

-- Trigger definitions

CREATE OR REPLACE TRIGGER PlaceLocatedInUpdate
	BEFORE UPDATE OF LocatedIn ON Place
	FOR EACH STATEMENT 
	WHEN (pg_trigger_depth() < 1) --allow update triggered by Area or Child geometry update
	EXECUTE FUNCTION deny_place_update_locatedin();

CREATE CONSTRAINT TRIGGER AreaNoOverlap
	AFTER INSERT OR UPDATE OF Geom ON Area
	DEFERRABLE INITIALLY DEFERRED
	FOR EACH ROW EXECUTE FUNCTION check_area_geometry();
CREATE OR REPLACE TRIGGER SubclassAreaLocatedIn
	AFTER INSERT OR UPDATE OF Geom ON Area
	FOR EACH ROW EXECUTE FUNCTION update_area_hierarchy();

CREATE OR REPLACE TRIGGER SubclassAreaDel
	AFTER DELETE ON Area
	FOR EACH ROW EXECUTE FUNCTION delete_area_geometry();

CREATE OR REPLACE TRIGGER SubclassStationLocatedIn
	AFTER INSERT OR UPDATE OF Geom ON Station
	FOR EACH ROW EXECUTE FUNCTION update_geom_hierarchy();
