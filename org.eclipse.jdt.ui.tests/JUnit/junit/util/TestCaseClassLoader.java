package junit.util;

import java.util.*;
import java.io.*;
/**
 * A custom class loader which enables the reloading
 * of classes for each test run. The class loader
 * can be configured with a list of package paths that
 * should be excluded from loading. The loading
 * of these packages is delegated to the system class
 * loader. They will be shared across test runs.
 * <p>
 * The list of excluded package paths is specified in
 * a properties file "excluded.properties" that is located in 
 * the same place as the TestCaseClassLoader class.
 * <p>
 * <b>Known limitation:</b> the TestCaseClassLoader cannot load classes
 * from jar files.
 */


public class TestCaseClassLoader extends ClassLoader {
	/** scanned class path */
	private String[] fPathItems;
	/** excluded paths */
	private String[] fExcluded= { "com.sun.", "sun."};
	/** name of excluded properties file */
	static final String EXCLUDED_FILE= "excluded.properties";
	/**
	 * Constructs a TestCaseLoader. It scans the class path
	 * and the excluded package paths
	 */
	public TestCaseClassLoader() {
		super();
		String classPath= System.getProperty("java.class.path");
		String separator= System.getProperty("path.separator");
		
		// first pass: count elements
		StringTokenizer st= new StringTokenizer(classPath, separator);
		int i= 0;
		while (st.hasMoreTokens()) {
			st.nextToken();
			i++;
		}
		// second pass: split
		fPathItems= new String[i];
		st= new StringTokenizer(classPath, separator);
		i= 0;
		while (st.hasMoreTokens()) {
			fPathItems[i++]= st.nextToken();
		}

		String[] excluded= readExcludedPackages();
		if (excluded != null)
			fExcluded= excluded;	
	}
	public java.net.URL getResource(String name) {
		return ClassLoader.getSystemResource(name);
	}
	public InputStream getResourceAsStream(String name) {
		return ClassLoader.getSystemResourceAsStream(name);
	}
	protected boolean isExcluded(String name) {
		// exclude the "java" and "junit" packages.
		// They always need to be excluded so that they are loaded by the system class loader
		if (name.startsWith("java.") || 
			name.startsWith("junit.framework") ||
			name.startsWith("junit.extensions") ||
			name.startsWith("junit.util") ||
			name.startsWith("junit.ui"))
			return true;
			
		// exclude the user defined package paths
		for (int i= 0; i < fExcluded.length; i++) {
			if (name.startsWith(fExcluded[i])) {
				return true;
			}
		}
		return false;	
	}
	public synchronized Class loadClass(String name, boolean resolve)
		throws ClassNotFoundException {
			
		Class c= findLoadedClass(name);
		if (c != null)
			return c;
		//
		// Delegate the loading of excluded classes to the
		// standard class loader.
		//
		if (isExcluded(name)) {
			try {
				c= findSystemClass(name);
				return c;
			} catch (ClassNotFoundException e) {
				// keep searching
			}
		}
		if (c == null) {
			File file= locate(name);
			if (file == null)
				throw new ClassNotFoundException();
			byte data[]= loadClassData(file);
			c= defineClass(name, data, 0, data.length);
		}
		if (resolve) 
			resolveClass(c);
		return c;
	}
	private byte[] loadClassData(File f) throws ClassNotFoundException {
		try {
			//System.out.println("loading: "+f.getPath());
			FileInputStream stream= new FileInputStream(f);
			
			try {
				byte[] b= new byte[stream.available()];
				stream.read(b);
				stream.close();
				return b;
			}
			catch (IOException e) {
				throw new ClassNotFoundException();
			}
		}
		catch (FileNotFoundException e) {
			throw new ClassNotFoundException();
		}
	}
	/**
	 * Locate the given file.
	 * @return Returns null if file couldn't be found.
	 */
	private File locate(String fileName) { 
		fileName= fileName.replace('.', '/')+".class";
		File path= null;
		
		if (fileName != null) {
			for (int i= 0; i < fPathItems.length; i++) {
				path= new File(fPathItems[i], fileName);
				if (path.exists())
					return path;
			}
		}
		return null;
	}
	private String[] readExcludedPackages() {		
		InputStream is= getClass().getResourceAsStream(EXCLUDED_FILE);
		if (is == null) 
			return null;
		Properties p= new Properties();
		try {
			p.load(is);
		}
		catch (IOException e) {
			return null;
		}
		Vector v= new Vector(10);
		for (Enumeration e= p.propertyNames(); e.hasMoreElements(); ) {
			String key= (String)e.nextElement();
			if (key.startsWith("excluded.")) {
				String path= p.getProperty(key);
				if (path.endsWith("*"))
					path= path.substring(0, path.length()-1);
				if (path.length() > 0) 
					v.addElement(path);				
			}
		}
		String[] excluded= new String[v.size()];
		for (int i= 0; i < v.size(); i++)
			excluded[i]= (String)v.elementAt(i);
		return excluded;
	}
}