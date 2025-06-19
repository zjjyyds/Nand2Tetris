package VMTranslator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Parser {
    private BufferedReader reader;
    private String currentCommand;
    private String[] commandParts;

    // 命令类型枚举
    public enum CommandType {
        C_ARITHMETIC, C_PUSH, C_POP, C_LABEL, C_GOTO, C_IF, C_FUNCTION, C_RETURN, C_CALL
    }

    public Parser(String inputFile) throws IOException {
        reader = new BufferedReader(new FileReader(inputFile));
        currentCommand = null;
    }

    // 检查是否还有更多命令
    public boolean hasMoreCommands() throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            // 忽略空行和注释
            if (line.isEmpty() || line.startsWith("//")) {
                continue;
            }
            // 去除行内注释
            int commentIndex = line.indexOf("//");
            if (commentIndex != -1) {
                line = line.substring(0, commentIndex).trim();
            }
            if (!line.isEmpty()) {
                currentCommand = line;
                commandParts = currentCommand.split("\\s+");
                return true;
            }
        }
        return false;
    }

    // 读取下一条命令
    public void advance() {
        // 当前命令已在 hasMoreCommands 中读取
    }

    // 返回当前命令的类型
    public CommandType commandType() {
        String cmd = commandParts[0];
        if (cmd.equals("push")) return CommandType.C_PUSH;
        if (cmd.equals("pop")) return CommandType.C_POP;
        if (cmd.equals("label")) return CommandType.C_LABEL;
        if (cmd.equals("goto")) return CommandType.C_GOTO;
        if (cmd.equals("if-goto")) return CommandType.C_IF;
        if (cmd.equals("function")) return CommandType.C_FUNCTION;
        if (cmd.equals("return")) return CommandType.C_RETURN;
        if (cmd.equals("call")) return CommandType.C_CALL;
        return CommandType.C_ARITHMETIC; // 算术命令如 add, sub 等
    }

    // 返回第一个参数（算术命令本身或内存段）
    public String arg1() {
        CommandType type = commandType();
        if (type == CommandType.C_ARITHMETIC || type == CommandType.C_RETURN) {
            return commandParts[0];
        }
        return commandParts[1];
    }

    // 返回第二个参数（索引）
    public int arg2() {
        CommandType type = commandType();
        if (type == CommandType.C_PUSH || type == CommandType.C_POP ||
                type == CommandType.C_FUNCTION || type == CommandType.C_CALL) {
            return Integer.parseInt(commandParts[2]);
        }
        throw new IllegalStateException("arg2() called on invalid command type");
    }

    // 关闭文件
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }
}