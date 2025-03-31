package distribution_system.webApp.entities;

import com.fasterxml.jackson.annotation.JsonGetter;
import distribution_system.webApp.enums.DistributionMethods;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeSet;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Results implements Serializable {
    private DistributionMethods method;
    private double score;
    private long timeRequired;
    private Solution solution;
    private Map<Integer, Integer> relativeShelving;

    @JsonGetter("solution")
    public TreeSet<Pair> getSolutionForJSON(){
        return solution.prepareForJSON();
    }
}
