# HTTP Requests

Este diretório contém arquivos `.http` para testar as APIs do Ambassador Microservice.

## Arquivos

- **`users.http`** - Requisições para a API de usuários
- **`products.http`** - Requisições para a API de produtos  
- **`http-client.env.json`** - Variáveis de ambiente para diferentes ambientes

## Como usar

### IntelliJ IDEA / WebStorm
1. Abra qualquer arquivo `.http`
2. **Para selecionar ambiente:** Clique no dropdown no canto superior direito do editor e escolha o ambiente (development, test, staging, production)
3. Use o ícone de "play" ao lado de cada requisição para executá-la
4. Ou use `Ctrl+Enter` (Windows/Linux) ou `Cmd+Enter` (Mac)

### VS Code com REST Client
1. Instale a extensão "REST Client"
2. Abra qualquer arquivo `.http`
3. **Para selecionar ambiente:** Use `Ctrl+Shift+P` → "Rest Client: Switch Environment" → escolha o ambiente
4. Clique em "Send Request" que aparece acima de cada requisição

### Alternativa - Usar variável local
Se não conseguir selecionar o ambiente, descomente a linha de variável no início dos arquivos:
```
# @baseUrl = http://localhost:8080
```
Remova o `#` para ativar:
```
@baseUrl = http://localhost:8080
```

## Ambientes

O arquivo `http-client.env.json` define variáveis para diferentes ambientes:

- **development** - Ambiente local (porta 8080)
- **test** - Ambiente de testes (porta 8090)
- **staging** - Ambiente de homologação
- **production** - Ambiente de produção

### Usando variáveis de ambiente

Nas requisições, use `{{baseUrl}}` que será substituído automaticamente baseado no ambiente selecionado.

## APIs Disponíveis

### Users API (`/api/v1/users`)

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/v1/users/{id}` | Buscar usuário por ID |
| GET | `/api/v1/users` | Listar usuários com paginação |
| GET | `/api/v1/users/search` | Buscar usuários por query |
| POST | `/api/v1/users` | Criar novo usuário |
| PUT | `/api/v1/users/{id}` | Atualizar usuário |
| DELETE | `/api/v1/users/{id}` | Deletar usuário |

### Products API (`/api/v1/products`)

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/v1/products/{id}` | Buscar produto por ID |
| GET | `/api/v1/products` | Listar produtos com paginação |
| GET | `/api/v1/products/category/{category}` | Buscar produtos por categoria |
| POST | `/api/v1/products` | Criar novo produto |
| PUT | `/api/v1/products/{id}` | Atualizar produto |
| DELETE | `/api/v1/products/{id}` | Deletar produto |

## Exemplos de Teste

### Fluxo completo de usuário:
1. Criar usuário (`POST /api/v1/users`)
2. Buscar usuário criado (`GET /api/v1/users/{id}`)
3. Atualizar usuário (`PUT /api/v1/users/{id}`)
4. Buscar usuários (`GET /api/v1/users/search`)
5. Deletar usuário (`DELETE /api/v1/users/{id}`)

### Fluxo completo de produto:
1. Criar produto (`POST /api/v1/products`)
2. Buscar produto criado (`GET /api/v1/products/{id}`)
3. Buscar produtos por categoria (`GET /api/v1/products/category/{category}`)
4. Atualizar produto (`PUT /api/v1/products/{id}`)
5. Listar produtos (`GET /api/v1/products`)
6. Deletar produto (`DELETE /api/v1/products/{id}`)

## Estrutura das Respostas

Todas as respostas seguem o padrão:

```json
{
  "success": true,
  "data": { ... },
  "message": "Success message",
  "error": null
}
```

Em caso de erro:

```json
{
  "success": false,
  "data": null,
  "message": "Error message",
  "error": "Detailed error information"
}
```