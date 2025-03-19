package distribution_system.webApp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DistributionMethods {
    RANDOM,
    ELECTRE_TRI,
    TOPSIS,
    ELECTRE_TRI_TOPSIS,
    GA;

    @JsonCreator
    public static DistributionMethods fromString(String value) {
        return DistributionMethods.valueOf(value.toUpperCase());
    }

    @JsonValue
    public String toJson() {
        return this.name();
    }
}
