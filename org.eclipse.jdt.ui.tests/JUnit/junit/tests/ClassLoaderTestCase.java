package junit.tests;

/**
 * Test class used in TestTestCaseClassLoader
 */
import junit.framework.*;
import junit.util.*;

public class ClassLoaderTestCase extends Object {
	public ClassLoaderTestCase() {
	}
	public Boolean assertClassLoaders() {
		return new Boolean(loadedByTestCaseClassLoader() && systemClassNotLoadedByTestCaseClassLoader());
	}
	private boolean isTestCaseClassLoader(ClassLoader cl) {
		return (cl != null && cl.getClass().equals(junit.util.TestCaseClassLoader.class));
	}
	public boolean loadedByTestCaseClassLoader() {
		ClassLoader cl= getClass().getClassLoader();
		return isTestCaseClassLoader(cl);
	}
	public boolean systemClassNotLoadedByTestCaseClassLoader() {
		ClassLoader cl= Object.class.getClassLoader();
		ClassLoader cl2= TestCase.class.getClassLoader();
		return (!isTestCaseClassLoader(cl) && !isTestCaseClassLoader(cl2)); 
	}
}