/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.packageview;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.reorg.CopyRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.MoveRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.AddMethodStubAction;
import org.eclipse.jdt.internal.ui.dnd.JdtViewerDropAdapter;
import org.eclipse.jdt.internal.ui.dnd.LocalSelectionTransfer;
import org.eclipse.jdt.internal.ui.dnd.TransferDropTargetListener;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.reorg.CopyQueries;
import org.eclipse.jdt.internal.ui.reorg.DeleteSourceReferencesAction;
import org.eclipse.jdt.internal.ui.reorg.JdtMoveAction;
import org.eclipse.jdt.internal.ui.reorg.MockWorkbenchSite;
import org.eclipse.jdt.internal.ui.reorg.ReorgActionFactory;
import org.eclipse.jdt.internal.ui.reorg.SimpleSelectionProvider;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class SelectionTransferDropAdapter extends JdtViewerDropAdapter implements TransferDropTargetListener {

	private List fElements;
	private MoveRefactoring fMoveRefactoring;
	private int fCanMoveElements;
	private CopyRefactoring fCopyRefactoring;
	private int fCanCopyElements;
	private ISelection fSelection;
	private AddMethodStubAction fAddMethodStubAction;
	
	private static final int DROP_TIME_DIFF_TRESHOLD= 150;

	public SelectionTransferDropAdapter(StructuredViewer viewer) {
		super(viewer, DND.FEEDBACK_SCROLL | DND.FEEDBACK_EXPAND);
		fAddMethodStubAction= new AddMethodStubAction();
	}

	//---- TransferDropTargetListener interface ---------------------------------------
	
	public Transfer getTransfer() {
		return LocalSelectionTransfer.getInstance();
	}

	//---- Actual DND -----------------------------------------------------------------
	
	public void dragEnter(DropTargetEvent event) {
		clear();
		super.dragEnter(event);
	}
	
	public void dragLeave(DropTargetEvent event) {
		clear();
		super.dragLeave(event);
	}
	
	private void clear() {
		fElements= null;
		fSelection= null;
		fMoveRefactoring= null;
		fCanMoveElements= 0;
		fCopyRefactoring= null;
		fCanCopyElements= 0;
	}
	
	public void validateDrop(Object target, DropTargetEvent event, int operation) {
		event.detail= DND.DROP_NONE;
		
		if (tooFast(event)) 
			return;
		
		initializeSelection();
				
		try {
			if (operation == DND.DROP_DEFAULT) {
				event.detail= handleValidateDefault(target, event);
			} else if (operation == DND.DROP_COPY) {
				event.detail= handleValidateCopy(target, event);
			} else if (operation == DND.DROP_MOVE) {
				event.detail= handleValidateMove(target, event);
			} else if (operation == DND.DROP_LINK) {
				event.detail= handleValidateLink(target, event);
			}
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, PackagesMessages.getString("SelectionTransferDropAdapter.error.title"), PackagesMessages.getString("SelectionTransferDropAdapter.error.message")); //$NON-NLS-1$ //$NON-NLS-2$
			event.detail= DND.DROP_NONE;
		}	
	}

	protected void initializeSelection(){
		if (fElements != null)
			return;
		ISelection s= LocalSelectionTransfer.getInstance().getSelection();
		if (!(s instanceof IStructuredSelection))
			return;
		fSelection= s;	
		fElements= ((IStructuredSelection)s).toList();
	}
	
	protected ISelection getSelection(){
		return fSelection;
	}
	
	private boolean tooFast(DropTargetEvent event) {
		return Math.abs(LocalSelectionTransfer.getInstance().getSelectionSetTime() - event.time) < DROP_TIME_DIFF_TRESHOLD;
	}	

	public void drop(Object target, DropTargetEvent event) {
		try{
			if (event.detail == DND.DROP_MOVE) {
				handleDropMove(target, event);
				
				if (! canPasteSourceReferences(target))
					return;
				DeleteSourceReferencesAction delete= ReorgActionFactory.createDeleteSourceReferencesAction(getDragableSourceReferences());
				delete.setAskForDeleteConfirmation(true);
				delete.setCanDeleteGetterSetter(false);
				delete.update(delete.getSelection());
				if (delete.isEnabled())
					delete.run();
				
			} else if (event.detail == DND.DROP_COPY) {
				handleDropCopy(target, event);
			} else if (event.detail == DND.DROP_LINK) {
				handleDropLink(target, event);
			}
			
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, PackagesMessages.getString("SelectionTransferDropAdapter.error.title"), PackagesMessages.getString("SelectionTransferDropAdapter.error.message")); //$NON-NLS-1$ //$NON-NLS-2$
		}	finally{
			// The drag source listener must not perform any operation
			// since this drop adapter did the remove of the source even
			// if we moved something.
			event.detail= DND.DROP_NONE;
		}
	}
	
	private int handleValidateDefault(Object target, DropTargetEvent event) throws JavaModelException{
		if (target == null)
			return DND.DROP_NONE;
			
		if (canPasteSourceReferences(target))	
			return handleValidateCopy(target, event);
		else
			return handleValidateMove(target, event);	
	}
	
	private int handleValidateMove(Object target, DropTargetEvent event) throws JavaModelException{
		if (target == null)
			return DND.DROP_NONE;
		
		if (canPasteSourceReferences(target)){
			if (canMoveSelectedSourceReferences(target))
				return DND.DROP_MOVE;
			else	
				return DND.DROP_NONE;
		}	
		
		if (fMoveRefactoring == null){
			fMoveRefactoring= new MoveRefactoring(fElements, JavaPreferencesSettings.getCodeGenerationSettings());
		}	
		
		if (!canMoveElements())
			return DND.DROP_NONE;	
		
		if (fMoveRefactoring.isValidDestination(target))
			return DND.DROP_MOVE;
		else
			return DND.DROP_NONE;	
	}
	
	private boolean canMoveElements() {
		if (fCanMoveElements == 0) {
			fCanMoveElements= 2;
			if (! canActivate(fMoveRefactoring))
				fCanMoveElements= 1;
		}
		return fCanMoveElements == 2;
	}
	
	private boolean canActivate(ReorgRefactoring ref){
		try{
			return ref.checkActivation(new NullProgressMonitor()).isOK();
		} catch(JavaModelException e){
			ExceptionHandler.handle(e, PackagesMessages.getString("SelectionTransferDropAdapter.error.title"), PackagesMessages.getString("SelectionTransferDropAdapter.error.message")); //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		}
	}

	private void handleDropLink(Object target, DropTargetEvent event) {
		if (fAddMethodStubAction.init((IType)target, getSelection())) 
			fAddMethodStubAction.run();
	}

	private int handleValidateLink(Object target, DropTargetEvent event) {
		if (target instanceof IType && AddMethodStubAction.canActionBeAdded((IType)target, getSelection()))
			return DND.DROP_LINK;
		else		
			return DND.DROP_NONE;
	}
	
	private void handleDropMove(final Object target, DropTargetEvent event) throws JavaModelException{
		if (canPasteSourceReferences(target)){
			pasteSourceReferences(target, event);
			return;
		}
		new DragNDropMoveAction(new SimpleSelectionProvider(fElements), target).run();
	}

	private void pasteSourceReferences(final Object target, DropTargetEvent event) {
		SelectionDispatchAction pasteAction= ReorgActionFactory.createPasteAction(getDragableSourceReferences(), target);
		pasteAction.update(pasteAction.getSelection());
		if (!pasteAction.isEnabled()){
			event.detail= DND.DROP_NONE;
			return;
		}	
		pasteAction.run();	
		
		return;
	}
	
	private int handleValidateCopy(Object target, DropTargetEvent event) throws JavaModelException{

		if (canPasteSourceReferences(target))
			return DND.DROP_COPY;
		
		if (fCopyRefactoring == null)
			fCopyRefactoring= new CopyRefactoring(fElements, new CopyQueries());
		
		if (!canCopyElements())
			return DND.DROP_NONE;	

		if (fCopyRefactoring.isValidDestination(target))
			return DND.DROP_COPY;
		else
			return DND.DROP_NONE;					
	}

	private boolean canMoveSelectedSourceReferences(Object target) throws JavaModelException{
		ICompilationUnit targetCu= getCompilationUnit(target);
		if (targetCu == null)
			return false;
			
		ISourceReference[] elements= getDragableSourceReferences();		
		for (int i= 0; i < elements.length; i++) {
			if (targetCu.equals(SourceReferenceUtil.getCompilationUnit(elements[i])))	
				return false;
		}
		return true;
	}
	
	private static ICompilationUnit getCompilationUnit(Object target){
		if (target instanceof ISourceReference)
			return SourceReferenceUtil.getCompilationUnit((ISourceReference)target);
		else
			return null;	
	}
	
	private boolean canPasteSourceReferences(Object target) throws JavaModelException{
		ISourceReference[] elements= getDragableSourceReferences();
		if (elements.length != fElements.size())
			return false;
		SelectionDispatchAction pasteAction= ReorgActionFactory.createPasteAction(elements, target);
		pasteAction.update(pasteAction.getSelection());
		return pasteAction.isEnabled();
	}
	
	private ISourceReference[] getDragableSourceReferences(){
		List result= new ArrayList(fElements.size());
		for(Iterator iter= fElements.iterator(); iter.hasNext();){
			Object each= iter.next();
			if (isDragableSourceReferences(each))
				result.add(each);
		}
		return (ISourceReference[])result.toArray(new ISourceReference[result.size()]);
	}
	
	private static boolean isDragableSourceReferences(Object element) {
		if (!(element instanceof ISourceReference))
			return false;
		if (!(element instanceof IJavaElement))
			return false;
		if (element instanceof ICompilationUnit)
			return false;
		
		return true;	
	}
			
	private boolean canCopyElements() {
		if (fCanCopyElements == 0) {
			fCanCopyElements= 2;
			if (!canActivate(fCopyRefactoring))
				fCanCopyElements= 1;
		}
		return fCanCopyElements == 2;
	}		
	
	private void handleDropCopy(final Object target, DropTargetEvent event) throws JavaModelException{
		if (canPasteSourceReferences(target)){
			pasteSourceReferences(target, event);
			return;
		}
		
		SelectionDispatchAction action= ReorgActionFactory.createDnDCopyAction(fElements, ResourceUtil.getResource(target));
		action.run();
	}
	
	//--
	private static class DragNDropMoveAction extends JdtMoveAction{
		private Object fTarget;
		private static final int PREVIEW_ID= IDialogConstants.CLIENT_ID + 1;
		
		public DragNDropMoveAction(ISelectionProvider provider, Object target){
			super(new MockWorkbenchSite(provider));
			Assert.isNotNull(target);
			fTarget= target;
		}
		
		protected Object selectDestination(ReorgRefactoring ref) {
			return fTarget;
		}
		
		protected boolean isOkToProceed(ReorgRefactoring refactoring) throws JavaModelException{
			if (!super.isOkToProceed(refactoring))
				return false;
			return askIfUpdateReferences((MoveRefactoring)refactoring);
		}
		
		//returns false iff canceled or error
		private boolean askIfUpdateReferences(MoveRefactoring ref) throws JavaModelException{
			if (! ref.canUpdateReferences()){
				setShowPreview(false);
				return true;
			}	
			switch (askIfUpdateReferences()){
				case IDialogConstants.CANCEL_ID:
				   setShowPreview(false);
					return false;
				case IDialogConstants.NO_ID:
					ref.setUpdateReferences(false);
					setShowPreview(false);
					return true;	
				case IDialogConstants.YES_ID:
					ref.setUpdateReferences(true);
					setShowPreview(false);
					return true;
				case PREVIEW_ID:		 
					ref.setUpdateReferences(true);
					setShowPreview(true);
					return true;
				default: 
					Assert.isTrue(false); //not expected to get here
					return false;
			}
		}
		
		private static int askIfUpdateReferences(){
			Shell shell= JavaPlugin.getActiveWorkbenchShell().getShell();
			String title= PackagesMessages.getString("SelectionTransferDropAdapter.dialog.title"); //$NON-NLS-1$
			String preview= PackagesMessages.getString("SelectionTransferDropAdapter.dialog.preview.label"); //$NON-NLS-1$
			String question= PackagesMessages.getString("SelectionTransferDropAdapter.dialog.question"); //$NON-NLS-1$
			
			String[] labels= new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL,
																 preview,  IDialogConstants.CANCEL_LABEL };
			final MessageDialog dialog = new MessageDialog(shell, title, null, question, MessageDialog.QUESTION,	labels, 2); //preview is default
			shell.getDisplay().syncExec(new Runnable() {
				public void run() {
					dialog.open();
				}
			});
			int result = dialog.getReturnCode();
			if (result == 0)
				return IDialogConstants.YES_ID;
			if (result == 1)
				return IDialogConstants.NO_ID;
			if (result == 2)
				return PREVIEW_ID;
			return IDialogConstants.CANCEL_ID;
		}
	}
}