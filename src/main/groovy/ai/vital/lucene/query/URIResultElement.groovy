package ai.vital.lucene.query

import ai.vital.lucene.model.LuceneSegment
import ai.vital.vitalsigns.model.GraphObject

/**
 * internal result element
 *
 */
class URIResultElement {

	public String URI

	/* these properties only set in graph query */
	public String typeURI
	
	public String sourceURI
	
	public String destinationURI
	
	public double score

	//keep the reference to segment object to avoid miss-lookups
	public LuceneSegment segment
	
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof URIResultElement) && ((URIResultElement)obj).URI.equals(URI);
	}	
}
