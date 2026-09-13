# AI_USAGE.md

## Uso de Inteligência Artificial no desenvolvimento

A IA foi utilizada como ferramenta de apoio durante o desenvolvimento do desafio, principalmente para revisar decisões técnicas, estruturar etapas de implementação, validar configurações e discutir alternativas de arquitetura.

O uso da IA não substituiu a validação manual do código e das regras de negócio. As sugestões geradas foram revisadas antes de serem incorporadas ao projeto.

## Uso estratégico

A IA foi utilizada principalmente para:

- estruturar o planejamento inicial do backend e frontend;
- revisar a modelagem das entidades e responsabilidades das classes;
- discutir a separação entre Controller, Service, Repository e Strategy;
- revisar configurações do Spring Boot e PostgreSQL;
- apoiar a interpretação dos requisitos do desafio;
- revisar decisões relacionadas a precisão financeira, BigDecimal e arredondamento;
- ajudar na preparação dos casos de teste definidos pelo desafio.

## Exemplo de erro identificado

Durante a configuração da conexão com o PostgreSQL/Neon, foi sugerido o uso de variáveis em um arquivo `.env`.

Na primeira configuração, os valores foram definidos com aspas, por exemplo:

```env
PGSSLMODE='require'