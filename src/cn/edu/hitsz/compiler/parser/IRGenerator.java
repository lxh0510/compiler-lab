package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

// TODO: 实验三: 实现 IR 生成

/**
 *
 */
public class IRGenerator implements ActionObserver {
    // 设立符号栈
    private Stack<Token> tokenStack = new Stack<>();
    private SymbolTable symbolTable = null;
    // 用instruction列表存放生成的指令
    private List<Instruction> instructions = new ArrayList<>();
    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO
        // 将符号入栈
        tokenStack.add(currentToken);
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // 获取产生式编号
        int index = production.index();
        // 根据产生式进行相应操作
        switch (index) {
            case (6) -> {
                //S -> id = E;
                // 将产生式右侧符号出栈
                Token tok1 = tokenStack.pop();
                tokenStack.pop();
                Token tok2 = tokenStack.pop();
                // 获得等式右侧符号的值
                IRValue irValue = getFrom(tok1);
                // 生成指令添加到列表中
                instructions.add(Instruction.createMov(IRVariable.named(tok2.getText()), irValue));
            }
            case (7) -> {
                //S -> return E;
                // 将产生式右侧符号出栈
                Token tok1 = tokenStack.pop();
                tokenStack.pop();
                // 获得return右侧符号的值
                IRValue irValue = getFrom(tok1);
                // 生成指令添加到列表中
                instructions.add(Instruction.createRet(irValue));
            }
            case (8) -> {
                //E -> E + A;
                // 将产生式右侧符号出栈
                Token tok1 = tokenStack.pop();
                tokenStack.pop();
                Token tok2 = tokenStack.pop();
                // 获得两个加数符号的值
                IRValue irValue1 = getFrom(tok1);
                IRValue irValue2 = getFrom(tok2);
                // 生成等式左侧的临时变量
                IRVariable temp = IRVariable.temp();
                // 生成指令添加到列表中
                instructions.add(Instruction.createAdd(temp,irValue2,irValue1));
                // 将生成的临时变量压入符号栈中
                tokenStack.add(Token.normal("id", temp.getName()));
            }
            case (9) -> {
                //E -> E - A;
                // 与产生式8同理
                Token tok1 = tokenStack.pop();
                tokenStack.pop();
                Token tok2 = tokenStack.pop();
                IRValue irValue1 = getFrom(tok1);
                IRValue irValue2 = getFrom(tok2);
                IRVariable temp = IRVariable.temp();
                instructions.add(Instruction.createSub(temp,irValue2,irValue1));
                tokenStack.add(Token.normal("id",temp.getName()));
            }
            case (10) -> {
                //E -> A;
            }
            case (11) -> {
                //A -> A * B;
                // 与产生式8同理
                Token tok1 = tokenStack.pop();
                tokenStack.pop();
                Token tok2 = tokenStack.pop();
                IRValue irValue1 = getFrom(tok1);
                IRValue irValue2 = getFrom(tok2);
                IRVariable temp = IRVariable.temp();
                instructions.add(Instruction.createMul(temp,irValue2,irValue1));
                tokenStack.add(Token.normal("id",temp.getName()));
            }
            case (12) -> {
                //A -> B;
            }
            case (13) -> {
                //B -> ( E );
                // 将两个括号出栈
                tokenStack.pop();
                Token tok = tokenStack.pop();
                tokenStack.pop();
                tokenStack.add(tok);
            }
            case (14) -> {
                //B -> id;
            }
            case (15) -> {
                // B -> IntConst;
            }
            default -> {
            }
        }

    }


    @Override
    public void whenAccept(Status currentStatus) {
        // TODO
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO
       symbolTable=table;
    }

    public List<Instruction> getIR() {
        // TODO
        // 返回生成的指令列表
        return instructions;
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }

    // 该函数用于根据符号token获取它的值irvalue
    public IRValue getFrom(Token token){
        IRValue irValue = null;
        // 根据token的类型获取它的值或者它的名称
        if("IntConst".equals(token.getKindId())){
            irValue = IRImmediate.of(Integer.parseInt(token.getText()));
        }
        else if("id".equals(token.getKindId())){
            irValue = IRVariable.named(token.getText());
        }
        return irValue;
    }
}


