package VMTranslatorⅡ;

import java.io.*;
import java.util.*;

public class VMTranslator {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java VMTranslator <file.vm or directory>");
            return;
        }

        File input = new File(args[0]);
        File output;
        List<File> vmFiles = new ArrayList<>();

        // 检查输入是文件还是目录
        if (input.isDirectory()) {
            output = new File(input.getAbsolutePath() + "/" + input.getName() + ".asm");
            for (File file : input.listFiles()) {
                if (file.getName().endsWith(".vm")) {
                    vmFiles.add(file);
                }
            }
        } else if (input.getName().endsWith(".vm")) {
            vmFiles.add(input);
            output = new File(input.getAbsolutePath().replace(".vm", ".asm"));
        } else {
            System.out.println("Input must be a .vm file or a directory containing .vm files");
            return;
        }

        // 检查 vmFiles 是否为空
        if (vmFiles.isEmpty()) {
            System.out.println("No .vm files found in the input.");
            return;
        }

        // 验证输出文件路径
        try {
            if (!output.getParentFile().exists()) {
                output.getParentFile().mkdirs(); // 创建父目录
            }
            if (!output.createNewFile() && !output.canWrite()) {
                System.out.println("Cannot write to output file: " + output.getAbsolutePath());
                return;
            }
        } catch (IOException e) {
            System.out.println("Error creating output file: " + e.getMessage());
            return;
        }

        // 使用 try-with-resources 确保 CodeWriter 正确关闭
        try (CodeWriter codeWriter = new CodeWriter(output)) {
            // 如果是多文件程序，写入引导代码
            if (vmFiles.size() > 1) {
                codeWriter.writeBootstrap();
            }

            // 翻译每个 .vm 文件
            for (File vmFile : vmFiles) {
                Parser parser = new Parser(vmFile);
                codeWriter.setFileName(vmFile.getName());
                while (parser.hasMoreCommands()) {
                    parser.advance();
                    switch (parser.commandType()) {
                        case C_ARITHMETIC:
                            codeWriter.writeArithmetic(parser.arg1());
                            break;
                        case C_PUSH:
                        case C_POP:
                            codeWriter.writePushPop(parser.commandType(), parser.arg1(), parser.arg2());
                            break;
                        case C_LABEL:
                            codeWriter.writeLabel(parser.arg1());
                            break;
                        case C_GOTO:
                            codeWriter.writeGoto(parser.arg1());
                            break;
                        case C_IF:
                            codeWriter.writeIf(parser.arg1());
                            break;
                        case C_FUNCTION:
                            codeWriter.writeFunction(parser.arg1(), parser.arg2());
                            break;
                        case C_CALL:
                            codeWriter.writeCall(parser.arg1(), parser.arg2());
                            break;
                        case C_RETURN:
                            codeWriter.writeReturn();
                            break;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error during translation: " + e.getMessage());
        }
    }
}