require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { pool, testConnection, databaseConfig } = require('./src/config/database');

const app = express();
const PORT = process.env.PORT || 3001;
const CALENDAR_TABLE = 'calendario_2026';
const DEFAULT_CONFIRMATION_RADIUS_METERS = 100;

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
