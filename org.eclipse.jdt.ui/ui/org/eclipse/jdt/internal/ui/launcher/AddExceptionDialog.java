/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.launcher;import java.util.List;
import java.lang.reflect.InvocationTargetException;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.Status;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.ITypeHierarchy;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.core.search.SearchEngine;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.dialogs.SelectionList;import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;import org.eclipse.jdt.internal.ui.util.AllTypesSearchEngine;import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;import org.eclipse.jdt.internal.ui.util.TypeRef;import org.eclipse.jdt.internal.ui.util.TypeRefLabelProvider;import org.eclipse.jdt.ui.IJavaElementSearchConstants;import org.eclipse.jface.dialogs.IDialogSettings;import org.eclipse.jface.dialogs.ProgressMonitorDialog;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.swt.SWT;import org.eclipse.swt.events.SelectionAdapter;import org.eclipse.swt.events.SelectionEvent;import org.eclipse.swt.events.SelectionListener;import org.eclipse.swt.layout.GridData;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.Button;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Shell;

/**
 * Lots of stuff commented out because pending API change in 
 * Debugger
 */
public class AddExceptionDialog extends StatusDialog {		private static final String DIALOG_SETTINGS= "AddExceptionDialog";	private static final String SETTING_CAUGHT_CHECKED= "caughtChecked";
	private static final String SETTING_UNCAUGHT_CHECKED= "uncaughtChecked";
	//private Text fExceptionName;
	private SelectionList fTypeList;
	private boolean fTypeListInitialized= false;
	//private Button fAddFromTextRadio;
	//private Button fAddFromListRadio;
	
	private Button fCaughtBox;
	private Button fUncaughtBox;
	
	public static final int CHECKED_EXCEPTION= 0;
	public static final int UNCHECKED_EXCEPTION= 1;
	public static final int NO_EXCEPTION= -1;
	
	private SelectionListener fListListener= new SelectionAdapter() {
		public void widgetSelected(SelectionEvent evt) {
			validateListSelection();
		}
		
		public void widgetDefaultSelected(SelectionEvent e) {
			validateListSelection();
			if (getStatus().isOK())
				okPressed();
		}
	};
	
	/*private Listener fTextListener= new Listener() {
		public void handleEvent(Event evt) {
			validateExceptionName();
		}
	};*/
	
	private Object fResult;
	private int fExceptionType= NO_EXCEPTION;
	private boolean fIsCaughtSelected= true;
	private boolean fIsUncaughtSelected= true;
	/**
	 * Constructor for AddExceptionDialog
	 */
	protected AddExceptionDialog(Shell parentShell) {
		super(parentShell);
		setTitle("Add Exception");
	}
	
	protected Control createDialogArea(Composite ancestor) {
		initFromDialogSettings();		Composite parent= new Composite(ancestor, SWT.NULL);
		GridLayout layout= new GridLayout();
		parent.setLayout(layout);		
		/*fAddFromTextRadio= new Button(parent, SWT.RADIO);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.verticalAlignment= gd.BEGINNING;
		fAddFromTextRadio.setLayoutData(gd);
		fAddFromTextRadio.setText("Add Exception named:");
		fAddFromTextRadio.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event evt) {
				if (fAddFromTextRadio.getSelection())
					addFromTextSelected(true);
					addFromListSelected(false);
			}
		});
		
		fExceptionName= new Text(parent, SWT.BORDER);
		gd= new GridData(GridData.FILL_BOTH);
		gd.horizontalIndent= convertWidthInCharsToPixels(4);
		fExceptionName.setLayoutData(gd);*/
		
		/*fAddFromListRadio= new Button(parent, SWT.RADIO);
		fAddFromListRadio.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fAddFromListRadio.setText("Add Exception from list");
		fAddFromListRadio.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event evt) {
				if (fAddFromListRadio.getSelection())
					addFromTextSelected(false);
					addFromListSelected(true);
			}
		});*/

		Label l= new Label(parent, SWT.NULL);
		l.setLayoutData(new GridData());
		l.setText("Choose an Exception (? = any character, * = any string)");
		
		fTypeList= new SelectionList(parent, SWT.BORDER | SWT.SINGLE, 
				new TypeRefLabelProvider(TypeRefLabelProvider.SHOW_PACKAGE_POSTFIX), true);
		GridData gd= new GridData(GridData.FILL_BOTH);
		//gd.horizontalIndent= convertWidthInCharsToPixels(4);
		gd.widthHint= convertWidthInCharsToPixels(65);
		gd.heightHint= convertHeightInCharsToPixels(20);
		fTypeList.setLayoutData(gd);
		
		fCaughtBox= new Button(parent, SWT.CHECK);
		fCaughtBox.setLayoutData(new GridData());
		fCaughtBox.setText("Caught");
		fCaughtBox.setSelection(fIsCaughtSelected);
		
		fUncaughtBox= new Button(parent, SWT.CHECK);
		fUncaughtBox.setLayoutData(new GridData());
		fUncaughtBox.setText("Unaught");
		fUncaughtBox.setSelection(fIsUncaughtSelected);				
		//fAddFromTextRadio.setSelection(true);
		//addFromTextSelected(true);
		addFromListSelected(true);
		return parent;
	}
	
	/*public void addFromTextSelected(boolean selected) {
		fExceptionName.setEnabled(selected);
		if (selected) {
			fExceptionName.addListener(SWT.Modify, fTextListener);
			validateExceptionName();
		} else
			fExceptionName.removeListener(SWT.Modify, fTextListener);
	}*/
	
	public void addFromListSelected(boolean selected) {
		fTypeList.setEnabled(selected);
		if (selected) {
			if (!fTypeListInitialized) {
				initializeTypeList();
			}
			fTypeList.addSelectionListener(fListListener);
			validateListSelection();
		} else
			fTypeList.removeSelectionListener(fListListener);
	}
	
	protected void okPressed() {
		//if (fAddFromListRadio.getSelection()) {
		//	fResult= fTypeList.getSelection().get(0);
		//} else {
		//	fResult= fExceptionName.getText();
		//
		TypeRef typeRef= (TypeRef)fTypeList.getSelection().get(0);
		IType resolvedType= null;
		try {
			resolvedType= typeRef.resolveType(SearchEngine.createWorkspaceScope());
		} catch (JavaModelException e) {
			updateStatus(e.getStatus());
			return;
		}
		fExceptionType= getExceptionType(resolvedType);
		if (fExceptionType == NO_EXCEPTION) {
			updateStatus(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, "The selected class is not a subclass of java.lang.Throwable", null));
			return;
		}
		fIsCaughtSelected= fCaughtBox.getSelection();
		fIsUncaughtSelected= fUncaughtBox.getSelection();
		fResult= resolvedType;				saveDialogSettings();		
		super.okPressed();
	}
	
	private int getExceptionType(final IType type) {
		final int[] result= new int[] { NO_EXCEPTION };
	
		BusyIndicatorRunnableContext context= new BusyIndicatorRunnableContext();
		IRunnableWithProgress runnable= new IRunnableWithProgress() {
			public void run(IProgressMonitor pm) {
				IType supertype= type;
				try {
					ITypeHierarchy hierarchy= type.newSupertypeHierarchy(pm);
					while (supertype != null) {
						if ("java.lang.Throwable".equals(supertype.getFullyQualifiedName())) {
							result[0]= CHECKED_EXCEPTION;
							return;
						}
						if ("java.lang.RuntimeException".equals(supertype.getFullyQualifiedName())) {
							result[0]= UNCHECKED_EXCEPTION;
							return;
						}
						if ("java.lang.Error".equals(supertype.getFullyQualifiedName())) {
							result[0]= UNCHECKED_EXCEPTION;
							return;
						}
						supertype= hierarchy.getSuperclass(supertype);
					}
				} catch (JavaModelException e) {
				}
			}
		};
		try {		
			context.run(false, false, runnable);
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
		}

		return result[0];
	}
	
	public Object getResult() {
		return fResult;
	}
	
	public int getExceptionType() {
		return fExceptionType;
	}
	
	public boolean isCaughtSelected() {
		return fIsCaughtSelected;
	}
	
	public boolean isUncaughtSelected() {
		return fIsUncaughtSelected;
	}
	
	public void initializeTypeList() {
		AllTypesSearchEngine searchEngine= new AllTypesSearchEngine(JavaPlugin.getWorkspace());
		int flags= IJavaElementSearchConstants.CONSIDER_BINARIES |
					IJavaElementSearchConstants.CONSIDER_CLASSES |
					IJavaElementSearchConstants.CONSIDER_EXTERNAL_JARS;
		List result= searchEngine.searchTypes(new ProgressMonitorDialog(getShell()), searchEngine.createWorkspaceScope(), flags);
		fTypeList.setElements(result, false);
		fTypeList.setFilter("*Exception*", true);
		fTypeListInitialized= true;
	}
	
	private void validateListSelection() {
		if (fTypeList.getSelection().size() == 1) {
			updateStatus(new Status(IStatus.OK, JavaPlugin.getPluginId(), 0, "", null));
		} else {
			updateStatus(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, "Selection is required", null));
		}
	}
	
	/*private void validateExceptionName() {
		String text= fExceptionName.getText();
		IStatus status= JavaConventions.validateJavaTypeName(text);
		updateStatus(status);
	}*/		private void initFromDialogSettings() {		IDialogSettings allSetttings= JavaPlugin.getDefault().getDialogSettings();		IDialogSettings section= allSetttings.getSection(DIALOG_SETTINGS);		if (section == null) {			section= allSetttings.addNewSection(DIALOG_SETTINGS);			section.put(SETTING_CAUGHT_CHECKED, true);			section.put(SETTING_UNCAUGHT_CHECKED, true);		}		fIsCaughtSelected= section.getBoolean(SETTING_CAUGHT_CHECKED);		fIsUncaughtSelected= section.getBoolean(SETTING_UNCAUGHT_CHECKED);	}		private void saveDialogSettings() {		IDialogSettings allSetttings= JavaPlugin.getDefault().getDialogSettings();		IDialogSettings section= allSetttings.getSection(DIALOG_SETTINGS);		// won't be null since we initialize it in the method above.		section.put(SETTING_CAUGHT_CHECKED, fIsCaughtSelected);		section.put(SETTING_UNCAUGHT_CHECKED, fIsUncaughtSelected);	}		public void create() {		super.create();		fTypeList.selectFilterText();		fTypeList.setFocus();	}
}
