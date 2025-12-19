package reve_back.domain.model;

public enum BottlesStatus {
    SELLADA("sellada"),
    AGOTADA("agotada"),
    DECANTADA("decantada"),
    DECANT_AGOTADA("decant_agotada");

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
