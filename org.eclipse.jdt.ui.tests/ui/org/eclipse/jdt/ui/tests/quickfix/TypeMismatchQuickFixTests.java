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
 *     Benjamin Muskalla <bmuskalla@eclipsesource.com> - [quick fix] proposes wrong cast from Object to primitive int - https://bugs.eclipse.org/bugs/show_bug.cgi?id=100593
 *     Benjamin Muskalla <bmuskalla@eclipsesource.com> - [quick fix] "Add exceptions to..." quickfix does nothing - https://bugs.eclipse.org/bugs/show_bug.cgi?id=107924
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.text.tests.Accessor;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.core.manipulation.CUCorrectionProposalCore;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.proposals.TypeChangeCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.viewsupport.BindingLabelProvider;

public class TypeMismatchQuickFixTests extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup = new ProjectTestSetup();

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, String.valueOf(99));
		options.put(JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.IGNORE);

		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		StubUtility.setCodeTemplate(CodeTemplateContextType.CATCHBLOCK_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "", null);

		fJProject1= projectSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1,projectSetup.getDefaultClasspath());
	}

	@Test
	public void testTypeMismatchInVarDecl() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(Object o) {
			        Thread th= o;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo(Object o) {
			        Thread th= (Thread) o;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public void foo(Object o) {
			        Object th= o;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E {
			    public void foo(Thread o) {
			        Thread th= o;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testTypeMismatchInVarDecl2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.List;
			public class Container {
			    public List[] getLists() {
			        return null;
			    }
			}
			""";
		pack1.createCompilationUnit("Container.java", str, false, null);

		String str1= """
			package test1;
			import java.util.ArrayList;
			public class E {
			    public void foo(Container c) {
			         ArrayList[] lists= c.getLists();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.ArrayList;
			public class E {
			    public void foo(Container c) {
			         ArrayList[] lists= (ArrayList[]) c.getLists();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.ArrayList;
			import java.util.List;
			public class E {
			    public void foo(Container c) {
			         List[] lists= c.getLists();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			import java.util.ArrayList;
			import java.util.List;
			public class Container {
			    public ArrayList[] getLists() {
			        return null;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });

	}

	@Test
	public void testTypeMismatchInVarDecl3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        Thread th= foo();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public Thread foo() {
			        Thread th= foo();
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	@Test
	public void testTypeMismatchInVarDecl4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.List;
			public class Container {
			    public List getLists()[] {
			        return null;
			    }
			}
			""";
		pack1.createCompilationUnit("Container.java", str, false, null);

		String str1= """
			package test1;
			import java.util.ArrayList;
			public class E extends Container {
			    public void foo() {
			         ArrayList[] lists= super.getLists();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.ArrayList;
			public class E extends Container {
			    public void foo() {
			         ArrayList[] lists= (ArrayList[]) super.getLists();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.ArrayList;
			import java.util.List;
			public class E extends Container {
			    public void foo() {
			         List[] lists= super.getLists();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			import java.util.ArrayList;
			import java.util.List;
			public class Container {
			    public ArrayList[] getLists() {
			        return null;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });

	}


	@Test
	public void testTypeMismatchForInterface1() throws Exception {

		IPackageFragment pack0= fSourceFolder.createPackageFragment("test0", false, null);
		String str= """
			package test0;
			public interface PrimaryContainer {
			}
			""";
		pack0.createCompilationUnit("PrimaryContainer.java", str, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class Container {
			    public static Container getContainer() {
			        return null;
			    }
			}
			""";
		pack1.createCompilationUnit("Container.java", str1, false, null);

		String str2= """
			package test1;
			import test0.PrimaryContainer;
			public class E {
			    public void foo() {
			         PrimaryContainer list= Container.getContainer();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str2, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import test0.PrimaryContainer;
			public class E {
			    public void foo() {
			         Container list= Container.getContainer();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import test0.PrimaryContainer;
			public class E {
			    public void foo() {
			         PrimaryContainer list= (PrimaryContainer) Container.getContainer();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			
			import test0.PrimaryContainer;
			
			public class Container {
			    public static PrimaryContainer getContainer() {
			        return null;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		String expected4= """
			package test1;
			
			import test0.PrimaryContainer;
			
			public class Container implements PrimaryContainer {
			    public static Container getContainer() {
			        return null;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });

	}

	@Test
	public void testTypeMismatchForInterface2() throws Exception {
		IPackageFragment pack0= fSourceFolder.createPackageFragment("test0", false, null);
		String str= """
			package test0;
			public interface PrimaryContainer {
			    PrimaryContainer duplicate(PrimaryContainer container);
			}
			""";
		pack0.createCompilationUnit("PrimaryContainer.java", str, false, null);


		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class Container {
			    public static Container getContainer() {
			        return null;
			    }
			}
			""";
		pack1.createCompilationUnit("Container.java", str1, false, null);

		String str2= """
			package test1;
			import test0.PrimaryContainer;
			public class E {
			    public void foo(PrimaryContainer primary) {
			         primary.duplicate(Container.getContainer());
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str2, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import test0.PrimaryContainer;
			public class E {
			    public void foo(PrimaryContainer primary) {
			         primary.duplicate((PrimaryContainer) Container.getContainer());
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			
			import test0.PrimaryContainer;
			
			public class Container {
			    public static PrimaryContainer getContainer() {
			        return null;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			
			import test0.PrimaryContainer;
			
			public class Container implements PrimaryContainer {
			    public static Container getContainer() {
			        return null;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		String expected4= """
			package test0;
			
			import test1.Container;
			
			public interface PrimaryContainer {
			    PrimaryContainer duplicate(Container container);
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(4);
		String preview5= getPreviewContent(proposal);

		String expected5= """
			package test0;
			
			import test1.Container;
			
			public interface PrimaryContainer {
			    PrimaryContainer duplicate(PrimaryContainer container);
			
			    void duplicate(Container container);
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4, preview5 }, new String[] { expected1, expected2, expected3, expected4, expected5 });
	}

	@Test
	public void testTypeMismatchForInterfaceInGeneric() throws Exception {

		IPackageFragment pack0= fSourceFolder.createPackageFragment("test0", false, null);
		String str= """
			package test0;
			public interface PrimaryContainer<A> {
			}
			""";
		pack0.createCompilationUnit("PrimaryContainer.java", str, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class Container<A> {
			    public Container<A> getContainer() {
			        return null;
			    }
			}
			""";
		pack1.createCompilationUnit("Container.java", str1, false, null);

		String str2= """
			package test1;
			import test0.PrimaryContainer;
			public class E {
			    public void foo(Container<String> c) {
			         PrimaryContainer<String> list= c.getContainer();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str2, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import test0.PrimaryContainer;
			public class E {
			    public void foo(Container<String> c) {
			         Container<String> list= c.getContainer();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import test0.PrimaryContainer;
			public class E {
			    public void foo(Container<String> c) {
			         PrimaryContainer<String> list= (PrimaryContainer<String>) c.getContainer();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			
			import test0.PrimaryContainer;
			
			public class Container<A> {
			    public PrimaryContainer<String> getContainer() {
			        return null;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		String expected4= """
			package test1;
			
			import test0.PrimaryContainer;
			
			public class Container<A> implements PrimaryContainer<String> {
			    public Container<A> getContainer() {
			        return null;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });

	}

	@Test
	public void testTypeMismatchForInterfaceInGeneric2() throws Exception {

		IPackageFragment pack0= fSourceFolder.createPackageFragment("test0", false, null);
		String str= """
			package test0;
			public interface PrimaryContainer<A> {
			}
			""";
		pack0.createCompilationUnit("PrimaryContainer.java", str, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class Container<A> {
			    public Container<A> getContainer() {
			        return null;
			    }
			}
			""";
		pack1.createCompilationUnit("Container.java", str1, false, null);

		String str2= """
			package test1;
			import java.util.List;
			import test0.PrimaryContainer;
			public class E {
			    public void foo(Container<List<?>> c) {
			         PrimaryContainer<?> list= c.getContainer();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str2, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.List;
			import test0.PrimaryContainer;
			public class E {
			    public void foo(Container<List<?>> c) {
			         Container<List<?>> list= c.getContainer();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.List;
			import test0.PrimaryContainer;
			public class E {
			    public void foo(Container<List<?>> c) {
			         PrimaryContainer<?> list= (PrimaryContainer<?>) c.getContainer();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			
			import test0.PrimaryContainer;
			
			public class Container<A> {
			    public PrimaryContainer<?> getContainer() {
			        return null;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });

	}

	@Test
	public void testTypeMismatchForParameterizedType() throws Exception {
		Map<String, String> options= fJProject1.getOptions(false);
		try {
			Map<String, String> tempOptions= new HashMap<>(options);
			tempOptions.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.WARNING);
			tempOptions.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);
			fJProject1.setOptions(tempOptions);

			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String str= """
				package test1;
				import java.util.*;
				public class E {
				    public void foo() {
				        List list= new ArrayList<Integer>();
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

			CompilationUnit astRoot= getASTRoot(cu);
			ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
			assertCorrectLabels(proposals);
			assertNumberOfProposals(proposals, 6);

			String[] expected= new String[2];

			expected[0]= """
				package test1;
				import java.util.*;
				public class E {
				    public void foo() {
				        List<Integer> list= new ArrayList<Integer>();
				    }
				}
				""";

			expected[0]= """
				package test1;
				import java.util.*;
				public class E {
				    public void foo() {
				        ArrayList<Integer> list= new ArrayList<Integer>();
				    }
				}
				""";

			assertExpectedExistInProposals(proposals, expected);



		} finally {
			fJProject1.setOptions(options);
		}
	}

	@Test
	public void testTypeMismatchForParameterizedType2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.*;
			public class E {
			    public void foo() {
			        List<Integer> list= new ArrayList<Number>();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.*;
			public class E {
			    public void foo() {
			        List<Number> list= new ArrayList<Number>();
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });


		Accessor accessor= new Accessor(proposal, CUCorrectionProposal.class);
		CUCorrectionProposalCore proposalCore = (CUCorrectionProposalCore) accessor.get("fProposalCore");

		Accessor accessor2= new Accessor(proposalCore, TypeChangeCorrectionProposalCore.class);
		ITypeBinding[] typeProposals= (ITypeBinding[]) accessor2.get("fTypeProposals");
		String[] typeNames= new String[typeProposals.length];
		for (int i= 0; i < typeNames.length; i++) {
			typeNames[i]= BindingLabelProvider.getBindingLabel(typeProposals[i], JavaElementLabels.T_TYPE_PARAMETERS | JavaElementLabels.T_FULLY_QUALIFIED);
		}
		String[] expectedNames= new String[] {
				"java.util.List<Number>",
				"java.util.ArrayList<Number>",
				"java.util.Collection<Number>",
				"java.lang.Iterable<Number>",
				"java.util.RandomAccess",
				"java.lang.Cloneable",
				"java.io.Serializable",
				"java.util.AbstractList<Number>",
				"java.util.AbstractCollection<Number>",
				"java.lang.Object",
		};
		assertArrayEquals(expectedNames, typeNames);
	}

	@Test
	public void testTypeMismatchInFieldDecl() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    int time= System.currentTimeMillis();
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    int time= (int) System.currentTimeMillis();
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    long time= System.currentTimeMillis();
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testTypeMismatchInFieldDeclNoImport() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private class StringBuffer { }
			    private final StringBuffer sb;
			    public E() {
			        sb= new java.lang.StringBuffer();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    private class StringBuffer { }
			    private final java.lang.StringBuffer sb;
			    public E() {
			        sb= new java.lang.StringBuffer();
			    }
			}
			""";

		assertEqualString(preview1, expected1);
	}

	@Test
	public void testTypeMismatchInAssignment() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Iterator;
			public class E {
			    public void foo(Iterator iter) {
			        String str;
			        str= iter.next();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.Iterator;
			public class E {
			    public void foo(Iterator iter) {
			        String str;
			        str= (String) iter.next();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.Iterator;
			public class E {
			    public void foo(Iterator iter) {
			        Object str;
			        str= iter.next();
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}

	@Test
	public void testTypeMismatchInAssignment2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Iterator;
			public class E {
			    public void foo(Iterator iter) {
			        String str, str2;
			        str= iter.next();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.Iterator;
			public class E {
			    public void foo(Iterator iter) {
			        String str, str2;
			        str= (String) iter.next();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.Iterator;
			public class E {
			    public void foo(Iterator iter) {
			        Object str;
			        String str2;
			        str= iter.next();
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}

	@Test
	public void testTypeMismatchInAssignment3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Iterator;
			public enum E {
			    A, B;
			    String str, str2;
			    public void foo(Iterator iter) {
			        str2= iter.next();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.Iterator;
			public enum E {
			    A, B;
			    String str, str2;
			    public void foo(Iterator iter) {
			        str2= (String) iter.next();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.Iterator;
			public enum E {
			    A, B;
			    String str;
			    Object str2;
			    public void foo(Iterator iter) {
			        str2= iter.next();
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testTypeMismatchInAssignment4() throws Exception {
		// test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=540927
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Map;
			import java.util.Map.Entry;
			import java.util.Set;
			public class E {
			    static void foo(Map<Integer, ? extends Number> path) {
			        Set<Entry<Integer, ? extends Number>> s = path.entrySet();
			        System.out.println(s);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.Map;
			import java.util.Map.Entry;
			import java.util.Set;
			public class E {
			    static void foo(Map<Integer, ? extends Number> path) {
			        Set<?> s = path.entrySet();
			        System.out.println(s);
			    }
			}
			""";

		assertEqualString(preview1, expected1);
	}

	@Test
	public void testTypeMismatchInExpression() throws Exception {

		IPackageFragment pack0= fSourceFolder.createPackageFragment("test0", false, null);
		String str= """
			package test0;
			public class Other {
			    public Object[] toArray() {
			        return null;
			    }
			}
			""";
		pack0.createCompilationUnit("Other.java", str, false, null);


		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			import test0.Other;
			public class E {
			    public String[] foo(Other other) {
			        return other.toArray();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import test0.Other;
			public class E {
			    public String[] foo(Other other) {
			        return (String[]) other.toArray();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import test0.Other;
			public class E {
			    public Object[] foo(Other other) {
			        return other.toArray();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test0;
			public class Other {
			    public String[] toArray() {
			        return null;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testCastOnCastExpression() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.ArrayList;
			import java.util.List;
			public class E {
			    public void foo(List list) {
			        ArrayList a= (Cloneable) list;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.ArrayList;
			import java.util.List;
			public class E {
			    public void foo(List list) {
			        ArrayList a= (ArrayList) list;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.ArrayList;
			import java.util.List;
			public class E {
			    public void foo(List list) {
			        Cloneable a= (Cloneable) list;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}


	@Test
	public void testMismatchingReturnType1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class Base {
			    public String getName() {
			        return null;
			    }
			}
			""";
		pack1.createCompilationUnit("Base.java", str, false, null);

		String str1= """
			package test1;
			public class E extends Base {
			    public char[] getName() {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E extends Base {
			    public String getName() {
			        return null;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class Base {
			    public char[] getName() {
			        return null;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testMismatchingReturnType2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.List;
			public interface IBase {
			    List getCollection();
			}
			""";
		pack1.createCompilationUnit("IBase.java", str, false, null);

		String str1= """
			package test1;
			public class E implements IBase {
			    public String[] getCollection() {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			
			import java.util.List;
			
			public class E implements IBase {
			    public List getCollection() {
			        return null;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.List;
			public interface IBase {
			    String[] getCollection();
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testMismatchingReturnTypeOnGeneric() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class Base<T extends Number> {
			    public String getName(T... t) {
			        return null;
			    }
			}
			""";
		pack1.createCompilationUnit("Base.java", str, false, null);

		String str1= """
			package test1;
			public class E extends Base<Integer> {
			    public char[] getName(Integer... i) {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E extends Base<Integer> {
			    public String getName(Integer... i) {
			        return null;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class Base<T extends Number> {
			    public char[] getName(T... t) {
			        return null;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testMismatchingReturnTypeOnGeneric2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class Base {
			    public Number getVal() {
			        return null;
			    }
			}
			""";
		pack1.createCompilationUnit("Base.java", str, false, null);

		String str1= """
			package test1;
			public class E<T> extends Base {
			    public T getVal() {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E<T> extends Base {
			    public Number getVal() {
			        return null;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	@Test
	public void testMismatchingReturnTypeOnGenericMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			import java.lang.annotation.Annotation;
			import java.lang.reflect.AccessibleObject;
			public class E {
			    void m() {
			        new AccessibleObject() {
			            public <T extends Annotation> void getAnnotation(Class<T> annotationClass) {
			            }
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.lang.annotation.Annotation;
			import java.lang.reflect.AccessibleObject;
			public class E {
			    void m() {
			        new AccessibleObject() {
			            public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
			            }
			        };
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	@Test
	public void testMismatchingReturnTypeOnGenericMethod14() throws Exception {
		Map<String, String> options= fJProject1.getOptions(false);
		try {
			Map<String, String> options14= new HashMap<>(options);
			JavaModelUtil.setComplianceOptions(options14, JavaCore.VERSION_1_4);
			fJProject1.setOptions(options14);
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

			String str= """
				package test1;
				import java.lang.reflect.AccessibleObject;
				public class E {
				    void m() {
				        new AccessibleObject() {
				            public void getAnnotation(Class annotationClass) {
				            }
				        };
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

			CompilationUnit astRoot= getASTRoot(cu);
			ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
			assertNumberOfProposals(proposals, 1);
			assertCorrectLabels(proposals);

			CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
			String preview1= getPreviewContent(proposal);

			String expected1= """
				package test1;
				import java.lang.annotation.Annotation;
				import java.lang.reflect.AccessibleObject;
				public class E {
				    void m() {
				        new AccessibleObject() {
				            public Annotation getAnnotation(Class annotationClass) {
				            }
				        };
				    }
				}
				""";

			assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
		} finally {
			fJProject1.setOptions(options);
		}
	}

	@Test
	public void testMismatchingReturnTypeParameterized() throws Exception {
		// test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=165913
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class Base {
			    public Number getVal() {
			        return null;
			    }
			}
			""";
		pack1.createCompilationUnit("Base.java", str, false, null);

		String str1= """
			package test1;
			public class E<T> extends Base {
			    public E<T> getVal() {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E<T> extends Base {
			    public Number getVal() {
			        return null;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	@Test
	public void testMismatchingReturnTypeOnWildcardExtends() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.ArrayList;
			public class E {
			    public Integer getIt(ArrayList<? extends Number> b) {
			        return b.get(0);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.ArrayList;
			public class E {
			    public Number getIt(ArrayList<? extends Number> b) {
			        return b.get(0);
			    }
			}
			""";

		proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.ArrayList;
			public class E {
			    public Integer getIt(ArrayList<? extends Number> b) {
			        return (Integer) b.get(0);
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testMismatchingReturnTypeOnWildcardSuper() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.ArrayList;
			public class E {
			    public Integer getIt(ArrayList<? super Number> b) {
			        return b.get(0);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.ArrayList;
			public class E {
			    public Object getIt(ArrayList<? super Number> b) {
			        return b.get(0);
			    }
			}
			""";

		proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.ArrayList;
			public class E {
			    public Integer getIt(ArrayList<? super Number> b) {
			        return (Integer) b.get(0);
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testMismatchingExceptions1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public interface IBase {
			    String[] getValues();
			}
			""";
		pack1.createCompilationUnit("IBase.java", str, false, null);

		String str1= """
			package test1;
			import java.io.IOException;
			public class E implements IBase {
			    public String[] getValues() throws IOException {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			
			import java.io.IOException;
			
			public interface IBase {
			    String[] getValues() throws IOException;
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			
			public class E implements IBase {
			    public String[] getValues() {
			        return null;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testMismatchingExceptions2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.IOException;
			public class Base {
			    String[] getValues() throws IOException {
			        return null;
			    }
			}
			""";
		pack1.createCompilationUnit("Base.java", str, false, null);

		String str1= """
			package test1;
			import java.io.EOFException;
			import java.text.ParseException;
			public class E extends Base {
			    public String[] getValues() throws EOFException, ParseException {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.io.IOException;
			import java.text.ParseException;
			public class Base {
			    String[] getValues() throws IOException, ParseException {
			        return null;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.EOFException;
			public class E extends Base {
			    public String[] getValues() throws EOFException {
			        return null;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testMismatchingExceptions3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.IOException;
			public class Base {
			    /**
			     * @param i The parameter
			     *                  More about the parameter
			     * @return The returned argument
			     * @throws IOException IO problems
			     * @since 3.0
			     */
			    String[] getValues(int i) throws IOException {
			        return null;
			    }
			}
			""";
		pack1.createCompilationUnit("Base.java", str, false, null);

		String str1= """
			package test1;
			import java.io.EOFException;
			import java.text.ParseException;
			public class E extends Base {
			    /**
			     * @param i The parameter
			     *                  More about the parameter
			     * @return The returned argument
			     * @throws EOFException EOF problems
			     * @throws ParseException Parse problems
			     */
			    public String[] getValues(int i) throws EOFException, ParseException {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.io.IOException;
			import java.text.ParseException;
			public class Base {
			    /**
			     * @param i The parameter
			     *                  More about the parameter
			     * @return The returned argument
			     * @throws IOException IO problems
			     * @throws ParseException\s
			     * @since 3.0
			     */
			    String[] getValues(int i) throws IOException, ParseException {
			        return null;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.EOFException;
			public class E extends Base {
			    /**
			     * @param i The parameter
			     *                  More about the parameter
			     * @return The returned argument
			     * @throws EOFException EOF problems
			     */
			    public String[] getValues(int i) throws EOFException {
			        return null;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testMismatchingExceptionsOnGeneric() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public interface IBase<T> {
			    T[] getValues();
			}
			""";
		pack1.createCompilationUnit("IBase.java", str, false, null);

		String str1= """
			package test1;
			import java.io.IOException;
			public class E implements IBase<String> {
			    public String[] getValues() throws IOException {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			
			import java.io.IOException;
			
			public interface IBase<T> {
			    T[] getValues() throws IOException;
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			
			public class E implements IBase<String> {
			    public String[] getValues() {
			        return null;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testMismatchingExceptionsOnBinaryParent() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E implements Runnable {
			    public void run() throws ClassNotFoundException {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= """
			package test1;
			public class E implements Runnable {
			    public void run() {
			    }
			}
			""";

		assertEqualString(preview, expected);
	}

	@Test
	public void testTypeMismatchInAnnotationValues1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class E {
			    public @interface Annot {
			        String newAttrib();
			    }
			    @Annot(newAttrib= 1)
			    public void foo() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package pack;
			public class E {
			    public @interface Annot {
			        int newAttrib();
			    }
			    @Annot(newAttrib= 1)
			    public void foo() {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testTypeMismatchInAnnotationValues2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class Other<T> {
			    public @interface Annot {
			        String newAttrib();
			    }
			}
			""";
		pack1.createCompilationUnit("Other.java", str, false, null);

		String str1= """
			package pack;
			public class E {
			    @Other.Annot(newAttrib= 1)
			    public void foo() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package pack;
			public class Other<T> {
			    public @interface Annot {
			        int newAttrib();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testTypeMismatchInSingleMemberAnnotation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class E {
			    public @interface Annot {
			        String value();
			    }
			    @Annot(1)
			    public void foo() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package pack;
			public class E {
			    public @interface Annot {
			        int value();
			    }
			    @Annot(1)
			    public void foo() {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testTypeMismatchWithEnumConstant() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public enum E {
			    ONE;
			    int m(int i) {
			            return ONE;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package pack;
			public enum E {
			    ONE;
			    E m(int i) {
			            return ONE;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}


	@Test
	public void testTypeMismatchWithArrayLength() throws Exception {
		// test for bug 126488
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class TestShort {
			        public static void main(String[] args) {
			                short test=args.length;
			        }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("TestShort.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		expected[0]= """
			package pack;
			public class TestShort {
			        public static void main(String[] args) {
			                short test=(short) args.length;
			        }
			}
			""";

		expected[1]= """
			package pack;
			public class TestShort {
			        public static void main(String[] args) {
			                int test=args.length;
			        }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testTypeMismatchWithTypeInSamePackage() throws Exception {
		// test for bug 198586
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		String str= """
			package test1;
			public class E {}
			""";
		pack2.createCompilationUnit("E.java", str, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {}
			""";
		pack1.createCompilationUnit("E.java", str1, false, null);

		String str2= """
			package test1;
			public class Test {
			    test2.E e2= new Object();
			    E e1;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", str2, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		expected[0]= """
			package test1;
			public class Test {
			    test2.E e2= (test2.E) new Object();
			    E e1;
			}
			""";

		expected[1]= """
			package test1;
			public class Test {
			    Object e2= new Object();
			    E e1;
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testTypeMismatchInForEachProposalsList() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			import java.util.List;
			
			public class E {
			    public void foo() {
			        List<String> l= null;   \s
			        for (Number e : l) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package pack;
			
			import java.util.List;
			
			public class E {
			    public void foo() {
			        List<String> l= null;   \s
			        for (String e : l) {
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testTypeMismatchInForEachProposalsListExtends() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			import java.util.List;
			
			public class E {
			    public void foo() {
			        List<? extends String> l= null;   \s
			        for (Number e : l) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package pack;
			
			import java.util.List;
			
			public class E {
			    public void foo() {
			        List<? extends String> l= null;   \s
			        for (String e : l) {
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testTypeMismatchInForEachProposalsListSuper() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			import java.util.List;
			
			public class E {
			    public void foo() {
			        List<? super String> l= null;   \s
			        for (Number e : l) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package pack;
			
			import java.util.List;
			
			public class E {
			    public void foo() {
			        List<? super String> l= null;   \s
			        for (Object e : l) {
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testTypeMismatchInForEachProposalsArrays() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			import java.util.List;
			
			public class E {
			    public void foo() {
			        String[] l= null;
			        for (Number e : l) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package pack;
			
			import java.util.List;
			
			public class E {
			    public void foo() {
			        String[] l= null;
			        for (String e : l) {
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testTypeMismatchInForEachMissingType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			public class E {
			    public void foo(String[] strings) {
			        for (s: strings) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 3, 2);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package pack;
			
			public class E {
			    public void foo(String[] strings) {
			        for (String s: strings) {
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testNullCheck() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			public class E {
			    public static void main(String arg) {
			        while (arg) {
			        }
			    }
			}
			
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		expected[0]= """
			package pack;
			
			public class E {
			    public static void main(boolean arg) {
			        while (arg) {
			        }
			    }
			}
			
			""";

		expected[1]= """
			package pack;
			
			public class E {
			    public static void main(String arg) {
			        while (arg != null) {
			        }
			    }
			}
			
			""";

		assertExpectedExistInProposals(proposals, expected);
	}


	@Test
	public void testTypeMismatchObjectAndPrimitiveType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			
			public class E {
			    public void foo() {
			        Object o= new Object();
			        int i= o;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);

		ICompletionProposal proposal= proposals.get(0);
		assertNotEquals(-1, proposal.getDisplayString().indexOf("Integer"));

		String[] expected= new String[3];
		expected[0]= """
			package pack;
			
			
			public class E {
			    public void foo() {
			        Object o= new Object();
			        int i= (Integer) o;
			    }
			}
			""";

		expected[1]= """
			package pack;
			
			
			public class E {
			    public void foo() {
			        int o= new Object();
			        int i= o;
			    }
			}
			""";

		expected[2]= """
			package pack;
			
			
			public class E {
			    public void foo() {
			        Object o= new Object();
			        Object i= o;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testTypeMismatchPrimitiveTypes() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			
			public class E {
			    public void foo(long o) {
			        int i= o;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);

		ICompletionProposal proposal= proposals.get(0);
		assertEquals(-1, proposal.getDisplayString().indexOf("Integer"));

		ICompletionProposal proposal2= proposals.get(1);
		assertEquals(-1, proposal2.getDisplayString().indexOf("Integer"));

		ICompletionProposal proposal3= proposals.get(2);
		assertEquals(-1, proposal3.getDisplayString().indexOf("Integer"));

		String[] expected= new String[3];
		expected[0]= """
			package pack;
			
			
			public class E {
			    public void foo(long o) {
			        int i= (int) o;
			    }
			}
			""";

		expected[1]= """
			package pack;
			
			
			public class E {
			    public void foo(int o) {
			        int i= o;
			    }
			}
			""";

		expected[2]= """
			package pack;
			
			
			public class E {
			    public void foo(long o) {
			        long i= o;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

}
