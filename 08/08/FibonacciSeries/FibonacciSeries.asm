// C_PUSH argument 1
@ARG
D=M
@1
A=D+A
D=M
@SP
A=M
M=D
@SP
M=M+1
// C_POP pointer 1
@SP
AM=M-1
D=M
@THAT
M=D
// C_PUSH constant 0
@0
D=A
@SP
A=M
M=D
@SP
M=M+1
// C_POP that 0
@THAT
D=M
@0
D=D+A
@R13
M=D
@SP
AM=M-1
D=M
@R13
A=M
M=D
// C_PUSH constant 1
@1
D=A
@SP
A=M
M=D
@SP
M=M+1
// C_POP that 1
@THAT
D=M
@1
D=D+A
@R13
M=D
@SP
AM=M-1
D=M
@R13
A=M
M=D
// C_PUSH argument 0
@ARG
D=M
@0
A=D+A
D=M
@SP
A=M
M=D
@SP
M=M+1
// C_PUSH constant 2
@2
D=A
@SP
A=M
M=D
@SP
M=M+1
// sub
@SP
AM=M-1
D=M
A=A-1
M=M-D
// C_POP argument 0
@ARG
D=M
@0
D=D+A
@R13
M=D
@SP
AM=M-1
D=M
@R13
A=M
M=D
// label LOOP
($LOOP)
// C_PUSH argument 0
@ARG
D=M
@0
A=D+A
D=M
@SP
A=M
M=D
@SP
M=M+1
// if-goto COMPUTE_ELEMENT
@SP
AM=M-1
D=M
@$COMPUTE_ELEMENT
D;JNE
// goto END
@$END
0;JMP
// label COMPUTE_ELEMENT
($COMPUTE_ELEMENT)
// C_PUSH that 0
@THAT
D=M
@0
A=D+A
D=M
@SP
A=M
M=D
@SP
M=M+1
// C_PUSH that 1
@THAT
D=M
@1
A=D+A
D=M
@SP
A=M
M=D
@SP
M=M+1
// add
@SP
AM=M-1
D=M
A=A-1
M=M+D
// C_POP that 2
@THAT
D=M
@2
D=D+A
@R13
M=D
@SP
AM=M-1
D=M
@R13
A=M
M=D
// C_PUSH pointer 1
@THAT
D=M
@SP
A=M
M=D
@SP
M=M+1
// C_PUSH constant 1
@1
D=A
@SP
A=M
M=D
@SP
M=M+1
// add
@SP
AM=M-1
D=M
A=A-1
M=M+D
// C_POP pointer 1
@SP
AM=M-1
D=M
@THAT
M=D
// C_PUSH argument 0
@ARG
D=M
@0
A=D+A
D=M
@SP
A=M
M=D
@SP
M=M+1
// C_PUSH constant 1
@1
D=A
@SP
A=M
M=D
@SP
M=M+1
// sub
@SP
AM=M-1
D=M
A=A-1
M=M-D
// C_POP argument 0
@ARG
D=M
@0
D=D+A
@R13
M=D
@SP
AM=M-1
D=M
@R13
A=M
M=D
// goto LOOP
@$LOOP
0;JMP
// label END
($END)
