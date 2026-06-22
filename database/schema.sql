CREATE TABLE IF NOT EXISTS calendario_2026 (
    id SERIAL PRIMARY KEY,
    data DATE NOT NULL UNIQUE,
    data_formatada VARCHAR(10),
    ano INTEGER,
    mes INTEGER,
    dia INTEGER,
    dia_semana VARCHAR(20),
    fim_de_semana BOOLEAN,
    dia_util BOOLEAN,
    feriado BOOLEAN DEFAULT false,
    ponto_facultativo BOOLEAN DEFAULT false,
    descricao VARCHAR(255),
    tipo VARCHAR(50),
    observacao TEXT,
    eh_aula_furb BOOLEAN,
    periodo_furb VARCHAR(100)
);

INSERT INTO calendario_2026 (
    data,
    data_formatada,
    ano,
    mes,
    dia,
    dia_semana,
    fim_de_semana,
    dia_util,
    feriado,
    ponto_facultativo,
    eh_aula_furb
)
SELECT
    calendar_date::date,
    TO_CHAR(calendar_date, 'DD/MM/YYYY'),
    EXTRACT(YEAR FROM calendar_date)::int,
    EXTRACT(MONTH FROM calendar_date)::int,
    EXTRACT(DAY FROM calendar_date)::int,
    CASE EXTRACT(DOW FROM calendar_date)::int
        WHEN 0 THEN 'Domingo'
        WHEN 1 THEN 'Segunda-feira'
        WHEN 2 THEN 'Terça-feira'
        WHEN 3 THEN 'Quarta-feira'
        WHEN 4 THEN 'Quinta-feira'
        WHEN 5 THEN 'Sexta-feira'
        WHEN 6 THEN 'Sábado'
    END,
    EXTRACT(DOW FROM calendar_date)::int IN (0, 6),
    EXTRACT(DOW FROM calendar_date)::int NOT IN (0, 6),
    false,
    false,
    EXTRACT(DOW FROM calendar_date)::int NOT IN (0, 6)
FROM generate_series('2026-01-01'::date, '2026-12-31'::date, interval '1 day') AS calendar_date
ON CONFLICT (data) DO NOTHING;

CREATE TABLE IF NOT EXISTS "PresenceConfirmation" (
    "Id" SERIAL PRIMARY KEY,
    "NomeAluno" VARCHAR(255) NOT NULL,
    "DataConfirmacao" DATE NOT NULL,
    "TipoDeslocamento" VARCHAR(30) NOT NULL,
    "LocalEmbarque" VARCHAR(255) NOT NULL,
    "Latitude" NUMERIC(10, 6),
    "Longitude" NUMERIC(10, 6),
    "CreatedAt" TIMESTAMP DEFAULT NOW()
);

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
);
