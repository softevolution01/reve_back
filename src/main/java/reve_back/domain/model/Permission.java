package reve_back.domain.model;

public record Permission(
        Long id,
        String name
) {
    public Permission(String name) {
        this(null, name);
    }
}
