/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 University of Manchester
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package uk.ac.man.cs.llvm.ir.types;

public class FunctionType implements Type {

    private final Type type;

    private final Type[] args;

    private final boolean isVarArg;

    public FunctionType(Type type, Type[] args, boolean isVarArg) {
        this.type = type;
        this.args = args;
        this.isVarArg = isVarArg;
    }

    public Type[] getArgumentTypes() {
        return args;
    }

    public Type getReturnType() {
        return type;
    }

    public boolean isVarArg() {
        return isVarArg;
    }

    @Override
    public int sizeof() {
        return 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(type).append(" (");

        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(args[i]);
        }

        if (isVarArg) {
            if (args.length > 0) {
                sb.append(", ");
            }
            sb.append("...");
        }
        sb.append(")");

        return sb.toString();
    }
}
