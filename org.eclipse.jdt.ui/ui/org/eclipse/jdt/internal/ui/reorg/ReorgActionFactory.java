/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.reorg;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgRefactoring;

public class ReorgActionFactory {
	private static final String GROUP_NAME= IContextMenuConstants.GROUP_REORGANIZE;

	private ReorgActionFactory(){
	}
	public static SelectionDispatchAction createCutAction(IWorkbenchSite site, ISelectionProvider p){
		SelectionDispatchAction a1= new CutSourceReferencesToClipboardAction(site);
		p.addSelectionChangedListener(a1);
		return a1;
	}
	
	public static SelectionDispatchAction createCopyAction(IWorkbenchSite site, ISelectionProvider p){
		SelectionDispatchAction a1= new CopyResourcesToClipboardAction(site);
		SelectionDispatchAction a2= new CopySourceReferencesToClipboardAction(site);
		SelectionDispatchAction dual= new DualReorgAction(site, ReorgMessages.getString("ReorgGroup.copy"), ReorgMessages.getString("copyAction.description"), a1, a2);//$NON-NLS-1$ //$NON-NLS-2$
		p.addSelectionChangedListener(dual);
		return dual;
	}
	
	public static SelectionDispatchAction createPasteAction(IWorkbenchSite site, ISelectionProvider p){
		SelectionDispatchAction a1= new PasteResourcesFromClipboardAction(site);
		SelectionDispatchAction a2= new PasteSourceReferencesFromClipboardAction(site);
		SelectionDispatchAction dual= new DualReorgAction(site, ReorgMessages.getString("ReorgGroup.paste"), ReorgMessages.getString("ReorgGroup.pasteAction.description"), a1, a2);//$NON-NLS-1$ //$NON-NLS-2$
		p.addSelectionChangedListener(dual);
		return dual;
	}
	
	public static SelectionDispatchAction createDeleteAction(IWorkbenchSite site, ISelectionProvider p){
		DeleteResourcesAction a1= new DeleteResourcesAction(site);
		DeleteSourceReferencesAction a2= new DeleteSourceReferencesAction(site);
		DualReorgAction dual= new DualReorgAction(site, ReorgMessages.getString("ReorgGroup.delete"), ReorgMessages.getString("deleteAction.description"), a1, a2); //$NON-NLS-1$ //$NON-NLS-2$
		p.addSelectionChangedListener(dual);
		return dual;
	}
	
	public static SelectionDispatchAction createPasteAction(final ISourceReference[] elements, Object target){
		return new PasteSourceReferencesFromClipboardAction(new MockUnifiedSite(new Object[]{target})){
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
		return new DeleteSourceReferencesAction(new MockUnifiedSite(elements));
	}	
	
	public static JdtCopyAction createDnDCopyAction(List elems, final IResource destination){
		JdtCopyAction action= new JdtCopyAction(new MockUnifiedSite(elems)){
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