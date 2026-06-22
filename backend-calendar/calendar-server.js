require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { pool, testConnection, databaseConfig } = require('./src/config/database');
const { authenticateCredentials, authenticateRequest, createToken, requireRole } = require('./src/auth/token-auth');
const { BullyElection } = require('./src/election/bully-election');
const { EventBus, ROUTING_KEYS } = require('./src/messaging/event-bus');

const app = express();
const PORT = process.env.PORT || 3001;
const CALENDAR_TABLE = 'calendario_2026';
const DEFAULT_CONFIRMATION_RADIUS_METERS = 100;
const eventBus = new EventBus();
const bullyElection = new BullyElection();

// Middleware
app.use(cors());
app.use(express.json());

async function ensurePresenceTable() {
    await pool.query(`
        CREATE TABLE IF NOT EXISTS "Confirmacao_Presenca_Diaria" (
            "NomeAluno" VARCHAR(255),
            "EmpresaTransporte" VARCHAR(255),
            "DataCalendar" DATE,
            "Confirmacao" BOOLEAN,
            "DataHoraPreConfirmacao" TIMESTAMP,
            "DataHoraConfEfetiva" TIME,
            "AlunoConfirmouEfetivacao" BOOLEAN,
            "LocalEmbarque" VARCHAR(255),
            "TipoDeslocamento" VARCHAR(30)
        )
    `);

    await pool.query('ALTER TABLE "Confirmacao_Presenca_Diaria" ADD COLUMN IF NOT EXISTS "TipoDeslocamento" VARCHAR(30)');
    await pool.query('ALTER TABLE "Confirmacao_Presenca_Diaria" ADD COLUMN IF NOT EXISTS "Id" SERIAL');
    await pool.query('ALTER TABLE "Confirmacao_Presenca_Diaria" ADD COLUMN IF NOT EXISTS "AlunoId" INTEGER');
    await pool.query('ALTER TABLE "Confirmacao_Presenca_Diaria" ADD COLUMN IF NOT EXISTS "CursoId" INTEGER');
    await pool.query('ALTER TABLE "Confirmacao_Presenca_Diaria" ADD COLUMN IF NOT EXISTS "StatusPresenca" VARCHAR(40)');
    await pool.query('ALTER TABLE "Confirmacao_Presenca_Diaria" ADD COLUMN IF NOT EXISTS "MotivoFalta" TEXT');
    await pool.query('ALTER TABLE "Confirmacao_Presenca_Diaria" ADD COLUMN IF NOT EXISTS "Justificativa" TEXT');
    await pool.query('ALTER TABLE "Confirmacao_Presenca_Diaria" ADD COLUMN IF NOT EXISTS "Justificado" BOOLEAN DEFAULT false');
}

async function ensureIntegrationTables() {
    await pool.query(`
        CREATE TABLE IF NOT EXISTS "Alunos_Snapshot" (
            "Matricula" VARCHAR(80) PRIMARY KEY,
            "Nome" VARCHAR(255) NOT NULL,
            "Email" VARCHAR(255),
            "Telefone" VARCHAR(80),
            "RotaTransporte" VARCHAR(255),
            "Payload" JSONB,
            "AtualizadoEm" TIMESTAMP DEFAULT NOW()
        )
    `);

    await pool.query(`
        CREATE TABLE IF NOT EXISTS "Eventos_Integracao" (
            "Id" SERIAL PRIMARY KEY,
            "Tipo" VARCHAR(120) NOT NULL,
            "Payload" JSONB NOT NULL,
            "CriadoEm" TIMESTAMP DEFAULT NOW()
        )
    `);
}

async function saveIntegrationEvent(tipo, payload) {
    await pool.query(
        'INSERT INTO "Eventos_Integracao" ("Tipo", "Payload") VALUES ($1, $2)',
        [tipo, JSON.stringify(payload)]
    );
}

async function upsertStudentSnapshot(aluno) {
    const matricula = aluno.matricula || aluno.Matricula || aluno.id || aluno.Id;
    const nome = aluno.nome || aluno.Nome;

    if (!matricula || !nome) {
        throw new Error('Evento de aluno sem matricula/id ou nome');
    }

    await pool.query(
        `
            INSERT INTO "Alunos_Snapshot"
            ("Matricula", "Nome", "Email", "Telefone", "RotaTransporte", "Payload", "AtualizadoEm")
            VALUES ($1, $2, $3, $4, $5, $6, NOW())
            ON CONFLICT ("Matricula") DO UPDATE SET
                "Nome" = EXCLUDED."Nome",
                "Email" = EXCLUDED."Email",
                "Telefone" = EXCLUDED."Telefone",
                "RotaTransporte" = EXCLUDED."RotaTransporte",
                "Payload" = EXCLUDED."Payload",
                "AtualizadoEm" = NOW()
        `,
        [
            String(matricula),
            nome,
            aluno.email || aluno.Email || null,
            aluno.telefone || aluno.Telefone || null,
            aluno.rotaTransporte || aluno.RotaTransporte || null,
            JSON.stringify(aluno)
        ]
    );
}

async function consumeStudentEvent(tipo, aluno) {
    await upsertStudentSnapshot(aluno);
    await saveIntegrationEvent(tipo, aluno);
}

async function configureMessaging() {
    await eventBus.connect();
    await eventBus.consume(ROUTING_KEYS.alunoCadastrado, (aluno) => consumeStudentEvent(ROUTING_KEYS.alunoCadastrado, aluno));
    await eventBus.consume(ROUTING_KEYS.alunoAtualizado, (aluno) => consumeStudentEvent(ROUTING_KEYS.alunoAtualizado, aluno));
}

async function publishPresenceRegisteredEvent(presenca) {
    const event = await eventBus.publishPresenceRegistered(presenca);
    await saveIntegrationEvent(ROUTING_KEYS.presencaRegistrada, event);
    return event;
}

function parseCoordinatePair(value) {
    if (!value || typeof value !== 'string') {
        return null;
    }

    const [latitude, longitude] = value.split(',').map((coordinate) => Number(coordinate.trim()));

    if (
        Number.isNaN(latitude) ||
        Number.isNaN(longitude) ||
        latitude < -90 ||
        latitude > 90 ||
        longitude < -180 ||
        longitude > 180
    ) {
        return null;
    }

    return { latitude, longitude };
}

function calculateDistanceMeters(origin, destination) {
    const earthRadiusMeters = 6371000;
    const toRadians = (degrees) => degrees * (Math.PI / 180);
    const deltaLatitude = toRadians(destination.latitude - origin.latitude);
    const deltaLongitude = toRadians(destination.longitude - origin.longitude);
    const originLatitude = toRadians(origin.latitude);
    const destinationLatitude = toRadians(destination.latitude);

    const a =
        Math.sin(deltaLatitude / 2) ** 2 +
        Math.cos(originLatitude) *
            Math.cos(destinationLatitude) *
            Math.sin(deltaLongitude / 2) ** 2;
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return earthRadiusMeters * c;
}

function formatDateKey(date = new Date()) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
}

function getDemoVanLocation() {
    const baseLatitude = Number(process.env.VAN_DEMO_LATITUDE || -26.9194);
    const baseLongitude = Number(process.env.VAN_DEMO_LONGITUDE || -49.0661);
    const timestamp = Date.now() / 1000;

    return {
        idVan: process.env.VAN_DEMO_ID || 'VAN-01',
        motorista: process.env.VAN_DEMO_MOTORISTA || 'Motorista',
        latitude: Number((baseLatitude + Math.sin(timestamp / 35) * 0.006).toFixed(6)),
        longitude: Number((baseLongitude + Math.cos(timestamp / 35) * 0.006).toFixed(6)),
        velocidadeKmh: Number((28 + Math.sin(timestamp / 12) * 8).toFixed(1)),
        atualizadoEm: new Date().toISOString()
    };
}

function normalizeStatus(status, fallback = 'PRESENTE') {
    const validStatuses = [
        'PRESENTE',
        'AUSENTE',
        'ATRASADO',
        'FALTA_JUSTIFICADA',
        'FALTA_NAO_JUSTIFICADA',
        'SAIDA_ANTECIPADA',
        'PRESENCA_EFETIVADA'
    ];

    const normalized = String(status || fallback).toUpperCase();
    if (!validStatuses.includes(normalized)) {
        throw new Error(`Status invalido. Validos: ${validStatuses.join(', ')}`);
    }

    return normalized;
}

function toPresenceDto(row) {
    return {
        id: row.Id,
        alunoId: row.AlunoId,
        alunoNome: row.NomeAluno,
        cursoId: row.CursoId,
        dataPresenca: row.DataCalendar,
        status: row.StatusPresenca || (row.Confirmacao ? 'PRESENTE' : 'AUSENTE'),
        horaEntrada: row.DataHoraPreConfirmacao,
        horaSaida: row.DataHoraConfEfetiva,
        motivoFalta: row.MotivoFalta,
        justificativa: row.Justificativa,
        justificado: row.Justificado,
        localEmbarque: row.LocalEmbarque,
        tipoDeslocamento: row.TipoDeslocamento,
        alunoConfirmouEfetivacao: row.AlunoConfirmouEfetivacao
    };
}

async function upsertPresenceV1({ alunoId, cursoId, data, status, motivoFalta = null, justificado = false }) {
    const statusPresenca = normalizeStatus(status);
    const existing = await pool.query(
        `
            SELECT "Id"
            FROM "Confirmacao_Presenca_Diaria"
            WHERE "AlunoId" = $1 AND "CursoId" = $2 AND "DataCalendar" = $3
            ORDER BY "DataHoraPreConfirmacao" DESC NULLS LAST
            LIMIT 1
        `,
        [alunoId, cursoId, data]
    );

    const params = [
        alunoId,
        cursoId,
        `Aluno ${alunoId}`,
        data,
        ['PRESENTE', 'ATRASADO', 'SAIDA_ANTECIPADA'].includes(statusPresenca),
        statusPresenca,
        motivoFalta,
        justificado,
        existing.rows[0]?.Id
    ];

    const query = existing.rows.length > 0
        ? `
            UPDATE "Confirmacao_Presenca_Diaria"
            SET "Confirmacao" = $5,
                "StatusPresenca" = $6,
                "MotivoFalta" = $7,
                "Justificado" = $8,
                "DataHoraPreConfirmacao" = COALESCE("DataHoraPreConfirmacao", NOW())
            WHERE "Id" = $9
            RETURNING *
        `
        : `
            INSERT INTO "Confirmacao_Presenca_Diaria"
            ("AlunoId", "CursoId", "NomeAluno", "DataCalendar", "Confirmacao", "DataHoraPreConfirmacao",
             "StatusPresenca", "MotivoFalta", "Justificado")
            VALUES ($1, $2, $3, $4, $5, NOW(), $6, $7, $8)
            RETURNING *
        `;

    const result = await pool.query(query, params);
    return toPresenceDto(result.rows[0]);
}

async function confirmarPresencaV1(req, res) {
    try {
        const { alunoId, cursoId } = req.params;
        const data = req.query.data || req.body.data || formatDateKey();
        const status = req.query.status || req.body.status || 'PRESENTE';
        const presenca = await upsertPresenceV1({ alunoId, cursoId, data, status });

        await publishPresenceRegisteredEvent(presenca);
        res.status(201).json(presenca);
    } catch (error) {
        res.status(400).json({ error: error.message });
    }
}

// ==================== ROTAS ====================

// GET / - Health Check
app.get('/', (req, res) => {
    res.json({ message: 'Servidor de Calendário Acadêmico rodando!' });
});

// GET /api/health - Verifica conexão com DB
app.get('/api/health', async (req, res) => {
    try {
        const connection = await testConnection();
        res.json({
            status: 'OK',
            database: 'Connected',
            databaseName: databaseConfig.database,
            checkedAt: connection.now
        });
    } catch (error) {
        res.status(500).json({ status: 'ERROR', error: error.message });
    }
});

// ==================== AUTENTICACAO ====================

// POST /api/auth/login
// Body: { username, password }
app.post('/api/auth/login', (req, res) => {
    const { username, password } = req.body;
    const user = authenticateCredentials(username, password);

    if (!user) {
        return res.status(401).json({ error: 'Credenciais invalidas' });
    }

    return res.json({
        token: createToken(user),
        user
    });
});

// GET /api/auth/me
app.get('/api/auth/me', authenticateRequest, (req, res) => {
    res.json({ user: req.user });
});

// ==================== ELEICAO BULLY ====================

app.get('/api/election/status', (req, res) => {
    res.json(bullyElection.status());
});

app.post('/api/election/start', authenticateRequest, requireRole('ADMIN'), async (req, res) => {
    try {
        res.json(await bullyElection.startElection());
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

app.post('/api/election/election', async (req, res) => {
    try {
        const { nodeId } = req.body;
        res.json(await bullyElection.receiveElection(Number(nodeId)));
    } catch (error) {
        res.status(400).json({ error: error.message });
    }
});

app.post('/api/election/leader', async (req, res) => {
    try {
        res.json(bullyElection.setLeader(req.body.leaderId));
    } catch (error) {
        res.status(400).json({ error: error.message });
    }
});

app.get('/api/election/leader', async (req, res) => {
    try {
        res.json(await bullyElection.discoverLeader());
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

app.get('/api/election/ping', (req, res) => {
    res.json({ ok: true, nodeId: bullyElection.nodeId });
});

app.post('/api/election/monitor', authenticateRequest, requireRole('ADMIN'), async (req, res) => {
    try {
        res.json(await bullyElection.monitorLeader());
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// ==================== MENSAGERIA / EVENTOS ====================

app.post('/api/events/alunos/cadastrado', authenticateRequest, requireRole('ADMIN'), async (req, res) => {
    try {
        const event = await eventBus.publishStudentRegistered(req.body);
        res.status(202).json(event);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

app.post('/api/events/alunos/atualizado', authenticateRequest, requireRole('ADMIN'), async (req, res) => {
    try {
        const event = await eventBus.publishStudentUpdated(req.body);
        res.status(202).json(event);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

app.get('/api/events/integracao', authenticateRequest, requireRole('ADMIN'), async (req, res) => {
    try {
        const result = await pool.query(
            'SELECT "Id", "Tipo", "Payload", "CriadoEm" FROM "Eventos_Integracao" ORDER BY "CriadoEm" DESC LIMIT 100'
        );
        res.json(result.rows);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// ==================== CALENDÁRIO ====================

// GET /api/calendar - Lista calendário do mês
// Query params: ?mes=2026-05
app.get('/api/calendar', async (req, res) => {
    try {
        const { mes } = req.query;

        if (!mes) {
            return res.status(400).json({ error: 'Parâmetro "mes" é obrigatório (formato: YYYY-MM)' });
        }

        const [year, month] = mes.split('-');

        const query = `
            SELECT
                data AS "Data",
                COALESCE(dia_util, false) AS "DiaUtil",
                CASE dia_semana
                    WHEN 'Domingo' THEN 0
                    WHEN 'Segunda-feira' THEN 1
                    WHEN 'Terça-feira' THEN 2
                    WHEN 'Quarta-feira' THEN 3
                    WHEN 'Quinta-feira' THEN 4
                    WHEN 'Sexta-feira' THEN 5
                    WHEN 'Sábado' THEN 6
                    ELSE EXTRACT(DOW FROM data)::int
                END AS "DiaSemana",
                data_formatada AS "DataFormatada",
                COALESCE(feriado, false) AS "Feriado",
                COALESCE(ponto_facultativo, false) AS "PontoFacultativo",
                descricao AS "Descricao",
                tipo AS "Tipo"
            FROM ${CALENDAR_TABLE}
            WHERE ano = $1
            AND mes = $2
            ORDER BY data ASC
        `;

        const result = await pool.query(query, [year, month]);
        res.json(result.rows);
    } catch (error) {
        console.error('Erro ao listar calendário:', error);
        res.status(500).json({ error: error.message });
    }
});

// GET /api/calendar/:id - Obtém um dia específico
app.get('/api/calendar/:id', async (req, res) => {
    try {
        const { id } = req.params;

        const result = await pool.query(
            `
                SELECT
                    data AS "Data",
                    COALESCE(dia_util, false) AS "DiaUtil",
                    CASE dia_semana
                        WHEN 'Domingo' THEN 0
                        WHEN 'Segunda-feira' THEN 1
                        WHEN 'Terça-feira' THEN 2
                        WHEN 'Quarta-feira' THEN 3
                        WHEN 'Quinta-feira' THEN 4
                        WHEN 'Sexta-feira' THEN 5
                        WHEN 'Sábado' THEN 6
                        ELSE EXTRACT(DOW FROM data)::int
                    END AS "DiaSemana",
                    data_formatada AS "DataFormatada",
                    COALESCE(feriado, false) AS "Feriado",
                    COALESCE(ponto_facultativo, false) AS "PontoFacultativo",
                    descricao AS "Descricao",
                    tipo AS "Tipo"
                FROM ${CALENDAR_TABLE}
                WHERE data = $1
            `,
            [id]
        );

        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Dia não encontrado' });
        }

        res.json(result.rows[0]);
    } catch (error) {
        console.error('Erro ao buscar dia:', error);
        res.status(500).json({ error: error.message });
    }
});

app.post('/api/calendar', (req, res) => {
    res.status(405).json({ error: 'O calendario universal e somente leitura.' });
});

// ==================== PRESENCA V1 ====================

app.post('/api/v1/presencas', async (req, res) => {
    try {
        const { alunoId, alunoNome, cursoId, dataPresenca, status, localEmbarque, tipoDeslocamento, motivoFalta, justificativa } = req.body;

        if (!alunoId || !cursoId || !dataPresenca) {
            return res.status(400).json({ error: 'Campos obrigatorios: alunoId, cursoId, dataPresenca' });
        }

        const statusPresenca = normalizeStatus(status);
        const existing = await pool.query(
            'SELECT "Id" FROM "Confirmacao_Presenca_Diaria" WHERE "AlunoId" = $1 AND "CursoId" = $2 AND "DataCalendar" = $3 LIMIT 1',
            [alunoId, cursoId, dataPresenca]
        );

        if (existing.rows.length > 0) {
            return res.status(409).json({ error: 'Ja existe registro de presenca para este aluno nesta data e curso' });
        }

        const result = await pool.query(
            `
                INSERT INTO "Confirmacao_Presenca_Diaria"
                ("AlunoId", "CursoId", "NomeAluno", "DataCalendar", "Confirmacao", "DataHoraPreConfirmacao",
                 "LocalEmbarque", "TipoDeslocamento", "StatusPresenca", "MotivoFalta", "Justificativa", "Justificado")
                VALUES ($1, $2, $3, $4, $5, NOW(), $6, $7, $8, $9, $10, $11)
                RETURNING *
            `,
            [
                alunoId,
                cursoId,
                alunoNome || `Aluno ${alunoId}`,
                dataPresenca,
                ['PRESENTE', 'ATRASADO', 'SAIDA_ANTECIPADA'].includes(statusPresenca),
                localEmbarque || '',
                tipoDeslocamento || null,
                statusPresenca,
                motivoFalta || null,
                justificativa || null,
                statusPresenca === 'FALTA_JUSTIFICADA'
            ]
        );

        const presenca = toPresenceDto(result.rows[0]);
        await publishPresenceRegisteredEvent(presenca);
        res.status(201).json(presenca);
    } catch (error) {
        res.status(400).json({ error: error.message });
    }
});

app.post('/api/v1/presencas/aluno/:alunoId/curso/:cursoId/confirmar-hoje', async (req, res) => {
    req.query.data = formatDateKey();
    return confirmarPresencaV1(req, res);
});

app.post('/api/v1/presencas/aluno/:alunoId/curso/:cursoId/confirmar', confirmarPresencaV1);

app.post('/api/v1/presencas/aluno/:alunoId/curso/:cursoId/ausencia', async (req, res) => {
    try {
        const { alunoId, cursoId } = req.params;
        const data = req.query.data || req.body.data;
        const motivo = req.query.motivo || req.body.motivo || null;
        const justificado = String(req.query.justificado || req.body.justificado || 'false') === 'true';

        if (!data) {
            return res.status(400).json({ error: 'Parametro data e obrigatorio (YYYY-MM-DD)' });
        }

        const presenca = await upsertPresenceV1({
            alunoId,
            cursoId,
            data,
            status: justificado ? 'FALTA_JUSTIFICADA' : 'FALTA_NAO_JUSTIFICADA',
            motivoFalta: motivo,
            justificado
        });

        await publishPresenceRegisteredEvent(presenca);
        res.status(201).json(presenca);
    } catch (error) {
        res.status(400).json({ error: error.message });
    }
});

app.put('/api/v1/presencas/:id/justificar', async (req, res) => {
    try {
        const justificativa = req.query.justificativa || req.body.justificativa;

        if (!justificativa) {
            return res.status(400).json({ error: 'Justificativa e obrigatoria' });
        }

        const result = await pool.query(
            `
                UPDATE "Confirmacao_Presenca_Diaria"
                SET "Justificativa" = $1, "Justificado" = true, "StatusPresenca" = 'FALTA_JUSTIFICADA'
                WHERE "Id" = $2
                RETURNING *
            `,
            [justificativa, req.params.id]
        );

        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Registro de presenca nao encontrado' });
        }

        const presenca = toPresenceDto(result.rows[0]);
        await publishPresenceRegisteredEvent(presenca);
        res.json(presenca);
    } catch (error) {
        res.status(400).json({ error: error.message });
    }
});

app.get('/api/v1/presencas/:id', async (req, res) => {
    try {
        const result = await pool.query('SELECT * FROM "Confirmacao_Presenca_Diaria" WHERE "Id" = $1', [req.params.id]);

        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Registro de presenca nao encontrado' });
        }

        res.json(toPresenceDto(result.rows[0]));
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

app.get('/api/v1/presencas/aluno/:alunoId/periodo', async (req, res) => {
    try {
        const result = await pool.query(
            'SELECT * FROM "Confirmacao_Presenca_Diaria" WHERE "AlunoId" = $1 AND "DataCalendar" BETWEEN $2 AND $3 ORDER BY "DataCalendar" ASC',
            [req.params.alunoId, req.query.dataInicio, req.query.dataFim]
        );
        res.json(result.rows.map(toPresenceDto));
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

app.get('/api/v1/presencas/curso/:cursoId/periodo', async (req, res) => {
    try {
        const result = await pool.query(
            'SELECT * FROM "Confirmacao_Presenca_Diaria" WHERE "CursoId" = $1 AND "DataCalendar" BETWEEN $2 AND $3 ORDER BY "DataCalendar" ASC',
            [req.params.cursoId, req.query.dataInicio, req.query.dataFim]
        );
        res.json(result.rows.map(toPresenceDto));
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

app.get('/api/v1/presencas/relatorio/aluno/:alunoId/curso/:cursoId', async (req, res) => {
    try {
        const result = await pool.query(
            'SELECT * FROM "Confirmacao_Presenca_Diaria" WHERE "AlunoId" = $1 AND "CursoId" = $2 AND "DataCalendar" BETWEEN $3 AND $4',
            [req.params.alunoId, req.params.cursoId, req.query.dataInicio, req.query.dataFim]
        );
        const presencas = result.rows.map(toPresenceDto);
        const count = (status) => presencas.filter((presenca) => presenca.status === status).length;
        const totalAulas = presencas.length;
        const presentes = count('PRESENTE');
        const atrasados = count('ATRASADO');
        const faltasJustificadas = count('FALTA_JUSTIFICADA');
        const frequencia = totalAulas > 0 ? ((presentes + atrasados + faltasJustificadas) * 100) / totalAulas : 0;

        res.json({
            alunoId: Number(req.params.alunoId),
            cursoId: Number(req.params.cursoId),
            totalAulas,
            presentes,
            ausentes: count('AUSENTE'),
            atrasados,
            faltasJustificadas,
            faltasNaoJustificadas: count('FALTA_NAO_JUSTIFICADA'),
            saidasAntecipadas: count('SAIDA_ANTECIPADA'),
            frequencia: Number(frequencia.toFixed(2)),
            statusFrequencia: frequencia >= 75 ? 'OK' : frequencia >= 50 ? 'AVISO' : 'CRITICO'
        });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

app.get('/api/v1/presencas/aluno/:alunoId/faltas-nao-justificadas', async (req, res) => {
    try {
        const result = await pool.query(
            'SELECT * FROM "Confirmacao_Presenca_Diaria" WHERE "AlunoId" = $1 AND "StatusPresenca" = $2 ORDER BY "DataCalendar" DESC',
            [req.params.alunoId, 'FALTA_NAO_JUSTIFICADA']
        );
        res.json(result.rows.map(toPresenceDto));
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

app.get('/api/v1/presencas/aluno/:alunoId/contar-faltas', async (req, res) => {
    try {
        const result = await pool.query(
            'SELECT COUNT(*)::int AS total FROM "Confirmacao_Presenca_Diaria" WHERE "AlunoId" = $1 AND "StatusPresenca" = $2 AND "DataCalendar" BETWEEN $3 AND $4',
            [req.params.alunoId, 'FALTA_NAO_JUSTIFICADA', req.query.dataInicio, req.query.dataFim]
        );
        res.json(result.rows[0].total);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});


// ==================== CONFIRMAÇÃO DE PRESENÇA ====================

// POST /api/presencas/confirmacao
// Body: { nomeAluno, dataConfirmacao, tipoDeslocamento, latitude, longitude }
app.post('/api/presencas/confirmacao', async (req, res) => {
    try {
        const { nomeAluno, dataConfirmacao, tipoDeslocamento, latitude, longitude } = req.body;

        if (!nomeAluno || !dataConfirmacao || !tipoDeslocamento || latitude === undefined || longitude === undefined) {
            return res.status(400).json({
                error: 'Campos obrigatórios: nomeAluno, dataConfirmacao, tipoDeslocamento, latitude, longitude'
            });
        }

        const tiposValidos = ['VOU_E_VOLTO', 'APENAS_VOLTO', 'APENAS_VOU'];
        if (!tiposValidos.includes(tipoDeslocamento)) {
            return res.status(400).json({
                error: `tipoDeslocamento inválido. Válidos: ${tiposValidos.join(', ')}`
            });
        }

        const lat = Number(latitude);
        const lng = Number(longitude);

        if (Number.isNaN(lat) || Number.isNaN(lng) || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            return res.status(400).json({ error: 'Coordenadas inválidas (latitude/longitude).' });
        }

        const coordenadasEmbarque = `${lat},${lng}`;

        const result = await pool.query(
            `
                INSERT INTO "Confirmacao_Presenca_Diaria"
                ("NomeAluno", "EmpresaTransporte", "DataCalendar", "Confirmacao", "DataHoraPreConfirmacao", "LocalEmbarque", "TipoDeslocamento")
                VALUES ($1, $2, $3, true, NOW(), $4, $5)
                RETURNING
                    "NomeAluno",
                    "EmpresaTransporte",
                    "DataCalendar",
                    "Confirmacao",
                    "DataHoraPreConfirmacao",
                    "LocalEmbarque",
                    "TipoDeslocamento"
            `,
            [nomeAluno.trim(), '', dataConfirmacao, coordenadasEmbarque, tipoDeslocamento]
        );

        await publishPresenceRegisteredEvent({
            nomeAluno: result.rows[0].NomeAluno,
            dataCalendar: result.rows[0].DataCalendar,
            status: 'PRESENTE'
        });

        return res.status(201).json({
            message: 'Presença confirmada com sucesso',
            table: 'Confirmacao_Presenca_Diaria',
            data: result.rows[0]
        });
    } catch (error) {
        console.error('Erro ao registrar confirmação de presença:', error);
        return res.status(500).json({ error: error.message });
    }
});

// GET /api/presencas/monitoramento?data=YYYY-MM-DD
app.get('/api/presencas/monitoramento', async (req, res) => {
    try {
        const data = req.query.data || formatDateKey();

        const result = await pool.query(
            `
                SELECT
                    "NomeAluno",
                    "EmpresaTransporte",
                    "DataCalendar",
                    "Confirmacao",
                    "AlunoConfirmouEfetivacao",
                    "DataHoraPreConfirmacao",
                    "DataHoraConfEfetiva",
                    "LocalEmbarque",
                    "TipoDeslocamento"
                FROM "Confirmacao_Presenca_Diaria"
                WHERE "DataCalendar" = $1
                AND "Confirmacao" = true
                ORDER BY "DataHoraPreConfirmacao" DESC NULLS LAST
            `,
            [data]
        );

        const presencas = result.rows
            .map((row) => {
                const coordenadas = parseCoordinatePair(row.LocalEmbarque);

                return {
                    nomeAluno: row.NomeAluno,
                    empresaTransporte: row.EmpresaTransporte,
                    dataCalendar: row.DataCalendar,
                    confirmacao: row.Confirmacao,
                    alunoConfirmouEfetivacao: row.AlunoConfirmouEfetivacao,
                    dataHoraPreConfirmacao: row.DataHoraPreConfirmacao,
                    dataHoraConfEfetiva: row.DataHoraConfEfetiva,
                    localEmbarque: row.LocalEmbarque,
                    tipoDeslocamento: row.TipoDeslocamento,
                    possuiCoordenadas: Boolean(coordenadas),
                    latitude: coordenadas ? coordenadas.latitude : null,
                    longitude: coordenadas ? coordenadas.longitude : null
                };
            });

        return res.json({
            data,
            total: presencas.length,
            presencas
        });
    } catch (error) {
        console.error('Erro ao listar presencas para monitoramento:', error);
        return res.status(500).json({ error: error.message });
    }
});

// GET /api/vans/localizacao/tempo-real
app.get('/api/vans/localizacao/tempo-real', (req, res) => {
    res.json(getDemoVanLocation());
});

// POST /api/presencas/efetivacao
// Body: { nomeAluno, dataCalendar, vanLatitude, vanLongitude, raioMetros? }
app.post('/api/presencas/efetivacao', async (req, res) => {
    try {
        const { nomeAluno, dataCalendar, vanLatitude, vanLongitude, raioMetros } = req.body;

        if (!nomeAluno || !dataCalendar || vanLatitude === undefined || vanLongitude === undefined) {
            return res.status(400).json({
                error: 'Campos obrigatórios: nomeAluno, dataCalendar, vanLatitude, vanLongitude'
            });
        }

        const vanCoordinates = {
            latitude: Number(vanLatitude),
            longitude: Number(vanLongitude)
        };

        if (
            Number.isNaN(vanCoordinates.latitude) ||
            Number.isNaN(vanCoordinates.longitude) ||
            vanCoordinates.latitude < -90 ||
            vanCoordinates.latitude > 90 ||
            vanCoordinates.longitude < -180 ||
            vanCoordinates.longitude > 180
        ) {
            return res.status(400).json({ error: 'Coordenadas da van inválidas.' });
        }

        const radiusMeters = raioMetros === undefined ? DEFAULT_CONFIRMATION_RADIUS_METERS : Number(raioMetros);

        if (Number.isNaN(radiusMeters) || radiusMeters <= 0) {
            return res.status(400).json({ error: 'raioMetros deve ser um número maior que zero.' });
        }

        const preConfirmationResult = await pool.query(
            `
                SELECT
                    ctid,
                    "NomeAluno",
                    "DataCalendar",
                    "LocalEmbarque",
                    "Confirmacao",
                    "AlunoConfirmouEfetivacao",
                    "DataHoraPreConfirmacao"
                FROM "Confirmacao_Presenca_Diaria"
                WHERE "NomeAluno" = $1
                AND "DataCalendar" = $2
                AND "Confirmacao" = true
                ORDER BY "DataHoraPreConfirmacao" DESC NULLS LAST
                LIMIT 1
            `,
            [nomeAluno.trim(), dataCalendar]
        );

        if (preConfirmationResult.rows.length === 0) {
            return res.status(404).json({ error: 'Pré-confirmação de presença não encontrada para este aluno e data.' });
        }

        const preConfirmation = preConfirmationResult.rows[0];
        const studentBoardingCoordinates = parseCoordinatePair(preConfirmation.LocalEmbarque);

        if (!studentBoardingCoordinates) {
            return res.status(422).json({ error: 'LocalEmbarque da pré-confirmação não possui coordenadas válidas.' });
        }

        const distanceMeters = calculateDistanceMeters(studentBoardingCoordinates, vanCoordinates);
        const isWithinRadius = distanceMeters <= radiusMeters;

        if (!isWithinRadius) {
            return res.status(200).json({
                efetivada: false,
                motivo: 'Van fora do raio permitido para efetivar presença.',
                distanciaMetros: Number(distanceMeters.toFixed(2)),
                raioMetros: radiusMeters
            });
        }

        const updateResult = await pool.query(
            `
                UPDATE "Confirmacao_Presenca_Diaria"
                SET
                    "AlunoConfirmouEfetivacao" = true,
                    "DataHoraConfEfetiva" = NOW()::time
                WHERE ctid = $1
                RETURNING
                    "NomeAluno",
                    "DataCalendar",
                    "Confirmacao",
                    "AlunoConfirmouEfetivacao",
                    "DataHoraPreConfirmacao",
                    "DataHoraConfEfetiva",
                    "LocalEmbarque",
                    "TipoDeslocamento"
            `,
            [preConfirmation.ctid]
        );

        await publishPresenceRegisteredEvent({
            nomeAluno: updateResult.rows[0].NomeAluno,
            dataCalendar: updateResult.rows[0].DataCalendar,
            status: 'PRESENCA_EFETIVADA'
        });

        return res.status(200).json({
            efetivada: true,
            message: 'Presença efetivada com sucesso.',
            distanciaMetros: Number(distanceMeters.toFixed(2)),
            raioMetros: radiusMeters,
            data: updateResult.rows[0]
        });
    } catch (error) {
        console.error('Erro ao efetivar confirmação de presença:', error);
        return res.status(500).json({ error: error.message });
    }
});

// ==================== ERRO 404 ====================

app.use((req, res) => {
    res.status(404).json({ error: 'Rota não encontrada' });
});

// ==================== INICIAR SERVIDOR ====================

async function startServer() {
    try {
        await testConnection();
        await ensurePresenceTable();
        await ensureIntegrationTables();
        await configureMessaging();

        app.listen(PORT, () => {
            console.log(`
╔════════════════════════════════════════╗
║   Servidor Calendário Acadêmico        ║
║   Rodando em: http://localhost:${PORT}       ║
║   Banco: ${databaseConfig.database}              ║
╚════════════════════════════════════════╝
            `);
        });
    } catch (error) {
        console.error('❌ Erro ao inicializar servidor:', error);
        process.exit(1);
    }
}

startServer();

module.exports = app;
