# 💬 Chat App Web – Cliente HTTP + Proxy + Servidor Java

Cliente web sencillo para una aplicación de chat, desarrollado con **HTML, CSS y JavaScript**, que se comunica con un **backend en Java** a través de un **proxy HTTP en Node.js (Express)**.

---

## 📌 Descripción general del proyecto

Este proyecto muestra una arquitectura cliente–servidor donde:

- El **cliente web** se ejecuta en el navegador.
- El **proxy HTTP (Node.js)** recibe las peticiones HTTP del cliente.
- El **servidor Java** se comunica por TCP con el proxy y procesa la lógica de chat.
- Comunicación cliente–servidor usando HTTP y sockets TCP.
- Integración entre **web estática**, **Node.js** y **Java**.
- Uso de un **proxy/intermediario** que traduce HTTP ↔ TCP.
- Manejo básico de estado y “tiempo real” mediante _polling_ periódico.

---

## 🧩 Funcionalidades principales

- ✅ **Registro / autenticación simple**
  - Login por nombre de usuario (sin contraseña).
- ✅ **Mensajes privados**
  - Enviar mensajes directos entre dos usuarios.
- ✅ **Mensajes de grupo**
  - Creación de grupos y envío de mensajes a todos sus miembros.
- ✅ **Historial de mensajes**
  - Consulta de mensajes recibidos para cada usuario.
- ✅ **Interfaz web**
  - Pantalla de login, área de mensajes, formularios para crear grupos y enviar mensajes.
- ✅ **Soporte multiusuario**
  - Uso simultáneo desde varias ventanas/pestañas (por ejemplo, modo normal + incógnito).

---

## 🏗️ Arquitectura

```text
Cliente Web (HTML/CSS/JS)
        ↓ HTTP (fetch)
Proxy HTTP (Node.js / Express)
        ↓ TCP
Servidor Java (Backend)
```

1. El **cliente web** hace peticiones HTTP al proxy (`fetch`).
2. El **proxy HTTP** traduce las peticiones HTTP a mensajes TCP.
3. El **servidor Java** procesa la lógica de chat.
4. El **proxy** devuelve las respuestas al cliente.

---


## 🔗 Endpoints del Proxy HTTP

> Base URL por defecto: `http://localhost:3000`

### Usuarios

**POST `/register`**  
Registra un nuevo usuario.

Body (JSON):

```json
{
  "username": "juan"
}
```

**DELETE `/register/:username`**  
Desregistra al usuario indicado.

---

### Mensajes

**POST `/messages/private`**  
Enviar mensaje privado usuario → usuario.

Body (JSON):

```json
{
  "from": "juan",
  "to": "pedro",
  "message": "Hola!"
}
```

**POST `/messages/group`**  
Enviar mensaje a un grupo.

Body (JSON):

```json
{
  "from": "juan",
  "group": "amigos",
  "message": "Hola grupo!"
}
```

---

### Grupos

**POST `/groups`**  
Crear un grupo de chat.

Body (JSON):

```json
{
  "group": "amigos",
  "members": ["juan", "pedro", "maria"]
}
```

**POST `/groups/join`**  
Unirse a un grupo existente.

Body (JSON):

```json
{
  "username": "pedro",
  "group": "amigos"
}
```

---

### Bandeja de entrada

**GET `/inbox/:username`**  
Devuelve los mensajes recibidos por el usuario:

- `username`: nombre del usuario del que se quiere obtener la bandeja.

---

## ⚙️ Requisitos previos

- **Java + Gradle** (para el servidor):
  - `gradlew.bat` en Windows o `./gradlew` en Linux/Mac.
- **Node.js y npm** (para el proxy HTTP).
- Navegador moderno (Chrome, Firefox, Edge, etc.).

---

## 🚀 Cómo ejecutar el proyecto

### 1️⃣ Levantar el servidor Java

**Windows (PowerShell / CMD):**

```powershell
cd chatApp
.\gradlew.bat :server:run
```

**Linux / Mac / Git Bash:**

```bash
cd chatApp
./gradlew :server:run
```

Salida esperada:

```text
Servidor iniciado en puerto 5000
```

> Deja esta terminal abierta.

---

### 2️⃣ Levantar el Proxy HTTP (Node.js)

Abrir **otra terminal**.

**Windows:**

```powershell
cd chatApp\proxy
npm install       # solo la primera vez
npm start
```

**Linux / Mac:**

```bash
cd chatApp/proxy
npm install       # solo la primera vez
npm start
```

Salida esperada:

```text
HTTP proxy listening on http://localhost:3000
```

> Esta terminal también debe permanecer abierta.

---

### 3️⃣ Abrir el cliente web

Abre tu navegador y entra a:

```text
http://localhost:3000
```

Deberías ver:

- Pantalla de login del chat.  
- Campo para nombre de usuario.  
- Botón **“Conectar”**.

---

## 🕹️ Uso desde la interfaz web

### 1. Conectarse

1. Ir a `http://localhost:3000`.
2. Escribir un **nombre de usuario**.
3. Pulsar **“Conectar”**.  
   Si el nombre está libre, entrarás al chat.

---

### 2. Crear un grupo

1. Completar el formulario **“Crear Grupo”**:
   - Nombre del grupo.
   - Lista de miembros separados por comas (ej: `juan,pedro,maria`).
2. Pulsar **“Crear Grupo”**.

---

### 3. Enviar mensajes

- Seleccionar si será:
  - **Mensaje privado** → se envía a un usuario.
  - **Mensaje de grupo** → se envía al grupo.
- Indicar destinatario (usuario o grupo).
- Escribir el texto.
- Pulsar **“Enviar”**.

---

### 4. Ver mensajes

- Los mensajes se cargan automáticamente cada cierto tiempo (_polling_).
- También puede existir un botón como **“Actualizar mensajes”** para forzar la recarga.

---

### 5. Desconectarse

- Usar el botón **“Desconectar”** para liberar el nombre de usuario y salir del chat.

---

## 🎮 Ejemplo de prueba con dos usuarios

1. Abrir una ventana normal del navegador:
   - Usuario: `juan`.
   - Conectarse.
2. Abrir una ventana en **modo incógnito**:
   - Usuario: `pedro`.
   - Conectarse.
3. Desde `juan`, crear un grupo `amigos` e incluir a `pedro`.
4. Enviar:
   - Un mensaje privado de `juan` a `pedro`.
   - Un mensaje al grupo `amigos`.

Si ambos usuarios ven sus mensajes, el flujo básico del sistema está funcionando correctamente.

---
