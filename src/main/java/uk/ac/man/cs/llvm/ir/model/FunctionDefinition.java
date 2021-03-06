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
package uk.ac.man.cs.llvm.ir.model;

import java.util.ArrayList;
import java.util.List;
import uk.ac.man.cs.llvm.ir.FunctionGenerator;
import uk.ac.man.cs.llvm.ir.InstructionGenerator;
import uk.ac.man.cs.llvm.ir.model.constants.*;
import uk.ac.man.cs.llvm.ir.model.elements.Instruction;
import uk.ac.man.cs.llvm.ir.model.elements.ValueInstruction;
import uk.ac.man.cs.llvm.ir.model.enums.BinaryOperator;
import uk.ac.man.cs.llvm.ir.model.enums.CastOperator;
import uk.ac.man.cs.llvm.ir.model.enums.CompareOperator;
import uk.ac.man.cs.llvm.ir.types.*;

public final class FunctionDefinition extends FunctionType implements Constant, FunctionGenerator, ValueSymbol {

    private final Symbols symbols = new Symbols();

    private final List<FunctionParameter> parameters = new ArrayList<>();

    private Block[] blocks = new Block[0];

    private int currentBlock = 0;

    private String name = ValueSymbol.UNKNOWN;

    public FunctionDefinition(FunctionType type) {
        super(type.getReturnType(), type.getArgumentTypes(), type.isVarArg());
    }

    public void accept(FunctionVisitor visitor) {
        for (Block block : blocks) {
            visitor.visit(block);
        }
    }

    @Override
    public void allocateBlocks(int count) {
        blocks = new Block[count];
        for (int i = 0; i < count; i++) {
            blocks[i] = new Block(this, i);
        }
        blocks[0].setName("");
    }

    @Override
    public void createParameter(Type type) {
        FunctionParameter parameter = new FunctionParameter(type, parameters.size());
        symbols.addSymbol(parameter);
        parameters.add(parameter);
    }

    @Override
    public void exitFunction() {
        int identifier = 1; // Zero clashes with entry block in sulong
        for (Block block : blocks) {
            if (block.getName().equals(ValueSymbol.UNKNOWN)) {
                block.setName(String.valueOf(identifier++));
            }
            for (int i = 0; i < block.getInstructionCount(); i++) {
                Instruction instruction = block.getInstruction(i);
                if (instruction instanceof ValueInstruction) {
                    ValueInstruction value = (ValueInstruction) instruction;
                    if (value.getName().equals(ValueSymbol.UNKNOWN)) {
                        value.setName(String.valueOf(identifier++));
                    }
                }
            }
        }
    }

    @Override
    public InstructionGenerator generateBlock() {
        return blocks[currentBlock++];
    }

    public Block getBlock(long idx) {
        return blocks[(int) idx];
    }

    public int getBlockCount() {
        return blocks.length;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Type getType() {
        return new PointerType(super.getType());
    }

    public List<FunctionParameter> getParameters() {
        return parameters;
    }

    public Symbols getSymbols() {
        return symbols;
    }

    @Override
    public void nameBlock(int index, String name) {
        blocks[index].setName(name);
    }

    @Override
    public void nameEntry(int index, String name) {
        symbols.setSymbolName(index, name);
    }

    @Override
    public void nameFunction(int index, int offset, String name) {
        symbols.setSymbolName(index, name);
    }

    @Override
    public void setName(String name) {
        this.name = "@" + name;
    }

    @Override
    public void createBinaryOperationExpression(Type type, int opcode, int lhs, int rhs) {
        boolean isFloatingPoint = type instanceof FloatingPointType
                || (type instanceof VectorType && ((VectorType) type).getElementType() instanceof FloatingPointType);

        symbols.addSymbol(new BinaryOperationConstant(
                type,
                BinaryOperator.decode(opcode, isFloatingPoint),
                symbols.getSymbol(lhs),
                symbols.getSymbol(rhs)));
    }

    @Override
    public void createBlockAddress(Type type, int method, int block) {
        symbols.addSymbol(new BlockAddressConstant(
                type,
                symbols.getSymbol(method),
                getBlock(block)));
    }

    @Override
    public void createCastExpression(Type type, int opcode, int value) {
        CastConstant cast = new CastConstant(type, CastOperator.decode(opcode));

        cast.setValue(symbols.getSymbol(value, cast));

        symbols.addSymbol(cast);
    }

    @Override
    public void createCompareExpression(Type type, int opcode, int lhs, int rhs) {
        CompareConstant compare = new CompareConstant(type, CompareOperator.decode(opcode));

        compare.setLHS(symbols.getSymbol(lhs, compare));
        compare.setRHS(symbols.getSymbol(rhs, compare));

        symbols.addSymbol(compare);
    }

    @Override
    public void createFloatingPoint(Type type, long bits) {
        symbols.addSymbol(new FloatingPointConstant((FloatingPointType) type, bits));
    }

    @Override
    public void createFromData(Type type, long[] data) {
        symbols.addSymbol(Constant.createFromData(type, data));
    }

    @Override
    public void creatFromString(Type type, String string, boolean isCString) {
        symbols.addSymbol(new StringConstant(type, string, isCString));
    }

    @Override
    public void createFromValues(Type type, int[] values) {
        symbols.addSymbol(Constant.createFromValues(type, symbols.getConstants(values)));
    }

    @Override
    public void createGetElementPointerExpression(Type type, int pointer, int[] indices, boolean isInbounds) {
        GetElementPointerConstant gep = new GetElementPointerConstant(type, isInbounds);

        gep.setBasePointer(symbols.getSymbol(pointer, gep));
        for (int index : indices) {
            gep.addIndex(symbols.getSymbol(index, gep));
        }

        symbols.addSymbol(gep);
    }

    @Override
    public void createInteger(Type type, long value) {
        symbols.addSymbol(new IntegerConstant((IntegerType) type, value));
    }

    @Override
    public void createNull(Type type) {
        symbols.addSymbol(new NullConstant(type));
    }

    @Override
    public void createUndefined(Type type) {
        symbols.addSymbol(new UndefinedConstant(type));
    }
}
