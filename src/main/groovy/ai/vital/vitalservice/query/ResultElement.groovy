package ai.vital.vitalservice.query

import ai.vital.vitalsigns.model.GraphObject;

class ResultElement implements Serializable {

	private static final long serialVersionUID = 222333444555666L;
	
	GraphObject graphObject
	
	double score

	public ResultElement() {
        
    }
    
	public ResultElement(GraphObject graphObject, double score) {
		super();
		this.graphObject = graphObject;
		this.score = score;
	}
	
	@Override
	public String toString() {
		return "Result,score:${score} - ${graphObject.toString()}";
	}
	
		
}
