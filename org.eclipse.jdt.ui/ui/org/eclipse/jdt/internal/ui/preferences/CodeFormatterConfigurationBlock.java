/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
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
	private static final String PREF_LINE_SPLIT= JavaCore.FORMATTER_LINE_SPLIT;
	private static final String PREF_STYLE_COMPACT_ASSIGNEMENT= JavaCore.FORMATTER_COMPACT_ASSIGNMENT;
	private static final String PREF_TAB_CHAR= JavaCore.FORMATTER_TAB_CHAR;
	private static final String PREF_TAB_SIZE= JavaCore.FORMATTER_TAB_SIZE;
	private static final String PREF_SPACE_CASTEXPRESSION= "org.eclipse.jdt.core.formatter.space.castexpression"; //TODO

	// values
	private static final String INSERT=  JavaCore.INSERT;
	private static final String DO_NOT_INSERT= JavaCore.DO_NOT_INSERT;
	
	private static final String COMPACT= JavaCore.COMPACT;
	private static final String NORMAL= JavaCore.NORMAL;
	
	private static final String TAB= JavaCore.TAB;
	private static final String SPACE= JavaCore.SPACE;
	
	private static final String CLEAR_ALL= JavaCore.CLEAR_ALL;
	private static final String PRESERVE_ONE= JavaCore.PRESERVE_ONE;
			
	private String fPreviewText;
	private IDocument fPreviewDocument;
	
	private Text fTabSizeTextBox;
	private SourceViewer fSourceViewer;
	
	private PixelConverter fPixelConverter;
	
	private IStatus fLineLengthStatus;
	private IStatus fTabSizeStatus;	
	
	public CodeFormatterConfigurationBlock(IStatusChangeListener context, IJavaProject project) {
		super(context, project);
			
		fPreviewDocument= new Document();
		fPreviewText= loadPreviewFile("CodeFormatterPreviewCode.txt");	//$NON-NLS-1$	

		fLineLengthStatus= new StatusInfo();
		fTabSizeStatus= new StatusInfo();
	}
	
	protected String[] getAllKeys() {
		return new String[] {
			PREF_NEWLINE_OPENING_BRACES, PREF_NEWLINE_CONTROL_STATEMENT, PREF_NEWLINE_CLEAR_ALL,
			PREF_NEWLINE_ELSE_IF, PREF_NEWLINE_EMPTY_BLOCK, PREF_LINE_SPLIT,
			PREF_STYLE_COMPACT_ASSIGNEMENT, PREF_TAB_CHAR, PREF_TAB_SIZE, PREF_SPACE_CASTEXPRESSION
		};	
	}	

	protected Control createContents(Composite parent) {
		fPixelConverter= new PixelConverter(parent);
		
		int textWidth= fPixelConverter.convertWidthInCharsToPixels(6);
		
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
		layout.numColumns= 2;
		
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
		layout.numColumns= 2;	
		
		Composite lineSplittingComposite= new Composite(folder, SWT.NULL);
		lineSplittingComposite.setLayout(layout);

		label= PreferencesMessages.getString("CodeFormatterPreferencePage.split_line.label"); //$NON-NLS-1$
		addTextField(lineSplittingComposite, label, PREF_LINE_SPLIT, 0, textWidth);

		layout= new GridLayout();
		layout.numColumns= 2;	
		
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

		TabItem item= new TabItem(folder, SWT.NONE);
		item.setText(PreferencesMessages.getString("CodeFormatterPreferencePage.tab.newline.tabtitle")); //$NON-NLS-1$
		item.setControl(newlineComposite);

		item= new TabItem(folder, SWT.NONE);
		item.setText(PreferencesMessages.getString("CodeFormatterPreferencePage.tab.linesplit.tabtitle")); //$NON-NLS-1$
		item.setControl(lineSplittingComposite);
		
		item= new TabItem(folder, SWT.NONE);
		item.setText(PreferencesMessages.getString("CodeFormatterPreferencePage.tab.style.tabtitle")); //$NON-NLS-1$
		item.setControl(styleComposite);		
		
		fSourceViewer= createPreview(parent);
			
		updatePreview();
					
		return composite;
	}
	
	private SourceViewer createPreview(Composite parent) {
		SourceViewer previewViewer= new SourceViewer(parent, null, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		JavaTextTools tools= JavaPlugin.getDefault().getJavaTextTools();
		previewViewer.configure(new JavaSourceViewerConfiguration(tools, null));
		previewViewer.getTextWidget().setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));
		previewViewer.getTextWidget().setTabs(getPositiveIntValue((String) fWorkingValues.get(PREF_TAB_SIZE), 0));
		previewViewer.setEditable(false);
		previewViewer.setDocument(fPreviewDocument);
		Control control= previewViewer.getControl();
		GridData gdata= new GridData(GridData.FILL_BOTH);
		gdata.widthHint= fPixelConverter.convertWidthInCharsToPixels(30);
		gdata.heightHint= fPixelConverter.convertHeightInCharsToPixels(12);
		control.setLayoutData(gdata);
		return previewViewer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#validateSettings(java.lang.String, java.lang.String)
	 */
	protected void validateSettings(String changedKey, String newValue) {
		if (changedKey == null || PREF_LINE_SPLIT.equals(changedKey)) {
			fLineLengthStatus= validatePositiveNumber(newValue);
		}
		if (changedKey == null || PREF_TAB_SIZE.equals(changedKey)) {
			fTabSizeStatus= validatePositiveNumber(newValue);
			int oldTabSize= fSourceViewer.getTextWidget().getTabs();
			if (fTabSizeStatus.matches(IStatus.ERROR)) {
				fWorkingValues.put(PREF_TAB_SIZE, String.valueOf(oldTabSize)); // set back
			} else {
				fSourceViewer.getTextWidget().setTabs(getPositiveIntValue(newValue, 0));
			}
		}
		updatePreview();
		fContext.statusChanged(StatusUtil.getMoreSevere(fLineLengthStatus, fTabSizeStatus));
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
		ICodeFormatter formatter= ToolFactory.createDefaultCodeFormatter(fWorkingValues);
		fPreviewDocument.set(formatter.format(fPreviewText, 0, null, "\n")); //$NON-NLS-1$
	}	
		
	private IStatus validatePositiveNumber(String number) {
		StatusInfo status= new StatusInfo();
		if (number.length() == 0) {
			status.setError(PreferencesMessages.getString("CodeFormatterPreferencePage.empty_input")); //$NON-NLS-1$
		} else {
			try {
				int value= Integer.parseInt(number);
				if (value < 0) {
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

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#getFullBuildDialogStrings(boolean)
	 */
	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		return null; // no build required
	}
		

}


