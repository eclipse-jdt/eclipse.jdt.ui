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

	private final String fPreview=
	createPreviewHeader(FormatterMessages.getString("BlankLinesTabPage.preview.header")) + //$NON-NLS-1$
	"package foo.bar.baz;" + //$NON-NLS-1$
	"import java.util.List;" + //$NON-NLS-1$
	"import java.util.Vector;" + //$NON-NLS-1$
	"public class Example {" + //$NON-NLS-1$
	"public static class Pair {" + //$NON-NLS-1$
	"public String first;" + //$NON-NLS-1$
	"public String second;" + //$NON-NLS-1$
	"};" + //$NON-NLS-1$
	"private LinkedList fList;" + //$NON-NLS-1$
	"public int counter;" + //$NON-NLS-1$
	"public Example(LinkedList list) {" + //$NON-NLS-1$
	"  fList= list;" + //$NON-NLS-1$
	"  counter= 0;" + //$NON-NLS-1$
	"}" + //$NON-NLS-1$
	"public void push(Pair p) {" + //$NON-NLS-1$
	"  fList.add(p);" + //$NON-NLS-1$
	"  ++counter;" + //$NON-NLS-1$
	"}" + //$NON-NLS-1$
	"public Object pop() {" + //$NON-NLS-1$
	"  --counter;" + //$NON-NLS-1$
	"  return (Pair)fList.getLast();" + //$NON-NLS-1$
	"}" + //$NON-NLS-1$
	"}"; //$NON-NLS-1$
	
	private final int MIN_NUMBER_LINES= 0;
	private final int MAX_NUMBER_LINES= 99;
	
	private final int NUM_COLUMNS= 4;
	
	
	/**
	 * Create a new BlankLinesTabPage.
	 * 
	 * @param workingValues The values wherein the options are stored. 
	 */
	public BlankLinesTabPage(ModifyDialog modifyDialog, Map workingValues) {
		super(modifyDialog, workingValues);
		fJavaPreview.setPreviewText(fPreview);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.preferences.codeformatter.ModifyDialogTabPage#doCreatePreferences(org.eclipse.swt.widgets.Composite)
	 */
	protected Composite doCreatePreferences(Composite parent) {
				
		Group group;
		
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(createGridLayout(NUM_COLUMNS, false));
		
		group= createGroup(NUM_COLUMNS, composite, FormatterMessages.getString("BlankLinesTabPage.package.group.title")); //$NON-NLS-1$
		createBlankLineTextField(group, FormatterMessages.getString("BlankLinesTabPage.package.option.before"), DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_PACKAGE); //$NON-NLS-1$
		createBlankLineTextField(group, FormatterMessages.getString("BlankLinesTabPage.package.option.after"), DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_PACKAGE); //$NON-NLS-1$
		
		group= createGroup(NUM_COLUMNS, composite, FormatterMessages.getString("BlankLinesTabPage.import.group.title")); //$NON-NLS-1$
		createBlankLineTextField(group, FormatterMessages.getString("BlankLinesTabPage.import.option.before"), DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_IMPORTS); //$NON-NLS-1$
		createBlankLineTextField(group, FormatterMessages.getString("BlankLinesTabPage.import.option.after"), DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_IMPORTS); //$NON-NLS-1$
		
		group= createGroup(NUM_COLUMNS, composite, FormatterMessages.getString("BlankLinesTabPage.class.group.title")); //$NON-NLS-1$
		createBlankLineTextField(group, FormatterMessages.getString("BlankLinesTabPage.class.option.before_first_decl"), DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_FIRST_CLASS_BODY_DECLARATION); //$NON-NLS-1$
		createBlankLineTextField(group, FormatterMessages.getString("BlankLinesTabPage.class.option.before_decls_of_same_kind"), DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_NEW_CHUNK); //$NON-NLS-1$
		createBlankLineTextField(group, FormatterMessages.getString("BlankLinesTabPage.class.option.before_member_class_decls"), DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_MEMBER_TYPE); //$NON-NLS-1$
		createBlankLineTextField(group, FormatterMessages.getString("BlankLinesTabPage.class.option.before_field_decls"), DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_FIELD); //$NON-NLS-1$
		createBlankLineTextField(group, FormatterMessages.getString("BlankLinesTabPage.class.option.before_method_decls"), DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_METHOD); //$NON-NLS-1$

		createLabel(NUM_COLUMNS, composite,""); //$NON-NLS-1$
		
		group= createGroup(NUM_COLUMNS, composite, FormatterMessages.getString("BlankLinesTabPage.blank_lines.group.title")); //$NON-NLS-1$
		createBlankLineTextField(group, FormatterMessages.getString("BlankLinesTabPage.blank_lines.option.empty_lines_to_preserve"), DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE); //$NON-NLS-1$
		return composite;
	}
	
	/**
	 * A helper method to create a number preference for blank lines.
	 */
	protected void createBlankLineTextField(Composite composite, String name, String key) {
		final NumberPreference numPref= new NumberPreference(composite, NUM_COLUMNS, fWorkingValues, 
				key, MIN_NUMBER_LINES, MAX_NUMBER_LINES, name);
		numPref.addObserver(fUpdater);
	}
}



















