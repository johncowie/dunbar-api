CREATE TABLE IF NOT EXISTS friends (
    id varchar(100) NOT NULL,
    user_id varchar(100) NOT NULL,
    first_name varchar(50) NOT NULL,
    last_name varchar(50) NOT NULL,
    PRIMARY KEY(id, user_id)
);