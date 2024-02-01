package ai.vital.query

class Edge extends Type {

	Map storage = [:]
	
	
	String type
	
	def propertyMissing(String name, value) { storage[name] = value }
	def propertyMissing(String name) { storage[name] }
	
	
}
