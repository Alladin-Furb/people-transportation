# 📅 Calendário Acadêmico - Guia de Uso

## O que é?

Interface para gerenciar quais dias **não são letivos** (férias, feriados, etc.) por empresa ou para todas.

## 🚀 Como Usar?

### 1. **Abrir o Calendário**
- Abra `frontend/calendar.html` no navegador

### 2. **Selecionar Empresa** (opcional)
- Escolha na dropdown:
  - "Todas as Empresas" (padrão)
  - "Empresa 1, 2, 3..." (específica)

### 3. **Navegar pelos Meses**
- Use "Anterior" e "Próximo" ou
- Use o input de mês/ano para ir direto

### 4. **Marcar Dias Não-Letivos**
- **Clique** em um dia para marcar/desmarcar
- Dias não-letivos aparecem em **vermelho com ✕**
- Dias letivos aparecem em **verde claro**

### 5. **Visualizar Resumo**
- Na tabela "Resumo do Mês" vê:
  - Total de dias letivos
  - Total de dias não-letivos

### 6. **Ações**
- 💾 **Salvar** - Persiste no localStorage
- 🔄 **Limpar** - Remove todos os marcados do mês/empresa
- 📥 **Exportar** - Baixa um CSV com os dados

## 🎨 Cores

| Cor | Significado |
|-----|------------|
| 🟢 Verde | Dia Letivo |
| 🔴 Vermelho | Dia Não-Letivo |
| ⚫ Cinza | Outro Mês |

## 💾 Armazenamento

**Atualmente:** localStorage do navegador
- Persiste ao fechar/reabrir
- Sem servidor necessário

**Futuramente:** PostgreSQL
- Execute o script: `database/calendar_schema.sql`

## 📊 Integração com PostgreSQL

### 1. Crie a tabela:
```bash
psql -U postgres -d confirmacao_presenca -f database/calendar_schema.sql
```

### 2. Estrutura da tabela:
```sql
CREATE TABLE calendar (
    id SERIAL PRIMARY KEY,
    empresa_id INTEGER,           -- NULL para todas as empresas
    data DATE NOT NULL,           -- Data do dia
    nao_letivo BOOLEAN,           -- true = não-letivo, false = letivo
    criado_em TIMESTAMP,
    atualizado_em TIMESTAMP
);
```

## 🔄 Fluxo de Dados

```
Interface HTML/JS
    ↓
localStorage (temporário)
    ↓
Exportar CSV ou
Salvar no PostgreSQL (futuro)
```

## 🎯 Campos de Entrada

- **empresa_id**: 1, 2, 3... (NULL para todas)
- **data**: YYYY-MM-DD
- **nao_letivo**: true ou false

## 📝 Exemplos de Dados

```
empresa_id | data       | nao_letivo
-----------|------------|----------
NULL       | 2026-05-03 | true      (Feriado - Todas)
1          | 2026-06-15 | true      (Férias - Empresa 1)
2          | 2026-07-20 | true      (Férias - Empresa 2)
NULL       | 2026-12-25 | true      (Natal - Todas)
```

## 🐛 Troubleshooting

**Os dados desaparecem?**
- Verifique se localStorage está habilitado
- Tente em modo não-privado

**Como limpar tudo?**
- Clique em "Limpar Seleção" ou
- Abra DevTools → Application → LocalStorage → Delete

**Como exportar?**
- Clique em "Exportar" e será baixado um CSV

## 🔗 Link para Acessar

```
file:///caminho/para/frontend/calendar.html
```

Ou coloque na mesma pasta do `index.html` e acesse como link.

---

**Próximas melhorias:**
- Integração com PostgreSQL
- API para salvar/carregar dados
- Importar CSV
- Validações de data
- Recurso de "copiar" mês anterior
