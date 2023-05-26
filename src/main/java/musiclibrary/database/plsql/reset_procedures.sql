CREATE OR REPLACE PROCEDURE add_song(p_spotify_id IN VARCHAR2, p_name IN VARCHAR2, p_album IN VARCHAR2)
AS
    v_dup NUMBER;
BEGIN 
  SELECT count(*) INTO v_dup FROM songs s WHERE p_spotify_id = s.spotify_id;
  
  IF v_dup = 0 THEN
    INSERT INTO songs VALUES (p_spotify_id, p_name, p_album);
  END IF;
END add_song; 
/

CREATE OR REPLACE PROCEDURE add_artist(p_spotify_id IN VARCHAR2, p_name IN VARCHAR2, p_song_id IN VARCHAR2)
AS
    v_dup NUMBER;
BEGIN 
  SELECT count(*) INTO v_dup FROM artists a WHERE p_spotify_id = a.spotify_id;
  
  IF v_dup = 0 THEN
    INSERT INTO artists VALUES (p_spotify_id, p_name);
  END IF;
  
  SELECT count(*) INTO v_dup FROM song_artists sa WHERE sa.song = p_song_id AND sa.artist = p_spotify_id;
  
  IF v_dup = 0 THEN
    INSERT INTO song_artists VALUES (0, p_spotify_id, p_song_id);
  END IF;

END add_artist; 
/

CREATE OR REPLACE PROCEDURE add_album(p_spotify_id IN VARCHAR2, p_name IN VARCHAR2, p_artist IN VARCHAR2)
AS
    v_dup NUMBER;
BEGIN 
  SELECT count(*) INTO v_dup FROM albums a WHERE p_spotify_id = a.spotify_id;
  
  IF v_dup = 0 THEN
    INSERT INTO albums VALUES (p_spotify_id, p_name, p_artist);
  END IF;
  
END add_album; 
/