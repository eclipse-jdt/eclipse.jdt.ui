package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.tagging.IPreactivatedRefactoring;
import org.eclipse.jdt.internal.ui.reorg.IRefactoringRenameSupport;
import org.eclipse.jdt.internal.ui.reorg.RefactoringSupportFactory;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;

public class RenameJavaElementAction extends Action implements IUpdate {
	
	private ITextEditor fEditor;
	
	public RenameJavaElementAction(ITextEditor editor){
		super("Rename...");
		Assert.isNotNull(editor);
		fEditor= editor;
	}

	/* non java-doc
	 * @see Action#run()
	 */
	public void run() {
		IJavaElement element=  JavaElementFinder.getJavaElement(getTextSelection());
		if (element == null || !element.exists()){
			beep();
			return;
		}	
		IRefactoringRenameSupport refactoringSupport= RefactoringSupportFactory.createRenameSupport(element);
		if (refactoringSupport == null){
			beep();
			return;
		}	
		if (! refactoringSupport.canRename(element)){
			beep();
			return;
		}	
		refactoringSupport.rename(element);
	}
	
	/* non java-doc
	 * @see IUpdate#update()
	 */
	public void update() {
		setEnabled(canOperate());
	}

	private void beep() {
		SWTUtil.getStandardDisplay().beep();
	}

	private static boolean canActivate(Refactoring ref) throws JavaModelException{
		//XX must change this
	     if (ref instanceof IPreactivatedRefactoring)
	     	return ((IPreactivatedRefactoring)ref).checkPreactivation().isOK();
	     return ref.checkActivation(new NullProgressMonitor()).isOK();
	}
	
	private boolean canOperate() {
		ISelection selection= fEditor.getSelectionProvider().getSelection();
		if (!(selection instanceof ITextSelection))
			return false;
		return ((ITextSelection)selection).getLength() > 0;
	}
	
	private ITextSelection getTextSelection() {
		return (ITextSelection)fEditor.getSelectionProvider().getSelection();
	}	
}

