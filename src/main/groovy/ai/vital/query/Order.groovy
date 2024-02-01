package ai.vital.query

enum Order {

	ASC, 
	ASCENDING,
	 
	DESC, 
	DESCENDING;

	public String toShortString() {

		if(this == ASC || this == ASCENDING) {
			return 'asc'
		} else {
			return 'desc'
		}
	}
	
}
