package org.eclipse.jdt.internal.ui.reorg;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.swt.custom.BusyIndicator;

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
import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringAction;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class PasteSourceReferencesAction extends RefactoringAction {

	/**
	 * Constructor for PasteSourceReferencesAction.
	 * @param provider
	 */
	public PasteSourceReferencesAction(StructuredSelectionProvider provider) {
		super("&Paste", provider);
	}

	/*
	 * @see RefactoringAction#canOperateOn(IStructuredSelection)
	 */
	public boolean canOperateOn(IStructuredSelection selection) {
		try{
			if (SourceReferenceClipboard.isEmpty())
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
	
	private static boolean canPaste(ISourceReference ref, Map elements){
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
		Map elems= getClipboardContents();
		
		IJavaElement element= (IJavaElement)selected;
		int tabWidth= CodeFormatterPreferencePage.getTabSize();
		for (Iterator iter= elems.keySet().iterator(); iter.hasNext();) {
			String[] source= new String[]{(String) iter.next()};
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
		Map elems= getClipboardContents();
		
		for (Iterator iter= elems.keySet().iterator(); iter.hasNext();) {
			String source= (String) iter.next();
			int type= ((Integer)elems.get(source)).intValue();
			tbe.addTextEdit(new PasteInCompilationUnitEdit(source, type, unit));
		}
		if (! tbe.canPerformEdits())
			return; ///XXX
		tbe.performEdits(new NullProgressMonitor());	
		TextBuffer.commitChanges(tb, false, new NullProgressMonitor());
		TextBuffer.release(tb);		
	}
	
	private static Map getClipboardContents(){
		Assert.isTrue(! SourceReferenceClipboard.isEmpty());
		return SourceReferenceClipboard.getContents();
	}
	
	private static boolean canPasteAfter(ISourceReference ref, Map elements){
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
	
	private static boolean canPasteAfterType(Map elems){
		return areAllValuesOfType(elems, IJavaElement.TYPE);
	}

	private static boolean canPasteAtTopLevel(Map elems){
		for (Iterator iter= elems.values().iterator(); iter.hasNext();) {
			Integer type= (Integer) iter.next();
			if (! canPasteAfterImportContainerOrDeclaration(type.intValue()))
				return false;
		}
		return true;
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

	private static boolean canPasteAfterMember(Map elems){
		return areAllMembers(elems);
	}
	
	private static boolean canPasteIn(ISourceReference ref, Map elements){
		if (ref instanceof IImportContainer)
			return canPasteInImportContainer(elements);	
		if (ref instanceof IType)
			return canPasteInType(elements);
		if (ref instanceof ICompilationUnit)
			return canPasteInCompilationUnit(elements);
		
		return false;	
	}
	
	private static boolean canPasteInImportContainer(Map elements){
		return areAllValuesOfType(elements, IJavaElement.IMPORT_DECLARATION);
	}
	
	private static boolean canPasteInType(Map elements){
		return areAllMembers(elements);
	}
		
	private static boolean canPasteInCompilationUnit(Map elements){
		for (Iterator iter= elements.values().iterator(); iter.hasNext();) {
			Integer type= (Integer) iter.next();
			if (! canPasteInCompilationUnit(type.intValue()))
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
	
	private static boolean areAllValuesOfType(Map elements, int type){
		for (Iterator iter= elements.values().iterator(); iter.hasNext();) {
			Integer value= (Integer)iter.next();
			if (value.intValue() != type)
				return false;		
		}
		return true;
	}
	
	private static boolean areAllMembers(Map elements){
		for (Iterator iter= elements.values().iterator(); iter.hasNext();) {
			Integer type= (Integer) iter.next();
			if (! isMember(type.intValue()))
				return false;	
		}
		return true;
	}
		
	private static boolean isMember(int type){
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
	
};