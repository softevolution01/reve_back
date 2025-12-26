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

    public static String generateAlphanumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC_CHARS.charAt(random.nextInt(ALPHANUMERIC_CHARS.length())));
        }
        return sb.toString();
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
    public static String generateFullBarcode(int length) {
        String text = generateAlphanumeric(length);
        return generateBarcodeImageBase64(text);
    }
}
