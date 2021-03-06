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
import uk.ac.man.cs.llvm.ir.InstructionGenerator;
import uk.ac.man.cs.llvm.ir.model.elements.*;
import uk.ac.man.cs.llvm.ir.model.enums.BinaryOperator;
import uk.ac.man.cs.llvm.ir.model.enums.CastOperator;
import uk.ac.man.cs.llvm.ir.model.enums.CompareOperator;
import uk.ac.man.cs.llvm.ir.model.enums.Flag;
import uk.ac.man.cs.llvm.ir.types.*;

public final class Block implements InstructionGenerator, ValueSymbol {

    private final FunctionDefinition method;

    private final int index;

    private final List<Instruction> instructions = new ArrayList<>();

    private String name = ValueSymbol.UNKNOWN;

    public Block(FunctionDefinition method, int index) {
        this.method = method;
        this.index = index;
    }

    public void accept(InstructionVisitor visitor) {
        for (Instruction instruction : instructions) {
            instruction.accept(visitor);
        }
    }

    private void addInstruction(Instruction element) {
        if (element instanceof ValueInstruction) {
            method.getSymbols().addSymbol(element);
        }
        instructions.add(element);
    }

    @Override
    public void createAllocation(Type type, int count, int align) {
        addInstruction(new AllocateInstruction(
                type,
                method.getSymbols().getSymbol(count),
                align));
    }

    @Override
    public void createBinaryOperation(Type type, int opcode, int flags, int lhs, int rhs) {
        boolean isFloatingPoint = type instanceof FloatingPointType
                || (type instanceof VectorType && ((VectorType) type).getElementType() instanceof FloatingPointType);

        BinaryOperator operator = BinaryOperator.decode(opcode, isFloatingPoint);

        BinaryOperationInstruction operation = new BinaryOperationInstruction(type, operator, Flag.decode(operator, flags));

        operation.setLHS(method.getSymbols().getSymbol(lhs, operation));
        operation.setRHS(method.getSymbols().getSymbol(rhs, operation));

        addInstruction(operation);
    }

    @Override
    public void createBranch(int block) {
        addInstruction(new BranchInstruction(
                method.getBlock(block)));
    }

    @Override
    public void createBranch(int condition, int blockTrue, int blockFalse) {
        addInstruction(new ConditionalBranchInstruction(
                method.getSymbols().getSymbol(condition),
                method.getBlock(blockTrue),
                method.getBlock(blockFalse)));
    }

    @Override
    public void createCall(Type type, int target, int[] arguments) {
        Call call;
        if (type == MetaType.VOID) {
            call = new VoidCallInstruction(method.getSymbols().getSymbol(target));
        } else {
            call = new CallInstruction(type, method.getSymbols().getSymbol(target));
        }

        for (int i = 0; i < arguments.length; i++) {
            call.addArgument(method.getSymbols().getSymbol(arguments[i], call));
        }

        addInstruction(call);
    }

    @Override
    public void createCast(Type type, int opcode, int value) {
        CastInstruction cast = new CastInstruction(type, CastOperator.decode(opcode));

        cast.setValue(method.getSymbols().getSymbol(value, cast));

        addInstruction(cast);
    }

    @Override
    public void createCompare(Type type, int opcode, int lhs, int rhs) {
        CompareInstruction compare = new CompareInstruction(type, CompareOperator.decode(opcode));

        compare.setLHS(method.getSymbols().getSymbol(lhs, compare));
        compare.setRHS(method.getSymbols().getSymbol(rhs, compare));

        addInstruction(compare);
    }

    @Override
    public void createExtractElement(Type type, int vector, int index) {
        addInstruction(new ExtractElementInstruction(
                type,
                method.getSymbols().getSymbol(vector),
                method.getSymbols().getSymbol(index)));
    }

    @Override
    public void createExtractValue(Type type, int aggregate, int index) {
        addInstruction(new ExtractValueInstruction(
                type,
                method.getSymbols().getSymbol(aggregate),
                index));
    }

    @Override
    public void createGetElementPointer(Type type, int pointer, int[] indices, boolean isInbounds) {
        GetElementPointerInstruction gep = new GetElementPointerInstruction(type, isInbounds);

        gep.setBasePointer(method.getSymbols().getSymbol(pointer, gep));
        for (int i = 0; i < indices.length; i++) {
            gep.addIndex(method.getSymbols().getSymbol(indices[i], gep));
        }

        addInstruction(gep);
    }

    @Override
    public void createIndirectBranch(int address, int[] successors) {
        Block[] blocks = new Block[successors.length];
        for (int i = 0; i < successors.length; i++) {
            blocks[i] = method.getBlock(successors[i]);
        }
        addInstruction(new IndirectBranchInstruction(
                method.getSymbols().getSymbol(address),
                blocks));
    }

    @Override
    public void createInsertElement(Type type, int vector, int index, int value) {
        addInstruction(new InsertElementInstruction(
                type,
                method.getSymbols().getSymbol(vector),
                method.getSymbols().getSymbol(index),
                method.getSymbols().getSymbol(value)));
    }

    @Override
    public void createInsertValue(Type type, int aggregate, int index, int value) {
        addInstruction(new InsertValueInstruction(
                type,
                method.getSymbols().getSymbol(aggregate),
                index,
                method.getSymbols().getSymbol(value)));
    }

    @Override
    public void createLoad(Type type, int source, int align, boolean isVolatile) {
        LoadInstruction load = new LoadInstruction(type, align, isVolatile);

        load.setSource(method.getSymbols().getSymbol(source, load));

        addInstruction(load);
    }

    @Override
    public void createPhi(Type type, int[] values, int[] blocks) {
        PhiInstruction phi = new PhiInstruction(type);

        for (int i = 0; i < values.length; i++) {
            phi.addCase(
                    method.getSymbols().getSymbol(values[i], phi),
                    method.getBlock(blocks[i]));
        }

        addInstruction(phi);
    }

    @Override
    public void createReturn() {
        addInstruction(new ReturnInstruction());
    }

    @Override
    public void createReturn(int value) {
        ReturnInstruction ret = new ReturnInstruction();

        ret.setValue(method.getSymbols().getSymbol(value, ret));

        addInstruction(ret);
    }

    @Override
    public void createSelect(Type type, int condition, int trueValue, int falseValue) {
        SelectInstruction select = new SelectInstruction(type);

        select.setCondition(method.getSymbols().getSymbol(condition, select));
        select.setTrueValue(method.getSymbols().getSymbol(trueValue, select));
        select.setFalseValue(method.getSymbols().getSymbol(falseValue, select));

        addInstruction(select);
    }

    @Override
    public void createShuffleVector(Type type, int vector1, int vector2, int mask) {
        addInstruction(new ShuffleVectorInstruction(type,
                method.getSymbols().getSymbol(vector1),
                method.getSymbols().getSymbol(vector2),
                method.getSymbols().getSymbol(mask)));
    }

    @Override
    public void createStore(int destination, int source, int align, boolean isVolatile) {
        StoreInstruction store = new StoreInstruction(align, isVolatile);

        store.setDestination(method.getSymbols().getSymbol(destination, store));
        store.setSource(method.getSymbols().getSymbol(source, store));

        addInstruction(store);
    }

    @Override
    public void createSwitch(int condition, int defaultBlock, int[] caseValues, int[] caseBlocks) {
        Symbol[] values = new Symbol[caseValues.length];
        Block[] blocks = new Block[caseBlocks.length];

        for (int i = 0; i < values.length; i++) {
            values[i] = method.getSymbols().getSymbol(caseValues[i]);
            blocks[i] = method.getBlock(caseBlocks[i]);
        }

        addInstruction(new SwitchInstruction(
                method.getSymbols().getSymbol(condition),
                method.getBlock(defaultBlock),
                values,
                blocks));
    }

    @Override
    public void createSwitchOld(int condition, int defaultBlock, long[] caseConstants, int[] caseBlocks) {
        Block[] blocks = new Block[caseBlocks.length];

        for (int i = 0; i < blocks.length; i++) {
            blocks[i] = method.getBlock(caseBlocks[i]);
        }

        addInstruction(new SwitchOldInstruction(
                method.getSymbols().getSymbol(condition),
                method.getBlock(defaultBlock),
                caseConstants,
                blocks));
    }

    @Override
    public void createUnreachable() {
        addInstruction(new UnreachableInstruction());
    }

    @Override
    public void enterBlock(long id) {
    }

    @Override
    public void exitBlock() {
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String getName() {
        return name;
    }

    public Instruction getInstruction(int index) {
        return instructions.get(index);
    }

    public int getInstructionCount() {
        return instructions.size();
    }

    @Override
    public Type getType() {
        return MetaType.VOID;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
}
