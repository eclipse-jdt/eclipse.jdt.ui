/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks;

import junit.framework.Test;
import junit.framework.TestSuite;

public class BindingLabels9Test extends AbstractBindingLabelsTest {
	private static final Class<BindingLabels9Test> THIS= BindingLabels9Test.class;

	public BindingLabels9Test(String name) {
		super(name);
	}

	public static Test suite() {
		return setUpTest(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new Java9ProjectTestSetup(test);
	}

	@Override
	protected void setUp() throws Exception {
		fJProject1= Java9ProjectTestSetup.getProject();

		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
		store.setValue(PreferenceConstants.APPEARANCE_COMPRESS_PACKAGE_NAMES, false);
	}

	@Override
	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, Java9ProjectTestSetup.getDefaultClasspath());
	}
	

	public void testModuleWithCategory1() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuffer buf= new StringBuffer();
		buf.append("/** @category test */\n");
		buf.append("module mymod {}\n");
		String content= buf.toString();
		ICompilationUnit cu= sourceFolder.getPackageFragment("").createCompilationUnit("module-info.java", content, false, null);

		IJavaElement[] elems= cu.codeSelect(content.lastIndexOf("mymod"), 5);
		IJavaElement i= elems[0];
		String lab= getBindingLabel(i, JavaElementLabels.ALL_CATEGORY | JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_FULLY_QUALIFIED);
		assertEquals("mymod", lab); // category not shown in binding labels
	}

	public void testModuleWithAnnotation() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuffer buf= new StringBuffer();
		buf.append("@Deprecated\n");
		buf.append("module mymod {}\n");
		String content= buf.toString();
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
