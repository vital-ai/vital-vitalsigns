package ai.vital.vitalsigns.model.property;

public interface PropertyInterface extends Comparable<Object> {


    default int compareTo(Object other) {

        return 0;

    }

    }
