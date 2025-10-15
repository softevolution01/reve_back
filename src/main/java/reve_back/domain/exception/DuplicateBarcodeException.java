package reve_back.domain.exception;

public class DuplicateBarcodeException extends RuntimeException{
    public DuplicateBarcodeException(String message){
        super(message);
    }
}
