package org.neo4j.rdf.model;

/**
 * A literal value, i.e. String, int, byte, etc.
 */
public class Literal implements Value
{    
    private final Object value;
    private final Uri datatype;
    private final String language;
    
    // TODO: We need to detect what data type people throw in here and if
    // it's something we support natively (i.e. Java's primitive types) then
    // we make sure we convert it properly... or else people will just embed
    // their longs (for example) as strings, with datatype xsd:long. Ugly,
    // but that's how for example SAIL works. :(
    
    public Literal( Object value )
    {
        this( value, null, null );
    }
    
    public Literal( Object value, Uri datatype )
    {
        this( value, datatype, null );
    }
    
    public Literal( Object value, Uri datatype, String language )
    {
        this.value = value;
        this.datatype = datatype;
        this.language = language;
    }
    
    /**
     * The literal value of this literal 
     * @return the value
     */
    public Object getValue()
    {
        return this.value;
    }
    
    /**
     * The optional data type of this literal
     * @return the optional data type of this literal
     */
    public Uri getDatatype()
    {
        return this.datatype;
    }
    
    /**
     * The optional language tag for this literal
     * @return the optional language tag for this literal
     */
    public String getLanguage()
    {
        return this.language;
    }
    
    @Override
    public String toString()
    {
        return "Literal[" + this.value + "]";
    }
}