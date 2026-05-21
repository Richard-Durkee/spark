-- Tests for SQL standard zip_explode table-valued function (SQL:2003 §7.6)
-- Supports multi-array zip semantics and correlated references via LATERAL

CREATE OR REPLACE TEMPORARY VIEW t1 AS SELECT * FROM VALUES
(1, array(10, 20, 30), array('a', 'b', 'c')),
(2, array(40, 50), array('d', 'e', 'f', 'g')),
(3, array(60), null)
AS t1(id, nums, letters);

-- Basic zip_explode of a single array literal
SELECT * FROM zip_explode(array(1, 2, 3)) AS t(x);

-- Unnest with multiple arrays (zip semantics, NULL padding for shorter arrays)
SELECT * FROM zip_explode(array(1, 2, 3), array('a', 'b')) AS t(x, y);

-- Unnest of a map (produces key and value columns)
SELECT * FROM zip_explode(map(1, 'one', 2, 'two', 3, 'three')) AS t(k, v);

-- Unnest with empty array
SELECT * FROM zip_explode(array()) AS t(x);

-- Unnest with NULL array
SELECT * FROM zip_explode(cast(null as array<int>)) AS t(x);

-- LATERAL zip_explode referencing table columns (Trino CROSS JOIN zip_explode equivalent)
SELECT t1.id, u.val
FROM t1, LATERAL zip_explode(t1.nums) AS u(val);

-- LATERAL zip_explode with multiple columns from table
SELECT t1.id, u.n, u.l
FROM t1, LATERAL zip_explode(t1.nums, t1.letters) AS u(n, l);

-- zip_explode_outer preserves rows when array is null
SELECT t1.id, u.val
FROM t1, LATERAL zip_explode_outer(t1.nums) AS u(val);

-- zip_explode in SELECT clause (generator syntax)
SELECT zip_explode(array(1, 2, 3));

-- Error: zip_explode with non-array argument
SELECT * FROM zip_explode(1) AS t(x);

-- Error: zip_explode with no arguments
SELECT * FROM zip_explode() AS t(x);
