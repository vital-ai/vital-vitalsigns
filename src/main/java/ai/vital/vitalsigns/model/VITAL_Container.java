package ai.vital.vitalsigns.model;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import ai.vital.lucene.model.LuceneSegment;
import ai.vital.lucene.model.LuceneSegmentType;
import ai.vital.service.lucene.impl.LuceneServiceQueriesImpl;
import ai.vital.service.lucene.model.LuceneSegmentConfig;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalservice.query.VitalQuery;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.model.container.GraphObjectsIterable;

public class VITAL_Container implements GraphObjectsIterable, Closeable, Serializable {

    private static final long serialVersionUID = 1L;

    public String URI;

    protected Map<String, GraphObject> graphObjects = null;//new HashMap<String, GraphObject>();

    transient private LuceneSegment segment = null;

    private boolean queryable;

    /**
     * non-queryable
     */
    public VITAL_Container() {
        this(false);
    }

    /**
     * @param queryable
     */
    public VITAL_Container(boolean queryable) {

        this.queryable = queryable;

        if(queryable) {

            initSegment();

        } else {
            graphObjects = new HashMap<String, GraphObject>();
        }

    }

    private void initSegment() {

        String id = RandomStringUtils.randomAlphanumeric(16);

        VitalOrganization org = VitalOrganization.withId(id);

        VitalApp app = VitalApp.withId(id);

        VitalSegment seg = VitalSegment.withId(id);

        LuceneSegmentConfig config = new LuceneSegmentConfig(LuceneSegmentType.memory, true, false, null);
        segment = new LuceneSegment(org, app, seg, config);
        try {
            segment.open();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(this.graphObjects != null) {
            try {
                segment.insertOrUpdateBatch(this.graphObjects.values());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        this.graphObjects = null;

    }

    public Collection<GraphObject> getAllObjects() {

        if(segment != null) {
            return segment.getAllObjects();
        } else {
            return new HashSet<GraphObject>( this.graphObjects.values() );
        }

    }

    public Map<String, GraphObject> getMap() {
        if(segment != null) {
            return segment.getMap();
        } else {
            return graphObjects;
        }
    }

    public void putGraphObject(GraphObject g) {
        if(segment != null) {
            try {
                segment.insertOrUpdate(g);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            graphObjects.put(g.getURI(), g);
        }
    }

    public void putGraphObjects(List<GraphObject> list) {
        if(segment != null) {
            try {
                segment.insertOrUpdateBatch(list);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            for(GraphObject go : list) {
                graphObjects.put(go.getURI(), go);
            }
        }
    }

    @Override
    public Iterator<GraphObject> iterator() {
        if(segment != null) {
            return segment.getMap().values().iterator();
        } else {
            return graphObjects.values().iterator();
        }
    }

    public void remove(String URI) {

        if(segment != null) {
            try {
                segment.delete(URI);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            graphObjects.remove(URI);
        }


    }

    /**
     * Returns objects of particular type
     * @param iterator
     * @param strict true if no subclasses, all compatible types otherwise
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends GraphObject> Iterator<T> iterator(Class<T> cls, boolean strict) {

        List<T> typedList = new ArrayList<T>();

        for(Iterator<GraphObject> iter = this.iterator(); iter.hasNext();) {

            GraphObject go = iter.next();

            if(strict && go.getClass().equals(cls)) {

                typedList.add((T) go);

            } else if(cls.isInstance(go)) {

                typedList.add((T) go);

            }

        }

        return typedList.iterator();

    }

    @Override
    public <T extends GraphObject> Iterator<T> iterator(Class<T> cls) {
        return this.iterator(cls, false);
    }

    @Override
    public GraphObject get(String uri) {
        if(segment != null) {
            return segment.get(uri);
        } else {
            return graphObjects.get(uri);
        }
    }


    @Override
    public boolean isEdgeIndexEnabled() {
        return segment != null;
    }

    @Override
    public Map<String, Set<String>> getSrcURI2Edge() {
        return segment.getSrcURI2Edge();
    }

    @Override
    public Map<String, Set<String>> getDestURI2Edge() {
        return segment.getDestURI2Edge();
    }

    @Override
    public Map<String, Set<String>> getSrcURI2HyperEdge() {
        return segment.getSrcURI2HyperEdge();
    }

    @Override
    public Map<String, Set<String>> getDestURI2HyperEdge() {
        return segment.getDestURI2HyperEdge();
    }

    @Override
    public LuceneSegment getLuceneSegment() {
        if(segment == null) throw new RuntimeException("container not set as a queryable");
        return segment;
    }

    public ResultList query(VitalQuery query) {
        if(segment == null) throw new RuntimeException("container not set as a queryable");
        return LuceneServiceQueriesImpl.handleQuery(VitalSigns.get().getOrganization(), VitalApp.withId("container"), query, Arrays.asList(segment));
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(segment);
    }


    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {

        // our "pseudo-constructor"
        in.defaultReadObject();

        if(this.queryable) {

            initSegment();

        } else {

            //graph objects should be copied

        }


    }


    private void writeObject(ObjectOutputStream out) throws IOException {

        if(segment != null) {

            this.graphObjects = new HashMap<String, GraphObject>();

            this.graphObjects.putAll(segment.getMap());

            this.close();

            this.segment = null;

        }

        out.defaultWriteObject();

    }


    public List<GraphObject> getGraphRoots() {

        List<GraphObject> gos = new ArrayList<GraphObject>();

        //look for top objects, objects that don't have
        Set<String> nonTopObjects = new HashSet<String>();

        Collection<GraphObject> c = null;

        if(segment != null) {
            c = segment.getAllObjects();
        } else {
            c = graphObjects.values();
        }

        for(GraphObject g : c) {
            if(g instanceof VITAL_Edge) {
                VITAL_Edge edge = (VITAL_Edge) g;
                nonTopObjects.add(edge.getDestinationURI());
            } else if(g instanceof VITAL_HyperEdge) {
                VITAL_HyperEdge hedge = (VITAL_HyperEdge) g;
                nonTopObjects.add(hedge.getDestinationURI());
            }
        }

        for(GraphObject g : c) {
            if(g instanceof VITAL_Node || g instanceof VITAL_HyperNode) {
                if(!nonTopObjects.contains(g.getURI())) {
                    gos.add(g);
                }
            }
        }

        return gos;

    }
}
