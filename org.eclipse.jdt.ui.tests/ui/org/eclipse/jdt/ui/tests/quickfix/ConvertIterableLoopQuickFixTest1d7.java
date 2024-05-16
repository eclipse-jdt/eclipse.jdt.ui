/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - copied and modified from ConvertibleLoopQuickFixTest
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import static org.junit.Assert.assertNull;

import java.util.Hashtable;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.Java1d7ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.QuickAssistProcessor;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposal;

public final class ConvertIterableLoopQuickFixTest1d7 extends QuickFixTest {

	@Rule
	public ProjectTestSetup projectSetup = new Java1d7ProjectTestSetup();

	private FixCorrectionProposal fConvertLoopProposal;

	private IJavaProject fProject;

	private IPackageFragmentRoot fSourceFolder;

	private List<IJavaCompletionProposal> fetchConvertingProposal(String sample, ICompilationUnit cu) throws Exception {
		int offset= sample.indexOf("for");
		return fetchConvertingProposal(cu, offset);
	}

	private List<IJavaCompletionProposal> fetchConvertingProposal(ICompilationUnit cu, int offset) throws Exception {
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		fConvertLoopProposal= (FixCorrectionProposal)findProposalByCommandId(QuickAssistProcessor.CONVERT_FOR_LOOP_ID, proposals);
		return proposals;
	}

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fProject= projectSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fProject, "src");
		fConvertLoopProposal= null;
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fProject, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testBug563267() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.Iterator;
			import java.util.List;
			public class A {
			    public void foo(List<String> list) {
			        List<InputStream> toClose = new ArrayList<>();
			        for (Iterator<InputStream> it = toClose.iterator(); it.hasNext();) {
			            try (InputStream r = it.next()) {
			            }
			        }
			        toClose.clear();
			    }
			}
			""";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

}
