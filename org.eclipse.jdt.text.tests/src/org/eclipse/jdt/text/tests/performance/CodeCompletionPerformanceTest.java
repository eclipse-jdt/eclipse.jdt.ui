/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.performance;

import java.util.Arrays;
import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.JavaCompletionProposalComparator;
import org.eclipse.jdt.ui.text.java.ResultCollector;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.java.ExperimentalResultCollector;

public class CodeCompletionPerformanceTest extends TextPerformanceTestCase {

	private static final Class THIS= CodeCompletionPerformanceTest.class;

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}

	public static Test suite() {
		return allTests();
	}

	private IJavaProject fJProject1;

	private static final int WARM_UP_RUNS= 10;
	private static final int MEASURED_RUNS= 10;

	private ICompilationUnit fCU;
	private String fContents;
	private int fCodeAssistOffset;
	private IPackageFragmentRoot fSourceFolder;

	public CodeCompletionPerformanceTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		JavaProjectHelper.addRTJar(fJProject1);
		JavaProjectHelper.addRequiredProject(fJProject1, ProjectTestSetup.getProject());

		Hashtable options= TestOptions.getFormatterOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "1");
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, true);
		store.setValue(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS, false);

		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);
		
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment fragment= fSourceFolder.createPackageFragment("test1", false, null);
		fContents= "package test1;\n" +
				 "\n" +
				 "public class Completion {\n" +
				 "    \n" +
				 "    void foomethod() {\n" +
				 "        int intVal=5;\n" +
				 "        long longVal=3;\n" +
				 "        Runnable run= null;\n" +
				 "        run.//here\n" +
				 "    }\n" +
				 "}\n";
		fCU= fragment.createCompilationUnit("Completion.java", fContents, false, null);

		String str= "//here";
		fCodeAssistOffset= fContents.indexOf(str);

		EditorTestHelper.joinJobs(1000, 10000, 100);
	}
	
	private IJavaCompletionProposal[] codeComplete(ResultCollector collector) throws JavaModelException {
		collector.setReplacementLength(0);

		fCU.codeComplete(fCodeAssistOffset, collector);
		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();
		JavaCompletionProposalComparator comparator= new JavaCompletionProposalComparator();
		comparator.setOrderAlphabetically(true);
		Arrays.sort(proposals, comparator);
		return proposals;
	}

	protected void tearDown() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setToDefault(PreferenceConstants.CODEGEN_ADD_COMMENTS);
		store.setToDefault(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS);

		JavaProjectHelper.delete(fJProject1);
		
		super.tearDown();
	}

	public void testCompletionNoParamters() throws Exception {
		measureCompletionNoParameters(getNullPerformanceMeter(), getWarmUpRuns());
		measureCompletionNoParameters(createPerformanceMeter(), getMeasuredRuns());
		commitAllMeasurements();
		assertAllPerformance();
	}
	
	private void measureCompletionNoParameters(PerformanceMeter meter, final int runs) throws Exception {
		for (int run= 0; run < runs; run++) {
			meter.start();
			
			ResultCollector collector= new ResultCollector(fCU);
			IJavaCompletionProposal[] proposals= codeComplete(collector);

			applyProposal(proposals[0], "clone()");
			applyProposal(proposals[1], "equals()");
			applyProposal(proposals[11], "wait()");

			meter.stop();
		}

	}
	
	public void testCompletionWithParamterNames() throws Exception {
		measureCompletionWithParamterNames(getNullPerformanceMeter(), getWarmUpRuns());
		measureCompletionWithParamterNames(createPerformanceMeter(), getMeasuredRuns());
		commitAllMeasurements();
		assertAllPerformance();
	}
	
	private void measureCompletionWithParamterNames(PerformanceMeter meter, final int runs) throws Exception {
		for (int run= 0; run < runs; run++) {
			meter.start();
			
			ResultCollector collector= new ExperimentalResultCollector(fCU);
			IJavaCompletionProposal[] proposals= codeComplete(collector);

			applyProposal(proposals[0], "clone()");
			applyProposal(proposals[1], "equals(arg0)");
			applyProposal(proposals[11], "wait(arg0, arg1)");

			meter.stop();
		}
	}

	public void testCompletionWithParamterGuesses() throws Exception {
		measureCompletionWithParamterGuesses(getNullPerformanceMeter(), getWarmUpRuns());
		measureCompletionWithParamterGuesses(createPerformanceMeter(), getMeasuredRuns());
		commitAllMeasurements();
		assertAllPerformance();
	}
	
	private void measureCompletionWithParamterGuesses(PerformanceMeter meter, final int runs) throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS, true);
		
		for (int run= 0; run < runs; run++) {
			meter.start();
			
			ResultCollector collector= new ExperimentalResultCollector(fCU);
			IJavaCompletionProposal[] proposals= codeComplete(collector);

			applyProposal(proposals[0], "clone()");
			applyProposal(proposals[1], "equals(run)");
			applyProposal(proposals[11], "wait(longVal, intVal)");

			meter.stop();
		}

	}

	public void testCompletionWithParamterGuesses2() throws Exception {
		createTypeHierarchy();

		measureCompletionWithParamterGuesses2(getNullPerformanceMeter(), getWarmUpRuns());
		measureCompletionWithParamterGuesses2(createPerformanceMeter(), getMeasuredRuns());
		commitAllMeasurements();
		assertAllPerformance();
	}
	
	private void createTypeHierarchy() throws JavaModelException {
		IPackageFragment fragment= fSourceFolder.createPackageFragment("test2", false, null);
		
		String parent= "HashMap";
		String content= null;

		for (int i= 0; i < 20; i++) {
			String cu= "Completion" + i;
			String field= "fField" + i;
			content= "package test2;\n" +
					"\n" +
					"public class " + cu + " extends " + parent + " {\n" +
					"    int" + field + ";\n" +
					"    \n" +
					"    void foomethod() {\n" +
					"        int intVal=5;\n" +
					"        long longVal=3;\n" +
					"        Runnable run= null;\n" +
					"        run.//here\n" +
					"    }\n" +
					"}\n";
			fCU= fragment.createCompilationUnit(cu + ".java", content, false, null);
			parent= cu;
		}
		
		String str= "//here";
		fCodeAssistOffset= content.indexOf(str);

		EditorTestHelper.joinJobs(1000, 10000, 100);
	}

	private void measureCompletionWithParamterGuesses2(PerformanceMeter meter, final int runs) throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS, true);
		
		for (int run= 0; run < runs; run++) {
			meter.start();
			
			ResultCollector collector= new ExperimentalResultCollector(fCU);
			IJavaCompletionProposal[] proposals= codeComplete(collector);

			applyProposal(proposals[0], "clone()");
			applyProposal(proposals[1], "equals(run)");
			applyProposal(proposals[11], "wait(longVal, intVal)");

			meter.stop();
		}

	}

	private void applyProposal(IJavaCompletionProposal proposal, String completion) {
		IDocument doc= new Document(fContents);
		proposal.apply(doc);
	}

}
