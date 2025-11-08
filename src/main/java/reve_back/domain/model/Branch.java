package reve_back.domain.model;

public record Branch(
    Long id,
    String name,
    String location
) {
    public Branch(String name, String location) {
        this(null, name, location);
    }
}
