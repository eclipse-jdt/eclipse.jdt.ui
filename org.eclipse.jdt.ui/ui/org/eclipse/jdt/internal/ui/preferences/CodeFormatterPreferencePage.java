package org.eclipse.jdt.internal.ui.preferences;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.eclipse.jdt.internal.compiler.ConfigurableOption;
import org.eclipse.jdt.internal.formatter.CodeFormatter;
import org.eclipse.jdt.internal.ui.JavaPlugin;

//TODO
// statusbar instead of message
//
class CodeFormatterUIModel
	{
		private ConfigurableOption[] fNewOptions;
		
		private static ConfigurableOption[] fgCurrentOptions;
		
		private static IPreferenceStore fgPreferenceStore;

		public static final int INDENTATION_LEVEL = 0;
		
		public CodeFormatterUIModel() {
			
			fNewOptions= getDefaultOptions();
			updateOptions(fNewOptions, fgCurrentOptions);
		}
		
		public static void init(IPreferenceStore store) {

			fgPreferenceStore = store;
			loadDefaults();
		}

		
		public static ConfigurableOption[] getDefaultOptions()
		{
			return CodeFormatter.getDefaultOptions(Locale.getDefault());
		}
		
		public static ConfigurableOption[] getCurrentOptions()
		{
			return fgCurrentOptions;
		}

		public ConfigurableOption[] getNewOptions()
		{
			return fNewOptions;
		}
		
		private static  void loadDefaults() {
			fgCurrentOptions= CodeFormatter.getDefaultOptions(Locale.getDefault());
			//fgPreferenceStore= new PreferenceStore(name);
		//try {
		//	fgPreferenceStore.load();
		//} catch (IOException io) {
		//	io.printStackTrace();
		//}
		}
		
		public void savePreferences() throws IOException {
		for (int i= 0; i < fgCurrentOptions.length; i++) {
			String preferenceID= getClass().getName() + "." + fgCurrentOptions[i].getID();
			fgPreferenceStore.setValue(preferenceID, String.valueOf(fgCurrentOptions[i].getCurrentValueIndex()));
			fgPreferenceStore.setDefault(preferenceID, String.valueOf(fgCurrentOptions[i].getCurrentValueIndex()));
		}
		//fgPreferenceStore.save();
		}

		public void initPreferences()
		 {
		 	for (int i= 0; i < fgCurrentOptions.length; i++) {
			String preferenceID= getClass().getName() + "." + fgCurrentOptions[i].getID();

			if (fgPreferenceStore.contains(preferenceID))
				fgCurrentOptions[i].setValueIndex(fgPreferenceStore.getInt(preferenceID));
			
		 }
	}

	public static Hashtable findCategories(ConfigurableOption[] options) {

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

	/**
	* Sorts categories in descending order
	*/
	 
	public static String[] sortCategories(Hashtable OptionCategories) {
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

	/**
	* Finds center of a sorted categories
	*/
	
	public static int findCategoryCenter(String[] SortedOptionCategories, Hashtable OptionCategories)
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
	
	public static void updateOptions(ConfigurableOption[] current, ConfigurableOption[] update) {
		for (int i= 0; i < current.length; i++) {
			current[i].setValueIndex(update[i].getCurrentValueIndex());
		}
	}
}


public class CodeFormatterPreferencePage extends PreferencePage implements IWorkbenchPreferencePage{

	
	private String fPreviewBuffer;
	private static final String fgpreviewFile= "CodeFormatterPreviewCode.txt";
	private CodeFormatterUIModel fModel;
	private Composite fMainPanel;
	private Button[] fCheckOptions;
	private Text[] fTextOptions;
	private Button fApplyButton;
	private Button fDefaultButton;
	private StyledText fPreviewText;
		
		
	public void init(IWorkbench workbench) {
		
	}	
	
	public CodeFormatterPreferencePage() {
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());	
		fModel = new CodeFormatterUIModel();
		fModel.initPreferences();
		fPreviewBuffer= loadPreviewFile(fgpreviewFile);
	}

	//BUG in JavaPlugin : initDefaults(store) is not called on startup
	
	public static void initDefaults(IPreferenceStore store) {
		System.out.println("initDefaults");
		CodeFormatterUIModel.init(store);		
	}
		
	public static ConfigurableOption[] getCurrentOptions() {
		return CodeFormatterUIModel.getCurrentOptions();
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
		
		
		System.out.println("done createContents");
		fMainPanel= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 1;
		fMainPanel.setLayout(layout);
		//createSimpleLayout(fMainPanel);
		//createCategoryLayout(fMainPanel);
		//createCustomLayout(fMainPanel);
		createOptimizedCategoryLayout(fMainPanel);
		createPreview(fMainPanel);
		//createMainControls(fMainPanel);	
		return (Control)fMainPanel;
	}


	public void performDefaults() {
		super.performDefaults();
		CodeFormatterUIModel.updateOptions(fModel.getNewOptions(), CodeFormatterUIModel.getDefaultOptions());
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
		updatePreview(fModel.getNewOptions());
	}

	public boolean performOk()
	{
		CodeFormatterUIModel.updateOptions(CodeFormatterUIModel.getCurrentOptions(), fModel.getNewOptions());
		try {
			fModel.savePreferences();
		} catch (IOException io) {
			io.printStackTrace();
		}
		return true;
	}
	
	public void performApply() {
		performOk();
		updatePreview(fModel.getNewOptions());
	}

	private void createCategoryLayout(Composite parent)
	{
	Composite panel= new Composite(parent, SWT.NONE);
		GridLayout gl= new GridLayout();
		gl.numColumns= 1;
		GridData gd= new GridData();
		gd.grabExcessHorizontalSpace= true;
		gd.grabExcessVerticalSpace= true;
		gd.horizontalAlignment= GridData.FILL;
		gd.verticalAlignment= GridData.FILL;
		panel.setLayoutData(gd);
		panel.setLayout(gl);
		Hashtable OptionCategories= CodeFormatterUIModel.findCategories(fModel.getNewOptions());
		String[] SortedOptionCategories = CodeFormatterUIModel.sortCategories(OptionCategories);
		ArrayList checkOptions= new ArrayList();
		ArrayList textOptions= new ArrayList();
		for (int cat= 0; cat < SortedOptionCategories.length; cat++) {
			String category= (String) SortedOptionCategories[cat];
			createSingleCategory(panel, (ConfigurableOption[])OptionCategories.get(category), textOptions, checkOptions, category);
		}
		fCheckOptions= (Button[]) checkOptions.toArray(new Button[checkOptions.size()]);
		fTextOptions= (Text[]) textOptions.toArray(new Text[textOptions.size()]);
	
	}
	
	private void createOptimizedCategoryLayout(Composite parent)
	{
		Hashtable OptionCategories= CodeFormatterUIModel.findCategories(fModel.getNewOptions());
		String[] SortedOptionCategories = CodeFormatterUIModel.sortCategories(OptionCategories);
		int categoryMiddle = CodeFormatterUIModel.findCategoryCenter(SortedOptionCategories, OptionCategories);
		ArrayList checkOptions= new ArrayList();
		ArrayList textOptions= new ArrayList();
		Composite panel= new Composite(parent, SWT.NONE);
		GridLayout gl= new GridLayout();
		gl.numColumns= 2;
		GridData gd= new GridData();
		gd.grabExcessHorizontalSpace= true;
		gd.grabExcessVerticalSpace= true;
		gd.horizontalAlignment= GridData.FILL;
		gd.verticalAlignment= GridData.FILL;
		panel.setLayoutData(gd);
		panel.setLayout(gl);

		// panel1
		Composite panel1= new Composite(panel, SWT.NONE);
		GridLayout gl1= new GridLayout();
		gl1.numColumns= 1;
		GridData gd1= new GridData();
		gd1.grabExcessHorizontalSpace= true;
		gd1.grabExcessVerticalSpace= true;
		gd1.horizontalAlignment= GridData.FILL;
		gd1.verticalAlignment= GridData.FILL;
		panel1.setLayoutData(gd1);
		panel1.setLayout(gl1);
		for (int i=0; i < categoryMiddle; i++)
			createSingleCategory(panel1, (ConfigurableOption[])OptionCategories.get(SortedOptionCategories[i]), textOptions, checkOptions, SortedOptionCategories[i]);

		//panel 2
		Composite panel2= new Composite(panel, SWT.NONE);
		GridLayout gl2= new GridLayout();
		gl2.numColumns= 1;
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
	
	private void createCustomLayout(Composite parent) {
		Hashtable OptionCategories= CodeFormatterUIModel.findCategories(fModel.getNewOptions());
		
		ArrayList checkOptions= new ArrayList();
		ArrayList textOptions= new ArrayList();
		Composite panel= new Composite(parent, SWT.NONE);
		GridLayout gl= new GridLayout();
		gl.numColumns= 2;
		GridData gd= new GridData();
		gd.grabExcessHorizontalSpace= true;
		gd.grabExcessVerticalSpace= true;
		gd.horizontalAlignment= GridData.FILL;
		gd.verticalAlignment= GridData.FILL;
		panel.setLayoutData(gd);
		panel.setLayout(gl);

		// panel1
		Composite panel1= new Composite(panel, SWT.NONE);
		GridLayout gl1= new GridLayout();
		gl1.numColumns= 1;
		GridData gd1= new GridData();
		gd1.grabExcessHorizontalSpace= true;
		gd1.grabExcessVerticalSpace= true;
		gd1.horizontalAlignment= GridData.FILL;
		gd1.verticalAlignment= GridData.FILL;
		panel1.setLayoutData(gd1);
		panel1.setLayout(gl1);
		createSingleCategory(panel1, (ConfigurableOption[])OptionCategories.get("Newline"), textOptions, checkOptions,  "Newline");

		//panel 2
		Composite panel2= new Composite(panel, SWT.NONE);
		GridLayout gl2= new GridLayout();
		gl2.numColumns= 1;
		GridData gd2= new GridData();
		gd2.grabExcessHorizontalSpace= true;
		gd2.grabExcessVerticalSpace= true;
		gd2.horizontalAlignment= GridData.FILL;
		gd2.verticalAlignment= GridData.FILL;
		panel2.setLayoutData(gd2);
		panel2.setLayout(gl2);
		createSingleCategory(panel2, (ConfigurableOption[])OptionCategories.get("Style"), textOptions, checkOptions,  "Style");
		createSingleCategory(panel2, (ConfigurableOption[])OptionCategories.get("Line splitting"), textOptions, checkOptions,  "Line splitting");	
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
					textOptions.add(createTextOption(opt.getName(), opt.getDescription(), opt, group, fTextfieldListener));
				else
					if (opt.getPossibleValues().length == 2)
						checkOptions.add(createCheckOption(opt.getName(), opt.getDescription(), opt, group, fCheckboxListener));
					else;
			}						;
	}
	
	private void createSimpleLayout(Composite parent) {
		Composite panel= new Composite(parent, SWT.NONE);
		GridLayout gl= new GridLayout();
		gl.numColumns= 1;
		GridData gd= new GridData();
		gd.grabExcessHorizontalSpace= true;
		gd.horizontalAlignment= GridData.FILL;
		panel.setLayoutData(gd);
		panel.setLayout(gl);
		ArrayList checkOptions= new ArrayList();
		ArrayList textOptions= new ArrayList();
		for (int i= 0; i < CodeFormatterUIModel.getCurrentOptions().length; i++) {
			ConfigurableOption opt= CodeFormatterUIModel.getCurrentOptions()[i];
			if (opt.getPossibleValues() == ConfigurableOption.NoDiscreteValue)
				textOptions.add(createTextOption(opt.getName(), opt.getDescription(), fModel.getNewOptions()[i], panel, fTextfieldListener));
			else
				if (opt.getPossibleValues().length == 2)
					checkOptions.add(createCheckOption(opt.getName(), opt.getDescription(), fModel.getNewOptions()[i], panel, fCheckboxListener));
				else
					;
		}
		System.out.println("ok");
		fCheckOptions= (Button[]) checkOptions.toArray(new Button[checkOptions.size()]);
		fTextOptions= (Text[]) textOptions.toArray(new Text[textOptions.size()]);
	}

	private void createMainControls(Composite parent) {
		Composite pan= new Composite(parent, SWT.RIGHT);
		pan.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		pan.setLayout(new RowLayout());
		fApplyButton= new Button(pan, SWT.RIGHT);
		fApplyButton.setText("Apply");

		fApplyButton.addSelectionListener(fButtonListener);
		fDefaultButton= new Button(pan, SWT.RIGHT);
		fDefaultButton.setText("Defaults");
		fDefaultButton.addSelectionListener(fButtonListener);
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
		check.setData("OPTION", option);
		check.setText(name);
		check.setSelection(option.getCurrentValueIndex() == 0 ? true : false);
		return check;
	}

	private Text createTextOption(String name, String description, ConfigurableOption option, Composite parent, FocusListener con) {
		Composite pan= new Composite(parent, SWT.NONE);
		GridLayout gl= new GridLayout();
		gl.numColumns= 2;
		pan.setLayout(gl);
		GridData lgd= new GridData();
		lgd.horizontalAlignment= GridData.FILL;
		lgd.grabExcessHorizontalSpace= true;
		pan.setLayoutData(lgd);
		Label flabel= new Label(pan, SWT.NONE);
		flabel.setText(name);
		Text text= new Text(pan, SWT.BORDER | SWT.SINGLE);
		text.setToolTipText(description);
		text.setData("OPTION", option);
		GridData gd= new GridData();
		gd.widthHint= 25;
		gd.horizontalAlignment= GridData.BEGINNING;
		text.setLayoutData(gd);
		text.addFocusListener(con);
		// TESTING text.addModifyListener(con); 
		text.setText(String.valueOf(option.getCurrentValueIndex()));
		return text;
	}

	private void createPreview(Composite parent) {
		Composite pan= new Composite(parent, SWT.BORDER);
		GridLayout gl= new GridLayout();
		gl.numColumns= 1;
		pan.setLayout(gl);
		GridData gd0= new GridData();
		gd0.grabExcessHorizontalSpace= true;
		gd0.horizontalAlignment= GridData.FILL;
		gd0.verticalAlignment= GridData.FILL;
		pan.setLayoutData(gd0);
		
		fPreviewText= new StyledText(pan, SWT.MULTI);
		GridData gd= new GridData();
		gd.grabExcessHorizontalSpace= true;
		gd.horizontalAlignment= GridData.FILL;
		gd.verticalAlignment= GridData.FILL;
		gd.heightHint= 200;
		gd.widthHint= 400;
		fPreviewText.setLayoutData(gd);
		fPreviewText.setFont(JFaceResources.getFontRegistry().get(JFaceResources.TEXT_FONT));
		updatePreview(fModel.getNewOptions());
		Label ltxt= new Label(pan, SWT.CENTER);
		GridData lgd= new GridData();
		lgd.horizontalAlignment= GridData.FILL;
		ltxt.setLayoutData(lgd);
		ltxt.setText("Preview");
	}

	public void updatePreview(final ConfigurableOption[] options) {
		if (fPreviewText== null)
			return;
		fPreviewText.setText(CodeFormatter.format(fPreviewBuffer, CodeFormatterUIModel.INDENTATION_LEVEL, options));
	}

	private ConfigurableOption retrieveOption(Widget widget) {
		return (ConfigurableOption) widget.getData("OPTION");
	}
	

	SelectionListener fButtonListener= new SelectionAdapter() {

		public void widgetSelected(SelectionEvent e) {

			Button src= (Button) e.widget;
			if (src == fApplyButton) {
				performApply();
			} else
				if (src == fDefaultButton) {
					performDefaults();
				}

		}
	};

	//private ModifyListener textListener = new ModifyListener()
	//{
	//	public void modifyText(ModifyEvent e)
	//	{
	//		int val;
	//		Text src= (Text) e.widget;
	//		ConfigurableOption option= retrieveOption(src);
	//		try {
	//			val= Integer.parseInt(src.getText());
	//			if (val < 0)
	//				throw new NumberFormatException("Negative number");
	//		} catch (NumberFormatException nex) {
	//			//seterrorMessage Message status line
	//			MessageBox msg= new MessageBox(fMainPanel.getShell());
	//			msg.setMessage("Error : " + src.getText() + " is NaN");
	//			msg.open();
	//			src.setText((String) String.valueOf(option.getCurrentValueIndex()));
	//			return;
	//		}
	//		option.setValueIndex(val);
	//		updatePreview(fModel.getNewOptions());
	//	}
//
//	};

	private FocusListener fTextfieldListener= new FocusAdapter() {

		public void focusLost(FocusEvent e) {
			int val;
			Text src= (Text) e.widget;
			ConfigurableOption option= retrieveOption(src);
			try {
				val= Integer.parseInt(src.getText());
				if (val < 0)
					throw new NumberFormatException("Negative number");
			} catch (NumberFormatException nex) {
				//seterrorMessage Message status line
				MessageBox msg= new MessageBox(fMainPanel.getShell());
				msg.setMessage("Error : " + src.getText() + " is NaN");
				msg.open();
				src.setText((String) String.valueOf(option.getCurrentValueIndex()));
				return;
			}
			option.setValueIndex(val);
			updatePreview(fModel.getNewOptions());
		}

	};

	private SelectionListener fCheckboxListener= new SelectionAdapter() {

		public void widgetSelected(SelectionEvent e) {
			Button src= (Button) e.widget;
			ConfigurableOption option=  retrieveOption(src);
			if (src.getSelection() == true)
				option.setValueIndex(0);
			else
				option.setValueIndex(1);
			updatePreview(fModel.getNewOptions());
		}
	};

	
//TESTING
	public static void main(String[] args) throws IOException {
		Display display= new Display();
		Shell shell= new Shell(display);
		shell.setLayout(new RowLayout());
		//CodeFormatterPreferencePage demo= new CodeFormatterPreferencePage();
		//demo.createContents(shell);
		shell.pack();
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}

}


