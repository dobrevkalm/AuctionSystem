# Simple Auction System

Makes use of TCP. Allows multiple users to place bids. Backup server runs in the background and will be automatically connected to all current bidders in case the main server fails.

## Bidder
Can place a bid by sending "bid" to the server or check for results with "reuslt".

## Server
Handles bidder connections and requests. Keeps track of the current highest bidder. Sends backup information to the backup server.

## BackupServer
Keeps backup information. Only stores the current highest bidder.