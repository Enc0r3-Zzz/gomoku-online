import { GameSocket } from './ws.js';

const button = document.getElementById('matchBtn');
const statusEl = document.getElementById('matchStatus');
const socket = new GameSocket();

let queued = false;

socket.on('QUEUED', () => {
    statusEl.textContent = '匹配中，请稍候…';
});

socket.on('MATCH_FOUND', data => {
    statusEl.textContent = '匹配成功，进入对局…';
    window.location.href = `/game/${data.matchId}`;
});

socket.on('ERROR', data => {
    statusEl.textContent = data.message || '操作失败';
});

socket.onOpen = () => {
    if (queued) {
        socket.send('QUEUE_JOIN', {});
    }
};

socket.connect();

button.addEventListener('click', () => {
    queued = !queued;
    if (queued) {
        button.textContent = '取消匹配';
        statusEl.textContent = '正在匹配…';
        socket.send('QUEUE_JOIN', {});
    } else {
        button.textContent = '开始匹配';
        statusEl.textContent = '';
        socket.send('QUEUE_LEAVE', {});
    }
});
