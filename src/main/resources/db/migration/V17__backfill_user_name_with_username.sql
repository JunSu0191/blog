UPDATE users
SET name = username
WHERE name IS NULL
   OR TRIM(name) = '';
