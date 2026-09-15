# SRM Credit Engine — Especificação

## 1. Objetivo

O SRM Credit Engine é uma aplicação para cadastro, precificação e liquidação de recebíveis, permitindo operações em BRL e USD.

O sistema permite:

- cadastrar cedentes;
- cadastrar recebíveis;
- registrar taxas de câmbio;
- simular a precificação de um recebível;
- liquidar recebíveis em BRL ou USD;
- consultar o extrato de liquidações por período, cedente e moeda.

O foco principal é garantir precisão financeira, rastreabilidade e consistência da liquidação.

---

## 2. Premissas adotadas

O enunciado possui ambiguidades intencionais. Para a implementação foram adotadas as seguintes premissas.

### Taxa base

Foi adotada uma taxa base fixa de:

`1,00% ao mês`

Esta taxa segue os casos de aferição fornecidos no desafio.

Em um sistema real, a origem da taxa deveria ser definida pelo negócio e possivelmente armazenada/configurada externamente.

### Spread por tipo de recebível

Os spreads utilizados são:

- Duplicata Mercantil: `1,5% a.m.`
- Cheque Pré-datado: `2,5% a.m.`

A escolha da regra de precificação é realizada por meio do padrão Strategy.

### Prazo

O prazo é calculado entre a data da operação e a data de vencimento.

A unidade utilizada é mês inteiro.

Quando existir uma fração residual de mês, o prazo é arredondado para o próximo mês inteiro.

Exemplo:

- 2 meses completos → prazo 2;
- 2 meses e alguns dias → prazo 3.

Recebíveis com vencimento igual ou anterior à data da operação não são aceitos.

### Precisão monetária

Valores financeiros são representados na aplicação utilizando `BigDecimal`.

Não são utilizados `float` ou `double` para cálculos monetários.

Os cálculos intermediários utilizam precisão `DECIMAL128`.

Valores monetários finais utilizam duas casas decimais e:

`RoundingMode.HALF_EVEN`

Na persistência:

- valores monetários utilizam `NUMERIC(19,2)`;
- taxas utilizam `NUMERIC(19,8)`.

A maior escala das taxas permite armazenar valores como `5.43210000` sem reduzir a precisão antes do cálculo.

### Câmbio

Os recebíveis são considerados denominados em BRL.

As moedas suportadas para pagamento são:

- BRL
- USD

Quando a liquidação ocorre em BRL, o valor presente é utilizado diretamente.

Quando ocorre em USD:

1. o valor presente em BRL é calculado;
2. o valor presente é arredondado para duas casas;
3. é localizada a taxa BRL/USD vigente;
4. o valor em BRL é dividido pela taxa;
5. o resultado em USD é arredondado para duas casas utilizando HALF_EVEN.

A taxa considerada vigente é a taxa mais recente cuja data/hora de vigência seja menor ou igual ao instante da operação.

Taxas futuras não são utilizadas.

### Liquidação

Um recebível inicia com status:

`PENDENTE`

Após uma liquidação concluída passa para:

`LIQUIDADO`

A alteração do recebível e a gravação da liquidação são executadas dentro da mesma transação.

Assim, uma falha durante o processo provoca rollback da operação, evitando liquidações parcialmente registradas.

### Idempotência

Toda requisição de liquidação deve possuir o header:

`Idempotency-Key`

O comportamento definido é:

- chave nova + recebível pendente → cria a liquidação e retorna `201 Created`;
- mesma chave + mesmo recebível + mesma moeda → retorna a liquidação existente sem criar outra e responde `200 OK`;
- mesma chave utilizada para outra operação → retorna `409 Conflict`;
- nova chave para recebível já liquidado → retorna `409 Conflict`.

A chave de idempotência e o recebível possuem restrições de unicidade na persistência.

### Auditabilidade

A liquidação registra um snapshot dos dados financeiros utilizados na operação:

- valor de face;
- taxa base;
- spread;
- prazo;
- valor presente;
- valor do deságio;
- moeda de pagamento;
- taxa de câmbio utilizada;
- data/hora de vigência da taxa;
- valor final;
- data/hora da liquidação;
- Idempotency-Key.

Não existem endpoints públicos de alteração ou exclusão de liquidações registradas.

---

## 3. Fórmula de precificação

A fórmula utilizada é:

`VP = VF / (1 + taxaBase + spread) ^ prazo`

Onde:

- `VP` = valor presente;
- `VF` = valor de face;
- `taxaBase` = 1% a.m.;
- `spread` = spread correspondente ao tipo de recebível;
- `prazo` = quantidade de meses inteiros.

O deságio é calculado por:

`Deságio = Valor de Face - Valor Presente`

---

## 4. Perguntas para o negócio

Em um projeto real, antes da implementação seriam necessárias algumas definições adicionais:

1. A taxa base continuará fixa ou deverá vir de uma fonte externa/configuração?
2. Como devem ser tratados dias residuais na determinação do prazo?
3. Todos os recebíveis continuarão denominados em BRL?
4. Quais moedas deverão ser suportadas futuramente?
5. A taxa de câmbio utilizada deve ser a vigente no momento da operação, no fechamento do dia ou em outro instante definido pelo negócio?
6. Taxas cambiais poderão ser corrigidas ou deverão ser sempre inseridas como novos registros?
7. Existe horário de corte para liquidações?
8. Como devem ser tratados finais de semana e feriados na determinação de vencimentos e taxas?
9. Existe necessidade de cancelamento ou estorno de uma liquidação?
10. Há limites de valor ou regras adicionais de risco por cedente?

---

## 5. Critérios de aceite

### Corretude

Os três golden cases fornecidos no desafio devem ser reproduzidos ao centavo:

- Duplicata de R$ 100.000,00 por 3 meses → R$ 92.859,94;
- Cheque de R$ 25.000,00 por 2 meses → R$ 23.337,77;
- Duplicata de R$ 100.000,00 por 3 meses com câmbio 5,4321 → US$ 17.094,67.

Esses cenários devem possuir testes automatizados.

### Integridade

- uma liquidação não pode ficar parcialmente registrada;
- um recebível não pode possuir duas liquidações;
- retries idempotentes não podem gerar novos registros;
- valores financeiros não podem utilizar ponto flutuante binário.

### API

A API deve:

- utilizar códigos HTTP semanticamente adequados;
- retornar `400` para requisições inválidas;
- retornar `404` para recursos inexistentes;
- retornar `409` para conflitos de estado;
- disponibilizar documentação OpenAPI/Swagger.

### Usabilidade

O frontend deve permitir:

- cadastrar cedentes e recebíveis;
- cadastrar taxas cambiais;
- simular a precificação;
- liquidar recebíveis;
- consultar o extrato.

As regras financeiras permanecem exclusivamente no backend.

### Desempenho

Como meta de uso interativo, as operações síncronas de cadastro, consulta e simulação devem responder em até `1 segundo` em ambiente de desenvolvimento, considerando:

- backend e frontend executando localmente;
- banco de dados acessível e sem indisponibilidade;
- ausência de carga concorrente significativa;
- sem contabilizar atrasos causados por rede externa ou infraestrutura remota fora da aplicação.

Essa meta é um critério de projeto e não representa um benchmark formal já executado.

Não foram introduzidos cache, filas ou processamento distribuído porque não são necessários para o volume e o escopo propostos.
