package distribution_system.webApp.entities;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Objects;

@RequiredArgsConstructor
@ToString
@Getter
public class Pair{
    private final String id;
    private final Cell cell;
    private final Product product;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Pair pair = (Pair) object;
        return Objects.equals(getCell(), pair.getCell()) && Objects.equals(getProduct(), pair.getProduct());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCell());
    }


}
