Protocol
========

No acknowledgements are sent between the client and the server.

An event based model is used i.e. an event happens on either the client or the server and a message is sent for the other to react too.

Server sends to client
----------------------

| - | - |
| Command | Operator | Parameters |

| Check protocol version | 0 | protocol |

| Set matrix | 1 | cols, rows, argb, border, visible |

| Set matrix colour | 2 | argb |
| Set matrix border | 3 | border |
| Set matrix visible | 4 | visible |

| Set cell | 5 | col, row, argb, border, visible |
| Set cell colour | 5 | col, row, argb |
| Set cell border | 6 | col, row, border |
| Set cell visible | 7 | col, row, visible |

Client sends to server
----------------------

| - | - |
| Command | Operator | Parameters |
| Check protocol version | 0 | protocol |
