package reve_back.domain.model;

import java.util.List;

public record Warehouse(
        Long id,
        String name,
        String location
) {
    public Warehouse(String name,String location) {
        this(null,name,location);
    }
}
