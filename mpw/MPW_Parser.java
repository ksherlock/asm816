package mpw;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import asm816.AddressMode;
import asm816.AsmException;
import asm816.ContextMap;
import asm816.Error;
import asm816.INSTR;
import asm816.JunkPile;
import asm816.Lexer;
import asm816.Parser;
import asm816.Token;
import asm816.__TokenIterator;
import asm816.ctype;

import expression.*;

import omf.OMF;
import omf.OMF_DS;
import omf.OMF_Eof;
import omf.OMF_Local;
import omf.OMF_Opcode;
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


public class MPW_Parser extends Parser
{
    private int fStringMode;
    private int fAlign;
    private boolean fMSB;

    private boolean fEndFile;
    
    private JunkPile fData;
    private OMF_Segment fSegment;
    
    private ContextMap<String, Record> fRecords;
    private ContextMap<String, Boolean> fExternals;
    private MPW_SymbolTable fSymbols;
    private ContextMap<String, Boolean> fExports;
    
    private HashMap<String, ArrayList<String>> fDataSegs;
    private String fOpcode;
    
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
        fExternals = new ContextMap<String, Boolean>();
        
        fDataSegs = new HashMap<String, ArrayList<String>>();
        
        fExports = new ContextMap<String, Boolean>();
        
        
        fData = null;
        fSegment = null;
        fStringMode = STRING_ASIS;
        fMSB = false;
        fM = fX = true;
        fMachine = INSTR.m65816;
        
        fEndFile = false;  
        fOpcode = "";
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
        fImpliedAnop = MPW_Directive.IMPLIED_ANOP;
        
        for (MPW_Directive i : MPW_Directive.values())
                fDirectives.put(i.name(), i);
        fDirectives.remove("IMPLIED_ANOP");
        
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
    
    protected void AddOpcodes()
    {
        super.AddOpcodes();

        // synonyms
        fOpcodes.put("BLT", INSTR.BCC);
        fOpcodes.put("BGE", INSTR.BCS);
        fOpcodes.put("CPA", INSTR.CMP);
        
        fOpcodes.put("TSA", INSTR.TSC);
        fOpcodes.put("TAS", INSTR.TCS);
        fOpcodes.put("TAD", INSTR.TCD);
        fOpcodes.put("TDA", INSTR.TDC);
        
        fOpcodes.put("SWA", INSTR.XBA);
    }
    
    private void EndSegment(String lab)
    {
        if (lab != null)
        {
            fSymbols.Put(lab, new RelExpression(fPC));
            AddLabel(lab);
        }
        
        Reduce();
        fSymbols.PrintTable(System.out);
        
        fSymbols.ClearWith();
        
        fRecords.Pop();
        fExternals.Pop();
        fSymbols.Pop();
        fExports.Pop();
        
        
        fData = null;
        fSegment = null;
    }
    
    private void NewSegment(String lab, boolean priv, boolean data)
    {
        fSegment = new OMF_Segment();
        fSegment.SetSegmentName(lab);
        fData = new JunkPile();
        
        if (priv && (fExternals.Get(lab) != null)) priv = false;
        
        if (priv) fSegment.SetAttributes(OMF.KIND_PRIVATE);
        fSegment.SetKind(data ? OMF.KIND_DATA : OMF.KIND_CODE);
        
        fSymbols.Push(); 
        fRecords.Push();
        fExternals.Push();
        fExports.Push(); 
        
        
        fSymbols.Put(fSegment.SegmentName(), new RelExpression(0)); 

        // reset fStringMode?
        fMSB = false;
        fSymbols.ClearWith(); // shouldn't be necessary
    }
    
    protected void ParseSegment(String lab, __TokenIterator ti) throws AsmException 
    {
        boolean export = false;
        
        Token t;
        String s;
        
        t = ti.Expect(Token.SYMBOL, Token.EOL);
        
        if (t.Type() == Token.SYMBOL)
        {
            int i = t.ExpectSymbol("ENTRY", "EXPORT");
            if (i == 2) export = true;
            ti.Expect(Token.EOL);
        }
        
        NewSegment(lab, !export, false);
       
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
   }
    
    // everytime simplification happens, the current WITH could
    // affect it, even if it shouldn't... 
    // should have a SymbolTAble for WITH definitions, simplify
    // when an expression is originally parsed.  fSymbols is then
    // a normal SymbolTable.
    
    protected void Reduce()
    {
        
        FileOutputStream outf = null;
        try
        {
            outf = new FileOutputStream("mpwout.o");
        }
        catch (FileNotFoundException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } 
        
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
        fSegment.Save(outf);
    }

    protected void ParseLine(Lexer lex)
    {
        Token t;
        String s;
        String lab = null;
        
        boolean inSeg = fSegment != null;
        boolean inData = inSeg ? fSegment.Kind() == OMF.KIND_DATA : false;
        fPC = fData == null ? 0 : fData.Size();
        
        
        
        try
        {         
            t = lex.Expect(Token.SPACE, Token.EOF, Token.EOL, Token.SYMBOL);

            int type = t.Type();
            
            if (type == Token.EOF)
            {
                // TODO -- should warn if not currently inSeg.
                fEndFile = true;
                return;
            }
            if (type == Token.EOL)
            {
                return;
            }
            if (type == Token.SYMBOL)
            {
                Token oldt = t;
                lab = t.toString();
                // check for '@' label
                if (lab.indexOf('@') >= 0)
                {
                    // TODO -- warn if not in a segment.
                }
                else if (inSeg)
                {
                    ((MPW_Lexer)lex).SetLocalLabel(lab);
                }
                t = lex.Expect(Token.EOL, Token.SPACE);
                if (t.Type() == Token.EOL)
                {
                    boolean tf = ParseDirective(lab, lex, MPW_Directive.IMPLIED_ANOP);
                    if (!tf)
                    {
                        throw new AsmException(Error.E_UNEXPECTED, oldt);
                    }
                    return;
                }
            }

            t = lex.Expect(Token.SYMBOL);
            s = t.toString().toUpperCase();
            fOpcode = s;

            
            MPW_Directive dir = (MPW_Directive)fDirectives.get(s);
            if (dir != null)
            {
                boolean tf = ParseDirective(lab, lex, dir);
                if (!tf)
                    throw new AsmException(Error.E_UNEXPECTED, t);
               return;
            }
            
            if (inSeg && !inData)
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
    
    /*
     * TODO -- find a more OOP way to do this via a directive
     * class & inheritance?
     * 
     */
    protected boolean ParseDirective(String lab, Lexer lex, Enum d) throws AsmException
    {
        boolean inSeg = fSegment != null;
        boolean inData = inSeg ? fSegment.Kind() == OMF.KIND_DATA : false;

        __Expression e = null;
        MPW_Directive dir = (MPW_Directive)d;
        
        __TokenIterator ti = lex.Arguments(true);
        
        // add a label except for these:
        if (inSeg && lab != null)
        {
            switch(dir)
            {
            case EQU:
            case END:
            case PROC:
            case ENDPROC:
            case RECORD:
            case ENDR:            
                break;
                
                /* TODO -- once ALIGN is figured out, 
                 * DC/DCB/DS might go here too. 
                 */
                
            default:
                AddLabel(lab);
                fSymbols.Put(lab, new RelExpression(fPC));
            }
        }
        
        switch(dir)
        {
        case IMPLIED_ANOP:
            // EOL already processed.
            if (!inSeg) return false;
            break;
        
        case EQU:
            {
                e = ParseExpression(ti);    
                
                if (lab != null)
                {
                    e = e.Simplify(fSymbols, false);
                    fSymbols.Put(lab, e);
                    
                    if (inData)
                    {
                        Expression ex = new Expression(e);
                        ex.SetName(lab);
                        ex.SetType(OMF.OMF_GEQU);
                        ex.SetSize(0);
                        fData.add(ex);
                    }
                    
                }
                e = null;
                lab = null;
            }
            break;
        
        case PROC:
            if (inSeg) EndSegment(null);
            ParseSegment(lab, ti);
            lex.SetLocalLabel(lab);

                
            lab = null;
            break;
            
        case ENDR:
        case ENDPROC:
                
            if (inSeg == false) return false;
    
            EndSegment(lab);
            lab = null;
            break;
            
        case END:
            if (inSeg)
            {    
                EndSegment(lab);
                lab = null;                
            }
            fEndFile = true;
            break;
            
        case RECORD:
            if (inData)
                EndSegment(null);
            ParseRecord(lex, lab);
            lab = null;
            break;
            
        
        case CASE:
            fCase = ParseOnOff(ti, false);
            lex.SetCase(fCase);
            break;
        
        case DS:
            if (!inSeg) return false;
            ParseDS(ti, lab, DotSize('w'));
            break;
            
        case DC:
            if (!inSeg) return false;
            ParseDC(ti, lab, DotSize('w'));
            break;
            
        case DCB:
            if (!inSeg) return false;
            ParseDCB(ti, lab, DotSize('w'));
            break;
            
            
        case MACHINE:
            {
                lex.Expect(Token.SPACE);                
                int i = lex.ExpectSymbol("M65816", "M65C02", "M6502");
                lex.Expect(Token.EOL);
                
                switch(i)
                {
                case 1:
                    fMachine = INSTR.m65816;
                    fM = fX = true;
                    break;
                case 2:
                    fMachine = INSTR.m65c02;
                    fM = fX = false;
                    break;
                case 3:
                    fMachine = INSTR.m6502;
                    fM = fX = false;
                    break;
                }          
            }
            break;
            
        case MSB:
            fMSB = ParseOnOff(ti, false);
            break;            
            
        case STRING:
            {
                Token t;
                t = lex.Expect(Token.EOL, Token.SPACE);
                if (t.Type() == Token.EOL)
                    fStringMode = STRING_PASCAL;
                else
                {
                    int i = lex.ExpectSymbol("PASCAL", "C", "ASIS", "GSOS");
                    switch(i)
                    {
                    case 1:
                        fStringMode = STRING_PASCAL;
                        break;
                    case 2:
                        fStringMode = STRING_C;
                        break;
                    case 3:
                        fStringMode = STRING_ASIS;
                        break;
                    case 4:
                        fStringMode = STRING_GSOS;
                        break;
                    }
                    lex.Expect(Token.EOL);            
                }
            }
            break;
            
            /*
             * WITH name[,name]
             * 
             * dual purpose --- equivalent to USING 
             * if name is a dat segment.
             * 
             */
        case WITH:
            if (!inSeg) return false;
            ParseWith(ti);
            break;
            
        case ENDWITH:
            if (!inSeg) return false;
            fSymbols.PopWith();
            ti.Expect(Token.EOL);
            break;
            
        case IMPORT:
            ParseImport(ti);
            break;

        case EXPORT:
            ParseExport(ti);
            break;
            
            
        default:
            return false;
        }        
        return true;
    }
    
    private void AddLabel(String lab)
    {
        if (lab == null || fSegment == null) return;
        if (lab.indexOf('@') != -1) return; // local label.
        
        OMF_Opcode op = null;
        
        if (fExports.Get(lab, false) != null)
            op = new OMF_Local(OMF.OMF_GLOBAL, lab, 0, 'N', false);
       
        else if (fSegment.Kind() == OMF.KIND_DATA)
            op = new OMF_Local(OMF.OMF_LOCAL, lab, 0, 'N', true);
            
        else return;
        
        fData.add(op);
    }
    
    protected boolean ParseInstruction(String lab, Lexer lex, INSTR instr) throws AsmException
    {
        int opcode = 0x00;
        int size;
        
        if (lab != null)
        {
            AddLabel(lab);
            fSymbols.Put(lab, new RelExpression(fPC));
        }

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
            opcode = -1;
            
            // TODO -- warn if we need to truncate a number.
            if (mode == AddressMode.ASSUMED_ABS)
            {
                Integer v = e.Value();
                if (e == null)
                    opcode = instr.find_opcode(fMachine, AddressMode.ABS, AddressMode.ABSLONG, AddressMode.DP);

                else
                {
                    int i = v.intValue();
                    if (i <= 0xff)
                        opcode = instr.find_opcode(fMachine, AddressMode.DP, AddressMode.ABS, AddressMode.ABSLONG);

                    else if (i > 0xffff)                        
                        opcode = instr.find_opcode(fMachine, AddressMode.ABSLONG, AddressMode.ABS, AddressMode.DP);

                    else 
                        opcode = instr.find_opcode(fMachine, AddressMode.ABS, AddressMode.ABSLONG, AddressMode.DP); 
                }
            }
            else if (mode == AddressMode.ASSUMED_ABS_X)
            {
                Integer v = e.Value();
                if (e == null)                     
                    opcode = instr.find_opcode(fMachine, AddressMode.ABS_X, AddressMode.ABSLONG_X, AddressMode.DP_X);

                else
                {
                    int i = v.intValue();
                    if (i <= 0xff)
                        opcode = instr.find_opcode(fMachine, AddressMode.DP_X, AddressMode.ABS_X, AddressMode.ABSLONG_X);

                    else if (i > 0xffff)                        
                        opcode = instr.find_opcode(fMachine, AddressMode.ABSLONG_X, AddressMode.ABS_X, AddressMode.DP_X);

                    else 
                        opcode = instr.find_opcode(fMachine, AddressMode.ABS_X, AddressMode.ABSLONG_X, AddressMode.DP_X); 
                }            
            }
            else if (mode == AddressMode.ASSUMED_ABS_Y)
            {
                Integer v = e.Value();
                if (e == null) 
                    opcode = instr.find_opcode(fMachine, AddressMode.ABS_Y, AddressMode.DP_Y);
                else
                {
                    int i = v.intValue();
                    if (i <= 0xff)
                        opcode = instr.find_opcode(fMachine, AddressMode.DP_Y, AddressMode.ABS_Y);

                    else                     
                        opcode = instr.find_opcode(fMachine, AddressMode.ABS_Y, AddressMode.DP_Y);
                }                  
            }
            else
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
 
        t = lex.Expect(Token.SPACE, Token.EOL);
        if (t.Type() == Token.EOL) return blank;
        
        
        int i = lex.ExpectSymbol("ON", "YES", "Y", "OFF", "NO", "N");
        lex.Expect(Token.EOL);
        switch(i)
        {
        case 1:
        case 2:
        case 3:
            return true;
        case 4:
        case 5:
        case 6:
            return false;
        }
        return blank; // not possible.
    }
    
    private boolean ParseOnOff(__TokenIterator ti, boolean blank) throws AsmException
    {
        Token t;
 
        t = ti.Expect(Token.SYMBOL, Token.EOL);
        
        if (t.Type() == Token.SYMBOL)
        {
            int i = t.ExpectSymbol("ON", "YES", "Y", "OFF", "NO", "N");
            ti.Expect(Token.EOL);
            switch(i)
            {
            case 1:
            case 2:
            case 3:
                return true;
            case 4:
            case 5:
            case 6:
                return false;
            }
        }
        return blank; // not possible.
    }    
    
    /*
     * not currently needed.
     * 
     */
    private int DotSize(int blank)
    {
        if (fOpcode == null) return blank; //
        if (fOpcode.indexOf('.') > 0) 
            return ctype.tolower(fOpcode.charAt(fOpcode.length() -1));
        return blank;
    }
    
    
    /*
     *  DCB[.size] expression,(expression|string)
     *  
     *  first expression is repeat count.
     */
    
    private void ParseDCB(__TokenIterator ti, String lab, int q)
    throws AsmException
    {
        int size = QualifierToInt(q);
        __Expression e;
        String s;
        Token t;
        
        int repeat = ParseIntExpression(ti);
        ti.Expect(',');
        
        t = ti.Peek();
        if (t.Type() == Token.STRING)
        {
            t = ti.Expect(Token.STRING);
            s = t.toString();
            // todo -- check if int or float
            // todo -- null pad to correspond with qalifier.
            
            byte[] data = StringToByte(s);
            for (int i = 0; i < repeat; i++)
                fData.add(data);       
        }
        else
        {
            e = ParseExpression(ti);
            e = e.Simplify(fSymbols, false);
            Expression ex = new Expression(e);
            ex.SetSize(QualifierToInt(size));
            for (int i = 0; i < repeat; i++)
                fData.add(ex);          
        }
        
        ti.Expect(Token.EOL);
    }
    
    /*
     * DC[.size] (expression|string)[,(expression|string)]
     * 
     */
    private void ParseDC(__TokenIterator ti, String lab, int q)
    throws AsmException
    {
        int size = QualifierToInt(q);
        __Expression e;
        String s;
        Token t;
        
     
        for (;;)
        {
            t = ti.Peek();
            if (t.Type() == Token.STRING)
            {
                t = ti.Expect(Token.STRING);
                s = t.toString();
                // TODO -- check if a float.
                fData.add(StringToByte(s));
                // TODO -- if size == 2 or 4, align.
            }
            else
            {
                e = ParseExpression(ti);
                e = e.Simplify(fSymbols, false);
                Expression ex = new Expression(e);
                ex.SetSize(size);
                fData.add(ex); 
            }
            t = ti.Expect(Token.EOL, (int)',');
            if (t.Type() == Token.EOL) break;
        }
    }
    
    /*
     * DS[.size] (record-name | expression)
     * 
     * if record name, creates equates for all members.
     * 
     */
    
    private void ParseDS(__TokenIterator ti, String lab, int q)
    throws AsmException
    {
        int size = QualifierToInt(q);
        __Expression e;
        String s;
        
        e = ParseExpression(ti);
        e = e.Simplify(fSymbols, false);
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

       
        /*
         * To make life easier, we'll only support
         * RECORD NUMBER EOL
         * RECORD (EXPORT | ENTRY) EOL
         * RECORD EOL
         */
   
        boolean export = false;
        boolean segment = false;
        
        t = lex.Expect(Token.SPACE, Token.EOL);
        if (t.Type() == Token.EOL)
        {
            segment = true;
            export = false;
        }
        else
        {
            t = lex.Expect(Token.SYMBOL, Token.NUMBER);
            if (t.Type() == Token.SYMBOL)
            {
                int i = t.ExpectSymbol("EXPORT", "ENTRY");
                switch(i)
                {
                case 1: // export
                    export = true;
                case 2: // entry
                    segment = true;
                }
            }
            else
            {
                offset = t.Value();
            }
        }
        if (segment)
        {
            NewSegment(RecordName, !export, true);
            ((MPW_Lexer)lex).SetLocalLabel(fSegment.SegmentName());
            return;
        }
        
        
        
        /*
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
        */
 
        if (RecordName != null)
        {
            // allow record members to be accessed.
            // within the record.
            fSymbols.PushWith();
            fSymbols.AddWith(RecordName);
        }        
        
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

    
    /*
     * EXPORT symbol[,symbol]*
     * 
     */
    private void ParseExport(__TokenIterator ti) throws AsmException
    {
        ArrayList<Token> tokens = ti.toList(Token.SYMBOL);
       
        for (Token t : tokens)
            fExports.Put(t.toString(), Boolean.TRUE);
    }
        
    /*
     * MPW says:
     * IMPORT --> implist [,implist]*
     * implist --> '(' symbol [,symbol] ')' [imptype]
     * implist --> symbol [imptype] [, symbol [imptype]]*
     * imptype -> : (CODE | DATA | record name)
     * 
     * I say: 
     * IMPORT symbol [,symbol]*
     */
    private void ParseImport(__TokenIterator ti) throws AsmException
    {
        ArrayList<Token> tokens = ti.toList(Token.SYMBOL);
       
        for (Token t : tokens)
            fExternals.Put(t.toString(), Boolean.TRUE);
    }
    

    /*
     * WITH symbol[,symbol]
     * TODO -- with can also be equivalent to USING.
     * in that case, we should import all labels from the 
     * record/data segment.
     */
    private void ParseWith(__TokenIterator ti) throws AsmException
    {
        
        fSymbols.PushWith();
        
        ArrayList<Token> tokens = ti.toList(Token.SYMBOL);
       
        for (Token t : tokens)
            fSymbols.AddWith(t.toString());
    }
    
    
    private __Expression ParseExpression(Lexer lex) throws AsmException
    {
        __Expression e;
        
        e = ExpressionParser.Parse(lex, fPC);
        
        return e.Simplify(fSymbols, false);     
    }
    
    protected ComplexExpression ParseExpression(__TokenIterator iter) throws AsmException
    {       
        return MPWExpression.Parse(iter, fPC);   
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
    private int ParseIntExpression(__TokenIterator ti) throws AsmException
    {        
        __Expression e;
        
        e = MPWExpression.Parse(ti, fPC); 
        e = e.Simplify(fSymbols, true);
        Integer v = e.Value();
        
        if (v == null)
            throw new AsmException(Error.E_EXPRESSION);
        
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
