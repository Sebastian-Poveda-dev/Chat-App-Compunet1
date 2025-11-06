ChatApp HTTP Proxy (Express → Java TCP)

Overview
- Exposes simple HTTP endpoints that translate to the existing Java socket protocol.
- Keeps one persistent TCP socket per user (REGISTERed on the backend).
- Collects server responses in a per-user inbox that the client can poll.

Config
- `PORT` (default `3000`): HTTP port for the proxy.
- `BACKEND_HOST` (default `192.168.1.8`): Java server IP.
- `BACKEND_PORT` (default `5000`): Java server TCP port.

Install & Run
1) cd chatApp/proxy
2) npm install
3) BACKEND_HOST=192.168.1.8 BACKEND_PORT=5000 npm start

HTTP API
- POST `/register` { "username": "alice" }
  Opens a persistent TCP socket and sends `REGISTER:alice`.

- DELETE `/register/:username`
  Closes the user’s socket.

- POST `/messages/private` { "from":"alice", "to":"bob", "message":"hi" }
  Sends `PRIVATE_MSG:alice:bob:hi` over Alice's TCP socket.

- POST `/messages/group` { "from":"alice", "group":"friends", "message":"hey" }
  Sends `GROUP_MSG:alice:friends:hey`.

- POST `/groups` { "admin":"alice", "group":"friends", "members":["bob","carol"] }
  Sends `CREATE_GROUP:friends:alice:bob,carol`.

- POST `/groups/join` { "username":"bob", "group":"friends" }
  Sends `JOIN_GROUP:friends` using Bob’s session.

- GET `/inbox/:username`
  Returns and clears any messages received from the server for that user.

Notes
- Framing matches Java `DataOutputStream.writeUTF` (2-byte length + UTF-8 payload). For ASCII protocol messages, UTF-8 equals Java’s modified UTF-8.
- The proxy waits briefly after send to surface immediate errors. For real-time push, the browser should poll `/inbox/:username`.

