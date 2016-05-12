/**
 * PointeurType -- class representing the pointeur type
 * 
 * @author J. Guilbon
 * @version 0.1
 */
package mcs.symtab;

public class PointerType implements Type {

	// Attributes
	private Type type;

	/**
	 * Constructor
	 */
	public PointeurType(Type t) {
            type = t;
	}

	/**
	 * toString();
	 */
	public String toString() {
		return "PTR";
	}

	/**
	 * isCompatible()
	 */
	public boolean isCompatible(Type other) {
		return (other instanceof PointerType) && other.getType.isCompatible(this.type);
	}

	public Type getType() {
		return this.type;
	}
}