package com.mcd.gdw.daas.mapreduce;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.mcd.gdw.daas.DaaSConstants;
import com.mcd.gdw.daas.driver.APTDriver;
import com.mcd.gdw.daas.util.APTUtil;
import com.mcd.gdw.daas.util.PmixData;

/**
 * 
 * Mapper class to parse STLD XML files.
 *
 */
public class APTStldMapper extends Mapper<LongWritable, Text, Text, Text>{
	
	private BigDecimal totalAmount;
	
	private MultipleOutputs<Text, Text> multipleOutputs;
	
	private DocumentBuilderFactory docFactory = null;
	private DocumentBuilder docBuilder = null;
	private Document document = null;
	private Element rootElement;
	private NodeList nlEvent;
	private Element eleEvent;
	private String eventType;
	private Element eleTrx;
	private NodeList nlTRX;
	private Element eleOrder;
	private NodeList nlOrder;
	private NodeList nlNode;
	private Element eleNode;
	
	private String[] inputParts = null;
	private String xmlText = "";
	private String ownerShipType = "";
	
	private String terrCd = "";
	private String lgcyLclRfrDefCd = "";	
	private String businessDate = "";
	private String gdwBusinessDate = "";
	private String haviBusinessDate = "";
	private String status = "";	
	private String trxPOD = "";	
	private String orderKind = "";
	private String orderKey = "";
	private String gdwMcdGbalLcatIdNu = "";
	private String saleType = "";
	private String eventTimeStamp = "";
	private String modifiedEventTimeStamp = "";
	private long haviTimeKey = 0;
	
	private Map<String, PmixData> pmixDataMap = null;
		
	private Map<String, List<String>> comboItemLookup =  null;
	private Map<String, String> primaryItemLookup = null;
	
	private Text aptHdrTxt = null;
	private Text aptDtlKeyTxt = null;
	private Text aptDtlValueTxt = null;
	
	private StringBuffer aptHdrStrBuf = null;
	private StringBuffer aptDtlKeyStrBuf = null;
	private StringBuffer aptDtlValueStrBuf = null;
	
	/**
	 * Setup method to create multipleOutputs
	 */
	@Override
	public void setup(Context context) throws IOException, InterruptedException {
		
		multipleOutputs = new MultipleOutputs<Text, Text>(context);	
        docFactory = DocumentBuilderFactory.newInstance();
        aptHdrTxt = new Text();
    	aptDtlKeyTxt = new Text();
    	aptDtlValueTxt = new Text();
    	
    	aptHdrStrBuf = new StringBuffer();
        aptDtlKeyStrBuf = new StringBuffer();
        aptDtlValueStrBuf = new StringBuffer();
    	
        URI[] cacheFiles = context.getCacheFiles();
        
        comboItemLookup =  new HashMap<String, List<String>>();
        primaryItemLookup = new HashMap<String, String>();
        
        BufferedReader br = null;
        String inputLookUpData = "";
        try {
        	docBuilder = docFactory.newDocumentBuilder();
            for (int fileCounter = 0; fileCounter < cacheFiles.length; fileCounter++) {
            	//AWS START
            	System.out.println("Cache File = " + cacheFiles[fileCounter].toString());
            	//br = new BufferedReader(new FileReader(cacheFiles[fileCounter].toString()));
            	String[] cacheFileParts = cacheFiles[fileCounter].toString().split("#");
            	br = new BufferedReader(new FileReader("./" + cacheFileParts[1]));
            	//AWS END 
            	List<String> comboItems = null;
            	String[] inputDataArray = null;
            	while ((inputLookUpData = br.readLine()) != null) {
                	inputDataArray = inputLookUpData.split("\\|");
                	
                	//AWS START
                	//if (fileCounter == 0) {
                    if (cacheFileParts[fileCounter].equals(APTDriver.CACHE_FILE_EMIX_CMBCMP)) {
                	//AWS END
                		comboItems = comboItemLookup.containsKey(inputDataArray[0]) ? comboItemLookup.get(inputDataArray[0]) : new ArrayList<String>();
                    	comboItems.add(String.format("%04d", Integer.parseInt(inputDataArray[1])) + DaaSConstants.PIPE_DELIMITER + inputDataArray[2] + 
                    										 DaaSConstants.PIPE_DELIMITER + inputDataArray[4] + DaaSConstants.PIPE_DELIMITER + inputDataArray[6]);
                		comboItemLookup.put(inputDataArray[0], comboItems);
                   	//AWS START
                    //} else if (fileCounter == 1) {
                    } else if (cacheFileParts[fileCounter].equals(APTDriver.CACHE_FILE_EMIX_CMIM)) {
                    //AWS END
                		primaryItemLookup.put(inputDataArray[0], String.format("%04d", Integer.parseInt(inputDataArray[1])));
                	} 
                }
            }
        } catch (Exception ex) {
        	ex.printStackTrace();
        } finally {
        	br.close();
        }	
	}
    
	/**
	 * Map method to parse XML and generate psv output
	 */
	public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
		
		inputParts = value.toString().split(DaaSConstants.TAB_DELIMITER);
		ownerShipType = inputParts[DaaSConstants.XML_REC_REST_OWSH_TYP_SHRT_DS_POS]; 
		xmlText = inputParts[DaaSConstants.XML_REC_XML_TEXT_POS];
		
        try{
               document = docBuilder.parse(new InputSource(new StringReader(xmlText)));
               rootElement = (Element) document.getFirstChild();
               
               if(rootElement.getNodeName().equals("TLD")) {
            	   
            	    pmixDataMap = new HashMap<String, PmixData>();
            	    
					terrCd = rootElement.getAttribute("gdwTerrCd");
					lgcyLclRfrDefCd = String.valueOf(Integer.parseInt(rootElement.getAttribute("gdwLgcyLclRfrDefCd")));
					businessDate = rootElement.getAttribute("businessDate");
					gdwMcdGbalLcatIdNu = rootElement.getAttribute("gdwMcdGbalLcatIdNu");
					
					gdwBusinessDate = businessDate.substring(0, 4) + APTDriver.GDW_DATE_SEPARATOR + businessDate.substring(4, 6) + APTDriver.GDW_DATE_SEPARATOR + businessDate.substring(6);
					haviBusinessDate = businessDate.substring(4, 6) + APTDriver.HAVI_DATE_SEPARATOR + businessDate.substring(6) + APTDriver.HAVI_DATE_SEPARATOR  + businessDate.substring(0,4);
					
					haviTimeKey= APTUtil.calculateJulianNumber(Integer.parseInt(businessDate.substring(4, 6)), Integer.parseInt(businessDate.substring(6)), Integer.parseInt(businessDate.substring(0,4)));
					
                    nlNode = rootElement.getChildNodes();
      				if (nlNode != null && nlNode.getLength() > 0) {
      					for (int idxNode = 0; idxNode < nlNode.getLength(); idxNode++) {						
      						if(nlNode.item(idxNode).getNodeType() == Node.ELEMENT_NODE) {
      							eleNode = (Element) nlNode.item(idxNode);
      							if (eleNode.getNodeName().equals("Node")) { 
      								nlEvent = eleNode.getChildNodes();
      								if (nlEvent != null && nlEvent.getLength() > 0) {
      									for (int idxEvent = 0; idxEvent < nlEvent.getLength(); idxEvent++) {
      										if (nlEvent.item(idxEvent).getNodeType() == Node.ELEMENT_NODE) {
      											eleEvent = (Element) nlEvent.item(idxEvent);
      											if (eleEvent.getNodeName().equals("Event")) {
      												
      												eventType = eleEvent.getAttribute("Type");
      												eventTimeStamp = eleEvent.getAttribute("Time");
      												
      												if (eventType.equals("TRX_Sale") || eventType.equals("TRX_Refund") || eventType.equals("TRX_Overring")) {
      													eleTrx = null;
    													nlTRX = eleEvent.getChildNodes();
    													int idxTRX = 0;
    													while (eleTrx == null && idxTRX < nlTRX.getLength()) { 
    														if (nlEvent.item(idxTRX).getNodeType() == Node.ELEMENT_NODE) {
    															eleTrx = (Element) nlTRX.item(idxTRX);
    															status = eleTrx.getAttribute("status");
    															trxPOD = eleTrx.getAttribute("POD");	
    														}
    														idxTRX++;
    													}
      													
      													if ((eventType.equals("TRX_Sale") && status.equals("Paid")) || (eventType.equals("TRX_Refund") || eventType.equals("TRX_Overring"))) {
    														eleOrder = null;
    														nlOrder = eleTrx.getChildNodes();
    														int idxOrder = 0;
    														while (eleOrder == null && idxOrder < nlOrder.getLength()) {
    															if (nlOrder.item(idxOrder).getNodeType() == Node.ELEMENT_NODE) {
    																eleOrder = (Element) nlOrder.item(idxOrder);
    															}
    															idxOrder++;
    														}	
    														
    														totalAmount = new BigDecimal(eleOrder.getAttribute("totalAmount"));
    														orderKind = eleOrder.getAttribute("kind");
    														orderKey = eleOrder.getAttribute("key");
    														saleType = eleOrder.getAttribute("saleType");
    														
    														if (eventTimeStamp.length() == 14) {
    															modifiedEventTimeStamp = eventTimeStamp.substring(0, 4) + APTDriver.GDW_DATE_SEPARATOR + eventTimeStamp.substring(4, 6) + APTDriver.GDW_DATE_SEPARATOR + eventTimeStamp.substring(6, 8) + " " +
        																eventTimeStamp.substring(8, 10) + APTDriver.TIME_SEPARATOR  + eventTimeStamp.substring(10, 12) + APTDriver.TIME_SEPARATOR + eventTimeStamp.substring(12, 14);
    														} else {
    															modifiedEventTimeStamp = "0000-00-00 00:00:00";
    														}
    														
    														aptHdrStrBuf.setLength(0);
    														aptHdrStrBuf.append(terrCd).append(DaaSConstants.PIPE_DELIMITER)
																		.append(lgcyLclRfrDefCd).append(DaaSConstants.PIPE_DELIMITER)
																		.append(gdwMcdGbalLcatIdNu).append(DaaSConstants.PIPE_DELIMITER)
																		.append(gdwBusinessDate).append(DaaSConstants.PIPE_DELIMITER)
																		.append(modifiedEventTimeStamp).append(DaaSConstants.PIPE_DELIMITER)
			            							   					.append(orderKey).append(DaaSConstants.PIPE_DELIMITER)
			            							   					.append(eventType).append(DaaSConstants.PIPE_DELIMITER)
			            							   					.append(totalAmount).append(DaaSConstants.PIPE_DELIMITER)
			            							   					.append(trxPOD).append(DaaSConstants.PIPE_DELIMITER)
			            							   					.append(orderKind).append(DaaSConstants.PIPE_DELIMITER)
			            							   					.append(saleType);
    														aptHdrTxt.set(aptHdrStrBuf.toString());
    														
    														multipleOutputs.write(APTDriver.APTUS_TDA_Header, NullWritable.get(), aptHdrTxt);
    															
        													pmixDataMap = APTUtil.generateOrderDetail(eleOrder, pmixDataMap, lgcyLclRfrDefCd, orderKind, orderKey, eventType, 
        																							  comboItemLookup, primaryItemLookup);
    													}
      												}
      											}	
      										}	
      									}								
      								}
      							} 						
      						} 
      					}					
      				}
 			}
			
            String uniqueComponentString = "";  
			// PMIX Output
			for (String eachPmixKey : pmixDataMap.keySet()) {
				PmixData pmixData = pmixDataMap.get(eachPmixKey);
				if (pmixData.getMitmKey() < 80000000) {
					
					uniqueComponentString = APTUtil.getUniqueComponents(pmixData.getAllComboComponents());
					
					aptDtlKeyStrBuf.setLength(0);
					aptDtlValueStrBuf.setLength(0);
					
					aptDtlKeyStrBuf.append(lgcyLclRfrDefCd).append(DaaSConstants.PIPE_DELIMITER).append(businessDate);
					aptDtlValueStrBuf.append(APTDriver.STLD_TAG_INDEX).append(DaaSConstants.PIPE_DELIMITER)
									 .append(terrCd).append(DaaSConstants.PIPE_DELIMITER)
									 .append(lgcyLclRfrDefCd).append(DaaSConstants.PIPE_DELIMITER)
									 .append(gdwBusinessDate).append(DaaSConstants.PIPE_DELIMITER)
									 .append(pmixData.getMitmKey()).append(DaaSConstants.PIPE_DELIMITER)
									 .append(pmixData.getDlyPmixPrice()).append(DaaSConstants.PIPE_DELIMITER)
									 .append(APTDriver.DECIMAL_ZERO).append(DaaSConstants.PIPE_DELIMITER)
									 .append(pmixData.getUnitsSold()).append(DaaSConstants.PIPE_DELIMITER)
									 .append(pmixData.getComboQty()).append(DaaSConstants.PIPE_DELIMITER)
									 .append(pmixData.getTotalQty()).append(DaaSConstants.PIPE_DELIMITER)
									 .append(pmixData.getPromoQty()).append(DaaSConstants.PIPE_DELIMITER)
				                     .append(pmixData.getPromoComboQty()).append(DaaSConstants.PIPE_DELIMITER)
				                     .append(pmixData.getPromoTotalQty()).append(DaaSConstants.PIPE_DELIMITER)
									 .append(pmixData.getConsPrice()).append(DaaSConstants.PIPE_DELIMITER)
									 .append(ownerShipType).append(DaaSConstants.PIPE_DELIMITER)
									 .append(haviBusinessDate).append(DaaSConstants.PIPE_DELIMITER)
									 .append(haviTimeKey).append(DaaSConstants.PIPE_DELIMITER)
									 .append(pmixData.getOrderKey()).append(DaaSConstants.PIPE_DELIMITER)
									 .append(pmixData.getLvl0PmixFlag()).append(DaaSConstants.PIPE_DELIMITER)
									 .append(pmixData.getLvl0PmixQty()).append(DaaSConstants.PIPE_DELIMITER)
									 .append(uniqueComponentString).append(DaaSConstants.PIPE_DELIMITER)
									 .append(pmixData.getLvl0InCmbCmpFlag()).append(DaaSConstants.PIPE_DELIMITER)
									 .append(pmixData.getItemLevel()).append(DaaSConstants.PIPE_DELIMITER)
									 .append(gdwMcdGbalLcatIdNu).append(DaaSConstants.PIPE_DELIMITER)
									 .append(pmixData.getComboBrkDwnPrice());
					
					aptDtlKeyTxt.set(aptDtlKeyStrBuf.toString());
					aptDtlValueTxt.set(aptDtlValueStrBuf.toString());
					
					context.write(aptDtlKeyTxt, aptDtlValueTxt);
				}
			}
			
 		} catch (SAXException e) {
 			e.printStackTrace();
 		} catch (IOException e) {
 			e.printStackTrace();
 		} catch (InterruptedException e) {
 			e.printStackTrace();
 		}	
 	}
	
	/**
	 * Cleanup method to close multiple outputs
	 */
	@Override
	public void cleanup(Context context) throws IOException,InterruptedException {
		multipleOutputs.close();
        aptHdrTxt.clear();
    	aptDtlKeyTxt.clear();
    	aptDtlValueTxt.clear();
	}
}
