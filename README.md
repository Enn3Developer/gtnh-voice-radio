# GTNH Voice Radio

An addon for [GTNH Voice](https://github.com/Enn3Developer/gtnh-voice) that adds an in-world **Radio** block tuned to a
station the server configures. Everyone near a powered radio hears the same song at the same moment, positioned in 3D
through GTNH Voice's OpenAL context.

## How it works

- The **server** loads a playlist and runs the station clock (wall-clock based, it keeps playing even when nobody listens).
  It only tells clients *what* is playing and *how far in* - no audio passes through the server.
- **Clients** stream the current MP3 straight from its URL, seek to the station's position and play it from the nearest
  powered radio within range (one radio at a time, so nearby radios never echo).
- Requires GTNH Voice on both sides and a live voice session on the client.

## Server setup

In `config/gtnhvoiceradio.cfg`:

```
server {
    S:playlistUrl=https://example.org/music/playlist.m3u
    B:shuffle=true
}
```

The playlist is an `.m3u` (or a plain list of URLs, one per line). Relative entries resolve against the playlist URL.
Add `#EXTINF:<seconds>,<title>` lines to set titles and durations - entries without a duration are downloaded once at
startup to measure them. Tracks must be MP3 and reachable by every client.

`/radio now` shows the current song; ops get `/radio skip` and `/radio reload` (re-reads the config and playlist).

## Client config

`volume` (0-200 %) and `range` (4-64 blocks) under `client` in the same file.
