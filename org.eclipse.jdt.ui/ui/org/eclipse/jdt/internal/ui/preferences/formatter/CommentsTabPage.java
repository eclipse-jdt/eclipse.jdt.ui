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

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

import org.eclipse.jdt.ui.PreferenceConstants;

/**
 * Tab page for the comment formatter settings.
 */
public class CommentsTabPage extends ModifyDialogTabPage {
	
	
	private final static class Controller implements Observer {
		
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
			    final Object obj= iter.next();
			    if (obj instanceof Preference) {
			        ((Preference)obj).setEnabled(enabled);
			    } else if (obj instanceof Control) {
			        ((Group)obj).setEnabled(enabled);
			    }
			}
		}
	}
	
	
	private final static String PREVIEW=
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
	
	private CompilationUnitPreview fPreview;

	public CommentsTabPage(ModifyDialog modifyDialog, Map workingValues) {
		super(modifyDialog, workingValues);
	}

	protected void doCreatePreferences(Composite composite, int numColumns) {
	    
		// global group
		final Group globalGroup= createGroup(numColumns, composite, FormatterMessages.getString("CommentsTabPage.group1.title")); //$NON-NLS-1$
		final CheckboxPreference global= createPref(globalGroup, numColumns, FormatterMessages.getString("CommentsTabPage.enable_comment_formatting"), PreferenceConstants.FORMATTER_COMMENT_FORMAT); //$NON-NLS-1$
		final CheckboxPreference header= createPref(globalGroup, numColumns, FormatterMessages.getString("CommentsTabPage.format_header"), PreferenceConstants.FORMATTER_COMMENT_FORMATHEADER); //$NON-NLS-1$
		final CheckboxPreference html= createPref(globalGroup, numColumns, FormatterMessages.getString("CommentsTabPage.format_html"), PreferenceConstants.FORMATTER_COMMENT_FORMATHTML); //$NON-NLS-1$
		final CheckboxPreference code= createPref(globalGroup, numColumns, FormatterMessages.getString("CommentsTabPage.format_code_snippets"), PreferenceConstants.FORMATTER_COMMENT_FORMATSOURCE); //$NON-NLS-1$

		// blank lines group
		final Group settingsGroup= createGroup(numColumns, composite, FormatterMessages.getString("CommentsTabPage.group2.title")); //$NON-NLS-1$
		final CheckboxPreference blankComments= createPref(settingsGroup, numColumns, FormatterMessages.getString("CommentsTabPage.clear_blank_lines"), PreferenceConstants.FORMATTER_COMMENT_CLEARBLANKLINES); //$NON-NLS-1$
		final CheckboxPreference blankJavadoc= createPref(settingsGroup, numColumns, FormatterMessages.getString("CommentsTabPage.blank_line_before_javadoc_tags"), PreferenceConstants.FORMATTER_COMMENT_SEPARATEROOTTAGS); //$NON-NLS-1$
		final CheckboxPreference indentJavadoc= createPref(settingsGroup, numColumns, FormatterMessages.getString("CommentsTabPage.indent_javadoc_tags"), PreferenceConstants.FORMATTER_COMMENT_INDENTROOTTAGS); //$NON-NLS-1$
		
		final CheckboxPreference indentDesc= createCheckboxPref(settingsGroup, numColumns , FormatterMessages.getString("CommentsTabPage.indent_description_after_param"), PreferenceConstants.FORMATTER_COMMENT_INDENTPARAMETERDESCRIPTION, FALSE_TRUE); //$NON-NLS-1$
		((GridData)indentDesc.getControl().getLayoutData()).horizontalIndent= fPixelConverter.convertWidthInCharsToPixels(4);
		final CheckboxPreference nlParam= createPref(settingsGroup, numColumns, FormatterMessages.getString("CommentsTabPage.new_line_after_param_tags"), PreferenceConstants.FORMATTER_COMMENT_NEWLINEFORPARAMETER); //$NON-NLS-1$
		
		final Group widthGroup= createGroup(numColumns, composite, FormatterMessages.getString("CommentsTabPage.group3.title")); //$NON-NLS-1$
		final NumberPreference lineWidth= createNumberPref(widthGroup, numColumns, FormatterMessages.getString("CommentsTabPage.line_width"), PreferenceConstants.FORMATTER_COMMENT_LINELENGTH, 0, 9999); //$NON-NLS-1$

		Collection masters, slaves;

		masters= new ArrayList();
		masters.add(global);
		
		slaves= new ArrayList();
		slaves.add(settingsGroup);
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
	}
	
	protected void initializePage() {
		fPreview.setPreviewText(PREVIEW);
	}
	
    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.ui.preferences.formatter.ModifyDialogTabPage#doCreateJavaPreview(org.eclipse.swt.widgets.Composite)
     */
    protected JavaPreview doCreateJavaPreview(Composite parent) {
        fPreview= new CompilationUnitPreview(fWorkingValues, parent);
        return fPreview;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.ui.preferences.formatter.ModifyDialogTabPage#doUpdatePreview()
     */
    protected void doUpdatePreview() {
        fPreview.update();
    }
	
	private CheckboxPreference createPref(Composite composite, int numColumns, String text, String key) {
		return createCheckboxPref(composite, numColumns, text, key, FALSE_TRUE);
	}
}
