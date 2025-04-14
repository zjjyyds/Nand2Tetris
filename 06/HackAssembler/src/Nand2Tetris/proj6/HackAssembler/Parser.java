package Nand2Tetris.proj6.HackAssembler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Parser {
    // 用于读取输入文件的缓冲读取器
    private BufferedReader reader;
    // 当前命令（已经解析并清理后的命令）
    private String currentCommand;
    // 下一条命令（预读的命令，尚未设为当前命令）
    private String nextCommand;
    // 输入文件的路径（供 Assembler 重置 Parser 时使用）
    private String filePath;
    // 定义命令类型的常量，用于 commandType() 方法的返回值
    public static final String A_COMMAND = "A_COMMAND"; // A 指令，例如 @xxx
    public static final String C_COMMAND = "C_COMMAND"; // C 指令，例如 D=M+1;JGT
    public static final String L_COMMAND = "L_COMMAND"; // L 指令，例如 (LOOP)

    /**
     * 构造函数：初始化 Parser，打开输入文件并准备解析。
     * @param filePath 输入文件的路径（.asm 文件）
     * @throws IOException 如果文件无法打开或读取时抛出异常
     */
    public Parser(String filePath) throws IOException {
        this.filePath = filePath;
        reader = new BufferedReader(new FileReader(filePath)); // 打开文件
        currentCommand = null; // 初始化当前命令为空
        nextCommand = null; // 初始化下一条命令为空
        advanceToNextCommand(); // 预读第一条有效命令，存入 nextCommand
    }
    /**
     * 获取输入文件的路径。
     * @return 文件路径（供 Assembler 重置 Parser 时使用）
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * 检查是否还有更多命令需要处理。
     * @return 如果有更多命令（nextCommand 不为 null），返回 true；否则返回 false
     */
    public boolean hasMoreCommands() {
        return nextCommand != null;
    }

    /**
     * 前进到下一条命令，将 nextCommand 设为 currentCommand，并读取下一条有效命令。
     * @throws IOException 如果文件读取过程中发生错误
     */
    public void advance() throws IOException {
        if (!hasMoreCommands()) {
            return; // 如果没有更多命令，直接返回
        }
        currentCommand = nextCommand; // 将预读的命令设为当前命令
        advanceToNextCommand(); // 读取下一条有效命令
    }

    /**
     * 辅助方法：读取下一条有效命令，忽略空行和注释。
     * 将读取到的有效命令存储在 nextCommand 中。
     * @throws IOException 如果文件读取过程中发生错误
     */
    private void advanceToNextCommand() throws IOException {
        nextCommand = null; // 重置 nextCommand
        String line;
        // 循环读取文件，直到找到有效命令或文件结束
        while ((line = reader.readLine()) != null) {
            line = line.trim(); // 去除首尾空白字符
            if (line.isEmpty()) {
                continue; // 忽略空行
            }
            // 处理行内注释：去除 // 之后的内容
            int commentIndex = line.indexOf("//");
            if (commentIndex != -1) {
                line = line.substring(0, commentIndex).trim();
            }
            if (line.isEmpty()) {
                continue; // 如果去掉注释后是空行，忽略
            }
            nextCommand = line; // 找到有效命令，存储到 nextCommand
            break; // 退出循环
        }
    }
    /**
     * 返回当前命令的类型（A_COMMAND、C_COMMAND 或 L_COMMAND）。
     * @return 命令类型；如果当前命令为空，返回 null
     */
    public String commandType() {
        if (currentCommand == null) {
            return null; // 如果没有当前命令，返回 null
        }
        if (currentCommand.startsWith("@")) {
            return A_COMMAND; // 以 @ 开头的是 A 指令
        } else if (currentCommand.startsWith("(") && currentCommand.endsWith(")")) {
            return L_COMMAND; // 以 ( 开头并以 ) 结尾的是 L 指令
        } else {
            return C_COMMAND; // 其他情况为 C 指令
        }
    }
    /**
     * 对于 A_COMMAND 和 L_COMMAND，返回符号或数值。
     * @return 对于 A_COMMAND，返回 @ 后的符号或数值；
     *         对于 L_COMMAND，返回括号内的标签名；
     *         其他情况返回 null
     */
    public String symbol() {
        String type = commandType();
        if (type.equals(A_COMMAND)) {
            // 去掉 @ 前缀，例如 @123 或 @LOOP 返回 123 或 LOOP
            return currentCommand.substring(1);
        } else if (type.equals(L_COMMAND)) {
            // 去掉括号，例如 (LOOP) 返回 LOOP
            return currentCommand.substring(1, currentCommand.length() - 1);
        }
        return null; // C 指令没有 symbol 字段
    }
    /**
     * 对于 C_COMMAND，返回 dest 字段。
     * @return dest 字段（= 之前的内容）；如果没有 dest，返回空字符串；
     *         如果不是 C 指令，返回 null
     */
    public String dest() {
        if (!commandType().equals(C_COMMAND)) {
            return null; // 仅对 C 指令有效
        }
        // C 指令格式：dest=comp;jump
        int equalsIndex = currentCommand.indexOf("=");
        if (equalsIndex == -1) {
            return ""; // 没有 =，表示无 dest 字段
        }
        return currentCommand.substring(0, equalsIndex); // 返回 = 之前的内容
    }
    /**
     * 对于 C_COMMAND，返回 comp 字段。
     * @return comp 字段（= 和 ; 之间的内容，或仅 comp 的内容）；
     *         如果不是 C 指令，返回 null
     */
    public String comp() {
        if (!commandType().equals(C_COMMAND)) {
            return null; // 仅对 C 指令有效
        }
        // C 指令格式：dest=comp;jump
        int equalsIndex = currentCommand.indexOf("=");
        int semicolonIndex = currentCommand.indexOf(";");
        String compPart;
        if (equalsIndex != -1 && semicolonIndex != -1) {
            // 既有 dest 又有 jump，例如 D=M+1;JGT
            compPart = currentCommand.substring(equalsIndex + 1, semicolonIndex);
        } else if (equalsIndex != -1) {
            // 只有 dest，无 jump，例如 D=M+1
            compPart = currentCommand.substring(equalsIndex + 1);
        } else if (semicolonIndex != -1) {
            // 无 dest，有 jump，例如 M+1;JGT
            compPart = currentCommand.substring(0, semicolonIndex);
        } else {
            // 仅 comp，例如 M+1
            compPart = currentCommand;
        }
        return compPart;
    }

    /**
     * 对于 C_COMMAND，返回 jump 字段。
     * @return jump 字段（; 之后的内容）；如果没有 jump，返回空字符串；
     *         如果不是 C 指令，返回 null
     */
    public String jump() {
        if (!commandType().equals(C_COMMAND)) {
            return null; // 仅对 C 指令有效
        }
        // C 指令格式：dest=comp;jump
        int semicolonIndex = currentCommand.indexOf(";");
        if (semicolonIndex == -1) {
            return ""; // 没有 ;，表示无 jump 字段
        }
        return currentCommand.substring(semicolonIndex + 1); // 返回 ; 之后的内容
    }

    /**
     * 关闭输入文件。
     * @throws IOException 如果关闭文件时发生错误
     */
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }

}
