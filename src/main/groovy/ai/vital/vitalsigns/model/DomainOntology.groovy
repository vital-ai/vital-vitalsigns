package ai.vital.vitalsigns.model

import java.util.Set;
import java.util.regex.Matcher
import java.util.regex.Pattern;

import ai.vital.vitalsigns.command.patterns.OwlFileInfo;
import ai.vital.vitalsigns.ontology.VitalCoreOntology;

class DomainOntology implements Comparable<DomainOntology>, Serializable, Cloneable {

	static String defaultVersion = '0.1.0'
	
	static Pattern versionPattern = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)")
	
	static Pattern versionIRIPattern = Pattern.compile("([^\$]+)\\\$(\\d+)\\.(\\d+)\\.(\\d+)")
	
    private Set<String> parentOntologies
    
	public DomainOntology(String uri, String version, String backwardCompatibleVersion) {
		
		this.uri = uri
		
		Matcher matcher = versionPattern.matcher(version)
		
		if( ! matcher.matches() ) throw new RuntimeException("Ontology ${uri} owl:version string: ${version} invalid. It has to match pattern: ${versionPattern}")
		
		this.major = Integer.parseInt(matcher.group(1))
		
		this.minor = Integer.parseInt(matcher.group(2))
		
		this.patch = Integer.parseInt(matcher.group(3))
		
        if(backwardCompatibleVersion) {
            
            Matcher matcher2 = versionPattern.matcher(backwardCompatibleVersion)
            
            if( ! matcher2.matches() ) throw new RuntimeException("Ontology ${uri} vital-core:${VitalCoreOntology.hasBackwardCompatibilityVersion.getLocalName()} version string: ${backwardCompatibleVersion} invalid. It has to match pattern: ${versionPattern}")

            DomainOntology backV = new DomainOntology(uri, backwardCompatibleVersion);
            
            int c = this.compareTo(backV);
            
            if(c <= 0) throw new RuntimeException("Ontology ${uri} version ${this.toVersionString()} } must be greater than its vital-core:${VitalCoreOntology.hasBackwardCompatibilityVersion.getLocalName()} value: ${backwardCompatibleVersion}")
            
            this.backwardCompatibleVersion = backV
            
        }
		
	}
    
    public DomainOntology(String uri, String version) {
        this(uri, version, null)
    }
    
	
	public DomainOntology(String uri, Integer major, Integer minor, Integer patch) {
		
		this.uri = uri
		this.major = major
		this.minor = minor
		this.patch = patch
		
	}
	
	public DomainOntology(String versionIRI) {
		
		Matcher matcher = versionIRIPattern.matcher(versionIRI)
		
		if( ! matcher.matches() ) throw new RuntimeException("Version IRI ${versionIRI} does not match pattern: ${versionIRIPattern.pattern()}")
		
		this.uri = matcher.group(1)
		
		this.major = Integer.parseInt(matcher.group(2))
		
		this.minor = Integer.parseInt(matcher.group(3))
		
		this.patch = Integer.parseInt(matcher.group(4))
		
	}
	
	String uri
	
	String defaultPackage
	
	Integer major
	
	Integer minor
	
	Integer patch
    
    DomainOntology backwardCompatibleVersion
    
    private Set<String> preferredImportVersions
    
    String defaultArtifactId
    
    String defaultGroupId
    
	public Set<String> getPreferredImportVersions() {
        return preferredImportVersions;
    }

    public void setPreferredImportVersions(Set<String> preferredImportVersions) {
        
        if(preferredImportVersions != null) {
            
            for(String v : preferredImportVersions) {
                OwlFileInfo ofi = OwlFileInfo.fromStringUnsafe(v)
                if(ofi == null) throw new RuntimeException("Preferred version import value: $v does no match pattern: " + OwlFileInfo.domain_version.pattern());
            }
            
        }
        this.preferredImportVersions = preferredImportVersions;
    }

    @Override
	public String toString() {
		return toVersionIRI()
	}
	
	public String toVersionIRI() {
		return "${uri}\$${major}.${minor}.${patch}"
	}
	
	public static DomainOntology fromVersionIRI(String versionIRI) {
		return new DomainOntology(versionIRI)
	} 
	
	public String toVersionString() {
		return "${major}.${minor}.${patch}"
	}
    

	@Override
	public int compareTo(DomainOntology o) {
		
		int max = major.compareTo(o.major)
		
		if(max != 0) return max
		
		int min = minor.compareTo(o.minor)
		
		if(min != 0) return min 
		
		return patch.compareTo(o.patch);
	}
	
	@Override
	public boolean equals(Object obj) {

		if(obj instanceof DomainOntology) {
			DomainOntology o = (DomainOntology)obj
			return this.uri.equals(o.uri) && this.compareTo(o) == 0
		}
		
		return super.equals(obj);
	}

    public void setDirectParentOntologies(Set<String> parentOntologies) {
        this.parentOntologies = parentOntologies;
    }

    public Set<String> getParentOntologies() {
        return parentOntologies;
    }
    	
}
