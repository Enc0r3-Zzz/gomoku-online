import { GameSocket } from './ws.js';
import { GomokuBoard } from './board.js';

const root = document.getElementById('gameRoot');
const matchId = Number(root.dataset.matchId);
const myId = Number(root.dataset.myId);
const myColor = root.dataset.myColor;
const board = new GomokuBoard(document.getElementById('board'));
const socket = new GameSocket();

const turnText = document.getElementById('turnText');
const timerText = document.getElementById('timerText');
const overlay = document.getElementById('resultOverlay');
const resultTitle = document.getElementById('resultTitle');
const resultDetail = document.getElementById('resultDetail');
const chatLog = document.getElementById('chatLog');
const chatForm = document.getElementById('chatForm');
const chatInput = document.getElementById('chatInput');

let status = 'WAITING';
let turn = 'BLACK';
let seq = 0;
let deadline = null;
let myTurn = false;

function appendSystem(text) {
    const line = document.createElement('div');
    line.className = 'chat-system';
    line.textContent = text;
    chatLog.appendChild(line);
    chatLog.scrollTop = chatLog.scrollHeight;
}

function appendChat(data) {
    const mine = Number(data.senderId) === myId;
    const line = document.createElement('div');
    line.className = mine ? 'chat-line mine' : 'chat-line';

    const name = document.createElement('span');
    name.className = 'chat-name';
    name.textContent = mine ? '我' : root.dataset.opponentName;

    const text = document.createElement('span');
    text.className = 'chat-text';
    text.textContent = data.text;

    line.append(name, text);
    chatLog.appendChild(line);
    chatLog.scrollTop = chatLog.scrollHeight;
}

function renderChats(chats) {
    chatLog.innerHTML = '';
    if (!chats || chats.length === 0) {
        appendSystem('对局开始，打个招呼吧');
        return;
    }
    chats.forEach(appendChat);
}

function updateTurn(nextTurn) {
    if (nextTurn) {
        turn = nextTurn;
    }
    myTurn = status === 'PLAYING' && turn === myColor;
    if (status === 'WAITING') {
        turnText.textContent = '等待对手加入…';
    } else if (status === 'FINISHED') {
        turnText.textContent = '对局结束';
    } else {
        turnText.textContent = myTurn ? '轮到你落子' : '等待对手落子';
    }
}

function showResult(result) {
    status = 'FINISHED';
    myTurn = false;
    updateTurn(turn);

    let title = '对局结束';
    if (result === 'DRAW') {
        title = '平局';
    } else if (result === 'ABORT') {
        title = '对局中止';
    } else if ((result === 'BLACK_WIN' && myColor === 'BLACK')
            || (result === 'WHITE_WIN' && myColor === 'WHITE')) {
        title = '你赢了';
    } else if (result) {
        title = '你输了';
    }

    resultTitle.textContent = title;
    if (result === 'DRAW') {
        resultDetail.textContent = '棋盘已满，双方各得 0.5 分。';
    } else if (result === 'ABORT') {
        resultDetail.textContent = '双方掉线，本局不计分。';
    } else {
        resultDetail.textContent = '本局结果已记录。';
    }
    overlay.classList.remove('hidden');
}

socket.on('STATE', data => {
    status = data.status;
    seq = data.seq;
    deadline = data.deadline;
    board.setMoves(data.moves || []);
    renderChats(data.chats || []);
    if (status === 'FINISHED') {
        showResult(data.result);
    } else {
        updateTurn(data.turn);
    }
});

socket.on('MOVE', data => {
    board.addMove(data);
    seq = data.seq;
    deadline = data.deadline;
    if (data.nextTurn) {
        updateTurn(data.nextTurn);
    }
});

socket.on('CHAT', data => appendChat(data));

socket.on('GAME_OVER', data => {
    appendSystem('对局结束');
    showResult(data.result);
});

socket.on('OPPONENT_LEFT', data => {
    turnText.textContent = '对手掉线，等待重连…';
    myTurn = false;
    appendSystem(`对手掉线，等待重连（${data.graceSeconds || 60} 秒）…`);
});

socket.on('OPPONENT_BACK', () => {
    appendSystem('对手已重新连接');
    updateTurn(turn);
});

socket.on('ERROR', data => {
    appendSystem(data.message || '操作失败');
});

socket.onOpen = () => socket.send('RESUME', { matchId });
socket.connect();

board.onMove = ({ x, y }) => {
    if (status !== 'PLAYING' || !myTurn) {
        return;
    }
    socket.send('MOVE', { matchId, x, y, seq: seq + 1 });
    myTurn = false;
    turnText.textContent = '等待对手落子';
};

document.getElementById('resignBtn').addEventListener('click', () => {
    if (status !== 'PLAYING') {
        return;
    }
    if (window.confirm('确定认输吗？')) {
        socket.send('RESIGN', { matchId });
    }
});

chatForm.addEventListener('submit', event => {
    event.preventDefault();
    const text = chatInput.value.trim();
    if (!text) {
        return;
    }
    socket.send('CHAT', { matchId, text });
    chatInput.value = '';
    chatInput.focus();
});

window.setInterval(() => {
    if (!deadline || status !== 'PLAYING') {
        timerText.textContent = '';
        return;
    }
    const left = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
    timerText.textContent = `${left}s`;
}, 250);
