package org.eclipse.jdt.internal.ui.reorg;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

class SourceReferenceClipboard {

	private static Map fgMap= new HashMap(0);
	
	//no instances
	private SourceReferenceClipboard(){}
	
	public static boolean isEmpty(){
		return fgMap.isEmpty();
	}

	public static void clear(){
		fgMap.clear();
	}
	
	/**
	 * @return Map: String -> Integer (source to element type)
	 */
	public static Map getContents(){
		return fgMap;
	}
	
	public static void setContent(ISourceReference[] content){
		setContent(convertToMap(content));
	}
	
	private static void setContent(Map map){
		fgMap= map;
	}
	
	/**
	 * Converts the given <code>ISourceReference</code> array to a <code>Map</code>
	 * between <code>String</code> (element's source) and <code>Integer</code>
	 * (the element's type - as specified by <code>IJavaElements</code>).
	 * All elements that do not exist will not be taken into account.
	 * @return Map: String -> Integer (source to element type)
	 * @see IJavaElement
	 */
	public static Map convertToMap(ISourceReference[] content){
		if (content == null)
			return new HashMap(0);
			
		Map result= new HashMap();
		for (int i= 0; i < content.length; i++) {
			ISourceReference elem= content[i];
			if (!(elem instanceof IJavaElement))
				continue;
			IJavaElement je= (IJavaElement)elem;
			if (! je.exists())
				continue;
			try{
				result.put(SourceReferenceSourceRangeComputer.computeSource(elem), new Integer(je.getElementType()));
			} catch (JavaModelException e){
				//ignore
			}	
		}
		return result;
	}
}

