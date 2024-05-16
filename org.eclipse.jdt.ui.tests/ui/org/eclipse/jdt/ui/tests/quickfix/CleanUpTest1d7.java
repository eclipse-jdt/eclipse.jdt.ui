/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
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
		String sample= """
			package test1;
			import java.util.ArrayList;
			import java.util.HashMap;
			import java.util.List;
			import java.util.Map;
			public class E {
			    void foo() {
			        new ArrayList<String>().add("a")
			        List<String> a = new ArrayList<String>();
			        Map<Integer, String> m = new HashMap<Integer, String>();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_REDUNDANT_TYPE_ARGUMENTS);

		sample= """
			package test1;
			import java.util.ArrayList;
			import java.util.HashMap;
			import java.util.List;
			import java.util.Map;
			public class E {
			    void foo() {
			        new ArrayList<String>().add("a")
			        List<String> a = new ArrayList<>();
			        Map<Integer, String> m = new HashMap<>();
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testHash() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String input= """
			package test1;
			
			import java.util.Arrays;
			import java.util.Map;
			import java.util.Observable;
			
			public class E {
			    public class RefactoredClass {
			        private Map<Integer, String> innerTextById;
			        private Observable innerObservable;
			        private String innerText;
			        private String[] innerTexts;
			        private int[] innerIntegers;
			        private char innerChar;
			        private byte innerByte;
			        private boolean innerBoolean;
			        private int innerInt;
			        private long innerLong;
			        private double innerDouble;
			        private short innerShort;
			        private float innerFloat;
			        private double innerOtherDouble;
			        private Boolean innerBooleanWrapper;
			
			        @Override
			        public int hashCode() {
			            // Keep this comment
			            final int prime = 31;
			            int result = 1;
			            result = prime * result + getEnclosingInstance().hashCode();
			            result = prime * result + (RefactoredClass.this.innerBoolean ? 1231 : 1237);
			            result = prime * result + this.innerByte;
			            result = prime * result + innerChar;
			            long temp = Double.doubleToLongBits(innerDouble);
			            result = prime * result + (int) ((temp >>> 32) ^ temp);
			            result = prime * result + Float.floatToIntBits(innerFloat);
			            result = result * prime + innerInt;
			            result = prime * result + Arrays.hashCode(innerIntegers);
			            result = prime * result + (int) (innerLong ^ (this.innerLong >>> 32));
			            result = prime * result + ((innerObservable == null) ? 0 : innerObservable.hashCode());
			            temp = Double.doubleToLongBits(innerOtherDouble);
			            result = prime * result + (int) (temp ^ (temp >>> 32));
			            result = prime * result + innerShort;
			            result = prime * result + ((innerText == null) ? 0 : innerText.hashCode());
			            result = prime * result + ((innerTextById != null) ? this.innerTextById.hashCode() : 0);
			            result = prime * result + ((this.innerBooleanWrapper != null) ? innerBooleanWrapper.hashCode() : 0);
			            return prime * result + Arrays.hashCode(innerTexts);
			        }
			
			        private E getEnclosingInstance() {
			            return E.this;
			        }
			    }
			
			    private Map<Integer, String> textById;
			    private Observable anObservable;
			    private String aText;
			    private String[] texts;
			    private int[] integers;
			    private char aChar;
			    private byte aByte;
			    private boolean aBoolean;
			    private int anInt;
			    private long aLong;
			    private double aDouble;
			    private short aShort;
			    private float aFloat;
			    private double anotherDouble;
			
			    @Override
			    public int hashCode() {
			        // Keep this comment
			        final int prime = 31;
			        int result = 1;
			        result = prime * result + (E.this.aBoolean ? 1231 : 1237);
			        result = prime * result + aByte;
			        result = prime * result + aChar;
			        result = prime * result + Float.floatToIntBits(aFloat);
			        result = prime * result + (int) (aLong ^ (aLong >>> 32));
			        long temp;
			        temp = Double.doubleToLongBits(aDouble);
			        result = prime * result + (int) (temp ^ (temp >>> 32));
			        result = prime * result + aShort;
			        result = prime * result + ((null == aText) ? 0 : aText.hashCode());
			        result = prime * result + anInt;
			        result = prime * result + ((anObservable == null) ? 0 : anObservable.hashCode());
			        result = prime * result + Arrays.hashCode(integers);
			        result = prime * result + ((textById == null) ? 0 : textById.hashCode());
			        result = prime * result + Arrays.hashCode(texts);
			        temp = Double.doubleToLongBits(anotherDouble);
			        result = prime * result + (int) (temp ^ (temp >>> 32));
			        return result;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", input, false, null);

		enable(CleanUpConstants.MODERNIZE_HASH);

		String output= """
			package test1;
			
			import java.util.Arrays;
			import java.util.Map;
			import java.util.Objects;
			import java.util.Observable;
			
			public class E {
			    public class RefactoredClass {
			        private Map<Integer, String> innerTextById;
			        private Observable innerObservable;
			        private String innerText;
			        private String[] innerTexts;
			        private int[] innerIntegers;
			        private char innerChar;
			        private byte innerByte;
			        private boolean innerBoolean;
			        private int innerInt;
			        private long innerLong;
			        private double innerDouble;
			        private short innerShort;
			        private float innerFloat;
			        private double innerOtherDouble;
			        private Boolean innerBooleanWrapper;
			
			        @Override
			        public int hashCode() {
			            // Keep this comment
			            return Objects.hash(getEnclosingInstance().hashCode(), innerBoolean, innerByte, innerChar, innerDouble,
			                    innerFloat, innerInt, Arrays.hashCode(innerIntegers), innerLong, innerObservable, innerOtherDouble,
			                    innerShort, innerText, innerTextById, innerBooleanWrapper, Arrays.hashCode(innerTexts));
			        }
			
			        private E getEnclosingInstance() {
			            return E.this;
			        }
			    }
			
			    private Map<Integer, String> textById;
			    private Observable anObservable;
			    private String aText;
			    private String[] texts;
			    private int[] integers;
			    private char aChar;
			    private byte aByte;
			    private boolean aBoolean;
			    private int anInt;
			    private long aLong;
			    private double aDouble;
			    private short aShort;
			    private float aFloat;
			    private double anotherDouble;
			
			    @Override
			    public int hashCode() {
			        // Keep this comment
			        return Objects.hash(aBoolean, aByte, aChar, aFloat, aLong,
			                aDouble, aShort, aText, anInt, anObservable, Arrays.hashCode(integers), textById,
			                Arrays.hashCode(texts), anotherDouble);
			    }
			}
			""";
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { output },
				new HashSet<>(Arrays.asList(MultiFixMessages.HashCleanup_description)));
	}

	@Test
	public void testKeepHash() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E {
			    public class DoNotRefactorNewClass {
			        private boolean innerBoolean;
			
			        @Override
			        public int hashCode() {
			            final int prime = 31;
			            int result = 1;
			            result = prime * result + getEnclosingInstance().hashCode();
			            result = prime * result + (innerBoolean ? 1231 : 1237);
			            return result;
			        }
			
			        private E getEnclosingInstance() {
			            return new E();
			        }
			    }
			
			    public class DoNotRefactorCustomHash {
			        private boolean innerBoolean;
			
			        @Override
			        public int hashCode() {
			            final int prime = 63;
			            int result = 1;
			            result = prime * result + (innerBoolean ? 1231 : 1237);
			            return result;
			        }
			    }
			
			    private boolean innerBoolean;
			
			    @Override
			    public int hashCode() {
			        final int prime = 31;
			        int result = 1;
			        result += prime * result + (innerBoolean ? 1231 : 1237);
			        return result;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.MODERNIZE_HASH);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testObjectsEquals() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.Map;
			import java.util.Observable;
			
			public class E1 {
			    private Map<Integer, String> textById;
			    private Observable anObservable;
			    private String aText;
			
			    /* (non-Javadoc)
			     * @see java.lang.Object#equals(java.lang.Object)
			     */
			    @Override
			    public boolean equals(Object obj) {
			        if (this == obj)
			            return true;
			        if (obj == null)
			            return false;
			        if (getClass() != obj.getClass())
			            return false;
			        E1 other = (E1) obj;
			        if (aText == null) {
			            if (other.aText != null)
			                return false;
			        } else if (!aText.equals(other.aText))
			            return false;
			        if (null == anObservable) {
			            if (null != other.anObservable)
			                return false;
			        } else if (!anObservable.equals(other.anObservable))
			            return false;
			        if (this.textById == null) {
			            if (other.textById != null)
			                return false;
			        } else if (!this.textById.equals(other.textById)) {
			            return false;
			        }
			        return true;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_OBJECTS_EQUALS);

		sample= """
			package test1;
			
			import java.util.Map;
			import java.util.Objects;
			import java.util.Observable;
			
			public class E1 {
			    private Map<Integer, String> textById;
			    private Observable anObservable;
			    private String aText;
			
			    /* (non-Javadoc)
			     * @see java.lang.Object#equals(java.lang.Object)
			     */
			    @Override
			    public boolean equals(Object obj) {
			        if (this == obj)
			            return true;
			        if (obj == null)
			            return false;
			        if (getClass() != obj.getClass())
			            return false;
			        E1 other = (E1) obj;
			        if (!Objects.equals(aText, other.aText)) {
			            return false;
			        }
			        if (!Objects.equals(anObservable, other.anObservable)) {
			            return false;
			        }
			        if (!Objects.equals(this.textById, other.textById)) {
			            return false;
			        }
			        return true;
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testDoNotRefactorObjectsEquals() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.Map;
			import java.util.Observable;
			
			public class E1 {
			    private Map<Integer, String> textById;
			    private Observable anObservable;
			    private String aText;
			
			    /* (non-Javadoc)
			     * @see java.lang.Object#equals(java.lang.Object)
			     */
			    @Override
			    public boolean equals(Object obj) {
			        if (this == obj)
			            return true;
			        if (obj == null)
			            return false;
			        if (getClass() != obj.getClass())
			            return false;
			        E1 other = (E1) obj;
			        if (aText == null) {
			            if (other.aText != null)
			                return true;
			        } else if (!aText.equals(other.aText))
			            return false;
			        if (null == anObservable) {
			            if (null != other.anObservable)
			                return false;
			        } else if (!anObservable.equals(other.anObservable))
			            return true;
			        if (this.textById == null) {
			            if (other.textById != null)
			                return false;
			        } else if (this.textById.equals(other.textById))
			            return false;
			        return true;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_OBJECTS_EQUALS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
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
			        final FileInputStream inputStream = new FileInputStream("out.txt");
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
			
			    public void refactorNullInitializedResourceRemoveFinally() throws Exception {
			        // Keep this comment
			        FileInputStream inputStream = null;
			        // Keep this comment too
			        try {
			            inputStream = new FileInputStream("out.txt");
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
			            inputStream = new FileInputStream("out.txt");
			            System.out.println(inputStream.read());
			        } finally {
			            if (inputStream != null) {
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
			        // Keep this comment too
			        try (FileInputStream inputStream = new FileInputStream("out.txt")) {
			            System.out.println(inputStream.read());
			        }
			    }
			
			    public void refactorFullyInitializedResourceDoNotRemoveFinally() throws Exception {
			        // Keep this comment
			        // Keep this comment too
			        try (FileInputStream inputStream = new FileInputStream("out.txt")) {
			            System.out.println(inputStream.read());
			        } finally {
			            System.out.println("Done");
			        }
			    }
			
			    public void refactorNullInitializedResourceRemoveFinally() throws Exception {
			        // Keep this comment
			        // Keep this comment too
			        try (FileInputStream inputStream = new FileInputStream("out.txt")) {
			            System.out.println(inputStream.read());
			        }
			    }
			
			    public void refactorNullInitializedResourceDoNotRemoveFinally() throws Exception {
			        // Keep this comment
			        // Keep this comment too
			        try (FileInputStream inputStream = new FileInputStream("out.txt")) {
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
			    public void doNotRefactorNonEffectivelyFinalResource() throws Exception {
			        try (FileInputStream inputStream = new FileInputStream("out.txt")) {
			            System.out.println(inputStream.read());
			        }
			    }
			
			    public void doNotRefactorFurtherAssignmentsToResource() throws Exception {
			        FileInputStream inputStream = null;
			        try {
			            inputStream = new FileInputStream("out.txt");
			            System.out.println(inputStream.read());
			            inputStream = new FileInputStream("out.txt");
			        } finally {
			            inputStream.close();
			        }
			    }
			
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
			
			    public void doNotRefactorUnrelated() throws Exception {
			        FileInputStream aStream = new FileInputStream("out.txt");
			        Object o = null;
			        try {
			            o = aStream.read();
			        } finally {
			            aStream.close();
			        }
			    }
			
			    public void doNotRefactorUnclosedStream(int i) throws Exception {
			        FileInputStream inputStream = null;
			        try {
			            inputStream = new FileInputStream("out.txt");
			            System.out.println(inputStream.read());
			        } finally {
			            if (inputStream != null) {
			                i = inputStream.available();
			            }
			        }
			    }
			
			    public void doNotMoveVariableFromOtherScope(boolean isValid) throws Exception {
			        final FileInputStream inputStream = new FileInputStream("out.txt");
			        if (isValid) {
			            try {
			                System.out.println(inputStream.read());
			            } finally {
			                inputStream.close();
			            }
			        }
			    }
			
			    public void doNotMoveReusedVariable() throws Exception {
			        final FileInputStream inputStream = new FileInputStream("out.txt");
			        try {
			            System.out.println(inputStream.read());
			        } finally {
			            inputStream.close();
			        }
			
			        inputStream.getFD();
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.TRY_WITH_RESOURCE);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testMultiCatch() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;
			
			import java.io.IOException;
			
			public class E {
			    private static final class ThrowingObject<E1 extends Throwable, E2 extends Throwable> {
			        private void throwingMethod() throws E1, E2 {
			        }
			    }
			
			    private static final class Ex1 extends Exception {
			        private void print() {
			        }
			
			        private String getExplanation() {
			            return "";
			        }
			    }
			
			    private static final class Ex2 extends Exception {
			        private void print() {
			        }
			    }
			
			    private static final class OverridingException1 extends Exception {
			        @Override
			        public void printStackTrace() {
			            super.printStackTrace();
			        }
			    }
			
			    private static final class OverridingException2 extends Exception {
			        @Override
			        public void printStackTrace() {
			            super.printStackTrace();
			        }
			    }
			
			    public void refactorMultiCatch(ThrowingObject<IllegalArgumentException, IOException> obj) {
			        try {
			            obj.throwingMethod();
			        } catch (IllegalArgumentException iae) {
			            iae.printStackTrace();
			        } catch (IOException ioe) {
			            ioe.printStackTrace();
			        }
			    }
			
			    public void refactorAddToMultiCatch(ThrowingObject<IllegalArgumentException, IOException> obj) {
			        try {
			            obj.throwingMethod();
			        } catch (IllegalArgumentException | IllegalStateException iae) {
			            iae.printStackTrace();
			        } catch (IOException ioe) {
			            ioe.printStackTrace();
			        }
			    }
			
			    public void removeMoreSpecializedException(ThrowingObject<IllegalArgumentException, RuntimeException> obj) {
			        try {
			            obj.throwingMethod();
			        } catch (IllegalArgumentException iae) {
			            iae.printStackTrace();
			        } catch (RuntimeException re) {
			            re.printStackTrace();
			        }
			    }
			
			    public void refactorMultiCatchWithOverridenMethods(ThrowingObject<IllegalArgumentException, OverridingException1> obj) {
			        try {
			            obj.throwingMethod();
			        } catch (IllegalArgumentException iae) {
			            iae.printStackTrace();
			        } catch (OverridingException1 oe1) {
			            oe1.printStackTrace();
			        }
			    }
			
			    public void refactorMultiCatchWithOverridenMethodsFromSupertype(ThrowingObject<OverridingException1, OverridingException2> obj) {
			        try {
			            obj.throwingMethod();
			        } catch (OverridingException1 oe1) {
			            oe1.printStackTrace();
			        } catch (OverridingException2 oe2) {
			            oe2.printStackTrace();
			        }
			    }
			
			    public void refactorUp(ThrowingObject<IllegalArgumentException, IllegalAccessException> obj) {
			        try {
			            obj.throwingMethod();
			        } catch (IllegalArgumentException iae) {
			            iae.printStackTrace();
			        } catch (RuntimeException re) {
			            re.toString();
			        } catch (IllegalAccessException ne) {
			            ne.printStackTrace();
			        }
			    }
			
			    public void refactorDown(ThrowingObject<IllegalAccessException, RuntimeException> obj, int errorCount) {
			        try {
			            obj.throwingMethod();
			        } catch (IllegalAccessException iae) {
			            errorCount++;
			            iae.printStackTrace();
			        } catch (RuntimeException ioe) {
			            errorCount++;
			            ioe.toString();
			        } catch (Exception e) {
			            errorCount = errorCount + 1;
			            e.printStackTrace();
			        }
			        System.out.println("Error count: " + errorCount);
			    }
			
			    public void refactorMultiCatchWithLocalVariables(ThrowingObject<IllegalArgumentException, IOException> obj) {
			        try {
			            obj.throwingMethod();
			        } catch (IllegalArgumentException iae) {
			            String s = "[" + iae;
			            String s1 = "]";
			            System.out.println(s + s1);
			        } catch (IOException ioe) {
			            String s = "[" + ioe;
			            String s2 = "]";
			            System.out.println(s + s2);
			        }
			    }
			
			    public class EA extends Exception {}
			    public class EB extends Exception {}
			    public class EB1 extends EB {}
			    public class EC extends Exception {}
			
			    public String refactorUp2() {
			        try {
			            return throwingMethod();
			        } catch (EA | EB1 e) {
			            throw new RuntimeException("v1", e);
			        } catch (EB e) {
			            throw new RuntimeException("v2", e);
			        } catch (EC e) {
			            throw new RuntimeException("v1", e);
			        }
			    }
			
			    private String throwingMethod() throws EA, EB1, EB, EC {
			        return null;
			    }
			}
			""";

		String expected= """
			package test1;
			
			import java.io.IOException;
			
			public class E {
			    private static final class ThrowingObject<E1 extends Throwable, E2 extends Throwable> {
			        private void throwingMethod() throws E1, E2 {
			        }
			    }
			
			    private static final class Ex1 extends Exception {
			        private void print() {
			        }
			
			        private String getExplanation() {
			            return "";
			        }
			    }
			
			    private static final class Ex2 extends Exception {
			        private void print() {
			        }
			    }
			
			    private static final class OverridingException1 extends Exception {
			        @Override
			        public void printStackTrace() {
			            super.printStackTrace();
			        }
			    }
			
			    private static final class OverridingException2 extends Exception {
			        @Override
			        public void printStackTrace() {
			            super.printStackTrace();
			        }
			    }
			
			    public void refactorMultiCatch(ThrowingObject<IllegalArgumentException, IOException> obj) {
			        try {
			            obj.throwingMethod();
			        } catch (IllegalArgumentException | IOException ioe) {
			            ioe.printStackTrace();
			        }
			    }
			
			    public void refactorAddToMultiCatch(ThrowingObject<IllegalArgumentException, IOException> obj) {
			        try {
			            obj.throwingMethod();
			        } catch (IllegalArgumentException | IllegalStateException | IOException ioe) {
			            ioe.printStackTrace();
			        }
			    }
			
			    public void removeMoreSpecializedException(ThrowingObject<IllegalArgumentException, RuntimeException> obj) {
			        try {
			            obj.throwingMethod();
			        } catch (RuntimeException re) {
			            re.printStackTrace();
			        }
			    }
			
			    public void refactorMultiCatchWithOverridenMethods(ThrowingObject<IllegalArgumentException, OverridingException1> obj) {
			        try {
			            obj.throwingMethod();
			        } catch (IllegalArgumentException | OverridingException1 oe1) {
			            oe1.printStackTrace();
			        }
			    }
			
			    public void refactorMultiCatchWithOverridenMethodsFromSupertype(ThrowingObject<OverridingException1, OverridingException2> obj) {
			        try {
			            obj.throwingMethod();
			        } catch (OverridingException1 | OverridingException2 oe2) {
			            oe2.printStackTrace();
			        }
			    }
			
			    public void refactorUp(ThrowingObject<IllegalArgumentException, IllegalAccessException> obj) {
			        try {
			            obj.throwingMethod();
			        } catch (IllegalArgumentException | IllegalAccessException iae) {
			            iae.printStackTrace();
			        } catch (RuntimeException re) {
			            re.toString();
			        }
			    }
			
			    public void refactorDown(ThrowingObject<IllegalAccessException, RuntimeException> obj, int errorCount) {
			        try {
			            obj.throwingMethod();
			        } catch (RuntimeException ioe) {
			            errorCount++;
			            ioe.toString();
			        } catch (Exception e) {
			            errorCount = errorCount + 1;
			            e.printStackTrace();
			        }
			        System.out.println("Error count: " + errorCount);
			    }
			
			    public void refactorMultiCatchWithLocalVariables(ThrowingObject<IllegalArgumentException, IOException> obj) {
			        try {
			            obj.throwingMethod();
			        } catch (IllegalArgumentException | IOException ioe) {
			            String s = "[" + ioe;
			            String s2 = "]";
			            System.out.println(s + s2);
			        }
			    }
			
			    public class EA extends Exception {}
			    public class EB extends Exception {}
			    public class EB1 extends EB {}
			    public class EC extends Exception {}
			
			    public String refactorUp2() {
			        try {
			            return throwingMethod();
			        } catch (EA | EB1 | EC e) {
			            throw new RuntimeException("v1", e);
			        } catch (EB e) {
			            throw new RuntimeException("v2", e);
			        }
			    }
			
			    private String throwingMethod() throws EA, EB1, EB, EC {
			        return null;
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.MULTI_CATCH);

		// Then
		assertNotEquals("The class must be changed", given, expected);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.MultiCatchCleanUp_description)));
	}

	@Test
	public void testDoNotUseMultiCatch() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E {
			    private static final class MyException extends RuntimeException {
			        private static final long serialVersionUID = 1L;
			
			        private MyException(Ex1 ex1) {
			        }
			
			        private MyException(Ex2 ex2) {
			        }
			    }
			
			    private static final class ThrowingObject<E1 extends Throwable, E2 extends Throwable> {
			        private void throwingMethod() throws E1, E2 {
			        }
			    }
			
			    private static final class Ex1 extends Exception {
			        private static final long serialVersionUID = 1L;
			
			        private void print() {
			        }
			
			        private String getExplanation() {
			            return "";
			        }
			    }
			
			    private static final class Ex2 extends Exception {
			        private static final long serialVersionUID = 1L;
			
			        private void print() {
			        }
			    }
			
			    private static final class Ex3 extends Exception {
			        private static final long serialVersionUID = 1L;
			
			        private void print() {
			        }
			
			        private String getExplanation() {
			            return "";
			        }
			    }
			
			    public void doNotRefactorMultiCatchWithNoOverridenMethods(ThrowingObject<Ex3, Ex1> obj) {
			        try {
			            obj.throwingMethod();
			        } catch (Ex3 ne) {
			            ne.getExplanation();
			        } catch (Ex1 ex1) {
			            ex1.getExplanation();
			        }
			    }
			
			    public void doNotRefactorNoCommonSuperType(ThrowingObject<Ex1, Ex2> obj) {
			        try {
			            obj.throwingMethod();
			        } catch (Ex1 e1) {
			            e1.print();
			        } catch (Ex2 e2) {
			            e2.print();
			        }
			    }
			
			    public void doNotRefactorChangeInBehaviourClassHierarchy(ThrowingObject<IllegalArgumentException, Exception> obj) {
			        try {
			            obj.throwingMethod();
			        } catch (IllegalArgumentException iae) {
			            iae.printStackTrace();
			        } catch (Exception ioe) {
			            ioe.toString();
			        } catch (Throwable t) {
			            t.printStackTrace();
			        }
			    }
			
			    public void doNotRefactorMultiCatchWhenMethodDoesNotCallCommonSupertype(ThrowingObject<Ex1, Ex2> object) {
			        try {
			            object.throwingMethod();
			        } catch (Ex1 ex1) {
			            throw new MyException(ex1);
			        } catch (Ex2 ex2) {
			            throw new MyException(ex2);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.MULTI_CATCH);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testObjectsEqualsWithImportConflict() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.Map;
			import java.util.Observable;
			
			public class Objects {
			    private Map<Integer, String> textById;
			    private Observable anObservable;
			    private String aText;
			
			    /* (non-Javadoc)
			     * @see java.lang.Object#equals(java.lang.Object)
			     */
			    @Override
			    public boolean equals(Object obj) {
			        if (this == obj)
			            return true;
			        if (obj == null)
			            return false;
			        if (getClass() != obj.getClass())
			            return false;
			        Objects other = (Objects) obj;
			        if (aText == null) {
			            if (other.aText != null)
			                return false;
			        } else if (!aText.equals(other.aText))
			            return false;
			        if (null == anObservable) {
			            if (null != other.anObservable)
			                return false;
			        } else if (!anObservable.equals(other.anObservable))
			            return false;
			        if (this.textById == null) {
			            if (other.textById != null)
			                return false;
			        } else if (!this.textById.equals(other.textById))
			            return false;
			        return true;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("Objects.java", sample, false, null);

		enable(CleanUpConstants.USE_OBJECTS_EQUALS);

		sample= """
			package test1;
			
			import java.util.Map;
			import java.util.Observable;
			
			public class Objects {
			    private Map<Integer, String> textById;
			    private Observable anObservable;
			    private String aText;
			
			    /* (non-Javadoc)
			     * @see java.lang.Object#equals(java.lang.Object)
			     */
			    @Override
			    public boolean equals(Object obj) {
			        if (this == obj)
			            return true;
			        if (obj == null)
			            return false;
			        if (getClass() != obj.getClass())
			            return false;
			        Objects other = (Objects) obj;
			        if (!java.util.Objects.equals(aText, other.aText)) {
			            return false;
			        }
			        if (!java.util.Objects.equals(anObservable, other.anObservable)) {
			            return false;
			        }
			        if (!java.util.Objects.equals(this.textById, other.textById)) {
			            return false;
			        }
			        return true;
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoop563267() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=563267
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=565282
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.io.IOException;
			import java.io.InputStream;
			import java.util.Iterator;
			import java.util.List;
			public class E1 {
			    public void foo(List<InputStream> toClose) throws IOException {
			        for (Iterator<InputStream> it = toClose.iterator(); it.hasNext();) {
			            try (InputStream r = it.next()) {
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			import java.io.IOException;
			import java.io.InputStream;
			import java.util.List;
			public class E1 {
			    public void foo(List<InputStream> toClose) throws IOException {
			        for (InputStream inputStream : toClose) {
			            try (InputStream r = inputStream) {
			            }
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 },
				new HashSet<>(Arrays.asList(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description)));
	}

	@Test
	public void testInstanceVarToLocal() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class Bar {
			    private transient int zoz=38, fubu;
			   \s
			    public void baz() {
			        zoz = 37;
			        System.out.println(zoz);
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("Bar.java", sample, false, null);

		enable(CleanUpConstants.SINGLE_USED_FIELD);


		String expected=  """
			package test1;
			
			public class Bar {
			    private transient int fubu;
			   \s
			    public void baz() {
			        int zoz = 37;
			        System.out.println(zoz);
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}
}
