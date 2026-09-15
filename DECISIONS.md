# SRM Credit Engine — Decisões Técnicas

Este documento registra decisões de implementação e, principalmente, escolhas de escopo feitas para manter o projeto correto, simples e defensável para o nível Júnior do desafio.

## 1. Arquitetura

Foi adotado um monólito em camadas no backend, separado em:

- controller;
- service;
- repository;
- model/entity;
- DTO;
- strategy;
- exception.

A escolha prioriza clareza, baixo acoplamento entre responsabilidades e facilidade de manutenção.

Não foram criados microsserviços, pois o domínio e o escopo do desafio não justificam a complexidade adicional de comunicação distribuída, observabilidade e operação.

## 2. Persistência relacional

Foi utilizado PostgreSQL com Spring Data JPA/Hibernate.

A escolha por banco relacional atende bem às necessidades de:

- integridade referencial;
- relacionamentos entre cedente, recebível e liquidação;
- restrições de unicidade;
- transações ACID.

Não foi utilizado NoSQL porque o modelo possui relacionamentos claros e exige consistência transacional forte.

## 3. Precisão financeira

Valores monetários e taxas são representados com `BigDecimal`.

A decisão evita erros de precisão de ponto flutuante binário em cálculos financeiros.

Os cálculos intermediários usam `MathContext.DECIMAL128` e o resultado monetário final é arredondado com `RoundingMode.HALF_EVEN`.

## 4. Strategy para precificação

Foi utilizado o padrão Strategy para separar a regra de precificação por tipo de recebível.

Atualmente existem estratégias para:

- Duplicata Mercantil;
- Cheque Pré-datado.

Isso permite adicionar novos tipos de recebível sem concentrar regras condicionais em um único serviço.

## 5. Taxa base

A taxa base foi mantida fixa em `1% a.m.`.

Essa escolha segue os golden cases do desafio e evita introduzir uma camada de configuração que não agrega valor ao escopo atual.

Em um sistema real, a taxa base poderia ser parametrizada ou obtida de uma fonte externa.

## 6. Gestão de câmbio

Foi adotado cadastro manual de taxas de câmbio.

Cada taxa possui data/hora de vigência e o sistema utiliza a taxa mais recente cuja vigência seja menor ou igual ao instante da operação.

Não foi implementada integração com provedor externo porque o desafio permite endpoint manual ou integração mockada, e a solução manual é suficiente para o escopo.

## 7. Liquidação transacional

A liquidação é executada em método `@Transactional`.

Dentro da mesma transação são realizadas:

- alteração do status do recebível;
- gravação da liquidação.

Essa decisão evita estados parciais em caso de falha.

## 8. Auditabilidade por snapshot

A liquidação grava um snapshot dos dados financeiros usados no momento da operação.

São persistidos, entre outros:

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
- timestamp da liquidação.

Assim, alterações futuras em regras ou taxas não mudam o histórico da operação já concluída.

## 9. Imutabilidade da liquidação

Não existem endpoints `PUT`, `PATCH` ou `DELETE` para liquidações.

A imutabilidade é garantida pelo fluxo público da aplicação.

Não foi adicionada uma camada extra de imutabilidade JPA porque não foi necessária para atender o escopo proposto.

## 10. Idempotência

A liquidação exige o header `Idempotency-Key`.

A chave é persistida e possui restrição de unicidade.

Comportamento:

- requisição nova cria a liquidação;
- retry com mesma chave, recebível e moeda retorna a liquidação já existente;
- reutilização da chave em outra operação gera conflito;
- um recebível já liquidado não pode ser liquidado novamente.

Essa abordagem evita duplicidade causada por retry de rede ou duplo clique.

## 11. API REST e erros

A API utiliza códigos HTTP semânticos:

- `200 OK`;
- `201 Created`;
- `400 Bad Request`;
- `404 Not Found`;
- `409 Conflict`.

O tratamento de erros é centralizado em `GlobalExceptionHandler`.

OpenAPI/Swagger foi utilizado para facilitar teste e documentação dos endpoints.

## 12. Frontend

O frontend foi desenvolvido com React, TypeScript e Vite.

Foi mantida uma estrutura simples, sem:

- Redux;
- Zustand;
- Context global;
- React Router.

O estado local é suficiente para o tamanho da aplicação.

As regras financeiras permanecem no backend. O frontend apenas envia dados, recebe respostas e apresenta os resultados.

## 13. Listagem simples

O extrato utiliza uma listagem simples com filtros por:

- período;
- cedente;
- moeda.

Não foi implementada paginação server-side porque, para o nível Júnior, o próprio desafio informa que uma listagem simples atende.

## 14. Itens deliberadamente não implementados

Os itens abaixo não foram implementados por pertencerem a níveis superiores, por não serem necessários ao escopo atual ou por adicionarem complexidade sem ganho proporcional:

- Docker e Docker Compose;
- optimistic locking;
- teste de concorrência;
- observabilidade estruturada;
- métricas;
- tracing;
- circuit breaker;
- retry de provedor externo;
- CI/CD;
- paginação server-side;
- microsserviços;
- autenticação/autorização;
- estado global no frontend;
- gráficos e dashboard de KPIs.

Esses itens foram conscientemente priorizados abaixo de corretude, clareza, testes dos golden cases, integração ponta a ponta e capacidade de defesa técnica.

## 15. Critério de priorização

A principal decisão de escopo foi evitar volume de código sem necessidade.

A implementação priorizou:

1. corretude do cálculo;
2. precisão monetária;
3. consistência da liquidação;
4. idempotência;
5. auditabilidade;
6. API clara;
7. integração funcional com o frontend;
8. testes e documentação.

O objetivo foi entregar uma solução pequena, coerente e fácil de explicar, em vez de adicionar funcionalidades de níveis superiores sem necessidade.
