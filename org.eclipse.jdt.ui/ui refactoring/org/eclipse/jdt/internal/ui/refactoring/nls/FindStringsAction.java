/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.nls;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.compiler.parser.InvalidInputException;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.core.refactoring.nls.NLSElement;
import org.eclipse.jdt.internal.core.refactoring.nls.NLSLine;
import org.eclipse.jdt.internal.core.refactoring.nls.NLSScanner;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.viewsupport.ListContentProvider;

public class FindStringsAction implements IWorkbenchWindowActionDelegate {

	/*
	 * @see IWorkbenchWindowActionDelegate#dispose()
	 */
	public void dispose() {
	}

	/*
	 * @see IWorkbenchWindowActionDelegate#init(IWorkbenchWindow)
	 */
	public void init(IWorkbenchWindow window) {
	}

	/*
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		Iterator iter= getPackageFragments(getSelection());
		if (iter == null)
			return;
		
		try{
			List l= new ArrayList();	
			while (iter.hasNext()){
				l.addAll(analyze((IPackageFragment)iter.next()));
			}
			showResults(l);
		}catch(JavaModelException e) {
			ExceptionHandler.handle(e, Messages.getString("FindStringsAction.title"), Messages.getString("FindStringsAction.see_log")); //$NON-NLS-2$ //$NON-NLS-1$
		}
	}
	
	private void showResults(List l){
		new NonNLSListDialog(l, countStrings(l)).open();
	}
	
	/*
	 * returns List of Strings
	 */
	private List analyze(IPackageFragment pack) throws JavaModelException{
		if (pack == null)
			return new ArrayList(0);
			
		ICompilationUnit[] cus= pack.getCompilationUnits();
		
		List l= new ArrayList(cus.length);
		for (int i= 0; i < cus.length; i++)
			l.add(analyze(cus[i]));
		return l;					
	}
	
	
	/*
	 * @param List of NonNLSElements
	 */
	private static int countStrings(List elements){
		int i= 0;
		for (Iterator iter= elements.iterator(); iter.hasNext(); )
			i += (((NonNLSElement)iter.next()).count);
		return i;	
	} 
	
	private NonNLSElement analyze(ICompilationUnit cu) throws JavaModelException{
		return new NonNLSElement(cu, countNotExternalizedStrings(cu));
	}
	
	private int countNotExternalizedStrings(ICompilationUnit cu){
		try{
			List l= NLSScanner.scan(cu);
						
			int result= 0;
			for (Iterator iter= l.iterator(); iter.hasNext();){
				result += countNotExternalizedStrings((NLSLine)iter.next());
			}
			return result;
		}catch(JavaModelException e) {
			ExceptionHandler.handle(e, Messages.getString("FindStringsAction.title"), Messages.getString("FindStringsAction.see_log")); //$NON-NLS-2$ //$NON-NLS-1$
			return 0;
		}catch(InvalidInputException iie) {
			JavaPlugin.log(iie);
			return 0;
		}	
	}

	private int countNotExternalizedStrings(NLSLine line){
		int result= 0;
		NLSElement[] elements= line.getElements();
		for (int i= 0; i < elements.length; i++){
			if (! elements[i].hasTag())
				result++;
		}
		return result;
	}

	/*
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

	protected IStructuredSelection getSelection() {
		IWorkbenchWindow window= JavaPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
		if (window == null)
			return null;
		ISelection selection= window.getSelectionService().getSelection();
		if (selection instanceof IStructuredSelection)
			return (IStructuredSelection) selection;
		return null;
	}
	
	/**
	 * returns Iterator over IPackageFragments
	 */
	protected Iterator getPackageFragments(IStructuredSelection selection) {
		if (selection == null)
			return null;
			
		return selection.iterator();
	}
		
	private static LabelProvider createLabelProvider(){
		return new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT){ 
			public String getText(Object element) {
				NonNLSElement nlsel= (NonNLSElement)element;
				String res= nlsel.count + Messages.getString("FindStringsAction.in"); //$NON-NLS-1$
				try{
					return res + Refactoring.getResource(nlsel.cu).getProjectRelativePath();
				}catch (JavaModelException e){
					//show at least this
					return res + nlsel.cu.getElementName();
				}	
			}
			
			public Image getImage(Object element) {
				return super.getImage(((NonNLSElement)element).cu);
			}
		};
	}
		
	//-------private classes --------------
		
	private static class NonNLSListDialog extends ListDialog{
		private Button fCheckbox;
		
		NonNLSListDialog(Object input, int count){
			super(JavaPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell());
			setInput(input);
			setTitle(Messages.getString("FindStringsAction.NLS_Tool"));  //$NON-NLS-1$
			setMessage(count + Messages.getString("FindStringsAction.not_externalized")); //$NON-NLS-1$, 
			setContentProvider(new ListContentProvider());
			setLabelProvider(createLabelProvider());
		}

		public void create() {
			setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN);
			super.create();
		}

		protected Point getInitialSize() {
			return getShell().computeSize(400, SWT.DEFAULT, true);
		}

		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		}	

		protected Control createDialogArea(Composite parent) {
			Composite result= (Composite)super.createDialogArea(parent);
			fCheckbox= new Button(result, SWT.CHECK);
			fCheckbox.setText("Show compilation units with no strings to externalize");
			fCheckbox.setSelection(true);
			fCheckbox.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					boolean show= NonNLSListDialog.this.fCheckbox.getSelection();
					if  (show && NonNLSListDialog.this.hasFilters())
						NonNLSListDialog.this.getTableViewer().resetFilters();
					else if (! show && ! NonNLSListDialog.this.hasFilters())	
						NonNLSListDialog.this.getTableViewer().addFilter(new ZeroStringsFilter());
				}
			});
			return result;
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
	
	private static class ZeroStringsFilter extends ViewerFilter{
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			return ((NonNLSElement)element).count != 0;
		}
	}
}
