/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alex Blewitt - https://bugs.eclipse.org/bugs/show_bug.cgi?id=168954
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.performance.views;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CleanUpPreferenceUtil;
import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring;
import org.eclipse.jdt.internal.corext.fix.IFix;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.fix.CodeStyleCleanUp;
import org.eclipse.jdt.internal.ui.fix.ControlStatementsCleanUp;
import org.eclipse.jdt.internal.ui.fix.ConvertLoopCleanUp;
import org.eclipse.jdt.internal.ui.fix.ExpressionsCleanUp;
import org.eclipse.jdt.internal.ui.fix.ICleanUp;
import org.eclipse.jdt.internal.ui.fix.Java50CleanUp;
import org.eclipse.jdt.internal.ui.fix.SortMembersCleanUp;
import org.eclipse.jdt.internal.ui.fix.StringCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnnecessaryCodeCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnusedCodeCleanUp;
import org.eclipse.jdt.internal.ui.fix.VariableDeclarationCleanUp;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpProfileVersioner;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileStore;

import org.eclipse.test.performance.Dimension;

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
				IPackageFragmentRoot root= (IPackageFragmentRoot)element;
				addAllCUs(cleanUp, root.getChildren());
			} else if (element instanceof IPackageFragment) {
				IPackageFragment pack= (IPackageFragment)element;
				addAllCUs(cleanUp, pack.getChildren());
			}
		}
	}
	
	private static Map getNullSettings() {
		Map result= new HashMap();
		
		Collection keys= CleanUpConstants.getEclipseDefaultSettings().keySet();
		for (Iterator iterator= keys.iterator(); iterator.hasNext();) {
			String key= (String)iterator.next();
			result.put(key, CleanUpConstants.FALSE);
		}
		
		return result;
	}
	
	private static void storeSettings(Map node) throws CoreException {
		ProfileManager.CustomProfile profile= new ProfileManager.CustomProfile("testProfile", node, CleanUpProfileVersioner.CURRENT_VERSION, CleanUpProfileVersioner.PROFILE_KIND);
		new InstanceScope().getNode(JavaUI.ID_PLUGIN).put(CleanUpConstants.CLEANUP_PROFILE, profile.getID());
		
		List profiles= CleanUpPreferenceUtil.getBuiltInProfiles();
		profiles.add(profile);
		
		CleanUpProfileVersioner versioner= new CleanUpProfileVersioner();
		ProfileStore profileStore= new ProfileStore(CleanUpConstants.CLEANUP_PROFILES, versioner);
		profileStore.writeProfiles(profiles, new InstanceScope());
	}
	
	public void testNullCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());
		cleanUpRefactoring.addCleanUp(new ICleanUp() {
			public IFix createFix(CompilationUnit compilationUnit) throws CoreException {
				return null;
			}
			
			public Map getRequiredOptions() {
				return null;
			}
			
			public String[] getDescriptions() {
				return null;
			}
			
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
			
			public String getPreview() {
				return null;
			}
			
			public boolean needsFreshAST(CompilationUnit compilationUnit) {
				return false;
			}
			
			public void initialize(Map settings) throws CoreException {}
			
			public IFix createFix(ICompilationUnit unit) throws CoreException {
				return null;
			}
			
			public boolean requireAST(ICompilationUnit unit) throws CoreException {
				return true;
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
		
		Map node= getNullSettings();
		
		node.put(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS, CleanUpConstants.TRUE);
		
		node.put(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_ALWAYS, CleanUpConstants.TRUE);
		
		node.put(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS, CleanUpConstants.TRUE);
		
		node.put(CleanUpConstants.ADD_MISSING_ANNOTATIONS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE, CleanUpConstants.TRUE);
		
		node.put(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_DEFAULT, CleanUpConstants.TRUE);
		
		node.put(CleanUpConstants.ADD_MISSING_NLS_TAGS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.REMOVE_UNNECESSARY_CASTS, CleanUpConstants.TRUE);
		
		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_METHODS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES, CleanUpConstants.TRUE);
		
		node.put(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS, CleanUpConstants.TRUE);
		
		node.put(CleanUpConstants.FORMAT_SOURCE_CODE, CleanUpConstants.TRUE);
		
		node.put(CleanUpConstants.ORGANIZE_IMPORTS, CleanUpConstants.TRUE);
		
		storeSettings(node);
		
		ICleanUp[] cleanUps= CleanUpRefactoring.createCleanUps();
		for (int i= 0; i < cleanUps.length; i++) {
			cleanUpRefactoring.addCleanUp(cleanUps[i]);
		}
		
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
		tagAsSummary("Clean Up - Code Style", Dimension.ELAPSED_PROCESS);
		
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());
		
		Map node= getNullSettings();
		
		node.put(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS, CleanUpConstants.TRUE);
		
		storeSettings(node);
		
		cleanUpRefactoring.addCleanUp(new CodeStyleCleanUp());
		
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
		
		Map node= getNullSettings();
		
		node.put(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_ALWAYS, CleanUpConstants.TRUE);
		
		storeSettings(node);
		
		cleanUpRefactoring.addCleanUp(new ControlStatementsCleanUp());
		cleanUpRefactoring.addCleanUp(new ConvertLoopCleanUp());
		
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
	
	public void testConvertLoopCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());
		
		Map node= getNullSettings();
		
		node.put(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED, CleanUpConstants.TRUE);
		
		storeSettings(node);
		
		cleanUpRefactoring.addCleanUp(new ControlStatementsCleanUp());
		cleanUpRefactoring.addCleanUp(new ConvertLoopCleanUp());
		
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
		
		Map node= getNullSettings();
		
		node.put(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS, CleanUpConstants.TRUE);
		
		storeSettings(node);
		
		cleanUpRefactoring.addCleanUp(new ExpressionsCleanUp());
		
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
		
		Map node= getNullSettings();
		
		node.put(CleanUpConstants.ADD_MISSING_ANNOTATIONS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE, CleanUpConstants.TRUE);
		
		storeSettings(node);
		
		cleanUpRefactoring.addCleanUp(new Java50CleanUp());
		
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
		
		Map node= getNullSettings();
		
		node.put(CleanUpConstants.ADD_MISSING_NLS_TAGS, CleanUpConstants.TRUE);
		
		storeSettings(node);
		
		cleanUpRefactoring.addCleanUp(new StringCleanUp());
		
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
	
	public void testSortMembersCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());
		
		Map node= getNullSettings();
		
		node.put(CleanUpConstants.SORT_MEMBERS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.SORT_MEMBERS_ALL, CleanUpConstants.TRUE);
		
		storeSettings(node);
		
		cleanUpRefactoring.addCleanUp(new SortMembersCleanUp());
		
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
		
		Map node= getNullSettings();
		node.put(CleanUpConstants.REMOVE_UNNECESSARY_CASTS, CleanUpConstants.TRUE);
		storeSettings(node);
		
		cleanUpRefactoring.addCleanUp(new UnnecessaryCodeCleanUp());
		
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
		
		Map node= getNullSettings();
		
		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_METHODS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES, CleanUpConstants.TRUE);
		
		storeSettings(node);
		
		cleanUpRefactoring.addCleanUp(new UnusedCodeCleanUp());
		
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
		
		Map node= getNullSettings();
		
		node.put(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS, CleanUpConstants.TRUE);
		node.put(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS, CleanUpConstants.TRUE);
		
		storeSettings(node);
		
		cleanUpRefactoring.addCleanUp(new VariableDeclarationCleanUp());
		
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
