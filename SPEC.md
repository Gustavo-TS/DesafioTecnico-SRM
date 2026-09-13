# SPEC.md — SRM Credit Engine

## 1. Objetivo

O SRM Credit Engine tem como objetivo receber recebíveis, calcular seu valor presente com base no tipo do ativo, prazo e taxa aplicável, realizar conversões cambiais quando necessário e registrar a liquidação de forma consistente e auditável.

A solução será desenvolvida com foco em corretude financeira, simplicidade e clareza.

---

## 2. Premissas adotadas

### 2.1 Moeda do recebível

Nesta implementação, todos os recebíveis são considerados originalmente denominados em BRL.

A moeda de pagamento poderá ser:

- BRL
- USD

Quando o pagamento for realizado em USD, o valor presente será calculado inicialmente em BRL e posteriormente convertido utilizando a taxa de câmbio definida para a operação.

Essa decisão foi tomada com base no cenário apresentado pelo desafio, que descreve o fluxo cross-currency como um título em BRL com pagamento em USD.

---

### 2.2 Prazo

Para a implementação principal, o prazo da operação será considerado em meses inteiros.

O prazo será calculado entre a data da operação e a data de vencimento do recebível.

Para os golden cases fornecidos pelo desafio, serão utilizados exatamente os prazos especificados nos casos de aferição.

Em um sistema real, seria necessário validar com a área de negócio se o cálculo deve utilizar:

- meses completos;
- dias corridos;
- dias úteis;
- ou alguma convenção financeira específica.

---

### 2.3 Taxa base

A taxa base será considerada uma configuração da regra de precificação.

Para os golden cases será utilizada:

1,00% ao mês.

A taxa será representada na aplicação como valor decimal:

0.01

A taxa base não será armazenada como `double` ou `float`.

---

### 2.4 Spread

Cada tipo de recebível possui seu próprio spread:

- Duplicata Mercantil: 1,5% a.m.
- Cheque Pré-datado: 2,5% a.m.

A definição do spread será implementada utilizando o padrão Strategy.

---

### 2.5 Fórmula de precificação

O valor presente será calculado utilizando:

Valor Presente = Valor de Face / (1 + Taxa Base + Spread)^Prazo

O cálculo será realizado com `BigDecimal`.

---

### 2.6 Arredondamento

Será utilizado:

- `RoundingMode.HALF_EVEN`
- 2 casas decimais
- arredondamento apenas no resultado final

Valores intermediários manterão precisão suficiente para evitar perda de informação.

Para operações em USD:

1. calcula-se o valor presente em BRL;
2. arredonda-se o valor presente em BRL;
3. realiza-se a conversão cambial;
4. arredonda-se o valor final em USD.

---

### 2.7 Taxa de câmbio

Cada taxa de câmbio possuirá:

- moeda de origem;
- moeda de destino;
- valor da taxa;
- data e hora de vigência.

Para uma liquidação, será utilizada a taxa vigente mais recente no momento da operação.

A taxa efetivamente utilizada será registrada dentro da própria liquidação para preservar a auditabilidade histórica.

Uma alteração futura na tabela de câmbio não poderá alterar os valores de uma liquidação já realizada.

---

## 3. Precisão numérica

### Aplicação

Valores financeiros serão representados com:

`BigDecimal`

Não serão utilizados:

- `float`
- `double`

para cálculos financeiros.

### Banco de dados

Valores monetários:

`NUMERIC(19,2)`

Taxas e câmbio:

`NUMERIC(19,8)`

Datas de vencimento:

`DATE`

Timestamps:

`TIMESTAMP WITH TIME ZONE`

Identificadores:

`UUID`

---

## 4. Liquidação e integridade

A liquidação deverá ser executada de forma transacional.

As seguintes operações devem ocorrer na mesma transação:

1. validar o recebível;
2. calcular o valor da operação;
3. registrar a liquidação;
4. alterar o recebível para o status LIQUIDADO.

Se qualquer etapa falhar, nenhuma alteração deverá permanecer persistida.

---

## 5. Idempotência

O endpoint de liquidação deverá aceitar uma chave de idempotência.

A chave será armazenada na liquidação e possuirá restrição `UNIQUE`.

Além disso, cada recebível poderá possuir no máximo uma liquidação.

Essas duas restrições protegem contra:

- repetição da mesma requisição;
- duplo clique;
- retry de rede;
- tentativa de liquidar novamente o mesmo recebível.

---

## 6. Auditabilidade

Uma liquidação registrada será considerada imutável.

Serão registrados no momento da liquidação:

- valor de face;
- taxa base;
- spread;
- prazo;
- valor presente;
- valor do deságio;
- moeda de pagamento;
- taxa de câmbio utilizada;
- data/hora da taxa de câmbio;
- valor final;
- data/hora da liquidação.

Não haverá endpoint de alteração de uma liquidação existente.

---

## 7. Perguntas para a área de negócio

Em um projeto real, seriam validadas as seguintes questões:

- O prazo deve ser calculado em meses, dias corridos ou dias úteis?
- Qual é a origem oficial da taxa base?
- A taxa base pode variar por fundo, cedente ou operação?
- Qual taxa de câmbio deve ser utilizada: última disponível ou taxa da data da operação?
- É permitido liquidar recebíveis vencidos?
- É permitido cancelar ou estornar uma liquidação?
- Como deve funcionar o arredondamento em cenários não cobertos pelos golden cases?
- Existem outras moedas além de BRL e USD?
- Existem outros tipos de recebíveis além de duplicata e cheque?

---

## 8. Critérios de aceite

### Corretude

- Os três golden cases devem ser reproduzidos exatamente.
- Nenhum cálculo financeiro utilizará ponto flutuante binário.
- Um recebível não poderá ser liquidado mais de uma vez.

### Integridade

- A liquidação será transacional.
- Uma falha não poderá gerar liquidação parcial.
- A chave de idempotência deverá impedir duplicação da mesma operação.

### Auditabilidade

- Toda liquidação deverá registrar os valores utilizados no cálculo.
- Liquidações não poderão ser alteradas.

### API

- Endpoints REST deverão utilizar códigos HTTP semanticamente adequados.
- A API será documentada utilizando OpenAPI/Swagger.

### Usabilidade

- O frontend deverá permitir simular uma operação antes de liquidá-la.
- O usuário deverá visualizar valor de face, deságio e valor líquido.

### Desempenho

Para o escopo do desafio, a aplicação deverá responder adequadamente a operações individuais e listagens simples, sem otimizações prematuras.