import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class OrderSystem {
    
    // Simulando banco de dados de usuários
    private Map<String, String> users;
    
    // Cache de pedidos
    private Map<String, Boolean> requestCache;
    
    // Controle de tentativas de login
    private Map<String, List<LocalDateTime>> failedAttempts;

    public OrderSystem() {
        users = new HashMap<>();
        users.put("user1", "password123");
        users.put("admin", "admin123");

        requestCache = new HashMap<>();
        failedAttempts = new HashMap<>();
    }

    public String processOrder(String userId, String password, String ipAddress, Map<String, Object> orderData) {
        try {
            LocalDateTime currentTime = LocalDateTime.now();

            // Verificação de tentativas de força bruta
            if (failedAttempts.containsKey(ipAddress)) {
                List<LocalDateTime> recentAttempts = new ArrayList<>();
                for (LocalDateTime attempt : failedAttempts.get(ipAddress)) {
                    if (Duration.between(attempt, currentTime).getSeconds() < 300) {
                        recentAttempts.add(attempt);
                    }
                }

                if (recentAttempts.size() >= 3) {
                    return "Erro: Muitas tentativas falhas. IP bloqueado temporariamente";
                }

                failedAttempts.put(ipAddress, recentAttempts);
            }

            // Verificação de autenticação
            if (!users.containsKey(userId) || !users.get(userId).equals(password)) {
                failedAttempts.putIfAbsent(ipAddress, new ArrayList<>());
                failedAttempts.get(ipAddress).add(currentTime);
                return "Erro: Credenciais inválidas";
            }

            // Validação dos dados do pedido
            if (!validateOrderData(orderData)) {
                return "Erro: Dados do pedido inválidos ou incompletos";
            }

            // Sanitização dos dados
            Map<String, Object> sanitizedData = sanitizeData(orderData);

            // Verificação de cache
            String cacheKey = generateCacheKey(userId, sanitizedData);
            if (requestCache.containsKey(cacheKey)) {
                return "Pedido recuperado do cache";
            }

            // Processamento do pedido
            String result = processOrderInternal(sanitizedData);

            // Armazena no cache
            requestCache.put(cacheKey, true);

            return result;

        } catch (Exception e) {
            return "Erro interno: " + e.getMessage();
        }
    }

    private boolean validateOrderData(Map<String, Object> orderData) {
        List<String> requiredFields = List.of("product_id", "quantity");

        // Verifica campos obrigatórios
        for (String field : requiredFields) {
            if (!orderData.containsKey(field)) {
                return false;
            }
        }

        // Verifica tipos de dados
        if (!(orderData.get("product_id") instanceof String)) {
            return false;
        }

        if (!(orderData.get("quantity") instanceof Number)) {
            return false;
        }

        // Verifica valores válidos
        if (((Number) orderData.get("quantity")).doubleValue() <= 0) {
            return false;
        }

        return true;
    }

    private Map<String, Object> sanitizeData(Map<String, Object> orderData) {
        Map<String, Object> sanitized = new HashMap<>();

        // Remove espaços em branco extras
        for (Map.Entry<String, Object> entry : orderData.entrySet()) {
            if (entry.getValue() instanceof String) {
                sanitized.put(entry.getKey(), ((String) entry.getValue()).trim());
            } else {
                sanitized.put(entry.getKey(), entry.getValue());
            }
        }

        // Converte quantidade para inteiro
        if (sanitized.containsKey("quantity")) {
            sanitized.put("quantity", ((Number) sanitized.get("quantity")).intValue());
        }

        return sanitized;
    }

    private String generateCacheKey(String userId, Map<String, Object> data) throws NoSuchAlgorithmException {
        String content = userId + data.toString();
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(content.getBytes());
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    private String processOrderInternal(Map<String, Object> orderData) {
        return "Pedido processado com sucesso! Produto: " + orderData.get("product_id") + 
               ", Quantidade: " + orderData.get("quantity");
    }

    public static void main(String[] args) {
        OrderSystem system = new OrderSystem();

        // Teste 1: Pedido válido
        System.out.println("\nTestando pedido válido:");
        Map<String, Object> orderData1 = new HashMap<>();
        orderData1.put("product_id", "PROD123");
        orderData1.put("quantity", 2);
        String result1 = system.processOrder("user1", "password123", "192.168.1.1", orderData1);
        System.out.println(result1);

        // Teste 2: Credenciais inválidas
        System.out.println("\nTestando credenciais inválidas:");
        String result2 = system.processOrder("user1", "senha_errada", "192.168.1.1", orderData1);
        System.out.println(result2);

        // Teste 3: Dados inválidos
        System.out.println("\nTestando dados inválidos:");
        Map<String, Object> orderData3 = new HashMap<>();
        orderData3.put("product_id", "PROD123");
        orderData3.put("quantity", -1);  // quantidade inválida
        String result3 = system.processOrder("user1", "password123", "192.168.1.1", orderData3);
        System.out.println(result3);
    }
}
