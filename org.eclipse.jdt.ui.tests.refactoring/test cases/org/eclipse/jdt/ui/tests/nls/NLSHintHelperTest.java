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

package org.eclipse.jdt.ui.tests.nls;

import java.io.File;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.Path;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.corext.refactoring.nls.AccessorClassReference;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSHintHelper;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;


/**
 * Tests the NLSHintHelper.
 * 
 * @since 3.1
 */
public class NLSHintHelperTest extends TestCase {

	private IJavaProject fJProject;
	private IPackageFragmentRoot fLibrary;

	
	public static Test suite() {
		return new ProjectTestSetup(new TestSuite(NLSHintHelperTest.class));
	}


	protected void setUp() throws Exception {
		fJProject= ProjectTestSetup.getProject();
		File lib= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.NLS_LIB);
		assertTrue("lib does not exist",  lib != null && lib.exists());
		fLibrary= JavaProjectHelper.addLibrary(fJProject, Path.fromOSString(lib.getPath())); // add library to project
	}
	
	public void testFind() {
		try {
			assertNotNull(NLSHintHelper.getResourceBundle(fLibrary, "pkg", "Messages.properties"));
		} catch (JavaModelException e) {
			fail();
		}
		IPackageFragment pkg= fLibrary.getPackageFragment("pkg");
		IClassFile classFile= pkg.getClassFile("Client.class");
		IRegion region= new Region(648, 4);
		CompilationUnit ast= JavaPlugin.getDefault().getASTProvider().getAST(classFile, ASTProvider.WAIT_YES, null);
		AccessorClassReference accessor= NLSHintHelper.getAccessorClassReference(ast, region);
		assertNotNull(accessor);
		Properties properties= NLSHintHelper.getProperties(fJProject, accessor.getBinding());
		assertNotNull(properties);
		assertEquals("Hello World", properties.get("test"));
		try {
			assertNotNull(NLSHintHelper.getResourceBundle(fJProject, accessor.getBinding()));
		} catch (JavaModelException e1) {
			fail();
		}
		
	}
	
	public void testDoNotFind() {
		try {
			assertNull(NLSHintHelper.getResourceBundle(fJProject, "pkg", "Messages.properties"));
		} catch (JavaModelException e) {
			fail();
		}
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject, ProjectTestSetup.getDefaultClasspath());
	}
}
