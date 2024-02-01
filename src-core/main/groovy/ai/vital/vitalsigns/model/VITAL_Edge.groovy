package ai.vital.vitalsigns.model

import ai.vital.vitalsigns.model.GraphObject

import ai.vital.vitalsigns.model.property.IProperty;
import ai.vital.vitalsigns.model.property.URIProperty;


class VITAL_Edge extends GraphObject<VITAL_Edge> {

    private static final long serialVersionUID = 1L;


    public VITAL_Edge() {
        super()
    }

    public VITAL_Edge(Map<String, Object> props) {
        super(props)
    }


    @Override
    public void setProperty(String pname, Object newValue) {
        if(pname.equals("sourceURI")) {
            pname = "edgeSource"
        } else if(pname.equals("destinationURI")) {
            pname = "edgeDestination"
        } else if(pname.equals("index")) {
            pname = "listIndex"
        }
        super.setProperty(pname, newValue);
    }

    @Override
    public Object getProperty(String pname) {
        if(pname.equals("destinationURI") || pname.equals("sourceURI") || pname.equals("index")) {
            String _pname = pname.equals("destinationURI") ? "edgeDestination" : ( pname.equals( "sourceURI" ) ? "edgeSource" : "listIndex")
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

    public VITAL_Edge addSource(VITAL_Node source) {
        if(source == null) throw new NullPointerException("Source node cannot be null")
        if(source.URI == null) throw new NullPointerException("Source node's URI cannot be null")
        this.edgeSource = source.URI;
        return this;
    }

    public VITAL_Edge addDestination(VITAL_Node destination) {
        if(destination == null) throw new NullPointerException("Destination node cannot be null")
        if(destination.URI == null) throw new NullPointerException("Destination node's URI cannot be null")
        this.edgeDestination = destination.URI;
        return this;
    }

    public void setSourceURI(String srcURI) {
        this.setProperty("edgeSource", srcURI);
    }

    public String getSourceURI() {
        IProperty srcURI = this.getProperty("edgeSource")
        if(srcURI != null) return ((URIProperty)srcURI.unwrapped()).get();
        return null;
    }

    public void setDestinationURI(String destURI) {
        this.setProperty("edgeDestination", destURI);
    }

    public String getDestinationURI() {
        IProperty destURI = this.getProperty("edgeDestination")
        if(destURI != null) return ((URIProperty)destURI.unwrapped()).get();
        return null;
    }

}
