package VMTranslatorⅡ;

import java.io.*;

public class Parser {
    private BufferedReader reader;
    private String currentCommand;
    private CommandType commandType;
    private String arg1;
    private int arg2;

    public enum CommandType {
        C_ARITHMETIC, C_PUSH, C_POP, C_LABEL, C_GOTO, C_IF, C_FUNCTION, C_RETURN, C_CALL
    }

    public Parser(File file) throws IOException {
        reader = new BufferedReader(new FileReader(file));
        currentCommand = null;
    }

    public boolean hasMoreCommands() throws IOException {
        return reader.ready();
    }

    public void advance() throws IOException {
        currentCommand = null;
        while (reader.ready()) {
            String line = reader.readLine().trim();
            if (line.isEmpty() || line.startsWith("//")) {
                continue;
            }
            currentCommand = line.split("//")[0].trim();
            break;
        }
        parseCommand();
    }

    private void parseCommand() {
        if (currentCommand == null) return;
        String[] parts = currentCommand.split("\\s+");
        String cmd = parts[0].toLowerCase();

        if (cmd.equals("add") || cmd.equals("sub") || cmd.equals("neg") ||
                cmd.equals("eq") || cmd.equals("gt") || cmd.equals("lt") ||
                cmd.equals("and") || cmd.equals("or") || cmd.equals("not")) {
            commandType = CommandType.C_ARITHMETIC;
            arg1 = cmd;
        } else if (cmd.equals("push")) {
            commandType = CommandType.C_PUSH;
            arg1 = parts[1];
            arg2 = Integer.parseInt(parts[2]);
        } else if (cmd.equals("pop")) {
            commandType = CommandType.C_POP;
            arg1 = parts[1];
            arg2 = Integer.parseInt(parts[2]);
        } else if (cmd.equals("label")) {
            commandType = CommandType.C_LABEL;
            arg1 = parts[1];
        } else if (cmd.equals("goto")) {
            commandType = CommandType.C_GOTO;
            arg1 = parts[1];
        } else if (cmd.equals("if-goto")) {
            commandType = CommandType.C_IF;
            arg1 = parts[1];
        } else if (cmd.equals("function")) {
            commandType = CommandType.C_FUNCTION;
            arg1 = parts[1];
            arg2 = Integer.parseInt(parts[2]);
        } else if (cmd.equals("call")) {
            commandType = CommandType.C_CALL;
            arg1 = parts[1];
            arg2 = Integer.parseInt(parts[2]);
        } else if (cmd.equals("return")) {
            commandType = CommandType.C_RETURN;
        }
    }

    public CommandType commandType() {
        return commandType;
    }

    public String arg1() {
        return arg1;
    }

    public int arg2() {
        return arg2;
    }
}