/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.surround;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.DeleteNodeEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.MultiTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;

public class SurroundWithTryCatchRefactoring extends Refactoring {

	private Selection fSelection;
	private int fTabWidth;
	private CodeGenerationSettings fSettings;
	private SurroundWithTryCatchAnalyzer fAnalyzer;
	private boolean fSaveChanges;

	private ICompilationUnit fCUnit;

	private static final class LocalDeclarationEdit extends MultiTextEdit {
		private int fOffset;
		public LocalDeclarationEdit(int offset, ASTNode[] locals) {
			for (int i= 0; i < locals.length; i++) {
				add(new DeleteNodeEdit(locals[i], true));
			}
			fOffset= offset;
		}
		private LocalDeclarationEdit(int offset, List children, boolean copy) throws CoreException {
			super(children);
			fOffset= offset;
		}
		public void connect(TextBufferEditor editor) throws CoreException {
			super.connect(editor);
			for (Iterator iter= iterator(); iter.hasNext();) {
				DeleteNodeEdit edit= (DeleteNodeEdit)iter.next();
				if (edit.getTextRange().getOffset() == fOffset)
					return;
			}
			editor.add(SimpleTextEdit.createInsert(fOffset, "\t")); //$NON-NLS-1$
		}
		public MultiTextEdit copy() throws CoreException {
			return new LocalDeclarationEdit(fOffset, getChildren(), true);	
		}
	}

	public SurroundWithTryCatchRefactoring(ICompilationUnit cu, ITextSelection selection,  int tabWidth, CodeGenerationSettings settings) {
		fCUnit= cu;
		fSelection= Selection.createFromStartLength(selection.getOffset(), selection.getLength());
		fTabWidth= tabWidth;
		fSettings= settings;
	}
	
	public SurroundWithTryCatchRefactoring(ICompilationUnit cu, int offset, int length, CodeGenerationSettings settings) {
		fCUnit= cu;
		fSelection= Selection.createFromStartLength(offset, length);
		fTabWidth= settings.tabWidth;
		fSettings= settings;
	}

	
	
	public void setSaveChanges(boolean saveChanges) {
		fSaveChanges= saveChanges;
	}
	
	/* non Java-doc
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("SurroundWithTryCatchRefactoring.name"); //$NON-NLS-1$
	}

	public RefactoringStatus checkActivationBasics(CompilationUnit rootNode, IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
			
		fAnalyzer= new SurroundWithTryCatchAnalyzer(fCUnit, fSelection);
		rootNode.accept(fAnalyzer);
		result.merge(fAnalyzer.getStatus());
		return result;
	}


	/*
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		try {
			RefactoringStatus result= new RefactoringStatus();
			result.merge(Checks.validateModifiesFiles(ResourceUtil.getFiles(new ICompilationUnit[]{fCUnit})));
			if (result.hasFatalError())
				return result;
			
			CompilationUnit rootNode= AST.parseCompilationUnit(fCUnit, true);
			return checkActivationBasics(rootNode, pm);
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}	
	}

	/*
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		return new RefactoringStatus();
	}

	/* non Java-doc
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		TextBuffer buffer= null;
		try {
			CompilationUnitChange result= new CompilationUnitChange(getName(), fCUnit);
			result.setSave(fSaveChanges);
			// We already have a text buffer since we start a surround with from the editor. So it is fast
			// to acquire it here.
			buffer= TextBuffer.acquire(getFile());
			int start= buffer.getLineOfOffset(fAnalyzer.getFirstSelectedNode().getStartPosition());
			String indent= CodeFormatterUtil.createIndentString(buffer.getLineIndent(start, fTabWidth));
			String delimiter= buffer.getLineDelimiter(start);
			addEdits(result, buffer, delimiter, indent);
			return result;
		} catch (CoreException e) {
			throw new JavaModelException(e);
		} finally {
			if (buffer != null)
				TextBuffer.release(buffer);
		}
	}
	
	private void addEdits(TextChange change, TextBuffer buffer, String delimiter, String indent) throws JavaModelException {
		final String NN= ""; //$NON-NLS-1$
		final ITypeBinding[] exceptions= fAnalyzer.getExceptions();
		final VariableDeclaration[] locals= fAnalyzer.getAffectedLocals();
		int start= getStartOffset();
		int end= getEndOffset();
		int firstLine= buffer.getLineOfOffset(start);
		int lastLine= buffer.getLineOfOffset(end);
		int offset= buffer.getLineInformationOfOffset(start).getOffset();
		for (int i= locals.length - 1; i >= 0; i--) {
			change.addTextEdit(NN, SimpleTextEdit.createInsert(start, createLocal(buffer, locals[i], delimiter, indent)));
		}
		change.addTextEdit(NN, SimpleTextEdit.createInsert(start, "try {")); //$NON-NLS-1$
		ASTNode[] affectedLocals= getAffectedLocals(buffer.getLineInformation(firstLine), locals);
		if (affectedLocals.length == 0) {
			change.addTextEdit(NN, SimpleTextEdit.createInsert(start, delimiter + indent + "\t")); //$NON-NLS-1$
		} else {
			for (int i= 0; i < affectedLocals.length; i++) {
				change.addTextEdit(NN, new DeleteNodeEdit(affectedLocals[i], false));
			}
		}
		for (int i= firstLine + 1; i <= lastLine; i++) {
			processLine(change, buffer, i, locals);
		}
		offset= end;
		ImportEdit importEdit= new ImportEdit(fCUnit, fSettings);
		for (int i= 0; i < exceptions.length; i++) {
			ITypeBinding exception= exceptions[i];
			change.addTextEdit(NN, SimpleTextEdit.createInsert(offset, createCatchBlock(exception, delimiter, indent)));
			importEdit.addImport(Bindings.getFullyQualifiedImportName(exception));
		}
		change.addTextEdit(NN, SimpleTextEdit.createInsert(offset, delimiter + indent + "}"));	 //$NON-NLS-1$
		if (!importEdit.isEmpty())
			change.addTextEdit(NN, importEdit);
	}
	
	private int getStartOffset() throws JavaModelException {
		return fAnalyzer.getFirstSelectedNode().getStartPosition();
	}
	
	private int getEndOffset() throws JavaModelException {
		return ASTNodes.getExclusiveEnd(fAnalyzer.getLastSelectedNode());
	}
	
	private String createLocal(TextBuffer buffer, VariableDeclaration local, String delimiter, String indent) {
		StringBuffer result= new StringBuffer();
		if (local instanceof VariableDeclarationFragment) {
			result.append(ASTNodes.asString(ASTNodes.getType(local)));
			result.append(" "); //$NON-NLS-1$
		}
		result.append(buffer.getContent(local.getStartPosition(), local.getLength()));
		result.append(';');
		result.append(delimiter);
		result.append(indent);
		return result.toString();
	}
	
	private void processLine(TextChange change, TextBuffer buffer, int line, VariableDeclaration[] locals) {
		TextRegion region= buffer.getLineInformation(line);
		ASTNode[] coveredLocals= getAffectedLocals(region, locals);
		if (coveredLocals.length == 0) {
			change.addTextEdit("", SimpleTextEdit.createInsert(region.getOffset(), "\t"));			 //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			change.addTextEdit("", new LocalDeclarationEdit(region.getOffset(), coveredLocals)); //$NON-NLS-1$
		}
	}
	
	private ASTNode[] getAffectedLocals(TextRegion region, VariableDeclaration[] locals) {
		List result= new ArrayList(1);
		for (int i= 0; i < locals.length; i++) {
			VariableDeclaration local= locals[i];
			if (region.getOffset() <= local.getStartPosition() && ASTNodes.getInclusiveEnd(local) < region.getOffset() + region.getLength()) {
				if (ASTNodes.isSingleDeclaration(local))
					result.add(local.getParent());
				else 
					result.add(local);
			}
		}
		return (ASTNode[]) result.toArray(new ASTNode[result.size()]);
	}
	
	private String createCatchBlock(ITypeBinding exception, String delimiter, String indent) {
		StringBuffer buffer= new StringBuffer();
		buffer.append(delimiter);
		buffer.append(indent);
		buffer.append("} catch("); //$NON-NLS-1$
		buffer.append(exception.getName());
		buffer.append(" e) {"); //$NON-NLS-1$
		return buffer.toString();
	}
	
	private IFile getFile() throws JavaModelException {
		if (fCUnit.isWorkingCopy())
			return (IFile)fCUnit.getOriginalElement().getCorrespondingResource();
		else
			return (IFile)fCUnit.getCorrespondingResource();
	}
}
