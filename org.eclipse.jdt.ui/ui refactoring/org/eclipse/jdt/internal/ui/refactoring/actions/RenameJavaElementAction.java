/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringExecutionStarter;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.reorg.RenameLinkedMode;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionCommandHandler;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedNamesAssistProposal;
import org.eclipse.jdt.internal.ui.util.ASTHelper;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class RenameJavaElementAction extends SelectionDispatchAction {

	private JavaEditor fEditor;

	public RenameJavaElementAction(IWorkbenchSite site) {
		super(site);
	}

	public RenameJavaElementAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}

	//---- Structured selection ------------------------------------------------

	@Override
	public void selectionChanged(IStructuredSelection selection) {
		try {
			if (selection.size() == 1) {
				setEnabled(canEnable(selection));
				return;
			}
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.isExceptionToBeLogged(e))
				JavaPlugin.log(e);
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
		setEnabled(false);
	}

	private static boolean canEnable(IStructuredSelection selection) throws CoreException {
		IJavaElement element= getJavaElement(selection);
		if (element == null)
			return false;
		return RefactoringAvailabilityTester.isRenameElementAvailable(element);
	}

	private static IJavaElement getJavaElement(IStructuredSelection selection) {
		if (selection.size() != 1)
			return null;
		Object first= selection.getFirstElement();
		if (! (first instanceof IJavaElement))
			return null;
		return (IJavaElement)first;
	}

	@Override
	public void run(IStructuredSelection selection) {
		IJavaElement element= getJavaElement(selection);
		if (element == null)
			return;
		if (!ActionUtil.isEditable(getShell(), element))
			return;
		try {
			run(element, false);
		} catch (CoreException e){
			ExceptionHandler.handle(e, RefactoringMessages.RenameJavaElementAction_name, RefactoringMessages.RenameJavaElementAction_exception);
		}
	}

	//---- text selection ------------------------------------------------------------

	@Override
	public void selectionChanged(ITextSelection selection) {
		if (selection instanceof JavaTextSelection) {
			try {
				JavaTextSelection javaTextSelection= (JavaTextSelection)selection;
				IJavaElement[] elements= javaTextSelection.resolveElementAtOffset();
				if (isVarTypeSelection(javaTextSelection)) {
					setEnabled(false);
				}
				else {
					if (elements.length == 1) {
						setEnabled(RefactoringAvailabilityTester.isRenameElementAvailable(elements[0], true));
					} else {
						ASTNode node= javaTextSelection.resolveCoveringNode();
						setEnabled(node instanceof SimpleName);
					}
				}
			} catch (CoreException e) {
				setEnabled(false);
			}
		} else {
			setEnabled(true);
		}
	}

	@Override
	public void run(ITextSelection selection) {
		if (!ActionUtil.isEditable(fEditor))
			return;
		boolean isVarTypeSelection= isVarTypeSelection(selection);
		if (!isVarTypeSelection && canRunInEditor())
			doRun(true);
		else if (!isVarTypeSelection && canBeRenamed(false)) {
			MessageDialog.openInformation(getShell(), RefactoringMessages.RenameAction_rename, RefactoringMessages.RenameAction_unavailable_in_editor);
		} else {
			MessageDialog.openInformation(getShell(), RefactoringMessages.RenameAction_rename, RefactoringMessages.RenameAction_unavailable);
		}
	}

	public void doRun() {
		doRun(false);
	}

	public void doRun(boolean isTextSelection) {
		RenameLinkedMode activeLinkedMode= RenameLinkedMode.getActiveLinkedMode();
		if (activeLinkedMode != null) {
			if (activeLinkedMode.isCaretInLinkedPosition()) {
				activeLinkedMode.startFullDialog();
				return;
			} else {
				activeLinkedMode.cancel();
			}
		}

		try {
			IJavaElement element= getJavaElementFromEditor();
			IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
			boolean lightweight= store.getBoolean(PreferenceConstants.REFACTOR_LIGHTWEIGHT);
			if (element != null && RefactoringAvailabilityTester.isRenameElementAvailable(element, isTextSelection)) {
				run(element, lightweight);
				return;
			} else if (lightweight) {
				// fall back to local rename:
				CorrectionCommandHandler handler= new CorrectionCommandHandler(fEditor, LinkedNamesAssistProposal.ASSIST_ID, true);
				if (handler.doExecute()) {
					fEditor.setStatusLineErrorMessage(RefactoringMessages.RenameJavaElementAction_started_rename_in_file);
					return;
				}
			}
		} catch (CoreException e) {
			ExceptionHandler.handle(e, RefactoringMessages.RenameJavaElementAction_name, RefactoringMessages.RenameJavaElementAction_exception);
		}
		MessageDialog.openInformation(getShell(), RefactoringMessages.RenameJavaElementAction_name, RefactoringMessages.RenameJavaElementAction_not_available);
	}

	public boolean canRunInEditor() {
		if (RenameLinkedMode.getActiveLinkedMode() != null)
			return true;

		return canBeRenamed(true);
	}

	private boolean canBeRenamed(boolean isTextSelection) {
		try {
			IJavaElement element= getJavaElementFromEditor();
			if (element == null)
				return true;

			return RefactoringAvailabilityTester.isRenameElementAvailable(element, isTextSelection);
		} catch (JavaModelException e) {
			if (JavaModelUtil.isExceptionToBeLogged(e))
				JavaPlugin.log(e);
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
		return false;
	}

	private IJavaElement getJavaElementFromEditor() throws JavaModelException {
		IJavaElement[] elements= SelectionConverter.codeResolve(fEditor);
		if (elements == null || elements.length != 1)
			return null;
		IField field= getRecordComponentToRename(elements[0]);
		if (field != null)
			return field;
		return elements[0];
	}

	//---- helper methods -------------------------------------------------------------------

	private void run(IJavaElement element, boolean lightweight) throws CoreException {
		// Work around for http://dev.eclipse.org/bugs/show_bug.cgi?id=19104
		if (! ActionUtil.isEditable(fEditor, getShell(), element))
			return;
		//XXX workaround bug 31998
		if (ActionUtil.mustDisableJavaModelAction(getShell(), element))
			return;

		if (lightweight && fEditor instanceof CompilationUnitEditor && ! (element instanceof IPackageFragment)) {
			new RenameLinkedMode(element, (CompilationUnitEditor) fEditor).start();
		} else {
			RefactoringExecutionStarter.startRenameRefactoring(element, getShell());
		}
	}

	private boolean isVarTypeSelection(ITextSelection textSelection) {
		if (textSelection instanceof JavaTextSelection) {
			ASTNode node= ((JavaTextSelection) textSelection).resolveCoveringNode();
			if (node instanceof SimpleName && node.getAST().apiLevel() >= ASTHelper.JLS10 && ((SimpleName) node).isVar()) {
				return true;
			}
		} else if (textSelection != null) {
			ITypeRoot typeRoot= EditorUtility.getEditorInputJavaElement(fEditor, true);
			if (typeRoot != null) {
				IDocument document= JavaUI.getDocumentProvider().getDocument(fEditor.getEditorInput());
				if (document != null) {
					JavaTextSelection javaTextSelection= new JavaTextSelection(typeRoot, document, textSelection.getOffset(), textSelection.getLength());
					return isVarTypeSelection(javaTextSelection);
				}
			}
		}
		return false;
	}

	private IField getField(ILocalVariable localVar) {
		IField field= null;
		if (localVar != null) {
			IJavaElement parent= localVar.getParent();
			if (parent != null && parent.getElementType() == IJavaElement.METHOD) {
				IMethod parentMethod= (IMethod) parent;
				try {
					if (parentMethod.isConstructor()) {
						IJavaElement topParent= parent.getParent();
						if (topParent != null && topParent.getElementType() == IJavaElement.TYPE) {
							IType type= (IType) topParent;
							if (type.isRecord()) {
								CompilationUnit astRoot= SharedASTProviderCore.getAST(type.getCompilationUnit(), SharedASTProviderCore.WAIT_YES, null);
								MethodDeclaration mDecl= ASTNodeSearchUtil.getMethodDeclarationNode(parentMethod, astRoot);
								if (mDecl != null) {
									IMethodBinding mBinding= mDecl.resolveBinding();
									if (mBinding != null && mBinding.isCanonicalConstructor()) {
										field= type.getRecordComponent(localVar.getElementName());
										return field;
									}
								}
							}
						}
					}
				} catch (JavaModelException e) {
					//do nothing
				}
			}

		}
		return field;
	}

	private IField getRecordComponentToRename(IJavaElement element) {
		IField recCompField= null;
		if (element != null) {
			if (element.getElementType() == IJavaElement.LOCAL_VARIABLE) {
				IJavaElement parent= element.getParent();
				if (parent != null && parent.getElementType() == IJavaElement.FIELD) {
					IField parentField= (IField) parent;
					IJavaElement topParent= parent.getParent();
					if (topParent != null && topParent.getElementType() == IJavaElement.TYPE) {
						try {
							if (((IType) topParent).isRecord()) {
								recCompField= parentField;
							}
						} catch (JavaModelException e) {
							//do nothing
						}
					}
				} else if (parent != null && parent.getElementType() == IJavaElement.METHOD) {
					IMethod parentMethod= (IMethod) parent;
					try {
						if (parentMethod.isConstructor()) {
							IJavaElement topParent= parent.getParent();
							if (topParent != null && topParent.getElementType() == IJavaElement.TYPE) {
								if (((IType) topParent).isRecord()) {
									IField fieldElem= getField((ILocalVariable) element);
									if (fieldElem != null) {
										recCompField= fieldElem;
									}
								}
							}
						}
					} catch (JavaModelException e) {
						//do nothing
					}
				}
			} else if (element.getElementType() == IJavaElement.METHOD) {
				IMethod mElem= (IMethod) element;
				IJavaElement parent= mElem.getParent();
				if (parent != null && parent.getElementType() == IJavaElement.TYPE) {
					try {
						IType pType= (IType) parent;
						if (mElem.getParameters().length == 0
								&& pType.isRecord()
								&& !Flags.isStatic(mElem.getFlags())) {
							IField[] fields= pType.getRecordComponents();
							for (IField field : fields) {
								if (!Flags.isStatic(field.getFlags()) && mElem.getElementName().equals(field.getElementName())) {
									recCompField= field;
									break;
								}
							}
						}
					} catch (JavaModelException e) {
						//do nothing
					}
				}
			}
		}

		return recCompField;
	}
}
