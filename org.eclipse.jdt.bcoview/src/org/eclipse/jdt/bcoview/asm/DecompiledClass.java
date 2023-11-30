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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import org.eclipse.jdt.bcoview.ui.JdtUtils;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;

public class DecompiledClass {

	/** key is DecompiledMethod, value is IJavaElement (Member) */
	private final Map<DecompiledMethod, IJavaElement> methodToJavaElt;

	private final List<Object> text;

	private String value;

	private final ClassNode classNode;

	private int classSize;

	private final DecompiledClassInfo classInfo;

	public DecompiledClass(final List<Object> text, DecompiledClassInfo classInfo, ClassNode classNode) {
		this.text = text;
		this.classInfo = classInfo;
		this.classNode = classNode;
		methodToJavaElt = new HashMap<>();
	}

	/**
	 * @return true if the class is either abstract or interface
	 */
	public boolean isAbstractOrInterface() {
		int accessFlags = classInfo.accessFlags;
		return (accessFlags & Opcodes.ACC_ABSTRACT) != 0 || (accessFlags & Opcodes.ACC_INTERFACE) != 0;
	}

	public boolean isDefaultMethodPossible() {
		return classInfo.major >= 8;
	}

	public String getText() {
		if (value == null) {
			StringBuffer buf = new StringBuffer();
			for (Object o : text) {
				if (o instanceof DecompiledMethod) {
					buf.append(((DecompiledMethod) o).getText());
				} else {
					buf.append(o);
				}
			}
			value = buf.toString();
		}
		return value;
	}

	public String[][] getTextTable() {
		List<String[]> lines = new ArrayList<>();
		for (int i = 0; i < text.size(); ++i) {
			Object o = text.get(i);
			if (o instanceof DecompiledMethod) {
				String[][] mlines = ((DecompiledMethod) o).getTextTable();
				for (String[] mline : mlines) {
					lines.add(mline);
				}
			} else {
				lines.add(new String[] { "", "", "", o.toString(), "" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
		}
		return lines.toArray(new String[lines.size()][]);
	}

	public int getBytecodeOffset(final int decompiledLine) {
		int currentDecompiledLine = 0;
		for (Object o : text) {
			if (o instanceof DecompiledMethod) {
				DecompiledMethod m = (DecompiledMethod) o;
				Integer offset = m.getBytecodeOffset(decompiledLine - currentDecompiledLine);
				if (offset != null) {
					return offset.intValue();
				}
				currentDecompiledLine += m.getLineCount();
			} else {
				currentDecompiledLine++;
			}
		}
		return -1;
	}

	public int getBytecodeInsn(final int decompiledLine) {
		int currentDecompiledLine = 0;
		for (Object o : text) {
			if (o instanceof DecompiledMethod) {
				DecompiledMethod m = (DecompiledMethod) o;
				Integer opcode = m.getBytecodeInsn(decompiledLine - currentDecompiledLine);
				if (opcode != null) {
					return opcode.intValue();
				}
				currentDecompiledLine += m.getLineCount();
			} else {
				currentDecompiledLine++;
			}
		}
		return -1;
	}

	public int getSourceLine(final int decompiledLine) {
		int currentDecompiledLine = 0;
		for (Object o : text) {
			if (o instanceof DecompiledMethod) {
				DecompiledMethod m = (DecompiledMethod) o;
				int l = m.getSourceLine(decompiledLine - currentDecompiledLine);
				if (l != -1) {
					return l;
				}
				currentDecompiledLine += m.getLineCount();
			} else {
				currentDecompiledLine++;
			}
		}
		return -1;
	}

	public DecompiledMethod getMethod(final int decompiledLine) {
		int currentDecompiledLine = 0;
		for (Object o : text) {
			if (o instanceof DecompiledMethod) {
				DecompiledMethod m = (DecompiledMethod) o;
				int l = m.getSourceLine(decompiledLine - currentDecompiledLine);
				if (l != -1) {
					return m;
				}
				currentDecompiledLine += m.getLineCount();
			} else {
				currentDecompiledLine++;
			}
		}
		return null;
	}

	public IJavaElement getJavaElement(int decompiledLine, IClassFile clazz) {
		DecompiledMethod method = getMethod(decompiledLine);
		if (method != null) {
			IJavaElement javaElement = methodToJavaElt.get(method);
			if (javaElement == null) {
				javaElement = JdtUtils.getMethod(clazz, method.getSignature());
				if (javaElement != null) {
					methodToJavaElt.put(method, javaElement);
				} else {
					javaElement = clazz;
				}
			}
			return javaElement;
		}
		return clazz;
	}

	public int getDecompiledLine(String methSignature) {
		int currentDecompiledLine = 0;
		for (Object o : text) {
			if (o instanceof DecompiledMethod) {
				DecompiledMethod m = (DecompiledMethod) o;
				if (methSignature.equals(m.getSignature())) {
					return currentDecompiledLine;
				}
				currentDecompiledLine += m.getLineCount();
			} else {
				currentDecompiledLine++;
			}
		}
		return 0;
	}

	public String[][][] getFrameTablesForInsn(final int insn, boolean useQualifiedNames) {
		for (Object o : text) {
			if (o instanceof DecompiledMethod) {
				DecompiledMethod m = (DecompiledMethod) o;
				String[][][] frame = m.getFrameTablesForInsn(insn, useQualifiedNames);
				if (frame != null) {
					return frame;
				}
			}
		}
		return null;
	}

	public String[][][] getFrameTables(final int decompiledLine, boolean useQualifiedNames) {
		int currentDecompiledLine = 0;
		for (Object o : text) {
			if (o instanceof DecompiledMethod) {
				DecompiledMethod m = (DecompiledMethod) o;
				String[][][] frame = m.getFrameTables(decompiledLine - currentDecompiledLine, useQualifiedNames);
				if (frame != null) {
					return frame;
				}
				currentDecompiledLine += m.getLineCount();
			} else {
				currentDecompiledLine++;
			}
		}
		return null;
	}

	public int getDecompiledLine(final int sourceLine) {
		int currentDecompiledLine = 0;
		for (Object o : text) {
			if (o instanceof DecompiledMethod) {
				DecompiledMethod m = (DecompiledMethod) o;
				int l = m.getDecompiledLine(sourceLine);
				if (l != -1) {
					return l + currentDecompiledLine;
				}
				currentDecompiledLine += m.getLineCount();
			} else {
				currentDecompiledLine++;
			}
		}
		return -1;
	}

	/**
	 * Converts method relative decompiled line to class absolute decompiled position
	 *
	 * @param m1 method for which we need absolute line position
	 * @param decompiledLine decompiled line, relative to given method (non global coord)
	 * @return class absolute decompiled line
	 */
	public int getDecompiledLine(final DecompiledMethod m1, final int decompiledLine) {
		int currentDecompiledLine = 0;
		for (Object o : text) {
			if (o instanceof DecompiledMethod) {
				if (o == m1) {
					return currentDecompiledLine + decompiledLine;
				}
				DecompiledMethod m = (DecompiledMethod) o;
				currentDecompiledLine += m.getLineCount();
			} else {
				currentDecompiledLine++;
			}
		}
		return -1;
	}

	public List<Integer> getErrorLines() {
		List<Integer> errors = new ArrayList<>();
		int currentDecompiledLine = 0;
		for (int i = 0; i < text.size(); ++i) {
			Object o = text.get(i);
			if (o instanceof DecompiledMethod) {
				DecompiledMethod m = (DecompiledMethod) o;
				int l = m.getErrorLine();
				if (l != -1) {
					errors.add(Integer.valueOf(l + currentDecompiledLine));
				}
				currentDecompiledLine += m.getLineCount();
			} else {
				currentDecompiledLine++;
			}
		}
		return errors;
	}

	public DecompiledMethod getBestDecompiledMatch(int sourceLine) {
		DecompiledMethod bestM = null;

		for (Object o : text) {
			if (o instanceof DecompiledMethod) {
				DecompiledMethod m = (DecompiledMethod) o;
				int line = m.getBestDecompiledLine(sourceLine);
				if (line > 0) {
					// doesn't work if it is a <init> or <cinit> which spawns over
					// multiple locations in code
					if (m.isInit()) {
						if (bestM != null) {
							int d1 = sourceLine - bestM.getFirstSourceLine();
							int d2 = sourceLine - m.getFirstSourceLine();
							if (d2 < d1) {
								bestM = m;
							}
						} else {
							bestM = m;
						}
					} else {
						return m;
					}
				} else {
					// check for init blocks which composed from different code lines
					if (bestM != null && bestM.isInit()) {
						if (bestM.getFirstSourceLine() < m.getFirstSourceLine()
								&& bestM.getLastSourceLine() > m.getLastSourceLine()) {
							bestM = null;
						}
					}
				}
			}
		}
		return bestM;
	}

	public LineRange getDecompiledRange(ITextSelection sourceRange) {
		int startLine = sourceRange.getStartLine() + 1;
		int endLine = sourceRange.getEndLine() + 1;
		int startDecompiledLine = getDecompiledLine(startLine);
		DecompiledMethod m1 = null;
		DecompiledMethod m2 = null;
		if (startDecompiledLine < 0) {
			m1 = getBestDecompiledMatch(startLine);
			m2 = getBestDecompiledMatch(endLine);
			if (m1 != null && m1.equals(m2)) {
				int methodStartLine = getDecompiledLine(m1.getSignature());
				startDecompiledLine = m1.getBestDecompiledLine(startLine);
				if (startDecompiledLine >= 0) {
					startDecompiledLine = methodStartLine + startDecompiledLine;
				} else {
					startDecompiledLine = methodStartLine + m1.getLineCount();
				}
			}
		}
		int endDecompiledLine = getDecompiledLine(endLine);
		if (endDecompiledLine < 0) {
			if (m2 == null) {
				m2 = getBestDecompiledMatch(endLine);
			}
			if (m2 != null && m2.equals(m1)) {
				int methodStartLine = getDecompiledLine(m2.getSignature());
				endDecompiledLine = m2.getBestDecompiledLine(endLine);
				if (endDecompiledLine >= 0) {
					endDecompiledLine = methodStartLine + endDecompiledLine;
				} else {
					endDecompiledLine = methodStartLine + m2.getLineCount();
				}
				// TODO dirty workaround
				if (endDecompiledLine < startDecompiledLine) {
					endDecompiledLine = startDecompiledLine + 1;
				}
			}
		}
		return new LineRange(startDecompiledLine, endDecompiledLine);
	}

	public ClassNode getClassNode() {
		return classNode;
	}

	public void setClassSize(int classSize) {
		this.classSize = classSize;
	}

	public int getClassSize() {
		return classSize;
	}

	public String getJavaVersion() {
		return classInfo.javaVersion.humanReadable();
	}
}
