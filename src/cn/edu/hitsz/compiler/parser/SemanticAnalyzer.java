package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Action;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.parser.table.Term;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.symtab.SymbolTableEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

// TODO: 实验三: 实现语义分析
public class SemanticAnalyzer implements ActionObserver {
    private SymbolTable symbolTable = null;
    // 创建类型栈和符号栈
    private Stack<SourceCodeType> typeStack = new Stack<>();
    private Stack<Token> tokenStack = new Stack<>();

    @Override
    public void whenAccept(Status currentStatus) {
        // TODO: 该过程在遇到 Accept 时要采取的代码动作
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO: 该过程在遇到 reduce production 时要采取的代码动作
        // 获取产生式编号
        int index = production.index();
        // 出栈产生式右侧符号
        Token token = tokenStack.pop();
        // 根据产生式进行相应操作
        switch (index) {
            case (4) -> {
                //S -> D id;
                // 更新符号表中相应变量的type信息
                if (symbolTable.has(token.getText())) {
                    SymbolTableEntry symbolTableEntry = symbolTable.get(token.getText());
                    symbolTableEntry.setType(typeStack.pop());
                }
                tokenStack.pop();
                // 压入空记录占位
                tokenStack.add(null);
            }
            // D -> int;
            // 将符号的type属性入栈
            case (5) -> {
                typeStack.add(SourceCodeType.Int);
                tokenStack.add(token);
            }
            // 其余情况压入空记录占位
            default -> {
                tokenStack.add(null);
            }
        }

    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO: 该过程在遇到 shift 时要采取的代码动作
        // 将符号入栈
        tokenStack.add(currentToken);
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO: 设计你可能需要的符号表存储结构
        // 如果需要使用符号表的话, 可以将它或者它的一部分信息存起来, 比如使用一个成员变量存储
        symbolTable=table;
    }



}


