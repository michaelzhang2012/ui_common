package com.vmware.ui.common;

import java.util.List;
import java.util.Map;

public class CommonHelper {
   /**
    * This method is the moc for java.util.Arrays.toString() in java 5. We need
    * this function work in java 1.4.
    * 
    * @param array
    *           an object array, which contents will be returned as a String.
    * @return a string
    */
   public static String arrayToString(Object[] array) {
      if (array == null) {
         return null;
      }

      String aS = new String("[");
      for (int i = 0; i < array.length; i++) {
         aS = aS + array[i];
         if (i != (array.length - 1)) {
            aS = aS + ",";
         }
      }

      return aS + "]";
   }

   public static String listOfMapToString(List<?> l) {
      if (l == null) {
         return null;
      }

      if (l.isEmpty()) {
         return "{\n}";
      }

      String s = "{\n";
      for (int i = 0; i < l.size(); i++) {
         s += DataSetManager.mapToString((Map<?, ?>) l.get(i));
      }

      s += "\n}";

      return s;
   }
}
