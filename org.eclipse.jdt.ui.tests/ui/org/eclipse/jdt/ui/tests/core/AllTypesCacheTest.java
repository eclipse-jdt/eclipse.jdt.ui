/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.runtime.Path;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.corext.util.AllTypesCache;
import org.eclipse.jdt.internal.corext.util.TypeInfo;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;


public class AllTypesCacheTest extends TestCase {
	
	private static final Class THIS= AllTypesCacheTest.class;
	
	private IJavaProject fJProject1;
	private IJavaProject fJProject2;
	
	private IPackageFragmentRoot fLibrary;
	private IPackageFragmentRoot fSourceFolder;

	private boolean fWasAutobuild;
	
	public AllTypesCacheTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test suite() {
		if (false) {
			return allTests();
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new AllTypesCacheTest("testClasspathChange"));
			return new ProjectTestSetup(suite);
		}	
	}


	protected void setUp() throws Exception {
		fWasAutobuild= JavaProjectHelper.setAutoBuilding(false);
		
		fJProject1= ProjectTestSetup.getProject();
		
		File lib= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.MYLIB);
		assertTrue("lib does not exist",  lib != null && lib.exists());
		fLibrary= JavaProjectHelper.addLibrary(fJProject1, new Path(lib.getPath())); // add library to proj1
		
		
		fJProject2= JavaProjectHelper.createJavaProject("TestProject2", "bin");
		assertNotNull("jre is null", JavaProjectHelper.addRTJar(fJProject2));
		
		// add Junit source to project 2
		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC);
		assertTrue("Junit source", junitSrcArchive != null && junitSrcArchive.exists());

		fSourceFolder= JavaProjectHelper.addSourceContainerWithImport(fJProject2, "src", junitSrcArchive);
		
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
		JavaProjectHelper.delete(fJProject2);		
		JavaProjectHelper.setAutoBuilding(fWasAutobuild);
	}

	public void testDifferentScopes() throws Exception {
		IJavaSearchScope workspaceScope= SearchEngine.createWorkspaceScope();
		IJavaSearchScope proj1Scope= SearchEngine.createJavaSearchScope(new IJavaElement[] { fJProject1 });
		IJavaSearchScope proj2Scope= SearchEngine.createJavaSearchScope(new IJavaElement[] { fJProject2 });
		
		ArrayList res1= new ArrayList();
		
		AllTypesCache.getTypes(workspaceScope, IJavaSearchConstants.TYPE, null, res1);
		assertTrue("542 types in workspace expected, is " + res1.size(), res1.size() == 542);
		
		int nFlushes= AllTypesCache.getNumberOfCacheFlushes();
		
		res1.clear();		
		AllTypesCache.getTypes(workspaceScope, IJavaSearchConstants.INTERFACE, null, res1);
		assertTrue("73 interfaces in workspace expected, is " + res1.size(), res1.size() == 73);
		
		assertTrue("unnecessary flush of cache", AllTypesCache.getNumberOfCacheFlushes() == nFlushes);	
		
		res1.clear();
		AllTypesCache.getTypes(workspaceScope, IJavaSearchConstants.CLASS, null, res1);
		assertTrue("469 classes in workspace expected, is " + res1.size(), res1.size() == 469);
		
		assertTrue("unnecessary flush of cache", AllTypesCache.getNumberOfCacheFlushes() == nFlushes);
		
		res1.clear();
		AllTypesCache.getTypes(proj1Scope, IJavaSearchConstants.TYPE, null, res1);
		assertTrue("471 types in proj1 expected, is " + res1.size(), res1.size() == 471);
		
		assertTrue("unnecessary flush of cache", AllTypesCache.getNumberOfCacheFlushes() == nFlushes);
		
		res1.clear();		
		AllTypesCache.getTypes(proj1Scope, IJavaSearchConstants.INTERFACE, null, res1);
		assertTrue("65 interfaces in proj1 expected, is " + res1.size(), res1.size() == 65);
		
		assertTrue("unnecessary flush of cache", AllTypesCache.getNumberOfCacheFlushes() == nFlushes);
		
		res1.clear();
		AllTypesCache.getTypes(proj1Scope, IJavaSearchConstants.CLASS, null, res1);
		assertTrue("406 classes in proj1 expected, is " + res1.size(), res1.size() == 406);
		
		assertTrue("unnecessary flush of cache", AllTypesCache.getNumberOfCacheFlushes() == nFlushes);
		
		res1.clear();
		AllTypesCache.getTypes(proj2Scope, IJavaSearchConstants.TYPE, null, res1);
		assertTrue("539 types in proj2 expected, is " + res1.size(), res1.size() == 539);
		
		assertTrue("unnecessary flush of cache", AllTypesCache.getNumberOfCacheFlushes() == nFlushes);
		
		res1.clear();		
		AllTypesCache.getTypes(proj2Scope, IJavaSearchConstants.INTERFACE, null, res1);
		assertTrue("73 interfaces in proj2 expected, is " + res1.size(), res1.size() == 73);
		
		assertTrue("unnecessary flush of cache", AllTypesCache.getNumberOfCacheFlushes() == nFlushes);
		
		res1.clear();
		AllTypesCache.getTypes(proj2Scope, IJavaSearchConstants.CLASS, null, res1);
		assertTrue("466 classes in proj2 expected, is " + res1.size(), res1.size() == 466);
		
		assertTrue("unnecessary flush of cache", AllTypesCache.getNumberOfCacheFlushes() == nFlushes);
	}
	
	public void testClasspathChange() throws Exception {	
		IJavaSearchScope workspaceScope= SearchEngine.createWorkspaceScope();
		
		ArrayList res1= new ArrayList();
		
		AllTypesCache.getTypes(workspaceScope, IJavaSearchConstants.TYPE, null, res1);
		assertNotNull("mylib.Foo not found", findTypeRef(res1, "mylib.Foo"));
		assertTrue("542 types expected, is " + res1.size(), res1.size() == 542);
		
		int nFlushes= AllTypesCache.getNumberOfCacheFlushes();
		
		JavaProjectHelper.removeFromClasspath(fJProject1, fLibrary.getPath());	
		assertTrue("cache not flushed", nFlushes != AllTypesCache.getNumberOfCacheFlushes());
		
		res1.clear();
		AllTypesCache.getTypes(workspaceScope, IJavaSearchConstants.TYPE, null, res1);
		assertNull("mylib.Foo still found", findTypeRef(res1, "mylib.Foo"));
		assertTrue("539 types in workspace expected, is " + res1.size(), res1.size() == 539);
	}
	
	public void testNewElementCreation() throws Exception {
		IJavaSearchScope workspaceScope= SearchEngine.createWorkspaceScope();
	
		ArrayList res1= new ArrayList();
		
		AllTypesCache.getTypes(workspaceScope, IJavaSearchConstants.TYPE, null, res1);
		assertTrue("542 types expected, is " + res1.size(), res1.size() == 542);
		
		// add type
		int nFlushes= AllTypesCache.getNumberOfCacheFlushes();
		IPackageFragment pack= fSourceFolder.getPackageFragment("");
		ICompilationUnit newCU= pack.getCompilationUnit("A.java");
		IType type= newCU.createType("public class A {\n}\n", null, true, null);
		assertTrue("cache not flushed after adding type", nFlushes != AllTypesCache.getNumberOfCacheFlushes());		
		res1.clear();
		AllTypesCache.getTypes(workspaceScope, IJavaSearchConstants.TYPE, null, res1);
		assertNotNull("A not found", findTypeRef(res1, "A"));
		assertTrue("543 types in workspace expected, is " + res1.size(), res1.size() == 543);
		
		
		// create a field: should not flush cache
		nFlushes= AllTypesCache.getNumberOfCacheFlushes();
		type.createField("public int fCount;", null, true, null);
		assertTrue("cache was flushed", nFlushes == AllTypesCache.getNumberOfCacheFlushes());
		res1.clear();
		AllTypesCache.getTypes(workspaceScope, IJavaSearchConstants.TYPE, null, res1);		
		assertTrue("still 543 types in workspace expected, is " + res1.size(), res1.size() == 543);
		

		// create an inner type: should flush cache
		nFlushes= AllTypesCache.getNumberOfCacheFlushes();
		type.createType("public class AInner {}", null, true, null);
		res1.clear();
		AllTypesCache.getTypes(workspaceScope, IJavaSearchConstants.TYPE, null, res1);		
		assertTrue("cache not flushed after inner type creation", nFlushes != AllTypesCache.getNumberOfCacheFlushes());
		assertNotNull("AInner not found", findTypeRef(res1, "A.AInner"));
		assertTrue("still 544 types in workspace expected, is " + res1.size(), res1.size() == 544);
	}
	
	public void testWorkingCopies() throws Exception {
		// change a type in the editor and save
		
		IJavaSearchScope workspaceScope= SearchEngine.createWorkspaceScope();
		
		ArrayList res1= new ArrayList();
		
		AllTypesCache.getTypes(workspaceScope, IJavaSearchConstants.TYPE, null, res1);
		TypeInfo ref= findTypeRef(res1, "junit.framework.TestCase");
		assertNotNull("TestCase not found", ref);
		assertTrue("542 types expected, is " + res1.size(), res1.size() == 542);
		
		int nFlushes= AllTypesCache.getNumberOfCacheFlushes();
		
		IType type= fJProject2.findType("junit.framework.TestCase");
		assertNotNull("TestCase not found", type);
		
		IEditorPart part= EditorUtility.openInEditor(type);
		try {
			IDocument document= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			ISourceRange range= type.getNameRange();
			document.replace(range.getOffset(), range.getLength(), "A");
			
			part.doSave(null);
			
			res1.clear();
			AllTypesCache.getTypes(workspaceScope, IJavaSearchConstants.TYPE, null, res1);
			assertNotNull("A not found", findTypeRef(res1, "junit.framework.A"));
			TypeInfo ref2= findTypeRef(res1, "junit.framework.TestCase");
			assertNull("TestCase still found", ref2);
			
			assertTrue("542 types in workspace expected, is " + res1.size(), res1.size() == 542);
			
			assertTrue("cache not flushed", nFlushes != AllTypesCache.getNumberOfCacheFlushes());
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	
	}
	
	public void testWorkingCopies2() throws Exception {
		// change a type in the editor and do not save
		
		IJavaSearchScope workspaceScope= SearchEngine.createWorkspaceScope();
		
		ArrayList res1= new ArrayList();
		
		AllTypesCache.getTypes(workspaceScope, IJavaSearchConstants.TYPE, null, res1);
		assertNotNull("TestCase not found", findTypeRef(res1, "junit.framework.TestCase"));
		assertTrue("542 types expected, is " + res1.size(), res1.size() == 542);
		
		int nFlushes= AllTypesCache.getNumberOfCacheFlushes();
		
		IType type= fJProject2.findType("junit.framework.TestCase");
		assertNotNull("TestCase not found", type);
		
		IEditorPart part= EditorUtility.openInEditor(type);
		try {
			IDocument document= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			ISourceRange range= type.getNameRange();
			document.replace(range.getOffset(), range.getLength(), "A");
			
			res1.clear();
			AllTypesCache.getTypes(workspaceScope, IJavaSearchConstants.TYPE, null, res1);
			assertNotNull("A not found", findTypeRef(res1, "junit.framework.A"));
			assertNull("TestCase still found", findTypeRef(res1, "junit.framework.TestCase"));
			
			assertTrue("542 types in workspace expected, is " + res1.size(), res1.size() == 542);
			
			assertTrue("cache not flushed", nFlushes != AllTypesCache.getNumberOfCacheFlushes());
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	
	}
	
	private TypeInfo findTypeRef(List refs, String fullname) {
		for (int i= 0; i <refs.size(); i++) {
			TypeInfo curr= (TypeInfo) refs.get(i);
			if (fullname.equals(curr.getFullyQualifiedName())) {
				return curr;
			}
		}
		return null;
	}
		
	
}
