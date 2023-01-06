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

public class Anon4 {
    final static String v15 = "Anon4";
    final static String v14 = "Anon4";

    Anon4() {
        new Object() {
            final static String v15 = "Anon4$3";
            final static String v14 = "Anon4$7";
        };
        new Object() {
            final static String v15 = "Anon4$4";
            final static String v14 = "Anon4$8";
        };
    }

    void instanceMethod() {
        new Object() {
            final static String v15 = "Anon4$5";
            final static String v14 = "Anon4$9";
        };
        new Object() {
            final static String v15 = "Anon4$6";
            final static String v14 = "Anon4$10";
        };

        class A3 {
            final static String v15 = "Anon4$1A3";
            final static String v14 = "Anon4$1$A3";

            void instanceMethod() {
                new Object() {
                    final static String v15 = "Anon4$1A3$3";
                    final static String v14 = "Anon4$13";
                };
                new Object() {
                    final static String v15 = "Anon4$1A3$4";
                    final static String v14 = "Anon4$14";
                };
            }

            {
                new Object() {
                    final static String v15 = "Anon4$1A3$1";
                    final static String v14 = "Anon4$11";
                };
                new Object() {
                    final static String v15 = "Anon4$1A3$2";
                    final static String v14 = "Anon4$12";
                };
            }

        }
    }

    static void staticMethod() {
        new Object() {
            final static String v15 = "Anon4$7";
            final static String v14 = "Anon4$15";
        };
        new Object() {
            final static String v15 = "Anon4$8";
            final static String v14 = "Anon4$16";
        };
    }

    static {
        new Object() {
            final static String v15 = "Anon4$1";
            final static String v14 = "Anon4$1";
        };

        new Object() {
            final static String v15 = "Anon4$2";
            final static String v14 = "Anon4$2";
        };
    }

    class A2 {
        final static String v15 = "Anon4$A2";
        final static String v14 = "Anon4$A2";

        void instanceMethod() {
            new Object() {
                final static String v15 = "Anon4$A2$3";
                final static String v14 = "Anon4$5";
            };
            new Object() {
                final static String v15 = "Anon4$A2$4";
                final static String v14 = "Anon4$6";
            };
        }

        {
            new Object() {
                final static String v15 = "Anon4$A2$1";
                final static String v14 = "Anon4$3";
            };
            new Object() {
                final static String v15 = "Anon4$A2$2";
                final static String v14 = "Anon4$4";
            };
        }


    }
}
