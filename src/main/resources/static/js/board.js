const SIZE = 15;

export class GomokuBoard {
    constructor(canvas) {
        this.canvas = canvas;
        this.ctx = canvas.getContext('2d');
        this.moves = [];
        this.lastMove = null;
        this.onMove = null;
        this.resize();
        window.addEventListener('resize', () => {
            this.resize();
            this.draw();
        });
        this.canvas.addEventListener('click', event => this.click(event));
    }

    resize() {
        const rect = this.canvas.getBoundingClientRect();
        const cssSize = Math.max(280, Math.min(rect.width || 600, 640));
        const dpr = window.devicePixelRatio || 1;
        this.canvas.width = Math.round(cssSize * dpr);
        this.canvas.height = Math.round(cssSize * dpr);
        this.ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        this.size = cssSize;
        this.padding = Math.max(18, cssSize * 0.055);
        this.cell = (cssSize - this.padding * 2) / (SIZE - 1);
    }

    setMoves(moves) {
        this.moves = Array.isArray(moves) ? moves.slice() : [];
        this.lastMove = this.moves.length ? this.moves[this.moves.length - 1] : null;
        this.draw();
    }

    addMove(move) {
        this.moves.push(move);
        this.lastMove = move;
        this.draw();
    }

    pointOf(move) {
        return {
            x: this.padding + move.x * this.cell,
            y: this.padding + move.y * this.cell
        };
    }

    draw() {
        const ctx = this.ctx;
        const size = this.size;
        const padding = this.padding;
        const cell = this.cell;

        ctx.clearRect(0, 0, size, size);

        const background = ctx.createLinearGradient(0, 0, size, size);
        background.addColorStop(0, '#e6c07b');
        background.addColorStop(1, '#d2a45f');
        ctx.fillStyle = background;
        ctx.fillRect(0, 0, size, size);

        ctx.strokeStyle = 'rgba(70, 45, 15, 0.75)';
        ctx.lineWidth = 1;
        for (let i = 0; i < SIZE; i++) {
            const at = padding + i * cell;
            ctx.beginPath();
            ctx.moveTo(padding, at);
            ctx.lineTo(size - padding, at);
            ctx.stroke();
            ctx.beginPath();
            ctx.moveTo(at, padding);
            ctx.lineTo(at, size - padding);
            ctx.stroke();
        }

        const stars = [[3, 3], [11, 3], [3, 11], [11, 11], [7, 7]];
        ctx.fillStyle = 'rgba(60, 38, 12, 0.9)';
        for (const [x, y] of stars) {
            ctx.beginPath();
            ctx.arc(padding + x * cell, padding + y * cell, Math.max(2.2, cell * 0.08), 0, Math.PI * 2);
            ctx.fill();
        }

        this.moves.forEach(move => this.drawStone(move));

        if (this.lastMove) {
            const point = this.pointOf(this.lastMove);
            ctx.strokeStyle = '#c0392b';
            ctx.lineWidth = 2;
            ctx.beginPath();
            ctx.arc(point.x, point.y, Math.max(3, cell * 0.12), 0, Math.PI * 2);
            ctx.stroke();
        }
    }

    drawStone(move) {
        const ctx = this.ctx;
        const point = this.pointOf(move);
        const radius = this.cell * 0.42;
        const isBlack = move.seq % 2 === 1;

        const gradient = ctx.createRadialGradient(
            point.x - radius * 0.35,
            point.y - radius * 0.35,
            radius * 0.1,
            point.x,
            point.y,
            radius);

        if (isBlack) {
            gradient.addColorStop(0, '#6b6b6b');
            gradient.addColorStop(1, '#111111');
        } else {
            gradient.addColorStop(0, '#ffffff');
            gradient.addColorStop(1, '#d8d2c4');
        }

        ctx.beginPath();
        ctx.arc(point.x, point.y, radius, 0, Math.PI * 2);
        ctx.fillStyle = gradient;
        ctx.fill();
        ctx.strokeStyle = 'rgba(0, 0, 0, 0.25)';
        ctx.lineWidth = 0.8;
        ctx.stroke();
    }

    click(event) {
        if (!this.onMove) {
            return;
        }
        const rect = this.canvas.getBoundingClientRect();
        const px = event.clientX - rect.left;
        const py = event.clientY - rect.top;
        const x = Math.round((px - this.padding) / this.cell);
        const y = Math.round((py - this.padding) / this.cell);
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) {
            return;
        }
        const center = this.pointOf({ x, y });
        if (Math.hypot(px - center.x, py - center.y) > this.cell * 0.48) {
            return;
        }
        if (this.moves.some(move => move.x === x && move.y === y)) {
            return;
        }
        this.onMove({ x, y });
    }
}
