require('dotenv').config();

const { pool, testConnection, databaseConfig } = require('./src/config/database');

async function testDatabaseConnection() {
    try {
        const connection = await testConnection();

        console.log('Conexao com PostgreSQL realizada com sucesso.');
        console.log(`Host: ${databaseConfig.host}`);
        console.log(`Porta: ${databaseConfig.port}`);
        console.log(`Banco: ${databaseConfig.database}`);
        console.log(`Horario do banco: ${connection.now}`);

        const tablesResult = await pool.query(`
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = 'public'
            ORDER BY table_name
        `);

        const tables = tablesResult.rows.map((row) => row.table_name);
        console.log(`Tabelas encontradas: ${tables.length ? tables.join(', ') : 'nenhuma'}`);
    } catch (error) {
        console.error('Erro ao conectar no PostgreSQL:');
        console.error(error.message);
        process.exitCode = 1;
    } finally {
        await pool.end();
    }
}

testDatabaseConnection();
