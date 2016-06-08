/**
 * Namespace -- class for representing a namespace
 *
 * @author J. Guilbon
 * @version 0.1
 */

package mcs.symtab;

public class NamespaceInfo {
    // Attributes
    public String name; // Name of the namespace
    public NamespaceInfo parent; // The parent of the namespace
    
    /**
     * @param n name of the namespace
     */
    public NamespaceInfo(String n) {
        this.name = n;
        this.parent = null;
    }

    /**
     * @param n name of the namespace
     * @param ns Parent of the namespace
     */
    public NamespaceInfo(String n, NamespaceInfo ns) {
        this.name = n;
        this.parent = ns;
    }
    
    /**
     * Get the name of the namespace
     * @return the name
     */
    public String name() {
        return this.name;
    }

    /**
     * Get the parent of the namespace
     * @return the parent
     */
    public NamespaceInfo parent() {
        return this.parent;
    }

    /**
     * Convert the information into string
     */
    public String toString() {
        return "Namespace : " + this.name;
    }

		public boolean equals(NamespaceInfo ni) {
			return ni.name.equals(this.name);
		}
}
