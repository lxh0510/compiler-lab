package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * TODO: 实验一: 实现词法分析
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    //符号表
    private final SymbolTable symbolTable;
    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }
    //词法分析器需要分析的内容
    private String content;
    //需要得到的tokens列表
    private final List<Token> tokens = new ArrayList<>();

    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) throws IOException {
        // TODO: 词法分析前的缓冲区实现
        // 可自由实现各类缓冲区
        // 或直接采用完整读入方法
        StringBuilder stringBuilder = new StringBuilder();
        String tempString;
        FileReader fileReader = new FileReader(path);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        //读取文件每一行完整内容
        while ((tempString = bufferedReader.readLine())!=null){
            stringBuilder.append(tempString);
        }
        bufferedReader.close();
        //读取的内容保存到字符串content中
        content = stringBuilder.toString();
        System.out.println(content);
        //throw new NotImplementedException();
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        // TODO: 自动机实现的词法分析过程
        char c;
        int i=0;
        while(i<content.length()){
            c=content.charAt(i);
            //读取过程忽略空格与换行
            while(c=='\t'||c=='\n'||c=='\0'){
                i++;
                c=content.charAt(i);
            }
            //如果待读取的字符为字母
            if(Character.isLetter(c)){
                //如果接连三个字符为"int"，则获取int类型
                if(c=='i'&&content.charAt(i+1)=='n'&&content.charAt(i+2)=='t'){
                    //若符号表中没有该类型则需要添加进去
                    if(!symbolTable.has("int")){
                        symbolTable.add("int");
                    }
                    i=i+3;
                    //tokens列表加入int类型
                    tokens.add(Token.simple("int"));
                }
                //如果接连六个字符为"return"，则获取return类型
                else if(c=='r'&&content.charAt(i+1)=='e'&&content.charAt(i+2)=='t'&&content.charAt(i+3)=='u'&&content.charAt(i+4)=='r'&&content.charAt(i+5)=='n'){
                    //若符号表中没有该类型则需要添加进去
                    if(!symbolTable.has("return")){
                        symbolTable.add("return");
                    }
                    i=i+6;
                    //tokens列表加入int类型
                    tokens.add(Token.simple("return"));
                }
                //其余情况说明读取的类型为id
                else{
                    //获取id内容
                    StringBuilder id = new StringBuilder();
                    while(Character.isLetter(c)||Character.isDigit(c)){
                        id.append(c);
                        i++;
                        c = content.charAt(i);
                    }
                    //若符号表中没有该类型则需要添加进去
                    if(!symbolTable.has(id.toString())){
                        symbolTable.add(id.toString());
                    }
                    //tokens列表加入id类型
                    tokens.add(Token.normal("id", id.toString()));
                }
            }
            //如果待读取的字符为数字，说明读取的类型为IntConst
            else if(Character.isDigit(c)){
                //获取IntConst内容
                StringBuilder num = new StringBuilder();
                while(Character.isDigit(c)){
                    num.append(c);
                    i++;
                    c = content.charAt(i);
                }
                //若符号表中没有该类型则需要添加进去
                if(!symbolTable.has("IntConst")){
                    symbolTable.add("IntConst");
                }
                //tokens列表加入IntConst类型
                tokens.add(Token.normal("IntConst", num.toString()));
            }
            //其余则为各种符号类型
            else{
                switch (c) {
                    //对每一种符号，先将其加入至tokens列表，再判断符号表是否包含该符号，若不包含则添加
                    case '*':
                        tokens.add(Token.simple("*"));
                        if(!symbolTable.has("*")){
                            symbolTable.add("*");
                        }
                        break;
                    case '=':
                        tokens.add(Token.simple("="));
                        if(!symbolTable.has("=")){
                            symbolTable.add("=");
                        }
                        break;
                    case ',':
                        tokens.add(Token.simple(","));
                        if(!symbolTable.has(",")){
                            symbolTable.add(",");
                        }
                        break;
                    case ';':
                        tokens.add(Token.simple("Semicolon"));
                        if(!symbolTable.has("Semicolon")){
                            symbolTable.add("Semicolon");
                        }
                        break;
                    case '+':
                        tokens.add(Token.simple("+"));
                        if(!symbolTable.has("+")){
                            symbolTable.add("+");
                        }
                        break;
                    case '-':
                        tokens.add(Token.simple("-"));
                        if(!symbolTable.has("-")){
                            symbolTable.add("-");
                        }
                        break;
                    case '/':
                        tokens.add(Token.simple("/"));
                        if(!symbolTable.has("/")){
                            symbolTable.add("/");
                        }
                        break;
                    case '(':
                        tokens.add(Token.simple("("));
                        if(!symbolTable.has("(")){
                            symbolTable.add("(");
                        }
                        break;
                    case ')':
                        tokens.add(Token.simple(")"));
                        if(!symbolTable.has(")")){
                            symbolTable.add(")");
                        }
                        break;
                    default:break;
                }
                i++;
            }
        }
        tokens.add(Token.eof());
        System.out.println(tokens);
    }

    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        // TODO: 从词法分析过程中获取 Token 列表
        // 词法分析过程可以使用 Stream 或 Iterator 实现按需分析
        // 亦可以直接分析完整个文件
        // 总之实现过程能转化为一列表即可
        return tokens;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
            path,
            StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }


}
