import express from 'express';
import { createBridgeFromEnv } from './tcpBridge.js';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const bridge = createBridgeFromEnv();

// Simple CORS without dependency
app.use((req, res, next) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET,POST,DELETE,OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  if (req.method === 'OPTIONS') return res.sendStatus(200);
  next();
});

app.use(express.json());

// Servir archivos estáticos del cliente web
app.use(express.static(path.join(__dirname, '../web-client')));

app.get('/health', (req, res) => {
  res.json({ ok: true, proxy: 'up' });
});

// Register a user: opens TCP socket and sends REGISTER
app.post('/register', async (req, res) => {
  const { username } = req.body || {};
  if (!username) return res.status(400).json({ error: 'username required' });
  try {
    const result = await bridge.register(username);
    res.json({ ok: true, ...result });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// Unregister: close TCP socket
app.delete('/register/:username', (req, res) => {
  const { username } = req.params;
  const result = bridge.unregister(username);
  res.json(result);
});

// Send private message
app.post('/messages/private', async (req, res) => {
  const { from, to, message } = req.body || {};
  if (!from || !to || typeof message !== 'string') {
    return res.status(400).json({ error: 'from, to, message required' });
  }
  try {
    const wire = `PRIVATE_MSG:${from}:${to}:${message}`;
    const result = await bridge.send(from, wire);
    res.json(result);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// Send group message
app.post('/messages/group', async (req, res) => {
  const { from, group, message } = req.body || {};
  if (!from || !group || typeof message !== 'string') {
    return res.status(400).json({ error: 'from, group, message required' });
  }
  try {
    const wire = `GROUP_MSG:${from}:${group}:${message}`;
    const result = await bridge.send(from, wire);
    res.json(result);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// Create group
app.post('/groups', async (req, res) => {
  const { admin, group, members } = req.body || {};
  if (!admin || !group) {
    return res.status(400).json({ error: 'admin and group required' });
  }
  const membersPart = Array.isArray(members) ? members.join(',') : '';
  try {
    const wire = `CREATE_GROUP:${group}:${admin}:${membersPart}`;
    const result = await bridge.send(admin, wire);
    res.json(result);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// Join group
app.post('/groups/join', async (req, res) => {
  const { username, group } = req.body || {};
  if (!username || !group) {
    return res.status(400).json({ error: 'username and group required' });
  }
  try {
    const wire = `JOIN_GROUP:${group}`; // server uses session username
    const result = await bridge.send(username, wire);
    res.json(result);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// Pull inbox (messages server sent to this user)
app.get('/inbox/:username', (req, res) => {
  const { username } = req.params;
  const messages = bridge.drainInbox(username);
  res.json({ messages });
});

const PORT = Number(process.env.PORT || '3000');
app.listen(PORT, () => {
  console.log(`HTTP proxy listening on http://localhost:${PORT}`);
});

