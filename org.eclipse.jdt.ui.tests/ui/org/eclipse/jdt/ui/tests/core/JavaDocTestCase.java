/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.tests.core;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestPluginLauncher;

import org.eclipse.jdt.internal.ui.text.javadoc.JavaDocAccess;
import org.eclipse.jdt.internal.ui.text.javadoc.JavaDocTextReader;
import org.eclipse.jdt.internal.ui.text.javadoc.StandardDocletPageBuffer;
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;

public class JavaDocTestCase extends TestCase {
	
	private IJavaProject fJavaProject;

	public JavaDocTestCase(String name) {
		super(name);
	}
	public static void main(String[] args) {
		TestPluginLauncher.run(TestPluginLauncher.getLocationFromProperties(), JavaDocTestCase.class, args);
	}		
			
	public static Test suite() {
		TestSuite suite= new TestSuite();
		suite.addTest(new JavaDocTestCase("doTest1"));
		return suite;
	}
	
	/**
	 * Creates a new test Java project.
	 */
	protected void setUp() throws Exception {
		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");

		IPackageFragmentRoot jdk= JavaProjectHelper.addRTJar(fJavaProject);
		assertTrue("jdk not found", jdk != null);
		
		
/*		File jdocDir= new File("M:\\JAVA\\jdk1.2\\docs\\api");
		assertTrue("Must be existing directory", jdocDir.isDirectory());
		JavaDocAccess.setJavaDocLocation(jdk, jdocDir.toURL());*/

		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		IPackageFragment pack= root.createPackageFragment("ibm.util", true, null);
		
		ICompilationUnit cu= pack.getCompilationUnit("A.java");
		IType type= cu.createType("public class A {\n\n}\n", null, true, null);
		type.createMethod(getMethodBody("a", 1), null, true, null);
		type.createMethod(getMethodBody("b", 1), null, true, null);
	}
	
	private void addLine(String line, int indent, StringBuffer buf) {
		for (int i= 0; i < indent; i++) {
			buf.append('\t');
		}
		buf.append(line);
		buf.append('\n');
	}
	
	private String getMethodBody(String name, int indent) {
		StringBuffer buf= new StringBuffer("\n");
		addLine("/**", indent, buf);
		addLine(" * My <code>Java</code>   comment\t&lt;&#169;&gt;", indent, buf);
		addLine(" */", indent, buf);
		addLine("public void " + name + "(String arg) {", indent, buf);
		addLine("System.out.println(arg);", indent + 1, buf);
		addLine("}", indent, buf);
		return buf.toString();
	}
			
	/**
	 * Removes the test java project.
	 */
	protected void tearDown () throws Exception {
		JavaProjectHelper.delete(fJavaProject);
		fJavaProject= null;
	}
				
	public void doTest1() throws Exception {
		
		String name= "ibm/util/A.java";
		ICompilationUnit cu= (ICompilationUnit) fJavaProject.findElement(new Path(name));
		assertTrue("A.java must exist", cu != null);
		System.out.println(cu.getSource());
		IType type= cu.getType("A");
		assertTrue("Type A must exist", type != null);
			
		System.out.println("methods of A");
		IMethod[] methods= type.getMethods();
		assertTrue("Should contain 2 methods", methods.length == 2);
		Reader reader;
		for (int i= 0; i < methods.length; i++) {
			System.out.println(methods[i].getElementName());
			System.out.println("JavaDoc:");
			reader= JavaDocAccess.getJavaDoc(methods[i]);
			assertTrue("Java doc must be found", reader != null);
			JavaDocTextReader txtreader= new JavaDocTextReader(reader);
			String str= txtreader.getString();
			System.out.println(str);
			String expectedComment="My Java comment <©>";
			assertTrue("Java doc text not as expected", expectedComment.equals(str));
		}
		
	}
	
	/**
	 * Currently not working (JavaDocAccess.getJavaDocLocation disabled)
	 */
	public void doTest2() throws Exception {		
		
		String name= "java/io/Reader.java";
		IClassFile cf= (IClassFile) fJavaProject.findElement(new Path(name));
		assertTrue(name + " must exist", cf != null);
		IType type= cf.getType();
		assertTrue("Type must exist", type != null);
		
		IPackageFragmentRoot root= JavaModelUtility.getPackageFragmentRoot(type);
		assertTrue("PackageFragmentRoot must exist", root != null);
		
		URL jdocLocation= JavaDocAccess.getJavaDocLocation(root);
		assertTrue("JavaDoc location must exist", jdocLocation != null);
		
		StandardDocletPageBuffer page= new StandardDocletPageBuffer(type);

		Reader reader= page.getJavaDoc(type);
		if (reader == null) {
			System.out.println("JavaDoc not found for type " + type.getElementName());
		} else {
			JavaDocTextReader txtreader= new JavaDocTextReader(reader);
			System.out.println("JavaDoc of type " + type.getElementName());
			System.out.println(txtreader.getString());
		}		
		IMethod[] methods= type.getMethods();
		for (int i= 0; i < methods.length; i++) {
			IMethod curr= methods[i];
			reader= page.getJavaDoc(curr);
			if (reader == null) {
				System.out.println("JavaDoc not found for method " + curr.getElementName());
			} else {
				JavaDocTextReader txtreader= new JavaDocTextReader(reader);
				System.out.println("JavaDoc of method " + curr.getElementName());
				System.out.println(txtreader.getString());
			}
		}
		
		IField[] fields= type.getFields();
		for (int i= 0; i < fields.length; i++) {
			IField curr= fields[i];
			reader= page.getJavaDoc(curr);
			if (reader == null) {
				System.out.println("JavaDoc not found for field " + curr.getElementName());
			} else {
				JavaDocTextReader txtreader= new JavaDocTextReader(reader);
				System.out.println("JavaDoc of field " + curr.getElementName());
				System.out.println(txtreader.getString());
			}
		}		
	}	

	/**
	 * Interactive test
	 */	
	public void doTest3() throws Exception {		
	
		String name= "java/lang/Math.java";
		IClassFile cf= (IClassFile) fJavaProject.findElement(new Path(name));
		assertTrue(name + " must exist", cf != null);
		IType type= cf.getType();
		assertTrue("Type must exist", type != null);
		
		System.out.println("methods of " + name);
		IMethod[] methods= type.getMethods();
		for (int i= 0; i < methods.length; i++) {
			System.out.println(methods[i].getElementName());
			System.out.println("JavaDoc:");
			Reader reader= JavaDocAccess.getJavaDoc(methods[i]);
			if (reader != null) {
				JavaDocTextReader txtreader= new JavaDocTextReader(reader);
				BufferedReader bufRd= new BufferedReader(txtreader);
				String line;
				do {
					line= bufRd.readLine();
					if (line != null) {
						System.out.println(line);
					}
				} while (line != null);
				
			} else {
				System.out.println("not found");
			}
		}
	}
	
		
	
	/**
	 * Gets the comment as a String
	 */
	public static String getString(Reader rd) throws IOException {
		StringBuffer buf= new StringBuffer();
		int ch;
		while ((ch= rd.read()) != -1) {
			buf.append((char)ch);
		}
		return buf.toString();
	}	

			
}