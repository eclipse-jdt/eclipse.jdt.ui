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
		return new Object[] { convertToInputForTextTransfer(typedSources), typedSources, getResourcesForPrimaryTypes(refs)};
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
	
	private static IResource[] getResourcesForPrimaryTypes(ISourceReference[] refs){
		IType[] primaryTypes= getPrimaryTypes(refs);
		List resources= new ArrayList();
		for (int i= 0; i < primaryTypes.length; i++) {
			IResource resource= getResource(primaryTypes[i]);
			if (resource != null)
				resources.add(resource);
		}
		return (IResource[]) resources.toArray(new IResource[resources.size()]);
	}
	
	private static IType[] getPrimaryTypes(ISourceReference[] refs){
		List primaryTypes= new ArrayList();
		for (int i= 0; i < refs.length; i++) {
			if (isPrimaryType(refs[i]))
				primaryTypes.add(refs[i]);
		}
		return (IType[]) primaryTypes.toArray(new IType[primaryTypes.size()]);
	}
	
	private static boolean isPrimaryType(ISourceReference ref){
		if (! (ref  instanceof IType))
			return false;
		if (! ref.exists())	
			return false;
		IType type= (IType)ref;
		if (type.getDeclaringType() != null)
			return false;
		return type.getElementName().equals(Signature.getQualifier(type.getCompilationUnit().getElementName()));
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

