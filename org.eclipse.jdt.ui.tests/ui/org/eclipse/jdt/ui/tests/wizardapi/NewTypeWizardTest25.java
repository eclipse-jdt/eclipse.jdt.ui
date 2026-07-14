/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ui.tests.wizardapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.StringAsserts;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.Java25ProjectTestSetup;
import org.eclipse.jdt.ui.wizards.NewCompactWizardPage;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class NewTypeWizardTest25 {

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	private IPackageFragment fpack1;

	@Rule
	public Java25ProjectTestSetup projectSetup= new Java25ProjectTestSetup();

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= projectSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		fpack1= fSourceFolder.createPackageFragment("test1", false, null);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testCreateCompactType() throws Exception {
		NewCompactWizardPage wizardPage= new NewCompactWizardPage();
		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		wizardPage.setPackageFragment(fpack1, true);
		wizardPage.setTypeName("Hello", true);
		wizardPage.setModifiers(Flags.AccPublic, true);

		assertEquals(Flags.AccPublic, wizardPage.getModifiers());

		wizardPage.createType(null);

		String actual= wizardPage.getCreatedType().getCompilationUnit().getSource();
		assertFalse("Source must not start with a blank line", actual.startsWith("\n"));
		String expected= """
			public void main() {
			    // TODO Auto-generated method stub

			}
			""";
		String cuName= wizardPage.getCreatedType().getCompilationUnit().getElementName();
		assertEquals("Hello.java", cuName);
		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}
}