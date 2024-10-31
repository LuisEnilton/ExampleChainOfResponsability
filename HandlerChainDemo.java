import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// Classe Request
class Request {
    String userId;
    String password;
    String ipAddress;
    Map<String, Object> data;
    LocalDateTime timestamp;

    public Request(String userId, String password, String ipAddress, Map<String, Object> data) {
        this.userId = userId;
        this.password = password;
        this.ipAddress = ipAddress;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }
}

interface Handler{
    public Handler setNext(Handler handler);

    public String handle(Request request);
}

// Classe Handler (Abstract)
abstract class BaseHandler implements Handler {
    private Optional<Handler> nextHandler = Optional.empty();

    @Override
    public Handler setNext(Handler handler) {
        this.nextHandler = Optional.of(handler);
        return handler;
    }

    @Override
    public String handle(Request request) {
        if (processRequest(request)) {
            return nextHandler.map(handler -> handler.handle(request))
                    .orElse("Pedido processado com sucesso!");
        }
        return "Pedido rejeitado";
    }
    
    protected abstract boolean processRequest(Request request);
}

// Classe AuthenticationHandler
class AuthenticationHandler extends BaseHandler {
    private Map<String, String> users;

    public AuthenticationHandler() {
        users = new HashMap<>();
        users.put("user1", "password123");
        users.put("admin", "admin123");
    }

    @Override
    protected boolean processRequest(Request request) {
        if (users.containsKey(request.userId) && users.get(request.userId).equals(request.password)) {
            System.out.println("Autenticação bem-sucedida");
            return true;
        }
        System.out.println("Falha na autenticação");
        return false;
    }
}

// Classe BruteForceProtectionHandler
class BruteForceProtectionHandler extends BaseHandler {
    private Map<String, LocalDateTime[]> failedAttempts;

    public BruteForceProtectionHandler() {
        failedAttempts = new HashMap<>();
    }

    @Override
    protected boolean processRequest(Request request) {
        LocalDateTime currentTime = LocalDateTime.now();
        if (failedAttempts.containsKey(request.ipAddress)) {
            LocalDateTime[] attempts = failedAttempts.get(request.ipAddress);
            long recentAttempts = Duration.between(attempts[0], currentTime).getSeconds();
            if (recentAttempts < 300) {
                System.out.println("Muitas tentativas falhas. IP bloqueado temporariamente");
                return false;
            }
        }
        return true;
    }
}

// Classe DataValidationHandler
class DataValidationHandler extends BaseHandler {
    @Override
    protected boolean processRequest(Request request) {
        String[] requiredFields = {"product_id", "quantity"};
        for (String field : requiredFields) {
            if (!request.data.containsKey(field)) {
                System.out.println("Dados inválidos ou incompletos");
                return false;
            }
        }
        System.out.println("Dados validados com sucesso");
        return true;
    }
}

// Classe CacheHandler
class CacheHandler extends BaseHandler {
    private Map<String, Boolean> cache;

    public CacheHandler() {
        cache = new HashMap<>();
    }

    @Override
    protected boolean processRequest(Request request) {
        String requestHash = generateHash(request.userId + request.data.toString());

        if (cache.containsKey(requestHash)) {
            System.out.println("Retornando resultado do cache");
            return false;
        }

        cache.put(requestHash, true);
        System.out.println("Cache miss - processando pedido");
        return true;
    }

    private String generateHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}

// Criação da cadeia de responsabilidade
public class HandlerChainDemo {
    public static Handler createHandlerChain() {
        BruteForceProtectionHandler bruteForce = new BruteForceProtectionHandler();
        AuthenticationHandler auth = new AuthenticationHandler();
        DataValidationHandler validation = new DataValidationHandler();
        CacheHandler cache = new CacheHandler();

        bruteForce.setNext(auth);
        auth.setNext(validation);
        validation.setNext(cache);

        return bruteForce;
    }

    public static void main(String[] args) {
        Handler handlerChain = createHandlerChain();

        // Pedido válido
        System.out.println("\nProcessando pedido válido:");
        Map<String, Object> data = new HashMap<>();
        data.put("product_id", "123");
        data.put("quantity", 1);
        Request validRequest = new Request("user1", "password123", "192.168.1.1", data);
        String result = handlerChain.handle(validRequest);
        System.out.println("Resultado: " + result);

        // Pedido com autenticação inválida
        System.out.println("\nProcessando pedido com senha inválida:");
        Request invalidRequest = new Request("user1", "wrong_password", "192.168.1.1", data);
        result = handlerChain.handle(invalidRequest);
        System.out.println("Resultado: " + result);
    }
}
