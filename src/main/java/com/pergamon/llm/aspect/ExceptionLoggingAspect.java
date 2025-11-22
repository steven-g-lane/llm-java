package com.pergamon.llm.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aspect that logs all exceptions thrown in the LLM service layer.
 * Intercepts exceptions from all methods in com.pergamon.llm package and logs them to the error log.
 */
@Aspect
public class ExceptionLoggingAspect {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionLoggingAspect.class);

    /**
     * After throwing advice that logs all exceptions.
     * Captures exceptions from any method in com.pergamon.llm package and its subpackages.
     *
     * @param joinPoint the join point where the exception was thrown
     * @param exception the exception that was thrown
     */
    @AfterThrowing(
        pointcut = "execution(* com.pergamon.llm..*(..))",
        throwing = "exception"
    )
    public void logException(JoinPoint joinPoint, Throwable exception) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        logger.error("Exception in {}.{}(): {}",
            className,
            methodName,
            exception.getMessage(),
            exception
        );
    }
}
