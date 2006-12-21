package mpw;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import asm816.AddressMode;
import asm816.AsmException;
import asm816.ContextMap;
import asm816.Error;
import asm816.INSTR;
import asm816.JunkPile;
import asm816.Lexer;
import asm816.Parser;
import asm816.SymbolTable;
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
import omf.OMF_Using;


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
    private boolean fEnd;
    
    private JunkPile fData;
    private OMF_Segment fSegment;
    
    private SymbolTable fWith;
    
    private ContextMap<String, Record> fRecords;
    private ContextMap<String, Boolean> fExternals;
    private MPW_SymbolTable fSymbols;
    private ContextMap<String, Boolean> fExports;
    
    private HashMap<String, ArrayList<String>> fDataSegs;
    private HashMap<String, Macro> fMacros;
    
    private SymbolTable fMacroVars;
    
    private String fOpcode;
    private int fLine;
    
    private MPW_Lexer fLexer;
    private int fSegCounter;
    private String fSegname;
    private int fMacnum;
   
    private static final int STRING_PASCAL = 0;
    private static final int STRING_ASIS = 1;
    private static final int STRING_C = 2;
    private static final int STRING_GSOS = 3;
    
    public MPW_Parser()
    {
        super();

    }
    
    public void Reset()
    {
        super.Reset();

        fPC = 0;
        fSymbols = new MPW_SymbolTable();
        fRecords = new ContextMap<String, Record>();
        fExternals = new ContextMap<String, Boolean>();
        
        fDataSegs = new HashMap<String, ArrayList<String>>();
        
        fExports = new ContextMap<String, Boolean>();
        fMacros = new HashMap<String, Macro>();
        fMacroVars = new SymbolTable();
        
        
        fData = null;
        fSegment = null;
        fStringMode = STRING_ASIS;
        fMSB = false;
        fM = fX = true;
        fMachine = INSTR.m65816;
        
        fEndFile = false;  
        fEnd = false;
        fOpcode = "";
        fLexer = null;
        fSegCounter = 0;
        fSegname = "";
        fLine = 0;
        
        fMacnum = 0;
        
        fWith = null;
        
    }
    
    
    public void ParseFile(InputStream stream)
    {
        MPW_Lexer lex = new MPW_Lexer(stream);
        Parse(lex);
    }   
    
    private void Parse(MPW_Lexer lex)
    {
        MPW_Lexer oLexer = fLexer;
        
        fEndFile = false;
        fLexer = lex;
        lex.SetCase(fCase);
        for (;;)
        {           
            ParseLine(lex);
            if (fEndFile) break;
        }
        //fSymbols.PrintTable(System.out);
        
        if (fEndFile && !fEnd)
            fEndFile = false;
        
        fLexer = oLexer;
    }
    
    protected void AddDirectives()
    {
        fImpliedAnop = MPW_Directive.IMPLIED_ANOP;
        
        for (MPW_Directive i : MPW_Directive.values())
                fDirectives.put(i.name(), i);
        fDirectives.remove("IMPLIED_ANOP");
        
        
        //synonyms
        fDirectives.put("ENDP", MPW_Directive.ENDPROC);
        fDirectives.put("FUNCTION", MPW_Directive.PROC);
        fDirectives.put("ENDFUNCTION", MPW_Directive.ENDPROC);
        fDirectives.put("ENDF", MPW_Directive.ENDPROC);
        fDirectives.put("ENDM", MPW_Directive.MEND);
        
        
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
        fWith = null;
    }
    
    private void NewSegment(String lab, boolean priv, boolean data)
    {
        fSegment = new OMF_Segment();
        fSegment.SetSegmentName(lab);
        fData = new JunkPile();
        
        if (priv && (fExternals.Get(lab) != null)) priv = false;
        
        if (priv) fSegment.SetAttributes(OMF.KIND_PRIVATE);
        fSegment.SetKind(data ? OMF.KIND_DATA : OMF.KIND_CODE);
        
        fSegment.SetSegmentNumber(++fSegCounter);
        fSegment.SetLoadName(fSegname);
        fSymbols.Push(); 
        fRecords.Push();
        fExternals.Push();
        fExports.Push(); 
                
        fSymbols.Put(fSegment.SegmentName(), new RelExpression(0)); 

        fLexer.SetLocalLabel(lab);        
        
        // reset fStringMode?
        fMSB = false;
        fSymbols.ClearWith(); // shouldn't be necessary
        fWith = new SymbolTable();
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
    
    /*
     * TODO -- currently, 
     *  brx label
     * will create a RELEXPR record, even if label is known.
     * While not a problem (the linker should deal with it 
     * correctly), we could handle it here by converting the 
     * relative expression to a constant expression and doing
     * the branch math.
     * 
     */
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
        
        
        if (fOutfile != null)
            fSegment.Save(fOutfile);
    }

    
    private Line GetLine(Lexer lex) throws AsmException
    {
       Line l = new Line();
        
        Token t;
        String s;
        int type;
        
        
        t = lex.Expect(Token.EOF, Token.EOL, 
                Token.SPACE, Token.SYMBOL);
        
        type = t.Type();
        
        if (type == Token.EOL)
            return null;
        if (type == Token.EOF)
        {
            fEndFile = true;
            return null;
        }
        if (type == Token.SYMBOL)
        {
            l.lab = t.toString();
            t = lex.Expect(Token.SPACE, Token.EOL);
            
            if (t.Type() == Token.EOL)
                return l;
        }
        t = lex.Expect(Token.SYMBOL);
        
        l.opcode = t.toString().toUpperCase();
        
        l.operand = lex.Arguments(true);

        return l;
    }
    
    private MacroLine GetMacroLine(Lexer lex) throws AsmException
    {
        MacroLine l = new MacroLine();
     
        Token t;
        String s;
        int type;
        
        
        t = lex.Expect(Token.EOF, Token.EOL, 
                Token.SPACE, Token.MACRO_LAB, Token.MACRO_PARM);
        
        type = t.Type();
        
        if (type == Token.EOL)
            return null;
        if (type == Token.EOF)
        {
            fEndFile = true;
            return null;
        }
        // TODO -- only @labels should be allowed.
        if (type == Token.MACRO_LAB || type == Token.MACRO_PARM)
        {
            l.lab = t.toString();
            t = lex.Expect(Token.SPACE, Token.EOL);
            
            if (t.Type() == Token.EOL)
                return l;
        }
        t = lex.Expect(Token.SYMBOL);
        
        l.opcode = t.toString().toUpperCase();
        
        l.operand = lex.Arguments(true);

        return l;
    }
    
    
    protected void ParseLine(Lexer lex)
    {
        
        fLine = lex.Line();
        
        try
        {         

            Line l = GetLine(lex);
            
            if (l == null) return;
            DoLine(l.lab, l.opcode, l.operand);
        }
        catch (AsmException error)
        {
            error.SetLine(fLine);
            error.print(System.err);
            lex.SkipLine();
        }      
    }
    
    void DoLine(String lab, String opcode, __TokenIterator ti)
    throws AsmException
    {
        boolean inSeg = fSegment != null;
        boolean inData = inSeg ? fSegment.Kind() == OMF.KIND_DATA : false;
        fPC = fData == null ? 0 : fData.Size();
                
        fOpcode = opcode;
        MPW_Directive d;
        if (opcode == null)
        {
            d = MPW_Directive.IMPLIED_ANOP;
        }
        else d = (MPW_Directive)fDirectives.get(opcode);
        
        if (d != null)
        {
            boolean tf = ParseDirective(lab, d, ti);
            if (!tf)
                throw new AsmException(Error.E_UNEXPECTED, opcode);
           return;
        }
                    
        if (inSeg && !inData)
        {
            INSTR instr = fOpcodes.get(opcode);
            if (instr != null)
            {
                ParseInstruction(lab, instr, ti);
                
                return;
            }
            
            Macro m;
            m = fMacros.get(opcode);
            if (m != null)
            {
                ExpandMacro(lab, m, ti);
                return;
            }
            
        }
        throw new AsmException(Error.E_UNEXPECTED, opcode);
        
    }
    
    public void ExpandMacro(String lab, Macro m, __TokenIterator ti)
        throws AsmException
    {
        MacroLine ml;
        
        HashMap<String, Object> map = new HashMap<String, Object>();
        
        // TODO -- prevent infinite loops.
        
       fMacnum++;
       String labname = m.name + "@" + fMacnum;
        
        if (ti != null)
        {
            ArrayList<Token> tmp;
            tmp = new ArrayList<Token>();
            int index = 0;

            // TODO -- check if index out of bounds, etc.
            while (!ti.EndOfLine())
            {
                Token t = ti.Next();
                if (t.Type() == ',')
                {
                    map.put(m.args[index++], tmp.toArray());
                    tmp.clear();
                }
                else tmp.add(t);
            }
            map.put(m.args[index++], tmp.toArray());   
        }
        
               
        fMacroVars.Push();

        /*
         * if no macro label, do label here.
         * 
         */
        
        if (m.lab == null)
        {
            if (lab != null)
                DoLine(lab, null, null);
        }
        else
        {
            fMacroVars.Put(m.lab, new SymbolExpression(lab));
            map.put(m.lab, new Token(Token.SYMBOL, lab, null));
        }
        
        // map arguments to macro arguments.
        
        
        // TODO -- setup local variables and substitute
        ml = m.line;
        
        while (ml != null)
        {
            // TODO -- check if opcode is macro command
            // TODO -- label replacement for @/&
            // TODO -- variable replacement in strings.
            //ml.operand.Reset();
            
            ti = ml.operand;
            if (ti != null)
            {
                if (ti.Contains(Token.MACRO_PARM, Token.MACRO_LAB))
                    ti = new MacroIterator(ti, map, labname);
            }
            // TODO -- this is wrong... could be any parameter.
            String l = ml.lab;
            if (l != null)
            {
                int c = l.charAt(0);
                if (c == '@') l = labname + l;
                else if (c == '&' && l.compareTo(m.lab) == 0) l = lab;
            }
            
            
            DoLine(l, ml.opcode, ti);
            
            ml = ml.next;
        }   
        
        fMacroVars.Pop();
    }
    
    
    /*
     * TODO -- find a more OOP way to do this via a directive
     * class & inheritance?
     * 
     */
    protected boolean ParseDirective(String lab, Enum d, __TokenIterator ti) throws AsmException
    {
        boolean inSeg = fSegment != null;
        boolean inData = inSeg ? fSegment.Kind() == OMF.KIND_DATA : false;

        __Expression e = null;
        MPW_Directive dir = (MPW_Directive)d;
              
        
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
            case MACRO:
            case MEND:
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
        case LONGA:
            if (fMachine != INSTR.m65816) return false;
            fM = ParseOnOff(ti, true);
            break;
        case LONGI:
            if (fMachine != INSTR.m65816) return false;
            fX = ParseOnOff(ti, true);
            break;
        
        case SEG:
            {
                Token t = ti.Expect(Token.EOL, Token.STRING);
                if (t.Type() == Token.STRING)
                {
                    String s = t.toString();
                    if (inSeg)
                        fSegment.SetLoadName(s);
                    else
                        fSegname = s;
                    ti.Expect(Token.EOL);
                }
                
                else fSegname = "";
            }
            break;
            
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
            fEnd = true;
            fEndFile = true;
            break;
            
        case RECORD:
            if (inData)
                EndSegment(null);
            ParseRecord(ti, lab);
            lab = null;
            break;
            
        case MACRO:
            // if in data give error?
            ParseMacro(ti);
            break;
        
        case CASE:
            fCase = ParseOnOff(ti, false);
            fLexer.SetCase(fCase);
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
                int i = ti.ExpectSymbol("M65816", "M65C02", "M6502");
                ti.Expect(Token.EOL);
                
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
                if (ti.EndOfLine())
                    fStringMode = STRING_ASIS;
                else
                {
                    int i = ti.ExpectSymbol("PASCAL", "C", "ASIS", "GSOS");
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
                    ti.Expect(Token.EOL);            
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
            fWith.Pop();
            //fSymbols.PopWith();
            ti.Expect(Token.EOL);
            break;
            
        case IMPORT:
            ParseImport(ti);
            break;

        case EXPORT:
            ParseExport(ti);
            break;
            
            
        case INCLUDE:
            ParseInclude(ti);
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
        
        fLexer.SetLocalLabel(lab);
        
        if (fExports.Get(lab, false) != null)
            op = new OMF_Local(OMF.OMF_GLOBAL, lab, 0, 'N', false);
       
        else if (fSegment.Kind() == OMF.KIND_DATA)
            op = new OMF_Local(OMF.OMF_LOCAL, lab, 0, 'N', true);
            
        else return;
        
        fData.add(op);
    }
    
    protected boolean ParseInstruction(String lab, INSTR instr, __TokenIterator ti) 
    throws AsmException
    {
        int opcode = 0x00;
        int size;
        
        if (lab != null)
        {
            AddLabel(lab);
            fSymbols.Put(lab, new RelExpression(fPC));
        }

        boolean check_a = false;
        
        //__TokenIterator ti = lex.Arguments(true);
        
        // special case mvn/mvp
        switch (instr)
        {
        case MVN:
        case MVP:
            {   
                __Expression e1, e2;
                Expression ex1, ex2;
                
                e1 = ParseExpression(ti);
                ti.Expect(',');
                e2 = ParseExpression(ti);
                ti.Expect(Token.EOL);

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
            
            operand oper = ParseOperand(ti);
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
                if (v == null)
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
                if (v == null)                     
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
                if (v == null) 
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
                throw new AsmException(Error.E_OPCODE, instr.toString());
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
                
                if (opcode == 0x22)
                    ex.SetType(OMF.OMF_LEXPR);
                
                
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
                    fRecords.Put(lab, r);
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
    
    private void ParseRecord(__TokenIterator ti, String RecordName) throws AsmException
    {
        
        Token t;
        __Expression e;
        int type;
        
        Lexer lex = fLexer;
        
        if (RecordName == null)
        {
            throw new AsmException(Error.E_LABEL_REQUIRED);
        }
        
        /*
         * For now, we only support:
         * 
         * Data Segment:
         * RECORD [ENTRY|EXPORT]
         * 
         * RECORD <number>
         * 
         */
        
        t = ti.Peek();
        type = t.Type();
        // data segment.
        if (type == Token.SYMBOL || type == Token.EOL)
        {
            boolean export = false;
            
            if (type == Token.SYMBOL)
            {
                t = ti.Expect(Token.SYMBOL);
                int i = t.ExpectSymbol("ENTRY", "EXPORT");
                if (i == 2) export = true;
            }

            ti.Expect(Token.EOL);
            
            NewSegment(RecordName, !export, true);
            return;
        }
        
        t = ti.Expect(Token.NUMBER);
        ti.Expect(Token.EOL);
        
        int start = fPC = t.Value();
        
        fSymbols.PushWith();
        fSymbols.AddWith(RecordName);
        
        
        Record r = new Record();
        RecordItem baseri = null;
        RecordItem lastri = null;
        
        boolean done = false;
        
        for(;!done;)
        {
            MPW_Directive d;
            String lab = null;
            String s;
            
            
            t = lex.Expect(Token.EOL, Token.SPACE, Token.SYMBOL);
            type = t.Type();
            if (type == Token.EOL) continue;
            
            if (type == Token.SYMBOL)
            {
                lab = t.toString();
                t = lex.Expect(Token.EOL, Token.SPACE);
                if (t.Type() == Token.EOL)
                {
                    fSymbols.Put(RecordName + "." + lab,
                            new RelExpression(fPC));
                    continue;
                }
            }
            
            t = lex.Expect(Token.SYMBOL);
            s = t.toString().toUpperCase();
            fOpcode = s;
            
            ti = lex.Arguments(true);

            d = (MPW_Directive)fDirectives.get(s);
            if (d == null)
            {
                throw new AsmException(Error.E_UNEXPECTED, t);
            }
            
            switch(d)
            {
            case DS:
                {
                    RecordItem ri = new RecordItem();
                    ri.Offset = fPC;
                    ri.Name = lab;
                    int delta = ParseRecordDS(ti, ri, DotSize('w'));
                                           
                    fSymbols.Put(RecordName + "." + lab, 
                        new ConstExpression(fPC));
           
                    fPC += delta;                
                    
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
                
            case EQU:
                
            case ORG:
            case ALIGN:
            case SET:
                break;
            
            case END:
                fEnd = true;
                fEndFile = true;
            case ENDR:
                done = true;
                ti.Expect(Token.EOL);
                break;
            default:
                throw new AsmException(Error.E_UNEXPECTED, t);
            }   
        }
        
        r.Children = baseri;
        r.Size = fPC;
        // create entries for them.
        AddRecordMembers(RecordName, baseri, 0, true);
        fRecords.Put(RecordName, r);
        
        fSymbols.PopWith();
    }
    
    /*
     * updates the ri.  Returns the size of the item.
     */
    private int ParseRecordDS(__TokenIterator ti, RecordItem ri, int q) throws AsmException
    {
        String s = "";
        Record r;
            
        int size = QualifierToInt(q);
        

        __Expression e = ParseExpression(ti); 
        e = e.Simplify(fSymbols, false);
        ti.Expect(Token.EOL);
        
        s = e.toString();
        if (s != null && ((r = fRecords.Get(s)) != null))
        {
            ri.Children = r.Children;
            return r.Size;
        }
        
        Integer v = e.Value();
        if (v == null) throw new AsmException(Error.E_EXPRESSION);
         
        return size * v.intValue();
    }

    // TODO -- disallow if in a macro.
    private void ParseMacro(__TokenIterator ti) 
        throws AsmException
    {
        Macro m = new Macro();
        MacroLine first;
        MacroLine last;
        MacroLine ml;
        String name = null;
        
        ti.Expect(Token.EOL);
        
        // TODO -- parameters and stuff...
        
        first = last = null;
        boolean done = false;
        for(int i = 0;!done;)
        {
            ml = GetMacroLine(fLexer);
            if (ml == null)
            {
                // error
                if (fEnd) return;
                continue;
            }
            i++;
            
            // todo -- label[0] must be @ or &
            String s = ml.opcode;
            if (s != null)
            {
                MPW_Directive d;
                d = (MPW_Directive)fDirectives.get(s);
                if (d != null)
                    switch(d)
                    {
                    case MEND:
                        ml.operand = null;
                        ml.opcode = null;
                        done = true;
                        break;
                    case MACRO:
                        throw new AsmException(Error.E_UNEXPECTED, s);

                    }
            }
            // header
            if (i == 1)
            {
                if (s == null)
                    throw new AsmException(Error.E_MACRO_NAME, "");
                
                if (fDirectives.get(s) != null 
                        || fOpcodes.get(s) != null)
                    throw new AsmException(Error.E_MACRO_NAME, name);
                
                // todo -- qualifier/arguments
                m.lab = ml.lab;
                name = s;
                
                if (ml.operand != null)
                {
                    // TODO -- should be MACRO_PARM, not symbol...
                    __TokenIterator ai = ml.operand;
                    ArrayList<Token> list = ai.toList(Token.MACRO_PARM);
                    
                    int size = list.size();
                    m.args = new String[size];
                    for (int j = 0; j < size; j++)
                        m.args[j] = list.get(j).toString();                    
                }
                
                continue;
            }
            // if this was from a MEND line, it 
            // may be empty.
            if (ml.lab != null || ml.opcode != null)
            {
                if (first == null)
                {
                    first = last = ml;                
                }
                else
                {
                    last.next = ml;
                    last = ml;
                }
            }
            
        }
        m.line = first;
        m.name = name;
        fMacros.put(name, m);
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
        
        //fSymbols.PushWith();
        fWith.Push();
        
        ArrayList<Token> tokens = ti.toList(Token.SYMBOL);
       
        for (Token t : tokens)
        {
            ArrayList<String> al;
            String s = t.toString();
            
            // if it's a data segment, then add a USING
            // record and import the externals.
            if ((al = fDataSegs.get(s)) != null)
            {
                for (String s2 : al)
                {
                    fExternals.Put(s2, Boolean.TRUE);
                }
                fData.add(new OMF_Using(s));
                continue;
            }
            
            // otherwise, add the data to fWith.
            // TODO check for xx.xxx
            
            Record r = fRecords.Get(s);
            if (r == null)
            {
                System.err.printf("%1$s : unknown record\n",
                        s);
                continue;
            }
            
            AddRecord(fWith, r, s);          
        }
        
        
        //fWith.PrintTable(System.out);
        
        /*
        for (Token t : tokens)
            fSymbols.AddWith(t.toString());
        */
    }

    private void ParseInclude(__TokenIterator ti) throws AsmException
    {
        Token t;
        String s;
        t = ti.Expect(Token.STRING);
        ti.Expect(Token.EOL);
        
        s = t.toString();
        
        Include(s);
        
    }
    
    
    private __Expression ParseExpression(Lexer lex) throws AsmException
    {
        __Expression e;
        
        e = ExpressionParser.Parse(lex, fPC);
        
        return e.Simplify(fSymbols, false);     
    }
    
    protected ComplexExpression ParseExpression(__TokenIterator iter) throws AsmException
    {       
        ComplexExpression exp =  MPWExpression.Parse(iter, fPC);
        if (fWith != null) exp.Remap(fWith);
        return exp;
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
            throw new AsmException(Error.E_EXPRESSION);
        
        return v.intValue();
    }
    
    private int ParseIntExpression(__TokenIterator ti) throws AsmException
    {      
        ComplexExpression ce;
        __Expression e;
        
        ce = MPWExpression.Parse(ti, fPC); 
        if (fWith != null) ce.Remap(fWith);
        e = ce.Simplify(fSymbols, true);
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
    
    
    private void AddRecord(SymbolTable st, Record r, String as)
    {
        AddRecord(st, r.Children, as, "");   
    }
    // add record members to the symboltable.
    private void AddRecord(SymbolTable st, RecordItem r, String path, String prefix)
    {
        while (r != null)
        {
            __Expression e;
            // TODO -- any reason NOT to put the actual value?
            /*
            e = new SymbolExpression(path + "." + r.Name);
            
            */
            e = fSymbols.Get(path + "." + r.Name);
            
            st.Put(prefix + r.Name, e,true);
            
            if (r.Children != null)
                AddRecord(st, r.Children, path + "." + r.Name,  prefix + r.Name + ".");
            
            r = r.Next;
        }
        
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
    

    class Line
    {
        public Line()
        {
            lab = null;
            opcode = null;
            operand = null;
        }
        public String lab;
        public String opcode;
        public __TokenIterator operand;
    }
    
    class MacroLine extends Line
    {
        public MacroLine()
        {
            super();
            next = null;
        }
        public MacroLine next;
    }
    
    class Macro
    {
        public Macro()
        {
            lab = null;
            qualifier = null;
            args = null;
            line = null;
            name = null;
        }
        String name;
        String lab;
        String qualifier;
        String[] args;
        MacroLine line;
    }
}
