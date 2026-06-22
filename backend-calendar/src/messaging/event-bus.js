const EventEmitter = require('events');

const EXCHANGES = {
    aluno: 'aluno.events',
    presenca: 'presenca.events'
};

const ROUTING_KEYS = {
    alunoCadastrado: 'aluno.cadastrado',
    alunoAtualizado: 'aluno.atualizado',
    presencaRegistrada: 'presenca.registrada'
};

const QUEUES = {
    alunoCadastrado: 'presenca.aluno.cadastrado',
    alunoAtualizado: 'presenca.aluno.atualizado'
};

class EventBus {
    constructor() {
        this.localBus = new EventEmitter();
        this.connection = null;
        this.channel = null;
        this.connected = false;
    }

    async connect() {
        if (this.connected || process.env.RABBITMQ_ENABLED !== 'true') {
            return;
        }

        let amqp;
        try {
            amqp = require('amqplib');
        } catch {
            console.warn('amqplib nao instalado. Mensageria RabbitMQ em modo local.');
            return;
        }

        const url = process.env.RABBITMQ_URL || 'amqp://admin:admin123@localhost:5672';
        this.connection = await amqp.connect(url);
        this.channel = await this.connection.createChannel();

        await this.channel.assertExchange(EXCHANGES.aluno, 'topic', { durable: true });
        await this.channel.assertExchange(EXCHANGES.presenca, 'topic', { durable: true });
        await this.bindQueue(QUEUES.alunoCadastrado, EXCHANGES.aluno, ROUTING_KEYS.alunoCadastrado);
        await this.bindQueue(QUEUES.alunoAtualizado, EXCHANGES.aluno, ROUTING_KEYS.alunoAtualizado);

        this.connected = true;
        console.log('Mensageria RabbitMQ conectada');
    }

    async bindQueue(queue, exchange, routingKey) {
        await this.channel.assertQueue(queue, { durable: true });
        await this.channel.bindQueue(queue, exchange, routingKey);
    }

    async publish(exchange, routingKey, payload) {
        const message = {
            ...payload,
            routingKey,
            publishedAt: new Date().toISOString()
        };

        if (this.connected && this.channel) {
            this.channel.publish(exchange, routingKey, Buffer.from(JSON.stringify(message)), {
                contentType: 'application/json',
                persistent: true
            });
        }

        this.localBus.emit(routingKey, message);
        return message;
    }

    async consume(routingKey, handler) {
        this.localBus.on(routingKey, handler);

        if (!this.connected || !this.channel) {
            return;
        }

        const queue = routingKey === ROUTING_KEYS.alunoCadastrado
            ? QUEUES.alunoCadastrado
            : QUEUES.alunoAtualizado;

        await this.channel.consume(queue, async (message) => {
            if (!message) {
                return;
            }

            try {
                const payload = JSON.parse(message.content.toString('utf8'));
                await handler(payload);
                this.channel.ack(message);
            } catch (error) {
                console.error(`Erro ao consumir ${routingKey}:`, error);
                this.channel.nack(message, false, false);
            }
        });
    }

    async publishPresenceRegistered(presenca) {
        return this.publish(EXCHANGES.presenca, ROUTING_KEYS.presencaRegistrada, {
            id: presenca.id,
            alunoId: presenca.alunoId || null,
            alunoNome: presenca.alunoNome || presenca.nomeAluno,
            dataPresenca: presenca.dataPresenca || presenca.dataCalendar,
            status: presenca.status
        });
    }

    async publishStudentRegistered(aluno) {
        return this.publish(EXCHANGES.aluno, ROUTING_KEYS.alunoCadastrado, aluno);
    }

    async publishStudentUpdated(aluno) {
        return this.publish(EXCHANGES.aluno, ROUTING_KEYS.alunoAtualizado, aluno);
    }
}

module.exports = {
    EventBus,
    EXCHANGES,
    QUEUES,
    ROUTING_KEYS
};
