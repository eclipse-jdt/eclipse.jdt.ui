/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.Java9ProjectTestSetup;

import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks;

public class BindingLabels9Test extends AbstractBindingLabelsTest {

	@Rule
	public Java9ProjectTestSetup j9p= new Java9ProjectTestSetup();

	public void setUp() throws Exception {
		fJProject1= j9p.getProject();

		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
		store.setValue(PreferenceConstants.APPEARANCE_COMPRESS_PACKAGE_NAMES, false);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, j9p.getDefaultClasspath());
	}

	@Test
	public void testModuleWithCategory1() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String content= """
			/** @category test */
			module mymod {}
			""";
		ICompilationUnit cu= sourceFolder.getPackageFragment("").createCompilationUnit("module-info.java", content, false, null);

		IJavaElement[] elems= cu.codeSelect(content.lastIndexOf("mymod"), 5);
		IJavaElement i= elems[0];
		String lab= getBindingLabel(i, JavaElementLabels.ALL_CATEGORY2 | JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_FULLY_QUALIFIED);
		assertEquals("mymod", lab); // category not shown in binding labels
	}

	@Test
	public void testModuleWithAnnotation() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String content= """
			@Deprecated
			module mymod {}
			""";
		ICompilationUnit cu= sourceFolder.getPackageFragment("").createCompilationUnit("module-info.java", content, false, null);

		IJavaElement[] elems= cu.codeSelect(content.lastIndexOf("mymod"), 5);
		IJavaElement i= elems[0];
		String lab= getBindingLabel(i, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_FULLY_QUALIFIED);
		assertLinkMatch(lab, "@{{java.lang.Deprecated}} mymod");

		IJavaElement link= JavaElementLinks.parseURI(extractURI(lab));
		String linkID= link.getHandleIdentifier();
		String jreBaseID= fJProject1.findModule("java.base", null).getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT).getHandleIdentifier();
		assertEquals("Linked element", jreBaseID+"<java.lang(Deprecated.class[Deprecated", linkID);
	}
}
