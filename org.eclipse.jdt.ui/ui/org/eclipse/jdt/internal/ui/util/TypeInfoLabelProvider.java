/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.util;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.JavaUIMessages;

public class TypeInfoLabelProvider extends LabelProvider {
	
	public static final int SHOW_FULLYQUALIFIED=		0x01;
	public static final int SHOW_PACKAGE_POSTFIX= 	0x02;
	public static final int SHOW_PACKAGE_ONLY= 		0x04;
	public static final int SHOW_ROOT_POSTFIX= 			0x08;
	
	private static final Image CLASS_ICON= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_CLASS);
	private static final Image INTERFACE_ICON= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_INTERFACE);
	private static final Image PKG_ICON= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_PACKAGE);
	
	private int fFlags;
	
	public TypeInfoLabelProvider(int flags) {
		fFlags= flags;
	}	
	
	private boolean isSet(int flag) {
		return (fFlags & flag) != 0;
	}

	private String getPackageName(TypeInfo typeRef) {
		String packName= typeRef.getPackageName();
		if (packName.length() == 0)
			return JavaUIMessages.getString("TypeRefLabelProvider.default_package"); //$NON-NLS-1$
		else
			return packName;
	}

	/* non java-doc
	 * @see ILabelProvider#getText
	 */
	public String getText(Object element) {
		if (! (element instanceof TypeInfo)) 
			return super.getText(element);
		
		TypeInfo typeRef= (TypeInfo) element;
		StringBuffer buf= new StringBuffer();
		if (isSet(SHOW_PACKAGE_ONLY)) {
			buf.append(getPackageName(typeRef));
		}else {
			if (isSet(SHOW_FULLYQUALIFIED))
				buf.append(typeRef.getFullyQualifiedName());
			else
				buf.append(typeRef.getTypeQualifiedName());

			if (isSet(SHOW_PACKAGE_POSTFIX)) {
				buf.append(" - "); //$NON-NLS-1$
				buf.append(getPackageName(typeRef));
			}
		}
		if (isSet(SHOW_ROOT_POSTFIX)) {
			buf.append(" - "); //$NON-NLS-1$
			buf.append(typeRef.getPackageFragmentRootPath().toString());
		}
		return buf.toString();				
	}
	
	/* non java-doc
	 * @see ILabelProvider#getImage
	 */	
	public Image getImage(Object element) {
		if (! (element instanceof TypeInfo)) 
			return super.getImage(element);	
			
		if (isSet(SHOW_PACKAGE_ONLY))
			return PKG_ICON;
		else
			return ((TypeInfo) element).isInterface() ? INTERFACE_ICON : CLASS_ICON;
	}	
}