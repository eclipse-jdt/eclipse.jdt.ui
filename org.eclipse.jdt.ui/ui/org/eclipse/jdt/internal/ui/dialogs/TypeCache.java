/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.dialogs;import java.util.ArrayList;import java.util.List;import org.eclipse.jdt.core.ElementChangedEvent;import org.eclipse.jdt.core.IElementChangedListener;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IJavaElementDelta;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.search.IJavaSearchScope;import org.eclipse.jdt.internal.core.search.JavaWorkspaceScope;import org.eclipse.jdt.internal.ui.util.AllTypesSearchEngine;import org.eclipse.jface.operation.IRunnableContext;
public class TypeCache{

	private static int fgLastStyle= -1;
	private static List fgTypeList;
	private static boolean fgIsRegistered= false;		//no instances	private TypeCache(){	}

	public static List findTypes(AllTypesSearchEngine engine, int style, IRunnableContext runnableContext, IJavaSearchScope scope) {
				checkIfOkToReuse(style, scope);
		if (fgTypeList == null) {
			fgTypeList= engine.searchTypes(runnableContext, scope, style);
			if (!fgIsRegistered){
				JavaCore.addElementChangedListener(new DeltaListener());				fgIsRegistered= true;			}	
		}				//must not return null		if (fgTypeList == null)			return new ArrayList(0); 		else				return fgTypeList;
	}

	private static void checkIfOkToReuse(int style, IJavaSearchScope scope) {
		if (style != fgLastStyle)
			flushCache();
				if (! (scope instanceof JavaWorkspaceScope))			flushCache();					fgLastStyle= style;	
	}		private static void flushCache(){		fgTypeList= null;	}

	private static class DeltaListener implements IElementChangedListener {
		public void elementChanged(ElementChangedEvent event) {
			if (fgTypeList == null)
				return;
	
			IJavaElementDelta delta= event.getDelta();
			IJavaElement element= delta.getElement();			int type= element.getElementType();
	
			if (type == IJavaElement.CLASS_FILE)
				return;
	
			processDelta(delta);
		}
	
		private boolean mustFlush(IJavaElementDelta delta) {
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
	
			if ((delta.getAddedChildren() != null)
				&& (delta.getAddedChildren().length != 0))
				return true;
	
			return false;
		}
	
		/*
		 * returns false iff list is flushed and we can stop processing
		 */
		private boolean processDelta(IJavaElementDelta delta) {
			if (shouldStopProcessing(delta))
				return true;
	
			if (mustFlush(delta)) {
				flushCache();
				return false;
			}
			IJavaElementDelta[] affectedChildren= delta.getAffectedChildren();
			if (affectedChildren == null)
				return true;
	
			for (int i= 0; i < affectedChildren.length; i++) {
				if (!processDelta(affectedChildren[i]))
					return false;
			}
			return true;
		}
	
		private static boolean shouldStopProcessing(IJavaElementDelta delta) {
			int type= delta.getElement().getElementType();			if (type == IJavaElement.CLASS_FILE)				return true;
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