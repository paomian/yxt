DROP TABLE IF EXISTS yxt_user;
CREATE TABLE yxt_user (
	id serial,
	created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT (NOW() AT TIME ZONE 'UTC'),
	updated_at TIMESTAMP WITHOUT TIME ZONE,
	pic_path VARCHAR(100) UNIQUE,
	person_id VARCHAR(100) UNIQUE,
        email VARCHAR(100) UNIQUE,
        session_token VARCHAR(100) UNIQUE,
        user_info JSON,
	PRIMARY KEY (id)
);

CREATE OR REPLACE FUNCTION updated_at()
RETURNS TRIGGER AS $$
BEGIN
NEW.updated_at = (NOW() AT TIME ZONE 'UTC');
RETURN NEW;
END;
$$ LANGUAGE 'plpgsql';

CREATE TRIGGER u_updated_at BEFORE UPDATE
ON yxt_user FOR EACH ROW EXECUTE PROCEDURE updated_at();

CREATE INDEX yxt_user_person_id ON yxt_user (person_id);
