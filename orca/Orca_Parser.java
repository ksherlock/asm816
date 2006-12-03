package orca;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;


import expression.*;

import asm816.AddressMode;
import asm816.AsmException;
import asm816.Error;
import asm816.INSTR;
import asm816.JunkPile;
import asm816.Lexer;
import asm816.Parser;
import asm816.SymbolTable;
import asm816.Token;
import asm816.ctype;

import omf.*;

/*
 * Created on Feb 24, 2006
 * Feb 24, 2006 11:32:44 PM
 */


public class Orca_Parser extends Parser
{
    /*
     * start the parsing.
     */
    

    private JunkPile fData;
    private OMF_Segment fSegment;

    private int fAlign;

    private boolean fMSB;
    
    //private HashMap<String, Expression> fLocals;
    //private HashMap<String, Expression> fGlobals;
    
    private SymbolTable fSymbols;
    
    private FileOutputStream fFile;
   

    public Orca_Parser(FileOutputStream outfile)
    {
        super();
        
        fSegment = null;
        //fLocals = null;
        fData = null;

        //fGlobals = new HashMap<String, Expression>();
        
        fSymbols = new SymbolTable();
        

        fPC = 0;
        fAlign = 0;
        fCase = false;

        fMSB = false;
        
        fFile = outfile;
               

    }
    
    
    protected void AddDirectives()
    {
        for (Orca_Directive i : Orca_Directive.values())
            fDirectives.put(i.name(), i);
    }
    
    protected void AddOpcodes()
    {
        super.AddOpcodes();

        // synonyms
        fOpcodes.put("BLT", INSTR.BCC);
        fOpcodes.put("BGE", INSTR.BCS);
        fOpcodes.put("CPA", INSTR.CMP);
    }
 
    
    
    
    
    
    
    
    private void ParseFile(String filename)
    {
        // TODO -- check -I directories for the file
        File f = new File(filename);
        try
        {
            FileInputStream stream = new FileInputStream(f);
            Orca_Lexer lex = new Orca_Lexer(stream);
            Parse(lex);
        }
        catch (FileNotFoundException e)
        {
            System.err.print("Unable to open file " + filename);
        }
    }
    
    public void Parse(Orca_Lexer lex)
    {
        Token t;
        String lab;
        
        lex.SetCase(fCase);
        
       for(;;)
        {           
            try
            {
                t = lex.NextToken();
                lab = null;
                
                switch (t.Type()) {
                case Token.EOF:
                    return;
                case Token.EOL:
                    continue;
                case Token.SPACE:
                    break;
                case Token.SYMBOL:
                    lab = t.toString();
                    
                    lex.Expect(Token.SPACE);
                    break;
                default:
                    throw new AsmException(Error.E_UNEXPECTED, t);
                }
                
                t = lex.Expect(Token.SYMBOL);
                String s = t.toString().toUpperCase();
                
                Orca_Directive dir = (Orca_Directive)fDirectives.get(s);
                if (dir == null)
                    throw new AsmException(Error.E_UNEXPECTED, t);
 
                boolean tf = ParseDirective(lab, lex, dir);
                if (!tf)
                    throw new AsmException(Error.E_UNEXPECTED, t);
                
            }
            catch (AsmException e)
            {
                // TODO -- check if fatal, update error count.
                e.print(System.err);
                lex.SkipLine();
            }

        }

    }



    /*
     * parse a code section
     */
    @SuppressWarnings("unchecked")
    private void ParseSegment(Orca_Lexer lex, boolean isData)
            throws AsmException
    {
        fSymbols.Push();        
        fData = new JunkPile();

        if (fMachine == INSTR.m65816)
        {
            fM = true;
            fX = true;
        }
        else
        {
            fM = false;
            fX = false;
        }
        // current program counter
        fPC = 0;
        fMSB = false;

        fSymbols.Put(fSegment.SegmentName(), new RelExpression(0));

        while(ParseLine(lex) == true) /* do nothing */;
        
        // now finish up.
        // iterate through fData
        // reducing expressions,
        // merging DS
        // merging byte[]s
        
        Reduce();

        fSymbols.Pop();
 
        
    }
    
    protected void Reduce()
    {
        ArrayList ops = fData.GetArrayList();
        fData = new JunkPile();

        for(Object op : ops)
        {
            if (op instanceof Expression)
            {
                Expression e = (Expression)op;
               // reduce
                //try
                //{
                    e.Simplify(fSymbols, true);
                //}
                /*
                catch (AsmException err)
                {
                    // TODO Auto-generated catch block
                    err.printStackTrace();
                }
                */
                Integer v = e.Value();
                if (v != null)
                {                        
                    // todo -- check for dp overflow.
                    int value = v.intValue();
                    switch(e.Size())
                    {
                    case 0:
                        break;
                    case 1:
                        fData.add8(value, 0);
                        break;
                    case 2:
                        fData.add16(value, 0);
                        break;
                    case 3:
                        fData.add24(value, 0);
                        break;
                    case 4:
                        fData.add32(value, 0);
                        break;
                    }
                }
                else
                {
                    fData.add(e);
                }
            }
            else if (op instanceof OMF_Opcode)
                fData.add((OMF_Opcode)op);
        }

        // Now, go through a final time 
        // save to disk.
        ops = fData.GetArrayList();

        for (Object op: ops)
        {
            if (op instanceof Expression)
            {
                Expression e = (Expression)op;
                op = e.toOpcode(fSegment.SegmentName());
            }
            fSegment.AddOpcode((OMF_Opcode)op);
        }
        fSegment.AddOpcode(new OMF_Eof());
        fSegment.Save(fFile);
    }

    /*
     * returns false once an END token is processed.
     */
    @SuppressWarnings("unchecked")
    private boolean ParseLine(Orca_Lexer lex)
    {
        String lab = null;
        String s = null;

        try
        {

            fPC = fData.Size();
            
            Token t = lex.NextToken();

            if (t.Type() == Token.EOL)
                return true;  
            
            if (t.Type() == Token.SYMBOL)
            {
                lab = t.toString();
                t = lex.NextToken();
            }
            t.Expect(Token.SPACE);

            t = lex.Expect(Token.SYMBOL);

            s = t.toString().toUpperCase();

            Orca_Directive dir = (Orca_Directive)fDirectives.get(s);
            if (dir != null)
            {
                boolean tf = ParseDirective(lab, lex, dir);
                if (!tf)
                    throw new AsmException(Error.E_UNEXPECTED, t);
                if (dir == Orca_Directive.END) return false; // all done.
                
                return true;
            }
                

            INSTR instr = fOpcodes.get(s);
            if (instr != null)
            {
                ParseInstruction(lab, lex, instr);
                
                return true;
            }
            
            // TODO -- macro support.
            
            throw new AsmException(Error.E_UNEXPECTED, t);

        }
        catch (AsmException Error)
        {
            // skip to the next line.
            Error.print(System.err);
            lex.SkipLine();
        }

        return true;
    }
    
    /*
     * 
     * lab is the label, or null.
     * lex is the lexer
     * d is the directive
     * 
     * returns false if the directive is invalid for this segment type. 
     */
    private boolean ParseDirective(String lab, Orca_Lexer lex, Orca_Directive d) throws AsmException
    {
        __Expression e = null;
        int segkind = -1;
        boolean inSeg = false;
        
        if (fSegment != null)
        {
            inSeg = true;
            segkind = fSegment.Kind();            
        }
        
        switch(d)
        {
        case START:
        case DATA:
        case PRIVATE:
        case PRIVDATA:
            
            if (inSeg)
                return false;
            {
                Token t;
                String s;
                // need to check for a possible loadname.
                t = lex.Expect(Token.EOL, Token.SPACE);
                if (t.Type() == Token.EOL)
                    s = null;
                else
                {
                    t = lex.Expect(Token.SYMBOL);
                    s = t.toString();
                    t.Expect(Token.EOL);
                }
                
                int attr = 0;
                int type = 0;

                switch (d) {
                case PRIVATE:
                    attr = OMF.KIND_PRIVATE;
                case START:
                    type = OMF.KIND_CODE;
                    break;
                case PRIVDATA:
                    attr = OMF.KIND_PRIVATE;
                case DATA:
                    type = OMF.KIND_DATA;
                    break;
                }
                             
 
                fSegment = new OMF_Segment();
                fSegment.SetSegmentName(lab);
                if (s != null)
                    fSegment.SetLoadName(s);
                fSegment.SetKind(type);
                fSegment.SetAttributes(attr);
                fSegment.SetAlignment(fAlign);

                ParseSegment(lex, type == OMF.KIND_DATA);
                fSegment = null;
                fAlign = 0;                
                
                // prevent double dipping.
                lab = null;
            }
            break;
        
        
        case ALIGN:
            /*
             * The ALIGN directive has two distinct uses, depending on where in the 
             * program the directive occurs. If it appears before a START, PRIVATE, 
             * DATA or PRIVDATA directive, it tells the link editor to align the 
             * segment to a byte boundary divisible by the absolute number in the 
             * operand of the ALIGN directive. This number must be a power of two.
             * 
             * Within a segment, ALIGN inserts enough zeros to force the next byte 
             * to fall at the indicated alignment. This is done at assembly time, 
             * so the zeros show up in the program listing. If align is used in a 
             * subroutine, it must also have been used before the segment, and the 
             * internal align must be to a boundary that is less than or equal to 
             * the external align. 
             */
            {
                lex.Expect(Token.SPACE);
                
                int i, v;
                
                i = v = ParseIntExpression(lex);
                lex.Expect(Token.EOL);

                while ((i & 0x01) == 0)
                {
                    i = i >> 1;
                }
                if (i != 1)
                {
                    throw new AsmException(Error.E_ALIGN, lex);
                }
                if (inSeg)
                {
                    // ORCA/M uses a DS record rather than an OMF_ALIGN record.
                    // align must be <= segment align.
                    if (v > fAlign)
                        throw new AsmException(Error.E_ALIGN, lex);
                    
                    // insert a DS record
                    int newpc = (fPC + v) & ~(v - 1);
                    if (newpc > fPC)
                        fData.add(new OMF_DS(newpc - fPC));
                }
                else
                {
                    fAlign = v;
                }
            }
            break;
        
        case ANOP:
            if (!inSeg) return false;
            lex.Expect(Token.EOL);
            break;
        
        case CASE:
            fCase = ParseOnOff(lex, false);
            lex.SetCase(fCase);
            break;
            
        case COPY:
            {
                Token t;
                String name;
                lex.Expect(Token.SPACE);
                t = lex.Expect(Token.STRING);
                lex.Expect(Token.EOL);
                name = t.toString();
                this.ParseFile(name);
            }
            break;
            
        case DC:
            if (!inSeg) return false;
            ParseData(lex);
            break;
            
        case DS:
            {
                if (!inSeg) return false;
                
                lex.Expect(Token.SPACE);
                int i = ParseIntExpression(lex);
                lex.Expect(Token.EOL);
 
                fData.add(new OMF_DS(i));
            }
            break;
            
        case END:
            if (!inSeg) return false;
            break;
            
        case ENTRY:
            /*
             * create an OMF_GLOBAL record 
             */
            if (!inSeg) return false;
            
            lex.Expect(Token.EOL);
            if (lab != null)
            {
                fData.add(new OMF_Local(OMF.OMF_GLOBAL, lab, 0, 'N',
                        false));
            }
            break;
        
        case EQU:
            if (!inSeg) return false;
        case GEQU:
            {
                lex.Expect(Token.SPACE);
                
                e = ParseExpression(lex);
                
                lex.Expect(Token.EOL);
                
                if (lab == null) break; // don't bother.
                
                
                fSymbols.Put(lab, e); 
                
                if (inSeg && (segkind == OMF.KIND_DATA 
                        || d == Orca_Directive.GEQU))               
                {
                    Expression ex = new Expression(e);
                    ex.SetSize(2);
                    ex.SetType(d == Orca_Directive.EQU ? 
                               OMF.OMF_EQU : OMF.OMF_GEQU);
                    
                    ex.SetName(lab);
                    fData.add(ex);
               }
            }
            
            e = null;
            lab = null;
            break;
            

        case KIND:
            {
                if (!inSeg) return false;
                
                lex.Expect(Token.SPACE);
                
                int i = ParseIntExpression(lex);
                
                fSegment.SetKind(i);
                fSegment.SetAttributes(i);
            }
            break;
            
        case LONGA:
            if (fMachine != INSTR.m65816)
                throw new AsmException(Error.E_MACHINE);
            fM = ParseOnOff(lex, true);
            break;

        case LONGI:
            if (fMachine != INSTR.m65816)
                throw new AsmException(Error.E_MACHINE);
            fX = ParseOnOff(lex, true);
            break;
            
        case MSB:
            fMSB = ParseOnOff(lex, false);
            break;            
            
            
        case M6502:
            if (inSeg) return false;
            fMachine = INSTR.m6502;
            fM = false;
            fX = false;
            lex.Expect(Token.EOL);
            break;
            
        case M65C02:
            if (inSeg) return false;
            fMachine = INSTR.m65c02;
            fM = false;
            fX = false;
            lex.Expect(Token.EOL);
            break;
            
        case M65816:
            if (inSeg) return false;
            fMachine = INSTR.m65816;
            fM = true;
            fX = true;
            lex.Expect(Token.EOL);
            break;
            
        case USING:
            if (!inSeg) return false;
            {
                Token t;
                String s;
                lex.Expect(Token.SPACE);
                t = lex.Expect(Token.SYMBOL);
                lex.Expect(Token.EOL);
                
                s = t.toString();
                fData.add(new OMF_Using(s));
            }
            break;
            
        case RENAME:
            if (inSeg) return false;
            {
                String sold, snew;
                Token t;
                lex.Expect(Token.SPACE);
                t = lex.Expect(Token.SYMBOL);
                sold = t.toString();
                lex.Expect(',');
                t = lex.Expect(Token.SYMBOL);
                snew = t.toString();
                lex.Expect(Token.EOL);
                //TODO -- check if new/old are valid?
                INSTR instr = fOpcodes.remove(sold);
                fOpcodes.put(snew, instr);
            }
            break;
                  
         /*
         * unsupported / not yet implemented
         */    
        case MCOPY:
        case MDROP:
        case MLOAD:
        
            
        case ABSADDR:
        case APPEND:
        case CODECHK:
        case DATACHK:
        case DYNCHK:
        case EJECT:
        case ERR:
        case EXPAND:
        case GEN:
        case IEEE:
        case INSTIME:
        case KEEP:
        case LIST:
        case MEM:
        case MERR:
        case NUMSEX:
        case OBJ:
        case OBJCASE:
        case OBJEND:
        case ORG:
        case PRINTER:
        case SETCOM:
        case SYMBOL:
        case TITLE:
        case TRACE:
    
        default: 
            System.err.println("Unimplemented directive: " + d.toString());
                   
            return false;
        }
        
        if (inSeg && lab != null)
        {
            if (e == null) e = new RelExpression(fPC);
            fSymbols.Put(lab, e);
        }
        
        return true;
    }
 
    protected boolean ParseInstruction(String lab, Lexer lex, INSTR instr) throws AsmException
    {
        int opcode = 0x00;
        int size;

        boolean check_a = false;
        
        // special case mvn/mvp
        switch (instr)
        {
        case MVN:
        case MVP:
            {   
                __Expression e1, e2;
                Expression ex1, ex2;
                lex.Expect(Token.SPACE);
                e1 = ExpressionParser.Parse(lex, fPC);
                lex.Expect(',');
                e2 = ExpressionParser.Parse(lex, fPC);
    
                e1 = e1.Simplify(fSymbols, false);
                e2 = e2.Simplify(fSymbols, false);
                switch (instr)
                {
                case MVN:
                    opcode = 0x54;
                    break;
                case MVP:
                    opcode = 0x44;
                    break;
                }
                fData.add8(opcode, 0);
                
                ex1 = new Expression(e1);
                ex2 = new Expression(e2);
                ex1.SetSize(1);
                ex2.SetSize(1);
                
                fData.add(ex1);
                fData.add(ex2);
                
                break;
            }
            
            /*
             *TODO -- perhaps these should be macros ?
             * special case -- a --> implied
             */
        case ASL:
        case DEC:
        case INC:
        case LSR:
        case ROL:
        case ROR:
            check_a = true;
            
        default:
            
            operand oper = ParseOperand(lex);
            AddressMode mode = oper.mode;
            __Expression e = oper.expression;
            if (e != null)
                e = e.Simplify(fSymbols, false);
            
            if (check_a)
            {
                String oplab = e.toString();
                if (oplab != null 
                        && oplab.compareToIgnoreCase("a") == 0)
                    mode = AddressMode.IMPLIED;
            }
            if (mode == AddressMode.ASSUMED_ABS || mode == AddressMode.ASSUMED_ABS_X || mode == AddressMode.ASSUMED_ABS_Y)
            {
                //...
                mode = AddressMode.ABS;
            }
            opcode = instr.opcode(mode, fMachine);
            if (opcode == -1)
            {
                throw new AsmException(Error.E_OPCODE);
            }
            size = INSTR.Size(opcode, fM, fX);

            fData.add8(opcode, 0);
            if (size > 1)
            {
                Expression ex = new Expression(e);
                ex.SetSize(size - 1);
                //TODO -- set type.
                if (INSTR.isBranch(opcode))
                    ex.SetType(OMF.OMF_RELEXPR);
                
                fData.add(ex);
            }

        } /* switch (instr) */
        
        if (lab != null)
        {
            fSymbols.Put(lab, new RelExpression(fPC));
        }
        
        return true;
    }
    
    
    /*
     * expect: EOL <implicit on> SPACE ON EOL <explicit on> SPACE OFF EOL
     * <explicit off>
     */
    private boolean ParseOnOff(Orca_Lexer lex, boolean blank) throws AsmException
    {
        Token t;

        t = lex.NextToken();

        if (t.Type() == Token.EOL)
            return blank;

        t.Expect(Token.SPACE);
        t = lex.Expect(Token.SYMBOL);
        String s = t.toString().toLowerCase();

        boolean v;
        if (s.compareTo("on") == 0)
            v = true;
        else if (s.compareTo("off") == 0)
            v = false;
        else
            throw new AsmException(Error.E_UNEXPECTED, t);

        lex.Expect(Token.EOL);
        return v;
        
    }

    
    
    /*
     * called after a DC opcode.
     * integer:
     * [<integer>]? 'i' ['1' - '8']? ['^' '>' '<'] <string> -- integer
     * ^>< only valid if 1 byte integer.
     * 
     * [<integer>]? 'a' ['1' - '4']? <string> -- address
     * 'R' <string> -- reference
     * 'S' ['1' - '4']? <string> -- soft reference
     * [<integer>]? h <string> -- hex
     * [<integer>]? b <string> -- binary 
     * [<integer>]? c <string> -- character string
     * [<integer>]? f <string> -- floating point
     * [<integer>]? d <string> -- double
     * [<integer>]? e <string> -- extended
     * string is evaluated as an <expression> [,<expression>]*
     */
    private void ParseData(Orca_Lexer lex) throws AsmException
    {
        int repeat;
        int size;
        int type;
        Token t;
        String s;
        int mod;
        
        lex.Expect(Token.SPACE);
        
        for (;;)
        {
            repeat = 0;
            mod = 0;
            
            int c = lex.Peek();
            if (ctype.isdigit(c))
            {
                t = lex.Expect(Token.NUMBER);
                repeat = t.Value();
            }
            else repeat = 1;
            
            c = lex.NextChar();
            type = ctype.tolower(c);

            switch (type)
            {
            case 'i':
            case 'a':
                c = lex.Peek();
                if (ctype.isdigit(c))
                {
                    t = lex.Expect(Token.NUMBER);
                    size = t.Value();
                    // TODO -- verify 1-8
                }
                else size = 2;
                
                if (type == 'i' && size == 1)
                {
                    // <, >, ^ allowed as a modifier.
                    mod = lex.Peek();
                    if (mod == '>' || mod == '<' || mod == '^')
                    {
                        lex.NextChar();
                    }
                    else mod = 0;
                }
                
                break;
            case 's':
            case 'r':
                c = lex.Peek();
                if (ctype.isdigit(c))
                {
                    t = lex.Expect(Token.NUMBER);
                    size = t.Value();
                    // TODO -- verify 1-4
                }
                else size = 2;
                break;
            
            case 'h':
            case 'b':
            case 'c':
            case 'e':
            case 'd':
            case 'f':
                size = -1;
                break;
            default:
                throw new AsmException(Error.E_UNEXPECTED, new Token(type));
            
            }
            
            t = lex.Expect(Token.STRING);
            s = t.toString();
            
            
            // TODO -- some of them can result in a list of references...
            // have to decode the best way ot handle it, with support for
            // repeat.
            Object data = null;
            switch(type)
            {
            case 'b':
                data = Orca_DC.DC_B(s);
                break;
                
            case 'c':
                data = Orca_DC.DC_C(s, fMSB);
                break;
 
            case 'h':
                data = Orca_DC.DC_H(s);
                break;
                
            case 'i':
            case 'a':
                data = Orca_DC.DC_I(s, size, fPC, fCase, mod);
                break;
                
            case 'r':
                data = Orca_DC.DC_R(s, fCase);
                break;
                
            case 's':
                data = Orca_DC.DC_S(s, size, fCase);
                break;
                
            }
            // store repeat times.
            if (repeat < 1) repeat = 1;
            for (int i = 0; i < repeat; i++)
                fData.add_object(data);
            
            t = lex.NextToken();
            type = t.Type();
            if (type == Token.EOL) break;
            if (type == (int)',')
                continue;
            
            throw new AsmException(Error.E_UNEXPECTED, t);        
        }
        
    }
    
    
    private __Expression ParseExpression(Lexer lex) throws AsmException
    {
        __Expression e;
        
        e = ExpressionParser.Parse(lex, fPC);
        
        return e.Simplify(fSymbols, false);     
    }
    
    /*
     * parses an expression and reduces it to an integer, or
     *  throws an error.
     */
    private int ParseIntExpression(Lexer lex) throws AsmException
    {
        
        __Expression e;
        
        e = ExpressionParser.Parse(lex, fPC);
        
        e = e.Simplify(fSymbols, true);
        Integer v = e.Value();
        
        if (v == null)
            throw new AsmException(Error.E_EXPRESSION, lex);
        
        return v.intValue();
    }



 
}