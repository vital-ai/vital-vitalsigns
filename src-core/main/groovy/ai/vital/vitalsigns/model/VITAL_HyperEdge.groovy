package ai.vital.vitalsigns.model

import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.property.IProperty
import ai.vital.vitalsigns.model.property.URIProperty;


class VITAL_HyperEdge extends GraphObject<VITAL_HyperEdge> {

    private static final long serialVersionUID = 1L;


    public VITAL_HyperEdge() {
        super()
    }

    public VITAL_HyperEdge(Map<String, Object> props) {
        super(props)
    }


    @Override
    public void setProperty(String pname, Object newValue) {
        if(pname.equals("sourceURI")) {
            pname = "hyperEdgeSource"
        } else if(pname.equals("destinationURI")) {
            pname = "hyperEdgeDestination"
        } else if(pname.equals("index")) {
            pname = "listIndex"
        }
        super.setProperty(pname, newValue);
    }

    @Override
    public Object getProperty(String pname) {
        if(pname.equals("destinationURI") || pname.equals("sourceURI") || pname.equals("index")) {
            String _pname = pname.equals("destinationURI") ? "hyperEdgeDestination" : ( pname.equals( "sourceURI" ) ? "hyperEdgeSource" : "listIndex")
            Object p = super.getProperty(_pname);
            if(p != null) {
                if(pname.equals("index")) {
                    return p.rawValue()
                } else {
                    return p.get();
                }
            }
            return null;
        }

        return super.getProperty(pname);

    }

    public ValidationStatus validate() {

        ValidationStatus status = super.validate()

        if(sourceURI == null) status.putError('sourceURI', 'null_source_uri_field')
        if(sourceURI.isEmpty()) status.putError('sourceURI', 'empty_source_uri_field')

        if(destinationURI == null) status.putError('destinationURI', 'null_destination_uri_field')
        if(destinationURI.isEmpty()) status.putError('destinationURI', 'empty_destination_uri_field')

        //TODO endpoints URI validation

        return status

    }

    public VITAL_HyperEdge addSource(GraphObject source) {
        if(source == null) throw new NullPointerException("Source graph object cannot be null")
        if(source.URI == null) throw new NullPointerException("Source graph object's URI cannot be null")
        this.hyperEdgeSource = source.URI;
        return this;
    }

    public VITAL_HyperEdge addDestination(GraphObject destination) {
        if(destination == null) throw new NullPointerException("Destination graph object cannot be null")
        if(destination.URI == null) throw new NullPointerException("Destination graph object's URI cannot be null")
        this.hyperEdgeDestination = destination.URI;
        return this;
    }

    public void setSourceURI(String srcURI) {
        this.setProperty("hyperEdgeSource", srcURI);
    }

    public String getSourceURI() {
        IProperty srcURI = this.getProperty("hyperEdgeSource")
        if(srcURI != null) return ((URIProperty)srcURI.unwrapped()).get();
        return null;
    }

    public void setDestinationURI(String destURI) {
        this.setProperty("hyperEdgeDestination", destURI);
    }

    public String getDestinationURI() {
        IProperty destURI = this.getProperty("hyperEdgeDestination")
        if(destURI != null) return ((URIProperty)destURI.unwrapped()).get();
        return null;
    }

}
