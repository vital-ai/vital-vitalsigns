package ai.vital.query.ops

class Ref {

	String name

	String referencedURI
	
	@Override
	public String toString() {
		return referencedURI
	}
		
}
