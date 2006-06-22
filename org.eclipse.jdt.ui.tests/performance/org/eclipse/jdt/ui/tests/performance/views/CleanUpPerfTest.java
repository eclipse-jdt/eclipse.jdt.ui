/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.performance.views;

import java.io.File;
import java.util.Map;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring;
import org.eclipse.jdt.internal.corext.fix.IFix;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.fix.CodeStyleCleanUp;
import org.eclipse.jdt.internal.ui.fix.ControlStatementsCleanUp;
import org.eclipse.jdt.internal.ui.fix.ExpressionsCleanUp;
import org.eclipse.jdt.internal.ui.fix.ICleanUp;
import org.eclipse.jdt.internal.ui.fix.Java50CleanUp;
import org.eclipse.jdt.internal.ui.fix.PotentialProgrammingProblemsCleanUp;
import org.eclipse.jdt.internal.ui.fix.StringCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnnecessaryCodeCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnusedCodeCleanUp;
import org.eclipse.jdt.internal.ui.fix.VariableDeclarationCleanUp;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.jdt.ui.tests.performance.JdtPerformanceTestCase;

public class CleanUpPerfTest extends JdtPerformanceTestCase {
	
	private static class MyTestSetup extends TestSetup {
		public static final String SRC_CONTAINER= "src";
		
		public static IJavaProject fJProject1;
		public static IPackageFragmentRoot fJunitSrcRoot;
		
		public MyTestSetup(Test test) {
			super(test);
		}
		
		protected void setUp() throws Exception {
			fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
			assertTrue("rt not found", JavaProjectHelper.addRTJar(fJProject1) != null);
			File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);
			fJunitSrcRoot= JavaProjectHelper.addSourceContainerWithImport(fJProject1, SRC_CONTAINER, junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);
		}
	
		protected void tearDown() throws Exception {
			if (fJProject1 != null && fJProject1.exists())
				JavaProjectHelper.delete(fJProject1);
		}
	}
	
	public static Test suite() {
		return new MyTestSetup(new TestSuite(CleanUpPerfTest.class));
	}

	public static Test setUpTest(Test someTest) {
		return new MyTestSetup(someTest);
	}
	
	private void addAllCUs(CleanUpRefactoring cleanUp, IJavaElement[] children) throws JavaModelException {
		for (int i= 0; i < children.length; i++) {
			IJavaElement element= children[i];
			if (element instanceof ICompilationUnit) {
				cleanUp.addCompilationUnit((ICompilationUnit)element);
			} else if (element instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot root= ((IPackageFragmentRoot)element);
				addAllCUs(cleanUp, root.getChildren());
			} else if (element instanceof IPackageFragment) {
				IPackageFragment pack= ((IPackageFragment)element);
				addAllCUs(cleanUp, pack.getChildren());
			}
		}
	}
	
	public void testNullCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());
		cleanUpRefactoring.addCleanUp(new ICleanUp() {
			public IFix createFix(CompilationUnit compilationUnit) throws CoreException {return null;}
			public Map getRequiredOptions() {return null;}
			public void saveSettings(IDialogSettings settings) {}
			public String[] getDescriptions() {return null;}
			public boolean canFix(CompilationUnit compilationUnit, IProblemLocation problem) throws CoreException {
				return true;
			}
			public IFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
				return null;
			}
			public RefactoringStatus checkPostConditions(IProgressMonitor monitor) throws CoreException {
				return new RefactoringStatus();
			}
			public RefactoringStatus checkPreConditions(IJavaProject project, ICompilationUnit[] compilationUnits, IProgressMonitor monitor) throws CoreException {
				return new RefactoringStatus();
			}
			public int maximalNumberOfFixes(CompilationUnit compilationUnit) {
				return 0;
			}
			public int getDefaultFlag() {
				return 0;
			}
			public void setFlag(int flag, boolean b) {
			}
			public boolean isFlag(int flag) {
				return true;
			}
			public String getPreview() {
				return null;
			}
		});
		
		cleanUpRefactoring.checkAllConditions(new NullProgressMonitor());
		cleanUpRefactoring.createChange(null);
		
		joinBackgroudActivities();
		for (int i= 0; i < 10; i++) {
			startMeasuring();
			cleanUpRefactoring.checkAllConditions(new NullProgressMonitor());
			cleanUpRefactoring.createChange(null);
			finishMeasurements();
		}
	}
	
	public void testAllCleanUps() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());
		
		cleanUpRefactoring.addCleanUp(new CodeStyleCleanUp(
				CodeStyleCleanUp.QUALIFY_FIELD_ACCESS |
				CodeStyleCleanUp.CHANGE_NON_STATIC_ACCESS_TO_STATIC |
				CodeStyleCleanUp.QUALIFY_STATIC_FIELD_ACCESS |
				CodeStyleCleanUp.CHANGE_INDIRECT_STATIC_ACCESS_TO_DIRECT |
				CodeStyleCleanUp.QUALIFY_METHOD_ACCESS |
				CodeStyleCleanUp.QUALIFY_STATIC_METHOD_ACCESS));
		
		cleanUpRefactoring.addCleanUp(new ControlStatementsCleanUp(
				ControlStatementsCleanUp.ADD_BLOCK_TO_CONTROL_STATEMENTS |
				ControlStatementsCleanUp.CONVERT_FOR_LOOP_TO_ENHANCED_FOR_LOOP |
				ControlStatementsCleanUp.REMOVE_UNNECESSARY_BLOCKS));
		
		cleanUpRefactoring.addCleanUp(new ExpressionsCleanUp(
				ExpressionsCleanUp.ADD_PARANOIC_PARENTHESIS));
		
		cleanUpRefactoring.addCleanUp(new Java50CleanUp(
				Java50CleanUp.ADD_DEPRECATED_ANNOTATION | 
				Java50CleanUp.ADD_OVERRIDE_ANNOATION));
		
		cleanUpRefactoring.addCleanUp(new PotentialProgrammingProblemsCleanUp(
				PotentialProgrammingProblemsCleanUp.ADD_DEFAULT_SERIAL_VERSION_ID));
		
		cleanUpRefactoring.addCleanUp(new StringCleanUp(
				StringCleanUp.ADD_MISSING_NLS_TAG |
				StringCleanUp.REMOVE_UNNECESSARY_NLS_TAG));
		
		cleanUpRefactoring.addCleanUp(new UnnecessaryCodeCleanUp(
				UnnecessaryCodeCleanUp.REMOVE_UNUSED_CAST));
		
		cleanUpRefactoring.addCleanUp(new UnusedCodeCleanUp(
				UnusedCodeCleanUp.REMOVE_UNUSED_IMPORTS |
				UnusedCodeCleanUp.REMOVE_UNUSED_PRIVATE_CONSTRUCTORS |
				UnusedCodeCleanUp.REMOVE_UNUSED_PRIVATE_METHODS |
				UnusedCodeCleanUp.REMOVE_UNUSED_PRIVATE_TYPES |
				UnusedCodeCleanUp.REMOVE_UNUSED_PRIVATE_FIELDS |
				UnusedCodeCleanUp.REMOVE_UNUSED_LOCAL_VARIABLES));
		
		cleanUpRefactoring.addCleanUp(new VariableDeclarationCleanUp(
				VariableDeclarationCleanUp.ADD_FINAL_MODIFIER_FIELDS |
				VariableDeclarationCleanUp.ADD_FINAL_MODIFIER_PARAMETERS |
				VariableDeclarationCleanUp.ADD_FINAL_MODIFIER_LOCAL_VARIABLES));
		
		//See https://bugs.eclipse.org/bugs/show_bug.cgi?id=135219
//		tagAsSummary("Code Clean Up - 25 clean-ups", Dimension.ELAPSED_PROCESS);
		
		cleanUpRefactoring.checkAllConditions(new NullProgressMonitor());
		cleanUpRefactoring.createChange(null);
		
		joinBackgroudActivities();
		for (int i= 0; i < 10; i++) {
			startMeasuring();
			cleanUpRefactoring.checkAllConditions(new NullProgressMonitor());
			cleanUpRefactoring.createChange(null);
			finishMeasurements();
		}
	}
	
	public void testCodeStyleCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());
		
		cleanUpRefactoring.addCleanUp(new CodeStyleCleanUp(
				CodeStyleCleanUp.QUALIFY_FIELD_ACCESS |
				CodeStyleCleanUp.CHANGE_NON_STATIC_ACCESS_TO_STATIC |
				CodeStyleCleanUp.QUALIFY_STATIC_FIELD_ACCESS |
				CodeStyleCleanUp.CHANGE_INDIRECT_STATIC_ACCESS_TO_DIRECT |
				CodeStyleCleanUp.QUALIFY_METHOD_ACCESS |
				CodeStyleCleanUp.QUALIFY_STATIC_METHOD_ACCESS));
		
		cleanUpRefactoring.checkAllConditions(new NullProgressMonitor());
		cleanUpRefactoring.createChange(null);
		
		joinBackgroudActivities();
		for (int i= 0; i < 10; i++) {
			startMeasuring();
			cleanUpRefactoring.checkAllConditions(new NullProgressMonitor());
			cleanUpRefactoring.createChange(null);
			finishMeasurements();
		}
	}
	
	public void testControlStatementsCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());
		
		cleanUpRefactoring.addCleanUp(new ControlStatementsCleanUp(
				ControlStatementsCleanUp.ADD_BLOCK_TO_CONTROL_STATEMENTS |
				ControlStatementsCleanUp.CONVERT_FOR_LOOP_TO_ENHANCED_FOR_LOOP |
				ControlStatementsCleanUp.REMOVE_UNNECESSARY_BLOCKS));
		
		cleanUpRefactoring.checkAllConditions(new NullProgressMonitor());
		cleanUpRefactoring.createChange(null);
		
		joinBackgroudActivities();
		for (int i= 0; i < 10; i++) {
			startMeasuring();
			cleanUpRefactoring.checkAllConditions(new NullProgressMonitor());
			cleanUpRefactoring.createChange(null);
			finishMeasurements();
		}
	}

	public void testExpressionsCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());
		
		cleanUpRefactoring.addCleanUp(new ExpressionsCleanUp(
				ExpressionsCleanUp.ADD_PARANOIC_PARENTHESIS));
		
		cleanUpRefactoring.checkAllConditions(new NullProgressMonitor());
		cleanUpRefactoring.createChange(null);
		
		joinBackgroudActivities();
		for (int i= 0; i < 10; i++) {
			startMeasuring();
			cleanUpRefactoring.checkAllConditions(new NullProgressMonitor());
			cleanUpRefactoring.createChange(null);
			finishMeasurements();
		}
	}
	
	public void testJava50CleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());
		
		cleanUpRefactoring.addCleanUp(new Java50CleanUp(
				Java50CleanUp.ADD_DEPRECATED_ANNOTATION | 
				Java50CleanUp.ADD_OVERRIDE_ANNOATION));
		
		cleanUpRefactoring.checkAllConditions(new NullProgressMonitor());
		cleanUpRefactoring.createChange(null);
		
		joinBackgroudActivities();
		for (int i= 0; i < 10; i++) {
			startMeasuring();
			cleanUpRefactoring.checkAllConditions(new NullProgressMonitor());
			cleanUpRefactoring.createChange(null);
			finishMeasurements();
		}
	}
	
	public void testStringCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());
		
		cleanUpRefactoring.addCleanUp(new StringCleanUp(
				StringCleanUp.ADD_MISSING_NLS_TAG |
				StringCleanUp.REMOVE_UNNECESSARY_NLS_TAG));
		
		cleanUpRefactoring.checkAllConditions(new NullProgressMonitor());
		cleanUpRefactoring.createChange(null);
		
		joinBackgroudActivities();
		for (int i= 0; i < 10; i++) {
			startMeasuring();
			cleanUpRefactoring.checkAllConditions(new NullProgressMonitor());
			cleanUpRefactoring.createChange(null);
			finishMeasurements();
		}
	}
	
	public void testUnnecessaryCodeCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());
		
		cleanUpRefactoring.addCleanUp(new UnnecessaryCodeCleanUp(
				UnnecessaryCodeCleanUp.REMOVE_UNUSED_CAST));
		
		cleanUpRefactoring.checkAllConditions(new NullProgressMonitor());
		cleanUpRefactoring.createChange(null);
		
		joinBackgroudActivities();
		for (int i= 0; i < 10; i++) {
			startMeasuring();
			cleanUpRefactoring.checkAllConditions(new NullProgressMonitor());
			cleanUpRefactoring.createChange(null);
			finishMeasurements();
		}
	}
	
	public void testUnusedCodeCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		cleanUpRefactoring.addCleanUp(new UnusedCodeCleanUp(
				UnusedCodeCleanUp.REMOVE_UNUSED_IMPORTS |
				UnusedCodeCleanUp.REMOVE_UNUSED_PRIVATE_CONSTRUCTORS |
				UnusedCodeCleanUp.REMOVE_UNUSED_PRIVATE_METHODS |
				UnusedCodeCleanUp.REMOVE_UNUSED_PRIVATE_TYPES |
				UnusedCodeCleanUp.REMOVE_UNUSED_PRIVATE_FIELDS |
				UnusedCodeCleanUp.REMOVE_UNUSED_LOCAL_VARIABLES));
	
		cleanUpRefactoring.checkAllConditions(new NullProgressMonitor());
		cleanUpRefactoring.createChange(null);
		
		joinBackgroudActivities();
		for (int i= 0; i < 10; i++) {
			startMeasuring();
			cleanUpRefactoring.checkAllConditions(new NullProgressMonitor());
			cleanUpRefactoring.createChange(null);
			finishMeasurements();
		}
	}
	
	public void testVariableDeclarationCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());
		
		cleanUpRefactoring.addCleanUp(new VariableDeclarationCleanUp(
				VariableDeclarationCleanUp.ADD_FINAL_MODIFIER_FIELDS |
				VariableDeclarationCleanUp.ADD_FINAL_MODIFIER_PARAMETERS |
				VariableDeclarationCleanUp.ADD_FINAL_MODIFIER_LOCAL_VARIABLES));
		
		cleanUpRefactoring.checkAllConditions(new NullProgressMonitor());
		cleanUpRefactoring.createChange(null);
		
		joinBackgroudActivities();
		for (int i= 0; i < 10; i++) {
			startMeasuring();
			cleanUpRefactoring.checkAllConditions(new NullProgressMonitor());
			cleanUpRefactoring.createChange(null);
			finishMeasurements();
		}
	}
}
