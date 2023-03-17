/*******************************************************************************
 * Copyright (c) 2023 Eric Bruneton and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eric Bruneton - initial API and implementation
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.bcoview.asm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.SimpleVerifier;
import org.objectweb.asm.tree.analysis.Value;

import org.eclipse.jdt.bcoview.BytecodeOutlinePlugin;
import org.eclipse.jdt.bcoview.preferences.BCOConstants;

import org.eclipse.core.runtime.IStatus;

public class DecompiledMethod {

	private final List<Object> text;

	private final List<LocalVariableNode> localVariables;

	/**
	 * decompiled line -> source line
	 */
	private final Map<Integer, Integer> sourceLines;

	/**
	 * source line -> decompiled line
	 */
	private final Map<Integer, Integer> decompiledLines;

	/**
	 * decompiled line -> insn
	 */
	private final Map<Integer, Integer> insns;

	/**
	 * decompiled line -> opcode
	 */
	private final Map<Integer, Integer> opcodes;

	/**
	 * insn -> decompile line
	 */
	private final Map<Integer, Integer> insnLines;

	private int lineCount;

	/**
	 * first source line, if any
	 */
	private int firstSourceLine;

	/**
	 * last source line, if any
	 */
	private int lastSourceLine;

	MethodNode meth;

	private Frame<?>[] frames;

	private String error;

	private int errorInsn;

	private final String owner;


	private final Map<Label, Integer> lineNumbers;

	private final DecompilerOptions options;

	private final int access;


	public DecompiledMethod(String owner, Map<Label, Integer> lineNumbers, MethodNode meth, DecompilerOptions options, int access) {
		this.meth = meth;
		this.owner = owner;
		this.lineNumbers = lineNumbers;
		this.options = options;
		this.access = access;
		this.text = new ArrayList<>();
		this.localVariables = meth.localVariables;
		this.sourceLines = new HashMap<>();
		this.decompiledLines = new HashMap<>();
		this.insns = new HashMap<>();
		this.opcodes = new HashMap<>();
		this.insnLines = new HashMap<>();
	}

	void setText(List<?> inputText) {
		formatText(inputText, new HashMap<>(), new StringBuffer(), this.text);
		computeMaps(lineNumbers);

		if (options.modes.get(BCOConstants.F_SHOW_ANALYZER) && (access & Opcodes.ACC_ABSTRACT) == 0) {
			analyzeMethod();
		}
	}

	void addLineNumber(Label start, Integer integer) {
		lineNumbers.put(start, integer);
	}

	public boolean isInit() {
		return ("<init>".equals(meth.name) && "()V".equals(meth.desc)) //$NON-NLS-1$ //$NON-NLS-2$
				|| "<clinit>".equals(meth.name); //$NON-NLS-1$
	}

	public String getSignature() {
		return meth.name + meth.desc;
	}

	public boolean containsSource(int sourceLine) {
		return sourceLine >= getFirstSourceLine() && sourceLine <= getLastSourceLine();
	}

	/**
	 * @param sourceLine line in sources
	 * @return nearest match above given source line or the given line for perfect match or -1 for
	 *         no match. The return value is method-relative, and need to be transformed to class
	 *         absolute
	 */
	public int getBestDecompiledLine(final int sourceLine) {
		if (!containsSource(sourceLine)) {
			return -1;
		}
		Set<Integer> set = decompiledLines.keySet();
		if (set.size() == 0) {
			return -1;
		}
		int bestMatch = -1;
		for (Integer integer : set) {
			int line = integer.intValue();
			int delta = sourceLine - line;
			if (delta < 0) {
				continue;
			} else if (delta == 0) {
				return line;
			}
			if (bestMatch < 0 || delta < sourceLine - bestMatch) {
				bestMatch = line;
			}
		}
		if (bestMatch < 0) {
			return -1;
		}
		return decompiledLines.get(Integer.valueOf(bestMatch)).intValue();
	}

	private void analyzeMethod() {
		Interpreter<BasicValue> interpreter;
		try {
			Type type = Type.getType(owner);
			interpreter = new SimpleVerifier(DecompilerOptions.LATEST_ASM_VERSION, type, null, null, false) {
				//
			};
		} catch (Exception e) {
			interpreter = new BasicVerifier();
		}

		Analyzer<BasicValue> a = new Analyzer<>(interpreter);
		try {
			a.analyze(owner, meth);
		} catch (AnalyzerException e) {
			error = e.getMessage();
			if (error.startsWith("Error at instruction ")) { //$NON-NLS-1$
				error = error.substring("Error at instruction ".length()); //$NON-NLS-1$
				errorInsn = Integer.parseInt(error.substring(0, error.indexOf(':')));
				error = error.substring(error.indexOf(':') + 2);
			} else {
				BytecodeOutlinePlugin.log(e, IStatus.ERROR);
				error = null;
			}
		}
		frames = a.getFrames();
	}

	private void formatText(List<?> input, Map<Integer, String> locals, StringBuffer line, List<Object> result) {
		for (int i = 0; i < input.size(); ++i) {
			Object o = input.get(i);
			if (o instanceof List) {
				formatText((List<?>) o, locals, line, result);
			} else if (o instanceof Index) {
				result.add(o);
				updateLocals((Index) o, locals);
			} else if (o instanceof Integer) {
				String localVariableName = locals.get(o);
				if (localVariableName == null) {
					Index index = getNextIndex(input, i);
					if (index != null) {
						updateLocals(index, locals);
						localVariableName = locals.get(o);
					}
				}
				if (localVariableName != null) {
					line.append(": ").append(localVariableName); //$NON-NLS-1$
				}
			} else {
				String s = o.toString();
				int p;
				do {
					p = s.indexOf('\n');
					if (p == -1) {
						line.append(s);
					} else {
						result.add(line.toString() + s.substring(0, p + 1));
						s = s.substring(p + 1);
						line.setLength(0);
					}
				} while (p != -1);
			}
		}
	}

	private static Index getNextIndex(List<?> input, int startOffset) {
		for (int i = startOffset + 1; i < input.size(); i++) {
			Object object = input.get(i);
			if (object instanceof Index) {
				return (Index) object;
			}
		}
		return null;
	}

	private void updateLocals(Index index, Map<Integer, String> locals) {
		for (LocalVariableNode lvNode : localVariables) {
			if (lvNode.start == index.labelNode) {
				locals.put(Integer.valueOf(lvNode.index), lvNode.name);
			} else if (lvNode.end == index.labelNode) {
				locals.remove(Integer.valueOf(lvNode.index));
			}
		}
	}

	private void computeMaps(Map<Label, Integer> lineNumbers1) {
		int currentDecompiledLine = 0;
		int firstLine = -1;
		int lastLine = -1;
		for (Object o : text) {
			int currentOpcode = -1;
			int currentInsn1 = -1;
			int currentSourceLine = -1;
			if (o instanceof Index) {
				Index index = (Index) o;
				Integer sourceLine = null;
				if (index.labelNode != null) {
					sourceLine = lineNumbers1.get(index.labelNode.getLabel());
				}
				if (sourceLine != null) {
					currentSourceLine = sourceLine.intValue();
					if (firstLine == -1 || currentSourceLine < firstLine) {
						firstLine = currentSourceLine;
					}
					if (lastLine == -1 || currentSourceLine > lastLine) {
						lastLine = currentSourceLine;
					}
				}
				currentInsn1 = index.insn;
				currentOpcode = index.opcode;
			} else {
				++currentDecompiledLine;
			}
			Integer cdl = Integer.valueOf(currentDecompiledLine);
			Integer ci = Integer.valueOf(currentInsn1);
			Integer co = Integer.valueOf(currentOpcode);
			if (currentSourceLine >= 0) {
				Integer csl = Integer.valueOf(currentSourceLine);
				sourceLines.put(cdl, csl);
				if (decompiledLines.get(csl) == null) {
					decompiledLines.put(csl, cdl);
				}
			}
			insns.put(cdl, ci);
			opcodes.put(cdl, co);
			if (insnLines.get(ci) == null) {
				insnLines.put(ci, cdl);
			}
		}
		lineCount = currentDecompiledLine;
		firstSourceLine = firstLine;
		lastSourceLine = lastLine;
	}

	public String getText() {
		StringBuffer buf = new StringBuffer();
		for (Object o : text) {
			if (!(o instanceof Index)) {
				buf.append((String) o);
			}
		}
		return buf.toString();
	}

	public String[][] getTextTable() {
		Frame<?> frame = null;
		String error1 = ""; //$NON-NLS-1$
		List<String[]> lines = new ArrayList<>();
		String offsStr = null;
		for (int i = 0; i < text.size(); ++i) {
			Object o = text.get(i);
			if (o instanceof Index) {
				Index index = (Index) o;
				int insn = index.insn;

				offsStr = "" + insn; //$NON-NLS-1$
				if (frames != null && insn < frames.length) {
					frame = frames[insn];
					if (this.error != null && insn == this.errorInsn) {
						error1 = this.error;
					}
				}
			} else {
				if (offsStr == null) {
					offsStr = ""; //$NON-NLS-1$
				}
				String locals = " "; //$NON-NLS-1$
				String stack = " "; //$NON-NLS-1$
				if (frame != null) {
					StringBuffer buf = new StringBuffer();
					appendFrame(buf, frame);
					int p = buf.indexOf(" "); //$NON-NLS-1$
					locals = buf.substring(0, p);
					if ("".equals(locals)) { //$NON-NLS-1$
						locals = " "; //$NON-NLS-1$
					}
					stack = buf.substring(p + 1);
					if ("".equals(stack)) { //$NON-NLS-1$
						stack = " "; //$NON-NLS-1$
					}
				}

				lines.add(new String[] { offsStr, locals, stack, o.toString(), error1 });
				frame = null;
				error1 = ""; //$NON-NLS-1$
				offsStr = null;
			}
		}
		return lines.toArray(new String[lines.size()][]);
	}

	public int getLineCount() {
		return lineCount;
	}

	public int getErrorLine() {
		if (error == null) {
			return -1;
		}
		Integer i = insnLines.get(Integer.valueOf(errorInsn));
		return i == null ? -1 : i.intValue();
	}

	private static void appendFrame(StringBuffer buf, Frame<?> f) {
		try {
			for (int i = 0; i < f.getLocals(); ++i) {
				appendValue(buf, f.getLocal(i));
			}
			buf.append(' ');
			for (int i = 0; i < f.getStackSize(); ++i) {
				appendValue(buf, f.getStack(i));
			}
		} catch (IndexOutOfBoundsException e) {
			BytecodeOutlinePlugin.log(e, IStatus.ERROR);
		}
	}

	private static void appendValue(StringBuffer buf, Value v) {
		if (((BasicValue) v).isReference()) {
			buf.append("R"); //$NON-NLS-1$
		} else {
			buf.append(v.toString());
		}
	}

	public int getFirstSourceLine() {
		return firstSourceLine;
	}

	public int getLastSourceLine() {
		return lastSourceLine;
	}

	public int getSourceLine(int decompiledLine) {
		Integer i = sourceLines.get(Integer.valueOf(decompiledLine));
		return i == null ? -1 : i.intValue();
	}

	public Integer getBytecodeOffset(int decompiledLine) {
		return insns.get(Integer.valueOf(decompiledLine));
	}

	public Integer getBytecodeInsn(int decompiledLine) {
		return opcodes.get(Integer.valueOf(decompiledLine));
	}

	public String[][][] getFrameTables(int decompiledLine, boolean useQualifiedNames) {
		Integer insn = getBytecodeOffset(decompiledLine);
		if (insn == null) {
			return null;
		}
		return getFrameTablesForInsn(insn.intValue(), useQualifiedNames);
	}

	public String[][][] getFrameTablesForInsn(int insn, boolean useQualifiedNames) {
		if (error != null && insn == errorInsn) {
			return null;
		}
		if (frames != null && insn >= 0 && insn < frames.length) {
			Frame<?> f = frames[insn];
			if (f == null) {
				return null;
			}

			try {
				ArrayList<String[]> locals = new ArrayList<>();
				for (int i = 0; i < f.getLocals(); ++i) {
					String varName = ""; //$NON-NLS-1$
					for (LocalVariableNode lvnode : localVariables) {
						int n = lvnode.index;
						if (n == i) {
							varName = lvnode.name;
							// TODO take into account variable scope!
							break;
						}
					}

					locals.add(new String[] {
							"" + i, //$NON-NLS-1$
							getTypeName(useQualifiedNames, f.getLocal(i).toString()),
							varName });
				}

				ArrayList<String[]> stack = new ArrayList<>();
				for (int i = 0; i < f.getStackSize(); ++i) {
					stack.add(new String[] {
							"" + i, //$NON-NLS-1$
							getTypeName(useQualifiedNames, f.getStack(i).toString()) });
				}
				return new String[][][] {
					locals.toArray(new String[3][]),
					stack.toArray(new String[2][]) };
			} catch (IndexOutOfBoundsException e) {
				BytecodeOutlinePlugin.log(e, IStatus.ERROR);
			}
		}
		return null;
	}

	private static String getTypeName(boolean useQualifiedNames, String s) {
		if (!useQualifiedNames) {
			// get leading array symbols
			String arraySymbols = ""; //$NON-NLS-1$
			while (s.startsWith("[")) { //$NON-NLS-1$
				arraySymbols += "["; //$NON-NLS-1$
				s = s.substring(1);
			}

			int idx = s.lastIndexOf('/');
			if (idx > 0) {
				// from "Ljava/lang/Object;" to "Object"
				return arraySymbols + s.substring(idx + 1, s.length() - 1);
			}
			// this is the case on LVT view - ignore it
			if ("." == s) { //$NON-NLS-1$
				return arraySymbols + s;
			}
			// XXX Unresolved type
			if ("R" == s) { //$NON-NLS-1$
				return arraySymbols + s;
			}
			// resolve primitive types
			return arraySymbols + CommentedClassVisitor.getSimpleName(Type.getType(s));
		}
		return "Lnull;".equals(s) ? "null" : s; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public int getDecompiledLine(int sourceLine) {
		Integer i = decompiledLines.get(Integer.valueOf(sourceLine));
		return i == null ? -1 : i.intValue();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof DecompiledMethod)) {
			return false;
		}
		DecompiledMethod another = (DecompiledMethod) o;
		return getSignature().equals(another.getSignature()) && (owner != null ? owner.equals(another.owner) : true);
	}

	@Override
	public int hashCode() {
		return getSignature().hashCode() + (owner != null ? owner.hashCode() : 0);
	}
}
