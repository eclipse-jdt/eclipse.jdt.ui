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
package org.eclipse.jdt.internal.ui.preferences;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.formatter.IContentFormatterExtension2;
import org.eclipse.jface.text.formatter.IFormattingContext;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.text.IJavaPartitions;
import org.eclipse.jdt.internal.ui.text.comment.CommentFormattingContext;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.util.TabFolderLayout;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;

/*
 * The page for setting code formatter options
 */
public class CodeFormatterConfigurationBlock extends OptionsConfigurationBlock {

	// Preference store keys, see JavaCore.getOptions
	private static final String PREF_NEWLINE_OPENING_BRACES= JavaCore.FORMATTER_NEWLINE_OPENING_BRACE; 
	private static final String PREF_NEWLINE_CONTROL_STATEMENT= JavaCore.FORMATTER_NEWLINE_CONTROL;
	private static final String PREF_NEWLINE_CLEAR_ALL= JavaCore.FORMATTER_CLEAR_BLANK_LINES;
	private static final String PREF_NEWLINE_ELSE_IF= JavaCore.FORMATTER_NEWLINE_ELSE_IF;
	private static final String PREF_NEWLINE_EMPTY_BLOCK= JavaCore.FORMATTER_NEWLINE_EMPTY_BLOCK;
	private static final String PREF_CODE_SPLIT= JavaCore.FORMATTER_LINE_SPLIT;
	private static final String PREF_STYLE_COMPACT_ASSIGNEMENT= JavaCore.FORMATTER_COMPACT_ASSIGNMENT;
	private static final String PREF_TAB_CHAR= JavaCore.FORMATTER_TAB_CHAR;
	private static final String PREF_TAB_SIZE= JavaCore.FORMATTER_TAB_SIZE;
	private static final String PREF_SPACE_CASTEXPRESSION= JavaCore.FORMATTER_SPACE_CASTEXPRESSION;
	private static final String PREF_COMMENT_FORMATSOURCE= PreferenceConstants.FORMATTER_COMMENT_FORMATSOURCE;
	private static final String PREF_COMMENT_INDENTPARAMDESC= PreferenceConstants.FORMATTER_COMMENT_INDENTPARAMETERDESCRIPTION;
	private static final String PREF_COMMENT_FORMATHEADER= PreferenceConstants.FORMATTER_COMMENT_FORMATHEADER;
	private static final String PREF_COMMENT_INDENTROOTTAGS= PreferenceConstants.FORMATTER_COMMENT_INDENTROOTTAGS;
	private static final String PREF_COMMENT_FORMAT= PreferenceConstants.FORMATTER_COMMENT_FORMAT;
	private static final String PREF_COMMENT_NEWLINEPARAM= PreferenceConstants.FORMATTER_COMMENT_NEWLINEFORPARAMETER;
	private static final String PREF_COMMENT_SEPARATEROOTTAGS= PreferenceConstants.FORMATTER_COMMENT_SEPARATEROOTTAGS;
	private static final String PREF_COMMENT_CLEARBLANKLINES= PreferenceConstants.FORMATTER_COMMENT_CLEARBLANKLINES;
	private static final String PREF_COMMENT_LINELENGTH= PreferenceConstants.FORMATTER_COMMENT_LINELENGTH;

	// values
	private static final String INSERT=  JavaCore.INSERT;
	private static final String DO_NOT_INSERT= JavaCore.DO_NOT_INSERT;
	
	private static final String COMPACT= JavaCore.COMPACT;
	private static final String NORMAL= JavaCore.NORMAL;
	
	private static final String TAB= JavaCore.TAB;
	private static final String SPACE= JavaCore.SPACE;
	
	private static final String CLEAR_ALL= JavaCore.CLEAR_ALL;
	private static final String PRESERVE_ONE= JavaCore.PRESERVE_ONE;

	private IDocument fPreviewDocument;
	
	private Text fTabSizeTextBox;
	private String fPreviewText;
	private SourceViewer fSourceViewer;
	private SourceViewerConfiguration fViewerConfiguration;
	private JavaTextTools fTextTools;

	private PixelConverter fPixelConverter;
	
	private IStatus fCodeLengthStatus;
	private IStatus fCommentLengthStatus;
	private IStatus fTabSizeStatus;	
	
	public CodeFormatterConfigurationBlock(IStatusChangeListener context, IJavaProject project) {
		super(context, project);

		fTextTools= JavaPlugin.getDefault().getJavaTextTools();
		fViewerConfiguration= new JavaSourceViewerConfiguration(fTextTools, null, IJavaPartitions.JAVA_PARTITIONING);
		fPreviewText= loadPreviewFile("CodeFormatterPreviewCode.txt"); //$NON-NLS-1$
		fPreviewDocument= new Document(fPreviewText);
		fTextTools.setupJavaDocumentPartitioner(fPreviewDocument, IJavaPartitions.JAVA_PARTITIONING);

		fCodeLengthStatus= new StatusInfo();
		fCommentLengthStatus= new StatusInfo();
		fTabSizeStatus= new StatusInfo();
	}
	
	protected String[] getAllKeys() {
		return new String[] { PREF_NEWLINE_OPENING_BRACES, PREF_NEWLINE_CONTROL_STATEMENT, PREF_NEWLINE_CLEAR_ALL, PREF_NEWLINE_ELSE_IF, PREF_NEWLINE_EMPTY_BLOCK, PREF_CODE_SPLIT, PREF_STYLE_COMPACT_ASSIGNEMENT, PREF_TAB_CHAR, PREF_TAB_SIZE, PREF_SPACE_CASTEXPRESSION };
	}

	protected Control createContents(Composite parent) {
		fPixelConverter= new PixelConverter(parent);
		setShell(parent.getShell());
		
		int textWidth= fPixelConverter.convertWidthInCharsToPixels(6);
		
		int nColumns= 3;
		
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(layout);
		
		TabFolder folder= new TabFolder(composite, SWT.NONE);
		folder.setLayout(new TabFolderLayout());	
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		String[] insertNotInsert= new String[] { INSERT, DO_NOT_INSERT };
		
		layout= new GridLayout();
		layout.numColumns= nColumns;
		
		Composite newlineComposite= new Composite(folder, SWT.NULL);
		newlineComposite.setLayout(layout);

		String label= PreferencesMessages.getString("CodeFormatterPreferencePage.newline_opening_braces.label"); //$NON-NLS-1$
		addCheckBox(newlineComposite, label, PREF_NEWLINE_OPENING_BRACES, insertNotInsert, 0);	
		
		label= PreferencesMessages.getString("CodeFormatterPreferencePage.newline_control_statement.label"); //$NON-NLS-1$
		addCheckBox(newlineComposite, label, PREF_NEWLINE_CONTROL_STATEMENT, insertNotInsert, 0);	

		label= PreferencesMessages.getString("CodeFormatterPreferencePage.newline_clear_lines"); //$NON-NLS-1$
		addCheckBox(newlineComposite, label, PREF_NEWLINE_CLEAR_ALL, new String[] { CLEAR_ALL, PRESERVE_ONE }, 0);	

		label= PreferencesMessages.getString("CodeFormatterPreferencePage.newline_else_if.label"); //$NON-NLS-1$
		addCheckBox(newlineComposite, label, PREF_NEWLINE_ELSE_IF, insertNotInsert, 0);	

		label= PreferencesMessages.getString("CodeFormatterPreferencePage.newline_empty_block.label"); //$NON-NLS-1$
		addCheckBox(newlineComposite, label, PREF_NEWLINE_EMPTY_BLOCK, insertNotInsert, 0);	
		
		layout= new GridLayout();
		layout.numColumns= nColumns;	
		
		Composite lineSplittingComposite= new Composite(folder, SWT.NULL);
		lineSplittingComposite.setLayout(layout);

		label= PreferencesMessages.getString("CodeFormatterPreferencePage.split_code.label"); //$NON-NLS-1$
		addTextField(lineSplittingComposite, label, PREF_CODE_SPLIT, 0, textWidth);

		label= PreferencesMessages.getString("CodeFormatterPreferencePage.split_comment.label"); //$NON-NLS-1$
		addTextField(lineSplittingComposite, label, PREF_COMMENT_LINELENGTH, 0, textWidth);

		layout= new GridLayout();
		layout.numColumns= nColumns;	
		
		Composite styleComposite= new Composite(folder, SWT.NULL);
		styleComposite.setLayout(layout);
		
		label= PreferencesMessages.getString("CodeFormatterPreferencePage.style_compact_assignement.label"); //$NON-NLS-1$
		addCheckBox(styleComposite, label, PREF_STYLE_COMPACT_ASSIGNEMENT, new String[] { COMPACT, NORMAL }, 0);		

		label= PreferencesMessages.getString("CodeFormatterPreferencePage.style_space_castexpression.label"); //$NON-NLS-1$
		addCheckBox(styleComposite, label, PREF_SPACE_CASTEXPRESSION, insertNotInsert, 0);		

		label= PreferencesMessages.getString("CodeFormatterPreferencePage.tab_char.label"); //$NON-NLS-1$
		addCheckBox(styleComposite, label, PREF_TAB_CHAR, new String[] { TAB, SPACE }, 0);		

		label= PreferencesMessages.getString("CodeFormatterPreferencePage.tab_size.label"); //$NON-NLS-1$
		fTabSizeTextBox= addTextField(styleComposite, label, PREF_TAB_SIZE, 0, textWidth);		
		fTabSizeTextBox.setTextLimit(3);

		layout= new GridLayout();
		layout.numColumns= 1;

		Composite commentComposite= new Composite(folder, SWT.NULL);
		commentComposite.setLayout(layout);

		final String[] trueFalse= new String[] { IPreferenceStore.TRUE, IPreferenceStore.FALSE };

		label= PreferencesMessages.getString("CodeFormatterPreferencePage.comment_format.label"); //$NON-NLS-1$
		Button master= addCheckBox(commentComposite, label, PREF_COMMENT_FORMAT, trueFalse, 0);

		label= PreferencesMessages.getString("CodeFormatterPreferencePage.comment_formatheader.label"); //$NON-NLS-1$
		Button slave= addCheckBox(commentComposite, label, PREF_COMMENT_FORMATHEADER, trueFalse, 20);
		createSelectionDependency(master, slave);

		label= PreferencesMessages.getString("CodeFormatterPreferencePage.comment_formatsource.label"); //$NON-NLS-1$
		slave= addCheckBox(commentComposite, label, PREF_COMMENT_FORMATSOURCE, trueFalse, 20);
		createSelectionDependency(master, slave);

		label= PreferencesMessages.getString("CodeFormatterPreferencePage.newline_clear_lines"); //$NON-NLS-1$
		slave= addCheckBox(commentComposite, label, PREF_COMMENT_CLEARBLANKLINES, trueFalse, 20);
		createSelectionDependency(master, slave);

		label= PreferencesMessages.getString("CodeFormatterPreferencePage.comment_separateroottags.label"); //$NON-NLS-1$
		slave= addCheckBox(commentComposite, label, PREF_COMMENT_SEPARATEROOTTAGS, trueFalse, 20);
		createSelectionDependency(master, slave);

		label= PreferencesMessages.getString("CodeFormatterPreferencePage.comment_newlineparam.label"); //$NON-NLS-1$
		slave= addCheckBox(commentComposite, label, PREF_COMMENT_NEWLINEPARAM, trueFalse, 20);
		createSelectionDependency(master, slave);

		label= PreferencesMessages.getString("CodeFormatterPreferencePage.comment_indentroottags.label"); //$NON-NLS-1$
		Button indentRootTags= addCheckBox(commentComposite, label, PREF_COMMENT_INDENTROOTTAGS, trueFalse, 20);
		createSelectionDependency(master, indentRootTags);

		label= PreferencesMessages.getString("CodeFormatterPreferencePage.comment_indentparamdesc.label"); //$NON-NLS-1$
		slave= addCheckBox(commentComposite, label, PREF_COMMENT_INDENTPARAMDESC, trueFalse, 20);
		createEnableDependency(master, indentRootTags, slave);

		TabItem item= new TabItem(folder, SWT.NONE);
		item.setText(PreferencesMessages.getString("CodeFormatterPreferencePage.tab.newline.tabtitle")); //$NON-NLS-1$
		item.setControl(newlineComposite);

		item= new TabItem(folder, SWT.NONE);
		item.setText(PreferencesMessages.getString("CodeFormatterPreferencePage.tab.linesplit.tabtitle")); //$NON-NLS-1$
		item.setControl(lineSplittingComposite);
		
		item= new TabItem(folder, SWT.NONE);
		item.setText(PreferencesMessages.getString("CodeFormatterPreferencePage.tab.style.tabtitle")); //$NON-NLS-1$
		item.setControl(styleComposite);

		item= new TabItem(folder, SWT.NONE);
		item.setText(PreferencesMessages.getString("CodeFormatterPreferencePage.tab.comment.tabtitle")); //$NON-NLS-1$
		item.setControl(commentComposite);

		fSourceViewer= createPreview(parent);
			
		updatePreview();
					
		return composite;
	}

	private static void createEnableDependency(final Button chief, final Button master, final Control slave) {
		SelectionListener listener= new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				slave.setEnabled(master.getSelection() && chief.getSelection());
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};
		chief.addSelectionListener(listener);
		master.addSelectionListener(listener);
		slave.setEnabled(master.getSelection() && chief.getSelection());
	}

	private static void createSelectionDependency(final Button master, final Control slave) {
		master.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				slave.setEnabled(master.getSelection());
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		slave.setEnabled(master.getSelection());
	}

	private SourceViewer createPreview(Composite parent) {
		
		SourceViewer previewViewer= new JavaSourceViewer(parent, null, null, false, SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		previewViewer.configure(fViewerConfiguration);
		previewViewer.getTextWidget().setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));
		previewViewer.getTextWidget().setTabs(getPositiveIntValue((String) fWorkingValues.get(PREF_TAB_SIZE), 0));
		previewViewer.setDocument(fPreviewDocument);
		
		Control control= previewViewer.getControl();
		GridData gdata= new GridData(GridData.FILL_BOTH);
		gdata.widthHint= fPixelConverter.convertWidthInCharsToPixels(30);
		gdata.heightHint= fPixelConverter.convertHeightInCharsToPixels(12);
		control.setLayoutData(gdata);

		return previewViewer;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#validateSettings(java.lang.String, java.lang.String)
	 */
	protected void validateSettings(String changedKey, String newValue) {

		if (changedKey == null || PREF_CODE_SPLIT.equals(changedKey)) {
			String lineNumber= (String)fWorkingValues.get(PREF_CODE_SPLIT);
			fCodeLengthStatus= validatePositiveNumber(lineNumber, 4);
		}

		if (changedKey == null || PREF_COMMENT_LINELENGTH.equals(changedKey)) {
			String lineNumber= (String)fWorkingValues.get(PREF_COMMENT_LINELENGTH);
			fCommentLengthStatus= validatePositiveNumber(lineNumber, 4);
		}

		if (changedKey == null || PREF_TAB_SIZE.equals(changedKey)) {
			String tabSize= (String)fWorkingValues.get(PREF_TAB_SIZE);
			fTabSizeStatus= validatePositiveNumber(tabSize, 0);
			int oldTabSize= fSourceViewer.getTextWidget().getTabs();
			if (fTabSizeStatus.matches(IStatus.ERROR)) {
				fWorkingValues.put(PREF_TAB_SIZE, String.valueOf(oldTabSize)); // set back
			} else {
				fSourceViewer.getTextWidget().setTabs(getPositiveIntValue(tabSize, 0));
			}
		}

		final IStatus status= StatusUtil.getMostSevere(new IStatus[] { fCodeLengthStatus, fCommentLengthStatus, fTabSizeStatus });
		fContext.statusChanged(status);

		if (!status.matches(IStatus.ERROR))
			updatePreview();
	}

	private String loadPreviewFile(String filename) {
		String separator= System.getProperty("line.separator"); //$NON-NLS-1$
		StringBuffer btxt= new StringBuffer(512);
		BufferedReader rin= null;
		try {
			rin= new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(filename)));
			String line;
			while ((line= rin.readLine()) != null) {
				if (btxt.length() > 0) {
					btxt.append(separator);
				}
				btxt.append(line);
			}
		} catch (IOException io) {
			JavaPlugin.log(io);
		} finally {
			if (rin != null) {
				try { rin.close(); } catch (IOException e) {}
			}
		}
		return btxt.toString();
	}

	private void updatePreview() {

		fSourceViewer.setRedraw(false);
		fPreviewDocument.set(fPreviewText);

		final IFormattingContext context= new CommentFormattingContext();
		try {

			final IContentFormatter formatter= fViewerConfiguration.getContentFormatter(fSourceViewer);
			if (formatter instanceof IContentFormatterExtension2) {

				final IContentFormatterExtension2 extension= (IContentFormatterExtension2)formatter;

				context.setProperty(FormattingContextProperties.CONTEXT_PREFERENCES, fWorkingValues);
				context.setProperty(FormattingContextProperties.CONTEXT_DOCUMENT, Boolean.valueOf(true));

				extension.format(fPreviewDocument, context);

			} else
				formatter.format(fPreviewDocument, new Region(0, fPreviewDocument.getLength()));

		} finally {

			fSourceViewer.setSelectedRange(0, 0);
			context.dispose();

			fSourceViewer.setRedraw(true);
		}
	}

	private IStatus validatePositiveNumber(String number, int threshold) {
		StatusInfo status= new StatusInfo();
		if (number.length() == 0) {
			status.setError(PreferencesMessages.getString("CodeFormatterPreferencePage.empty_input")); //$NON-NLS-1$
		} else {
			try {
				int value= Integer.parseInt(number);
				if (value < threshold) {
					status.setError(PreferencesMessages.getFormattedString("CodeFormatterPreferencePage.invalid_input", number)); //$NON-NLS-1$
				}
			} catch (NumberFormatException e) {
				status.setError(PreferencesMessages.getFormattedString("CodeFormatterPreferencePage.invalid_input", number)); //$NON-NLS-1$
			}
		}
		return status;
	}
	
	private static int getPositiveIntValue(String string, int dflt) {
		try {
			int i= Integer.parseInt(string);
			if (i >= 0) {
				return i;
			}
		} catch (NumberFormatException e) {
		}
		return dflt;
	}		

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#getFullBuildDialogStrings(boolean)
	 */
	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		return null; // no build required
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#getDefaultOptions()
	 */
	protected Map getDefaultOptions() {

		final Map map= super.getDefaultOptions();

		final IFormattingContext context= new CommentFormattingContext();
		context.storeToMap(PreferenceConstants.getPreferenceStore(), map, true);

		return map;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#getOptions(boolean)
	 */
	protected Map getOptions(boolean inheritJavaCoreOptions) {

		final Map map= super.getOptions(inheritJavaCoreOptions);

		final IFormattingContext context= new CommentFormattingContext();
		context.storeToMap(PreferenceConstants.getPreferenceStore(), map, false);

		return map;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#performOk(boolean)
	 */
	public boolean performOk(boolean enabled) {

		if (super.performOk(enabled)) {

			final IFormattingContext context= new CommentFormattingContext();
			context.mapToStore(fWorkingValues, PreferenceConstants.getPreferenceStore());

			return true;
		}
		return false;
	}
}


