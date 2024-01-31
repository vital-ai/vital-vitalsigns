package ai.vital.vitalsigns.impl

import ai.vital.vitalsigns.model.property.BooleanPropertyValue
import ai.vital.vitalsigns.model.property.IntegerPropertyValue
import ai.vital.vitalsigns.model.property.StringPropertyValue

import ai.vital.vitalsigns.model.property.PropertyImpl
import ai.vital.vitalsigns.model.property.PropertyInterface
import ai.vital.vitalsigns.property.trait.PropertyTrait

class PropertyFactory {

    public static PropertyInterface createPropertyInstance(Class propertyValueClass, Class<? extends PropertyTrait> traitClass) {

        if (BooleanPropertyValue.class.isAssignableFrom(propertyValueClass)) {

            return createBooleanProperty(traitClass)
        }

        if (IntegerPropertyValue.class.isAssignableFrom(propertyValueClass)) {

            return createIntegerProperty(traitClass)
        }

        if (StringPropertyValue.class.isAssignableFrom(propertyValueClass)) {

            return createStringProperty(traitClass)
        }


        return null
    }


    private static PropertyInterface createBooleanProperty(Class<? extends PropertyTrait> traitClass) {

        PropertyInterface inst = new PropertyImpl<BooleanPropertyValue>().withTraits(traitClass)

        return inst

    }

    private static PropertyInterface createIntegerProperty(Class<? extends PropertyTrait> traitClass) {

        PropertyInterface inst = new PropertyImpl<IntegerPropertyValue>().withTraits(traitClass)

        return inst

    }

    private static PropertyInterface createStringProperty(Class<? extends PropertyTrait> traitClass) {

        PropertyInterface inst = new PropertyImpl<StringPropertyValue>().withTraits(traitClass)

        return inst

    }

}
