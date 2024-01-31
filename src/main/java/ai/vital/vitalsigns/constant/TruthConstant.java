package ai.vital.vitalsigns.constant;

import ai.vital.vitalsigns.datatype.Truth;

public final class TruthConstant extends Truth implements ConsiderValueConstant {

    private Truth truthValue = null;

    public static TruthConstant YES = new TruthConstant(Truth.YES);

    public static TruthConstant NO = new TruthConstant(Truth.NO);

    public static TruthConstant UNKNOWN = new TruthConstant(Truth.UNKNOWN);

    public static TruthConstant MU = new TruthConstant(Truth.MU);

    @Override
    public boolean equals(Object obj) {

        if(obj == null) { return false; }

        if(truthValue == null) { return false; }

        if(this == obj) { return true; }

        if(obj instanceof TruthConstant) {

            TruthConstant tc = (TruthConstant) obj;

            if(tc.truthValue == null) { return false; }

            return truthValue.equals(tc.truthValue);

        }

        if(obj instanceof Truth) {

            return truthValue.equals(obj);

        }

        return false;
    }

    public TruthConstant(Truth t) {
        super(t);
        truthValue = t;
    }

}
