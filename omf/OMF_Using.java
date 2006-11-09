/*
 * Created on Dec 23, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package omf;

/**
 * @author Kelvin

 */
public class OMF_Using extends OMF_Label {
	public OMF_Using(__OMF_Reader omf)
	{
		super(0xe4, omf);
	}
	public OMF_Using(String s)
	{
		super(0xe4, s);
	}
	public OMF_Using(OMF_Label l)
	{
		super(0xe4, l);
	}
}
