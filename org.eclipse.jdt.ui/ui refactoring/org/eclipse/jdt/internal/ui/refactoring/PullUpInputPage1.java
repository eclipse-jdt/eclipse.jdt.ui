package org.eclipse.jdt.internal.ui.refactoring;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoring;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class PullUpInputPage1 extends UserInputWizardPage {
	
	public static final String PAGE_NAME= "PullUpMethodsInputPage1"; //$NON-NLS-1$
	private static final int ROW_COUNT= 5;
	private CheckboxTableViewer fTableViewer;
	private Combo fSuperclassCombo;
	
	public PullUpInputPage1() {
		super(PAGE_NAME, false);
		setMessage("Select the members to pull up and the desired new declaring class for them.\n" +							"If you select methods, them press Next to specify which matching methods you wish to delete.");
	}

	public void createControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout gl= new GridLayout();
		gl.numColumns= 2;
		composite.setLayout(gl);

		createSuperTypeCombo(composite);
		createSpacer(composite);
		createMemberTableLabel(composite);
		createMemberTableComposite(composite);
				
		setControl(composite);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.PULL_UP_WIZARD_PAGE);			
	}
	
	private void createSpacer(Composite parent) {
		Label label= new Label(parent, SWT.NONE) ;
		GridData gd0= new GridData();
		gd0.horizontalSpan= 2;
		label.setLayoutData(gd0);
	}
	
	private PullUpRefactoring getPullUpRefactoring(){
		return (PullUpRefactoring)getRefactoring();
	}

	private void createSuperTypeCombo(final Composite parent) {
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(false, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor pm) throws InvocationTargetException {
					try {
						createSuperTypeCombo(pm, parent);						
					} catch (JavaModelException e) {
						throw new InvocationTargetException(e);
					} finally {
						pm.done();
					}
				}
			});
		} catch(InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(), RefactoringMessages.getString("PullUpInputPage.pull_Up"), RefactoringMessages.getString("PullUpInputPage.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch(InterruptedException e) {
			Assert.isTrue(false);//not cancellable
		}
	}
	private void createSuperTypeCombo(IProgressMonitor pm, Composite parent) throws JavaModelException {
		Label label= new Label(parent, SWT.NONE) ;
		label.setText("&Select destination class:");
		label.setLayoutData(new GridData());
		
		fSuperclassCombo= new Combo(parent, SWT.READ_ONLY);
		IType[] superclasses= getPullUpRefactoring().getPossibleSuperclasses(pm);
		Assert.isTrue(superclasses.length > 0);
		for (int i= 0; i < superclasses.length; i++) {
			fSuperclassCombo.add(createComboLabel(superclasses[i]));
		}
		fSuperclassCombo.setText(createComboLabel(superclasses[superclasses.length - 1]));
		fSuperclassCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	private static String createComboLabel(IType superclass) {
		return JavaModelUtil.getFullyQualifiedName(superclass);
	}

	private void createMemberTableComposite(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan= 2;
		composite.setLayoutData(gd);
		GridLayout gl= new GridLayout();
		gl.numColumns= 2;
		gl.marginWidth= 0;
		gl.marginHeight= 0;
		composite.setLayout(gl);

		createMemberTable(composite);
		createButtonComposite(composite);
	}

	private void createMemberTableLabel(Composite parent) {
		Label label= new Label(parent, SWT.NONE) ;
		label.setText("Select &member(s) to pull up:");
		GridData gd0= new GridData();
		gd0.horizontalSpan= 2;
		label.setLayoutData(gd0);
	}
	
	private void createButtonComposite(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		GridLayout gl= new GridLayout();
		gl.marginHeight= 0;
		gl.marginWidth= 0;
		composite.setLayout(gl);
		
		createSelectButton(composite, "Se&lect All", true);
		createSelectButton(composite, "&Deselect All", false);
	}
	
	private void createSelectButton(Composite composite, String label, final boolean select){
		Button button= new Button(composite, SWT.PUSH);
		button.setText(label);
		button.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent event) {
				fTableViewer.setAllChecked(select);
				dissallowIfNothingChecked();
			}
		});
	}

	private void createMemberTable(Composite parent) {
		final Table table= new Table(parent, SWT.CHECK | SWT.MULTI | SWT.BORDER);
		table.setHeaderVisible(false);
		table.setLinesVisible(true);
		
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		fTableViewer= new CheckboxTableViewer(table);
		fTableViewer.setUseHashlookup(true);
		fTableViewer.setContentProvider(createMemberContentProvider());
		fTableViewer.setLabelProvider(new JavaElementLabelProvider());
		
		fTableViewer.setInput(getPullUpRefactoring().getPullableMembersOfDeclaringType());
		fTableViewer.setCheckedElements(getPullUpRefactoring().getElementsToPullUp());
		fTableViewer.addCheckStateListener(new ICheckStateListener(){
			public void checkStateChanged(CheckStateChangedEvent event) {
				dissallowIfNothingChecked();
			}
		});
	}

	private void dissallowIfNothingChecked() {
		if (fTableViewer.getCheckedElements().length == 0){
			setErrorMessage("Select member(s) to pull up");
			setPageComplete(false);
		} else {
			setErrorMessage(null);
			setPageComplete(true);
		}
	}
		
	private IStructuredContentProvider createMemberContentProvider() {
		return new IStructuredContentProvider(){
			public void dispose() {
			}
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
			public Object[] getElements(Object inputElement) {
				return (Object[])inputElement;
			}
		};
	}

	/*
	 * @see org.eclipse.jface.wizard.IWizardPage#getNextPage()
	 */
	public IWizardPage getNextPage() {
		initializeRefactoring();
		if (canSkipSecondInputPage())
			return getRefactoringWizard().computeUserInputSuccessorPage(this);
		else 
			return super.getNextPage();
	}
	
	/*
	 * @see org.eclipse.jface.wizard.IWizardPage#canFlipToNextPage()
	 */
	public boolean canFlipToNextPage() {
		if (canSkipSecondInputPage())
		    //cannot call super here because it tries to compute successor page, which is expensive
			return isPageComplete();
		else
			return super.canFlipToNextPage();
	}

	private boolean canSkipSecondInputPage() {
		return getCheckedMethods().length == 0;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardPage#performFinish()
	 */
	protected boolean performFinish() {
		initializeRefactoring();
		getPullUpRefactoring().setMethodsToDelete(getCheckedMethods());
		return super.performFinish();
	}
	
	private void initializeRefactoring() {
		getPullUpRefactoring().setElementsToPullUp(getCheckedMembers());
	}

	private IMethod[] getCheckedMethods() {
		Object[] checkedElements= fTableViewer.getCheckedElements();
		List list= new ArrayList(checkedElements.length);
		for (int i= 0; i < checkedElements.length; i++) {
			if (checkedElements[i] instanceof IMethod)
				list.add(checkedElements[i]);
		}
		return (IMethod[]) list.toArray(new IMethod[list.size()]);
	}
	
	private IMember[] getCheckedMembers(){
		List checked= Arrays.asList(fTableViewer.getCheckedElements());
		return (IMember[]) checked.toArray(new IMember[checked.size()]);
	}
}
