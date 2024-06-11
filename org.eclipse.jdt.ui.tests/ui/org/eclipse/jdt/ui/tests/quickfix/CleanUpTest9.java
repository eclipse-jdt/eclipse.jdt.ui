/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
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

import static org.junit.Assert.assertNotEquals;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.tests.core.rules.Java9ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

/**
 * Tests the cleanup features related to Java 9.
 */
public class CleanUpTest9 extends CleanUpTestCase {
	@Rule
	public ProjectTestSetup projectSetup= new Java9ProjectTestSetup();

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	@Test
	public void testUseTryWithResource() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;
			
			import java.io.FileInputStream;
			
			public class E {
			    public void refactorFullyInitializedResourceRemoveFinally() throws Exception {
			        // Keep this comment
			        final FileInputStream inputStream = new FileInputStream("data.txt");
			        // Keep this comment too
			        try {
			            System.out.println(inputStream.read());
			        } finally {
			            inputStream.close();
			        }
			    }
			
			    public void refactorFullyInitializedResourceDoNotRemoveFinally() throws Exception {
			        // Keep this comment
			        final FileInputStream inputStream = new FileInputStream("out.txt");
			        // Keep this comment too
			        try {
			            System.out.println(inputStream.read());
			        } finally {
			            inputStream.close();
			            System.out.println("Done");
			        }
			    }
			
			    public boolean removeClosureOnStillUsedCloseable() throws Exception {
			        // Keep this comment
			        final FileInputStream inputStream = new FileInputStream("input.txt");
			        // Keep this comment too
			        try {
			            System.out.println(inputStream.read());
			        } finally {
			            inputStream.close();
			            System.out.println("Done");
			        }
			
			        return inputStream != null;
			    }
			
			    public void refactorFullyInitializedResourceOnlyRemoveFinallyIf() throws Exception {
			        // Keep this comment
			        final FileInputStream inputStream = new FileInputStream("out.txt");
			        // Keep this comment too
			        try {
			            System.out.println(inputStream.read());
			        } finally {
			            if (inputStream != null) {
			                inputStream.close();
			            }
			            System.out.println("Done");
			        }
			    }
			
			    public void refactorNullInitializedResourceRemoveFinally() throws Exception {
			        // Keep this comment
			        FileInputStream inputStream = null;
			        // Keep this comment too
			        try {
			            inputStream = new FileInputStream("output.txt");
			            System.out.println(inputStream.read());
			        } finally {
			            if (inputStream != null) {
			                inputStream.close();
			            }
			        }
			    }
			
			    public void refactorNullInitializedResourceDoNotRemoveFinally() throws Exception {
			        // Keep this comment
			        FileInputStream inputStream = null;
			        // Keep this comment too
			        try {
			            inputStream = new FileInputStream("file.txt");
			            System.out.println(inputStream.read());
			        } finally {
			            if (null != inputStream) {
			                inputStream.close();
			            }
			            System.out.println("Done");
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);

		enable(CleanUpConstants.TRY_WITH_RESOURCE);

		String expected= """
			package test1;
			
			import java.io.FileInputStream;
			
			public class E {
			    public void refactorFullyInitializedResourceRemoveFinally() throws Exception {
			        // Keep this comment
			        final FileInputStream inputStream = new FileInputStream("data.txt");
			        // Keep this comment too
			        try (inputStream) {
			            System.out.println(inputStream.read());
			        }
			    }
			
			    public void refactorFullyInitializedResourceDoNotRemoveFinally() throws Exception {
			        // Keep this comment
			        final FileInputStream inputStream = new FileInputStream("out.txt");
			        // Keep this comment too
			        try (inputStream) {
			            System.out.println(inputStream.read());
			        } finally {
			            System.out.println("Done");
			        }
			    }
			
			    public boolean removeClosureOnStillUsedCloseable() throws Exception {
			        // Keep this comment
			        final FileInputStream inputStream = new FileInputStream("input.txt");
			        // Keep this comment too
			        try (inputStream) {
			            System.out.println(inputStream.read());
			        } finally {
			            System.out.println("Done");
			        }
			
			        return inputStream != null;
			    }
			
			    public void refactorFullyInitializedResourceOnlyRemoveFinallyIf() throws Exception {
			        // Keep this comment
			        final FileInputStream inputStream = new FileInputStream("out.txt");
			        // Keep this comment too
			        try (inputStream) {
			            System.out.println(inputStream.read());
			        } finally {
			            System.out.println("Done");
			        }
			    }
			
			    public void refactorNullInitializedResourceRemoveFinally() throws Exception {
			        // Keep this comment
			        // Keep this comment too
			        try (FileInputStream inputStream = new FileInputStream("output.txt")) {
			            System.out.println(inputStream.read());
			        }
			    }
			
			    public void refactorNullInitializedResourceDoNotRemoveFinally() throws Exception {
			        // Keep this comment
			        // Keep this comment too
			        try (FileInputStream inputStream = new FileInputStream("file.txt")) {
			            System.out.println(inputStream.read());
			        } finally {
			            System.out.println("Done");
			        }
			    }
			}
			""";

		assertNotEquals("The class must be changed", given, expected);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.TryWithResourceCleanup_description)));
	}

	@Test
	public void testDoNotUseTryWithResource() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.io.FileInputStream;
			
			public class E {
			    public boolean doNotRefactorStillUsedCloseable() throws Exception {
			        FileInputStream inputStream = null;
			        try {
			            inputStream = new FileInputStream("out.txt");
			            System.out.println(inputStream.read());
			        } finally {
			            inputStream.close();
			        }
			
			        return inputStream != null;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.TRY_WITH_RESOURCE);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}
}
