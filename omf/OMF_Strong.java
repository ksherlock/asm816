/*
 * Created on Dec 23, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package omf;


/**
 * @author Kelvin
 *
 */
public class OMF_Strong extends OMF_Label {
	public OMF_Strong(__OMF_Reader omf)
	{
		super(0xe5, omf);
	}
	public OMF_Strong(String s)
	{
		super(0xe5, s);
	}
	public OMF_Strong(OMF_Label l)
	{
		super(0xe5, l);
	}
}
