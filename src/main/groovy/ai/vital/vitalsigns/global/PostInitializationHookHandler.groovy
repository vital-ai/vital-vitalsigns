package ai.vital.vitalsigns.global

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import ai.vital.vitalsigns.utils.StringUtils;

public class PostInitializationHookHandler {

    private final static Logger log = LoggerFactory
            .getLogger(PostInitializationHookHandler.class);

    private static Pattern hookPattern = Pattern
            .compile("^(.+)\\.([^.]+)\\(\\)\$");

    /**
     * calls post initialization hook
     * 
     * @param postInitializationHook
     *            string matching [FQCN].[method]()
     * @param timeoutSeconds
     *            timeout
     * @return null on success or captured exception
     */
    public static Exception callPostinitializationHook(
            String postInitializationHook, Double timeoutSeconds) {

        if (StringUtils.isEmpty(postInitializationHook)) {
            return null;
        }

        log.info("postInitializationHook {}", postInitializationHook);

        ExecutorService executor = null;

        try {

            Matcher matcher = hookPattern.matcher(postInitializationHook);
            if (!matcher.matches()) {
                throw new Exception(
                        "postInitializationHook does not match pattern: "
                                + hookPattern.pattern());
            }

            String fqcn = matcher.group(1);
            String methodName = matcher.group(2);
            log.info("Checking postInitializationHook class {} method {}", fqcn,
                    methodName);
            Class<?> cls = Class.forName(fqcn);

            final Method method = cls.getMethod(methodName);
            if (!Modifier.isStatic(method.getModifiers())) {
                throw new Exception("class " + fqcn + " method " + methodName
                        + " is not static");
            }

            executor = Executors.newFixedThreadPool(1);

            Callable<Exception> task = new Callable<Exception>() {
                public Exception call() {
                    try {
                        method.invoke(null);
                    } catch (Exception e) {
                        return e;
                    }
                    return null;
                }
            };

            log.info("Calling postInitializationHook {}",
                    postInitializationHook);
            Future<Exception> future = executor.submit(task);
            try {
                TimeUnit unit = TimeUnit.SECONDS;
                int value = timeoutSeconds.intValue();
                if (timeoutSeconds < 1d) {
                    unit = TimeUnit.MILLISECONDS;
                    value = new Double(timeoutSeconds.doubleValue() * 1000d)
                            .intValue();
                }
                Exception result = future.get(value, unit);
                if (result != null)
                    throw result;
            } finally {
                future.cancel(true); // may or may not desire this
            }

            log.info("postInitializationHook called successfully");

        } catch (Exception e) {
            log.warn("postInitializationHook failed: " + e.getLocalizedMessage());
            return e;
        } finally {
            if (executor != null) {
                try {
                    executor.shutdown();
                } catch (Exception e2) {
                }
            }
        }

        return null;

    }

}
