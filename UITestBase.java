package com.vmware.ui.common;

import com.beust.jcommander.ParameterException;
import com.sun.jersey.core.util.Base64;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.vmware.flexui.selenium.BrowserUtil.selenium;

import com.vmware.ui.common.TestLogger;
import com.vmware.flexui.selenium.MethodCallUtil;
import com.vmware.flexui.selenium.BrowserUtil;

import org.openqa.selenium.server.RemoteControlConfiguration;
import org.openqa.selenium.server.SeleniumServer;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;



/**
 * Created by IntelliJ IDEA. User: jshao Date: 12/17/11 Time: 7:46 PM To change
 * this template use File | Settings | File Templates.
 */
public abstract class UITestBase {

   //protected Logger log = LoggerHelper.getLogger(getClass().getName());
   protected TestLogger log = new TestLogger();

   /*for set up and launch Browser*/
   protected static String selenium_ip_address;
   protected static int selenium_port;
   protected static String browser_type;
   protected static String base_url;
   protected static String url = base_url;
   protected static String loginApp = "container_app";
   protected static String screenshotFolder = "test-output/screenShots";
   protected static String stackTraceFolder = "test-output/screenShots";
   protected static String timeout = "600000";


   // selenium server settings
   protected static SeleniumServer seleniumServer;
   protected static boolean remoteServer;
   protected String seleniumServerLog;
   protected Boolean singleWindow;
   protected String fireFoxProfileLocation;
   protected Boolean trustAllSSLCert;
   protected String userName;
   protected String password;
   protected String cellIp;
   protected Boolean ensureCleanSession;
   protected Boolean reuseBrowserSessions;
   protected Boolean debugMode;
   protected Boolean slowResources;
   protected RemoteControlConfiguration rcConfig;

   //hudson deployment properties file
   protected String adminAccount;
   protected String adminPasswd;
   protected String cmsIntFQDN;
   protected String ldapExtFQDN;
   protected String ldapIntFQDN;
   protected String rpFullName;
   protected String dataStorage;
   protected String backupStorage;
   protected String internalNetwork;
   protected String publicNetwork;
   protected String ldapNetwork;
   protected String rpWholeName;
   protected String sysrpWholeName;
   protected String rpName;
   protected String sysrpName;
   protected String oracleVmName;
   protected String postgresVmName;
   protected String sqlServerVmName;
   protected String sqlServerTemplateName;
   protected String postgresTemplateName;
   protected String oracleTemplateName;
   protected DataSetManager dsm = DataSetManager.getInstance();
   private String currentTestCaseName;
   private boolean isSetDataFile;
   protected boolean isThrowMyExcpetion = true;

   protected String oracleVersion;

   //Backend user agent

   public UITestBase() {
      initializeConfigSettings();
      getDeploymentSettings();
   }

   /**
    * open Browser
    * 
    * @throws Exception
    */
   public static void openBrowser() throws Exception {
      BrowserUtil.ip = selenium_ip_address;
      BrowserUtil.browser = browser_type;
      BrowserUtil.baseURL = base_url;
      BrowserUtil.port = selenium_port;
      BrowserUtil.loginApp = loginApp;
      BrowserUtil.URL = url;

      BrowserUtil.screenshotDir = screenshotFolder;
      BrowserUtil.isScreenshotDirFullPath = false;
      MethodCallUtil.CUSTOM_TIMEOUT = Integer.parseInt(timeout);

      BrowserUtil.openBrowserInstance();

   }


   /**
    * Starts the selenium remote control(rc) server as per the configuration
    * parameters passed.
    * 
    * @param slowResources
    *           boolean value.
    * @param enableBrowserSideLog
    *           boolean value.
    * @throws Exception
    */
   @BeforeSuite(groups = { "startup" })
   public void uiTestBaseBeforeClass() throws Exception {

      remoteServer = true;
      if (selenium_ip_address.equals("localhost")
            || selenium_ip_address.equals("127.0.0.1")) {
         rcConfig = new RemoteControlConfiguration();
         rcConfig.setPort(selenium_port);
         rcConfig.setLogOutFile(new File(seleniumServerLog));
         if (fireFoxProfileLocation != null) {
            rcConfig
                  .setFirefoxProfileTemplate(new File(fireFoxProfileLocation));
         }
         rcConfig.setTrustAllSSLCertificates(trustAllSSLCert);
         rcConfig.setSingleWindow(singleWindow);
         rcConfig.setEnsureCleanSession(ensureCleanSession);
         rcConfig.setReuseBrowserSessions(reuseBrowserSessions);
         /*rcConfig.setDebugMode(debugMode);*/

         seleniumServer = new SeleniumServer(slowResources, rcConfig);
         Thread.sleep(new Long("15000").longValue());
         seleniumServer.start();
         remoteServer = false;
      }

      openBrowser();
   }


   /**
    * Read all configuration settings from config data
    */
   private void initializeConfigSettings() {

      selenium_ip_address = "localhost";
      selenium_port = 4444;
      browser_type = "*iexplore";
      base_url = "https://10.111.96.241:9443/vsphere-client/#";
      url = base_url;
      loginApp = "container_app";
      screenshotFolder = "test-output/screenShots";
      timeout = "600000";

      remoteServer = false;
      seleniumServerLog = "selenium.log";
      singleWindow = false;
      fireFoxProfileLocation = "C:\\Selenium\\selenium-ffox-profile";
      trustAllSSLCert = true;
      slowResources = false;
      ensureCleanSession = true;
      reuseBrowserSessions = false;
      /*debugMode = true;*/
   }

   /**
    * Read the hudson deployment data from deploy.properties file
    */
   private void getDeploymentSettings() {

   }

   public void closeBrowser() {
      BrowserUtil.closeBrowserInstance();
   }

   @AfterSuite(groups = { "shutdown" })
   public void uiTestBaseAfterClass() throws Exception {
      closeBrowser();

      if (!remoteServer) {
         System.gc();
         seleniumServer.stop();
         invokeTaskKill("iexplore.exe");
      }

      Thread.sleep(5000);
   }

   public void invokeTaskKill(String processName) {
      String taskKillCommand = "taskkill /IM " + processName + " /F";
      Runtime run = Runtime.getRuntime();
      Process pr;
      try {
         pr = run.exec(taskKillCommand);
         pr.waitFor();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }


   protected void throwMyException(Throwable e) {
      if (isThrowMyExcpetion) {
         throw new UITestException(e);
      }
   }

   protected void throwMyException(String message) {
      if (isThrowMyExcpetion) {
         throw new UITestException(message);
      }
   }

   protected void throwMyException(String message, Throwable e) {
      if (isThrowMyExcpetion) {
         throw new UITestException(message, e);
      }
   }

   /**
    * self-define Exception
    * 
    * @param e
    */
   protected void throwMyUIException(UITestException e) {
      if (isThrowMyExcpetion) {
         throw e;
      }
   }


   protected void captureException(Exception e) {
      try {
         String methodName =
               Thread.currentThread().getStackTrace()[2].getMethodName();
         captureScreenShot(methodName + "Exception.png");
         captureStackTraceToErrorDir(methodName + "Exception.txt", e);
         System.out.println(methodName + "Exception.png");
      } catch (Throwable t) {
         log.error("Fail capture exception");
      }
   }

   protected void captureThrowable(Throwable e) {
      try {
         String methodName =
               Thread.currentThread().getStackTrace()[2].getMethodName();
         Date curDate = new Date(System.currentTimeMillis());
         SimpleDateFormat sDateFormat =
               new SimpleDateFormat("yyyy-MM-dd_hh_mm_ss_");
         String date = sDateFormat.format(curDate);

         captureScreenShot(date + methodName + "Error.png");
         captureStackTraceToErrorDir(date + methodName + "Error.txt", e);
      } catch (Throwable t) {
         log.error("Fail capture throwable");
      }
   }

   protected void exceptionProcess(Throwable e) {
      exceptionProcess(e, true, true);
   }

   protected void exceptionProcess(Throwable e, boolean isOutputStack,
         boolean isScreenShot) {
      exceptionProcess(e, isOutputStack, isScreenShot, null);
   }

   protected void exceptionProcess(Throwable e, boolean isOutputStack,
         boolean isScreenShot, String[] mathodNames) {
      try {
         String methodName =
               Thread.currentThread().getStackTrace()[2].getMethodName();
         Date curDate = new Date(System.currentTimeMillis());
         SimpleDateFormat sDateFormat =
               new SimpleDateFormat("yyyy-MM-dd_hh_mm_ss_");
         String date = sDateFormat.format(curDate);
         if (isOutputStack) {
            captureScreenShot(date + methodName + "_fail.png");
         }
         if (isScreenShot) {
            captureStackTraceToErrorDir(date + methodName + "_fail.txt", e);
         }
      } catch (Throwable t) {
         log.error("Fail capture exception");
      }

      try {
         if (mathodNames != null) {
            for (String methodName : mathodNames) {
               if (methodName != null && !methodName.equals("")) {
                  Method method =
                        this.getClass().getMethod(methodName, new Class[0]);
                  method.invoke(this, new Object[0]);
               }
            }
         }
      } catch (Throwable t) {
         log.error("refection failed");
      }

      throwMyException(e);
   }

   /**
    * It's used to take a screenshot with the given name
    * 
    * @param fileName
    *           ,e.g: "test-output/screenShots/screenshot.png"
    */
   public void captureScreenShot(String fileName) {
      try {
         log.info("File name and path for the screen capture :  " + fileName);
         File destinationFile = new File(screenshotFolder + "/" + fileName);
         String png = selenium.captureScreenshotToString();
         //         selenium.captureEntirePageScreenshot(screenshotFolder+"/111" + fileName, "");
         FileOutputStream fos = new FileOutputStream(destinationFile);
         fos.write(Base64.decode(png.getBytes()));
         fos.close();

      } catch (Throwable exception) {
         log.error("Failed to capture the screen shot: ");
      }
   }

   public void captureStackTraceToErrorDir(String fileName, Throwable error) {
      try {
         final Writer errorStack = new StringWriter();
         final PrintWriter printWriter = new PrintWriter(errorStack);
         error.printStackTrace(printWriter);

         BufferedWriter out =
               new BufferedWriter(new FileWriter(stackTraceFolder + "/"
                     + fileName));
         out.write(errorStack.toString());
         out.close();
      } catch (IOException e) {
         log.error("Failed to capture the stack trace to a file");
      }
   }
   
   public void setDataProvider(DataSetManager dsm) {
      this.dsm = dsm;
   }

   protected void setDataLocation(String fileName, String testCaseBlock,
         String datablock) throws ParameterException, Exception,
         FileNotFoundException {
      isSetDataFile = true;
      dsm.setDataFile(fileName);
      dsm.setTestName(testCaseBlock);
      dsm.setDataBlock(datablock);
   }


   public void StartTestCase(String testCaseName) throws Exception {
      if (testCaseName == null) {
         throw new NullPointerException("testCaseName must not be null!");
      } else if (currentTestCaseName == null) {
         System.out.println(testCaseName + "Start");
      }

      currentTestCaseName = testCaseName;

      try {
         if (!isSetDataFile) {
            setDataFile(getDefaultDataFileLocation(getClass()));
         }
         setDataTest(currentTestCaseName);
      } catch (Exception e) {


         throw e;
      }

   }

   private String getDefaultDataFileLocation(Class<?> c) {
      return "resources/" + c.getSimpleName() + ".xml";
   }

   public void setDataFile(String fileName) throws FileNotFoundException {
      isSetDataFile = true;
      dsm.setDataFile(fileName);
   }

   protected void setDataTest(String testCaseBlock) throws Exception

   {
      dsm.setTestName(testCaseBlock);
   }


   protected void setDataLocation(String testCaseBlock, String datablock)
         throws Exception {
      dsm.setTestName(testCaseBlock);
      dsm.setDataBlock(datablock);
   }


   protected void setDataBlock(String datablock) throws Exception {
      dsm.setDataBlock(datablock);
   }

   protected String getInputParameter(String parameterName) throws Exception {
      return (String) dsm.getInputParameter(parameterName);
   }

   protected String getOutputParameter(String parameterName) throws Exception {
      String param = (String) dsm.getResultParameter(parameterName);
      if (param.equals("NULL")) {
         param = null;
      }
      return param;
   }

   /**
    * @param parameterName
    *           The name of the parameter to find in the data file. This method
    *           will only return parameters found in the environment (Env)
    *           section of your data file.
    * @return
    * @throws ParameterMissingException
    */
   protected String getEnvParameter(String parameterName)
         throws ParameterException {
      return (String) dsm.getEnvParameter(parameterName);
   }


   protected void logout() throws InterruptedException {
     
   }
   
}
