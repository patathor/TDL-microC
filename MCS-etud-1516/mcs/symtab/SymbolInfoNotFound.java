/**
 * SymbolInfoNotFound -- specialisation of SymbolInfo indicating the symbol is not found.
 *
 * This is mainly for error treatment.
 *
 * @author G. Dupont
 * @version 0.1
 */
package mcs.symtab;

public class SymbolInfoNotFound extends SymbolInfo {
  public SymbolInfoNotFound() {
    super(null);
  }

  public String toString() {
    return "NOT FOUND";
  }
}


