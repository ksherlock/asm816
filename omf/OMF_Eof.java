/*
 * OMF_EOF.java
 *
 * Created on December 22, 2005, 3:12 PM
 */

package omf;

import java.io.*;

/**
 *
 * @author Kelvin
 */
public class OMF_Eof extends OMF_Opcode {
    
    /** Creates a new instance of OMF_EOF */
    public OMF_Eof() {
        super(0x00);
    }
    
    public int Save(OMF omf, FileOutputStream io) throws IOException 
    {

    	io.write(0x00);

    	return 1;
    }

    @Override
    public int CodeSize()
    {
        return 0;
    }

    @Override
    public void Save(__OMF_Writer out)
    {
        out.Write8(0x00);
    }
}
