package junit.runner;

/*-
 * #%L
 * org.eclipse.jdt.ui.junit.sampleproject
 * %%
 * Copyright (C) 2020 Eclipse Foundation
 * %%
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 * #L%
 */

import java.util.*;
import java.io.*;

/**
 * An implementation of a TestCollector that consults the class path. It
 * considers all classes on the class path excluding classes in JARs. It leaves
 * it up to subclasses to decide whether a class is a runnable Test.
 *
 * @see TestCollector
 */
public abstract class ClassPathTestCollector implements TestCollector {

	static final int SUFFIX_LENGTH = ".class".length();

	public ClassPathTestCollector() {
	}

	public Enumeration collectTests() {
		String classPath = System.getProperty("java.class.path");
		Hashtable result = collectFilesInPath(classPath);
		return result.elements();
	}

	public Hashtable collectFilesInPath(String classPath) {
		Hashtable result = collectFilesInRoots(splitClassPath(classPath));
		return result;
	}

	Hashtable collectFilesInRoots(Vector roots) {
		Hashtable result = new Hashtable(100);
		Enumeration e = roots.elements();
		while (e.hasMoreElements())
			gatherFiles(new File((String) e.nextElement()), "", result);
		return result;
	}

	void gatherFiles(File classRoot, String classFileName, Hashtable result) {
		File thisRoot = new File(classRoot, classFileName);
		if (thisRoot.isFile()) {
			if (isTestClass(classFileName)) {
				String className = classNameFromFile(classFileName);
				result.put(className, className);
			}
			return;
		}
		String[] contents = thisRoot.list();
		if (contents != null) {
			for (String content : contents)
				gatherFiles(classRoot, classFileName + File.separatorChar + content, result);
		}
	}

	Vector splitClassPath(String classPath) {
		Vector result = new Vector();
		String separator = System.getProperty("path.separator");
		StringTokenizer tokenizer = new StringTokenizer(classPath, separator);
		while (tokenizer.hasMoreTokens())
			result.addElement(tokenizer.nextToken());
		return result;
	}

	protected boolean isTestClass(String classFileName) {
		return classFileName.endsWith(".class") && classFileName.indexOf('$') < 0 && classFileName.indexOf("Test") > 0;
	}

	protected String classNameFromFile(String classFileName) {
		// convert /a/b.class to a.b
		String s = classFileName.substring(0, classFileName.length() - SUFFIX_LENGTH);
		String s2 = s.replace(File.separatorChar, '.');
		if (s2.startsWith("."))
			return s2.substring(1);
		return s2;
	}
}
