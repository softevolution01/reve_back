package reve_back.infrastructure.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class BarcodeGenerator {

    public static String generateBarcodeImageBase64(String barcodeText) {
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
