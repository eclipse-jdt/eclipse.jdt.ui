/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.preferences;

import java.io.BufferedReader;import java.io.IOException;import java.io.InputStreamReader;import java.util.ArrayList;import java.util.Enumeration;import java.util.Locale;import java.util.Hashtable;
import org.eclipse.jdt.internal.compiler.ConfigurableOption;import org.eclipse.jdt.internal.formatter.CodeFormatter;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;import org.eclipse.jdt.ui.text.JavaTextTools;import org.eclipse.jface.preference.IPreferenceStore;import org.eclipse.jface.preference.PreferencePage;import org.eclipse.jface.resource.JFaceResources;import org.eclipse.jface.text.Document;import org.eclipse.jface.text.source.SourceViewer;import org.eclipse.swt.SWT;import org.eclipse.swt.events.ModifyEvent;import org.eclipse.swt.events.ModifyListener;import org.eclipse.swt.events.SelectionAdapter;import org.eclipse.swt.events.SelectionEvent;import org.eclipse.swt.events.SelectionListener;import org.eclipse.swt.layout.GridData;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.layout.RowLayout;import org.eclipse.swt.widgets.Button;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Display;import org.eclipse.swt.widgets.Group;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Shell;import org.eclipse.swt.widgets.Text;import org.eclipse.swt.widgets.Widget;import org.eclipse.ui.IWorkbench;import org.eclipse.ui.IWorkbenchPreferencePage;


public class CodeFormatterPreferencePage extends PreferencePage implements IWorkbenchPreferencePage{

	private String fPreviewText;
	private Document fPreviewDocument;
	private Composite fMainPanel;
	private Button[] fCheckOptions;
	private Text[] fTextOptions;
	private Button fApplyButton;
	private Button fDefaultButton;
	private SourceViewer fPreviewViewer;
	private boolean fEraseStatusMessage;
	private String fErrorMessage;
	private ConfigurableOption[] fNewOptions;
	private static ConfigurableOption[] fgCurrentOptions;
	private static IPreferenceStore fgPreferenceStore;
	
	private static final int INDENTATION_LEVEL = 0;
	private static final int TEXT_LIMIT=3;
	private static final int TAB_CHECK_OPTION_ID=9;
	private static final int TAB_TEXT_OPTION_ID=10;
	private static final String PREVIEW_FILE= "CodeFormatterPreviewCode.txt";
	private static final String ERROR_MESSAGE_EMPTY="Empty Input";
	private static final String ERROR_MESSAGE_INVALID=" is not a valid Input";
	private static final String PREFERENCE_NAME="CodeFormatterPreferencePage";
	private static final String WIDGET_DATA_KEY="OPTION";
	
	private ModifyListener   fTextListener = new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				if (fTextOptions == null) return; 
				Text source = (Text)e.widget;
				if (checkAllTextInputs(source))
				{
					ConfigurableOption option= retrieveOption(source);
					option.setValueIndex(parseTextInput(source.getText()));
					updatePreview(fNewOptions);
				}
		}
	};
	
	private boolean checkAllTextInputs(Text source)
	{
		boolean valueOK = false;
		fErrorMessage = null;
		for (int i=0; i < fTextOptions.length; i++)
			{
				int val=0;
				Text next = fTextOptions[i];
				String text= next.getText();
				try 
				{
					val = parseTextInput(text);
					if (next == source)
						valueOK = true;
				} catch (NumberFormatException nx) {
					if (text.equals(""))
						fErrorMessage = new String(ERROR_MESSAGE_EMPTY);
					else
						fErrorMessage = new String(text + ERROR_MESSAGE_INVALID);
				}	
			}
			setErrorMessage(fErrorMessage);
			return valueOK;
	}	
	
	private SelectionListener fButtonListener = new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				Button src= (Button) e.widget;
				if (src == fApplyButton) 
					performApply();
			 	else if (src == fDefaultButton) 
					performDefaults();
			}
		};
	
		
		private static ConfigurableOption[] getDefaultOptions()
		{
			return CodeFormatter.getDefaultOptions(Locale.getDefault());
		}
		
		public static ConfigurableOption[] getCurrentOptions()
		{
			return fgCurrentOptions;
		}

		private ConfigurableOption[] getNewOptions()
		{
			return fNewOptions;
		}
		
		
		
		private void savePreferences() throws IOException {
		for (int i= 0; i < fgCurrentOptions.length; i++) {
			String preferenceID= PREFERENCE_NAME + "." + fgCurrentOptions[i].getID();
			fgPreferenceStore.setValue(preferenceID, String.valueOf(fgCurrentOptions[i].getCurrentValueIndex()));
		}
		}


	private static Hashtable findCategories(ConfigurableOption[] options) {

		Hashtable OptionCategories= new Hashtable(options.length);
		for (int i= 0; i < options.length; i++) {
			String category= options[i].getCategory();
			if (!OptionCategories.containsKey(category))
				OptionCategories.put(category, new ArrayList());
			((ArrayList) OptionCategories.get(category)).add(options[i]);
		}
		Enumeration categories= OptionCategories.keys();
		while (categories.hasMoreElements()) {
			String category= (String) categories.nextElement();
			OptionCategories.put(category, ((ArrayList)OptionCategories.get(category)).toArray(new ConfigurableOption[1]));
		}
		return OptionCategories;
	}

	
	private static String[] sortCategories(Hashtable OptionCategories) {
		ArrayList SortedOptionCategories= new ArrayList(OptionCategories.size());
		Enumeration categories= OptionCategories.keys();
		while (categories.hasMoreElements()) {
			String category= (String) categories.nextElement();
			int pos;
			for (pos= 0; pos < SortedOptionCategories.size(); pos++)
				if (
				((ConfigurableOption[]) OptionCategories.get(category)).length > ((ConfigurableOption[]) OptionCategories.get((String) SortedOptionCategories.get(pos))).length)
					break;
			SortedOptionCategories.add(pos, category);
		}
		return (String[]) SortedOptionCategories.toArray(new String[1]);
	}

	
	
	private static int findCategoryCenter(String[] SortedOptionCategories, Hashtable OptionCategories)
	{
		int middle = SortedOptionCategories.length;
		int sizeLeft=0; 
		int sizeRight=0;
		do 
		{
			sizeLeft=sizeRight=0; 
			for (int i=SortedOptionCategories.length -1; i >=0 ; i--)
			{
				if (i<middle)
					sizeLeft += ((ConfigurableOption[])OptionCategories.get(SortedOptionCategories[i])).length;
				else
					sizeRight += ((ConfigurableOption[])OptionCategories.get(SortedOptionCategories[i])).length;
			}
			middle--;
		} while ( (sizeLeft > sizeRight) && middle > 1);
		return middle;
	}
	
	private static void updateOptions(ConfigurableOption[] current, ConfigurableOption[] update) {
		for (int i= 0; i < current.length; i++) {
			current[i].setValueIndex(update[i].getCurrentValueIndex());
		}
	}	
	public void init(IWorkbench workbench) {
		
	}	
	
	public CodeFormatterPreferencePage() {
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());	
		fNewOptions= getDefaultOptions();
		updateOptions(fNewOptions, fgCurrentOptions);		
		fPreviewDocument = new Document();
		fPreviewText= loadPreviewFile(PREVIEW_FILE);
	}

	
	public static void initDefaults(IPreferenceStore store) {
			fgPreferenceStore = store;
			fgCurrentOptions= CodeFormatter.getDefaultOptions(Locale.getDefault());
		 	for (int i= 0; i < fgCurrentOptions.length; i++) {
				String preferenceID= PREFERENCE_NAME + "." + fgCurrentOptions[i].getID();
				if (fgPreferenceStore.contains(preferenceID))
				fgCurrentOptions[i].setValueIndex(fgPreferenceStore.getInt(preferenceID));
		
			}
	}
		
	

	private  String loadPreviewFile(String fn) {
		String separator= System.getProperty("line.separator");
		StringBuffer btxt= new StringBuffer(512);
		try {
			BufferedReader rin= new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(fn)));

			String l;
			while ((l= rin.readLine()) != null) {
				btxt.append(l);
				btxt.append(separator);
			}
		} catch (IOException io) {
			io.printStackTrace();
		}
		return btxt.toString();
	}	

	
	public Control createContents(Composite parent) {		
		fMainPanel= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 1;
		layout.marginWidth=0;
		fMainPanel.setLayout(layout);
		createOptimizedCategoryLayout(fMainPanel);
		createPreview(fMainPanel);
		updateTabWidgetDependency();
		return (Control)fMainPanel;
	}


	public void performDefaults() {
		super.performDefaults();
		 updateOptions(fNewOptions,  getDefaultOptions());
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

	public boolean performOk()
	{
		 updateOptions( getCurrentOptions(), fNewOptions);
		try {
			savePreferences();
		} catch (IOException io) {
			io.printStackTrace();
		}
		return true;
	}
	
	public void performApply() {
		performOk();
		updatePreview(fNewOptions);
	}

	
	private void createOptimizedCategoryLayout(Composite parent)
	{
		Hashtable OptionCategories=  findCategories(fNewOptions);
		String[] SortedOptionCategories =  sortCategories(OptionCategories);
		int categoryMiddle =  findCategoryCenter(SortedOptionCategories, OptionCategories);
		ArrayList checkOptions= new ArrayList();
		ArrayList textOptions= new ArrayList();
		Composite panel= new Composite(parent, SWT.NONE);
		GridLayout gl= new GridLayout();
		gl.numColumns= 2;
		gl.marginWidth=0;
		GridData gd= new GridData();
		gd.grabExcessHorizontalSpace= true;
		gd.grabExcessVerticalSpace= true;
		gd.horizontalAlignment= GridData.FILL;
		gd.verticalAlignment= GridData.FILL;
		panel.setLayoutData(gd);
		panel.setLayout(gl);

		
		Composite panel1= new Composite(panel, SWT.NONE);
		GridLayout gl1= new GridLayout();
		gl1.numColumns= 1;
		gl1.marginWidth=0;
		GridData gd1= new GridData();
		gd1.grabExcessHorizontalSpace= true;
		gd1.grabExcessVerticalSpace= true;
		gd1.horizontalAlignment= GridData.FILL;
		gd1.verticalAlignment= GridData.FILL;
		panel1.setLayoutData(gd1);
		panel1.setLayout(gl1);
		for (int i=0; i < categoryMiddle; i++)
			createSingleCategory(panel1, (ConfigurableOption[])OptionCategories.get(SortedOptionCategories[i]), textOptions, checkOptions, SortedOptionCategories[i]);

		
		Composite panel2= new Composite(panel, SWT.NONE);
		GridLayout gl2= new GridLayout();
		gl2.numColumns= 1;
		gl2.marginWidth=0;
		GridData gd2= new GridData();
		gd2.grabExcessHorizontalSpace= true;
		gd2.grabExcessVerticalSpace= true;
		gd2.horizontalAlignment= GridData.FILL;
		gd2.verticalAlignment= GridData.FILL;
		panel2.setLayoutData(gd2);
		panel2.setLayout(gl2);
		for (int i=categoryMiddle; i < SortedOptionCategories.length; i++)
			createSingleCategory(panel2, (ConfigurableOption[])OptionCategories.get(SortedOptionCategories[i]), textOptions, checkOptions, SortedOptionCategories[i]);
		fCheckOptions= (Button[]) checkOptions.toArray(new Button[checkOptions.size()]);
		fTextOptions= (Text[]) textOptions.toArray(new Text[textOptions.size()]);
	
	}
	
	
	private void createSingleCategory(Composite parent,ConfigurableOption[] options, ArrayList textOptions, ArrayList checkOptions,  String category)
	{	
			Group group= new Group(parent, SWT.NONE);
			group.setText(category);
			GridLayout gl2= new GridLayout();
			gl2.numColumns= 1;
			GridData gd2= new GridData();
			gd2.grabExcessHorizontalSpace= true;
			gd2.grabExcessVerticalSpace= true;
			gd2.horizontalAlignment= GridData.FILL;
			gd2.verticalAlignment= GridData.FILL;
			group.setLayoutData(gd2);
			group.setLayout(gl2);
			for (int i= 0; i < options.length; i++) {
				ConfigurableOption opt= options[i];
				if (opt.getPossibleValues() == ConfigurableOption.NoDiscreteValue)
					textOptions.add(createTextOption(opt.getName(), opt.getDescription(), opt, group, fTextListener));
				else
					if (opt.getPossibleValues().length == 2)
						checkOptions.add(createCheckOption(opt.getName(), opt.getDescription(), opt, group, fCheckboxListener));
			}		
			
	}
	
	private Button createCheckOption(String name, String description, ConfigurableOption option, Composite parent, SelectionListener con) {
		Composite pan= new Composite(parent, SWT.NONE);
		GridLayout gl= new GridLayout();
		gl.numColumns= 1;
		pan.setLayout(gl);
		GridData lgd= new GridData();
		lgd.horizontalAlignment= GridData.FILL;
		lgd.grabExcessHorizontalSpace= true;
		pan.setLayoutData(lgd);
		Button check= new Button(pan, SWT.CHECK);
		check.setToolTipText(description);
		check.addSelectionListener(con);
		check.setData(WIDGET_DATA_KEY, option);
		check.setText(name);
		check.setSelection(option.getCurrentValueIndex() == 0 ? true : false);
		return check;
	}

	private Text createTextOption(String name, String description, ConfigurableOption option, Composite parent, ModifyListener con) {
		Composite pan= new Composite(parent, SWT.NONE);
		GridLayout gl= new GridLayout();
		gl.numColumns= 1;
		pan.setLayout(gl);
		GridData lgd= new GridData();
		lgd.horizontalAlignment= GridData.FILL;
		lgd.grabExcessHorizontalSpace= true;
		pan.setLayoutData(lgd);
		Label flabel= new Label(pan, SWT.NONE);
		flabel.setText(name);
		flabel.setToolTipText(description);
		Text text= new Text(pan, SWT.BORDER | SWT.SINGLE);
		text.setTextLimit(TEXT_LIMIT);
		text.setToolTipText(description);
		text.setData(WIDGET_DATA_KEY, option);
		GridData gd= new GridData();
		gd.widthHint= 20;
		gd.horizontalAlignment= GridData.BEGINNING;
		text.setLayoutData(gd);
		text.addModifyListener(con); 
		text.setText(String.valueOf(option.getCurrentValueIndex()));
		return text;
	}

	private void createPreview(Composite parent) {
		fPreviewViewer= new SourceViewer(parent, null,  SWT.H_SCROLL | SWT.V_SCROLL);
		JavaTextTools tools= JavaPlugin.getDefault().getJavaTextTools();
		fPreviewViewer.configure(new JavaSourceViewerConfiguration(tools, null));
		fPreviewViewer.getTextWidget().setFont(JFaceResources.getFontRegistry().get(JFaceResources.TEXT_FONT));
		fPreviewViewer.setEditable(false);
		fPreviewViewer.setDocument(fPreviewDocument);
		Control control = fPreviewViewer.getControl();
		GridData gd= new GridData();
		gd.grabExcessHorizontalSpace= true;
		gd.horizontalAlignment= GridData.FILL;
		gd.verticalAlignment= GridData.FILL;
		gd.horizontalSpan=1;
		gd.heightHint= 250;
		control.setLayoutData(gd);
		updatePreview(fNewOptions);
	}

	public void updatePreview(final ConfigurableOption[] options) {
		fPreviewDocument.set(CodeFormatter.format(fPreviewText,  INDENTATION_LEVEL, options));
	}

	private ConfigurableOption retrieveOption(Widget widget) {
		return (ConfigurableOption) widget.getData(WIDGET_DATA_KEY);
	}
	


	private int parseTextInput(String input) throws NumberFormatException
	{
		if (input.equals(""))
			throw new NumberFormatException();							
		int val= Integer.parseInt(input);
		if (val < 0)
			throw new NumberFormatException();
		return val;
		 
	}
	
	
	
	
	
	private void updateTabWidgetDependency()
	{
		boolean value = false;
		for (int i=0; i < fCheckOptions.length; i++)
		{
			ConfigurableOption option = retrieveOption(fCheckOptions[i]);
			if ( option.getID() == TAB_CHECK_OPTION_ID)
			{
				value =  (option.getCurrentValueIndex() == 0) ? true : false;
				break;
			}
		}
		
		for (int i=0; i < fTextOptions.length; i++)
		{
			if (retrieveOption(fTextOptions[i]).getID() == TAB_TEXT_OPTION_ID)
			{
				fTextOptions[i].setEnabled(!value);
				break;
			}
		}
	}
					
	private SelectionListener fCheckboxListener= new SelectionAdapter() {

		public void widgetSelected(SelectionEvent e) {
			Button src= (Button) e.widget;
			ConfigurableOption option=  retrieveOption(src);
			int value = (src.getSelection() == true) ? 0: 1; // See CodeFormatter logic
			option.setValueIndex(value);
			if (option.getID() == TAB_CHECK_OPTION_ID)
			{
				updateTabWidgetDependency();
			}		
			updatePreview(fNewOptions);
		}
	};


}


