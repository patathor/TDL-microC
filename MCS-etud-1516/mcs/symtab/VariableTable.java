/**
 * VariableTable -- class representing a symbol table used for the variable
 *
 * @author G. Dupont
 * @version 0.1
 */
package mcs.symtab;

import java.utils.Map;
import java.utils.HashMap;

class VariableTable implements SymbolTable {
  // Attributes
	private Map<String, SymbolInfo> content;
  private SymbolTable parent; // Parent of this table

  /**
   * Constructor
   * Create a table from a parent table that could be null
   */
  public VariableTable(SymbolTable p = null) {
    this.parent = p;
		this.content = new HashMap<String, SymbolInfo>();
  }

  /**
   * Look up into the table
   */
  public SymbolInfo lookup(String name, boolean local = false) {
    for (String key : this.content.keySet()) {
      if (key.equals(name))
        return this.content.get(name);
    }

    if (!local)
      return parent().lookup(name, true);

    return new SymbolInfoNotFound();
  }

  public boolean insert(String name, SymbolInfo info) {
    if (this.content.containsKey(name))
      return false;
    this.content.put(name, info);
    return true;
  }

  public SymbolTable parent() {
    return this.parent;
  }
}

