/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.DialogPageContextComputer;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.compiler.ConfigurableOption;
import org.eclipse.jdt.internal.formatter.CodeFormatter;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.util.TabFolderLayout;

public class CodeFormatterPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private final static int OPTION_CLEAR_ALL_NEWLINES= 3;
	private final static int OPTION_COMPACT_ASSIGNMENT= 7;
	private final static int OPTION_USETABS= 9;
	private final static int OPTION_TABSIZE= 10;

	private static ConfigurableOption[] fgCurrentOptions;
	
	private static final String PREVIEW_FILE= "CodeFormatterPreviewCode.txt"; //$NON-NLS-1$
	private static final String PREFERENCE_NAME= "CodeFormatterPreferencePage"; //$NON-NLS-1$
	private static final String WIDGET_DATA_KEY= "OPTION"; //$NON-NLS-1$

	private String fPreviewText;
	private Document fPreviewDocument;
	private Button[] fCheckOptions;
	private Text[] fTextOptions;
	private SourceViewer fPreviewViewer;
	private ConfigurableOption[] fNewOptions;


	public static ConfigurableOption[] getCurrentOptions() {
		return fgCurrentOptions;
	}
	
	/**
	 * Gets the currently configured tab size
	 */
	public static int getTabSize() {
		ConfigurableOption option= findOption(OPTION_TABSIZE, fgCurrentOptions);
		if (option != null) {
			return option.getCurrentValueIndex();
		}
		return 4;
	}

	/**
	 * Gets the current compating assignement configuration
	 */	
	public static boolean isCompactingAssignment() {
		ConfigurableOption option= findOption(OPTION_COMPACT_ASSIGNMENT, fgCurrentOptions);
		if (option != null) {
			return option.getCurrentValueIndex() == 0;
		}
		return false;
	}
		
	/**
	 * Gets the current is clearing all black lines configuration
	 */		
	public static boolean isClearingAllBlankLines() {
		ConfigurableOption option= findOption(OPTION_CLEAR_ALL_NEWLINES, fgCurrentOptions);
		if (option != null) {
			return option.getCurrentValueIndex() == 0;
		}
		return false;
	}
	
	private static ConfigurableOption findOption(int option, ConfigurableOption[] options) {
		for (int i= 0; i < options.length; i++) {
			if (options[i].getID() == option) {
				return options[i];
			}
		}
		return null;
	}	
	
	/**
	 * Initializes the current options (read from preference store)
	 */
	public static void initDefaults(IPreferenceStore store) {
		fgCurrentOptions= getDefaultOptions();
		for (int i= 0; i < fgCurrentOptions.length; i++) {
			String preferenceID= getPreferenceID(fgCurrentOptions[i].getID());
			if (store.contains(preferenceID))
				fgCurrentOptions[i].setValueIndex(store.getInt(preferenceID));

		}
	}

	private static ConfigurableOption[] getDefaultOptions() {
		return CodeFormatter.getDefaultOptions(Locale.getDefault());
	}

	public CodeFormatterPreferencePage() {
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		fNewOptions= getDefaultOptions();
		updateOptions(fNewOptions, fgCurrentOptions);
		fPreviewDocument= new Document();
		fPreviewText= loadPreviewFile(PREVIEW_FILE);
	}
	
	public void init(IWorkbench workbench) {
	}
		
	private static String getPreferenceID(int id) {
		return PREFERENCE_NAME + '.' + id;
	}	

	private void savePreferences(IPreferenceStore store) throws IOException {
		for (int i= 0; i < fgCurrentOptions.length; i++) {
			String preferenceID= getPreferenceID(fgCurrentOptions[i].getID());
			store.setValue(preferenceID, String.valueOf(fgCurrentOptions[i].getCurrentValueIndex()));
		}
	}	

	private String loadPreviewFile(String fn) {
		String separator= System.getProperty("line.separator"); //$NON-NLS-1$
		StringBuffer btxt= new StringBuffer(512);
		try {
			BufferedReader rin= new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(fn)));
			String line;
			while ((line= rin.readLine()) != null) {
				btxt.append(line);
				btxt.append(separator);
			}
		} catch (IOException io) {
			JavaPlugin.log(io);
		}
		return btxt.toString();
	}

	/**
	 * @see PreferencePage#createContents
	 */
	protected Control createContents(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 1;
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		composite.setLayout(layout);
		createCategoryFolders(composite);
		createPreview(composite);
		updateTabWidgetDependency();
		WorkbenchHelp.setHelp(parent, new DialogPageContextComputer(this, IJavaHelpContextIds.CODEFORMATTER_PREFERENCE_PAGE));
		return composite;
	}


	/**
	 * @see PreferencePage#performDefaults
	 */
	public void performDefaults() {
		super.performDefaults();
		updateOptions(fNewOptions, getDefaultOptions());
		for (int i= 0; i < fCheckOptions.length; i++) {
			ConfigurableOption option= retrieveOption(fCheckOptions[i]);
			int defaultValue= option.getCurrentValueIndex();
			fCheckOptions[i].setSelection(defaultValue == 0 ? true : false);
			option.setValueIndex(defaultValue);
		}
		for (int i= 0; i < fTextOptions.length; i++) {
			ConfigurableOption option= retrieveOption(fTextOptions[i]);
			int defaultValue= option.getCurrentValueIndex();
			fTextOptions[i].setText(String.valueOf(defaultValue));
			option.setValueIndex(defaultValue);
		}
		updateTabWidgetDependency();
		updatePreview(fNewOptions);
	}
	
	/**
	 * @see PreferencePage#performOk
	 */
	public boolean performOk() {
		updateOptions(getCurrentOptions(), fNewOptions);
		try {
			savePreferences(getPreferenceStore());
		} catch (IOException io) {
			JavaPlugin.log(io);
		}
		return true;
	}

	/**
	 * @see PreferencePage#performApply
	 */
	public void performApply() {
		performOk();
		updatePreview(fNewOptions);
	}

	private void createCategoryFolders(Composite parent) {
		List optionCategories= findCategories(fNewOptions);


		ModifyListener textListener= new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (fTextOptions == null || e.widget.isDisposed())
					return;
				Text source= (Text) e.widget;
				if (checkAllTextInputs(source)) {
					ConfigurableOption option= retrieveOption(source);
					option.setValueIndex(parseTextInput(source.getText()));
					updatePreview(fNewOptions);
				}
			}
		};
		
		SelectionListener checkboxListener= new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Button src= (Button) e.widget;
				if (src.isDisposed()) {
					return;
				}
				ConfigurableOption option= retrieveOption(src);
				int value= (src.getSelection() == true) ? 0 : 1; // See CodeFormatter logic
				option.setValueIndex(value);
				if (option.getID() == OPTION_USETABS) {
					updateTabWidgetDependency();
				}
				updatePreview(fNewOptions);
			}
		};	

		ArrayList checkOptions= new ArrayList();
		ArrayList textOptions= new ArrayList();

		TabFolder folder= new TabFolder(parent, SWT.NONE);
		folder.setLayout(new TabFolderLayout());	
		folder.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		for (int i= 0; i < optionCategories.size(); i++) {
			String category= (String) optionCategories.get(i);
			createSingleCategory(folder, category, textListener, checkboxListener, textOptions, checkOptions);
		}

		fCheckOptions= (Button[]) checkOptions.toArray(new Button[checkOptions.size()]);
		fTextOptions= (Text[]) textOptions.toArray(new Text[textOptions.size()]);

	}

	private void createSingleCategory(TabFolder folder, String category, ModifyListener textListener, SelectionListener checkboxListener, ArrayList textControls, ArrayList checkControls) {

		Composite composite= new Composite(folder, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		for (int i= 0; i < fNewOptions.length; i++) {
			ConfigurableOption opt= fNewOptions[i];
			if (category.equals(opt.getCategory())) {
				if (opt.getPossibleValues() == ConfigurableOption.NoDiscreteValue) {
					createTextOption(composite, opt, textListener, textControls);
				} else if (opt.getPossibleValues().length == 2) {
					createCheckOption(composite, opt, checkboxListener, checkControls);
				}	
			}
		}
		TabItem item= new TabItem(folder, SWT.NONE);
		item.setText(category);
		item.setControl(composite);
	}

	private void createCheckOption(Composite parent, ConfigurableOption option, SelectionListener listener, List checkControls) {
		Button check= new Button(parent, SWT.CHECK);
		check.setToolTipText(option.getDescription());
		check.addSelectionListener(listener);
		check.setData(WIDGET_DATA_KEY, option);
		check.setText(option.getName());
		check.setSelection(option.getCurrentValueIndex() == 0 ? true : false);
		check.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		checkControls.add(check);
	}

	private void createTextOption(Composite parent, ConfigurableOption option, ModifyListener listener, List textControls) {
		Composite pan= new Composite(parent, SWT.NONE);
		GridLayout gl= new GridLayout();
		gl.numColumns= 2; gl.marginWidth= 0; gl.marginHeight= 0;
		pan.setLayout(gl);
		pan.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Label flabel= new Label(pan, SWT.NONE);
		flabel.setText(option.getName());
		flabel.setToolTipText(option.getDescription());
		
		Text text= new Text(pan, SWT.BORDER | SWT.SINGLE);
		text.setTextLimit(3); // limit to 3 digits
		text.setToolTipText(option.getDescription());
		text.setData(WIDGET_DATA_KEY, option);
		text.addModifyListener(listener);
		text.setText(String.valueOf(option.getCurrentValueIndex()));
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.widthHint= convertWidthInCharsToPixels(5); 
		text.setLayoutData(gd);
		
		textControls.add(text);
	}

	private void createPreview(Composite parent) {
		SourceViewer previewViewer= new SourceViewer(parent, null, SWT.H_SCROLL | SWT.V_SCROLL);
		JavaTextTools tools= JavaPlugin.getDefault().getJavaTextTools();
		previewViewer.configure(new JavaSourceViewerConfiguration(tools, null));
		previewViewer.getTextWidget().setFont(JFaceResources.getFontRegistry().get(JFaceResources.TEXT_FONT));
		previewViewer.setEditable(false);
		previewViewer.setDocument(fPreviewDocument);
		Control control= previewViewer.getControl();
		control.setLayoutData(new GridData(GridData.FILL_BOTH));
		updatePreview(fNewOptions);
	}

	public void updatePreview(final ConfigurableOption[] options) {
		fPreviewDocument.set(CodeFormatter.format(fPreviewText, 0, options));
	}

	private ConfigurableOption retrieveOption(Widget widget) {
		return (ConfigurableOption) widget.getData(WIDGET_DATA_KEY);
	}

	private int parseTextInput(String input) throws NumberFormatException {
		if (input.equals("")) //$NON-NLS-1$
			throw new NumberFormatException();
		int val= Integer.parseInt(input);
		if (val < 0)
			throw new NumberFormatException();
		return val;
	}
	
	private boolean checkAllTextInputs(Text source) {
		boolean valueOK= false;
		String errorMessage= null;
		for (int i= 0; i < fTextOptions.length; i++) {
			int val= 0;
			Text next= fTextOptions[i];
			String text= next.getText();
			try {
				val= parseTextInput(text);
				if (next == source)
					valueOK= true;
			} catch (NumberFormatException nx) {
				if (text.length() == 0)
					errorMessage= JavaUIMessages.getString("CodeFormatterPreferencePage.empty_input"); //$NON-NLS-1$
				else
					errorMessage= JavaUIMessages.getFormattedString("CodeFormatterPreferencePage.invalid_input", text); //$NON-NLS-1$
			}
		}
		setErrorMessage(errorMessage);
		setValid(errorMessage == null);
		return valueOK;
	}	

	private void updateTabWidgetDependency() {
		ConfigurableOption option= findOption(OPTION_USETABS, fNewOptions);
		if (option != null) {
			boolean tabSizeEnabled= option.getCurrentValueIndex() != 0;
			for (int i= 0; i < fTextOptions.length; i++) {
				if (retrieveOption(fTextOptions[i]).getID() == OPTION_TABSIZE) {
					fTextOptions[i].setEnabled(tabSizeEnabled);
					break;
				}
			}
		}
	}

	private static List findCategories(ConfigurableOption[] options) {
		ArrayList optionCategories= new ArrayList(options.length);
		for (int i= 0; i < options.length; i++) {
			String category= options[i].getCategory();
			if (!optionCategories.contains(category)) {
				optionCategories.add(category);
			}
		}
		return optionCategories;
	}


	private static void updateOptions(ConfigurableOption[] current, ConfigurableOption[] update) {
		for (int i= 0; i < current.length; i++) {
			current[i].setValueIndex(update[i].getCurrentValueIndex());
		}
	}	

}


