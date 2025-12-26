package reve_back.infrastructure.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reve_back.application.ports.in.GetPaymentMethodsUseCase;
import reve_back.infrastructure.web.dto.PaymentMethodResponse;

import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final GetPaymentMethodsUseCase getPaymentMethodsUseCase;

    @GetMapping
    public List<PaymentMethodResponse> findAll() {
        return getPaymentMethodsUseCase.findAll().stream()
                .map(model -> new PaymentMethodResponse(
                        model.id(),
                        model.name(),
                        model.surchargePercentage()
                )).toList();
    }
}
