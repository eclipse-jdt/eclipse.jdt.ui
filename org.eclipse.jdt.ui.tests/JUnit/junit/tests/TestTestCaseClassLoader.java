package junit.tests;

import java.lang.reflect.*;
import junit.framework.*;
import junit.util.*;


/**
 * A TestCase for testing the TestCaseClassLoader
 *
 */
public class TestTestCaseClassLoader extends TestCase {

	public TestTestCaseClassLoader(String name) {
		super(name);
	}
	public void testClassLoading() {
		TestCaseClassLoader loader= new TestCaseClassLoader();
		Class loadedClass= null;
		try {
			loadedClass= loader.loadClass("junit.tests.ClassLoaderTestCase", true);
		}
		catch (Exception e) {
			fail("Exception during class loading");
		}
		assert(loadedClass != null);
		Object o= null;
		try {
			o= loadedClass.newInstance();			
		}
		catch (Exception e) {
			fail("Can't instantiate loaded class");
		}
		assertNotNull(o);
		//
		// Invoke the assertClassLoaders method via reflection.
		// We use reflection since the class is loaded by
		// another class loader and we can't do a downcast to
		// ClassLoaderTestCase.
		// 
		String methodName= "assertClassLoaders";
		Method method= null;
		Boolean result= null;
		try {
			method= loadedClass.getDeclaredMethod(methodName, new Class[0]);
		} catch (NoSuchMethodException e) {
			fail("Method \""+methodName+"\" not found");
		}
		try {
			result= (Boolean)method.invoke(o, new Class[0]);
		}
		catch (InvocationTargetException e) {
			fail("InvocationTargetException");
		}
		catch (IllegalAccessException e) {
			fail("IllegalAccessException");
		}
		assert(result.booleanValue());
	}
}