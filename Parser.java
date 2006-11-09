import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;
import java.io.FileOutputStream;

import omf.*;


/*
 * Created on Feb 24, 2006
 * Feb 24, 2006 11:32:44 PM
 */


public class Parser
{
    /*
     * start the parsing.
     */
    
        
    private HashMap<String, INSTR> fOpcodes;
    private HashMap<String, Directive> fDirectives;
    private ArrayList fData;
    private OMF_Segment fSegment;
    private boolean fM;
    private boolean fX;
    private int fPC;
    private int fAlign;
    private boolean fCase;
    private int fMachine;
    private boolean fMSB;
    
    private HashMap<String, Expression> fLocals;
    
    private int fError;
    private FileOutputStream fFile;

    

    public Parser()
    {
        fSegment = null;
        fLocals = null;
        fData = null;

        fM = true;
        fX = true;
        fPC = 0;
        fAlign = 0;
        fCase = false;
        fMachine = INSTR.m65816;
        fMSB = false;
        
        
        fOpcodes = new HashMap<String, INSTR>();
        fDirectives = new HashMap<String, Directive>();
        
        
        for (INSTR i: INSTR.values())
        {
            fOpcodes.put(i.name(), i);
        }

        for(Directive d: Directive.values())
        {
            fDirectives.put(d.name(), d);
        }

        fError = 0;
        try
        {
            File f = File.createTempFile("asm65816", "tmp");
            
            fFile = new FileOutputStream(f);
        }
        catch (Exception e)
        {}
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
                    break;
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
                
                Directive dir = fDirectives.get(s);
                if (dir == null)
                    throw new AsmException(Error.E_UNEXPECTED, t);
 
                switch (dir) {
                case START:
                case DATA:
                case PRIVATE:
                case PRIVDATA:
                    {
                        t = lex.NextToken();
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
                        switch (dir) {
                        case PRIVATE:
                            type = OMF.KIND_PRIVATE;
                        case START:
                            type = OMF.KIND_CODE;
                            break;
                        case PRIVDATA:
                            type = OMF.KIND_PRIVATE;
                        case DATA:
                            type = OMF.KIND_DATA;
                            break;
                        }
                        if (!fCase)
                        {
                            if (s != null) s = s.toUpperCase();
                            lab = lab.toUpperCase();
                        }
                        fSegment = new OMF_Segment();
                        fSegment.SetSegmentName(lab);
                        if (s != null)
                            fSegment.SetLoadName(s);
                        fSegment.SetKind(type);
                        fSegment.SetAttributes(attr);
                        fSegment.SetAlignment(fAlign);

                        ParseSegment(lex, type == OMF.KIND_DATA);
                    }
                    break;
                
                case ALIGN:
                    {
                        lex.Expect(Token.SPACE);
                        Expression e = new Expression();
                        e.ParseExpression(lex);
                        lex.Expect(Token.EOL);
                        e.Reduce(null, false);
                        Integer v = e.Value();
                        if (v == null)
                            throw new AsmException(Error.E_EXPRESSION);
                        
                        // must be a power of 2.
                        int i = v.intValue();
                        while ((i  & 0x01) == 0)
                        {
                            i = i >> 1;
                        }
                        if (i != 1)
                        {
                            throw new AsmException(Error.E_ALIGN);
                        }
                        fAlign = v.intValue();
                        
                    }
                    break;
                case CASE:
                    fCase = ParseOnOff(lex);
                    break;
                case M6502:
                    fMachine = INSTR.m6502;
                    fM = false;
                    fX = false;
                    lex.Expect(Token.EOL);
                    break;
                case M65C02:
                    fMachine = INSTR.m65c02;
                    fM = false;
                    fX = false;
                    lex.Expect(Token.EOL);
                    break;
                case M65816:
                    fMachine = INSTR.m65816;
                    fM = true;
                    fX = true;
                    lex.Expect(Token.EOL);
                    break;
                case RENAME:
                {
                    String sold, snew;
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
                default:
                    // unexpected/unsupported
                }

            }
            catch (AsmException e)
            {

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

        fData = new ArrayList();

       
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
        
        ArrayList temp = new ArrayList();
        OMF_Data data = new OMF_Data();
        int l = fData.size();
        int i = 0;
        while (i < l)
        {
            Object o = fData.get(i++);
            if (o instanceof OMF_DS)
            {
                int size = ((OMF_DS)o).CodeSize();
                while (i < l)
                {
                    Object o2 = fData.get(i);
                    if (!(o2 instanceof OMF_DS)) break;
                    size += ((OMF_DS)o2).CodeSize();
                    i++;
                }
                o = new OMF_DS(size);
            }
            else if (o instanceof byte[])
            {
                data.AppendData((byte[])o);
                continue;
            }
            else if (o instanceof String)
            {
                String s = (String)o;
                data.AppendData(s.getBytes());
            }
            if (o instanceof Expression)
            {
                Expression e = (Expression)o;
                e.Reduce(fLocals, false);
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
                        data.AppendInt8(value, 0);
                        break;
                    case 2:
                        data.AppendInt16(value, 0);
                        break;
                    case 3:
                        data.AppendInt24(value, 0);
                        break;
                    case 4:
                        data.AppendInt32(value, 0);
                        break;
                    }
                    continue;
                }
                // TODO -- convert to appropriate equ/gequ/expr/lexpr/ etc.
            }
            
            if (data.CodeSize() != 0)
            {
                fSegment.AddOpcode(data.toConst());
                data.Reset();
            }
            fSegment.AddOpcode((OMF_Opcode)o);
        }
        // save any lingering data.
        if (data.CodeSize() != 0)
        {
            fSegment.AddOpcode(data.toConst());
            data.Reset();
        }
        fSegment.AddOpcode(new OMF_Eof());
        // todo -- save.
        
    }

    /*
     * returns false once an END token is processed.
     */
    @SuppressWarnings("unchecked")
    private boolean ParseLine(Lexer_Orca lex)
    {
        String lab = null;
        String s = null;

        Token t = lex.NextToken();

        if (t.Type() == Token.EOL)
            return true;

        int pc = fPC;
        Expression e = null;

        try
        {

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
                e = null;
                switch (dir)
                {
                case ANOP:
                    lex.Expect(Token.EOL);
                    break;
                case KIND:
                    {
                        lex.Expect(Token.SPACE);
                        e = new Expression();
                        e.ParseExpression(lex);
                        lex.Expect(Token.EOL);
                        e.Reduce(null, false);
                        Integer v = e.Value();
                        if (v == null)
                            throw new AsmException(Error.E_EXPRESSION);
                        int i = v.intValue();
                        fSegment.SetKind(i);
                        fSegment.SetAttributes(i);
                    }
                    break;

                case EQU:
                case GEQU:
                    lex.Expect(Token.SPACE);
                    e = new Expression();
                    e.ParseExpression(lex);
                    e.SetPC(fPC);
                    lex.Expect(Token.EOL);
                    // if gequ, then also need to save in the OMF file.
                    break;

                case ENTRY:
                    // create an OMF_Global record...
                    if (lab != null)
                    {
                        fData.add(new OMF_Local(OMF.OMF_GLOBAL, lab, 0, 'N',
                                false));

                    }
                    lex.Expect(Token.EOL);
                    break;
                /*
                 * DS size size should be a number, equ, or gequ. since it
                 * affects the pc, it must already be defined.
                 */
                case DS:
                    {
                        Expression ex;
                        lex.Expect(Token.SPACE);
                        ex = new Expression();
                        ex.ParseExpression(lex);
                        lex.Expect(Token.EOL);
                        ex.SetPC(pc);
                        ex.Reduce(fLocals, true);
                        Integer v = ex.Value();
                        if (v == null)
                        {
                            throw new AsmException(Error.E_EXPRESSION, t);
                        }
                        int n = v.intValue();
                        fData.add(new OMF_DS(n));
                        fPC += n;
                    }
                    break;
                case DC:
                    ParseData(lex);
                    break;
                case LONGA:
                    if (fMachine != INSTR.m65816)
                    {
                        throw new AsmException(Error.E_MACHINE, t);
                    }
                    fM = ParseOnOff(lex);
                    break;

                case LONGI:
                    fX = ParseOnOff(lex);
                    break;
                case MSB:
                    fMSB = ParseOnOff(lex);
                    break;
                case END:
                    {
                        if (lab != null)
                            fLocals.put(lab, new Expression(pc));

                        return false;
                    }
                }// switch

                if (lab != null)
                {
                    if (e == null)
                        e = new Expression(pc);
                    fLocals.put(lab, e);
                }

                return true;
            } // end directive

            INSTR instr = fOpcodes.get(s);
            if (instr != null)
            {
                int opcode = 0x00;
                int size;

                // special case mvn/mvp
                switch (instr)
                {
                case MVN:
                case MVP:
                    Expression e1,
                    e2;
                    lex.Expect(Token.SPACE);
                    e1 = new Expression();
                    e2 = new Expression();
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
                    fData.add(new byte[] { (byte) opcode });
                    e1.SetSize(1);
                    e2.SetSize(1);
                    fData.add(e1);
                    fData.add(e2);

                    fPC += 3;
                    break;
                default:
                    Operand op = ParseOperand(lex);
                    op.SetPC(fPC);
                    opcode = instr.opcode(op, fMachine);
                    if (opcode == -1)
                    {
                        throw new AsmException(Error.E_OPCODE, t);
                    }
                    size = INSTR.Size(opcode, fM, fX);

                    fData.add(new byte[] { (byte) opcode });
                    if (size > 1)
                    {
                        op.SetSize(size - 1);
                        // TODO -- set type.
                        fData.add(op);
                    }
                    fPC += size;
                }
                if (lab != null)
                {
                    if (e == null)
                        e = new Expression(pc);
                    fLocals.put(lab, e);
                }
            }

        }
        catch (AsmException Error)
        {
            // skip to the next line.
            t = lex.LastToken();
            int type = t.Type();
            while (type != Token.EOL && type != Token.EOF)
            {
                t = lex.NextToken();
                type = t.Type();
            }
        }

        return true;
    }
    /*
     * expect: EOL <implicit on> SPACE ON EOL <explicit on> SPACE OFF EOL
     * <explicit off>
     */
    private boolean ParseOnOff(Lexer_Orca lex) throws AsmException
    {
        Token t;

        t = lex.NextToken();

        if (t.Type() == Token.EOL)
            return true;

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
            return new Operand(AddressMode.IMPLIED);
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
            op = new Operand(AddressMode.IMMEDIATE);
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
            op = new Operand(AddressMode.INDIRECT);
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
            
            op = new Operand(AddressMode.LINDIRECT);
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
            op = new Operand(AddressMode.ABS);
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
            
            op = new Operand(AddressMode.ABSLONG);
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
            
            op = new Operand(AddressMode.DP);
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
            op = new Operand(AddressMode.ABS);
            op.ParseExpression(lex);
            // check for ,x,y
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
        int mod;
        
        lex.Expect(Token.SPACE);
        
        for (;;)
        {
            t = lex.NextToken();
            type = t.Type();
            if (type == Token.EOL)
                throw new AsmException(Error.E_UNEXPECTED, t);

            repeat = 0;
            mod = 0;
            if (type == Token.NUMBER)
            {
                repeat = t.Value();
                t = lex.NextToken();
            }
            t.Expect(Token.SYMBOL);
            String s = t.toString();
            if (s.length() != 1)
            {
                throw new AsmException(Error.E_UNEXPECTED, t);
            }
            type = ctype.tolower(s.charAt(0));
            switch (type)
            {
            case 'i':
            case 'a':
                size = lex.Peek();
                if (ctype.isdigit(size))
                {
                    lex.NextChar();
                    size = ctype.toint(size);
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
                size = lex.Peek();
                if (ctype.isdigit(size))
                {
                    lex.NextChar();
                    size = size - '0';
                    // TODO -- verify 1-4
                }
                else size = 4;
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
                throw new AsmException(Error.E_UNEXPECTED, t);
            
            }
            
            t = lex.Expect(Token.STRING);
            s = t.toString();
            
            
            // todo -- have each one return a byte[] which will be written
            // appended to fData repeat times?
            switch(type)
            {


            case 'b':
                DC_B(s);
                break;
                
            case 'c':
                // if fMSB is true, we must set bit 8 on all chars.
                if (fMSB)
                {
                    int i;
                    int l = s.length();
                    StringBuffer s2 = new StringBuffer(l);
                    for (i = 0; i < s.length(); i++)
                    {
                        char c = s.charAt(i);
                        s2.setCharAt(i, (char)(c | 0x80));
                    }
                    fData.add(s2.toString());
                } 
            case 'h':
                DC_H(s);
                break;                
            }
            
            
            
            t = lex.NextToken();
            type = t.Type();
            if (type == Token.EOL) break;
            if (type == (int)',')
                continue;
            
            throw new AsmException(Error.E_UNEXPECTED, t);        
        }
        
    }
    
    @SuppressWarnings("unchecked")
    private void DC_H(String s)
    {
        int l = s.length();
        int i = 0;
        int temp = 0;
        boolean first = true;
        
        for (i = 0; i < l; i++)
        {
            int c = s.charAt(i);
            if (ctype.isspace(c)) continue;
            
            if (!ctype.isxdigit(c)) ; /* error */
            
            if (first)
            {
                temp = ctype.toint(c);
                first = false;
            }
            else
            {
                temp = (temp << 4) | ctype.toint(c);
                fData.add(new byte[] {(byte)temp});
                first = true;
            }
        }
        if (!first)
        {
            temp = temp << 4;
            fData.add(new byte[] {(byte)temp});
        }
    }
    
    @SuppressWarnings("unchecked")
    private void DC_B(String s)
    {
        int l = s.length();
        int pos = 0;
        int temp = 0;
        for (int i = 0; i < l; i++)
        {
            int c = s.charAt(i);
            if (ctype.isspace(c)) continue;
            
            
            temp = temp << 1;
            if (c == '1') temp |= 0x01;
            else if (c == '0') ;
            else ; /* error */
            if (pos++ == 7)
            {
                fData.add(new byte[] {(byte) temp});
                pos = 0;
            }
            
        }
        if (pos > 0)
        {
            temp = temp << (7-pos);
            fData.add(new byte[] {(byte) temp});
        }
    }


 
}