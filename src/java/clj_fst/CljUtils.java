package org.apache.lucene.util.clj;

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.UnicodeUtil;

/** Helper class to test FSTs. */
public class CljUtils {

  public static String inputToString(IntsRef term, boolean isValidUnicode) {
    if (!isValidUnicode) {
      return term.toString();
    } else {
      // utf8
      //return toBytesRef(term).utf8ToString() + " " + term;
      return UnicodeUtil.newString(term.ints, term.offset, term.length);
    }
  }

  private static BytesRef toBytesRef(IntsRef ir) {
    BytesRef br = new BytesRef(ir.length);
    for(int i=0;i<ir.length;i++) {
      int x = ir.ints[ir.offset+i];
      assert x >= 0 && x <= 255;
      br.bytes[i] = (byte) x;
    }
    br.length = ir.length;
    return br;
  }
}