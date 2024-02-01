package ai.vital.vitalservice.query

enum AggregationType {

	/**
	 * average value ( sum / count ), if count == 0 then Double.NaN
	 */
	average,
	
	/**
	 * returns number of occurrences of given property across select query results
	 */
	count,
	
	/**
	 * returns maximum value of given property across select query results, if count == 0 then Double.NaN 
	 */
	max,
	
	/**
	 * returns minimum value of given property across select query results, if count == 0 then Double.NaN
	 */
	min,
	
	/**
	 * returns the sum of all properties values across select query results
	 */
	sum
	
	
	
}
