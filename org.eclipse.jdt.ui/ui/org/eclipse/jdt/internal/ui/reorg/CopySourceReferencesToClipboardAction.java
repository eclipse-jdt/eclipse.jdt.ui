package org.eclipse.jdt.internal.ui.reorg;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;

import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.ui.part.ResourceTransfer;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

class CopySourceReferencesToClipboardAction extends SourceReferenceAction {

	public CopySourceReferencesToClipboardAction(ISelectionProvider provider) {
		super("&Copy", provider);
	}
	
	protected void perform() throws JavaModelException {
		copyToOSClipbard(getElementsToProcess());
	}

	private static Clipboard getSystemClipboard() {
		return new Clipboard(JavaPlugin.getActiveWorkbenchShell().getDisplay());
	}
	
	private static void copyToOSClipbard(ISourceReference[] refs)  throws JavaModelException {
		getSystemClipboard().setContents(createClipboardInput(refs), createTransfers());
	}
		
	private static Object[] createClipboardInput(ISourceReference[] refs) throws JavaModelException {
		TypedSource[] typedSources= convertToTypedSourceArray(refs);
		return new Object[] { convertToInputForTextTransfer(typedSources), typedSources, getResourcesForMainTypes(refs)};
	}
	private static Transfer[] createTransfers() {
		return new Transfer[] { TextTransfer.getInstance(), TypedSourceTransfer.getInstance(), ResourceTransfer.getInstance()};
	}

	private static String convertToInputForTextTransfer(TypedSource[] typedSources) throws JavaModelException {
		String lineDelim= System.getProperty("line.separator", "\n"); //$NON-NLS-1$
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
			return Refactoring.getResource(type);
		} catch(JavaModelException e) {
			JavaPlugin.log(e);//cannot show a dialog here
			return null;
		}
	}
}

