# 🚀 Guia Rápido - Sistema de Confirmação de Presença

## Como Iniciar?

### ⚡ Forma Mais Rápida (Docker)

```bash
# 1. Clone o repositório (se não tiver)
cd people-transportation

# 2. Inicie tudo
docker-compose up -d

# 3. Aguarde ~10 segundos
# 4. Abra no navegador: http://localhost:3000/frontend/index.html
```

**Pronto! 🎉**

---

### 📦 Forma Manual (Sem Docker)

**Pré-requisitos:**
- PostgreSQL instalado e rodando
- Node.js v16+

**Passos:**

```bash
# 1. Crie o banco (no PostgreSQL)
CREATE DATABASE people_transportation;

# 2. Configure o backend
cd backend

# 3. Instale dependências
npm install

# 4. Configure .env (já vem pronto no projeto)

# 5. Initialize o banco
npm run init-db

# 6. Inicie o servidor
npm start

# 7. Abra no navegador
http://localhost:3000/frontend/index.html
```

---

## 🎮 Como Usar?

### 1. **Carregar Alunos**
- Selecione uma **Data** (padrão: hoje)
- Selecione uma **Disciplina** (opcional)
- Clique em **"Carregar Alunos"**

### 2. **Confirmar Presença**
- Na tabela, clique em **"Confirmar"** para cada aluno
- Escolha o status:
  - ✓ **Presente**
  - ✗ **Ausente**
  - ⏰ **Atrasado**
  - ⬅️ **Saída Antecipada**

### 3. **Ver Estatísticas**
- Acima da tabela há 3 cards:
  - **Total de Alunos**
  - **Confirmados** ✓
  - **Não Confirmados** ✗

### 4. **Exportar Dados**
- Clique em **"Exportar CSV"** para salvar
- Clique em **"Imprimir"** para impressão

---

## 🛢️ Estrutura do Banco

### Tabelas Criadas Automaticamente:

1. **disciplinas** - Disciplinas/Cursos
2. **alunos** - Lista de estudantes
3. **presencas** - Registro de presenças

### Dados de Exemplo:
- ✅ 4 Disciplinas
- ✅ 8 Alunos
- ✅ Algumas presenças de teste

---

## 🔌 Endpoints da API

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `GET` | `/api/alunos` | Lista todos os alunos |
| `POST` | `/api/alunos` | Cria novo aluno |
| `GET` | `/api/disciplinas` | Lista disciplinas |
| `POST` | `/api/presencas` | Registra presença |
| `GET` | `/api/presencas` | Lista presenças |
| `GET` | `/api/presencas/relatorio/:id` | Relatório do aluno |
| `GET` | `/api/health` | Verifica status |

---

## 🆘 Troubleshooting

### ❌ "Erro ao conectar ao banco"
```bash
# Verifique se PostgreSQL está rodando
docker ps | grep postgres

# Ou inicie manualmente (Windows):
"C:\Program Files\PostgreSQL\15\bin\psql" -U postgres
```

### ❌ "Porta 3000 já em uso"
```bash
# Altere em backend/.env
PORT=3001
```

### ❌ "npm install não funciona"
```bash
# Limpe cache e tente novamente
rm -rf node_modules
npm install
```

### ❌ "Banco não inicializa"
```bash
# Reinicie os containers
docker-compose down
docker-compose up -d --force-recreate
```

---

## 🧪 Testar via cURL

```bash
# Listar alunos
curl http://localhost:3000/api/alunos

# Registrar presença
curl -X POST http://localhost:3000/api/presencas \
  -H "Content-Type: application/json" \
  -d '{"alunoId": 1, "status": "PRESENTE"}'

# Ver saúde da API
curl http://localhost:3000/api/health
```

---

## 📁 Arquivos Importantes

```
├── frontend/index.html      → Interface principal
├── backend/server.js        → API do sistema
├── database/schema.sql      → Estrutura do banco
└── docker-compose.yml       → Configuração containers
```

---

## 💡 Dicas

- 🔄 Dados são persistidos no PostgreSQL
- 📊 Estatísticas atualizam em tempo real
- 🖨️ Use "Imprimir" para gerar PDF
- 📥 Exporte dados em CSV para análise
- 🔒 (TODO) Adicionar autenticação em breve

---

## 📞 Precisa de Ajuda?

1. Verificar logs:
```bash
docker logs people-transportation-backend
```

2. Conectar ao banco:
```bash
docker exec -it people-transportation-postgres psql -U postgres -d people_transportation
```

3. Reiniciar tudo:
```bash
docker-compose restart
```

---

**Bora começar! 🚀**
