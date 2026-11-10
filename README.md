# Sistema de Gerenciamento de Biblioteca

Projeto em Java para cadastro de livros e usuários, empréstimos e devoluções, consulta de atrasos, cálculo de multas e estatísticas. Os dados ficam em memória.

## Integrantes

- João Paulo X. N. Santos

## Requisitos

- JDK 25 ou superior, para executar o método `main` simplificado usado em `src/Main.java`.

## Compilar e executar

Na raiz do projeto:

```bash
mkdir -p out
find src -name '*.java' -print0 | xargs -0 javac -d out
java -cp out Main
```

O programa imprime um exemplo de cadastro, empréstimo, devolução, filtragem de livros, comparação entre processamento imperativo e funcional, relatório de multas e estatísticas.

Para executar os testes de comportamento:

```bash
mkdir -p out
find src -name '*.java' -print0 | xargs -0 javac -d out
java -cp out tests.LibraryBehaviorTest
```

A suíte executa seis grupos de testes: empréstimo/devolução e multas; filtragem e estatísticas; concorrência no empréstimo; validações de cadastro e empréstimo; validações de devolução e exclusão de empréstimos devolvidos dos atrasos; e imutabilidade das listas retornadas. Em caso de sucesso, imprime `All 6 library behavior test groups passed.`; uma falha interrompe a execução com uma `AssertionError` descritiva.

## Funcionalidades

- Cadastro de livros e usuários com validação de identificadores/matrículas duplicados.
- Empréstimos com prazo de 15 dias; cada exemplar só pode ter um empréstimo ativo.
- Devolução vinculada ao identificador do empréstimo, registro da data efetiva e liberação do livro.
- Consulta de empréstimos por identificador e usuário, além de filtragem por categoria.
- Identificação de empréstimos ativos atrasados a partir de uma data de referência.
- Relatório de multas dos empréstimos ativos em atraso, calculadas a R$ 2,00 por dia.
- Estatísticas de livros por categoria, empréstimos por usuário, empréstimos ativos, atrasados e valor total das multas.

## Organização e paradigmas

As classes `Book`, `User`, `Loan` e `Library` representam as entidades e operações do domínio. Identificadores, dados bibliográficos, matrículas e datas de empréstimo são imutáveis depois da criação. A classe `Library` coordena as operações com efeitos colaterais, como registrar empréstimos e devoluções; as operações de consulta e cálculo ficam em `LibraryFunctions`.

`LibraryFunctions` fornece funções sem efeitos colaterais observáveis: `filterBooksByCategory` filtra livros sem modificar a lista original; `calculateFine` retorna sempre o mesmo resultado para os mesmos dias e valor diário; `isOverdue`, `overdueDays` e `findOverdueLoans` recebem explicitamente a data de referência. O relatório aplica `filter` para obter empréstimos ativos atrasados, `map` para produzir cada multa e `reduce` para somar o total. As estatísticas também usam `reduce` para contar empréstimos ativos e atrasados.

A filtragem de livros por categoria está implementada em versões funcional (`filterBooksByCategory`) e imperativa (`filterBooksByCategoryImperative`), produzindo listas equivalentes sem alterar a coleção de entrada.

Os métodos de gerenciamento da mesma instância de `Library` são sincronizados. Isso serializa tentativas simultâneas de empréstimo e evita que duas chamadas concedam o mesmo livro com base no estado disponível. Funções puras e imutabilidade, isoladamente, não impedem essa condição de corrida; a coordenação da operação mutável é necessária. Esta proteção é em memória e vale para uma instância do processo.
