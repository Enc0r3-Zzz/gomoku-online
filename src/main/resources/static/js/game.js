import { GameSocket } from './ws.js';
import { GomokuBoard } from './board.js';

const root = document.getElementById('gameRoot');
const matchId = Number(root.dataset.matchId);
const myColor = root.dataset.myColor;
const board = new GomokuBoard(document.getElementById('board'));
const socket = new GameSocket();

const turnText = document.getElementById('turnText');
const timerText = document.getElementById('timerText');
const overlay = document.getElementById('resultOverlay');
const resultTitle = document.getElementById('resultTitle');
const resultDetail = document.getElementById('resultDetail');

let status = 'WAITING';
let turn = 'BLACK';
let seq = 0;
let deadline = null;
let myTurn = false;

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
    } else if ((result === 'BLACK_WIN' && myColor === 'BLACK')
            || (result === 'WHITE_WIN' && myColor === 'WHITE')) {
        title = '你赢了';
    } else if (result) {
        title = '你输了';
    }

    resultTitle.textContent = title;
    resultDetail.textContent = result === 'DRAW'
        ? '棋盘已满，双方各得 0.5 分。'
        : '本局结果已记录。';
    overlay.classList.remove('hidden');
}

socket.on('STATE', data => {
    status = data.status;
    seq = data.seq;
    deadline = data.deadline;
    board.setMoves(data.moves || []);
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

socket.on('GAME_OVER', data => showResult(data.result));

socket.on('OPPONENT_LEFT', () => {
    turnText.textContent = '对手掉线，等待重连…';
    myTurn = false;
});

socket.on('OPPONENT_BACK', () => updateTurn(turn));

socket.on('ERROR', data => {
    turnText.textContent = data.message || '操作失败';
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

window.setInterval(() => {
    if (!deadline || status !== 'PLAYING') {
        timerText.textContent = '';
        return;
    }
    const left = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
    timerText.textContent = `${left}s`;
}, 250);
