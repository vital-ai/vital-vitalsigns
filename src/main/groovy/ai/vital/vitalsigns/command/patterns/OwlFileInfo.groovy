package ai.vital.vitalsigns.command.patterns

import java.util.regex.Matcher
import java.util.regex.Pattern

class OwlFileInfo implements Cloneable, Comparable<OwlFileInfo> {

	public final static Pattern domain_user_down_version = 
		Pattern.compile('(?<domain>.+)\\-user-(?<user>.+)-down(?<down>\\d+)-(?<major>\\d+)\\.(?<minor>\\d+)\\.(?<patch>\\d+)\\.owl');
		
	public final static Pattern domain_user_version = 
		Pattern.compile('(?<domain>.+)\\-user-(?<user>.+)-(?<major>\\d+)\\.(?<minor>\\d+)\\.(?<patch>\\d+)\\.owl');
			
	public final static Pattern domain_down_version = 
		Pattern.compile('(?<domain>.+)\\-down(?<down>\\d+)-(?<major>\\d+)\\.(?<minor>\\d+)\\.(?<patch>\\d+)\\.owl');
			
	public final static Pattern domain_version = 
		Pattern.compile('(?<domain>.+)\\-(?<major>\\d+)\\.(?<minor>\\d+)\\.(?<patch>\\d+)\\.owl');
	
	public final static String archivePattern = "<domain>-[user-<user>-][down<down>-]-<major>.<minor>.<patch>.owl";
		
	String domain
	
	String useremail
	
	Integer down
	
	Integer major
	
	Integer minor 
	
	Integer patch
	
	public static OwlFileInfo fromStringUnsafe(String fname) {
		
		OwlFileInfo i = null
		
		try {
		
			i = OwlFileInfo.fromString(fname)
				
		} catch(Exception e) {
		
		}
		
		return i
		
	}
	
	public static OwlFileInfo fromString(String fname) {
		
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

			throw new Exception("Invalid OWL filename: ${fname}")
				
		}
		
		OwlFileInfo i = new OwlFileInfo()
		
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
		
		sb.append(major).append('.').append(minor).append('.').append(patch).append('.owl')
		
		return sb.toString()
		
	}
	
	@Override
	public boolean equals(Object obj) {

		if(!(obj instanceof OwlFileInfo)) return false
		
		OwlFileInfo a = (OwlFileInfo)obj
		
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

	public String toVersionNumberString() {
		return this.major + '.' + this.minor + '.' + this.patch
	}	
	
	@Override
	public int compareTo(OwlFileInfo o) {
		
		int max = major.compareTo(o.major)
		
		if(max != 0) return max
		
		int min = minor.compareTo(o.minor)
		
		if(min != 0) return min
		
		return patch.compareTo(o.patch);
	}
	
    public static OwlFileInfo fromJarInfo(JarFileInfo j) {
        
        OwlFileInfo ofi = new OwlFileInfo()
        
        ofi.domain = j.domain 
        ofi.down = j.down
        ofi.major = j.major
        ofi.minor = j.minor
        ofi.patch = j.patch
        ofi.useremail = j.useremail
        
        return ofi
        
    }
    
    public static OwlFileInfo fromJsonSchemaInfo(JsonSchemaFileInfo j) {
        
        OwlFileInfo ofi = new OwlFileInfo()
        
        ofi.domain = j.domain 
        ofi.down = j.down
        ofi.major = j.major
        ofi.minor = j.minor
        ofi.patch = j.patch
        ofi.useremail = j.useremail
        
        return ofi
                
    }
		
}
