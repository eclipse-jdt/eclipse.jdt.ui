/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.util.JavaModelUtil;


/**
 * Strategy for the construction of Java element UI labels.
 */
public class JavaTextLabelProvider extends org.eclipse.jdt.internal.ui.viewsupport.JavaTextLabelProvider {	

	/**
	 * Flag (bit mask) indicating that the label of a member should be fully qualified.
	 * For example, include the fully qualified name of a field.
	 */
	final static int SHOW_MEMBER_FULLY_QUALIFIED= 0x10000000;
	
	public JavaTextLabelProvider(int flags) {
		super(flags);
	}

	final boolean showMemberFullyQualified() {
		return (fFlags & SHOW_MEMBER_FULLY_QUALIFIED) != 0;
	}
	
	protected void renderPackageFragment(IPackageFragment element, StringBuffer buf) {
		String name= element.getElementName();
		if ("".equals(name)) { //$NON-NLS-1$
			buf.append(JavaUIMessages.getString("JavaTextLabelProvider.default_package")); //$NON-NLS-1$
		} else {
			buf.append(name);
		}
	}
	
	protected void renderMethod(IMethod method, StringBuffer buf) {
		try {
			if (showReturnTypes() && !method.isConstructor()) {
				buf.append(Signature.getSimpleName(Signature.toString(method.getReturnType())));
				buf.append(' ');
			}
			
			if (showMemberFullyQualified()) {
				buf.append(JavaModelUtil.getFullyQualifiedName(method.getDeclaringType()));
				buf.append("."); //$NON-NLS-1$
			}

			buf.append(method.getElementName());
			
			if (showParameters()) {
				buf.append('(');
				
				String[] types= method.getParameterTypes();
				if (types.length > 0) {
					buf.append(Signature.getSimpleName(Signature.toString(types[0])));
				}
				for (int i= 1; i < types.length; i++) {
					buf.append(", "); //$NON-NLS-1$
					buf.append(Signature.getSimpleName(Signature.toString(types[i])));
				}
				
				buf.append(')');
			}
			
			if (showContainer()) {
				buf.append(" - "); //$NON-NLS-1$
				renderName(method.getDeclaringType(), buf);
			}
			
		} catch (JavaModelException e) {
		}
	}
	
	protected void renderField(IField field, StringBuffer buf) {
		try {
			if (showMemberFullyQualified()) {
				buf.append(JavaModelUtil.getFullyQualifiedName(field.getDeclaringType()));
				buf.append("."); //$NON-NLS-1$
			}

			buf.append(field.getElementName());
			
			if (showContainer()) {
				buf.append(" - "); //$NON-NLS-1$
				renderName(field.getDeclaringType(), buf);
			} else if (showType()) {
				buf.append(" - "); //$NON-NLS-1$
				buf.append(Signature.toString(field.getTypeSignature()));
			}
		} catch (JavaModelException x) {
			// dont' show type on exception
		}
	}	
}