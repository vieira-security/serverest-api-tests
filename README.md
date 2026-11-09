# ServeRest – Testes de API com RestAssured, JUnit 5 e Postman

[![API Tests](https://github.com/vieira-security/serverest-api-tests/actions/workflows/api-tests.yml/badge.svg)](https://github.com/vieira-security/serverest-api-tests/actions/workflows/api-tests.yml)
![Java](https://img.shields.io/badge/Java-17%2B-007396?logo=openjdk&logoColor=white)
![RestAssured](https://img.shields.io/badge/RestAssured-5.5-4CAF50)
![JUnit 5](https://img.shields.io/badge/JUnit-5-25A162?logo=junit5&logoColor=white)
![Postman](https://img.shields.io/badge/Postman-Newman-FF6C37?logo=postman&logoColor=white)

Suíte de testes automatizados para a API REST [ServeRest](https://serverest.dev), uma API pública brasileira que simula uma loja virtual (usuários, login, produtos e carrinhos).

O projeto tem duas camadas:

- **RestAssured + JUnit 5 (Java):** 35 testes cobrindo contrato, regras de negócio, autenticação/autorização e valores-limite.
- **Postman + Newman:** coleção com um fluxo E2E de API (usuário → login → produto → carrinho → limpeza), 32 asserções.

As duas rodam no **GitHub Actions** a cada push e pull request, contra uma instância da ServeRest que sobe dentro do próprio job. Assim os testes não dependem da disponibilidade do serverest.dev.

## O que é testado

| Recurso | Tipo de teste | Exemplos |
|---|---|---|
| `POST /login` | Funcional, partição de equivalência, contrato | Login válido com token Bearer; senha errada, e-mail inexistente e senha com caixa diferente (401); campos em branco e e-mail inválido (400) |
| `/usuarios` | CRUD, contrato, negativos | Cadastro com conferência via GET; e-mail duplicado; campos obrigatórios; ID inexistente e ID fora do formato; edição e exclusão persistidas |
| `/produtos` | Autenticação, autorização, valor-limite (BVA) | Sem token e token adulterado (401); usuário não admin (403); nome duplicado; preço 0 inválido e 1 válido; quantidade -1 inválida e 0 válida; preço decimal |
| `/carrinhos` | Integração entre recursos | Carrinho baixa o estoque; só 1 carrinho por usuário; quantidade acima do estoque; produto em carrinho não pode ser excluído; cancelar compra devolve o estoque; concluir compra mantém a baixa |

### Casos de teste (RestAssured)

<details>
<summary>Ver os 35 casos</summary>

| ID | Cenário | Esperado |
|---|---|---|
| CT-L01 | Login com credenciais válidas | 200, token `Bearer`, contrato válido |
| CT-L02 | Login com credenciais inválidas (3 partições) | 401, sem token |
| CT-L03 | Login com campos em branco | 400, mensagem por campo |
| CT-L04 | Login com e-mail em formato inválido | 400 |
| CT-U01 | Cadastrar usuário | 201 e usuário disponível no GET por ID |
| CT-U02 | Cadastrar com e-mail duplicado | 400 |
| CT-U03 | Cadastrar sem campos obrigatórios | 400, mensagem por campo |
| CT-U04 | Listar usuários | Contrato válido e `quantidade` igual ao tamanho da lista |
| CT-U05 | Buscar usuário por e-mail (query param) | Retorna só o usuário filtrado |
| CT-U06 | Buscar ID inexistente | 400 |
| CT-U07 | Buscar ID fora do formato | 400 |
| CT-U08 | Editar usuário | 200 e alteração persistida |
| CT-U09 | Excluir usuário | 200 e usuário deixa de existir |
| CT-U10 | Excluir usuário inexistente | 200, "Nenhum registro excluído" |
| CT-P01 | Cadastrar produto sem token | 401 |
| CT-P02 | Cadastrar produto com token adulterado | 401 |
| CT-P03 | Cadastrar produto com usuário comum | 403 |
| CT-P04 | Cadastrar produto com admin | 201 e produto persistido |
| CT-P05 | Cadastrar produto com nome duplicado | 400 |
| CT-P06 | Preço e quantidade nos limites válidos (3 combinações) | 201 |
| CT-P07 | Preço 0 | 400 |
| CT-P08 | Quantidade -1 | 400 |
| CT-P09 | Preço decimal | 400 |
| CT-P10 | Excluir produto | 200 |
| CT-C01 | Criar carrinho | 201 e estoque baixado |
| CT-C02 | Segundo carrinho para o mesmo usuário | 400 |
| CT-C03 | Quantidade acima do estoque | 400 e estoque inalterado |
| CT-C04 | Excluir produto que está em carrinho | 400 |
| CT-C05 | Cancelar compra | 200 e estoque devolvido |
| CT-C06 | Concluir compra | 200 e estoque mantém a baixa |
| CT-C07 | Concluir compra sem carrinho | Mensagem de carrinho não encontrado |

Testes parametrizados contam uma vez por linha, por isso a tabela tem 31 IDs e a suíte executa 35 testes.

</details>

## Estrutura

```
src/test/java/br/com/gabrielvieira/serverest/
├── base/BaseTest.java        # configuração do RestAssured e helpers de pré-condição (cadastro, login, token)
├── factory/DataFactory.java  # massa de dados única por teste (e-mails e nomes com UUID)
├── model/                    # payloads como records Java (Usuario, Produto, Login)
└── tests/                    # LoginTest, UsuariosTest, ProdutosTest, CarrinhosTest
src/test/resources/schemas/   # JSON Schemas usados na validação de contrato
postman/                      # coleção e environment do Postman
.github/workflows/            # pipeline de CI
```

### Decisões de projeto

- **Testes independentes:** cada teste cria a própria massa de dados (usuário, produto, token). Nenhum teste depende da ordem de execução ou do resultado de outro.
- **Validar persistência, não só a resposta:** depois de um POST, PUT ou DELETE, o teste consulta o recurso de novo para confirmar que a mudança aconteceu de fato (inclusive o estoque, no fluxo de carrinho).
- **Contrato com JSON Schema:** as respostas de login, usuário e lista de usuários são validadas contra schemas, o que pega mudança de tipo ou campo removido.
- **Técnicas de design de teste (ISTQB):** partição de equivalência no login e análise de valor-limite em preço e quantidade de produto.
- **Log só na falha:** o RestAssured imprime request e response apenas quando uma validação falha, para deixar o relatório limpo.

## Como rodar

Pré-requisitos: Java 17+, Maven e Node.js 18+.

```bash
npm install            # instala a ServeRest e o Newman
npm run api:start      # sobe a API em http://localhost:3000 (deixe este terminal aberto)
```

Em outro terminal:

```bash
mvn test               # testes RestAssured + JUnit 5
npm run test:postman   # coleção Postman via Newman
```

Para rodar contra outra URL (por exemplo, a instância pública):

```bash
mvn test -DbaseUrl=https://serverest.dev
```

A coleção também pode ser importada direto no Postman: importe os dois arquivos da pasta `postman/` e selecione o environment **ServeRest - Local**.

## Stack

Java 17 · RestAssured 5.5 · JUnit 5 · Hamcrest · JSON Schema Validator · Jackson · Maven · Postman · Newman · GitHub Actions
