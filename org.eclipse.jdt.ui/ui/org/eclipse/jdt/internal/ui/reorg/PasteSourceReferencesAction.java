package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

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

import org.eclipse.jdt.internal.corext.codemanipulation.MemberEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.codemanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringAction;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class PasteSourceReferencesAction extends RefactoringAction {

	public PasteSourceReferencesAction(ISelectionProvider provider) {
		super("&Paste", provider);
	}

	/*
	 * @see RefactoringAction#canOperateOn(IStructuredSelection)
	 */
	public boolean canOperateOn(IStructuredSelection selection) {
		try{
			if (! isAnythingInInterestingClipboard())
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
				
			return canPaste((ISourceReference)selected, getClipboardContents());
		} catch (JavaModelException e){
			JavaPlugin.log(e);
			return false;
		}		
	}
	
	private static Clipboard getClipboard(){
		return new Clipboard(JavaPlugin.getActiveWorkbenchShell().getDisplay());
	}
	
	private static boolean isAnythingInInterestingClipboard(){
		IJavaElement[] elems= (IJavaElement[])getClipboard().getContents(JavaElementTransfer.getInstance());
		if (elems == null)
			return false;
		for (int i= 0; i < elems.length; i++) {
			if (! isSourceReference(elems[i]))
				return false;
		}
		return true;
	}
	
	private static boolean isSourceReference(IJavaElement je){
		if (! (je instanceof ISourceReference))
			return false;
		if (je instanceof IClassFile)
			return false;
		if (je instanceof ICompilationUnit)
			return false;					
		return true;		
	}
	
	private static ISourceReference[] getClipboardContents(){
		Assert.isTrue(isAnythingInInterestingClipboard());
		IJavaElement[] elems= (IJavaElement[])getClipboard().getContents(JavaElementTransfer.getInstance());
		ISourceReference[] result= new ISourceReference[elems.length];
		for (int i= 0; i < elems.length; i++) {
			result[i]= (ISourceReference)elems[i]; //checked before
		}
		return result;
	}
	
	private static boolean canPaste(ISourceReference ref, ISourceReference[] elements){
		if (canPasteIn(ref, elements))
			return true;
		if (canPasteAfter(ref, elements))
			return true;
		return false;		
	}
	
	private ISourceReference getSelectedElement(){
		return (ISourceReference)getStructuredSelection().getFirstElement();
	}
	
	/*
	 * @see Action#run
	 */
	public void run() {
		new BusyIndicator().showWhile(JavaPlugin.getActiveWorkbenchShell().getDisplay(), new Runnable() {
			public void run() {
				try {
					perform(getSelectedElement());
				} catch (CoreException e) {
					ExceptionHandler.handle(e, "Paste", "Unexpected exception. See log for details.");
				}
			}
		});
	}
	
	static void perform(ISourceReference selected) throws CoreException{
		if (canPasteIn(selected, getClipboardContents())){
			if (selected instanceof ICompilationUnit) //special case
				pasteInCompilationUnit((ICompilationUnit)selected);
			else
				paste(MemberEdit.ADD_AT_BEGINNING, selected);	
		}	else if (canPasteAfter(selected, getClipboardContents()))
			paste(MemberEdit.INSERT_AFTER, selected);
		else	
			Assert.isTrue(false);//should be checked already (on activation)	
	}
	
	private static void paste(int style, ISourceReference selected) throws CoreException{
		TextBuffer tb= TextBuffer.acquire(SourceReferenceUtil.getFile(selected));
		TextBufferEditor tbe= new TextBufferEditor(tb);
		ISourceReference[] elems= getClipboardContents();
		
		IJavaElement element= (IJavaElement)selected;
		int tabWidth= CodeFormatterPreferencePage.getTabSize();
		for (int i= 0; i < elems.length; i++) {
			String[] source= new String[]{SourceReferenceSourceRangeComputer.computeSource(elems[i])};
			tbe.addTextEdit(new MemberEdit(element, style, source, tabWidth));
		}
		if (! tbe.canPerformEdits())
			return; ///XXX
		tbe.performEdits(new NullProgressMonitor());	
		TextBuffer.commitChanges(tb, false, new NullProgressMonitor());
		TextBuffer.release(tb);
	}
	
	private static void pasteInCompilationUnit(ICompilationUnit unit) throws CoreException{
		TextBuffer tb= TextBuffer.acquire(SourceReferenceUtil.getFile(unit));
		TextBufferEditor tbe= new TextBufferEditor(tb);
		ISourceReference[] elems= getClipboardContents();
		
		for (int i= 0; i < elems.length; i++) {
			String source= SourceReferenceSourceRangeComputer.computeSource(elems[i]);
			int type= ((IJavaElement)elems[i]).getElementType();
			tbe.addTextEdit(new PasteInCompilationUnitEdit(source, type, unit));
		}
		if (! tbe.canPerformEdits())
			return; ///XXX
		tbe.performEdits(new NullProgressMonitor());	
		TextBuffer.commitChanges(tb, false, new NullProgressMonitor());
		TextBuffer.release(tb);		
	}
	
	private static boolean canPasteAfter(ISourceReference ref, ISourceReference[] elements){
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
	
	private static boolean canPasteAfterType(ISourceReference[] elems){
		return areAllValuesOfType(elems, IJavaElement.TYPE);
	}

	private static boolean canPasteAtTopLevel(ISourceReference[] elements){
		for (int i= 0; i < elements.length; i++) {
			if (! canPasteAfterImportContainerOrDeclaration(getElementType(elements[i])))
				return false;
		}
		return true;
	}
	
	private static int getElementType(ISourceReference ref){
		return ((IJavaElement)ref).getElementType();
	}
	
	private static boolean canPasteAfterImportContainerOrDeclaration(int type){
		if (type == IJavaElement.IMPORT_CONTAINER)
			return true;
		if (type == IJavaElement.IMPORT_DECLARATION)	
			return true;
		if (type == IJavaElement.TYPE)		
			return true;	
		return false;
	}

	private static boolean canPasteAfterMember(ISourceReference[] elems){
		return areAllMembers(elems);
	}
	
	private static boolean canPasteIn(ISourceReference ref, ISourceReference[] elements){
		if (ref instanceof IImportContainer)
			return canPasteInImportContainer(elements);	
		if (ref instanceof IType)
			return canPasteInType(elements);
		if (ref instanceof ICompilationUnit)
			return canPasteInCompilationUnit(elements);
		
		return false;	
	}
	
	private static boolean canPasteInImportContainer(ISourceReference[] elements){
		return areAllValuesOfType(elements, IJavaElement.IMPORT_DECLARATION);
	}
	
	private static boolean canPasteInType(ISourceReference[] elements){
		return areAllMembers(elements);
	}
		
	private static boolean canPasteInCompilationUnit(ISourceReference[] elements){
		for (int i= 0; i < elements.length; i++) {
			if (! canPasteInCompilationUnit(getElementType(elements[i])))
				return false;
		}
		return true;
	}
		
	private static boolean canPasteInCompilationUnit(int type){
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
	
	private static boolean areAllValuesOfType(ISourceReference[] elements, int type){
		for (int i= 0; i < elements.length; i++) {
			if (getElementType(elements[i]) != type)
				return false;
		}
		return true;
	}
	
	private static boolean areAllMembers(ISourceReference[] elements){
		for (int i= 0; i < elements.length; i++) {
			if (! (elements[i] instanceof IMember))
				return false;
		}
		return true;
	}
};