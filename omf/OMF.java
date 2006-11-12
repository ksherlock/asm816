/*
 * OMF.java
 *
 * Created on December 21, 2005, 4:29 PM
 */

package omf;
import java.io.*;
import java.util.ArrayList;

/**
 *
 * @author Kelvin
 */
public final class OMF {
    
    public static final int OMF_EOF = 0x00;
    public static final int OMF_ALIGN = 0xe0;
    public static final int OMF_ORG = 0xe1;
    public static final int OMF_RELOC = 0xe2;
    public static final int OMF_INTERSEG = 0xe3;
    public static final int OMF_USING = 0xe4;
    public static final int OMF_STRONG = 0xe5;
    public static final int OMF_GLOBAL = 0xe6;
    public static final int OMF_GEQU = 0xe7;
    public static final int OMF_MEM = 0xe8;
    public static final int OMF_EXPR = 0xeb;
    public static final int OMF_ZPEXPR = 0xec;
    public static final int OMF_BKEXPR = 0xed;
    public static final int OMF_RELEXPR = 0xee;
    public static final int OMF_LOCAL = 0xef;
    public static final int OMF_EQU = 0xf0;
    public static final int OMF_DS = 0xf1;
    public static final int OMF_LCONST = 0xf2;
    public static final int OMF_LEXPR = 0xf3;
    public static final int OMF_ENTRY = 0xf4;
    public static final int OMF_CRELOC = 0xf5;
    public static final int OMF_CINTERSEG = 0xf6;
    public static final int OMF_SUPER = 0xf7;
    
    
    public static final int TYPE_CODE = 0x00;
    public static final int TYPE_DATA = 0x01;
    public static final int TYPE_JUMPTABLE = 0x02;
    public static final int TYPE_PATHNAME = 0x04;
    public static final int TYPE_LIBDICT = 0x08;
    public static final int TYPE_INIT = 0x10;
    public static final int TYPE_ABSBANK = 0x11;
    public static final int TYPE_DP = 0x12;
    
    public static final int TYPE_POSIND = 0x20;      // position independent
    public static final int TYPE_PRIVATE = 0x40;     // private
    public static final int TYPE_DYNAMIC = 0x80;     // dynamic

    public static final int KIND_CODE = 0x00;
    public static final int KIND_DATA = 0x01;
    public static final int KIND_JUMPTABLE = 0x02;
    public static final int KIND_PATHNAME = 0x04;
    public static final int KIND_LIBDICT = 0x08;
    public static final int KIND_INIT = 0x10;
    public static final int KIND_DP = 0x12;    
    
    public static final int KIND_BANKREL = 0x0100;
    public static final int KIND_SKIPSEG = 0x0200;
    public static final int KIND_RELOADSEG = 0x0400;
    public static final int KIND_ABSBANK = 0x0800;
    public static final int KIND_NOSPEC = 0x1000;
    public static final int KIND_POSIND = 0x2000;
    public static final int KIND_PRIVATE = 0x4000;
    public static final int KIND_DYNAMIC = 0x8000;
            
    private OMF() {}
    
    /*
     * load all the segments in a file.
     * return null on error, an ArrayList of segments on success.
     */
    public static ArrayList<OMF_Segment> LoadFile(File f)
    {
        ArrayList<OMF_Segment>segments = new ArrayList<OMF_Segment>();
        
        try
        {
            
            FileInputStream io = new FileInputStream(f);

            while (io.available() > 0)
            {
                OMF_Segment o = new OMF_Segment(io);
                if (o.Error())
                    return null;
                segments.add(o);
            }
        }
        catch (Exception e)
        {
            return null;
        }
        
        return segments;
    }
}
