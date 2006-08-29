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

import java.util.Hashtable;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.Position;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;

public class CodeFormatterUtilTest extends CoreTests {
	
	private static final Class THIS= CodeFormatterUtilTest.class;
	
	private IJavaProject fJProject1;

	public CodeFormatterUtilTest(String name) {
		super(name);
	}
	
	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test suite() {
		if (true) {
			return allTests();
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new CodeFormatterUtilTest("testCUIndented"));
			return new ProjectTestSetup(suite);
		}	
	}


	protected void setUp() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		JavaProjectHelper.addRequiredProject(fJProject1, ProjectTestSetup.getProject());

		Hashtable options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, "999");
		JavaCore.setOptions(options);
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
	}
	
	
	public void testCU() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("    Runnable run= new Runnable() {};\n");
		buf.append("    }\n");
		buf.append("}\n");
		String contents= buf.toString();
		
	
		String formatted= CodeFormatterUtil.format(CodeFormatter.K_COMPILATION_UNIT, contents, 0, null, "\n", fJProject1);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Runnable run = new Runnable() {\n");
		buf.append("        };\n");	
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(formatted, expected);
	}
	
	public void testCUIndented() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("    Runnable run= new Runnable() {};\n");
		buf.append("    }\n");
		buf.append("}\n");
		String contents= buf.toString();
		
	
		String formatted= CodeFormatterUtil.format(CodeFormatter.K_COMPILATION_UNIT, contents, 1, null, "\n", fJProject1);

		buf= new StringBuffer();
		buf.append("    package test1;\n");
		buf.append("    public class A {\n");
		buf.append("        public void foo() {\n");
		buf.append("            Runnable run = new Runnable() {\n");
		buf.append("            };\n");	
		buf.append("        }\n");
		buf.append("    }\n");
		String expected= buf.toString();
		assertEqualString(formatted, expected);
	}	
	
	public void testCUNewAPI() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("    Runnable run= new Runnable() {};\n");
		buf.append("    }\n");
		buf.append("}\n");
		String contents= buf.toString();
			
		TextEdit edit= CodeFormatterUtil.format2(CodeFormatter.K_COMPILATION_UNIT, contents, 0, "\n", null);
		Document doc= new Document(contents);
		edit.apply(doc);
		String formatted= doc.get();
		
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Runnable run = new Runnable() {\n");
		buf.append("        };\n");	
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(formatted, expected);
	}
	
	public void testCUNewAPI2() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("/**\n");
		buf.append(" * comment\n");
		buf.append(" */\n");
		buf.append("    public void foo() {\n");
		buf.append("    Runnable run= new Runnable() {};\n");
		buf.append("    }\n");
		buf.append("}\n");
		String contents= buf.toString();
			
		TextEdit edit= CodeFormatterUtil.format2(CodeFormatter.K_COMPILATION_UNIT, contents, 0, "\n", null);
		Document doc= new Document(contents);
		edit.apply(doc);
		String formatted= doc.get();
		
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    /**\n");
		buf.append("     * comment\n");
		buf.append("     */\n");
		buf.append("    public void foo() {\n");
		buf.append("        Runnable run = new Runnable() {\n");
		buf.append("        };\n");	
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(formatted, expected);
	}		
	
	public void testCUWithPos() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("    Runnable run= new Runnable() {};\n");
		buf.append("    }\n");
		buf.append("}\n");
		String contents= buf.toString();
		
		String word1= "new";
		int start1= contents.indexOf(word1);
		int[] positions= { start1, start1 + word1.length() - 1};
		
		String formatted= CodeFormatterUtil.format(CodeFormatter.K_COMPILATION_UNIT, contents, 0, positions, "\n", fJProject1);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Runnable run = new Runnable() {\n");
		buf.append("        };\n");	
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(formatted, expected);
		
		String curr1= formatted.substring(positions[0], positions[1] + 1);
		assertEqualString(curr1, word1);
	}	
	
	public void testPackage() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("  package   com . test1;");
		String contents= buf.toString();
		
		AST ast= AST.newAST(AST.JLS3);
		PackageDeclaration decl= ast.newPackageDeclaration();
		
		String formatted= CodeFormatterUtil.format(decl, contents, 0, null, "\n", null);

		buf= new StringBuffer();
		buf.append("package com.test1;");
		String expected= buf.toString();
		assertEqualString(expected, formatted);
	}
	
	public void testPackageWithPos() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package   com . test1;");
		String contents= buf.toString();
		
		AST ast= AST.newAST(AST.JLS3);
		PackageDeclaration node= ast.newPackageDeclaration();
		
		String word1= "com";
		int start1= contents.indexOf(word1);
		
		String word2= ";";
		int start2= contents.indexOf(word2);		
		
		int[] positions= { start1, start1 + word1.length() - 1, start2, start2 + word2.length() - 1};
		
		String formatted= CodeFormatterUtil.format(node, contents, 0, positions, "\n", null);

		buf= new StringBuffer();
		buf.append("package com.test1;");
		String expected= buf.toString();
		assertEqualString(formatted, expected);
		
		String curr1= formatted.substring(positions[0], positions[1] + 1);
		assertEqualString(curr1, word1);
		
		String curr2= formatted.substring(positions[2], positions[3] + 1);
		assertEqualString(curr2, word2);
		
	}		
	
	public void testVarDeclStatemenetWithPos() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("x[ ]=\nnew  int[ offset]");
		String contents= buf.toString();
		
		AST ast= AST.newAST(AST.JLS3);
		VariableDeclarationFragment node= ast.newVariableDeclarationFragment();
		
		String word1= "new";
		int start1= contents.indexOf(word1);
		
		String word2= "offset";
		int start2= contents.indexOf(word2);		
		
		int[] positions= { start1, start1 + word1.length() - 1, start2, start2 + word2.length() - 1};
		
		String formatted= CodeFormatterUtil.format(node, contents, 0, positions, "\n", null);

		buf= new StringBuffer();
		buf.append("x[] = new int[offset]");
		String expected= buf.toString();
		assertEqualString(formatted, expected);
		
		String curr1= formatted.substring(positions[0], positions[1] + 1);
		assertEqualString(curr1, word1);
		
		String curr2= formatted.substring(positions[2], positions[3] + 1);
		assertEqualString(curr2, word2);
		
	}
	
	public void testJavadoc() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("/** bar\n");
		buf.append(" * foo\n");
		buf.append(" */\n");
		String contents= buf.toString();
		
		AST ast= AST.newAST(AST.JLS3);
		Javadoc node= ast.newJavadoc();
		
		String word1= "bar";
		int start1= contents.indexOf(word1);
		
		String word2= "foo";
		int start2= contents.indexOf(word2);		
		
		int[] positions= { start1, start1 + word1.length() - 1, start2, start2 + word2.length() - 1};
		
		String formatted= CodeFormatterUtil.format(node, contents, 0, positions, "\n", null);

		buf= new StringBuffer();
		buf.append("/** bar\n");
		buf.append(" * foo\n");
		buf.append(" */\n");
		String expected= buf.toString();
		assertEqualString(formatted, expected);
		
		String curr1= formatted.substring(positions[0], positions[1] + 1);
		assertEqualString(curr1, word1);
		
		String curr2= formatted.substring(positions[2], positions[3] + 1);
		assertEqualString(curr2, word2);
		
	}
	
	public void testJavadoc2() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("/** bar\n");
		buf.append(" * foo\n");
		buf.append(" */");
		String contents= buf.toString();
		
		AST ast= AST.newAST(AST.JLS3);
		Javadoc node= ast.newJavadoc();
		
		String word1= "bar";
		int start1= contents.indexOf(word1);
		
		String word2= "foo";
		int start2= contents.indexOf(word2);		
		
		int[] positions= { start1, start1 + word1.length() - 1, start2, start2 + word2.length() - 1};
		
		String formatted= CodeFormatterUtil.format(node, contents, 1, positions, "\n", null);

		buf= new StringBuffer();
		buf.append("    /** bar\n");
		buf.append("     * foo\n");
		buf.append("     */");
		String expected= buf.toString();
		assertEqualString(formatted, expected);
		
		String curr1= formatted.substring(positions[0], positions[1] + 1);
		assertEqualString(curr1, word1);
		
		String curr2= formatted.substring(positions[2], positions[3] + 1);
		assertEqualString(curr2, word2);
		
	}
	
	public void testJavadoc3() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("/** bar\n");
		buf.append(" * foo\n");
		buf.append(" */");
		String contents= buf.toString();
		
		AST ast= AST.newAST(AST.JLS3);
		Javadoc node= ast.newJavadoc();
		
		String word1= "bar";
		int start1= contents.indexOf(word1);
		
		String word2= "foo";
		int start2= contents.indexOf(word2);		
		
		int[] positions= { start1, start1 + word1.length() - 1, start2, start2 + word2.length() - 1};
		
		String formatted= CodeFormatterUtil.format(node, contents, 0, positions, "\r\n", null);

		buf= new StringBuffer();
		buf.append("/** bar\r\n");
		buf.append(" * foo\r\n");
		buf.append(" */");
		String expected= buf.toString();
		assertEqualString(formatted, expected);
		
		String curr1= formatted.substring(positions[0], positions[1] + 1);
		assertEqualString(curr1, word1);
		
		String curr2= formatted.substring(positions[2], positions[3] + 1);
		assertEqualString(curr2, word2);
		
	}	
	
	public void testCatchClause() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("catch\n");
		buf.append("(Exception e) {\n");
		buf.append("}");
		String contents= buf.toString();
		
		AST ast= AST.newAST(AST.JLS3);
		CatchClause node= ast.newCatchClause();
		
		String word1= "catch";
		int start1= contents.indexOf(word1);
		
		String word2= "Exception";
		int start2= contents.indexOf(word2);		
		
		int[] positions= { start1, start1 + word1.length() - 1, start2, start2 + word2.length() - 1};
		
		String formatted= CodeFormatterUtil.format(node, contents, 0, positions, "\n", null);

		buf= new StringBuffer();
		buf.append(" catch (Exception e) {\n");
		buf.append("}");
		String expected= buf.toString();
		assertEqualString(formatted, expected);
		
		String curr1= formatted.substring(positions[0], positions[1] + 1);
		assertEqualString(curr1, word1);
		
		String curr2= formatted.substring(positions[2], positions[3] + 1);
		assertEqualString(curr2, word2);
		
	}
	
	public void testCatchStringLiteral() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("\"Hello\" ");
		String contents= buf.toString();
		
		AST ast= AST.newAST(AST.JLS3);
		StringLiteral node= ast.newStringLiteral();
				
		String formatted= CodeFormatterUtil.format(node, contents, 0, null, "\n", null);

		buf= new StringBuffer();
		buf.append("\"Hello\"");
		String expected= buf.toString();
		assertEqualString(formatted, expected);
				
	}	
	
	public void testFormatSubstring() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("    Runnable runnable= new Runnable() {};\n");
		buf.append("    runnable.toString();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String contents= buf.toString();
		
		String formatString= "Runnable runnable= new Runnable() {};";
		int formatStart= contents.indexOf(formatString);
		int formatLength= formatString.length();
		
		
		String word1= "import";
		int start1= contents.indexOf(word1);
		
		String word2= "new";
		int start2= contents.indexOf(word2);
		
		String word3= "toString";
		int start3= contents.indexOf(word3);		
				
		Position pos1= new Position(start1, word1.length());
		Position pos2= new Position(start2, word2.length());
		Position pos3= new Position(start3, word3.length());
		
		TextEdit edit= CodeFormatterUtil.format2(CodeFormatter.K_COMPILATION_UNIT, contents, formatStart, formatLength, 0, "\n", null);
		assertNotNull(edit);
		String formatted= CodeFormatterUtil.evaluateFormatterEdit(contents, edit, new Position[] { pos1, pos2, pos3});
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Runnable runnable = new Runnable() {\n");
		buf.append("        };\n");
		buf.append("    runnable.toString();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(formatted, expected);
		
		String curr1= formatted.substring(pos1.offset, pos1.getOffset() + pos1.getLength());
		assertEqualString(curr1, word1);
		
		String curr2= formatted.substring(pos2.offset, pos2.getOffset() + pos2.getLength());
		assertEqualString(curr2, word2);
		
		String curr3= formatted.substring(pos3.offset, pos3.getOffset() + pos3.getLength());
		assertEqualString(curr3, word3);
		
	}	

	
	
}
