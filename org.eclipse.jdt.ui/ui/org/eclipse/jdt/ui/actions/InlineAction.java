package org.eclipse.jdt.ui.actions;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.actions.*;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineConstantRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineMethodRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineTempRefactoring;

/**
 * Inlines a method, local variable or a static final field.
 *
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
public class InlineAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;
	private final InlineTempAction fInlineTemp;
	private final InlineMethodAction fInlineMethod;
	private final InlineConstantAction fInlineConstant;

	/**
	 * Creates a new <code>InlineAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 *
	 * @param site the site providing context information for this action
	 */
	public InlineAction(IWorkbenchSite site) {
		super(site);
		setText("&Inline...");
		fInlineTemp		= new InlineTempAction(site);
		fInlineMethod	= new InlineMethodAction(site);
		fInlineConstant	= new InlineConstantAction(site);
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.INLINE_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public InlineAction(CompilationUnitEditor editor) {
		//don't want to call 'this' here - it'd create useless action objects
		super(editor.getEditorSite());
		setText("&Inline...");
		fEditor= editor;
		fInlineTemp		= new InlineTempAction(editor);
		fInlineMethod	= new InlineMethodAction(editor);
		fInlineConstant	= new InlineConstantAction(editor);
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.RENAME_ACTION);
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.viewers.ISelection)
	 */
	protected void selectionChanged(ISelection selection) {
		fInlineConstant.update(selection);
		fInlineMethod.update(selection);
		fInlineTemp.update(selection);
		setEnabled(computeEnablementState());
	}

	private boolean computeEnablementState() {
		return fInlineTemp.isEnabled() || fInlineConstant.isEnabled() || fInlineMethod.isEnabled();
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.text.ITextSelection)
	 */
	protected void run(ITextSelection selection) {
		ICompilationUnit cu= getCompilationUnit();
		if (cu == null)
			return;

		if (fInlineMethod.isEnabled() && tryInlineMethod(cu, selection))
			return;
		
		if (fInlineTemp.isEnabled() && tryInlineTemp(cu, selection))
			return;

		if (fInlineConstant.isEnabled() && tryInlineConstant(cu, selection))
			return;
	
		MessageDialog.openInformation(getShell(), "Inline", "Select a method declaration, a method invocation, a static final field or a local variable that you want to inline.");
	}
	
	private boolean tryInlineTemp(ICompilationUnit cu, ITextSelection selection){
		InlineTempRefactoring inlineTemp= new InlineTempRefactoring(cu, selection.getOffset(), selection.getLength());
		if (inlineTemp.checkIfTempSelectedSelected().hasFatalError())
			return false;
		fInlineTemp.run(selection);
		return true;
	}
	
	private boolean tryInlineMethod(ICompilationUnit cu, ITextSelection selection){
		InlineMethodRefactoring inlineMethodRef= InlineMethodRefactoring.create(
			cu, selection.getOffset(), selection.getLength(),
			JavaPreferencesSettings.getCodeGenerationSettings());
		if (inlineMethodRef == null)	
			return false;
		fInlineMethod.run(selection);
		return true;
	}
	
	private boolean tryInlineConstant(ICompilationUnit cu, ITextSelection selection){
		InlineConstantRefactoring inlineConstantRef= InlineConstantRefactoring.create(
			cu, selection.getOffset(), selection.getLength(),
			JavaPreferencesSettings.getCodeGenerationSettings());
		if (inlineConstantRef == null || inlineConstantRef.checkStaticFinalConstantNameSelected().hasFatalError())	
			return false;
		fInlineConstant.run(selection);
		return true;
	}
	
	private ICompilationUnit getCompilationUnit() {
		return SelectionConverter.getInputAsCompilationUnit(fEditor);
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected void run(IStructuredSelection selection) {
		if (fInlineConstant.isEnabled())
			fInlineConstant.run(selection);
		else if (fInlineMethod.isEnabled())
			fInlineMethod.run(selection);
		else	
			//inline temp will never be enabled on IStructuredSelection
			//don't bother running it
			Assert.isTrue(! fInlineTemp.isEnabled());
	}
}
