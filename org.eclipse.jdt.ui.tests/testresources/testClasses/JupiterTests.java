/*******************************************************************************
 * Copyright (c) 2023 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;

import junit.framework.TestCase;

/**
 * This class contains some internal classes that should be found/not found by the
 * JUnit5TestFinder.<br/>
 * <br/>
 * The names of the classes give a hint about whether or not the class should be found and the
 * reason for that. In order to be discovered, a class needs to fulfill these requirements:
 * <ul>
 * <li>It should be visible (it can <strong>not</strong> be <code>private</code>)</li>
 * <li>It must have tests in it.</li>
 * </ul>
 *
 * Whether or not the class can be instantiated (<i>i.e.</i> if they have a non-private empty
 * constructor) does not play a role in the discoverability, but running tests on that class will
 * throw an exception,
 */
class JupiterTests {
	/**
	 * Methods using this annotation are also considered tests
	 */
	@Test
	@Retention(RetentionPolicy.RUNTIME)
	@interface CustomTestAnnotation {}

	static class FoundStatic {
		@Test void myTest() {}
	}

	static class FoundStaticCustomTestAnnotation {
		@CustomTestAnnotation void myTest() {}
	}

	private static class NotFoundPrivate {
		@CustomTestAnnotation void myTest() {}
	}

	static class NotFoundHasNoTests {}

	static class FoundExtendsTestCase extends TestCase {
		@Test void myTest() {}
	}

	static class FoundExtendsTestCaseCustomTestAnnotation extends TestCase {
		@CustomTestAnnotation public void myTest() {}
	}

	private static class NotFoundPrivateExtendsTestCase extends TestCase {
		@Test public void myTest() {}
	}

	static class FoundTestTemplateClass {
		@TestTemplate void myTestTemplate() {}
	}

	static abstract class NotFoundAbstractWithInnerClass {
		class NotFoundInnerInstanceClassWithTest {
			@Test void myTest() {}
		}
	}
	
	static class NotFoundExtendsAbstractWithInnerWithTest extends NotFoundAbstractWithInnerClass {}

	static class FoundHasInnerClassWithNested {
		@Nested class FoundExtendsAbstractWithNested extends NotFoundAbstractWithInnerClass {
			@Test void myTest() {}
		}
	}
}
