package org.eclipse.jdt.internal.ui.snippeteditor;import java.lang.reflect.InvocationTargetException;import java.lang.reflect.Method;
public class ScrapbookMain1 {
	public static void eval(Class clazz) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		Method method=clazz.getDeclaredMethod("nop", new Class[0]);
		method.invoke(null, new Object[0]);
	}
}
