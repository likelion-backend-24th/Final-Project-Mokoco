SET @content_format_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'posts'
      AND column_name = 'content_format'
);

SET @content_format_sql = IF(
    @content_format_exists = 0,
    'ALTER TABLE posts ADD COLUMN content_format VARCHAR(20) NOT NULL DEFAULT ''PLAIN_TEXT''',
    'SELECT 1'
);

PREPARE content_format_statement FROM @content_format_sql;
EXECUTE content_format_statement;
DEALLOCATE PREPARE content_format_statement;
