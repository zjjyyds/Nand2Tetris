// 正确的乘法实现 (R0 × R1)

@R2
M=0       // 初始化结果为0

@R1
D=M
@END
D;JEQ     // 如果R1=0，直接结束

@347
M=D       // counter = R1

(LOOP)
@R0
D=M
@R2
M=D+M     // R2 += R0

@347
MD=M-1    // counter--, D=counter

@LOOP
D;JGT     // 如果counter>0，继续循环

(END)
@END
0;JMP     // 无限循环结束