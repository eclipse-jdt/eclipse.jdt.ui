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

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

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
	public void testHash() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String input= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Arrays;\n" //
				+ "import java.util.Map;\n" //
				+ "import java.util.Observable;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public class RefactoredClass {\n" //
				+ "        private Map<Integer, String> innerTextById;\n" //
				+ "        private Observable innerObservable;\n" //
				+ "        private String innerText;\n" //
				+ "        private String[] innerTexts;\n" //
				+ "        private int[] innerIntegers;\n" //
				+ "        private char innerChar;\n" //
				+ "        private byte innerByte;\n" //
				+ "        private boolean innerBoolean;\n" //
				+ "        private int innerInt;\n" //
				+ "        private long innerLong;\n" //
				+ "        private double innerDouble;\n" //
				+ "        private short innerShort;\n" //
				+ "        private float innerFloat;\n" //
				+ "        private double innerOtherDouble;\n" //
				+ "        private Boolean innerBooleanWrapper;\n" //
				+ "\n" //
				+ "        @Override\n" //
				+ "        public int hashCode() {\n" //
				+ "            // Keep this comment\n" //
				+ "            final int prime = 31;\n" //
				+ "            int result = 1;\n" //
				+ "            result = prime * result + getEnclosingInstance().hashCode();\n" //
				+ "            result = prime * result + (RefactoredClass.this.innerBoolean ? 1231 : 1237);\n" //
				+ "            result = prime * result + this.innerByte;\n" //
				+ "            result = prime * result + innerChar;\n" //
				+ "            long temp = Double.doubleToLongBits(innerDouble);\n" //
				+ "            result = prime * result + (int) ((temp >>> 32) ^ temp);\n" //
				+ "            result = prime * result + Float.floatToIntBits(innerFloat);\n" //
				+ "            result = result * prime + innerInt;\n" //
				+ "            result = prime * result + Arrays.hashCode(innerIntegers);\n" //
				+ "            result = prime * result + (int) (innerLong ^ (this.innerLong >>> 32));\n" //
				+ "            result = prime * result + ((innerObservable == null) ? 0 : innerObservable.hashCode());\n" //
				+ "            temp = Double.doubleToLongBits(innerOtherDouble);\n" //
				+ "            result = prime * result + (int) (temp ^ (temp >>> 32));\n" //
				+ "            result = prime * result + innerShort;\n" //
				+ "            result = prime * result + ((innerText == null) ? 0 : innerText.hashCode());\n" //
				+ "            result = prime * result + ((innerTextById != null) ? this.innerTextById.hashCode() : 0);\n" //
				+ "            result = prime * result + ((this.innerBooleanWrapper != null) ? innerBooleanWrapper.hashCode() : 0);\n" //
				+ "            return prime * result + Arrays.hashCode(innerTexts);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        private E getEnclosingInstance() {\n" //
				+ "            return E.this;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private Map<Integer, String> textById;\n" //
				+ "    private Observable anObservable;\n" //
				+ "    private String aText;\n" //
				+ "    private String[] texts;\n" //
				+ "    private int[] integers;\n" //
				+ "    private char aChar;\n" //
				+ "    private byte aByte;\n" //
				+ "    private boolean aBoolean;\n" //
				+ "    private int anInt;\n" //
				+ "    private long aLong;\n" //
				+ "    private double aDouble;\n" //
				+ "    private short aShort;\n" //
				+ "    private float aFloat;\n" //
				+ "    private double anotherDouble;\n" //
				+ "\n" //
				+ "    @Override\n" //
				+ "    public int hashCode() {\n" //
				+ "        // Keep this comment\n" //
				+ "        final int prime = 31;\n" //
				+ "        int result = 1;\n" //
				+ "        result = prime * result + (E.this.aBoolean ? 1231 : 1237);\n" //
				+ "        result = prime * result + aByte;\n" //
				+ "        result = prime * result + aChar;\n" //
				+ "        result = prime * result + Float.floatToIntBits(aFloat);\n" //
				+ "        result = prime * result + (int) (aLong ^ (aLong >>> 32));\n" //
				+ "        long temp;\n" //
				+ "        temp = Double.doubleToLongBits(aDouble);\n" //
				+ "        result = prime * result + (int) (temp ^ (temp >>> 32));\n" //
				+ "        result = prime * result + aShort;\n" //
				+ "        result = prime * result + ((null == aText) ? 0 : aText.hashCode());\n" //
				+ "        result = prime * result + anInt;\n" //
				+ "        result = prime * result + ((anObservable == null) ? 0 : anObservable.hashCode());\n" //
				+ "        result = prime * result + Arrays.hashCode(integers);\n" //
				+ "        result = prime * result + ((textById == null) ? 0 : textById.hashCode());\n" //
				+ "        result = prime * result + Arrays.hashCode(texts);\n" //
				+ "        temp = Double.doubleToLongBits(anotherDouble);\n" //
				+ "        result = prime * result + (int) (temp ^ (temp >>> 32));\n" //
				+ "        return result;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", input, false, null);

		enable(CleanUpConstants.MODERNIZE_HASH);

		String output= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Arrays;\n" //
				+ "import java.util.Map;\n" //
				+ "import java.util.Objects;\n" //
				+ "import java.util.Observable;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public class RefactoredClass {\n" //
				+ "        private Map<Integer, String> innerTextById;\n" //
				+ "        private Observable innerObservable;\n" //
				+ "        private String innerText;\n" //
				+ "        private String[] innerTexts;\n" //
				+ "        private int[] innerIntegers;\n" //
				+ "        private char innerChar;\n" //
				+ "        private byte innerByte;\n" //
				+ "        private boolean innerBoolean;\n" //
				+ "        private int innerInt;\n" //
				+ "        private long innerLong;\n" //
				+ "        private double innerDouble;\n" //
				+ "        private short innerShort;\n" //
				+ "        private float innerFloat;\n" //
				+ "        private double innerOtherDouble;\n" //
				+ "        private Boolean innerBooleanWrapper;\n" //
				+ "\n" //
				+ "        @Override\n" //
				+ "        public int hashCode() {\n" //
				+ "            // Keep this comment\n" //
				+ "            return Objects.hash(getEnclosingInstance().hashCode(), innerBoolean, innerByte, innerChar, innerDouble,\n" //
				+ "                    innerFloat, innerInt, Arrays.hashCode(innerIntegers), innerLong, innerObservable, innerOtherDouble,\n" //
				+ "                    innerShort, innerText, innerTextById, innerBooleanWrapper, Arrays.hashCode(innerTexts));\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        private E getEnclosingInstance() {\n" //
				+ "            return E.this;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private Map<Integer, String> textById;\n" //
				+ "    private Observable anObservable;\n" //
				+ "    private String aText;\n" //
				+ "    private String[] texts;\n" //
				+ "    private int[] integers;\n" //
				+ "    private char aChar;\n" //
				+ "    private byte aByte;\n" //
				+ "    private boolean aBoolean;\n" //
				+ "    private int anInt;\n" //
				+ "    private long aLong;\n" //
				+ "    private double aDouble;\n" //
				+ "    private short aShort;\n" //
				+ "    private float aFloat;\n" //
				+ "    private double anotherDouble;\n" //
				+ "\n" //
				+ "    @Override\n" //
				+ "    public int hashCode() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return Objects.hash(aBoolean, aByte, aChar, aFloat, aLong,\n" //
				+ "                aDouble, aShort, aText, anInt, anObservable, Arrays.hashCode(integers), textById,\n" //
				+ "                Arrays.hashCode(texts), anotherDouble);\n" //
				+ "    }\n" //
				+ "}\n";
		assertGroupCategoryUsed(new ICompilationUnit[] { cu }, new String[] { MultiFixMessages.HashCleanup_description });
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { output });
	}

	@Test
	public void testKeepHash() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public class DoNotRefactorNewClass {\n" //
				+ "        private boolean innerBoolean;\n" //
				+ "\n" //
				+ "        @Override\n" //
				+ "        public int hashCode() {\n" //
				+ "            final int prime = 31;\n" //
				+ "            int result = 1;\n" //
				+ "            result = prime * result + getEnclosingInstance().hashCode();\n" //
				+ "            result = prime * result + (innerBoolean ? 1231 : 1237);\n" //
				+ "            return result;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        private E getEnclosingInstance() {\n" //
				+ "            return new E();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public class DoNotRefactorCustomHash {\n" //
				+ "        private boolean innerBoolean;\n" //
				+ "\n" //
				+ "        @Override\n" //
				+ "        public int hashCode() {\n" //
				+ "            final int prime = 63;\n" //
				+ "            int result = 1;\n" //
				+ "            result = prime * result + (innerBoolean ? 1231 : 1237);\n" //
				+ "            return result;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private boolean innerBoolean;\n" //
				+ "\n" //
				+ "    @Override\n" //
				+ "    public int hashCode() {\n" //
				+ "        final int prime = 31;\n" //
				+ "        int result = 1;\n" //
				+ "        result += prime * result + (innerBoolean ? 1231 : 1237);\n" //
				+ "        return result;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.MODERNIZE_HASH);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
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

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

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
