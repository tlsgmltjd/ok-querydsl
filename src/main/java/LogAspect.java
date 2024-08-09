import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class LogAspect {

    @Pointcut("execution(* *..*.*order*(..))")
    public void orderMethods() {
    }

    @Around("orderMethods()")
    public Object logAroundOrderMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        System.out.println("start log : " + methodName);

        Object result;
        try {
            result = joinPoint.proceed();
        } finally {
            System.out.println("end log : " + methodName);
        }

        return result;
    }
}
