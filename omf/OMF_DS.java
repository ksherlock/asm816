/*
 * Created on Dec 25, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package omf;

/**
 * @author Kelvin

 */
public class OMF_DS extends OMF_Number {
	public OMF_DS(__OMF_Reader omf)
	{
		super(0xf1, omf);
	}
	public OMF_DS(int space)
	{
		super(0xf1, space);
	}
	public int CodeSize()
	{
		return Value();
	}
}
