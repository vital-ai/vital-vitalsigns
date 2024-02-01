package ai.vital.vitalsigns.csv;

import java.util.List;

import ai.vital.vitalsigns.model.GraphObject;

public interface ToCSVProvider {

    public void toCSV(GraphObject graphObject, List<String> targetList);

    public String getHeaders();

}
