# Calendario Academico

Interface de visualizacao do calendario universal de 2026.

## Fonte dos dados

O calendario usa a tabela `calendario_2026` no PostgreSQL como fonte oficial.

Campos principais:

- `data`
- `data_formatada`
- `dia_semana`
- `dia_util`
- `feriado`
- `ponto_facultativo`
- `descricao`
- `tipo`

## Regras

- `dia_util = true`: exibido como dia util.
- `dia_util = false`: exibido como stand by.
- Nao ha cadastro manual de feriados pela tela.
- Nao ha filtro por faculdade.
- A tela nao altera o calendario; ela apenas consulta e exporta os dados.

## Uso

1. Abra `frontend/calendar.html`.
2. Escolha o periodo no campo de mes.
3. Navegue com os botoes de mes anterior/proximo.
4. Use `Exportar` para baixar os dados visiveis em CSV.

## API usada

```text
GET /api/calendar?mes=2026-05
```

O retorno e normalizado para o frontend:

```json
{
  "Data": "2026-05-01T03:00:00.000Z",
  "DiaUtil": false,
  "DiaSemana": 5,
  "DataFormatada": "01/05/2026",
  "Feriado": true,
  "PontoFacultativo": false,
  "Descricao": "Dia do Trabalhador",
  "Tipo": "Feriado nacional"
}
```
