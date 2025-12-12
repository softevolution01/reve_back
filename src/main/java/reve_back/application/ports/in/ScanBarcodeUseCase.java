package reve_back.application.ports.in;

import reve_back.infrastructure.web.dto.ScanBarcodeResponse;

public interface ScanBarcodeUseCase {
    ScanBarcodeResponse scanBarcode(String barcode);
}
