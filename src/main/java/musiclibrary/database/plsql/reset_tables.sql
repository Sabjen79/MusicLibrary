DROP TABLE artists;
/

DROP TABLE albums;
/

DROP TABLE songs;
/

DROP TABLE song_artists;
/

CREATE TABLE artists (
    spotify_id VARCHAR2(100) NOT NULL PRIMARY KEY,
    name VARCHAR2(100)
);
/

CREATE TABLE albums (
    spotify_id VARCHAR2(100) NOT NULL PRIMARY KEY,
    name VARCHAR2(100),
    artist_id VARCHAR2(100)
);
/

CREATE TABLE songs (
    spotify_id VARCHAR2(100) NOT NULL PRIMARY KEY,
    name VARCHAR2(100),
    album VARCHAR2(100)
);
/


CREATE TABLE song_artists (
    id NUMBER(4) NOT NULL PRIMARY KEY,
    artist VARCHAR2(100),
    song VARCHAR2(100)
);
/

CREATE OR REPLACE TRIGGER song_artists_trigger
BEFORE INSERT ON song_artists
FOR EACH ROW
BEGIN
    SELECT count(*)+1 INTO :new.id FROM song_artists;
END;
/