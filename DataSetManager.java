package com.vmware.ui.common;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Node;
import com.beust.jcommander.ParameterException;

public class DataSetManager {
   private static Pattern IDAPattern = Pattern.compile("ValueOf_([[^;].]+);");


   // name of the test data file
   private String m_filename;

   // DOM element to store the datablock
   private Element m_method = null;

   // DOM element to store the <Scenario/> element.
   private Element m_scenario = null;

   // DOM elememt to store the <Test/> element
   private Element m_test = null;

   // test scenario name
   private String m_scenarioName;

   // flag to indicate if it is a pure unit test, no test element, only method
   // element
   private boolean m_isUT = false;

   // End to End registry for FVT
   private Map m_registry = null;

   // generated xpath map
   private Map m_generatedXPath = new HashMap();


   /**
    * Init method. Read the test data file and then setup every thing needed.
    * 
    * @throws FileNotFoundException
    * 
    */
   private void setupFromXML() throws FileNotFoundException {
      File dataFile = new File(m_filename);
      InputStream xmlFile;
      Document doc = null;

      try {
         // Enable to search data file under local folder or classpath
         if (dataFile.exists()) {
            xmlFile = new BufferedInputStream(new FileInputStream(dataFile));
         } else {
            xmlFile =
                  DataSetManager.class.getClassLoader().getResourceAsStream(
                        m_filename);
            if (xmlFile == null) {
               throw new FileNotFoundException(m_filename
                     + " not found under classpath!");
            }
         }

         DocumentBuilderFactory bldrFactory =
               DocumentBuilderFactory.newInstance();
         DocumentBuilder docBldr = bldrFactory.newDocumentBuilder();
         doc = docBldr.parse(xmlFile);

         Element root = doc.getDocumentElement();

         NodeList testNL = root.getElementsByTagName(DSMConstants.TEST);

         // added the flowing block to process UT specially
         if (testNL.getLength() == 0) {
            m_isUT = true;

            Element testNode =
                  (Element) root.getOwnerDocument().createElement(
                        DSMConstants.TEST);
            testNode.setAttribute(DSMConstants.NAME, DSMConstants.UT);

            NodeList childNL = root.getChildNodes();
            for (int i = 0; i < childNL.getLength(); i++) {
               Node n = childNL.item(i);

               if (!n.getNodeName().equals(DSMConstants.ENV)
                     && n.getNodeType() != Node.TEXT_NODE) {
                  testNode.appendChild(root.removeChild(n));

               }
            }


            root.appendChild(testNode);

         }

         m_scenario = root;
         m_scenarioName = root.getAttribute(DSMConstants.NAME);
      } catch (FileNotFoundException fnfe) {
         fnfe.printStackTrace();
         throw fnfe;
      } catch (Exception e) {
         e.printStackTrace();
         throw new RuntimeException(e);
         //	System.exit(0);
      }
   }

   /**
    * Constructor for the class.
    * 
    * @param filename
    *           the test data file name.
    * @param testName
    *           the test name.
    * @param datablock
    *           the datablock name
    * @throws FileNotFoundException
    */
   public DataSetManager(String filename, String testName, String datablock)
         throws FileNotFoundException {
      this(filename);
      try {
         setTestName(testName);
         setDataBlock(datablock);
      } catch (Exception e) {
         System.out.println(e.getMessage());
         e.printStackTrace();
      }
   }

   public DataSetManager() {


   }

   /**
    * Get all the scenario level env. parameters which not belong to a group.
    * 
    * @return map.
    */
   public Map getAllScenarioLevelEnvParameters() {
      return getAllParameters(m_scenario, DSMConstants.ENV);
   }

   /**
    * Get all the test level env. parameters which not belong to a group.
    * 
    * @return map.
    */
   public Map getAllTestLevelEnvParameters() {
      return getAllParameters(m_test, DSMConstants.ENV);
   }


   /**
    * Get all the datablock level env. parameters which not belong to a group.
    * 
    * @return map.
    */
   public Map getAllDatablockLevelEnvParameters() {
      return getAllParameters(m_method, DSMConstants.ENV);
   }

   /**
    * Get all datablock level env parameters in a group. &lt;groupTag/&gt;
    * 
    * @param groupTag
    *           group tag name.
    * @return parameters in the group as a map.
    */
   private Map getAllDatablockLevelEnvParameters(String groupTag) {
      return getAllDatablockLevelEnvParameters(groupTag, null);
   }


   /**
    * Get all datablock level env parameters in a group. &lt;groupTag
    * name='groupName'/&gt;
    * 
    * @param groupTag
    *           group tag name.
    * @return parameters in the group as a map.
    */

   private Map getAllDatablockLevelEnvParameters(String groupTag,
         String groupName) {
      return getAllGroupParametersByXPath(m_method, DSMConstants.ENV, groupTag,
            groupName);
   }

   private String buildXPathForGroup(String type, String groupTag,
         String groupName) {
      if (type == null || groupTag == null) {
         return null;
      }

      String xpath = type + "/" + groupTag;

      if (groupName != null) {
         xpath = xpath + "[@" + DSMConstants.NAME + "='" + groupName + "']";
      }

      return xpath;
   }

   private Map getGroupParametersByXPath(Node root, String xpath) {

      if (root == null || xpath == null)
         return null;

      Element groupE = null;
      try {
         groupE = (Element) XPathAPI.selectSingleNode(root, xpath);
      } catch (Exception e) {
         //m_logger.info(e.getMessage());
      }

      if (groupE == null) {
         return null;
      }

      // Set keySet = new HashSet();

      NodeList parameterNL =
            groupE.getElementsByTagName(DSMConstants.PARAMETER);
      if (parameterNL == null) {
         return null;
      }

      Map tmpMap = new HashMap();

      for (int i = 0; i < parameterNL.getLength(); i++) {
         // System.out.println(parameterNL.item(i).getNodeName());
         Element tmpE = (Element) parameterNL.item(i);
         if (tmpE.hasAttribute(DSMConstants.VALUE)
               && tmpE.hasAttribute(DSMConstants.NAME)) {
            String tmpKey = tmpE.getAttribute(DSMConstants.NAME).trim();
            String tmpValue = tmpE.getAttribute(DSMConstants.VALUE).trim();

            if (tmpMap.containsKey(tmpKey)) {
               String[] valueArray = null;

               Object o = tmpMap.get(tmpKey);
               if (o instanceof String[]) {
                  int length = ((String[]) o).length;
                  valueArray = new String[length + 1];
                  for (int j = 0; j < length; j++) {
                     valueArray[j] = ((String[]) o)[j];
                  }

                  valueArray[length] = tmpValue;
               } else {
                  valueArray = new String[2];
                  valueArray[0] = (String) o;
                  valueArray[1] = tmpValue;
               }

               tmpMap.put(tmpKey, valueArray);
            } else {
               tmpMap.put(tmpKey, tmpValue);
            }
         }
      }

      return tmpMap;

   }

   /**
    * Get the result/output parameter.
    * 
    * @param name
    *           parameter name.
    * @return A single string value when only unique value for the name; a
    *         string array when multiple values for the name.
    * @throws ParameterException
    *            if in the test data file, there is no parameter with the name,
    *            this exception will be thrown. <br>
    * 
    *            Below is an example: <br>
    *            &lt;output&gt; <br>
    *            &lt;parameter name="id" value="100001"/&gt;<br>
    *            &lt;parameter name="names" value="Frank Fu"/&gt;<br>
    *            &lt;parameter name="names" value="Frank Wang"/&gt; <br>
    *            &lt;/output&gt;<br>
    * 
    *            getResultParameter("id") will reutrn a String of "100001".<br>
    *            getResultParameter("names" will reutrn a String array
    *            {"Frank Fu", "Frank Wang"}<br>
    * 
    */
   public Object getResultParameter(String name) throws ParameterException {
      return getParameter(m_method, DSMConstants.OUTPUT, name);

   }

   private Map getAllGroupParametersByXPath(Node root, String type,
         String groupTag, String groupName) {
      if (root == null || groupTag == null) {
         return null;
      }

      String xpath = buildXPathForGroup(type, groupTag, groupName);

      if (xpath == null) {
         return null;
      }

      return getGroupParametersByXPath(root, xpath);

   }


   /**
    * Get all environment parameters (all level) which are not belog to groups
    * 
    * @return a map to contain all env. level parameters
    * 
    */
   public Map<String, Object> getAllEnvParameters() {
      Map newMap = new HashMap();
      Map tmpMap = null;

      tmpMap = getAllScenarioLevelEnvParameters();
      if (tmpMap != null) {
         newMap.putAll(tmpMap);
      }

      tmpMap = getAllTestLevelEnvParameters();
      if (tmpMap != null) {
         newMap.putAll(tmpMap);
      }

      tmpMap = getAllDatablockLevelEnvParameters();
      if (tmpMap != null) {
         newMap.putAll(tmpMap);
      }
      return newMap;
   }

   /**
    * Get all test level env parameters in a group. &lt;groupTag
    * name='groupName'/&gt;
    * 
    * @param groupTag
    *           group tag name.
    * @return parameters in the group as a map.
    */
   private Map getAllTestLevelEnvParameters(String groupTag, String groupName) {
      return getAllGroupParametersByXPath(m_test, DSMConstants.ENV, groupTag,
            groupName);
   }


   /**
    * Get all test level env parameters in a group. &lt;groupTag/&gt;
    * 
    * @param groupTag
    *           group tag name.
    * @return parameters in the group as a map.
    */

   private Map getAllTestLevelEnvParameters(String groupTag) {
      return getAllTestLevelEnvParameters(groupTag, null);
   }


   /**
    * Get all scenario level env parameters in a group. &lt;groupTag
    * name='groupName'/&gt;
    * 
    * @param groupTag
    *           group tag name.
    * @return parameters in the group as a map.
    */
   private Map getAllScenarioLevelEnvParameters(String groupTag,
         String groupName) {
      return getAllGroupParametersByXPath(m_scenario, DSMConstants.ENV,
            groupTag, groupName);
   }

   /**
    * Get all scenario level env parameters in a group. &lt;groupTag/&gt;
    * 
    * @param groupTag
    *           group tag name.
    * @return parameters in the group as a map.
    */

   private Map getAllScenarioLevelEnvParameters(String groupTag) {
      return getAllScenarioLevelEnvParameters(groupTag, null);
   }

   /**
    * Get all env parameters as a map which belog to a group: &lt;groupTag/&gt;
    * 
    * @param groupTag
    *           group tag name.
    * @return all parameters as a map.
    */
   private Map getAllEnvParameters(String groupTag) {
      Map newMap = new HashMap();
      Map tmpMap = null;

      tmpMap = getAllScenarioLevelEnvParameters(groupTag);
      if (tmpMap != null) {
         newMap.putAll(tmpMap);
      }

      tmpMap = getAllTestLevelEnvParameters(groupTag);
      if (tmpMap != null) {
         newMap.putAll(tmpMap);
      }

      tmpMap = getAllDatablockLevelEnvParameters(groupTag);
      if (tmpMap != null) {
         newMap.putAll(tmpMap);
      }
      return newMap;

   }

   /**
    * This method is to load all the java classes when needed for BOD test. <br>
    * The package name should be supplied as the env parameter. <br>
    * Below is an example: <br>
    * &lt;Scenario&gt; <br>
    * &lt;env&gt; <br>
    * &lt;parameter name="package" value=
    * "com.vmware.aurora.testsuite.facade.datatypes.impl.MemberPackageImpl"
    * /&gt;<br>
    * &lt;parameter name="package" value=
    * "com.vmware.aurora.testsuite.facade.datatypes.impl.OrderPackageImpl" /&gt; <br>
    * &lt;/env&gt; <br>
    * 
    */
   private void loadPackages() {

      try {

         Map sdoM = getAllEnvParameters(DSMConstants.SDOPACKAGE);
         if (sdoM == null) {
            return;
         }

         Object packages = sdoM.get(DSMConstants.PACKAGE);
         if (packages == null) {
            return;
         }

         String[] sdoPackage = null;
         if (packages instanceof String) {
            sdoPackage = new String[1];
            sdoPackage[0] = (String) packages;
         } else if (packages instanceof String[]) {
            sdoPackage = (String[]) packages;
         }

         for (int i = 0; i < sdoPackage.length; i++) {

            Class sdopackage_class = Class.forName(sdoPackage[i]);
            java.lang.reflect.Method init =
                  sdopackage_class.getMethod("init", (Class[]) null);
            init.invoke((Object[]) null, (Object[]) null);
         }
      } catch (Exception e) {

      }
   }

   public void setDataFile(String filename) throws FileNotFoundException {
      m_scenario = null;
      m_method = null;
      m_test = null;
      m_scenarioName = null;
      m_isUT = false;
      m_registry = null;
      m_generatedXPath = new HashMap();
      m_filename = filename;

      setupFromXML();
      loadPackages();
   }

   /**
    * Constructor for the class.
    * 
    * @param filename
    *           the test data file name.
    * @throws FileNotFoundException
    */
   public DataSetManager(String filename) throws FileNotFoundException {
      m_filename = filename;


      setupFromXML();
      loadPackages();

   }

   /**
    * Constructor for the class.
    * 
    * @param filename
    *           test data file name.
    * @param testName
    *           test name.
    * @throws FileNotFoundException
    */
   public DataSetManager(String filename, String testName)
         throws FileNotFoundException {
      this(filename);
      final String methodName = "DataSetManager";
      try {
         setTestName(testName);
      } catch (Exception e) {

      }
   }


   /**
    * Get the input parameter.
    * 
    * @param name
    *           parameter name.
    * @return Single string value, when unique value for the name; string array
    *         when multiple values for the name.
    * @throws ParameterException
    *            if in the test data file, there is no parameter with the name,
    *            this exception will be thrown.
    * 
    *            Below is an example: &lt;input&gt;<br>
    *            &lt;parameter name="id" value="100001"/&gt;<br>
    *            &lt;parameter name="names" value="Frank Fu"/&gt;<br>
    *            &lt;parameter name="names" value="Frank Wang"/&gt; <br>
    *            &lt;/input&gt;<br>
    * 
    *            getInputParameter("id") will reutrn a String of "100001". <br>
    *            getInputParameter("names") will reutrn a String array
    *            {"Frank Fu", "Frank Wang"} <br>
    * 
    */
   public Object getInputParameter(String name) throws ParameterException {
      //m_logger.fine(name);
      return getParameter(m_method, DSMConstants.INPUT, name);
   }

   /**
    * Get the input parameter as an String array.
    * 
    * @param name
    *           parameter name.
    * @return Single string value, when unique value for the name; string array
    *         when multiple values for the name.
    * @throws ParameterException
    *            if in the test data file, there is no parameter with the name,
    *            this exception will be thrown.
    * 
    *            Below is an example: &lt;input&gt;<br>
    *            &lt;parameter name="id" value="100001"/&gt;<br>
    *            &lt;parameter name="names" value="Frank Fu"/&gt;<br>
    *            &lt;parameter name="names" value="Frank Wang"/&gt; <br>
    *            &lt;/input&gt;<br>
    * 
    *            getInputParameter("id") will reutrn a String array of
    *            {"100001"}. <br>
    *            getInputParameter("names") will reutrn a String array
    *            {"Frank Fu", "Frank Wang"} <br>
    * 
    */
   public String[] getInputParameterAsArray(String name)
         throws ParameterException {
      return getParameterAsArray(m_method, DSMConstants.INPUT, name);
   }


   private String buildXPathOfParameter(String type, String name) {
      if (type.equals(DSMConstants.ENV))
         return buildXPathOfEnvParameter(name);
      if (type.equals(DSMConstants.INPUT))
         return buildXPathOfInputParameter(name);
      if (type.equals(DSMConstants.OUTPUT))
         return buildXPathOfResultParameter(name);

      return null;
   }


   private Object getParameter(Element root, String msgFlag, String name)
         throws ParameterException {
      if (root == null) {
         throw new ParameterException(
               "Current datablock or test block is null. Make sure you set the data location.");
      }

      try {

         String tmpXPath = buildXPathOfParameter(msgFlag, name);

         NodeList valueNL = XPathAPI.selectNodeList(root, tmpXPath);
         int len = valueNL.getLength();
         if (valueNL == null || len <= 0) {
            throw new ParameterException("Parameter: " + name
                  + " cannot be found in the current data block");
         }

         if (len == 1) {
            return IDAReplace(valueNL.item(0).getNodeValue().trim());
         }

         String valueArray[] = new String[len];

         for (int i = 0; i < len; i++) {
            valueArray[i] = IDAReplace(valueNL.item(i).getNodeValue().trim());
         }
         return valueArray;
      } catch (TransformerException e) {

         throw new ParameterException(e.getMessage());
      }
   }


   private String[] getParameterAsArray(Element root, String msgFlag,
         String name) throws ParameterException {
      if (root == null) {
         throw new ParameterException(
               "Current datablock or test block is null. Make sure you set the data location.");
      }

      try {

         NodeList valueNL =
               XPathAPI.selectNodeList(root,
                     buildXPathOfParameter(msgFlag, name));
         int len = valueNL.getLength();
         if (valueNL == null || len <= 0) {
            throw new ParameterException("Parameter: " + name
                  + " cannot be found in the current data block.");
         }

         String valueArray[] = new String[len];

         for (int i = 0; i < len; i++) {
            valueArray[i] = IDAReplace(valueNL.item(i).getNodeValue().trim());
         }
         return valueArray;
      } catch (TransformerException e) {

         throw new ParameterException(e.getMessage());
      }
   }

   /**
    * Get all the input parameters in a map.
    * 
    * @return A map which contained all the name/value. Single value will be
    *         returned as a string in the map, multiple values will be returen
    *         as an array in the map.
    * 
    *         Below is an example.<br>
    * 
    *         &lt;input&gt; <br>
    *         &lt;parameter name="id" value="10001"/&gt; <br>
    *         &lt;parameter name="name" value="Terry"/&gt; <br>
    *         &lt;parameter name="name" value="Shawn"/&gt; <br>
    *         &lt;parameter name="name" value="Frank"/&gt; <br>
    *         &lt;/input&gt; <br>
    * 
    *         will return the following map: <br>
    *         {<br>
    *         "id" =====> "10001" <br>
    *         "names" ====> {"Terry", "Shawn", "Frank"} <br>
    * <br>
    */
   public Map getAllInputParameters() {
      return getAllParameters(m_method, DSMConstants.INPUT);

   }

   private Map getAllParameters(Element root, String type) {
      if (root == null)
         return null;

      NodeList childNL = root.getChildNodes();
      if (childNL == null) {
         return null;
      }

      Element ioeE = null;
      for (int i = 0; i < childNL.getLength(); i++) {
         if (childNL.item(i).getNodeName().trim().equals(type.trim())) {
            ioeE = (Element) childNL.item(i);

            break;
         }
      }

      // Element inputE = (Element) root.getElementsByTagName(type).item(0);
      if (ioeE == null) {
         return null;
      }

      // Set keySet = new HashSet();
      Map tmpMap = new HashMap();
      NodeList parameterNL = ioeE.getChildNodes();

      for (int i = 0; i < parameterNL.getLength(); i++) {
         // System.out.println(parameterNL.item(i).getNodeName());

         // kick off the child note type such as text
         if (parameterNL.item(i).getNodeType() != Node.ELEMENT_NODE)
            continue;

         Element tmpE = (Element) parameterNL.item(i);
         if (tmpE.getNodeName().equals(DSMConstants.PARAMETER)
               && tmpE.hasAttribute(DSMConstants.VALUE)
               && tmpE.hasAttribute(DSMConstants.NAME)) {
            String tmpKey = tmpE.getAttribute(DSMConstants.NAME).trim();
            String tmpValue =
                  IDAReplace(tmpE.getAttribute(DSMConstants.VALUE).trim());

            if (tmpMap.containsKey(tmpKey)) {
               String[] valueArray = null;

               Object o = tmpMap.get(tmpKey);
               if (o instanceof String[]) {
                  int length = ((String[]) o).length;
                  valueArray = new String[length + 1];
                  for (int j = 0; j < length; j++) {
                     valueArray[j] = ((String[]) o)[j];
                  }

                  valueArray[length] = tmpValue;
               } else {
                  valueArray = new String[2];
                  valueArray[0] = (String) o;
                  valueArray[1] = tmpValue;
               }

               tmpMap.put(tmpKey, valueArray);
            } else {
               tmpMap.put(tmpKey, tmpValue);
            }
         }
      }
      return tmpMap;
   }


   private String buildXPathOfDBNode(String name) {
      return DSMConstants.DATABLOCK + "[@" + DSMConstants.NAME + "='" + name
            + "']";
   }

   private String buildXPathOfEnvParameter(String name) {
      return DSMConstants.ENV + "/" + DSMConstants.PARAMETER + "[@"
            + DSMConstants.NAME + "='" + name + "']/@" + DSMConstants.VALUE;
   }

   private String buildXPathOfInputParameter(String name) {
      return DSMConstants.INPUT + "/" + DSMConstants.PARAMETER + "[@"
            + DSMConstants.NAME + "='" + name + "']/@" + DSMConstants.VALUE;
   }

   private String buildXPathOfResultParameter(String name) {
      String comma = "'";

      if (name.indexOf("'") != -1) {
         comma = "\"";
      }

      return DSMConstants.OUTPUT + "/" + DSMConstants.PARAMETER + "[@"
            + DSMConstants.NAME + "=" + comma + name + comma + "]/@"
            + DSMConstants.VALUE;
   }


   /**
    * Set the datablock. Hereafter you can get parameters of this datablock and
    * you are at the datablock level.
    * 
    * @param blockName
    *           the datablock name.
    * @return true if successful.
    * @throws DatablockMissingException
    *            if the datablock name does not exist in the test data file,
    *            this exception will be thrown.
    */
   public boolean setDataBlock(String blockName) throws Exception {

      if (m_test == null) {
         if (m_isUT) {
            try {
               setTestName(DSMConstants.UT);
            } catch (Exception e) {

            }
         } else {
            throw new RuntimeException(
                  "Before seting the data block, please first setTestName.");
         }
      }
      try {
         m_method =
               (Element) XPathAPI.selectSingleNode(m_test,
                     buildXPathOfDBNode(blockName));
         if (m_method == null) {
            throw new Exception(DSMConstants.DATABLOCK + blockName
                  + " not existed");
         }
      } catch (TransformerException e) {

      }

      return true;
   }

   private String buildXPathOfTestNode(String name) {
      return "//" + DSMConstants.TEST + "[@" + DSMConstants.NAME + "='" + name
            + "']";
   }

   /**
    * Set the test name. Hereafter you are at the test level.
    * 
    * @param name
    *           the test name.
    * @return true if successful.
    * @throws Exception
    *            if the testname is not existed in the test data file, this
    *            exception will be thrown.
    */
   public boolean setTestName(String name) throws Exception {

      if (m_scenario == null) {
         throw new Exception("There is nothing in the data file");
      }
      try {
         m_test =
               (Element) XPathAPI.selectSingleNode(m_scenario,
                     buildXPathOfTestNode(name));
         if (m_test == null) {
            throw new Exception("Test: " + name
                  + " does not exist in the data file!");
         }

         m_method = null;

      } catch (TransformerException e) {

      }

      return true;
   }


   private static class DataSetManagerHolder {
      public static DataSetManager INSTANCE = null;
   }

   public static DataSetManager getInstance() {
      if (DataSetManagerHolder.INSTANCE == null) {
         DataSetManagerHolder.INSTANCE = new DataSetManager();
      }
      return DataSetManagerHolder.INSTANCE;
   }

   public static String IDAReplace(String str) {
      Matcher m = IDAPattern.matcher(str);
      StringBuffer sb = new StringBuffer();
      while (m.find()) {
         System.out.println(m.group(1));

      }
      m.appendTail(sb);
      return sb.toString();
   }

   /**
    * Transfer the map into a String.
    * 
    * @param m
    *           input map.
    * @return a string.
    */
   public static String mapToString(Map m) {
      String aS = "{";
      Set keys = m.keySet();
      Iterator itor = keys.iterator();
      while (itor.hasNext()) {
         String key = (String) itor.next();
         Object value = m.get(key);
         if (value instanceof String[]) {
            aS = aS + key + "==>" + arrayToString((String[]) value) + " ";
         } else {
            aS = aS + key + "==>" + value + " ";
         }

      }

      aS = aS + "}";

      return aS;
   }

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


   public Object getEnvParameter(String name) throws ParameterException {

      try {
         return getParameter(m_method, DSMConstants.ENV, name);
      } catch (ParameterException e) {
         try {
            return getParameter(m_test, DSMConstants.ENV, name);
         } catch (ParameterException ee) {
            try {
               return getParameter(m_scenario, DSMConstants.ENV, name);
            } catch (ParameterException eee) {
               throw eee;
            }
         }
      }
   }

   String[] getAllblockName() {
      assert m_scenario != null;
      assert m_test != null;

      NodeList blocks = m_test.getElementsByTagName(DSMConstants.DATABLOCK);
      String[] block_names = new String[blocks.getLength()];
      for (int i = 0; i < blocks.getLength(); i++) {
         block_names[i] =
               blocks.item(i).getAttributes().getNamedItem("name")
                     .getNodeValue();
      }
      return block_names;
   }
}
