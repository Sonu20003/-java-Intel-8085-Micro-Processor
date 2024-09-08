import java.io.*;
import java.util.Arrays;
import java.util.Scanner;

public class Microprocessor8085 {


    static short SP = (short)65535; // stack pointer pre-defined value
    static short[] reg = new short[7];
    static short[] memory = new short[65536];
    //static short M;
    static String F = "00000000";

    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    static int binToDec(String binary) {
        return Integer.parseInt(binary, 2);
    }

    static String decToBin(int decimal) {
        return Integer.toBinaryString(decimal);
    }

    static int hexToDec(String hex) {
        return Integer.parseInt(hex, 16);
    }

    static short stPop() {
        return memory[SP++];
    }

    static void stPush(short val) {
        memory[--SP] = val;
    }

    static String decToHex(int dec) {
        String val = Integer.toHexString(dec);
        if(val.length() == 1) val = "0"+val;
        return val.toUpperCase();
    }

    static short mem(String loc) {
        int decloc = hexToDec(loc);
        return memory[decloc];
    }

    static short get_reg(char r) {
        return switch (r) {
            case 'A' -> reg[0];
            case 'B' -> reg[1];
            case 'C' -> reg[2];
            case 'D' -> reg[3];
            case 'E' -> reg[4];
            case 'H' -> reg[5];
            case 'L' -> reg[6];
            case 'F' -> (short) binToDec(F);
            default -> throw new IllegalArgumentException("Invalid register: " + r);
        };
    }
    static String getPointer() {
        // returns the address of HL pointer
        return decToHex(get_reg('H')) + decToHex(get_reg('L'));
    }

    static short get_M() {
        String s1 = decToHex(get_reg('H'));
        String s2 = decToHex(get_reg('L'));
        String HLpointer = s1 + s2;
        return mem(HLpointer);
    }

    static void put_M(short val) {
        // adds the given value to the memory at HL;
        memory[hexToDec(getPointer())] = val;
    }
//!!
    static String increaseHexByOne(String hex) {
        //if(hex.equals("$")) return hex;
        int dec = Integer.parseInt(hex, 16);
        dec += 1;
        StringBuilder result = new StringBuilder();
        while (dec != 0) {
            int remainder = dec % 16;
            if (remainder < 10) {
                result.insert(0, (char) (remainder + 48));
            } else {
                result.insert(0, (char) (remainder + 55));
            }
            dec /= 16;
        }
        if (hexToDec(result.toString()) > 65535) {
            throw new IllegalArgumentException("Memory Location Exceeded");
        }
        return result.toString();
    }

    static String decreaseHex(String hexNumber) {
        int len = hexNumber.length();
        int i = len - 1;
        char[] chars = hexNumber.toCharArray();
        while (i >= 0 && chars[i] == '0') {
            chars[i] = 'f';
            i--;
        }
        if (i >= 0) {
            chars[i] = (char) (chars[i] - 1);
        }
        return new String(chars);
    }

    static void saveToFile() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("memory.txt"))) {
            for (int i = 0; i < 65536; i++) {
                writer.write(String.valueOf(memory[i]));
                writer.newLine();
            }
        }
    }

    static void loadMemory() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader("memory.txt"))) {
            String line;
            int i = 0;
            while ((line = reader.readLine()) != null) {
                memory[i++] = Short.parseShort(line);
            }
        }
    }

    static boolean check_flag(int opcode) {
        return true;
    }

    static void xra(int val) {
        reg[0] = (short) (reg[0] ^ val);
    }

    static void cma(int val) {
        reg[0] = (short) (~val);
    }

    static int parity(int num) {
        int count = 0;
        while (num != 0) {
            count++;
            num = num & (num - 1);
        }
        return count % 2 == 0 ? 1 : 0;
    }
//!!
    static void change_flag(int val) {
        //[ S | 0 | - | AC | - | P | - | C ]
        if (val < 0) {
            F = "1" + F.substring(1);
        } else {
            F = "0" + F.substring(1);
        }
        if (val == 0) {
            F = F.charAt(0) + "1" + F.substring(2);
        } else {
            F = F.charAt(0) + "0" + F.substring(2);
        }
        if (parity(val) == 1) {
            F = F.substring(0, 5) + "1" + F.substring(6);
        } else {
            F = F.substring(0, 5) + "0" + F.substring(6);
        }
        if (val > 255 || val < 0) {
            val = Math.abs(val);
            String val_ = decToHex(val).substring(decToHex(val).length() - 2);
            F = F.substring(0, 7) + "1";
            reg[0] = (short) hexToDec(val_);
        } else {
            F = F.substring(0, 7) + "0";
        }
    }

    static void ora(int val) {
        reg[0] = (short) (reg[0] | val);
    }

    static void ana(int val) {
        reg[0] = (short) (reg[0] & val);
    }

    static void runProgram(int loc){
        System.out.println("EXECUTING");
        while(loc < 65536){
            String hex_mem = decToHex(memory[loc]);
            if(hex_mem.length() == 1 && !hex_mem.equals("$")) hex_mem = "0"+hex_mem;
            //skipping IF
            char teleport = hex_mem.charAt(0);
            switch (teleport) {
                case '0' -> {
                    switch (hex_mem) {
                        case "00": // NOP
                            break;
                        case "01": // LXI B, data16 - Load 16-bit immediate data into registers B and C
                            loc++;
                            reg[2] = memory[loc];
                            loc++;
                            reg[1] = memory[loc];
                            break;
                        case "02": {// STAX B - Store accumulator indirect
                            String s = decToHex(reg[1]) + decToHex(reg[2]);
                            memory[hexToDec(s)] = reg[0];
                            break;
                        }
                        case "03": {// INX B - Increment registers B and C
                            String s = decToHex(reg[1]) + decToHex(reg[2]);
                            s = increaseHexByOne(s);
                            String s1 = s.substring(0, 2);
                            String s2 = s.substring(2);
                            reg[1] = (short) hexToDec(s1);
                            reg[2] = (short) hexToDec(s2); // Flag does not change

                            break;
                        }
                        case "04": {// INR B - Increment register B
                            reg[1]++;
                            int x = reg[0];
                            change_flag(reg[1]);
                            reg[0] = (short) x;
                            break;
                        }
                        case "05": {// DCR B - Decrement register B
                            reg[1]--;
                            int x = reg[0];
                            change_flag(reg[1]);
                            reg[0] = (short) x;
                            break;
                        }
                        case "06": // MVI B, data8 - Move immediate 8-bit data into register B
                            loc++;
                            reg[1] = memory[loc];
                            break;
                        case "07":  // RLC - Rotate accumulator left

                            break;
                        case "09": // DAD B - Double add registers B and C to HL

                            break;
                        case "0A": {// LDAX B - Load accumulator indirect
                            String s = decToHex(reg[1]) + decToHex(reg[2]);
                            reg[0] = mem(s);
                            break;
                        }
                        case "0B": {// DCX B - Decrement registers B and C
                            String s = decToHex(reg[1]) + decToHex(reg[2]);
                            s = decreaseHex(s);
                            String s1 = s.substring(0, 2);
                            String s2 = s.substring(2);
                            reg[1] = (short) hexToDec(s1);
                            reg[2] = (short) hexToDec(s2);
                            break;
                        }
                        case "0C": {// INR C - Increment register C
                            reg[2]++;
                            int x = reg[0];
                            change_flag(reg[2]);
                            reg[0] = (short) x;
                            break;
                        }
                        case "0D": {// DCR C - Decrement register C
                            reg[2]--;
                            int x = reg[0];
                            reg[0] = (short) x;
                            change_flag(reg[2]);
                            break;
                        }
                        case "0E": // MVI C, data8 - Move immediate 8-bit data into register C
                            loc++;
                            reg[2] = memory[loc];
                            break;
                        case "0F": // RRC - Rotate accumulator right
                            break;
                    }
                    loc++;
                }
                case '1' -> {
                    if (hex_mem.equals("11")) {// LXI D, data16 - Load 16-bit immediate data into registers D and E
                        loc++;
                        reg[4] = memory[loc];
                        loc++;
                        reg[3] = memory[loc];
                    } else if (hex_mem.equals("12")) {// STAX D - Store accumulator indirect
                        String s = decToHex(reg[3]) + decToHex(reg[4]);
                        memory[hexToDec(s)] = reg[0];
                    } else if (hex_mem.equals("13")) { // INX D - Increment registers D and E
                        String s = decToHex(reg[3]) + decToHex(reg[4]);
                        s = increaseHexByOne(s);
                        String s1 = s.substring(0, 2);
                        reg[3] = (short) hexToDec(s1);
                        String s2 = s.substring(2);
                        reg[4] = (short) hexToDec(s2);
                    } else if (hex_mem.equals("14")) {// INR D - Increment register D
                        reg[3]++;
                        int x = reg[0];
                        change_flag(reg[3]);
                        reg[0] = (short) x;
                    } else if (hex_mem.equals("15")) {// DCR D - Decrement register D
                        reg[3]--;
                        int x = reg[0];
                        change_flag(reg[3]);
                        reg[0] = (short) x;
                    } else if (hex_mem.equals("16")) {// MVI D, data8 - Move immediate 8-bit data into register D
                        loc++;
                        reg[3] = memory[loc];
                    } else if (hex_mem.equals("17")) {// RAL - Rotate accumulator left through carry
                    } else if (hex_mem.equals("19")) {// DAD D - Double add registers D and E to HL
                    } else if (hex_mem.equals("1A")) {// LDAX D - Load accumulator indirect
                        String s = decToHex(reg[3]) + decToHex(reg[4]);
                        reg[0] = mem(s);
                    }
                    //!! 1B missing of 13
                    else if (hex_mem.equals("1B")) { // INX D - Increment registers D and E
                        String s = decToHex(reg[3]) + decToHex(reg[4]);
                        s = increaseHexByOne(s);
                        reg[3] = (short) hexToDec(s.substring(0, 2));
                        reg[4] = (short) hexToDec(s.substring(2));
                    } else if (hex_mem.equals("1C")) {// INR E - Increment register E
                        reg[4]++;
                        int x = reg[0];
                        change_flag(reg[4]);
                        reg[0] = (short) x;
                    } else if (hex_mem.equals("1D")) {// DCR E - Decrement register E
                        reg[4]--;
                        int x = reg[0];
                        change_flag(reg[4]);
                        reg[0] = (short) x;
                    } else if (hex_mem.equals("1E")) {// MVI E, data8 - Move immediate 8-bit data into register E
                        loc++;
                        reg[4] = memory[loc];
                    } else if (hex_mem.equals("1F")) {// RAR - Rotate accumulator right through carry
                    }
                    loc++;
                }
                case '2' -> {
                    if (hex_mem.equals("20")) {// RIM - Read interrupt mask
                    } else if (hex_mem.equals("21")) {// LXI H, data16 - Load 16-bit immediate data into registers H and L
                        loc++;
                        reg[6] = memory[loc];
                        loc++;
                        reg[5] = memory[loc];
                    } else if (hex_mem.equals("22")) {// SHLD address - Store H and L registers direct
                        loc++;
                        String s = decToHex(loc);
                        loc++;
                        s += decToHex(loc);
                        memory[hexToDec(s)] = reg[6];
                        s = increaseHexByOne(s);
                        memory[hexToDec(s)] = reg[5];
                    } else if (hex_mem.equals("23")) { // INX H - Increment registers H and L
                        String s = decToHex(reg[5]) + decToHex(reg[6]);
                        s = increaseHexByOne(s);
                        reg[5] = (short) hexToDec(s.substring(0, 2));
                        reg[6] = (short) hexToDec(s.substring(2));
                    } else if (hex_mem.equals("24")) {// INR H - Increment register H
                        reg[5]++;
                        int x = reg[0];
                        change_flag(reg[5]);
                        reg[0] = (short) x;
                    } else if (hex_mem.equals("25")) {// DCR H - Decrement register H
                        reg[5]--;
                        int x = reg[0];
                        change_flag(reg[5]);
                        reg[0] = (short) x;
                    } else if (hex_mem.equals("26")) {// MVI H, data8 - Move immediate 8-bit data into register H
                        loc++;
                        reg[5] = memory[loc];
                    } else if (hex_mem.equals("27")) {// DAA - Decimal adjust accumulator
                    } else if (hex_mem.equals("29")) {// DAD H - Double add registers H and L to HL
                    } else if (hex_mem.equals("2A")) {// LHLD address - Load H and L registers direct
                        loc++;
                        String s = decToHex(loc);
                        loc++;
                        s += decToHex(loc);
                        reg[6] = mem(s);
                        s = increaseHexByOne(s);
                        reg[5] = mem(s);
                    } else if (hex_mem.equals("2B")) { // DCX H - Decrement registers H and L
                        String s = decToHex(reg[5]) + decToHex(reg[6]);
                        s = decreaseHex(s);
                        reg[5] = (short) hexToDec(s.substring(0, 2));
                        reg[6] = (short) hexToDec(s.substring(2));
                    } else if (hex_mem.equals("2C")) {// INR L - Increment register L
                        reg[6]++;
                        int x = reg[0];
                        change_flag(reg[6]);
                        reg[0] = (short) x;
                    } else if (hex_mem.equals("2D")) {// DCR L - Decrement register L
                        reg[6]--;
                        int x = reg[0];
                        change_flag(reg[6]);
                        reg[0] = (short) x;
                    } else if (hex_mem.equals("2E")) {// MVI L, data8 - Move immediate 8-bit data into register L
                        loc++;
                        reg[6] = memory[loc];
                    } else if (hex_mem.equals("2F")) {// CMA - Complement accumulator
                        cma(reg[0]);
                    }
                    loc++;
                }
                case '3' -> {
                    if (hex_mem.equals("30")) {// SIM - Set interrupt mask
                    } else if (hex_mem.equals("31")) {// LXI SP, data16 - Load 16-bit immediate data into stack pointer
                    } else if (hex_mem.equals("32")) {
                        loc++;
                        String s2 = decToHex(memory[loc]);
                        loc++;
                        String s1 = decToHex(memory[loc]);
                        String s3 = s1 + s2;
                        memory[hexToDec(s3)] = reg[0];
                    } else if (hex_mem.equals("33")) {// INX SP - Increment stack pointer
                    } else if (hex_mem.equals("34")) {// INR M - Increment memory pointed by HL
                        String s = decToHex(reg[5]) + decToHex(reg[6]);
                        memory[hexToDec(s)]++;
                    } else if (hex_mem.equals("35")) {// DCR M - Decrement memory pointed by HL
                        String s = decToHex(reg[5]) + decToHex(reg[6]);
                        memory[hexToDec(s)]--;
                    } else if (hex_mem.equals("36")) {// MVI M, data8 - Move immediate 8-bit data into memory pointed by HL
                        loc++;
                        int x = memory[loc];
                        String s = decToHex(reg[5]) + decToHex(reg[6]);
                        memory[hexToDec(s)] = (short) x;
                    } else if (hex_mem.equals("37")) {// STC - Set carry
                        char[] fArray = F.toCharArray();
                        fArray[7] = '1';
                        F = new String(fArray);
                    }
                    else if (hex_mem.equals("39")) {// DAD SP - Double add stack pointer to HL
                    } else if (hex_mem.equals("3A")) {// LDA address - Load accumulator direct
                        loc++;
                        String s2 = decToHex(memory[loc]);
                        loc++;
                        String s1 = decToHex(memory[loc]);
                        String s = s1 + s2;
                        reg[0] = mem(s);
                    } else if (hex_mem.equals("3B")) {// DCX SP - Decrement stack pointer
                    } else if (hex_mem.equals("3C")) {// INR A - Increment accumulator
                        reg[0]++;
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("3D")) {// DCR A - Decrement accumulator
                        reg[0]--;
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("3E")) {// MVI A, data8 - Move immediate 8-bit data into accumulator
                        loc++;
                        reg[0] = memory[loc];
                    } else if (hex_mem.equals("3F")) { // CMC - Complement carry
                        char[] fArray = F.toCharArray();
                        if (fArray[7] == '1') {
                            fArray[7] = '0';
                        } else {
                            fArray[7] = '1';
                        }
                        F = new String(fArray);
                    }
                    loc++;
                }
                case '4' -> {
                    if (hex_mem.equals("41")) {//MOV B,C
                        reg[1] = reg[2];
                    } else if (hex_mem.equals("42")) {//MOV B,D
                        reg[1] = reg[3];
                    } else if (hex_mem.equals("43")) {//MOV B,E
                        reg[1] = reg[4];
                    } else if (hex_mem.equals("44")) {//MOV B,H
                        reg[1] = reg[5];
                    } else if (hex_mem.equals("45")) {//MOV B,L
                        reg[1] = reg[6];
                    } else if (hex_mem.equals("46")) {//MOV B,M
                        reg[1] = get_M();
                    } else if (hex_mem.equals("47")) {//MOV B,A
                        reg[1] = reg[0];
                    } else if (hex_mem.equals("48")) {//MOV C,B
                        reg[2] = reg[1];
                    } else if (hex_mem.equals("4A")) {//MOV C,D
                        reg[2] = reg[3];
                    } else if (hex_mem.equals("4B")) {//MOV C,E
                        reg[2] = reg[4];
                    } else if (hex_mem.equals("4C")) {//MOV C,H
                        reg[2] = reg[5];
                    } else if (hex_mem.equals("4D")) {//MOV C,L
                        reg[2] = reg[6];
                    } else if (hex_mem.equals("4E")) {//MOV C,M
                        reg[2] = get_M();
                    } else if (hex_mem.equals("4F")) {//MOV C,A
                        reg[2] = reg[0];
                    }
                    loc++;
                }
                case '5' -> {
                    if (hex_mem.equals("50")) {//MOV D,B
                        reg[3] = reg[1];
                    } else if (hex_mem.equals("51")) {//MOV D,C
                        reg[3] = reg[2];
                    } else if (hex_mem.equals("53")) {//MOV D,E
                        reg[3] = reg[4];
                    } else if (hex_mem.equals("54")) {//MOV D,H
                        reg[3] = reg[5];
                    } else if (hex_mem.equals("55")) {//MOV D,L
                        reg[3] = reg[6];
                    } else if (hex_mem.equals("56")) {//MOV D,M
                        reg[3] = get_M();
                    } else if (hex_mem.equals("57")) {//MOV D,A
                        reg[3] = reg[0];
                    } else if (hex_mem.equals("58")) {//MOV E,B
                        reg[4] = reg[1];
                    } else if (hex_mem.equals("59")) {//MOV E,C
                        reg[4] = reg[2];
                    } else if (hex_mem.equals("5A")) {//MOV E,D
                        reg[4] = reg[3];
                    } else if (hex_mem.equals("5C")) {//MOV E,H
                        reg[4] = reg[5];
                    } else if (hex_mem.equals("5D")) {//MOV E,L
                        reg[4] = reg[6];
                    } else if (hex_mem.equals("5E")) {//MOV E,M
                        reg[4] = get_M();
                    } else if (hex_mem.equals("5F")) {//MOV E,A
                        reg[4] = reg[0];
                    }
                    loc++;
                }
                case '6' -> {
                    if (hex_mem.equals("60")) {//MOV H,B
                        reg[5] = reg[1];
                    } else if (hex_mem.equals("61")) {//MOV H,C
                        reg[5] = reg[2];
                    } else if (hex_mem.equals("62")) {//MOV H,D
                        reg[5] = reg[3];
                    } else if (hex_mem.equals("63")) {//MOV H,E
                        reg[5] = reg[4];
                    } else if (hex_mem.equals("65")) {//MOV H,L
                        reg[5] = reg[6];
                    } else if (hex_mem.equals("66")) {//MOV H,M
                        reg[5] = get_M();
                    } else if (hex_mem.equals("67")) {//MOV H,A
                        reg[5] = reg[0];
                    } else if (hex_mem.equals("68")) {//MOV L,B
                        reg[6] = reg[1];
                    } else if (hex_mem.equals("69")) {//MOV L,C
                        reg[6] = reg[2];
                    } else if (hex_mem.equals("6A")) {//MOV L,D
                        reg[6] = reg[3];
                    } else if (hex_mem.equals("6B")) {//MOV L,E
                        reg[6] = reg[4];
                    } else if (hex_mem.equals("6C")) {//MOV L,H
                        reg[6] = reg[5];
                    } else if (hex_mem.equals("6E")) {//MOV L,M
                        reg[6] = get_M();
                    } else if (hex_mem.equals("6F")) {//MOV L,A
                        reg[6] = reg[0];
                    }
                    loc++;
                }
                case '7' -> {
                    if (hex_mem.equals("70")) {//MOV M,B
                        put_M(reg[1]);
                    } else if (hex_mem.equals("71")) {//MOV M,C
                        put_M(reg[2]);
                    } else if (hex_mem.equals("72")) {//MOV M,D
                        put_M(reg[3]);
                    } else if (hex_mem.equals("73")) {//MOV M,E
                        put_M(reg[4]);
                    } else if (hex_mem.equals("74")) {//MOV M,H
                        put_M(reg[5]);
                    } else if (hex_mem.equals("75")) {//MOV M,L
                        put_M(reg[6]);
                    } else if (hex_mem.equals("77")) {//MOV M,A
                        put_M(reg[0]);
                    } else if (hex_mem.equals("78")) {// Mov A,B
                        reg[0] = reg[1];
                    } else if (hex_mem.equals("79")) {//MOV A,C
                        reg[0] = reg[2];
                    } else if (hex_mem.equals("7A")) {//MOV A,D
                        reg[0] = reg[3];
                    } else if (hex_mem.equals("7B")) {//MOV A,E
                        reg[0] = reg[4];
                    } else if (hex_mem.equals("7C")) {//MOV A,H
                        reg[0] = reg[5];
                    } else if (hex_mem.equals("7D")) {//MOV A,L
                        reg[0] = reg[6];
                    } else if (hex_mem.equals("7E")) {//MOV A,M
                        reg[0] = get_M();
                    }
                    loc++;
                }
                case '8' -> {
                    if (hex_mem.equals("80")) {// ADD B
                        reg[0] = (short) (reg[0] + reg[1]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("81")) {// ADD C
                        reg[0] = (short) (reg[0] + reg[2]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("82")) {// ADD D
                        reg[0] = (short) (reg[0] + reg[3]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("83")) {// ADD E
                        reg[0] = (short) (reg[0] + reg[4]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("84")) {// ADD H
                        reg[0] = (short) (reg[0] + reg[5]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("85")) {// ADD L
                        reg[0] = (short) (reg[0] + reg[6]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("86")) {// ADD M
                        reg[0] = (short) (reg[0] + get_M());
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("87")) {// ADD A
                        reg[0] = (short) (reg[0] + reg[0]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("88")) {// ADC B
                        reg[0] += reg[1] + (F.charAt(7) - '0');
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("89")) {// ADC C
                        reg[0] += reg[2] + (F.charAt(7) - '0');
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("8A")) {// ADC D
                        reg[0] += reg[3] + (F.charAt(7) - '0');
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("8B")) { // ADC E
                        reg[0] += reg[4] + (F.charAt(7) - '0');
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("8C")) { // ADC H
                        reg[0] += reg[5] + (F.charAt(7) - '0');
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("8D")) { // ADC L
                        reg[0] += reg[6] + (F.charAt(7) - '0');
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("8E")) { // ADC M
                        reg[0] += get_M() + (F.charAt(7) - '0');
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("8F")) { // ADC A
                        reg[0] += reg[0] + (F.charAt(7) - '0');
                        change_flag(reg[0]);
                    }
                    loc++;
                }
                case '9' -> {
                    if (hex_mem.equals("90")) {// SUB B
                        reg[0] = (short) (reg[0] - reg[1]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("91")) {// SUB C
                        reg[0] = (short) (reg[0] - reg[2]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("92")) {// SUB D
                        reg[0] = (short) (reg[0] - reg[3]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("93")) {// SUB E
                        reg[0] = (short) (reg[0] - reg[4]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("94")) {// SUB H
                        reg[0] = (short) (reg[0] - reg[5]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("95")) {// SUB L
                        reg[0] = (short) (reg[0] - reg[6]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("96")) {// SUB M
                        reg[0] = (short) (reg[0] - get_M());
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("97")) {// SUB A
                        reg[0] = (short) (reg[0] - reg[0]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("98")) { // SBB B
                        reg[0] = (short) (reg[0] - reg[1] - (F.charAt(7) - '0'));
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("99")) { // SBB C
                        reg[0] = (short) (reg[0] - reg[2] - (F.charAt(7) - '0'));
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("9A")) { // SBB D
                        reg[0] = (short) (reg[0] - reg[3] - (F.charAt(7) - '0'));
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("9B")) { // SBB E
                        reg[0] = (short) (reg[0] - reg[4] - (F.charAt(7) - '0'));
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("9C")) { // SBB H
                        reg[0] = (short) (reg[0] - reg[5] - (F.charAt(7) - '0'));
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("9D")) { // SBB L
                        reg[0] = (short) (reg[0] - reg[6] - (F.charAt(7) - '0'));
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("9E")) { // SBB M
                        reg[0] = (short) (reg[0] - get_M() - (F.charAt(7) - '0'));
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("9F")) { // SBB A
                        reg[0] = (short) (-(F.charAt(7) - '0'));
                        change_flag(reg[0]);
                    }
                    loc++;
                }
                case 'A' -> {
                    if (hex_mem.equals("A0")) {// ANA B
                        ana(reg[1]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("A1")) {// ANA C
                        ana(reg[2]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("A2")) {// ANA D
                        ana(reg[3]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("A3")) {// ANA E
                        ana(reg[4]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("A4")) {// ANA H
                        ana(reg[5]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("A5")) {// ANA L
                        ana(reg[6]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("A6")) {// ANA M
                        ana(get_M());
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("A7")) {// ANA A
                        ana(reg[0]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("A8")) {// XRA B
                        xra(reg[1]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("A9")) {// XRA C
                        xra(reg[2]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("AA")) {// XRA D
                        xra(reg[3]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("AB")) {// XRA E
                        xra(reg[4]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("AC")) {// XRA H
                        xra(reg[5]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("AD")) {// XRA L
                        xra(reg[6]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("AE")) {// XRA M
                        xra(get_M());
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("AF")) {// XRA A
                        xra(reg[0]);
                        change_flag(reg[0]);
                    }
                    loc++;
                }
                case 'B' -> {
                    if (hex_mem.equals("B0")) {// ORA B
                        ora(reg[1]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("B1")) {// ORA C
                        ora(reg[2]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("B2")) {// ORA D
                        ora(reg[3]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("B3")) {// ORA E
                        ora(reg[4]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("B4")) {// ORA H
                        ora(reg[5]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("B5")) {// ORA L
                        ora(reg[6]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("B6")) {// ORA M
                        ora(get_M());
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("B7")) {// ORA A
                        ora(reg[0]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("B8")) {// CMP B
                        int x = reg[0];
                        change_flag(reg[0] - reg[1]);
                        reg[0] = (short) x;
                    } else if (hex_mem.equals("B9")) {// CMP C
                        int x = reg[0];
                        change_flag(reg[0] - reg[2]);
                        reg[0] = (short) x;
                    } else if (hex_mem.equals("BA")) {// CMP D
                        int x = reg[0];
                        change_flag(reg[0] - reg[3]);
                        reg[0] = (short) x;
                    } else if (hex_mem.equals("BB")) {// CMP E
                        int x = reg[0];
                        change_flag(reg[0] - reg[4]);
                        reg[0] = (short) x;
                    } else if (hex_mem.equals("BC")) {// CMP H
                        int x = reg[0];
                        change_flag(reg[0] - reg[5]);
                        reg[0] = (short) x;
                    } else if (hex_mem.equals("BD")) {// CMP L
                        int x = reg[0];
                        change_flag(reg[0] - reg[6]);
                        reg[0] = (short) x;
                    } else if (hex_mem.equals("BE")) {// CMP M
                        int x = reg[0];
                        change_flag(reg[0] - get_M());
                        reg[0] = (short) x;
                    } else if (hex_mem.equals("BF")) {// CMP A
                        int x = reg[0];
                        change_flag(0);
                        reg[0] = (short) x;
                    }
                    loc++;
                }
                case 'C' -> {
                    if (hex_mem.equals("C0")) {// RNZ
                        if (F.charAt(1) == '0') {
                            loc = stPop();
                            continue;
                        }
                    } else if (hex_mem.equals("C1")) {// POP B
                        //store into C then B;
                        reg[2] = stPop();
                        reg[1] = stPop();
                    } else if (hex_mem.equals("C2")) {// JNZ
                        if (F.charAt(1) == '0') {
                            loc++;
                            String s2 = decToHex(memory[loc]);
                            loc++;
                            String s1 = decToHex(memory[loc]);
                            String s = s1 + s2;
                            loc = hexToDec(s);
                            continue;
                        } else {
                            loc++;
                            loc++;
                        }
                    } else if (hex_mem.equals("C3")) {// JMP
                        loc++;
                        String s2 = decToHex(memory[loc]);
                        loc++;
                        String s1 = decToHex(memory[loc]);
                        String s = s1 + s2;
                        loc = hexToDec(s);
                        continue;
                    } else if (hex_mem.equals("C4")) {// CNZ
                        if (F.charAt(1) == '0') {
                            loc++;
                            String s2 = decToHex(memory[loc]);
                            loc++;
                            String s1 = decToHex(memory[loc]);
                            String s = s1 + s2;
                            stPush((short) ++loc);
                            loc = hexToDec(s);
                            continue;
                        } else {
                            loc++;
                            loc++;
                        }
                    } else if (hex_mem.equals("C5")) {// PUSH B
                        //store value of B then C into the Stack
                        stPush(get_reg('B'));
                        stPush(get_reg('C'));
                    } else if (hex_mem.equals("C6")) {// ADI
                    } else if (hex_mem.equals("C7")) {// RST 0
                    } else if (hex_mem.equals("C8")) {// RZ
                        if (F.charAt(1) == '1') {
                            loc = stPop();
                            continue;
                        }
                    } else if (hex_mem.equals("C9")) {// RET
                        loc = stPop();
                        continue;
                    }
                    else if (hex_mem.equals("CA")) {// JZ Address
                        if (F.charAt(1) == '1') {
                            loc++;
                            String s2 = decToHex(memory[loc]);
                            loc++;
                            String s1 = decToHex(memory[loc]);
                            String s = s1 + s2;
                            loc = hexToDec(s);
                            continue;
                        } else {
                            loc++;
                            loc++;
                        }
                    }
                    else if (hex_mem.equals("CC")) {// CZ
                        if (F.charAt(1) == '1') {
                            loc++;
                            String s2 = decToHex(memory[loc]);
                            loc++;
                            String s1 = decToHex(memory[loc]);
                            String s = s1 + s2;
                            stPush((short) ++loc);
                            loc = hexToDec(s);
                            continue;
                        } else {
                            loc++;
                            loc++;
                        }
                    } else if (hex_mem.equals("CD")) {// CALL
                        loc++;
                        String s2 = decToHex(memory[loc]);
                        loc++;
                        String s1 = decToHex(memory[loc]);
                        String s = s1 + s2;
                        stPush((short) ++loc);
                        loc = hexToDec(s);
                        continue;
                    } else if (hex_mem.equals("CE")) {// ACI
                        loc++;
                        reg[0] = (short) (reg[0] + memory[loc] + Character.getNumericValue(F.charAt(7)));
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("CF")) {// RST 1
                    }
                    loc++;
                }
                case 'D' -> {
                    if (hex_mem.equals("D0")) {// RNC
                        if (F.charAt(7) == '0') {
                            loc = stPop();
                            continue;
                        }
                    } else if (hex_mem.equals("D1")) {// POP D
                        reg[4] = stPop();
                        reg[3] = stPop();
                    } else if (hex_mem.equals("D2")) {// JNC
                        if (F.charAt(7) == '1') {
                            loc++;
                            loc++;
                        } else {
                            loc++;
                            String s2 = decToHex(memory[loc]);
                            loc++;
                            String s1 = decToHex(memory[loc]);
                            String s = s1 + s2;
                            loc = hexToDec(s);
                            continue;
                        }
                    } else if (hex_mem.equals("D3")) {// OUT
                    } else if (hex_mem.equals("D4")) {// CNC
                        if (F.charAt(7) == '1') {
                            loc++;
                            loc++;
                        } else {
                            loc++;
                            String s2 = decToHex(memory[loc]);
                            loc++;
                            String s1 = decToHex(memory[loc]);
                            String s = s1 + s2;
                            stPush((short) ++loc);
                            loc = hexToDec(s);
                            continue;
                        }
                    } else if (hex_mem.equals("D5")) {// PUSH D
                        stPush(get_reg('D'));
                        stPush(get_reg('E'));
                    } else if (hex_mem.equals("D6")) {// SUI
                        loc++;
                        reg[0] = (short) (reg[0] - memory[loc]);
                        change_flag(reg[0]);
                    } else if (hex_mem.equals("D7")) {// RST 2
                    } else if (hex_mem.equals("D8")) {// RC
                        if (F.charAt(7) == '1') {
                            loc = stPop();
                            continue;
                        }
                    } else if (hex_mem.equals("DA")) {// JC
                        if (F.charAt(7) == '1') {
                            loc++;
                            String s2 = decToHex(memory[loc]);
                            loc++;
                            String s1 = decToHex(memory[loc]);
                            String s = s1 + s2;
                            loc = hexToDec(s);
                            continue;
                        } else {
                            loc++;
                            loc++;
                        }
                    } else if (hex_mem.equals("DB")) {// IN
                    } else if (hex_mem.equals("DC")) {// CC
                        if (F.charAt(7) == '1') {
                            loc++;
                            String s2 = decToHex(memory[loc]);
                            loc++;
                            String s1 = decToHex(memory[loc]);
                            String s = s1 + s2;
                            stPush((short) ++loc);
                            loc = hexToDec(s);
                            continue;
                        } else {
                            loc++;
                            loc++;
                        }
                    }
                    //!!
                    else if (hex_mem.equals("DE")) {// SBI
                    } else if (hex_mem.equals("DF")) {// RST 3
                    }
                    loc++;
                }
                case 'E' -> {
                    if (hex_mem.equals("E0")) {// RPO
                        if (F.charAt(5) == '0') {
                            loc = stPop();
                            continue;
                        }
                    } else if (hex_mem.equals("E1")) {// POP H
                        reg[6] = stPop();
                        reg[5] = stPop();
                    } else if (hex_mem.equals("E2")) {// JPO
                        if (F.charAt(5) == '0') {
                            loc++;
                            String s2 = decToHex(memory[loc]);
                            loc++;
                            String s1 = decToHex(memory[loc]);
                            String s = s1 + s2;
                            loc = hexToDec(s);
                            continue;
                        } else {
                            loc++;
                            loc++;
                        }
                    } else if (hex_mem.equals("E3")) {// XTHL - exchange HL with top stack pointer sp & sp+1
                        //int temp = reg[5];
                        //reg[5] = memory[sp+1];
                        //memory[sp+1] = temp;
                        //temp = reg[6];
                        //reg[6] = memory[sp];
                        //memory[sp] = temp;
                    } else if (hex_mem.equals("E4")) {// CPO
                        if (F.charAt(5) == '0') {
                            loc++;
                            String s2 = decToHex(memory[loc]);
                            loc++;
                            String s1 = decToHex(memory[loc]);
                            String s = s1 + s2;
                            stPush((short) ++loc);
                            loc = hexToDec(s);
                            continue;
                        } else {
                            loc++;
                            loc++;
                        }
                    } else if (hex_mem.equals("E5")) {// PUSH H
                        stPush(get_reg('H'));
                        stPush(get_reg('L'));
                    } else if (hex_mem.equals("E6")) {// ANI
                    } else if (hex_mem.equals("E7")) {// RST 4
                    } else if (hex_mem.equals("E8")) {// RPE
                        if (F.charAt(5) == '1') {
                            loc = stPop();
                            continue;
                        }
                    } else if (hex_mem.equals("E9")) {// PCHL
                        String s = decToHex(reg[5]) + decToHex(reg[6]);
                        loc = hexToDec(s);
                        continue;
                    } else if (hex_mem.equals("EA")) {// JPE
                        if (F.charAt(5) == '1') {
                            loc++;
                            String s2 = decToHex(memory[loc]);
                            loc++;
                            String s1 = decToHex(memory[loc]);
                            String s = s1 + s2;
                            loc = hexToDec(s);
                            continue;
                        } else {
                            loc++;
                            loc++;
                        }
                    } else if (hex_mem.equals("EB")) {// XCHG
                        short temp = reg[3];
                        reg[3] = reg[5];
                        reg[5] = temp;
                        temp = reg[4];
                        reg[4] = reg[6];
                        reg[6] = temp;
                    } else if (hex_mem.equals("EC")) {// CPE
                        if (F.charAt(5) == '1') {
                            loc++;
                            String s2 = decToHex(memory[loc]);
                            loc++;
                            String s1 = decToHex(memory[loc]);
                            String s = s1 + s2;
                            stPush((short) ++loc);
                            loc = hexToDec(s);
                            continue;
                        } else {
                            loc++;
                            loc++;
                        }
                    } else if (hex_mem.equals("EE")) {// XRI
                    } else if (hex_mem.equals("EF")) {// RST 5
                        break;
                    }
                    loc++;
                }
                case 'F' -> {
                    if (hex_mem.equals("F0")) {// RP
                        if (F.charAt(0) == '0') {
                            loc = stPop();
                            continue;
                        }
                    } else if (hex_mem.equals("F1")) {// POP PSW
                        F = decToBin(stPop());
                    } else if (hex_mem.equals("F2")) {// JP
                        if (F.charAt(0) == '0') {
                            loc++;
                            String s2 = decToHex(memory[loc]);
                            loc++;
                            String s1 = decToHex(memory[loc]);
                            String s = s1 + s2;
                            loc = hexToDec(s);
                            continue;
                        } else {
                            loc++;
                            loc++;
                        }
                    } else if (hex_mem.equals("F3")) {// DI
                    } else if (hex_mem.equals("F4")) {// CP
                        if (F.charAt(0) == '0') {
                            loc++;
                            String s2 = decToHex(memory[loc]);
                            loc++;
                            String s1 = decToHex(memory[loc]);
                            String s = s1 + s2;
                            stPush((short) ++loc);
                            loc = hexToDec(s);
                            continue;
                        } else {
                            loc++;
                            loc++;
                        }
                    } else if (hex_mem.equals("F5")) {// PUSH PSW
                        stPush((short) binToDec(F));
                    } else if (hex_mem.equals("F6")) {// ORI
                    } else if (hex_mem.equals("F7")) {// RST 6
                    } else if (hex_mem.equals("F8")) {// RM
                        if (F.charAt(0) == '1') {
                            loc = stPop();
                            continue;
                        }
                    } else if (hex_mem.equals("F9")) {// SPMHL
                    } else if (hex_mem.equals("FA")) {// JM
                        if (F.charAt(0) == '1') {
                            loc++;
                            String s2 = decToHex(memory[loc]);
                            loc++;
                            String s1 = decToHex(memory[loc]);
                            String s = s1 + s2;
                            loc = hexToDec(s);
                            continue;
                        } else {
                            loc++;
                            loc++;
                        }
                    } else if (hex_mem.equals("FB")) {// EI
                    } else if (hex_mem.equals("FC")) {// CM
                        if (F.charAt(0) == '1') {
                            loc++;
                            String s2 = decToHex(memory[loc]);
                            loc++;
                            String s1 = decToHex(memory[loc]);
                            String s = s1 + s2;
                            stPush((short) ++loc);
                            loc = hexToDec(s);
                            continue;
                        } else {
                            loc++;
                            loc++;
                        }
                    }
                    //!!
                    else if (hex_mem.equals("FE")) {// CPI
                    } else if (hex_mem.equals("FF")) {// RST 7
                    }
                    loc++;
                }
                default -> throw new IllegalArgumentException("Invalid teleport value: " + teleport);
            }
        }
        clearScreen();
    }


    public static void main(String[] args) throws IOException {
        clearScreen();
        Arrays.fill(memory, (byte) 0);
        loadMemory();
        Scanner scanner = new Scanner(System.in);
        while (true) {
            try {
                System.out.println("--8085 Microprocessor--");
                char in = scanner.next().charAt(0);
                //clearScreen();
                if (in == 'M' || in == 'm') {
                    String loc = scanner.next();
                    int decLoc = hexToDec(loc);
                    if (loc.length() > 4 || decLoc >= memory.length || decLoc < 0) {
                        throw new Exception("Unrecognized Memory Location");
                    }

                    String cond = "";
                    scanner.nextLine(); // to flush the std input
                    while (!cond.equals("$")) {
                        System.out.print("M" + loc + ":");
                        System.out.print(decToHex(memory[decLoc]) + "-");
                        cond = scanner.nextLine();
                        clearScreen();

                        if (cond.isEmpty() || (cond.length() == 1 && cond.charAt(0) == '\n')) {
                            loc = increaseHexByOne(loc);
                            decLoc = hexToDec(loc);
                            if (decLoc >= memory.length || decLoc < 0) {
                                throw new Exception("Memory location out of bounds");
                            }
                        } else if (!cond.equals("$")) {
                            int l = cond.length();
                            if (l == 1 || l == 2) {
                                memory[decLoc] = (short) hexToDec(cond);
                            } else {
                                String val = cond.substring(cond.length() - 2);
                                val = new StringBuilder(val).reverse().toString();
                                memory[decLoc] = (short) hexToDec(val);
                            }
                            loc = increaseHexByOne(loc);
                            decLoc = hexToDec(loc);
                            if (decLoc >= memory.length || decLoc < 0) {
                                throw new Exception("Memory location out of bounds");
                            }
                        }
                    }
                    saveToFile();
                } else if (in == 'R' || in == 'r') {
                    int i = 0;
                    String registe = "ABCDEHLF";
                    scanner.nextLine(); // to flush the std input
                    while (i < 8) {
                        System.out.print(registe.charAt(i) + ":");
                        System.out.print(decToHex(reg[i]) + "-");
                        String cond = scanner.nextLine();
                        clearScreen();
                        if (!cond.equals("$")) {
                            int l = cond.length();
                            if (l == 1 || l == 2) {
                                reg[i] = (short) hexToDec(cond);
                            } else if (l != 0) {
                                String val = cond.substring(cond.length() - 2);
                                val = new StringBuilder(val).reverse().toString();
                                reg[i] = (short) hexToDec(val);
                            }
                            i++;
                        } else if (cond.equals("$")) {
                            break;
                        } else {
                            throw new Exception("Unexpected Error");
                        }
                    }
                } else if (in == 'G' || in == 'g') {
                    String loc = scanner.next();
                    clearScreen();
                    runProgram(hexToDec(loc));
                }
            } catch (Exception e) {
                System.out.println("\n" + e.getMessage() + "\n Press Enter");
                scanner.nextLine();
                scanner.nextLine();
                clearScreen();
            }
        }
    }
}
