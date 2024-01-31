package ai.vital.vitalsigns.constant;

public final class HasValueConstant implements ConsiderValueConstant {

    private String id;

    private HasValueConstant(String id) {

        this.id = id;

    }

    private static final String hasValue = "hasValue";

    public static HasValueConstant HasValue = new HasValueConstant(hasValue);

}
