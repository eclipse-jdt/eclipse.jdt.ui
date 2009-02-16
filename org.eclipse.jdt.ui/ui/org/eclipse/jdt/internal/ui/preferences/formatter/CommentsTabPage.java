/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;


/**
 * Tab page for the comment formatter settings.
 */
public class CommentsTabPage extends FormatterTabPage {

	/**
	 * Constant array for boolean selection
	 */
	private static String[] FALSE_TRUE = {
		DefaultCodeFormatterConstants.FALSE,
		DefaultCodeFormatterConstants.TRUE
	};
	
	/**
	 * Constant array for boolean selection.
	 * @since 3.5
	 */
	private static String[] TRUE_FALSE = {
		DefaultCodeFormatterConstants.TRUE,
		DefaultCodeFormatterConstants.FALSE
	};

    /**
     * Constant array for insert / not_insert.
     */
    private static String[] DO_NOT_INSERT_INSERT = {
        JavaCore.DO_NOT_INSERT,
        JavaCore.INSERT
    };

	private static abstract class Controller implements Observer {

		private final Collection fMasters;
		private final Collection fSlaves;

		public Controller(Collection masters, Collection slaves) {
			fMasters= masters;
			fSlaves= slaves;
			for (final Iterator iter= fMasters.iterator(); iter.hasNext();) {
				((CheckboxPreference)iter.next()).addObserver(this);
			}
		}

		public void update(Observable o, Object arg) {
			boolean enabled= areSlavesEnabled();

			for (final Iterator iter= fSlaves.iterator(); iter.hasNext();) {
				final Object obj= iter.next();
				if (obj instanceof Preference) {
					((Preference)obj).setEnabled(enabled);
				} else if (obj instanceof Control) {
					((Group)obj).setEnabled(enabled);
				}
			}
		}

		public Collection getMasters() {
			return fMasters;
		}

		protected abstract boolean areSlavesEnabled();
	}

	private final static class OrController extends Controller {

		public OrController(Collection masters, Collection slaves) {
			super(masters, slaves);
			update(null, null);
		}

		/**
		 * {@inheritDoc}
		 */
		protected boolean areSlavesEnabled() {
			for (final Iterator iter= getMasters().iterator(); iter.hasNext();) {
				if (((CheckboxPreference)iter.next()).getChecked())
					return true;
			}
			return false;
		}
	}

	private final String PREVIEW=
		createPreviewHeader("An example for comment formatting. This example is meant to illustrate the various possibilities offered by <i>Eclipse</i> in order to format comments.") +	//$NON-NLS-1$
		"package mypackage;\n" + //$NON-NLS-1$
		"/**\n" + //$NON-NLS-1$
		" * This is the comment for the example interface.\n" + //$NON-NLS-1$
		" */\n" + //$NON-NLS-1$
		" interface Example {\n" + //$NON-NLS-1$
		"// This is a long comment that should be split in multiple line comments in case the line comment formatting is enabled\n" + //$NON-NLS-1$
		"int foo3();\n" + //$NON-NLS-1$
		"/*\n" + //$NON-NLS-1$
		"*\n" + //$NON-NLS-1$
		"* These possibilities include:\n" + //$NON-NLS-1$
		"* <ul><li>Formatting of header comments.</li><li>Formatting of Javadoc tags</li></ul>\n" + //$NON-NLS-1$
		"*/\n" + //$NON-NLS-1$
		"int foo4();\n" + //$NON-NLS-1$
		" /**\n" + //$NON-NLS-1$
		" *\n" + //$NON-NLS-1$
		" * These possibilities include:\n" + //$NON-NLS-1$
		" * <ul><li>Formatting of header comments.</li><li>Formatting of Javadoc tags</li></ul>\n" + //$NON-NLS-1$
		" */\n" + //$NON-NLS-1$
		" int bar();\n" + //$NON-NLS-1$
		" /*\n" + //$NON-NLS-1$
		" *\n" + //$NON-NLS-1$
		" * These possibilities include:\n" + //$NON-NLS-1$
		" * <ul><li>Formatting of header comments.</li><li>Formatting of Javadoc tags</li></ul>\n" + //$NON-NLS-1$
		" */\n" + //$NON-NLS-1$
		" int bar2();" + //$NON-NLS-1$
		" // This is a long comment that should be split in multiple line comments in case the line comment formatting is enabled\n" + //$NON-NLS-1$
		" int foo2();" + //$NON-NLS-1$
		" /**\n" + //$NON-NLS-1$
		" * The following is some sample code which illustrates source formatting within javadoc comments:\n" + //$NON-NLS-1$
		" * <pre>public class Example {final int a= 1;final boolean b= true;}</pre>\n" + //$NON-NLS-1$
		" * Descriptions of parameters and return values are best appended at end of the javadoc comment.\n" + //$NON-NLS-1$
		" * @param a The first parameter. For an optimum result, this should be an odd number\n" + //$NON-NLS-1$
		" * between 0 and 100.\n" + //$NON-NLS-1$
		" * @param b The second parameter.\n" + //$NON-NLS-1$
		" * @return The result of the foo operation, usually within 0 and 1000.\n" + //$NON-NLS-1$
		" */" + //$NON-NLS-1$
		" int foo(int a, int b);\n" + //$NON-NLS-1$
		"}"; //$NON-NLS-1$

	private CompilationUnitPreview fPreview;

	public CommentsTabPage(ModifyDialog modifyDialog, Map workingValues) {
		super(modifyDialog, workingValues);
	}

	protected void doCreatePreferences(Composite composite, int numColumns) {

		// global group
		final Group globalGroup= createGroup(numColumns, composite, FormatterMessages.CommentsTabPage_group1_title);
		final CheckboxPreference javadoc= createPrefFalseTrue(globalGroup, numColumns, FormatterMessages.commentsTabPage_enable_javadoc_comment_formatting, DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT, false);
		final CheckboxPreference blockComment= createPrefFalseTrue(globalGroup, numColumns, FormatterMessages.CommentsTabPage_enable_block_comment_formatting, DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT, false);
		final CheckboxPreference singleLineComments= createPrefFalseTrue(globalGroup, numColumns, FormatterMessages.CommentsTabPage_enable_line_comment_formatting, DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_LINE_COMMENT, false);
		final CheckboxPreference header= createPrefFalseTrue(globalGroup, numColumns, FormatterMessages.CommentsTabPage_format_header, DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_HEADER, false);
		createPrefFalseTrue(globalGroup, numColumns, FormatterMessages.CommentsTabPage_never_indent_block_comments_on_first_column, DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_BLOCK_COMMENTS_ON_FIRST_COLUMN, false);
		createPrefFalseTrue(globalGroup, numColumns, FormatterMessages.CommentsTabPage_never_indent_line_comments_on_first_column, DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_LINE_COMMENTS_ON_FIRST_COLUMN, false);
		createPrefFalseTrue(globalGroup, numColumns, FormatterMessages.CommentsTabPage_do_not_join_lines, DefaultCodeFormatterConstants.FORMATTER_JOIN_LINES_IN_COMMENTS, true);

		// javadoc comment formatting settings
		final Group settingsGroup= createGroup(numColumns, composite, FormatterMessages.CommentsTabPage_group2_title);
		final CheckboxPreference html= createPrefFalseTrue(settingsGroup, numColumns, FormatterMessages.CommentsTabPage_format_html, DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_HTML, false);
		final CheckboxPreference code= createPrefFalseTrue(settingsGroup, numColumns, FormatterMessages.CommentsTabPage_format_code_snippets, DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_SOURCE, false);
		final CheckboxPreference blankJavadoc= createPrefInsert(settingsGroup, numColumns, FormatterMessages.CommentsTabPage_blank_line_before_javadoc_tags, DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BEFORE_ROOT_TAGS);
		final CheckboxPreference indentJavadoc= createPrefFalseTrue(settingsGroup, numColumns, FormatterMessages.CommentsTabPage_indent_javadoc_tags, DefaultCodeFormatterConstants.FORMATTER_COMMENT_INDENT_ROOT_TAGS, false);
		final CheckboxPreference indentDesc= createPrefFalseTrue(settingsGroup, numColumns , FormatterMessages.CommentsTabPage_indent_description_after_param, DefaultCodeFormatterConstants.FORMATTER_COMMENT_INDENT_PARAMETER_DESCRIPTION, false);
		((GridData)indentDesc.getControl().getLayoutData()).horizontalIndent= fPixelConverter.convertWidthInCharsToPixels(4);
		final CheckboxPreference nlParam= createPrefInsert(settingsGroup, numColumns, FormatterMessages.CommentsTabPage_new_line_after_param_tags, DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_NEW_LINE_FOR_PARAMETER);
		final CheckboxPreference blankLinesJavadoc= createPrefFalseTrue(settingsGroup, numColumns, FormatterMessages.CommentsTabPage_clear_blank_lines, DefaultCodeFormatterConstants.FORMATTER_COMMENT_CLEAR_BLANK_LINES_IN_JAVADOC_COMMENT, false);

		// block comment settings
		final Group blockSettingsGroup= createGroup(numColumns, composite, FormatterMessages.CommentsTabPage_group4_title);
		final CheckboxPreference blankLinesBlock= createPrefFalseTrue(blockSettingsGroup, numColumns, FormatterMessages.CommentsTabPage_remove_blank_block_comment_lines, DefaultCodeFormatterConstants.FORMATTER_COMMENT_CLEAR_BLANK_LINES_IN_BLOCK_COMMENT, false);

		final Group widthGroup= createGroup(numColumns, composite, FormatterMessages.CommentsTabPage_group3_title);
		final NumberPreference lineWidth= createNumberPref(widthGroup, numColumns, FormatterMessages.CommentsTabPage_line_width, DefaultCodeFormatterConstants.FORMATTER_COMMENT_LINE_LENGTH, 0, 9999);

		ArrayList javaDocMaster= new ArrayList();
		javaDocMaster.add(javadoc);
		javaDocMaster.add(header);

		ArrayList javaDocSlaves= new ArrayList();
		javaDocSlaves.add(settingsGroup);
		javaDocSlaves.add(html);
		javaDocSlaves.add(code);
		javaDocSlaves.add(blankJavadoc);
		javaDocSlaves.add(indentJavadoc);
		javaDocSlaves.add(nlParam);
		javaDocSlaves.add(blankLinesJavadoc);

		new OrController(javaDocMaster, javaDocSlaves);

		ArrayList indentMasters= new ArrayList();
		indentMasters.add(javadoc);
		indentMasters.add(header);
		indentMasters.add(indentJavadoc);

		ArrayList indentSlaves= new ArrayList();
		indentSlaves.add(indentDesc);

		new Controller(indentMasters, indentSlaves) {
			protected boolean areSlavesEnabled() {
				return (javadoc.getChecked() || header.getChecked()) && indentJavadoc.getChecked();
            }
		}.update(null, null);

		ArrayList blockMasters= new ArrayList();
		blockMasters.add(blockComment);
		blockMasters.add(header);

		ArrayList blockSlaves= new ArrayList();
		blockSlaves.add(blockSettingsGroup);
		blockSlaves.add(blankLinesBlock);

		new OrController(blockMasters, blockSlaves);

		ArrayList lineWidthMasters= new ArrayList();
		lineWidthMasters.add(javadoc);
		lineWidthMasters.add(blockComment);
		lineWidthMasters.add(singleLineComments);
		lineWidthMasters.add(header);

		ArrayList lineWidthSlaves= new ArrayList();
		lineWidthSlaves.add(widthGroup);
		lineWidthSlaves.add(lineWidth);

		new OrController(lineWidthMasters, lineWidthSlaves);
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
    	super.doUpdatePreview();
        fPreview.update();
    }

	private CheckboxPreference createPrefFalseTrue(Composite composite, int numColumns, String text, String key, boolean invertPreference) {
		if (invertPreference)
			return createCheckboxPref(composite, numColumns, text, key, TRUE_FALSE);
		return createCheckboxPref(composite, numColumns, text, key, FALSE_TRUE);
	}

    private CheckboxPreference createPrefInsert(Composite composite, int numColumns, String text, String key) {
        return createCheckboxPref(composite, numColumns, text, key, DO_NOT_INSERT_INSERT);
    }
}
