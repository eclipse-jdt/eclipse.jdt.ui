/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.surround;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.DeleteNodeEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.refactoring.ExtendedBuffer;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.util.AST;
import org.eclipse.jdt.internal.corext.refactoring.util.Selection;
import org.eclipse.jdt.internal.corext.textmanipulation.MultiTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;
import org.eclipse.jdt.internal.corext.textmanipulation.TextUtil;
import org.eclipse.jdt.internal.corext.util.Bindings;

public class SurroundWithTryCatchRefactoring extends Refactoring {

	private ICompilationUnit fCUnit;
	private Selection fSelection;
	private int fTabWidth;
	private CodeGenerationSettings fSettings;
	private SurroundWithTryCatchAnalyzer fAnalyzer;
	private boolean fSaveChanges;

	private static final class LocalDeclarationEdit extends MultiTextEdit {
		private int fOffset;
		public LocalDeclarationEdit(int offset, List locals) {
			for (Iterator iter= locals.iterator(); iter.hasNext();) {
				LocalDeclaration element= (LocalDeclaration)iter.next();
				add(new DeleteNodeEdit(element, true));
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
			editor.add(SimpleTextEdit.createInsert(fOffset, "\t"));
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
	
	public void setSaveChanges(boolean saveChanges) {
		fSaveChanges= saveChanges;
	}
	
	/* non Java-doc
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return "Surround with try/catch block";
	}
	/*
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		AST ast= new AST(fCUnit);
		if (ast.isMalformed()) {
			IProblem[] problems= ast.getProblems();
			if (problems.length > 0 && problems[0].isError()) {
				IProblem problem= problems[0];
				return RefactoringStatus.createFatalErrorStatus(
					RefactoringCoreMessages.getFormattedString("Refactoring.compilation_error", //$NON-NLS-1$
						new Object[] {new Integer(problem.getSourceLineNumber()), problem.getMessage() }),
					JavaSourceContext.create(fCUnit, new SourceRange(problem.getSourceStart(),
						problem.getSourceEnd() - problem.getSourceStart() + 1)));
			} else {
				return RefactoringStatus.createFatalErrorStatus(
					RefactoringCoreMessages.getString("Refactoring.generic_compilation_error")); //$NON-NLS-1$
			}
		}
		fAnalyzer= new SurroundWithTryCatchAnalyzer(new ExtendedBuffer(fCUnit.getBuffer()), fSelection);
		ast.accept(fAnalyzer);
		result.merge(fAnalyzer.getStatus());
		return result;
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
			int start= buffer.getLineOfOffset(fAnalyzer.getStartOfFirstSelectedNode());
			String indent= TextUtil.createIndentString(buffer.getLineIndent(start, fTabWidth));
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
		final String NN= "";
		final TypeBinding[] exceptions= fAnalyzer.getExceptions();
		final LocalDeclaration[] locals= fAnalyzer.getAffectedLocals();
		int start= getStartOffset();
		int end= getEndOffset();
		int firstLine= buffer.getLineOfOffset(start);
		int lastLine= buffer.getLineOfOffset(end);
		int offset= buffer.getLineInformationOfOffset(start).getOffset();
		for (int i= locals.length - 1; i >= 0; i--) {
			change.addTextEdit(NN, SimpleTextEdit.createInsert(start, createLocal(buffer, locals[i], delimiter, indent)));
		}
		change.addTextEdit(NN, SimpleTextEdit.createInsert(start, "try {"));
		List affectedLocals= getLocals(buffer.getLineInformation(firstLine), locals);
		if (affectedLocals.isEmpty()) {
			change.addTextEdit(NN, SimpleTextEdit.createInsert(start, delimiter + indent + "\t"));
		} else {
			for (Iterator iter= affectedLocals.iterator(); iter.hasNext();) {
				LocalDeclaration element= (LocalDeclaration)iter.next();
				change.addTextEdit(NN, new DeleteNodeEdit(element, false));
			}
		}
		for (int i= firstLine + 1; i <= lastLine; i++) {
			processLine(change, buffer, i, locals);
		}
		offset= end;
		ImportEdit importEdit= new ImportEdit(fCUnit, fSettings);
		for (int i= 0; i < exceptions.length; i++) {
			TypeBinding exception= exceptions[i];
			change.addTextEdit(NN, SimpleTextEdit.createInsert(offset, createCatchBlock(exception, delimiter, indent)));
			importEdit.addImport(Bindings.makeFullyQualifiedName(exception.qualifiedPackageName(), exception.qualifiedSourceName()));
		}
		change.addTextEdit(NN, SimpleTextEdit.createInsert(offset, delimiter + indent + "}"));	
		if (!importEdit.isEmpty())
			change.addTextEdit(NN, importEdit);
	}
	
	private int getStartOffset() throws JavaModelException {
		return fAnalyzer.getStartOfFirstSelectedNode();
	}
	
	private int getEndOffset() throws JavaModelException {
		ExtendedBuffer scanner= new ExtendedBuffer(fCUnit.getBuffer());
		int offset= fAnalyzer.getEndOfLastSelectedNode() + 1;
		int nextStatement= scanner.indexOfStatementCharacter(offset);
		int semicolon= scanner.indexOf(Scanner.TokenNameSEMICOLON, offset);
		if (semicolon != -1 && semicolon < nextStatement)
			return semicolon + 1;
		else
			return offset;
	}
	
	private String createLocal(TextBuffer buffer, LocalDeclaration local, String delimiter, String indent) {
		StringBuffer result= new StringBuffer();
		result.append(buffer.getContent(local.declarationSourceStart, local.declarationSourceEnd - local.declarationSourceStart + 1));
		result.append(';');
		result.append(delimiter);
		result.append(indent);
		return result.toString();
	}
	
	private void processLine(TextChange change, TextBuffer buffer, int line, LocalDeclaration[] locals) {
		TextRegion region= buffer.getLineInformation(line);
		List coveredLocals= getLocals(region, locals);
		if (coveredLocals.isEmpty()) {
			change.addTextEdit("", SimpleTextEdit.createInsert(region.getOffset(), "\t"));			
		} else {
			change.addTextEdit("", new LocalDeclarationEdit(region.getOffset(), coveredLocals));
		}
	}
	
	private List getLocals(TextRegion region, LocalDeclaration[] locals) {
		List result= new ArrayList(1);
		for (int i= 0; i < locals.length; i++) {
			LocalDeclaration local= locals[i];
			if (region.getOffset() <= local.declarationSourceStart && local.declarationSourceEnd < region.getOffset() + region.getLength()) {
				result.add(local);
			}
		}
		return result;
	}
	
	private String createCatchBlock(TypeBinding exception, String delimiter, String indent) {
		StringBuffer buffer= new StringBuffer();
		buffer.append(delimiter);
		buffer.append(indent);
		buffer.append("} catch(");
		buffer.append(exception.sourceName());
		buffer.append(" e) {");
		return buffer.toString();
	}
	
	private IFile getFile() throws JavaModelException {
		if (fCUnit.isWorkingCopy())
			return (IFile)fCUnit.getOriginalElement().getCorrespondingResource();
		else
			return (IFile)fCUnit.getCorrespondingResource();
	}
}
