/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dialogs;

import java.util.ArrayList;import java.util.List;import org.eclipse.swt.widgets.Shell;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.jface.operation.IRunnableContext;import org.eclipse.jface.util.Assert;import org.eclipse.jdt.core.ElementChangedEvent;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IElementChangedListener;import org.eclipse.jdt.core.IField;import org.eclipse.jdt.core.IImportContainer;import org.eclipse.jdt.core.IImportDeclaration;import org.eclipse.jdt.core.IInitializer;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IJavaElementDelta;import org.eclipse.jdt.core.IMethod;import org.eclipse.jdt.core.IPackageDeclaration;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.IWorkingCopy;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.util.AllTypesSearchEngine;import org.eclipse.jdt.internal.ui.util.JdtHackFinder;import org.eclipse.jdt.internal.ui.util.TypeRef;import org.eclipse.jdt.internal.ui.util.TypeRefLabelProvider;import org.eclipse.jdt.core.search.IJavaSearchScope;


/**
 * A dialog to select a type from a list of types.
 */
public class TypeSelectionDialog extends TwoPaneElementSelector {

	private IRunnableContext fRunnableContext;
	private IJavaSearchScope fScope;
	private int fStyle;
	
	private final static String PREFIX= "type_selector.";
	private final static String NO_MAPPING_PREFIX= PREFIX+"no_mapping.";
	
	private static List fgAllTypesList;
	private static IElementChangedListener fgListener;
			
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
		if (fgAllTypesList == null){
			fgAllTypesList= engine.searchTypes(fRunnableContext, fScope, fStyle);
			if (fgListener == null){
				fgListener= new DeltaListener();
				JavaCore.addElementChangedListener(fgListener);
			}	
		}	

		if (fgAllTypesList.isEmpty())
			return CANCEL;
			
		TypeRef[] typeRefs= (TypeRef[])fgAllTypesList.toArray(new TypeRef[fgAllTypesList.size()]);
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
	
	private static class DeltaListener implements IElementChangedListener{
		public void elementChanged(ElementChangedEvent event){
			if (fgAllTypesList == null)
				return;
				
			IJavaElementDelta delta= event.getDelta();
			IJavaElement element= delta.getElement();
			int type= element.getElementType();
			
			if (type == IJavaElement.CLASS_FILE)
				return;				
			
			processDelta(delta);
		}
		
		private boolean mustFlush(IJavaElementDelta delta){
			if (delta.getKind() != IJavaElementDelta.CHANGED)
				return true;
			
			//if it's a cu we wait
			if (delta.getElement().getElementType() == IJavaElement.COMPILATION_UNIT) 
				return false;
			
			//must be only children
			if (delta.getFlags() != IJavaElementDelta.F_CHILDREN)
				return true;
			
			//special case: if it's a type then _it_ must be added or removed
			if (delta.getElement().getElementType() == IJavaElement.TYPE) 
				return false;
				
			if ((delta.getAddedChildren() != null) && (delta.getAddedChildren().length != 0))
				return true;
				
			return false;	
		}
		
		/*
		 * returns false iff list is flushed and we can stop processing
		 */
		private boolean processDelta(IJavaElementDelta delta){
			if (shouldStopProcessing(delta))
				return true;
				
			if (mustFlush(delta)){
				fgAllTypesList= null;
				return false;
			}	
			IJavaElementDelta[] affectedChildren= delta.getAffectedChildren();
			if (affectedChildren == null)
				return true;
				
			for (int i= 0; i < affectedChildren.length; i++){
				if (! processDelta(affectedChildren[i]))
					return false;
			}	
			return true;
		}
		
		private static boolean shouldStopProcessing(IJavaElementDelta delta){
			int type= delta.getElement().getElementType();
			if (type == IJavaElement.FIELD)
				return true;
			if (type == IJavaElement.METHOD)
				return true;
			if (type == IJavaElement.INITIALIZER)
				return true;
			if (type == IJavaElement.PACKAGE_DECLARATION)
				return true;
			if (type == IJavaElement.IMPORT_CONTAINER)
				return true;	
			if (type == IJavaElement.IMPORT_DECLARATION)
				return true;		
			return false;	
		}
	}
}