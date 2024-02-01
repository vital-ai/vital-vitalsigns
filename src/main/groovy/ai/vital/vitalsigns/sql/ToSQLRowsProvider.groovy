package ai.vital.vitalsigns.sql

import java.util.List

import ai.vital.vitalsigns.model.GraphObject

public interface ToSQLRowsProvider {

    public List<String> getColumns()

    public List<String> toSQLRows(GraphObject graphObject)
}
