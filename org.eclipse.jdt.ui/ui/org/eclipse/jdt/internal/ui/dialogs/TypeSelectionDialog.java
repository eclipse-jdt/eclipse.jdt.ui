package org.eclipse.jdt.internal.ui.dialogs;
/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000, 2001
 */

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.AllTypesSearchEngine;
import org.eclipse.jdt.internal.ui.util.JdtHackFinder;
import org.eclipse.jdt.internal.ui.util.TypeRef;
import org.eclipse.jdt.internal.ui.util.TypeRefLabelProvider;


/**
 * A dialog to select a type from a list of types.
 */
public class TypeSelectionDialog extends TwoPaneElementSelector {

	private IRunnableContext fRunnableContext;
	private IJavaSearchScope fScope;
	private int fStyle;
	
	private final static String PREFIX= "type_selector.";
	private final static String NO_MAPPING_PREFIX= PREFIX+"no_mapping.";
	
	private static class PackageRenderer extends LabelProvider {
		private final Image PACKAGE_ICON= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_PACKAGE);
		
		public String getText(Object element) {
			String packName= ((TypeRef) element).getPackageName();
			if (packName.length() == 0) {
				return JavaPlugin.getResourceString("DefaultPackage.label");
			} else {
				return packName;
			}							
		}
		public Image getImage(Object element) {
			return PACKAGE_ICON;
		}
	}
		
	public TypeSelectionDialog(Shell parent, IRunnableContext context, IJavaSearchScope scope, int style, boolean ignoreCase, boolean matchEmtpyString) {
		super(parent, "", null, new TypeRefLabelProvider(0), new PackageRenderer(), ignoreCase, matchEmtpyString);		
		fRunnableContext= context;
		Assert.isNotNull(fRunnableContext);
		fScope= scope;
		Assert.isNotNull(fScope);
		fStyle= style;
	}
	
	/**
	 * @private
	 */
	public int open() {
		AllTypesSearchEngine engine= new AllTypesSearchEngine(JavaPlugin.getWorkspace());
		List typesFound= engine.searchTypes(fRunnableContext, fScope, fStyle);

		if (typesFound.size() == 0)
			return CANCEL;
			
		TypeRef[] typeRefs= (TypeRef[])typesFound.toArray(new TypeRef[typesFound.size()]);
		setElements(typeRefs);
		setInitialSelection("A");
		return super.open();
	}
	
	/**
	 * @private
	 */
	protected void computeResult() {
		TypeRef ref= (TypeRef)getWidgetSelection();
		if (ref != null) {
			try {
				IType type= ref.resolveType(fScope);
				if (type == null) {
					String title= JavaPlugin.getResourceString(NO_MAPPING_PREFIX+"title");
					String message= JavaPlugin.getResourceString(NO_MAPPING_PREFIX+"message");
					MessageDialog.openError(getShell(), title, message);
					JdtHackFinder.fixme("java model");
					setResult(null);
				} else {
					List result= new ArrayList(1);
					result.add(type);
					setResult(result);
				}
			} catch (JavaModelException e) {
				String title= JavaPlugin.getResourceString(NO_MAPPING_PREFIX+"title");
				String message= JavaPlugin.getResourceString(NO_MAPPING_PREFIX+"message");
				MessageDialog.openError(getShell(), title, message);
				setResult(null);
			}
		}
	}
}