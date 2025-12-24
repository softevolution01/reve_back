package reve_back.domain.model;

public enum BottlesStatus {
    SELLADA("SELLADA"),
    AGOTADA("AGOTADA"),
    DECANTADA("DECANTADA"),
    DECANT_AGOTADA("DECANT_AGOTADA");

    private final String value;

    BottlesStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
