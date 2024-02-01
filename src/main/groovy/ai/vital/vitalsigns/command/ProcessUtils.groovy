package ai.vital.vitalsigns.command

class ProcessUtils {

	public static int runProcess(File processHome, List cmd, boolean exitOnError) {
		return runProcess(processHome, cmd, exitOnError, null)
	}
	
	public static int runProcess(File processHome, List cmd, boolean exitOnError, final StringBuilder buffer) {

//		StringBuilder c = new StringBuilder()
//		for(String s : cmd) {
//			if(c.length() > 0) c.append(' ')
//			c.append(s)
//		}
//		println("SKIPPED: ${c}");
//		return 0;

		Process process = new ProcessBuilder(cmd).directory(processHome).redirectErrorStream(true).start()
		
		InputStream is = process.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		
		String line;
		int exit = -1;
		
		while ((line = br.readLine()) != null) {
			// Outputs your process execution
			if(buffer != null) {
				buffer.append(line).append((String)"\n")
			} else {
				println line
			}
		}
		
		br.close()
		
		Integer v = null
		
		try {
			v = process.exitValue();
//			println "Process returned immediatey, code: ${v}"
		} catch (IllegalThreadStateException t) {
		}

		if(v == null) {
//			println "Still waiting for the process..."
			v = process.waitFor()
//			println "finally returned code: ${v}"
		}
				
		/*
		process.inputStream.eachLine {
			println it
			if(buffer != null) {
				buffer.append(it).append("\n")
			}
		}
		*/
		
		if(v != null && v.intValue() != 0 && exitOnError) {
			
			System.err.println "Process return code: ${v} - exiting..."
			
			System.exit(v)
			
		}
		
		return v != null ? v : 0
		
	}
	
}
