package reve_back.domain.model;

import java.util.List;

public record Warehouse(
        Long id,
        String name,
        String location,
        List<Bottle> bottles
) {
    public Warehouse(String name,String location,List<Bottle> bottles) {
        this(null,name,location,bottles);
    }
}
