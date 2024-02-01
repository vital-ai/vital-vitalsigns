package ai.vital.vitalsigns.utils

/**
 *  source: http://blog.joda.org/2014/02/exiting-jvm.html 
 */
class SystemExit {

	
	/**
	 * Exits the JVM, trying to do it nicely, otherwise doing it nastily.
	 *
	 * @param status  the exit status, zero for OK, non-zero for error
	 * @param maxDelay  the maximum delay in milliseconds
	 */
	public static void exit(final int status, long maxDelayMillis) {
	  try {
		// setup a timer, so if nice exit fails, the nasty exit happens
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
		  @Override
		  public void run() {
			Runtime.getRuntime().halt(status);
		  }
		}, maxDelayMillis);
		// try to exit nicely
		System.exit(status);
		
	  } catch (Throwable ex) {
		// exit nastily if we have a problem
		Runtime.getRuntime().halt(status);
	  } finally {
		// should never get here
		Runtime.getRuntime().halt(status);
	  }
	  
	}
}
