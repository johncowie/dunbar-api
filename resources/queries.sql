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