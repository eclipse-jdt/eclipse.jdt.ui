/*******************************************************************************
 * Copyright (c) 2018 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package inner;

public class Anon1 {
    final static String v15 = "Anon1";
    final static String v14 = "Anon1";

    void instanceMethod() {
        new Object() {
            final static String v15 = "Anon1$3";
            final static String v14 = "Anon1$11";
        };
        new Object() {
            final static String v15 = "Anon1$4";
            final static String v14 = "Anon1$12";
        };
    }

    Anon1() {
        new Object() {
            final static String v15 = "Anon1$5";
            final static String v14 = "Anon1$13";
        };
        new Object() {
            final static String v15 = "Anon1$6";
            final static String v14 = "Anon1$14";
        };
    }

    static void staticMethod() {
        new Object() {
            final static String v15 = "Anon1$7";
            final static String v14 = "Anon1$15";
        };
        new Object() {
            final static String v15 = "Anon1$8";
            final static String v14 = "Anon1$16";
        };
    }

    static {
        new Object() {
            final static String v15 = "Anon1$1";
            final static String v14 = "Anon1$1";
        };

        new Object() {
            final static String v15 = "Anon1$2";
            final static String v14 = "Anon1$2";
        };
    }

    static class A2 {
        final static String v15 = "Anon1$A2";
        final static String v14 = "Anon1$A2";

        void instanceMethod() {
            new Object() {
                final static String v15 = "Anon1$A2$5";
                final static String v14 = "Anon1$7";
            };
            new Object() {
                final static String v15 = "Anon1$A2$6";
                final static String v14 = "Anon1$8";
            };
        }

        {
            new Object() {
                final static String v15 = "Anon1$A2$1";
                final static String v14 = "Anon1$3";
            };
            new Object() {
                final static String v15 = "Anon1$A2$2";
                final static String v14 = "Anon1$4";
            };
        }

        static void staticMethod() {
            new Object() {
                final static String v15 = "Anon1$A2$7";
                final static String v14 = "Anon1$9";
            };
            new Object() {
                final static String v15 = "Anon1$A2$8";
                final static String v14 = "Anon1$10";
            };
        }

        static {
            new Object() {
                final static String v15 = "Anon1$A2$3";
                final static String v14 = "Anon1$5";
            };
            new Object() {
                final static String v15 = "Anon1$A2$4";
                final static String v14 = "Anon1$6";
            };
        }
    }
}
