import http from 'k6/http';
import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { randomString } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
    vus: 20,
    duration: '20s',
    thresholds: {
        checks: ['rate>0.95'],
    },
};

const BASE = __ENV.BASE_URL || 'http://127.0.0.1:8080';

export default function () {
    const username = 'lt_' + randomString(10);
    const password = 'gomoku123';

    const page = http.get(`${BASE}/login`);
    const csrf = page.body.match(/name="_csrf" content="([^"]+)"/)[1];

    http.post(`${BASE}/register`, {
        username,
        nickname: username,
        password,
        _csrf: csrf,
    });

    const loginPage = http.get(`${BASE}/login`);
    const loginCsrf = loginPage.body.match(/name="_csrf" content="([^"]+)"/)[1];
    http.post(`${BASE}/login`, {
        username,
        password,
        _csrf: loginCsrf,
    });

    const lobby = http.get(`${BASE}/lobby`);
    const ticketMatch = lobby.body.match(/data-ws-ticket="([^"]+)"/);
    if (!ticketMatch) {
        return;
    }

    const wsUrl = BASE.replace(/^http/, 'ws') + '/ws/game?ticket=' + ticketMatch[1];
    const response = ws.connect(wsUrl, {}, socket => {
        socket.on('open', () => {
            socket.send(JSON.stringify({ op: 'PING', data: {} }));
        });
        socket.on('message', message => {
            const parsed = JSON.parse(message);
            if (parsed.op === 'PONG') {
                socket.close();
            }
        });
        sleep(2);
    });

    check(response, {
        'websocket upgraded': r => r && r.status === 101,
    });
}
