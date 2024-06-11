/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *     Mateusz Matela <mateusz.matela@gmail.com> - [formatter] Formatter does not format Java code correctly, especially when max line width is set
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

public class CleanUpStressTest extends CleanUpTestCase {

	@Rule
    public ProjectTestSetup projectSetup = new ProjectTestSetup();

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	private static final String SRC_CONTAINER= "src";

	protected static IPackageFragmentRoot fJunitSrcRoot;

	@Override
	public void setUp() throws Exception {
		super.setUp();

		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);
		fJunitSrcRoot= JavaProjectHelper.addSourceContainerWithImport(getProject(), SRC_CONTAINER, junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);
	}

	private void addAllCUs(IJavaElement[] children, List<IJavaElement> result) throws JavaModelException {
		for (IJavaElement element : children) {
			if (element instanceof ICompilationUnit) {
				result.add(element);
			} else if (element instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot root= (IPackageFragmentRoot)element;
				addAllCUs(root.getChildren(), result);
			} else if (element instanceof IPackageFragment) {
				IPackageFragment pack= (IPackageFragment)element;
				addAllCUs(pack.getChildren(), result);
			}
		}
	}

    private Hashtable<String, String> fExpectedChangesAllTests;
    {
        fExpectedChangesAllTests= new Hashtable<>();
        String str= """
			package junit.runner;
			
			import java.io.BufferedReader;
			import java.io.File;
			import java.io.FileInputStream;
			import java.io.FileOutputStream;
			import java.io.IOException;
			import java.io.InputStream;
			import java.io.PrintWriter;
			import java.io.StringReader;
			import java.io.StringWriter;
			import java.lang.reflect.InvocationTargetException;
			import java.lang.reflect.Method;
			import java.lang.reflect.Modifier;
			import java.text.NumberFormat;
			import java.util.Properties;
			
			import junit.framework.AssertionFailedError;
			import junit.framework.Test;
			import junit.framework.TestListener;
			import junit.framework.TestSuite;
			
			/**
			 * Base class for all test runners. This class was born live on stage in
			 * Sardinia during XP2000.
			 */
			public abstract class BaseTestRunner implements TestListener {
			    static boolean fgFilterStack = true;
			
			    static int fgMaxMessageLength = 500;
			    private static Properties fPreferences;
			    public static final String SUITE_METHODNAME = "suite"; //$NON-NLS-1$
			    static {
			        BaseTestRunner.fgMaxMessageLength = BaseTestRunner
			                .getPreference("maxmessage", BaseTestRunner.fgMaxMessageLength); //$NON-NLS-1$
			    }
			
			    static boolean filterLine(final String line) {
			        final String[] patterns = new String[]{"junit.framework.TestCase", //$NON-NLS-1$
			                "junit.framework.TestResult", //$NON-NLS-1$
			                "junit.framework.TestSuite", //$NON-NLS-1$
			                "junit.framework.Assert.", // don't filter //$NON-NLS-1$
			                                           // AssertionFailure
			                "junit.swingui.TestRunner", //$NON-NLS-1$
			                "junit.awtui.TestRunner", //$NON-NLS-1$
			                "junit.textui.TestRunner", //$NON-NLS-1$
			                "java.lang.reflect.Method.invoke(" //$NON-NLS-1$
			        };
			        for (final String pattern : patterns) {
			            if (line.indexOf(pattern) > 0) {
			                return true;
			            }
			        }
			        return false;
			    }
			
			    /**
			     * Filters stack frames from internal JUnit classes
			     */
			    public static String getFilteredTrace(final String stack) {
			        if (BaseTestRunner.showStackRaw()) {
			            return stack;
			        }
			
			        final StringWriter sw = new StringWriter();
			        final PrintWriter pw = new PrintWriter(sw);
			        final StringReader sr = new StringReader(stack);
			        final BufferedReader br = new BufferedReader(sr);
			
			        String line;
			        try {
			            while ((line = br.readLine()) != null) {
			                if (!BaseTestRunner.filterLine(line)) {
			                    pw.println(line);
			                }
			            }
			        } catch (final Exception IOException) {
			            return stack; // return the stack unfiltered
			        }
			        return sw.toString();
			    }
			
			    /**
			     * Returns a filtered stack trace
			     */
			    public static String getFilteredTrace(final Throwable t) {
			        final StringWriter stringWriter = new StringWriter();
			        final PrintWriter writer = new PrintWriter(stringWriter);
			        t.printStackTrace(writer);
			        final StringBuffer buffer = stringWriter.getBuffer();
			        final String trace = buffer.toString();
			        return BaseTestRunner.getFilteredTrace(trace);
			    }
			
			    public static String getPreference(final String key) {
			        return BaseTestRunner.getPreferences().getProperty(key);
			    }
			
			    public static int getPreference(final String key, final int dflt) {
			        final String value = BaseTestRunner.getPreference(key);
			        int intValue = dflt;
			        if (value == null) {
			            return intValue;
			        }
			        try {
			            intValue = Integer.parseInt(value);
			        } catch (final NumberFormatException ne) {
			        }
			        return intValue;
			    }
			
			    protected static Properties getPreferences() {
			        if (BaseTestRunner.fPreferences == null) {
			            BaseTestRunner.fPreferences = new Properties();
			            BaseTestRunner.fPreferences.put("loading", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			            BaseTestRunner.fPreferences.put("filterstack", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			            BaseTestRunner.readPreferences();
			        }
			        return BaseTestRunner.fPreferences;
			    }
			
			    private static File getPreferencesFile() {
			        final String home = System.getProperty("user.home"); //$NON-NLS-1$
			        return new File(home, "junit.properties"); //$NON-NLS-1$
			    }
			
			    public static boolean inVAJava() {
			        try {
			            Class.forName("com.ibm.uvm.tools.DebugSupport"); //$NON-NLS-1$
			        } catch (final Exception e) {
			            return false;
			        }
			        return true;
			    }
			
			    // TestRunListener implementation
			
			    private static void readPreferences() {
			        InputStream is = null;
			        try {
			            is = new FileInputStream(BaseTestRunner.getPreferencesFile());
			            BaseTestRunner.setPreferences(
			                    new Properties(BaseTestRunner.getPreferences()));
			            BaseTestRunner.getPreferences().load(is);
			        } catch (final IOException e) {
			            try {
			                if (is != null) {
			                    is.close();
			                }
			            } catch (final IOException e1) {
			            }
			        }
			    }
			
			    public static void savePreferences() throws IOException {
			        final FileOutputStream fos = new FileOutputStream(
			                BaseTestRunner.getPreferencesFile());
			        try {
			            BaseTestRunner.getPreferences().store(fos, ""); //$NON-NLS-1$
			        } finally {
			            fos.close();
			        }
			    }
			
			    protected static void setPreferences(final Properties preferences) {
			        BaseTestRunner.fPreferences = preferences;
			    }
			
			    protected static boolean showStackRaw() {
			        return !BaseTestRunner.getPreference("filterstack").equals("true") //$NON-NLS-1$ //$NON-NLS-2$
			                || (BaseTestRunner.fgFilterStack == false);
			    }
			
			    /**
			     * Truncates a String to the maximum length.
			     */
			    public static String truncate(String s) {
			        if ((BaseTestRunner.fgMaxMessageLength != -1)
			                && (s.length() > BaseTestRunner.fgMaxMessageLength)) {
			            s = s.substring(0, BaseTestRunner.fgMaxMessageLength) + "..."; //$NON-NLS-1$
			        }
			        return s;
			    }
			
			    boolean fLoading = true;
			
			    public synchronized void addError(final Test test, final Throwable t) {
			        this.testFailed(TestRunListener.STATUS_ERROR, test, t);
			    }
			    public synchronized void addFailure(final Test test,
			            final AssertionFailedError t) {
			        this.testFailed(TestRunListener.STATUS_FAILURE, test, t);
			    }
			
			    /**
			     * Clears the status message.
			     */
			    protected void clearStatus() { // Belongs in the GUI TestRunner class
			    }
			
			    /**
			     * Returns the formatted string of the elapsed time.
			     */
			    public String elapsedTimeAsString(final long runTime) {
			        return NumberFormat.getInstance().format((double) runTime / 1000);
			    }
			
			    public synchronized void endTest(final Test test) {
			        this.testEnded(test.toString());
			    }
			
			    /**
			     * Extract the class name from a String in VA/Java style
			     */
			    public String extractClassName(final String className) {
			        if (className.startsWith("Default package for")) { // $NON-NLS-1$
			            return className.substring(className.lastIndexOf(".") + 1); //$NON-NLS-1$
			        }
			        return className;
			    }
			
			    /**
			     * Returns the loader to be used.
			     */
			    public TestSuiteLoader getLoader() {
			        if (this.useReloadingTestSuiteLoader()) {
			            return new ReloadingTestSuiteLoader();
			        }
			        return new StandardTestSuiteLoader();
			    }
			
			    /**
			     * Returns the Test corresponding to the given suite. This is a template
			     * method, subclasses override runFailed(), clearStatus().
			     */
			    public Test getTest(final String suiteClassName) {
			        if (suiteClassName.length() <= 0) {
			            this.clearStatus();
			            return null;
			        }
			        Class testClass = null;
			        try {
			            testClass = this.loadSuiteClass(suiteClassName);
			        } catch (final ClassNotFoundException e) {
			            String clazz = e.getMessage();
			            if (clazz == null) {
			                clazz = suiteClassName;
			            }
			            this.runFailed("Class not found \\"" + clazz + "\\""); //$NON-NLS-1$ //$NON-NLS-2$
			            return null;
			        } catch (final Exception e) {
			            this.runFailed("Error: " + e.toString()); //$NON-NLS-1$
			            return null;
			        }
			        Method suiteMethod = null;
			        try {
			            suiteMethod = testClass.getMethod(BaseTestRunner.SUITE_METHODNAME,
			                    new Class[0]);
			        } catch (final Exception e) {
			            // try to extract a test suite automatically
			            this.clearStatus();
			            return new TestSuite(testClass);
			        }
			        if (!Modifier.isStatic(suiteMethod.getModifiers())) {
			            this.runFailed("Suite() method must be static"); //$NON-NLS-1$
			            return null;
			        }
			        Test test = null;
			        try {
			            test = (Test) suiteMethod.invoke(null, new Class[0]); // static
			                                                                  // method
			            if (test == null) {
			                return test;
			            }
			        } catch (final InvocationTargetException e) {
			            this.runFailed("Failed to invoke suite():" //$NON-NLS-1$
			                    + e.getTargetException().toString());
			            return null;
			        } catch (final IllegalAccessException e) {
			            this.runFailed("Failed to invoke suite():" + e.toString()); //$NON-NLS-1$
			            return null;
			        }
			
			        this.clearStatus();
			        return test;
			    }
			
			    /**
			     * Returns the loaded Class for a suite name.
			     */
			    protected Class loadSuiteClass(final String suiteClassName)
			            throws ClassNotFoundException {
			        return this.getLoader().load(suiteClassName);
			    }
			
			    /**
			     * Processes the command line arguments and returns the name of the suite
			     * class to run or null
			     */
			    protected String processArguments(final String[] args) {
			        String suiteName = null;
			        for (int i = 0; i < args.length; i++) {
			            if (args[i].equals("-noloading")) { //$NON-NLS-1$
			                this.setLoading(false);
			            } else if (args[i].equals("-nofilterstack")) { //$NON-NLS-1$
			                BaseTestRunner.fgFilterStack = false;
			            } else if (args[i].equals("-c")) { //$NON-NLS-1$
			                if (args.length > (i + 1)) {
			                    suiteName = this.extractClassName(args[i + 1]);
			                } else {
			                    System.out.println("Missing Test class name"); //$NON-NLS-1$
			                }
			                i++;
			            } else {
			                suiteName = args[i];
			            }
			        }
			        return suiteName;
			    }
			
			    /**
			     * Override to define how to handle a failed loading of a test suite.
			     */
			    protected abstract void runFailed(String message);
			
			    /**
			     * Sets the loading behaviour of the test runner
			     */
			    public void setLoading(final boolean enable) {
			        this.fLoading = enable;
			    }
			
			    public void setPreference(final String key, final String value) {
			        BaseTestRunner.getPreferences().setProperty(key, value);
			    }
			
			    /*
			     * Implementation of TestListener
			     */
			    public synchronized void startTest(final Test test) {
			        this.testStarted(test.toString());
			    }
			
			    public abstract void testEnded(String testName);
			
			    public abstract void testFailed(int status, Test test, Throwable t);
			
			    public abstract void testStarted(String testName);
			
			    protected boolean useReloadingTestSuiteLoader() {
			        return BaseTestRunner.getPreference("loading").equals("true") //$NON-NLS-1$ //$NON-NLS-2$
			                && !BaseTestRunner.inVAJava() && this.fLoading;
			    }
			
			}""";
        fExpectedChangesAllTests.put("junit.runner.BaseTestRunner.java", str);
        String str1= """
			package junit.tests.framework;
			
			/**
			 * Test class used in SuiteTest
			 */
			import junit.framework.TestCase;
			
			public class NotVoidTestCase extends TestCase {
			    public int testNotVoid() {
			        return 1;
			    }
			    public void testVoid() {
			    }
			}""";
        fExpectedChangesAllTests.put("junit.tests.framework.NotVoidTestCase.java", str1);
        String str2= """
			package junit.tests.runner;
			
			import java.io.PrintWriter;
			import java.io.StringWriter;
			
			import junit.framework.Assert;
			import junit.framework.TestCase;
			import junit.runner.BaseTestRunner;
			
			public class StackFilterTest extends TestCase {
			    String fFiltered;
			    String fUnfiltered;
			
			    @Override
			    protected void setUp() {
			        final StringWriter swin = new StringWriter();
			        final PrintWriter pwin = new PrintWriter(swin);
			        pwin.println("junit.framework.AssertionFailedError"); //$NON-NLS-1$
			        pwin.println("    at junit.framework.Assert.fail(Assert.java:144)"); //$NON-NLS-1$
			        pwin.println("    at junit.framework.Assert.assert(Assert.java:19)"); //$NON-NLS-1$
			        pwin.println("    at junit.framework.Assert.assert(Assert.java:26)"); //$NON-NLS-1$
			        pwin.println("    at MyTest.f(MyTest.java:13)"); //$NON-NLS-1$
			        pwin.println("    at MyTest.testStackTrace(MyTest.java:8)"); //$NON-NLS-1$
			        pwin.println("    at java.lang.reflect.Method.invoke(Native Method)"); //$NON-NLS-1$
			        pwin.println(
			                "    at junit.framework.TestCase.runTest(TestCase.java:156)"); //$NON-NLS-1$
			        pwin.println(
			                "    at junit.framework.TestCase.runBare(TestCase.java:130)"); //$NON-NLS-1$
			        pwin.println(
			                "    at junit.framework.TestResult$1.protect(TestResult.java:100)"); //$NON-NLS-1$
			        pwin.println(
			                "    at junit.framework.TestResult.runProtected(TestResult.java:118)"); //$NON-NLS-1$
			        pwin.println(
			                "    at junit.framework.TestResult.run(TestResult.java:103)"); //$NON-NLS-1$
			        pwin.println("    at junit.framework.TestCase.run(TestCase.java:121)"); //$NON-NLS-1$
			        pwin.println(
			                "    at junit.framework.TestSuite.runTest(TestSuite.java:157)"); //$NON-NLS-1$
			        pwin.println(
			                "    at junit.framework.TestSuite.run(TestSuite.java, Compiled Code)"); //$NON-NLS-1$
			        pwin.println(
			                "    at junit.swingui.TestRunner$17.run(TestRunner.java:669)"); //$NON-NLS-1$
			        this.fUnfiltered = swin.toString();
			
			        final StringWriter swout = new StringWriter();
			        final PrintWriter pwout = new PrintWriter(swout);
			        pwout.println("junit.framework.AssertionFailedError"); //$NON-NLS-1$
			        pwout.println("    at MyTest.f(MyTest.java:13)"); //$NON-NLS-1$
			        pwout.println("    at MyTest.testStackTrace(MyTest.java:8)"); //$NON-NLS-1$
			        this.fFiltered = swout.toString();
			    }
			
			    public void testFilter() {
			        Assert.assertEquals(this.fFiltered,
			                BaseTestRunner.getFilteredTrace(this.fUnfiltered));
			    }
			}""";
        fExpectedChangesAllTests.put("junit.tests.runner.StackFilterTest.java", str2);
        String str3= """
			package junit.tests.framework;
			
			import junit.framework.Assert;
			import junit.framework.AssertionFailedError;
			import junit.framework.TestCase;
			
			public class DoublePrecisionAssertTest extends TestCase {
			
			    /**
			     * Test for the special Double.NaN value.
			     */
			    public void testAssertEqualsNaNFails() {
			        try {
			            Assert.assertEquals(1.234, Double.NaN, 0.0);
			        } catch (final AssertionFailedError e) {
			            return;
			        }
			        Assert.fail();
			    }
			
			    public void testAssertNaNEqualsFails() {
			        try {
			            Assert.assertEquals(Double.NaN, 1.234, 0.0);
			        } catch (final AssertionFailedError e) {
			            return;
			        }
			        Assert.fail();
			    }
			
			    public void testAssertNaNEqualsNaNFails() {
			        try {
			            Assert.assertEquals(Double.NaN, Double.NaN, 0.0);
			        } catch (final AssertionFailedError e) {
			            return;
			        }
			        Assert.fail();
			    }
			
			    public void testAssertNegInfinityEqualsInfinity() {
			        Assert.assertEquals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
			                0.0);
			    }
			
			    public void testAssertPosInfinityEqualsInfinity() {
			        Assert.assertEquals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
			                0.0);
			    }
			
			    public void testAssertPosInfinityNotEquals() {
			        try {
			            Assert.assertEquals(Double.POSITIVE_INFINITY, 1.23, 0.0);
			        } catch (final AssertionFailedError e) {
			            return;
			        }
			        Assert.fail();
			    }
			
			    public void testAssertPosInfinityNotEqualsNegInfinity() {
			        try {
			            Assert.assertEquals(Double.POSITIVE_INFINITY,
			                    Double.NEGATIVE_INFINITY, 0.0);
			        } catch (final AssertionFailedError e) {
			            return;
			        }
			        Assert.fail();
			    }
			
			}
			""";
        fExpectedChangesAllTests.put("junit.tests.framework.DoublePrecisionAssertTest.java", str3);
        String str4= """
			package junit.tests.framework;
			
			import junit.framework.Assert;
			import junit.framework.AssertionFailedError;
			import junit.framework.ComparisonFailure;
			import junit.framework.TestCase;
			
			public class AssertTest extends TestCase {
			
			    public void testAssertEquals() {
			        final Object o = new Object();
			        Assert.assertEquals(o, o);
			        try {
			            Assert.assertEquals(new Object(), new Object());
			        } catch (final AssertionFailedError e) {
			            return;
			        }
			        Assert.fail();
			    }
			
			    public void testAssertEqualsNull() {
			        Assert.assertEquals(null, null);
			    }
			
			    public void testAssertFalse() {
			        Assert.assertFalse(false);
			        try {
			            Assert.assertFalse(true);
			        } catch (final AssertionFailedError e) {
			            return;
			        }
			        Assert.fail();
			    }
			
			    public void testAssertNotNull() {
			        Assert.assertNotNull(new Object());
			        try {
			            Assert.assertNotNull(null);
			        } catch (final AssertionFailedError e) {
			            return;
			        }
			        Assert.fail();
			    }
			
			    public void testAssertNotSame() {
			        Assert.assertNotSame(new Integer(1), null);
			        Assert.assertNotSame(null, new Integer(1));
			        Assert.assertNotSame(new Integer(1), new Integer(1));
			        try {
			            final Integer obj = new Integer(1);
			            Assert.assertNotSame(obj, obj);
			        } catch (final AssertionFailedError e) {
			            return;
			        }
			        Assert.fail();
			    }
			
			    public void testAssertNotSameFailsNull() {
			        try {
			            Assert.assertNotSame(null, null);
			        } catch (final AssertionFailedError e) {
			            return;
			        }
			        Assert.fail();
			    }
			
			    public void testAssertNull() {
			        Assert.assertNull(null);
			        try {
			            Assert.assertNull(new Object());
			        } catch (final AssertionFailedError e) {
			            return;
			        }
			        Assert.fail();
			    }
			
			    public void testAssertNullNotEqualsNull() {
			        try {
			            Assert.assertEquals(null, new Object());
			        } catch (final AssertionFailedError e) {
			            e.getMessage(); // why no assertion?
			            return;
			        }
			        Assert.fail();
			    }
			
			    public void testAssertNullNotEqualsString() {
			        try {
			            Assert.assertEquals(null, "foo"); //$NON-NLS-1$
			            Assert.fail();
			        } catch (final ComparisonFailure e) {
			        }
			    }
			
			    public void testAssertSame() {
			        final Object o = new Object();
			        Assert.assertSame(o, o);
			        try {
			            Assert.assertSame(new Integer(1), new Integer(1));
			        } catch (final AssertionFailedError e) {
			            return;
			        }
			        Assert.fail();
			    }
			
			    public void testAssertStringEquals() {
			        Assert.assertEquals("a", "a"); //$NON-NLS-1$ //$NON-NLS-2$
			    }
			
			    public void testAssertStringNotEqualsNull() {
			        try {
			            Assert.assertEquals("foo", null); //$NON-NLS-1$
			            Assert.fail();
			        } catch (final ComparisonFailure e) {
			            e.getMessage(); // why no assertion?
			        }
			    }
			
			    public void testAssertTrue() {
			        Assert.assertTrue(true);
			        try {
			            Assert.assertTrue(false);
			        } catch (final AssertionFailedError e) {
			            return;
			        }
			        Assert.fail();
			    }
			
			    /*
			     * In the tests that follow, we can't use standard formatting for exception
			     * tests: try { somethingThatShouldThrow(); fail(); catch
			     * (AssertionFailedError e) { } because fail() would never be reported.
			     */
			    public void testFail() {
			        // Also, we are testing fail, so we can't rely on fail() working.
			        // We have to throw the exception manually, .
			        try {
			            Assert.fail();
			        } catch (final AssertionFailedError e) {
			            return;
			        }
			        throw new AssertionFailedError();
			    }
			}""";
        fExpectedChangesAllTests.put("junit.tests.framework.AssertTest.java", str4);
        String str5= """
			package junit.samples;
			
			import junit.framework.Test;
			import junit.framework.TestSuite;
			
			/**
			 * TestSuite that runs all the sample tests
			 *
			 */
			public class AllTests {
			
			    public static void main(final String[] args) {
			        junit.textui.TestRunner.run(AllTests.suite());
			    }
			    public static Test suite() {
			        final TestSuite suite = new TestSuite("All JUnit Tests"); //$NON-NLS-1$
			        suite.addTest(VectorTest.suite());
			        suite.addTest(new TestSuite(junit.samples.money.MoneyTest.class));
			        suite.addTest(junit.tests.AllTests.suite());
			        return suite;
			    }
			}""";
        fExpectedChangesAllTests.put("junit.samples.AllTests.java", str5);
        String str6= """
			package junit.tests.extensions;
			
			import junit.extensions.ExceptionTestCase;
			import junit.framework.Assert;
			import junit.framework.TestResult;
			
			public class ExceptionTestCaseTest extends junit.framework.TestCase {
			
			    static public class ThrowExceptionTestCase extends ExceptionTestCase {
			        public ThrowExceptionTestCase(final String name,
			                final Class exception) {
			            super(name, exception);
			        }
			        public void test() {
			            throw new IndexOutOfBoundsException();
			        }
			    }
			
			    static public class ThrowNoExceptionTestCase extends ExceptionTestCase {
			        public ThrowNoExceptionTestCase(final String name,
			                final Class exception) {
			            super(name, exception);
			        }
			        public void test() {
			        }
			    }
			
			    static public class ThrowRuntimeExceptionTestCase
			            extends
			                ExceptionTestCase {
			        public ThrowRuntimeExceptionTestCase(final String name,
			                final Class exception) {
			            super(name, exception);
			        }
			        public void test() {
			            throw new RuntimeException();
			        }
			    }
			
			    public void testExceptionSubclass() {
			        final ExceptionTestCase test = new ThrowExceptionTestCase("test", //$NON-NLS-1$
			                IndexOutOfBoundsException.class);
			        final TestResult result = test.run();
			        Assert.assertEquals(1, result.runCount());
			        Assert.assertTrue(result.wasSuccessful());
			    }
			    public void testExceptionTest() {
			        final ExceptionTestCase test = new ThrowExceptionTestCase("test", //$NON-NLS-1$
			                IndexOutOfBoundsException.class);
			        final TestResult result = test.run();
			        Assert.assertEquals(1, result.runCount());
			        Assert.assertTrue(result.wasSuccessful());
			    }
			    public void testFailure() {
			        final ExceptionTestCase test = new ThrowRuntimeExceptionTestCase("test", //$NON-NLS-1$
			                IndexOutOfBoundsException.class);
			        final TestResult result = test.run();
			        Assert.assertEquals(1, result.runCount());
			        Assert.assertEquals(1, result.errorCount());
			    }
			    public void testNoException() {
			        final ExceptionTestCase test = new ThrowNoExceptionTestCase("test", //$NON-NLS-1$
			                Exception.class);
			        final TestResult result = test.run();
			        Assert.assertEquals(1, result.runCount());
			        Assert.assertEquals(1, result.failureCount());
			    }
			    public void testWrongException() {
			        final ExceptionTestCase test = new ThrowRuntimeExceptionTestCase("test", //$NON-NLS-1$
			                IndexOutOfBoundsException.class);
			        final TestResult result = test.run();
			        Assert.assertEquals(1, result.runCount());
			        Assert.assertEquals(1, result.errorCount());
			    }
			}""";
        fExpectedChangesAllTests.put("junit.tests.extensions.ExceptionTestCaseTest.java", str6);
        String str7= """
			package junit.tests.framework;
			
			/**
			 * Test class used in SuiteTest
			 */
			import junit.framework.Assert;
			import junit.framework.AssertionFailedError;
			import junit.framework.Test;
			import junit.framework.TestCase;
			import junit.framework.TestListener;
			import junit.framework.TestResult;
			
			public class TestListenerTest extends TestCase implements TestListener {
			    private int fEndCount;
			    private int fErrorCount;
			    private int fFailureCount;
			    private TestResult fResult;
			    private int fStartCount;
			
			    public void addError(final Test test, final Throwable t) {
			        this.fErrorCount++;
			    }
			    public void addFailure(final Test test, final AssertionFailedError t) {
			        this.fFailureCount++;
			    }
			    public void endTest(final Test test) {
			        this.fEndCount++;
			    }
			    @Override
			    protected void setUp() {
			        this.fResult = new TestResult();
			        this.fResult.addListener(this);
			
			        this.fStartCount = 0;
			        this.fEndCount = 0;
			        this.fFailureCount = 0;
			    }
			    public void startTest(final Test test) {
			        this.fStartCount++;
			    }
			    public void testError() {
			        final TestCase test = new TestCase("noop") { //$NON-NLS-1$
			            @Override
			            public void runTest() {
			                throw new Error();
			            }
			        };
			        test.run(this.fResult);
			        Assert.assertEquals(1, this.fErrorCount);
			        Assert.assertEquals(1, this.fEndCount);
			    }
			    public void testFailure() {
			        final TestCase test = new TestCase("noop") { //$NON-NLS-1$
			            @Override
			            public void runTest() {
			                Assert.fail();
			            }
			        };
			        test.run(this.fResult);
			        Assert.assertEquals(1, this.fFailureCount);
			        Assert.assertEquals(1, this.fEndCount);
			    }
			    public void testStartStop() {
			        final TestCase test = new TestCase("noop") { //$NON-NLS-1$
			            @Override
			            public void runTest() {
			            }
			        };
			        test.run(this.fResult);
			        Assert.assertEquals(1, this.fStartCount);
			        Assert.assertEquals(1, this.fEndCount);
			    }
			}""";
        fExpectedChangesAllTests.put("junit.tests.framework.TestListenerTest.java", str7);
        String str8= """
			package junit.tests.runner;
			
			import java.util.Vector;
			
			import junit.framework.Assert;
			import junit.framework.TestCase;
			import junit.runner.Sorter;
			
			public class SorterTest extends TestCase {
			
			    static class Swapper implements Sorter.Swapper {
			        public void swap(final Vector values, final int left, final int right) {
			            final Object tmp = values.elementAt(left);
			            values.setElementAt(values.elementAt(right), left);
			            values.setElementAt(tmp, right);
			        }
			    }
			
			    public void testSort() throws Exception {
			        final Vector v = new Vector();
			        v.addElement("c"); //$NON-NLS-1$
			        v.addElement("b"); //$NON-NLS-1$
			        v.addElement("a"); //$NON-NLS-1$
			        Sorter.sortStrings(v, 0, v.size() - 1, new Swapper());
			        Assert.assertEquals(v.elementAt(0), "a"); //$NON-NLS-1$
			        Assert.assertEquals(v.elementAt(1), "b"); //$NON-NLS-1$
			        Assert.assertEquals(v.elementAt(2), "c"); //$NON-NLS-1$
			    }
			}""";
        fExpectedChangesAllTests.put("junit.tests.runner.SorterTest.java", str8);
        String str9= """
			package junit.tests.framework;
			
			/**
			 * Test class used in SuiteTest
			 */
			import junit.framework.TestCase;
			
			public class OneTestCase extends TestCase {
			    public void noTestCase() {
			    }
			    public void testCase() {
			    }
			    public void testCase(final int arg) {
			    }
			}""";
        fExpectedChangesAllTests.put("junit.tests.framework.OneTestCase.java", str9);
        String str10= """
			package junit.tests.framework;
			
			import junit.framework.Assert;
			import junit.framework.Protectable;
			import junit.framework.Test;
			import junit.framework.TestCase;
			import junit.framework.TestResult;
			
			/**
			 * Test an implementor of junit.framework.Test other than TestCase or TestSuite
			 */
			public class TestImplementorTest extends TestCase {
			    public static class DoubleTestCase implements Test {
			        private final TestCase fTestCase;
			
			        public DoubleTestCase(final TestCase testCase) {
			            this.fTestCase = testCase;
			        }
			
			        public int countTestCases() {
			            return 2;
			        }
			
			        public void run(final TestResult result) {
			            result.startTest(this);
			            final Protectable p = new Protectable() {
			                public void protect() throws Throwable {
			                    DoubleTestCase.this.fTestCase.runBare();
			                    DoubleTestCase.this.fTestCase.runBare();
			                }
			            };
			            result.runProtected(this, p);
			            result.endTest(this);
			        }
			    }
			
			    private final DoubleTestCase fTest;
			
			    public TestImplementorTest() {
			        final TestCase testCase = new TestCase() {
			            @Override
			            public void runTest() {
			            }
			        };
			        this.fTest = new DoubleTestCase(testCase);
			    }
			
			    public void testSuccessfulRun() {
			        final TestResult result = new TestResult();
			        this.fTest.run(result);
			        Assert.assertEquals(this.fTest.countTestCases(), result.runCount());
			        Assert.assertEquals(0, result.errorCount());
			        Assert.assertEquals(0, result.failureCount());
			    }
			}
			""";
        fExpectedChangesAllTests.put("junit.tests.framework.TestImplementorTest.java", str10);
        String str11= """
			package junit.extensions;
			
			import junit.framework.Assert;
			import junit.framework.Test;
			import junit.framework.TestResult;
			
			/**
			 * A Decorator for Tests. Use TestDecorator as the base class for defining new
			 * test decorators. Test decorator subclasses can be introduced to add behaviour
			 * before or after a test is run.
			 *
			 */
			public class TestDecorator extends Assert implements Test {
			    protected Test fTest;
			
			    public TestDecorator(final Test test) {
			        this.fTest = test;
			    }
			    /**
			     * The basic run behaviour.
			     */
			    public void basicRun(final TestResult result) {
			        this.fTest.run(result);
			    }
			    public int countTestCases() {
			        return this.fTest.countTestCases();
			    }
			    public Test getTest() {
			        return this.fTest;
			    }
			
			    public void run(final TestResult result) {
			        this.basicRun(result);
			    }
			
			    @Override
			    public String toString() {
			        return this.fTest.toString();
			    }
			}""";
        fExpectedChangesAllTests.put("junit.extensions.TestDecorator.java", str11);
        String str12= """
			package junit.runner;
			
			/**
			 * An interface to define how a test suite should be loaded.
			 */
			public interface TestSuiteLoader {
			    Class load(String suiteClassName) throws ClassNotFoundException;
			    Class reload(Class aClass) throws ClassNotFoundException;
			}""";
        fExpectedChangesAllTests.put("junit.runner.TestSuiteLoader.java", str12);
        String str13= """
			package junit.framework;
			
			import java.util.Enumeration;
			import java.util.Vector;
			
			/**
			 * A <code>TestResult</code> collects the results of executing a test case. It
			 * is an instance of the Collecting Parameter pattern. The test framework
			 * distinguishes between <i>failures</i> and <i>errors</i>. A failure is
			 * anticipated and checked for with assertions. Errors are unanticipated
			 * problems like an <code>ArrayIndexOutOfBoundsException</code>.
			 *
			 * @see Test
			 */
			public class TestResult extends Object {
			    protected Vector fErrors;
			    protected Vector fFailures;
			    protected Vector fListeners;
			    protected int fRunTests;
			    private boolean fStop;
			
			    public TestResult() {
			        this.fFailures = new Vector();
			        this.fErrors = new Vector();
			        this.fListeners = new Vector();
			        this.fRunTests = 0;
			        this.fStop = false;
			    }
			    /**
			     * Adds an error to the list of errors. The passed in exception caused the
			     * error.
			     */
			    public synchronized void addError(final Test test, final Throwable t) {
			        this.fErrors.addElement(new TestFailure(test, t));
			        for (final Object element : this.cloneListeners()) {
			            ((TestListener) element).addError(test, t);
			        }
			    }
			    /**
			     * Adds a failure to the list of failures. The passed in exception caused
			     * the failure.
			     */
			    public synchronized void addFailure(final Test test,
			            final AssertionFailedError t) {
			        this.fFailures.addElement(new TestFailure(test, t));
			        for (final Object element : this.cloneListeners()) {
			            ((TestListener) element).addFailure(test, t);
			        }
			    }
			    /**
			     * Registers a TestListener
			     */
			    public synchronized void addListener(final TestListener listener) {
			        this.fListeners.addElement(listener);
			    }
			    /**
			     * Returns a copy of the listeners.
			     */
			    private synchronized Vector cloneListeners() {
			        return (Vector) this.fListeners.clone();
			    }
			    /**
			     * Informs the result that a test was completed.
			     */
			    public void endTest(final Test test) {
			        for (final Object element : this.cloneListeners()) {
			            ((TestListener) element).endTest(test);
			        }
			    }
			    /**
			     * Gets the number of detected errors.
			     */
			    public synchronized int errorCount() {
			        return this.fErrors.size();
			    }
			    /**
			     * Returns an Enumeration for the errors
			     */
			    public synchronized Enumeration errors() {
			        return this.fErrors.elements();
			    }
			    /**
			     * Gets the number of detected failures.
			     */
			    public synchronized int failureCount() {
			        return this.fFailures.size();
			    }
			    /**
			     * Returns an Enumeration for the failures
			     */
			    public synchronized Enumeration failures() {
			        return this.fFailures.elements();
			    }
			    /**
			     * Unregisters a TestListener
			     */
			    public synchronized void removeListener(final TestListener listener) {
			        this.fListeners.removeElement(listener);
			    }
			    /**
			     * Runs a TestCase.
			     */
			    protected void run(final TestCase test) {
			        this.startTest(test);
			        final Protectable p = new Protectable() {
			            public void protect() throws Throwable {
			                test.runBare();
			            }
			        };
			        this.runProtected(test, p);
			
			        this.endTest(test);
			    }
			    /**
			     * Gets the number of run tests.
			     */
			    public synchronized int runCount() {
			        return this.fRunTests;
			    }
			    /**
			     * Runs a TestCase.
			     */
			    public void runProtected(final Test test, final Protectable p) {
			        try {
			            p.protect();
			        } catch (final AssertionFailedError e) {
			            this.addFailure(test, e);
			        } catch (final ThreadDeath e) { // don't catch ThreadDeath by accident
			            throw e;
			        } catch (final Throwable e) {
			            this.addError(test, e);
			        }
			    }
			    /**
			     * Checks whether the test run should stop
			     */
			    public synchronized boolean shouldStop() {
			        return this.fStop;
			    }
			    /**
			     * Informs the result that a test will be started.
			     */
			    public void startTest(final Test test) {
			        final int count = test.countTestCases();
			        synchronized (this) {
			            this.fRunTests += count;
			        }
			        for (final Object element : this.cloneListeners()) {
			            ((TestListener) element).startTest(test);
			        }
			    }
			    /**
			     * Marks that the test run should stop.
			     */
			    public synchronized void stop() {
			        this.fStop = true;
			    }
			    /**
			     * Returns whether the entire test was successful or not.
			     */
			    public synchronized boolean wasSuccessful() {
			        return (this.failureCount() == 0) && (this.errorCount() == 0);
			    }
			}""";
        fExpectedChangesAllTests.put("junit.framework.TestResult.java", str13);
        String str14= """
			package junit.tests.framework;
			
			/**
			 * Test class used in SuiteTest
			 */
			import junit.framework.TestCase;
			
			public class NotPublicTestCase extends TestCase {
			    protected void testNotPublic() {
			    }
			    public void testPublic() {
			    }
			}""";
        fExpectedChangesAllTests.put("junit.tests.framework.NotPublicTestCase.java", str14);
        String str15= """
			package junit.extensions;
			
			import junit.framework.Test;
			import junit.framework.TestResult;
			import junit.framework.TestSuite;
			
			/**
			 * A TestSuite for active Tests. It runs each test in a separate thread and
			 * waits until all threads have terminated. -- Aarhus Radisson Scandinavian
			 * Center 11th floor
			 */
			public class ActiveTestSuite extends TestSuite {
			    private volatile int fActiveTestDeathCount;
			
			    public ActiveTestSuite() {
			    }
			
			    public ActiveTestSuite(final Class theClass) {
			        super(theClass);
			    }
			
			    public ActiveTestSuite(final Class theClass, final String name) {
			        super(theClass, name);
			    }
			
			    public ActiveTestSuite(final String name) {
			        super(name);
			    }
			
			    @Override
			    public void run(final TestResult result) {
			        this.fActiveTestDeathCount = 0;
			        super.run(result);
			        this.waitUntilFinished();
			    }
			
			    synchronized public void runFinished(final Test test) {
			        this.fActiveTestDeathCount++;
			        this.notifyAll();
			    }
			
			    @Override
			    public void runTest(final Test test, final TestResult result) {
			        final Thread t = new Thread() {
			            @Override
			            public void run() {
			                try {
			                    // inlined due to limitation in VA/Java
			                    // ActiveTestSuite.super.runTest(test, result);
			                    test.run(result);
			                } finally {
			                    ActiveTestSuite.this.runFinished(test);
			                }
			            }
			        };
			        t.start();
			    }
			
			    synchronized void waitUntilFinished() {
			        while (this.fActiveTestDeathCount < this.testCount()) {
			            try {
			                this.wait();
			            } catch (final InterruptedException e) {
			                return; // ignore
			            }
			        }
			    }
			}""";
        fExpectedChangesAllTests.put("junit.extensions.ActiveTestSuite.java", str15);
        String str16= """
			package junit.tests.framework;
			
			import junit.framework.Assert;
			import junit.framework.Test;
			import junit.framework.TestCase;
			import junit.framework.TestResult;
			import junit.framework.TestSuite;
			
			/**
			 * A fixture for testing the "auto" test suite feature.
			 *
			 */
			public class SuiteTest extends TestCase {
			    public static Test suite() {
			        final TestSuite suite = new TestSuite("Suite Tests"); //$NON-NLS-1$
			        // build the suite manually, because some of the suites are testing
			        // the functionality that automatically builds suites
			        suite.addTest(new SuiteTest("testNoTestCaseClass")); //$NON-NLS-1$
			        suite.addTest(new SuiteTest("testNoTestCases")); //$NON-NLS-1$
			        suite.addTest(new SuiteTest("testOneTestCase")); //$NON-NLS-1$
			        suite.addTest(new SuiteTest("testNotPublicTestCase")); //$NON-NLS-1$
			        suite.addTest(new SuiteTest("testNotVoidTestCase")); //$NON-NLS-1$
			        suite.addTest(new SuiteTest("testNotExistingTestCase")); //$NON-NLS-1$
			        suite.addTest(new SuiteTest("testInheritedTests")); //$NON-NLS-1$
			        suite.addTest(new SuiteTest("testShadowedTests")); //$NON-NLS-1$
			        suite.addTest(new SuiteTest("testAddTestSuite")); //$NON-NLS-1$
			
			        return suite;
			    }
			    protected TestResult fResult;
			    public SuiteTest(final String name) {
			        super(name);
			    }
			    @Override
			    protected void setUp() {
			        this.fResult = new TestResult();
			    }
			    public void testAddTestSuite() {
			        final TestSuite suite = new TestSuite();
			        suite.addTestSuite(OneTestCase.class);
			        suite.run(this.fResult);
			        Assert.assertEquals(1, this.fResult.runCount());
			    }
			    public void testInheritedTests() {
			        final TestSuite suite = new TestSuite(InheritedTestCase.class);
			        suite.run(this.fResult);
			        Assert.assertTrue(this.fResult.wasSuccessful());
			        Assert.assertEquals(2, this.fResult.runCount());
			    }
			    public void testNoTestCaseClass() {
			        final Test t = new TestSuite(NoTestCaseClass.class);
			        t.run(this.fResult);
			        Assert.assertEquals(1, this.fResult.runCount()); // warning test
			        Assert.assertTrue(!this.fResult.wasSuccessful());
			    }
			    public void testNoTestCases() {
			        final Test t = new TestSuite(NoTestCases.class);
			        t.run(this.fResult);
			        Assert.assertTrue(this.fResult.runCount() == 1); // warning test
			        Assert.assertTrue(this.fResult.failureCount() == 1);
			        Assert.assertTrue(!this.fResult.wasSuccessful());
			    }
			    public void testNotExistingTestCase() {
			        final Test t = new SuiteTest("notExistingMethod"); //$NON-NLS-1$
			        t.run(this.fResult);
			        Assert.assertTrue(this.fResult.runCount() == 1);
			        Assert.assertTrue(this.fResult.failureCount() == 1);
			        Assert.assertTrue(this.fResult.errorCount() == 0);
			    }
			    public void testNotPublicTestCase() {
			        final TestSuite suite = new TestSuite(NotPublicTestCase.class);
			        // 1 public test case + 1 warning for the non-public test case
			        Assert.assertEquals(2, suite.countTestCases());
			    }
			    public void testNotVoidTestCase() {
			        final TestSuite suite = new TestSuite(NotVoidTestCase.class);
			        Assert.assertTrue(suite.countTestCases() == 1);
			    }
			    public void testOneTestCase() {
			        final Test t = new TestSuite(OneTestCase.class);
			        t.run(this.fResult);
			        Assert.assertTrue(this.fResult.runCount() == 1);
			        Assert.assertTrue(this.fResult.failureCount() == 0);
			        Assert.assertTrue(this.fResult.errorCount() == 0);
			        Assert.assertTrue(this.fResult.wasSuccessful());
			    }
			    public void testShadowedTests() {
			        final TestSuite suite = new TestSuite(OverrideTestCase.class);
			        suite.run(this.fResult);
			        Assert.assertEquals(1, this.fResult.runCount());
			    }
			}""";
        fExpectedChangesAllTests.put("junit.tests.framework.SuiteTest.java", str16);
        String str17= """
			package junit.runner;
			
			/**
			 * An implementation of a TestCollector that considers a class to be a test
			 * class when it contains the pattern "Test" in its name
			 *\s
			 * @see TestCollector
			 */
			public class SimpleTestCollector extends ClassPathTestCollector {
			
			    public SimpleTestCollector() {
			    }
			
			    @Override
			    protected boolean isTestClass(final String classFileName) {
			        return classFileName.endsWith(".class") && //$NON-NLS-1$
			                (classFileName.indexOf('$') < 0)
			                && (classFileName.indexOf("Test") > 0); //$NON-NLS-1$
			    }
			}
			""";
        fExpectedChangesAllTests.put("junit.runner.SimpleTestCollector.java", str17);
        String str18= """
			package junit.framework;
			
			/**
			 * A <em>Test</em> can be run and collect its results.
			 *
			 * @see TestResult
			 */
			public interface Test {
			    /**
			     * Counts the number of test cases that will be run by this test.
			     */
			    int countTestCases();
			    /**
			     * Runs a test and collects its result in a TestResult instance.
			     */
			    void run(TestResult result);
			}""";
        fExpectedChangesAllTests.put("junit.framework.Test.java", str18);
        String str19= """
			package junit.tests.framework;
			
			/**
			 * Test class used in SuiteTest
			 */
			
			public class NoTestCaseClass extends Object {
			    public void testSuccess() {
			    }
			}""";
        fExpectedChangesAllTests.put("junit.tests.framework.NoTestCaseClass.java", str19);
        String str20= """
			package junit.tests.framework;
			
			import junit.framework.TestCase;
			
			/**
			 * A test case testing the testing framework.
			 *
			 */
			public class Success extends TestCase {
			
			    @Override
			    public void runTest() {
			    }
			
			    public void testSuccess() {
			    }
			}""";
        fExpectedChangesAllTests.put("junit.tests.framework.Success.java", str20);
        String str21= """
			package junit.runner;
			
			import java.lang.reflect.Modifier;
			
			import junit.framework.Test;
			import junit.framework.TestSuite;
			
			/**
			 * An implementation of a TestCollector that loads all classes on the class path
			 * and tests whether it is assignable from Test or provides a static suite
			 * method.
			 *\s
			 * @see TestCollector
			 */
			public class LoadingTestCollector extends ClassPathTestCollector {
			
			    TestCaseClassLoader fLoader;
			
			    public LoadingTestCollector() {
			        this.fLoader = new TestCaseClassLoader();
			    }
			
			    Class classFromFile(final String classFileName)
			            throws ClassNotFoundException {
			        final String className = this.classNameFromFile(classFileName);
			        if (!this.fLoader.isExcluded(className)) {
			            return this.fLoader.loadClass(className, false);
			        }
			        return null;
			    }
			
			    boolean hasPublicConstructor(final Class testClass) {
			        try {
			            TestSuite.getTestConstructor(testClass);
			        } catch (final NoSuchMethodException e) {
			            return false;
			        }
			        return true;
			    }
			
			    boolean hasSuiteMethod(final Class testClass) {
			        try {
			            testClass.getMethod(BaseTestRunner.SUITE_METHODNAME, new Class[0]);
			        } catch (final Exception e) {
			            return false;
			        }
			        return true;
			    }
			
			    boolean isTestClass(final Class testClass) {
			        if (this.hasSuiteMethod(testClass)) {
			            return true;
			        }
			        if (Test.class.isAssignableFrom(testClass)
			                && Modifier.isPublic(testClass.getModifiers())
			                && this.hasPublicConstructor(testClass)) {
			            return true;
			        }
			        return false;
			    }
			
			    @Override
			    protected boolean isTestClass(final String classFileName) {
			        try {
			            if (classFileName.endsWith(".class")) { //$NON-NLS-1$
			                final Class testClass = this.classFromFile(classFileName);
			                return (testClass != null) && this.isTestClass(testClass);
			            }
			        } catch (final ClassNotFoundException expected) {
			        } catch (final NoClassDefFoundError notFatal) {
			        }
			        return false;
			    }
			}
			""";
        fExpectedChangesAllTests.put("junit.runner.LoadingTestCollector.java", str21);
        String str22= """
			package junit.runner;
			
			import java.io.ByteArrayOutputStream;
			import java.io.File;
			import java.io.FileInputStream;
			import java.io.IOException;
			import java.io.InputStream;
			import java.net.URL;
			import java.util.Enumeration;
			import java.util.Properties;
			import java.util.StringTokenizer;
			import java.util.Vector;
			import java.util.zip.ZipEntry;
			import java.util.zip.ZipFile;
			
			/**
			 * A custom class loader which enables the reloading of classes for each test
			 * run. The class loader can be configured with a list of package paths that
			 * should be excluded from loading. The loading of these packages is delegated
			 * to the system class loader. They will be shared across test runs.
			 * <p>
			 * The list of excluded package paths is specified in a properties file
			 * "excluded.properties" that is located in the same place as the
			 * TestCaseClassLoader class.
			 * <p>
			 * <b>Known limitation:</b> the TestCaseClassLoader cannot load classes from jar
			 * files.
			 */
			
			public class TestCaseClassLoader extends ClassLoader {
			    /** name of excluded properties file */
			    static final String EXCLUDED_FILE = "excluded.properties"; //$NON-NLS-1$
			    /** default excluded paths */
			    private final String[] defaultExclusions = {"junit.framework.", //$NON-NLS-1$
			            "junit.extensions.", //$NON-NLS-1$
			            "junit.runner." //$NON-NLS-1$
			    };
			    /** excluded paths */
			    private Vector fExcluded;
			    /** scanned class path */
			    private Vector fPathItems;
			
			    /**
			     * Constructs a TestCaseLoader. It scans the class path and the excluded
			     * package paths
			     */
			    public TestCaseClassLoader() {
			        this(System.getProperty("java.class.path")); //$NON-NLS-1$
			    }
			
			    /**
			     * Constructs a TestCaseLoader. It scans the class path and the excluded
			     * package paths
			     */
			    public TestCaseClassLoader(final String classPath) {
			        this.scanPath(classPath);
			        this.readExcludedPackages();
			    }
			
			    private byte[] getClassData(final File f) {
			        try {
			            final FileInputStream stream = new FileInputStream(f);
			            final ByteArrayOutputStream out = new ByteArrayOutputStream(1000);
			            final byte[] b = new byte[1000];
			            int n;
			            while ((n = stream.read(b)) != -1) {
			                out.write(b, 0, n);
			            }
			            stream.close();
			            out.close();
			            return out.toByteArray();
			
			        } catch (final IOException e) {
			        }
			        return null;
			    }
			
			    @Override
			    public URL getResource(final String name) {
			        return ClassLoader.getSystemResource(name);
			    }
			
			    @Override
			    public InputStream getResourceAsStream(final String name) {
			        return ClassLoader.getSystemResourceAsStream(name);
			    }
			
			    public boolean isExcluded(final String name) {
			        for (int i = 0; i < this.fExcluded.size(); i++) {
			            if (name.startsWith((String) this.fExcluded.elementAt(i))) {
			                return true;
			            }
			        }
			        return false;
			    }
			
			    boolean isJar(final String pathEntry) {
			        return pathEntry.endsWith(".jar") || pathEntry.endsWith(".zip"); //$NON-NLS-1$ //$NON-NLS-2$
			    }
			
			    @Override
			    public synchronized Class loadClass(final String name,
			            final boolean resolve) throws ClassNotFoundException {
			
			        Class c = this.findLoadedClass(name);
			        if (c != null) {
			            return c;
			        }
			        //
			        // Delegate the loading of excluded classes to the
			        // standard class loader.
			        //
			        if (this.isExcluded(name)) {
			            try {
			                c = this.findSystemClass(name);
			                return c;
			            } catch (final ClassNotFoundException e) {
			                // keep searching
			            }
			        }
			        if (c == null) {
			            final byte[] data = this.lookupClassData(name);
			            if (data == null) {
			                throw new ClassNotFoundException();
			            }
			            c = this.defineClass(name, data, 0, data.length);
			        }
			        if (resolve) {
			            this.resolveClass(c);
			        }
			        return c;
			    }
			
			    private byte[] loadFileData(final String path, final String fileName) {
			        final File file = new File(path, fileName);
			        if (file.exists()) {
			            return this.getClassData(file);
			        }
			        return null;
			    }
			
			    private byte[] loadJarData(final String path, final String fileName) {
			        ZipFile zipFile = null;
			        InputStream stream = null;
			        final File archive = new File(path);
			        if (!archive.exists()) {
			            return null;
			        }
			        try {
			            zipFile = new ZipFile(archive);
			        } catch (final IOException io) {
			            return null;
			        }
			        final ZipEntry entry = zipFile.getEntry(fileName);
			        if (entry == null) {
			            return null;
			        }
			        final int size = (int) entry.getSize();
			        try {
			            stream = zipFile.getInputStream(entry);
			            final byte[] data = new byte[size];
			            int pos = 0;
			            while (pos < size) {
			                final int n = stream.read(data, pos, data.length - pos);
			                pos += n;
			            }
			            zipFile.close();
			            return data;
			        } catch (final IOException e) {
			        } finally {
			            try {
			                if (stream != null) {
			                    stream.close();
			                }
			            } catch (final IOException e) {
			            }
			        }
			        return null;
			    }
			
			    private byte[] lookupClassData(final String className)
			            throws ClassNotFoundException {
			        byte[] data = null;
			        for (int i = 0; i < this.fPathItems.size(); i++) {
			            final String path = (String) this.fPathItems.elementAt(i);
			            final String fileName = className.replace('.', '/') + ".class"; //$NON-NLS-1$
			            if (this.isJar(path)) {
			                data = this.loadJarData(path, fileName);
			            } else {
			                data = this.loadFileData(path, fileName);
			            }
			            if (data != null) {
			                return data;
			            }
			        }
			        throw new ClassNotFoundException(className);
			    }
			
			    private void readExcludedPackages() {
			        this.fExcluded = new Vector(10);
			        for (final String defaultExclusion : this.defaultExclusions) {
			            this.fExcluded.addElement(defaultExclusion);
			        }
			
			        final InputStream is = this.getClass()
			                .getResourceAsStream(TestCaseClassLoader.EXCLUDED_FILE);
			        if (is == null) {
			            return;
			        }
			        final Properties p = new Properties();
			        try {
			            p.load(is);
			        } catch (final IOException e) {
			            return;
			        } finally {
			            try {
			                is.close();
			            } catch (final IOException e) {
			            }
			        }
			        for (final Enumeration e = p.propertyNames(); e.hasMoreElements();) {
			            final String key = (String) e.nextElement();
			            if (key.startsWith("excluded.")) { //$NON-NLS-1$
			                String path = p.getProperty(key);
			                path = path.trim();
			                if (path.endsWith("*")) { //$NON-NLS-1$
			                    path = path.substring(0, path.length() - 1);
			                }
			                if (path.length() > 0) {
			                    this.fExcluded.addElement(path);
			                }
			            }
			        }
			    }
			
			    private void scanPath(final String classPath) {
			        final String separator = System.getProperty("path.separator"); //$NON-NLS-1$
			        this.fPathItems = new Vector(10);
			        final StringTokenizer st = new StringTokenizer(classPath, separator);
			        while (st.hasMoreTokens()) {
			            this.fPathItems.addElement(st.nextToken());
			        }
			    }
			}""";
        fExpectedChangesAllTests.put("junit.runner.TestCaseClassLoader.java", str22);
        String str23= """
			package junit.framework;
			
			/**
			 * Thrown when an assertion failed.
			 */
			public class AssertionFailedError extends Error {
			
			    /* Test */
			    private static final long serialVersionUID = 1L;
			    public AssertionFailedError() {
			    }
			    public AssertionFailedError(final String message) {
			        super(message);
			    }
			}""";
        fExpectedChangesAllTests.put("junit.framework.AssertionFailedError.java", str23);
        String str24= """
			package junit.tests.framework;
			
			/**
			 * Test class used in SuiteTest
			 */
			public class InheritedTestCase extends OneTestCase {
			    public void test2() {
			    }
			}""";
        fExpectedChangesAllTests.put("junit.tests.framework.InheritedTestCase.java", str24);
        String str25= """
			package junit.samples;
			
			import junit.framework.Assert;
			import junit.framework.Test;
			import junit.framework.TestCase;
			import junit.framework.TestSuite;
			
			/**
			 * Some simple tests.
			 *
			 */
			public class SimpleTest extends TestCase {
			    public static void main(final String[] args) {
			        junit.textui.TestRunner.run(SimpleTest.suite());
			    }
			    public static Test suite() {
			
			        /*
			         * the type safe way
			         *
			         * TestSuite suite= new TestSuite(); suite.addTest( new
			         * SimpleTest("add") { protected void runTest() { testAdd(); } } );
			         *\s
			         * suite.addTest( new SimpleTest("testDivideByZero") { protected void
			         * runTest() { testDivideByZero(); } } ); return suite;
			         */
			
			        /*
			         * the dynamic way
			         */
			        return new TestSuite(SimpleTest.class);
			    }
			
			    protected int fValue1;
			    protected int fValue2;
			    @Override
			    protected void setUp() {
			        this.fValue1 = 2;
			        this.fValue2 = 3;
			    }
			    public void testAdd() {
			        final double result = this.fValue1 + this.fValue2;
			        // forced failure result == 5
			        Assert.assertTrue(result == 6);
			    }
			    public void testDivideByZero() {
			        final int zero = 0;
			    }
			    public void testEquals() {
			        Assert.assertEquals(12, 12);
			        Assert.assertEquals(12L, 12L);
			        Assert.assertEquals(new Long(12), new Long(12));
			
			        Assert.assertEquals("Size", 12, 13); //$NON-NLS-1$
			        Assert.assertEquals("Capacity", 12.0, 11.99, 0.0); //$NON-NLS-1$
			    }
			}""";
        fExpectedChangesAllTests.put("junit.samples.SimpleTest.java", str25);
        String str26= """
			package junit.runner;
			
			/**
			 * This class defines the current version of JUnit
			 */
			public class Version {
			    public static String id() {
			        return "3.8.1"; //$NON-NLS-1$
			    }
			
			    private Version() {
			        // don't instantiate
			    }
			}
			""";
        fExpectedChangesAllTests.put("junit.runner.Version.java", str26);
        String str27= """
			
			package junit.tests.runner;
			
			import junit.framework.Test;
			import junit.framework.TestCase;
			import junit.runner.BaseTestRunner;
			
			public class BaseTestRunnerTest extends TestCase {
			
			    public class MockRunner extends BaseTestRunner {
			        @Override
			        protected void runFailed(final String message) {
			        }
			
			        @Override
			        public void testEnded(final String testName) {
			        }
			
			        @Override
			        public void testFailed(final int status, final Test test,
			                final Throwable t) {
			        }
			
			        @Override
			        public void testStarted(final String testName) {
			        }
			    }
			
			    public static class NonStatic {
			        public Test suite() {
			            return null;
			        }
			    }
			
			    public void testInvokeNonStaticSuite() {
			        final BaseTestRunner runner = new MockRunner();
			        runner.getTest("junit.tests.runner.BaseTestRunnerTest$NonStatic"); // Used //$NON-NLS-1$
			                                                                           // to
			                                                                           // throw
			                                                                           // NullPointerException
			    }
			}
			""";
        fExpectedChangesAllTests.put("junit.tests.runner.BaseTestRunnerTest.java", str27);
        String str28= """
			package junit.tests;
			
			import junit.framework.TestCase;
			
			/**
			 * A helper test case for testing whether the testing method is run.
			 */
			public class WasRun extends TestCase {
			    public boolean fWasRun = false;
			    @Override
			    protected void runTest() {
			        this.fWasRun = true;
			    }
			}""";
        fExpectedChangesAllTests.put("junit.tests.WasRun.java", str28);
        String str29= """
			package junit.framework;
			
			import java.io.PrintWriter;
			import java.io.StringWriter;
			import java.lang.reflect.Constructor;
			import java.lang.reflect.InvocationTargetException;
			import java.lang.reflect.Method;
			import java.lang.reflect.Modifier;
			import java.util.Enumeration;
			import java.util.Vector;
			
			/**
			 * A <code>TestSuite</code> is a <code>Composite</code> of Tests. It runs a
			 * collection of test cases. Here is an example using the dynamic test
			 * definition.
			 *\s
			 * <pre>
			 * TestSuite suite = new TestSuite();
			 * suite.addTest(new MathTest("testAdd"));
			 * suite.addTest(new MathTest("testDivideByZero"));
			 * </pre>
			 *\s
			 * Alternatively, a TestSuite can extract the tests to be run automatically. To
			 * do so you pass the class of your TestCase class to the TestSuite constructor.
			 *\s
			 * <pre>
			 * TestSuite suite = new TestSuite(MathTest.class);
			 * </pre>
			 *\s
			 * This constructor creates a suite with all the methods starting with "test"
			 * that take no arguments.
			 *
			 * @see Test
			 */
			public class TestSuite implements Test {
			
			    /**
			     * ...as the moon sets over the early morning Merlin, Oregon mountains, our
			     * intrepid adventurers type...
			     */
			    static public Test createTest(final Class theClass, final String name) {
			        Constructor constructor;
			        try {
			            constructor = TestSuite.getTestConstructor(theClass);
			        } catch (final NoSuchMethodException e) {
			            return TestSuite.warning("Class " + theClass.getName() //$NON-NLS-1$
			                    + " has no public constructor TestCase(String name) or TestCase()"); //$NON-NLS-1$
			        }
			        Object test;
			        try {
			            if (constructor.getParameterTypes().length == 0) {
			                test = constructor.newInstance(new Object[0]);
			                if (test instanceof TestCase) {
			                    ((TestCase) test).setName(name);
			                }
			            } else {
			                test = constructor.newInstance(new Object[]{name});
			            }
			        } catch (final InstantiationException e) {
			            return (TestSuite.warning("Cannot instantiate test case: " + name //$NON-NLS-1$
			                    + " (" + TestSuite.exceptionToString(e) + ")")); //$NON-NLS-1$ //$NON-NLS-2$
			        } catch (final InvocationTargetException e) {
			            return (TestSuite.warning("Exception in constructor: " + name + " (" //$NON-NLS-1$ //$NON-NLS-2$
			                    + TestSuite.exceptionToString(e.getTargetException())
			                    + ")")); //$NON-NLS-1$
			        } catch (final IllegalAccessException e) {
			            return (TestSuite.warning("Cannot access test case: " + name + " (" //$NON-NLS-1$ //$NON-NLS-2$
			                    + TestSuite.exceptionToString(e) + ")")); //$NON-NLS-1$
			        }
			        return (Test) test;
			    }
			    /**
			     * Converts the stack trace into a string
			     */
			    private static String exceptionToString(final Throwable t) {
			        final StringWriter stringWriter = new StringWriter();
			        final PrintWriter writer = new PrintWriter(stringWriter);
			        t.printStackTrace(writer);
			        return stringWriter.toString();
			
			    }
			
			    /**
			     * Gets a constructor which takes a single String as its argument or a no
			     * arg constructor.
			     */
			    public static Constructor getTestConstructor(final Class theClass)
			            throws NoSuchMethodException {
			        final Class[] args = {String.class};
			        try {
			            return theClass.getConstructor(args);
			        } catch (final NoSuchMethodException e) {
			            // fall through
			        }
			        return theClass.getConstructor(new Class[0]);
			    }
			
			    /**
			     * Returns a test which will fail and log a warning message.
			     */
			    private static Test warning(final String message) {
			        return new TestCase("warning") { //$NON-NLS-1$
			            @Override
			            protected void runTest() {
			                Assert.fail(message);
			            }
			        };
			    }
			
			    private String fName;
			
			    private final Vector fTests = new Vector(10);
			
			    /**
			     * Constructs an empty TestSuite.
			     */
			    public TestSuite() {
			    }
			
			    /**
			     * Constructs a TestSuite from the given class. Adds all the methods
			     * starting with "test" as test cases to the suite. Parts of this method was
			     * written at 2337 meters in the Hüffihütte, Kanton Uri
			     */
			    public TestSuite(final Class theClass) {
			        this.fName = theClass.getName();
			        try {
			            TestSuite.getTestConstructor(theClass); // Avoid generating multiple
			                                                    // error messages
			        } catch (final NoSuchMethodException e) {
			            this.addTest(TestSuite.warning("Class " + theClass.getName() //$NON-NLS-1$
			                    + " has no public constructor TestCase(String name) or TestCase()")); //$NON-NLS-1$
			            return;
			        }
			
			        if (!Modifier.isPublic(theClass.getModifiers())) {
			            this.addTest(TestSuite
			                    .warning("Class " + theClass.getName() + " is not public")); //$NON-NLS-1$ //$NON-NLS-2$
			            return;
			        }
			
			        Class superClass = theClass;
			        final Vector names = new Vector();
			        while (Test.class.isAssignableFrom(superClass)) {
			            final Method[] methods = superClass.getDeclaredMethods();
			            for (final Method method : methods) {
			                this.addTestMethod(method, names, theClass);
			            }
			            superClass = superClass.getSuperclass();
			        }
			        if (this.fTests.size() == 0) {
			            this.addTest(TestSuite
			                    .warning("No tests found in " + theClass.getName())); //$NON-NLS-1$
			        }
			    }
			
			    /**
			     * Constructs a TestSuite from the given class with the given name.
			     *\s
			     * @see TestSuite#TestSuite(Class)
			     */
			    public TestSuite(final Class theClass, final String name) {
			        this(theClass);
			        this.setName(name);
			    }
			
			    /**
			     * Constructs an empty TestSuite.
			     */
			    public TestSuite(final String name) {
			        this.setName(name);
			    }
			
			    /**
			     * Adds a test to the suite.
			     */
			    public void addTest(final Test test) {
			        this.fTests.addElement(test);
			    }
			
			    private void addTestMethod(final Method m, final Vector names,
			            final Class theClass) {
			        final String name = m.getName();
			        if (names.contains(name)) {
			            return;
			        }
			        if (!this.isPublicTestMethod(m)) {
			            if (this.isTestMethod(m)) {
			                this.addTest(TestSuite
			                        .warning("Test method isn't public: " + m.getName())); //$NON-NLS-1$
			            }
			            return;
			        }
			        names.addElement(name);
			        this.addTest(TestSuite.createTest(theClass, name));
			    }
			
			    /**
			     * Adds the tests from the given class to the suite
			     */
			    public void addTestSuite(final Class testClass) {
			        this.addTest(new TestSuite(testClass));
			    }
			
			    /**
			     * Counts the number of test cases that will be run by this test.
			     */
			    public int countTestCases() {
			        int count = 0;
			        for (final Enumeration e = this.tests(); e.hasMoreElements();) {
			            final Test test = (Test) e.nextElement();
			            count = count + test.countTestCases();
			        }
			        return count;
			    }
			
			    /**
			     * Returns the name of the suite. Not all test suites have a name and this
			     * method can return null.
			     */
			    public String getName() {
			        return this.fName;
			    }
			
			    private boolean isPublicTestMethod(final Method m) {
			        return this.isTestMethod(m) && Modifier.isPublic(m.getModifiers());
			    }
			
			    private boolean isTestMethod(final Method m) {
			        final String name = m.getName();
			        final Class[] parameters = m.getParameterTypes();
			        final Class returnType = m.getReturnType();
			        return (parameters.length == 0) && name.startsWith("test") //$NON-NLS-1$
			                && returnType.equals(Void.TYPE);
			    }
			
			    /**
			     * Runs the tests and collects their result in a TestResult.
			     */
			    public void run(final TestResult result) {
			        for (final Enumeration e = this.tests(); e.hasMoreElements();) {
			            if (result.shouldStop()) {
			                break;
			            }
			            final Test test = (Test) e.nextElement();
			            this.runTest(test, result);
			        }
			    }
			
			    public void runTest(final Test test, final TestResult result) {
			        test.run(result);
			    }
			
			    /**
			     * Sets the name of the suite.
			     *\s
			     * @param name The name to set
			     */
			    public void setName(final String name) {
			        this.fName = name;
			    }
			
			    /**
			     * Returns the test at the given index
			     */
			    public Test testAt(final int index) {
			        return (Test) this.fTests.elementAt(index);
			    }
			
			    /**
			     * Returns the number of tests in this suite
			     */
			    public int testCount() {
			        return this.fTests.size();
			    }
			
			    /**
			     * Returns the tests as an enumeration
			     */
			    public Enumeration tests() {
			        return this.fTests.elements();
			    }
			
			    /**
			     */
			    @Override
			    public String toString() {
			        if (this.getName() != null) {
			            return this.getName();
			        }
			        return super.toString();
			    }
			}""";
        fExpectedChangesAllTests.put("junit.framework.TestSuite.java", str29);
        String str30= """
			package junit.extensions;
			
			import junit.framework.Assert;
			import junit.framework.TestCase;
			
			/**
			 * A TestCase that expects an Exception of class fExpected to be thrown. The
			 * other way to check that an expected exception is thrown is:
			 *\s
			 * <pre>
			 * try {
			 *     shouldThrow();
			 * } catch (SpecialException e) {
			 *     return;
			 * }
			 * fail("Expected SpecialException");
			 * </pre>
			 *
			 * To use ExceptionTestCase, create a TestCase like:
			 *\s
			 * <pre>
			 * new ExceptionTestCase("testShouldThrow", SpecialException.class);
			 * </pre>
			 */
			public class ExceptionTestCase extends TestCase {
			    Class fExpected;
			
			    public ExceptionTestCase(final String name, final Class exception) {
			        super(name);
			        this.fExpected = exception;
			    }
			    /**
			     * Execute the test method expecting that an Exception of class fExpected or
			     * one of its subclasses will be thrown
			     */
			    @Override
			    protected void runTest() throws Throwable {
			        try {
			            super.runTest();
			        } catch (final Exception e) {
			            if (this.fExpected.isAssignableFrom(e.getClass())) {
			                return;
			            } else {
			                throw e;
			            }
			        }
			        Assert.fail("Expected exception " + this.fExpected); //$NON-NLS-1$
			    }
			}""";
        fExpectedChangesAllTests.put("junit.extensions.ExceptionTestCase.java", str30);
        String str31= """
			package junit.framework;
			
			/**
			 * A set of assert methods. Messages are only displayed when an assert fails.
			 */
			
			public class Assert {
			    /**
			     * Asserts that two booleans are equal.
			     */
			    static public void assertEquals(final boolean expected,
			            final boolean actual) {
			        Assert.assertEquals(null, expected, actual);
			    }
			
			    /**
			     * Asserts that two bytes are equal.
			     */
			    static public void assertEquals(final byte expected, final byte actual) {
			        Assert.assertEquals(null, expected, actual);
			    }
			    /**
			     * Asserts that two chars are equal.
			     */
			    static public void assertEquals(final char expected, final char actual) {
			        Assert.assertEquals(null, expected, actual);
			    }
			    /**
			     * Asserts that two doubles are equal concerning a delta. If the expected
			     * value is infinity then the delta value is ignored.
			     */
			    static public void assertEquals(final double expected, final double actual,
			            final double delta) {
			        Assert.assertEquals(null, expected, actual, delta);
			    }
			    /**
			     * Asserts that two floats are equal concerning a delta. If the expected
			     * value is infinity then the delta value is ignored.
			     */
			    static public void assertEquals(final float expected, final float actual,
			            final float delta) {
			        Assert.assertEquals(null, expected, actual, delta);
			    }
			    /**
			     * Asserts that two ints are equal.
			     */
			    static public void assertEquals(final int expected, final int actual) {
			        Assert.assertEquals(null, expected, actual);
			    }
			    /**
			     * Asserts that two longs are equal.
			     */
			    static public void assertEquals(final long expected, final long actual) {
			        Assert.assertEquals(null, expected, actual);
			    }
			    /**
			     * Asserts that two objects are equal. If they are not an
			     * AssertionFailedError is thrown.
			     */
			    static public void assertEquals(final Object expected,
			            final Object actual) {
			        Assert.assertEquals(null, expected, actual);
			    }
			    /**
			     * Asserts that two shorts are equal.
			     */
			    static public void assertEquals(final short expected, final short actual) {
			        Assert.assertEquals(null, expected, actual);
			    }
			    /**
			     * Asserts that two booleans are equal. If they are not an
			     * AssertionFailedError is thrown with the given message.
			     */
			    static public void assertEquals(final String message,
			            final boolean expected, final boolean actual) {
			        Assert.assertEquals(message, new Boolean(expected),
			                new Boolean(actual));
			    }
			    /**
			     * Asserts that two bytes are equal. If they are not an AssertionFailedError
			     * is thrown with the given message.
			     */
			    static public void assertEquals(final String message, final byte expected,
			            final byte actual) {
			        Assert.assertEquals(message, new Byte(expected), new Byte(actual));
			    }
			    /**
			     * Asserts that two chars are equal. If they are not an AssertionFailedError
			     * is thrown with the given message.
			     */
			    static public void assertEquals(final String message, final char expected,
			            final char actual) {
			        Assert.assertEquals(message, new Character(expected),
			                new Character(actual));
			    }
			    /**
			     * Asserts that two doubles are equal concerning a delta. If they are not an
			     * AssertionFailedError is thrown with the given message. If the expected
			     * value is infinity then the delta value is ignored.
			     */
			    static public void assertEquals(final String message, final double expected,
			            final double actual, final double delta) {
			        // handle infinity specially since subtracting to infinite values gives
			        // NaN and the
			        // the following test fails
			        if (Double.isInfinite(expected)) {
			            if (!(expected == actual)) {
			                Assert.failNotEquals(message, new Double(expected),
			                        new Double(actual));
			            }
			        } else if (!(Math.abs(expected - actual) <= delta)) { // Because
			                                                              // comparison with
			                                                              // NaN always
			                                                              // returns false
			            Assert.failNotEquals(message, new Double(expected),
			                    new Double(actual));
			        }
			    }
			    /**
			     * Asserts that two floats are equal concerning a delta. If they are not an
			     * AssertionFailedError is thrown with the given message. If the expected
			     * value is infinity then the delta value is ignored.
			     */
			    static public void assertEquals(final String message, final float expected,
			            final float actual, final float delta) {
			        // handle infinity specially since subtracting to infinite values gives
			        // NaN and the
			        // the following test fails
			        if (Float.isInfinite(expected)) {
			            if (!(expected == actual)) {
			                Assert.failNotEquals(message, new Float(expected),
			                        new Float(actual));
			            }
			        } else if (!(Math.abs(expected - actual) <= delta)) {
			            Assert.failNotEquals(message, new Float(expected),
			                    new Float(actual));
			        }
			    }
			    /**
			     * Asserts that two ints are equal. If they are not an AssertionFailedError
			     * is thrown with the given message.
			     */
			    static public void assertEquals(final String message, final int expected,
			            final int actual) {
			        Assert.assertEquals(message, new Integer(expected),
			                new Integer(actual));
			    }
			    /**
			     * Asserts that two longs are equal. If they are not an AssertionFailedError
			     * is thrown with the given message.
			     */
			    static public void assertEquals(final String message, final long expected,
			            final long actual) {
			        Assert.assertEquals(message, new Long(expected), new Long(actual));
			    }
			    /**
			     * Asserts that two objects are equal. If they are not an
			     * AssertionFailedError is thrown with the given message.
			     */
			    static public void assertEquals(final String message, final Object expected,
			            final Object actual) {
			        if ((expected == null) && (actual == null)) {
			            return;
			        }
			        if ((expected != null) && expected.equals(actual)) {
			            return;
			        }
			        Assert.failNotEquals(message, expected, actual);
			    }
			    /**
			     * Asserts that two shorts are equal. If they are not an
			     * AssertionFailedError is thrown with the given message.
			     */
			    static public void assertEquals(final String message, final short expected,
			            final short actual) {
			        Assert.assertEquals(message, new Short(expected), new Short(actual));
			    }
			    /**
			     * Asserts that two Strings are equal.
			     */
			    static public void assertEquals(final String expected,
			            final String actual) {
			        Assert.assertEquals(null, expected, actual);
			    }
			    /**
			     * Asserts that two Strings are equal.
			     */
			    static public void assertEquals(final String message, final String expected,
			            final String actual) {
			        if ((expected == null) && (actual == null)) {
			            return;
			        }
			        if ((expected != null) && expected.equals(actual)) {
			            return;
			        }
			        throw new ComparisonFailure(message, expected, actual);
			    }
			    /**
			     * Asserts that a condition is false. If it isn't it throws an
			     * AssertionFailedError.
			     */
			    static public void assertFalse(final boolean condition) {
			        Assert.assertFalse(null, condition);
			    }
			    /**
			     * Asserts that a condition is false. If it isn't it throws an
			     * AssertionFailedError with the given message.
			     */
			    static public void assertFalse(final String message,
			            final boolean condition) {
			        Assert.assertTrue(message, !condition);
			    }
			    /**
			     * Asserts that an object isn't null.
			     */
			    static public void assertNotNull(final Object object) {
			        Assert.assertNotNull(null, object);
			    }
			    /**
			     * Asserts that an object isn't null. If it is an AssertionFailedError is
			     * thrown with the given message.
			     */
			    static public void assertNotNull(final String message,
			            final Object object) {
			        Assert.assertTrue(message, object != null);
			    }
			    /**
			     * Asserts that two objects refer to the same object. If they are not the
			     * same an AssertionFailedError is thrown.
			     */
			    static public void assertNotSame(final Object expected,
			            final Object actual) {
			        Assert.assertNotSame(null, expected, actual);
			    }
			    /**
			     * Asserts that two objects refer to the same object. If they are not an
			     * AssertionFailedError is thrown with the given message.
			     */
			    static public void assertNotSame(final String message,
			            final Object expected, final Object actual) {
			        if (expected == actual) {
			            Assert.failSame(message);
			        }
			    }
			    /**
			     * Asserts that an object is null.
			     */
			    static public void assertNull(final Object object) {
			        Assert.assertNull(null, object);
			    }
			    /**
			     * Asserts that an object is null. If it is not an AssertionFailedError is
			     * thrown with the given message.
			     */
			    static public void assertNull(final String message, final Object object) {
			        Assert.assertTrue(message, object == null);
			    }
			    /**
			     * Asserts that two objects refer to the same object. If they are not the
			     * same an AssertionFailedError is thrown.
			     */
			    static public void assertSame(final Object expected, final Object actual) {
			        Assert.assertSame(null, expected, actual);
			    }
			    /**
			     * Asserts that two objects refer to the same object. If they are not an
			     * AssertionFailedError is thrown with the given message.
			     */
			    static public void assertSame(final String message, final Object expected,
			            final Object actual) {
			        if (expected == actual) {
			            return;
			        }
			        Assert.failNotSame(message, expected, actual);
			    }
			    /**
			     * Asserts that a condition is true. If it isn't it throws an
			     * AssertionFailedError.
			     */
			    static public void assertTrue(final boolean condition) {
			        Assert.assertTrue(null, condition);
			    }
			    /**
			     * Asserts that a condition is true. If it isn't it throws an
			     * AssertionFailedError with the given message.
			     */
			    static public void assertTrue(final String message,
			            final boolean condition) {
			        if (!condition) {
			            Assert.fail(message);
			        }
			    }
			    /**
			     * Fails a test with no message.
			     */
			    static public void fail() {
			        Assert.fail(null);
			    }
			    /**
			     * Fails a test with the given message.
			     */
			    static public void fail(final String message) {
			        throw new AssertionFailedError(message);
			    }
			    static private void failNotEquals(final String message,
			            final Object expected, final Object actual) {
			        Assert.fail(Assert.format(message, expected, actual));
			    }
			
			    static private void failNotSame(final String message, final Object expected,
			            final Object actual) {
			        String formatted = ""; //$NON-NLS-1$
			        if (message != null) {
			            formatted = message + " "; //$NON-NLS-1$
			        }
			        Assert.fail(formatted + "expected same:<" + expected + "> was not:<" //$NON-NLS-1$ //$NON-NLS-2$
			                + actual + ">"); //$NON-NLS-1$
			    }
			
			    static private void failSame(final String message) {
			        String formatted = ""; //$NON-NLS-1$
			        if (message != null) {
			            formatted = message + " "; //$NON-NLS-1$
			        }
			        Assert.fail(formatted + "expected not same"); //$NON-NLS-1$
			    }
			
			    static String format(final String message, final Object expected,
			            final Object actual) {
			        String formatted = ""; //$NON-NLS-1$
			        if (message != null) {
			            formatted = message + " "; //$NON-NLS-1$
			        }
			        return formatted + "expected:<" + expected + "> but was:<" + actual //$NON-NLS-1$ //$NON-NLS-2$
			                + ">"; //$NON-NLS-1$
			    }
			
			    /**
			     * Protect constructor since it is a static only class
			     */
			    protected Assert() {
			    }
			}""";
        fExpectedChangesAllTests.put("junit.framework.Assert.java", str31);
        String str32= """
			package junit.runner;
			
			import java.io.File;
			import java.util.Enumeration;
			import java.util.Hashtable;
			import java.util.StringTokenizer;
			import java.util.Vector;
			
			/**
			 * An implementation of a TestCollector that consults the class path. It
			 * considers all classes on the class path excluding classes in JARs. It leaves
			 * it up to subclasses to decide whether a class is a runnable Test.
			 *
			 * @see TestCollector
			 */
			public abstract class ClassPathTestCollector implements TestCollector {
			
			    static final int SUFFIX_LENGTH = ".class".length(); //$NON-NLS-1$
			
			    public ClassPathTestCollector() {
			    }
			
			    protected String classNameFromFile(final String classFileName) {
			        // convert /a/b.class to a.b
			        final String s = classFileName.substring(0,
			                classFileName.length() - ClassPathTestCollector.SUFFIX_LENGTH);
			        final String s2 = s.replace(File.separatorChar, '.');
			        if (s2.startsWith(".")) { //$NON-NLS-1$
			            return s2.substring(1);
			        }
			        return s2;
			    }
			
			    public Hashtable collectFilesInPath(final String classPath) {
			        final Hashtable result = this
			                .collectFilesInRoots(this.splitClassPath(classPath));
			        return result;
			    }
			
			    Hashtable collectFilesInRoots(final Vector roots) {
			        final Hashtable result = new Hashtable(100);
			        final Enumeration e = roots.elements();
			        while (e.hasMoreElements()) {
			            this.gatherFiles(new File((String) e.nextElement()), "", result); //$NON-NLS-1$
			        }
			        return result;
			    }
			
			    public Enumeration collectTests() {
			        final String classPath = System.getProperty("java.class.path"); //$NON-NLS-1$
			        final Hashtable result = this.collectFilesInPath(classPath);
			        return result.elements();
			    }
			
			    void gatherFiles(final File classRoot, final String classFileName,
			            final Hashtable result) {
			        final File thisRoot = new File(classRoot, classFileName);
			        if (thisRoot.isFile()) {
			            if (this.isTestClass(classFileName)) {
			                final String className = this.classNameFromFile(classFileName);
			                result.put(className, className);
			            }
			            return;
			        }
			        final String[] contents = thisRoot.list();
			        if (contents != null) {
			            for (final String content : contents) {
			                this.gatherFiles(classRoot,
			                        classFileName + File.separatorChar + content, result);
			            }
			        }
			    }
			
			    protected boolean isTestClass(final String classFileName) {
			        return classFileName.endsWith(".class") && //$NON-NLS-1$
			                (classFileName.indexOf('$') < 0)
			                && (classFileName.indexOf("Test") > 0); //$NON-NLS-1$
			    }
			
			    Vector splitClassPath(final String classPath) {
			        final Vector result = new Vector();
			        final String separator = System.getProperty("path.separator"); //$NON-NLS-1$
			        final StringTokenizer tokenizer = new StringTokenizer(classPath,
			                separator);
			        while (tokenizer.hasMoreTokens()) {
			            result.addElement(tokenizer.nextToken());
			        }
			        return result;
			    }
			}
			""";
        fExpectedChangesAllTests.put("junit.runner.ClassPathTestCollector.java", str32);
        String str33= """
			package junit.framework;
			
			/**
			 * A Listener for test progress
			 */
			public interface TestListener {
			    /**
			     * An error occurred.
			     */
			    void addError(Test test, Throwable t);
			    /**
			     * A failure occurred.
			     */
			    void addFailure(Test test, AssertionFailedError t);
			    /**
			     * A test ended.
			     */
			    void endTest(Test test);
			    /**
			     * A test started.
			     */
			    void startTest(Test test);
			}""";
        fExpectedChangesAllTests.put("junit.framework.TestListener.java", str33);
        String str34= """
			package junit.tests.extensions;
			
			import junit.extensions.ActiveTestSuite;
			import junit.extensions.RepeatedTest;
			import junit.framework.Assert;
			import junit.framework.Test;
			import junit.framework.TestCase;
			import junit.framework.TestResult;
			
			/**
			 * Testing the ActiveTest support
			 */
			
			public class ActiveTestTest extends TestCase {
			
			    public static class SuccessTest extends TestCase {
			        @Override
			        public void runTest() {
			        }
			    }
			
			    ActiveTestSuite createActiveTestSuite() {
			        final ActiveTestSuite suite = new ActiveTestSuite();
			        for (int i = 0; i < 100; i++) {
			            suite.addTest(new SuccessTest());
			        }
			        return suite;
			    }
			
			    public void testActiveRepeatedTest() {
			        final Test test = new RepeatedTest(this.createActiveTestSuite(), 5);
			        final TestResult result = new TestResult();
			        test.run(result);
			        Assert.assertEquals(500, result.runCount());
			        Assert.assertEquals(0, result.failureCount());
			        Assert.assertEquals(0, result.errorCount());
			    }
			
			    public void testActiveRepeatedTest0() {
			        final Test test = new RepeatedTest(this.createActiveTestSuite(), 0);
			        final TestResult result = new TestResult();
			        test.run(result);
			        Assert.assertEquals(0, result.runCount());
			        Assert.assertEquals(0, result.failureCount());
			        Assert.assertEquals(0, result.errorCount());
			    }
			
			    public void testActiveRepeatedTest1() {
			        final Test test = new RepeatedTest(this.createActiveTestSuite(), 1);
			        final TestResult result = new TestResult();
			        test.run(result);
			        Assert.assertEquals(100, result.runCount());
			        Assert.assertEquals(0, result.failureCount());
			        Assert.assertEquals(0, result.errorCount());
			    }
			
			    public void testActiveTest() {
			        final Test test = this.createActiveTestSuite();
			        final TestResult result = new TestResult();
			        test.run(result);
			        Assert.assertEquals(100, result.runCount());
			        Assert.assertEquals(0, result.failureCount());
			        Assert.assertEquals(0, result.errorCount());
			    }
			
			}""";
        fExpectedChangesAllTests.put("junit.tests.extensions.ActiveTestTest.java", str34);
        String str35= """
			package junit.framework;
			
			/**
			 * A <em>Protectable</em> can be run and can throw a Throwable.
			 *
			 * @see TestResult
			 */
			public interface Protectable {
			
			    /**
			     * Run the the following method protected.
			     */
			    void protect() throws Throwable;
			}""";
        fExpectedChangesAllTests.put("junit.framework.Protectable.java", str35);
        String str36= """
			package junit.samples.money;
			
			/**
			 * The common interface for simple Monies and MoneyBags
			 *
			 */
			public interface IMoney {
			    /**
			     * Adds a money to this money.
			     */
			    IMoney add(IMoney m);
			    /**
			     * Adds a simple Money to this money. This is a helper method for
			     * implementing double dispatch
			     */
			    IMoney addMoney(Money m);
			    /**
			     * Adds a MoneyBag to this money. This is a helper method for implementing
			     * double dispatch
			     */
			    IMoney addMoneyBag(MoneyBag s);
			    /**
			     * Append this to a MoneyBag m.
			     */
			    void appendTo(MoneyBag m);
			    /**
			     * Tests whether this money is zero
			     */
			    boolean isZero();
			    /**
			     * Multiplies a money by the given factor.
			     */
			    IMoney multiply(int factor);
			    /**
			     * Negates this money.
			     */
			    IMoney negate();
			    /**
			     * Subtracts a money from this money.
			     */
			    IMoney subtract(IMoney m);
			}""";
        fExpectedChangesAllTests.put("junit.samples.money.IMoney.java", str36);
        String str37= """
			package junit.textui;
			
			import java.io.PrintStream;
			
			import junit.framework.Test;
			import junit.framework.TestResult;
			import junit.framework.TestSuite;
			import junit.runner.BaseTestRunner;
			import junit.runner.StandardTestSuiteLoader;
			import junit.runner.TestSuiteLoader;
			import junit.runner.Version;
			
			/**
			 * A command line based tool to run tests.
			 *\s
			 * <pre>
			 * java junit.textui.TestRunner [-wait] TestCaseClass
			 * </pre>
			 *\s
			 * TestRunner expects the name of a TestCase class as argument. If this class
			 * defines a static <code>suite</code> method it will be invoked and the
			 * returned test is run. Otherwise all the methods starting with "test" having
			 * no arguments are run.
			 * <p>
			 * When the wait command line argument is given TestRunner waits until the users
			 * types RETURN.
			 * <p>
			 * TestRunner prints a trace as the tests are executed followed by a summary at
			 * the end.
			 */
			public class TestRunner extends BaseTestRunner {
			    public static final int EXCEPTION_EXIT = 2;
			
			    public static final int FAILURE_EXIT = 1;
			    public static final int SUCCESS_EXIT = 0;
			    public static void main(final String args[]) {
			        final TestRunner aTestRunner = new TestRunner();
			        try {
			            final TestResult r = aTestRunner.start(args);
			            if (!r.wasSuccessful()) {
			                System.exit(TestRunner.FAILURE_EXIT);
			            }
			            System.exit(TestRunner.SUCCESS_EXIT);
			        } catch (final Exception e) {
			            System.err.println(e.getMessage());
			            System.exit(TestRunner.EXCEPTION_EXIT);
			        }
			    }
			
			    /**
			     * Runs a suite extracted from a TestCase subclass.
			     */
			    static public void run(final Class testClass) {
			        TestRunner.run(new TestSuite(testClass));
			    }
			
			    /**
			     * Runs a single test and collects its results. This method can be used to
			     * start a test run from your program.
			     *\s
			     * <pre>
			     * public static void main(String[] args) {
			     *     test.textui.TestRunner.run(suite());
			     * }
			     * </pre>
			     */
			    static public TestResult run(final Test test) {
			        final TestRunner runner = new TestRunner();
			        return runner.doRun(test);
			    }
			
			    /**
			     * Runs a single test and waits until the user types RETURN.
			     */
			    static public void runAndWait(final Test suite) {
			        final TestRunner aTestRunner = new TestRunner();
			        aTestRunner.doRun(suite, true);
			    }
			
			    private ResultPrinter fPrinter;
			
			    /**
			     * Constructs a TestRunner.
			     */
			    public TestRunner() {
			        this(System.out);
			    }
			
			    /**
			     * Constructs a TestRunner using the given stream for all the output
			     */
			    public TestRunner(final PrintStream writer) {
			        this(new ResultPrinter(writer));
			    }
			
			    /**
			     * Constructs a TestRunner using the given ResultPrinter all the output
			     */
			    public TestRunner(final ResultPrinter printer) {
			        this.fPrinter = printer;
			    }
			
			    /**
			     * Creates the TestResult to be used for the test run.
			     */
			    protected TestResult createTestResult() {
			        return new TestResult();
			    }
			
			    public TestResult doRun(final Test test) {
			        return this.doRun(test, false);
			    }
			
			    public TestResult doRun(final Test suite, final boolean wait) {
			        final TestResult result = this.createTestResult();
			        result.addListener(this.fPrinter);
			        final long startTime = System.currentTimeMillis();
			        suite.run(result);
			        final long endTime = System.currentTimeMillis();
			        final long runTime = endTime - startTime;
			        this.fPrinter.print(result, runTime);
			
			        this.pause(wait);
			        return result;
			    }
			
			    /**
			     * Always use the StandardTestSuiteLoader. Overridden from BaseTestRunner.
			     */
			    @Override
			    public TestSuiteLoader getLoader() {
			        return new StandardTestSuiteLoader();
			    }
			
			    protected void pause(final boolean wait) {
			        if (!wait) {
			            return;
			        }
			        this.fPrinter.printWaitPrompt();
			        try {
			            System.in.read();
			        } catch (final Exception e) {
			        }
			    }
			
			    @Override
			    protected void runFailed(final String message) {
			        System.err.println(message);
			        System.exit(TestRunner.FAILURE_EXIT);
			    }
			
			    public void setPrinter(final ResultPrinter printer) {
			        this.fPrinter = printer;
			    }
			
			    /**
			     * Starts a test run. Analyzes the command line arguments and runs the given
			     * test suite.
			     */
			    protected TestResult start(final String args[]) throws Exception {
			        String testCase = ""; //$NON-NLS-1$
			        boolean wait = false;
			
			        for (int i = 0; i < args.length; i++) {
			            if (args[i].equals("-wait")) { //$NON-NLS-1$
			                wait = true;
			            } else if (args[i].equals("-c")) { //$NON-NLS-1$
			                testCase = this.extractClassName(args[++i]);
			            } else if (args[i].equals("-v")) { //$NON-NLS-1$
			                System.err.println("JUnit " + Version.id() //$NON-NLS-1$
			                        + " by Kent Beck and Erich Gamma"); //$NON-NLS-1$
			            } else { // $NON-NLS-1$
			                testCase = args[i];
			            }
			        }
			
			        if (testCase.equals("")) { // $NON-NLS-1$
			            throw new Exception(
			                    "Usage: TestRunner [-wait] testCaseName, where name is the name of the TestCase class"); //$NON-NLS-1$
			        }
			
			        try {
			            final Test suite = this.getTest(testCase);
			            return this.doRun(suite, wait);
			        } catch (final Exception e) {
			            throw new Exception("Could not create and run test suite: " + e); //$NON-NLS-1$
			        }
			    }
			
			    @Override
			    public void testEnded(final String testName) {
			    }
			
			    @Override
			    public void testFailed(final int status, final Test test,
			            final Throwable t) {
			    }
			
			    @Override
			    public void testStarted(final String testName) {
			    }
			
			}""";
        fExpectedChangesAllTests.put("junit.textui.TestRunner.java", str37);
        String str38= """
			package junit.tests.runner;
			
			/**
			 * Test class used in TestTestCaseClassLoader
			 */
			import junit.framework.Assert;
			import junit.framework.TestCase;
			
			public class ClassLoaderTest extends Assert {
			    public ClassLoaderTest() {
			    }
			    private boolean isTestCaseClassLoader(final ClassLoader cl) {
			        return ((cl != null) && cl.getClass().getName()
			                .equals(junit.runner.TestCaseClassLoader.class.getName()));
			    }
			    public void verify() {
			        this.verifyApplicationClassLoadedByTestLoader();
			        this.verifySystemClassNotLoadedByTestLoader();
			    }
			    private void verifyApplicationClassLoadedByTestLoader() {
			        Assert.assertTrue(
			                this.isTestCaseClassLoader(this.getClass().getClassLoader()));
			    }
			    private void verifySystemClassNotLoadedByTestLoader() {
			        Assert.assertTrue(
			                !this.isTestCaseClassLoader(Object.class.getClassLoader()));
			        Assert.assertTrue(
			                !this.isTestCaseClassLoader(TestCase.class.getClassLoader()));
			    }
			}""";
        fExpectedChangesAllTests.put("junit.tests.runner.ClassLoaderTest.java", str38);
        String str39= """
			package junit.runner;
			/**
			 * A listener interface for observing the execution of a test run. Unlike
			 * TestListener, this interface using only primitive objects, making it suitable
			 * for remote test execution.
			 */
			public interface TestRunListener {
			    /* test status constants */
			    int STATUS_ERROR = 1;
			    int STATUS_FAILURE = 2;
			
			    void testEnded(String testName);
			    void testFailed(int status, String testName, String trace);
			    void testRunEnded(long elapsedTime);
			    void testRunStarted(String testSuiteName, int testCount);
			    void testRunStopped(long elapsedTime);
			    void testStarted(String testName);
			}
			""";
        fExpectedChangesAllTests.put("junit.runner.TestRunListener.java", str39);
        String str40= """
			
			package junit.tests.runner;
			
			import java.io.ByteArrayOutputStream;
			import java.io.OutputStream;
			import java.io.PrintStream;
			
			import junit.framework.Assert;
			import junit.framework.AssertionFailedError;
			import junit.framework.TestCase;
			import junit.framework.TestResult;
			import junit.framework.TestSuite;
			import junit.textui.ResultPrinter;
			import junit.textui.TestRunner;
			
			public class TextFeedbackTest extends TestCase {
			    class TestResultPrinter extends ResultPrinter {
			        TestResultPrinter(final PrintStream writer) {
			            super(writer);
			        }
			
			        /*
			         * Spoof printing time so the tests are deterministic
			         */
			        @Override
			        protected String elapsedTimeAsString(final long runTime) {
			            return "0"; //$NON-NLS-1$
			        }
			    }
			    public static void main(final String[] args) {
			        TestRunner.run(TextFeedbackTest.class);
			    }
			
			    OutputStream output;
			
			    TestRunner runner;
			
			    private String expected(final String[] lines) {
			        final OutputStream expected = new ByteArrayOutputStream();
			        final PrintStream expectedWriter = new PrintStream(expected);
			        for (final String line : lines) {
			            expectedWriter.println(line);
			        }
			        return expected.toString();
			    }
			
			    @Override
			    public void setUp() {
			        this.output = new ByteArrayOutputStream();
			        this.runner = new TestRunner(
			                new TestResultPrinter(new PrintStream(this.output)));
			    }
			
			    public void testEmptySuite() {
			        final String expected = this
			                .expected(new String[]{"", "Time: 0", "", "OK (0 tests)", ""}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			        this.runner.doRun(new TestSuite());
			        Assert.assertEquals(expected.toString(), this.output.toString());
			    }
			
			    public void testError() {
			        final String expected = this.expected(
			                new String[]{".E", "Time: 0", "Errors here", "", "FAILURES!!!", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			                        "Tests run: 1,  Failures: 0,  Errors: 1", ""}); //$NON-NLS-1$ //$NON-NLS-2$
			        final ResultPrinter printer = new TestResultPrinter(
			                new PrintStream(this.output)) {
			            @Override
			            public void printErrors(final TestResult result) {
			                this.getWriter().println("Errors here"); //$NON-NLS-1$
			            }
			        };
			        this.runner.setPrinter(printer);
			        final TestSuite suite = new TestSuite();
			        suite.addTest(new TestCase() {
			            @Override
			            public void runTest() throws Exception {
			                throw new Exception();
			            }
			        });
			        this.runner.doRun(suite);
			        Assert.assertEquals(expected.toString(), this.output.toString());
			    }
			
			    public void testFailure() {
			        final String expected = this.expected(new String[]{".F", "Time: 0", //$NON-NLS-1$ //$NON-NLS-2$
			                "Failures here", "", "FAILURES!!!", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			                "Tests run: 1,  Failures: 1,  Errors: 0", ""}); //$NON-NLS-1$ //$NON-NLS-2$
			        final ResultPrinter printer = new TestResultPrinter(
			                new PrintStream(this.output)) {
			            @Override
			            public void printFailures(final TestResult result) {
			                this.getWriter().println("Failures here"); //$NON-NLS-1$
			            }
			        };
			        this.runner.setPrinter(printer);
			        final TestSuite suite = new TestSuite();
			        suite.addTest(new TestCase() {
			            @Override
			            public void runTest() {
			                throw new AssertionFailedError();
			            }
			        });
			        this.runner.doRun(suite);
			        Assert.assertEquals(expected.toString(), this.output.toString());
			    }
			
			    public void testOneTest() {
			        final String expected = this
			                .expected(new String[]{".", "Time: 0", "", "OK (1 test)", ""}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			        final TestSuite suite = new TestSuite();
			        suite.addTest(new TestCase() {
			            @Override
			            public void runTest() {
			            }
			        });
			        this.runner.doRun(suite);
			        Assert.assertEquals(expected.toString(), this.output.toString());
			    }
			
			    public void testTwoTests() {
			        final String expected = this.expected(
			                new String[]{"..", "Time: 0", "", "OK (2 tests)", ""}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			        final TestSuite suite = new TestSuite();
			        suite.addTest(new TestCase() {
			            @Override
			            public void runTest() {
			            }
			        });
			        suite.addTest(new TestCase() {
			            @Override
			            public void runTest() {
			            }
			        });
			        this.runner.doRun(suite);
			        Assert.assertEquals(expected.toString(), this.output.toString());
			    }
			
			}
			""";
        fExpectedChangesAllTests.put("junit.tests.runner.TextFeedbackTest.java", str40);
        String str41= """
			package junit.tests.extensions;
			
			import junit.extensions.TestSetup;
			import junit.framework.Assert;
			import junit.framework.Test;
			import junit.framework.TestCase;
			import junit.framework.TestResult;
			import junit.framework.TestSuite;
			import junit.tests.WasRun;
			
			/**
			 * A test case testing the extensions to the testing framework.
			 *
			 */
			public class ExtensionTest extends TestCase {
			    static class TornDown extends TestSetup {
			        boolean fTornDown = false;
			
			        TornDown(final Test test) {
			            super(test);
			        }
			        @Override
			        protected void tearDown() {
			            this.fTornDown = true;
			        }
			    }
			    public void testRunningErrorInTestSetup() {
			        final TestCase test = new TestCase("failure") { //$NON-NLS-1$
			            @Override
			            public void runTest() {
			                Assert.fail();
			            }
			        };
			
			        final TestSetup wrapper = new TestSetup(test);
			
			        final TestResult result = new TestResult();
			        wrapper.run(result);
			        Assert.assertTrue(!result.wasSuccessful());
			    }
			    public void testRunningErrorsInTestSetup() {
			        final TestCase failure = new TestCase("failure") { //$NON-NLS-1$
			            @Override
			            public void runTest() {
			                Assert.fail();
			            }
			        };
			
			        final TestCase error = new TestCase("error") { //$NON-NLS-1$
			            @Override
			            public void runTest() {
			                throw new Error();
			            }
			        };
			
			        final TestSuite suite = new TestSuite();
			        suite.addTest(failure);
			        suite.addTest(error);
			
			        final TestSetup wrapper = new TestSetup(suite);
			
			        final TestResult result = new TestResult();
			        wrapper.run(result);
			
			        Assert.assertEquals(1, result.failureCount());
			        Assert.assertEquals(1, result.errorCount());
			    }
			    public void testSetupErrorDontTearDown() {
			        final WasRun test = new WasRun();
			
			        final TornDown wrapper = new TornDown(test) {
			            @Override
			            public void setUp() {
			                Assert.fail();
			            }
			        };
			
			        final TestResult result = new TestResult();
			        wrapper.run(result);
			
			        Assert.assertTrue(!wrapper.fTornDown);
			    }
			    public void testSetupErrorInTestSetup() {
			        final WasRun test = new WasRun();
			
			        final TestSetup wrapper = new TestSetup(test) {
			            @Override
			            public void setUp() {
			                Assert.fail();
			            }
			        };
			
			        final TestResult result = new TestResult();
			        wrapper.run(result);
			
			        Assert.assertTrue(!test.fWasRun);
			        Assert.assertTrue(!result.wasSuccessful());
			    }
			}""";
        fExpectedChangesAllTests.put("junit.tests.extensions.ExtensionTest.java", str41);
        String str42= """
			package junit.tests;
			
			import junit.framework.Test;
			import junit.framework.TestSuite;
			
			/**
			 * TestSuite that runs all the JUnit tests
			 *
			 */
			public class AllTests {
			
			    public static void main(final String[] args) {
			        junit.textui.TestRunner.run(AllTests.suite());
			    }
			
			    public static Test suite() {
			        final TestSuite suite = new TestSuite("Framework Tests"); //$NON-NLS-1$
			        suite.addTest(junit.tests.framework.AllTests.suite());
			        suite.addTest(junit.tests.runner.AllTests.suite());
			        suite.addTest(junit.tests.extensions.AllTests.suite());
			        return suite;
			    }
			}""";
        fExpectedChangesAllTests.put("junit.tests.AllTests.java", str42);
        String str43= """
			package junit.tests.runner;
			
			/**
			 * Test class used in TestTestCaseClassLoader
			 */
			import junit.framework.Assert;
			
			public class LoadedFromJar extends Assert {
			    private boolean isTestCaseClassLoader(final ClassLoader cl) {
			        return ((cl != null) && cl.getClass().getName()
			                .equals(junit.runner.TestCaseClassLoader.class.getName()));
			    }
			    public void verify() {
			        this.verifyApplicationClassLoadedByTestLoader();
			    }
			    private void verifyApplicationClassLoadedByTestLoader() {
			        Assert.assertTrue(
			                this.isTestCaseClassLoader(this.getClass().getClassLoader()));
			    }
			}""";
        fExpectedChangesAllTests.put("junit.tests.runner.LoadedFromJar.java", str43);
        String str44= """
			package junit.tests.framework;
			
			import junit.framework.Assert;
			import junit.framework.ComparisonFailure;
			import junit.framework.TestCase;
			
			public class ComparisonFailureTest extends TestCase {
			
			    public void testComparisonErrorEndSame() {
			        final ComparisonFailure failure = new ComparisonFailure(null, "ab", //$NON-NLS-1$
			                "cb"); //$NON-NLS-1$
			        Assert.assertEquals("expected:<a...> but was:<c...>", //$NON-NLS-1$
			                failure.getMessage());
			    }
			
			    public void testComparisonErrorEndSameComplete() {
			        final ComparisonFailure failure = new ComparisonFailure(null, "bc", //$NON-NLS-1$
			                "abc"); //$NON-NLS-1$
			        Assert.assertEquals("expected:<...> but was:<a...>", //$NON-NLS-1$
			                failure.getMessage());
			    }
			
			    public void testComparisonErrorMessage() {
			        final ComparisonFailure failure = new ComparisonFailure("a", "b", "c"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			        Assert.assertEquals("a expected:<b> but was:<c>", failure.getMessage()); //$NON-NLS-1$
			    }
			
			    public void testComparisonErrorOverlapingMatches() {
			        final ComparisonFailure failure = new ComparisonFailure(null, "abc", //$NON-NLS-1$
			                "abbc"); //$NON-NLS-1$
			        Assert.assertEquals("expected:<......> but was:<...b...>", //$NON-NLS-1$
			                failure.getMessage());
			    }
			
			    public void testComparisonErrorOverlapingMatches2() {
			        final ComparisonFailure failure = new ComparisonFailure(null, "abcdde", //$NON-NLS-1$
			                "abcde"); //$NON-NLS-1$
			        Assert.assertEquals("expected:<...d...> but was:<......>", //$NON-NLS-1$
			                failure.getMessage());
			    }
			
			    public void testComparisonErrorSame() {
			        final ComparisonFailure failure = new ComparisonFailure(null, "ab", //$NON-NLS-1$
			                "ab"); //$NON-NLS-1$
			        Assert.assertEquals("expected:<ab> but was:<ab>", failure.getMessage()); //$NON-NLS-1$
			    }
			
			    public void testComparisonErrorStartAndEndSame() {
			        final ComparisonFailure failure = new ComparisonFailure(null, "abc", //$NON-NLS-1$
			                "adc"); //$NON-NLS-1$
			        Assert.assertEquals("expected:<...b...> but was:<...d...>", //$NON-NLS-1$
			                failure.getMessage());
			    }
			
			    public void testComparisonErrorStartSame() {
			        final ComparisonFailure failure = new ComparisonFailure(null, "ba", //$NON-NLS-1$
			                "bc"); //$NON-NLS-1$
			        Assert.assertEquals("expected:<...a> but was:<...c>", //$NON-NLS-1$
			                failure.getMessage());
			    }
			
			    public void testComparisonErrorStartSameComplete() {
			        final ComparisonFailure failure = new ComparisonFailure(null, "ab", //$NON-NLS-1$
			                "abc"); //$NON-NLS-1$
			        Assert.assertEquals("expected:<...> but was:<...c>", //$NON-NLS-1$
			                failure.getMessage());
			    }
			
			    public void testComparisonErrorWithActualNull() {
			        final ComparisonFailure failure = new ComparisonFailure(null, "a", //$NON-NLS-1$
			                null);
			        Assert.assertEquals("expected:<a> but was:<null>", //$NON-NLS-1$
			                failure.getMessage());
			    }
			
			    public void testComparisonErrorWithExpectedNull() {
			        final ComparisonFailure failure = new ComparisonFailure(null, null,
			                "a"); //$NON-NLS-1$
			        Assert.assertEquals("expected:<null> but was:<a>", //$NON-NLS-1$
			                failure.getMessage());
			    }
			}
			""";
        fExpectedChangesAllTests.put("junit.tests.framework.ComparisonFailureTest.java", str44);
        String str45= """
			
			package junit.textui;
			
			import java.io.PrintStream;
			import java.text.NumberFormat;
			import java.util.Enumeration;
			
			import junit.framework.AssertionFailedError;
			import junit.framework.Test;
			import junit.framework.TestFailure;
			import junit.framework.TestListener;
			import junit.framework.TestResult;
			import junit.runner.BaseTestRunner;
			
			public class ResultPrinter implements TestListener {
			    int fColumn = 0;
			    PrintStream fWriter;
			
			    public ResultPrinter(final PrintStream writer) {
			        this.fWriter = writer;
			    }
			
			    /*
			     * API for use by textui.TestRunner
			     */
			
			    /**
			     * @see junit.framework.TestListener#addError(Test, Throwable)
			     */
			    public void addError(final Test test, final Throwable t) {
			        this.getWriter().print("E"); //$NON-NLS-1$
			    }
			
			    /**
			     * @see junit.framework.TestListener#addFailure(Test, AssertionFailedError)
			     */
			    public void addFailure(final Test test, final AssertionFailedError t) {
			        this.getWriter().print("F"); //$NON-NLS-1$
			    }
			
			    /*
			     * Internal methods
			     */
			
			    /**
			     * Returns the formatted string of the elapsed time. Duplicated from
			     * BaseTestRunner. Fix it.
			     */
			    protected String elapsedTimeAsString(final long runTime) {
			        return NumberFormat.getInstance().format((double) runTime / 1000);
			    }
			
			    /**
			     * @see junit.framework.TestListener#endTest(Test)
			     */
			    public void endTest(final Test test) {
			    }
			
			    public PrintStream getWriter() {
			        return this.fWriter;
			    }
			
			    synchronized void print(final TestResult result, final long runTime) {
			        this.printHeader(runTime);
			        this.printErrors(result);
			        this.printFailures(result);
			        this.printFooter(result);
			    }
			
			    public void printDefect(final TestFailure booBoo, final int count) { // only
			                                                                         // public
			                                                                         // for
			                                                                         // testing
			                                                                         // purposes
			        this.printDefectHeader(booBoo, count);
			        this.printDefectTrace(booBoo);
			    }
			
			    protected void printDefectHeader(final TestFailure booBoo,
			            final int count) {
			        // I feel like making this a println, then adding a line giving the
			        // throwable a chance to print something
			        // before we get to the stack trace.
			        this.getWriter().print(count + ") " + booBoo.failedTest()); //$NON-NLS-1$
			    }
			
			    protected void printDefects(final Enumeration booBoos, final int count,
			            final String type) {
			        if (count == 0) {
			            return;
			        }
			        if (count == 1) {
			            this.getWriter().println("There was " + count + " " + type + ":"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			        } else {
			            this.getWriter().println("There were " + count + " " + type + "s:"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			        }
			        for (int i = 1; booBoos.hasMoreElements(); i++) {
			            this.printDefect((TestFailure) booBoos.nextElement(), i);
			        }
			    }
			
			    protected void printDefectTrace(final TestFailure booBoo) {
			        this.getWriter().print(BaseTestRunner.getFilteredTrace(booBoo.trace()));
			    }
			
			    protected void printErrors(final TestResult result) {
			        this.printDefects(result.errors(), result.errorCount(), "error"); //$NON-NLS-1$
			    }
			
			    protected void printFailures(final TestResult result) {
			        this.printDefects(result.failures(), result.failureCount(), "failure"); //$NON-NLS-1$
			    }
			    protected void printFooter(final TestResult result) {
			        if (result.wasSuccessful()) {
			            this.getWriter().println();
			            this.getWriter().print("OK"); //$NON-NLS-1$
			            this.getWriter().println(" (" + result.runCount() + " test" //$NON-NLS-1$ //$NON-NLS-2$
			                    + (result.runCount() == 1 ? "" : "s") + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			
			        } else {
			            this.getWriter().println();
			            this.getWriter().println("FAILURES!!!"); //$NON-NLS-1$
			            this.getWriter().println("Tests run: " + result.runCount() + //$NON-NLS-1$
			                    ",  Failures: " + result.failureCount() + //$NON-NLS-1$
			                    ",  Errors: " + result.errorCount()); //$NON-NLS-1$
			        }
			        this.getWriter().println();
			    }
			
			    protected void printHeader(final long runTime) {
			        this.getWriter().println();
			        this.getWriter().println("Time: " + this.elapsedTimeAsString(runTime)); //$NON-NLS-1$
			    }
			
			    void printWaitPrompt() {
			        this.getWriter().println();
			        this.getWriter().println("<RETURN> to continue"); //$NON-NLS-1$
			    }
			
			    /**
			     * @see junit.framework.TestListener#startTest(Test)
			     */
			    public void startTest(final Test test) {
			        this.getWriter().print("."); //$NON-NLS-1$
			        if (this.fColumn++ >= 40) {
			            this.getWriter().println();
			            this.fColumn = 0;
			        }
			    }
			
			}
			""";
        fExpectedChangesAllTests.put("junit.textui.ResultPrinter.java", str45);
        String str46= """
			package junit.samples;
			
			import java.util.Vector;
			
			import junit.framework.Assert;
			import junit.framework.Test;
			import junit.framework.TestCase;
			import junit.framework.TestSuite;
			
			/**
			 * A sample test case, testing <code>java.util.Vector</code>.
			 *
			 */
			public class VectorTest extends TestCase {
			    public static void main(final String[] args) {
			        junit.textui.TestRunner.run(VectorTest.suite());
			    }
			    public static Test suite() {
			        return new TestSuite(VectorTest.class);
			    }
			
			    protected Vector fEmpty;
			    protected Vector fFull;
			    @Override
			    protected void setUp() {
			        this.fEmpty = new Vector();
			        this.fFull = new Vector();
			        this.fFull.addElement(new Integer(1));
			        this.fFull.addElement(new Integer(2));
			        this.fFull.addElement(new Integer(3));
			    }
			    public void testCapacity() {
			        final int size = this.fFull.size();
			        for (int i = 0; i < 100; i++) {
			            this.fFull.addElement(new Integer(i));
			        }
			        Assert.assertTrue(this.fFull.size() == (100 + size));
			    }
			    public void testClone() {
			        final Vector clone = (Vector) this.fFull.clone();
			        Assert.assertTrue(clone.size() == this.fFull.size());
			        Assert.assertTrue(clone.contains(new Integer(1)));
			    }
			    public void testContains() {
			        Assert.assertTrue(this.fFull.contains(new Integer(1)));
			        Assert.assertTrue(!this.fEmpty.contains(new Integer(1)));
			    }
			    public void testElementAt() {
			        final Integer i = (Integer) this.fFull.elementAt(0);
			        Assert.assertTrue(i.intValue() == 1);
			
			        try {
			            this.fFull.elementAt(this.fFull.size());
			        } catch (final ArrayIndexOutOfBoundsException e) {
			            return;
			        }
			        Assert.fail("Should raise an ArrayIndexOutOfBoundsException"); //$NON-NLS-1$
			    }
			    public void testRemoveAll() {
			        this.fFull.removeAllElements();
			        this.fEmpty.removeAllElements();
			        Assert.assertTrue(this.fFull.isEmpty());
			        Assert.assertTrue(this.fEmpty.isEmpty());
			    }
			    public void testRemoveElement() {
			        this.fFull.removeElement(new Integer(3));
			        Assert.assertTrue(!this.fFull.contains(new Integer(3)));
			    }
			}""";
        fExpectedChangesAllTests.put("junit.samples.VectorTest.java", str46);
        String str47= """
			package junit.framework;
			
			/**
			 * Thrown when an assert equals for Strings failed.
			 *\s
			 * Inspired by a patch from Alex Chaffee mailto:alex@purpletech.com
			 */
			public class ComparisonFailure extends AssertionFailedError {
			    /* Test */
			    private static final long serialVersionUID = 1L;
			    private final String fActual;
			    private final String fExpected;
			
			    /**
			     * Constructs a comparison failure.
			     *\s
			     * @param message  the identifying message or null
			     * @param expected the expected string value
			     * @param actual   the actual string value
			     */
			    public ComparisonFailure(final String message, final String expected,
			            final String actual) {
			        super(message);
			        this.fExpected = expected;
			        this.fActual = actual;
			    }
			
			    /**
			     * Returns "..." in place of common prefix and "..." in place of common
			     * suffix between expected and actual.
			     *\s
			     * @see java.lang.Throwable#getMessage()
			     */
			    @Override
			    public String getMessage() {
			        if ((this.fExpected == null) || (this.fActual == null)) {
			            return Assert.format(super.getMessage(), this.fExpected,
			                    this.fActual);
			        }
			
			        final int end = Math.min(this.fExpected.length(),
			                this.fActual.length());
			
			        int i = 0;
			        for (; i < end; i++) {
			            if (this.fExpected.charAt(i) != this.fActual.charAt(i)) {
			                break;
			            }
			        }
			        int j = this.fExpected.length() - 1;
			        int k = this.fActual.length() - 1;
			        for (; (k >= i) && (j >= i); k--, j--) {
			            if (this.fExpected.charAt(j) != this.fActual.charAt(k)) {
			                break;
			            }
			        }
			        String actual, expected;
			
			        // equal strings
			        if ((j < i) && (k < i)) {
			            expected = this.fExpected;
			            actual = this.fActual;
			        } else {
			            expected = this.fExpected.substring(i, j + 1);
			            actual = this.fActual.substring(i, k + 1);
			            if ((i <= end) && (i > 0)) {
			                expected = "..." + expected; //$NON-NLS-1$
			                actual = "..." + actual; //$NON-NLS-1$
			            }
			
			            if (j < (this.fExpected.length() - 1)) {
			                expected = expected + "..."; //$NON-NLS-1$
			            }
			            if (k < (this.fActual.length() - 1)) {
			                actual = actual + "..."; //$NON-NLS-1$
			            }
			        }
			        return Assert.format(super.getMessage(), expected, actual);
			    }
			}""";
        fExpectedChangesAllTests.put("junit.framework.ComparisonFailure.java", str47);
        String str48= """
			package junit.tests.framework;
			
			import junit.framework.Assert;
			import junit.framework.AssertionFailedError;
			import junit.framework.Test;
			import junit.framework.TestCase;
			import junit.framework.TestFailure;
			import junit.framework.TestResult;
			import junit.framework.TestSuite;
			import junit.tests.WasRun;
			
			/**
			 * A test case testing the testing framework.
			 *
			 */
			public class TestCaseTest extends TestCase {
			
			    static class TornDown extends TestCase {
			        boolean fTornDown = false;
			
			        @Override
			        protected void runTest() {
			            throw new Error();
			        }
			        @Override
			        protected void tearDown() {
			            this.fTornDown = true;
			        }
			    }
			
			    public void testCaseToString() {
			        // This test wins the award for twisted snake tail eating while
			        // writing self tests. And you thought those weird anonymous
			        // inner classes were bad...
			        Assert.assertEquals(
			                "testCaseToString(junit.tests.framework.TestCaseTest)", //$NON-NLS-1$
			                this.toString());
			    }
			    public void testError() {
			        final TestCase error = new TestCase("error") { //$NON-NLS-1$
			            @Override
			            protected void runTest() {
			                throw new Error();
			            }
			        };
			        this.verifyError(error);
			    }
			    public void testExceptionRunningAndTearDown() {
			        // This test documents the current behavior. With 1.4, we should
			        // wrap the exception thrown while running with the exception thrown
			        // while tearing down
			        final Test t = new TornDown() {
			            @Override
			            public void tearDown() {
			                throw new Error("tearDown"); //$NON-NLS-1$
			            }
			        };
			        final TestResult result = new TestResult();
			        t.run(result);
			        final TestFailure failure = (TestFailure) result.errors().nextElement();
			        Assert.assertEquals("tearDown", failure.thrownException().getMessage()); //$NON-NLS-1$
			    }
			    public void testFailure() {
			        final TestCase failure = new TestCase("failure") { //$NON-NLS-1$
			            @Override
			            protected void runTest() {
			                Assert.fail();
			            }
			        };
			        this.verifyFailure(failure);
			    }
			    public void testNamelessTestCase() {
			        final TestCase t = new TestCase() {
			        };
			        try {
			            t.run();
			            Assert.fail();
			        } catch (final AssertionFailedError e) {
			        }
			    }
			    public void testNoArgTestCasePasses() {
			        final Test t = new TestSuite(NoArgTestCaseTest.class);
			        final TestResult result = new TestResult();
			        t.run(result);
			        Assert.assertTrue(result.runCount() == 1);
			        Assert.assertTrue(result.failureCount() == 0);
			        Assert.assertTrue(result.errorCount() == 0);
			    }
			
			    public void testRunAndTearDownFails() {
			        final TornDown fails = new TornDown() {
			            @Override
			            protected void runTest() {
			                throw new Error();
			            }
			            @Override
			            protected void tearDown() {
			                super.tearDown();
			                throw new Error();
			            }
			        };
			        this.verifyError(fails);
			        Assert.assertTrue(fails.fTornDown);
			    }
			
			    public void testSetupFails() {
			        final TestCase fails = new TestCase("success") { //$NON-NLS-1$
			            @Override
			            protected void runTest() {
			            }
			            @Override
			            protected void setUp() {
			                throw new Error();
			            }
			        };
			        this.verifyError(fails);
			    }
			    public void testSuccess() {
			        final TestCase success = new TestCase("success") { //$NON-NLS-1$
			            @Override
			            protected void runTest() {
			            }
			        };
			        this.verifySuccess(success);
			    }
			    public void testTearDownAfterError() {
			        final TornDown fails = new TornDown();
			        this.verifyError(fails);
			        Assert.assertTrue(fails.fTornDown);
			    }
			    public void testTearDownFails() {
			        final TestCase fails = new TestCase("success") { //$NON-NLS-1$
			            @Override
			            protected void runTest() {
			            }
			            @Override
			            protected void tearDown() {
			                throw new Error();
			            }
			        };
			        this.verifyError(fails);
			    }
			
			    public void testTearDownSetupFails() {
			        final TornDown fails = new TornDown() {
			            @Override
			            protected void setUp() {
			                throw new Error();
			            }
			        };
			        this.verifyError(fails);
			        Assert.assertTrue(!fails.fTornDown);
			    }
			
			    public void testWasRun() {
			        final WasRun test = new WasRun();
			        test.run();
			        Assert.assertTrue(test.fWasRun);
			    }
			
			    void verifyError(final TestCase test) {
			        final TestResult result = test.run();
			        Assert.assertTrue(result.runCount() == 1);
			        Assert.assertTrue(result.failureCount() == 0);
			        Assert.assertTrue(result.errorCount() == 1);
			    }
			    void verifyFailure(final TestCase test) {
			        final TestResult result = test.run();
			        Assert.assertTrue(result.runCount() == 1);
			        Assert.assertTrue(result.failureCount() == 1);
			        Assert.assertTrue(result.errorCount() == 0);
			    }
			    void verifySuccess(final TestCase test) {
			        final TestResult result = test.run();
			        Assert.assertTrue(result.runCount() == 1);
			        Assert.assertTrue(result.failureCount() == 0);
			        Assert.assertTrue(result.errorCount() == 0);
			    }
			}""";
        fExpectedChangesAllTests.put("junit.tests.framework.TestCaseTest.java", str48);
        String str49= """
			package junit.framework;
			
			import java.io.PrintWriter;
			import java.io.StringWriter;
			
			/**
			 * A <code>TestFailure</code> collects a failed test together with the caught
			 * exception.
			 *\s
			 * @see TestResult
			 */
			public class TestFailure extends Object {
			    protected Test fFailedTest;
			    protected Throwable fThrownException;
			
			    /**
			     * Constructs a TestFailure with the given test and exception.
			     */
			    public TestFailure(final Test failedTest, final Throwable thrownException) {
			        this.fFailedTest = failedTest;
			        this.fThrownException = thrownException;
			    }
			    public String exceptionMessage() {
			        return this.thrownException().getMessage();
			    }
			    /**
			     * Gets the failed test.
			     */
			    public Test failedTest() {
			        return this.fFailedTest;
			    }
			    public boolean isFailure() {
			        return this.thrownException() instanceof AssertionFailedError;
			    }
			    /**
			     * Gets the thrown exception.
			     */
			    public Throwable thrownException() {
			        return this.fThrownException;
			    }
			    /**
			     * Returns a short description of the failure.
			     */
			    @Override
			    public String toString() {
			        final StringBuffer buffer = new StringBuffer();
			        buffer.append(
			                this.fFailedTest + ": " + this.fThrownException.getMessage()); //$NON-NLS-1$
			        return buffer.toString();
			    }
			    public String trace() {
			        final StringWriter stringWriter = new StringWriter();
			        final PrintWriter writer = new PrintWriter(stringWriter);
			        this.thrownException().printStackTrace(writer);
			        final StringBuffer buffer = stringWriter.getBuffer();
			        return buffer.toString();
			    }
			}""";
        fExpectedChangesAllTests.put("junit.framework.TestFailure.java", str49);
        String str50= """
			package junit.runner;
			
			/**
			 * A TestSuite loader that can reload classes.
			 */
			public class ReloadingTestSuiteLoader implements TestSuiteLoader {
			
			    protected TestCaseClassLoader createLoader() {
			        return new TestCaseClassLoader();
			    }
			
			    public Class load(final String suiteClassName)
			            throws ClassNotFoundException {
			        return this.createLoader().loadClass(suiteClassName, true);
			    }
			
			    public Class reload(final Class aClass) throws ClassNotFoundException {
			        return this.createLoader().loadClass(aClass.getName(), true);
			    }
			}""";
        fExpectedChangesAllTests.put("junit.runner.ReloadingTestSuiteLoader.java", str50);
        String str51= """
			package junit.runner;
			
			/**
			 * The standard test suite loader. It can only load the same class once.
			 */
			public class StandardTestSuiteLoader implements TestSuiteLoader {
			    /**
			     * Uses the system class loader to load the test class
			     */
			    public Class load(final String suiteClassName)
			            throws ClassNotFoundException {
			        return Class.forName(suiteClassName);
			    }
			    /**
			     * Uses the system class loader to load the test class
			     */
			    public Class reload(final Class aClass) throws ClassNotFoundException {
			        return aClass;
			    }
			}""";
        fExpectedChangesAllTests.put("junit.runner.StandardTestSuiteLoader.java", str51);
        String str52= """
			package junit.extensions;
			
			import junit.framework.Protectable;
			import junit.framework.Test;
			import junit.framework.TestResult;
			
			/**
			 * A Decorator to set up and tear down additional fixture state. Subclass
			 * TestSetup and insert it into your tests when you want to set up additional
			 * state once before the tests are run.
			 */
			public class TestSetup extends TestDecorator {
			
			    public TestSetup(final Test test) {
			        super(test);
			    }
			    @Override
			    public void run(final TestResult result) {
			        final Protectable p = new Protectable() {
			            public void protect() throws Exception {
			                TestSetup.this.setUp();
			                TestSetup.this.basicRun(result);
			                TestSetup.this.tearDown();
			            }
			        };
			        result.runProtected(this, p);
			    }
			    /**
			     * Sets up the fixture. Override to set up additional fixture state.
			     */
			    protected void setUp() throws Exception {
			    }
			    /**
			     * Tears down the fixture. Override to tear down the additional fixture
			     * state.
			     */
			    protected void tearDown() throws Exception {
			    }
			}""";
        fExpectedChangesAllTests.put("junit.extensions.TestSetup.java", str52);
        String str53= """
			package junit.tests.runner;
			
			import java.io.File;
			import java.io.IOException;
			import java.io.InputStream;
			import java.io.OutputStream;
			import java.io.PrintStream;
			
			import junit.framework.Assert;
			import junit.framework.TestCase;
			import junit.framework.TestResult;
			import junit.framework.TestSuite;
			
			public class TextRunnerTest extends TestCase {
			
			    void execTest(final String testClass, final boolean success)
			            throws Exception {
			        final String java = System.getProperty("java.home") + File.separator //$NON-NLS-1$
			                + "bin" + File.separator + "java"; //$NON-NLS-1$ //$NON-NLS-2$
			        final String cp = System.getProperty("java.class.path"); //$NON-NLS-1$
			        // use -classpath for JDK 1.1.7 compatibility
			        final String[] cmd = {java, "-classpath", cp, "junit.textui.TestRunner", //$NON-NLS-1$ //$NON-NLS-2$
			                testClass};
			        final Process p = Runtime.getRuntime().exec(cmd);
			        final InputStream i = p.getInputStream();
			        while ((i.read()) != -1) {
			            ; // System.out.write(b);
			        }
			        Assert.assertTrue((p.waitFor() == 0) == success);
			        if (success) {
			            Assert.assertEquals(junit.textui.TestRunner.SUCCESS_EXIT,
			                    p.exitValue());
			        } else {
			            Assert.assertEquals(junit.textui.TestRunner.FAILURE_EXIT,
			                    p.exitValue());
			        }
			    }
			
			    public void testError() throws Exception {
			        this.execTest("junit.tests.BogusDude", false); //$NON-NLS-1$
			    }
			
			    public void testFailure() throws Exception {
			        this.execTest("junit.tests.framework.Failure", false); //$NON-NLS-1$
			    }
			
			    public void testRunReturnsResult() {
			        final PrintStream oldOut = System.out;
			        System.setOut(new PrintStream(new OutputStream() {
			            @Override
			            public void write(final int arg0) throws IOException {
			            }
			        }));
			        try {
			            final TestResult result = junit.textui.TestRunner
			                    .run(new TestSuite());
			            Assert.assertTrue(result.wasSuccessful());
			        } finally {
			            System.setOut(oldOut);
			        }
			    }
			
			    public void testSuccess() throws Exception {
			        this.execTest("junit.tests.framework.Success", true); //$NON-NLS-1$
			    }
			
			}""";
        fExpectedChangesAllTests.put("junit.tests.runner.TextRunnerTest.java", str53);
        String str54= """
			package junit.tests.framework;
			
			import junit.framework.Assert;
			import junit.framework.TestCase;
			
			/**
			 * A test case testing the testing framework.
			 *
			 */
			public class Failure extends TestCase {
			    @Override
			    public void runTest() {
			        Assert.fail();
			    }
			}""";
        fExpectedChangesAllTests.put("junit.tests.framework.Failure.java", str54);
        String str55= """
			package junit.tests.framework;
			
			/**
			 * Test class used in SuiteTest
			 */
			public class OverrideTestCase extends OneTestCase {
			    @Override
			    public void testCase() {
			    }
			}""";
        fExpectedChangesAllTests.put("junit.tests.framework.OverrideTestCase.java", str55);
        String str56= """
			package junit.extensions;
			
			import junit.framework.Test;
			import junit.framework.TestResult;
			
			/**
			 * A Decorator that runs a test repeatedly.
			 *
			 */
			public class RepeatedTest extends TestDecorator {
			    private final int fTimesRepeat;
			
			    public RepeatedTest(final Test test, final int repeat) {
			        super(test);
			        if (repeat < 0) {
			            throw new IllegalArgumentException("Repetition count must be > 0"); //$NON-NLS-1$
			        }
			        this.fTimesRepeat = repeat;
			    }
			    @Override
			    public int countTestCases() {
			        return super.countTestCases() * this.fTimesRepeat;
			    }
			    @Override
			    public void run(final TestResult result) {
			        for (int i = 0; i < this.fTimesRepeat; i++) {
			            if (result.shouldStop()) {
			                break;
			            }
			            super.run(result);
			        }
			    }
			    @Override
			    public String toString() {
			        return super.toString() + "(repeated)"; //$NON-NLS-1$
			    }
			}""";
        fExpectedChangesAllTests.put("junit.extensions.RepeatedTest.java", str56);
        String str57= """
			
			package junit.tests.framework;
			
			import junit.framework.TestCase;
			
			public class NoArgTestCaseTest extends TestCase {
			    public void testNothing() { // If this compiles, the no arg ctor is there
			    }
			}
			""";
        fExpectedChangesAllTests.put("junit.tests.framework.NoArgTestCaseTest.java", str57);
        String str58= """
			package junit.runner;
			
			import java.util.Vector;
			
			/**
			 * A custom quick sort with support to customize the swap behaviour. NOTICE: We
			 * can't use the the sorting support from the JDK 1.2 collection classes because
			 * of the JDK 1.1.7 compatibility.
			 */
			public class Sorter {
			    public interface Swapper {
			        void swap(Vector values, int left, int right);
			    }
			
			    public static void sortStrings(final Vector values, int left, int right,
			            final Swapper swapper) {
			        final int oleft = left;
			        final int oright = right;
			        final String mid = (String) values.elementAt((left + right) / 2);
			        do {
			            while (((String) (values.elementAt(left))).compareTo(mid) < 0) {
			                left++;
			            }
			            while (mid.compareTo((String) (values.elementAt(right))) < 0) {
			                right--;
			            }
			            if (left <= right) {
			                swapper.swap(values, left, right);
			                left++;
			                right--;
			            }
			        } while (left <= right);
			
			        if (oleft < right) {
			            Sorter.sortStrings(values, oleft, right, swapper);
			        }
			        if (left < oright) {
			            Sorter.sortStrings(values, left, oright, swapper);
			        }
			    }
			}""";
        fExpectedChangesAllTests.put("junit.runner.Sorter.java", str58);
        String str59= """
			package junit.tests.framework;
			
			import junit.framework.Test;
			import junit.framework.TestSuite;
			
			/**
			 * TestSuite that runs all the sample tests
			 *
			 */
			public class AllTests {
			
			    public static void main(final String[] args) {
			        junit.textui.TestRunner.run(AllTests.suite());
			    }
			
			    public static Test suite() {
			        final TestSuite suite = new TestSuite("Framework Tests"); //$NON-NLS-1$
			        suite.addTestSuite(TestCaseTest.class);
			        suite.addTest(SuiteTest.suite()); // Tests suite building, so can't use
			                                          // automatic test extraction
			        suite.addTestSuite(TestListenerTest.class);
			        suite.addTestSuite(AssertTest.class);
			        suite.addTestSuite(TestImplementorTest.class);
			        suite.addTestSuite(NoArgTestCaseTest.class);
			        suite.addTestSuite(ComparisonFailureTest.class);
			        suite.addTestSuite(DoublePrecisionAssertTest.class);
			        return suite;
			    }
			
			}""";
        fExpectedChangesAllTests.put("junit.tests.framework.AllTests.java", str59);
        String str60= """
			package junit.tests.extensions;
			
			import junit.extensions.RepeatedTest;
			import junit.framework.Assert;
			import junit.framework.Test;
			import junit.framework.TestCase;
			import junit.framework.TestResult;
			import junit.framework.TestSuite;
			
			/**
			 * Testing the RepeatedTest support.
			 */
			
			public class RepeatedTestTest extends TestCase {
			    public static class SuccessTest extends TestCase {
			
			        @Override
			        public void runTest() {
			        }
			    }
			
			    private final TestSuite fSuite;
			
			    public RepeatedTestTest(final String name) {
			        super(name);
			        this.fSuite = new TestSuite();
			        this.fSuite.addTest(new SuccessTest());
			        this.fSuite.addTest(new SuccessTest());
			    }
			
			    public void testRepeatedMoreThanOnce() {
			        final Test test = new RepeatedTest(this.fSuite, 3);
			        Assert.assertEquals(6, test.countTestCases());
			        final TestResult result = new TestResult();
			        test.run(result);
			        Assert.assertEquals(6, result.runCount());
			    }
			
			    public void testRepeatedNegative() {
			        try {
			            new RepeatedTest(this.fSuite, -1);
			        } catch (final IllegalArgumentException e) {
			            return;
			        }
			        Assert.fail("Should throw an IllegalArgumentException"); //$NON-NLS-1$
			    }
			
			    public void testRepeatedOnce() {
			        final Test test = new RepeatedTest(this.fSuite, 1);
			        Assert.assertEquals(2, test.countTestCases());
			        final TestResult result = new TestResult();
			        test.run(result);
			        Assert.assertEquals(2, result.runCount());
			    }
			
			    public void testRepeatedZero() {
			        final Test test = new RepeatedTest(this.fSuite, 0);
			        Assert.assertEquals(0, test.countTestCases());
			        final TestResult result = new TestResult();
			        test.run(result);
			        Assert.assertEquals(0, result.runCount());
			    }
			}""";
        fExpectedChangesAllTests.put("junit.tests.extensions.RepeatedTestTest.java", str60);
        String str61= """
			package junit.tests.runner;
			
			import junit.framework.Test;
			import junit.framework.TestSuite;
			import junit.runner.BaseTestRunner;
			
			/**
			 * TestSuite that runs all the sample tests
			 *
			 */
			public class AllTests {
			
			    static boolean isJDK11() {
			        final String version = System.getProperty("java.version"); //$NON-NLS-1$
			        return version.startsWith("1.1"); //$NON-NLS-1$
			    }
			
			    public static void main(final String[] args) {
			        junit.textui.TestRunner.run(AllTests.suite());
			    }
			
			    public static Test suite() { // Collect tests manually because we have to
			                                 // test class collection code
			        final TestSuite suite = new TestSuite("Framework Tests"); //$NON-NLS-1$
			        suite.addTestSuite(StackFilterTest.class);
			        suite.addTestSuite(SorterTest.class);
			        suite.addTestSuite(SimpleTestCollectorTest.class);
			        suite.addTestSuite(BaseTestRunnerTest.class);
			        suite.addTestSuite(TextFeedbackTest.class);
			        if (!BaseTestRunner.inVAJava()) {
			            suite.addTestSuite(TextRunnerTest.class);
			            if (!AllTests.isJDK11()) {
			                suite.addTest(new TestSuite(TestCaseClassLoaderTest.class));
			            }
			        }
			        return suite;
			    }
			}""";
        fExpectedChangesAllTests.put("junit.tests.runner.AllTests.java", str61);
        String str62= """
			package junit.runner;
			
			import java.util.Enumeration;
			
			/**
			 * Collects Test class names to be presented by the TestSelector.
			 *\s
			 * @see TestSelector
			 */
			public interface TestCollector {
			    /**
			     * Returns an enumeration of Strings with qualified class names
			     */
			    Enumeration collectTests();
			}
			""";
        fExpectedChangesAllTests.put("junit.runner.TestCollector.java", str62);
        String str63= """
			package junit.samples.money;
			
			import java.util.Vector;
			
			/**
			 * A MoneyBag defers exchange rate conversions. For example adding 12 Swiss
			 * Francs to 14 US Dollars is represented as a bag containing the two Monies 12
			 * CHF and 14 USD. Adding another 10 Swiss francs gives a bag with 22 CHF and 14
			 * USD. Due to the deferred exchange rate conversion we can later value a
			 * MoneyBag with different exchange rates.
			 *
			 * A MoneyBag is represented as a list of Monies and provides different
			 * constructors to create a MoneyBag.
			 */
			class MoneyBag implements IMoney {
			    static IMoney create(final IMoney m1, final IMoney m2) {
			        final MoneyBag result = new MoneyBag();
			        m1.appendTo(result);
			        m2.appendTo(result);
			        return result.simplify();
			    }
			
			    private final Vector fMonies = new Vector(5);
			    public IMoney add(final IMoney m) {
			        return m.addMoneyBag(this);
			    }
			    public IMoney addMoney(final Money m) {
			        return MoneyBag.create(m, this);
			    }
			    public IMoney addMoneyBag(final MoneyBag s) {
			        return MoneyBag.create(s, this);
			    }
			    void appendBag(final MoneyBag aBag) {
			        for (final Object element : aBag.fMonies) {
			            this.appendMoney((Money) element);
			        }
			    }
			    void appendMoney(final Money aMoney) {
			        if (aMoney.isZero()) {
			            return;
			        }
			        final IMoney old = this.findMoney(aMoney.currency());
			        if (old == null) {
			            this.fMonies.addElement(aMoney);
			            return;
			        }
			        this.fMonies.removeElement(old);
			        final IMoney sum = old.add(aMoney);
			        if (sum.isZero()) {
			            return;
			        }
			        this.fMonies.addElement(sum);
			    }
			    public void appendTo(final MoneyBag m) {
			        m.appendBag(this);
			    }
			    private boolean contains(final Money m) {
			        final Money found = this.findMoney(m.currency());
			        if (found == null) {
			            return false;
			        }
			        return found.amount() == m.amount();
			    }
			    @Override
			    public boolean equals(final Object anObject) {
			        if (this.isZero()) {
			            if (anObject instanceof IMoney) {
			                return ((IMoney) anObject).isZero();
			            }
			        }
			
			        if (anObject instanceof MoneyBag) {
			            final MoneyBag aMoneyBag = (MoneyBag) anObject;
			            if (aMoneyBag.fMonies.size() != this.fMonies.size()) {
			                return false;
			            }
			
			            for (final Object element : this.fMonies) {
			                final Money m = (Money) element;
			                if (!aMoneyBag.contains(m)) {
			                    return false;
			                }
			            }
			            return true;
			        }
			        return false;
			    }
			    private Money findMoney(final String currency) {
			        for (final Object element : this.fMonies) {
			            final Money m = (Money) element;
			            if (m.currency().equals(currency)) {
			                return m;
			            }
			        }
			        return null;
			    }
			    @Override
			    public int hashCode() {
			        int hash = 0;
			        for (Object m : this.fMonies) {
			            hash ^= m.hashCode();
			        }
			        return hash;
			    }
			    public boolean isZero() {
			        return this.fMonies.size() == 0;
			    }
			    public IMoney multiply(final int factor) {
			        final MoneyBag result = new MoneyBag();
			        if (factor != 0) {
			            for (final Object element : this.fMonies) {
			                final Money m = (Money) element;
			                result.appendMoney((Money) m.multiply(factor));
			            }
			        }
			        return result;
			    }
			    public IMoney negate() {
			        final MoneyBag result = new MoneyBag();
			        for (final Object element : this.fMonies) {
			            final Money m = (Money) element;
			            result.appendMoney((Money) m.negate());
			        }
			        return result;
			    }
			    private IMoney simplify() {
			        if (this.fMonies.size() == 1) {
			            return (IMoney) this.fMonies.elements().nextElement();
			        }
			        return this;
			    }
			    public IMoney subtract(final IMoney m) {
			        return this.add(m.negate());
			    }
			    @Override
			    public String toString() {
			        final StringBuffer buffer = new StringBuffer();
			        buffer.append("{"); //$NON-NLS-1$
			        for (final Object element : this.fMonies) {
			            buffer.append(element);
			        }
			        buffer.append("}"); //$NON-NLS-1$
			        return buffer.toString();
			    }
			}""";
        fExpectedChangesAllTests.put("junit.samples.money.MoneyBag.java", str63);
        String str64= """
			package junit.tests.runner;
			
			import junit.framework.Assert;
			import junit.framework.TestCase;
			import junit.runner.SimpleTestCollector;
			
			public class SimpleTestCollectorTest extends TestCase {
			
			    public void testMissingDirectory() {
			        final SimpleTestCollector collector = new SimpleTestCollector();
			        Assert.assertFalse(collector.collectFilesInPath("foobar").elements() //$NON-NLS-1$
			                .hasMoreElements());
			    }
			
			}
			""";
        fExpectedChangesAllTests.put("junit.tests.runner.SimpleTestCollectorTest.java", str64);
        String str65= """
			package junit.framework;
			
			import java.lang.reflect.InvocationTargetException;
			import java.lang.reflect.Method;
			import java.lang.reflect.Modifier;
			
			/**
			 * A test case defines the fixture to run multiple tests. To define a test
			 * case<br>
			 * 1) implement a subclass of TestCase<br>
			 * 2) define instance variables that store the state of the fixture<br>
			 * 3) initialize the fixture state by overriding <code>setUp</code><br>
			 * 4) clean-up after a test by overriding <code>tearDown</code>.<br>
			 * Each test runs in its own fixture so there can be no side effects among test
			 * runs. Here is an example:
			 *\s
			 * <pre>
			 * public class MathTest extends TestCase {
			 *     protected double fValue1;
			 *     protected double fValue2;
			 *
			 *     protected void setUp() {
			 *         fValue1 = 2.0;
			 *         fValue2 = 3.0;
			 *     }
			 * }
			 * </pre>
			 *
			 * For each test implement a method which interacts with the fixture. Verify the
			 * expected results with assertions specified by calling <code>assertTrue</code>
			 * with a boolean.
			 *\s
			 * <pre>
			 * public void testAdd() {
			 *     double result = fValue1 + fValue2;
			 *     assertTrue(result == 5.0);
			 * }
			 * </pre>
			 *\s
			 * Once the methods are defined you can run them. The framework supports both a
			 * static type safe and more dynamic way to run a test. In the static way you
			 * override the runTest method and define the method to be invoked. A convenient
			 * way to do so is with an anonymous inner class.
			 *\s
			 * <pre>
			 * TestCase test = new MathTest("add") {
			 *     public void runTest() {
			 *         testAdd();
			 *     }
			 * };
			 * test.run();
			 * </pre>
			 *\s
			 * The dynamic way uses reflection to implement <code>runTest</code>. It
			 * dynamically finds and invokes a method. In this case the name of the test
			 * case has to correspond to the test method to be run.
			 *\s
			 * <pre>
			 * TestCase = new MathTest("testAdd");
			 * test.run();
			 * </pre>
			 *\s
			 * The tests to be run can be collected into a TestSuite. JUnit provides
			 * different <i>test runners</i> which can run a test suite and collect the
			 * results. A test runner either expects a static method <code>suite</code> as
			 * the entry point to get a test to run or it will extract the suite
			 * automatically.
			 *\s
			 * <pre>
			 * public static Test suite() {
			 *     suite.addTest(new MathTest("testAdd"));
			 *     suite.addTest(new MathTest("testDivideByZero"));
			 *     return suite;
			 * }
			 * </pre>
			 *\s
			 * @see TestResult
			 * @see TestSuite
			 */
			
			public abstract class TestCase extends Assert implements Test {
			    /**
			     * the name of the test case
			     */
			    private String fName;
			
			    /**
			     * No-arg constructor to enable serialization. This method is not intended
			     * to be used by mere mortals without calling setName().
			     */
			    public TestCase() {
			        this.fName = null;
			    }
			    /**
			     * Constructs a test case with the given name.
			     */
			    public TestCase(final String name) {
			        this.fName = name;
			    }
			    /**
			     * Counts the number of test cases executed by run(TestResult result).
			     */
			    public int countTestCases() {
			        return 1;
			    }
			    /**
			     * Creates a default TestResult object
			     *
			     * @see TestResult
			     */
			    protected TestResult createResult() {
			        return new TestResult();
			    }
			    /**
			     * Gets the name of a TestCase
			     *\s
			     * @return returns a String
			     */
			    public String getName() {
			        return this.fName;
			    }
			    /**
			     * A convenience method to run this test, collecting the results with a
			     * default TestResult object.
			     *
			     * @see TestResult
			     */
			    public TestResult run() {
			        final TestResult result = this.createResult();
			        this.run(result);
			        return result;
			    }
			    /**
			     * Runs the test case and collects the results in TestResult.
			     */
			    public void run(final TestResult result) {
			        result.run(this);
			    }
			    /**
			     * Runs the bare test sequence.
			     *\s
			     * @exception Throwable if any exception is thrown
			     */
			    public void runBare() throws Throwable {
			        this.setUp();
			        try {
			            this.runTest();
			        } finally {
			            this.tearDown();
			        }
			    }
			    /**
			     * Override to run the test and assert its state.
			     *\s
			     * @exception Throwable if any exception is thrown
			     */
			    protected void runTest() throws Throwable {
			        Assert.assertNotNull(this.fName);
			        Method runMethod = null;
			        try {
			            // use getMethod to get all public inherited
			            // methods. getDeclaredMethods returns all
			            // methods of this class but excludes the
			            // inherited ones.
			            runMethod = this.getClass().getMethod(this.fName, null);
			        } catch (final NoSuchMethodException e) {
			            Assert.fail("Method \\"" + this.fName + "\\" not found"); //$NON-NLS-1$ //$NON-NLS-2$
			        }
			        if (!Modifier.isPublic(runMethod.getModifiers())) {
			            Assert.fail("Method \\"" + this.fName + "\\" should be public"); //$NON-NLS-1$ //$NON-NLS-2$
			        }
			
			        try {
			            runMethod.invoke(this, new Class[0]);
			        } catch (final InvocationTargetException e) {
			            e.fillInStackTrace();
			            throw e.getTargetException();
			        } catch (final IllegalAccessException e) {
			            e.fillInStackTrace();
			            throw e;
			        }
			    }
			    /**
			     * Sets the name of a TestCase
			     *\s
			     * @param name The name to set
			     */
			    public void setName(final String name) {
			        this.fName = name;
			    }
			    /**
			     * Sets up the fixture, for example, open a network connection. This method
			     * is called before a test is executed.
			     */
			    protected void setUp() throws Exception {
			    }
			    /**
			     * Tears down the fixture, for example, close a network connection. This
			     * method is called after a test is executed.
			     */
			    protected void tearDown() throws Exception {
			    }
			    /**
			     * Returns a string representation of the test case
			     */
			    @Override
			    public String toString() {
			        return this.getName() + "(" + this.getClass().getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			    }
			}""";
        fExpectedChangesAllTests.put("junit.framework.TestCase.java", str65);
        String str66= """
			package junit.samples.money;
			
			import junit.framework.Assert;
			import junit.framework.TestCase;
			
			public class MoneyTest extends TestCase {
			    public static void main(final String args[]) {
			        junit.textui.TestRunner.run(MoneyTest.class);
			    }
			    private Money f12CHF;
			    private Money f14CHF;
			    private Money f21USD;
			
			    private Money f7USD;
			    private IMoney fMB1;
			
			    private IMoney fMB2;
			    @Override
			    protected void setUp() {
			        this.f12CHF = new Money(12, "CHF"); //$NON-NLS-1$
			        this.f14CHF = new Money(14, "CHF"); //$NON-NLS-1$
			        this.f7USD = new Money(7, "USD"); //$NON-NLS-1$
			        this.f21USD = new Money(21, "USD"); //$NON-NLS-1$
			
			        this.fMB1 = MoneyBag.create(this.f12CHF, this.f7USD);
			        this.fMB2 = MoneyBag.create(this.f14CHF, this.f21USD);
			    }
			    public void testBagMultiply() {
			        // {[12 CHF][7 USD]} *2 == {[24 CHF][14 USD]}
			        final IMoney expected = MoneyBag.create(new Money(24, "CHF"), //$NON-NLS-1$
			                new Money(14, "USD")); //$NON-NLS-1$
			        Assert.assertEquals(expected, this.fMB1.multiply(2));
			        Assert.assertEquals(this.fMB1, this.fMB1.multiply(1));
			        Assert.assertTrue(this.fMB1.multiply(0).isZero());
			    }
			    public void testBagNegate() {
			        // {[12 CHF][7 USD]} negate == {[-12 CHF][-7 USD]}
			        final IMoney expected = MoneyBag.create(new Money(-12, "CHF"), //$NON-NLS-1$
			                new Money(-7, "USD")); //$NON-NLS-1$
			        Assert.assertEquals(expected, this.fMB1.negate());
			    }
			    public void testBagNotEquals() {
			        final IMoney bag = MoneyBag.create(this.f12CHF, this.f7USD);
			        Assert.assertFalse(bag.equals(new Money(12, "DEM").add(this.f7USD))); //$NON-NLS-1$
			    }
			    public void testBagSimpleAdd() {
			        // {[12 CHF][7 USD]} + [14 CHF] == {[26 CHF][7 USD]}
			        final IMoney expected = MoneyBag.create(new Money(26, "CHF"), //$NON-NLS-1$
			                new Money(7, "USD")); //$NON-NLS-1$
			        Assert.assertEquals(expected, this.fMB1.add(this.f14CHF));
			    }
			    public void testBagSubtract() {
			        // {[12 CHF][7 USD]} - {[14 CHF][21 USD] == {[-2 CHF][-14 USD]}
			        final IMoney expected = MoneyBag.create(new Money(-2, "CHF"), //$NON-NLS-1$
			                new Money(-14, "USD")); //$NON-NLS-1$
			        Assert.assertEquals(expected, this.fMB1.subtract(this.fMB2));
			    }
			    public void testBagSumAdd() {
			        // {[12 CHF][7 USD]} + {[14 CHF][21 USD]} == {[26 CHF][28 USD]}
			        final IMoney expected = MoneyBag.create(new Money(26, "CHF"), //$NON-NLS-1$
			                new Money(28, "USD")); //$NON-NLS-1$
			        Assert.assertEquals(expected, this.fMB1.add(this.fMB2));
			    }
			    public void testIsZero() {
			        Assert.assertTrue(this.fMB1.subtract(this.fMB1).isZero());
			        Assert.assertTrue(MoneyBag
			                .create(new Money(0, "CHF"), new Money(0, "USD")).isZero()); //$NON-NLS-1$ //$NON-NLS-2$
			    }
			    public void testMixedSimpleAdd() {
			        // [12 CHF] + [7 USD] == {[12 CHF][7 USD]}
			        final IMoney expected = MoneyBag.create(this.f12CHF, this.f7USD);
			        Assert.assertEquals(expected, this.f12CHF.add(this.f7USD));
			    }
			    public void testMoneyBagEquals() {
			        Assert.assertTrue(!this.fMB1.equals(null));
			
			        Assert.assertEquals(this.fMB1, this.fMB1);
			        final IMoney equal = MoneyBag.create(new Money(12, "CHF"), //$NON-NLS-1$
			                new Money(7, "USD")); //$NON-NLS-1$
			        Assert.assertTrue(this.fMB1.equals(equal));
			        Assert.assertTrue(!this.fMB1.equals(this.f12CHF));
			        Assert.assertTrue(!this.f12CHF.equals(this.fMB1));
			        Assert.assertTrue(!this.fMB1.equals(this.fMB2));
			    }
			    public void testMoneyBagHash() {
			        final IMoney equal = MoneyBag.create(new Money(12, "CHF"), //$NON-NLS-1$
			                new Money(7, "USD")); //$NON-NLS-1$
			        Assert.assertEquals(this.fMB1.hashCode(), equal.hashCode());
			    }
			    public void testMoneyEquals() {
			        Assert.assertTrue(!this.f12CHF.equals(null));
			        final Money equalMoney = new Money(12, "CHF"); //$NON-NLS-1$
			        Assert.assertEquals(this.f12CHF, this.f12CHF);
			        Assert.assertEquals(this.f12CHF, equalMoney);
			        Assert.assertEquals(this.f12CHF.hashCode(), equalMoney.hashCode());
			        Assert.assertTrue(!this.f12CHF.equals(this.f14CHF));
			    }
			    public void testMoneyHash() {
			        Assert.assertTrue(!this.f12CHF.equals(null));
			        final Money equal = new Money(12, "CHF"); //$NON-NLS-1$
			        Assert.assertEquals(this.f12CHF.hashCode(), equal.hashCode());
			    }
			    public void testNormalize2() {
			        // {[12 CHF][7 USD]} - [12 CHF] == [7 USD]
			        final Money expected = new Money(7, "USD"); //$NON-NLS-1$
			        Assert.assertEquals(expected, this.fMB1.subtract(this.f12CHF));
			    }
			    public void testNormalize3() {
			        // {[12 CHF][7 USD]} - {[12 CHF][3 USD]} == [4 USD]
			        final IMoney ms1 = MoneyBag.create(new Money(12, "CHF"), //$NON-NLS-1$
			                new Money(3, "USD")); //$NON-NLS-1$
			        final Money expected = new Money(4, "USD"); //$NON-NLS-1$
			        Assert.assertEquals(expected, this.fMB1.subtract(ms1));
			    }
			    public void testNormalize4() {
			        // [12 CHF] - {[12 CHF][3 USD]} == [-3 USD]
			        final IMoney ms1 = MoneyBag.create(new Money(12, "CHF"), //$NON-NLS-1$
			                new Money(3, "USD")); //$NON-NLS-1$
			        final Money expected = new Money(-3, "USD"); //$NON-NLS-1$
			        Assert.assertEquals(expected, this.f12CHF.subtract(ms1));
			    }
			    public void testPrint() {
			        Assert.assertEquals("[12 CHF]", this.f12CHF.toString()); //$NON-NLS-1$
			    }
			    public void testSimpleAdd() {
			        // [12 CHF] + [14 CHF] == [26 CHF]
			        final Money expected = new Money(26, "CHF"); //$NON-NLS-1$
			        Assert.assertEquals(expected, this.f12CHF.add(this.f14CHF));
			    }
			    public void testSimpleBagAdd() {
			        // [14 CHF] + {[12 CHF][7 USD]} == {[26 CHF][7 USD]}
			        final IMoney expected = MoneyBag.create(new Money(26, "CHF"), //$NON-NLS-1$
			                new Money(7, "USD")); //$NON-NLS-1$
			        Assert.assertEquals(expected, this.f14CHF.add(this.fMB1));
			    }
			    public void testSimpleMultiply() {
			        // [14 CHF] *2 == [28 CHF]
			        final Money expected = new Money(28, "CHF"); //$NON-NLS-1$
			        Assert.assertEquals(expected, this.f14CHF.multiply(2));
			    }
			    public void testSimpleNegate() {
			        // [14 CHF] negate == [-14 CHF]
			        final Money expected = new Money(-14, "CHF"); //$NON-NLS-1$
			        Assert.assertEquals(expected, this.f14CHF.negate());
			    }
			    public void testSimpleSubtract() {
			        // [14 CHF] - [12 CHF] == [2 CHF]
			        final Money expected = new Money(2, "CHF"); //$NON-NLS-1$
			        Assert.assertEquals(expected, this.f14CHF.subtract(this.f12CHF));
			    }
			    public void testSimplify() {
			        final IMoney money = MoneyBag.create(new Money(26, "CHF"), //$NON-NLS-1$
			                new Money(28, "CHF")); //$NON-NLS-1$
			        Assert.assertEquals(new Money(54, "CHF"), money); //$NON-NLS-1$
			    }
			}""";
        fExpectedChangesAllTests.put("junit.samples.money.MoneyTest.java", str66);
        String str67= """
			package junit.tests.extensions;
			
			import junit.framework.Test;
			import junit.framework.TestSuite;
			
			/**
			 * TestSuite that runs all the extension tests
			 *
			 */
			public class AllTests {
			
			    public static void main(final String[] args) {
			        junit.textui.TestRunner.run(AllTests.suite());
			    }
			
			    public static Test suite() { // Collect tests manually because we have to
			                                 // test class collection code
			        final TestSuite suite = new TestSuite("Framework Tests"); //$NON-NLS-1$
			        suite.addTestSuite(ExtensionTest.class);
			        suite.addTestSuite(ExceptionTestCaseTest.class);
			        suite.addTestSuite(ActiveTestTest.class);
			        suite.addTestSuite(RepeatedTestTest.class);
			        return suite;
			    }
			}""";
        fExpectedChangesAllTests.put("junit.tests.extensions.AllTests.java", str67);
        String str68= """
			package junit.tests.framework;
			
			/**
			 * Test class used in SuiteTest
			 */
			import junit.framework.TestCase;
			
			public class NoTestCases extends TestCase {
			    public void noTestCase() {
			    }
			}""";
        fExpectedChangesAllTests.put("junit.tests.framework.NoTestCases.java", str68);
        String str69= """
			package junit.tests.runner;
			
			import java.lang.reflect.Method;
			import java.net.URL;
			
			import junit.framework.Assert;
			import junit.framework.TestCase;
			import junit.runner.TestCaseClassLoader;
			
			/**
			 * A TestCase for testing the TestCaseClassLoader
			 *
			 */
			public class TestCaseClassLoaderTest extends TestCase {
			
			    public void testClassLoading() throws Exception {
			        final TestCaseClassLoader loader = new TestCaseClassLoader();
			        final Class loadedClass = loader
			                .loadClass("junit.tests.runner.ClassLoaderTest", true); //$NON-NLS-1$
			        final Object o = loadedClass.newInstance();
			        //
			        // Invoke the assertClassLoaders method via reflection.
			        // We use reflection since the class is loaded by
			        // another class loader and we can't do a successfull downcast to
			        // ClassLoaderTestCase.
			        //
			        final Method method = loadedClass.getDeclaredMethod("verify", //$NON-NLS-1$
			                new Class[0]);
			        method.invoke(o, new Class[0]);
			    }
			
			    public void testJarClassLoading() throws Exception {
			        final URL url = this.getClass().getResource("test.jar"); //$NON-NLS-1$
			        Assert.assertNotNull("Cannot find test.jar", url); //$NON-NLS-1$
			        final String path = url.getFile();
			        final TestCaseClassLoader loader = new TestCaseClassLoader(path);
			        final Class loadedClass = loader
			                .loadClass("junit.tests.runner.LoadedFromJar", true); //$NON-NLS-1$
			        final Object o = loadedClass.newInstance();
			        //
			        // Invoke the assertClassLoaders method via reflection.
			        // We use reflection since the class is loaded by
			        // another class loader and we can't do a successfull downcast to
			        // ClassLoaderTestCase.
			        //
			        final Method method = loadedClass.getDeclaredMethod("verify", //$NON-NLS-1$
			                new Class[0]);
			        method.invoke(o, new Class[0]);
			    }
			}""";
        fExpectedChangesAllTests.put("junit.tests.runner.TestCaseClassLoaderTest.java", str69);
        String str70= """
			package junit.samples.money;
			
			/**
			 * A simple Money.
			 *
			 */
			public class Money implements IMoney {
			
			    private final int fAmount;
			    private final String fCurrency;
			
			    /**
			     * Constructs a money from the given amount and currency.
			     */
			    public Money(final int amount, final String currency) {
			        this.fAmount = amount;
			        this.fCurrency = currency;
			    }
			    /**
			     * Adds a money to this money. Forwards the request to the addMoney helper.
			     */
			    public IMoney add(final IMoney m) {
			        return m.addMoney(this);
			    }
			    public IMoney addMoney(final Money m) {
			        if (m.currency().equals(this.currency())) {
			            return new Money(this.amount() + m.amount(), this.currency());
			        }
			        return MoneyBag.create(this, m);
			    }
			    public IMoney addMoneyBag(final MoneyBag s) {
			        return s.addMoney(this);
			    }
			    public int amount() {
			        return this.fAmount;
			    }
			    public /* this makes no sense */ void appendTo(final MoneyBag m) {
			        m.appendMoney(this);
			    }
			    public String currency() {
			        return this.fCurrency;
			    }
			    @Override
			    public boolean equals(final Object anObject) {
			        if (this.isZero()) {
			            if (anObject instanceof IMoney) {
			                return ((IMoney) anObject).isZero();
			            }
			        }
			        if (anObject instanceof Money) {
			            final Money aMoney = (Money) anObject;
			            return aMoney.currency().equals(this.currency())
			                    && (this.amount() == aMoney.amount());
			        }
			        return false;
			    }
			    @Override
			    public int hashCode() {
			        return this.fCurrency.hashCode() + this.fAmount;
			    }
			    public boolean isZero() {
			        return this.amount() == 0;
			    }
			    public IMoney multiply(final int factor) {
			        return new Money(this.amount() * factor, this.currency());
			    }
			    public IMoney negate() {
			        return new Money(-this.amount(), this.currency());
			    }
			    public IMoney subtract(final IMoney m) {
			        return this.add(m.negate());
			    }
			    @Override
			    public String toString() {
			        final StringBuffer buffer = new StringBuffer();
			        buffer.append("[" + this.amount() + " " + this.currency() + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			        return buffer.toString();
			    }
			}""";
        fExpectedChangesAllTests.put("junit.samples.money.Money.java", str70);
    }

	@Test
	public void testAllCleanUps() throws Exception {
		List<IJavaElement> cus= new ArrayList<>();
		addAllCUs(getProject().getChildren(), cus);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS);

		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION);

		enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID);
		enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_DEFAULT);

		enable(CleanUpConstants.ADD_MISSING_METHODES);

		enable(CleanUpConstants.ADD_MISSING_NLS_TAGS);
		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_METHODS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);

		enable(CleanUpConstants.ORGANIZE_IMPORTS);

		enable(CleanUpConstants.SORT_MEMBERS);
		enable(CleanUpConstants.SORT_MEMBERS_ALL);

		enable(CleanUpConstants.REMOVE_REDUNDANT_MODIFIERS);
		enable(CleanUpConstants.REMOVE_REDUNDANT_SEMICOLONS);

		ICompilationUnit[] units= cus.toArray(new ICompilationUnit[cus.size()]);
		performRefactoring(units, null);


//		generateTable(units);
		for (ICompilationUnit cu : units) {
			String previewContent= getNormalizedContent(new Document(cu.getBuffer().getContents()));
			String compilationUnitName= getCompilationUnitName(cu);

			String expected= fExpectedChangesAllTests.get(compilationUnitName);

			assertNotNull("No expected value in table for " + compilationUnitName, expected);
			assertEquals("Content not as expected for " + compilationUnitName, expected, previewContent);
		}
	}

	private static String getCompilationUnitName(ICompilationUnit cu) {
		StringBuffer result= new StringBuffer();
		JavaElementLabels.getCompilationUnitLabel(cu, JavaElementLabels.CU_QUALIFIED, result);
		return result.toString();
	}

	private static String getNormalizedContent(IDocument document) {
		StringBuilder buf= new StringBuilder();
		try {
			int selectionOffset= 0;
			int selectionLength= document.getLength();
			int startLine= document.getLineOfOffset(selectionOffset);
			int endLine= document.getLineOfOffset(selectionOffset + selectionLength);

			for (int i= startLine; i <= endLine; i++) {
				IRegion lineInfo= document.getLineInformation(i);
				String lineContent= document.get(lineInfo.getOffset(), lineInfo.getLength());

				for (int k= 0; k < lineContent.length(); k++) {
                	char ch= lineContent.charAt(k);
                	if (ch == '\t') {
                		buf.append("    "); // 4 spaces
                	} else {
                		buf.append(ch);
                	}
                }

				if (i != endLine) {
					buf.append('\n');
				}
			}
		} catch (BadLocationException e) {
			// ignore
		}
		return buf.toString();
	}

//	Do not remove, used to generate the table
//	private static final String[] CU_ORDER= new String[71];
//	static {
//		CU_ORDER[0]= "junit.runner.BaseTestRunner.java";
//		CU_ORDER[1]= "junit.tests.framework.NotVoidTestCase.java";
//		CU_ORDER[2]= "junit.tests.runner.StackFilterTest.java";
//		CU_ORDER[3]= "junit.tests.framework.DoublePrecisionAssertTest.java";
//		CU_ORDER[4]= "junit.tests.framework.AssertTest.java";
//		CU_ORDER[5]= "junit.samples.AllTests.java";
//		CU_ORDER[6]= "junit.tests.extensions.ExceptionTestCaseTest.java";
//		CU_ORDER[7]= "junit.tests.framework.TestListenerTest.java";
//		CU_ORDER[8]= "junit.tests.runner.SorterTest.java";
//		CU_ORDER[9]= "junit.tests.framework.OneTestCase.java";
//		CU_ORDER[10]= "junit.tests.framework.TestImplementorTest.java";
//		CU_ORDER[11]= "junit.extensions.TestDecorator.java";
//		CU_ORDER[12]= "junit.runner.TestSuiteLoader.java";
//		CU_ORDER[13]= "junit.framework.TestResult.java";
//		CU_ORDER[14]= "junit.tests.framework.NotPublicTestCase.java";
//		CU_ORDER[15]= "junit.extensions.ActiveTestSuite.java";
//		CU_ORDER[16]= "junit.tests.framework.SuiteTest.java";
//		CU_ORDER[17]= "junit.runner.SimpleTestCollector.java";
//		CU_ORDER[18]= "junit.framework.Test.java";
//		CU_ORDER[19]= "junit.tests.framework.NoTestCaseClass.java";
//		CU_ORDER[20]= "junit.tests.framework.Success.java";
//		CU_ORDER[21]= "junit.runner.LoadingTestCollector.java";
//		CU_ORDER[22]= "junit.runner.TestCaseClassLoader.java";
//		CU_ORDER[23]= "junit.framework.AssertionFailedError.java";
//		CU_ORDER[24]= "junit.tests.framework.InheritedTestCase.java";
//		CU_ORDER[25]= "junit.samples.SimpleTest.java";
//		CU_ORDER[26]= "junit.runner.Version.java";
//		CU_ORDER[27]= "junit.tests.runner.BaseTestRunnerTest.java";
//		CU_ORDER[28]= "junit.tests.WasRun.java";
//		CU_ORDER[29]= "junit.framework.TestSuite.java";
//		CU_ORDER[30]= "junit.extensions.ExceptionTestCase.java";
//		CU_ORDER[31]= "junit.framework.Assert.java";
//		CU_ORDER[32]= "junit.runner.ClassPathTestCollector.java";
//		CU_ORDER[33]= "junit.framework.TestListener.java";
//		CU_ORDER[34]= "junit.tests.extensions.ActiveTestTest.java";
//		CU_ORDER[35]= "junit.framework.Protectable.java";
//		CU_ORDER[36]= "junit.samples.money.IMoney.java";
//		CU_ORDER[37]= "junit.textui.TestRunner.java";
//		CU_ORDER[38]= "junit.tests.runner.ClassLoaderTest.java";
//		CU_ORDER[39]= "junit.runner.TestRunListener.java";
//		CU_ORDER[40]= "junit.tests.runner.TextFeedbackTest.java";
//		CU_ORDER[41]= "junit.tests.extensions.ExtensionTest.java";
//		CU_ORDER[42]= "junit.tests.AllTests.java";
//		CU_ORDER[43]= "junit.tests.runner.LoadedFromJar.java";
//		CU_ORDER[44]= "junit.tests.framework.ComparisonFailureTest.java";
//		CU_ORDER[45]= "junit.textui.ResultPrinter.java";
//		CU_ORDER[46]= "junit.samples.VectorTest.java";
//		CU_ORDER[47]= "junit.framework.ComparisonFailure.java";
//		CU_ORDER[48]= "junit.tests.framework.TestCaseTest.java";
//		CU_ORDER[49]= "junit.framework.TestFailure.java";
//		CU_ORDER[50]= "junit.runner.ReloadingTestSuiteLoader.java";
//		CU_ORDER[51]= "junit.runner.StandardTestSuiteLoader.java";
//		CU_ORDER[52]= "junit.extensions.TestSetup.java";
//		CU_ORDER[53]= "junit.tests.runner.TextRunnerTest.java";
//		CU_ORDER[54]= "junit.tests.framework.Failure.java";
//		CU_ORDER[55]= "junit.tests.framework.OverrideTestCase.java";
//		CU_ORDER[56]= "junit.extensions.RepeatedTest.java";
//		CU_ORDER[57]= "junit.tests.framework.NoArgTestCaseTest.java";
//		CU_ORDER[58]= "junit.runner.Sorter.java";
//		CU_ORDER[59]= "junit.tests.framework.AllTests.java";
//		CU_ORDER[60]= "junit.tests.extensions.RepeatedTestTest.java";
//		CU_ORDER[61]= "junit.tests.runner.AllTests.java";
//		CU_ORDER[62]= "junit.runner.TestCollector.java";
//		CU_ORDER[63]= "junit.samples.money.MoneyBag.java";
//		CU_ORDER[64]= "junit.tests.runner.SimpleTestCollectorTest.java";
//		CU_ORDER[65]= "junit.framework.TestCase.java";
//		CU_ORDER[66]= "junit.samples.money.MoneyTest.java";
//		CU_ORDER[67]= "junit.tests.extensions.AllTests.java";
//		CU_ORDER[68]= "junit.tests.framework.NoTestCases.java";
//		CU_ORDER[69]= "junit.tests.runner.TestCaseClassLoaderTest.java";
//		CU_ORDER[70]= "junit.samples.money.Money.java";
//	}
//
//	private void generateTable(ICompilationUnit[] units) throws CoreException {
//
//		assertNoCompileErrors(units);
//
//		Hashtable expected= new Hashtable();
//		for (int i= 0; i < units.length; i++) {
//			expected.put(getCompilationUnitName(units[i]), units[i].getBuffer().getContents());
//		}
//
//		StringBuffer buf= new StringBuffer();
//
//		buf.append("    private Hashtable fExpectedChangesAllTests;").append("\n");
//		buf.append("    {").append("\n");
//		buf.append("        fExpectedChangesAllTests= new Hashtable();").append("\n");
//		buf.append("        StringBuffer buf= null;").append("\n");
//
//		for (int i= 0; i < CU_ORDER.length; i++) {
//			String previewContent= (String) expected.get(CU_ORDER[i]);
//			String bufWrappedContext= getBufWrappedContext(new Document(previewContent));
//
//			buf.append("        buf= new StringBuffer();").append("\n");
//			buf.append(bufWrappedContext).append("\n");
//			buf.append("        fExpectedChangesAllTests.put(\"" + CU_ORDER[i] + "\", buf.toString());").append("\n");
//		}
//
//		buf.append("    }").append("\n");
//
//		Clipboard clipboard= new Clipboard(null);
//		clipboard.setContents(new Object[] { buf.toString() }, new Transfer[] { TextTransfer.getInstance() });
//		clipboard.dispose();
//	}
//
//	private static String getBufWrappedContext(IDocument document) {
//		StringBuffer buf= new StringBuffer();
//		try {
//			int selectionOffset= 0;
//			int selectionLength= document.getLength();
//			int startLine= document.getLineOfOffset(selectionOffset);
//			int endLine= document.getLineOfOffset(selectionOffset + selectionLength);
//
//			for (int i= startLine; i <= endLine; i++) {
//				IRegion lineInfo= document.getLineInformation(i);
//				String lineContent= document.get(lineInfo.getOffset(), lineInfo.getLength());
//				buf.append("        buf.append(\"");
//				for (int k= 0; k < lineContent.length(); k++) {
//					char ch= lineContent.charAt(k);
//					if (ch == '\t') {
//						buf.append("    "); // 4 spaces
//					} else if (ch == '"' || ch == '\\') {
//						buf.append('\\').append(ch);
//					} else {
//						buf.append(ch);
//					}
//				}
//
//				if (i != endLine) {
//					buf.append("\\n\");");
//					buf.append('\n');
//				} else {
//					buf.append("\");");
//				}
//			}
//		} catch (BadLocationException e) {
//			// ignore
//		}
//		return buf.toString();
//	}
//
//	private void assertNoCompileErrors(ICompilationUnit[] units) throws JavaModelException, CoreException {
//		ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
//		parser.setResolveBindings(true);
//		parser.setProject(fJProject1);
//
//		parser.createASTs(units, new String[0], new ASTRequestor() {
//			public void acceptAST(ICompilationUnit source, CompilationUnit ast) {
//				IProblem[] problems= ast.getProblems();
//
//				StringBuffer buf= new StringBuffer();
//				for (int i= 0; i < problems.length; i++) {
//					if (problems[i].isError()) {
//						buf.append(problems[i].getMessage()).append('\n');
//					}
//				}
//				if (buf.length() != 0) {
//					buf.insert(0, "Found errors in " + source.getElementName() + ":\n");
//					try {
//						buf.append(source.getBuffer().getContents());
//					} catch (JavaModelException e) {
//						JavaPlugin.log(e);
//					}
//					assertTrue(buf.toString(), false);
//				}
//			}
//		}, new NullProgressMonitor());
//	}

}