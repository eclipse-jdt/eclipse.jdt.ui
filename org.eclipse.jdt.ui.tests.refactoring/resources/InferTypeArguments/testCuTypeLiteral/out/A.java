package p;

import java.lang.reflect.Method;

@SuppressWarnings("deprecation")
public class A {
	void m(A a) throws Exception {
		Class<? extends A> clazz = a.getClass();
		Method method = clazz.getMethod("m", A.class);
		SuppressWarnings suppressed = method.getAnnotation(SuppressWarnings.class);
	}
}
