package ai.vital.vitalsigns.test


import ai.vital.vitalsigns.VitalSignsSingleton

import ai.vital.vitalsigns.impl.PropertyFactory
import ai.vital.vitalsigns.inf.ConsiderInterface
import ai.vital.vitalsigns.model.VITAL_Node
import ai.vital.vitalsigns.model.property.StringPropertyValue


import static ai.vital.vitalsigns.datatype.Truth.YES
import static ai.vital.vitalsigns.datatype.Truth.NO
import static ai.vital.vitalsigns.datatype.Truth.UNKNOWN
import static ai.vital.vitalsigns.datatype.Truth.MU

import static ai.vital.vitalsigns.constant.HasValueConstant.HasValue


// @Mixin(ConsiderMixin)
class TestTwo implements ConsiderInterface {

    static void main(args) {

        TestTwo script = new TestTwo()

        script.runTest(args)

    }


    public runTest(args) {

        println "Hello"

        VitalSignsSingleton vs = VitalSignsSingleton.get()

        def p1 = PropertyFactory.createPropertyInstance(StringPropertyValue.class, Property_hasName.class)

        println "Property: " + p1

        def p2 = PropertyFactory.createPropertyInstance(StringPropertyValue.class, Property_hasOrganizationID.class)

        println "Property: " + p2

        if(p2 > p1) {

            println "Greater than"

        }
        else {

            println "Not greater than"
        }

        VITAL_Node node = new VITAL_Node()

        node.name = "Marc"

        String name = "Marc"

        if(name == p1) {

            // if(name.equals(p1)) {

            println "equals"

        }
        else {

            println "Not equals"
        }


        // { println "Hello" }

        method()


        println "Consider: " + consider(node)

        println "ConsiderValue: " + considerValue(node)

        // pass expression that is evaluated and then considered
        // or pass closure that consider can subject to further transformation
        // into truth values at runtime


        switch( consider( { node } ) ) {

            case NO ->  { println "NO" }

            case YES -> { println "YES" }

            case UNKNOWN -> { println "UNKNOWN" }

            case MU -> { println "MU" }

            default -> { println "None" }

        }


        switch( considerValue(node) ) {

            case UNKNOWN: { println "UNKNOWN" }; break

            case MU: { println "MU" }; break

            case HasValue: { println "HasValue" }; break

            default: { println "None" }; break

        }


        // Truth t1 = YES

        // Truth t2 = NO

        // def o = t1 && t2

        // println "Anding: " + o



    }

    def method() {

        def a = { println "Block Hello" }

        { println "Block Hello" }


        return
    }

}
