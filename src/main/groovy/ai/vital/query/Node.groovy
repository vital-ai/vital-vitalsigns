package ai.vital.query

class Node extends Type {

	Map storage = [:]
	
	
	String type
	
	def propertyMissing(String name, value) { storage[name] = value }
	def propertyMissing(String name) { storage[name] }
	
	
}
