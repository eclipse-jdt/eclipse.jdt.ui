/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */package org.eclipse.jdt.internal.ui.nls;import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.compiler.parser.InvalidInputException;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.nls.model.NLSElement;
import org.eclipse.jdt.internal.ui.nls.model.NLSLine;
import org.eclipse.jdt.internal.ui.nls.model.NLSScanner;
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
	public void run(IAction action) {		Iterator iter= getPackageFragments(getSelection());		if (iter == null)			return;				try{			List l= new ArrayList();				while (iter.hasNext()){				l.addAll(analyze((IPackageFragment)iter.next()));			}			showResults(l);		}catch(JavaModelException e) {			ExceptionHandler.handle(e, Messages.getString("FindStringsAction.Find_Strings_Action_1"), Messages.getString("FindStringsAction.Unexpected_Exception._See_log._2")); //$NON-NLS-2$ //$NON-NLS-1$		}	}		private void showResults(List l){		NonNLSListDialog d= new NonNLSListDialog(l, countStrings(l));		int res= d.open();	}		/*	 * returns List of Strings	 */	private List analyze(IPackageFragment pack) throws JavaModelException{		if (pack == null)			return new ArrayList(0);					ICompilationUnit[] cus= pack.getCompilationUnits();				List l= new ArrayList(cus.length);		for (int i= 0; i < cus.length; i++)			l.add(analyze(cus[i]));		return l;						}			/*	 * @param List of NonNLSElements	 */	private static int countStrings(List elements){		int i= 0;		for (Iterator iter= elements.iterator(); iter.hasNext(); )			i += (((NonNLSElement)iter.next()).count);		return i;		} 		private NonNLSElement analyze(ICompilationUnit cu) throws JavaModelException{		return new NonNLSElement(cu, countNotExternalizedStrings(cu));	}		private int countNotExternalizedStrings(ICompilationUnit cu){		try{			List l= NLSScanner.scan(cu);									int result= 0;			for (Iterator iter= l.iterator(); iter.hasNext();){				result += countNotExternalizedStrings((NLSLine)iter.next());			}			return result;		}catch(JavaModelException e) {			ExceptionHandler.handle(e, Messages.getString("FindStringsAction.Find_Strings_Action_6"), Messages.getString("FindStringsAction.Unexpected_Exception._See_log._7")); //$NON-NLS-2$ //$NON-NLS-1$			return 0;		}catch(InvalidInputException iie) {			JavaPlugin.log(iie);			return 0;		}		}
	private int countNotExternalizedStrings(NLSLine line){		int result= 0;		NLSElement[] elements= line.getElements();		for (int i= 0; i < elements.length; i++){			if (! elements[i].hasTag())				result++;		}		return result;	}
	/*
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}
	protected IStructuredSelection getSelection() {		IWorkbenchWindow window= JavaPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();		if (window == null)			return null;		ISelection selection= window.getSelectionService().getSelection();		if (selection instanceof IStructuredSelection)			return (IStructuredSelection) selection;		return null;	}		/**	 * returns Iterator over IPackageFragments	 */	protected Iterator getPackageFragments(IStructuredSelection selection) {		if (selection == null)			return null;					return selection.iterator();	}			private static LabelProvider createLabelProvider(){		return new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT){ 			public String getText(Object element) {				NonNLSElement nlsel= (NonNLSElement)element;				String res= nlsel.count + Messages.getString("FindStringsAction._in__5"); //$NON-NLS-1$				try{					return res + Refactoring.getResource(nlsel.cu).getProjectRelativePath();				}catch (JavaModelException e){					//show at least this					return res + nlsel.cu.getElementName();				}				}						public Image getImage(Object element) {				return super.getImage(((NonNLSElement)element).cu);			}		};	}			//-------private classes --------------			private static class NonNLSListDialog extends ListDialog{		NonNLSListDialog(Object input, int count){			super(JavaPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(), 					input, 					Messages.getString("FindStringsAction.NLS_Plugin_3"), //$NON-NLS-1$, 					count + Messages.getString("FindStringsAction.Not_Externalized_Strings_4"), //$NON-NLS-1$, 					new ListContentProvider(), createLabelProvider());		}		public void create() {			setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN);			super.create();		}		protected Point getInitialSize() {			return getShell().computeSize(400, SWT.DEFAULT, true);		}	}		private static class NonNLSElement{		ICompilationUnit cu;		int count;		NonNLSElement(ICompilationUnit cu, int count){			this.cu= cu;			this.count= count;		}	}}
