from datetime import datetime
import hashlib
from typing import Dict, List

class OrderSystem:
    def __init__(self):
        # Simulando banco de dados de usuários
        self.users = {
            "user1": "password123",
            "admin": "admin123"
        }
        
        # Cache de pedidos
        self.request_cache: Dict[str, bool] = {}
        
        # Controle de tentativas de login
        self.failed_attempts: Dict[str, List[datetime]] = {}
        
    def process_order(self, user_id: str, password: str, ip_address: str, order_data: dict) -> str:
        """
        Método monolítico que realiza todas as verificações necessárias
        """
        try:
            # Verificação de tentativas de força bruta
            current_time = datetime.now()
            if ip_address in self.failed_attempts:
                # Remove tentativas antigas (mais de 5 minutos)
                recent_attempts = [
                    attempt 
                    for attempt in self.failed_attempts[ip_address] 
                    if (current_time - attempt).seconds < 300
                ]
                
                if len(recent_attempts) >= 3:
                    return "Erro: Muitas tentativas falhas. IP bloqueado temporariamente"
                
                self.failed_attempts[ip_address] = recent_attempts
            
            # Verificação de autenticação
            if user_id not in self.users or self.users[user_id] != password:
                # Registra tentativa falha
                if ip_address not in self.failed_attempts:
                    self.failed_attempts[ip_address] = []
                self.failed_attempts[ip_address].append(current_time)
                return "Erro: Credenciais inválidas"
            
            # Validação dos dados do pedido
            if not self._validate_order_data(order_data):
                return "Erro: Dados do pedido inválidos ou incompletos"
            
            # Sanitização dos dados
            sanitized_data = self._sanitize_data(order_data)
            
            # Verificação de cache
            cache_key = self._generate_cache_key(user_id, sanitized_data)
            if cache_key in self.request_cache:
                return "Pedido recuperado do cache"
            
            # Processamento do pedido
            result = self._process_order_internal(sanitized_data)
            
            # Armazena no cache
            self.request_cache[cache_key] = True
            
            return result
            
        except Exception as e:
            return f"Erro interno: {str(e)}"
    
    def _validate_order_data(self, order_data: dict) -> bool:
        """
        Valida os dados do pedido
        """
        required_fields = ['product_id', 'quantity']
        
        # Verifica campos obrigatórios
        if not all(field in order_data for field in required_fields):
            return False
        
        # Verifica tipos de dados
        if not isinstance(order_data.get('product_id'), str):
            return False
        
        if not isinstance(order_data.get('quantity'), (int, float)):
            return False
        
        # Verifica valores válidos
        if order_data.get('quantity') <= 0:
            return False
            
        return True
    
    def _sanitize_data(self, order_data: dict) -> dict:
        """
        Sanitiza os dados do pedido
        """
        sanitized = {}
        
        # Remove espaços em branco extras
        for key, value in order_data.items():
            if isinstance(value, str):
                sanitized[key] = value.strip()
            else:
                sanitized[key] = value
        
        # Converte quantidade para inteiro
        if 'quantity' in sanitized:
            sanitized['quantity'] = int(sanitized['quantity'])
            
        return sanitized
    
    def _generate_cache_key(self, user_id: str, data: dict) -> str:
        """
        Gera uma chave única para o cache
        """
        content = f"{user_id}{str(data)}"
        return hashlib.md5(content.encode()).hexdigest()
    
    def _process_order_internal(self, order_data: dict) -> str:
        """
        Processa o pedido após todas as validações
        """
        # Aqui entraria a lógica real de processamento do pedido
        return f"Pedido processado com sucesso! Produto: {order_data['product_id']}, Quantidade: {order_data['quantity']}"


# Exemplo de uso
if __name__ == "__main__":
    system = OrderSystem()
    
    # Teste 1: Pedido válido
    print("\nTestando pedido válido:")
    result = system.process_order(
        user_id="user1",
        password="password123",
        ip_address="192.168.1.1",
        order_data={
            "product_id": "PROD123",
            "quantity": 2
        }
    )
    print(result)
    
    # Teste 2: Credenciais inválidas
    print("\nTestando credenciais inválidas:")
    result = system.process_order(
        user_id="user1",
        password="senha_errada",
        ip_address="192.168.1.1",
        order_data={
            "product_id": "PROD123",
            "quantity": 2
        }
    )
    print(result)
    
    # Teste 3: Dados inválidos
    print("\nTestando dados inválidos:")
    result = system.process_order(
        user_id="user1",
        password="password123",
        ip_address="192.168.1.1",
        order_data={
            "product_id": "PROD123",
            "quantity": -1  # quantidade inválida
        }
    )
    print(result)