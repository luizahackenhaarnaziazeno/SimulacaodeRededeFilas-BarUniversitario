import java.util.PriorityQueue;
import java.util.Random;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Comparator;

public class T2ModeloMelhorado {

    // --- CONFIGURAÇÃO DO MODELO INICIAL (PARÂMETROS DE ENTRADA) ---
    private static final int NUM_CLIENTES_SIM = 20000; 
    private static final Random random = new Random(42); 

    // Tempos de serviço/chegada variam entre [min, max] em minutos (Distribuição G/G).
    private static final double[] MU_1_TEMPOS = {0.3, 0.5}; // F1: Escolha (Rápido)
    private static final double[] MU_2_TEMPOS = {0.1, 0.3}; // F2: Pagamento (Muito Rápido)
    private static final double[] MU_3_TEMPOS = {1.5, 3.0}; // F3: Retirada/Preparo (LENTO - Gargalo Potencial)
    private static final double[] CH_TEMPOS = {0.2, 0.3};  // Tempo entre chegadas externas

    // Servidores e Capacidade
    private static final int SERVERS_F1 = 2; 
    private static final int SERVERS_F2 = 1; 
    private static final int SERVERS_F3 = 3; // Modelo MELHORADO: 3 servidores em F3
    private static final int CAPACITY_F3 = 5; // Capacidade máxima de espera em F3

    // ROTEAMENTO (RETORNO OBRIGATÓRIO PARA O T2)
    private static final double P_3_TO_1 = 0.20; // 20% de RETORNO de F3 para F1

    // --- Variáveis de Estado Global e Rastreamento ---
    private static double globalTime = 0.0;
    private static int totalArrivalsExternal = 0;
    private static int totalLostCustomers = 0;
    private static final List<Double> systemResponseTimes = new LinkedList<>(); 
    private static int clienteIdCounter = 0;

    // --- Classes Auxiliares ---

    static class Fila {
        String name; int servers; int capacity; int customersInSystem = 0;
        PriorityQueue<Double> serviceFinishes = new PriorityQueue<>();
        double lastEventTime = 0.0; HashMap<Integer, Double> accumulatedTimeByState = new HashMap<>();
        int totalDeparturesFila = 0; double accumulatedServiceTime = 0.0;

        public Fila(String name, int servers, int capacity) {
            this.name = name; this.servers = servers; this.capacity = capacity;
            for(int i = 0; i < servers; i++) this.serviceFinishes.add(0.0);
        }
        public boolean isFull() { return (capacity != -1 && customersInSystem >= (capacity + servers)); }
    }

    static class Cliente {
        int id; double arrivalTime;
        public Cliente() { this.id = ++clienteIdCounter; this.arrivalTime = globalTime; }
    }

    static class Event implements Comparable<Event> {
        final double eventTime; final String eventType; final String targetFila; Cliente customer;
        public Event(double eventTime, String eventType, String targetFila, Cliente customer) {
            this.eventTime = eventTime; this.eventType = eventType; this.targetFila = targetFila; this.customer = customer;
        }
        @Override public int compareTo(Event other) { return Double.compare(this.eventTime, other.eventTime); }
    }


    // ------------------------------------------------------------------------------------------------
    // --- LÓGICA PRINCIPAL (MAIN) ---
    // ------------------------------------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("------------------------------------------------------------------");
        System.out.println("   SIMULAÇÃO DE REDE DE FILAS: BAR UNIVERSITÁRIO (MODELO MELHORADO) ");
        System.out.println("------------------------------------------------------------------");

        // Inicializa as Filas
        Fila fila1 = new Fila("Fila_1_EscolhaDoPedido", SERVERS_F1, -1);
        Fila fila2 = new Fila("Fila_2_Pagamentos", SERVERS_F2, -1);
        Fila fila3 = new Fila("Fila_3_RetiradaDosLanches", SERVERS_F3, CAPACITY_F3);
        
        HashMap<String, Fila> network = new HashMap<>();
        network.put(fila1.name, fila1); network.put(fila2.name, fila2); network.put(fila3.name, fila3);

        PriorityQueue<Event> eventScheduler = new PriorityQueue<>();
        eventScheduler.add(new Event(getNextInterArrivalTime(), "ARRIVAL_EXTERNAL", fila1.name, null));

        // Loop Principal da Simulação
        while (!eventScheduler.isEmpty() && totalArrivalsExternal < NUM_CLIENTES_SIM) {
            Event currentEvent = eventScheduler.poll();
            
            // Acumular Tempo no estado ANTERIOR ao evento
            for(Fila f : network.values()) {
                double timeInState = currentEvent.eventTime - f.lastEventTime;
                if (timeInState > 0) f.accumulatedTimeByState.put(f.customersInSystem, f.accumulatedTimeByState.getOrDefault(f.customersInSystem, 0.0) + timeInState);
                f.lastEventTime = currentEvent.eventTime;
            }
            globalTime = currentEvent.eventTime;

            // Processar o Evento
            if (currentEvent.eventType.equals("ARRIVAL_EXTERNAL")) {
                totalArrivalsExternal++; processArrivalExternal(currentEvent, network, eventScheduler);
            } else if (currentEvent.eventType.equals("DEPARTURE")) {
                processDeparture(currentEvent, network, eventScheduler);
            }
        }
        
        // Exibição dos Resultados (M9)
        exibirResultadosM9(network);
    }
    
    // ------------------------------------------------------------------------------------------------
    // --- FUNÇÕES DE PROCESSAMENTO E ROTEAMENTO ---
    // ------------------------------------------------------------------------------------------------
    private static void processArrivalExternal(Event currentEvent, HashMap<String, Fila> network, PriorityQueue<Event> eventScheduler) {
        Fila targetFila = network.get(currentEvent.targetFila);
        
        if (!targetFila.isFull()) {
            Cliente newCustomer = new Cliente(); targetFila.customersInSystem++; scheduleServiceOrQueue(targetFila, eventScheduler, newCustomer);
        } else { totalLostCustomers++; }
        
        eventScheduler.add(new Event(globalTime + getNextInterArrivalTime(), "ARRIVAL_EXTERNAL", targetFila.name, null));
    }

    private static void processDeparture(Event currentEvent, HashMap<String, Fila> network, PriorityQueue<Event> eventScheduler) {
        Fila departedFila = network.get(currentEvent.targetFila);
        departedFila.customersInSystem--; departedFila.totalDeparturesFila++; 

        if (departedFila.customersInSystem >= departedFila.servers) scheduleServiceOrQueue(departedFila, eventScheduler, currentEvent.customer); 

        // ROTEAMENTO COM RETORNO
        if (departedFila.name.equals("Fila_3_RetiradaDosLanches")) {
            if (random.nextDouble() < P_3_TO_1) routeCustomer(network, eventScheduler, currentEvent.customer, "Fila_1_EscolhaDoPedido");
            else { 
                // SAÍDA FINAL
                systemResponseTimes.add(globalTime - currentEvent.customer.arrivalTime); 
            }
        } else if (departedFila.name.equals("Fila_1_EscolhaDoPedido")) {
            routeCustomer(network, eventScheduler, currentEvent.customer, "Fila_2_Pagamentos");
        } else if (departedFila.name.equals("Fila_2_Pagamentos")) {
            routeCustomer(network, eventScheduler, currentEvent.customer, "Fila_3_RetiradaDosLanches");
        }
    }
    
    private static void routeCustomer(HashMap<String, Fila> network, PriorityQueue<Event> eventScheduler, Cliente customer, String nextFilaName) {
        Fila nextFila = network.get(nextFilaName);
        if (!nextFila.isFull()) { nextFila.customersInSystem++; scheduleServiceOrQueue(nextFila, eventScheduler, customer); } 
        else { totalLostCustomers++; }
    }
    
    // --- Funções Auxiliares de Serviço e Geração de Tempos (G/G) ---
    private static void scheduleServiceOrQueue(Fila fila, PriorityQueue<Event> eventScheduler, Cliente customer) {
        double earliestFinishTime = fila.serviceFinishes.poll();
        double serviceStartTime = Math.max(globalTime, earliestFinishTime);
        double serviceTime = getNextServiceTime(fila.name);
        double departureTime = serviceStartTime + serviceTime;
        fila.accumulatedServiceTime += serviceTime; 
        fila.serviceFinishes.add(departureTime); 
        eventScheduler.add(new Event(departureTime, "DEPARTURE", fila.name, customer));
    }
    
    private static double getNextInterArrivalTime() { return random.nextDouble() * (CH_TEMPOS[1] - CH_TEMPOS[0]) + CH_TEMPOS[0]; }
    
    private static double getNextServiceTime(String filaName) {
        double min, max;
        if (filaName.equals("Fila_1_EscolhaDoPedido")) { min = MU_1_TEMPOS[0]; max = MU_1_TEMPOS[1]; } 
        else if (filaName.equals("Fila_2_Pagamentos")) { min = MU_2_TEMPOS[0]; max = MU_2_TEMPOS[1]; } 
        else { min = MU_3_TEMPOS[0]; max = MU_3_TEMPOS[1]; }
        return random.nextDouble() * (max - min) + min;
    }


    // ------------------------------------------------------------------------------------------------
    // --- FUNÇÃO DE EXIBIÇÃO DE RESULTADOS (FORMATADA PARA M9) ---
    // ------------------------------------------------------------------------------------------------
    private static void exibirResultadosM9(HashMap<String, Fila> network) {
        System.out.println("\n==================================================================================");
        System.out.println("RESULTADOS DA SIMULAÇÃO DO BAR UNIVERSITÁRIO | ÍNDICES DE DESEMPENHO");
        System.out.println("==================================================================================");
        
        // W TOTAL DO SISTEMA
        double avgResponseTime = systemResponseTimes.stream().mapToDouble(d -> d).average().orElse(0.0);
        System.out.printf("TEMPO MÉDIO DE RESPOSTA TOTAL DO SISTEMA: %.4f minutos\n", avgResponseTime);
        System.out.printf("Clientes Perdidos por Bloqueio: %d\n", totalLostCustomers);
        System.out.println("----------------------------------------------------------------------------------");

        for (Fila f : network.values()) {
            double rho = f.accumulatedServiceTime / (f.servers * globalTime);
            double lambda_ef = f.totalDeparturesFila / globalTime;
            double L = 0.0;
            
            System.out.printf("\n### FILA ANALISADA: %s (Servidores: %d)\n", f.name.toUpperCase().replace("_", " "), f.servers);
            System.out.println("----------------------------------------------------------------------------------");

            // 1. ÍNDICES DE DESEMPENHO (Utilização e Vazão)
            System.out.println(">> ÍNDICES DE DESEMPENHO <<");
            System.out.printf("  1. Utilização :          %.4f (%.2f%%) \n", rho, rho * 100);
            System.out.printf("  2. Vazão Efetiva : %.4f clientes/min\n", lambda_ef);
            
            // 2. PROBABILIDADES DOS ESTADOS DAS FILAS
            System.out.println("\n>> PROBABILIDADES DOS ESTADOS DAS FILAS <<");
            
            List<HashMap.Entry<Integer, Double>> sortedStates = f.accumulatedTimeByState.entrySet().stream()
                .sorted(Comparator.comparing(HashMap.Entry::getKey))
                .toList();

            System.out.println("   -------------------------------------------------");
            System.out.printf("   | %-10s | %-12s | %-12s |\n", "Estado ", "Tempo Acumulado", "Probabilidade ");
            System.out.println("   -------------------------------------------------");

            for (HashMap.Entry<Integer, Double> entry : sortedStates) {
                double Pn = (globalTime > 0) ? entry.getValue() / globalTime : 0;
                System.out.printf("   | n=%-7d | %-12.2f | %.4f%% |\n", entry.getKey(), entry.getValue(), Pn * 100);
                L += entry.getKey() * Pn; // Cálculo da População Média
            }
            System.out.println("   -------------------------------------------------");
            
            // 3. POPULAÇÃO (L)
            System.out.printf("\n  3. População Média : %.4f clientes\n", L);
        }
        System.out.println("\n==================================================================================");
    }
}