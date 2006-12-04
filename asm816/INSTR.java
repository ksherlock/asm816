package asm816;
/*
 * Created on Mar 13, 2006
 * Mar 13, 2006 11:19:10 PM
 */

public enum INSTR
{
    ADC,
    AND,
    ASL,
    BCC,
    BCS,
    BEQ,
    BNE,
    BMI,
    BPL,
    BVC,
    BVS,
    BRA,
    BRL,
    BIT,
    BRK,
    CLC,
    CLD,
    CLI,
    CLV,
    SEC,
    SED,
    SEI,
    CMP,
    COP,
    CPX,
    CPY,
    DEA,
    DEC,
    DEX,
    DEY,
    EOR,
    INA,
    INC,
    INX,
    INY,
    JMP,
    JML,
    JSR,
    JSL,
    LDA,
    LDX,
    LDY,
    LSR,
    MVP,
    MVN,
    NOP,
    ORA,
    PEA,
    PEI,
    PER,
    PHA,
    PHP,
    PHX,
    PHY,
    PLA,
    PLP,
    PLX,
    PLY,
    PHB,
    PHD,
    PHK,
    PLB,
    PLD,
    REP,
    ROL,
    ROR,
    RTI,
    RTL,
    RTS,
    SBC,
    SEP,
    STA,
    STP,
    STX,
    STY,
    STZ,
    TAX,
    TAY,
    TXA,
    TYA,
    TSX,
    TXS,
    TXY,
    TYX,
    TCD,
    TDC,
    TCS,
    TSC,
    TRB,
    TSB,
    WAI,
    WDM,
    XBA,
    XCE;
    
    public static final int m6502 = 0x30;
    public static final int m65c02 = 0x10;
    public static final int m65816 = 0x00;
    
    public static final int mM = 0x80;
    public static final int mX = 0x100;
    public static final int mBranch = 0x200;
    
    
   public int opcode(AddressMode mode, int machine)
    {     
       short[] tmp = __opcodes[this.ordinal()];
       int index = mode.ordinal();
       if (index >= tmp.length) return -1;
       
       int opcode = tmp[index];
       if (opcode == -1) return -1;
       if (!isValid(opcode, machine)) return -1;
       return opcode;
    }
    static public int attributes(int opcode)
    {
        return __attributes[opcode];
    }
    static public boolean isBranch(int opcode)
    {
        return ((__attributes[opcode] & mBranch) == mBranch);
    }
    static public boolean isValid(int opcode, int machine)
    {
        return ((__attributes[opcode] & machine) == machine);
    }
    static public int Size(int opcode, boolean m, boolean x)
    {
        int attr = __attributes[opcode];
        int size = attr & 0x0f;
        
        if ((attr & mM) == mM)
            if (m) size++;
        if ((attr & mX) == mX)
            if (x) size++;
        
        return size;       
    }
    static public int Time(int opcode, boolean m, boolean x)
    {
        int attr = __timing[opcode];
        int time = attr & 0x0f;
        if (m) time += (attr >> 8) & 0x0f;
        if (x) time += (attr >> 16) & 0x0f;
        
        return time;
    }
     
    private static final short[][] __opcodes =
    {
        //ADC,
        {
            -1, 0x69, 0x6d, 0x7d, 
            0x79, 0x6f, 0x7f, 0x65,
            0x75, -1, 0x72, 0x67,
            0x61, 0x71, 0x73, 0x77,
            0x63
        },
        //AND,
        {
            -1, 0x29, 0x2d, 0x3d, 
            0x39, 0x2f, 0x3f, 0x25,
            0x35, -1, 0x32, 0x27,
            0x21, 0x31, 0x33, 0x37,
            0x23
        },
        //ASL,
        {
            0x0a, -1, 0x0e, 0x1e, 
            -1, -1, -1, 0x06,
            0x16, -1, -1, -1,
        },        
        //BCC
        {
            -1, -1, 0x90, 
        },
        //BCS,
        {
            -1, -1, 0xb0, 
        },
        //BEQ,
        {
            -1, -1, 0xf0,
        },
        //BNE,
        {
            -1, -1, 0xd0, 
        },
        //BMI,
        {
            -1, -1, 0x30,
        },
        //BPL,
        {
            -1, -1, 0x10, 
        },
        //BVC,
        {
            -1, -1, 0x50, 
        },
        //BVS,
        {
            -1, -1, 0x70, 
        },
        //BRA,
        {
            -1, -1, 0x80,
        },
        //BRL,
        {
            -1, -1, 0x82,
        },
        //BIT imm, abs, dp, abs,x dp,x
        {
            -1, 0x89, 0x2c, 0x3c,
            -1, -1, -1, 0x24,
            0x34, -1, -1, -1,
        },
        //BRK -- abs & immediate both allowed.
        {
            -1, 0x00, 0x00,
        },
        //CLC,
        {0x18},
        //CLD,
        {0xd8},
        //CLI,
        {0x58},
        //CLV,
        {0xb8},
        //SEC,
        {0x38},
        //SED,
        {0xf8},
        //SEI,
        {0x78},
        //CMP,
        {
            -1, 0xc9, 0xcd, 0xdd, 
            0xd9, 0xcf, 0xdf, 0xc5,
            0xd5, -1, 0xd2, 0xc7,
            0xc1, 0xd1, 0xd3, 0xd7,
            0xc3
        },
        //COP -- abs & immediate both allowed for convenience.
        {
            -1, 0x02, 0x02, 
        },
        //CPX,
        {
            -1, 0xe0, 0xec, -1, 
            -1, -1, -1, 0xe4,
        },
        //CPY,
        {
            -1, 0xc0, 0xcc, -1, 
            -1, -1, -1, 0xc4,
        },
        //DEA
        {0x3a},
        //DEC,
        {
            0x3a, -1, 0xce, 0xde, 
            -1, -1, -1, 0xc6,
            0xd6, -1, -1, -1,
        },
        //DEX,
        {0xca},
        //DEY,
        {0x88},
        //EOR,
        {
            -1, 0x49, 0x4d, 0x5d, 
            0x59, 0x4f, 0x5f, 0x45,
            0x55, -1, 0x52, 0x47,
            0x41, 0x51, 0x53, 0x57,
            0x43
        },
        //INA
        {0x1a},
        //INC,
        {
            0x1a, -1, 0xee, 0xfe, 
            -1, -1, -1, 0xe6,
            0xf6, -1, -1, -1,
        },
        //INX,
        {0xe8},
        //INY,
        {0xc8},
        //JMP -- also includes JML.
        {
            -1, -1, 0x4c, -1, 
            -1, 0x5c, -1, -1,
            -1, -1, 0x6c, 0xdc,
            0x7c, -1, -1, -1,
        },
        //JML -- subset of JMP.
        {
            -1, -1, -1, -1, 
            -1, 0x5c, -1, -1,
            -1, -1, -1, 0xdc,

        },
        //JSR -- also includes JSR >abslong --> JSL.
        {
            -1, -1, 0x20, -1,
            -1, 0x22, -1, -1,
            -1, -1, -1, -1,
            0xfc, -1, -1, -1,
        },
        //JSL,
        {
            -1, -1, 0x22, -1,
            -1, 0x22, -1, -1,
        },
        //LDA,
        {
            -1, 0xa9, 0xad, 0xbd, 
            0xb9, 0xaf, 0xbf, 0xa5,
            0xb5, -1, 0xb2, 0xa7,
            0xa1, 0xb1, 0xb3, 0xb7,
            0xa3
        },
        //LDX,
        {
            -1, 0xa2, 0xae, -1,
            0xbe, -1, -1, 0xa6,
            -1, 0xb6, -1, -1,
        },  
        //LDY,
        {
            -1, 0xa0, 0xac, 0xbc,
            -1, -1, -1, 0xa4,
            0xb4, -1, -1, -1,
        },
        //LSR,
        {
            0x4a, -1, 0x4e, 0x5e,
            -1, -1, -1, 0x46,
            0x56, -1, -1, -1,
        },
        //MVP -- not handled here.,
        {},
        //MVN -- not handled here,
        {},
        //NOP,
        {0xea},
        //ORA,
        {
            -1, 0x09, 0x0d, 0x1d, 
            0x19, 0x0f, 0x1f, 0x05,
            0x15, -1, 0x12, 0x07,
            0x01, 0x11, 0x13, 0x17,
            0x03
        },
        //PEA-- abs & immediate both allowed for convenience.
        {
            -1, 0xf4, 0xf4
        },
        //PEI,
        {
            -1, -1, -1, -1,
            -1, -1, -1, -1,
            -1, -1, 0xd4, -1,
        },
        //PER,
        {
            -1, -1, 0x62
        },
        //PHA,
        {0x48},
        //PHP,
        {0x08},
        //PHX,
        {0xda},
        //PHY,
        {0x5a},
        //PLA,
        {0x68},
        //PLP,
        {0x28},
        //PLX,
        {0xfa},
        //PLY,
        {0x7a},
        //PHB,
        {0x8b},
        //PHD,
        {0x0b},
        //PHK,
        {0x4b},
        //PLB,
        {0xab},
        //PLD,
        {0x2b},
        //REP,
        {-1, 0xc2},
        //ROL,
        {
            0x2a, -1, 0x2e, 0x3e,
            -1, -1, -1, 0x26,
            0x36, -1, -1, -1,
        },
        //ROR,
        {
            0x6a, -1, 0x6e, 0x7e,
            -1, -1, -1, 0x66,
            0x76, -1, -1, -1,
        },
        //RTI,
        {0x40},
        //RTL,
        {0x6b},
        //RTS,
        {0x60},
        //SBC,
        {
            -1, 0xe9, 0xed, 0xfd, 
            0xf9, 0xef, 0xff, 0xe5,
            0xf5, -1, 0xf2, 0xe7,
            0xe1, 0xf1, 0xf3, 0xf7,
            0xe3
        },
        //SEP,
        {-1, 0xe2},
        //STA,
        {
            -1, -1, 0x8d, 0x9d, 
            0x99, 0x8f, 0x9f, 0x85,
            0x95, -1, 0x92, 0x87,
            0x81, 0x91, 0x93, 0x97,
            0x83
        },
        //STP,
        {0xdb},
        //STX,
        {
            -1, -1, 0x8e, -1,
            -1, -1, -1, 0x86,
            -1, 0x96, -1, -1,           
        },
        //STY,
        {
            -1, -1, 0x8c, -1,
            -1, -1, -1, 0x84,
            0x94, -1, -1, -1,           
        },
        //STZ,
        {
            -1, -1, 0x9c, 0x9e,
            -1, -1, -1, 0x64,
            0x74, -1, -1, -1,           
        },
        //TAX,
        {0xaa},
        //TAY,
        {0xa8},
        //TXA,
        {0x8a},
        //TYA,
        {0x98},
        //TSX,
        {0xba},
        //TXS,
        {0x9a},
        //TXY,
        {0x9b},
        //TYX
        {0xbb},
        //TCD,
        {0x5b},
        //TDC,
        {0x7b},
        //TCS,
        {0x1b},
        //TSC,
        {0x3b},
        //TRB,
        {
            -1, -1, 0x1c, -1,
            -1, -1, -1, 0x14,
        },
        //TSB,
        {
            -1, -1, 0x0c, -1,
            -1, -1, -1, 0x04,
        },
        //WAI,
        {0xcb},
        //WDM -- abs & immediate both allowed for convenience.
        {
            -1, 0x42, 0x42,
        },
        //XBA
        {0xeb},
        //XCE
        {0xfb},
    };
    // a table containing the size, machine, and any flags...
    private static final int[] __attributes = 
    {
        2 | m6502,                  // 00 brk #imm
        2 | m6502,                  // 01 ora (dp,x)
        2 | m65816,                 // 02 cop #imm
        2 | m65816,                 // 03 ora ,s
        2 | m65c02,                 // 04 tsb <dp
        2 | m6502,                  // 05 ora <dp
        2 | m6502,                  // 06 asl <dp
        2 | m65816,                 // 07 ora [dp]
        1 | m6502,                  // 08 php
        2 | m6502 | mM,             // 09 ora #imm
        1 | m6502,                  // 0a asl a
        1 | m65816,                 // 0b phd
        3 | m65c02,                 // 0c tsb |abs
        3 | m6502,                  // 0d ora |abs
        3 | m6502,                  // 0e asl |abs
        4 | m65816,                 // 0f ora >abs

        2 | m6502 | mBranch,        // 10 bpl
        2 | m6502,                  // 11 ora (dp),y
        2 | m65c02,                 // 12 ora (dp)
        2 | m65816,                 // 13 ora ,s,y
        2 | m65c02,                 // 14 trb <dp
        2 | m6502,                  // 15 ora <dp,x
        2 | m6502,                  // 16 asl <dp,x
        2 | m65816,                 // 17 ora [dp],y
        1 | m6502,                  // 18 clc
        3 | m6502,                  // 19 ora |abs,y
        1 | m65c02,                 // 1a inc a
        1 | m65816,                 // 1b tcs
        3 | m65c02,                 // 1c trb |abs
        3 | m6502,                  // 1d ora |abs,x
        3 | m6502,                  // 1e asl |abs,x
        4 | m65816,                 // 1f ora >abs,x
        
        3 | m6502,                  // 20 jsr |abs
        2 | m6502,                  // 21 and (dp,x)
        4 | m65816,                 // 22 jsl >abs
        2 | m65816,                 // 23 and ,s
        2 | m6502,                  // 24 bit <dp
        2 | m6502,                  // 25 and <dp
        2 | m6502,                  // 26 rol <dp
        2 | m65816,                 // 27 and [dp]
        1 | m6502,                  // 28 plp
        2 | m6502 | mM,             // 29 and #imm
        1 | m6502,                  // 2a rol a
        1 | m65816,                 // 2b pld
        3 | m6502,                  // 2c bit |abs
        3 | m6502,                  // 2d and |abs
        3 | m6502,                  // 2e rol |abs
        4 | m65816,                 // 2f and >abs
        
        2 | m6502 | mBranch,        // 30 bmi 
        2 | m6502,                  // 31 and (dp),y
        2 | m65c02,                 // 32 and (dp)
        2 | m65816,                 // 33 and ,s,y
        2 | m65c02,                 // 34 bit dp,x
        2 | m6502,                  // 35 and dp,x
        2 | m6502,                  // 36 rol <dp,x
        2 | m65816,                 // 37 and [dp],y
        1 | m6502,                  // 38 sec
        3 | m6502,                  // 39 and |abs,y
        1 | m65c02,                 // 3a dec a
        1 | m65816,                 // 3b tsc
        3 | m65c02,                 // 3c bits |abs,x
        3 | m6502,                  // 3d and |abs,x
        3 | m6502,                  // 3e rol |abs,x
        4 | m65816,                 // 3f and >abs,x
        
        1 | m6502,                  // 40 rti
        2 | m6502,                  // 41 eor (dp),x
        2 | m65816,                 // 42 wdm #imm
        2 | m65816,                 // 43 eor ,s
        3 | m65816,                 // 44 mvp x,x
        2 | m6502,                  // 45 eor dp
        2 | m6502,                  // 46 lsr dp
        2 | m65816,                 // 47 eor [dp],y
        1 | m6502,                  // 48 pha
        2 | m6502 | mM,             // 49 eor #imm
        1 | m6502,                  // 4a lsr a
        1 | m65816,                 // 4b phk
        3 | m6502,                  // 4c jmp |abs
        3 | m6502,                  // 4d eor |abs
        3 | m6502,                  // 4e lsr |abs
        4 | m65816,                 // 4f eor >abs      
        
        2 | m6502 | mBranch,        // 50 bvc
        2 | m6502,                  // 51 eor (dp),y
        2 | m65c02,                 // 52 eor (dp)
        2 | m65816,                 // 53 eor ,s,y
        3 | m65816,                 // 54 mvn x,x
        2 | m6502,                  // 55 eor dp,x
        2 | m6502,                  // 56 lsr dp,x
        2 | m65816,                 // 57 eor [dp],y
        1 | m6502,                  // 58 cli
        3 | m6502,                  // 59 eor |abs,y
        1 | m65c02,                 // 5a phy
        1 | m65816,                 // 5b tcd
        4 | m65816,                 // 5c jml >abs
        3 | m6502,                  // 5d eor |abs,x
        3 | m6502,                  // 5e lsr |abs,x
        4 | m65816,                 // 5f eor >abs,x

        1 | m6502,                  // 60 rts
        2 | m6502,                  // 61 adc (dp,x)
        3 | m65816,                 // 62 per |abs
        2 | m65816,                 // 63 adc ,s
        2 | m65c02,                 // 64 stz <dp
        2 | m6502,                  // 65 adc <dp
        2 | m6502,                  // 66 ror <dp
        2 | m65816,                 // 67 adc [dp]
        1 | m6502,                  // 68 pla
        2 | m6502 |mM,              // 69 adc #imm
        1 | m6502,                  // 6a ror a 
        1 | m65816,                 // 6b rtl
        3 | m6502,                  // 6c jmp (abs)
        3 | m6502,                  // 6d adc |abs
        3 | m6502,                  // 6e ror |abs
        4 | m65816,                 // 6f adc >abs
  
        2 | m6502 | mBranch,        // 70 bvs
        2 | m6502,                  // 71 adc (dp),y
        2 | m65c02,                 // 72 adc (dp)
        2 | m65816,                 // 73 adc ,s,y
        2 | m65c02,                 // 74 stz dp,x
        2 | m6502,                  // 75 adc dp,x
        2 | m6502,                  // 76 ror dp,x
        2 | m65816,                 // 77 adc [dp],y
        1 | m6502,                  // 78 sei
        3 | m6502,                  // 79 adc |abs,y
        1 | m65c02,                 // 7a ply
        1 | m65816,                 // 7b tdc
        3 | m65c02,                 // 7c jmp (abs,x)
        3 | m6502,                  // 7d adc |abs,x
        3 | m6502,                  // 7e ror |abs,x
        4 | m65816,                 // 7f adc >abs,x
        
        2 | m65c02 | mBranch,       // 80 bra 
        2 | m6502,                  // 81 sta (dp,x)
        3 | m65816 | mBranch,       // 82 brl |abs
        2 | m65816,                 // 83 sta ,s
        2 | m6502,                  // 84 sty <dp
        2 | m6502,                  // 85 sta <dp
        2 | m6502,                  // 86 stx <dp
        2 | m65816,                 // 87 sta [dp]
        1 | m6502,                  // 88 dey
        2 | m65c02 | mM,            // 89 bit #imm
        1 | m6502,                  // 8a txa
        1 | m65816,                 // 8b phb
        3 | m6502,                  // 8c sty |abs
        3 | m6502,                  // 8d sta |abs
        3 | m6502,                  // 8e stx |abs
        4 | m65816,                 // 8f sta >abs
        
        2 | m6502 | mBranch,        // 90 bcc
        2 | m6502,                  // 91 sta (dp),y
        2 | m65c02,                 // 92 sta (dp)
        2 | m65816,                 // 93 sta ,s,y
        2 | m6502,                  // 94 sty dp,x
        2 | m6502,                  // 95 sta dp,x
        2 | m6502,                  // 96 stx dp,y
        2 | m65816,                 // 97 sta [dp],y
        1 | m6502,                  // 98 tya
        3 | m6502,                  // 99 sta |abs,y
        1 | m6502,                  // 9a txs
        1 | m65816,                 // 9b txy
        3 | m65c02,                 // 9c stz |abs
        3 | m6502,                  // 9d sta |abs,x
        3 | m65c02,                 // 9e stz |abs,x
        4 | m65816,                 // 9f sta >abs,x
        
        2 | m6502 | mX,             // a0 ldy #imm
        2 | m6502,                  // a1 lda (dp,x)
        2 | m6502 | mX,             // a2 ldx #imm
        2 | m65816,                 // a3 lda ,s
        2 | m6502,                  // a4 ldy <dp
        2 | m6502,                  // a5 lda <dp
        2 | m6502,                  // a6 ldx <dp
        2 | m65816,                 // a7 lda [dp]
        1 | m6502,                  // a8 tay
        2 | m6502 | mM,             // a9 lda #imm
        1 | m6502,                  // aa tax
        1 | m65816,                 // ab plb
        3 | m6502,                  // ac ldy |abs
        3 | m6502,                  // ad lda |abs
        3 | m6502,                  // ae ldx |abs
        4 | m65816,                 // af lda >abs   
        
        2 | m6502 | mBranch,        // b0 bcs
        2 | m6502,                  // b1 lda (dp),y
        2 | m65c02,                 // b2 lda (dp)
        2 | m65816,                 // b3 lda ,s,y
        2 | m6502,                  // b4 ldy <dp,x
        2 | m6502,                  // b5 lda <dp,x
        2 | m6502,                  // b6 ldx <dp,y
        2 | m65816,                 // b7 lda [dp],y
        1 | m6502,                  // b8 clv
        3 | m6502,                  // b9 lda |abs,y
        1 | m6502,                  // ba tsx
        1 | m65816,                 // bb tyx
        3 | m6502,                  // bc ldy |abs,x
        3 | m6502,                  // bd lda |abs,x
        3 | m6502,                  // be ldx |abs,y
        4 | m65816,                 // bf lda >abs,x
        
        2 | m6502 | mX,             // c0 cpy #imm
        2 | m6502,                  // c1 cmp (dp,x)
        2 | m65816,                 // c2 rep #
        2 | m65816,                 // c3 cmp ,s
        2 | m6502,                  // c4 cpy <dp
        2 | m6502,                  // c5 cmp <dp
        2 | m6502,                  // c6 dec <dp
        2 | m65816,                 // c7 cmp [dp]
        1 | m6502,                  // c8 iny
        2 | m6502 | mM,             // c9 cmp #imm
        1 | m6502,                  // ca dex
        1 | m65816,                 // cb WAI
        3 | m6502,                  // cc cpy |abs
        3 | m6502,                  // cd cmp |abs
        3 | m6502,                  // ce dec |abs
        4 | m65816,                 // cf cmp >abs
        
        2 | m6502 | mBranch,        // d0 bne
        2 | m6502,                  // d1 cmp (dp),y
        2 | m65c02,                 // d2 cmp (dp)
        2 | m65816,                 // d3 cmp ,s,y
        2 | m65816,                 // d4 pei (dp)
        2 | m6502,                  // d5 cmp dp,x
        2 | m6502,                  // d6 dec dp,x
        2 | m65816,                 // d7 cmp [dp],y
        1 | m6502,                  // d8 cld
        3 | m6502,                  // d9 cmp |abs,y
        1 | m65c02,                 // da phx
        1 | m65816,                 // db stp
        3 | m65816,                 // dc jml [abs]
        3 | m6502,                  // dd cmp |abs,x
        3 | m6502,                  // de dec |abs,x
        4 | m65816,                 // df cmp >abs,x
        
        2 | m6502 | mX,             // e0 cpx #imm
        2 | m6502,                  // e1 sbc (dp,x)
        2 | m65816,                 // e2 sep #imm
        2 | m65816,                 // e3 sbc ,s
        2 | m6502,                  // e4 cpx <dp
        2 | m6502,                  // e5 sbc <dp
        2 | m6502,                  // e6 inc <dp
        2 | m65816,                 // e7 sbc [dp]
        1 | m6502,                  // e8 inx
        2 | m6502 | mM,             // e9 sbc #imm
        1 | m6502,                  // ea nop
        1 | m65816,                 // eb xba
        3 | m6502,                  // ec cpx |abs
        3 | m6502,                  // ed abc |abs
        3 | m6502,                  // ee inc |abs
        4 | m65816,                 // ef sbc >abs
        
        2 | m6502 | mBranch,        // f0 beq
        2 | m6502,                  // f1 sbc (dp),y
        2 | m65c02,                 // f2 sbc (dp)
        2 | m65816,                 // f3 sbc ,s,y
        3 | m65816,                 // f4 pea |abs
        2 | m6502,                  // f5 sbc dp,x
        2 | m6502,                  // f6 inc dp,x
        2 | m65816,                 // f7 sbc [dp],y
        1 | m6502,                  // f8 sed
        3 | m6502,                  // f9 sbc |abs,y
        1 | m65c02,                 // fa plx
        1 | m65816,                 // fb xce
        3 | m65816,                 // fc jsr (abs)
        3 | m6502,                  // fd sbc |abs,x
        3 | m6502,                  // fe inc |abs,x
        4 | m65816,                 // ff sbc >abs,x      
        
    };

    //first byte: base time
    // 2nd byte: + if 16 bit akku
    // 3rd byte: + if 16 bit x/y
    private static final int[] __timing = 
    {
        7,                          // 00 brk #imm
        6 | 0x0100,                 // 01 ora (dp,x)
        7,                          // 02 cop #imm
        4 | 0x0100,                 // 03 ora ,s
        5 | 0x0200,                 // 04 tsb <dp
        3 | 0x0100,                 // 05 ora <dp
        5 | 0x0200,                 // 06 asl <dp
        6 | 0x0100,                 // 07 ora [dp]
        3,                          // 08 php
        2 | 0x0100,                 // 09 ora #imm
        2,                          // 0a asl a
        4,                          // 0b phd
        6 | 0x0200,                 // 0c tsb |abs
        4 | 0x0100,                 // 0d ora |abs
        6 | 0x0200,                 // 0e asl |abs
        5 | 0x0100,                 // 0f ora >abs

        2,                          // 10 bpl
        5 | 0x0100,                 // 11 ora (dp),y
        5 | 0x0100,                 // 12 ora (dp)
        7 | 0x0100,                 // 13 ora ,s,y
        5 | 0x0200,                 // 14 trb <dp
        4 | 0x0100,                 // 15 ora <dp,x
        6 | 0x0200,                 // 16 asl <dp,x
        6 | 0x0100,                 // 17 ora [dp],y
        2,                          // 18 clc
        4 | 0x0100,                 // 19 ora |abs,y
        2,                          // 1a inc a
        2,                          // 1b tcs
        6 | 0x0200,                 // 1c trb |abs
        4 | 0x0100,                 // 1d ora |abs,x
        7 | 0x0200,                 // 1e asl |abs,x
        5 | 0x0100,                 // 1f ora >abs,x
        
        6,                          // 20 jsr |abs
        6 | 0x0100,                 // 21 and (dp,x)
        8,                          // 22 jsl >abs
        4 | 0x0100,                 // 23 and ,s
        3 | 0x0100,                 // 24 bit <dp
        3 | 0x0100,                 // 25 and <dp
        5 | 0x0200,                 // 26 rol <dp
        6 | 0x0100,                 // 27 and [dp]
        4,                          // 28 plp
        2 | 0x0100,                 // 29 and #imm
        2,                          // 2a rol a
        5,                          // 2b pld
        4 | 0x0100,                 // 2c bit |abs
        4 | 0x0100,                 // 2d and |abs
        6 | 0x0200,                 // 2e rol |abs
        5 | 0x0100,                 // 2f and >abs
        
        2,                          // 30 bmi 
        5 | 0x0100,                 // 31 and (dp),y
        5 | 0x0100,                 // 32 and (dp)
        7 | 0x0100,                 // 33 and ,s,y
        4 | 0x0100,                 // 34 bit dp,x
        4 | 0x0100,                 // 35 and dp,x
        6 | 0x0200,                 // 36 rol <dp,x
        6 | 0x0100,                 // 37 and [dp],y
        2,                          // 38 sec
        4 | 0x0100,                 // 39 and |abs,y
        2,                          // 3a dec a
        2,                          // 3b tsc
        4 | 0x0100,                 // 3c bits |abs,x
        4 | 0x0100,                 // 3d and |abs,x
        7 | 0x0200,                 // 3e rol |abs,x
        5 | 0x0100,                 // 3f and >abs,x
        
        6,                          // 40 rti
        6 | 0x0100,                 // 41 eor (dp),x
        0,                          // 42 wdm #imm
        4 | 0x0100,                 // 43 eor ,s
        0,                          // 44 mvp x,x
        3 | 0x0100,                 // 45 eor dp
        5 | 0x0200,                 // 46 lsr dp
        6 | 0x0100,                 // 47 eor [dp],y
        3 | 0x0100,                 // 48 pha
        2 | 0x0100,                 // 49 eor #imm
        2,                          // 4a lsr a
        3,                          // 4b phk
        3,                          // 4c jmp |abs
        4 | 0x0100,                 // 4d eor |abs
        6 | 0x0200,                 // 4e lsr |abs
        5 | 0x0100,                 // 4f eor >abs      
        
        2,                          // 50 bvc
        5 | 0x0100,                 // 51 eor (dp),y
        5 | 0x0100,                 // 52 eor (dp)
        7 | 0x0100,                 // 53 eor ,s,y
        0,                          // 54 mvn x,x
        4 | 0x0100,                 // 55 eor dp,x
        6 | 0x0200,                 // 56 lsr dp,x
        6 | 0x0100,                 // 57 eor [dp],y
        2,                          // 58 cli
        4 | 0x0100,                 // 59 eor |abs,y
        3 | 0x010000,               // 5a phy
        2,                          // 5b tcd
        4,                          // 5c jml >abs
        4 | 0x0100,                 // 5d eor |abs,x
        7 | 0x0200,                 // 5e lsr |abs,x
        5 | 0x0100,                 // 5f eor >abs,x

        6,                          // 60 rts
        6 | 0x0100,                 // 61 adc (dp,x)
        6,                          // 62 per |abs
        4 | 0x0100,                 // 63 adc ,s
        3 | 0x0100,                 // 64 stz <dp
        3 | 0x0100,                 // 65 adc <dp
        5 | 0x0100,                 // 66 ror <dp
        6 | 0x0100,                 // 67 adc [dp]
        4 | 0x0100,                 // 68 pla
        2 | 0x0100,                 // 69 adc #imm
        2,                          // 6a ror a 
        6,                          // 6b rtl
        5,                          // 6c jmp (abs)
        4 | 0x0100,                 // 6d adc |abs
        6 | 0x0200,                 // 6e ror |abs
        5 | 0x0100,                 // 6f adc >abs
  
        2,                          // 70 bvs
        5 | 0x0100,                 // 71 adc (dp),y
        5 | 0x0100,                 // 72 adc (dp)
        7 | 0x0100,                 // 73 adc ,s,y
        4 | 0x0100,                 // 74 stz dp,x
        4 | 0x0100,                 // 75 adc dp,x
        6 | 0x0200,                 // 76 ror dp,x
        6 | 0x0100,                 // 77 adc [dp],y
        2,                          // 78 sei
        4 | 0x0100,                 // 79 adc |abs,y
        4 | 0x010000,               // 7a ply
        2,                          // 7b tdc
        6,                          // 7c jmp (abs,x)
        4 | 0x0100,                 // 7d adc |abs,x
        7 | 0x0200,                 // 7e ror |abs,x
        5 | 0x0100,                 // 7f adc >abs,x
        
        3,                          // 80 bra 
        6 | 0x0100,                 // 81 sta (dp,x)
        4,                          // 82 brl |abs
        4 | 0x0100,                 // 83 sta ,s
        3 | 0x010000,               // 84 sty <dp
        3 | 0x0100,                 // 85 sta <dp
        3 | 0x010000,               // 86 stx <dp
        6 | 0x0100,                 // 87 sta [dp]
        2,                          // 88 dey
        2 | 0x0100,                 // 89 bit #imm
        2,                          // 8a txa
        3,                          // 8b phb
        4 | 0x010000,               // 8c sty |abs
        4 | 0x0100,                 // 8d sta |abs
        4 | 0x010000,               // 8e stx |abs
        5 | 0x0100,                 // 8f sta >abs
        
        2,                          // 90 bcc
        6 | 0x0100,                 // 91 sta (dp),y
        5 | 0x0100,                 // 92 sta (dp)
        7 | 0x0100,                 // 93 sta ,s,y
        4 | 0x010000,               // 94 sty dp,x
        4 | 0x0100,                 // 95 sta dp,x
        4 | 0x010000,               // 96 stx dp,y
        6 | 0x0100,                 // 97 sta [dp],y
        2,                          // 98 tya
        5 | 0x0100,                 // 99 sta |abs,y
        2,                          // 9a txs
        2,                          // 9b txy
        4 | 0x0100,                 // 9c stz |abs
        5 | 0x0100,                 // 9d sta |abs,x
        5 | 0x0100,                 // 9e stz |abs,x
        5 | 0x0100,                 // 9f sta >abs,x
        
        2 | 0x010000,               // a0 ldy #imm
        6 | 0x0100,                 // a1 lda (dp,x)
        2 | 0x010000,               // a2 ldx #imm
        4 | 0x0100,                 // a3 lda ,s
        3 | 0x010000,               // a4 ldy <dp
        3 | 0x0100,                 // a5 lda <dp
        3 | 0x010000,               // a6 ldx <dp
        6 | 0x0100,                 // a7 lda [dp]
        2,                          // a8 tay
        2 | 0x0100,                 // a9 lda #imm
        2,                          // aa tax
        4,                          // ab plb
        4 | 0x010000,               // ac ldy |abs
        4 | 0x0100,                 // ad lda |abs
        4 | 0x010000,               // ae ldx |abs
        5 | 0x0100,                 // af lda >abs   
        
        2,                          // b0 bcs
        5 | 0x0100,                 // b1 lda (dp),y
        5 | 0x0100,                 // b2 lda (dp)
        7 | 0x0100,                 // b3 lda ,s,y
        4 | 0x010000,               // b4 ldy <dp,x
        4 | 0x0100,                 // b5 lda <dp,x
        4 | 0x010000,               // b6 ldx <dp,y
        6 | 0x0100,                 // b7 lda [dp],y
        2,                          // b8 clv
        4 | 0x0100,                 // b9 lda |abs,y
        2,                          // ba tsx
        2,                          // bb tyx
        4 | 0x010000,               // bc ldy |abs,x
        4 | 0x0100,                 // bd lda |abs,x
        4 | 0x010000,               // be ldx |abs,y
        5 | 0x0100,                 // bf lda >abs,x
        
        2 | 0x010000,               // c0 cpy #imm
        6 | 0x0100,                 // c1 cmp (dp,x)
        3,                          // c2 rep #
        4 | 0x0100,                 // c3 cmp ,s
        3 | 0x010000,               // c4 cpy <dp
        3 | 0x0100,                 // c5 cmp <dp
        5 | 0x0200,                 // c6 dec <dp
        6 | 0x0100,                 // c7 cmp [dp]
        2,                          // c8 iny
        2 | 0x0100,                 // c9 cmp #imm
        2,                          // ca dex
        3,                          // cb WAI
        4 | 0x010000,               // cc cpy |abs
        4 | 0x0100,                 // cd cmp |abs
        6 | 0x0200,                 // ce dec |abs
        5 | 0x0100,                 // cf cmp >abs
        
        2,                          // d0 bne
        5 | 0x0100,                 // d1 cmp (dp),y
        5 | 0x0100,                 // d2 cmp (dp)
        7 | 0x0100,                 // d3 cmp ,s,y
        6,                          // d4 pei (dp)
        4 | 0x0100,                 // d5 cmp dp,x
        6 | 0x0200,                 // d6 dec dp,x
        6 | 0x0100,                 // d7 cmp [dp],y
        2,                          // d8 cld
        4 | 0x0100,                 // d9 cmp |abs,y
        3 | 0x010000,               // da phx
        3,                          // db stp
        6,                          // dc jml [abs]
        4 | 0x0100,                 // dd cmp |abs,x
        7 | 0x0200,                 // de dec |abs,x
        5 | 0x0100,                 // df cmp >abs,x
        
        2 | 0x010000,               // e0 cpx #imm
        6 | 0x0100,                 // e1 sbc (dp,x)
        3,                          // e2 sep #imm
        4 | 0x0100,                 // e3 sbc ,s
        3 | 0x010000,               // e4 cpx <dp
        3 | 0x0100,                 // e5 sbc <dp
        5 | 0x0200,                 // e6 inc <dp
        6 | 0x0100,                 // e7 sbc [dp]
        2,                          // e8 inx
        2 | 0x0100,                 // e9 sbc #imm
        2,                          // ea nop
        3,                          // eb xba
        4 | 0x010000,               // ec cpx |abs
        4 | 0x0100,                 // ed abc |abs
        6 | 0x0200,                 // ee inc |abs
        5 | 0x0100,                 // ef sbc >abs
        
        2,                          // f0 beq
        5 | 0x0100,                 // f1 sbc (dp),y
        5 | 0x0100,                 // f2 sbc (dp)
        7 | 0x0100,                 // f3 sbc ,s,y
        5,                          // f4 pea |abs
        4 | 0x0100,                 // f5 sbc dp,x
        6 | 0x0200,                 // f6 inc dp,x
        6 | 0x0100,                 // f7 sbc [dp],y
        2,                          // f8 sed
        4 | 0x0100,                 // f9 sbc |abs,y
        4 | 0x010000,               // fa plx
        2,                          // fb xce
        8,                          // fc jsr (abs)
        4 | 0x0100,                 // fd sbc |abs,x
        7 | 0x0200,                 // fe inc |abs,x
        5 | 0x0100,                 // ff sbc >abs,x      
        
    };    
    
       


}
