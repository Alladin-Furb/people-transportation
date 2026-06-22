require('dotenv').config();
const { pool, databaseConfig } = require('./src/config/database');

async function exploreDatabase() {
    try {
        console.log(`Banco configurado: ${databaseConfig.database}`);
        console.log('\n🔍 Explorando estrutura do banco de dados...\n');

        // 1. Listar tabelas
        const tableResult = await pool.query(`
            SELECT table_name 
            FROM information_schema.tables 
            WHERE table_schema = 'public'
            ORDER BY table_name
        `);

        console.log('📋 Tabelas no banco:');
        tableResult.rows.forEach(row => console.log(`   - ${row.table_name}`));

        // 2. Estrutura da tabela calendario_2026
        const columnsResult = await pool.query(`
            SELECT 
                column_name,
                data_type,
                is_nullable,
                column_default
            FROM information_schema.columns
            WHERE table_name = 'calendario_2026'
            ORDER BY ordinal_position
        `);

        console.log('\n📊 Estrutura da tabela calendario_2026:');
        columnsResult.rows.forEach(col => {
            console.log(`   - ${col.column_name} (${col.data_type}) ${col.is_nullable === 'NO' ? 'NOT NULL' : 'NULL'} ${col.column_default ? `DEFAULT: ${col.column_default}` : ''}`);
        });

        // 3. Amostra de dados
        const dataResult = await pool.query(`
            SELECT * FROM calendario_2026 LIMIT 5
        `);

        console.log('\n📈 Primeiros 5 registros:');
        console.log(dataResult.rows);

        // 4. Contar registros
        const countResult = await pool.query('SELECT COUNT(*) FROM calendario_2026');
        console.log(`\n✅ Total de registros: ${countResult.rows[0].count}`);

    } catch (error) {
        console.error('❌ Erro:', error.message);
    } finally {
        await pool.end();
    }
}

exploreDatabase();
