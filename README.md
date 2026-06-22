# People Transportation

Sistema para apoio ao transporte universitario, com frontend web, calendario academico, confirmacao de presenca, monitoramento de vans e servicos auxiliares.

## Estrutura principal

- `frontend/`: telas web em HTML, CSS e JavaScript.
- `backend-calendar/`: API Node.js/Express para calendario, presenca, monitoramento, autenticacao simples, mensageria e eleicao Bully via HTTP.
- `database/`: scripts SQL para o banco PostgreSQL usado pelo backend Node.
- `Presenca/`: servico Java/Spring de presenca preservado da branch principal.
- `RegisterAdm/`: servico Java/Spring de cadastro administrativo.
- `ElectionNode/`: servico .NET com algoritmo Bully e otimizacao de rotas.
- `Relatorio/`: servico .NET para relatorios.

## Backend Node

```bash
cd backend-calendar
npm install
npm start
```

Variaveis uteis:

- `PORT`: porta da API, por padrao `3001`.
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`: conexao PostgreSQL.
- `AUTH_DEFAULT_USER`, `AUTH_DEFAULT_PASSWORD`, `AUTH_SECRET`: autenticacao da API.
- `RABBITMQ_ENABLED=true`: habilita integracao RabbitMQ quando `amqplib` estiver instalado.
- `ELECTION_NODE_ID`, `ELECTION_NODES_JSON`: configuracao dos nos da eleicao Bully.

## Endpoints importantes

- `GET /api/health`
- `POST /api/auth/login`
- `GET /api/calendar?mes=YYYY-MM`
- `POST /api/presencas/confirmacao`
- `GET /api/presencas/monitoramento?data=YYYY-MM-DD`
- `POST /api/presencas/efetivacao`
- `POST /api/v1/presencas`
- `POST /api/election/start`
- `GET /api/election/status`

## Frontend

As telas podem ser abertas diretamente pelo navegador a partir da pasta `frontend/`, ou servidas por qualquer servidor estatico.
