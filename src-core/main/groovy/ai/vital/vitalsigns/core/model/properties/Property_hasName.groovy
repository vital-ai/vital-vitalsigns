package ai.vital.vitalsigns.core.model.properties

import ai.vital.vitalsigns.property.trait.PropertyTrait

trait Property_hasName extends PropertyTrait {

    public String getURI() {
        return 'http://vital.ai/ontology/vital-core#hasName'
    }

}
