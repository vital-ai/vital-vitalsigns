package ai.vital.vitalsigns.model

import ai.vital.vitalsigns.model.GraphObject

import java.util.List;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.classes.ClassMetadata;
import ai.vital.vitalsigns.meta.EdgeAccessImplementation;
import ai.vital.vitalsigns.meta.EdgesResolver
import ai.vital.vitalsigns.meta.GraphContext

class VITAL_Node extends GraphObject<VITAL_Node> {

    private static final long serialVersionUID = 1L;

    public VITAL_Node() {
        super()
    }

    public VITAL_Node(Map<String, Object> props) {
        super(props)
    }

    public List<VITAL_Edge> getOutgoingEdges() {
        return getOutgoingEdges(null, null);
    }

    public List<VITAL_Edge> getOutgoingEdges(GraphContext graphContext, VITAL_Container container) {

        if(graphContext == null) graphContext = GraphContext.Local;

        if(graphContext != GraphContext.Container && container != null) {
            throw new RuntimeException("GraphContext: " + graphContext + " cannot be used with containers." );
        }

        String uri = this.URI;

        EdgesResolver resolver = VitalSigns.get().getEdgesResolver(graphContext);

        if( resolver == null ) {
            throw new RuntimeException("No edges resolver set - vital signs requires it!");
        }

        List<VITAL_Edge> res = resolver.getEdgesForSrcURI(uri, container);

        return res;
    }

    //six variants
    //1
    public List<VITAL_Edge> getEdges() {
        return EdgeAccessImplementation.edgeAccessImplementation(this, null, true, true)
    }

    public List<VITAL_Edge> getEdgesOut() {
        return EdgeAccessImplementation.edgeAccessImplementation(this, null, true, false)
    }

    public List<VITAL_Edge> getEdgesIn() {
        return EdgeAccessImplementation.edgeAccessImplementation(this, null, false, true)
    }

    //2
    public List<VITAL_Edge> getEdges(VITAL_Node node2) {
        return EdgeAccessImplementation.edgeAccessImplementation(this, null, true, true, node2)
    }

    public List<VITAL_Edge> getEdgesOut(VITAL_Node node2) {
        return EdgeAccessImplementation.edgeAccessImplementation(this, null, true, false, node2)
    }

    public List<VITAL_Edge> getEdgesIn(VITAL_Node node2) {
        return EdgeAccessImplementation.edgeAccessImplementation(this, null, false, true, node2)
    }


    //3
    public List<VITAL_Edge> getEdges(GraphContext graphContext) {
        return EdgeAccessImplementation.edgeAccessImplementation(this, null, true, true, graphContext)
    }

    public List<VITAL_Edge> getEdgesOut(GraphContext graphContext) {
        return EdgeAccessImplementation.edgeAccessImplementation(this, null, true, false, graphContext)
    }

    public List<VITAL_Edge> getEdgesIn(GraphContext graphContext) {
        return EdgeAccessImplementation.edgeAccessImplementation(this, null, false, true, graphContext)
    }


    //4
    public List<VITAL_Edge> getEdges(GraphContext graphContext, VITAL_Node node2) {
        return EdgeAccessImplementation.edgeAccessImplementation(this, null, true, true, graphContext, node2)
    }

    public List<VITAL_Edge> getEdgesOut(GraphContext graphContext, VITAL_Node node2) {
        return EdgeAccessImplementation.edgeAccessImplementation(this, null, true, false, graphContext, node2)
    }

    public List<VITAL_Edge> getEdgesIn(GraphContext graphContext, VITAL_Node node2) {
        return EdgeAccessImplementation.edgeAccessImplementation(this, null, false, true, graphContext, node2)
    }


    //5
    public List<VITAL_Edge> getEdges(GraphContext graphContext, List<VITAL_Container> containers) {
        return EdgeAccessImplementation.edgeAccessImplementation(this, null, true, true, graphContext, containers)
    }

    public List<VITAL_Edge> getEdgesOut(GraphContext graphContext, List<VITAL_Container> containers) {
        return EdgeAccessImplementation.edgeAccessImplementation(this, null, true, false, graphContext, containers)
    }

    public List<VITAL_Edge> getEdgesIn(GraphContext graphContext, List<VITAL_Container> containers) {
        return EdgeAccessImplementation.edgeAccessImplementation(this, null, false, true, graphContext, containers)
    }


    //6
    public List<VITAL_Edge> getEdges(GraphContext graphContext, List<VITAL_Container> containers, VITAL_Node node2) {
        return EdgeAccessImplementation.edgeAccessImplementation(this, null, true, true, graphContext, containers, node2)
    }

    public List<VITAL_Edge> getEdgesOut(GraphContext graphContext, List<VITAL_Container> containers, VITAL_Node node2) {
        return EdgeAccessImplementation.edgeAccessImplementation(this, null, true, false, graphContext, containers, node2)
    }

    public List<VITAL_Edge> getEdgesIn(GraphContext graphContext, List<VITAL_Container> containers, VITAL_Node node2) {
        return EdgeAccessImplementation.edgeAccessImplementation(this, null, false, true, graphContext, containers, node2)
    }


    public List<VITAL_Edge> getIncomingEdges() {
        return getIncomingEdges(null, null);
    }

    public List<VITAL_Edge> getIncomingEdges(GraphContext graphContext, VITAL_Container container) {

        if(graphContext == null) graphContext = GraphContext.Local;

        if(graphContext != GraphContext.Container && container != null) {
            throw new RuntimeException("GraphContext: " + graphContext + " cannot be used with containers." );
        }

        String uri = this.URI;

        EdgesResolver resolver = VitalSigns.get().getEdgesResolver(graphContext);

        if( resolver == null ) {
            throw new RuntimeException("No edges resolver set - vital signs requires it!");
        }

        List<VITAL_Edge> res = resolver.getEdgesForDestURI(uri, container);

        return res;
    }


    //java hack?

    public List<VITAL_Node> getCollection(String collectionName) {
        return getCollection(collectionName, GraphContext.Local);
    }

    public List<VITAL_Node> getCollection(String collectionName, GraphContext graphContext) {
        return getCollection(collectionName, graphContext, null);
    }

    public List<VITAL_Node> getCollection(String collectionName, GraphContext graphContext, VITAL_Container... containers) {

        //look for
        Class<? extends VITAL_Edge> edgeClass = null;

        List<ClassMetadata> edges = VitalSigns.get().getClassesRegistry().getEdgeClassesWithSourceOrDestNodeClass(this.getClass(), true);

        for(ClassMetadata cm : edges) {

            if(cm.edgePluralName == collectionName) {
                edgeClass = cm.getClazz()
                break;
            }

        }

        if(edgeClass == null) throw new RuntimeException("Collection ${collectionName} not found for class: ${this.getClass()}");


        return EdgeAccessImplementation.collectionImplementation(this, edgeClass, graphContext, containers, true);

    }
}

