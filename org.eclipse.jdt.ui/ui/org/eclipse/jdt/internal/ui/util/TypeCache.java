/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.util;import java.util.ArrayList;import java.util.List;import org.eclipse.jdt.core.ElementChangedEvent;import org.eclipse.jdt.core.IElementChangedListener;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IJavaElementDelta;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.search.IJavaSearchScope;import org.eclipse.jdt.internal.core.search.JavaWorkspaceScope;import org.eclipse.jdt.internal.ui.util.AllTypesSearchEngine;import org.eclipse.jface.operation.IRunnableContext;/** * Cache used by AllTypesSeachEngine */
class TypeCache{

	private static int fgLastStyle= -1;
	private static List fgTypeList;
	private static boolean fgIsRegistered= false;		//no instances	private TypeCache(){	}
	static List getCachedTypes() {		//must not return null		if (fgTypeList == null)			return new ArrayList(0); 		else				return fgTypeList;	}		static boolean canReuse(int style, IJavaSearchScope scope){		if (style != fgLastStyle)			return false;		if (! (scope instanceof JavaWorkspaceScope))			return false;		if (fgTypeList == null)			return false;		if (fgTypeList.isEmpty())			return false;			return true;			}		static void flush(){		fgTypeList= null;	}		static void setConfiguration(int style){		fgLastStyle= style;	}		static void setCachedTypes(List types){ //copy is passed here - no reason to copy again		fgTypeList= types;	}		static void registerIfNecessary(){		if (fgIsRegistered)			return;		JavaCore.addElementChangedListener(new DeltaListener());		fgIsRegistered= true;	}	
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
	
			//special case: if it's a type then the type itself must be added or removed
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
			if (mustStopProcessing(delta))
				return true;
	
			if (mustFlush(delta)) {
				flush();
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
	
		private static boolean mustStopProcessing(IJavaElementDelta delta) {
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