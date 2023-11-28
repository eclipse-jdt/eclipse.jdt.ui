/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.nls;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.corext.refactoring.nls.NLSHint;

import org.eclipse.jdt.ui.tests.core.rules.Java15ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

public class NLSHintStripQuotesTest {

	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	@Rule
	public Java15ProjectTestSetup pts15= new Java15ProjectTestSetup();

    private IJavaProject javaProject;
    private IJavaProject javaProject15;


    @Before
	public void setUp() throws Exception {
        javaProject= pts.getProject();
        javaProject15= pts15.getProject();
    }

    @After
	public void tearDown() throws Exception {
        JavaProjectHelper.clear(javaProject, pts.getDefaultClasspath());
        JavaProjectHelper.clear(javaProject15, pts15.getDefaultClasspath());
    }

    @Test
    public void test01() throws Exception {
    	String x= "\"abc\n\"";
    	String y= NLSHint.stripQuotes(x, javaProject.getJavaProject());
    	String expected= "abc\n";
    	assertEquals(expected, y);
    }
    @Test
    public void test02() throws Exception {
    	String x= "\"\"\"abc\n\"\"\"";
    	String y= NLSHint.stripQuotes(x, javaProject.getJavaProject());
    	String expected= "\"\"abc\n\"\"";
    	assertEquals(expected, y);
    }
    @Test
    public void test03() throws Exception {
    	String x="\"\"\"\nabc\ndef\n\"\"\"";
    	String y= NLSHint.stripQuotes(x, javaProject15.getJavaProject());
    	String expected= "abc\ndef\n";
    	assertEquals(expected, y);
    }
    @Test
    public void test04() throws Exception {
    	String x="\"\"\" \nabc\\s\ndef\n\"\"\"";
    	String y= NLSHint.stripQuotes(x, javaProject15.getJavaProject());
    	String expected= "abc \ndef\n";
    	assertEquals(expected, y);
    }
    @Test
    public void test05() throws Exception {
    	String x="\"\"\" \n    abc\\s\n    def\n    \"\"\"";
    	String y= NLSHint.stripQuotes(x, javaProject15.getJavaProject());
    	String expected= "abc \ndef\n";
    	assertEquals(expected, y);
    }
    @Test
    public void test06() throws Exception {
    	String x="\"\"\" \nabc  \\s\ndef\n\"\"\"";
    	String y= NLSHint.stripQuotes(x, javaProject15.getJavaProject());
    	String expected= "abc   \ndef\n";
    	assertEquals(expected, y);
    }
    @Test
    public void test07() throws Exception {
    	String x="\"\"\" \nabc  \\\ndef\n\"\"\"";
    	String y= NLSHint.stripQuotes(x, javaProject15.getJavaProject());
    	String expected= "abc  def\n";
    	assertEquals(expected, y);
    }
    @Test
    public void test08() throws Exception {
    	String x="\"\"\" \n   abc\ndef\n\"\"\"";
    	String y= NLSHint.stripQuotes(x, javaProject15.getJavaProject());
    	String expected= "   abc\ndef\n";
    	assertEquals(expected, y);
    }
}
