package ai.vital.vitalsigns.command.patterns

import java.util.regex.Matcher
import java.util.regex.Pattern

class JarFileInfo implements Cloneable, Comparable<JarFileInfo> {

	public final static Pattern domain_user_down_version = 
		Pattern.compile('(?<domain>.+)\\-user-(?<user>.+)-down(?<down>\\d+)\\-groovy\\-(?<major>\\d+)\\.(?<minor>\\d+)\\.(?<patch>\\d+)\\.jar');
		
	public final static Pattern domain_user_version = 
		Pattern.compile('(?<domain>.+)\\-user-<?<user>\\-groovy\\-(?<major>\\d+)\\.(?<minor>\\d+)\\.(?<patch>\\d+)\\.jar');
			
	public final static Pattern domain_down_version = 
		Pattern.compile('(?<domain>.+)\\-down(?<down>\\d+)\\-groovy\\-(?<major>\\d+)\\.(?<minor>\\d+)\\.(?<patch>\\d+)\\.jar');
			
	public final static Pattern domain_version = 
		Pattern.compile('(?<domain>.+)\\-groovy\\-(?<major>\\d+)\\.(?<minor>\\d+)\\.(?<patch>\\d+)\\.jar');
	
	public final static String archivePattern = "<domain>-[user-<user>-][down<down>-]-groovy-<major>.<minor>.<patch>.jar";
		
	String domain
	
	String useremail
	
	Integer down
	
	Integer major
	
	Integer minor 
	
	Integer patch
	
	public static JarFileInfo fromStringUnsafe(String fname) {
		
		JarFileInfo i = null
		
		try {
		
			i = fromString(fname)
				
		} catch(Exception e) {
		
		}
		
		return i
	}
	
	public static JarFileInfo fromString(String fname) {
		
		boolean user = false
		boolean down = false
		
		Matcher m = null
		
		if( ( m = domain_user_down_version.matcher(fname)).matches() ) {
			
			user = true
			down = true
			
		} else if( ( m = domain_user_version.matcher(fname)).matches() ) {
		
			user = true
		
		} else if( ( m = domain_down_version.matcher(fname)).matches() ) {
		
			down = true	
		
		} else if( ( m = domain_version.matcher(fname)).matches() ) {
		
		} else {

			throw new Exception("Invalid JAR filename: ${fname}")
				
		}
		
		JarFileInfo i = new JarFileInfo()
		
		i.domain = m.group("domain")
		
		i.major = Integer.parseInt(m.group("major"))
		
		i.minor = Integer.parseInt(m.group("minor"))
		
		i.patch = Integer.parseInt(m.group("patch"))
		
		if(user) {
			
			i.useremail = unescapeEmail(m.group("user"))
			
		}
		
		if(down) {
			
			i.down = Integer.parseInt(m.group("down"))
			
		}
		
		return i
		
	}
	
	private static String escapeUserEmail(String input) {
		return input.replace("@", "-AT-")
	}
	
	private static String unescapeEmail(String input) {
		return input.replace("-AT-", "@")
	}
	
	public String toFileName() {
		
		StringBuilder sb = new StringBuilder(this.domain).append('-')
		
		if(useremail) {
			
			sb.append('user-').append(escapeUserEmail(useremail)).append('-')
			
		}
		
		if(down) {
			
			sb.append('down').append(down).append('-')
			
		}
		
		sb.append('groovy-').append(major).append('.').append(minor).append('.').append(patch).append('.jar')
		
		return sb.toString()
		
	}
	
	@Override
	public boolean equals(Object obj) {

		if(!(obj instanceof JarFileInfo)) return false
		
		JarFileInfo a = (JarFileInfo)obj
		
		if( ! f( domain, a.domain ) ) return false
		if( ! f( down, a.down ) ) return false
		if( ! f( major, a.major ) ) return false
		if( ! f( minor, a.minor ) ) return false
		if( ! f( patch, a.patch ) ) return false
		
		return true
		
	}

	private boolean f(Object v1, Object v2) {
		
		if(v1 == null && v2 == null) {
			return true
		} else if( ( v1 != null && v2 == null) || (v1 == null && v2 != null) ) {
			return false
		} else {
			return v1.equals(v2)
		}
		
		
	}

	@Override
	public Object clone() {
		return super.clone();
	}
	

	public static JarFileInfo fromOwlInfo(OwlFileInfo ofi) {
		
		JarFileInfo jfi = new JarFileInfo()
		
		jfi.domain = ofi.domain
		jfi.down = ofi.down
		jfi.major = ofi.major
		jfi.minor = ofi.minor
		jfi.patch = ofi.patch
		jfi.useremail = ofi.useremail
		
		return jfi
		
	}
    
	public static JarFileInfo fromJsonSchemaInfo(JsonSchemaFileInfo j) {
	    
	    JarFileInfo jfi = new JarFileInfo()
	    
	    jfi.domain = j.domain
	    jfi.down = j.down
	    jfi.major = j.major
	    jfi.minor = j.minor
	    jfi.patch = j.patch
	    jfi.useremail = j.useremail
	    
	    return jfi
	            
	}
	
	public String toVersionNumberString() {
		return this.major + '.' + this.minor + '.' + this.patch
	}

	@Override
	public int compareTo(JarFileInfo o) {
		
		int max = major.compareTo(o.major)
		
		if(max != 0) return max
		
		int min = minor.compareTo(o.minor)
		
		if(min != 0) return min
		
		return patch.compareTo(o.patch);
	}
}
