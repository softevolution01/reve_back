package reve_back.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reve_back.application.ports.in.CreateSaleUseCase;
import reve_back.application.ports.out.*;
import reve_back.domain.model.*;
import reve_back.infrastructure.web.dto.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleService implements CreateSaleUseCase {

    private final SalesRepositoryPort salesRepositoryPort;
    private final BranchRepositoryPort branchRepositoryPort;
    private final LoyaltyProgressRepositoryPort loyaltyProgressRepositoryPort;

    private final StockManagementService stockService;
    private final PaymentProcessingService paymentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long execute(SaleSimulationRequest request) {
        return 0L;
    }

    @Transactional(rollbackFor = Exception.class)
    public SaleResponse createSale(SaleCreationRequest request) {
        Branch branch = branchRepositoryPort.findById(request.branchId())
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        PaymentProcessingService.PaymentResult paymentResult = paymentService.processPayments(
                request.payments(), branch, request.userId());

        List<SaleItem> saleItems = new ArrayList<>();
        BigDecimal totalBruto = BigDecimal.ZERO;
        BigDecimal totalManualDiscount = BigDecimal.ZERO;
        List<TempItemCalculation> tempCalculations = new ArrayList<>();

        for (SaleItemRequest itemReq : request.items()) {
            SaleItem domainItem = stockService.processStock(itemReq, branch.warehouseId(), request.userId());

            BigDecimal itemManualDiscount = itemReq.manualDiscount() != null ? itemReq.manualDiscount() : BigDecimal.ZERO;
            BigDecimal lineGross = domainItem.unitPrice().multiply(new BigDecimal(domainItem.quantity()));
            BigDecimal lineNetBeforeSurcharge = lineGross.subtract(itemManualDiscount);

            totalBruto = totalBruto.add(lineGross);
            totalManualDiscount = totalManualDiscount.add(itemManualDiscount);

            tempCalculations.add(new TempItemCalculation(domainItem, itemManualDiscount, lineNetBeforeSurcharge, itemReq.blockedPromo()));
        }

        BigDecimal totalSystemDiscount = request.systemDiscount() != null ? request.systemDiscount() : BigDecimal.ZERO;
        BigDecimal totalDescuentos = totalManualDiscount.add(totalSystemDiscount);
        BigDecimal baseImponible = totalBruto.subtract(totalDescuentos);
        BigDecimal totalFinalCharged = baseImponible.add(paymentResult.totalSurcharge());

        for (TempItemCalculation temp : tempCalculations) {
            BigDecimal itemSurchargeShare = BigDecimal.ZERO;
            if (baseImponible.compareTo(BigDecimal.ZERO) > 0 && paymentResult.totalSurcharge().compareTo(BigDecimal.ZERO) > 0) {
                itemSurchargeShare = temp.lineNet.divide(baseImponible, 4, RoundingMode.HALF_UP).multiply(paymentResult.totalSurcharge());
            }

            BigDecimal finalItemSubtotal = temp.lineNet.add(itemSurchargeShare).setScale(2, RoundingMode.HALF_UP);

            saleItems.add(new SaleItem(
                    null, temp.domainItem.productId(), temp.domainItem.decantPriceId(),
                    temp.domainItem.productName(), temp.domainItem.productBrand(),
                    temp.domainItem.quantity(), temp.domainItem.unitPrice(),
                    BigDecimal.ZERO, temp.manualDiscount, finalItemSubtotal,
                    temp.domainItem.volumeMlPerUnit(),
                    temp.blockedPromo != null ? temp.blockedPromo : false,
                    false, "NONE"
            ));
        }

        Sale saleToSave = new Sale(
                null, LocalDateTime.now(), branch.id(), request.userId(), request.clientId(),
                null, totalBruto, totalDescuentos, new BigDecimal("0.18"),
                paymentResult.totalSurcharge(), totalFinalCharged,
                paymentResult.paymentMethodString(), saleItems, paymentResult.salePayments()
        );

        Sale savedSale = salesRepositoryPort.save(saleToSave);

        if (request.clientId() != null) {
            updateLoyalty(request.clientId(), totalFinalCharged);
        }

        return new SaleResponse(savedSale.id(), savedSale.saleDate(), branch.name(), "Vendedor", "Cliente",
                totalBruto, totalDescuentos, paymentResult.totalSurcharge(), totalFinalCharged, totalFinalCharged, null);
    }

    private void updateLoyalty(Long clientId, BigDecimal amount) {
        ClientLoyaltyProgress progress = loyaltyProgressRepositoryPort.findByClientId(clientId)
                .orElse(new ClientLoyaltyProgress(clientId, 1, 0, BigDecimal.ZERO, LocalDateTime.now()));

        BigDecimal total = progress.accumulatedMoney().add(amount);
        loyaltyProgressRepositoryPort.save(new ClientLoyaltyProgress(clientId, progress.currentTier(),
                progress.pointsInTier(), total, LocalDateTime.now()));
    }

    private record TempItemCalculation(SaleItem domainItem, BigDecimal manualDiscount, BigDecimal lineNet, Boolean blockedPromo) {}
}