package reve_back.infrastructure.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;

import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;

public class BarcodeGenerator {

    private static final String ALPHANUMERIC_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom random = new SecureRandom();

    private BarcodeGenerator() {
    }

    public static String generateNextSequence(String lastCode, String prefix) {
        if (lastCode == null || lastCode.isEmpty()) {
            return prefix + "0001";
        }

        try {
            String numericPart = lastCode.substring(1);
            int number = Integer.parseInt(numericPart);

            number++;

            return prefix + String.format("%04d", number);
        } catch (NumberFormatException e) {
            return prefix + "0001";
        }
    }

    public static String generateBarcodeImageBase64(String barcodeText) {
        if (barcodeText == null || barcodeText.isEmpty()) {
            return null;
        }
        try {
            Code128Writer writer = new Code128Writer();
            BitMatrix bitMatrix = writer.encode(barcodeText, BarcodeFormat.CODE_128, 300, 100);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos);
            byte [] imageBytes = baos.toByteArray();

            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        }catch (Exception e){
            throw  new RuntimeException("Error generando código de barras: " + e.getMessage());
        }
    }
}
