export class GameSocket {
    constructor() {
        this.handlers = new Map();
        this.pending = [];
        this.ws = null;
        this.closed = false;
        this.onOpen = null;
        this.heartbeat = null;
    }

    on(op, handler) {
        this.handlers.set(op, handler);
        return this;
    }

    connect() {
        const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
        const ticket = document.body ? document.body.dataset.wsTicket : '';
        const query = ticket ? `?ticket=${encodeURIComponent(ticket)}` : '';
        this.ws = new WebSocket(`${protocol}://${window.location.host}/ws/game${query}`);

        this.ws.addEventListener('open', () => {
            this.flush();
            this.startHeartbeat();
            if (this.onOpen) {
                this.onOpen();
            }
        });

        this.ws.addEventListener('message', event => {
            let message;
            try {
                message = JSON.parse(event.data);
            } catch (error) {
                console.error('invalid server message', event.data);
                return;
            }
            const handler = this.handlers.get(message.op);
            if (handler) {
                handler(message.data || {}, message);
            }
        });

        this.ws.addEventListener('close', () => {
            this.stopHeartbeat();
            if (!this.closed) {
                window.setTimeout(() => this.connect(), 1500);
            }
        });
    }

    send(op, data = {}) {
        const payload = JSON.stringify({ op, data });
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(payload);
        } else {
            this.pending.push(payload);
        }
    }

    close() {
        this.closed = true;
        this.stopHeartbeat();
        if (this.ws) {
            this.ws.close();
        }
    }

    flush() {
        const items = this.pending.splice(0);
        items.forEach(item => this.ws.send(item));
    }

    startHeartbeat() {
        this.stopHeartbeat();
        this.heartbeat = window.setInterval(() => this.send('PING'), 20000);
    }

    stopHeartbeat() {
        if (this.heartbeat) {
            window.clearInterval(this.heartbeat);
            this.heartbeat = null;
        }
    }
}
