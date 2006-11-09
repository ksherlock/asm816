/*
 * OMF_Opcode.java
 *
 * Created on December 22, 2005, 3:12 PM
 */

package omf;

/**
 *
 * @author Kelvin
 */
public abstract class OMF_Opcode {
    protected  int fOpcode;
    
    
    protected OMF_Opcode(int opcode)
    {
        fOpcode = opcode;
    }
    public int Opcode()
    {
        return fOpcode;
    }
    
    
   /*
    * size when when loaded into memory.
    */
    abstract public int CodeSize();
    
    /*
     * size of the code/data in the OMF segment.
     */ 
    public int Size(OMF omf)
    {
    	return 0;
    }
    /*
     * Save the data.
     */
    public abstract void Save(__OMF_Writer out);
    
    
 
}
