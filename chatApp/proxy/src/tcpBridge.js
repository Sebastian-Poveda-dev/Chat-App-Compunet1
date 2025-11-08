import net from 'net';

// Minimal Java DataOutputStream.writeUTF / readUTF framing compatible for ASCII
// - Prefix each message with 2-byte big-endian length
// - Payload encoded in UTF-8 (ASCII messages match Java's modified UTF-8)

function encodeWriteUTF(str) {
  const payload = Buffer.from(str, 'utf8');
  if (payload.length > 65535) {
    throw new Error('Message too long for writeUTF framing');
  }
  const header = Buffer.allocUnsafe(2);
  header.writeUInt16BE(payload.length, 0);
  return Buffer.concat([header, payload]);
}

function decodeReadUTF(buffer) {
  // Returns { messages: string[], rest: Buffer }
  const messages = [];
  let offset = 0;
  while (buffer.length - offset >= 2) {
    const len = buffer.readUInt16BE(offset);
    if (buffer.length - offset - 2 < len) break; // incomplete frame
    const start = offset + 2;
    const end = start + len;
    messages.push(buffer.subarray(start, end).toString('utf8'));
    offset = end;
  }
  return { messages, rest: buffer.subarray(offset) };
}

export class TcpBridge {
  constructor({ host, port }) {
    this.host = host;
    this.port = port;
    // Map username -> { socket, inbox: string[], buffer: Buffer }
    this.sessions = new Map();
  }

  has(username) {
    return this.sessions.has(username);
  }

  async register(username) {
    if (this.sessions.has(username)) {
      throw new Error('User already registered in proxy');
    }
    const socket = new net.Socket();
    const inbox = [];
    let buffer = Buffer.alloc(0);

    socket.on('data', (chunk) => {
      buffer = Buffer.concat([buffer, chunk]);
      const { messages, rest } = decodeReadUTF(buffer);
      buffer = rest;
      for (const m of messages) inbox.push(m);
    });
    socket.on('error', (err) => {
      inbox.push(`ERROR: TCP socket error - ${err.message}`);
    });
    socket.on('close', () => {
      // Cleanup on close
      this.sessions.delete(username);
    });

    await new Promise((resolve, reject) => {
      socket.connect(this.port, this.host, resolve);
      socket.once('error', reject);
    });

    // Send REGISTER:username
    socket.write(encodeWriteUTF(`REGISTER:${username}`));

    // Store session
    this.sessions.set(username, { socket, inbox, buffer });

    // Heuristic: wait briefly to surface immediate errors (e.g., duplicate username)
    await new Promise((r) => setTimeout(r, 150));
    const hasDup = inbox.find((m) => m.startsWith('ERROR: Username already taken'));
    if (hasDup) {
      socket.destroy();
      this.sessions.delete(username);
      throw new Error('Username already taken on backend');
    }

    return { ok: true };
  }

  async send(username, message) {
    const session = this.sessions.get(username);
    if (!session) throw new Error('User not registered');
    const { socket, inbox } = session;
    // Track current inbox length to return new messages after send
    const startIdx = inbox.length;
    socket.write(encodeWriteUTF(message));
    // Wait a short time to collect any immediate responses
    await new Promise((r) => setTimeout(r, 150));
    return { ok: true, responses: inbox.slice(startIdx) };
  }

  drainInbox(username) {
    const session = this.sessions.get(username);
    if (!session) return [];
    const { inbox } = session;
    const out = inbox.splice(0, inbox.length);
    return out;
  }

  unregister(username) {
    const session = this.sessions.get(username);
    if (!session) return { ok: true };
    session.socket.destroy();
    this.sessions.delete(username);
    return { ok: true };
  }
}

export function createBridgeFromEnv() {
  const host = process.env.BACKEND_HOST || 'localhost';
  const port = Number(process.env.BACKEND_PORT || '5000');
  return new TcpBridge({ host, port });
}

export default TcpBridge;

