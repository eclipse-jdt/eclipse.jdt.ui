/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.StringAsserts;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.Java17ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.tests.quickfix.QuickFixTest14;
import org.eclipse.jdt.ui.wizards.NewClassWizardPage;
import org.eclipse.jdt.ui.wizards.NewInterfaceWizardPage;
import org.eclipse.jdt.ui.wizards.NewRecordWizardPage;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

public class NewTypeWizardTest17 {
	private IJavaProject fJProject1, fJProject2;

	private IJavaProject fJProjectM1, fJProjectM2;

	private IPackageFragmentRoot fSourceFolder, fMSourceFolder;

	private IPackageFragment fpack1, fMpack1;

	private ICompilationUnit fSealedSuperCls, fSealedSuperInterface, fMSealedSuperCls, fMSealedSuperInterface;

	private ITypeBinding fSealedClsBinding, fSealedInterfaceBinding, fMSealedClsBinding, fMSealedInterfaceBinding;

	@Rule
	public ProjectTestSetup projectSetup= new Java17ProjectTestSetup(false);

	@Rule
	public ProjectTestSetup projectSetup2= new Java17ProjectTestSetup(Java17ProjectTestSetup.PROJECT_NAME17 + "_2", false);

	@Rule
	public ProjectTestSetup projectMSetup= new Java17ProjectTestSetup(Java17ProjectTestSetup.PROJECT_NAME17 + "_M", false);

	@Rule
	public ProjectTestSetup projectMSetup2= new Java17ProjectTestSetup(Java17ProjectTestSetup.PROJECT_NAME17 + "_M_2", false);

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);
		store.setValue(PreferenceConstants.CODEGEN_USE_OVERRIDE_ANNOTATION, true);

		String newFileTemplate= "${filecomment}\n${package_declaration}\n\n${typecomment}\n${type_declaration}";
		StubUtility.setCodeTemplate(CodeTemplateContextType.NEWTYPE_ID, newFileTemplate, null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, "/**\n * Type\n */", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.FILECOMMENT_ID, "/**\n * File\n */", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORCOMMENT_ID, "/**\n * Constructor\n */", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODCOMMENT_ID, "/**\n * Method\n */", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.OVERRIDECOMMENT_ID, "/**\n * Overridden\n */", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "${body_statement}", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "${body_statement}", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CLASSBODY_ID, "/* class body */\n", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.INTERFACEBODY_ID, "/* interface body */\n", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.ENUMBODY_ID, "/* enum body */\n", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.ANNOTATIONBODY_ID, "/* annotation body */\n", null);

	}

	private void initNonModularProject() throws Exception {
		fJProject1= projectSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		fpack1= fSourceFolder.createPackageFragment("test1", false, null);
		String test= """
			package test;
			
			public sealed class Shape permits Square {
			}
			""";
		fSealedSuperCls= fpack1.createCompilationUnit("Shape.java", test, false, null);
		test= """
			package test;
			
			public non-sealed class Square extends Shape {
			}
			""";
		fpack1.createCompilationUnit("Square.java", test, false, null);
		fSealedClsBinding= getTypeBinding(fSealedSuperCls);
		test= """
			package test;
			
			public sealed interface IShape permits ISquare {
			}
			""";
		fSealedSuperInterface= fpack1.createCompilationUnit("IShape.java", test, false, null);
		test= """
			package test;
			
			public non-sealed interface ISquare extends IShape {
			}
			""";
		fpack1.createCompilationUnit("ISquare.java", test, false, null);
		fSealedInterfaceBinding= getTypeBinding(fSealedSuperInterface);
	}

	private void initModularProject() throws Exception {
		// Init Modular Projects();
		fJProjectM1= projectMSetup.getProject();
		fMSourceFolder= JavaProjectHelper.addSourceContainer(fJProjectM1, "src");
		fMpack1= fMSourceFolder.createPackageFragment("test1", false, null);
		String test= """
			package test;
			
			public sealed class Shape permits Square {
			}
			""";
		fMSealedSuperCls= fMpack1.createCompilationUnit("Shape.java", test, false, null);
		test= """
			package test;
			
			public non-sealed class Square extends Shape {
			}
			""";
		fMpack1.createCompilationUnit("Square.java", test, false, null);
		fMSealedClsBinding= getTypeBinding(fMSealedSuperCls);
		test= """
			package test;
			
			public sealed interface IShape permits ISquare {
			}
			""";
		fMSealedSuperInterface= fMpack1.createCompilationUnit("IShape.java", test, false, null);
		test= """
			package test;
			
			public non-sealed interface ISquare extends IShape {
			}
			""";
		fMpack1.createCompilationUnit("ISquare.java", test, false, null);
		fMSealedInterfaceBinding= getTypeBinding(fMSealedSuperInterface);
	}

	private void initSecondProject() throws Exception {
		fJProject2= projectSetup2.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject2, "src");
		fpack1= fSourceFolder.createPackageFragment("test3", false, null);
		IPath path= fJProject1.getPath();
		IClasspathEntry dep= JavaCore.newContainerEntry(path);
		IClasspathEntry[] old= fJProject2.getRawClasspath();
		IClasspathEntry[] newPath= new IClasspathEntry[old.length + 1];
		System.arraycopy(old, 0, newPath, 0, old.length);
		newPath[old.length]= dep;
		fJProject2.setRawClasspath(newPath, null);
	}

	private void createModuleInfo() throws Exception {
		String test= """
			
			
			module modTest1 {
				exports test1;
			}
			""";
		IPackageFragment def= fMSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit(QuickFixTest14.MODULE_INFO_FILE, test, false, null);
	}

	private void initSecondModularProject() throws Exception {
		createModuleInfo();
		fJProjectM2= projectMSetup2.getProject();
		fMSourceFolder= JavaProjectHelper.addSourceContainer(fJProjectM2, "src");
		fMpack1= fMSourceFolder.createPackageFragment("test3", false, null);
		String test= """
			
			
			module modTest2 {
				requires modTest2;
			}
			""";
		IPackageFragment def= fMSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit(QuickFixTest14.MODULE_INFO_FILE, test, false, null);
		IPath path= fJProjectM1.getPath();
		IClasspathEntry dep= JavaCore.newContainerEntry(path);
		IClasspathEntry[] old= fJProjectM2.getRawClasspath();
		IClasspathEntry[] newPath= new IClasspathEntry[old.length + 1];
		System.arraycopy(old, 0, newPath, 0, old.length);
		newPath[old.length]= dep;
		fJProjectM2.setRawClasspath(newPath, null);
	}

	@After
	public void tearDown() throws Exception {
		if (fJProject1 != null)
			JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
		if (fJProjectM1 != null)
			JavaProjectHelper.clear(fJProjectM1, projectMSetup.getDefaultClasspath());
		if (fJProject2 != null)
			JavaProjectHelper.clear(fJProject2, projectSetup.getDefaultClasspath());
		if (fJProjectM2 != null)
			JavaProjectHelper.clear(fJProjectM2, projectSetup.getDefaultClasspath());
	}


	// ---------------------------------
	// Testing in a non-modular project
	// ---------------------------------

	// Throw error if the sealed super class is in different package to the new class
	@Test
	public void testNonModularCreateClassError1() throws Exception {
		initNonModularProject();
		NewClassWizardPage wizardPage= new NewClassWizardPage();
		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		wizardPage.setPackageFragment(pack2, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("E", true);

		wizardPage.setSuperClass(fSealedClsBinding, true);

		List<ITypeBinding> interfaces= new ArrayList<>();
		wizardPage.setSuperInterfacesList(interfaces, true);

		wizardPage.setMethodStubSelection(false, false, false, true);
		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		IStatus status= wizardPage.getSealedSuperClassStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.ERROR);
		assertTrue(NewWizardMessages.NewTypeWizardPage_error_class_SealedSuperClassInDifferentPackage.equals(status.getMessage()));
	}

	// Throw error if the sealed super interface is in different package to the new class
	@Test
	public void testNonModularCreateClassError2() throws Exception {
		initNonModularProject();
		NewClassWizardPage wizardPage= new NewClassWizardPage();
		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		wizardPage.setPackageFragment(pack2, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("E", true);

		List<ITypeBinding> interfaces= new ArrayList<>();
		interfaces.add(fSealedInterfaceBinding);
		wizardPage.setSuperInterfacesList(interfaces, true);

		wizardPage.setMethodStubSelection(false, false, false, true);
		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		IStatus status= wizardPage.getSealedSuperInterfaceStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.ERROR);
		assertTrue(NewWizardMessages.NewTypeWizardPage_error_class_SealedSuperInterfaceInDifferentPackage.equals(status.getMessage()));
	}

	// Throw error if the sealed super interface is in different package to the new interface
	@Test
	public void testNonModularCreateInterfaceError1() throws Exception {
		initNonModularProject();
		NewInterfaceWizardPage wizardPage= new NewInterfaceWizardPage();
		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		wizardPage.setPackageFragment(pack2, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("IE", true);

		List<ITypeBinding> interfaces= new ArrayList<>();
		interfaces.add(fSealedInterfaceBinding);
		wizardPage.setSuperInterfacesList(interfaces, true);

		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		IStatus status= wizardPage.getSealedSuperInterfaceStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.ERROR);
		assertTrue(NewWizardMessages.NewTypeWizardPage_error_interface_SealedSuperInterfaceInDifferentPackage.equals(status.getMessage()));
	}

	// Successfully Create non-sealed class which has sealed super class
	@Test
	public void testNonModularCreateClassSuccess1() throws Exception {
		initNonModularProject();
		NewClassWizardPage wizardPage= new NewClassWizardPage();
		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		wizardPage.setPackageFragment(fpack1, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("E", true);

		wizardPage.setSuperClass(fSealedClsBinding, true);

		List<ITypeBinding> interfaces= new ArrayList<>();
		wizardPage.setSuperInterfacesList(interfaces, true);

		wizardPage.setMethodStubSelection(false, false, false, true);
		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		IStatus status= wizardPage.getSealedSuperClassStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.OK);

		status= wizardPage.getSealedModifierStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.ERROR);
		assertTrue(NewWizardMessages.NewTypeWizardPage_error_SealedFinalNonSealedClass_extend_superclass_notSelected_message.equals(status.getMessage()));
		int modifiers= wizardPage.getModifiers();
		wizardPage.setModifiers(modifiers | Flags.AccNonSealed, true);
		status= wizardPage.getSealedModifierStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.OK);

		wizardPage.createType(null);

		String actual= wizardPage.getCreatedType().getCompilationUnit().getSource();

		String expected= """
			/**
			 * File
			 */
			package test1;
			
			/**
			 * Type
			 */
			public non-sealed class E extends Shape {
			    /* class body */
			}
			""";

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);

		actual= fSealedSuperCls.getSource();

		expected= """
			package test;
			
			public sealed class Shape permits Square, E {
			}
			""";

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}

	// Successfully Create non-sealed class which has sealed super interface
	@Test
	public void testNonModularCreateClassSuccess2() throws Exception {
		initNonModularProject();
		NewClassWizardPage wizardPage= new NewClassWizardPage();
		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		wizardPage.setPackageFragment(fpack1, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("E", true);

		List<ITypeBinding> interfaces= new ArrayList<>();
		interfaces.add(fSealedInterfaceBinding);
		wizardPage.setSuperInterfacesList(interfaces, true);

		wizardPage.setMethodStubSelection(false, false, false, true);
		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		IStatus status= wizardPage.getSealedSuperInterfaceStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.OK);

		status= wizardPage.getSealedModifierStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.ERROR);
		assertTrue(NewWizardMessages.NewTypeWizardPage_error_SealedFinalNonSealedClass_implement_superinterface_notSelected_message.equals(status.getMessage()));
		int modifiers= wizardPage.getModifiers();
		wizardPage.setModifiers(modifiers | Flags.AccNonSealed, true);
		status= wizardPage.getSealedModifierStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.OK);

		wizardPage.createType(null);

		String actual= wizardPage.getCreatedType().getCompilationUnit().getSource();

		String expected= """
			/**
			 * File
			 */
			package test1;
			
			/**
			 * Type
			 */
			public non-sealed class E implements IShape {
			    /* class body */
			}
			""";

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);

		actual= fSealedSuperInterface.getSource();

		expected= """
			package test;
			
			public sealed interface IShape permits ISquare, E {
			}
			""";

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}

	// Successfully Create final class which has sealed super class
	@Test
	public void testNonModularCreateClassSuccess3() throws Exception {
		initNonModularProject();
		NewClassWizardPage wizardPage= new NewClassWizardPage();
		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		wizardPage.setPackageFragment(fpack1, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("E", true);

		wizardPage.setSuperClass(fSealedClsBinding, true);

		List<ITypeBinding> interfaces= new ArrayList<>();
		wizardPage.setSuperInterfacesList(interfaces, true);

		wizardPage.setMethodStubSelection(false, false, false, true);
		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		IStatus status= wizardPage.getSealedSuperClassStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.OK);

		status= wizardPage.getSealedModifierStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.ERROR);
		assertTrue(NewWizardMessages.NewTypeWizardPage_error_SealedFinalNonSealedClass_extend_superclass_notSelected_message.equals(status.getMessage()));
		int modifiers= wizardPage.getModifiers();
		wizardPage.setModifiers(modifiers | Flags.AccFinal, true);
		status= wizardPage.getSealedModifierStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.OK);

		wizardPage.createType(null);

		String actual= wizardPage.getCreatedType().getCompilationUnit().getSource();

		String expected= """
			/**
			 * File
			 */
			package test1;
			
			/**
			 * Type
			 */
			public final class E extends Shape {
			    /* class body */
			}
			""";

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);

		actual= fSealedSuperCls.getSource();

		expected= """
			package test;
			
			public sealed class Shape permits Square, E {
			}
			""";

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}

	// Successfully Create final class which has sealed super interface
	@Test
	public void testNonModularCreateClassSuccess4() throws Exception {
		initNonModularProject();
		NewClassWizardPage wizardPage= new NewClassWizardPage();
		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		wizardPage.setPackageFragment(fpack1, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("E", true);

		List<ITypeBinding> interfaces= new ArrayList<>();
		interfaces.add(fSealedInterfaceBinding);
		wizardPage.setSuperInterfacesList(interfaces, true);

		wizardPage.setMethodStubSelection(false, false, false, true);
		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		IStatus status= wizardPage.getSealedSuperInterfaceStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.OK);

		status= wizardPage.getSealedModifierStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.ERROR);
		assertTrue(NewWizardMessages.NewTypeWizardPage_error_SealedFinalNonSealedClass_implement_superinterface_notSelected_message.equals(status.getMessage()));
		int modifiers= wizardPage.getModifiers();
		wizardPage.setModifiers(modifiers | Flags.AccFinal, true);
		status= wizardPage.getSealedModifierStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.OK);

		wizardPage.createType(null);

		String actual= wizardPage.getCreatedType().getCompilationUnit().getSource();

		String expected= """
			/**
			 * File
			 */
			package test1;
			
			/**
			 * Type
			 */
			public final class E implements IShape {
			    /* class body */
			}
			""";

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);

		actual= fSealedSuperInterface.getSource();

		expected= """
			package test;
			
			public sealed interface IShape permits ISquare, E {
			}
			""";

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}

	// Successfully Create non-sealed interface which has sealed super interface
	@Test
	public void testNonModularCreateInterfaceSuccess1() throws Exception {
		initNonModularProject();
		NewInterfaceWizardPage wizardPage= new NewInterfaceWizardPage();
		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		wizardPage.setPackageFragment(fpack1, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("IE", true);

		List<ITypeBinding> interfaces= new ArrayList<>();
		interfaces.add(fSealedInterfaceBinding);
		wizardPage.setSuperInterfacesList(interfaces, true);

		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		IStatus status= wizardPage.getSealedSuperInterfaceStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.OK);

		status= wizardPage.getSealedModifierStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.ERROR);
		assertTrue(NewWizardMessages.NewTypeWizardPage_error_SealedFinalNonSealedInterface_extend_superinterface_notSelected_message.equals(status.getMessage()));
		int modifiers= wizardPage.getModifiers();
		wizardPage.setModifiers(modifiers | Flags.AccNonSealed, true);
		status= wizardPage.getSealedModifierStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.OK);

		wizardPage.createType(null);

		String actual= wizardPage.getCreatedType().getCompilationUnit().getSource();

		String expected= """
			/**
			 * File
			 */
			package test1;
			
			/**
			 * Type
			 */
			public non-sealed interface IE extends IShape {
			    /* interface body */
			}
			""";

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);

		actual= fSealedSuperInterface.getSource();

		expected= """
			package test;
			
			public sealed interface IShape permits ISquare, IE {
			}
			""";

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}

	// -----------------------------------------
	// Testing in dependent non-modular projects
	// -----------------------------------------

	// Throw error if the sealed super class is in different project to the new class
	@Test
	public void testNonModularDependantCreateClassError1() throws Exception {
		initNonModularProject();
		initSecondProject();
		NewClassWizardPage wizardPage= new NewClassWizardPage();
		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		wizardPage.setPackageFragment(pack2, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("E", true);

		wizardPage.setSuperClass(fSealedClsBinding, true);

		List<ITypeBinding> interfaces= new ArrayList<>();
		wizardPage.setSuperInterfacesList(interfaces, true);

		wizardPage.setMethodStubSelection(false, false, false, true);
		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		IStatus status= wizardPage.getSealedSuperClassStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.ERROR);
		assertTrue(NewWizardMessages.NewTypeWizardPage_error_class_SealedSuperClassInDifferentProject.equals(status.getMessage()));
	}

	// Throw error if the sealed super interface is in different project to the new class
	@Test
	public void testNonModularDependantCreateClassError2() throws Exception {
		initNonModularProject();
		initSecondProject();
		NewClassWizardPage wizardPage= new NewClassWizardPage();
		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		wizardPage.setPackageFragment(pack2, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("E", true);

		List<ITypeBinding> interfaces= new ArrayList<>();
		interfaces.add(fSealedInterfaceBinding);
		wizardPage.setSuperInterfacesList(interfaces, true);

		wizardPage.setMethodStubSelection(false, false, false, true);
		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		IStatus status= wizardPage.getSealedSuperInterfaceStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.ERROR);
		assertTrue(NewWizardMessages.NewTypeWizardPage_error_class_SealedSuperInterfaceInDifferentProject.equals(status.getMessage()));
	}

	// Throw error if the sealed super interface is in different project to the new interface
	@Test
	public void testNonModularDependantCreateInterfaceError1() throws Exception {
		initNonModularProject();
		initSecondProject();
		NewInterfaceWizardPage wizardPage= new NewInterfaceWizardPage();
		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		wizardPage.setPackageFragment(pack2, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("IE", true);

		List<ITypeBinding> interfaces= new ArrayList<>();
		interfaces.add(fSealedInterfaceBinding);
		wizardPage.setSuperInterfacesList(interfaces, true);

		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		IStatus status= wizardPage.getSealedSuperInterfaceStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.ERROR);
		assertTrue(NewWizardMessages.NewTypeWizardPage_error_interface_SealedSuperInterfaceInDifferentProject.equals(status.getMessage()));
	}


	// -----------------------------
	// Testing in a modular projects
	// -----------------------------

	// Successfully Create non-sealed class which has sealed super class in a
	// different package in a modular project
	@Test
	public void testModularCreateClassSuccess1() throws Exception {
		initModularProject();
		createModuleInfo();
		NewClassWizardPage wizardPage= new NewClassWizardPage();
		wizardPage.setPackageFragmentRoot(fMSourceFolder, true);
		IPackageFragment pack2= fMSourceFolder.createPackageFragment("test2", false, null);
		wizardPage.setPackageFragment(pack2, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("E", true);

		wizardPage.setSuperClass(fMSealedClsBinding, true);

		List<ITypeBinding> interfaces= new ArrayList<>();
		wizardPage.setSuperInterfacesList(interfaces, true);

		wizardPage.setMethodStubSelection(false, false, false, true);
		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		IStatus status= wizardPage.getSealedSuperClassStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.OK);

		status= wizardPage.getSealedModifierStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.ERROR);
		assertTrue(NewWizardMessages.NewTypeWizardPage_error_SealedFinalNonSealedClass_extend_superclass_notSelected_message.equals(status.getMessage()));
		int modifiers= wizardPage.getModifiers();
		wizardPage.setModifiers(modifiers | Flags.AccNonSealed, true);
		status= wizardPage.getSealedModifierStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.OK);

		wizardPage.createType(null);

		String actual= wizardPage.getCreatedType().getCompilationUnit().getSource();

		String expected= """
			/**
			 * File
			 */
			package test2;
			
			import test1.Shape;
			
			/**
			 * Type
			 */
			public non-sealed class E extends Shape {
			    /* class body */
			}
			""";

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);

		actual= fMSealedSuperCls.getSource();

		expected= """
			package test;
			
			import test2.E;
			
			public sealed class Shape permits Square, E {
			}
			""";

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}

	// Successfully Create non-sealed class which has sealed super interface in a
	// different package in a modular project
	@Test
	public void testModularCreateClassSuccess2() throws Exception {
		initModularProject();
		createModuleInfo();
		NewClassWizardPage wizardPage= new NewClassWizardPage();
		wizardPage.setPackageFragmentRoot(fMSourceFolder, true);
		IPackageFragment pack2= fMSourceFolder.createPackageFragment("test2", false, null);
		wizardPage.setPackageFragment(pack2, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("E", true);

		List<ITypeBinding> interfaces= new ArrayList<>();
		interfaces.add(fMSealedInterfaceBinding);
		wizardPage.setSuperInterfacesList(interfaces, true);

		wizardPage.setMethodStubSelection(false, false, false, true);
		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		IStatus status= wizardPage.getSealedSuperInterfaceStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.OK);

		status= wizardPage.getSealedModifierStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.ERROR);
		assertTrue(NewWizardMessages.NewTypeWizardPage_error_SealedFinalNonSealedClass_implement_superinterface_notSelected_message.equals(status.getMessage()));
		int modifiers= wizardPage.getModifiers();
		wizardPage.setModifiers(modifiers | Flags.AccNonSealed, true);
		status= wizardPage.getSealedModifierStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.OK);

		wizardPage.createType(null);

		String actual= wizardPage.getCreatedType().getCompilationUnit().getSource();

		String expected= """
			/**
			 * File
			 */
			package test2;
			
			import test1.IShape;
			
			/**
			 * Type
			 */
			public non-sealed class E implements IShape {
			    /* class body */
			}
			""";

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);

		actual= fMSealedSuperInterface.getSource();

		expected= """
			package test;
			
			import test2.E;
			
			public sealed interface IShape permits ISquare, E {
			}
			""";

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}

	// Successfully Create final class which has sealed super class in a
	// different package in a modular project
	@Test
	public void testModularCreateClassSuccess3() throws Exception {
		initModularProject();
		createModuleInfo();
		NewClassWizardPage wizardPage= new NewClassWizardPage();
		wizardPage.setPackageFragmentRoot(fMSourceFolder, true);
		IPackageFragment pack2= fMSourceFolder.createPackageFragment("test2", false, null);
		wizardPage.setPackageFragment(pack2, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("E", true);

		wizardPage.setSuperClass(fMSealedClsBinding, true);

		List<ITypeBinding> interfaces= new ArrayList<>();
		wizardPage.setSuperInterfacesList(interfaces, true);

		wizardPage.setMethodStubSelection(false, false, false, true);
		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		IStatus status= wizardPage.getSealedSuperClassStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.OK);

		status= wizardPage.getSealedModifierStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.ERROR);
		assertTrue(NewWizardMessages.NewTypeWizardPage_error_SealedFinalNonSealedClass_extend_superclass_notSelected_message.equals(status.getMessage()));
		int modifiers= wizardPage.getModifiers();
		wizardPage.setModifiers(modifiers | Flags.AccFinal, true);
		status= wizardPage.getSealedModifierStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.OK);

		wizardPage.createType(null);

		String actual= wizardPage.getCreatedType().getCompilationUnit().getSource();

		String expected= """
			/**
			 * File
			 */
			package test2;
			
			import test1.Shape;
			
			/**
			 * Type
			 */
			public final class E extends Shape {
			    /* class body */
			}
			""";

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);

		actual= fMSealedSuperCls.getSource();

		expected= """
			package test;
			
			import test2.E;
			
			public sealed class Shape permits Square, E {
			}
			""";

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}

	// Successfully Create final class which has sealed super interface in a
	// different package in a modular project
	@Test
	public void testModularCreateClassSuccess4() throws Exception {
		initModularProject();
		createModuleInfo();
		NewClassWizardPage wizardPage= new NewClassWizardPage();
		wizardPage.setPackageFragmentRoot(fMSourceFolder, true);
		IPackageFragment pack2= fMSourceFolder.createPackageFragment("test2", false, null);
		wizardPage.setPackageFragment(pack2, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("E", true);

		List<ITypeBinding> interfaces= new ArrayList<>();
		interfaces.add(fMSealedInterfaceBinding);
		wizardPage.setSuperInterfacesList(interfaces, true);

		wizardPage.setMethodStubSelection(false, false, false, true);
		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		IStatus status= wizardPage.getSealedSuperInterfaceStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.OK);

		status= wizardPage.getSealedModifierStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.ERROR);
		assertTrue(NewWizardMessages.NewTypeWizardPage_error_SealedFinalNonSealedClass_implement_superinterface_notSelected_message.equals(status.getMessage()));
		int modifiers= wizardPage.getModifiers();
		wizardPage.setModifiers(modifiers | Flags.AccFinal, true);
		status= wizardPage.getSealedModifierStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.OK);

		wizardPage.createType(null);

		String actual= wizardPage.getCreatedType().getCompilationUnit().getSource();

		String expected= """
			/**
			 * File
			 */
			package test2;
			
			import test1.IShape;
			
			/**
			 * Type
			 */
			public final class E implements IShape {
			    /* class body */
			}
			""";

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);

		actual= fMSealedSuperInterface.getSource();

		expected= """
			package test;
			
			import test2.E;
			
			public sealed interface IShape permits ISquare, E {
			}
			""";

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}

	// Successfully Create non-sealed interface which has sealed super interface in a
	// different package in a modular project
	@Test
	public void testModularCreateInterfaceSuccess1() throws Exception {
		initModularProject();
		createModuleInfo();
		NewInterfaceWizardPage wizardPage= new NewInterfaceWizardPage();
		wizardPage.setPackageFragmentRoot(fMSourceFolder, true);
		IPackageFragment pack2= fMSourceFolder.createPackageFragment("test2", false, null);
		wizardPage.setPackageFragment(pack2, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("IE", true);

		List<ITypeBinding> interfaces= new ArrayList<>();
		interfaces.add(fMSealedInterfaceBinding);
		wizardPage.setSuperInterfacesList(interfaces, true);

		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		IStatus status= wizardPage.getSealedSuperInterfaceStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.OK);

		status= wizardPage.getSealedModifierStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.ERROR);
		assertTrue(NewWizardMessages.NewTypeWizardPage_error_SealedFinalNonSealedInterface_extend_superinterface_notSelected_message.equals(status.getMessage()));
		int modifiers= wizardPage.getModifiers();
		wizardPage.setModifiers(modifiers | Flags.AccNonSealed, true);
		status= wizardPage.getSealedModifierStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.OK);

		wizardPage.createType(null);

		String actual= wizardPage.getCreatedType().getCompilationUnit().getSource();

		String expected= """
			/**
			 * File
			 */
			package test2;
			
			import test1.IShape;
			
			/**
			 * Type
			 */
			public non-sealed interface IE extends IShape {
			    /* interface body */
			}
			""";

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);

		actual= fMSealedSuperInterface.getSource();

		expected= """
			package test;
			
			import test2.IE;
			
			public sealed interface IShape permits ISquare, IE {
			}
			""";

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}

	// -------------------------------------
	// Testing in dependent modular projects
	// -------------------------------------

	// Throw error if the sealed super class is in different module to the new class
	@Test
	public void testModularDependentCreateClassError1() throws Exception {
		initModularProject();
		initSecondModularProject();
		NewClassWizardPage wizardPage= new NewClassWizardPage();
		wizardPage.setPackageFragmentRoot(fMSourceFolder, true);
		IPackageFragment pack2= fMSourceFolder.createPackageFragment("test2", false, null);
		wizardPage.setPackageFragment(pack2, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("E", true);

		wizardPage.setSuperClass(fMSealedClsBinding, true);

		List<ITypeBinding> interfaces= new ArrayList<>();
		wizardPage.setSuperInterfacesList(interfaces, true);

		wizardPage.setMethodStubSelection(false, false, false, true);
		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		IStatus status= wizardPage.getSealedSuperClassStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.ERROR);
		assertTrue(NewWizardMessages.NewTypeWizardPage_error_class_SealedSuperClassInDifferentModule.equals(status.getMessage()));
	}

	// Throw error if the sealed super interface is in different module to the new class
	@Test
	public void testModularDependentCreateClassError2() throws Exception {
		initModularProject();
		initSecondModularProject();
		NewClassWizardPage wizardPage= new NewClassWizardPage();
		wizardPage.setPackageFragmentRoot(fMSourceFolder, true);
		IPackageFragment pack2= fMSourceFolder.createPackageFragment("test2", false, null);
		wizardPage.setPackageFragment(pack2, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("E", true);

		List<ITypeBinding> interfaces= new ArrayList<>();
		interfaces.add(fMSealedInterfaceBinding);
		wizardPage.setSuperInterfacesList(interfaces, true);

		wizardPage.setMethodStubSelection(false, false, false, true);
		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		IStatus status= wizardPage.getSealedSuperInterfaceStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.ERROR);
		assertTrue(NewWizardMessages.NewTypeWizardPage_error_class_SealedSuperInterfaceInDifferentModule.equals(status.getMessage()));
	}

	// Throw error if the sealed super interface is in different module to the new interface
	@Test
	public void testModularDependentCreateInterfaceError1() throws Exception {
		initModularProject();
		initSecondModularProject();
		NewInterfaceWizardPage wizardPage= new NewInterfaceWizardPage();
		wizardPage.setPackageFragmentRoot(fMSourceFolder, true);
		IPackageFragment pack2= fMSourceFolder.createPackageFragment("test2", false, null);
		wizardPage.setPackageFragment(pack2, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("IE", true);

		List<ITypeBinding> interfaces= new ArrayList<>();
		interfaces.add(fMSealedInterfaceBinding);
		wizardPage.setSuperInterfacesList(interfaces, true);

		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		IStatus status= wizardPage.getSealedSuperInterfaceStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.ERROR);
		assertTrue(NewWizardMessages.NewTypeWizardPage_error_interface_SealedSuperInterfaceInDifferentModule.equals(status.getMessage()));
	}

	@Test
	public void testAddRecordSuperClassError1() throws Exception {
		fJProject1= projectSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		fpack1= fSourceFolder.createPackageFragment("test1", false, null);
		String test= """
			package test;
			
			public record Rec1(int x){
			}
			""";
		ICompilationUnit superClsUnit= fpack1.createCompilationUnit("Rec1.java", test, false, null);
		ITypeBinding superCls= getTypeBinding(superClsUnit);

		NewClassWizardPage wizardPage= new NewClassWizardPage();
		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		wizardPage.setPackageFragment(fpack1, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("E", true);

		wizardPage.setSuperClass(superCls, true);

		List<ITypeBinding> interfaces= new ArrayList<>();
		wizardPage.setSuperInterfacesList(interfaces, true);

		wizardPage.setMethodStubSelection(false, false, false, true);
		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		String sclassName= wizardPage.getSuperClass();
		String expected= Messages.format(NewWizardMessages.NewTypeWizardPage_error_InvalidSuperClassRecord, BasicElementLabels.getJavaElementName(sclassName));

		IStatus status= wizardPage.getSuperClassStatus();
		assertNotNull(status);
		assertTrue(status.getSeverity() == IStatus.ERROR);
		assertTrue(expected.equals(status.getMessage()));
	}

	@Test
	public void testCreateRecordWithAbstractMethodStubs() throws Exception {
		fJProject1= projectSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		fpack1= fSourceFolder.createPackageFragment("test1", false, null);

		NewRecordWizardPage wizardPage= new NewRecordWizardPage();
		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		wizardPage.setPackageFragment(fpack1, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("Rec1", true);

		wizardPage.setMethodStubSelection(false, true, true);

		List<ITypeBinding> interfaces= new ArrayList<>();
		wizardPage.setSuperInterfacesList(interfaces, true);

		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		wizardPage.createType(null);

		String actual= wizardPage.getCreatedType().getCompilationUnit().getSource();

		String expected= """
			/**
			 * File
			 */
			package test1;
			
			/**
			 * Type
			 */
			public record Rec1() {
			
			    /**
			     * Overridden
			     */
			    @Override
			    public boolean equals(Object arg0) {
			        return false;
			    }
			
			    /**
			     * Overridden
			     */
			    @Override
			    public int hashCode() {
			        return 0;
			    }
			
			    /**
			     * Overridden
			     */
			    @Override
			    public String toString() {
			        return null;
			    }
			
			}""" ;

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}

	@Test
	public void testCreateRecordWithOutAbstractMethodStubsAndMain() throws Exception {
		fJProject1= projectSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		fpack1= fSourceFolder.createPackageFragment("test1", false, null);

		NewRecordWizardPage wizardPage= new NewRecordWizardPage();
		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		wizardPage.setPackageFragment(fpack1, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("Rec1", true);

		wizardPage.setMethodStubSelection(false, false, true);

		List<ITypeBinding> interfaces= new ArrayList<>();
		wizardPage.setSuperInterfacesList(interfaces, true);

		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		wizardPage.createType(null);

		String actual= wizardPage.getCreatedType().getCompilationUnit().getSource();

		String expected= """
			/**
			 * File
			 */
			package test1;
			
			/**
			 * Type
			 */
			public record Rec1() {
			
			}""" ;

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}

	@Test
	public void testCreateRecordWithMain() throws Exception {
		fJProject1= projectSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		fpack1= fSourceFolder.createPackageFragment("test1", false, null);

		NewRecordWizardPage wizardPage= new NewRecordWizardPage();
		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		wizardPage.setPackageFragment(fpack1, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("Rec1", true);

		wizardPage.setMethodStubSelection(true, false, true);

		List<ITypeBinding> interfaces= new ArrayList<>();
		wizardPage.setSuperInterfacesList(interfaces, true);

		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		wizardPage.createType(null);

		String actual= wizardPage.getCreatedType().getCompilationUnit().getSource();

		String expected= """
			/**
			 * File
			 */
			package test1;
			
			/**
			 * Type
			 */
			public record Rec1() {
			
			    /**
			     * Method
			     */
			    public static void main(String[] args) {
			
			    }
			
			}""" ;

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}

	@Test
	public void testCreateRecordWithAbstractMethodStubsAndMain() throws Exception {
		fJProject1= projectSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		fpack1= fSourceFolder.createPackageFragment("test1", false, null);

		NewRecordWizardPage wizardPage= new NewRecordWizardPage();
		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		wizardPage.setPackageFragment(fpack1, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("Rec1", true);

		wizardPage.setMethodStubSelection(true, true, true);

		List<ITypeBinding> interfaces= new ArrayList<>();
		wizardPage.setSuperInterfacesList(interfaces, true);

		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		wizardPage.createType(null);

		String actual= wizardPage.getCreatedType().getCompilationUnit().getSource();

		String expected= """
			/**
			 * File
			 */
			package test1;
			
			/**
			 * Type
			 */
			public record Rec1() {
			
			    /**
			     * Overridden
			     */
			    @Override
			    public boolean equals(Object arg0) {
			        return false;
			    }
			
			    /**
			     * Overridden
			     */
			    @Override
			    public int hashCode() {
			        return 0;
			    }
			
			    /**
			     * Overridden
			     */
			    @Override
			    public String toString() {
			        return null;
			    }
			
			    /**
			     * Method
			     */
			    public static void main(String[] args) {
			
			    }
			
			}""" ;

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}

	protected static CompilationUnit getASTRoot(ICompilationUnit cu) {
		return ASTResolving.createQuickFixAST(cu, null);
	}

	private static ITypeBinding getTypeBinding(ICompilationUnit cu) {
		CompilationUnit compUnit= ASTResolving.createQuickFixAST(cu, null);
		ITypeBinding tBinding= null;
		if (compUnit != null) {
			Object typeDecl= compUnit.types().get(0);
			if (typeDecl instanceof AbstractTypeDeclaration) {
				tBinding= ((AbstractTypeDeclaration) typeDecl).resolveBinding();
			}
		}
		return tBinding;
	}
}
