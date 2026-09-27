# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/2.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Radio block tuned to a server-configured station (M3U or plain URL list, optional `#EXTINF` titles/durations)
- Server-side station clock with shuffle, broadcasting the current track and position to clients
- Client playback through a GTNH Voice addon source, from the nearest powered radio in range
- `/radio now|skip|reload`
