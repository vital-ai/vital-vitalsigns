package ai.vital.vitalsigns.model.property;

import ai.vital.vitalsigns.datatype.Truth;

public final class TruthDataTypeValue extends Truth implements PropertyValue, PropertyValueInterface  {

    private Truth truthValue;

    public static TruthDataTypeValue YES = new TruthDataTypeValue(Truth.YES);

    public static TruthDataTypeValue NO = new TruthDataTypeValue(Truth.NO);

    public static TruthDataTypeValue UNKNOWN = new TruthDataTypeValue(Truth.UNKNOWN);

    public static TruthDataTypeValue MU = new TruthDataTypeValue(Truth.MU);


    /*
    public TruthDataTypeValue() {
        super(YES_URI);
        truthValue = Truth.YES;
    }
    */


    public TruthDataTypeValue(Truth t) {
        super(t);
        truthValue = t;
    }


    @Override
    public boolean equals(Object obj) {

        if(obj == null) { return false; }

        if(truthValue == null) { return false; }

        if(this == obj) { return true; }

        if(obj instanceof TruthDataTypeValue) {

            TruthDataTypeValue tdt = (TruthDataTypeValue) obj;

            if(tdt.truthValue == null) { return false; }

            return truthValue.equals(tdt.truthValue);
        }

        if(obj instanceof Truth) {

            return truthValue.equals(obj);
        }

        return false;
    }

}
