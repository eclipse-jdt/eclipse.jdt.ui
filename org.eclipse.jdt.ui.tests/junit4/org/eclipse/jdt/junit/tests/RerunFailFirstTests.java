/*******************************************************************************
 * Copyright (c) 2009 SpringSource and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Andrew Eisenberg - initial API and implementation
 *     Andrew Eisenberg <andrew@eisenberg.as> - [JUnit] Rerun failed first does not work with JUnit4 - https://bugs.eclipse.org/bugs/show_bug.cgi?id=140392
 *******************************************************************************/

package org.eclipse.jdt.junit.tests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Sorter;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

import org.eclipse.jdt.internal.junit4.runner.FailuresFirstSorter;

/**
 * Tests for FailuresFirstSorter in org.eclipse.jdt.junit4.runtime.
 * 
 * @since 3.6
 */
@SuppressWarnings("nls")
public class RerunFailFirstTests {

	static class Data {
		public static class T1 {
			public T1() { }
			@Test
			public void m1() { }
			@Test
			public void m2() { }
			@Test
			public void m3() { }
		}
		public static class T2 {
			public T2() { }
			@Test
			public void m1() { }
			@Test
			public void m2() { }
			@Test
			public void m3() { }
		}
		public static class T3 {
			public T3() { }
			@Test
			public void m1() { }
			@Test
			public void m2() { }
			@Test
			public void m3() { }
		}
		@RunWith(Suite.class)
		@Suite.SuiteClasses({
		  T1.class,
		  T2.class,
		  T3.class 
		})
		static class M0 {
			public M0() { }
		}
	}
	
	private Suite runner;

	@Before
	public void setUp() throws InitializationError {
		runner= new Suite(Data.M0.class, new RunnerBuilder() {
			@Override
			public Runner runnerForClass(Class<?> testClass) throws Throwable {
				return new BlockJUnit4ClassRunner(testClass);
			}
		});
	}

	@Test
	public void noFailures() throws Exception {
		Assert.assertEquals("M0 T1 m2 m3 m1 T2 m2 m3 m1 T3 m2 m3 m1 ", buildDescriptionOrder(runner.getDescription()));
	}

	@Test
	public void noFailuresWithSorter() throws Exception {
		runner.sort(new Sorter(new FailuresFirstSorter(new String[0])));
		Assert.assertEquals("M0 T1 m2 m3 m1 T2 m2 m3 m1 T3 m2 m3 m1 ", buildDescriptionOrder(runner.getDescription()));
	}

	@Test
	public void failuresWithSorter1() throws Exception {
		runner.sort(new Sorter(new FailuresFirstSorter(new String[] { "m2(" + Data.T1.class.getName() + ")" })));
		Assert.assertEquals("M0 T1 m2 m3 m1 T2 m2 m3 m1 T3 m2 m3 m1 ", buildDescriptionOrder(runner.getDescription()));
	}

	@Test
	public void failuresWithSorter2() throws Exception {
		runner.sort(new Sorter(new FailuresFirstSorter(new String[] { "m1(" + Data.T1.class.getName() + ")" })));
		Assert.assertEquals("M0 T1 m1 m2 m3 T2 m2 m3 m1 T3 m2 m3 m1 ", buildDescriptionOrder(runner.getDescription()));
	}

	@Test
	public void failuresWithSorter3() throws Exception {
		runner.sort(new Sorter(new FailuresFirstSorter(new String[] { "m1(" + Data.T2.class.getName() + ")" })));
		Assert.assertEquals("M0 T2 m1 m2 m3 T1 m2 m3 m1 T3 m2 m3 m1 ", buildDescriptionOrder(runner.getDescription()));
	}

	@Test
	public void failuresWithSorter4() throws Exception {
		runner.sort(new Sorter(new FailuresFirstSorter(
				new String[] { "m1(" + Data.T2.class.getName() + ")", 
							   "m1(" + Data.T3.class.getName() + ")", 
							   "m3(" + Data.T2.class.getName() + ")" })));
		Assert.assertEquals("M0 T2 m3 m1 m2 T3 m1 m2 m3 T1 m2 m3 m1 ", buildDescriptionOrder(runner.getDescription()));
	}

	private String buildDescriptionOrder(Description description) {
		StringBuilder sb= new StringBuilder();
		String displayName= description.getDisplayName();
		if (description.isSuite()) {
			int dollarIndex= displayName.lastIndexOf('$');
			sb.append(displayName.substring(dollarIndex + 1, dollarIndex + 3));
		} else {
			int parenIndex= displayName.indexOf('(');
			sb.append(displayName.substring(parenIndex - 2, parenIndex));
		}
		sb.append(" ");
		for (Description child : description.getChildren()) {
			sb.append(buildDescriptionOrder(child));
		}
		return sb.toString();
	}
}
