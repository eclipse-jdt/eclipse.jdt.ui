/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IWorkingCopy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.corext.refactoring.SearchResultCollector;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

public class PrimaryWorkingCopyTest extends TestCase {
	
	private static final Class THIS= PrimaryWorkingCopyTest.class;
	
	private IJavaProject fJavaProject1;
	
	public PrimaryWorkingCopyTest(String name) {
		super(name);
	}
			
	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test suite() {
		if (true) {
			return allTests();
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new AllTypesCacheTest("testHierarchyWithWorkingCopy"));
			return suite;
		}	
	}
	
	protected void setUp() throws Exception {
		fJavaProject1= ProjectTestSetup.getProject();
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJavaProject1, ProjectTestSetup.getDefaultClasspath());
	}
					
	
	public void testSearchInWorkingCopies() throws Exception {
	
		IPackageFragmentRoot root1= JavaProjectHelper.addSourceContainer(fJavaProject1, "src");
		IPackageFragment pack1= root1.createPackageFragment("pack1", true, null);
		
		StringBuffer buf= new StringBuffer();
		buf.append("public class A {\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.getCompilationUnit("A.java");
		cu1.createType(buf.toString(), null, true, null);

		buf= new StringBuffer();
		buf.append("public class B {\n");
		buf.append("    A a;\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.getCompilationUnit("B.java");
		cu2.createType(buf.toString(), null, true, null);
		
		IPackageFragment pack2= root1.createPackageFragment("pack2", true, null);
		
		buf= new StringBuffer();
		buf.append("public class C {\n");
		buf.append("    //Here\n");
		buf.append("}\n");
		ICompilationUnit cu3= pack2.getCompilationUnit("C.java");
		cu3.createType(buf.toString(), null, true, null);
		
		
		// empty array when USE_WORKING_COPY_OWNERS is true
		IWorkingCopy[] workingCopies= JavaUI.getSharedWorkingCopiesOnClasspath();
			
		List result= doSearchForReferences("A", JavaUI.getSharedWorkingCopiesOnClasspath());
		assertTrue("Should contain 1 references, contains: " + result.size(), result.size() == 1);

		
		IEditorPart part= EditorUtility.openInEditor(cu3);
		try {
			IDocument document= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			String replacedString= "//Here";
			
			int offset= document.search(0, replacedString, true, true, false);
			
			document.replace(offset, replacedString.length(), "A a;");
			
			result= doSearchForReferences("A", workingCopies);
			assertTrue("Should contain 2 references, contains: " + result.size(), result.size() == 2);

			//save
			part.doSave(null);

			result= doSearchForReferences("A", workingCopies);
			assertTrue("Should contain 2 references, contains: " + result.size(), result.size() == 2);

			
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
		
		result= doSearchForReferences("A", workingCopies);
		assertTrue("Should contain 2 references, contains: " + result.size(), result.size() == 2);
	}
	
	public void testSearchInWorkingCopies2() throws Exception {
	
		IPackageFragmentRoot root1= JavaProjectHelper.addSourceContainer(fJavaProject1, "src");
		IPackageFragment pack1= root1.createPackageFragment("pack1", true, null);
		
		StringBuffer buf= new StringBuffer();
		buf.append("public class A {\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.getCompilationUnit("A.java");
		cu1.createType(buf.toString(), null, true, null);

		buf= new StringBuffer();
		buf.append("public class B {\n");
		buf.append("    A a;\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.getCompilationUnit("B.java");
		cu2.createType(buf.toString(), null, true, null);
		
		IPackageFragment pack2= root1.createPackageFragment("pack2", true, null);
		
		buf= new StringBuffer();
		buf.append("public class C {\n");
		buf.append("    //Here\n");
		buf.append("}\n");
		ICompilationUnit cu3= pack2.getCompilationUnit("C.java");
		cu3.createType(buf.toString(), null, true, null);
		
		// empty array when USE_WORKING_COPY_OWNERS is true
		IWorkingCopy[] workingCopies= JavaUI.getSharedWorkingCopiesOnClasspath();	
		
		List result= doSearchForReferences("A", workingCopies);
		assertTrue("Should contain 1 references, contains: " + result.size(), result.size() == 1);

		
		IEditorPart part= EditorUtility.openInEditor(cu3);
		try {
			IDocument document= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			String replacedString= "//Here";
			
			int offset= document.search(0, replacedString, true, true, false);
			
			document.replace(offset, replacedString.length(), "A a;");
			
			result= doSearchForReferences("A", workingCopies);
			assertTrue("Should contain 2 references, contains: " + result.size(), result.size() == 2);
			
			ICompilationUnit wcopy= (ICompilationUnit) cu2.getWorkingCopy(); // create sand box working copy
			try {
				String source= wcopy.getSource();
				replacedString= "A a;";
				offset= source.indexOf(replacedString);
				source= source.substring(0, offset) + source.substring(offset + replacedString.length()); // remove reference
				wcopy.getBuffer().setContents(source);
	
				IWorkingCopy[] newWorkingCopies= new IWorkingCopy[workingCopies.length + 1];
				System.arraycopy(workingCopies, 0, newWorkingCopies, 0, workingCopies.length);
				newWorkingCopies[workingCopies.length]= wcopy;
				
				result= doSearchForReferences("A", newWorkingCopies);
				assertTrue("Should contain 1 references, contains: " + result.size(), result.size() == 1);
				
				// no save
			} finally {
				wcopy.destroy();
			}
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
		
		result= doSearchForReferences("A", workingCopies);
		assertTrue("Should contain 1 references, contains: " + result.size(), result.size() == 1);
	}
	
	private static boolean BUG_43300= true;
	
	
	private List doSearchForReferences(String ref, IWorkingCopy[] workingCopies) throws JavaModelException {
		SearchEngine engine= new SearchEngine(workingCopies);
		if (BUG_43300 && workingCopies.length == 0) {
			engine= new SearchEngine();
		}
		
		IJavaSearchScope scope= SearchEngine.createWorkspaceScope();
		SearchResultCollector collector= new SearchResultCollector(null);

		engine.search(ResourcesPlugin.getWorkspace(), ref, IJavaSearchConstants.TYPE, IJavaSearchConstants.REFERENCES, scope, collector);

		return collector.getResults();
	}
	
	
}