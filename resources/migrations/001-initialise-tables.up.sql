CREATE TABLE friends (
    id varchar(100) NOT NULL,
    user_id varchar(100) NOT NULL,
    first_name varchar(50) NOT NULL,
    last_name varchar(50) NOT NULL,
    PRIMARY KEY(id, user_id)
);

CREATE TABLE user_tokens (
    user_id varchar(100) NOT NULL,
    token varchar(100) NOT NULL,
    expiry TIMESTAMP WITH TIME ZONE NOT NULL
)