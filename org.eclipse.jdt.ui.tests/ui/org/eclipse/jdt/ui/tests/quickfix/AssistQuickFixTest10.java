/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
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

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.Java10ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.TypeChangeCorrectionProposal;

public class AssistQuickFixTest10 extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup= new Java10ProjectTestSetup();

	private static final Class<?>[] TYPE_CHANGE_PROPOSAL_TYPE= { TypeChangeCorrectionProposal.class };
	private static final Class<?>[] LINKED_PROPOSAL_TYPE= { LinkedCorrectionProposal.class };

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

//	public static Test setUpTest(Test test) {
//		return new Java10ProjectTestSetup(test) {
//			@Override
//			protected void setUp() throws Exception {
//				JavaProjectHelper.PERFORM_DUMMY_SEARCH++;
//				super.setUp();
//			}
//
//			@Override
//			protected void tearDown() throws Exception {
//				super.tearDown();
//				JavaProjectHelper.PERFORM_DUMMY_SEARCH--;
//			}
//		};
//	}

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "//TODO\n${body_statement}", null);

		fJProject1= projectSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testChangeVarTypeToBindingTypeProposal() throws Exception {
		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			 {
			    var one= new String();
			 }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int offset= str1.indexOf("one");

		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= getExpectedProposals(collectAssists(context, false), TYPE_CHANGE_PROPOSAL_TYPE);

		assertNumberOfProposals(proposals, 1);

		String preview= getPreviewContent((TypeChangeCorrectionProposal) proposals.get(0));
		String str2= """
			package test;
			public class Cls {
			 {
			    String one= new String();
			 }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testChangeTypeToVarTypeProposal() throws Exception {
		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			 {
			    String one= new String();
			 }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int offset= str1.indexOf("one");

		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= getExpectedProposals(collectAssists(context, false), TYPE_CHANGE_PROPOSAL_TYPE);

		assertNumberOfProposals(proposals, 1);

		String preview= getPreviewContent((TypeChangeCorrectionProposal) proposals.get(0));
		String str2= """
			package test;
			public class Cls {
			 {
			    var one= new String();
			 }
			}
			""";
		assertEqualString(preview, str2);

	}

	@Test
	public void testChangeVarTypeToBindingTypeProposalWithTypeAnnotation() throws Exception {
		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			
			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			
			public class Cls {
			 @Target(ElementType.TYPE_USE)
			 public @interface N {
			
			 }
			
			 {
			    var one= (@N String) new String();
			 }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int offset= str1.indexOf("one");

		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= getExpectedProposals(collectAssists(context, false), TYPE_CHANGE_PROPOSAL_TYPE);

		assertNumberOfProposals(proposals, 1);

		String preview= getPreviewContent((TypeChangeCorrectionProposal) proposals.get(0));
		String str2= """
			package test;
			
			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			
			public class Cls {
			 @Target(ElementType.TYPE_USE)
			 public @interface N {
			
			 }
			
			 {
			    @N String one= (@N String) new String();
			 }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testChangeTypeToVarTypeProposalWithoutTypeAnnotation() throws Exception {
		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			
			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			
			public class Cls {
			 @Target(ElementType.TYPE_USE)
			 public @interface N {
			
			 }
			
			 {
			    @N String one= new String();
			 }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int offset= str1.indexOf("one");

		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= getExpectedProposals(collectAssists(context, false), TYPE_CHANGE_PROPOSAL_TYPE);

		assertNumberOfProposals(proposals, 1);

		String preview= getPreviewContent((TypeChangeCorrectionProposal) proposals.get(0));
		String str2= """
			package test;
			
			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			
			public class Cls {
			 @Target(ElementType.TYPE_USE)
			 public @interface N {
			
			 }
			
			 {
			    var one= new String();
			 }
			}
			""";
		assertEqualString(preview, str2);

	}

	@Test
	public void testChangeVarToTypeNoTypeChangeProposal() throws Exception {
		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			 {
			   var one = new Object() {     // inferred
			       int field = 2;
			   };
			   var two= (CharSequence & Comparable<String>) "x";
			 }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int offset= str1.indexOf("one");

		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= getExpectedProposals(collectAssists(context, false), TYPE_CHANGE_PROPOSAL_TYPE);

		assertNumberOfProposals(proposals, 0);

		offset= str1.indexOf("two");

		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= getExpectedProposals(collectAssists(context, false), TYPE_CHANGE_PROPOSAL_TYPE);

		assertNumberOfProposals(proposals, 0);

	}

	@Test
	public void testChangeTypeToVarChangeProposalRemoveUnusedImport() throws Exception {
		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			
			import java.util.List;
			
			public class Second {
			   public static List<String> getList() {\s
			      return null;
			   }
			}
			""";
		pack.createCompilationUnit("Second.java", str1, false, null);


		String str2= """
			package test;
			
			import java.util.List;
			
			public class Cls {
			 {
			   List<String> one = Second.getList();\s
			 }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str2, false, null);

		int offset= str2.indexOf("one");

		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= getExpectedProposals(collectAssists(context, false), TYPE_CHANGE_PROPOSAL_TYPE);

		assertNumberOfProposals(proposals, 1);

		String preview= getPreviewContent((TypeChangeCorrectionProposal) proposals.get(0));
		String str3= """
			package test;
			
			public class Cls {
			 {
			   var one = Second.getList();\s
			 }
			}
			""";
		assertEqualString(preview, str3);
	}

	@Test
	public void testChangeTypeToVarChangeProposalDonotRemoveUsedImport() throws Exception {
		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			
			import java.util.List;
			
			public class Second {
			   public static List<String> getList() {\s
			      return null;
			   }
			}
			""";
		pack.createCompilationUnit("Second.java", str1, false, null);


		String str2= """
			package test;
			
			import java.util.List;
			
			public class Cls {
			 {
			   List<String> one = Second.getList();\s
			   List<String> two = Second.getList();\s
			 }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str2, false, null);

		int offset= str2.indexOf("one");

		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= getExpectedProposals(collectAssists(context, false), TYPE_CHANGE_PROPOSAL_TYPE);

		assertNumberOfProposals(proposals, 1);

		String preview= getPreviewContent((TypeChangeCorrectionProposal) proposals.get(0));
		String str3= """
			package test;
			
			import java.util.List;
			
			public class Cls {
			 {
			   var one = Second.getList();\s
			   List<String> two = Second.getList();\s
			 }
			}
			""";
		assertEqualString(preview, str3);
	}

	@Test
	public void testChangeParametrizedTypeToVarTypeProposalDoesNotLoseTypeArguments() throws Exception {
		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			
			import java.util.ArrayList;
			
			public class Cls {
			 {
			    ArrayList<String> one= new ArrayList<>();
			 }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int offset= str1.indexOf("one");

		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= getExpectedProposals(collectAssists(context, false), TYPE_CHANGE_PROPOSAL_TYPE);

		assertNumberOfProposals(proposals, 1);

		String preview= getPreviewContent((TypeChangeCorrectionProposal) proposals.get(0));
		String str2= """
			package test;
			
			import java.util.ArrayList;
			
			public class Cls {
			 {
			    var one= new ArrayList<String>();
			 }
			}
			""";
		assertEqualString(preview, str2);

	}

	@Test
	public void testConvertToForUsingIndexWithVar() throws Exception {
		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			
			public class Cls {
			  public void foo() {
			    for (var y : new int[0]) {
			      System.out.println(y);
			    }
			  }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int offset= str1.indexOf("for");

		AssistContext context= getCorrectionContext(cu, offset, 3);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= getExpectedProposals(collectAssists(context, false), LINKED_PROPOSAL_TYPE);

		String str2= """
			package test;
			
			public class Cls {
			  public void foo() {
			    var is = new int[0];
			    for (int i = 0; i < is.length; i++) {
			        var y = is[i];
			        System.out.println(y);
			    }
			  }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	private static final ArrayList<IJavaCompletionProposal> getExpectedProposals(ArrayList<IJavaCompletionProposal> proposals, Class<?>[] expectedTypes) {
		ArrayList<IJavaCompletionProposal> expected= new ArrayList<>(proposals);
		if (expectedTypes != null && expectedTypes.length > 0) {
			for (Iterator<IJavaCompletionProposal> iter= expected.iterator(); iter.hasNext();) {
				if (!isexpected(iter.next(), expectedTypes)) {
					iter.remove();
				}
			}
		}
		return expected;
	}

	private static boolean isexpected(Object curr, Class<?>[] expectedTypes) {
		for (Class<?> expectedType : expectedTypes) {
			if (expectedType.isInstance(curr)) {
				return true;
			}
		}
		return false;
	}

}
