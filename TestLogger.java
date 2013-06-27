package com.vmware.ui.common;

import org.apache.log4j.Logger;

public class TestLogger {
   private static Logger logger = Logger.getLogger("FVT");

   //Logs repro step here
   public static void repro(String message) {
      logger.info("thread id :" + Thread.currentThread().getId() + " "
            + "REPRO STEP: " + message);
   }

   //Logs General Information here
   public static void info(String message) {
      logger.info("thread id :" + Thread.currentThread().getId() + " "
            + message);
   }

   //Logs Warning message
   public static void warning(String message) {
      logger.warn("thread id :" + Thread.currentThread().getId() + " "
            + message);
   }

   //Logs Error message
   public static void error(String message) {
      logger.error("thread id :" + Thread.currentThread().getId() + " "
            + message);
   }

   //Logs Error message
   public static void debug(String message) {
      logger.debug("thread id :" + Thread.currentThread().getId() + " "
            + message);
   }

   public static void main(String[] args) {
      TestLogger.info("infor");
      TestLogger.debug("debug");
      TestLogger.warning("warning");
      TestLogger.error("error");

   }
}
