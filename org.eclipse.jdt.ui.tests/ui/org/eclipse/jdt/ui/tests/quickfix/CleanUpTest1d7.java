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

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.FixMessages;

import org.eclipse.jdt.ui.tests.core.rules.Java1d7ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

public class CleanUpTest1d7 extends CleanUpTestCase {
	@Rule
    public ProjectTestSetup projectSetup= new Java1d7ProjectTestSetup();

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	@Test
	public void testRemoveRedundantTypeArguments1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.ArrayList;\n" //
				+ "import java.util.HashMap;\n" //
				+ "import java.util.List;\n" //
				+ "import java.util.Map;\n" //
				+ "public class E {\n" //
				+ "    void foo() {\n" //
				+ "        new ArrayList<String>().add(\"a\")\n" //
				+ "        List<String> a = new ArrayList<String>();\n" //
				+ "        Map<Integer, String> m = new HashMap<Integer, String>();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_REDUNDANT_TYPE_ARGUMENTS);

		sample= "" //
				+ "package test1;\n" //
				+ "import java.util.ArrayList;\n" //
				+ "import java.util.HashMap;\n" //
				+ "import java.util.List;\n" //
				+ "import java.util.Map;\n" //
				+ "public class E {\n" //
				+ "    void foo() {\n" //
				+ "        new ArrayList<String>().add(\"a\")\n" //
				+ "        List<String> a = new ArrayList<>();\n" //
				+ "        Map<Integer, String> m = new HashMap<>();\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testObjectsEquals() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Map;\n" //
				+ "import java.util.Observable;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    private Map<Integer, String> textById;\n" //
				+ "    private Observable anObservable;\n" //
				+ "    private String aText;\n" //
				+ "\n" //
				+ "    /* (non-Javadoc)\n" //
				+ "     * @see java.lang.Object#equals(java.lang.Object)\n" //
				+ "     */\n" //
				+ "    @Override\n" //
				+ "    public boolean equals(Object obj) {\n" //
				+ "        if (this == obj)\n" //
				+ "            return true;\n" //
				+ "        if (obj == null)\n" //
				+ "            return false;\n" //
				+ "        if (getClass() != obj.getClass())\n" //
				+ "            return false;\n" //
				+ "        E1 other = (E1) obj;\n" //
				+ "        if (aText == null) {\n" //
				+ "            if (other.aText != null)\n" //
				+ "                return false;\n" //
				+ "        } else if (!aText.equals(other.aText))\n" //
				+ "            return false;\n" //
				+ "        if (null == anObservable) {\n" //
				+ "            if (null != other.anObservable)\n" //
				+ "                return false;\n" //
				+ "        } else if (!anObservable.equals(other.anObservable))\n" //
				+ "            return false;\n" //
				+ "        if (this.textById == null) {\n" //
				+ "            if (other.textById != null)\n" //
				+ "                return false;\n" //
				+ "        } else if (!this.textById.equals(other.textById)) {\n" //
				+ "            return false;\n" //
				+ "        }\n" //
				+ "        return true;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_OBJECTS_EQUALS);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Map;\n" //
				+ "import java.util.Objects;\n" //
				+ "import java.util.Observable;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    private Map<Integer, String> textById;\n" //
				+ "    private Observable anObservable;\n" //
				+ "    private String aText;\n" //
				+ "\n" //
				+ "    /* (non-Javadoc)\n" //
				+ "     * @see java.lang.Object#equals(java.lang.Object)\n" //
				+ "     */\n" //
				+ "    @Override\n" //
				+ "    public boolean equals(Object obj) {\n" //
				+ "        if (this == obj)\n" //
				+ "            return true;\n" //
				+ "        if (obj == null)\n" //
				+ "            return false;\n" //
				+ "        if (getClass() != obj.getClass())\n" //
				+ "            return false;\n" //
				+ "        E1 other = (E1) obj;\n" //
				+ "        if (!Objects.equals(aText, other.aText)) {\n" //
				+ "            return false;\n" //
				+ "        }\n" //
				+ "        if (!Objects.equals(anObservable, other.anObservable)) {\n" //
				+ "            return false;\n" //
				+ "        }\n" //
				+ "        if (!Objects.equals(this.textById, other.textById)) {\n" //
				+ "            return false;\n" //
				+ "        }\n" //
				+ "        return true;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testDoNotRefactorObjectsEquals() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Map;\n" //
				+ "import java.util.Observable;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    private Map<Integer, String> textById;\n" //
				+ "    private Observable anObservable;\n" //
				+ "    private String aText;\n" //
				+ "\n" //
				+ "    /* (non-Javadoc)\n" //
				+ "     * @see java.lang.Object#equals(java.lang.Object)\n" //
				+ "     */\n" //
				+ "    @Override\n" //
				+ "    public boolean equals(Object obj) {\n" //
				+ "        if (this == obj)\n" //
				+ "            return true;\n" //
				+ "        if (obj == null)\n" //
				+ "            return false;\n" //
				+ "        if (getClass() != obj.getClass())\n" //
				+ "            return false;\n" //
				+ "        E1 other = (E1) obj;\n" //
				+ "        if (aText == null) {\n" //
				+ "            if (other.aText != null)\n" //
				+ "                return true;\n" //
				+ "        } else if (!aText.equals(other.aText))\n" //
				+ "            return false;\n" //
				+ "        if (null == anObservable) {\n" //
				+ "            if (null != other.anObservable)\n" //
				+ "                return false;\n" //
				+ "        } else if (!anObservable.equals(other.anObservable))\n" //
				+ "            return true;\n" //
				+ "        if (this.textById == null) {\n" //
				+ "            if (other.textById != null)\n" //
				+ "                return false;\n" //
				+ "        } else if (this.textById.equals(other.textById))\n" //
				+ "            return false;\n" //
				+ "        return true;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_OBJECTS_EQUALS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testObjectsEqualsWithImportConflict() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Map;\n" //
				+ "import java.util.Observable;\n" //
				+ "\n" //
				+ "public class Objects {\n" //
				+ "    private Map<Integer, String> textById;\n" //
				+ "    private Observable anObservable;\n" //
				+ "    private String aText;\n" //
				+ "\n" //
				+ "    /* (non-Javadoc)\n" //
				+ "     * @see java.lang.Object#equals(java.lang.Object)\n" //
				+ "     */\n" //
				+ "    @Override\n" //
				+ "    public boolean equals(Object obj) {\n" //
				+ "        if (this == obj)\n" //
				+ "            return true;\n" //
				+ "        if (obj == null)\n" //
				+ "            return false;\n" //
				+ "        if (getClass() != obj.getClass())\n" //
				+ "            return false;\n" //
				+ "        Objects other = (Objects) obj;\n" //
				+ "        if (aText == null) {\n" //
				+ "            if (other.aText != null)\n" //
				+ "                return false;\n" //
				+ "        } else if (!aText.equals(other.aText))\n" //
				+ "            return false;\n" //
				+ "        if (null == anObservable) {\n" //
				+ "            if (null != other.anObservable)\n" //
				+ "                return false;\n" //
				+ "        } else if (!anObservable.equals(other.anObservable))\n" //
				+ "            return false;\n" //
				+ "        if (this.textById == null) {\n" //
				+ "            if (other.textById != null)\n" //
				+ "                return false;\n" //
				+ "        } else if (!this.textById.equals(other.textById))\n" //
				+ "            return false;\n" //
				+ "        return true;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("Objects.java", sample, false, null);

		enable(CleanUpConstants.USE_OBJECTS_EQUALS);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Map;\n" //
				+ "import java.util.Observable;\n" //
				+ "\n" //
				+ "public class Objects {\n" //
				+ "    private Map<Integer, String> textById;\n" //
				+ "    private Observable anObservable;\n" //
				+ "    private String aText;\n" //
				+ "\n" //
				+ "    /* (non-Javadoc)\n" //
				+ "     * @see java.lang.Object#equals(java.lang.Object)\n" //
				+ "     */\n" //
				+ "    @Override\n" //
				+ "    public boolean equals(Object obj) {\n" //
				+ "        if (this == obj)\n" //
				+ "            return true;\n" //
				+ "        if (obj == null)\n" //
				+ "            return false;\n" //
				+ "        if (getClass() != obj.getClass())\n" //
				+ "            return false;\n" //
				+ "        Objects other = (Objects) obj;\n" //
				+ "        if (!java.util.Objects.equals(aText, other.aText)) {\n" //
				+ "            return false;\n" //
				+ "        }\n" //
				+ "        if (!java.util.Objects.equals(anObservable, other.anObservable)) {\n" //
				+ "            return false;\n" //
				+ "        }\n" //
				+ "        if (!java.util.Objects.equals(this.textById, other.textById)) {\n" //
				+ "            return false;\n" //
				+ "        }\n" //
				+ "        return true;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testJava50ForLoop563267() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=563267
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=565282
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.io.IOException;\n" //
				+ "import java.io.InputStream;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(List<InputStream> toClose) throws IOException {\n" //
				+ "        for (Iterator<InputStream> it = toClose.iterator(); it.hasNext();) {\n" //
				+ "            try (InputStream r = it.next()) {\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "import java.io.IOException;\n" //
				+ "import java.io.InputStream;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(List<InputStream> toClose) throws IOException {\n" //
				+ "        for (InputStream inputStream : toClose) {\n" //
				+ "            try (InputStream r = inputStream) {\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertGroupCategoryUsed(new ICompilationUnit[] { cu1 }, new String[] { FixMessages.Java50Fix_ConvertToEnhancedForLoop_description });
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

}
