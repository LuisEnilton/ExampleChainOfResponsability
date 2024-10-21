from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Optional
from datetime import datetime
import hashlib

@dataclass
class Request:
    user_id: str
    password: str
    ip_address: str
    data: dict
    timestamp: datetime = datetime.now()

class Handler(ABC):
    def __init__(self):
        self._next_handler: Optional[Handler] = None

    def set_next(self, handler: 'Handler') -> 'Handler':
        self._next_handler = handler
        return handler

    def handle(self, request: Request) -> Optional[str]:
        if self.process_request(request):
            return self._next_handler.handle(request) if self._next_handler else "Pedido processado com sucesso!"
        return "Pedido rejeitado"

    @abstractmethod
    def process_request(self, request: Request) -> bool:
        pass

class AuthenticationHandler(Handler):
    def __init__(self):
        super().__init__()
        # Simulando banco de dados de usuários
        self._users = {
            "user1": "password123",
            "admin": "admin123"
        }

    def process_request(self, request: Request) -> bool:
        if request.user_id in self._users and self._users[request.user_id] == request.password:
            print("Autenticação bem-sucedida")
            return True
        print("Falha na autenticação")
        return False

class BruteForceProtectionHandler(Handler):
    def __init__(self):
        super().__init__()
        self._failed_attempts = {}

    def process_request(self, request: Request) -> bool:
        # Verifica tentativas falhas nos últimos 5 minutos
        current_time = datetime.now()
        if request.ip_address in self._failed_attempts:
            attempts = [t for t in self._failed_attempts[request.ip_address] 
                       if (current_time - t).seconds < 300]
            if len(attempts) >= 3:
                print("Muitas tentativas falhas. IP bloqueado temporariamente")
                return False
            self._failed_attempts[request.ip_address] = attempts
        return True

class DataValidationHandler(Handler):
    def process_request(self, request: Request) -> bool:
        # Verifica se os dados necessários estão presentes
        required_fields = ['product_id', 'quantity']
        if all(field in request.data for field in required_fields):
            print("Dados validados com sucesso")
            return True
        print("Dados inválidos ou incompletos")
        return False

class CacheHandler(Handler):
    def __init__(self):
        super().__init__()
        self._cache = {}

    def process_request(self, request: Request) -> bool:
        # Cria um hash dos dados do pedido
        request_hash = hashlib.md5(
            f"{request.user_id}{str(request.data)}".encode()
        ).hexdigest()

        if request_hash in self._cache:
            print("Retornando resultado do cache")
            return False
        
        self._cache[request_hash] = True
        print("Cache miss - processando pedido")
        return True

# Uso do padrão
def create_handler_chain():
    brute_force = BruteForceProtectionHandler()
    auth = AuthenticationHandler()
    validation = DataValidationHandler()
    cache = CacheHandler()

    brute_force.set_next(auth)
    auth.set_next(validation)
    validation.set_next(cache)

    return brute_force

# Exemplo de uso
if __name__ == "__main__":
    handler_chain = create_handler_chain()

    # Pedido válido
    request = Request(
        user_id="user1",
        password="password123",
        ip_address="192.168.1.1",
        data={"product_id": "123", "quantity": 1}
    )
    
    print("\nProcessando pedido válido:")
    result = handler_chain.handle(request)
    print(f"Resultado: {result}\n")

    # Pedido com autenticação inválida
    request_invalid = Request(
        user_id="user1",
        password="wrong_password",
        ip_address="192.168.1.1",
        data={"product_id": "123", "quantity": 1}
    )
    
    print("Processando pedido com senha inválida:")
    result = handler_chain.handle(request_invalid)
    print(f"Resultado: {result}")