// Configuración de la API
const API_URL = 'http://localhost:3000';

// Estado de la aplicación
let currentUser = null;
let messages = [];
let messageCheckInterval = null;

// ============ FUNCIONES DE UI ============

function showScreen(screenId) {
    document.querySelectorAll('.screen').forEach(screen => {
        screen.classList.add('hidden');
    });
    document.getElementById(screenId).classList.remove('hidden');
}

function showNotification(message, type = 'info') {
    const notification = document.getElementById('notification');
    notification.textContent = message;
    notification.className = `notification ${type}`;
    notification.classList.remove('hidden');
    
    setTimeout(() => {
        notification.classList.add('hidden');
    }, 3000);
}

function displayMessages() {
    const container = document.getElementById('messages-container');
    
    if (messages.length === 0) {
        container.innerHTML = '<div class="empty-messages">No hay mensajes aún. ¡Envía tu primer mensaje!</div>';
        return;
    }
    
    container.innerHTML = messages.map(msg => {
        const isGroup = msg.includes('GROUP_MESSAGE_FROM:') || msg.includes('GROUP_AUDIO_FROM:') || msg.includes('GROUP_MESSAGE_SENT:');
        const isSent = msg.includes('MESSAGE_SENT_TO:') || msg.includes('GROUP_MESSAGE_SENT:');
        
        let messageClass = 'message';
        if (isSent) {
            messageClass += ' sent';
            if (isGroup) messageClass += ' group';
        } else if (isGroup) {
            messageClass += ' group';
        }
        
        let sender = '';
        let content = '';
        
        if (msg.startsWith('MESSAGE_FROM:')) {
            // Formato: MESSAGE_FROM: sender: content
            const parts = msg.substring(13).split(':');
            sender = parts[0].trim();
            content = parts.slice(1).join(':').trim();
        } else if (msg.startsWith('MESSAGE_SENT_TO:')) {
            // Formato: MESSAGE_SENT_TO: recipient: content
            const parts = msg.substring(16).split(':');
            sender = `Tú → ${parts[0].trim()}`;
            content = parts.slice(1).join(':').trim();
        } else if (msg.startsWith('GROUP_MESSAGE_FROM:')) {
            // Formato: GROUP_MESSAGE_FROM: sender@group: content
            const parts = msg.substring(19).split(':');
            sender = parts[0].trim();
            content = parts.slice(1).join(':').trim();
        } else if (msg.startsWith('GROUP_MESSAGE_SENT:')) {
            // Formato: GROUP_MESSAGE_SENT: sender@group: content
            const parts = msg.substring(19).split(':');
            sender = `Tú → ${parts[0].trim()}`;
            content = parts.slice(1).join(':').trim();
        } else if (msg.startsWith('AUDIO_FROM:')) {
            // Formato: AUDIO_FROM: sender: filename: size
            const parts = msg.substring(11).split(':');
            sender = parts[0].trim();
            content = `🔊 Audio recibido: ${parts[1].trim()}`;
        } else if (msg.startsWith('GROUP_AUDIO_FROM:')) {
            // Formato: GROUP_AUDIO_FROM: sender@group: filename: size
            const parts = msg.substring(17).split(':');
            sender = parts[0].trim();
            content = `🔊 Audio grupal recibido: ${parts[1].trim()}`;
        } else if (msg.startsWith('GROUP_CREATED:')) {
            sender = 'Sistema';
            content = `✓ Grupo creado: ${msg.substring(14).trim()}`;
        } else if (msg.startsWith('ADDED_TO_GROUP:')) {
            const parts = msg.substring(15).split(' by ');
            sender = 'Sistema';
            content = `✓ Añadido al grupo: ${parts[0].trim()}`;
        } else if (msg.startsWith('JOINED_GROUP:')) {
            sender = 'Sistema';
            content = `✓ Te uniste al grupo: ${msg.substring(13).trim()}`;
        } else if (msg.startsWith('ERROR:')) {
            sender = 'Error';
            content = msg.substring(6).trim();
        } else {
            sender = 'Sistema';
            content = msg;
        }
        
        const time = new Date().toLocaleTimeString();
        
        return `
            <div class="${messageClass}">
                <div class="message-header">
                    <span class="message-sender">${sender}</span>
                    <span class="message-time">${time}</span>
                </div>
                <div class="message-content">${content}</div>
            </div>
        `;
    }).join('');
    
    // Scroll al final
    container.scrollTop = container.scrollHeight;
}

// ============ FUNCIONES DE API ============

async function apiCall(endpoint, method = 'GET', body = null) {
    try {
        const options = {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            }
        };
        
        if (body) {
            options.body = JSON.stringify(body);
        }
        
        const response = await fetch(`${API_URL}${endpoint}`, options);
        const data = await response.json();
        
        if (!response.ok) {
            throw new Error(data.error || 'Error en la petición');
        }
        
        return data;
    } catch (error) {
        console.error('Error en API:', error);
        throw error;
    }
}

// ============ FUNCIONES PRINCIPALES ============

async function login() {
    const username = document.getElementById('username-input').value.trim();
    const errorDiv = document.getElementById('login-error');
    
    errorDiv.textContent = '';
    
    if (!username) {
        errorDiv.textContent = 'Por favor ingresa un nombre de usuario';
        return;
    }
    
    try {
        // Registrar usuario en el servidor
        await apiCall('/register', 'POST', { username: username });
        
        currentUser = username;
        document.getElementById('current-user').textContent = username;
        
        // Cambiar a pantalla de chat
        showScreen('chat-screen');
        showNotification(`Bienvenido, ${username}!`, 'success');
        
        // Iniciar polling de mensajes
        startMessagePolling();
        
    } catch (error) {
        errorDiv.textContent = error.message;
    }
}

async function logout() {
    if (!currentUser) return;
    
    try {
        // Desregistrar usuario
        await apiCall(`/register/${currentUser}`, 'DELETE');
        
        // Detener polling
        stopMessagePolling();
        
        // Limpiar estado
        currentUser = null;
        messages = [];
        document.getElementById('username-input').value = '';
        
        // Volver a pantalla de login
        showScreen('login-screen');
        showNotification('Desconectado exitosamente', 'info');
        
    } catch (error) {
        console.error('Error al desconectar:', error);
    }
}

async function createGroup() {
    if (!currentUser) return;
    
    const groupName = document.getElementById('group-name').value.trim();
    const membersInput = document.getElementById('group-members').value.trim();
    
    if (!groupName) {
        showNotification('Ingresa un nombre para el grupo', 'error');
        return;
    }
    
    try {
        // Procesar miembros
        const members = membersInput ? membersInput.split(',').map(m => m.trim()).filter(m => m) : [];
        
        await apiCall('/groups', 'POST', {
            admin: currentUser,
            group: groupName,
            members: members
        });
        
        showNotification(`Grupo "${groupName}" creado exitosamente`, 'success');
        
        // Limpiar campos
        document.getElementById('group-name').value = '';
        document.getElementById('group-members').value = '';
        
        // Recargar mensajes para ver confirmación
        setTimeout(() => loadMessages(), 500);
        
    } catch (error) {
        showNotification(error.message, 'error');
    }
}

async function sendMessage() {
    if (!currentUser) return;
    
    const messageType = document.getElementById('message-type').value;
    const recipient = document.getElementById('message-recipient').value.trim();
    const content = document.getElementById('message-content').value.trim();
    
    if (!recipient || !content) {
        showNotification('Completa todos los campos', 'error');
        return;
    }
    
    try {
        if (messageType === 'private') {
            await apiCall('/messages/private', 'POST', {
                from: currentUser,
                to: recipient,
                message: content
            });
            
            // Mostrar el mensaje enviado inmediatamente
            messages.push(`MESSAGE_SENT_TO: ${recipient}: ${content}`);
            displayMessages();
            
            showNotification(`Mensaje enviado a ${recipient}`, 'success');
        } else {
            await apiCall('/messages/group', 'POST', {
                from: currentUser,
                group: recipient,
                message: content
            });
            
            // Mostrar el mensaje grupal enviado inmediatamente
            messages.push(`GROUP_MESSAGE_SENT: ${currentUser}@${recipient}: ${content}`);
            displayMessages();
            
            showNotification(`Mensaje enviado al grupo ${recipient}`, 'success');
        }
        
        // Limpiar campo de mensaje
        document.getElementById('message-content').value = '';
        
        // Recargar mensajes para obtener respuestas
        setTimeout(() => loadMessages(), 500);
        
    } catch (error) {
        showNotification(error.message, 'error');
    }
}

async function loadMessages() {
    if (!currentUser) return;
    
    try {
        const response = await apiCall(`/inbox/${currentUser}`);
        
        // Agregar nuevos mensajes al historial
        if (response.messages && response.messages.length > 0) {
            messages.push(...response.messages);
            displayMessages();
        }
        
    } catch (error) {
        console.error('Error al cargar mensajes:', error);
    }
}

function startMessagePolling() {
    // Cargar mensajes cada 2 segundos
    messageCheckInterval = setInterval(() => {
        loadMessages();
    }, 2000);
}

function stopMessagePolling() {
    if (messageCheckInterval) {
        clearInterval(messageCheckInterval);
        messageCheckInterval = null;
    }
}

// ============ EVENTOS ============

// Permitir login con Enter
document.getElementById('username-input')?.addEventListener('keypress', function(e) {
    if (e.key === 'Enter') {
        login();
    }
});

// Permitir enviar mensaje con Enter (Ctrl+Enter para nueva línea)
document.getElementById('message-content')?.addEventListener('keypress', function(e) {
    if (e.key === 'Enter' && !e.ctrlKey) {
        e.preventDefault();
        sendMessage();
    }
});

// Limpiar cuando se cierra la ventana
window.addEventListener('beforeunload', () => {
    if (currentUser) {
        logout();
    }
});

// ============ INICIALIZACIÓN ============

console.log('Cliente web de chat iniciado');
console.log('Conectando al proxy en:', API_URL);
