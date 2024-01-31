package ai.vital.vitalsigns

import ai.vital.vitalsigns.datatype.Truth

import java.util.regex.Matcher

class VitalSigns {

    static {

        println "Running VitalSigns initialization code..."

        initialize()

    }


    private static VitalSigns instance = null

    public static VitalSigns get() {

        synchronized (VitalSigns.class) {

            if (instance != null) { return instance }

            instance = new VitalSigns()

            // initialize()

            return instance
        }

    }
    private VitalSigns() {

    }

    public static Boolean equality(Object a, Object b) {

        println "VitalSigns equality"

        // defers to extension module
        return a.equals(b)

        // return null
    }

    private static void initialize() {

        // Move into AST?
        Object.metaClass.compareTo = { Object other ->

            /*
            if (delegate instanceof String && other instanceof Person) {
                return delegate.compareTo(other.name)
            }
            */

            println "Comparing " + delegate + "to " + other

            return delegate <=> other
        }

        // Move into AST?
        Object.metaClass.asBoolean = {

            println "AsBoolean"

            if(delegate instanceof Truth) {

                Truth t = (Truth) delegate;

                if(t.equals(Truth.YES)) { return true }

                if(t.equals(Truth.NO)) { return false }

                // This should be an exception, or we could return null
                if(t.equals(Truth.UNKNOWN)) { return false }

                // this should be an exception
                if(t.equals(Truth.MU)) { return false }

            }

            if (delegate == null) return false // null is considered false
            if (delegate instanceof Boolean) return delegate
            if (delegate instanceof Number) return delegate != 0
            if (delegate instanceof String) return !delegate.isEmpty()
            if (delegate instanceof Collection) return !delegate.isEmpty()
            if (delegate.getClass().isArray()) return delegate.length != 0
            if (delegate instanceof Matcher) return delegate.find()
            if (delegate instanceof Map) return !delegate.isEmpty()

            // Catch-all case
            return true // non-null, non-empty objects are considered true

        }


    }


}
