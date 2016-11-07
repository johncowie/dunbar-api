-- name: sql-create-friend<!
  INSERT INTO friends
  VALUES (
    :id,
    :user,
    :firstName,
    :lastName
  );

-- name: sql-retrieve-friend
  SELECT *
  FROM friends
  WHERE
    id = :id;

-- name: sql-delete-all-friends!
  DELETE FROM friends;

-- name: sql-create-user-token<!
  INSERT INTO user_tokens
  VALUES (
    :user,
    :token,
    :expiry
  );

-- name: sql-delete-user-token!
  DELETE FROM user_tokens
  WHERE
    user_id = :user;

-- name: sql-retrieve-user-token
  SELECT * FROM user_tokens
  WHERE
    user_id = :user;

-- name: sql-delete-all-tokens!
  DELETE FROM user_tokens;