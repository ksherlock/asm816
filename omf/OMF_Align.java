/*
 * OMF_Align.java
 *
 * Created on December 22, 2005, 3:21 PM
 */

package omf;



/**
 *
 * @author Kelvin
 */
public class OMF_Align extends OMF_Number {
    
	public OMF_Align(__OMF_Reader omf)
	{
		super(0xe0, omf);
	}
	public OMF_Align(int align)
	{
		super(0xe0, align);
	}
}

