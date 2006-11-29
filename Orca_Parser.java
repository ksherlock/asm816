import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import omf.*;


/*
 * Created on Feb 24, 2006
 * Feb 24, 2006 11:32:44 PM
 */


public class Orca_Parser
{
    /*
     * start the parsing.
     */
    
        
    private HashMap<String, INSTR> fOpcodes;
    private HashMap<String, Directive> fDirectives;
    private JunkPile fData;
    private OMF_Segment fSegment;
    private boolean fM;
    private boolean fX;
    private int fPC;
    private int fAlign;
    private boolean fCase;
    private int fMachine;
    private boolean fMSB;
    
    private HashMap<String, Expression> fLocals;
    private HashMap<String, Expression> fGlobals;
    
    private int fError;
    private FileOutputStream fFile;

    

    public Orca_Parser(FileOutputStream outfile)
    {
        fSegment = null;
        fLocals = null;
        fData = null;

        fGlobals = new HashMap<String, Expression>();
        
        fM = true;
        fX = true;
        fPC = 0;
        fAlign = 0;
        fCase = false;
        fMachine = INSTR.m65816;
        fMSB = false;
        
        fFile = outfile;
        
        fOpcodes = new HashMap<String, INSTR>();
        fDirectives = new HashMap<String, Directive>();
        
        
        for (INSTR i: INSTR.values())
        {
            fOpcodes.put(i.name(), i);
        }
        // synonyms
        fOpcodes.put("BLT", INSTR.BCC);
        fOpcodes.put("BGE", INSTR.BCS);
        fOpcodes.put("CPA", INSTR.CMP);

        for(Directive d: Directive.values())
        {
            fDirectives.put(d.name(), d);
        }

        fError = 0;

    }
    private void ParseFile(String filename)
    {
        // TODO -- check -I directories for the file
        File f = new File(filename);
        try
        {
            FileInputStream stream = new FileInputStream(f);
            Lexer_Orca lex = new Lexer_Orca(stream);
            Parse(lex);
        }
        catch (FileNotFoundException e)
        {
            System.err.print("Unable to open file " + filename);
        }
    }
    
    public void Parse(Lexer_Orca lex)
    {
        Token t;
        String lab;
        
       for(;;)
        {
            
           /*
            * expect:
            * 
            * <eol>
            * [lab] CASE ON|OFF <eol>
            * lab DATA|PRIVATE|PRIVDATA|START <eol>
            * [lab] M6502|M65C02|M65816 <eol>
            * [lab] MCOPY <path> <eol>
            * [lab] APPEND|COPY <path> <eol>
            * [lab] RENAME symbol,symbol
            * 
            */
            
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
                    if (!fCase)
                        lab = lab.toUpperCase();
                    
                    lex.Expect(Token.SPACE);
                    break;
                default:
                    throw new AsmException(Error.E_UNEXPECTED, t);
                }
                
                t = lex.Expect(Token.SYMBOL);
                String s = t.toString().toUpperCase();
                
                Directive dir = fDirectives.get(s);
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
    private void ParseSegment(Lexer_Orca lex, boolean isData)
            throws AsmException
    {

        fLocals = new HashMap<String, Expression>();

        fLocals.putAll(fGlobals);
        
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

        fLocals.put(fSegment.SegmentName(), new Expression(0));

        while(ParseLine(lex) == true) /* do nothing */;
        
        // now finish up.
        // iterate through fData
        // reducing expressions,
        // merging DS
        // merging byte[]s
        
        Reduce();
        
        fLocals = null;
 
        
    }
    
    private void Reduce()
    {
        
        boolean delta = false;
        
        // loop through fData and reduce any expressions. 
        // repeat until no more changes are possible.
              
        do
        {
            delta = false;
            ArrayList ops = fData.GetArrayList();
            fData = new JunkPile();

            for(Object op : ops)
            {
                if (op instanceof Expression)
                {
                    Expression e = (Expression)op;
                   // reduce
                    try
                    {
                        e.Reduce(fLocals, false);
                    }
                    catch (AsmException err)
                    {
                        // TODO Auto-generated catch block
                        err.printStackTrace();
                    }
                    Integer v = e.Value();
                    if (v != null)
                    {
                        delta = true;
                            
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
                        
        } while (delta);
        

        // Now, go through a final time 
        // save to disk.
        ArrayList ops = fData.GetArrayList();

        for (Object op: ops)
        {
            if (op instanceof Expression)
            {
                Expression e = (Expression)op;
                op = e.toOpcode();
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
    private boolean ParseLine(Lexer_Orca lex)
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
                if (!fCase)
                    lab = lab.toUpperCase();
                t = lex.NextToken();
            }
            t.Expect(Token.SPACE);

            t = lex.Expect(Token.SYMBOL);

            s = t.toString().toUpperCase();

            Directive dir = fDirectives.get(s);
            if (dir != null)
            {
                boolean tf = ParseDirective(lab, lex, dir);
                if (!tf)
                    throw new AsmException(Error.E_UNEXPECTED, t);
                if (dir == Directive.END) return false; // all done.
                
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
    private boolean ParseDirective(String lab, Lexer_Orca lex, Directive d) throws AsmException
    {
        Expression e = null;
        int segkind = -1;
        boolean inSeg = false;
        
        int pc = fPC;
        
        //private HashMap<String, Expression> map;
        //map = segkind == -1 ? fGlobals : fLocals;
        

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
                
                if (!fCase && s != null)
                {
                    s = s.toUpperCase();
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
                e = new Expression(fCase);
                e.ParseExpression(lex);
                lex.Expect(Token.EOL);
                e.Reduce(inSeg ? fLocals : fGlobals, false);

                Integer v = e.Value();
                if (v == null)
                    throw new AsmException(Error.E_EXPRESSION, lex);
               
                // must be a power of 2
                int i = v.intValue();
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

                lex.Expect(Token.SPACE);
                e = new Expression(fCase);
                e.ParseExpression(lex);
                lex.Expect(Token.EOL);
                e.SetPC(fPC);
                e.Reduce(fLocals, true);
                Integer v = e.Value();
                if (v == null)
                {
                    throw new AsmException(Error.E_EXPRESSION);
                }
                int n = v.intValue();
                fData.add(new OMF_DS(n));
                               
                e = null; // reset for below.
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
                e = new Expression(fCase);
                e.ParseExpression(lex);
                lex.Expect(Token.EOL);
                   
                if (!inSeg)
                {
                    // must be reducable.
                    e.Reduce(fGlobals, false);
                    if (e.Value() == null)
                       throw new AsmException(Error.E_EXPRESSION);
                    
                    if (lab != null)
                        fGlobals.put(lab, e);   
                }
                else
                {
                   e.SetPC(fPC);
                   if (lab != null)
                   {
                       e.SetExpressionName(lab);
                       e.SetExpressionType(d == Directive.EQU ? 
                               OMF.OMF_EQU : OMF.OMF_GEQU);
                       
                       fLocals.put(lab, e);
                       // in a segment, GEQU is always written to disk.
                       // EQU is only written in a data segment.
                       if (segkind == OMF.KIND_DATA || d == Directive.GEQU)
                       {
                           e.Reduce(fGlobals, false);
                           fData.add(e.toOpcode());
                       }
                   }
                }
                // prevent double dipping.
                lab = null;
            }
            break;
            
            
            
        case KIND:
            {
                if (!inSeg) return false;
                
                lex.Expect(Token.SPACE);
                e = new Expression(fCase);
                e.ParseExpression(lex);
                lex.Expect(Token.EOL);
                e.Reduce(fLocals, false);
                Integer v = e.Value();
                if (v == null)
                    throw new AsmException(Error.E_EXPRESSION);
                int i = v.intValue();

                fSegment.SetKind(i);
                fSegment.SetAttributes(i);
                lab = null;
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
                if (!fCase) s = s.toUpperCase();
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
         * unsupported
         */    
            
        default: return false;
        }
        
        if (inSeg && lab != null)
        {
            if (e == null) e = new Expression(pc);
            fLocals.put(lab, e);
        }
        
        return true;
    }
 
    private boolean ParseInstruction(String lab, Lexer_Orca lex, INSTR instr) throws AsmException
    {
        int opcode = 0x00;
        int size;

        boolean check_a = false;
        
        // special case mvn/mvp
        switch (instr)
        {
        case MVN:
        case MVP:
            Expression e1,
            e2;
            lex.Expect(Token.SPACE);
            e1 = new Expression(fCase);
            e2 = new Expression(fCase);
            e1.ParseExpression(lex);
            lex.Expect((int) ',');
            e2.ParseExpression(lex);
            e1.SetPC(fPC);
            e2.SetPC(fPC);
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
            e1.SetSize(1);
            e2.SetSize(1);
            fData.add(e1);
            fData.add(e2);
            
            break;
            
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
            Operand op = ParseOperand(lex);
            op.SetPC(fPC);
            
            if (check_a)
            {
                String oplab = op.Label();
                if (oplab != null && oplab.compareToIgnoreCase("a") == 0)
                    op.SetType(AddressMode.IMPLIED);
            }
            
            opcode = instr.opcode(op, fMachine);
            if (opcode == -1)
            {
                throw new AsmException(Error.E_OPCODE);
            }
            size = INSTR.Size(opcode, fM, fX);

            fData.add8(opcode, 0);
            if (size > 1)
            {
                op.SetSize(size - 1);
                //TODO -- set type.
                if (INSTR.isBranch(opcode))
                    op.SetExpressionType(OMF.OMF_RELEXPR);
                
                fData.add(op);
            }

        } /* switch (instr) */
        
        if (lab != null)
        {
            fLocals.put(lab, new Expression(fPC));
        }
        
        return true;
    }
    
    
    /*
     * expect: EOL <implicit on> SPACE ON EOL <explicit on> SPACE OFF EOL
     * <explicit off>
     */
    private boolean ParseOnOff(Lexer_Orca lex, boolean blank) throws AsmException
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

    private Operand ParseOperand(Lexer_Orca lex) throws AsmException
    {
        int c;
        Token t;
        Operand op;

        t = lex.NextToken();
        if (t.Type() == Token.EOL)
        {
            return new Operand(AddressMode.IMPLIED, fCase);
        }
        t.Expect(Token.SPACE);
        
        c = lex.Peek();

        
        switch (c) {
        /*
         * immediate mode. 
         * '#' [ '<' | '>' | '|' ]? <expression>
         */
        case '#':
            lex.NextChar();

            c = lex.Peek();
            if (c == '^' || c == '>' || c == '<')
            {
                lex.NextChar();
            }
            op = new Operand(AddressMode.IMMEDIATE, fCase);
            op.ParseExpression(lex);

            // ^ > --> shift it.
            if (c == '^')
                op.Shift(-16);
            else if (c == '>')
                op.Shift(-8);
            break;
            
            /*
             * indirect
             * '(' ['<']? <expression> ')' [',' 'y']?
             * '(' ['<']? <expression> ',' 'x' ')'
             * '(' ['<']? <expression> ',' 's' ')' ',' 'y'
             */
        case '(':
            boolean stack = false;
            lex.NextChar();
            
            c = lex.Peek();
            if (c == '<')
                lex.NextChar();
            op = new Operand(AddressMode.INDIRECT, fCase);
            op.ParseExpression(lex);

            // next char must be , or )
            c = lex.Peek();
            if (c == ',')
            {
                lex.NextChar();
                t = lex.NextToken();
                c = t.Register();
                if (c == 'x')
                {
                    op.SetType(AddressMode.INDIRECT_X);
                    lex.Expect((int)')');
                    break;
                }
                
                else if (c == 's')
                {
                    stack = true;
                }
                else 
                    throw new AsmException(Error.E_UNEXPECTED, t);
                    
            }
            lex.Expect((int)')');

            c = lex.Peek();
            if (c == ',')
            {
                lex.NextChar();

                t = lex.NextToken();
                c = t.Register();
                if (c == 'y')
                {
                    op.SetType(stack ? AddressMode.INDIRECT_S_Y : AddressMode.INDIRECT_Y);
                    stack = false;
                }
                else 
                    throw new AsmException(Error.E_UNEXPECTED, t);
            }
            if (stack) 
                throw new AsmException(Error.E_UNEXPECTED, t);
            break;
            
            /*
             * long indirect
             * '[' ['<']? <expression> ']' [',' 'y']?
             */
        case '[':
            lex.NextChar();
            
            c = lex.Peek();
            if (c == '<')
                lex.NextChar();
            
            op = new Operand(AddressMode.LINDIRECT, fCase);
            op.ParseExpression(lex);
            lex.Expect((int)']');

            c = lex.Peek();
            if (c == ',')
            {
                lex.NextChar();
                t = lex.NextToken();
                c = t.Register();
                if (c == 'y')
                    op.SetType(AddressMode.LINDIRECT_Y);
                    
                else
                    throw new AsmException(Error.E_UNEXPECTED, t);
                
                
            }
            break;
            
            /*
             * absolute mode
             * '|' <expression> [',' ['x' | 'y'] ]?
             */
        case '|':
            lex.NextChar();
            op = new Operand(AddressMode.ABS, fCase);
            op.ParseExpression(lex);

            c = lex.Peek();
            if (c == ',')
            {
                lex.NextChar();
                t = lex.NextToken();

                // t must be a symbol, either X or Y
                c = t.Register();
                if (c == 'y')
                    op.SetType(AddressMode.ABS_Y);
                else if (c == 'x')
                    op.SetType(AddressMode.ABS_X);

                else
                    throw new AsmException(Error.E_UNEXPECTED, t);

            }
            break;
            
            /*
             * absolute long mode
             * '>' <expression> [',' 'x']?
             */
        case '>':
            lex.NextChar();
            
            op = new Operand(AddressMode.ABSLONG, fCase);
            op.ParseExpression(lex);
            c = lex.Peek();
            if (c == ',')
            {
                lex.NextChar();
                t = lex.NextToken();

                c = t.Register();

                if (c == 'x')
                    op.SetType(AddressMode.ABSLONG_X);

                else
                    throw new AsmException(Error.E_UNEXPECTED, t);
            }
            break;
            
            /*
             * dp mode
             * <dp,s is allowed as a convenience.
             * '<' <expression> [',' ['x' | 'y' | 's']]?
             */
        case '<':
            lex.NextChar();
            
            op = new Operand(AddressMode.DP, fCase);
            op.ParseExpression(lex);

            c = lex.Peek();
            if (c == ',')
            {
                lex.NextChar();
                t = lex.NextToken();
                // t must be a symbol, either S, X, or Y
                c = t.Register();
                
                if (c == 's')
                    op.SetType(AddressMode.STACK);
                else if (c == 'x')
                    op.SetType(AddressMode.DP_X);
                else if (c == 'y')
                    op.SetType(AddressMode.DP_Y);

                else
                    throw new AsmException(Error.E_UNEXPECTED, t);
            }
            
            break;
            
            /*
             * just another expression...
             * <expression> [',' ['x' | 'y' | 's']]?
             */
        default:
            // TODO -- reduce, convert to dp/abslong ???
            // or mark as possibly unknown??
            op = new Operand(AddressMode.ABS, fCase);
        
            /* TODO -- if the operand is a known value, set to DP or long as appropriate
            * eg: 
            * blah equ $e12000
            *      lda blah
            * should be handled as lda >blah
            */ 
        
            op.ParseExpression(lex);
            // check for ,x,y
            // ,s --> stack relative (ie, dp)
            c = lex.Peek();
            if (c == ',')
            {
                lex.NextChar();
                t = lex.NextToken();
                c = t.Register();

                // t should be x,y,s

                if (c == 'x')
                    op.SetType(AddressMode.ABS_X);
                else if (c == 'y')
                    op.SetType(AddressMode.ABS_Y);
                else if (c == 's')
                    op.SetType(AddressMode.STACK);
                else
                    throw new AsmException(Error.E_UNEXPECTED, t);
            }
        }

        lex.Expect(Token.EOL);
        return op;
        
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
    private void ParseData(Lexer_Orca lex) throws AsmException
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
    
    



 
}