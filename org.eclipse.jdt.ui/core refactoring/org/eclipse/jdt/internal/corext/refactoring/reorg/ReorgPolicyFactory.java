/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.OldASTRewrite;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CopyCompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CopyPackageChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CopyPackageFragmentRootChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CopyResourceChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.MoveCompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.MovePackageChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.MovePackageFragmentRootChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.MoveResourceChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.participants.ResourceModifications;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy.ICopyPolicy;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy.IMovePolicy;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.Changes;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.QualifiedNameFinder;
import org.eclipse.jdt.internal.corext.refactoring.util.QualifiedNameSearchResult;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.ltk.core.refactoring.participants.ValidateEditChecker;

class ReorgPolicyFactory {
	private ReorgPolicyFactory() {
		//private
	}
	
	public static ICopyPolicy createCopyPolicy(IResource[] resources, IJavaElement[] javaElements, CodeGenerationSettings settings) throws JavaModelException{
		return (ICopyPolicy)createReorgPolicy(true, resources, javaElements, settings);
	}
	
	public static IMovePolicy createMovePolicy(IResource[] resources, IJavaElement[] javaElements, CodeGenerationSettings settings) throws JavaModelException{
		return (IMovePolicy)createReorgPolicy(false, resources, javaElements, settings);
	}
	
	private static IReorgPolicy createReorgPolicy(boolean copy, IResource[] selectedResources, IJavaElement[] selectedJavaElements, CodeGenerationSettings settings) throws JavaModelException{
		final IReorgPolicy NO;
		if (copy)
			NO= new NoCopyPolicy();
		else
			NO= new NoMovePolicy();

		ActualSelectionComputer selectionComputer= new ActualSelectionComputer(selectedJavaElements, selectedResources);
		IResource[] resources= selectionComputer.getActualResourcesToReorg();
		IJavaElement[] javaElements= selectionComputer.getActualJavaElementsToReorg();
	
		if (isNothingToReorg(resources, javaElements) || 
			containsNull(resources) ||
			containsNull(javaElements) ||
			ReorgUtils.hasElementsOfType(javaElements, IJavaElement.JAVA_PROJECT) ||
			ReorgUtils.hasElementsOfType(javaElements, IJavaElement.JAVA_MODEL) ||
			ReorgUtils.hasElementsOfType(resources, IResource.PROJECT | IResource.ROOT) ||
			! haveCommonParent(resources, javaElements))
			return NO;
			
		if (ReorgUtils.hasElementsOfType(javaElements, IJavaElement.PACKAGE_FRAGMENT)){
			if (resources.length != 0 || ReorgUtils.hasElementsNotOfType(javaElements, IJavaElement.PACKAGE_FRAGMENT))
				return NO;
			if (copy)
				return new CopyPackagesPolicy(ArrayTypeConverter.toPackageArray(javaElements));
			else
				return new MovePackagesPolicy(ArrayTypeConverter.toPackageArray(javaElements));
		}
		
		if (ReorgUtils.hasElementsOfType(javaElements, IJavaElement.PACKAGE_FRAGMENT_ROOT)){
			if (resources.length != 0 || ReorgUtils.hasElementsNotOfType(javaElements, IJavaElement.PACKAGE_FRAGMENT_ROOT))
				return NO;
			if (copy)
				return new CopyPackageFragmentRootsPolicy(ArrayTypeConverter.toPackageFragmentRootArray(javaElements));
			else
				return new MovePackageFragmentRootsPolicy(ArrayTypeConverter.toPackageFragmentRootArray(javaElements));
		}
		
		if (ReorgUtils.hasElementsOfType(resources, IResource.FILE | IResource.FOLDER) || ReorgUtils.hasElementsOfType(javaElements, IJavaElement.COMPILATION_UNIT)){
			if (ReorgUtils.hasElementsNotOfType(javaElements, IJavaElement.COMPILATION_UNIT))
				return NO;
			if (ReorgUtils.hasElementsNotOfType(resources, IResource.FILE | IResource.FOLDER))
				return NO;
			if (copy)
				return new CopyFilesFoldersAndCusPolicy(ReorgUtils.getFiles(resources), ReorgUtils.getFolders(resources), ArrayTypeConverter.toCuArray(javaElements));
			else
				return new MoveFilesFoldersAndCusPolicy(ReorgUtils.getFiles(resources), ReorgUtils.getFolders(resources), ArrayTypeConverter.toCuArray(javaElements), settings);
		}
		
		if (hasElementsSmallerThanCuOrClassFile(javaElements)){
			//assertions guaranteed by common parent
			Assert.isTrue(resources.length == 0);
			Assert.isTrue(! ReorgUtils.hasElementsOfType(javaElements, IJavaElement.COMPILATION_UNIT));
			Assert.isTrue(! ReorgUtils.hasElementsOfType(javaElements, IJavaElement.CLASS_FILE));
			Assert.isTrue(! hasElementsLargerThanCuOrClassFile(javaElements));
			if (copy)
				return new CopySubCuElementsPolicy(javaElements);
			else
				return new MoveSubCuElementsPolicy(javaElements);
		}
		return NO;
	}
		
	private static boolean containsNull(Object[] objects) {
		for (int i= 0; i < objects.length; i++) {
			if (objects[i]==null) return  true;
		}
		return false;
	}

	private static boolean hasElementsSmallerThanCuOrClassFile(IJavaElement[] javaElements) {
		for (int i= 0; i < javaElements.length; i++) {
			if (ReorgUtils.isInsideCompilationUnit(javaElements[i]))
				return true;
			if (ReorgUtils.isInsideClassFile(javaElements[i]))
				return true;
		}
		return false;
	}

	private static boolean hasElementsLargerThanCuOrClassFile(IJavaElement[] javaElements) {
		for (int i= 0; i < javaElements.length; i++) {
			if (! ReorgUtils.isInsideCompilationUnit(javaElements[i]) &&
				! ReorgUtils.isInsideClassFile(javaElements[i]))
				return true;
		}
		return false;
	}

	private static boolean haveCommonParent(IResource[] resources, IJavaElement[] javaElements) {
		return new ParentChecker(resources, javaElements).haveCommonParent();
	}

	private static boolean isNothingToReorg(IResource[] resources, IJavaElement[] javaElements) {
		return resources.length + javaElements.length == 0;
	}

	private static abstract class ReorgPolicy implements IReorgPolicy {
		//invariant: only 1 of these can ever be not null
		private IResource fResourceDestination;
		private IJavaElement fJavaElementDestination;
			
		public final RefactoringStatus setDestination(IResource destination) throws JavaModelException {
			Assert.isNotNull(destination);
			resetDestinations();
			fResourceDestination= destination;
			return verifyDestination(destination);
		}
		public final RefactoringStatus setDestination(IJavaElement destination) throws JavaModelException {
			Assert.isNotNull(destination);
			resetDestinations();
			fJavaElementDestination= destination;
			return verifyDestination(destination);
		}
		protected abstract RefactoringStatus verifyDestination(IJavaElement destination) throws JavaModelException;
		protected abstract RefactoringStatus verifyDestination(IResource destination) throws JavaModelException;

		private void resetDestinations() {
			fJavaElementDestination= null;
			fResourceDestination= null;
		}
		public final IResource getResourceDestination(){
			return fResourceDestination;
		}
		public final IJavaElement getJavaElementDestination(){
			return fJavaElementDestination;
		}
		public IFile[] getAllModifiedFiles() {
			return new IFile[0];
		}
		public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, IReorgQueries reorgQueries) throws JavaModelException{
			Assert.isNotNull(reorgQueries);
			ValidateEditChecker checker= (ValidateEditChecker)context.getChecker(ValidateEditChecker.class);
			if (checker != null) {
				checker.addFiles(getAllModifiedFiles());
				return new RefactoringStatus();
			} else {
				return Checks.validateModifiesFiles(getAllModifiedFiles());
			}
		}
		public boolean hasAllInputSet() {
			return fJavaElementDestination != null || fResourceDestination != null;
		}
		public boolean canUpdateReferences() {
			return false;
		}
		public boolean getUpdateReferences() {
			Assert.isTrue(false);//should not be called if canUpdateReferences is not overridden and returns false
			return false;
		}
		public void setUpdateReferences(boolean update) {
			Assert.isTrue(false);//should not be called if canUpdateReferences is not overridden and returns false
		}
		public boolean canEnableQualifiedNameUpdating() {
			return false;
		}
		public boolean canUpdateQualifiedNames() {
			Assert.isTrue(false);//should not be called if canEnableQualifiedNameUpdating is not overridden and returns false
			return false;
		}
		public String getFilePatterns() {
			Assert.isTrue(false);//should not be called if canEnableQualifiedNameUpdating is not overridden and returns false
			return null;
		}
		public boolean getUpdateQualifiedNames() {
			Assert.isTrue(false);//should not be called if canEnableQualifiedNameUpdating is not overridden and returns false
			return false;
		}
		public void setFilePatterns(String patterns) {
			Assert.isTrue(false);//should not be called if canEnableQualifiedNameUpdating is not overridden and returns false
		}
		public void setUpdateQualifiedNames(boolean update) {
			Assert.isTrue(false);//should not be called if canEnableQualifiedNameUpdating is not overridden and returns false
		}
		public boolean canEnable() throws JavaModelException {
			IResource[] resources= getResources();
			for (int i= 0; i < resources.length; i++) {
				IResource resource= resources[i];
				if (! resource.exists() || resource.isPhantom() || ! resource.isAccessible())
					return false;
			}
			
			IJavaElement[] javaElements= getJavaElements();
			for (int i= 0; i < javaElements.length; i++) {
				IJavaElement element= javaElements[i];
				if (!element.exists()) return false;
			}
			return true;
		}
	}

	private static abstract class FilesFoldersAndCusReorgPolicy extends ReorgPolicy{

		private ICompilationUnit[] fCus;
		private IFolder[] fFolders;
		private IFile[] fFiles;

		public FilesFoldersAndCusReorgPolicy(IFile[] files, IFolder[] folders, ICompilationUnit[] cus){
			fFiles= files;
			fFolders= folders;
			fCus= cus;
		}

		protected RefactoringStatus verifyDestination(IJavaElement javaElement) throws JavaModelException {
			Assert.isNotNull(javaElement);
			if (! javaElement.exists())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.doesnotexist0")); //$NON-NLS-1$
			if (javaElement instanceof IJavaModel)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.jmodel")); //$NON-NLS-1$
	
			if (javaElement.isReadOnly())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.readonly")); //$NON-NLS-1$
	
			if (! javaElement.isStructureKnown())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.structure")); //$NON-NLS-1$
	
			if (javaElement instanceof IOpenable){
				IOpenable openable= (IOpenable)javaElement;
				if (! openable.isConsistent())
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.inconsistent")); //$NON-NLS-1$
			}				
	
			if (javaElement instanceof IPackageFragmentRoot){
				IPackageFragmentRoot root= (IPackageFragmentRoot)javaElement;
				if (root.isArchive())
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.archive")); //$NON-NLS-1$
				if (root.isExternal())
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.external")); //$NON-NLS-1$
			}
			
			if (ReorgUtils.isInsideCompilationUnit(javaElement)) {
				ICompilationUnit cu= ReorgUtils.getCompilationUnit(javaElement);
				if (cu != null)
					return verifyDestination(cu);
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.notSubCu")); //$NON-NLS-1$
			}
	
			if (containsLinkedResources() && !ReorgUtils.canBeDestinationForLinkedResources(javaElement))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.linked")); //$NON-NLS-1$
			return new RefactoringStatus();
		}

		protected RefactoringStatus verifyDestination(IResource resource) throws JavaModelException {
			Assert.isNotNull(resource);
			if (! resource.exists() || resource.isPhantom())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.phantom"));			 //$NON-NLS-1$
			if (!resource.isAccessible())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.inaccessible")); //$NON-NLS-1$
			Assert.isTrue(resource.getType() != IResource.ROOT);
					
			if (isChildOfOrEqualToAnyFolder(resource))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.not_this_resource")); //$NON-NLS-1$
						
			if (containsLinkedResources() && !ReorgUtils.canBeDestinationForLinkedResources(resource))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.linked")); //$NON-NLS-1$
	
			return new RefactoringStatus();
		}

		private boolean isChildOfOrEqualToAnyFolder(IResource resource) {
			for (int i= 0; i < fFolders.length; i++) {
				IFolder folder= fFolders[i];
				if (folder.equals(resource) || ParentChecker.isDescendantOf(resource, folder))
					return true;
			}
			return false;
		}
	
		private static IContainer getAsContainer(IResource resDest){
			if (resDest instanceof IContainer)
				return (IContainer)resDest;
			if (resDest instanceof IFile)
				return ((IFile)resDest).getParent();
			return null;				
		}
				
		protected final IContainer getDestinationAsContainer(){
			IResource resDest= getResourceDestination();
			if (resDest != null)
				return getAsContainer(resDest);
			IJavaElement jelDest= getJavaElementDestination();
			Assert.isNotNull(jelDest);				
			return getAsContainer(ReorgUtils.getResource(jelDest));
		}
		
		protected final IJavaElement getDestinationContainerAsJavaElement() {
			if (getJavaElementDestination() != null)
				return getJavaElementDestination();
			IContainer destinationAsContainer= getDestinationAsContainer();
			if (destinationAsContainer == null)
				return null;
			IJavaElement je= JavaCore.create(destinationAsContainer);
			if (je != null && je.exists())
				return je;
			return null;
		}
		
		protected final IPackageFragment getDestinationAsPackageFragment() {
			IPackageFragment javaAsPackage= getJavaDestinationAsPackageFragment(getJavaElementDestination());
			if (javaAsPackage != null)
				return javaAsPackage;
			return getResourceDestinationAsPackageFragment(getResourceDestination());
		}
				
		private static IPackageFragment getJavaDestinationAsPackageFragment(IJavaElement javaDest){
			if( javaDest == null || ! javaDest.exists())
				return null;					
			if (javaDest instanceof IPackageFragment)
				return (IPackageFragment) javaDest;
			if (javaDest instanceof IPackageFragmentRoot)
				return ((IPackageFragmentRoot) javaDest).getPackageFragment(""); //$NON-NLS-1$
			if (javaDest instanceof IJavaProject) {
				try {
					IPackageFragmentRoot root= ReorgUtils.getCorrespondingPackageFragmentRoot((IJavaProject)javaDest);
					if (root != null)
						return root.getPackageFragment("");  //$NON-NLS-1$
				} catch (JavaModelException e) {
					// fall through
				}
			}
			return (IPackageFragment) javaDest.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
		}
				
		private static IPackageFragment getResourceDestinationAsPackageFragment(IResource resource){
			if (resource instanceof IFile)
				return getJavaDestinationAsPackageFragment(JavaCore.create(resource.getParent()));
			return null;	
		}

		public final IJavaElement[] getJavaElements(){
			return fCus;
		}

		public final IResource[] getResources() {
			return ReorgUtils.union(fFiles, fFolders);
		}

		protected boolean containsLinkedResources() {
			return 	ReorgUtils.containsLinkedResources(fFiles) || 
					ReorgUtils.containsLinkedResources(fFolders) || 
					ReorgUtils.containsLinkedResources(fCus);
		}

		protected final IFolder[] getFolders(){
			return fFolders;
		}
		protected final IFile[] getFiles(){
			return fFiles;
		}
		protected final ICompilationUnit[] getCus(){
			return fCus;
		}
		public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, IReorgQueries reorgQueries) throws JavaModelException {
			RefactoringStatus status= super.checkFinalConditions(pm, context, reorgQueries);
			confirmOverwritting(reorgQueries);
			return status;
		}

		private void confirmOverwritting(IReorgQueries reorgQueries) {
			OverwriteHelper oh= new OverwriteHelper();
			oh.setFiles(fFiles);
			oh.setFolders(fFolders);
			oh.setCus(fCus);
			IPackageFragment destPack= getDestinationAsPackageFragment();
			if (destPack != null) {
				oh.confirmOverwritting(reorgQueries, destPack);
			} else {
				IContainer destinationAsContainer= getDestinationAsContainer();
				if (destinationAsContainer != null)
					oh.confirmOverwritting(reorgQueries, destinationAsContainer);
			}	
			fFiles= oh.getFilesWithoutUnconfirmedOnes();
			fFolders= oh.getFoldersWithoutUnconfirmedOnes();
			fCus= oh.getCusWithoutUnconfirmedOnes();
		}
	}

	private static abstract class SubCuElementReorgPolicy extends ReorgPolicy{
		private final IJavaElement[] fJavaElements;
		SubCuElementReorgPolicy(IJavaElement[] javaElements){
			Assert.isNotNull(javaElements);
			fJavaElements= javaElements;
		}

		protected final RefactoringStatus verifyDestination(IResource destination) throws JavaModelException {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.no_resource")); //$NON-NLS-1$
		}

		protected final ICompilationUnit getSourceCu() {
			//all have a common parent, so all must be in the same cu
			//we checked before that the array in not null and not empty
			return (ICompilationUnit) fJavaElements[0].getAncestor(IJavaElement.COMPILATION_UNIT);
		}

		public final IJavaElement[] getJavaElements() {
			return fJavaElements;
		}
		public final IResource[] getResources() {
			return new IResource[0];
		}
	
		protected final ICompilationUnit getDestinationCu() {
			return getDestinationCu(getJavaElementDestination());
		}

		protected static final ICompilationUnit getDestinationCu(IJavaElement destination) {
			if (destination instanceof ICompilationUnit)
				return (ICompilationUnit) destination;
			return (ICompilationUnit) destination.getAncestor(IJavaElement.COMPILATION_UNIT);
		}
	
		protected static TextChange addTextEditFromRewrite(ICompilationUnit cu, OldASTRewrite rewrite) throws CoreException {
			TextBuffer textBuffer= TextBuffer.create(cu.getBuffer().getContents());
			TextEdit resultingEdits= new MultiTextEdit();
			rewrite.rewriteNode(textBuffer, resultingEdits);
				
			TextChange textChange= new CompilationUnitChange(cu.getElementName(), cu);
			if (textChange instanceof TextFileChange){
				TextFileChange tfc= (TextFileChange)textChange;
				if (cu.isWorkingCopy()) 
					tfc.setSaveMode(TextFileChange.LEAVE_DIRTY);
			}
			String message= RefactoringCoreMessages.getString("ReorgPolicyFactory.copy"); //$NON-NLS-1$
			TextChangeCompatibility.addTextEdit(textChange, message, resultingEdits);
			rewrite.removeModifications();
			return textChange;
		}
	
		protected void copyToDestination(IJavaElement element, OldASTRewrite rewrite, CompilationUnit sourceCuNode, CompilationUnit destinationCuNode) throws JavaModelException {
			switch(element.getElementType()){
				case IJavaElement.FIELD: 
					copyFieldToDestination((IField)element, rewrite, sourceCuNode, destinationCuNode);
					break;
				case IJavaElement.IMPORT_CONTAINER:
					copyImportsToDestination((IImportContainer)element, rewrite, sourceCuNode, destinationCuNode);
					break;
				case IJavaElement.IMPORT_DECLARATION:
					copyImportToDestination((IImportDeclaration)element, rewrite, sourceCuNode, destinationCuNode);
					break;
				case IJavaElement.INITIALIZER:
					copyInitializerToDestination((IInitializer)element, rewrite, destinationCuNode);
					break;
				case IJavaElement.METHOD: 
					copyMethodToDestination((IMethod)element, rewrite, destinationCuNode);
					break;
				case IJavaElement.PACKAGE_DECLARATION:
					copyPackageDeclarationToDestination((IPackageDeclaration)element, rewrite, sourceCuNode, destinationCuNode);
					break;
				case IJavaElement.TYPE :
					copyTypeToDestination((IType) element, rewrite, destinationCuNode);
					break;

				default: Assert.isTrue(false); 
			}
		}

		private void copyFieldToDestination(IField field, OldASTRewrite targetRewrite, CompilationUnit sourceCuNode, CompilationUnit destinationCuNode) throws JavaModelException {
			//cannot copy the whole field declaration - it can contain more than 1 field
			FieldDeclaration copiedNode= createNewFieldDeclarationNode(field, getAST(targetRewrite), sourceCuNode);
			targetRewrite.markAsInserted(copiedNode);
			TypeDeclaration targetClass= getTargetType(destinationCuNode);
			targetClass.bodyDeclarations().add(copiedNode);
		}

		private FieldDeclaration createNewFieldDeclarationNode(IField field, AST targetAst, CompilationUnit sourceCuNode) throws JavaModelException {
			FieldDeclaration fieldDeclaration= ASTNodeSearchUtil.getFieldDeclarationNode(field, sourceCuNode);
			if (fieldDeclaration.fragments().size() == 1)
				return (FieldDeclaration) ASTNode.copySubtree(targetAst, fieldDeclaration);
			VariableDeclarationFragment originalFragment= ASTNodeSearchUtil.getFieldDeclarationFragmentNode(field, sourceCuNode);
			VariableDeclarationFragment copiedFragment= (VariableDeclarationFragment) ASTNode.copySubtree(targetAst, originalFragment);
			FieldDeclaration newFieldDeclaration= targetAst.newFieldDeclaration(copiedFragment);
			if (fieldDeclaration.getJavadoc() != null)
				newFieldDeclaration.setJavadoc((Javadoc) ASTNode.copySubtree(targetAst, fieldDeclaration.getJavadoc()));
			newFieldDeclaration.setModifiers(fieldDeclaration.getModifiers());
			newFieldDeclaration.setType((Type) ASTNode.copySubtree(targetAst, fieldDeclaration.getType()));
			return newFieldDeclaration;
		}

		private void copyImportsToDestination(IImportContainer container, OldASTRewrite rewrite, CompilationUnit sourceCuNode, CompilationUnit destinationCuNode) throws JavaModelException {
			//there's no special AST node for the container - we copy all imports
			IJavaElement[] importDeclarations= container.getChildren();
			for (int i= 0; i < importDeclarations.length; i++) {
				Assert.isTrue(importDeclarations[i] instanceof IImportDeclaration);//promised in API
				IImportDeclaration importDeclaration= (IImportDeclaration)importDeclarations[i];
				copyImportToDestination(importDeclaration, rewrite, sourceCuNode, destinationCuNode);
			}
		}

		private void copyImportToDestination(IImportDeclaration declaration, OldASTRewrite targetRewrite, CompilationUnit sourceCuNode, CompilationUnit destinationCuNode) throws JavaModelException {
			ImportDeclaration sourceNode= ASTNodeSearchUtil.getImportDeclarationNode(declaration, sourceCuNode);
			ImportDeclaration copiedNode= (ImportDeclaration) ASTNode.copySubtree(getAST(targetRewrite), sourceNode);
			targetRewrite.markAsInserted(copiedNode);
			destinationCuNode.imports().add(copiedNode);
		}

		private void copyPackageDeclarationToDestination(IPackageDeclaration declaration, OldASTRewrite targetRewrite, CompilationUnit sourceCuNode, CompilationUnit destinationCuNode) throws JavaModelException {
			PackageDeclaration sourceNode= ASTNodeSearchUtil.getPackageDeclarationNode(declaration, sourceCuNode);
			Initializer copiedNode= (Initializer) ASTNode.copySubtree(getAST(targetRewrite), sourceNode);
			targetRewrite.markAsInserted(copiedNode);
			destinationCuNode.types().add(copiedNode);
		}

		private void copyInitializerToDestination(IInitializer initializer, OldASTRewrite targetRewrite, CompilationUnit destinationCuNode) throws JavaModelException {
			BodyDeclaration newInitializer= (BodyDeclaration) targetRewrite.createStringPlaceholder(getUnindentedSource(initializer), ASTNode.INITIALIZER);
			targetRewrite.markAsInserted(newInitializer);
			TypeDeclaration targetClass= getTargetType(destinationCuNode);
			targetClass.bodyDeclarations().add(newInitializer);
		}

		private void copyTypeToDestination(IType type, OldASTRewrite targetRewrite, CompilationUnit destinationCuNode) throws JavaModelException {
			TypeDeclaration newType= (TypeDeclaration) targetRewrite.createStringPlaceholder(getUnindentedSource(type), ASTNode.TYPE_DECLARATION);
			targetRewrite.markAsInserted(newType);
			//always put on top level - we could create member types but that is wrong most of the time
			destinationCuNode.types().add(newType);
		}

		private void copyMethodToDestination(IMethod method, OldASTRewrite targetRewrite, CompilationUnit destinationCuNode) throws JavaModelException {
			BodyDeclaration newMethod= (BodyDeclaration) targetRewrite.createStringPlaceholder(getUnindentedSource(method), ASTNode.METHOD_DECLARATION);
			targetRewrite.markAsInserted(newMethod);
			TypeDeclaration targetClass= getTargetType(destinationCuNode);
			targetClass.bodyDeclarations().add(newMethod);
		}

		private static String getUnindentedSource(ISourceReference sourceReference) throws JavaModelException {
			Assert.isTrue(sourceReference instanceof IJavaElement);
			String[] lines= Strings.convertIntoLines(sourceReference.getSource());
			Strings.trimIndentation(lines, CodeFormatterUtil.getTabWidth(), false);
			return Strings.concatenate(lines, StubUtility.getLineDelimiterUsed((IJavaElement) sourceReference));
		}

		private static AST getAST(OldASTRewrite rewrite){
			return rewrite.getAST();
		}
		
		private TypeDeclaration getTargetType(CompilationUnit destinationCuNode) throws JavaModelException {
			return ASTNodeSearchUtil.getTypeDeclarationNode(getDestinationAsType(), destinationCuNode);
		}

		private IType getDestinationAsType() throws JavaModelException {
			IJavaElement destination= getJavaElementDestination();
			IType enclosingType= getEnclosingType(destination);
			if (enclosingType != null)
				return enclosingType;
			ICompilationUnit enclosingCu= getEnclosingCu(destination);
			Assert.isNotNull(enclosingCu);
			IType mainType= JavaElementUtil.getMainType(enclosingCu);
			Assert.isNotNull(mainType);
			return mainType;
		}

		private static ICompilationUnit getEnclosingCu(IJavaElement destination) {
			if (destination instanceof ICompilationUnit)
				return (ICompilationUnit) destination;
			return (ICompilationUnit)destination.getAncestor(IJavaElement.COMPILATION_UNIT);
		}

		private static IType getEnclosingType(IJavaElement destination) {
			if (destination instanceof IType)
				return (IType) destination;
			return (IType)destination.getAncestor(IJavaElement.TYPE);
		}
		
		public boolean canEnable() throws JavaModelException {
			if (! super.canEnable()) return false;
			for (int i= 0; i < fJavaElements.length; i++) {
				if (fJavaElements[i] instanceof IMember){
					IMember member= (IMember) fJavaElements[i];
					//we can copy some binary members, but not all
					if (member.isBinary() && member.getSourceRange() == null)
						return false;
				}
			}
			return true;
		}	
		
		protected RefactoringStatus verifyDestination(IJavaElement destination) throws JavaModelException {
			Assert.isNotNull(destination);
			if (!destination.exists())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.doesnotexist1")); //$NON-NLS-1$
			if (destination instanceof IJavaModel)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.jmodel")); //$NON-NLS-1$
			if (! (destination instanceof ICompilationUnit) && ! ReorgUtils.isInsideCompilationUnit(destination))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.cannot")); //$NON-NLS-1$

			ICompilationUnit destinationCu= getDestinationCu(destination);
			Assert.isNotNull(destinationCu);
			if (destinationCu.isReadOnly())//the resource read-onliness is handled by validateEdit
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.cannot_modify")); //$NON-NLS-1$
				
			switch(destination.getElementType()){
				case IJavaElement.COMPILATION_UNIT:
					int[] types0= new int[]{IJavaElement.FIELD, IJavaElement.INITIALIZER, IJavaElement.METHOD};
					if (ReorgUtils.hasElementsOfType(getJavaElements(), types0))
						return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.cannot"));				 //$NON-NLS-1$
					break;
				case IJavaElement.PACKAGE_DECLARATION:
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.package_decl")); //$NON-NLS-1$
				
				case IJavaElement.IMPORT_CONTAINER:
					if (ReorgUtils.hasElementsNotOfType(getJavaElements(), IJavaElement.IMPORT_DECLARATION))
						return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.cannot")); //$NON-NLS-1$
					break;
					
				case IJavaElement.IMPORT_DECLARATION:
					if (ReorgUtils.hasElementsNotOfType(getJavaElements(), IJavaElement.IMPORT_DECLARATION))
						return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.cannot")); //$NON-NLS-1$
					break;
				
				case IJavaElement.FIELD://fall thru
				case IJavaElement.INITIALIZER://fall thru
				case IJavaElement.METHOD://fall thru
					return verifyDestination(destination.getParent());
				
				case IJavaElement.TYPE:
					int[] types1= new int[]{IJavaElement.IMPORT_DECLARATION, IJavaElement.IMPORT_CONTAINER, IJavaElement.PACKAGE_DECLARATION};
					if (ReorgUtils.hasElementsOfType(getJavaElements(), types1))
						return verifyDestination(destination.getParent());
					break;
			}
				
			return new RefactoringStatus();
		}
	}

	private static abstract class PackageFragmentRootsReorgPolicy extends ReorgPolicy {

		private IPackageFragmentRoot[] fPackageFragmentRoots;

		public IJavaElement[] getJavaElements(){
			return fPackageFragmentRoots;
		}

		public IResource[] getResources() {
			return new IResource[0];
		}

		public PackageFragmentRootsReorgPolicy(IPackageFragmentRoot[] roots){
			Assert.isNotNull(roots);
			fPackageFragmentRoots= roots;
		}
			
		public boolean canEnable() throws JavaModelException {
			if (! super.canEnable()) return false;
			for (int i= 0; i < fPackageFragmentRoots.length; i++) {
				if (! (ReorgUtils.isSourceFolder(fPackageFragmentRoots[i])
						|| (fPackageFragmentRoots[i].isArchive()
								&& ! fPackageFragmentRoots[i].isExternal()))) return false;
			}
			if (ReorgUtils.containsLinkedResources(fPackageFragmentRoots)) 
				return false;					
			return true;
		}
	
		protected RefactoringStatus verifyDestination(IResource resource) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.src2proj")); //$NON-NLS-1$
		}
			
		protected RefactoringStatus verifyDestination(IJavaElement javaElement) throws JavaModelException {
			Assert.isNotNull(javaElement);
			if (! javaElement.exists())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.cannot1")); //$NON-NLS-1$
			if (javaElement instanceof IJavaModel)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.jmodel")); //$NON-NLS-1$
			if (! (javaElement instanceof IJavaProject))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.src2proj")); //$NON-NLS-1$
			if (javaElement.isReadOnly())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.src2writable")); //$NON-NLS-1$
			if (ReorgUtils.isPackageFragmentRoot((IJavaProject)javaElement))
				//TODO: adapt message to archives:
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.src2nosrc")); //$NON-NLS-1$
			return new RefactoringStatus();
		}
	
		protected IJavaProject getDestinationJavaProject(){
			return (IJavaProject) getJavaElementDestination();
		}
	
		protected IPackageFragmentRoot[] getPackageFragmentRoots(){
			return fPackageFragmentRoots;
		}
		public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, IReorgQueries reorgQueries) throws JavaModelException {
			 RefactoringStatus status= super.checkFinalConditions(pm, context, reorgQueries);
			 confirmOverwritting(reorgQueries);
			 return status;
		}

		private void confirmOverwritting(IReorgQueries reorgQueries) {
			OverwriteHelper oh= new OverwriteHelper();
			oh.setPackageFragmentRoots(fPackageFragmentRoots);
			IJavaProject javaProject= getDestinationJavaProject();
			oh.confirmOverwritting(reorgQueries, javaProject);
			fPackageFragmentRoots= oh.getPackageFragmentRootsWithoutUnconfirmedOnes();
		}
	}

	private static abstract class PackagesReorgPolicy extends ReorgPolicy {
		private IPackageFragment[] fPackageFragments;
	
		public IJavaElement[] getJavaElements(){
			return fPackageFragments;
		}
		
		public IResource[] getResources() {
			return new IResource[0];
		}
		
		protected IPackageFragment[] getPackages(){
			return fPackageFragments;
		}

		public PackagesReorgPolicy(IPackageFragment[] packageFragments){
			Assert.isNotNull(packageFragments);
			fPackageFragments= packageFragments;
		}

		public boolean canEnable() throws JavaModelException {
			for (int i= 0; i < fPackageFragments.length; i++) {
				if (JavaElementUtil.isDefaultPackage(fPackageFragments[i]) || 
					fPackageFragments[i].isReadOnly())
					return false;
			}
			if (ReorgUtils.containsLinkedResources(fPackageFragments))
				return false;
			return true;
		}

		protected RefactoringStatus verifyDestination(IResource resource) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.packages")); //$NON-NLS-1$
		}
		
		protected IPackageFragmentRoot getDestinationAsPackageFragmentRoot() throws JavaModelException {
			return getDestinationAsPackageFragmentRoot(getJavaElementDestination());
		}
		
		private IPackageFragmentRoot getDestinationAsPackageFragmentRoot(IJavaElement javaElement) throws JavaModelException {
			if (javaElement == null)
				return null;
				
			if (javaElement instanceof IPackageFragmentRoot)
				return (IPackageFragmentRoot) javaElement;

			if (javaElement instanceof IPackageFragment){
				IPackageFragment pack= (IPackageFragment)javaElement;
				if (pack.getParent() instanceof IPackageFragmentRoot)
					return (IPackageFragmentRoot) pack.getParent();
			}

			if (javaElement instanceof IJavaProject)
				return ReorgUtils.getCorrespondingPackageFragmentRoot((IJavaProject) javaElement);				
			return null;
		}
		
		protected RefactoringStatus verifyDestination(IJavaElement javaElement) throws JavaModelException {
			Assert.isNotNull(javaElement);
			if (! javaElement.exists())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.cannot1")); //$NON-NLS-1$
			if (javaElement instanceof IJavaModel)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.jmodel")); //$NON-NLS-1$
			IPackageFragmentRoot destRoot= getDestinationAsPackageFragmentRoot(javaElement);
			if (! ReorgUtils.isSourceFolder(destRoot))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.packages")); //$NON-NLS-1$
			return new RefactoringStatus();
		}
		
		public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, IReorgQueries reorgQueries) throws JavaModelException {
			RefactoringStatus refactoringStatus= super.checkFinalConditions(pm, context, reorgQueries);
			confirmOverwritting(reorgQueries);
			return refactoringStatus;
		}
		
		private void confirmOverwritting(IReorgQueries reorgQueries) throws JavaModelException {
			OverwriteHelper oh= new OverwriteHelper();
			oh.setPackages(fPackageFragments);
			IPackageFragmentRoot destRoot= getDestinationAsPackageFragmentRoot();
			oh.confirmOverwritting(reorgQueries, destRoot);
			fPackageFragments= oh.getPackagesWithoutUnconfirmedOnes();
		}
	}

	private static class CopySubCuElementsPolicy extends SubCuElementReorgPolicy implements ICopyPolicy{
		CopySubCuElementsPolicy(IJavaElement[] javaElements){
			super(javaElements);
		}
			
		public RefactoringParticipant[] loadParticipants(RefactoringStatus status, RefactoringProcessor processor, String[] natures, SharableParticipants shared) {
			return new RefactoringParticipant[0];
		}
		
		public Change createChange(IProgressMonitor pm, INewNameQueries copyQueries) throws JavaModelException {
			try {					
				CompilationUnit sourceCuNode= createSourceCuNode();
				ICompilationUnit destinationCu= getDestinationCu();
				ASTParser parser= ASTParser.newParser(AST.LEVEL_2_0);
				parser.setSource(destinationCu);
				CompilationUnit destinationCuNode= (CompilationUnit) parser.createAST(pm);
				OldASTRewrite rewrite= new OldASTRewrite(destinationCuNode);
				IJavaElement[] javaElements= getJavaElements();
				for (int i= 0; i < javaElements.length; i++) {
					copyToDestination(javaElements[i], rewrite, sourceCuNode, destinationCuNode);
				}
				return addTextEditFromRewrite(destinationCu, rewrite);
			} catch (JavaModelException e){
				throw e;
			} catch (CoreException e) {
				throw new JavaModelException(e);
			}
		}

		private CompilationUnit createSourceCuNode(){
			Assert.isTrue(getSourceCu() != null || getSourceClassFile() != null);
			Assert.isTrue(getSourceCu() == null || getSourceClassFile() == null);
			ASTParser parser= ASTParser.newParser(AST.LEVEL_2_0);
			if (getSourceCu() != null)
				parser.setSource(getSourceCu());
			else
				parser.setSource(getSourceClassFile());
			return (CompilationUnit) parser.createAST(null);
		}

		private IClassFile getSourceClassFile() {
			//all have a common parent, so all must be in the same classfile
			//we checked before that the array in not null and not empty
			return (IClassFile) getJavaElements()[0].getAncestor(IJavaElement.CLASS_FILE);
		}
			
		public boolean canEnable() throws JavaModelException {
			return 	super.canEnable() && 
					(getSourceCu() != null || getSourceClassFile() != null);
		}
			
		public IFile[] getAllModifiedFiles() {
			return ReorgUtils.getFiles(new IResource[]{ReorgUtils.getResource(getDestinationCu())});
		}
	}
	private static class CopyFilesFoldersAndCusPolicy extends FilesFoldersAndCusReorgPolicy implements ICopyPolicy{

		CopyFilesFoldersAndCusPolicy(IFile[] files, IFolder[] folders, ICompilationUnit[] cus){
			super(files, folders, cus);
		}
				
		public RefactoringParticipant[] loadParticipants(RefactoringStatus status, RefactoringProcessor processor, String[] natures, SharableParticipants shared) {
			return new RefactoringParticipant[0];
		}
		
		public Change createChange(IProgressMonitor pm, INewNameQueries copyQueries) {
			IFile[] file= getFiles();
			IFolder[] folders= getFolders();
			ICompilationUnit[] cus= getCus();
			pm.beginTask("", cus.length + file.length + folders.length); //$NON-NLS-1$
			NewNameProposer nameProposer= new NewNameProposer();
			CompositeChange composite= new CompositeChange(RefactoringCoreMessages.getString("ReorgPolicy.copy")); //$NON-NLS-1$
			composite.markAsSynthetic();
			for (int i= 0; i < cus.length; i++) {
				composite.add(createChange(cus[i], nameProposer, copyQueries));
				pm.worked(1);
			}
			for (int i= 0; i < file.length; i++) {
				composite.add(createChange(file[i], nameProposer, copyQueries));
				pm.worked(1);
			}
			for (int i= 0; i < folders.length; i++) {
				composite.add(createChange(folders[i], nameProposer, copyQueries));
				pm.worked(1);
			}
			pm.done();
			return composite;
		}

		private Change createChange(ICompilationUnit unit, NewNameProposer nameProposer, INewNameQueries copyQueries) {
			IPackageFragment pack= getDestinationAsPackageFragment();
			if (pack != null)
				return copyCuToPackage(unit, pack, nameProposer, copyQueries);
			IContainer container= getDestinationAsContainer();
			return copyFileToContainer(unit, container, nameProposer, copyQueries);
		}
	
		private static Change copyFileToContainer(ICompilationUnit cu, IContainer dest, NewNameProposer nameProposer, INewNameQueries copyQueries) {
			IResource resource= ReorgUtils.getResource(cu);
			return createCopyResourceChange(resource, nameProposer, copyQueries, dest);
		}

		private Change createChange(IResource resource, NewNameProposer nameProposer, INewNameQueries copyQueries) {
			IContainer dest= getDestinationAsContainer();
			return createCopyResourceChange(resource, nameProposer, copyQueries, dest);
		}
			
		private static Change createCopyResourceChange(IResource resource, NewNameProposer nameProposer, INewNameQueries copyQueries, IContainer destination) {
			if (resource == null || destination == null)
				return new NullChange();
			INewNameQuery nameQuery;
			String name= nameProposer.createNewName(resource, destination);
			if (name == null)
				nameQuery= copyQueries.createNullQuery();
			else
				nameQuery= copyQueries.createNewResourceNameQuery(resource, name);
			return new CopyResourceChange(resource, destination, nameQuery);
		}

		private static Change copyCuToPackage(ICompilationUnit cu, IPackageFragment dest, NewNameProposer nameProposer, INewNameQueries copyQueries) {
			//XXX workaround for bug 31998 we will have to disable renaming of linked packages (and cus)
			IResource res= ReorgUtils.getResource(cu);
			if (res != null && res.isLinked()){
				if (ResourceUtil.getResource(dest) instanceof IContainer)
					return copyFileToContainer(cu, (IContainer)ResourceUtil.getResource(dest), nameProposer, copyQueries);
			}
		
			String newName= nameProposer.createNewName(cu, dest);
			Change simpleCopy= new CopyCompilationUnitChange(cu, dest, copyQueries.createStaticQuery(newName));
			if (newName == null || newName.equals(cu.getElementName()))
				return simpleCopy;
		
			try {
				IPath newPath= ResourceUtil.getResource(cu).getParent().getFullPath().append(newName);				
				INewNameQuery nameQuery= copyQueries.createNewCompilationUnitNameQuery(cu, newName);
				return new CreateCopyOfCompilationUnitChange(newPath, cu.getSource(), cu, nameQuery);
			} catch(CoreException e) {
				return simpleCopy; //fallback - no ui here
			}
		}
	}
	private static class CopyPackageFragmentRootsPolicy extends PackageFragmentRootsReorgPolicy implements ICopyPolicy{
		public CopyPackageFragmentRootsPolicy(IPackageFragmentRoot[] roots){
			super(roots);
		}
		
		public RefactoringParticipant[] loadParticipants(RefactoringStatus status, RefactoringProcessor processor, String[] natures, SharableParticipants shared) {
			return new RefactoringParticipant[0];
		}
		
		public Change createChange(IProgressMonitor pm, INewNameQueries copyQueries) {
			NewNameProposer nameProposer= new NewNameProposer();
			IPackageFragmentRoot[] roots= getPackageFragmentRoots();
			pm.beginTask("", roots.length); //$NON-NLS-1$
			CompositeChange composite= new CompositeChange(RefactoringCoreMessages.getString("ReorgPolicy.copy_source_folder")); //$NON-NLS-1$
			composite.markAsSynthetic();
			IJavaProject destination= getDestinationJavaProject();
			Assert.isNotNull(destination);
			for (int i= 0; i < roots.length; i++) {
				composite.add(createChange(roots[i], destination, nameProposer, copyQueries));
				pm.worked(1);
			}
			pm.done();
			return composite;
		}
		
		private Change createChange(IPackageFragmentRoot root, IJavaProject destination, NewNameProposer nameProposer, INewNameQueries copyQueries) {
			IResource res= root.getResource();
			IProject destinationProject= destination.getProject();
			String newName= nameProposer.createNewName(res, destinationProject);
			INewNameQuery nameQuery;
			if (newName == null )
				nameQuery= copyQueries.createNullQuery();
			else
				nameQuery= copyQueries.createNewPackageFragmentRootNameQuery(root, newName);
			//TODO sounds wrong that this change works on IProjects
			//TODO fix the query problem
			return new CopyPackageFragmentRootChange(root, destinationProject, nameQuery,  null);
		}
	}
	private static class CopyPackagesPolicy extends PackagesReorgPolicy implements ICopyPolicy{

		public CopyPackagesPolicy(IPackageFragment[] packageFragments){
			super(packageFragments);
		}

		public RefactoringParticipant[] loadParticipants(RefactoringStatus status, RefactoringProcessor processor, String[] natures, SharableParticipants shared) {
			return new RefactoringParticipant[0];
		}
		
		public Change createChange(IProgressMonitor pm, INewNameQueries newNameQueries) throws JavaModelException {
			NewNameProposer nameProposer= new NewNameProposer();
			IPackageFragment[] fragments= getPackages();
			pm.beginTask("", fragments.length); //$NON-NLS-1$
			CompositeChange composite= new CompositeChange(RefactoringCoreMessages.getString("ReorgPolicy.copy_package")); //$NON-NLS-1$
			composite.markAsSynthetic();
			IPackageFragmentRoot root= getDestinationAsPackageFragmentRoot();
			for (int i= 0; i < fragments.length; i++) {
				composite.add(createChange(fragments[i], root, nameProposer, newNameQueries));
				pm.worked(1);
			}
			pm.done();
			return composite;
		}

		private Change createChange(IPackageFragment pack, IPackageFragmentRoot destination, NewNameProposer nameProposer, INewNameQueries copyQueries) {
			String newName= nameProposer.createNewName(pack, destination);
			if (newName == null || JavaConventions.validatePackageName(newName).getSeverity() < IStatus.ERROR){
				INewNameQuery nameQuery;
				if (newName == null)
					nameQuery= copyQueries.createNullQuery();
				else
					nameQuery= copyQueries.createNewPackageNameQuery(pack, newName);
				return new CopyPackageChange(pack, destination, nameQuery);
			} else {
				if (destination.getResource() instanceof IContainer){
					IContainer dest= (IContainer)destination.getResource();
					IResource res= pack.getResource();
					INewNameQuery nameQuery= copyQueries.createNewResourceNameQuery(res, newName);
					return new CopyResourceChange(res, dest, nameQuery);
				}else
					return new NullChange();
			}	
		}
	}
	private static class NoCopyPolicy extends ReorgPolicy implements ICopyPolicy{
		public boolean canEnable() throws JavaModelException {
			return false;
		}
		public RefactoringParticipant[] loadParticipants(RefactoringStatus status, RefactoringProcessor processor, String[] natures, SharableParticipants shared) {
			return new RefactoringParticipant[0];
		}
		protected RefactoringStatus verifyDestination(IResource resource) throws JavaModelException {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.noCopying")); //$NON-NLS-1$
		}
		protected RefactoringStatus verifyDestination(IJavaElement javaElement) throws JavaModelException {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.noCopying")); //$NON-NLS-1$
		}
		public Change createChange(IProgressMonitor pm, INewNameQueries copyQueries) {
			return new NullChange();
		}
		public IResource[] getResources() {
			return new IResource[0];
		}
		public IJavaElement[] getJavaElements() {
			return new IJavaElement[0];
		}
	}
	private static class NewNameProposer{
		private final Set fAutoGeneratedNewNames= new HashSet(2);
			
		public String createNewName(ICompilationUnit cu, IPackageFragment destination){
			if (isNewNameOk(destination, cu.getElementName()))
				return null;
			if (! ReorgUtils.isParentInWorkspaceOrOnDisk(cu, destination))
				return null;
			int i= 1;
			while (true){
				String newName;
				if (i == 1)
					newName= RefactoringCoreMessages.getFormattedString("CopyRefactoring.cu.copyOf1", //$NON-NLS-1$
								cu.getElementName());
				else	
					newName= RefactoringCoreMessages.getFormattedString("CopyRefactoring.cu.copyOfMore", //$NON-NLS-1$
								new String[]{String.valueOf(i), cu.getElementName()});
				if (isNewNameOk(destination, newName) && ! fAutoGeneratedNewNames.contains(newName)){
					fAutoGeneratedNewNames.add(newName);
					return removeTrailingJava(newName);
				}
				i++;
			}
		}
		private static String removeTrailingJava(String name) {
			Assert.isTrue(name.endsWith(".java")); //$NON-NLS-1$
			return name.substring(0, name.length() - ".java".length()); //$NON-NLS-1$
		}
		public String createNewName(IResource res, IContainer destination){
			if (isNewNameOk(destination, res.getName()))
				return null;
			if (! ReorgUtils.isParentInWorkspaceOrOnDisk(res, destination))
				return null;
			int i= 1;
			while (true){
				String newName;
				if (i == 1)
					newName= RefactoringCoreMessages.getFormattedString("CopyRefactoring.resource.copyOf1", //$NON-NLS-1$
								res.getName());
				else
					newName= RefactoringCoreMessages.getFormattedString("CopyRefactoring.resource.copyOfMore", //$NON-NLS-1$
								new String[]{String.valueOf(i), res.getName()});
				if (isNewNameOk(destination, newName) && ! fAutoGeneratedNewNames.contains(newName)){
					fAutoGeneratedNewNames.add(newName);
					return newName;
				}
				i++;
			}	
		}
	
		public String createNewName(IPackageFragment pack, IPackageFragmentRoot destination){
			if (isNewNameOk(destination, pack.getElementName()))
				return null;
			if (! ReorgUtils.isParentInWorkspaceOrOnDisk(pack, destination))
				return null;
			int i= 1;
			while (true){
				String newName;
				if (i == 1)
					newName= RefactoringCoreMessages.getFormattedString("CopyRefactoring.package.copyOf1", //$NON-NLS-1$
								pack.getElementName());
				else
					newName= RefactoringCoreMessages.getFormattedString("CopyRefactoring.package.copyOfMore", //$NON-NLS-1$
								new String[]{String.valueOf(i), pack.getElementName()});
				if (isNewNameOk(destination, newName) && ! fAutoGeneratedNewNames.contains(newName)){
					fAutoGeneratedNewNames.add(newName);
					return newName;
				}
				i++;
			}	
		}
		private static boolean isNewNameOk(IPackageFragment dest, String newName) {
			return ! dest.getCompilationUnit(newName).exists();
		}
	
		private static boolean isNewNameOk(IContainer container, String newName) {
			return container.findMember(newName) == null;
		}

		private static boolean isNewNameOk(IPackageFragmentRoot root, String newName) {
			return ! root.getPackageFragment(newName).exists() ;
		}
	}

	private static class MovePackageFragmentRootsPolicy extends PackageFragmentRootsReorgPolicy implements IMovePolicy{
			
		MovePackageFragmentRootsPolicy(IPackageFragmentRoot[] roots){
			super(roots);
		}
		public RefactoringParticipant[] loadParticipants(RefactoringStatus status, RefactoringProcessor processor, String[] natures, SharableParticipants shared) throws CoreException {
			List result=new ArrayList();
			ResourceModifications modifications= new ResourceModifications();
			IJavaProject destination= getDestinationJavaProject();
			boolean updateReferences= canUpdateReferences() && getUpdateReferences();
			if (destination != null) {
				IPackageFragmentRoot[] roots= getPackageFragmentRoots();
				for (int i= 0; i < roots.length; i++) {
					IPackageFragmentRoot root= roots[i];
					result.addAll(Arrays.asList(ParticipantManager.loadMoveParticipants(status, 
						processor, root, 
						new MoveArguments(destination, updateReferences), natures, shared)));
					if (root.getResource() != null && destination.getResource() != null)
						modifications.addMove(root.getResource(), 
							new MoveArguments(destination.getResource(), updateReferences));
				}
			}
			result.addAll(Arrays.asList(modifications.getParticipants(status, processor, natures, shared)));
			return (RefactoringParticipant[])result.toArray(new RefactoringParticipant[result.size()]);
		}
		
		public Change createChange(IProgressMonitor pm) throws JavaModelException {
			IPackageFragmentRoot[] roots= getPackageFragmentRoots();
			pm.beginTask("", roots.length); //$NON-NLS-1$
			CompositeChange composite= new CompositeChange(RefactoringCoreMessages.getString("ReorgPolicy.move_source_folder")); //$NON-NLS-1$
			composite.markAsSynthetic();
			IJavaProject destination= getDestinationJavaProject();
			Assert.isNotNull(destination);
			for (int i= 0; i < roots.length; i++) {
				composite.add(createChange(roots[i], destination));
				pm.worked(1);
			}
			pm.done();
			return composite;
		}
		
		public Change postCreateChange(Change[] participantChanges, IProgressMonitor pm) throws CoreException {
			return null;
		}

		private Change createChange(IPackageFragmentRoot root, IJavaProject destination) {
			///XXX fix the query
			return new MovePackageFragmentRootChange(root, destination.getProject(), null);
		}
		
		protected RefactoringStatus verifyDestination(IJavaElement javaElement) throws JavaModelException {
			RefactoringStatus superStatus= super.verifyDestination(javaElement);
			if (superStatus.hasFatalError())
				return superStatus;
			IJavaProject javaProject= getDestinationJavaProject();
			if (isParentOfAny(javaProject, getPackageFragmentRoots()))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.element2parent")); //$NON-NLS-1$
			return superStatus;
		}

		private static boolean isParentOfAny(IJavaProject javaProject, IPackageFragmentRoot[] roots) {
			for (int i= 0; i < roots.length; i++) {
				if (ReorgUtils.isParentInWorkspaceOrOnDisk(roots[i], javaProject)) return true;
			}
			return false;
		}
		
		public boolean canEnable() throws JavaModelException {
			if (! super.canEnable()) return false;
			IPackageFragmentRoot[] roots= getPackageFragmentRoots();
			for (int i= 0; i < roots.length; i++) {
				if (roots[i].isReadOnly() && 
						! (roots[i].isArchive() && ! roots[i].getResource().isReadOnly()))	return  false;
			}
			return true;
		}

		public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, IReorgQueries reorgQueries) throws JavaModelException {
			try{
				RefactoringStatus status= super.checkFinalConditions(pm, context, reorgQueries);
				confirmMovingReadOnly(reorgQueries);
				return status;
			} catch(JavaModelException e){
				throw e;
			} catch (CoreException e) {
				throw new JavaModelException(e);
			}
		}

		private void confirmMovingReadOnly(IReorgQueries reorgQueries) throws CoreException {
			if (! ReadOnlyResourceFinder.confirmMoveOfReadOnlyElements(getJavaElements(), getResources(), reorgQueries))
				throw new OperationCanceledException(); //saying 'no' to this one is like cancelling the whole operation
		}		
	}
	
	private static class MovePackagesPolicy extends PackagesReorgPolicy implements IMovePolicy{

		MovePackagesPolicy(IPackageFragment[] packageFragments){
			super(packageFragments);
		}
		public RefactoringParticipant[] loadParticipants(RefactoringStatus status, RefactoringProcessor processor, String[] natures, SharableParticipants shared) throws CoreException {
			List result= new ArrayList();
			ResourceModifications modifications= new ResourceModifications();
			boolean updateReferences= canUpdateReferences() && getUpdateReferences();
			IPackageFragment[] packages= getPackages();
			IPackageFragmentRoot javaDestination= getDestinationAsPackageFragmentRoot();
			for (int i= 0; i < packages.length; i++) {
				IPackageFragment pack= packages[i];
				result.addAll(Arrays.asList(ParticipantManager.loadMoveParticipants(status, 
					processor, pack, 
					new MoveArguments(javaDestination, updateReferences), natures, shared)));
				IResource resourceDestination= javaDestination.getResource();
				IContainer container= (IContainer)pack.getResource();
				if (container == null || resourceDestination == null)
					continue;
				IPath path= resourceDestination.getFullPath();
				path= path.append(pack.getElementName().replace('.', '/'));
				IResource[] members= container.members();
				int files= 0;
				IFolder target= ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
				if (!target.exists()) {
					modifications.addCreate(target);
				}
				for (int m= 0; m < members.length; m++) {
					IResource member= members[m];
					if (member instanceof IFile) {
						files++;
						IFile file= (IFile)member;
						if ("class".equals(file.getFileExtension()) && file.isDerived()) //$NON-NLS-1$
							continue;
						modifications.addMove(member, new MoveArguments(target, updateReferences));
					}
				}
				if (files == members.length) {
					modifications.addDelete(container);
				}
			}
			result.addAll(Arrays.asList(modifications.getParticipants(status, processor, natures, shared)));
			return (RefactoringParticipant[]) result.toArray(new RefactoringParticipant[result.size()]);
		}
		protected RefactoringStatus verifyDestination(IJavaElement javaElement) throws JavaModelException {
			RefactoringStatus superStatus= super.verifyDestination(javaElement);
			if (superStatus.hasFatalError())
				return superStatus;
			
			IPackageFragmentRoot root= getDestinationAsPackageFragmentRoot();
			if (isParentOfAny(root, getPackages()))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.package2parent")); //$NON-NLS-1$
			return superStatus;
		}
		
		private static boolean isParentOfAny(IPackageFragmentRoot root, IPackageFragment[] fragments) {
			for (int i= 0; i < fragments.length; i++) {
				IPackageFragment fragment= fragments[i];
				if (ReorgUtils.isParentInWorkspaceOrOnDisk(fragment, root)) return true;
			}
			return false;
		}

		public Change createChange(IProgressMonitor pm) throws JavaModelException {
			IPackageFragment[] fragments= getPackages();
			pm.beginTask("", fragments.length); //$NON-NLS-1$
			CompositeChange result= new CompositeChange(RefactoringCoreMessages.getString("ReorgPolicy.move_package")); //$NON-NLS-1$
			result.markAsSynthetic();
			IPackageFragmentRoot root= getDestinationAsPackageFragmentRoot();
			for (int i= 0; i < fragments.length; i++) {
				result.add(createChange(fragments[i], root));
				pm.worked(1);
			}
			pm.done();
			return result;
		}
		
		public Change postCreateChange(Change[] participantChanges, IProgressMonitor pm) throws CoreException {
			return null;
		}

		private Change createChange(IPackageFragment pack, IPackageFragmentRoot destination) {
			return new MovePackageChange(pack, destination);
		}
		
		public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, IReorgQueries reorgQueries) throws JavaModelException {
			try{
				RefactoringStatus status= super.checkFinalConditions(pm, context, reorgQueries);
				confirmMovingReadOnly(reorgQueries);
				return status;
			} catch(JavaModelException e){
				throw e;
			} catch (CoreException e) {
				throw new JavaModelException(e);
			}
		}

		private void confirmMovingReadOnly(IReorgQueries reorgQueries) throws CoreException {
			if (! ReadOnlyResourceFinder.confirmMoveOfReadOnlyElements(getJavaElements(), getResources(), reorgQueries))
				throw new OperationCanceledException(); //saying 'no' to this one is like cancelling the whole operation
		}		
	}
	
	private static class MoveFilesFoldersAndCusPolicy extends FilesFoldersAndCusReorgPolicy implements IMovePolicy{
		
		private boolean fUpdateReferences;
		private boolean fUpdateQualifiedNames;
		private QualifiedNameSearchResult fQualifiedNameSearchResult;
		private String fFilePatterns;
		private TextChangeManager fChangeManager;
		private final CodeGenerationSettings fSettings;

		MoveFilesFoldersAndCusPolicy(IFile[] files, IFolder[] folders, ICompilationUnit[] cus, CodeGenerationSettings settings){
			super(files, folders, cus);
			Assert.isNotNull(settings);
			fUpdateReferences= true;
			fUpdateQualifiedNames= false;
			fQualifiedNameSearchResult= new QualifiedNameSearchResult();
			fSettings= settings;
		}
		
		public RefactoringParticipant[] loadParticipants(RefactoringStatus status, RefactoringProcessor processor, String[] natures, SharableParticipants shared) throws CoreException {
			IPackageFragment pack= getDestinationAsPackageFragment();
			IContainer container= getDestinationAsContainer();
			List result= new ArrayList();
			List derived= new ArrayList();
			ResourceModifications modifications= new ResourceModifications();
			Object unitDestination= null;
			if (pack != null)
				unitDestination= pack;
			else
				unitDestination= container;
			
			// don't use fUpdateReferences directly since it is only valid if 
			// canUpdateReferences is true
			boolean updateReferenes= canUpdateReferences() && getUpdateReferences();
			if (unitDestination != null) {
				ICompilationUnit[] units= getCus();
				for (int i= 0; i < units.length; i++) {
					ICompilationUnit unit= units[i];
					result.addAll(Arrays.asList(ParticipantManager.loadMoveParticipants(status, 
						processor, unit, 
						new MoveArguments(unitDestination, updateReferenes), natures, shared)));
					IType[] types= unit.getTypes();
					for (int tt= 0; tt < types.length; tt++) {
						IType type= types[tt];
						derived.addAll(Arrays.asList(ParticipantManager.loadMoveParticipants(status, 
							processor, type, 
							new MoveArguments(unitDestination, updateReferenes), natures, shared)));
					}
					if (container != null && unit.getResource() != null) {
						modifications.addMove(unit.getResource(), new MoveArguments(container, updateReferenes));
					}
				}
			}
			if (container != null) {
				IFile[] files= getFiles();
				for (int i= 0; i < files.length; i++) {
					IFile file= files[i];
					result.addAll(Arrays.asList(ParticipantManager.loadMoveParticipants(status, 
						processor, file, 
						new MoveArguments(container, updateReferenes), natures, shared)));
				}
				IFolder[] folders= getFolders();
				for (int i= 0; i < folders.length; i++) {
					IFolder folder= folders[i];
					result.addAll(Arrays.asList(ParticipantManager.loadMoveParticipants(status, 
						processor, folder, 
						new MoveArguments(container, updateReferenes), natures, shared)));
				}
			}
			result.addAll(derived);
			result.addAll(Arrays.asList(modifications.getParticipants(status, processor, natures, shared)));
			return (RefactoringParticipant[])result.toArray(new RefactoringParticipant[result.size()]);
		}
		
		protected RefactoringStatus verifyDestination(IJavaElement destination) throws JavaModelException {
			RefactoringStatus superStatus= super.verifyDestination(destination);
			if (superStatus.hasFatalError())
				return superStatus;

			Object commonParent= new ParentChecker(getResources(), getJavaElements()).getCommonParent();
			if (destination.equals(commonParent)) 
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.parent")); //$NON-NLS-1$
			IContainer destinationAsContainer= getDestinationAsContainer();
			if (destinationAsContainer != null && destinationAsContainer.equals(commonParent))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.parent")); //$NON-NLS-1$
			IPackageFragment destinationAsPackage= getDestinationAsPackageFragment();
			if (destinationAsPackage != null && destinationAsPackage.equals(commonParent))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.parent")); //$NON-NLS-1$
				
			return superStatus;
		}
		
		protected RefactoringStatus verifyDestination(IResource destination) throws JavaModelException {
			RefactoringStatus superStatus= super.verifyDestination(destination);
			if (superStatus.hasFatalError())
				return superStatus;

			Object commonParent= getCommonParent();
			if (destination.equals(commonParent)) 
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.parent")); //$NON-NLS-1$
			IContainer destinationAsContainer= getDestinationAsContainer();
			if (destinationAsContainer != null && destinationAsContainer.equals(commonParent))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.parent")); //$NON-NLS-1$
			IJavaElement destinationContainerAsPackage= getDestinationContainerAsJavaElement();
			if (destinationContainerAsPackage != null && destinationContainerAsPackage.equals(commonParent))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.parent")); //$NON-NLS-1$
			
			return superStatus;
		}
		
		private Object getCommonParent(){
			return new ParentChecker(getResources(), getJavaElements()).getCommonParent();
		}

		public Change createChange(IProgressMonitor pm) throws JavaModelException {
			if (! fUpdateReferences) {
				return createSimpleMoveChange(pm);
			} else {
				return createReferenceUpdatingMoveChange(pm);
			}
		}
		
		public Change postCreateChange(Change[] participantChanges, IProgressMonitor pm) throws CoreException {
			if (fQualifiedNameSearchResult != null) {
				return fQualifiedNameSearchResult.getSingleChange(Changes.getModifiedFiles(participantChanges));
			} else {
				return null;
			}
		}

		private Change createReferenceUpdatingMoveChange(IProgressMonitor pm) throws JavaModelException {
			pm.beginTask("", 2 + (fUpdateQualifiedNames ? 1 : 0)); //$NON-NLS-1$
			try{
				CompositeChange composite= new CompositeChange(RefactoringCoreMessages.getString("ReorgPolicy.move")); //$NON-NLS-1$
				composite.markAsSynthetic();
				//XX workaround for bug 13558
				//<workaround>
				if (fChangeManager == null){
					fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1));
					RefactoringStatus status= Checks.validateModifiesFiles(getAllModifiedFiles());
					if (status.hasFatalError())
						fChangeManager= new TextChangeManager();
				}	
				//</workaround>
							
				composite.merge(new CompositeChange(RefactoringCoreMessages.getString("MoveRefactoring.reorganize_elements"), fChangeManager.getAllChanges())); //$NON-NLS-1$
						
				Change fileMove= createSimpleMoveChange(new SubProgressMonitor(pm, 1));
				if (fileMove instanceof CompositeChange) {
					composite.merge(((CompositeChange)fileMove));		
				} else{
					composite.add(fileMove);
				}
				return composite;
			} finally{
				pm.done();
			}
		}

		private TextChangeManager createChangeManager(IProgressMonitor pm) throws JavaModelException {
			pm.beginTask("", 1);//$NON-NLS-1$
			try{
				if (! fUpdateReferences)
					return new TextChangeManager();
			
				IPackageFragment packageDest= getDestinationAsPackageFragment();
				if (packageDest != null){			
					MoveCuUpdateCreator creator= new MoveCuUpdateCreator(getCus(), packageDest, fSettings);
					return creator.createChangeManager(new SubProgressMonitor(pm, 1));
				} else 
					return new TextChangeManager();
			} finally {
				pm.done();
			}		
		}

		private Change createSimpleMoveChange(IProgressMonitor pm) {
			CompositeChange result= new CompositeChange(RefactoringCoreMessages.getString("ReorgPolicy.move")); //$NON-NLS-1$
			result.markAsSynthetic();
			IFile[] files= getFiles();
			IFolder[] folders= getFolders();
			ICompilationUnit[] cus= getCus();
			pm.beginTask("", files.length + folders.length + cus.length); //$NON-NLS-1$
			for (int i= 0; i < files.length; i++) {
				result.add(createChange(files[i]));
				pm.worked(1);
			}
			for (int i= 0; i < folders.length; i++) {
				result.add(createChange(folders[i]));
				pm.worked(1);
			}
			for (int i= 0; i < cus.length; i++) {
				result.add(createChange(cus[i]));
				pm.worked(1);
			}
			pm.done();
			return result;
		}

		private Change createChange(ICompilationUnit cu){
			IPackageFragment pack= getDestinationAsPackageFragment();
			if (pack != null)
				return moveCuToPackage(cu, pack);
			IContainer container= getDestinationAsContainer();
			if (container == null)
				return new NullChange();
			return moveFileToContainer(cu, container);
		}

		private static Change moveCuToPackage(ICompilationUnit cu, IPackageFragment dest) {
			//XXX workaround for bug 31998 we will have to disable renaming of linked packages (and cus)
			IResource resource= ResourceUtil.getResource(cu);
			if (resource != null && resource.isLinked()){
				if (ResourceUtil.getResource(dest) instanceof IContainer)
					return moveFileToContainer(cu, (IContainer)ResourceUtil.getResource(dest));
			}
			return new MoveCompilationUnitChange(cu, dest);
		}

		private static Change moveFileToContainer(ICompilationUnit cu, IContainer dest) {
			return new MoveResourceChange(ResourceUtil.getResource(cu), dest);
		}

		private Change createChange(IResource res) {
			IContainer destinationAsContainer= getDestinationAsContainer();
			if (destinationAsContainer == null)
				return new NullChange();
			return new MoveResourceChange(res, destinationAsContainer);
		}

		private void computeQualifiedNameMatches(IProgressMonitor pm) throws JavaModelException {
			if (!fUpdateQualifiedNames)
				return;
			IPackageFragment destination= getDestinationAsPackageFragment();
			
			ICompilationUnit[] cus= getCus();
			pm.beginTask("", cus.length); //$NON-NLS-1$
			pm.subTask(RefactoringCoreMessages.getString("MoveRefactoring.scanning_qualified_names")); //$NON-NLS-1$
			for (int i= 0; i < cus.length; i++) {
				ICompilationUnit cu= cus[i];
				IType[] types= cu.getTypes();
				IProgressMonitor typesMonitor= new SubProgressMonitor(pm, 1);
				typesMonitor.beginTask("", types.length); //$NON-NLS-1$
				for (int j= 0; j < types.length; j++) {
					handleType(types[j], destination, new SubProgressMonitor(typesMonitor, 1));
				}
				typesMonitor.done();
			}
			pm.done();
		}

		private void handleType(IType type, IPackageFragment destination, IProgressMonitor pm) throws JavaModelException {
			QualifiedNameFinder.process(fQualifiedNameSearchResult, type.getFullyQualifiedName(),  destination.getElementName() + "." + type.getTypeQualifiedName(), //$NON-NLS-1$
				fFilePatterns, type.getJavaProject().getProject(), pm);
		}	
		
		public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, IReorgQueries reorgQueries) throws JavaModelException {
			try{
				pm.beginTask("", 3); //$NON-NLS-1$
				confirmMovingReadOnly(reorgQueries);
				fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1));
				if (fUpdateQualifiedNames) {
					computeQualifiedNameMatches(new SubProgressMonitor(pm, 2));
				}
				RefactoringStatus status= super.checkFinalConditions(new SubProgressMonitor(pm, 1), context, reorgQueries);
				return status;
			} catch (JavaModelException e){
				throw e;
			} catch (CoreException e) {
				throw new JavaModelException(e);
			} finally{
				pm.done();
			}
		}
		
		private void confirmMovingReadOnly(IReorgQueries reorgQueries) throws CoreException {
			if (! ReadOnlyResourceFinder.confirmMoveOfReadOnlyElements(getJavaElements(), getResources(), reorgQueries))
				throw new OperationCanceledException(); //saying 'no' to this one is like cancelling the whole operation
		}		
		
		public IFile[] getAllModifiedFiles() {
			Set result= new HashSet();
			result.addAll(Arrays.asList(ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits())));
			result.addAll(Arrays.asList(fQualifiedNameSearchResult.getAllFiles()));
			if (getDestinationAsPackageFragment() != null)
				result.addAll(Arrays.asList(ResourceUtil.getFiles(getCus())));
			return (IFile[]) result.toArray(new IFile[result.size()]);
		}
		public boolean hasAllInputSet() {
			return super.hasAllInputSet() && ! canUpdateReferences() && ! canUpdateQualifiedNames();
		}
		public boolean canUpdateReferences(){
			if (getCus().length == 0)
				return false;
			IPackageFragment pack= getDestinationAsPackageFragment();
			if (pack == null || pack.isDefaultPackage())
				return false;
			Object commonParent= getCommonParent();
			if (JavaElementUtil.isDefaultPackage(commonParent))
				return false;
			return true;							
		}
		public boolean getUpdateReferences() {
			return fUpdateReferences;
		}
		public void setUpdateReferences(boolean update) {
			fUpdateReferences= update;
		}
		
		public boolean canEnableQualifiedNameUpdating() {
			return getCus().length > 0 && ! JavaElementUtil.isDefaultPackage(getCommonParent()) ;
		}
		
		public boolean canUpdateQualifiedNames() {
			IPackageFragment pack= getDestinationAsPackageFragment();
			return (canEnableQualifiedNameUpdating() && pack != null && !pack.isDefaultPackage());
		}

		public boolean getUpdateQualifiedNames() {
			return fUpdateQualifiedNames;
		}

		public void setUpdateQualifiedNames(boolean update) {
			fUpdateQualifiedNames= update;
		}

		public String getFilePatterns() {
			return fFilePatterns;
		}

		public void setFilePatterns(String patterns) {
			Assert.isNotNull(patterns);
			fFilePatterns= patterns;
		}
	}
	
	private static class MoveSubCuElementsPolicy extends SubCuElementReorgPolicy implements IMovePolicy{
		MoveSubCuElementsPolicy(IJavaElement[] javaElements){
			super(javaElements);
		}
		public RefactoringParticipant[] loadParticipants(RefactoringStatus status, RefactoringProcessor processor, String[] natures, SharableParticipants shared) {
			return new RefactoringParticipant[0];
		}
		protected RefactoringStatus verifyDestination(IJavaElement destination) throws JavaModelException {
			RefactoringStatus superStatus= super.verifyDestination(destination);
			if (superStatus.hasFatalError())
				return superStatus;
				
			Object commonParent= new ParentChecker(new IResource[0], getJavaElements()).getCommonParent();
			if (destination.equals(commonParent) || destination.getParent().equals(commonParent))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.element2parent")); //$NON-NLS-1$
			return superStatus;
		}

		private CompilationUnit createSourceCuNode(ICompilationUnit cu){
			ASTParser parser= ASTParser.newParser(AST.LEVEL_2_0);
			parser.setSource(cu);
			return (CompilationUnit) parser.createAST(null);
		}

		public Change createChange(IProgressMonitor pm) throws JavaModelException {
			try {
				CompilationUnit sourceCuNode= createSourceCuNode(getSourceCu());
				ICompilationUnit destinationCu= getDestinationCu();
				CompilationUnit destinationCuNode= getSourceCu().equals(destinationCu) ? sourceCuNode: createSourceCuNode(destinationCu);
				OldASTRewrite targetRewrite= new OldASTRewrite(destinationCuNode);
				IJavaElement[] javaElements= getJavaElements();
				for (int i= 0; i < javaElements.length; i++) {
					copyToDestination(javaElements[i], targetRewrite, sourceCuNode, destinationCuNode);
				}
				OldASTRewrite sourceRewrite= getSourceCu().equals(destinationCu) ? targetRewrite: new OldASTRewrite(sourceCuNode);
				ASTNodeDeleteUtil.markAsDeleted(javaElements, sourceCuNode, sourceRewrite);
				Change targetCuChange= addTextEditFromRewrite(destinationCu, targetRewrite);
				if (getSourceCu().equals(destinationCu)){
					return targetCuChange;
				} else{
					CompositeChange result= new CompositeChange(RefactoringCoreMessages.getString("ReorgPolicy.move_members")); //$NON-NLS-1$
					result.markAsSynthetic();
					result.add(targetCuChange);
					if (Arrays.asList(getJavaElements()).containsAll(Arrays.asList(getSourceCu().getTypes())))
						result.add(DeleteChangeCreator.createDeleteChange(null, 
							new IResource[0], new ICompilationUnit[]{getSourceCu()},
							RefactoringCoreMessages.getString("ReorgPolicy.move"))); //$NON-NLS-1$);
					else
						result.add(addTextEditFromRewrite(getSourceCu(), sourceRewrite));						
					return result;
				}
			} catch (JavaModelException e){
				throw e;
			} catch (CoreException e) {
				throw new JavaModelException(e);
			}
		}
		
		public Change postCreateChange(Change[] participantChanges, IProgressMonitor pm) throws CoreException {
			return null;
		}

		public boolean canEnable() throws JavaModelException {
			return super.canEnable() && getSourceCu() != null; //can move only elements from cus
		}

		public IFile[] getAllModifiedFiles() {
			return ReorgUtils.getFiles(new IResource[]{ReorgUtils.getResource(getSourceCu()), ReorgUtils.getResource(getDestinationCu())});
		}
	}
		
	private static class NoMovePolicy extends ReorgPolicy implements IMovePolicy{
		protected RefactoringStatus verifyDestination(IResource resource) throws JavaModelException {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.noMoving")); //$NON-NLS-1$
		}
		public RefactoringParticipant[] loadParticipants(RefactoringStatus status, RefactoringProcessor processor, String[] natures, SharableParticipants shared) {
			return new RefactoringParticipant[0];
		}
		protected RefactoringStatus verifyDestination(IJavaElement javaElement) throws JavaModelException {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ReorgPolicyFactory.noMoving")); //$NON-NLS-1$
		}
		public Change createChange(IProgressMonitor pm) {
			return new NullChange();
		}
		public Change postCreateChange(Change[] participantChanges, IProgressMonitor pm) throws CoreException {
			return null;
		}
		public boolean canEnable() throws JavaModelException {
			return false;
		}
		public IResource[] getResources() {
			return new IResource[0];
		}
		public IJavaElement[] getJavaElements() {
			return new IJavaElement[0];
		}
	}
	
	private static class ActualSelectionComputer {
		private final IResource[] fResources;
		private final IJavaElement[] fJavaElements;

		public ActualSelectionComputer(IJavaElement[]  javaElements, IResource[] resources) {
			fJavaElements= javaElements;
			fResources= resources;
		}
		
		public IJavaElement[] getActualJavaElementsToReorg() throws JavaModelException {
			List result= new ArrayList();
			for (int i= 0; i < fJavaElements.length; i++) {
				IJavaElement element= fJavaElements[i];
				if (element == null)
					continue;
				if (ReorgUtils.isDeletedFromEditor(element))
					continue;
				if (element instanceof IType) {
					IType type= (IType)element;
					ICompilationUnit cu= type.getCompilationUnit();
					if (cu != null && type.getDeclaringType() == null && cu.exists() && cu.getTypes().length == 1 && ! result.contains(cu))
						result.add(cu);
					else if (! result.contains(type))
						result.add(type);
				} else if (! result.contains(element)){
					result.add(element);
				}
			}
			return (IJavaElement[]) result.toArray(new IJavaElement[result.size()]);
		}
		
		public IResource[] getActualResourcesToReorg() {
			Set javaElementSet= new HashSet(Arrays.asList(fJavaElements));	
			List result= new ArrayList();
			for (int i= 0; i < fResources.length; i++) {
				if (fResources[i] == null)
					continue;
				IJavaElement element= JavaCore.create(fResources[i]);
				if (element == null || ! element.exists() || ! javaElementSet.contains(element))
					if (! result.contains(fResources[i]))
							result.add(fResources[i]);
			}
			return (IResource[]) result.toArray(new IResource[result.size()]);

		}
	}
}