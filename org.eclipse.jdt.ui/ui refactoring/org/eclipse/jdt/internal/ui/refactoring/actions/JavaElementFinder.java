package org.eclipse.jdt.internal.ui.refactoring.actions;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditorInput;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

class JavaElementFinder {
	
	public static  IJavaElement getJavaElement(ITextSelection selection) {
		return getJavaElement(selection, true);
	}
	
	public static  IJavaElement getJavaElement(ITextSelection selection, boolean shouldUserBePrompted) {
		IEditorPart editorPart= JavaPlugin.getActivePage().getActiveEditor();

		if (editorPart == null)
			return null;

		ICodeAssist assist= getCodeAssist(editorPart);
		ITextSelection ts= (ITextSelection) selection;
		if (assist != null) {
			IJavaElement[] elements;
			try {
				elements= assist.codeSelect(ts.getOffset(), ts.getLength());
			} catch (JavaModelException ex) {
				ExceptionHandler.handle(ex, "Exception", "Unexpected excpetion. See log for details."); 
				return null;
			}
			if (elements != null && elements.length > 0) {
				if (elements.length == 1 || !shouldUserBePrompted)
					return elements[0];
				else if  (elements.length > 1)
					return chooseFromList(Arrays.asList(elements));
			}
		}
		return null;
	}
	
	private static IJavaElement chooseFromList(List openChoices) {
		ILabelProvider labelProvider= new JavaElementLabelProvider(
			  JavaElementLabelProvider.SHOW_DEFAULT 
			| JavaElementLabelProvider.SHOW_CONTAINER_QUALIFICATION);
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(JavaPlugin.getActiveWorkbenchShell(), labelProvider);
		dialog.setTitle("Select"); 
		dialog.setMessage("Select the correct element from the list");
		dialog.setElements(openChoices.toArray());
		if (dialog.open() == dialog.OK)
			return (IJavaElement)dialog.getFirstResult();
		return null;
	}
	
	private static ICodeAssist getCodeAssist(IEditorPart editorPart) {
		IEditorInput input= editorPart.getEditorInput();
		if (input instanceof ClassFileEditorInput)
			return ((ClassFileEditorInput)input).getClassFile();
		IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();				
		return manager.getWorkingCopy(input);
	}
}

