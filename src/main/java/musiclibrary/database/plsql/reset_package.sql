DROP PACKAGE MusicPackage;
/

DROP TYPE songs_table;
/

DROP TYPE table_entry;
/

CREATE OR REPLACE TYPE table_entry AS OBJECT (
    id VARCHAR2(100),
    song VARCHAR2(100),
    artists VARCHAR2(100),
    album VARCHAR2(100),
    genres VARCHAR2(300)
);
/

CREATE OR REPLACE TYPE songs_table AS TABLE OF table_entry;
/

CREATE OR REPLACE PACKAGE MusicPackage AS
    FUNCTION id_exists(p_spotify_id IN VARCHAR2, p_type IN VARCHAR2) RETURN BOOLEAN;
    FUNCTION get_songs RETURN songs_table;
    PROCEDURE add_song(p_spotify_id IN VARCHAR2, p_name IN VARCHAR2, p_album IN VARCHAR2);
    PROCEDURE add_artist(p_spotify_id IN VARCHAR2, p_name IN VARCHAR2, p_song_id IN VARCHAR2);
    PROCEDURE add_album(p_spotify_id IN VARCHAR2, p_name IN VARCHAR2, p_artist IN VARCHAR2);
    PROCEDURE add_genre(p_name IN VARCHAR2, p_artist IN VARCHAR2);
    PROCEDURE save_file(dir_name VARCHAR2, file_name VARCHAR2);
END MusicPackage;
/

CREATE OR REPLACE PACKAGE BODY MusicPackage IS
    FUNCTION id_exists(p_spotify_id IN VARCHAR2, p_type IN VARCHAR2) RETURN BOOLEAN AS
        v_dup NUMBER;
        type_exception EXCEPTION;
        PRAGMA EXCEPTION_INIT(type_exception, -20001);
    BEGIN
        CASE p_type
            WHEN 'song' THEN SELECT count(*) INTO v_dup FROM songs s WHERE p_spotify_id = s.spotify_id;
            WHEN 'artist' THEN SELECT count(*) INTO v_dup FROM artists a WHERE p_spotify_id = a.spotify_id;
            WHEN 'album' THEN SELECT count(*) INTO v_dup FROM albums a WHERE p_spotify_id = a.spotify_id;
            WHEN 'genre' THEN SELECT count(*) INTO v_dup FROM genres g WHERE p_spotify_id = g.name;
            ELSE RAISE type_exception;
        END CASE;
        
        RETURN (v_dup > 0);
        
        EXCEPTION
            WHEN type_exception THEN
                DBMS_OUTPUT.put_line ('Tipul nu e specificat cum trebuie'); 
                RETURN false;
    END id_exists;
    
    FUNCTION get_songs RETURN songs_table AS
        v_cursor_id INTEGER;
        v_ok INTEGER;
        
        v_song_id VARCHAR2(100);
        v_song VARCHAR2(100);
        v_artists VARCHAR2(100);
        v_album VARCHAR2(100);
        v_genres VARCHAR2(300);
        
        v_main_artist VARCHAR2(100);
        v_ret songs_table;
    BEGIN
        v_ret := songs_table();
        
        v_cursor_id := DBMS_SQL.OPEN_CURSOR;
        DBMS_SQL.PARSE(v_cursor_id, 'SELECT s.spotify_id, s.name, a.name FROM songs s JOIN albums a ON s.album = a.spotify_id', DBMS_SQL.NATIVE);
        DBMS_SQL.DEFINE_COLUMN(v_cursor_id, 1, v_song_id, 100);
        DBMS_SQL.DEFINE_COLUMN(v_cursor_id, 2, v_song, 100);
        DBMS_SQL.DEFINE_COLUMN(v_cursor_id, 3, v_album, 100);
        v_ok := DBMS_SQL.EXECUTE(v_cursor_id);
        
        LOOP
            IF DBMS_SQL.FETCH_ROWS(v_cursor_id)>0 THEN 
                DBMS_SQL.COLUMN_VALUE(v_cursor_id, 1, v_song_id); 
                DBMS_SQL.COLUMN_VALUE(v_cursor_id, 2, v_song); 
                DBMS_SQL.COLUMN_VALUE(v_cursor_id, 3, v_album);
                
                SELECT LISTAGG(ar.name, ', ') WITHIN GROUP (ORDER BY ar.name) INTO v_artists 
                FROM song_artists sa JOIN artists ar ON sa.artist = ar.spotify_id WHERE sa.song = v_song_id; 
                
                SELECT a.artist_id INTO v_main_artist 
                FROM songs s JOIN albums a ON a.spotify_id = s.album WHERE s.spotify_id = v_song_id;
                
                SELECT LISTAGG(g.name, ', ') WITHIN GROUP (ORDER BY g.name) INTO v_genres 
                FROM artist_genres ag JOIN genres g ON ag.genre = g.id 
                WHERE ag.artist = v_main_artist;
                
                v_ret.extend;
                v_ret(v_ret.count) := table_entry(v_song_id, v_song, v_artists, v_album, v_genres);
            ELSE 
                EXIT; 
            END IF; 
        END LOOP;
        DBMS_SQL.CLOSE_CURSOR(v_cursor_id);
        RETURN v_ret;
    END get_songs;

    PROCEDURE add_song(p_spotify_id IN VARCHAR2, p_name IN VARCHAR2, p_album IN VARCHAR2) AS
    BEGIN 
      IF MusicPackage.id_exists(p_spotify_id, 'song') = FALSE THEN
        INSERT INTO songs VALUES (p_spotify_id, p_name, p_album);
      END IF;
    END add_song;
    
    PROCEDURE add_artist(p_spotify_id IN VARCHAR2, p_name IN VARCHAR2, p_song_id IN VARCHAR2) AS
        v_dup NUMBER;
    BEGIN 
      IF MusicPackage.id_exists(p_spotify_id, 'artist') = FALSE THEN
        INSERT INTO artists VALUES (p_spotify_id, p_name);
      END IF;
      
      SELECT count(*) INTO v_dup FROM song_artists sa WHERE sa.song = p_song_id AND sa.artist = p_spotify_id;
      
      IF v_dup = 0 THEN
        INSERT INTO song_artists VALUES (0, p_spotify_id, p_song_id);
      END IF;
    END add_artist; 
    
    PROCEDURE add_album(p_spotify_id IN VARCHAR2, p_name IN VARCHAR2, p_artist IN VARCHAR2) AS
    BEGIN 
      IF MusicPackage.id_exists(p_spotify_id, 'album') = FALSE THEN
        INSERT INTO albums VALUES (p_spotify_id, p_name, p_artist);
      END IF;
    END add_album;
    
    PROCEDURE add_genre(p_name IN VARCHAR2, p_artist IN VARCHAR2) AS
        v_id NUMBER;
        v_dup NUMBER;
    BEGIN
        IF MusicPackage.id_exists(p_name, 'genre') = FALSE THEN
            INSERT INTO genres VALUES (0, p_name);
        END IF;
        
        SELECT id INTO v_id FROM genres g WHERE g.name = p_name; 
        SELECT count(*) INTO v_dup FROM artist_genres ag WHERE ag.artist = p_artist AND ag.genre = v_id;
      
        IF v_dup = 0 THEN
            INSERT INTO artist_genres VALUES (0, p_artist, v_id);
        END IF;
    END add_genre;
    
    PROCEDURE save_file(dir_name VARCHAR2, file_name VARCHAR2) AS
        v_fisier UTL_FILE.FILE_TYPE;
        v_cursor_id INTEGER;
        v_ok INTEGER;
        
        v_song VARCHAR2(100);
        v_artists VARCHAR2(100);
        v_album VARCHAR2(100);
        v_genres VARCHAR2(300);
    BEGIN
        EXECUTE IMMEDIATE 'create or replace directory MYDIR as ''' || dir_name || '''';

        v_fisier:=UTL_FILE.FOPEN('MYDIR', file_name, 'W');
      
        v_cursor_id := DBMS_SQL.OPEN_CURSOR;
        DBMS_SQL.PARSE(v_cursor_id, 'SELECT song, artists, album, genres FROM table(MusicPackage.get_songs)', DBMS_SQL.NATIVE);
        DBMS_SQL.DEFINE_COLUMN(v_cursor_id, 1, v_song, 100);
        DBMS_SQL.DEFINE_COLUMN(v_cursor_id, 2, v_artists, 100);
        DBMS_SQL.DEFINE_COLUMN(v_cursor_id, 3, v_album, 100);
        DBMS_SQL.DEFINE_COLUMN(v_cursor_id, 4, v_genres, 300);
        v_ok := DBMS_SQL.EXECUTE(v_cursor_id);
        
        LOOP
            IF DBMS_SQL.FETCH_ROWS(v_cursor_id)>0 THEN 
                DBMS_SQL.COLUMN_VALUE(v_cursor_id, 1, v_song); 
                DBMS_SQL.COLUMN_VALUE(v_cursor_id, 2, v_artists); 
                DBMS_SQL.COLUMN_VALUE(v_cursor_id, 3, v_album);
                DBMS_SQL.COLUMN_VALUE(v_cursor_id, 4, v_genres);
                
                UTL_FILE.PUTF(v_fisier,v_song || ', ' || v_artists || ', ' || v_album || ', ' || v_genres || '\n');
            ELSE 
                EXIT; 
            END IF; 
        END LOOP;
        DBMS_SQL.CLOSE_CURSOR(v_cursor_id);
        UTL_FILE.FCLOSE(v_fisier);
        
    END save_file;
END MusicPackage; 
/

