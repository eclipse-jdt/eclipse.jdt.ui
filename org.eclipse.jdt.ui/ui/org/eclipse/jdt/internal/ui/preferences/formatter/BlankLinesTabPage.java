/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;


public class BlankLinesTabPage extends ModifyDialogTabPage {

	private final String PREVIEW=
	"/**\n" +
	"* Blank Lines\n" +
	"*/\n\n" +
	"package foo.bar.baz;" +
	"import java.util.List;" +
	"import java.util.Vector;" +
	"public class Example {" +
	"" +
	"public static class Pair {" +
	"public String first;" +
	"public String second;" +
	"};" +
	"" +
	"private LinkedList fList;" +
	"public int counter;" +
	"" +
	"public Example(LinkedList list) {" +
	"  fList= list;" +
	"  counter= 0;" +
	"}" +
	"" +
	"public void push(Pair p) {" +
	"  fList.add(p);" +
	"  ++counter;" +
	"}" +
	"" +
	"public Object pop() {" +
	"  --counter;" +
	"  return (Pair)fList.getLast();" +
	"}" +
	"}";
	
	private final int minimumNumberLines= 0;
	private final int maximumNumberLines= 10000;
	
	private final int numColumns= 4;
	
	
	/**
	 * Create a new BlankLinesTabPage.
	 * 
	 * @param workingValues The values wherein the options are stored. 
	 */
	public BlankLinesTabPage(Map workingValues) {
		super(workingValues);
		fJavaPreview.setPreviewText(PREVIEW);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.preferences.codeformatter.ModifyDialogTabPage#doCreatePreferences(org.eclipse.swt.widgets.Composite)
	 */
	protected Composite doCreatePreferences(Composite parent) {
				
		Group group;
		
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(createGridLayout(numColumns, false));
		
		group= createGroup(numColumns, composite, "Blank lines around import declarations");
		createBlankLineTextField(group, "Before p&ackage declaration:", DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_PACKAGE);
		createBlankLineTextField(group, "After &package declaration:", DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_PACKAGE);
		
		group= createGroup(numColumns, composite, "Blank lines around import declarations");
		createBlankLineTextField(group, "Before import declaratio&n:", DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_IMPORTS);
		createBlankLineTextField(group, "After import de&claration:", DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_IMPORTS);
		
		group= createGroup(numColumns, composite, "Blank lines within class declarations");
		createBlankLineTextField(group, "Before declarations of the same &kind:", DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_NEW_CHUNK);
		createBlankLineTextField(group, "Before member cla&ss declarations:", DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_MEMBER_TYPE);
		createBlankLineTextField(group, "Before &field declarations:", DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_FIELD);
		createBlankLineTextField(group, "Before met&hod declarations:", DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_METHOD);

		createLabel(numColumns, composite,"");
		
		group= createGroup(numColumns, composite, "Existing &blank lines");
		createBlankLineTextField(group, "Number of empt&y lines to preserve:", DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE);
		return composite;
	}
	
	/**
	 * A helper method to create a number preference for blank lines.
	 */
	protected void createBlankLineTextField(Composite composite, String name, String key) {
		final NumberPreference numPref= new NumberPreference(composite, numColumns, fWorkingValues, 
				key, minimumNumberLines, maximumNumberLines, name);
		numPref.addObserver(fUpdater);
	}
}



















