# Intel 8085 Virtual Emulator

This is an open-source project that aims to simulate the functionality of the Intel 8085 microprocessor in a software environment. The program is written in Java and provides a simple interface for users to interact with the virtual machine.

This program provides exact functionality using the original architecture of an actual Intel 8085, and can provide every use-case.

The working for this project is simple, it consists of only a single file and it will work on any device with java installed in it.
For some reasons in this version i couldn't make up the parser and its corresponding instruction-cycle for this project but you can run it directly using machine code.


Steps to run a basic Program.
(Example) -> To add two numbers
Note: better of use single instruction at a time to get flawless results.

m
3000
3A 00 20  ; LDA 2000H 
47        ; MOV B, A
3A 01 20  ; LDA 2001H 
80        ; ADD B
32 02 20  ; STA 2002H 
76        ; HLT
// initialize
m
2000

val1 (input)
val2 (input)

//run
G
3000
$
M
2002