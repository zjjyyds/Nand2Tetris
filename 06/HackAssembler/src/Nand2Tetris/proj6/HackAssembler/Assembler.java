import Nand2Tetris.proj6.HackAssembler.Code;
import Nand2Tetris.proj6.HackAssembler.Parser;
import Nand2Tetris.proj6.HackAssembler.SymbolTable;

import java.io.*;
import java.util.ArrayList;

/**
 * Assembler 类：Hack 汇编器的主类，负责将 .asm 文件翻译为 .hack 文件。
 * 整合 Parser、SymbolTable 和 Code 类，完成汇编过程。
 * 实现两次扫描：
 * 1. 第一次扫描：构建符号表，处理 L_COMMAND（标签）。
 * 2. 第二次扫描：翻译 A_COMMAND 和 C_COMMAND，生成二进制代码。
 */
public class Assembler {
    // 用于解析 .asm 文件的 Parser 对象
    private Parser parser;
    // 用于管理符号和地址的 SymbolTable 对象
    private SymbolTable symbolTable;
    // 用于将 C 指令字段翻译为二进制的 Code 对象
    private Code code;
    // 用于写入 .hack 文件的输出流
    private PrintWriter writer;

    /**
     * 构造函数：初始化 Assembler，创建所需的组件。
     * @param inputFile 输入的 .asm 文件路径
     * @throws IOException 如果文件无法打开或写入时抛出异常
     */
    public Assembler(String inputFile) throws IOException {
        parser = new Parser(inputFile); // 初始化 Parser，读取输入文件
        symbolTable = new SymbolTable(); // 初始化符号表，包含预定义符号
        code = new Code(); // 初始化 Code，准备翻译 C 指令
        // 构造输出文件名：将 .asm 替换为 .hack
        String outputFile = inputFile.replace(".asm", ".hack");
        // 使用 BufferedWriter 包装 FileWriter，确保控制换行符
        writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
    }

    /**
     * 执行汇编过程：完成两次扫描，生成 .hack 文件。
     * 修复：确保最后一行不以 \n 结尾。
     * @throws IOException 如果文件读取或写入过程中发生错误
     */
    public void assemble() throws IOException {
        // 第一次扫描：构建符号表，处理 L_COMMAND（标签）
        int romAddress = 0; // ROM 地址计数器，记录指令的地址
        while (parser.hasMoreCommands()) {
            parser.advance(); // 前进到下一条命令
            String commandType = parser.commandType();
            if (commandType == null) {
                continue; // 如果命令类型为空（可能是 Parser 的 bug），跳过
            }
            if (commandType.equals(Parser.L_COMMAND)) {
                // 遇到 L_COMMAND，将标签添加到符号表
                String symbol = parser.symbol();
                symbolTable.addEntry(symbol, romAddress);
            } else if (commandType.equals(Parser.A_COMMAND) || commandType.equals(Parser.C_COMMAND)) {
                // 遇到 A_COMMAND 或 C_COMMAND，增加 ROM 地址计数器
                romAddress++;
            }
            // L_COMMAND 不占用 ROM 地址，仅用于标记
        }

        // 重置 Parser，准备第二次扫描
        parser = new Parser(parser.getFilePath());

        // 第二次扫描：翻译 A_COMMAND 和 C_COMMAND，生成二进制代码
        // 使用 ArrayList 存储所有二进制代码行
        ArrayList<String> binaryLines = new ArrayList<>();
        while (parser.hasMoreCommands()) {
            parser.advance(); // 前进到下一条命令
            String commandType = parser.commandType();
            // 确保 commandType 不为 null（避免空命令）
            if (commandType == null) {
                continue; // 跳过无效命令
            }

            if (commandType.equals(Parser.A_COMMAND)) {
                // 处理 A 指令：@xxx，翻译为 16 位二进制地址
                String symbol = parser.symbol();
                if (symbol == null || symbol.trim().isEmpty()) {
                    continue; // 如果 symbol 为空，跳过
                }
                int address;
                if (symbol.matches("\\d+")) {
                    // 如果是数字（例如 @123），直接转换为整数
                    address = Integer.parseInt(symbol);
                } else {
                    // 如果是符号（例如 @LOOP 或 @i）
                    if (!symbolTable.contains(symbol)) {
                        // 如果符号不存在（变量），分配新地址
                        address = symbolTable.allocateVariableAddress(symbol);
                    } else {
                        // 如果符号已存在（标签或预定义符号），获取地址
                        address = symbolTable.getAddress(symbol);
                    }
                }
                // 将地址转换为 16 位二进制字符串（高位补 0）
                String binary = String.format("%16s", Integer.toBinaryString(address)).replace(' ', '0');
                binaryLines.add(binary); // 添加到列表
            } else if (commandType.equals(Parser.C_COMMAND)) {
                // 处理 C 指令：dest=comp;jump，翻译为 16 位二进制
                // 格式：111 + comp(7 位) + dest(3 位) + jump(3 位)
                String comp = parser.comp();
                if (comp == null || comp.trim().isEmpty()) {
                    continue; // 如果 comp 为空，跳过
                }
                String compBinary = code.comp(comp); // 获取 comp 的 7 位二进制
                String destBinary = code.dest(parser.dest()); // 获取 dest 的 3 位二进制
                String jumpBinary = code.jump(parser.jump()); // 获取 jump 的 3 位二进制
                String binary = "111" + compBinary + destBinary + jumpBinary;
                binaryLines.add(binary); // 添加到列表
            }
            // L_COMMAND 在第一次扫描中已处理，此处忽略
        }

        // 写入文件：确保最后一行不以 \n 结尾
        for (int i = 0; i < binaryLines.size(); i++) {
            String binary = binaryLines.get(i);
            if (i == binaryLines.size() - 1) {
                // 最后一行，不添加 \n
                writer.print(binary);
            } else {
                // 其他行，添加 \n
                writer.print(binary + "\n");
            }
        }

        // 关闭输出文件
        writer.close();
        // 关闭 Parser 的输入文件
        parser.close();
    }

    /**
     * 主方法：命令行入口，接受 .asm 文件路径，执行汇编。
     * @param args 命令行参数，args[0] 为输入文件路径
     * @throws IOException 如果文件处理过程中发生错误
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            // 检查命令行参数是否正确
            System.out.println("用法: java Assembler <input.asm>");
            return;
        }
        Assembler assembler = new Assembler(args[0]); // 创建 Assembler 实例
        assembler.assemble(); // 执行汇编
    }
}