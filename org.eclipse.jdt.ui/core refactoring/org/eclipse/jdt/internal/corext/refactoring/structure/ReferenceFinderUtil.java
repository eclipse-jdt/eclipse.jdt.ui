package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultCollector;

public class ReferenceFinderUtil {
	
	//no instances
	private ReferenceFinderUtil(){
	}

	//----- referenced types -
	
	public static IType[] getTypesReferencedIn(IMember[] elements, IProgressMonitor pm) throws JavaModelException {
		List referencedTypes= new ArrayList();
		pm.beginTask("", elements.length);
		for (int i = 0; i < elements.length; i++) {
			referencedTypes.addAll(getTypesReferencedIn(elements[i], new SubProgressMonitor(pm, 1)));
		}
		pm.done();
		return (IType[]) referencedTypes.toArray(new IType[referencedTypes.size()]);
	}
	
	private static List getTypesReferencedIn(IMember element, IProgressMonitor pm) throws JavaModelException {
		SearchResultCollector collector= new SearchResultCollector(pm);
		new SearchEngine().searchDeclarationsOfReferencedTypes(ResourcesPlugin.getWorkspace(), element, collector);
		return extractElements(collector);
	}
	
	//----- referenced fields ----
	
	public static IField[] getFieldsReferencedIn(IMember[] elements, IProgressMonitor pm) throws JavaModelException {
		List referencedFields= new ArrayList();
		pm.beginTask("", elements.length);
		for (int i = 0; i < elements.length; i++) {
			referencedFields.addAll(getFieldsReferencedIn(elements[i], new SubProgressMonitor(pm, 1)));
		}
		pm.done();
		return (IField[]) referencedFields.toArray(new IField[referencedFields.size()]);
	}
	
	private static List getFieldsReferencedIn(IMember element, IProgressMonitor pm) throws JavaModelException {
		SearchResultCollector collector= new SearchResultCollector(pm);
		new SearchEngine().searchDeclarationsOfAccessedFields(ResourcesPlugin.getWorkspace(), element, collector);
		return extractElements(collector);
	}	
	
	//----- referenced methods ----
	
	public static IMethod[] getMethodsReferencedIn(IMember[] elements, IProgressMonitor pm) throws JavaModelException {
		List referencedFields= new ArrayList();
		pm.beginTask("", elements.length);
		for (int i = 0; i < elements.length; i++) {
			referencedFields.addAll(getMethodsReferencedIn(elements[i], new SubProgressMonitor(pm, 1)));
		}
		pm.done();
		return (IMethod[]) referencedFields.toArray(new IMethod[referencedFields.size()]);
	}
	
	private static List getMethodsReferencedIn(IMember element, IProgressMonitor pm) throws JavaModelException {
		SearchResultCollector collector= new SearchResultCollector(pm);
		new SearchEngine().searchDeclarationsOfSentMessages(ResourcesPlugin.getWorkspace(), element, collector);
		return extractElements(collector);
	}	
	
	/// private helpers 
	private static List extractElements(SearchResultCollector collector){
		List elements= new ArrayList(collector.getResults().size());
		for (Iterator iter = collector.getResults().iterator(); iter.hasNext();) {
			elements.add(((SearchResult) iter.next()).getEnclosingElement());
		}
		return elements;		
	}
}
