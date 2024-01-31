package ai.vital.vitalsigns.model.property;

import ai.vital.vitalsigns.datatype.Truth;

public sealed interface PropertyValue permits
        TruthDataTypeValue,
        BooleanPropertyValue,
        DateTimePropertyValue,
        DoublePropertyValue,
        GeoLocationPropertyValue,
        IntegerPropertyValue,
        LongPropertyValue,
        MultiPropertyValue,
        OtherPropertyValue,
        StringPropertyValue,
        TruthPropertyValue,
        URIPropertyValue {


}
