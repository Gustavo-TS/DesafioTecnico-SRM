# SRM Credit Engine — Uso de Inteligência Artificial

## 1. Como a IA foi utilizada

A IA foi utilizada como ferramenta de apoio durante o desenvolvimento do desafio.

O objetivo não foi delegar decisões de negócio ou aceitar código automaticamente, mas acelerar análise, revisão e implementação de partes do projeto que depois foram verificadas manualmente.

Os principais usos foram:

- interpretar e decompor os requisitos do desafio;
- estruturar o plano de implementação do backend e frontend;
- revisar a separação entre Controller, Service, Repository e Strategy;
- discutir alternativas para idempotência e semântica HTTP;
- revisar regras de precisão financeira com `BigDecimal`;
- apoiar a criação e atualização dos testes dos golden cases;
- revisar integração entre frontend e backend;
- apoiar refatorações pequenas e revisão final do código;
- estruturar a documentação da entrega.

## 2. Exemplos de prompts estratégicos

Os prompts foram usados principalmente para revisar decisões e executar mudanças pequenas e verificáveis.

Exemplos de direcionamento utilizados:

### Planejamento do backend

> Analise o desafio e proponha uma implementação simples para nível Júnior, priorizando corretude, clareza, precisão financeira, idempotência e auditabilidade. Evite requisitos de Pleno/Sênior que não sejam necessários.

### Revisão da precificação

> Valide se a implementação reproduz os golden cases exatamente, usando BigDecimal, HALF_EVEN e arredondamento somente no momento definido pela regra.

### Revisão de idempotência

> Revise o fluxo de liquidação para garantir que a mesma Idempotency-Key não gere uma nova liquidação, que reutilizações incompatíveis retornem conflito e que os códigos HTTP sejam semânticos.

### Integração frontend/backend

> Confira os endpoints e DTOs existentes antes de alterar o frontend. Não invente campos nem regras financeiras no cliente.

### Revisão final

> Compare a implementação com o enunciado e identifique apenas lacunas reais. Não proponha funcionalidades extras apenas para aumentar o volume da entrega.

## 3. Caso concreto em que a IA errou

Durante a configuração da conexão com PostgreSQL/Neon, uma sugestão de configuração colocou aspas em valores do arquivo `.env`, por exemplo:

```env
PGSSLMODE='require'
```

O valor chegou à aplicação incluindo as aspas, fazendo com que a configuração fosse interpretada incorretamente.

### Como o erro foi detectado

O problema apareceu durante a execução real da aplicação, e não em uma análise teórica.

A configuração foi comparada com o valor efetivamente lido pelo driver e corrigida para:

```env
PGSSLMODE=require
```

Esse caso reforçou a decisão de não considerar uma sugestão da IA válida apenas porque o código ou configuração parecia plausível.

## 4. Outro caso de revisão importante

Durante a implementação dos filtros do extrato, uma consulta com parâmetros opcionais apresentou erro no PostgreSQL quando os filtros eram nulos:

```text
could not determine data type of parameter
```

A solução inicial precisou ser revista.

A consulta foi ajustada para tratar os parâmetros opcionais de forma compatível com PostgreSQL e depois validada manualmente com:

- consulta sem filtros;
- filtro por moeda;
- filtro por cedente;
- filtro por período;
- combinação de filtros.

A correção só foi considerada concluída depois do teste real do endpoint.

## 5. Como as respostas da IA foram verificadas

As sugestões foram verificadas usando diferentes formas de validação:

- execução local da aplicação;
- Swagger/Postman para os endpoints;
- `mvn clean test`;
- `npm run build`;
- comparação com os golden cases;
- inspeção do banco e dos retornos da API;
- revisão manual dos diffs antes dos commits;
- testes de erro, idempotência e filtros.

Um exemplo foi a liquidação idempotente.

A implementação inicialmente retornava `201 Created` inclusive quando uma requisição com a mesma `Idempotency-Key` apenas retornava uma liquidação existente.

O comportamento funcional estava correto, mas a semântica HTTP não estava.

Após revisão, o fluxo foi ajustado para:

- nova liquidação → `201 Created`;
- retry idempotente → `200 OK`;
- reutilização incompatível da chave → `409 Conflict`.

## 6. O que não foi delegado à IA

Algumas decisões foram mantidas sob responsabilidade direta durante o desenvolvimento:

- quais premissas do domínio seriam adotadas;
- quais requisitos seriam implementados ou deliberadamente cortados;
- decisão de manter o projeto dentro do escopo Júnior;
- aceitação final dos cálculos financeiros;
- validação dos golden cases;
- definição do comportamento esperado de idempotência;
- decisão de não adicionar Docker, paginação, optimistic locking, observabilidade ou outras funcionalidades apenas para aumentar o volume;
- revisão final dos commits;
- execução dos testes e validação ponta a ponta;
- capacidade de explicar e modificar o código na defesa técnica.

## 7. Princípio adotado

A IA foi tratada como uma ferramenta de engenharia, não como fonte de verdade.

O fluxo utilizado foi:

```text
requisito
→ proposta/apoio da IA
→ revisão manual
→ implementação
→ teste
→ correção, se necessária
→ commit
```

A decisão final sobre o que permaneceria no projeto foi sempre baseada no comportamento real da aplicação, nos requisitos do desafio e na capacidade de defender tecnicamente a solução.
