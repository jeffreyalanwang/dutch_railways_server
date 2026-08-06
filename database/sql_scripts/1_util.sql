--TODO3 delete this
-- CREATE OR REPLACE FUNCTION funcname(argname [INT | table_name.column_name%TYPE]) RETURNS rettype
-- 	[LANGUAGE lang_name]
--	CALLED ON NULL INPUT | RETURNS NULL ON NULL INPUT
-- 	IMMUTABLE | STABLE
-- 	AS 'def' | BEGIN ATOMIC
-- 		statement
-- 	END;
--TODO1 PREPARE statements https://www.postgresql.org/docs/current/sql-prepare.html
--TODO3 make all of these names start with _

--	Place With Geometry
--		Contains an row for every `Place` entity that has a Geometry.
CREATE OR REPLACE VIEW PlaceWithGeom(Id, Geom) AS (
	SELECT Id, Geom::Geometry FROM Area
) UNION (
	SELECT Id, Geom::Geometry FROM Station
);

--	Test Area Geometry
--		Check if a hypothetical new area geometry would conflict with existing ones.
--	Args:
--		test_geom:	The geometry to test.
--		test_id:	`Id` of a row in `Area` to ignore (e.g. because it is about to be removed), optional.
--	Returns:
--		NULL if new geometry is allowed.
--		Otherwise, `AreaGeomReason` value corresponding to conflict reason.
CREATE TYPE AreaGeomReason AS ENUM ('DUPLICATE', 'CROSSES');
CREATE OR REPLACE FUNCTION test_area_geometry(
	test_geom	Area.Geom%TYPE, 
	test_id		Area.Id%TYPE	DEFAULT NULL
)	RETURNS		AreaGeomReason
	CALLED ON NULL INPUT
AS $$ BEGIN
	IF test_geom IS NULL THEN
		RAISE EXCEPTION 'No argument provided for test_geom';
	END IF;

	IF EXISTS (
		SELECT * FROM Area
		WHERE ST_equals(Area.Geom, test_geom)
		  AND Area.Id <> test_id
	) THEN
		RETURN 'DUPLICATE';
	END IF;

	IF EXISTS (
        SELECT * FROM Area
        WHERE ST_overlaps(Area.Geom, test_geom) --false if one fully covers the other; false if overlap is along only line(s) and point(s)
	      AND Area.Id <> test_id
	) THEN
		RETURN 'CROSSES';
	END IF;

	RETURN NULL;
END; $$ LANGUAGE plpgsql;

--	Find Area's Direct Children
--		NULL argument represents the parent set of top-level `Place`s.
--		A Geometry argument must not presently exist in `Area`.
--	Args:
--		a:			Area of which to find direct children. Geometry if the area does not yet exist in `Area`.
--		within_:	Direct parent of `a`. Required when `a` is a Geometry.
--	Returns:
--		`Id` of `Place`s that are the direct child of an `Area`.
CREATE OR REPLACE FUNCTION _find_area_children_direct(
	a		Area.Id%TYPE
)	RETURNS	TABLE(Id Place.Id%TYPE)
	STABLE CALLED ON NULL INPUT
AS $$
	SELECT Id FROM Place
	WHERE LocatedIn IS NOT DISTINCT FROM a
$$ LANGUAGE sql;
CREATE OR REPLACE FUNCTION _find_area_children_direct(
	a		Area.Geom%TYPE,
	within_	Area.Id%TYPE
)	RETURNS	TABLE(Id Place.Id%TYPE)
	STABLE CALLED ON NULL INPUT
AS $$
	SELECT Id
	FROM _find_area_children_direct(within_) NATURAL JOIN PlaceWithGeom
	WHERE ST_contains(a, Geom) --polygon (parent border-inclusive); point (..exclusive)
$$ LANGUAGE sql;

--	Place Hierarchy
--		Contains an row for every existing combination
--		  of ancestor and descendant in `Place`.
--		Does not include NULL for top-level `Area`s.
--TODO3 rename parent/child to asc/desc
CREATE OR REPLACE RECURSIVE VIEW PlaceHierarchy(Parent, Child) AS (
	SELECT LocatedIn AS Parent, Id AS Child 
	FROM Place
	WHERE LocatedIn IS NOT NULL
) UNION (
	--each row of this subquery takes one anc-desc relationship
	--and extends it by one (i.e. `Child` is one generation lower)
	SELECT	P.Parent AS Parent	, C.Id AS Child
	FROM	PlaceHierarchy P	, Place C
	WHERE	P.Child				= C.LocatedIn
);

--	Find All Children Of Area
--		Returns `Id` of `Place`s that are within an `Area`.
--		On NULL, finds all `Place`s.
CREATE OR REPLACE FUNCTION find_area_children_recursive(
	aid		Area.Id%TYPE
)	RETURNS	TABLE(Id Place.Id%TYPE)
	STABLE CALLED ON NULL INPUT
AS $$ BEGIN
	IF aid IS NULL THEN
		RETURN QUERY (SELECT Id FROM Place);
	END IF;
	
	RETURN QUERY
		WITH AidDescendants AS (
			SELECT Child as Id
			FROM PlaceHierarchy
			WHERE Parent = aid
		)
		SELECT Id
		FROM Place
		NATURAL JOIN AidDescendants;
END; $$ LANGUAGE plpgsql;

--	Find All Parents Of Place
--		Returns `Id` of `Area`s within which a `Place` is located.
--		When providing Geometry argument, `Area.Geom` should have an index supporting Geometry.
CREATE OR REPLACE FUNCTION find_place_parent_recursive(
	pid		Place.Id%TYPE
)	RETURNS	Table(Id Area.Id%TYPE)
	STABLE RETURNS NULL ON NULL INPUT
RETURN (
	SELECT Parent AS Id
	FROM PlaceHierarchy
	WHERE Child = pid
);
CREATE OR REPLACE FUNCTION find_place_parent_recursive(
	g		Geometry
)	RETURNS	Table(Id Area.Id%TYPE)
	STABLE RETURNS NULL ON NULL INPUT AS
$$
    SELECT Id FROM Area
    WHERE ST_Contains(Geom, g);
$$ LANGUAGE sql;
-- RETURN (
-- 	SELECT Id FROM Area
-- 	WHERE ST_Contains(Geom, g)
-- );

--	Find Direct Parent Area
--		`g` must be a valid area geometry (see `test_area_geometry()`).
--		Not the fastest way to retrieve an entire hierarchy of parent `Area`s.
--		May return null.
--		`Area.Geom` should have an index supporting Geometry.
CREATE OR REPLACE FUNCTION find_parent_area_direct(
	g		Geometry,
    exclude Area.Id%TYPE DEFAULT NULL
)	RETURNS	Area.Id%TYPE --TODO1 make sure return types are consistent
	STABLE CALLED ON NULL INPUT AS
$$
	WITH Ancestors AS (
		SELECT Id, LocatedIn
		FROM find_place_parent_recursive(g)
		NATURAL JOIN Place
		WHERE Id IS DISTINCT FROM exclude
	)
	--TODO1 try this option | returns immediately on smallest child | no hint about known parent | finds direct children for every Ancestor
	-- SELECT Id
	-- FROM Ancestors ret
	-- WHERE NOT EXISTS (
	-- 	SELECT * 
	-- 	FROM find_area_children_direct(ret.id) AS child
	-- 	WHERE child.Subclass = 'Area'
	-- 	AND child.Id NOT IN (SELECT Id FROM Ancestors)
	-- )
	-- LIMIT 1 --TODOX idk cus then we can't be annoyingly strict about scalar return
	--option | must calculate every Ancestor
	SELECT Id
	FROM Ancestors AS ret
	WHERE NOT EXISTS (
		SELECT * 
		FROM (SELECT * FROM Ancestors AS x WHERE x.Id <> ret.Id) AS other
		WHERE other.LocatedIn = ret.Id
	);
$$ LANGUAGE sql;

--TODO2 Finish set of combinations (recursive? - parent/child - of geom/entity?)
--TODO4 Look into possibility of index based on our manual hierarchy?

--	Give Children To Parent
--		Finds `Place`s listed as a direct child of `aid`,
--		  and makes them a direct child of `a`'s parent instead.
--		Useful before `aid` is removed.
CREATE OR REPLACE FUNCTION _give_children_to_parent(
	aid		Area.Id%TYPE
)	RETURNS	void
	CALLED ON NULL INPUT
BEGIN ATOMIC
	WITH Parent AS ( --TODO2.5 change if we develop a view for Area
		SELECT LocatedIn as Id
		FROM Place 
		WHERE Id = aid
	)
	UPDATE Place --confirms OLD.Id corresponds to `Area`
	SET LocatedIn = (SELECT Id FROM Parent)
	FROM _find_area_children_direct(aid) AS Children
	WHERE Place.Id = Children.Id;
END;