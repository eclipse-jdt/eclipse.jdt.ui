package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.corext.codemanipulation.MemberEdit;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class PasteSourceReferencesFromClipboardAction extends SelectionDispatchAction {

	private Clipboard fClipboard;

	protected PasteSourceReferencesFromClipboardAction(IWorkbenchSite site, Clipboard clipboard) {
		super(site);
		fClipboard= clipboard; //can be null
	}
	
	protected void selectionChanged(IStructuredSelection selection) {
		setEnabled(canOperateOn(selection));
	}

	public boolean canOperateOn(IStructuredSelection selection) {
		try{
			if (! isAnythingToPaste())
				return false;
				
			if (selection.size() != 1)
				return false;				
			
			Object selected= selection.getFirstElement();
			if (selected instanceof IClassFile)
				return false;
				 
			if (! (selected instanceof ISourceReference))
				return false;
	
			if (! (selected instanceof IJavaElement))
				return false;
			
			if (((IJavaElement)selected).isReadOnly())
				return false;
			
			if (! ((IJavaElement)selected).exists())
				return false;
				
			IFile file= SourceReferenceUtil.getFile((ISourceReference)selected);
			if (file.isReadOnly())
				return false;	
			
			if (! file.isAccessible())
				return false;	
				
			if (selected instanceof IMember && ((IMember)selected).isBinary())
				return false;

			ISourceReference workingCopyEl= getWorkingCopyElement((ISourceReference)selected);
			if (workingCopyEl == null || ! ((IJavaElement)workingCopyEl).exists())
				return false;
			
			return canPaste((ISourceReference)selected, getContentsToPaste());
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e);
			return false;
		}		
	}
	
	private boolean isAnythingToPaste(){
		TypedSource[] elems= getContentsToPaste();
		if (elems == null)
			return false;
		for (int i= 0; i < elems.length; i++) {
			if (! isInterestingSourceReference(elems[i]))
				return false;
		}
		return true;
	}
	
	private  boolean isInterestingSourceReference(TypedSource je){
		if (je.getType() == IJavaElement.CLASS_FILE)
			return false;
		if (je.getType() == IJavaElement.COMPILATION_UNIT)
			return false;	
		return true;		
	}

	protected TypedSource[] getContentsToPaste(){
		if (fClipboard == null)
			return new TypedSource[0];
		else	
			return (TypedSource[])fClipboard.getContents(TypedSourceTransfer.getInstance());
	}
	
	private  boolean canPaste(ISourceReference ref, TypedSource[] elements){
		return (canPasteIn(ref, elements) || canPasteAfter(ref, elements));
	}
	
	private ISourceReference getSelectedElement(IStructuredSelection selection){
		return (ISourceReference)selection.getFirstElement();
	}
	
	public void run(final IStructuredSelection selection) {
		if (! canOperateOn(selection))
			return;
		
		new BusyIndicator().showWhile(JavaPlugin.getActiveWorkbenchShell().getDisplay(), new Runnable() {
			public void run() {
				try {
					perform(getSelectedElement(selection));
				} catch (CoreException e) {
					ExceptionHandler.handle(e, ReorgMessages.getString("PasteSourceReferencesFromClipboardAction.paste1"), ReorgMessages.getString("PasteSourceReferencesFromClipboardAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		});
	}
	
	 void perform(ISourceReference selected) throws CoreException{
		ISourceReference selectedWorkingCopyElement= getWorkingCopyElement(selected);
		if (selectedWorkingCopyElement == null || ! ((IJavaElement)selectedWorkingCopyElement).exists())
			return;
		
		if (canPasteIn(selectedWorkingCopyElement, getContentsToPaste())){
			if (selectedWorkingCopyElement instanceof ICompilationUnit) //special case
				pasteInCompilationUnit((ICompilationUnit)selectedWorkingCopyElement);
			else
				paste(MemberEdit.ADD_AT_BEGINNING, selectedWorkingCopyElement);	
		}	else if (canPasteAfter(selectedWorkingCopyElement, getContentsToPaste()))
			paste(MemberEdit.INSERT_AFTER, selectedWorkingCopyElement);
		else	
			Assert.isTrue(false);//should be checked already (on activation)	
	}

	private  ISourceReference getWorkingCopyElement(ISourceReference selected) throws JavaModelException {
		ICompilationUnit cu= SourceReferenceUtil.getCompilationUnit(selected);
		ICompilationUnit workingCopy= WorkingCopyUtil.getWorkingCopyIfExists(cu);
		return (ISourceReference)JavaModelUtil.findInCompilationUnit(workingCopy, (IJavaElement)selected);
	}
		
	private  void paste(int style, ISourceReference selected) throws CoreException{
		TextBuffer tb= TextBuffer.acquire(SourceReferenceUtil.getFile(selected));
		try{
			TextBufferEditor tbe= new TextBufferEditor(tb);
			TypedSource[] elems= getContentsToPaste();
			
			IJavaElement element= (IJavaElement)selected;
			int tabWidth= CodeFormatterUtil.getTabWidth();
			for (int i= 0; i < elems.length; i++) {
				String[] source= Strings.convertIntoLines(elems[i].getSource());
				MemberEdit edit= new MemberEdit(element, style, source, tabWidth);
				tbe.add(edit);
			}
			if (! tbe.canPerformEdits())
				return; ///XXX
			tbe.performEdits(new NullProgressMonitor());	
			TextBuffer.commitChanges(tb, false, new NullProgressMonitor());
		} finally{	
			if (tb != null)
				TextBuffer.release(tb);
		}
	}
	
	private  void pasteInCompilationUnit(ICompilationUnit unit) throws CoreException{
		TextBuffer tb= TextBuffer.acquire(SourceReferenceUtil.getFile(unit));
		try{
			TextBufferEditor tbe= new TextBufferEditor(tb);
			TypedSource[] elems= getContentsToPaste();
			
			for (int i= 0; i < elems.length; i++) {
				tbe.add(new PasteInCompilationUnitEdit(elems[i].getSource(), elems[i].getType(), unit));
			}
			if (! tbe.canPerformEdits())
				return; ///XXX
			tbe.performEdits(new NullProgressMonitor());	
			TextBuffer.commitChanges(tb, false, new NullProgressMonitor());
		} finally{	
			if (tb != null)
				TextBuffer.release(tb);		
		}	
	}
	
	private  boolean canPasteAfter(ISourceReference ref, TypedSource[] elements){
		if (ref instanceof ICompilationUnit)
			return false;
		if (ref instanceof IImportContainer)
			return canPasteAtTopLevel(elements);
		if (ref instanceof IImportDeclaration)
			return canPasteAtTopLevel(elements);
		if (ref instanceof IPackageDeclaration)
			return canPasteAtTopLevel(elements);
		
		//order important
		if (ref instanceof IType)
			return canPasteAfterType(elements);
				
		if (ref instanceof IMember)
			return  canPasteAfterMember(elements);
		return false;
	}
	
	private  boolean canPasteAfterType(TypedSource[] elems){
		return areAllValuesOfType(elems, IJavaElement.TYPE);
	}

	private  boolean canPasteAtTopLevel(TypedSource[] elements){
		for (int i= 0; i < elements.length; i++) {
			if (! canPasteAfterImportContainerOrDeclaration(elements[i].getType()))
				return false;
		}
		return true;
	}
	
	private  int getElementType(ISourceReference ref){
		return ((IJavaElement)ref).getElementType();
	}
	
	private  boolean canPasteAfterImportContainerOrDeclaration(int type){
		if (type == IJavaElement.IMPORT_CONTAINER)
			return true;
		if (type == IJavaElement.IMPORT_DECLARATION)	
			return true;
		if (type == IJavaElement.TYPE)		
			return true;	
		return false;
	}

	private  boolean canPasteAfterMember(TypedSource[] elems){
		return areAllMembers(elems);
	}
	
	private  boolean canPasteIn(ISourceReference ref, TypedSource[] elements){
		if (ref instanceof IImportContainer)
			return canPasteInImportContainer(elements);	
		if (ref instanceof IType)
			return canPasteInType(elements);
		if (ref instanceof ICompilationUnit)
			return canPasteInCompilationUnit(elements);
		
		return false;	
	}
	
	private  boolean canPasteInImportContainer(TypedSource[] elements){
		//not supported in MemberEdit yet
		return false;
	}
	
	private  boolean canPasteInType(TypedSource[] elements){
		return areAllMembers(elements);
	}
		
	private  boolean canPasteInCompilationUnit(TypedSource[] elements){
		for (int i= 0; i < elements.length; i++) {
			if (! canPasteInCompilationUnit(elements[i].getType()))
				return false;
		}
		return true;
	}
		
	private  boolean canPasteInCompilationUnit(int type){
		if (type == IJavaElement.IMPORT_CONTAINER)
			return true;
		if (type == IJavaElement.IMPORT_DECLARATION)	
			return true; //XXX maybe only when there is no ImportContainer?
		if (type == IJavaElement.PACKAGE_DECLARATION)		
			return true; //XXX even if there's one already?
		if (type == IJavaElement.TYPE)		
			return true;
		return false;	
	}	
	
	//--- helpers
	
	private  boolean areAllValuesOfType(TypedSource[] elements, int type){
		for (int i= 0; i < elements.length; i++) {
			if (elements[i].getType() != type)
				return false;
		}
		return true;
	}
	
	private  boolean areAllMembers(TypedSource[] elements){
		for (int i= 0; i < elements.length; i++) {
			if (! isMember(elements[i].getType()))
				return false;
		}
		return true;
	}

	private  boolean isMember(int type){
		if (type == IJavaElement.FIELD)
			return true;
		if (type == IJavaElement.INITIALIZER)
			return true;
		if (type == IJavaElement.METHOD)
			return true;
		if (type == IJavaElement.TYPE)
			return true;
		return false;		
	}

}
