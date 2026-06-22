const { Pool } = require('pg');

const databaseConfig = {
    user: process.env.DB_USER || 'postgres',
    host: process.env.DB_HOST || 'localhost',
    database: process.env.DB_NAME || 'people_transportation',
    password: process.env.DB_PASSWORD || 'postgres',
    port: Number(process.env.DB_PORT || 5432),
};

const pool = new Pool(databaseConfig);

pool.on('connect', () => {
    console.log('Conectado ao PostgreSQL');
});

pool.on('error', (err) => {
    console.error('Erro inesperado na conexao com PostgreSQL:', err);
});

async function testConnection() {
    const result = await pool.query('SELECT NOW() AS now');
    return result.rows[0];
}

module.exports = {
    pool,
    testConnection,
    databaseConfig,
};
