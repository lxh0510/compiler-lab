package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.*;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cn.edu.hitsz.compiler.asm.Reg.*;


/**
 * TODO: 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {
    private List<Instruction> originInstruction = new ArrayList<>();             // 中间代码列表
    private Map<IRVariable,Reg> regAllocation = new HashMap<>();            // 寄存器分配情况，即与变量对应关系，本程序中仅用到通过变量查找寄存器，因此通过哈希表结构即可实现
    private List<Reg> freeReg = Stream.of(new Reg[]{t0,t1,t2,t3,t4,t5,t6}).collect(Collectors.toList());   // 空闲寄存器列表，存储未被使用的寄存器
    private List<String> asmList = new ArrayList<>(); // 汇编代码列表，用于保存生成的汇编代码

    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        // TODO: 读入前端提供的中间代码并生成所需要的信息
        originInstruction=originInstructions;
    }

    /**
     * 对中间代码进行预处理
     *
     * @param originInstructions 中间代码列表
     */
    public void preSet(List<Instruction> originInstructions) {
        Instruction instruction;
        for(int i=0;i<originInstructions.size();i++){
            instruction = originInstructions.get(i);
            InstructionKind kind = instruction.getKind();
            // 对于两个操作数的指令
            if(kind.isBinary()){
                IRValue lhs = instruction.getLHS();
                IRValue rhs = instruction.getRHS();
                // 将操作两个立即数的指令直接求值得到结果，然后替换成MOV指令
                if(lhs.isImmediate()&&rhs.isImmediate()){
                    int result = 0;
                    switch (kind){
                        case ADD -> result = lhs.getValue()+rhs.getValue();
                        case MUL -> result = lhs.getValue()*rhs.getValue();
                        case SUB -> result = lhs.getValue()-rhs.getValue();
                    }
                    originInstructions.set(i,Instruction.createMov(instruction.getResult(),IRImmediate.of(result)));
                }
                // 对于只有一个立即数的指令
                else if((lhs.isImmediate()||rhs.isImmediate())){
                    // 若不是乘法或左立即数减法，则调整指令格式，将立即数放在指令右侧
                    if((kind!=InstructionKind.MUL)&&!(kind==InstructionKind.SUB&& lhs.isImmediate())){
                        if(lhs.isImmediate()){
                            originInstructions.set(i,Instruction.createAdd(instruction.getResult(),rhs,lhs));
                        }
                    }
                    // 将乘法与左立即数减法指令调整，前插一条MOV指令，将指令转换为无立即数指令
                    else {
                        IRVariable variable = IRVariable.temp();
                        if(kind==InstructionKind.SUB) {
                            originInstructions.add(i,Instruction.createMov(variable,lhs));
                            originInstructions.set(i+1,Instruction.createSub(instruction.getResult(),variable,rhs));
                        }
                        else if(lhs.isImmediate()) {
                            originInstructions.add(i,Instruction.createMov(variable,lhs));
                            originInstructions.set(i+1,Instruction.createMul(instruction.getResult(),variable,rhs));
                        }
                        else if(rhs.isImmediate()) {
                            originInstructions.add(i,Instruction.createMov(variable,rhs));
                            originInstructions.set(i+1,Instruction.createMul(instruction.getResult(),lhs,variable));
                        }
                    }
                }
            }
            // 对于单个操作数的指令，当遇到RET指令舍弃后续指令
            else if(kind.isUnary()){
                if(kind == InstructionKind.RET) {
                    for (int j=i+1;j<originInstructions.size();j++){
                        originInstructions.remove(j);
                    }
                }
            }
        }
        System.out.print(originInstructions);
        System.out.print("\n");
    }

    /**
     * 检查变量在后续代码中是否被使用
     *
     * @param irVariable 待检查的变量
     * @param i   变量处在代码序号
     */
    public boolean isUsed(IRVariable irVariable,int i){
        // 遍历后续代码，检查变量是否在后续代码中使用
        for(int j=i;j<originInstruction.size();j++){
            List<IRValue> operands = originInstruction.get(j).getOperands();
            for(IRValue irValue:operands){
                if(irValue.isIRVariable()){
                    if(irValue.getName().equals(irVariable.getName())){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 为变量分配寄存器
     *
     * @param irVariable 待分配的变量
     * @param i   变量处在代码序号
     */
    public void allocateReg(IRVariable irVariable,int i) {
        // 如果还有空闲寄存器，将变量存入寄存器，为方便，将每次均选择空闲寄存器列表中的第一个
        if (!freeReg.isEmpty()) {
            regAllocation.put(irVariable, freeReg.get(0));
            freeReg.remove(0);
        }
        // 若全部寄存器均被占用
        else {
            // 用find表示是否找到可以夺取的寄存器
            boolean find = false;
            // 遍历已经保存在寄存器中的变量
            for (IRVariable variable : regAllocation.keySet()) {
                // 若变量在后续代码不再使用，则夺取该变量所占的寄存器
                if (!isUsed(variable, i)) {
                    Instruction instruction = originInstruction.get(i);
                    // rhs不能夺取lhs和result占用的寄存器
                    if (instruction.getKind() != InstructionKind.MOV) {
                        if (irVariable.equals(instruction.getRHS())) {
                            if (variable.equals(instruction.getLHS()) || variable.equals(instruction.getResult())) {
                                continue;
                            }
                        }
                    }
                    // 夺取寄存器
                    Reg tempReg = regAllocation.get(variable);
                    regAllocation.remove(variable, tempReg);
                    regAllocation.put(irVariable, tempReg);
                    find = true;
                    break;
                }
            }
            // 寄存器无法安排时，则抛出异常
            if (!find){
                throw new RuntimeException();
            }
        }
    }

    /**
     * 将中间代码转换为汇编代码
     *
     * @param instruction 待转换的中间代码
     */
    private String instructiontoAsm(Instruction instruction){
        InstructionKind instructionKind = instruction.getKind();
        // 先定义两种指令格式，分别有一个操作数或两个操作数，进行代码转换时，只要根据代码种类替换相应部分即可
        String asm1 = "kind %rd, %rs1";
        String asm2 = "kind %rd, %rs1 ,%rs2";
        // 根据代码种类进行代码转换
        switch (instructionKind) {
            // MOV类型语句可能生成两种汇编代码
            case MOV -> {
                // 若From为立即数，则生成li类型的汇编代码
                if (instruction.getFrom().isImmediate()) {
                    String movasm = asm1.replaceAll("kind", "li");
                    movasm = movasm.replaceAll("%rd",regAllocation.get(instruction.getResult()).toString());
                    return movasm.replaceAll("%rs1", "" + instruction.getFrom().getValue());
                // 若From为变量。则生成mv类型的汇编代码
                } else {
                    String movasm = asm1.replaceAll("kind", "mv").replaceAll("%rd",regAllocation.get(instruction.getResult()).toString());
                    return movasm.replaceAll("%rs1", regAllocation.get((IRVariable) instruction.getFrom()).toString());
                }
            }
            // ADD类型根据rhs是否为立即数生成add或addi两种类型的汇编代码
            case ADD -> {
                String addasm = asm2.replaceAll("%rd", regAllocation.get(instruction.getResult()).toString());
                addasm = addasm.replaceAll("%rs1",regAllocation.get((IRVariable) instruction.getLHS()).toString());
                if (instruction.getRHS().isImmediate()) {
                    return addasm.replaceAll("kind", "addi").replaceAll("%rs2", "" + instruction.getRHS().getValue());
                }
                else {
                    return addasm.replaceAll("kind", "add").replaceAll("%rs2", regAllocation.get((IRVariable) instruction.getRHS()).toString());
                }
            }
            // SUB需判断右侧变量是否为立即数，分别进行对应的转换
            case SUB -> {
                String subasm = asm2.replaceAll("kind",  "sub").replaceAll("%rs1", regAllocation.get((IRVariable) instruction.getLHS()).toString());
                subasm =  subasm.replaceAll("%rd", regAllocation.get(instruction.getResult()).toString());
                if(instruction.getRHS().isImmediate()){
                    return subasm.replaceAll("%rs2","" + instruction.getRHS().getValue());
                }
                else {
                    return subasm.replaceAll("%rs2",regAllocation.get((IRVariable) instruction.getRHS()).toString());
                }
            }
            // MUL进行对应位置的转换即可
            case MUL -> {
                String mulasm = asm2.replaceAll("kind", "mul").replaceAll("%rs1", regAllocation.get((IRVariable) instruction.getLHS()).toString());
                return mulasm.replaceAll("%rd", regAllocation.get(instruction.getResult()).toString()).replaceAll("%rs2",regAllocation.get((IRVariable) instruction.getRHS()).toString());

            }
            // RET应将result的返回寄存器设置为a0
            case RET -> {
                String retasm = asm1.replaceAll("kind","mv").replaceAll("%rd", a0.toString());
                return retasm.replaceAll("%rs1",regAllocation.get((IRVariable)instruction.getReturnValue()).toString());
            }
            default -> {
                return null;
            }
        }
    }

    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        // TODO: 执行寄存器分配与代码生成
        // 对中间代码进行预处理
        preSet(originInstruction);
        // 遍历每个中间代码
        for(Instruction instruction:originInstruction){
            switch (instruction.getKind()){
                // 对于三地址语句
                case SUB , MUL ,ADD->{
                    // 为result选择寄存器
                    allocateReg(instruction.getResult(),originInstruction.indexOf(instruction));
                    List<IRValue> operands = instruction.getOperands();
                    // 对于左右操作数
                    for(IRValue irValue:operands){
                        if(irValue.isIRVariable()){
                            IRVariable irVariable = (IRVariable) irValue;
                            // 若不再寄存器中，为其选择寄存器
                            if(!regAllocation.containsKey(irVariable)){
                                allocateReg(irVariable,originInstruction.indexOf(instruction));
                            }
                        }
                    }
                    // 生成汇编语句
                    asmList.add(instructiontoAsm(instruction));
                }
                case MOV ->{
                    // 为result选择寄存器
                    allocateReg(instruction.getResult(),originInstruction.indexOf(instruction));
                    // 生成汇编语句
                    asmList.add(instructiontoAsm(instruction));
                }
                case RET -> {
                    // 生成汇编语句
                    asmList.add(instructiontoAsm(instruction));
                    System.out.print(asmList);
                    return;
                }
            }

        }
    }


    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) throws IOException {
        // TODO: 输出汇编代码到文件
        FileUtils.writeFile(path, ".text\n " + String.join("\n ", asmList));
    }
}

