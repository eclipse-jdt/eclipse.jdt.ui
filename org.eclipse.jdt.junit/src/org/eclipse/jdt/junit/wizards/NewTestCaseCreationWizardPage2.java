/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.junit.wizards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.viewsupport.ContainerCheckedTreeViewer;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * Wizard page to select the methods from a class under test.
 */
public class NewTestCaseCreationWizardPage2 extends WizardPage {

	private final static String PAGE_NAME= "NewTestCaseCreationWizardPage2"; //$NON-NLS-1$
	public final static String PREFIX= "test"; //$NON-NLS-1$

	private NewTestCaseCreationWizardPage fFirstPage;	
	private IType fClassToTest;

	private Button fCreateTasksButton;
	private ContainerCheckedTreeViewer fMethodsTree;
	private Button fSelectAllButton;
	private Button fDeselectAllButton;
	private Label fSelectedMethodsLabel;

	/**
	 * Constructor for NewTestCaseCreationWizardPage2.
	 */
	protected NewTestCaseCreationWizardPage2(NewTestCaseCreationWizardPage firstPage) {
		super(PAGE_NAME);
		fFirstPage= firstPage;
		setTitle(Messages.getString("NewTestClassWizPage2.title")); //$NON-NLS-1$
		setDescription(Messages.getString("NewTestClassWizPage2.description")); //$NON-NLS-1$
	}

	/*
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite container= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		container.setLayout(layout);

		createMethodsTreeControls(container);
		createSpacer(container);
		createTasksControls(container);
		setControl(container);
	}

	protected void createTasksControls(Composite container) {
		GridLayout layout;
		GridData gd;
		Composite prefixContainer= new Composite(container, SWT.NONE);
		gd= new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.horizontalSpan = 2;
		prefixContainer.setLayoutData(gd);
		
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		prefixContainer.setLayout(layout);
		
		fCreateTasksButton= new Button(prefixContainer, SWT.CHECK | SWT.LEFT);
		fCreateTasksButton.setText(Messages.getString("NewTestClassWizPage2.create_tasks.text")); //$NON-NLS-1$
		fCreateTasksButton.setEnabled(true);
		fCreateTasksButton.setSelection(true);
		gd= new GridData();
		gd.horizontalAlignment= GridData.FILL;
		gd.horizontalSpan= 2;
		fCreateTasksButton.setLayoutData(gd);
	}

	protected void createMethodsTreeControls(Composite container) {
		Label label= new Label(container, SWT.LEFT | SWT.WRAP);
		label.setFont(container.getFont());
		label.setText(Messages.getString("NewTestClassWizPage2.methods_tree.label")); //$NON-NLS-1$
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		fMethodsTree= new ContainerCheckedTreeViewer(container, SWT.BORDER);
		gd= new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
		gd.heightHint= 180;
		fMethodsTree.getTree().setLayoutData(gd);

		fMethodsTree.setLabelProvider(new JavaElementLabelProvider());
		fMethodsTree.setAutoExpandLevel(2);			
		fMethodsTree.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				updateSelectedMethodsLabel();
			}	
		});

		Composite buttonContainer= new Composite(container, SWT.NONE);
		gd= new GridData(GridData.FILL_VERTICAL);
		buttonContainer.setLayoutData(gd);
		GridLayout buttonLayout= new GridLayout();
		buttonLayout.marginWidth= 0;
		buttonLayout.marginHeight= 0;
		buttonContainer.setLayout(buttonLayout);

		fSelectAllButton= new Button(buttonContainer, SWT.PUSH);
		fSelectAllButton.setText(Messages.getString("NewTestClassWizPage2.selectAll")); //$NON-NLS-1$
		gd= new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		fSelectAllButton.setLayoutData(gd);
		fSelectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fMethodsTree.setCheckedElements((Object[]) fMethodsTree.getInput());
				updateSelectedMethodsLabel();
			}
		});

		fDeselectAllButton= new Button(buttonContainer, SWT.PUSH);
		fDeselectAllButton.setText(Messages.getString("NewTestClassWizPage2.deselectAll")); //$NON-NLS-1$
		gd= new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		fDeselectAllButton.setLayoutData(gd);
		fDeselectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fMethodsTree.setCheckedElements(new Object[0]);
				updateSelectedMethodsLabel();
			}
		});

		/* No of selected methods label */
		fSelectedMethodsLabel= new Label(container, SWT.LEFT);
		fSelectedMethodsLabel.setFont(container.getFont());
		updateSelectedMethodsLabel();
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= 1;
		fSelectedMethodsLabel.setLayoutData(gd);
		
		Label emptyLabel= new Label(container, SWT.LEFT);
		gd= new GridData();
		gd.horizontalSpan= 1;
		emptyLabel.setLayoutData(gd);
	}

	protected void createSpacer(Composite container) {
		Label spacer= new Label(container, SWT.NONE);
		GridData data= new GridData();
		data.horizontalSpan= 2;
		data.horizontalAlignment= GridData.FILL;
		data.verticalAlignment= GridData.BEGINNING;
		data.heightHint= 4;
		spacer.setLayoutData(data);
	}

	public void setVisible(boolean visible) {
		if (visible) {
			fClassToTest= fFirstPage.getClassToTest();
			IType currType= fClassToTest;
			ArrayList types= null;
			try {
				ITypeHierarchy hierarchy= currType.newSupertypeHierarchy(null);
				IType[] superTypes= hierarchy.getAllSuperclasses(currType);
				types= new ArrayList(superTypes.length+1);
				types.add(currType);
				types.addAll(Arrays.asList(superTypes));
			} catch(JavaModelException e) {
			}
			fMethodsTree.setContentProvider(new MethodsTreeContentProvider(types.toArray()));
			if (types == null)
				types= new ArrayList();
			fMethodsTree.setInput(types.toArray());
			fMethodsTree.setSelection(new StructuredSelection(currType), true);
		}
		updateSelectedMethodsLabel();
		super.setVisible(visible);
	}

	public IMethod[] getCheckedMethods() {
		Object[] checkedObjects= fMethodsTree.getCheckedElements();
		int methodCount= 0;
		for (int i = 0; i < checkedObjects.length; i++) {
			if (checkedObjects[i] instanceof IMethod)
				methodCount++;
		}
		IMethod[] checkedMethods= new IMethod[methodCount];
		int j= 0;
		for (int i = 0; i < checkedObjects.length; i++) {
			if (checkedObjects[i] instanceof IMethod) {
				checkedMethods[j]= (IMethod)checkedObjects[i];
				j++;
			}
		}
		return checkedMethods;
	}
	
	private static class MethodsTreeContentProvider implements ITreeContentProvider {
		private Object[] fTypes;
		private IMethod[] fMethods;
		private final Object[] fEmpty= new Object[0];

		public MethodsTreeContentProvider(Object[] types) {
			fTypes= types;
			Vector methods= new Vector();
			for (int i = types.length-1; i > -1; i--) {
				Object object = types[i];
				if (object instanceof IType) {
					IType type = (IType) object;
					try {
						IMethod[] currMethods= type.getMethods();
						for_currMethods:
						for (int j = 0; j < currMethods.length; j++) {
							IMethod currMethod = currMethods[j];
							int flags= currMethod.getFlags();
							if (!Flags.isPrivate(flags)) {
								for (int k = 0; k < methods.size(); k++) {
									IMethod m= ((IMethod)methods.get(k));
									if (m.getElementName().equals(currMethod.getElementName())
										&& m.getSignature().equals(currMethod.getSignature())) {
										methods.set(k,currMethod);
										continue for_currMethods;
									}
								}
								methods.add(currMethod);
							}
						}
					} catch (JavaModelException e) {
						System.err.println("MethodsTreeContentProvider: "+e); //$NON-NLS-1$
					}
				}
			}
			fMethods= new IMethod[methods.size()];
			methods.copyInto(fMethods);
		}
		
		/*
		 * @see ITreeContentProvider#getChildren(Object)
		 */
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof IType) {
				IType parentType= (IType)parentElement;
				ArrayList result= new ArrayList(fMethods.length);
				for (int i= 0; i < fMethods.length; i++) {
					if (fMethods[i].getDeclaringType().equals(parentType)) {
						result.add(fMethods[i]);
					}
				}
				return result.toArray();
			}
			return fEmpty;
		}

		/*
		 * @see ITreeContentProvider#getParent(Object)
		 */
		public Object getParent(Object element) {
			if (element instanceof IMethod) 
				return ((IMethod)element).getDeclaringType();
			return null;
		}

		/*
		 * @see ITreeContentProvider#hasChildren(Object)
		 */
		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}

		/*
		 * @see IStructuredContentProvider#getElements(Object)
		 */
		public Object[] getElements(Object inputElement) {
			return fTypes;
		}

		/*
		 * @see IContentProvider#dispose()
		 */
		public void dispose() {
		}

		/*
		 * @see IContentProvider#inputChanged(Viewer, Object, Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
		
		public IMethod[] getAllMethods() {
			return fMethods;
		}
	}

	public boolean getCreateTasksButtonSelection() {
		return fCreateTasksButton.getSelection();
	}
	
	private void updateSelectedMethodsLabel() {
		Object[] checked= fMethodsTree.getCheckedElements();
		int checkedMethodCount= 0;
		for (int i= 0; i < checked.length; i++) {
			if (checked[i] instanceof IMethod)
				checkedMethodCount++;
		}
		fSelectedMethodsLabel.setText(Messages.getFormattedString(((checkedMethodCount==1)?"NewTestClassWizPage2.selected_methods.label_one":"NewTestClassWizPage2.selected_methods.label_many"), new Integer(checkedMethodCount))); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public IMethod[] getAllMethods() {
		return ((MethodsTreeContentProvider)fMethodsTree.getContentProvider()).getAllMethods();
	}
}