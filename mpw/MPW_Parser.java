package mpw;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import asm816.AddressMode;
import asm816.AsmException;
import asm816.ContextMap;
import asm816.Error;
import asm816.INSTR;
import asm816.JunkPile;
import asm816.Lexer;
import asm816.Parser;
import asm816.Token;
import asm816.ctype;

import expression.*;

import omf.OMF;
import omf.OMF_DS;
import omf.OMF_Segment;


/*
 * Created on Nov 29, 2006
 * Nov 29, 2006 10:18:38 PM
 */

/*
 * 
 * MPW IIgs Assember syntax.
 * 
 * 
 */

// TODO -- create base Parser class to deal with common functionality.


public class MPW_Parser extends Parser
{
    private int fStringMode;
    private int fAlign;
    private boolean fMSB;

    
    private boolean fEndSeg;
    private boolean fEndFile;
    
    private JunkPile fData;
    private OMF_Segment fSegment;
    
    private ContextMap<String, Record> fRecords;
    
    private MPW_SymbolTable fSymbols;
    
    private static final int STRING_PASCAL = 0;
    private static final int STRING_ASIS = 1;
    private static final int STRING_C = 2;
    private static final int STRING_GSOS = 3;
    
    public MPW_Parser()
    {
        super();
        fPC = 0;
        fSymbols = new MPW_SymbolTable();
        fRecords = new ContextMap<String, Record>();
        fData = null;
        fSegment = null;
        fStringMode = STRING_PASCAL;
        fMSB = false;
        fM = fX = true;
        fMachine = INSTR.m65816;
        
        fEndFile = false;
        fEndSeg = false;
        
    }
    
    
    public void ParseFile(File f)
    {
        try
        {
            FileInputStream in = new FileInputStream(f);
            
            MPW_Lexer lex = new MPW_Lexer(in);
            Parse(lex);
            
        }
        catch (FileNotFoundException e)
        {
            
        }
        
    }
    
    public void Parse(Lexer lex)
    {
        fEndFile = false;
        lex.SetCase(fCase);
        for (;;)
        {
            ParseLine(lex);
            if (fEndFile) break;
        }
        //fSymbols.PrintTable(System.out);
        
    }
    
    protected void AddDirectives()
    {
        for (MPW_Directive i : MPW_Directive.values())
                fDirectives.put(i.name(), i);
        fDirectives.put("ENDP", MPW_Directive.ENDPROC);
        fDirectives.put("FUNCTION", MPW_Directive.PROC);
        fDirectives.put("ENDFUNCTION", MPW_Directive.ENDPROC);
        fDirectives.put("ENDF", MPW_Directive.ENDPROC);
        
        // need to do these since '.' is a valid symbol char.
        fDirectives.put("DC.B", MPW_Directive.DC);
        fDirectives.put("DC.W", MPW_Directive.DC);
        fDirectives.put("DC.L", MPW_Directive.DC);
 
        fDirectives.put("DCB.B", MPW_Directive.DCB);
        fDirectives.put("DCB.W", MPW_Directive.DCB);
        fDirectives.put("DCB.L", MPW_Directive.DCB);        
        
        fDirectives.put("DS.B", MPW_Directive.DS);
        fDirectives.put("DS.W", MPW_Directive.DS);
        fDirectives.put("DS.L", MPW_Directive.DS);        
    }
    
    protected void ParseSegment(Lexer lex) throws AsmException 
    {
        fSymbols.Push(); // create local context.
        fRecords.Push();
        
        fSymbols.Put(fSegment.SegmentName(), new RelExpression(0)); 
        fSymbols.ClearWith(); // shouldn't be necessary
        
        ((MPW_Lexer)lex).SetLocalLabel(fSegment.SegmentName());
        
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
        
        fData = new JunkPile();

        fMSB = false;
        
        fEndSeg = false;
        fEndFile = false;
        
        for (;;)
        {
            ParseLine(lex);
            if (fEndFile || fEndSeg) break;
        }
        
        if (!fEndSeg)
        {
            // TODO -- warn about premature EOF
        }
        
        // Local symbol table.
        
        fSymbols.PrintTable(System.out);
       
        Reduce();
        fSymbols.Pop();
        fSymbols.ClearWith(); 
        fRecords.Pop();
        fSegment = null;
        fData = null;
       
    }
    
    // everytime simplification happens, the current WITH could
    // affect it, even if it shouldn't... 
    // should have a SymbolTAble for WITH definitions, simplify
    // when an expression is originally parsed.  fSymbols is then
    // a normal SymbolTable.
    
    protected void Reduce()
    {
        ArrayList ops = fData.GetArrayList();
        fData = new JunkPile();
        
        /*
        for (Object op : ops)
        {
            if (op instanceof IExpression)
            {
                IExpression ie = ((IExpression)op).Simplify(fSymbols);
                fData.add(ie.toOpcode(fSegment.SegmentName()));
            }
            else fData.add_object(op);
        }
        */

    }

    protected void ParseLine(Lexer lex)
    {
        Token t;
        String s;
        String lab = null;
        
        boolean inSeg = fSegment != null;
        fPC = fData == null ? 0 : fData.Size();
        
        
        try
        {
            t = lex.Expect(Token.SPACE, Token.EOF, Token.EOL, Token.SYMBOL);

            switch(t.Type())
            {
            case Token.EOF:
                fEndFile = true;
                return;
            case Token.EOL:
                return;
            case Token.SPACE:
                break;
            case Token.SYMBOL:
                lab = t.toString();
                
                // check for '@' label
                if (lab.indexOf('@') >= 0)
                {
                    // TODO -- warn if not in a segment.
                }
                else if (inSeg) ((MPW_Lexer)lex).SetLocalLabel(lab);
                
                t = lex.Expect(Token.SPACE, Token.EOL);
                if (t.Type() == Token.SPACE) break;
                           
                // label only, treat as anop
                if (inSeg)
                    fSymbols.Put(lab, new RelExpression(fPC));
                return;   
            }
            
            t = lex.Expect(Token.SYMBOL);
            s = t.toString().toUpperCase();
            
            MPW_Directive dir = (MPW_Directive)fDirectives.get(s);
            if (dir != null)
            {
                boolean tf = ParseDirective(lab, lex, dir);
                if (!tf)
                    throw new AsmException(Error.E_UNEXPECTED, t);
               return;
            }
            
            if (inSeg)
            {
                INSTR instr = fOpcodes.get(s);
                if (instr != null)
                {
                    ParseInstruction(lab, lex, instr);
                    
                    return;
                }
                
                // TODO -- macro support.
                
            }
            throw new AsmException(Error.E_UNEXPECTED, t);
    
        }
        catch (AsmException error)
        {
            error.print(System.err);
            lex.SkipLine();
        }      
    }
    
    protected boolean ParseDirective(String lab, Lexer lex, Enum d) throws AsmException
    {
        boolean inSeg = (fSegment != null);
        __Expression e = null;
        MPW_Directive dir = (MPW_Directive)d;
        
        switch(dir)
        {
        
        case EQU:
            {
                lex.Expect(Token.SPACE);
                e = ParseExpression(lex);
                
                if (lab != null)
                {
                    fSymbols.Put(lab, e);
                }
                e = null;
                lab = null;
            }
            break;
        
            /*
             * PROC (|ENTRY|EXPORT)
             * 
             */
        case PROC:
            if (inSeg) return false;
            {     
                boolean entry = true;
                
                // TODO MPW allows label to be optional...
                if (lab == null)
                    throw new AsmException(Error.E_LABEL_REQUIRED, lex);
                
                Token t;
                t = lex.Expect(Token.SPACE, Token.EOL);
                
                if (t.Type() == Token.SPACE)
                {
                    t = lex.Expect(Token.SYMBOL);
                    String s = t.toString().toUpperCase();
                    if (s.equals("ENTRY"))
                        entry = true;
                    else if (s.equals("EXPORT"))
                        entry = false;
                    else throw new AsmException(Error.E_UNEXPECTED, t);
                    
                    lex.Expect(Token.EOL);
                }
                    
                
                
                fSegment = new OMF_Segment();
                fSegment.SetSegmentName(lab);
                
                ParseSegment(lex);
                
                lab = null;
            }
            break;
            
        case ENDPROC:
            fEndSeg = true;
            break;
            
        case END:
            fEndSeg = true;
            fEndFile = true;
            break;
            
        case RECORD:
            ParseRecord(lex, lab);
            lab = null;
            break;
            
        
        case CASE:
            fCase = ParseOnOff(lex, false);
            lex.SetCase(fCase);
            break;
        
        case DS:
            if (!inSeg) return false;
            
            ParseDS(lex, lab);

            break;
            
        case DC:
            ParseDC(lex);
            break;
            
        case DCB:
            ParseDCB(lex);
            break;
            
            
        case MACHINE:
            {
                Token t;
                String s;
            
                lex.Expect(Token.SPACE);
                t = lex.Expect(Token.SYMBOL);
                s = t.toString().toUpperCase();
                
                if (s.equals("M65816"))
                {
                    fMachine = INSTR.m65816;
                    fM = fX = true;
                }
                else if (s.equals("M65C02"))
                {
                    fMachine = INSTR.m65c02;
                    fM = fX = false;
                }
                else if (s.equals("M6502"))
                {
                    fMachine = INSTR.m6502;
                    fM = fX = false;
                }
                else throw new AsmException(Error.E_UNEXPECTED, t);

                lex.Expect(Token.EOL);
            
            }
            break;
            
        case MSB:
            fMSB = ParseOnOff(lex, false);
            break;            
            
        case STRING:
            {
                Token t;
                String s;
                t = lex.Expect(Token.EOL, Token.SPACE);
                if (t.Type() == Token.EOL)
                    fStringMode = STRING_PASCAL;
                else
                {
                    t = lex.Expect(Token.SYMBOL);
                   
                    s = t.toString().toLowerCase();
                    if (s.equals("pascal"))
                        fStringMode = STRING_PASCAL;
                    else if (s.equals("c"))
                        fStringMode = STRING_C;
                    else if (s.equals("asis"))
                        fStringMode = STRING_ASIS;
                    else if (s.equals("gsos"))
                        fStringMode = STRING_GSOS;
                    else
                        throw new AsmException(Error.E_UNEXPECTED, t);
  
                    lex.Expect(Token.EOL);             
                }
            }
            break;
            
            /*
             * WITH name[,name]
             */
            
            /*
             * TODO -- WITH/ENDWITH can be nested.
             * WITH a,b,c == c has highest priority.
             * 
             */
        case WITH:
            if (!inSeg) return false;
            {
                fSymbols.PushWith();
                
                Token t;
                String s;
                int c;
                lex.Expect(Token.SPACE);
                for (;;)
                {
                    t = lex.Expect(Token.SYMBOL);
                    s = t.toString();
                    fSymbols.AddWith(s);
                    c = lex.Peek();
                    if (c != ',') break;
                    lex.NextChar();
                }
                lex.Expect(Token.EOL);
            }
            break;
            
        case ENDWITH:
            if (!inSeg) return false;
            fSymbols.PopWith();
            lex.Expect(Token.EOL);
            break;
            
        default:
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
     * ON | YES | Y ==> true
     * OFF | NO | N ==> false
     * 
     *  unsupported OBJ | OBJECT.
     */
    private boolean ParseOnOff(Lexer lex, boolean blank) throws AsmException
    {
        Token t;
        String s;
        
        t = lex.Expect(Token.SPACE, Token.EOL);
        if (t.Type() == Token.EOL) return blank;
        
        t = lex.Expect(Token.SYMBOL);
        
        lex.Expect(Token.EOL);
        
        s = t.toString().toUpperCase();
        if (s.equals("ON") || s.equals("Y") || s.equals("YES"))
            return true;
        
        if (s.equals("OFF") || s.equals("N") || s.equals("NO"))
            return false;
              
        throw new AsmException(Error.E_UNEXPECTED, t);
    }
    
    /*
     * not currently needed.
     * 
     */
    private int ParseDotSize(Lexer lex, int blank) throws AsmException
    {
        int c;
        Token t;
        String s;
        c = lex.Peek();
        if (c != '.') return blank;
        lex.NextChar();
        
        t = lex.Expect(Token.SYMBOL);
        s = t.toString();
        if (s.length() == 1)
        {
            c = s.charAt(0);
            c = ctype.tolower(c);
            
            switch(c)
            {
            case 's':
            case 'd':
            case 'x':
            case 'p':
            
            case 'b':
            case 'w':
            case 'l':
                return c;
            }
            
        }
        throw new AsmException(Error.E_UNEXPECTED, t);
    }
    
    
    /*
     *  DCB[.size] expression,(expression|string)
     *  
     *  first expression is repeat count.
     */
    
    private void ParseDCB(Lexer lex) throws AsmException
    { 
        String s;
        int qualifier = 'w';
        s = lex.LastToken().toString();
        if (s.indexOf('.') > 0) 
            qualifier = ctype.tolower(s.charAt(s.length() -1));
        
        
        int size = QualifierToInt(qualifier);
        
        __Expression e;
        Token t; 
        int c;      
        
        lex.Expect(Token.SPACE);
        
        int repeat = ParseIntExpression(lex);
        lex.Expect(',');
        
        c = lex.Peek();
        if (c == '\'')
        {
            t = lex.Expect(Token.STRING);
            s = t.toString();
            // todo -- check if int or float
            // todo -- null pad to correspond with qalifier.
            
            byte[] data = StringToByte(s);
            for (int i = 0; i < repeat; i++)
                fData.add(data);       
        }
        else
        {
            e = ParseExpression(lex);
            Expression ex = new Expression(e);
            ex.SetSize(QualifierToInt(size));
            for (int i = 0; i < repeat; i++)
                fData.add(ex);          
        }
        
        lex.Expect(Token.EOL);
    }
    
    /*
     * DC[.size] (expression|string)[,(expression|string)]
     * 
     */
    private void ParseDC(Lexer lex) throws AsmException
    {
        String s;

        int qualifier = 'w';
        s = lex.LastToken().toString();
        if (s.indexOf('.') > 0) 
            qualifier = ctype.tolower(s.charAt(s.length() -1));
        
        
        int size = QualifierToInt(qualifier);
        
        __Expression e;
        Token t;
        
        
        lex.Expect(Token.SPACE);
        
        for (;;)
        {
            int c = lex.Peek();
            if (c == '\'')
            {
                t = lex.Expect(Token.STRING);
                s = t.toString();
                fData.add(StringToByte(s));
                
            }
            else
            {
                e = ParseExpression(lex);
                Expression ex = new Expression(e);
                ex.SetSize(size);
                fData.add(ex);
            }
            c = lex.Peek();
            if (c != ',') break;
        }
        
        lex.Expect(Token.EOL);
    }
    
    /*
     * DS[.size] (record-name | expression)
     * 
     * if record name, creates equates for all members.
     * 
     */
    private void ParseDS(Lexer lex, String lab) throws AsmException
    {
        String s;

        int qualifier = 'w';
        s = lex.LastToken().toString();
        if (s.indexOf('.') > 0) 
            qualifier = ctype.tolower(s.charAt(s.length() -1));

        
        int size = QualifierToInt(qualifier);
        
        lex.Expect(Token.SPACE);
        __Expression e = ParseExpression(lex);
        
        
        // check if this is a record template
        s = e.toString();
        if (s != null)
        {
            Record r = fRecords.Get(s);
            if (r != null)
            {
                // TODO -- any alignment voodoo
                fData.add(new OMF_DS(r.Size));
                
                if (lab != null)
                {                    
                    AddRecordMembers(lab, r.Children, fPC, false);
                }
                
                return;
            }
        }
        
        Integer v = e.Value();
        if (v == null)
            throw new AsmException(Error.E_EXPRESSION);
        int length = v.intValue();
        
        /*
         * All data sizes except byte (B) are aligned to 
         * the next word boundary unless an ALIGN 0 directive 
         * is in force. The optional label is associated 
         * with the first byte of data after alignment.
         * 
         * A DS directive with a length of 0 aligns code or 
         * data to a word boundary.
         */

        fData.add(new OMF_DS(length * size));
    }
    
    /*
     * 
     * RECORD (expression | { symbol } | IMPORT) [,(INCR|INCREMENT|DECR|DECREMENT)]
     * 
     * NB - IMPORT is not yet supported.
     */
        
    /*
     * TODO -- record has 2nd usage:
     * RECORD (|EXPORT|ENTRY) 
     * to define a data module (???)
     */
    
    private void ParseRecord(Lexer lex, String RecordName) throws AsmException
    {
        String s;
       
        Token t;
        __Expression e;
        boolean incr = true;
        int offset = 0;
        String origin = null;
        int c;
        
        boolean inSeg = (fSegment != null);
        
        /*
         * todo -- error if no label name.
         */
        if (RecordName != null)
        {
            // allow record members to be accessed.
            // within the record.
            fSymbols.PushWith();
            fSymbols.AddWith(RecordName);
        }
        
   
        
        lex.Expect(Token.SPACE);
        c = lex.Peek();
        if (c == '{')
        {
            lex.NextChar();
            t = lex.Expect(Token.SYMBOL);
            origin = t.toString();
            lex.Expect('}');
        }
        else
        {
            Integer v;
            e = ParseExpression(lex);
            v = e.Value();
            if (v == null)
                throw new AsmException(Error.E_EXPRESSION, lex);
            offset = v.intValue();
        }
        c = lex.Peek();
        if (c == ',')
        {
            lex.NextChar();
            t = lex.Expect(Token.SYMBOL);
            s = t.toString().toUpperCase();
            if (s.equals("INCR") || s.equals("INCREMENT"))
                incr = true;
            else if (s.equals("DECR") || s.equals("DECREMENT"))
                incr = false;
            else throw new AsmException(Error.E_UNEXPECTED, t);
        }
        lex.Expect(Token.EOL);
        
        
        Record r = new Record();
        RecordItem baseri = null;
        RecordItem lastri = null;
        
        
        fPC = offset;
        
        // TODO -- implicit WITH .. to refer to self records...
        // need to duplicate symbol table while parsing the record?
        boolean done = false;
        for( ;!done; )
        {
            String lab = null;
            MPW_Directive d;
            
            t = lex.Expect(Token.EOL, Token.SYMBOL, Token.SPACE);
            if (t.Type() == Token.EOL) continue;
            
            if (t.Type() == Token.SYMBOL)
            {
                lab = t.toString();
                lex.Expect(Token.SPACE);
            }
            
            t = lex.Expect(Token.SYMBOL);
            s = t.toString().toUpperCase();
            
            d = (MPW_Directive)fDirectives.get(s);
            if (d == null)
            {
                throw new AsmException(Error.E_UNEXPECTED, t);
            }
            switch(d)
            {
            //TODO -- behave nice at END
            case DS:
                {
                    RecordItem ri = new RecordItem();
                    ri.Offset = fPC;
                    ri.Name = lab;
                    int delta = ParseRecordDS(lex, ri);
                                           
                    fSymbols.Put(RecordName + "." + lab, 
                        new ConstExpression(fPC));
           
                    if (incr) fPC += delta;
                    else fPC -= delta;
                    if (baseri == null)
                    {
                        baseri = lastri = ri;
                    }
                    else
                    {
                        lastri.Next = ri;
                        lastri = ri;
                    }                  
                }
                break;
                
            case ORG:
                {
                    lex.Expect(Token.SPACE);
                    int i = ParseIntExpression(lex);
                    fPC = i;    
                }
            break;
            
            case ALIGN:
                //??
                break;
                
            case EQU:
                {
                    
                    lex.Expect(Token.SPACE);
                    e = ParseExpression(lex);
                    if (lab != null)
                        fSymbols.Put(RecordName + "." + lab, e);     
                }
                break;
                
            case SET:
                break;
                
            case ENDR:
                done = true;
                lex.Expect(Token.EOL);
                break;
                
            default: throw new AsmException(Error.E_UNEXPECTED, t);
            }          
        }
        // if origin, then we need to go through and adjust the
        // offsets.
        
        r.Children = baseri;
        r.Size = fPC;
        // create entries for them.
        AddRecordMembers(RecordName, baseri, 0, true);
        fRecords.Put(RecordName, r);

        if (RecordName != null)
            fSymbols.PopWith();

    }
    
    /*
     * updates the ri.  Returns the size of the item.
     */
    private int ParseRecordDS(Lexer lex, RecordItem ri) throws AsmException
    {
        String s = "";
        Record r;
        
        int qualifier = 'w';
        s = lex.LastToken().toString();
        if (s.indexOf('.') > 0) 
            qualifier = ctype.tolower(s.charAt(s.length() -1));
        
        int size = QualifierToInt(qualifier);
        
        lex.Expect(Token.SPACE);
        __Expression e = ParseExpression(lex); 
        lex.Expect(Token.EOL);
        
        s = e.toString();
        if (s != null && ((r = fRecords.Get(s)) != null))
        {
            ri.Children = r.Children;
            return r.Size;
        }
        
        Integer v = e.Value();
        if (v == null) throw new AsmException(Error.E_EXPRESSION, lex);
         
        return size * v.intValue();
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
    
    private static final boolean QualifierIsInt(int q)
    {
        switch(q)
        {
        case 'b':
        case 'w':
        case 'l':
            return true;
       default:
           return false;
        }    
    }
    
    private static final int QualifierToInt(int q)
    {
        switch(q)
        {
        case 'b':
            return 1;
        case 'w':
            return 2;
        case 'l':
            return 4;
        case 's':
            return 4;
        case 'd':
            return 8;
        case 'x':
            return 12;
        case 'p':
            return 12;
        default:
            return 0;
        }
    }
   
    /*
     * TODO --alignment padding as well?
     */
    private byte[] StringToByte(String s)
    {
        int extra = 0;
        int offset = 0;
        int length; 
        byte[] data;
        
        /*
         * TODO -- macro replacement within string.
         */
        
        length = s.length();
        
        switch(fStringMode)
        {
        case STRING_GSOS:
            offset = 2;
            extra = 2;
            break;
        case STRING_C:
            extra = 1;
            offset = 0;
            break;
        case STRING_PASCAL:
            // throw error if length > 255?
            extra = 1;
            offset = 1;
            break;
        case STRING_ASIS:
            extra = 0;
            offset = 0;
            break;
        }
   
        
        
        data = new byte[length + extra];
        
        for (int i = 0; i < length; i++)
        {
            int c = s.charAt(i);
            if (fMSB) c |= 0x80;
            data[i + offset] = (byte)c;
        }
        
        
        switch (fStringMode)
        {
        case STRING_PASCAL:
            data[0] = (byte)length;
            break;
        case STRING_GSOS:
            data[0] = (byte)(length & 0xff);
            data[1] = (byte)((length >> 8) & 0xff);
            break;
        case STRING_C:
            data[length] = 0;
            break;
        case STRING_ASIS:
            break;
                    
        }
        
        return data;
    }
    
    private void AddRecordMembers(String basename, RecordItem r, 
            int pc, boolean absolute)
    {
        while (r != null)
        { 
            /*
             * TODO -- sometimes, this should be a const expression...
             */
            fSymbols.Put(basename + "." + r.Name, 
                        absolute 
                            ? new ConstExpression(pc + r.Offset)
                            : new RelExpression(pc + r.Offset));

            if (r.Children != null)
            {
                AddRecordMembers(basename + "." + r.Name, 
                        r.Children, pc + r.Offset, absolute);
            }
            r = r.Next;
        }
    }
    
    class Record
    {
        int Size;
        RecordItem Children;
    }
    class RecordItem
    {
        RecordItem()
        {
            Name = null;
            Children = null;
            Offset = 0;
            Next = null;
        }
        
        String Name;
        RecordItem Children;
        int Offset;
        RecordItem Next;
    }
    

    
}
