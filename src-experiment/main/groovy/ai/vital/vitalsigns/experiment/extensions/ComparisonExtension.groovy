package ai.vital.vitalsigns.experiment.extensions

import ai.vital.vitalsigns.model.property.PropertyInterface

class ComparisonExtension {

    static boolean equals(String self, PropertyInterface other) {

        println "String equals"

        if (other == null) return false

        return false

        //return other.equals(self)
    }


}
