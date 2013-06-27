package com.vmware.ui.common;

public class UITestException extends RuntimeException {
   private Throwable previous = null;

   public UITestException(Throwable e) {
      super(e);
      previous = e;
   }

   public UITestException(String message) {
      super(message);
   }


   public UITestException(String message, Throwable e) {
      super(message, e);
      previous = e;
   }


   public Throwable previous() {
      return previous;
   }

   public String toString() {
      if (previous != null) {
         return super.toString() + "; " + previous.toString();
      } else {
         return super.toString();
      }
   }

   private static final long serialVersionUID = -1280508026861128493L;
}
