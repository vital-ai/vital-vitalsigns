package ai.vital.vitalsigns.command

class GitUtils {

	public static String mockUserEmail = null
	
	public static String getGitUserEmail(File workingDir) throws Exception {
		
		if(mockUserEmail != null) return mockUserEmail
		
		if(System.getenv("git_user_email")) {
			return System.getenv("git_user_email")
		}
		
		StringBuilder sb = new StringBuilder()
		
		int status = ProcessUtils.runProcess(workingDir, ['git', 'config', 'user.email'], false, sb)
		
		if(status != 0) {
			throw new Exception("Command error: ${status} - ${sb.toString()}")
		}
		
		String email = sb.toString().trim();
		
		if( ! email ) throw new Exception("No git config user.email set")
		
		return email
		
	}
	
}
