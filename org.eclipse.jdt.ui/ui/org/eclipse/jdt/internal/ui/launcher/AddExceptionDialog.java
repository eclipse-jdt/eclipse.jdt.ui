package org.eclipse.jdt.internal.ui.launcher;/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */ import java.lang.reflect.InvocationTargetException;import java.util.List;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.ITypeHierarchy;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.core.search.SearchEngine;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.dialogs.FilteredList;import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;import org.eclipse.jdt.internal.ui.util.AllTypesSearchEngine;import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;import org.eclipse.jdt.internal.ui.util.JavaModelUtil;import org.eclipse.jdt.internal.ui.util.TypeInfo;import org.eclipse.jdt.internal.ui.util.TypeInfoLabelProvider;import org.eclipse.jdt.ui.IJavaElementSearchConstants;import org.eclipse.jface.dialogs.IDialogSettings;import org.eclipse.jface.dialogs.ProgressMonitorDialog;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.swt.SWT;import org.eclipse.swt.events.SelectionAdapter;import org.eclipse.swt.events.SelectionEvent;import org.eclipse.swt.events.SelectionListener;import org.eclipse.swt.layout.GridData;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.Button;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Event;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Listener;import org.eclipse.swt.widgets.Shell;import org.eclipse.swt.widgets.Text;

/**
 * A dialog to select an exception type to add as an exception breakpoint.
 */
public class AddExceptionDialog extends StatusDialog {		private static final String DIALOG_SETTINGS= "AddExceptionDialog"; //$NON-NLS-1$	private static final String SETTING_CAUGHT_CHECKED= "caughtChecked"; //$NON-NLS-1$
	private static final String SETTING_UNCAUGHT_CHECKED= "uncaughtChecked"; //$NON-NLS-1$	private Text fFilterText;
	private FilteredList fTypeList;
	private boolean fTypeListInitialized= false;
	
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
		
	private Object fResult;
	private int fExceptionType= NO_EXCEPTION;
	private boolean fIsCaughtSelected= true;
	private boolean fIsUncaughtSelected= true;
	
	public AddExceptionDialog(Shell parentShell) {
		super(parentShell);
		setTitle(LauncherMessages.getString("AddExceptionDialog.title")); //$NON-NLS-1$
	}
	
	protected Control createDialogArea(Composite ancestor) {
		initFromDialogSettings();		Composite parent= new Composite(ancestor, SWT.NULL);
		GridLayout layout= new GridLayout();
		parent.setLayout(layout);		
		Label l= new Label(parent, SWT.NULL);
		l.setLayoutData(new GridData());
		l.setText(LauncherMessages.getString("AddExceptionDialog.message")); //$NON-NLS-1$
		fFilterText = new Text(parent, SWT.BORDER);				GridData data= new GridData();		data.grabExcessVerticalSpace= false;		data.grabExcessHorizontalSpace= true;		data.horizontalAlignment= GridData.FILL;		data.verticalAlignment= GridData.BEGINNING;		fFilterText.setLayoutData(data);						Listener listener= new Listener() {			public void handleEvent(Event e) {				fTypeList.setFilter(fFilterText.getText());			}		};				fFilterText.addListener(SWT.Modify, listener);										
		fTypeList= new FilteredList(parent, SWT.BORDER | SWT.SINGLE, 
				new TypeInfoLabelProvider(TypeInfoLabelProvider.SHOW_PACKAGE_POSTFIX),				true, true, true);		GridData gd= new GridData(GridData.FILL_BOTH);
		//gd.horizontalIndent= convertWidthInCharsToPixels(4);
		gd.widthHint= convertWidthInCharsToPixels(65);
		gd.heightHint= convertHeightInCharsToPixels(20);
		fTypeList.setLayoutData(gd);				
		
		fCaughtBox= new Button(parent, SWT.CHECK);
		fCaughtBox.setLayoutData(new GridData());
		fCaughtBox.setText(LauncherMessages.getString("AddExceptionDialog.caught")); //$NON-NLS-1$
		fCaughtBox.setSelection(fIsCaughtSelected);
		
		fUncaughtBox= new Button(parent, SWT.CHECK);
		fUncaughtBox.setLayoutData(new GridData());		// fix: 1GEUWCI: ITPDUI:Linux - Add Exception box has confusing checkbox
		fUncaughtBox.setText(LauncherMessages.getString("AddExceptionDialog.uncaught")); //$NON-NLS-1$		// end fix.
		fUncaughtBox.setSelection(fIsUncaughtSelected);				addFromListSelected(true);
		return parent;
	}
		
	protected void addFromListSelected(boolean selected) {
		fTypeList.setEnabled(selected);
		if (selected) {
			if (!fTypeListInitialized) {
				initializeTypeList();				if (!fTypeListInitialized) {					return; //cancelled				}
			}
			fTypeList.addSelectionListener(fListListener);
			validateListSelection();
		} else
			fTypeList.removeSelectionListener(fListListener);
	}
	
	protected void okPressed() {
		TypeInfo typeRef= (TypeInfo)fTypeList.getSelection()[0];		IType resolvedType= null;
		try {
			resolvedType= typeRef.resolveType(SearchEngine.createWorkspaceScope());
		} catch (JavaModelException e) {
			updateStatus(e.getStatus());
			return;
		}
		fExceptionType= getExceptionType(resolvedType);
		if (fExceptionType == NO_EXCEPTION) {			StatusInfo status= new StatusInfo();			status.setError(LauncherMessages.getString("AddExceptionDialog.error.notThrowable"));  //$NON-NLS-1$
			updateStatus(status);
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
				try {
					ITypeHierarchy hierarchy= type.newSupertypeHierarchy(pm);					IType curr= type;
					while (curr != null) {						String name= JavaModelUtil.getFullyQualifiedName(curr);						
						if ("java.lang.Throwable".equals(name)) { //$NON-NLS-1$
							result[0]= CHECKED_EXCEPTION;
							return;
						}
						if ("java.lang.RuntimeException".equals(name) || "java.lang.Error".equals(name)) { //$NON-NLS-2$ //$NON-NLS-1$
							result[0]= UNCHECKED_EXCEPTION;
							return;
						}
						curr= hierarchy.getSuperclass(curr);
					}
				} catch (JavaModelException e) {					JavaPlugin.log(e);
				}
			}
		};
		try {		
			context.run(false, false, runnable);
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {			JavaPlugin.log(e);
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
	
	protected void initializeTypeList() {
		AllTypesSearchEngine searchEngine= new AllTypesSearchEngine(JavaPlugin.getWorkspace());
		int flags= IJavaElementSearchConstants.CONSIDER_BINARIES |
					IJavaElementSearchConstants.CONSIDER_CLASSES |
					IJavaElementSearchConstants.CONSIDER_EXTERNAL_JARS;		ProgressMonitorDialog dialog= new ProgressMonitorDialog(getShell());		final List result= searchEngine.searchTypes(dialog, SearchEngine.createWorkspaceScope(), flags);		if (dialog.getReturnCode() == dialog.CANCEL) {			getShell().getDisplay().asyncExec( 				new Runnable() {					public void run() {						cancelPressed();					}				}			);			return;		}
		fFilterText.setText("*Exception*"); //$NON-NLS-1$				BusyIndicatorRunnableContext context= new BusyIndicatorRunnableContext();		IRunnableWithProgress runnable= new IRunnableWithProgress() {			public void run(IProgressMonitor pm) {				fTypeList.setElements(result.toArray()); // XXX inefficient			}		};		try {					context.run(false, false, runnable);		} catch (InterruptedException e) {		} catch (InvocationTargetException e) {			JavaPlugin.log(e);		}		
		fTypeListInitialized= true;
	}
	
	private void validateListSelection() {		StatusInfo status= new StatusInfo();
		if (fTypeList.getSelection().length != 1) {			status.setError(LauncherMessages.getString("AddExceptionDialog.error.noSelection"));  //$NON-NLS-1$		}		updateStatus(status);
	}
		private void initFromDialogSettings() {		IDialogSettings allSetttings= JavaPlugin.getDefault().getDialogSettings();		IDialogSettings section= allSetttings.getSection(DIALOG_SETTINGS);		if (section == null) {			section= allSetttings.addNewSection(DIALOG_SETTINGS);			section.put(SETTING_CAUGHT_CHECKED, true);			section.put(SETTING_UNCAUGHT_CHECKED, true);		}		fIsCaughtSelected= section.getBoolean(SETTING_CAUGHT_CHECKED);		fIsUncaughtSelected= section.getBoolean(SETTING_UNCAUGHT_CHECKED);	}		private void saveDialogSettings() {		IDialogSettings allSetttings= JavaPlugin.getDefault().getDialogSettings();		IDialogSettings section= allSetttings.getSection(DIALOG_SETTINGS);		// won't be null since we initialize it in the method above.		section.put(SETTING_CAUGHT_CHECKED, fIsCaughtSelected);		section.put(SETTING_UNCAUGHT_CHECKED, fIsUncaughtSelected);	}		public void create() {		super.create();		fFilterText.selectAll();		fFilterText.setFocus();	}
}
