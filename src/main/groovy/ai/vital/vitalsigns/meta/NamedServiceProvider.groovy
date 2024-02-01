package ai.vital.vitalsigns.meta;

// entry point for names services support
public abstract class NamedServiceProvider {

    public static NamedServiceProvider provider;
    
    public abstract Object getNamedService(String name);
    
}
