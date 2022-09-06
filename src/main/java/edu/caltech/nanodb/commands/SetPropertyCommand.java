package edu.caltech.nanodb.commands;


import edu.caltech.nanodb.expressions.Expression;
import edu.caltech.nanodb.server.NanoDBServer;
import edu.caltech.nanodb.server.properties.PropertyRegistry;


/** Implements the "SET VARIABLE ..." command. */
public class SetPropertyCommand extends Command {

    /** The name of the property to set. */
    private String propertyName;

    /** The value to set the property to. */
    private Expression valueExpr;


    public SetPropertyCommand(String propertyName, Expression valueExpr) {
        super(Command.Type.UTILITY);

        this.propertyName = propertyName;
        this.valueExpr = valueExpr;
    }


    @Override
    public void execute(NanoDBServer server) {
        PropertyRegistry propReg = server.getPropertyRegistry();
        Object value = valueExpr.evaluate();
        propReg.setPropertyValue(propertyName, value);
        out.printf("Set property \"%s\" to value %s%n", propertyName, value);
    }
}
