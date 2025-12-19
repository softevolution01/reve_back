package reve_back.infrastructure.persistence.enums.global;

// Tipos de reglas de promoción
public enum PromotionRuleType {
    NTH_ITEM_FREE,     // Ej: 3x2 (El 3ro es gratis)
    NTH_ITEM_DISCOUNT, // Ej: 4to al 50%
    GLOBAL_PERCENTAGE,  // Descuento a todo
    FIXED_AMOUNT_DISCOUNT  //Descuento fijo en soles
}
