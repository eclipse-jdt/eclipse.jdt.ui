/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.astrewrite;

import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;

import org.eclipse.jface.text.Document;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.corext.dom.SourceModifier;

/**
 *
 */
public class SourceModifierTest extends ASTRewritingTest {
	
	private static final Class THIS= SourceModifierTest.class;
	
	private IJavaProject fJProject1;

	public SourceModifierTest(String name) {
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
			suite.addTest(new SourceModifierTest("testCollapsedTargetNodes2"));
			return new ProjectTestSetup(suite);
		}
	}
	
	protected void setUp() throws Exception {
		Hashtable options= JavaCore.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);			
		
		fJProject1= ProjectTestSetup.getProject();
	}
	
	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}

	
	
	public void testRemoveIndents() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        while (i == 0) {\n");
		buf.append("            foo();\n");
		buf.append("            i++; // comment\n");
		buf.append("            i++;\n");
		buf.append("        }\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("}\n");

		Document buffer= new Document(buf.toString());
		
		int offset= buf.toString().indexOf("while");
		int length= buf.toString().indexOf("return;") + "return;".length() - offset;
		
		String content= buffer.get(offset, length);
		SourceModifier modifier= new SourceModifier(2, "    ", 4);
		MultiTextEdit edit= new MultiTextEdit(0, content.length());
		ReplaceEdit[] replaces= modifier.getModifications(content);
		for (int i= 0; i < replaces.length; i++) {
			edit.addChild(replaces[i]);
		}
		
		Document innerBuffer= new Document(content);
		edit.apply(innerBuffer);
		
		buffer.replace(offset, length, innerBuffer.get());
				
		String preview= buffer.get();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        while (i == 0) {\n");
		buf.append("        foo();\n");
		buf.append("        i++; // comment\n");
		buf.append("        i++;\n");
		buf.append("    }\n");
		buf.append("    return;\n");
		buf.append("    }\n");
		buf.append("}\n");		
		String expected= buf.toString();		

		assertEqualString(preview, expected);
	}
	
	public void testAddIndents() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        while (i == 0) {\n");
		buf.append("            foo();\n");
		buf.append("            i++; // comment\n");
		buf.append("            i++;\n");
		buf.append("        }\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("}\n");

		Document buffer= new Document(buf.toString());
		
		int offset= buf.toString().indexOf("while");
		int length= buf.toString().indexOf("return;") + "return;".length() - offset;
		
		String content= buffer.get(offset, length);
		SourceModifier modifier= new SourceModifier(2, "            ", 4);
		MultiTextEdit edit= new MultiTextEdit(0, content.length());
		ReplaceEdit[] replaces= modifier.getModifications(content);
		for (int i= 0; i < replaces.length; i++) {
			edit.addChild(replaces[i]);
		}
		
		Document innerBuffer= new Document(content);
		edit.apply(innerBuffer);
		
		buffer.replace(offset, length, innerBuffer.get());
		
		String preview= buffer.get();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        while (i == 0) {\n");
		buf.append("                foo();\n");
		buf.append("                i++; // comment\n");
		buf.append("                i++;\n");
		buf.append("            }\n");
		buf.append("            return;\n");
		buf.append("    }\n");
		buf.append("}\n");		
		String expected= buf.toString();		

		assertEqualString(preview, expected);
	}
}
