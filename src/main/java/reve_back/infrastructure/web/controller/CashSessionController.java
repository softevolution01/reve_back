package reve_back.infrastructure.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reve_back.application.ports.in.ManageCashSessionUseCase;
import reve_back.infrastructure.web.dto.CashStatusResponse;
import reve_back.infrastructure.web.dto.CloseSessionRequest;
import reve_back.infrastructure.web.dto.OpenSessionRequest;
import reve_back.infrastructure.web.dto.RegisterMovementRequest;

@RestController
@RequestMapping("/cashSessions")
@RequiredArgsConstructor
public class CashSessionController {

    private final ManageCashSessionUseCase manageCashSessionUseCase;


    @GetMapping("/status")
    @PreAuthorize("hasAuthority('sales:create:client')")
    public ResponseEntity<CashStatusResponse> getStatus(@RequestParam Long branchId) {
        CashStatusResponse status = manageCashSessionUseCase.getSessionStatus(branchId);
        return ResponseEntity.ok(status);
    }


    @PostMapping("/open")
    @PreAuthorize("hasAuthority('sales:create:client')")
    public ResponseEntity<Void> openSession(@RequestBody OpenSessionRequest request) {
        manageCashSessionUseCase.openSession(
                request.branchId(),
                request.userId(),
                request.initialAmount()
        );
        return ResponseEntity.ok().build();
    }


    @PostMapping("/close")
    @PreAuthorize("hasAuthority('sales:create:client')")
    public ResponseEntity<Void> closeSession(@RequestBody CloseSessionRequest request) {
        manageCashSessionUseCase.closeSession(
                request.branchId(),
                request.userId(),
                request.countedCash(),
                request.notes()
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/movements")
    @PreAuthorize("hasAuthority('sales:create:client')")
    public ResponseEntity<Void> registerMovement(@RequestBody RegisterMovementRequest request) {
        manageCashSessionUseCase.registerMovement(
                request.branchId(),
                request.userId(),
                request.type(),
                request.amount(),
                request.description(),
                request.method()
        );
        return ResponseEntity.ok().build();
    }
}
