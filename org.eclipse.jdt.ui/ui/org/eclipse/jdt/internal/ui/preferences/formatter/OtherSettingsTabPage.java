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

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;


public class OtherSettingsTabPage extends ModifyDialogTabPage {
	
	private final String fPreview=
		createPreviewHeader(FormatterMessages.getString("OtherSettingsTabPage.preview.header")) + //$NON-NLS-1$
		"class Example {" + //$NON-NLS-1$
		"int theInt= 1;" + //$NON-NLS-1$
		"String someString= \"Hello\";" + //$NON-NLS-1$
		"double aDouble= 3.0;" + //$NON-NLS-1$
		"void foo(int a, int b, int c, int d, int e, int f) {" + //$NON-NLS-1$
		"}" + //$NON-NLS-1$
		"}"; //$NON-NLS-1$
		
	private final String [] MULTI_ALIGN_VALUES= {
	    DefaultCodeFormatterConstants.FORMATTER_NO_ALIGNMENT,
	    DefaultCodeFormatterConstants.FORMATTER_MULTICOLUMN
	};
	
	private final int NUM_COLUMNS= 4;
	
	/**
	 * Create a new GeneralSettingsTabPage.
	 */
	public OtherSettingsTabPage(ModifyDialog modifyDialog, Map workingValues) {
		super(modifyDialog, workingValues);
		fJavaPreview.setPreviewText(fPreview);
	}
	
	
	protected Composite doCreatePreferences(Composite parent) {
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(createGridLayout(NUM_COLUMNS, false));

		final Group generalGroup= createGroup(NUM_COLUMNS, composite, FormatterMessages.getString("OtherSettingsTabPage.general_group.title")); //$NON-NLS-1$
		createNumberPref(generalGroup, NUM_COLUMNS, FormatterMessages.getString("OtherSettingsTabPage.general_group.option.max_line_width"), DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, 0, Integer.MAX_VALUE); //$NON-NLS-1$
		createNumberPref(generalGroup, NUM_COLUMNS, FormatterMessages.getString("OtherSettingsTabPage.general_group.option.tab_size"), DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, 0, 999); //$NON-NLS-1$
		createCheckboxPref(generalGroup, NUM_COLUMNS, FormatterMessages.getString("OtherSettingsTabPage.general_group.option.use_tab_char"), DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, new String [] {JavaCore.SPACE, JavaCore.TAB}); //$NON-NLS-1$
		
		final Group typeMemberGroup= createGroup(NUM_COLUMNS, composite, FormatterMessages.getString("OtherSettingsTabPage.field_alignment_group.title")); //$NON-NLS-1$
		createCheckboxPref(typeMemberGroup, NUM_COLUMNS, FormatterMessages.getString("OtherSettingsTabPage.field_alignment_group.align_fields_in_columns"), DefaultCodeFormatterConstants.FORMATTER_TYPE_MEMBER_ALIGNMENT, MULTI_ALIGN_VALUES); //$NON-NLS-1$
		
		return composite;
	}
}
