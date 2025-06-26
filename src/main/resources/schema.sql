-- Ensure panelist_property_code table exists before Hibernate attempts to manage it further
CREATE TABLE IF NOT EXISTS panelist_property_code (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT,
    code VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    panelist_property_id BIGINT,
    PRIMARY KEY (id)
);

-- It's also good practice to ensure the panelist_property table exists if it's referenced by FKs
-- created by Hibernate later, or if panelist_property_code.panelist_property_id needs to be
-- immediately valid (though Hibernate with ddl-auto=update should handle panelist_property itself).
-- For now, focusing only on panelist_property_code as it's the one causing the immediate error.

-- Add the tool column to the survey table
ALTER TABLE survey ADD COLUMN tool VARCHAR(255) NOT NULL DEFAULT 'SURVEYTOGO';

-- Update existing rows to have a default value if necessary (optional, depending on policy)
-- UPDATE survey SET tool = 'SURVEYTOGO' WHERE tool IS NULL;
