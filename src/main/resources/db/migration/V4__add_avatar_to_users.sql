-- V4: Add avatar to users
-- Stores the S3 object key of the user's avatar (never the URL - presigned URLs
-- expire and are generated at read time). Nullable: avatar is optional and
-- existing users have none.

ALTER TABLE users ADD COLUMN avatar_key VARCHAR(255);
