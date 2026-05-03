# Sistema de Confirmação de Presença - Versão Básica

## 🎯 Objetivo

Sistema simples para confirmar presença de alunos no transporte universitário.

## ✨ Características Atuais

- ✅ **Frontend puro** - HTML, CSS, JavaScript
- ✅ **Dados locais** - localStorage (sem servidor)
- ✅ **Adicionar alunos** - Nome e email
- ✅ **Confirmar presença** - Presente ou Ausente
- ✅ **Estatísticas** - Total, confirmados, não confirmados
- ✅ **Responsivo** - Funciona em desktop, tablet e mobile

## 📁 Estrutura

```
people-transportation/
├── frontend/
│   ├── index.html      (Interface)
│   ├── styles.css      (Estilos)
│   └── script.js       (Lógica - localStorage)
│
└── database/
    └── schema.sql      (Script SQL para PostgreSQL)
```

## 🚀 Como Usar

### 1. **Abrir no navegador**

Abra o arquivo `frontend/index.html` no navegador (duplo clique ou arrastar para o navegador).

### 2. **Adicionar alunos**

- Digite o nome do aluno
- Digite o email
- Clique em "Adicionar Aluno"

### 3. **Confirmar presença**

- Para cada aluno, escolha:
  - ✓ **Presente**
  - ✗ **Ausente**
  - 🗑️ **Remover**

### 4. **Limpar dados**

Clique em "🗑️ Limpar Tudo" para remover todos os alunos.

## 💾 Dados

Atualmente os dados são salvos no **localStorage** do navegador:
- Ficam salvos automaticamente
- Persistem ao fechar e reabrir o navegador
- Cada navegador tem seus próprios dados

## 🔌 Próximo Passo: PostgreSQL

Quando quiser integrar com PostgreSQL:

1. Crie o banco de dados:
```bash
# No PostgreSQL
CREATE DATABASE confirmacao_presenca;
```

2. Execute o script SQL:
```bash
psql -U postgres -d confirmacao_presenca -f database/schema.sql
```

3. Depois, adicionaremos um backend simples para conexão.

## 🎮 Botões

| Botão | Ação |
|-------|------|
| ✓ Presente | Marca como presente |
| ✗ Ausente | Marca como ausente |
| 🗑️ Remover | Remove o aluno |
| 🗑️ Limpar Tudo | Remove todos os alunos |

## 📊 Status

- **Pendente** - Não confirmado (amarelo)
- **Presente** - Confirmado como presente (verde)
- **Ausente** - Confirmado como ausente (vermelho)

## 🔍 Troubleshooting

**Os dados não estão salvando?**
- Verifique se localStorage está habilitado no navegador
- Tente em modo não-privado

**Como limpar os dados?**
- Clique em "Limpar Tudo" ou
- Abra DevTools → Application → LocalStorage → Delete

---

**Versão: 1.0 - Básica com localStorage**

Próximas versões integrarão PostgreSQL e Node.js

