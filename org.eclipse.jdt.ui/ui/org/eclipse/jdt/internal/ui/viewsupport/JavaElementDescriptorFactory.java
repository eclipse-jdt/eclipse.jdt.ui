package org.eclipse.jdt.internal.ui.viewsupport;


import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.packageview.ErrorTickManager;
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;

public class JavaElementDescriptorFactory implements IOverlayDescriptorFactory  {
	
	/**
	 * Although all methods of a Java interface are abstract, the 
	 * abstract icon should not be shown.
	 */
	protected boolean confirmAbstract(IMember member) {
		IType t= member.getDeclaringType();
		if (t == null && member instanceof IType)
			t= (IType) member;
		if (t != null) {
			try {
				return !t.isInterface();
			} catch (JavaModelException x) {
			}
		}
		return true;
	}
	
	/**
	 * Synchronized types are allowed but meaningless.
	 */
	protected boolean confirmSynchronized(IMember member) {
		return !(member instanceof IType);
	}
	
	public IOverlayDescriptor createDescriptor(String baseName, Object element) {
		
		if (!(element instanceof IJavaElement))
			return new JavaOverlayDescriptor(baseName, 0);

		int flags= 0;

		IJavaElement jElement= (IJavaElement)element;
		ErrorTickManager errorManager= JavaPlugin.getErrorTickManager();
		boolean[] errorInfo= errorManager.getErrorInfo(jElement);
		if (errorInfo[1]) {
			flags|=JavaOverlayDescriptor.ERROR;
		} else if (errorInfo[0]) {
			flags|=JavaOverlayDescriptor.WARNING;
		}
					
		if (element instanceof ISourceReference) { 
			ISourceReference sourceReference= (ISourceReference)element;
			int modifiers= getModifiers(sourceReference);
		
			if (Flags.isAbstract(modifiers) && confirmAbstract((IMember) sourceReference))
				flags |= JavaOverlayDescriptor.ABSTRACT;
			if (Flags.isFinal(modifiers))
				flags |= JavaOverlayDescriptor.FINAL;
			if (Flags.isSynchronized(modifiers) && confirmSynchronized((IMember) sourceReference))
				flags |= JavaOverlayDescriptor.SYNCHRONIZED;
			if (Flags.isStatic(modifiers))
				flags |= JavaOverlayDescriptor.STATIC;
				
			if (sourceReference instanceof IType) {
				if (JavaModelUtility.hasMainMethod((IType)sourceReference))
					flags |= JavaOverlayDescriptor.RUNNABLE;
			}
		}
		return new JavaOverlayDescriptor(baseName, flags);
	}
	
	protected int getModifiers(ISourceReference sourceReference) {
		if (sourceReference instanceof IMember) {
			try {
				return ((IMember) sourceReference).getFlags();
			} catch (JavaModelException x) {
			}
		}
		return 0;
	}
}	
