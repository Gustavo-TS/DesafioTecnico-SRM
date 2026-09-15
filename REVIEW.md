# SRM Credit Engine — Code Review Reverso

Este documento revisa o código do Anexo A do desafio, considerando impacto em produção e prioridade de correção.

## 1. SQL Injection — criticidade alta

O código monta SQL diretamente com valores recebidos da requisição:

```ts
SELECT * FROM receivables WHERE id = ${receivableId}
```

e também:

```ts
INSERT INTO settlements (...)
VALUES (${receivableId}, ${finalAmount.toFixed(2)}, '${currency}')
```

### Impacto

Um valor malicioso pode alterar a consulta, acessar dados indevidos ou executar comandos não esperados no banco.

### Correção proposta

Utilizar queries parametrizadas/prepared statements em todas as operações com banco.

Exemplo conceitual:

```ts
db.query(
  "SELECT * FROM receivables WHERE id = ?",
  [receivableId]
)
```

Nenhum dado vindo da requisição deve ser concatenado diretamente ao SQL.

---

## 2. Ausência de transação — criticidade alta

A inserção da liquidação e a atualização do recebível são executadas separadamente:

```ts
INSERT INTO settlements ...
```

seguido por:

```ts
UPDATE receivables SET status = 'SETTLED' ...
```

### Impacto

Se o primeiro comando funcionar e o segundo falhar, o banco fica inconsistente:

- existe uma liquidação registrada;
- o recebível continua com o status anterior.

Esse é exatamente o tipo de estado parcial que não pode ocorrer em uma operação financeira.

### Correção proposta

Executar ambas as operações dentro de uma única transação ACID.

Fluxo esperado:

1. iniciar transação;
2. validar o recebível;
3. inserir liquidação;
4. atualizar status;
5. commit;
6. em qualquer falha, rollback.

---

## 3. Exceção ignorada e resposta falsa de sucesso — criticidade alta

O código possui:

```ts
catch (e) {
  // se falhar aqui, o insert já rodou, então segue o jogo
}
```

e depois retorna:

```ts
res.status(200).json({ ok: true, ... })
```

### Impacto

A API pode informar sucesso mesmo quando houve erro no processamento.

Isso dificulta auditoria, esconde falhas e pode induzir sistemas clientes ou operadores a acreditar que a liquidação foi concluída corretamente.

### Correção proposta

Nunca ignorar a exceção.

A falha deve:

- provocar rollback da transação;
- ser registrada em log;
- retornar status HTTP de erro adequado;
- não retornar `ok: true`.

---

## 4. Falta de idempotência — criticidade alta

O endpoint não exige nem armazena uma chave de idempotência.

### Impacto

Retry de rede, duplo clique ou reenvio da mesma requisição pode criar múltiplas liquidações para o mesmo recebível.

Em um sistema financeiro isso pode resultar em pagamento duplicado.

### Correção proposta

Exigir uma `Idempotency-Key` por requisição e persistir essa chave com restrição de unicidade.

Comportamento esperado:

- primeira requisição cria a liquidação;
- retry com mesma chave retorna a operação existente;
- reutilização da chave para outra operação retorna conflito.

Também deve existir restrição única para impedir mais de uma liquidação por recebível.

---

## 5. Uso incorreto de taxas — criticidade alta

O código define:

```ts
const BASE_RATE = 1.0;
```

e spreads:

```ts
1.5
2.5
```

Na fórmula:

```ts
1 + BASE_RATE + spread
```

### Impacto

Os valores estão sendo tratados como `1`, `1.5` e `2.5`, e não como percentuais de `1%`, `1,5%` e `2,5%`.

Isso produz uma precificação completamente incorreta.

### Correção proposta

Representar corretamente as taxas:

```text
1%   = 0.01
1.5% = 0.015
2.5% = 0.025
```

Além disso, a taxa base não deveria ficar implícita no código sem uma decisão documentada.

---

## 6. Uso de ponto flutuante para cálculo financeiro — criticidade alta

O cálculo utiliza `number`/`Math.pow` e `toFixed`.

### Impacto

Ponto flutuante binário pode introduzir diferenças de centavos por erro de representação.

Em cálculos financeiros, esse tipo de divergência compromete a corretude e pode gerar resultados diferentes dos golden cases.

### Correção proposta

Utilizar um tipo decimal apropriado para valores financeiros e definir explicitamente:

- precisão;
- política de arredondamento;
- momento do arredondamento.

No projeto entregue foi adotado `BigDecimal` com `HALF_EVEN`.

---

## 7. Política de arredondamento não definida — criticidade média/alta

O código usa:

```ts
finalAmount.toFixed(2)
```

### Impacto

`toFixed` não documenta a política financeira esperada e o arredondamento pode ocorrer no momento errado.

O desafio exige `HALF_EVEN` e arredondamento somente no resultado final dos golden cases.

### Correção proposta

Definir uma política explícita de arredondamento e aplicá-la somente nos pontos determinados pela regra de negócio.

---

## 8. Taxa de câmbio sem regra de vigência explícita — criticidade média/alta

O código chama:

```ts
fxService.getLatestRate("USD")
```

### Impacto

“Latest” é ambíguo.

Não fica claro se a taxa deve ser:

- a mais recentemente cadastrada;
- a vigente no instante da operação;
- a taxa do vencimento;
- a taxa de fechamento do dia.

Uma taxa cadastrada no futuro poderia ser usada incorretamente.

### Correção proposta

Persistir `vigenteEm` e selecionar a taxa mais recente com:

```text
vigenteEm <= instante da operação
```

Também registrar na liquidação qual taxa e qual vigência foram efetivamente utilizadas.

---

## 9. Falta de validação do recebível — criticidade média/alta

O código assume que:

```ts
receivable
```

sempre existe e possui dados válidos.

### Impacto

Um ID inexistente pode causar erro inesperado.

Também não há verificação de:

- status atual;
- vencimento;
- tipo suportado;
- existência de liquidação anterior.

### Correção proposta

Validar o recurso antes do cálculo.

Exemplos:

- recurso inexistente → `404`;
- recebível já liquidado → `409`;
- dados inválidos → `400`.

---

## 10. Tipo de recebível tratado com condicional frágil — criticidade média

O spread é escolhido assim:

```ts
receivable.type === "DUPLICATA" ? 1.5 : 2.5
```

### Impacto

Qualquer tipo que não seja `DUPLICATA` automaticamente recebe spread de cheque.

Ao adicionar um terceiro tipo, o comportamento pode ficar incorreto sem erro explícito.

### Correção proposta

Usar uma estratégia explícita por tipo de recebível.

Tipos não suportados devem gerar erro, e não cair silenciosamente em uma regra padrão.

---

## 11. Ausência de validação da moeda — criticidade média

O código trata USD de forma especial e qualquer outra moeda segue como se fosse a moeda original.

### Impacto

Uma moeda inválida pode ser aceita sem erro.

### Correção proposta

Aceitar apenas moedas suportadas pelo domínio e rejeitar valores desconhecidos com `400 Bad Request`.

---

## 12. Falta de auditabilidade — criticidade média

A liquidação salva apenas:

- `receivable_id`;
- `amount`;
- `currency`.

### Impacto

Depois da operação não é possível reconstruir com segurança como o valor foi calculado.

Se taxa base, spread ou câmbio mudarem, perde-se o contexto original da liquidação.

### Correção proposta

Persistir um snapshot da operação contendo, no mínimo:

- valor de face;
- taxa base;
- spread;
- prazo;
- valor presente;
- deságio;
- moeda de pagamento;
- taxa de câmbio utilizada;
- vigência da taxa;
- valor final;
- timestamp;
- chave de idempotência.

---

## 13. Código HTTP inadequado — criticidade média

O endpoint sempre retorna:

```ts
200 OK
```

inclusive após criar uma liquidação e até mesmo quando ocorre uma falha interna.

### Impacto

O cliente não consegue distinguir corretamente criação, conflito e erro.

### Correção proposta

Utilizar códigos semânticos, por exemplo:

- `201 Created` ao criar a liquidação;
- `200 OK` em retry idempotente que apenas retorna a existente;
- `400 Bad Request` para entrada inválida;
- `404 Not Found` para recebível inexistente;
- `409 Conflict` para conflitos de estado;
- `500 Internal Server Error` para falhas inesperadas.

---

## Priorização

A ordem de correção que eu adotaria seria:

1. impedir SQL Injection;
2. colocar a liquidação em transação;
3. parar de ignorar exceções e falsos sucessos;
4. implementar idempotência e unicidade;
5. corrigir percentuais e precisão financeira;
6. definir arredondamento;
7. definir regra de vigência cambial;
8. validar recurso, status, tipo e moeda;
9. melhorar auditabilidade;
10. corrigir semântica HTTP;
11. desacoplar a regra de spread com Strategy.

Os primeiros itens podem causar perda financeira, duplicidade, inconsistência de dados ou vulnerabilidade de segurança e, por isso, devem ser tratados antes de melhorias de organização do código.
