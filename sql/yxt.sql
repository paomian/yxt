DROP TABLE IF EXISTS yxt_user;
CREATE TABLE yxt_user (
       id SERIAL,
       created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT (NOW() AT TIME ZONE 'UTC'),
       updated_at TIMESTAMP WITHOUT TIME ZONE,
       pic_name VARCHAR(100),
       person_id VARCHAR(100),
       email VARCHAR(100),
       session_token VARCHAR(100),
       nickname VARCHAR(50),
       gender VARCHAR(10),
       age SMALLINT,
       user_info JSONB,
       PRIMARY KEY (id)
);

CREATE OR REPLACE FUNCTION updated_at()
RETURNS TRIGGER AS $$
BEGIN
    IF row(NEW.*) IS DISTINCT FROM row(OLD.*) THEN
        NEW.updated_at = (NOW() AT TIME ZONE 'UTC');
        RETURN NEW;
    ELSE
        RETURN OLD;
    END IF;
END;
$$ LANGUAGE 'plpgsql';

CREATE TRIGGER u_updated_at BEFORE UPDATE
ON yxt_user FOR EACH ROW EXECUTE PROCEDURE updated_at();

CREATE INDEX yxt_user_person_id ON yxt_user (person_id);


DROP TABLE IF EXISTS yxt_post;
CREATE TABLE yxt_post (
    id SERIAL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    user_id INT,
    content VARCHAR(500),
    info JSONB,
    PRIMARY KEY (id)
);

CREATE TRIGGER p_updated_at BEFORE UPDATE
ON yxt_post FOR EACH ROW EXECUTE PROCEDURE updated_at();

DROP TABLE IF EXISTS yxt_comment;
CREATE TABLE yxt_comment (
    id SERIAL,
    post_id INT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    comment VARCHAR(500),
    info JSONB,
    PRIMARY KEY (id)
);

CREATE TRIGGER c_updated_at BEFORE UPDATE
ON yxt_comment FOR EACH ROW EXECUTE PROCEDURE updated_at();

DROP TABLE IF EXISTS yxt_hello;
CREATE TABLE yxt_hello (
   id SERIAL,
   user_id INT,
   created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT (NOW() AT TIME ZONE 'UTC'),
   hello VARCHAR(1000),
   info JSONB,
   PRIMARY KEY (id)
);
