package org.eclipse.jdt.internal.ui.reorg;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;

import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.part.ResourceTransfer;

import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;

public class CopySourceReferencesToClipboardAction extends SourceReferenceAction{

	private Clipboard fClipboard;
	private SelectionDispatchAction fPasteAction;

	protected CopySourceReferencesToClipboardAction(IWorkbenchSite site, Clipboard clipboard, SelectionDispatchAction pasteAction) {
		super(site);
		Assert.isNotNull(clipboard);
		fClipboard= clipboard;
		fPasteAction= pasteAction;
	}

	protected void perform(IStructuredSelection selection) throws JavaModelException {
		copyToOSClipbard(getElementsToProcess(selection));
	}
	
	private void copyToOSClipbard(ISourceReference[] refs)  throws JavaModelException {
		try{
			fClipboard.setContents(createClipboardInput(refs), createTransfers());
					
			// update the enablement of the paste action
			// workaround since the clipboard does not suppot callbacks				
			if (fPasteAction != null && fPasteAction.getSelection() != null)
				fPasteAction.update(fPasteAction.getSelection());
			
		} catch (SWTError e){
			if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD)
				throw e;
			if (MessageDialog.openQuestion(getShell(), ReorgMessages.getString("CopyToClipboardProblemDialog.title"), ReorgMessages.getString("CopyToClipboardProblemDialog.message"))) //$NON-NLS-1$ //$NON-NLS-2$
				copyToOSClipbard(refs);
		}	
	}
		
	private static Object[] createClipboardInput(ISourceReference[] refs) throws JavaModelException {
		TypedSource[] typedSources= convertToTypedSourceArray(refs);
		return new Object[] { convertToInputForTextTransfer(typedSources), typedSources, getResourcesForMainTypes(refs)};
	}
	private static Transfer[] createTransfers() {
		return new Transfer[] { TextTransfer.getInstance(), TypedSourceTransfer.getInstance(), ResourceTransfer.getInstance()};
	}

	private static String convertToInputForTextTransfer(TypedSource[] typedSources) throws JavaModelException {
		String lineDelim= System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		StringBuffer buff= new StringBuffer();
		for (int i= 0; i < typedSources.length; i++) {
			buff.append(typedSources[i].getSource()).append(lineDelim);
		}
		return buff.toString();
	}

	private static TypedSource[] convertToTypedSourceArray(ISourceReference[] refs) throws JavaModelException {
		TypedSource[] elems= new TypedSource[refs.length];
		for (int i= 0; i < refs.length; i++) {
			elems[i]= new TypedSource(refs[i]);
		}
		return elems;
	}
	
	private static IResource[] getResourcesForMainTypes(ISourceReference[] refs){
		IType[] mainTypes= getMainTypes(refs);
		List resources= new ArrayList();
		for (int i= 0; i < mainTypes.length; i++) {
			IResource resource= getResource(mainTypes[i]);
			if (resource != null)
				resources.add(resource);
		}
		return (IResource[]) resources.toArray(new IResource[resources.size()]);
	}
	
	private static IType[] getMainTypes(ISourceReference[] refs){
		List mainTypes= new ArrayList();
		for (int i= 0; i < refs.length; i++) {
			try {
				if ((refs[i] instanceof IType) && JavaElementUtil.isMainType((IType)refs[i]))
					mainTypes.add(refs[i]);
			} catch(JavaModelException e) {
				JavaPlugin.log(e);//cannot show dialog
			}
		}
		return (IType[]) mainTypes.toArray(new IType[mainTypes.size()]);
	}
	
	private static IResource getResource(IType type){
		try {
			return ResourceUtil.getResource(type);
		} catch(JavaModelException e) {
			JavaPlugin.log(e);//cannot show a dialog here
			return null;
		}
	}

}
