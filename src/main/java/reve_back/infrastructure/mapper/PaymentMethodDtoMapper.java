package reve_back.infrastructure.mapper;

import reve_back.domain.model.PaymentMethod;
import reve_back.infrastructure.persistence.entity.PaymentMethodEntity;

import java.util.List;

public class PaymentMethodDtoMapper {

    public PaymentMethod toDomain(PaymentMethodEntity entity) {
        if (entity == null) return null;
        return new PaymentMethod(
                entity.getId(),
                entity.getName(),
                entity.getSurchargePercentage());
    }

}
