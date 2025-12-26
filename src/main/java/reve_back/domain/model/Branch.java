package reve_back.domain.model;

public record Branch(
    Long id,
    String name,
    String location,
    Long warehouseId,
    Boolean isCashManagedCentralized
) {
    public Branch(String name, String location, Long warehouseId,Boolean isCashManagedCentralized) {
        this(null, name, location, warehouseId,isCashManagedCentralized);
    }
}
