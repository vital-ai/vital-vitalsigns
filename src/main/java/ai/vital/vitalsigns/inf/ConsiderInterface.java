package ai.vital.vitalsigns.inf;

import ai.vital.vitalsigns.datatype.Truth;

public interface ConsiderInterface {


    // use static implementation in ConsiderUtils


    default public Truth consider(Object param) {

        return null;
    }

}
