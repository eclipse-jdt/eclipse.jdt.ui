package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.fields.RenameFieldRefactoring;
import org.eclipse.jdt.internal.core.refactoring.methods.RenameMethodRefactoring;
import org.eclipse.jdt.internal.core.refactoring.tagging.IPreactivatedRefactoring;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.core.refactoring.types.RenameTypeRefactoring;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardDialog;
import org.eclipse.jdt.internal.ui.refactoring.changes.DocumentTextBufferChangeCreator;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.Utilities;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;

public class RenameJavaElementAction extends Action implements IUpdate {
	
	private ITextEditor fEditor;
	
	public RenameJavaElementAction(ITextEditor editor){
		super("Rename");
		Assert.isNotNull(editor);
		fEditor= editor;
	}

	/* non java-doc
	 * @see Action#run()
	 */
	public void run() {
		try{
			RefactoringWizard wizard= createWizard();
			if (wizard != null && canActivate(wizard.getRefactoring())){
				new RefactoringWizardDialog(JavaPlugin.getActiveWorkbenchShell(), wizard).open();
			} else 	{			
				beep();
			}	
		}catch (JavaModelException e){
			ExceptionHandler.handle(e, "Exception", "Unexpected exception. See log for details.");
		}	
	}
	
	/* non java-doc
	 * @see IUpdate#update()
	 */
	public void update() {
		setEnabled(canOperate());
	}

	private void beep() {
		Utilities.getDisplay(null).beep();
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
	
	private static ITextBufferChangeCreator getChangeCreator(){
		return  new DocumentTextBufferChangeCreator(JavaPlugin.getDefault().getCompilationUnitDocumentProvider());
	}
	
	private RefactoringWizard createWizard() throws JavaModelException{
		IJavaElement element=  JavaElementFinder.getJavaElement(getTextSelection());
		if (element == null || !element.exists())
			return null;
		switch (element.getElementType()){
			case IJavaElement.FIELD: 
				 	return RefactoringWizardFactory.createWizard(new RenameFieldRefactoring(getChangeCreator(), (IField)element));
			case IJavaElement.METHOD:
				 	return RefactoringWizardFactory.createWizard(RenameMethodRefactoring.createInstance(getChangeCreator(), (IMethod)element));
			case IJavaElement.TYPE:
				 	return RefactoringWizardFactory.createWizard(new RenameTypeRefactoring(getChangeCreator(), (IType)element));
			default: return null;
		} 
	}
}

