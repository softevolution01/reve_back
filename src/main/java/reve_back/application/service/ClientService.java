package reve_back.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reve_back.application.ports.in.CreateClientUseCase;
import reve_back.application.ports.in.GetClientPointsUseCase;
import reve_back.application.ports.in.GetLoyaltyStatusUseCase;
import reve_back.application.ports.in.SearchClientUseCase;
import reve_back.application.ports.out.ClientRepositoryPort;
import reve_back.application.ports.out.LoyaltyProgressRepositoryPort;
import reve_back.application.ports.out.LoyaltyTiersRepositoryPort;
import reve_back.application.ports.out.SalesRepositoryPort;
import reve_back.domain.model.Client;
import reve_back.domain.model.ClientLoyaltyProgress;
import reve_back.infrastructure.mapper.ClientDtoMapper;
import reve_back.infrastructure.web.dto.ClientCreationRequest;
import reve_back.infrastructure.web.dto.ClientPointsResponse;
import reve_back.infrastructure.web.dto.ClientResponse;
import reve_back.infrastructure.web.dto.LoyaltyResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ClientService implements SearchClientUseCase, CreateClientUseCase, GetClientPointsUseCase, GetLoyaltyStatusUseCase {


    private final ClientRepositoryPort clientRepositoryPort;
    private final SalesRepositoryPort salesRepositoryPort;
    private final ClientDtoMapper clientDtoMapper;
    private final LoyaltyProgressRepositoryPort loyaltyProgressRepositoryPort;
    private final LoyaltyTiersRepositoryPort loyaltyTierRepositoryPort;

    @Override
    @Transactional(readOnly = true)
    public List<ClientResponse> searchClients(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        List<Client> clients = clientRepositoryPort.searchByFullnameOrDni(query.trim());

        return clients.stream()
                .map(clientDtoMapper::toResponse)
                .toList();
    }

    @Override
    public ClientResponse createClient(ClientCreationRequest request) {
        if (request.dni() != null && !request.dni().isBlank()) {
            if (clientRepositoryPort.existsByDni(request.dni())) {
                throw new RuntimeException("El DNI ya está registrado.");
            }
        }
        if (request.email() != null && !request.email().isBlank()) {
            if (clientRepositoryPort.existsByEmail(request.email())) {
                throw new RuntimeException("El email ya está registrado.");
            }
        }

        Client newClient = clientDtoMapper.toDomain(request);
        Client savedClient = clientRepositoryPort.save(newClient);

        return clientDtoMapper.toResponse(savedClient);
    }

    @Override
    public ClientPointsResponse getClientPoints(Long clientId) {
        // 1. Obtener Cliente (Necesitamos un método findById en el puerto, vamos a asumir que existe o agregarlo)
        // Por ahora usaré searchByFullnameOrDni si no tenemos findById, pero lo ideal es findById.
        // Vamos a suponer que agregamos findById al ClientRepositoryPort abajo.
        Client client = clientRepositoryPort.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        // 2. Calcular monto histórico
        BigDecimal historicalTotal = salesRepositoryPort.getTotalSalesByClient(clientId);

        // 3. Regla VIP: Si no es VIP pero superó 100 soles, actualizar.
        boolean isVip = client.isVip();
        LocalDateTime vipSince = client.vipSince();

        if (!isVip && historicalTotal.compareTo(new BigDecimal("100.00")) >= 0) {
            isVip = true;
            vipSince = LocalDateTime.now();

            // Actualizamos el cliente en BBDD (Inmutabilidad del Record: creamos uno nuevo)
            Client upgradedClient = new Client(
                    client.id(), client.fullname(), client.dni(), client.email(), client.phone(),
                    true, vipSince, client.vipPurchaseCounter(), client.createdAt()
            );
            clientRepositoryPort.save(upgradedClient);
        }

        // 4. Calcular puntos post-VIP [cite: 486]
        BigDecimal postVipTotal = BigDecimal.ZERO;
        BigDecimal cycleAccumulated = BigDecimal.ZERO;
        int cyclesCompleted = 0;

        if (isVip && vipSince != null) {
            postVipTotal = salesRepositoryPort.getTotalSalesByClientAfterDate(clientId, vipSince);

            BigDecimal cycleAmount = new BigDecimal("1800.00");

            // Ciclos completados (división entera)
            cyclesCompleted = postVipTotal.divide(cycleAmount, 0, RoundingMode.DOWN).intValue();

            // Monto acumulado en el ciclo actual (resto)
            cycleAccumulated = postVipTotal.remainder(cycleAmount);
        }

        return new ClientPointsResponse(
                client.id(),
                client.fullname(),
                isVip,
                historicalTotal,
                postVipTotal,
                cycleAccumulated,
                cyclesCompleted
        );
    }

    @Override
    @Transactional(readOnly = true)
    public LoyaltyResponse getLoyaltyStatus(Long clientId) {
        // 1. Validar existencia del cliente
        ClientLoyaltyProgress progress = loyaltyProgressRepositoryPort.findByClientId(clientId)
                .orElse(new ClientLoyaltyProgress(clientId, 1, 0, 0.0, LocalDateTime.now()));

        // 2. Llamada a la función solicitada
        Double costOfNextPoint = loyaltyTierRepositoryPort.findCostByTier(progress.currentTier());

        // 3. Validar si el cliente es VIP desde el repositorio de clientes
        boolean isVip = clientRepositoryPort.findById(clientId)
                .map(Client::isVip)
                .orElse(false);

        return new LoyaltyResponse(
                clientId,
                progress.currentTier(),
                progress.pointsInTier(),
                progress.accumulatesMoney(),
                costOfNextPoint,
                isVip
        );
    }
}
