require('dotenv').config();
const express = require('express');
const { Pool } = require('pg');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 3001;

// Middleware
app.use(cors());
app.use(express.json());

// Configuração do banco de dados
const pool = new Pool({
    user: process.env.DB_USER || 'postgres',
    host: process.env.DB_HOST || 'localhost',
    database: process.env.DB_NAME || 'people_transportation',
    password: process.env.DB_PASSWORD || 'postgres',
    port: process.env.DB_PORT || 5432,
});

// Testa conexão
pool.on('connect', () => {
    console.log('✓ Conectado ao PostgreSQL');
});

pool.on('error', (err) => {
    console.error('❌ Erro na conexão:', err);
});

async function ensurePresenceTable() {
    await pool.query(`
        CREATE TABLE IF NOT EXISTS "PresenceConfirmation" (
            "Id" SERIAL PRIMARY KEY,
            "NomeAluno" VARCHAR(255) NOT NULL,
            "DataConfirmacao" DATE NOT NULL,
            "TipoDeslocamento" VARCHAR(30) NOT NULL,
            "LocalEmbarque" VARCHAR(255) NOT NULL,
            "Latitude" NUMERIC(10, 6),
            "Longitude" NUMERIC(10, 6),
            "CreatedAt" TIMESTAMP DEFAULT NOW()
        )
    `);

    await pool.query('ALTER TABLE "PresenceConfirmation" ADD COLUMN IF NOT EXISTS "Latitude" NUMERIC(10, 6)');
    await pool.query('ALTER TABLE "PresenceConfirmation" ADD COLUMN IF NOT EXISTS "Longitude" NUMERIC(10, 6)');
}

// ==================== ROTAS ====================

// GET / - Health Check
app.get('/', (req, res) => {
    res.json({ message: 'Servidor de Calendário Acadêmico rodando!' });
});

// GET /api/health - Verifica conexão com DB
app.get('/api/health', async (req, res) => {
    try {
        await pool.query('SELECT NOW()');
        res.json({ status: 'OK', database: 'Connected' });
    } catch (error) {
        res.status(500).json({ status: 'ERROR', error: error.message });
    }
});

// ==================== CALENDÁRIO ====================

// GET /api/calendar - Lista calendário do mês
// Query params: ?mes=2026-05&faculdade=FURB
app.get('/api/calendar', async (req, res) => {
    try {
        const { mes, faculdade } = req.query;

        if (!mes) {
            return res.status(400).json({ error: 'Parâmetro "mes" é obrigatório (formato: YYYY-MM)' });
        }

        const [year, month] = mes.split('-');

        let query = `
            SELECT 
                "Data",
                "DiaUtil",
                "DiaSemana",
                "DataFormatada",
                "FURB",
                "UFSC",
                "SENAI",
                "Unisosciesc",
                "Uniasselvi"
            FROM "Calendar"
            WHERE EXTRACT(YEAR FROM "Data") = $1
            AND EXTRACT(MONTH FROM "Data") = $2
            ORDER BY "Data" ASC
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
            'SELECT * FROM "Calendar" WHERE "Data" = $1',
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

// POST /api/calendar - Atualiza dias úteis para uma faculdade ou todas
// Body: { data: "2026-05-01", faculdade: "FURB" ou "all", diaUtil: true }
app.post('/api/calendar', async (req, res) => {
    try {
        const { data, faculdade, diaUtil } = req.body;

        if (!data || !faculdade) {
            return res.status(400).json({ error: 'Campos "data" e "faculdade" são obrigatórios' });
        }

        // Valida faculdade
        const faculdadesValidas = ['FURB', 'UFSC', 'SENAI', 'Unisosciesc', 'Uniasselvi', 'all'];
        if (!faculdadesValidas.includes(faculdade)) {
            return res.status(400).json({ error: `Faculdade inválida. Válidas: ${faculdadesValidas.join(', ')}` });
        }

        let query;
        let params;

        if (faculdade === 'all') {
            // Atualiza todas as faculdades
            query = `
                UPDATE "Calendar"
                SET "FURB" = $1, "UFSC" = $1, "SENAI" = $1, "Unisosciesc" = $1, "Uniasselvi" = $1
                WHERE "Data" = $2
                RETURNING "Data", "DiaUtil", "DiaSemana", "FURB", "UFSC", "SENAI", "Unisosciesc", "Uniasselvi"
            `;
            params = [diaUtil === true || diaUtil === 'true', data];
        } else {
            // Atualiza apenas uma faculdade
            query = `
                UPDATE "Calendar"
                SET "${faculdade}" = $1
                WHERE "Data" = $2
                RETURNING "Data", "DiaUtil", "DiaSemana", "FURB", "UFSC", "SENAI", "Unisosciesc", "Uniasselvi"
            `;
            params = [diaUtil === true || diaUtil === 'true', data];
        }

        const result = await pool.query(query, params);

        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Data não encontrada no calendário' });
        }

        res.status(200).json({
            message: 'Dia atualizado com sucesso',
            data: result.rows[0]
        });
    } catch (error) {
        console.error('Erro ao atualizar dia:', error);
        res.status(500).json({ error: error.message });
    }
});

// ==================== FACULDADES ====================

// GET /api/faculdades - Lista todas as faculdades
app.get('/api/faculdades', async (req, res) => {
    try {
        res.json([
            { id: 'FURB', nome: 'FURB' },
            { id: 'UFSC', nome: 'UFSC' },
            { id: 'SENAI', nome: 'SENAI' },
            { id: 'Unisosciesc', nome: 'Unisosciesc' },
            { id: 'Uniasselvi', nome: 'Uniasselvi' }
        ]);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// GET /api/empresas - Alias para /api/faculdades (compatibilidade)
app.get('/api/empresas', async (req, res) => {
    try {
        res.json([
            { id: 'FURB', nome: 'FURB' },
            { id: 'UFSC', nome: 'UFSC' },
            { id: 'SENAI', nome: 'SENAI' },
            { id: 'Unisosciesc', nome: 'Unisosciesc' },
            { id: 'Uniasselvi', nome: 'Uniasselvi' }
        ]);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// ==================== CONFIRMAÇÃO DE PRESENÇA ====================

// POST /api/presencas/confirmacao
// Body: { nomeAluno, dataConfirmacao, tipoDeslocamento, localEmbarque, latitude, longitude }
app.post('/api/presencas/confirmacao', async (req, res) => {
    try {
        const { nomeAluno, dataConfirmacao, tipoDeslocamento, localEmbarque, latitude, longitude } = req.body;

        if (!nomeAluno || !dataConfirmacao || !tipoDeslocamento || !localEmbarque || latitude === undefined || longitude === undefined) {
            return res.status(400).json({
                error: 'Campos obrigatórios: nomeAluno, dataConfirmacao, tipoDeslocamento, localEmbarque, latitude, longitude'
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

        const result = await pool.query(
            `
                INSERT INTO "PresenceConfirmation"
                ("NomeAluno", "DataConfirmacao", "TipoDeslocamento", "LocalEmbarque", "Latitude", "Longitude")
                VALUES ($1, $2, $3, $4, $5, $6)
                RETURNING "Id", "NomeAluno", "DataConfirmacao", "TipoDeslocamento", "LocalEmbarque", "Latitude", "Longitude", "CreatedAt"
            `,
            [nomeAluno.trim(), dataConfirmacao, tipoDeslocamento, localEmbarque.trim(), lat, lng]
        );

        return res.status(201).json({
            message: 'Presença confirmada com sucesso',
            data: result.rows[0]
        });
    } catch (error) {
        console.error('Erro ao registrar confirmação de presença:', error);
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
        await ensurePresenceTable();

        app.listen(PORT, () => {
            console.log(`
╔════════════════════════════════════════╗
║   Servidor Calendário Acadêmico        ║
║   Rodando em: http://localhost:${PORT}       ║
║   Banco: ${process.env.DB_NAME}              ║
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
