package org.eclipse.jdt.internal.ui.preferences;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.text.template.Template;
import org.eclipse.jdt.internal.ui.text.template.TemplateContentProvider;
import org.eclipse.jdt.internal.ui.text.template.TemplateLabelProvider;
import org.eclipse.jdt.internal.ui.text.template.TemplateMessages;
import org.eclipse.jdt.internal.ui.text.template.TemplateSet;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.util.TabFolderLayout;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.DialogPageContextComputer;
import org.eclipse.ui.help.WorkbenchHelp;

public class TemplatePreferencePage	extends PreferencePage implements IWorkbenchPreferencePage {

	// preference store keys
	private static final String PREF_FORMAT_TEMPLATES= JavaUI.ID_PLUGIN + ".template.format"; //$NON-NLS-1$

	private CheckboxTableViewer fTableViewer;
	private Button fAddButton;
	private Button fRemoveButton;
	private Button fEnableAllButton;
	private Button fDisableAllButton;
	private Group fEditor;
	private Text fNameText;
	private Text fDescriptionText;
	private Combo fContextCombo;
	private SourceViewer fPatternEditor;
	private Button fFormatButton;
	private ControlEnableState fEditorEnabler;
	
	private List fEditorComponents;
	
	private Template fCurrent;
	
	public TemplatePreferencePage() {
		super();
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setDescription(TemplateMessages.getString("TemplatePreferencePage.message")); //$NON-NLS-1$
	}

	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite ancestor) {	
		Composite parent= new Composite(ancestor, SWT.NULL);

		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		
		parent.setLayout(layout);		

		TabFolder folder= new TabFolder(parent, SWT.NONE);
		folder.setLayout(new TabFolderLayout());	
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));

		Composite firstPage= new Composite(folder, SWT.NONE);
		layout= new GridLayout();
		layout.numColumns= 2;
		firstPage.setLayout(layout);		
		
		TabItem item= new TabItem(folder, SWT.NONE);
		item.setText(TemplateMessages.getString("TemplatePreferencePage.tab.edit")); //$NON-NLS-1$
		item.setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_TEMPLATE));
		item.setControl(firstPage);
		
		fTableViewer= new CheckboxTableViewer(firstPage, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		Table table= fTableViewer.getTable();
		
		GridData data= new GridData(GridData.FILL_BOTH);
		data.widthHint= convertWidthInCharsToPixels(80);
		data.heightHint= convertHeightInCharsToPixels(10);
		fTableViewer.getTable().setLayoutData(data);
				
		table.setHeaderVisible(true);
		table.setLinesVisible(true);		

		TableLayout tableLayout= new TableLayout();
		table.setLayout(tableLayout);
		
//		TableColumn column1= new TableColumn(table, SWT.NULL);
		TableColumn column1= table.getColumn(0);
		column1.setText(TemplateMessages.getString("TemplatePreferencePage.column.name")); //$NON-NLS-1$
	
		TableColumn column2= new TableColumn(table, SWT.NULL);
		column2.setText(TemplateMessages.getString("TemplatePreferencePage.column.description")); //$NON-NLS-1$
		
		tableLayout.addColumnData(new ColumnWeightData(30));
		tableLayout.addColumnData(new ColumnWeightData(70));
		
		fTableViewer.setLabelProvider(new TemplateLabelProvider());
		fTableViewer.setContentProvider(new TemplateContentProvider(fTableViewer, TemplateSet.getInstance()));
		
		fTableViewer.setSorter(new ViewerSorter() {
			public int compare(Viewer viewer, Object object1, Object object2) {
				if ((object1 instanceof Template) && (object2 instanceof Template)) {
					Template left= (Template) object1;
					Template right= (Template) object2;
					int result= left.getName().compareToIgnoreCase(right.getName());
					if (result != 0)
						return result;
					return left.getDescription().compareToIgnoreCase(right.getDescription());
				}
				return super.compare(viewer, object1, object2);
			}
			
			public boolean isSorterProperty(Object element, String property) {
				return true;
			}
		});
		
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				selectionChanged1();
			}
		});

		fTableViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				Template template=  (Template) event.getElement();
				template.setEnabled(event.getChecked());
			}
		});

		Composite buttons= new Composite(firstPage, SWT.NULL);
		buttons.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		buttons.setLayout(layout);
		
		fAddButton= new Button(buttons, SWT.PUSH);
		fAddButton.setLayoutData(getButtonGridData(fAddButton));
		fAddButton.setText(TemplateMessages.getString("TemplatePreferencePage.new")); //$NON-NLS-1$
		fAddButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				add();
			}
		});
		
		fRemoveButton= new Button(buttons, SWT.PUSH);
		fRemoveButton.setLayoutData(getButtonGridData(fRemoveButton));
		fRemoveButton.setText(TemplateMessages.getString("TemplatePreferencePage.remove")); //$NON-NLS-1$
		fRemoveButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				remove();
			}
		});
		
		fEnableAllButton= new Button(buttons, SWT.PUSH);
		fEnableAllButton.setLayoutData(getButtonGridData(fEnableAllButton));
		fEnableAllButton.setText(TemplateMessages.getString("TemplatePreferencePage.enable.all")); //$NON-NLS-1$
		fEnableAllButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				enableAll(true);
			}
		});

		fDisableAllButton= new Button(buttons, SWT.PUSH);
		fDisableAllButton.setLayoutData(getButtonGridData(fDisableAllButton));
		fDisableAllButton.setText(TemplateMessages.getString("TemplatePreferencePage.disable.all")); //$NON-NLS-1$
		fDisableAllButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				enableAll(false);
			}
		});

		fEditor= new Group(firstPage, SWT.NULL);
		fEditor.setText(TemplateMessages.getString("TemplatePreferencePage.editor")); //$NON-NLS-1$
		fEditor.setLayoutData(new GridData(GridData.FILL_BOTH));
		layout= new GridLayout();
		layout.numColumns= 2;
		fEditor.setLayout(layout);

		createLabel(fEditor, TemplateMessages.getString("TemplatePreferencePage.name")); //$NON-NLS-1$
		
		Composite composite= new Composite(fEditor, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		layout= new GridLayout();
		layout.numColumns= 3;
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		composite.setLayout(layout);

		fNameText= createText(composite);
		fNameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (fCurrent == null)
					return;

				fCurrent.setName(fNameText.getText());				
				fTableViewer.refresh(fCurrent);
				updateButtons();
			}
		});

		createLabel(composite, TemplateMessages.getString("TemplatePreferencePage.context")); //$NON-NLS-1$		
		fContextCombo= new Combo(composite, SWT.READ_ONLY);
		fContextCombo.setItems(new String[] {"java", "javadoc"}); //$NON-NLS-1$ //$NON-NLS-2$
		fContextCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (fCurrent == null)
					return;

				fCurrent.setContext(fContextCombo.getText());
				fTableViewer.refresh(fCurrent);
			}
		});
		
		createLabel(fEditor, TemplateMessages.getString("TemplatePreferencePage.description")); //$NON-NLS-1$		
		fDescriptionText= createText(fEditor);
		fDescriptionText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (fCurrent == null)
					return;
					
				fCurrent.setDescription(fDescriptionText.getText());
				fTableViewer.refresh(fCurrent);
			}
		});

		Label patternLabel= createLabel(fEditor, TemplateMessages.getString("TemplatePreferencePage.pattern")); //$NON-NLS-1$
		patternLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		fPatternEditor= createEditor(fEditor);

		Composite secondPage= new Composite(folder, SWT.NONE);
		layout= new GridLayout();
		layout.numColumns= 1;
		secondPage.setLayout(layout);		

		item= new TabItem(folder, SWT.NONE);
		item.setText(TemplateMessages.getString("TemplatePreferencePage.tab.options")); //$NON-NLS-1$
		item.setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_TEMPLATE));
		item.setControl(secondPage);
				
		fFormatButton= new Button(secondPage, SWT.CHECK);
		fFormatButton.setText(TemplateMessages.getString("TemplatePreferencePage.use.code.formatter")); //$NON-NLS-1$

		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		fFormatButton.setSelection(prefs.getBoolean(PREF_FORMAT_TEMPLATES));
		
		fTableViewer.setInput(TemplateSet.getInstance());		
		fTableViewer.setAllChecked(false);
		fTableViewer.setCheckedElements(getEnabledTemplates());		
		updateButtons();

		WorkbenchHelp.setHelp(parent, new DialogPageContextComputer(this, IJavaHelpContextIds.JRE_PREFERENCE_PAGE));		
		leaveEditor();
		
		return parent;
	}
	
	private Template[] getEnabledTemplates() {
		Template[] templates= TemplateSet.getInstance().getTemplates();
		
		List list= new ArrayList(templates.length);
		
		for (int i= 0; i != templates.length; i++)
			if (templates[i].isEnabled())
				list.add(templates[i]);
				
		return (Template[]) list.toArray(new Template[list.size()]);
	}

	private static Label createLabel(Composite parent, String name) {
		Label label= new Label(parent, SWT.NULL);
		label.setText(name);
		label.setLayoutData(new GridData());

		return label;
	}
	
	private static Text createText(Composite parent) {
		Text text= new Text(parent, SWT.BORDER);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));		
		
		return text;
	}
	
	private SourceViewer createEditor(Composite parent) {
		SourceViewer viewer= new SourceViewer(parent, null, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		JavaTextTools tools= JavaPlugin.getDefault().getJavaTextTools();
		viewer.configure(new JavaSourceViewerConfiguration(tools, null));
		viewer.setEditable(true);
		viewer.setDocument(new Document());
	
		Font font= JFaceResources.getFontRegistry().get(JFaceResources.TEXT_FONT);
		viewer.getTextWidget().setFont(font);
		
		Control control= viewer.getControl();
		GridData data= new GridData(GridData.FILL_BOTH);
		data.heightHint= convertHeightInCharsToPixels(5);
		control.setLayoutData(data);
		
		return viewer;
	}
	
	private static GridData getButtonGridData(Button button) {
		GridData data= new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint= SWTUtil.getButtonWidthHint(button);
		data.heightHint= SWTUtil.getButtonHeigthHint(button);
	
		return data;
	}
	
	private void selectionChanged1() {		
		IStructuredSelection selection= (IStructuredSelection) fTableViewer.getSelection();

		if (selection.size() == 1) {
			Template template= (Template) selection.getFirstElement();
			enterEditor(template);		
		} else {		
			leaveEditor();
		}
		
		updateButtons();
	}
	
	private void updateButtons() {
		int selectionCount= ((IStructuredSelection) fTableViewer.getSelection()).size();
		int itemCount= fTableViewer.getTable().getItemCount();
		
		boolean error= (fCurrent != null) && (fCurrent.getName().length() == 0);
		
		fAddButton.setEnabled(!error);
		fRemoveButton.setEnabled(selectionCount > 0 && selectionCount <= itemCount);
		fEnableAllButton.setEnabled(itemCount > 0);
		fDisableAllButton.setEnabled(itemCount > 0);

		StatusInfo status= new StatusInfo();
		if (error)
			status.setError(TemplateMessages.getString("TemplatePreferencePage.error.noname")); //$NON-NLS-1$
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);			
	}
	
	private static int getIndex(String context) {
		if (context.equals("java")) //$NON-NLS-1$
			return 0;
		else if (context.equals("javadoc")) //$NON-NLS-1$
			return 1;
		else
			return -1;
	}

	private void enterEditor(Template template) {
		if (template == fCurrent) // #4916
			return;
		
		if (fCurrent != null) // #4358
			leaveEditor();
		
		fCurrent= template;
	
		fNameText.setText(template.getName());
		fDescriptionText.setText(template.getDescription());
		fContextCombo.select(getIndex(template.getContext()));
		fPatternEditor.getDocument().set(template.getPattern());

		if (fEditorEnabler != null) {
			fEditorEnabler.restore();
			fEditorEnabler= null;
		}
	}
	
	private void leaveEditor() {
		if (fCurrent == null)
			return;
			
		// #4358			
		fCurrent.setPattern(fPatternEditor.getTextWidget().getText());
		
		fCurrent= null;

		fNameText.setText(""); //$NON-NLS-1$
		fDescriptionText.setText(""); //$NON-NLS-1$
		fContextCombo.select(getIndex("")); //$NON-NLS-1$
		fPatternEditor.getDocument().set(""); //$NON-NLS-1$

		if (fEditorEnabler == null)
			fEditorEnabler= ControlEnableState.disable(fEditor);		
	}
	
	private void add() {
		Template template= new Template();
		template.setContext("java"); //$NON-NLS-1$

		TemplateSet.getInstance().add(template);
		fTableViewer.refresh();
		fTableViewer.setChecked(template, template.isEnabled());
		enterEditor(template);

		fNameText.setFocus();
	}
	
	private void remove() {
		IStructuredSelection selection= (IStructuredSelection) fTableViewer.getSelection();

		Iterator elements= selection.iterator();
		while (elements.hasNext()) {
			Template template= (Template) elements.next();
			TemplateSet.getInstance().remove(template);
		}

		leaveEditor();
		fTableViewer.refresh();
	}
	
	private void enableAll(boolean enable) {
		Template[] templates= TemplateSet.getInstance().getTemplates();
		for (int i= 0; i != templates.length; i++)
			templates[i].setEnabled(enable);		
			
		fTableViewer.setAllChecked(enable);
	}
	
	/*
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {}

	/*
	 * @see Control#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible)
			setTitle(TemplateMessages.getString("TemplatePreferencePage.title")); //$NON-NLS-1$
	}
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		fFormatButton.setSelection(prefs.getDefaultBoolean(PREF_FORMAT_TEMPLATES));

		TemplateSet.getInstance().restoreDefaults();		
		fTableViewer.refresh();
	}

	/*
	 * @see PreferencePage#performOk()
	 */	
	public boolean performOk() {
		leaveEditor(); // #4358
		
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		prefs.setValue(PREF_FORMAT_TEMPLATES, fFormatButton.getSelection());

		TemplateSet.getInstance().save();
		
		return super.performOk();
	}	
	
	/*
	 * @see PreferencePage#performCancel()
	 */
	public boolean performCancel() {
		TemplateSet.getInstance().reset();
		return super.performCancel();
	}
	
	/**
	 * Initializes the default values of this page in the preference bundle.
	 * Will be called on startup of the JavaPlugin
	 */
	public static void initDefaults(IPreferenceStore prefs) {
		prefs.setDefault(PREF_FORMAT_TEMPLATES, true);
	}

	public static boolean useCodeFormatter() {
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		return prefs.getBoolean(PREF_FORMAT_TEMPLATES);
	}
	
}
