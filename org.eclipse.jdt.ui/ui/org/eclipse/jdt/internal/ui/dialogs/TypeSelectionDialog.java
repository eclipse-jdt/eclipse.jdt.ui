/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dialogs;

import java.util.ArrayList;import java.util.List;import org.eclipse.swt.widgets.Shell;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.jface.operation.IRunnableContext;import org.eclipse.jface.util.Assert;import org.eclipse.jdt.core.ElementChangedEvent;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IElementChangedListener;import org.eclipse.jdt.core.IField;import org.eclipse.jdt.core.IImportContainer;import org.eclipse.jdt.core.IImportDeclaration;import org.eclipse.jdt.core.IInitializer;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IJavaElementDelta;import org.eclipse.jdt.core.IMethod;import org.eclipse.jdt.core.IPackageDeclaration;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.IWorkingCopy;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.util.AllTypesSearchEngine;import org.eclipse.jdt.internal.ui.util.TypeRef;import org.eclipse.jdt.internal.ui.util.TypeRefLabelProvider;import org.eclipse.jdt.core.search.IJavaSearchScope;


/**
 * A dialog to select a type from a list of types.
 */
public class TypeSelectionDialog extends TwoPaneElementSelector {

	private IRunnableContext fRunnableContext;
	private IJavaSearchScope fScope;
	private int fStyle;
	
	private final static String PREFIX= "type_selector.";
	private final static String NO_MAPPING_PREFIX= PREFIX+"no_mapping.";
	
			
	public TypeSelectionDialog(Shell parent, IRunnableContext context, IJavaSearchScope scope, int style, boolean ignoreCase, boolean matchEmtpyString) {
		super(parent, "", null, new TypeRefLabelProvider(0), new TypeRefLabelProvider(TypeRefLabelProvider.SHOW_PACKAGE_ONLY + TypeRefLabelProvider.SHOW_ROOT_POSTFIX), ignoreCase, matchEmtpyString);		
		fRunnableContext= context;
		Assert.isNotNull(fRunnableContext);
		fScope= scope;
		Assert.isNotNull(fScope);
		fStyle= style;
		setUpperListLabel(JavaPlugin.getResourceString(PREFIX + "typeListLabel"));
		setLowerListLabel(JavaPlugin.getResourceString(PREFIX + "packageListLabel"));
	}
	
	/**
	 * @private
	 */
	public int open() {
		AllTypesSearchEngine engine= new AllTypesSearchEngine(JavaPlugin.getWorkspace());
		
		List typeList= TypeCache.findTypes(engine, fStyle, fRunnableContext, fScope);
		
		if (typeList.isEmpty())
			return CANCEL;
			
		TypeRef[] typeRefs= (TypeRef[])typeList.toArray(new TypeRef[typeList.size()]);
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
					//XXX: java model
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