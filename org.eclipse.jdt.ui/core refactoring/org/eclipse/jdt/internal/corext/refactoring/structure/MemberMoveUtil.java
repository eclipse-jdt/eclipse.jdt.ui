package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultCollector;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceRangeComputer;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

public class MemberMoveUtil {

	private final ImportEditManager fImportManager;
	private final Set fMovedMembers;
	
	private MemberMoveUtil(ImportEditManager importManager, IMember[] movedMembers) {
		Assert.isNotNull(importManager);
		fImportManager= importManager;
		fMovedMembers= new HashSet(Arrays.asList(movedMembers));
	}
	
	public static String computeNewSource(IMember member, IProgressMonitor pm, ImportEditManager manager, IMember[] allMovedMembers) throws JavaModelException {
		MemberMoveUtil inst= new MemberMoveUtil(manager, allMovedMembers);
		return inst.computeNewSource(member, pm);
	}
	
	private String computeNewSource(IMember member, IProgressMonitor pm) throws JavaModelException {
		String originalSource= SourceRangeComputer.computeSource(member);
		StringBuffer modifiedSource= new StringBuffer(originalSource);
		
		//ISourceRange -> String (new source)
		Map accessModifications= getStaticMemberAccessesInMovedMember(member, pm);
		ISourceRange[] ranges= (ISourceRange[]) accessModifications.keySet().toArray(new ISourceRange[accessModifications.keySet().size()]);
		ISourceRange[] sortedRanges= SourceRange.reverseSortByOffset(ranges);
		
		ISourceRange originalRange= SourceRangeComputer.computeSourceRange(member, member.getCompilationUnit().getSource());
		
		for (int i= 0; i < sortedRanges.length; i++) {
			int start= sortedRanges[i].getOffset() - originalRange.getOffset();
			int end= start + sortedRanges[i].getLength();
			modifiedSource.replace(start, end, (String)accessModifications.get(sortedRanges[i]));
		}
		return modifiedSource.toString();
	}
	
	//ISourceRange -> String (new source)
	private Map getStaticMemberAccessesInMovedMember(IMember member, IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 3); //$NON-NLS-1$
		Map resultMap= new HashMap();
		resultMap.putAll(getFieldAccessModificationsInMovedMember(member, new SubProgressMonitor(pm, 1)));
		resultMap.putAll(getMethodSendsInMovedMember(member, new SubProgressMonitor(pm, 1)));
		resultMap.putAll(getTypeReferencesInMovedMember(member, new SubProgressMonitor(pm, 1)));
		pm.done();
		return resultMap;
	}

	//ISourceRange -> String (new source)
	private Map getFieldAccessModificationsInMovedMember(IMember member, IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 2); //$NON-NLS-1$
		Map result= new HashMap();
		IField[] fields= ReferenceFinderUtil.getFieldsReferencedIn(new IMember[]{member}, new SubProgressMonitor(pm, 1));
		ICompilationUnit cu= getWorkingCopyForDeclaringTypeCu(member);
		IMember[] interestingFields= getMembersThatNeedReferenceConversion(fields);
		for (int i= 0; i < interestingFields.length; i++) {
			IField field= (IField)interestingFields[i];
			//XX side effect
			fImportManager.addImportTo(field.getDeclaringType(), cu);
			String newSource= field.getDeclaringType().getElementName() + "." + field.getElementName(); //$NON-NLS-1$
			SearchResult[] searchResults= findReferencesInMember(member, field, new SubProgressMonitor(pm, 1));
			ISourceRange[] ranges= FieldReferenceFinder.findFieldReferenceRanges(searchResults, cu);
			putAllToMap(result, newSource, ranges);		
		}
		return result;
	}

	//ISourceRange -> String (new source)
	private Map getMethodSendsInMovedMember(IMember member, IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 2); //$NON-NLS-1$
		Map result= new HashMap();
		IMethod[] methods= ReferenceFinderUtil.getMethodsReferencedIn(new IMember[]{member}, new SubProgressMonitor(pm, 1));
		ICompilationUnit cu= getWorkingCopyForDeclaringTypeCu(member);
		IMember[] interestingMethods= getMembersThatNeedReferenceConversion(methods);
		for (int i= 0; i < interestingMethods.length; i++) {
			IMethod method= (IMethod)interestingMethods[i];
			//XX side effect
			fImportManager.addImportTo(method.getDeclaringType(), cu);
			String newSource= method.getDeclaringType().getElementName() + "." + method.getElementName(); //$NON-NLS-1$
			SearchResult[] searchResults= findReferencesInMember(member, method, new SubProgressMonitor(pm, 1));
			ISourceRange[] ranges= MethodInvocationFinder.findMessageSendRanges(searchResults, cu);
			putAllToMap(result, newSource, ranges);
		}
		return result;
	}
	
	//ISourceRange -> String (new source)
	private Map getTypeReferencesInMovedMember(IMember member, IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 2); //$NON-NLS-1$
		Map result= new HashMap();
		IType[] types= ReferenceFinderUtil.getTypesReferencedIn(new IMember[]{member}, new SubProgressMonitor(pm, 1));
		ICompilationUnit cu= getWorkingCopyForDeclaringTypeCu(member);
		IMember[] interestingTypes= getMembersThatNeedReferenceConversion(types);
		for (int i= 0; i < interestingTypes.length; i++) {
			IType type= (IType)interestingTypes[i];
			//XX side effect
			fImportManager.addImportTo(type.getDeclaringType(), cu);
			String newSource= type.getDeclaringType().getElementName() + "." + type.getElementName(); //$NON-NLS-1$
			SearchResult[] searchResults= findReferencesInMember(member, type, new SubProgressMonitor(pm, 1));
			ISourceRange[] ranges= TypeReferenceFinder.findTypeReferenceRanges(searchResults, cu);
			putAllToMap(result, newSource, ranges);
		}
		return result;
	}

	private static ICompilationUnit getWorkingCopyForDeclaringTypeCu(IMember member){
		return WorkingCopyUtil.getWorkingCopyIfExists(member.getDeclaringType().getCompilationUnit());		
	}
	
	private IMember[] getMembersThatNeedReferenceConversion(IMember[] members) throws JavaModelException{
		Set memberSet= new HashSet(); //using set to remove dups
		for (int i= 0; i < members.length; i++) {
			if (willNeedToConvertReferenceTo(members[i]))
				memberSet.add(members[i]);
		}
		return (IMember[]) memberSet.toArray(new IMember[memberSet.size()]);
	}
	
	private boolean willNeedToConvertReferenceTo(IMember member) throws JavaModelException{
		if (! member.exists())
			return false;
		if (fMovedMembers.contains(member))
			return false;
		if (! JdtFlags.isStatic(member)) //convert all static references
			return false;
		return true;		
	}

	private static SearchResult[] findReferencesInMember(IMember scopeMember, IMember referenceMember, IProgressMonitor pm) throws JavaModelException {
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaElement[]{scopeMember});
		SearchResultCollector collector= new SearchResultCollector(pm);
		new SearchEngine().search(ResourcesPlugin.getWorkspace(), referenceMember, IJavaSearchConstants.REFERENCES, scope, collector);
		List results= collector.getResults();
		SearchResult[] searchResults= (SearchResult[]) results.toArray(new SearchResult[results.size()]);
		return searchResults;
	}
			
	private static void putAllToMap(Map result, String newSource, ISourceRange[] ranges) {
		for (int i= 0; i < ranges.length; i++) {
			result.put(ranges[i], newSource);
		}
	}
}
