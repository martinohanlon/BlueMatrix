Protocol
========

No acknowledgements are sent between the client and the server.

An event based model is used i.e. an event happens on either the client or the server and a message is sent for the other to react too.

Server sends to client
----------------------

| - | - |
| Command | Operator | Parameters |
| Check protocol version | 0 | protocol |
| Set matrix size | 1 | cols, rows |
| Set cell | 2 | col, row, rgba, border, visible |

Client sends to server
----------------------

| - | - |
| Command | Operator | Parameters |
| Check protocol version | 0 | protocol |
