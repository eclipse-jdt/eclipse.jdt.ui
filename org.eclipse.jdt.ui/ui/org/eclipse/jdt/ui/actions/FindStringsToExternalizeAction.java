/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.corext.refactoring.nls.NLSElement;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSLine;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSScanner;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.ui.actions.ActionMessages;

import org.eclipse.jdt.internal.ui.refactoring.actions.ListDialog;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.viewsupport.ListContentProvider;

/**
 * Find all strings in a package or project that are not externalized yet.
 * <p>
 * The action is applicable to selections containing projects or packages.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 * @since 2.0
 */
public class FindStringsToExternalizeAction extends SelectionDispatchAction {

	private NonNLSElement[] fElements;
	
	/**
	 * Creates a new <code>FindStringsToExternalizeAction</code>. The action 
	 * requires that the selection provided by the site's selection provider is of type 
	 * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public FindStringsToExternalizeAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.getString("FindStringsToExternalizeAction.label")); //$NON-NLS-1$
		fElements= new NonNLSElement[0];
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.FIND_STRINGS_TO_EXTERNALIZE_ACTION);
	}
		
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(computeEnablementState(selection));
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			setEnabled(false);//no ui - happens on selection changes
		}
	}
	
	private boolean computeEnablementState(IStructuredSelection selection) throws JavaModelException {
		if (selection.isEmpty())
			return false;
		for (Iterator iter= selection.iterator(); iter.hasNext();) {
			Object element= iter.next();
			if (!(element instanceof IJavaElement))
				return false;
			IJavaElement javaElement= (IJavaElement)element;
			if (! javaElement.exists() || javaElement.isReadOnly())
				return false;
			int elementType= javaElement.getElementType();
			if (elementType != IJavaElement.PACKAGE_FRAGMENT && 
				elementType != IJavaElement.PACKAGE_FRAGMENT_ROOT &&
				elementType != IJavaElement.JAVA_PROJECT)
				return false;
			if (elementType == IJavaElement.PACKAGE_FRAGMENT_ROOT){
				IPackageFragmentRoot root= (IPackageFragmentRoot)javaElement;
				if (root.isExternal() || ReorgUtils.isClassFolder(root))
					return false;
			}
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(final IStructuredSelection selection) {
		try {
			PlatformUI.getWorkbench().getProgressService().run(true, true, createRunnable(selection));
		} catch(InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(), 
				ActionMessages.getString("FindStringsToExternalizeAction.dialog.title"), //$NON-NLS-1$
				ActionMessages.getString("FindStringsToExternalizeAction.error.message")); //$NON-NLS-1$
			return;
		} catch(InterruptedException e) {
			//ok
			return;
		}
		showResults();
	}
	
	private IRunnableWithProgress createRunnable(final IStructuredSelection selection) {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor pm) {
				fElements= doRun(selection, pm);
			}
		};
	}

	private NonNLSElement[] doRun(IStructuredSelection selection, IProgressMonitor pm) {
		List elements= getSelectedElementList(selection);
		if (elements == null || elements.isEmpty())
			return new NonNLSElement[0];

		pm.beginTask(ActionMessages.getString("FindStringsToExternalizeAction.find_strings"), elements.size()); //$NON-NLS-1$
					
		try{
			List l= new ArrayList();	
			for (Iterator iter= elements.iterator(); iter.hasNext();) {
				IJavaElement element= (IJavaElement) iter.next();
				if (element.getElementType() == IJavaElement.PACKAGE_FRAGMENT)
					l.addAll(analyze((IPackageFragment) element, new SubProgressMonitor(pm, 1)));
				else if (element.getElementType() == IJavaElement.PACKAGE_FRAGMENT_ROOT)
					l.addAll(analyze((IPackageFragmentRoot) element, new SubProgressMonitor(pm, 1)));
				if (element.getElementType() == IJavaElement.JAVA_PROJECT)
					l.addAll(analyze((IJavaProject) element, new SubProgressMonitor(pm, 1)));
			}
			return (NonNLSElement[]) l.toArray(new NonNLSElement[l.size()]);
		} catch(JavaModelException e) {
			ExceptionHandler.handle(e, 
				getDialogTitle(),
				ActionMessages.getString("FindStringsToExternalizeAction.error.message")); //$NON-NLS-1$
			return new NonNLSElement[0];	
		} finally{
			pm.done();
		}
	}
	
	private void showResults() {
		if (noStrings())
			MessageDialog.openInformation(getShell(), 
				getDialogTitle(),
				ActionMessages.getString("FindStringsToExternalizeAction.noStrings")); //$NON-NLS-1$
		else	
			new NonNLSListDialog(getShell(), fElements, countStrings()).open();
	}
	
	private boolean noStrings() {
		for (int i = 0; i < fElements.length; i++) {
			if (fElements[i].count != 0)
				return false;	
		}
		return true;
	}
	
	/*
	 * returns List of Strings
	 */
	private List analyze(IPackageFragment pack, IProgressMonitor pm) throws JavaModelException{
		try{
			if (pack == null)
				return new ArrayList(0);
				
			ICompilationUnit[] cus= pack.getCompilationUnits();
	
			pm.beginTask("", cus.length); //$NON-NLS-1$
			pm.setTaskName(pack.getElementName());
			
			List l= new ArrayList(cus.length);
			for (int i= 0; i < cus.length; i++){
				pm.subTask(cus[i].getElementName());
				NonNLSElement element = analyze(cus[i]);
				if (element != null)
					l.add(element);
				pm.worked(1);
				if (pm.isCanceled())
					throw new OperationCanceledException();
			}	
			return l;					
		} finally {
			pm.done();
		}	
	}

	/*
	 * returns List of Strings
	 */	
	private List analyze(IPackageFragmentRoot sourceFolder, IProgressMonitor pm) throws JavaModelException{
		try{
			IJavaElement[] children= sourceFolder.getChildren();
			pm.beginTask("", children.length); //$NON-NLS-1$
			pm.setTaskName(sourceFolder.getElementName());
			List result= new ArrayList();
			for (int i= 0; i < children.length; i++) {
				IJavaElement iJavaElement= children[i];
				if (iJavaElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT){
					IPackageFragment pack= (IPackageFragment)iJavaElement;
					if (! pack.isReadOnly())
						result.addAll(analyze(pack, new SubProgressMonitor(pm, 1)));
					else
						pm.worked(1);	
				} else	
					pm.worked(1);
			}
			return result;
		} finally{
			pm.done();
		}	
	}
	
	/*
	 * returns List of Strings
	 */
	private List analyze(IJavaProject project, IProgressMonitor pm) throws JavaModelException{
		try{
			IPackageFragment[] packs= project.getPackageFragments();
			pm.beginTask("", packs.length); //$NON-NLS-1$
			List result= new ArrayList();
			for (int i= 0; i < packs.length; i++) {
				if (! packs[i].isReadOnly())
					result.addAll(analyze(packs[i], new SubProgressMonitor(pm, 1)));
				else 
					pm.worked(1);	
			}
			return result;		
		} finally{
			pm.done();
		}	
	}
	
	private int countStrings(){
		int found= 0;
		for (int i= 0; i < fElements.length; i++)
			found += fElements[i].count;
		return found;			
	} 
	
	private NonNLSElement analyze(ICompilationUnit cu) {
		int count = countNonExternalizedStrings(cu);
		if (count == 0)
			return null;
		else	
			return new NonNLSElement(cu, count);
	}
	
	private int countNonExternalizedStrings(ICompilationUnit cu){
		try{
			NLSLine[] lines= NLSScanner.scan(cu);
			int result= 0;
			for (int i= 0; i < lines.length; i++) {
				result += countNonExternalizedStrings(lines[i]);
			}
			return result;
		}catch(JavaModelException e) {
			ExceptionHandler.handle(e, 
				getDialogTitle(),
				ActionMessages.getString("FindStringsToExternalizeAction.error.message")); //$NON-NLS-1$
			return 0;
		}catch(InvalidInputException iie) {
			JavaPlugin.log(iie);
			return 0;
		}	
	}

	private int countNonExternalizedStrings(NLSLine line){
		int result= 0;
		NLSElement[] elements= line.getElements();
		for (int i= 0; i < elements.length; i++){
			if (! elements[i].hasTag())
				result++;
		}
		return result;
	}

	/**
	 * returns <code>List</code> of <code>IPackageFragments</code>,  <code>IPackageFragmentRoots</code> or 
	 * <code>IJavaProjects</code> (all entries are of the same kind)
	 */
	private static List getSelectedElementList(IStructuredSelection selection) {
		if (selection == null)
			return null;
			
		return selection.toList();
	}
			
	//-------private classes --------------
		
	private static class NonNLSListDialog extends ListDialog {
		
		private static final int OPEN_BUTTON_ID= IDialogConstants.CLIENT_ID + 1;
		
		private Button fOpenButton;
		
		NonNLSListDialog(Shell parent, NonNLSElement[] input, int count) {
			super(parent);
			setInput(Arrays.asList(input));
			setTitle(ActionMessages.getString("FindStringsToExternalizeAction.dialog.title"));  //$NON-NLS-1$
			setMessage(ActionMessages.getFormattedString("FindStringsToExternalizeAction.non_externalized", new Object[] {new Integer(count)} )); //$NON-NLS-1$
			setContentProvider(new ListContentProvider());
			setLabelProvider(createLabelProvider());
		}

		public void create() {
			setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN);
			super.create();
		}

		protected Point getInitialSize() {
			return getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		}

		protected Control createDialogArea(Composite parent) {
			Composite result= (Composite)super.createDialogArea(parent);
			getTableViewer().addSelectionChangedListener(new ISelectionChangedListener(){
				public void selectionChanged(SelectionChangedEvent event){
					if (fOpenButton != null){
						fOpenButton.setEnabled(! getTableViewer().getSelection().isEmpty());
					}
				}
			});
			getTableViewer().getTable().addSelectionListener(new SelectionAdapter(){
				public void widgetDefaultSelected(SelectionEvent e) {
					NonNLSElement element= (NonNLSElement)e.item.getData();
					openWizard(element.cu);
				}
			});
			getTableViewer().getTable().setFocus();
			applyDialogFont(result);		
			return result;
		}
		
		protected void createButtonsForButtonBar(Composite parent) {
			fOpenButton= createButton(parent, OPEN_BUTTON_ID, ActionMessages.getString("FindStringsToExternalizeAction.button.label"), true); //$NON-NLS-1$
			fOpenButton.setEnabled(false);
			
			//looks like a 'close' but it a 'cancel'
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		}

		protected void buttonPressed(int buttonId) {
			if (buttonId != OPEN_BUTTON_ID){
				super.buttonPressed(buttonId);
				return;
			}	
			ISelection s= getTableViewer().getSelection();
			if (s instanceof IStructuredSelection){
				IStructuredSelection ss= (IStructuredSelection)s;
				if (ss.getFirstElement() instanceof NonNLSElement)
					openWizard(((NonNLSElement)ss.getFirstElement()).cu);
			}
		}

		private void openWizard(ICompilationUnit unit) {
			try {
				ExternalizeStringsAction.openExternalizeStringsWizard(getShell(), unit);
			} catch (JavaModelException e) {
				ExceptionHandler.handle(e, 
					ActionMessages.getString("FindStringsToExternalizeAction.dialog.title"), //$NON-NLS-1$
					ActionMessages.getString("FindStringsToExternalizeAction.error.message")); //$NON-NLS-1$
			}
		}
		
		private static LabelProvider createLabelProvider() {
			return new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT){ 
				public String getText(Object element) {
					NonNLSElement nlsel= (NonNLSElement)element;
					String elementName= ResourceUtil.getResource(nlsel.cu).getFullPath().toString();
					return ActionMessages.getFormattedString(
						"FindStringsToExternalizeAction.foundStrings", //$NON-NLS-1$
						new Object[] {new Integer(nlsel.count), elementName} );
				}		
				public Image getImage(Object element) {
					return super.getImage(((NonNLSElement)element).cu);
				}
			};
		}
		
		/*
		 * @see org.eclipse.jface.window.Window#configureShell(Shell)
		 */
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.NONNLS_DIALOG);		
		}


	}
		
	private static class NonNLSElement{
		ICompilationUnit cu;
		int count;
		NonNLSElement(ICompilationUnit cu, int count){
			this.cu= cu;
			this.count= count;
		}
	}
	
	private String getDialogTitle() {
		return ActionMessages.getString("FindStringsToExternalizeAction.dialog.title"); //$NON-NLS-1$
	}	
}
