package ai.vital.query

abstract class AggregateFunction {

	//one of
	String propertyURI
	
	String variableName

	static class Average extends AggregateFunction {
		
	} 
	
	static class Count extends AggregateFunction {
		
	}
	
	static class CountDistinct extends AggregateFunction {
		
	}
	
	static class Max extends AggregateFunction {
		
	}
	
	static class Min extends AggregateFunction {
		
	}
	
	static class Sum extends AggregateFunction {
		
	}
	
}

