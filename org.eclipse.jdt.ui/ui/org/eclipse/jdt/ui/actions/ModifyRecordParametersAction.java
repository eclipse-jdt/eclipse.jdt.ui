package org.eclipse.jdt.ui.actions;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.Statement;

import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringExecutionStarter;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
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
		ASTNode node = selection.resolveCoveringNode();
		boolean isRecord = isRecord(node);
		setEnabled(isRecord);
	}

	private boolean isRecord(ASTNode node) {
		return getRecordClassInstanceCreation(node) != null;
	}

	/**
	 * Walks up the AST from <code>node</code> to find the enclosing
	 * {@link ClassInstanceCreation} whose resolved type is a record.
	 * Returns <code>null</code> if no such node exists.
	 */
	private ClassInstanceCreation getRecordClassInstanceCreation(ASTNode node) {
		if (node instanceof ClassInstanceCreation cic) {
			ITypeBinding binding = cic.resolveTypeBinding();
			if (binding != null && binding.isRecord()) {
				return cic;
			}
		}
		if (node == null || node instanceof Statement || node instanceof CompilationUnit || node instanceof BodyDeclaration) {
			return null;
		}
		return getRecordClassInstanceCreation(node.getParent());
	}

	@Override
	public void selectionChanged(ITextSelection selection) {
		setEnabled(true);
	}

	@Override
	public void selectionChanged(ISelection selection) {
		setEnabled(true);
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		setEnabled(true);
	}

    /*
     * @see SelectionDispatchAction#run(ITextSelection)
     */
	@Override
	public void run(ITextSelection selection) {
		// We need to identify the IType for the current record.
		// To do that I first need to get the CompilationUnit
		// And then with Nodefinder.perform() search for the astNode related to the selection.
		if (!ActionUtil.isEditable(fEditor))
			return;
		CompilationUnit cu = getASTCompilationUnit(fEditor);
		ASTNode node = NodeFinder.perform(cu, selection.getOffset(), selection.getLength());
		ClassInstanceCreation cic = getRecordClassInstanceCreation(node);
		if (cic != null) {
			ITypeBinding typeBinding = cic.resolveTypeBinding();
			if (typeBinding == null || !(typeBinding.getJavaElement() instanceof IType))
				return;

			IType recordType = (IType) typeBinding.getJavaElement();
			RefactoringExecutionStarter.startChangeRecordSignatureRefactoring(node, recordType, selection, this, getShell());
		}
	}

	private CompilationUnit getASTCompilationUnit(JavaEditor editor) {
		//Should this be moved to RefactoryAvailabilityTestCore
	    ICompilationUnit icu = SelectionConverter.getInputAsCompilationUnit(editor);
	    if (icu == null) return null;

	    ASTParser parser = ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
	    parser.setResolveBindings(true);
	    parser.setBindingsRecovery(true);
	    parser.setKind(ASTParser.K_COMPILATION_UNIT);
	    parser.setSource(icu);

	    return (CompilationUnit) parser.createAST(new NullProgressMonitor());
	}

	/*
	 * @see SelectionDispatchAction#run(IStructuredSelection)
	 */
	@Override
	public void run(IStructuredSelection selection) {
		if (! ActionUtil.isEditable(fEditor))
			return;
	}
}
