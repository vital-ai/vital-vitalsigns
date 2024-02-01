package ai.vital.vitalsigns.datatype;


public class Truth {

    protected static final String YES_URI = "truth:YES";
    protected static final String NO_URI = "truth:NO";
    protected static final String UNKNOWN_URI = "truth:UNKNOWN";
    protected static final String MU_URI = "truth:MU";

    public static final Truth YES = new Truth(YES_URI);
    public static final Truth NO = new Truth(NO_URI);
    public static final Truth UNKNOWN = new Truth(UNKNOWN_URI);
    public static final Truth MU = new Truth(MU_URI);

    protected String uri = null;

    protected Truth(String value) {
        this.uri = value;
    }

    protected Truth(Truth t) {
        this.uri = t.uri;
    }

    public static Truth of(String value) {

        return switch (value) {
            case YES_URI -> YES;
            case NO_URI -> NO;
            case UNKNOWN_URI -> UNKNOWN;
            case MU_URI -> MU;
            default -> throw new IllegalArgumentException("Invalid URI value for Truth: " + value);
        };
    }

    public String getValue() {
        return uri;
    }

    public String name() { return uri; }

    @Override
    public String toString() {
        return "Truth{" +
                "URI='" + uri + '\'' +
                '}';
    }

    public String asString() {

        return toString();

    }

    @Override
    public boolean equals(Object obj) {

        if(obj == null) { return false; }

        if(this == obj) { return true; }

        if(obj instanceof Truth) {

            Truth t = (Truth) obj;

            if(this.uri.equals(t.uri)) { return true; }

        }

      return false;
    }

    public static Truth fromBoolean(Boolean b) {

        if(b == true) { return Truth.YES; }

        if(b == false) { return Truth.NO; }

        // Null
        return Truth.UNKNOWN;
    }


    public static Truth fromString(String s) {

        if(s == null) throw new RuntimeException("Truth string must not be null");

        // TODO check string
        return of(s);

    }

}
