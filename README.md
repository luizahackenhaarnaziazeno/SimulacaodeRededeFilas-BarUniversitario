 <div align="center">

## Simulação de Rede de Filas: Bar Universitário
</div>

## 🎯 Sobre o Projeto 
Este projeto implementa uma simulação de eventos discretos para analisar o desempenho de uma rede de filas em um "Bar Universitário". 
O sistema modela o fluxo de clientes através de três etapas de atendimento (filas): Escolha do Pedido, Pagamento e Retirada/Preparo dos Lanches. 
O objetivo é calcular índices de desempenho (utilização, vazão efetiva, probabilidades de estado e tempo médio de resposta) simulando a passagem de 20.000 clientes.

O projeto conta com dois cenários:

- Modelo Inicial (T2ModeloInicial): Configuração base com 2 servidores na escolha, 1 no pagamento e 2 na retirada (gargalo potencial com capacidade máxima
  de 5 clientes na fila).
- Modelo Melhorado (T2ModeloMelhorado): Otimização do sistema adicionando um terceiro servidor na fila de retirada (F3) para mitigar o gargalo e melhorar o
 tempo de resposta.

 ## ⚙️ Pré-requisitos e Dependências
- Java Development Kit (JDK): Versão 8 ou superior.
- Nenhuma biblioteca externa é necessária, pois a simulação utiliza apenas classes nativas do Java (java.util.PriorityQueue, java.util.Random, java.util.HashMap, etc.).

## 📁 Estrutura de Diretórios
- T2ModeloInicial.java: Contém o código-fonte da simulação com a configuração original dos servidores.
- T2ModeloMelhorado.java: Contém a versão otimizada com a adição de um servidor extra na fila 3 (Retirada dos Lanches).

## 🚀 Como Executar

1. Abra o terminal na pasta onde os arquivos foram salvos.
2. Compile os arquivos Java executando:
   ```bash
   javac T2ModeloInicial.java T2ModeloMelhorado.java
   ```
3. Para rodar o modelo base, execute:
   ```bash
   java T2ModeloInicial
   ```
5. Para rodar o modelo otimizado, execute:
   ```bash
   java T2ModeloMelhorado
   ```   
## 🛠️ Pipeline de Execução

A simulação é conduzida por um escalonador de eventos baseado em uma Fila de Prioridade (`PriorityQueue`) ordenado pelo tempo cronológico de ocorrência (`eventTime`). O ciclo de vida da execução ocorre da seguinte forma:

* **Inicialização**: As três filas são instanciadas com suas capacidades e números de servidores específicos e o primeiro evento de chegada externa é agendado.
* **Loop Principal**: O sistema consome eventos da fila de prioridade enquanto o limite de clientes (20.000) não for atingido. A cada evento, o tempo de permanência no estado anterior é acumulado para posterior cálculo de estatísticas.
* **Processamento de Chegadas (`ARRIVAL_EXTERNAL`)**: O cliente entra na fila 1 caso não esteja cheia, ou é contabilizado como "cliente perdido por bloqueio". Um novo tempo de chegada é agendado utilizando distribuição Uniforme.
* **Processamento de Partidas e Roteamento (`DEPARTURE`)**: 
  * Clientes que finalizam a fila 1 vão para a fila 2.
  * Clientes que finalizam a fila 2 vão para a fila 3.
  * Clientes que finalizam a fila 3 possuem um roteamento probabilístico: 20% de chance de retornar à fila 1 (novo pedido/problema) e 80% de chance de finalizar o atendimento (saída final).
* **Exibição de Resultados**: Ao término dos eventos, o programa invoca o método `exibirResultadosM9`, calculando e imprimindo no terminal as métricas analíticas (rho, lambda, população média e tempo de resposta).

<div align="center">


# Autora:

| [<img loading="lazy" src="https://avatars.githubusercontent.com/u/142232479?v=4" width=115><br><sub>Luiza Hackenhaar Naziazeno</sub>](https://github.com/luizahackenhaarnaziazeno)|
| :---: |
