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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jdt.ui.PreferenceConstants;

/**
 * Tab page for the comment formatter settings.
 */
public class CommentsTabPage extends ModifyDialogTabPage {
	
	
	private final class Controller implements Observer {
		
		private final Collection fMasters;
		private final Collection fSlaves;
		
		public Controller(Collection masters, Collection slaves) {
			fMasters= masters;
			fSlaves= slaves;
			for (final Iterator iter= fMasters.iterator(); iter.hasNext();) {
			    ((CheckboxPreference)iter.next()).addObserver(this);
			}
			update(null, null);
		}

		public void update(Observable o, Object arg) {
		    boolean enabled= true; 

		    for (final Iterator iter= fMasters.iterator(); iter.hasNext();) {
		        enabled &= ((CheckboxPreference)iter.next()).getChecked();
		    }

			for (final Iterator iter = fSlaves.iterator(); iter.hasNext();) {
			    Object obj= iter.next();
			    if (obj instanceof Preference) {
			        ((Preference)obj).setEnabled(enabled);
			    } else if (obj instanceof Control) {
			        ((Group)obj).setEnabled(enabled);
			    }
			}
		}
	}
	
	
	final int NUM_COLUMNS= 4;
	

	final String fPreview=
		createPreviewHeader("An example for comment formatting. This example is meant to illustrate the various possiblilities offered by <i>Eclipse</i> in order to format comments.") +	//$NON-NLS-1$
		"package mypackage;\n" + //$NON-NLS-1$
		"/**\n" + //$NON-NLS-1$
		" * This is the comment for the example interface.\n" + //$NON-NLS-1$
		" */\n" + //$NON-NLS-1$
		" interface Example {" + //$NON-NLS-1$
		" /**\n" + //$NON-NLS-1$
		" *\n" + //$NON-NLS-1$
		" * These possibilities include:\n" + //$NON-NLS-1$
		" * <ul><li>Formatting of header comments.</li><li>Formatting of Javadoc tags</li></ul>\n" + //$NON-NLS-1$
		" */\n" + //$NON-NLS-1$
		" int bar();" + //$NON-NLS-1$
		" /**\n" + //$NON-NLS-1$
		" * The following is some sample code which illustrates source formatting within javadoc comments:\n" + //$NON-NLS-1$
		" * <pre>public class Example {final int a= 1;final boolean b= true;}</pre>\n" + //$NON-NLS-1$ 
		" * @param a The first parameter. For an optimum result, this should be an odd number\n" + //$NON-NLS-1$
		" * between 0 and 100.\n" + //$NON-NLS-1$
		" * @param b The second parameter.\n" + //$NON-NLS-1$
		" * @result The result of the foo operation, usually within 0 and 1000.\n" + //$NON-NLS-1$
		" */" + //$NON-NLS-1$
		" int foo(int a, int b);" + //$NON-NLS-1$
		"}"; //$NON-NLS-1$

	public CommentsTabPage(ModifyDialog modifyDialog, Map workingValues) {
		super(modifyDialog, workingValues);
		fJavaPreview.setPreviewText(fPreview);
	}

	public void updatePreview() {
	    super.updatePreview();
	}
	
	protected Composite doCreatePreferences(Composite parent) {
	    
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(createGridLayout(NUM_COLUMNS, false));
		
		// global group
		final Group globalGroup= createGroup(NUM_COLUMNS, composite, FormatterMessages.getString("CommentsTabPage.global_group.title")); //$NON-NLS-1$
		final CheckboxPreference global= createPref(globalGroup, FormatterMessages.getString("CommentsTabPage.global_group.enable_comment_formatting"), PreferenceConstants.FORMATTER_COMMENT_FORMAT); //$NON-NLS-1$
		
		// format group
		final Group formatGroup= createGroup(NUM_COLUMNS, composite, FormatterMessages.getString("CommentsTabPage.format_group.title")); //$NON-NLS-1$
		final CheckboxPreference header= createPref(formatGroup, FormatterMessages.getString("CommentsTabPage.format_group.format_header"), PreferenceConstants.FORMATTER_COMMENT_FORMATHEADER); //$NON-NLS-1$
		final CheckboxPreference html= createPref(formatGroup, FormatterMessages.getString("CommentsTabPage.format_group.format_html"), PreferenceConstants.FORMATTER_COMMENT_FORMATHTML); //$NON-NLS-1$
		final CheckboxPreference code= createPref(formatGroup, FormatterMessages.getString("CommentsTabPage.format_group.format_code_snippets"), PreferenceConstants.FORMATTER_COMMENT_FORMATSOURCE); //$NON-NLS-1$

		// blank lines group
		final Group blankLinesGroup= createGroup(NUM_COLUMNS, composite, FormatterMessages.getString("CommentsTabPage.blank_lines_group.title")); //$NON-NLS-1$
		final CheckboxPreference blankComments= createPref(blankLinesGroup, FormatterMessages.getString("CommentsTabPage.blank_lines_group.clear_blank_lines"), PreferenceConstants.FORMATTER_COMMENT_CLEARBLANKLINES); //$NON-NLS-1$
		final CheckboxPreference blankJavadoc= createPref(blankLinesGroup, FormatterMessages.getString("CommentsTabPage.blank_lines_group.blank_line_before_javadoc_tags"), PreferenceConstants.FORMATTER_COMMENT_SEPARATEROOTTAGS); //$NON-NLS-1$

		// indentation group
		final Group indentationGroup= createGroup(NUM_COLUMNS, composite, FormatterMessages.getString("CommentsTabPage.indentation_group.title")); //$NON-NLS-1$
		final CheckboxPreference indentJavadoc= createPref(indentationGroup, FormatterMessages.getString("CommentsTabPage.indentation_group.indent_javadoc_tags"), PreferenceConstants.FORMATTER_COMMENT_INDENTROOTTAGS); //$NON-NLS-1$
		
		Label l= new Label(indentationGroup, SWT.NONE);
		GridData gd= new GridData();
		gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(4);
		l.setLayoutData(gd);
		
		final CheckboxPreference indentDesc= createCheckboxPref(indentationGroup, NUM_COLUMNS - 1, FormatterMessages.getString("CommentsTabPage.indentation_group.indent_description_after_param"), PreferenceConstants.FORMATTER_COMMENT_INDENTPARAMETERDESCRIPTION, FALSE_TRUE); //$NON-NLS-1$

		// new lines group
		final Group newLinesGroup= createGroup(NUM_COLUMNS, composite, FormatterMessages.getString("CommentsTabPage.new_lines_group.title")); //$NON-NLS-1$
		final CheckboxPreference nlParam= createPref(newLinesGroup, FormatterMessages.getString("CommentsTabPage.new_lines_group.new_line_after_param_tags"), PreferenceConstants.FORMATTER_COMMENT_NEWLINEFORPARAMETER); //$NON-NLS-1$
		
		// line width group
		final Group lineWidthGroup= createGroup(NUM_COLUMNS, composite, FormatterMessages.getString("CommentsTabPage.line_width_group.title")); //$NON-NLS-1$
		final NumberPreference lineWidth= createNumberPref(lineWidthGroup, NUM_COLUMNS, FormatterMessages.getString("CommentsTabPage.line_width_group.line_width"), PreferenceConstants.FORMATTER_COMMENT_LINELENGTH, 0, Integer.MAX_VALUE); //$NON-NLS-1$

		Collection masters, slaves;

		masters= new ArrayList();
		masters.add(global);
		
		slaves= new ArrayList();
		slaves.add(formatGroup);
		slaves.add(blankLinesGroup);
		slaves.add(indentationGroup);
		slaves.add(newLinesGroup);
		slaves.add(lineWidthGroup);
		slaves.add(header);
		slaves.add(html);
		slaves.add(code);
		slaves.add(blankComments);
		slaves.add(blankJavadoc);
		slaves.add(indentJavadoc);
		slaves.add(nlParam);
		slaves.add(lineWidth);
		
		new Controller(masters, slaves);
		
		masters= new ArrayList();
		masters.add(global);
		masters.add(indentJavadoc);
		
		slaves= new ArrayList();
		slaves.add(indentDesc);
		
		new Controller(masters, slaves);
		
		return composite;
	}
	
	private CheckboxPreference createPref(Composite composite, String text, String key) {
	    return createCheckboxPref(composite, NUM_COLUMNS, text, key, FALSE_TRUE);
	}
}
