-- Run in the Repair database after nearby-schema.sql or ddl-auto=update.
-- Populate this temporary table using a reviewed, owner-provided Location export.
-- Never infer a request's original region from its author's CURRENT region.
CREATE TEMPORARY TABLE nearby_region_mapping (
    region_name VARCHAR(100) NOT NULL,
    region_code VARCHAR(20) NOT NULL
);

-- Insert the approved mappings here, in this same database session.
-- No example production mappings are supplied intentionally.

START TRANSACTION;
UPDATE posts p
JOIN (
    SELECT region_name, MIN(region_code) AS region_code
    FROM nearby_region_mapping
    WHERE TRIM(region_code) <> ''
    GROUP BY region_name
    HAVING COUNT(DISTINCT region_code) = 1
) m ON BINARY p.region_name = BINARY m.region_name
SET p.region_code = m.region_code
WHERE p.region_code IS NULL;

-- Review affected count and unresolved rows before issuing COMMIT or ROLLBACK.
SELECT ROW_COUNT() AS mapped_requests;
SELECT id, region_name FROM posts WHERE region_code IS NULL ORDER BY id;
