package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Binding2JavaModel;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.rename.TempOccurrenceFinder;
import org.eclipse.jdt.internal.corext.refactoring.rename.UpdateTypeReferenceEdit;
import org.eclipse.jdt.internal.corext.refactoring.util.JdtFlags;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;
import org.eclipse.jdt.internal.corext.template.Template;
import org.eclipse.jdt.internal.corext.template.Templates;
import org.eclipse.jdt.internal.corext.template.java.JavaContext;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;

public class ExtractInterfaceRefactoring extends Refactoring {

	private final CodeGenerationSettings fCodeGenerationSettings;

	private IType fInputClass;
	private String fNewInterfaceName;
	private IMember[] fExtractedMembers;
	private boolean fReplaceOccurrences;
	private TextChangeManager fChangeManager;
	private Set fBadVarSet;
	
	public ExtractInterfaceRefactoring(IType clazz, CodeGenerationSettings codeGenerationSettings){
		Assert.isNotNull(clazz);
		Assert.isNotNull(codeGenerationSettings);
		fInputClass= clazz;
		fCodeGenerationSettings= codeGenerationSettings;
		fExtractedMembers= new IMember[0];
		fBadVarSet= new HashSet(0);
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#getName()
	 */
	public String getName() {
		return "Extract Interface";
	}
	
	public IType getInputClass() {
		return fInputClass;
	}

	public String getNewInterfaceName() {
		return fNewInterfaceName;
	}

	public boolean isReplaceOccurrences() {
		return fReplaceOccurrences;
	}

	public void setNewInterfaceName(String newInterfaceName) {
		Assert.isNotNull(newInterfaceName);
		fNewInterfaceName= newInterfaceName;
	}

	public void setReplaceOccurrences(boolean replaceOccurrences) {
		fReplaceOccurrences= replaceOccurrences;
	}
	
	public void setExtractedMembers(IMember[] extractedMembers) throws JavaModelException{
		Assert.isTrue(areAllExtractableMembersOfClass(extractedMembers));
		fExtractedMembers= extractedMembers;
	}
	
	public IMember[] getExtractableMembers() throws JavaModelException{
		List members= new ArrayList();
		IJavaElement[] children= fInputClass.getChildren();
		for (int i= 0; i < children.length; i++) {
			if (children[i] instanceof IMember && isExtractableMember((IMember)children[i]))
				members.add(children[i]);
		}
		return (IMember[]) members.toArray(new IMember[members.size()]);
	}
	
	public RefactoringStatus checkPreactivation() throws JavaModelException {
		RefactoringStatus result= Checks.checkAvailability(fInputClass);	
		if (result.hasFatalError())
			return result;
		if (fInputClass.isInterface())
			result.addFatalError("Cannot perform Extract Interface on interfaces.");
		if (result.hasFatalError())
			return result;

		if (fInputClass.isLocal())
			result.addFatalError("Cannot perform Extract Interface on local types.");
		if (result.hasFatalError())
			return result;

		if (fInputClass.isAnonymous())
			result.addFatalError("Cannot perform Extract Interface on anonymous types.");
		if (result.hasFatalError())
			return result;

		//XXX for now
		if (! Checks.isTopLevel(fInputClass))
			result.addFatalError("Cannot perform Extract Interface on member classes.");
		if (result.hasFatalError())
			return result;
			
		return result;	
	}
	
	/* non java-doc
	 * @see Refactoring#checkPreconditions(IProgressMonitor)
	 */
	public RefactoringStatus checkPreconditions(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= checkPreactivation();
		if (result.hasFatalError())
			return result;
		result.merge(super.checkPreconditions(pm));
		return result;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		IType orig= (IType)WorkingCopyUtil.getOriginal(fInputClass);
		if (orig == null || ! orig.exists()){
			String key= "The selected type has been deleted from ''{0}''";
			String message= MessageFormat.format(key, new String[]{fInputClass.getCompilationUnit().getElementName()});
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
		fInputClass= orig;
		
		return Checks.checkIfCuBroken(fInputClass);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);//$NON-NLS-1$
		try{
			RefactoringStatus result= new RefactoringStatus();		
			
			if (getInputClassPackage().getCompilationUnit(getCuNameForNewInterface()).exists()){
				String pattern= "Compilation Unit named ''{0}'' already exists in package ''{1}''";
				String message= MessageFormat.format(pattern, new String[]{getCuNameForNewInterface(), getInputClassPackage().getElementName()});
				result.addFatalError(message);
			}	
				
			result.merge(checkNewInterfaceName(fNewInterfaceName));
			
			result.merge(checkInterfaceTypeName());
			
			fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1));
			result.merge(validateModifiesFiles());
			return result;
		} catch (CoreException e){
			throw new JavaModelException(e);
		} finally {
			pm.done();
		}	
	}

	private RefactoringStatus checkInterfaceTypeName() throws JavaModelException {
		ICompilationUnit[] cus= getInputClassPackage().getCompilationUnits();
		for (int i= 0; i < cus.length; i++) {
			if (cus[i].getType(fNewInterfaceName).exists()){
				String pattern= "Type named ''{0}'' already exists in package ''{1}''";
				String message= MessageFormat.format(pattern, new String[]{fNewInterfaceName, getInputClassPackage().getElementName()});
				return RefactoringStatus.createFatalErrorStatus(message);
			}	
		}
		return null;
	}
	
	public RefactoringStatus checkNewInterfaceName(String newName){
		RefactoringStatus result= Checks.checkTypeName(newName);
		if (result.hasFatalError())
			return result;
		result.merge(Checks.checkCompilationUnitName(newName + ".java"));
		return result;
	}

	private IFile[] getAllFilesToModify() throws CoreException{
		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
	}
	
	private RefactoringStatus validateModifiesFiles() throws CoreException{
		return Checks.validateModifiesFiles(getAllFilesToModify());
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("Creating change", 1);
			CompositeChange builder= new CompositeChange("Extract Interface");
			builder.addAll(fChangeManager.getAllChanges());
			builder.add(createExtractedInterface());
			return builder;	
		} catch(CoreException e){
			throw new JavaModelException(e);
		}	
	}
	
	private TextChangeManager createChangeManager(IProgressMonitor pm) throws CoreException{
		try{
			pm.beginTask("", 2); //$NON-NLS-1$
			TextChangeManager manager= new TextChangeManager();
			updateTypeDeclaration(manager, new SubProgressMonitor(pm, 1));
			if (fReplaceOccurrences)
				updateReferences(manager, new SubProgressMonitor(pm, 1));
			else
				pm.worked(1);	
			return manager;
		} finally{
			pm.done();
		}	
	}

	private void updateReferences(TextChangeManager manager, IProgressMonitor pm) throws JavaModelException, CoreException {
		pm.beginTask("", 2);//$NON-NLS-1$
		ISearchPattern pattern= SearchEngine.createSearchPattern(fInputClass, IJavaSearchConstants.REFERENCES);
		IJavaSearchScope scope= RefactoringScopeFactory.create(fInputClass);
		SearchResultGroup[] referenceGroups= RefactoringSearchEngine.search(new SubProgressMonitor(pm, 1), scope, pattern);
		addReferenceUpdatesAndImports(manager, new SubProgressMonitor(pm, 1), referenceGroups);
		pm.done();
	}
	
	private void addReferenceUpdatesAndImports(TextChangeManager manager, IProgressMonitor pm, SearchResultGroup[] resultGroups) throws CoreException {
		pm.beginTask("", resultGroups.length);
//		for (int i= 0; i < resultGroups.length; i++){
//			IJavaElement element= JavaCore.create(resultGroups[i].getResource());
//			if (!(element instanceof ICompilationUnit))
//				continue;
//			SearchResult[] results= resultGroups[i].getSearchResults();
//			ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists((ICompilationUnit)element);
//			boolean referencesUpdated= addReferenceUpdatesForCU(manager, new SubProgressMonitor(pm, 1), results, cu);
//			if (referencesUpdated && ! getInputClassPackage().equals(cu.getParent()))
//				addInterfaceImport(manager, cu);
//		}

		//----
//		Map mapping= getUpdateMapping(resultGroups);
//		for (Iterator iter= mapping.keySet().iterator(); iter.hasNext();) {
//			ICompilationUnit cu= (ICompilationUnit) iter.next();
//			ISourceRange[] updateRanges= (ISourceRange[])mapping.get(cu);
//			for (int i= 0; i < updateRanges.length; i++) {
//				ISourceRange range= updateRanges[i];
//				manager.get(cu).addTextEdit("update", createTypeUpdateEdit(range));	
//			}
//			if (updateRanges.length != 0 && ! getInputClassPackage().equals(cu.getParent()))
//				addInterfaceImport(manager, cu);			
//		}
		Map mapping= getNodeMapping(resultGroups); //ASTNode -> ICompilationUnit
		Set updatedCus= new HashSet(0);
		for (Iterator iter= mapping.keySet().iterator(); iter.hasNext();) {
			ASTNode node= (ASTNode) iter.next();
			ICompilationUnit cu= (ICompilationUnit)mapping.get(node);
			manager.get(cu).addTextEdit("update", createTypeUpdateEdit(new SourceRange(node)));	
			updatedCus.add(cu);
		}
		for (Iterator iter= updatedCus.iterator(); iter.hasNext();) {
			ICompilationUnit cu= (ICompilationUnit) iter.next();
			if (! getInputClassPackage().equals(cu.getParent()))
				addInterfaceImport(manager, cu);			
		}
		//----
		pm.done();
	}

	//ASTNode -> ICompilationUnit
	private Map getNodeMapping(SearchResultGroup[] resultGroups) throws JavaModelException {
		Map mapping= new HashMap();
		for (int i= 0; i < resultGroups.length; i++) {
			SearchResultGroup searchResultGroup= resultGroups[i];
			IJavaElement element= JavaCore.create(searchResultGroup.getResource());
			if (!(element instanceof ICompilationUnit))
				continue;
			ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists((ICompilationUnit)element);
			CompilationUnit cuNode= AST.parseCompilationUnit(cu, true);
			ASTNode[] nodes= getAstNodes(searchResultGroup.getSearchResults(), cuNode);
			for (int j= 0; j < nodes.length; j++) {
				mapping.put(nodes[j], cu);
			}			
		}
		retainUpdatableNodes(mapping);
		return mapping;
	}

	//ASTNode -> ICompilationUnit
	private void retainUpdatableNodes(Map mapping) throws JavaModelException {
		Collection nodesToRemove= computeNodesToRemove(mapping);
		for (Iterator iter= nodesToRemove.iterator(); iter.hasNext();) {
			mapping.remove(iter.next());
		}
	}
	
	//ASTNode -> ICompilationUnit	
	private Collection computeNodesToRemove(Map mapping) throws JavaModelException{
		Collection nodesToRemove= new HashSet(0);
		for (Iterator iter= mapping.keySet().iterator(); iter.hasNext();) {
			ASTNode node= (ASTNode) iter.next();
			if (canNeverUpdate(node))
				nodesToRemove.add(node);	
		}
		boolean reiterate;
		do {
			reiterate= false;
			for (Iterator iter= mapping.keySet().iterator(); iter.hasNext();) {
				ASTNode node= (ASTNode) iter.next();
				if (! nodesToRemove.contains(node) && hasIndirectProblems(node, nodesToRemove)){
					reiterate= true;
					nodesToRemove.add(node);			
				}
			}
		} while (reiterate);	
		return nodesToRemove;
	}

	//XXX
	private boolean hasIndirectProblems(ASTNode node, Collection nodesToRemove) {
		if (! (node.getParent() instanceof VariableDeclarationStatement))
			return false;

		VariableDeclarationStatement vds= (VariableDeclarationStatement)node.getParent();
		if (vds.getType() != node)
			return false; 
		VariableDeclarationFragment[] vdfs= getVariableDeclarationFragments(vds);	
		for (int i= 0; i < vdfs.length; i++) {
			VariableDeclarationFragment tempDeclaration= vdfs[i];
			ASTNode[] references= TempOccurrenceFinder.findTempOccurrenceNodes(tempDeclaration, true, false);
			for (int j= 0; j < references.length; j++) {
				ASTNode varReference= references[j];
				ASTNode parent= varReference.getParent();
				if (parent instanceof VariableDeclarationFragment){
					VariableDeclarationFragment r1= (VariableDeclarationFragment)parent;
					if (varReference == r1.getInitializer()){
						IVariableBinding vb= r1.resolveBinding();
						if (vb != null && fBadVarSet.contains(getCompilationUnitNode(varReference).findDeclaringNode(vb))){
							fBadVarSet.add(tempDeclaration);
							return true;
						}	
					}
				} else if (parent instanceof Assignment){
					Assignment assmnt= (Assignment)parent;
					if (varReference == assmnt.getRightHandSide()){
						Expression lhs= assmnt.getLeftHandSide();
						if (lhs instanceof SimpleName){
							IBinding binding= ((SimpleName)lhs).resolveBinding();
							if (binding != null && fBadVarSet.contains(getCompilationUnitNode(lhs).findDeclaringNode(binding))){
								fBadVarSet.add(tempDeclaration);
								return true;
							}	
						}
					}
				}
			}
		}
		return false;
	}
	
	private static CompilationUnit getCompilationUnitNode(ASTNode node) {
		return (CompilationUnit)ASTNodes.getParent(node, CompilationUnit.class);
	}

	private boolean canNeverUpdate(ASTNode node) throws JavaModelException {
		ASTNode parentNode= node.getParent();
		if (parentNode instanceof ClassInstanceCreation){
			if (node == ((ClassInstanceCreation)parentNode).getName())
				return true;
		}	
		if (parentNode instanceof TypeLiteral)
			return true;
		if (parentNode instanceof MethodDeclaration){
			if (((MethodDeclaration)parentNode).thrownExceptions().contains(node))
				return true;
		}	
		if (parentNode instanceof SingleVariableDeclaration && parentNode.getParent() instanceof CatchClause)
			return true;
		if (parentNode instanceof TypeDeclaration){
			if(node == ((TypeDeclaration)parentNode).getSuperclass())
			 	return true;
		}
		if (parentNode instanceof VariableDeclarationStatement){
			VariableDeclarationStatement vds= (VariableDeclarationStatement)parentNode;
			if (vds.getType() == node && ! canReplaceTypeInVariableDeclarationStatement(vds))
				return true; 
		}	
		 
		return false;	
	}
	private static ASTNode[] getAstNodes(SearchResult[] searchResults, CompilationUnit cuNode) {
		List result= new ArrayList(searchResults.length);
		for (int i= 0; i < searchResults.length; i++) {
			ASTNode node= getAstNode(searchResults[i], cuNode);
			if (node != null)
				result.add(node);
		}
		return (ASTNode[]) result.toArray(new ASTNode[result.size()]);
	}

	private static ASTNode getAstNode(SearchResult searchResult, CompilationUnit cuNode) {
		SelectionAnalyzer analyzer= new SelectionAnalyzer(Selection.createFromStartEnd(searchResult.getStart(), searchResult.getEnd()), true);
		cuNode.accept(analyzer);
		ASTNode selectedNode= analyzer.getFirstSelectedNode();
		if (selectedNode == null)
			return null;
		if (selectedNode.getParent() == null)
			return null;
		return selectedNode;
	}

	private void addInterfaceImport(TextChangeManager manager, ICompilationUnit cu) throws CoreException {
		ImportEdit importEdit= new ImportEdit(cu, fCodeGenerationSettings);
		importEdit.addImport(getFullyQualifiedInterfaceName());
		String pattern= "Adding import to ''{0}''";
		String editName= MessageFormat.format(pattern, new String[]{getFullyQualifiedInterfaceName()});
		manager.get(cu).addTextEdit(editName, importEdit);
	}

	private String getFullyQualifiedInterfaceName() {
		return getInputClassPackage().getElementName() + "." + fNewInterfaceName;
	}

	private TextEdit createTypeUpdateEdit(SearchResult searchResult) {
		int offset= searchResult.getStart();
		int length= searchResult.getEnd() - searchResult.getStart();
		return new UpdateTypeReferenceEdit(offset, length, fNewInterfaceName, fInputClass.getElementName());
	}

	private TextEdit createTypeUpdateEdit(ISourceRange sourceRange) {
		return new UpdateTypeReferenceEdit(sourceRange.getOffset(), sourceRange.getLength(), fNewInterfaceName, fInputClass.getElementName());
	}
		
	private boolean areAllExtractableMembersOfClass(IMember[] extractedMembers) throws JavaModelException {
		for (int i= 0; i < extractedMembers.length; i++) {
			if (! extractedMembers[i].getParent().equals(fInputClass))
				return false;
			if (! isExtractableMember(extractedMembers[i]))
				return false;
		}
		return true;
	}

	private static boolean isExtractableMember(IMember iMember) throws JavaModelException {
		if (iMember.getElementType() == IJavaElement.METHOD)
			return isExtractableMethod((IMethod)iMember);
		if (iMember.getElementType() == IJavaElement.FIELD)	
			return isExtractableField((IField)iMember);
		return false;	
	}

	private static boolean isExtractableField(IField iField) throws JavaModelException {
		if (! Flags.isPublic(iField.getFlags()))
			return false;
		if (! Flags.isStatic(iField.getFlags()))
			return false;
		if (! Flags.isFinal(iField.getFlags()))
			return false;
		return true;		
	}

	private static boolean isExtractableMethod(IMethod iMethod) throws JavaModelException {
		if (! Flags.isPublic(iMethod.getFlags()))
			return false;
		if (Flags.isStatic(iMethod.getFlags()))
			return false;
		return true;		
	}
	
	//----- methods related to creation of the new interface -------
	private IChange createExtractedInterface() throws CoreException {
		String lineSeparator= getLineSeperator(); 
		IPath cuPath= ResourceUtil.getFile(fInputClass.getCompilationUnit()).getFullPath();
		IPath interfaceCuPath= cuPath
										.removeLastSegments(1)
										.append(getCuNameForNewInterface());
		//XXX need to destroy
		ICompilationUnit newCuWC= getInputClassPackage().getCompilationUnit(getCuNameForNewInterface());
		String source= createExtractedInterfaceCUSource(newCuWC);
		String formattedSource= ToolFactory.createCodeFormatter().format(source, 0, null, lineSeparator);
		return new CreateTextFileChange(interfaceCuPath, formattedSource, true);	
	}
	
	private String getCuNameForNewInterface() {
		return fNewInterfaceName + ".java";
	}
	
	private IPackageFragment getInputClassPackage() {
		return (IPackageFragment)fInputClass.getCompilationUnit().getParent();
	}

	private static String getLineSeperator() {
		return System.getProperty("line.separator", "\n");//$NON-NLS-1$ //$NON-NLS-2$
	}

	private boolean inputClassHasDirectSuperinterfaces() throws JavaModelException {
		return fInputClass.getSuperInterfaceNames().length > 0;
	}

	private void updateTypeDeclaration(TextChangeManager manager, IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1);//$NON-NLS-1$
		String editName= "Update Type Declaration";
		int offset= computeIndexOfSuperinterfaceNameInsertion();
		String text=  computeTextOfSuperinterfaceNameInsertion();
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(fInputClass.getCompilationUnit());
		manager.get(cu).addTextEdit(editName, SimpleTextEdit.createInsert(offset, text));
		pm.done();
	}

	private String computeTextOfSuperinterfaceNameInsertion() throws JavaModelException {
		if (! inputClassHasDirectSuperinterfaces())
			return " implements " + fNewInterfaceName;	//$NON-NLS-1$
		else
			return ", " + fNewInterfaceName; //$NON-NLS-1$
	}

	private int computeIndexOfSuperinterfaceNameInsertion() throws JavaModelException {
		TypeDeclaration typeNode= getTypeDeclarationNode();
		if (typeNode.superInterfaces().isEmpty()){
			if (typeNode.getSuperclass() == null)
				return ASTNodes.getExclusiveEnd(typeNode.getName());
			else 
				return ASTNodes.getExclusiveEnd(typeNode.getSuperclass());
		} else {
			Name lastInterfaceName= (Name)typeNode.superInterfaces().get(typeNode.superInterfaces().size() - 1);
			return ASTNodes.getExclusiveEnd(lastInterfaceName);
		}
	}

	private TypeDeclaration getTypeDeclarationNode() throws JavaModelException {
		SelectionAnalyzer analyzer= new SelectionAnalyzer(Selection.createFromStartLength(fInputClass.getNameRange().getOffset(), fInputClass.getNameRange().getLength() +1), true);
		CompilationUnit cuNode= AST.parseCompilationUnit(fInputClass.getCompilationUnit(), false);
		cuNode.accept(analyzer);
		if (analyzer.getFirstSelectedNode() != null){
			if (analyzer.getFirstSelectedNode().getParent() instanceof TypeDeclaration)
				return (TypeDeclaration)analyzer.getFirstSelectedNode().getParent();
		}
		//XXX workaround for 21757
		if (analyzer.getLastCoveringNode() instanceof TypeDeclaration)
			return (TypeDeclaration) analyzer.getLastCoveringNode();
		return null;	
	}

	private String createExtractedInterfaceCUSource(ICompilationUnit newCu) throws CoreException {
		StringBuffer buffer= new StringBuffer();
		if (fCodeGenerationSettings.createFileComments)
			buffer.append(createFileCommentsSource(newCu));
		buffer.append(createPackageDeclarationSource());
		buffer.append(createImportsSource());
		if (fCodeGenerationSettings.createComments){
			buffer.append(getLineSeperator());
			buffer.append(createTypeCommentSource(newCu));
		}	
		buffer.append(createInterfaceSource());
		return buffer.toString();
	}

	private String createInterfaceSource() throws JavaModelException {
		StringBuffer buff= new StringBuffer();
		buff.append(createInterfaceModifierString())
			 .append("interface ")
			 .append(fNewInterfaceName)
			 .append(" {")
			 .append(createInterfaceMemberDeclarationsSource())
			 .append("}");
		return buff.toString();
	}

	private String createInterfaceModifierString() throws JavaModelException {
		if (JdtFlags.isPublic(fInputClass))
			return "public ";
		else
			return "";	
	}

	private String createInterfaceMemberDeclarationsSource() throws JavaModelException {
		StringBuffer buff= new StringBuffer();
		sortByOffset(fExtractedMembers);
		for (int i= 0; i < fExtractedMembers.length; i++) {
			buff.append(createInterfaceMemberDeclarationsSource(fExtractedMembers[i]));
		}
		return buff.toString();
	}
	
	private static void sortByOffset(IMember[] members) {
		Comparator comparator= new Comparator(){
			public int compare(Object o1, Object o2) {
				ISourceReference sr1= (ISourceReference)o1;
				ISourceReference sr2= (ISourceReference)o2;
				try {
					return sr1.getSourceRange().getOffset() - sr2.getSourceRange().getOffset();
				} catch (JavaModelException e) {
					return 0;
				}
			}
		};
		Arrays.sort(members, comparator);
	}

	private String createInterfaceMemberDeclarationsSource(IMember iMember) throws JavaModelException {
		Assert.isTrue(iMember.getElementType() == IJavaElement.FIELD || iMember.getElementType() == IJavaElement.METHOD);
		if (iMember.getElementType() == IJavaElement.FIELD)
			return iMember.getSource();
		else 
			return createInterfaceMethodDeclarationsSource((IMethod)iMember);
	}

	private String createInterfaceMethodDeclarationsSource(IMethod iMethod) throws JavaModelException {
		MethodDeclaration methodDeclaration= getMethodDeclarationNode(iMethod, false);
		if (methodDeclaration == null)
			return ""; 
	
		int offset= methodDeclaration.getReturnType().getStartPosition();
		int length= getMethodDeclarationLength(iMethod, methodDeclaration);
		String methodDeclarationSource= iMethod.getCompilationUnit().getBuffer().getText(offset, length);
		if (methodDeclaration.getBody() == null)
			return methodDeclarationSource;
		else
			return methodDeclarationSource + ";";
	}
	
	private static int getMethodDeclarationLength(IMethod iMethod, MethodDeclaration methodDeclaration) throws JavaModelException{
		int preDeclarationSourceLength= methodDeclaration.getReturnType().getStartPosition() - iMethod.getSourceRange().getOffset();
		if (methodDeclaration.getBody() == null)
			return methodDeclaration.getLength() - preDeclarationSourceLength;
		else
			return iMethod.getSourceRange().getLength() - methodDeclaration.getBody().getLength() - preDeclarationSourceLength;
	}

	private MethodDeclaration getMethodDeclarationNode(IMethod iMethod, boolean resolveBindings) throws JavaModelException{
		SelectionAnalyzer analyzer= new SelectionAnalyzer(Selection.createFromStartLength(iMethod.getSourceRange().getOffset(), iMethod.getSourceRange().getLength()), true);
		AST.parseCompilationUnit(fInputClass.getCompilationUnit(), resolveBindings).accept(analyzer);
		if (! (analyzer.getFirstSelectedNode() instanceof MethodDeclaration))
			return null; //???
		return (MethodDeclaration) analyzer.getFirstSelectedNode();
	}
	
	private String createImportsSource() throws JavaModelException {
		StringBuffer buff= new StringBuffer();
		ITypeBinding[] typesUsed= getTypesUsedInExtractedMemberDeclarations();
		for (int i= 0; i < typesUsed.length; i++) {
			ITypeBinding binding= typesUsed[i];
			if (binding != null && ! binding.isPrimitive()){
				buff.append("import ");//$NON-NLS-1$
				buff.append(Bindings.getFullyQualifiedImportName(binding));
				buff.append(";");//$NON-NLS-1$
			}	
		}
		return buff.toString();
	}

	private ITypeBinding[] getTypesUsedInExtractedMemberDeclarations() throws JavaModelException{
		Set typesUsed= new HashSet();
		for (int i= 0; i < fExtractedMembers.length; i++) {
			if (fExtractedMembers[i].getElementType() == IJavaElement.METHOD)
				typesUsed.addAll(getTypesUsedInDeclaration((IMethod)fExtractedMembers[i]));
			else if (fExtractedMembers[i].getElementType() == IJavaElement.FIELD)	
				typesUsed.addAll(getTypesUsedInDeclaration((IField)fExtractedMembers[i]));	
			else
				Assert.isTrue(false);	
		}
		return (ITypeBinding[]) typesUsed.toArray(new ITypeBinding[typesUsed.size()]);
	}
	
	//set of ITypeBindings
	private Set getTypesUsedInDeclaration(IField iField) {
		//XXX
		return new HashSet(0);
	}

	//set of ITypeBindings
	private Set getTypesUsedInDeclaration(IMethod iMethod) throws JavaModelException {
		MethodDeclaration methodDeclaration= getMethodDeclarationNode(iMethod, true);
		if (methodDeclaration == null)
			return new HashSet(0);
		Set result= new HashSet();	
		result.add(methodDeclaration.getReturnType().resolveBinding());
			
		for (Iterator iter= methodDeclaration.parameters().iterator(); iter.hasNext();) {
			result.add(((SingleVariableDeclaration) iter.next()).getType().resolveBinding()); 
		}
		
		for (Iterator iter= methodDeclaration.thrownExceptions().iterator(); iter.hasNext();) {
			result.add(((Name) iter.next()).resolveTypeBinding());
		}
		return result;
	}

	private String createPackageDeclarationSource() {
		return "package " + getInputClassPackage().getElementName() + ";";//$NON-NLS-2$ //$NON-NLS-1$
	}

	private String createTypeCommentSource(ICompilationUnit newCu) throws CoreException {
		return getTemplate("typecomment", 0, newCu);//$NON-NLS-1$
	}

	private String createFileCommentsSource(ICompilationUnit newCu) throws CoreException {
		return getTemplate("filecomment", 0, newCu);//$NON-NLS-1$
	}

	private static String getTemplate(String name, int pos, ICompilationUnit newCu) throws CoreException {
		Template[] templates= Templates.getInstance().getTemplates(name);
		if (templates.length == 0)
			return ""; //$NON-NLS-1$	
		String template= JavaContext.evaluateTemplate(templates[0], newCu, pos);
		if (template == null)
			return ""; //$NON-NLS-1$
		return template;
	}

	private static VariableDeclarationFragment[] getVariableDeclarationFragments(VariableDeclarationStatement vds){
		return (VariableDeclarationFragment[]) vds.fragments().toArray(new VariableDeclarationFragment[vds.fragments().size()]);
	}
	
	//--- 'can replace*' related methods 
	private boolean canReplaceTypeInVariableDeclarationStatement(VariableDeclarationStatement vds) throws JavaModelException{
		VariableDeclarationFragment[] fragments= getVariableDeclarationFragments(vds);
		for (int i= 0; i < fragments.length; i++) {
			if (! areAllTempReferencesOK(fragments[i])){
				addToBadVarSet(fragments[i]);
				return false;
			}	
		}
		return true;
	}

	private void addToBadVarSet(VariableDeclaration variableDeclaration) {
		fBadVarSet.add(variableDeclaration);
	}

	private boolean areAllTempReferencesOK(VariableDeclaration tempDeclaration) throws JavaModelException{
		ASTNode[] tempReferences= TempOccurrenceFinder.findTempOccurrenceNodes(tempDeclaration, true, false);			
		for (int i= 0; i < tempReferences.length; i++) {
			ASTNode tempRef= tempReferences[i];
			if (tempRef.getParent() instanceof FieldAccess)
				return false;
			if (! (tempRef.getParent() instanceof MethodInvocation))
				continue;
			MethodInvocation mi= (MethodInvocation)tempRef.getParent();
			IBinding miBinding= mi.getName().resolveBinding();
			if (miBinding == null || miBinding.getKind() != IBinding.METHOD)
				return false;
			IMethodBinding methodBinding= (IMethodBinding)miBinding;
			IMethod method= Binding2JavaModel.find(methodBinding, fInputClass);
			if (method == null)
				return false;
			if (! Arrays.asList(fExtractedMembers).contains(method))
				return false;	
		}	
		return true;
	}
}
