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
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.resources.IResource;
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
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

public class PrimaryWorkingCopyTest extends CoreTests {
	
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
		
		
		List result= doSearchForReferences("A", null);
		assertTrue("Should contain 1 references, contains: " + result.size(), result.size() == 1);

		
		IEditorPart part= EditorUtility.openInEditor(cu3);
		try {
			IDocument document= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			String replacedString= "//Here";
			
			int offset= document.get().indexOf(replacedString);
			
			document.replace(offset, replacedString.length(), "A a;");
			
			result= doSearchForReferences("A", null);
			assertTrue("Should contain 2 references, contains: " + result.size(), result.size() == 2);

			//save
			part.doSave(null);

			result= doSearchForReferences("A", null);
			assertTrue("Should contain 2 references, contains: " + result.size(), result.size() == 2);

			
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
		
		result= doSearchForReferences("A", null);
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
		
		
		List result= doSearchForReferences("A", null);
		assertTrue("Should contain 1 references, contains: " + result.size(), result.size() == 1);

		
		IEditorPart part= EditorUtility.openInEditor(cu3);
		try {
			IDocument document= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			String replacedString= "//Here";
			
			int offset= document.get().indexOf(replacedString);
			
			document.replace(offset, replacedString.length(), "A a;");
			
			result= doSearchForReferences("A", null);
			assertTrue("Should contain 2 references, contains: " + result.size(), result.size() == 2);
					
			ICompilationUnit wcopy= (ICompilationUnit) cu2.getWorkingCopy(); // create sand box working copy
			try {
				String source= wcopy.getSource();
				replacedString= "A a;";
				offset= source.indexOf(replacedString);
				source= source.substring(0, offset) + source.substring(offset + replacedString.length()); // remove reference
				wcopy.getBuffer().setContents(source);
	
				
				result= doSearchForReferences("A", wcopy);
				assertTrue("Should contain 1 references, contains: " + result.size(), result.size() == 1);
				
				// no save
			} finally {
				wcopy.destroy();
			}
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
		
		result= doSearchForReferences("A", null);
		assertTrue("Should contain 1 references, contains: " + result.size(), result.size() == 1);
	}
	
	
	private List doSearchForReferences(String ref, ICompilationUnit wcopy) throws JavaModelException {
		IWorkingCopy[] allWorkingCopies;
		if (wcopy == null) {
			if (JavaPlugin.USE_WORKING_COPY_OWNERS) {
				allWorkingCopies= new ICompilationUnit[0];
			} else {
				allWorkingCopies= JavaUI.getSharedWorkingCopies();
			}			

		} else {
			if (JavaPlugin.USE_WORKING_COPY_OWNERS) {
				allWorkingCopies= new ICompilationUnit[] { wcopy };
			} else {
				IWorkingCopy[] copies= JavaUI.getSharedWorkingCopies();
				allWorkingCopies= new ICompilationUnit[copies.length + 1];
				System.arraycopy(copies, 0, allWorkingCopies, 0, copies.length);
				allWorkingCopies[copies.length]= wcopy;
			}
		}
		
		SearchEngine engine= new SearchEngine(allWorkingCopies);
		
		IJavaSearchScope scope= SearchEngine.createWorkspaceScope();
		SearchResultCollector collector= new SearchResultCollector(null);

		engine.search(ResourcesPlugin.getWorkspace(), ref, IJavaSearchConstants.TYPE, IJavaSearchConstants.REFERENCES, scope, collector);

		return collector.getResults();
	}
	
	public void testWorkingCopyOfWorkingCopy() throws Exception {
	
		IPackageFragmentRoot root1= JavaProjectHelper.addSourceContainer(fJavaProject1, "src");
		IPackageFragment pack1= root1.createPackageFragment("pack1", true, null);
		
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("public class A {\n");
		buf.append("    //Here\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", buf.toString(), true, null);

		IEditorPart part= EditorUtility.openInEditor(cu1);
		try {
			IDocument document= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			String replacedString= "//Here";
			
			int offset= document.get().indexOf(replacedString);
			
			document.replace(offset, replacedString.length(), "A a;");
			
			cu1= JavaModelUtil.toWorkingCopy(cu1); // no-op when USE_WORKING_COPY_OWNERS is true

			buf= new StringBuffer();
			buf.append("package pack1;\n");
			buf.append("\n");
			buf.append("public class A {\n");
			buf.append("    A a;\n");
			buf.append("}\n");
			String expected= buf.toString();
			
			assertEqualString(cu1.getSource(), expected);
			
			ICompilationUnit wcopy= (ICompilationUnit) cu1.getWorkingCopy(); // create sand box working copy
			try {
				assertEqualString(wcopy.getSource(), expected);			
				// no save
			} finally {
				wcopy.destroy();
			}
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}
	
	public void testLineDelimiterConsistency1() throws Exception {
		// mixed line delimiters
		
		IPackageFragmentRoot root1= JavaProjectHelper.addSourceContainer(fJavaProject1, "src");
		IPackageFragment pack1= root1.createPackageFragment("pack1", true, null);
		
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("public class A {\n");
		buf.append("    //Here\r\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", buf.toString(), true, null);

		IEditorPart part= EditorUtility.openInEditor(cu1);
		try {
			IDocument document= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			String replacedString= "//Here";
			
			int offset= document.get().indexOf(replacedString);
			
			document.replace(offset, replacedString.length(), "A a;");
			
			cu1= JavaModelUtil.toWorkingCopy(cu1); // no-op when USE_WORKING_COPY_OWNERS is true

			buf= new StringBuffer();
			buf.append("package pack1;\n");
			buf.append("\n");
			buf.append("public class A {\n");
			buf.append("    A a;\r\n");
			buf.append("}\n");
			String expected= buf.toString();
			
			assertEqualString(cu1.getSource(), expected);
			
			ICompilationUnit wcopy= (ICompilationUnit) cu1.getWorkingCopy(); // create sand box working copy
			try {
				assertEqualString(wcopy.getSource(), expected);
				IResource resource= wcopy.getResource();
				assertNotNull(resource);
				// no save
			} finally {
				wcopy.destroy();
			}
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}
	
	public void testLineDelimiterConsistency2() throws Exception {
		// mixed line delimiters
		
		IPackageFragmentRoot root1= JavaProjectHelper.addSourceContainer(fJavaProject1, "src");
		IPackageFragment pack1= root1.createPackageFragment("pack1", true, null);
		
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("public class A {\n");
		buf.append("    //Here\r\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", buf.toString(), true, null);

		IEditorPart part= EditorUtility.openInEditor(cu1);
		try {
			IDocument document= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			String replacedString= "//Here";
			
			int offset= document.get().indexOf(replacedString);
			
			document.replace(offset, replacedString.length(), "A a;");
					
			
			cu1= JavaModelUtil.toWorkingCopy(cu1); // no-op when USE_WORKING_COPY_OWNERS is true

			buf= new StringBuffer();
			buf.append("package pack1;\n");
			buf.append("\n");
			buf.append("public class A {\n");
			buf.append("    A a;\r\n");
			buf.append("}\n");
			String expected= buf.toString();
			
			assertEqualString(cu1.getSource(), expected);
			
			part.doSave(null);
			
			assertEqualString(cu1.getSource(), expected);
			
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
		
		
	}
		
	
}