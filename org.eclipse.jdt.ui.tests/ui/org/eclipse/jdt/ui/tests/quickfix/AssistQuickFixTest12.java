/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.tests.core.rules.Java12ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

public class AssistQuickFixTest12 extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup = new Java12ProjectTestSetup(true);

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
		if (fJProject1 != null) {
			JavaProjectHelper.delete(fJProject1);
		}

	}

	@Test
	public void testSplitSwitchCaseStatement() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set12CompilerOptions(fJProject1, true);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

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
			    public static void foo(Day day) {
			        switch (day) {
			            case SATURDAY, SUNDAY: System.out.println("Weekend");
			            case MONDAY, TUESDAY, WEDNESDAY: System.out.println("Weekday");
			            case THURSDAY, FRIDAY: System.out.println("Weekday");
			            default :
			                break;
			        }
			    }
			}
			
			enum Day {
			    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		IInvocationContext ctx= getCorrectionContext(cu, 185, 0);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= """
			package test;
			public class Cls {
			    public static void foo(Day day) {
			        switch (day) {
			            case SATURDAY, SUNDAY: System.out.println("Weekend");
			            case MONDAY :
							System.out.println("Weekday");
						case TUESDAY :
							System.out.println("Weekday");
						case WEDNESDAY :
							System.out.println("Weekday");
						case THURSDAY, FRIDAY: System.out.println("Weekday");
			            default :
			                break;
			        }
			    }
			}
			
			enum Day {
			    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	@Test
	public void testSplitSwitchCaseLabelRuleStatement() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set12CompilerOptions(fJProject1, true);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

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
			    public static void foo(Day day) {
			        String weekDayOrEnd = switch (day) {
			            case SATURDAY, SUNDAY -> "Weekend";
			            case MONDAY, TUESDAY, WEDNESDAY -> "Weekday";
			            case THURSDAY, FRIDAY -> "Weekday";
			        };
			    }
			}
			
			enum Day {
			    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		IInvocationContext ctx= getCorrectionContext(cu, 239, 0);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= """
			package test;
			public class Cls {
			    public static void foo(Day day) {
			        String weekDayOrEnd = switch (day) {
			            case SATURDAY, SUNDAY -> "Weekend";
			            case MONDAY, TUESDAY, WEDNESDAY -> "Weekday";
			            case THURSDAY -> "Weekday";
						case FRIDAY -> "Weekday";
			        };
			    }
			}
			
			enum Day {
			    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}
}

