package cz.inqool.uas.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class Architecture {
    @Pointcut("within(cz.inqool.uas.store.DomainStore+)")
    public void isStore() {
    }

    @Pointcut("execution(* *.save(..))")
    public void isSave() {
    }

    @Pointcut("execution(* *.delete(..))")
    public void isDelete() {
    }
}
