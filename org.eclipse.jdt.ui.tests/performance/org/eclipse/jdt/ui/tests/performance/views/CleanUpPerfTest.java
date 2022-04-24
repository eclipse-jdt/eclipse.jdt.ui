/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     Alex Blewitt - https://bugs.eclipse.org/bugs/show_bug.cgi?id=168954
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.performance.views;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runners.MethodSorters;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;
import org.eclipse.test.performance.Dimension;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.PerformRefactoringOperation;
import org.eclipse.ltk.core.refactoring.RefactoringCore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CleanUpPreferenceUtil;
import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.jdt.ui.tests.performance.JdtPerformanceTestCaseCommon;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.fix.AbstractCleanUp;
import org.eclipse.jdt.internal.ui.fix.ArrayWithCurlyCleanUp;
import org.eclipse.jdt.internal.ui.fix.ArraysFillCleanUp;
import org.eclipse.jdt.internal.ui.fix.AutoboxingCleanUp;
import org.eclipse.jdt.internal.ui.fix.BitwiseConditionalExpressionCleanup;
import org.eclipse.jdt.internal.ui.fix.BooleanLiteralCleanUp;
import org.eclipse.jdt.internal.ui.fix.BooleanValueRatherThanComparisonCleanUp;
import org.eclipse.jdt.internal.ui.fix.BreakLoopCleanUp;
import org.eclipse.jdt.internal.ui.fix.CodeFormatCleanUp;
import org.eclipse.jdt.internal.ui.fix.CodeStyleCleanUp;
import org.eclipse.jdt.internal.ui.fix.CollectionCloningCleanUp;
import org.eclipse.jdt.internal.ui.fix.ComparingOnCriteriaCleanUp;
import org.eclipse.jdt.internal.ui.fix.ConstantsForSystemPropertyCleanUp;
import org.eclipse.jdt.internal.ui.fix.ControlStatementsCleanUp;
import org.eclipse.jdt.internal.ui.fix.ConvertLoopCleanUp;
import org.eclipse.jdt.internal.ui.fix.DoubleNegationCleanUp;
import org.eclipse.jdt.internal.ui.fix.ElseIfCleanUp;
import org.eclipse.jdt.internal.ui.fix.EmbeddedIfCleanUp;
import org.eclipse.jdt.internal.ui.fix.EvaluateNullableCleanUp;
import org.eclipse.jdt.internal.ui.fix.ExpressionsCleanUp;
import org.eclipse.jdt.internal.ui.fix.HashCleanUp;
import org.eclipse.jdt.internal.ui.fix.ImportsCleanUp;
import org.eclipse.jdt.internal.ui.fix.InvertEqualsCleanUp;
import org.eclipse.jdt.internal.ui.fix.Java50CleanUp;
import org.eclipse.jdt.internal.ui.fix.JoinCleanUp;
import org.eclipse.jdt.internal.ui.fix.LazyLogicalCleanUp;
import org.eclipse.jdt.internal.ui.fix.MapCloningCleanUp;
import org.eclipse.jdt.internal.ui.fix.MergeConditionalBlocksCleanUp;
import org.eclipse.jdt.internal.ui.fix.OneIfRatherThanDuplicateBlocksThatFallThroughCleanUp;
import org.eclipse.jdt.internal.ui.fix.PlainReplacementCleanUp;
import org.eclipse.jdt.internal.ui.fix.PrimitiveComparisonCleanUp;
import org.eclipse.jdt.internal.ui.fix.PrimitiveRatherThanWrapperCleanUp;
import org.eclipse.jdt.internal.ui.fix.PullOutIfFromIfElseCleanUp;
import org.eclipse.jdt.internal.ui.fix.ReduceIndentationCleanUp;
import org.eclipse.jdt.internal.ui.fix.RedundantComparatorCleanUp;
import org.eclipse.jdt.internal.ui.fix.ReturnExpressionCleanUp;
import org.eclipse.jdt.internal.ui.fix.SingleUsedFieldCleanUp;
import org.eclipse.jdt.internal.ui.fix.SortMembersCleanUp;
import org.eclipse.jdt.internal.ui.fix.StandardComparisonCleanUp;
import org.eclipse.jdt.internal.ui.fix.StringCleanUp;
import org.eclipse.jdt.internal.ui.fix.SwitchExpressionsCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnloopedWhileCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnnecessaryCodeCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnusedCodeCleanUp;
import org.eclipse.jdt.internal.ui.fix.ValueOfRatherThanInstantiationCleanUp;
import org.eclipse.jdt.internal.ui.fix.VariableDeclarationCleanUp;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpProfileVersioner;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.Profile;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileStore;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CleanUpPerfTest extends JdtPerformanceTestCaseCommon {

	private static class MyTestSetup extends ExternalResource {
		public static final String SRC_CONTAINER= "src";

		public static IJavaProject fJProject1;

		@Override
		public void before() throws Throwable {
			fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
			Assert.assertNotNull("rt not found", JavaProjectHelper.addRTJar(fJProject1));
			File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);
			JavaProjectHelper.addSourceContainerWithImport(fJProject1, SRC_CONTAINER, junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);
		}

		@Override
		public void after() {
			try {
				if (fJProject1 != null && fJProject1.exists())
					JavaProjectHelper.delete(fJProject1);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	@Rule
	public MyTestSetup stup= new MyTestSetup();

	private void addAllCUs(CleanUpRefactoring cleanUp, IJavaElement[] children) throws JavaModelException {
		for (IJavaElement element : children) {
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

	private static Map<String, String> getNullSettings() {
		Map<String, String> result= new HashMap<>();

		for (String key : JavaPlugin.getDefault().getCleanUpRegistry().getDefaultOptions(CleanUpConstants.DEFAULT_CLEAN_UP_OPTIONS).getKeys()) {
			result.put(key, CleanUpOptions.FALSE);
		}

		return result;
	}

	private static void storeSettings(Map<String, String> node) throws CoreException {
		ProfileManager.CustomProfile profile= new ProfileManager.CustomProfile("testProfile", node, CleanUpProfileVersioner.CURRENT_VERSION, CleanUpProfileVersioner.PROFILE_KIND);
		InstanceScope.INSTANCE.getNode(JavaUI.ID_PLUGIN).put(CleanUpConstants.CLEANUP_PROFILE, profile.getID());

		List<Profile> profiles= CleanUpPreferenceUtil.getBuiltInProfiles();
		profiles.add(profile);

		CleanUpProfileVersioner versioner= new CleanUpProfileVersioner();
		ProfileStore profileStore= new ProfileStore(CleanUpConstants.CLEANUP_PROFILES, versioner);
		profileStore.writeProfiles(profiles, InstanceScope.INSTANCE);
	}

	@Test
	public void testNullCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());
		cleanUpRefactoring.addCleanUp(new AbstractCleanUp() {

			/*
			 * @see org.eclipse.jdt.internal.ui.fix.AbstractCleanUp#getRequirements()
			 */
			@Override
			public CleanUpRequirements getRequirements() {
				return new CleanUpRequirements(true, false, false, null);
			}
		});

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testAllCleanUps() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS, CleanUpOptions.TRUE);

		node.put(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS, CleanUpOptions.TRUE);

		node.put(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS, CleanUpOptions.TRUE);

		node.put(CleanUpConstants.ADD_MISSING_ANNOTATIONS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE, CleanUpOptions.TRUE);

		node.put(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_DEFAULT, CleanUpOptions.TRUE);

		node.put(CleanUpConstants.ADD_MISSING_NLS_TAGS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.REMOVE_UNNECESSARY_CASTS, CleanUpOptions.TRUE);

		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_METHODS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_METHOD_PARAMETERS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES, CleanUpOptions.TRUE);

		node.put(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS, CleanUpOptions.TRUE);

		node.put(CleanUpConstants.FORMAT_SOURCE_CODE, CleanUpOptions.TRUE);

		node.put(CleanUpConstants.ORGANIZE_IMPORTS, CleanUpOptions.TRUE);

		node.put(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_FILE_SEPARATOR, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_LINE_SEPARATOR, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_PATH_SEPARATOR, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_FILE_ENCODING, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_BOXED, CleanUpOptions.TRUE);

		storeSettings(node);

		for (ICleanUp cleanUp : JavaPlugin.getDefault().getCleanUpRegistry().createCleanUps()) {
			cleanUpRefactoring.addCleanUp(cleanUp);
		}

		//See https://bugs.eclipse.org/bugs/show_bug.cgi?id=135219
		//		tagAsSummary("Code Clean Up - 25 clean-ups", Dimension.ELAPSED_PROCESS);

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testSingleUsedFieldCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.SINGLE_USED_FIELD, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new SingleUsedFieldCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testCodeStyleCleanUp() throws Exception {
		tagAsSummary("Clean Up - Code Style", Dimension.ELAPSED_PROCESS);

		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new CodeStyleCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testControlStatementsCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new ControlStatementsCleanUp());
		cleanUpRefactoring.addCleanUp(new ConvertLoopCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testConvertLoopCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new ControlStatementsCleanUp());
		cleanUpRefactoring.addCleanUp(new ConvertLoopCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testExpressionsCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new ExpressionsCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testJava50CleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.ADD_MISSING_ANNOTATIONS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new Java50CleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testUnloopedWhileCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.UNLOOPED_WHILE, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new UnloopedWhileCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testStringCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.ADD_MISSING_NLS_TAGS, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new StringCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testSortMembersCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.SORT_MEMBERS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.SORT_MEMBERS_ALL, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new SortMembersCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testUnnecessaryCodeCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();
		node.put(CleanUpConstants.REMOVE_UNNECESSARY_CASTS, CleanUpOptions.TRUE);
		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new UnnecessaryCodeCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testUnusedCodeCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_METHODS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_METHOD_PARAMETERS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new UnusedCodeCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testVariableDeclarationCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new VariableDeclarationCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testCodeFormatCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.FORMAT_SOURCE_CODE, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new CodeFormatCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testConvertToSwitchCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new SwitchExpressionsCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testOrganizeImports() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.ORGANIZE_IMPORTS, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new ImportsCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testHashCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.MODERNIZE_HASH, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new HashCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testSystemPropertiesCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_FILE_SEPARATOR, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_PATH_SEPARATOR, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_FILE_ENCODING, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_LINE_SEPARATOR, CleanUpOptions.TRUE);
		node.put(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_BOXED, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new ConstantsForSystemPropertyCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testArraysFillCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.ARRAYS_FILL, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new ArraysFillCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testAutoBoxingCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.USE_AUTOBOXING, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new AutoboxingCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testBitwiseConditionalExpressionsCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.CHECK_SIGN_OF_BITWISE_OPERATION, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new BitwiseConditionalExpressionCleanup());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testBooleanLiteralCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.PREFER_BOOLEAN_LITERAL, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new BooleanLiteralCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testBreakLoopCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.BREAK_LOOP, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new BreakLoopCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testPlainReplacementCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.PLAIN_REPLACEMENT, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new PlainReplacementCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testCollectionCloningCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.COLLECTION_CLONING, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new CollectionCloningCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testBooleanValueRatherThanComparisonCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.BOOLEAN_VALUE_RATHER_THAN_COMPARISON, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new BooleanValueRatherThanComparisonCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testDoubleNegationCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.DOUBLE_NEGATION, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new DoubleNegationCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testElseIfCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.ELSE_IF, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new ElseIfCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testReduceIndentationCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.REDUCE_INDENTATION, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new ReduceIndentationCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testEmbeddedIfCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.RAISE_EMBEDDED_IF, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new EmbeddedIfCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testEvaluateNullableCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.EVALUATE_NULLABLE, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new EvaluateNullableCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testOneIfRatherThanDuplicateBlocksThatFallThroughCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.ONE_IF_RATHER_THAN_DUPLICATE_BLOCKS_THAT_FALL_THROUGH, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new OneIfRatherThanDuplicateBlocksThatFallThroughCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testPullOutIfFromIfElseCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.PULL_OUT_IF_FROM_IF_ELSE, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new PullOutIfFromIfElseCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testComparingOnCriteriaCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.COMPARING_ON_CRITERIA, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new ComparingOnCriteriaCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testJoinCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.JOIN, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new JoinCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testLazyLogicalCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.USE_LAZY_LOGICAL_OPERATOR, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new LazyLogicalCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testValueOfRatherThanInstantiationCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.VALUEOF_RATHER_THAN_INSTANTIATION, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new ValueOfRatherThanInstantiationCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testPrimitiveComparisonCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.PRIMITIVE_COMPARISON, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new PrimitiveComparisonCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testPrimitiveRatherThanWrappernCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.PRIMITIVE_RATHER_THAN_WRAPPER, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new PrimitiveRatherThanWrapperCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testRedundantComparatorCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.REDUNDANT_COMPARATOR, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new RedundantComparatorCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testArrayWithCurlyCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.ARRAY_WITH_CURLY, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new ArrayWithCurlyCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testReturnExpressionCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.RETURN_EXPRESSION, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new ReturnExpressionCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testMapCloningCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.MAP_CLONING, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new MapCloningCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testMergeConditionalBlockCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.MERGE_CONDITIONAL_BLOCKS, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new MergeConditionalBlocksCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testInvertEqualsCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.INVERT_EQUALS, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new InvertEqualsCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	@Test
	public void testStandardComparisonCleanUp() throws Exception {
		CleanUpRefactoring cleanUpRefactoring= new CleanUpRefactoring();
		addAllCUs(cleanUpRefactoring, MyTestSetup.fJProject1.getChildren());

		Map<String, String> node= getNullSettings();

		node.put(CleanUpConstants.STANDARD_COMPARISON, CleanUpOptions.TRUE);

		storeSettings(node);

		cleanUpRefactoring.addCleanUp(new StandardComparisonCleanUp());

		doCleanUp(cleanUpRefactoring);
	}

	private void doCleanUp(CleanUpRefactoring refactoring) throws CoreException {
		refactoring.setUseOptionsFromProfile(true);

		performRefactoring(refactoring, false, IStatus.WARNING, true);
		performRefactoring(refactoring, false, IStatus.WARNING, true);

		for (int i= 0; i < 10; i++) {
			performRefactoring(refactoring, true, IStatus.WARNING, true);
		}

		commitMeasurements();
		assertPerformanceInRelativeBand(Dimension.ELAPSED_PROCESS, -100, +10);
	}

	private void performRefactoring(CleanUpRefactoring refactoring, boolean measure, int maxSeverity, boolean checkUndo) throws CoreException {

		// Need to clear the options field as we reuse the clean ups, which is not expected
		clearOptions(refactoring.getCleanUps());

		PerformRefactoringOperation operation= new PerformRefactoringOperation(refactoring, CheckConditionsOperation.ALL_CONDITIONS);
		joinBackgroudActivities();
		// Flush the undo manager to not count any already existing undo objects
		// into the heap consumption
		RefactoringCore.getUndoManager().flush();
		System.gc();
		if (measure)
			startMeasuring();
		ResourcesPlugin.getWorkspace().run(operation, null);
		if (measure)
			stopMeasuring();
		Assert.assertTrue(operation.getConditionStatus().getSeverity() <= maxSeverity);
		Assert.assertTrue(operation.getValidationStatus().isOK());
		if (checkUndo) {
			Assert.assertNotNull(operation.getUndoChange());
		}
		//undo the change, to have same code for each run
		RefactoringCore.getUndoManager().performUndo(null, null);
		RefactoringCore.getUndoManager().flush();
		System.gc();
		joinBackgroudActivities();
	}

	private void clearOptions(ICleanUp[] cleanUps) {
		for (ICleanUp cleanUp : cleanUps) {
			if (cleanUp instanceof AbstractCleanUp) {
				Accessor<AbstractCleanUp> accessor= new Accessor<>(cleanUp, AbstractCleanUp.class);
				accessor.set("fOptions", null);
			}
		}
	}

}
