/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.reorg;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;

import org.eclipse.swt.dnd.Clipboard;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgRefactoring;

public class ReorgActionFactory {
	private static final String GROUP_NAME= IContextMenuConstants.GROUP_REORGANIZE;

	private ReorgActionFactory(){
	}
	public static SelectionDispatchAction createCutAction(IWorkbenchSite site, Clipboard clipboard, SelectionDispatchAction pasteAction){
		String helpContextID= IJavaHelpContextIds.CUT_ACTION;
		SelectionDispatchAction a1= new CutSourceReferencesToClipboardAction(site, clipboard, pasteAction, helpContextID);
		return a1;
	}
	
	public static SelectionDispatchAction createCopyAction(IWorkbenchSite site, Clipboard clipboard, SelectionDispatchAction pasteAction){
		SelectionDispatchAction a1= new CopyResourcesToClipboardAction(site, clipboard, pasteAction);
		SelectionDispatchAction a2= new CopySourceReferencesToClipboardAction(site, clipboard, pasteAction);
		String helpContextID= IJavaHelpContextIds.COPY_ACTION;
		SelectionDispatchAction dual= new DualReorgAction(site, ReorgMessages.getString("ReorgGroup.copy"), ReorgMessages.getString("copyAction.description"), a1, a2, helpContextID);//$NON-NLS-1$ //$NON-NLS-2$
		return dual;
	}
	
	public static SelectionDispatchAction createPasteAction(IWorkbenchSite site, Clipboard clipboard){
		SelectionDispatchAction a1= new PasteResourcesFromClipboardAction(site, clipboard);
		SelectionDispatchAction a2= new PasteSourceReferencesFromClipboardAction(site, clipboard);
		String helpContextID= IJavaHelpContextIds.PASTE_ACTION;
		SelectionDispatchAction dual= new DualReorgAction(site, ReorgMessages.getString("ReorgGroup.paste"), ReorgMessages.getString("ReorgGroup.pasteAction.description"), a1, a2, helpContextID);//$NON-NLS-1$ //$NON-NLS-2$
		return dual;
	}
	
	public static SelectionDispatchAction createDeleteAction(IWorkbenchSite site){
		DeleteResourcesAction a1= new DeleteResourcesAction(site);
		DeleteSourceReferencesAction a2= new DeleteSourceReferencesAction(site);
		String helpContextID= IJavaHelpContextIds.DELETE_ACTION;
		DualReorgAction dual= new DualReorgAction(site, ReorgMessages.getString("ReorgGroup.delete"), ReorgMessages.getString("deleteAction.description"), a1, a2, helpContextID); //$NON-NLS-1$ //$NON-NLS-2$
		return dual;
	}
	
	public static SelectionDispatchAction createPasteAction(final ISourceReference[] elements, Object target){
		return new PasteSourceReferencesFromClipboardAction(new MockWorkbenchSite(new Object[]{target}), null){
			protected TypedSource[] getContentsToPaste(){
				List result= new ArrayList(elements.length);
				for(int i= 0; i < elements.length; i++){
					try {
						result.add(new TypedSource(elements[i]));
					} catch(JavaModelException e) {
						//ignore
					}
				}
				return (TypedSource[])result.toArray(new TypedSource[result.size()]);
			}
		};
	}
	
	public static DeleteSourceReferencesAction createDeleteSourceReferencesAction(ISourceReference[] elements){
		return new DeleteSourceReferencesAction(new MockWorkbenchSite(elements));
	}	
	
	public static JdtCopyAction createDnDCopyAction(List elems, final IResource destination){
		JdtCopyAction action= new JdtCopyAction(new MockWorkbenchSite(elems)){
			protected Object selectDestination(ReorgRefactoring ref) {
				return ClipboardActionUtil.tryConvertingToJava(destination);			
			}
		};
		return action;
	}
	
	public static JdtCopyAction createDnDCopyAction(IResource[] resourceData, final IResource destination){
		return createDnDCopyAction(ClipboardActionUtil.getConvertedResources(resourceData), destination);
	}	
}