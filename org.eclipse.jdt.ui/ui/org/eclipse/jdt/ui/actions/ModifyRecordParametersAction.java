package org.eclipse.jdt.ui.actions;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Statement;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

/**
 * Action to start the modify parameters refactoring of a record. The refactoring supports
 * swapping and renaming of arguments.
 * <p>
 * This action is applicable to selections containing a method with one or
 * more arguments.
 *
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 * @since 2.0
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ModifyRecordParametersAction extends SelectionDispatchAction {

	private JavaEditor fEditor;

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the java editor
	 *
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	public ModifyRecordParametersAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}

	protected ModifyRecordParametersAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.RefactoringGroup_modify_Record_Parameters_label);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.MODIFY_RECORD_PARAMETERS_ACTION);
	}

	@Override
	public void selectionChanged(IStructuredSelection selection) {
		super.selectionChanged(selection);
	}

	@Override
	public void selectionChanged(JavaTextSelection selection) {
		// TODO Auto-generated method stub
		super.selectionChanged(selection);
		ASTNode node = selection.resolveCoveringNode();
		boolean isRecord = isRecord(node);
		setEnabled(isRecord);
	}

	private boolean isRecord(ASTNode node) {
		// We can check if the node is a record if its type is ClassInstanceException.
		if (node instanceof ClassInstanceCreation) {
			ClassInstanceCreation cic = (ClassInstanceCreation)node;
			ITypeBinding binding = cic.resolveTypeBinding();
			return binding != null && binding.isRecord();
		}
		// If the current node is a Statement, or a Compilation unit or a BodyDeclarataion
		// we can safely assume is not a Record
		if(node == null || node instanceof Statement || node instanceof CompilationUnit || node instanceof BodyDeclaration ) {
			return false;
		}
		return isRecord(node.getParent());
	}

	@Override
	public void selectionChanged(ITextSelection selection) {
		// TODO Auto-generated method stub
		super.selectionChanged(selection);
	}

	@Override
	public void selectionChanged(ISelection selection) {
		// TODO Auto-generated method stub
		super.selectionChanged(selection);
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		// TODO Auto-generated method stub
		super.selectionChanged(event);
	}



}
