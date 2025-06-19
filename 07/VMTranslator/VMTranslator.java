package VMTranslator;

import java.io.File;
import java.io.IOException;

public class VMTranslator {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java VMTranslator <input.vm>");
            System.exit(1);
        }

        String inputFile = args[0];
        String outputFile = inputFile.substring(0, inputFile.lastIndexOf('.')) + ".asm";

        try {
            Parser parser = new Parser(inputFile);
            CodeWriter codeWriter = new CodeWriter(outputFile);

            // 设置文件名（用于 static 段）
            codeWriter.setFileName(new File(inputFile).getName());

            // 逐行解析并翻译
            while (parser.hasMoreCommands()) {
                parser.advance();
                Parser.CommandType commandType = parser.commandType();

                if (commandType == Parser.CommandType.C_ARITHMETIC) {
                    codeWriter.writeArithmetic(parser.arg1());
                } else if (commandType == Parser.CommandType.C_PUSH || commandType == Parser.CommandType.C_POP) {
                    codeWriter.writePushPop(commandType, parser.arg1(), parser.arg2());
                }
                // Project 7 不需要处理 label, goto, if, function, return, call
            }

            // 关闭文件
            parser.close();
            codeWriter.close();

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}