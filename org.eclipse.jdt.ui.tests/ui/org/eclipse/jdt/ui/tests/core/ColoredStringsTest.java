/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.TextStyle;

import org.eclipse.jdt.internal.ui.viewsupport.ColoredString;
import org.eclipse.jdt.internal.ui.viewsupport.ColoredString.Style;

public class ColoredStringsTest extends TestCase {
	
	public static class TestStyle extends Style {
		
		public final int borderStyle;

		public TestStyle(int borderStyle) {
			this.borderStyle= borderStyle;
		}
		
		public void applyStyles(TextStyle textStyle) {
			textStyle.borderStyle= borderStyle;
		}
	}
	
	public static final TestStyle STYLE1= new TestStyle(SWT.BORDER_DOT);
	public static final TestStyle STYLE2= new TestStyle(SWT.BORDER_DASH);
	
	
	public static Test allTests() {
		return new TestSuite(ColoredStringsTest.class);
	}

	public static Test suite() {
		return allTests();
	}
	
	public void testEmpty() {
		ColoredString coloredString= new ColoredString();
		
		String str= "";
		
		assertEquals(str.length(), coloredString.length());
		assertEquals(str, coloredString.getString());
		assertEquals(coloredString.getStyleRanges().length, 0);
	}
	
	public void testAppendString1() {
		ColoredString coloredString= new ColoredString();
		
		String str= "Hello";
		
		coloredString.append(str, STYLE1);
		
		assertEquals(str.length(), coloredString.length());
		assertEquals(str, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(1, styleRanges.length);
		
		assertEquals(styleRanges[0], STYLE1, 0, str.length());
	}
	
	public void testAppendString2() {
		ColoredString coloredString= new ColoredString();
		
		String str1= "Hello";
		String str2= "You";
		coloredString.append(str1);
		coloredString.append(str2, STYLE1);
		
		String res= str1 + str2;
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(1, styleRanges.length);
		assertEquals(styleRanges[0], STYLE1, str1.length(), str2.length());
	}
	
	public void testAppendString3() {
		ColoredString coloredString= new ColoredString();
		
		String str1= "Hello";
		String str2= "You";
		coloredString.append(str1, STYLE1);
		coloredString.append(str2);
		
		String res= str1 + str2;
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(1, styleRanges.length);
		assertEquals(styleRanges[0], STYLE1, 0, str1.length());
	}
	
	public void testAppendString4() {
		ColoredString coloredString= new ColoredString();
		
		String str1= "Hello";
		String str2= "You";
		coloredString.append(str1);
		coloredString.append(str2, STYLE1);
		coloredString.append(str2, STYLE1);
		
		String res= str1 + str2 + str2;
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(1, styleRanges.length);
		assertEquals(styleRanges[0], STYLE1, str1.length(), str2.length() * 2);
	}
	
	public void testAppendString5() {
		ColoredString coloredString= new ColoredString();
		
		String str1= "Hello";
		String str2= "You";
		String str3= "Me";
		coloredString.append(str1);
		coloredString.append(str2, STYLE1);
		coloredString.append(str3, STYLE2);
		
		String res= str1 + str2 + str3;
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(2, styleRanges.length);
		assertEquals(styleRanges[0], STYLE1, str1.length(), str2.length());
		assertEquals(styleRanges[1], STYLE2, str1.length() + str2.length(), str3.length());
	}
	
	public void testAppendString6() {
		ColoredString coloredString= new ColoredString();
		
		String str1= "Hello";
		String str2= "You";
		String str3= "Me";
		coloredString.append(str1, STYLE1);
		coloredString.append(str2);
		coloredString.append(str3, STYLE2);
		
		String res= str1 + str2 + str3;
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(2, styleRanges.length);
		assertEquals(styleRanges[0], STYLE1, 0, str1.length());
		assertEquals(styleRanges[1], STYLE2, str1.length() + str2.length(), str3.length());
	}
	
	public void testAppendString7() {
		ColoredString coloredString= new ColoredString();
		
		String str1= "Hello";
		String str2= "";
		String str3= "Me";
		coloredString.append(str1, STYLE1);
		coloredString.append(str2, STYLE2);
		coloredString.append(str3, STYLE1);
		
		String res= str1 + str2 + str3;
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(1, styleRanges.length);
		assertEquals(styleRanges[0], STYLE1, 0, res.length());
	}
	
	public void testAppendChar1() {
		ColoredString coloredString= new ColoredString();
		
		coloredString.append('H', STYLE1);
		coloredString.append('2', STYLE2);
		coloredString.append('O', STYLE1);
		
		String res= "H2O";
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(3, styleRanges.length);
		assertEquals(styleRanges[0], STYLE1, 0, 1);
		assertEquals(styleRanges[1], STYLE2, 1, 1);
		assertEquals(styleRanges[2], STYLE1, 2, 1);
	}
	
	public void testAppendChar2() {
		ColoredString coloredString= new ColoredString();
		
		coloredString.append('H', STYLE1);
		coloredString.append('2');
		coloredString.append('O', STYLE2);
		
		String res= "H2O";
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(2, styleRanges.length);
		assertEquals(styleRanges[0], STYLE1, 0, 1);
		assertEquals(styleRanges[1], STYLE2, 2, 1);
	}
	
	public void testAppendColoredString1() {
		ColoredString other= new ColoredString();
		
		String str2= "You";
		String str3= "Me";
		other.append(str2, STYLE1);
		other.append(str3, STYLE2);
		
		String str1= "We";
		
		ColoredString coloredString= new ColoredString(str1);
		coloredString.append(other);
		
		String res= str1 + str2 + str3;
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(2, styleRanges.length);
		assertEquals(styleRanges[0], STYLE1, str1.length(), str2.length());
		assertEquals(styleRanges[1], STYLE2, str1.length() + str2.length(), str3.length());
	}
	
	public void testAppendColoredString2() {
		ColoredString other= new ColoredString();
		
		String str2= "You";
		String str3= "Me";
		other.append(str2, STYLE1);
		other.append(str3, STYLE2);
		
		String str1= "We";
		
		ColoredString coloredString= new ColoredString(str1, STYLE1);
		coloredString.append(other);
		
		String res= str1 + str2 + str3;
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(2, styleRanges.length);
		assertEquals(styleRanges[0], STYLE1, 0, str1.length() + str2.length());
		assertEquals(styleRanges[1], STYLE2, str1.length() + str2.length(), str3.length());
	}
	
	public void testAppendColoredString3() {
		
		ColoredString other= new ColoredString();
		
		String str2= "You";
		String str3= "Me";
		other.append(str2);
		other.append(str3, STYLE2);
		
		String str1= "We";
		
		ColoredString coloredString= new ColoredString(str1, STYLE1);
		coloredString.append(other);
		
		String res= str1 + str2 + str3;
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(2, styleRanges.length);
		assertEquals(styleRanges[0], STYLE1, 0, str1.length());
		assertEquals(styleRanges[1], STYLE2, str1.length() + str2.length(), str3.length());
	}
	
	public void testAppendColoredString4() {
		
		ColoredString other= new ColoredString();
		
		String str2= "You";
		String str3= "Me";
		other.append(str2, STYLE2);
		other.append(str3);
		
		String str1= "We";
		
		ColoredString coloredString= new ColoredString(str1, STYLE1);
		coloredString.append(other);
		
		String res= str1 + str2 + str3;
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(2, styleRanges.length);
		assertEquals(styleRanges[0], STYLE1, 0, str1.length());
		assertEquals(styleRanges[1], STYLE2, str1.length(), str2.length());
	}
	
	public void testAppendColoredString5() {
		ColoredString other= new ColoredString();
		
		String str2= "You";
		String str3= "Me";
		other.append(str2);
		other.append(str3, STYLE1);
		
		String str1= "We";
		
		ColoredString coloredString= new ColoredString(str1);
		coloredString.append(other);
		
		String res= str1 + str2 + str3;
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(1, styleRanges.length);
		assertEquals(styleRanges[0], STYLE1, str1.length() +  str2.length(), str3.length());
	}
	
	public void testSetStyle1() {
		String str1= "One";
		String str2= "Two";
		String str3= "Three";
		
		String res= str1 + str2 + str3;
		
		ColoredString coloredString= new ColoredString();
		coloredString.append(res);
		
		coloredString.setStyle(0, str1.length(), STYLE1);
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(1, styleRanges.length);
		assertEquals(styleRanges[0], STYLE1, 0, str1.length());
	}
	
	public void testSetStyle2() {
		
		String str1= "One";
		String str2= "Two";
		String str3= "Three";
		
		String res= str1 + str2 + str3;
		
		ColoredString coloredString= new ColoredString();
		coloredString.append(res);
		
		coloredString.setStyle(str1.length(), str2.length(), STYLE1);
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(1, styleRanges.length);
		assertEquals(styleRanges[0], STYLE1, str1.length(), str2.length());
	}
	
	public void testSetStyle3() {
		
		String str1= "One";
		String str2= "Two";
		String str3= "Three";
		
		String res= str1 + str2 + str3;
		
		ColoredString coloredString= new ColoredString();
		coloredString.append(res);
		
		coloredString.setStyle(str1.length(), res.length() - str1.length(), STYLE1);
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(1, styleRanges.length);
		assertEquals(styleRanges[0], STYLE1, str1.length(), res.length() - str1.length());
	}
	
	public void testSetStyle4() {
		
		String str1= "One";
		String str2= "Two";
		String str3= "Three";
		
		String res= str1 + str2 + str3;
		
		ColoredString coloredString= new ColoredString();
		coloredString.append(res);
		
		coloredString.setStyle(0, res.length(), STYLE1);
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(1, styleRanges.length);
		assertEquals(styleRanges[0], STYLE1, 0, res.length());
	}
	
	public void testSetStyle5() {
		
		String str1= "One";
		String str2= "Two";
		String str3= "Three";
		
		String res= str1 + str2 + str3;
		
		ColoredString coloredString= new ColoredString();
		coloredString.append(res);
		
		coloredString.setStyle(0, res.length(), null);
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(0, styleRanges.length);
	}
	
	public void testSetStyle6() {
		
		String str1= "One";
		String str2= "Two";
		
		String res= str1 + str2;
		
		ColoredString coloredString= new ColoredString(str1, STYLE1);
		coloredString.append(str2);
		
		coloredString.setStyle(str1.length(), str2.length(), STYLE2);
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(2, styleRanges.length);
		assertEquals(styleRanges[0], STYLE1, 0, str1.length());
		assertEquals(styleRanges[1], STYLE2, str1.length(), str2.length());
	}
	
	public void testSetStyle7() {
		
		String str1= "One";
		String str2= "Two";
		
		String res= str1 + str2;
		
		ColoredString coloredString= new ColoredString(str1);
		coloredString.append(str2, STYLE1);
		
		coloredString.setStyle(0, str1.length(), STYLE2);
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(2, styleRanges.length);
		assertEquals(styleRanges[0], STYLE2, 0, str1.length());
		assertEquals(styleRanges[1], STYLE1, str1.length(), str2.length());
	}
	
	public void testSetStyle8() {
		
		String str1= "One";
		String str2= "Two";
		
		String res= str1 + str2;
		
		ColoredString coloredString= new ColoredString();
		coloredString.append(str1, STYLE1);
		coloredString.append(str2, STYLE2);
		
		coloredString.setStyle(0, str1.length(), STYLE2);
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(1, styleRanges.length);
		assertEquals(styleRanges[0], STYLE2, 0, res.length());
	}
	
	public void testSetStyle9() {
		
		String str1= "One";
		String str2= "Two";
		
		String res= str1 + str2;
		
		ColoredString coloredString= new ColoredString();
		coloredString.append(str1, STYLE1);
		coloredString.append(str2, STYLE2);
		
		coloredString.setStyle(0, res.length(), null);
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(0, styleRanges.length);
	}
	
	public void testSetStyle10() {
		
		String str1= "One";
		String str2= "Two";
		
		String res= str1 + str2;
		
		ColoredString coloredString= new ColoredString();
		coloredString.append(str1, STYLE1);
		coloredString.append(str2, STYLE2);
		
		coloredString.setStyle(1, res.length() - 2, null);
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(2, styleRanges.length);
		
		assertEquals(styleRanges[0], STYLE1, 0, 1);
		assertEquals(styleRanges[1], STYLE2, res.length() - 1, 1);
	}
	
	public void testSetStyle11() {
		
		String str1= "One";
		String str2= "Two";
		
		String res= str1 + str2;
		
		ColoredString coloredString= new ColoredString();
		coloredString.append(str1, STYLE1);
		coloredString.append(str2, STYLE2);
		
		coloredString.setStyle(1, res.length() - 1, STYLE1);
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(1, styleRanges.length);
		
		assertEquals(styleRanges[0], STYLE1, 0, res.length());
	}
	
	public void testSetStyle12() {
		
		String str1= "One";
		String str2= "Two";
		
		String res= str1 + str2;
		
		ColoredString coloredString= new ColoredString();
		coloredString.append(str1, STYLE1);
		coloredString.append(str2, STYLE2);
		
		coloredString.setStyle(0, res.length() - 1, STYLE2);
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(1, styleRanges.length);
		
		assertEquals(styleRanges[0], STYLE2, 0, res.length());
	}
	
	public void testSetStyle13() {
		
		String str1= "One";
		String str2= "Two";
		
		String res= str1 + str2;
		
		ColoredString coloredString= new ColoredString();
		coloredString.append(str1, STYLE1);
		coloredString.append(str2, STYLE2);
		
		coloredString.setStyle(1, res.length() - 2, STYLE1);
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(2, styleRanges.length);
		
		assertEquals(styleRanges[0], STYLE1, 0, res.length() - 1);
		assertEquals(styleRanges[1], STYLE2, res.length() - 1, 1);
	}
	
	public void testSetStyle14() {
		
		String str1= "One";
		String str2= "Two";
		
		String res= str1 + str2;
		
		ColoredString coloredString= new ColoredString();
		coloredString.append(str1, STYLE1);
		coloredString.append(str2, STYLE2);
		
		coloredString.setStyle(1, res.length() - 2, STYLE2);
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(2, styleRanges.length);
		
		assertEquals(styleRanges[0], STYLE1, 0, 1);
		assertEquals(styleRanges[1], STYLE2, 1, res.length() - 1);
	}
	
	public void testSetStyle15() {
		
		String str1= "One";
		String str2= "Two";
		
		String res= str1 + str2;
		
		ColoredString coloredString= new ColoredString();
		coloredString.append(str1, null);
		coloredString.append(str2, STYLE2);
		
		coloredString.setStyle(0, 1, STYLE1);
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(2, styleRanges.length);
		
		assertEquals(styleRanges[0], STYLE1, 0, 1);
		assertEquals(styleRanges[1], STYLE2, str1.length(), str2.length());
	}
	
	public void testSetStyle16() {
				
		String res= "H2O";
		
		ColoredString coloredString= new ColoredString();
		coloredString.append('H', null);
		coloredString.append('2', STYLE1);
		coloredString.append('O', STYLE2);
		
		coloredString.setStyle(0, res.length(), STYLE1);
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(1, styleRanges.length);
		
		assertEquals(styleRanges[0], STYLE1, 0, res.length());
	}
	
	public void testSetStyle17() {
		
		String res= "H2O";
		
		ColoredString coloredString= new ColoredString();
		coloredString.append('H', null);
		coloredString.append('2', STYLE1);
		coloredString.append('O', STYLE2);
		
		coloredString.setStyle(0, res.length(), null);
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(0, styleRanges.length);
	}
	
	public void testSetStyle18() {
		String res= "H2OH2O";
		
		ColoredString coloredString= new ColoredString();
		coloredString.append('H', null);
		coloredString.append('2', STYLE1);
		coloredString.append('O', STYLE2);
		coloredString.append('H', null);
		coloredString.append('2', STYLE2);
		coloredString.append('O', STYLE1);
		
		coloredString.setStyle(1, res.length() - 2, STYLE1);
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(1, styleRanges.length);
		
		assertEquals(styleRanges[0], STYLE1, 1, res.length() - 1);
	}
	
	public void testSetStyle19() {
		String res= "O2O2O2O2O2O2";
		
		ColoredString coloredString= new ColoredString();
		coloredString.append("O2", null);
		coloredString.append("O2", STYLE1);
		coloredString.append("O2", STYLE2);
		coloredString.append("O2", STYLE1);
		coloredString.append("O2", STYLE2);
		coloredString.append("O2", null);
		
		coloredString.setStyle(1, res.length() - 2, STYLE1);
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(1, styleRanges.length);
		
		assertEquals(styleRanges[0], STYLE1, 1, res.length() - 2);
	}
	
	public void testSetStyle20() {
		String res= "O2O2O2O2O2O2";
		
		ColoredString coloredString= new ColoredString();
		coloredString.append("O2", null);
		coloredString.append("O2", STYLE1);
		coloredString.append("O2", STYLE2);
		coloredString.append("O2", STYLE1);
		coloredString.append("O2", STYLE2);
		coloredString.append("O2", null);
		
		coloredString.setStyle(3, 6, null);
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(2, styleRanges.length);
		
		assertEquals(styleRanges[0], STYLE1, 2, 1);
		assertEquals(styleRanges[1], STYLE2, 9, 1);
	}
	
	public void testSetStyle21() {
		String res= "O2O2O2O2O2O2";
		
		ColoredString coloredString= new ColoredString();
		coloredString.append("O2", null);
		coloredString.append("O2", STYLE1);
		coloredString.append("O2", STYLE2);
		coloredString.append("O2", STYLE1);
		coloredString.append("O2", STYLE2);
		coloredString.append("O2", null);
		
		coloredString.setStyle(3, 6, STYLE1);
		coloredString.setStyle(3, 6, null);
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(2, styleRanges.length);
		
		assertEquals(styleRanges[0], STYLE1, 2, 1);
		assertEquals(styleRanges[1], STYLE2, 9, 1);
	}
	
	public void testCombination1() {
		String str1= "One";
		String str2= "Two";
		
		String res= str1 + str2 + str1;
		
		ColoredString coloredString= new ColoredString();
		coloredString.append(str1, null);
		coloredString.append(str2, STYLE2);
		
		coloredString.setStyle(str1.length(), str2.length(), STYLE1);
		
		coloredString.append(str1, STYLE1);
		
		assertEquals(res.length(), coloredString.length());
		assertEquals(res, coloredString.getString());
		StyleRange[] styleRanges= coloredString.getStyleRanges();
		assertEquals(1, styleRanges.length);
		
		assertEquals(styleRanges[0], STYLE1, str1.length(), str2.length() + str1.length());
	}
	
	
	private void assertEquals(StyleRange range, TestStyle style, int offset, int length) {
		assertEquals(offset, range.start);
		assertEquals(length, range.length);
		assertEquals(style.borderStyle, range.borderStyle);
	}
	
	
}