/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import java.lang.reflect.Field;
import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.StringAsserts;

import org.eclipse.jdt.internal.corext.util.CollectionsUtil;

import org.eclipse.jdt.internal.ui.text.JavaElementPrefixPatternMatcher;

public class JavaElementPrefixPatternMatcherTest extends TestCase {

	private static final String toString_int_int= "toString(int, int) : String - java.lang.Integer";

	private static final String toString_int= "toString(int) : String - java.lang.Integer";

	private static final String parseInt_String= "parseInt(String) : int - java.lang.Integer";

	private static final String parseInt_String_int= "parseInt(String, int) : int - java.lang.Integer";

	private static final String Integer_int= "Integer(int) - java.lang.Integer";

	private static final String MIN_VALUE__int= "MIN_VALUE : int - java.lang.Integer";

	private static final String TYPE__Class_Integer= "TYPE : Class<Integer> - java.lang.Integer";

	private static final String DigitTens__char_= "DigitTens : char[] - java.lang.Integer";


	private static final String Object= "Object - java.lang";

	private static final String AtomicInteger= "AtomicInteger - java.util.concurrent.atomic";

	private static final String Number= "Number - java.lang";

	@SuppressWarnings("unused")
	private static final String AtomicLong= "AtomicLong - java.util.concurrent.atomic";

	private static final String BigDecimal= "BigDecimal - java.math";

	private static final String Byte= "Byte - java.lang";

	private static final String Long= "Long - java.lang";

	private static final String BigInteger= "BigInteger - java.math";

	private static final String Double= "Double - java.lang";

	private static final String[] ALL_STRING_CONSTANTS;
	static {
		ArrayList<String> strings= new ArrayList<String>();
		for (Field field : JavaElementPrefixPatternMatcherTest.class.getDeclaredFields()) {
			if (String.class.equals(field.getType()))
				try {
					strings.add((String) field.get(null));
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
		}
		ALL_STRING_CONSTANTS= strings.toArray(new String[strings.size()]);
	}

	public static Test suite() {
		return new TestSuite(JavaElementPrefixPatternMatcherTest.class);
	}

	private static void doMatch(String pattern, String... labelStrings) {
		JavaElementPrefixPatternMatcher matcher= new JavaElementPrefixPatternMatcher(pattern);
		
		ArrayList<String> matched= new ArrayList<String>();
		for (String label : ALL_STRING_CONSTANTS) {
			if (matcher.matches(label))
				matched.add(label);
		}
		
		StringAsserts.assertEqualStringsIgnoreOrder(CollectionsUtil.toArray(matched, String.class), labelStrings);
	}


	public void testEmptyPattern() throws Exception {
		doMatch("", ALL_STRING_CONSTANTS);
	}

	public void testStarPattern() throws Exception {
		doMatch("*", ALL_STRING_CONSTANTS);
	}

	
	public void testMethodPattern_1() throws Exception {
		doMatch("*tos", toString_int_int, toString_int);
	}


	public void testMethodPattern_2() throws Exception {
		doMatch("tS", toString_int_int, toString_int);
	}

	public void testMethodPattern_3() throws Exception {
		doMatch("tS(", toString_int_int, toString_int);
	}

	public void testMethodPattern_4() throws Exception {
		doMatch("tS(int", toString_int_int, toString_int);
	}

	public void testMethodPattern_5() throws Exception {
		doMatch("tS(int, ", toString_int_int);
	}

	public void testMethodPattern_6() throws Exception {
		doMatch("toString(int, int) : S", toString_int_int);
	}

	public void testMethodPattern_7() throws Exception {
		doMatch("*(in", toString_int_int, toString_int, Integer_int);
	}

	public void testMethodPattern_8() throws Exception {
		doMatch("*) : St", toString_int_int, toString_int);
	}

	public void testMethodPattern_9() throws Exception {
		doMatch("p*) : i", parseInt_String, parseInt_String_int);
	}

	public void testMethodPattern_10() throws Exception {
		doMatch("to*ng(", toString_int_int, toString_int);		
	}

	public void testMethodPattern_11() throws Exception {
		doMatch("tS*ng(");
	}
	
	public void testReturnPattern_1() throws Exception {
		doMatch("*: int", parseInt_String, parseInt_String_int, MIN_VALUE__int);
	}

	public void testReturnPattern_2() throws Exception {
		doMatch("*: Class<In", TYPE__Class_Integer);
	}


	public void testFieldPattern_1() throws Exception {
		doMatch("DT : ", DigitTens__char_);
	}
	

	public void testTypePattern_2() throws Exception {
		doMatch("AI", AtomicInteger);
	}

	public void testTypePattern_3() throws Exception {
		doMatch("b", BigDecimal, Byte, BigInteger);
	}

	public void testDeclaringClassPattern_1() throws Exception {
		doMatch("*-", ALL_STRING_CONSTANTS);
	}

	public void testDeclaringClassPattern_2() throws Exception {
		doMatch("*- java.lang", toString_int_int, toString_int, parseInt_String, parseInt_String_int, Integer_int, MIN_VALUE__int, TYPE__Class_Integer, DigitTens__char_, Object, Number, Byte, Long, Double);
	}

}
