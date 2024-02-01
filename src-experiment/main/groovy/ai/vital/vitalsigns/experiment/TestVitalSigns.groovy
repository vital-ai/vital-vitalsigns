package ai.vital.vitalsigns.experiment

import ai.vital.vitalsigns.VitalSigns
import ai.vital.vitalsigns.model.VITAL_Node
import ai.vital.vitalsigns.model.VitalApp

class TestVitalSigns {

    static void main(String[] args) {

        VitalSigns vs = VitalSigns.get()

        VitalApp app = VitalApp.withId("vital")

        VITAL_Node node = new VITAL_Node().generateURI(app)

        node.name = "John"

        println "Node: " + node.toJSON()



    }


}
