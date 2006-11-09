


/*
 * Created on Mar 4, 2006
 * Mar 4, 2006 6:44:16 PM
 */

/*
 * <expression> :== <simple_expression> | <simple_expression> [ '=' '<>' '<='
 * '>=' '<' '>' ] <simple_expression>
 * 
 * <simple_expression> :== [ '+' '-' ] <term> | [ '+' '-' ] <term> [ + -
 * .OR. .EOR. ] <term>
 * 
 * <term> :== <factor> | <factor> [ '*' '/' '.AND.' '|' ] <factor>
 * 
 * <factor> :== <contant> | label | '(' expression ')' | .NOT. <factor> | *
 * 
 * <constant> :== ('+' '-')? NUMBER |SYMBOL | STRING (character constant)
 * 
 * 
 * 
 */

public class Operand extends Expression
{

    public Operand(AddressMode type)
    {
        super();
        fType = type;
    }
    public AddressMode Type()
    {
        return fType;
    }
    public void SetType(AddressMode type)
    {
        fType = type;
    }

    private AddressMode fType;
}
