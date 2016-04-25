package com.mcd.gdw.daas.mapreduce;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.Calendar;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

//import org.apache.hadoop.filecache.DistributedCache;
//import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.mcd.gdw.daas.DaaSConstants;

public class TLDDataHubMapper extends Mapper<LongWritable, Text, Text, Text> {

	public class IncludeListKey {
	
		private String terrCd;
		private String lgcyLclRfrDefCd;
		
		public IncludeListKey(String terrCd
				             ,String lgcyLclRfrDefCd) {
			
			this.terrCd = terrCd;
			this.lgcyLclRfrDefCd = lgcyLclRfrDefCd;
			
		}
		
		public String getTerrCd() {
			
			return(this.terrCd);
			
		}
		
		public String getLgcyLclRfrDefCd() {
			
			return(this.lgcyLclRfrDefCd);
			
		}
		
		public String toString() {
			
			return(this.terrCd + "_" + this.lgcyLclRfrDefCd);
			
		}
	}
	
	public class IncludeListDates {
		
		private Calendar fromDt = Calendar.getInstance();
		private Calendar toDt = Calendar.getInstance();
		
		public IncludeListDates(String fromDt
				               ,String toDt) {
			
			String[] parts;
			
			parts = (fromDt+"-1-1").split("-");
			this.fromDt.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));

			parts = (toDt+"-1-1").split("-");
			this.toDt.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
			
		}
		
		public boolean isIsoDateBetween(String date) {
			
			String[] parts;

			parts = (date+"-1-1").split("-");
			
			return(isDateBetween(parts[0] + parts[1] + parts[2]));
			
		}

		public boolean isDateBetween(String date) {
			
			Calendar dt = Calendar.getInstance();
			boolean isBetween = false;

			dt.set(Integer.parseInt(date.substring(0, 4)), Integer.parseInt(date.substring(4, 6)), Integer.parseInt(date.substring(6, 8)));

			if ( (dt.equals(fromDt) || dt.after(fromDt)) && ( dt.equals(toDt) || dt.before(toDt)) ) {
				isBetween = true;
			}
			return(isBetween);
			
		}
	}
	
	private final static String SEPARATOR_CHARACTER = "\t";
	private final static String REC_POSTRN          = "TRN";
	private final static String REC_POSTRNOFFR      = "OFR";
	private final static String REC_POSTRNITM       = "ITM";
	private final static String REC_POSTRNITMOFFR   = "IOF";
	
	private String[] parts = null;
	private Text mapKey = new Text();
	private Text mapValue = new Text();

	private DocumentBuilderFactory docFactory = null;
	private DocumentBuilder docBuilder = null;
	private InputSource xmlSource = null;
	private Document doc = null;
	private StringReader strReader = null;

	private FileSplit fileSplit = null;
	private String fileName = "";
	
	private Calendar cal = Calendar.getInstance();

	private String posBusnDt;
    private String mcdGbalLcatIdNu;
    private String terrCd;
	private String posOrdKey;
	private String posRestId;
	private String posDvceId;
	private String posAreaTypShrtDs;
	private String posTrnStrtTs;
	private String posTrnTypCd;
	private String posMfySideCd;
	private String posPrdDlvrMethCd;
	private String posTotNetTrnAm;
	private String posTotNprdNetTrnAm;
	private String posTotTaxAm;
	private String posTotNprdTaxAm;
	private String posTotItmQt;
	private String posPaidForOrdTs;
	private String posTotKeyPrssTs;
	private String posOrdStrInSysTs;
	private String posOrdUniqId;
	private String posOrdStrtDt;
	private String posOrdStrtTm;
	private String posOrdEndDt;
	private String posOrdEndTm;
	private String offrCustId;
	private String ordOffrApplFl;
	private String dyptIdNu;
	private String dyptDs;
	private String dyOfCalWkDs;
 	private String hrIdNu; 

	private String untlTotKeyPrssScQt;
	private String untlStrInSysScQt;
	private String untlOrdRcllScQt;
	private String untlDrwrClseScQt;
	private String untlPaidScQt;
	private String untlSrvScQt;
	private String totOrdTmScQt;
	private String abovPsntTmTrgtFl;
	private String abovTotTmTrgtFl;
	private String abovTotMfyTrgtTmTmFl;
	private String abovTotFrntCterTrgtTmFl;
	private String abovTotDrvTrgtTmFl;
	private String abov50ScFl;
	private String bel25ScFl;
	private String heldTmScQt;
	private String ordHeldFl;

	private String posTrnItmSeqNu;
	private String posItmLvlNu;
	private String sldMenuItmId;
	private String posPrdTypCd;
	private String posItmActnCd;
	private String posItmTotQt;
	private String posItmGrllQt;
	private String posItmGrllModCd;
	private String posItmPrmoQt;
	private String posChgAftTotCd;
	private String posItmNetUntPrcB4PrmoAm;
	private String posItmTaxB4PrmoAm;
	private String posItmNetUntPrcB4DiscAm;
	private String posItmTaxB4DiscAm;
	private String posItmActUntPrcAm;
	private String posItmActTaxAm;
	private String posItmCatCd;
	private String posItmFmlyGrpCd;
	private String posItmVoidQt;
	private String posItmTaxRateAm;
	private String posItmTaxBasAm;
	private String itmOffrAppdFl;
	
	private boolean orderCustomerFoundF1 = false;
	private boolean orderOfferFoundF1 	 = false;
	private boolean itmOfferAppdFoundFl;
	private boolean isItemOfferFl = false;
	private boolean isPromoAppledNode = false;
	
	private HashMap<String,String> dayPartMap = new HashMap<String,String>();
//	private HashMap<String,IncludeListDates> includeListMap = new HashMap<String,IncludeListDates>();
	private HashMap<String,Integer> promoIdMap = new HashMap<String,Integer>();

	private StringBuffer outputKey = new StringBuffer();
	private StringBuffer outputTextValue = new StringBuffer();
	
	
	
	
	
	private StringBuffer orderInfo 			  		= new StringBuffer();
	private StringBuffer allInfo 			 		= new StringBuffer();
	private StringBuffer itemInfo 			 	 	= new StringBuffer();
	private StringBuffer itemPromotionAppliedInfo 	= new StringBuffer();
	private StringBuffer orderCustomerInfo			= new StringBuffer();
	private StringBuffer orderPromotionsInfo 		= new StringBuffer();
	private StringBuffer orderOfferInfo 			= new StringBuffer();
	private StringBuffer orderPOSTimingsInfo 		= new StringBuffer();
	private StringBuffer orderCustomInfoInfo 		= new StringBuffer();
	private StringBuffer orderReductionInfo 		= new StringBuffer();
	
	private HashMap<Integer,String> tendersInfoMap = new HashMap<Integer,String>();
	
	private StringBuffer tenderInfo		    		= new StringBuffer();
	
	@Override
	public void setup(Context context) {
	      
		URI[] distPaths;
	    //Path distpath = null;
	    BufferedReader br = null;
	    String[] distPathParts;

        fileSplit = (FileSplit) context.getInputSplit();
        fileName = fileSplit.getPath().getName();

		try {
			docFactory = DocumentBuilderFactory.newInstance();
			docBuilder = docFactory.newDocumentBuilder();
			
		    //distPaths = DistributedCache.getLocalCacheFiles(context.getConfiguration());
		    distPaths = context.getCacheFiles();
		    
		    if (distPaths == null){
		    	System.err.println("distpath is null");
		    	System.exit(8);
		    }
		      
		    if ( distPaths != null && distPaths.length > 0 )  {
		    	  
		    	System.out.println(" number of distcache files : " + distPaths.length);
		    	  
		    	for ( int i=0; i<distPaths.length; i++ ) {
			    	  
			    	  //distpath = distPaths[i];
				     
			    	  System.out.println("distpaths:" + distPaths[i].toString());
			    	  //System.out.println("distpaths URI:" + distPaths[i].toUri());
			    	  
			    	  distPathParts = 	distPaths[i].toString().split("#");
			    	  
			    	  if( distPaths[i].toString().contains("DayPart_ID.psv") ) {
			    		  		      	  
			    		  br  = new BufferedReader(new FileReader("./" + distPathParts[1])); 
				      	  addDaypartKeyValuestoMap(br);
				      	  System.out.println("Loaded Daypart Values Map");
				      	  
				      } 
//			    	  else if ( distPaths[i].toString().contains("offers_include_list.txt") ) {
//    	  		      	  
//				    	  br  = new BufferedReader(new FileReader("./" + distPathParts[1])); 
//				    	  addIncludeListToMap(br);
//				      	  System.out.println("Loaded Include List Values Map");
//				      }
			      }
		      }
			
		} catch (Exception ex) {
			System.err.println("Error in initializing OffersReportFormatMapper:");
			System.err.println(ex.toString());
			System.exit(8);
		}
		
	}

	private void addDaypartKeyValuestoMap(BufferedReader br) {
	
		String line = null;
		String[] parts;
		
		String dayPartKey;
		String terrCd;
		String dayOfWeek;
		String startTime;
		String daypartId;
		String daypartDs;
		
		String timeSegment;
		
		try {
			while ((line = br.readLine()) != null) {
				if (line != null && !line.isEmpty()) {
					parts = line.split("\\|", -1);

					terrCd      = String.format("%03d", Integer.parseInt(parts[0]));
					dayOfWeek   = parts[1];
					timeSegment = parts[2];
					startTime   = parts[3];

					daypartId   = parts[5];
					daypartDs   = parts[6];
						
					if ( timeSegment.equalsIgnoreCase("QUARTER HOURLY") ) {
						dayPartKey = terrCd + SEPARATOR_CHARACTER + dayOfWeek + SEPARATOR_CHARACTER + startTime.substring(0, 2) + startTime.substring(3, 5);
							
						dayPartMap.put(dayPartKey,daypartId+SEPARATOR_CHARACTER+daypartDs);
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(8);
		} finally {
			try {
				if (br != null)
					br.close();
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

	}
	
//	private void addIncludeListToMap(BufferedReader br) {
//		
//		String line = null;
//		String[] parts;
//		
//		String terrCd;
//		String lgcyLclRfrDefCd;
//		String fromDt;
//		String toDt;
//		
//		try {
//			while ((line = br.readLine()) != null) {
//				if (line != null && !line.isEmpty()) {
//					parts = line.split("\t", -1);
//
//					terrCd          = String.format("%03d", Integer.parseInt(parts[0]));
//					lgcyLclRfrDefCd = parts[1];
//					fromDt          = parts[2];
//					toDt            = parts[3];
//
//					includeListMap.put(new IncludeListKey(terrCd,lgcyLclRfrDefCd).toString(),new IncludeListDates(fromDt,toDt));
//					
//					//System.out.println(terrCd + " " + lgcyLclRfrDefCd + " " + fromDt + " " + toDt );
//				}
//			}
//		} catch (Exception ex) {
//			ex.printStackTrace();
//			System.exit(8);
//		} finally {
//			try {
//				if (br != null)
//					br.close();
//				
//			} catch (Exception ex) {
//				ex.printStackTrace();
//			}
//		}
//	}
	
	@Override
	public void map(LongWritable key, Text value,Context context) throws IOException, InterruptedException {

		boolean includeFile = false;
		IncludeListKey includeListKey;
		
		try {
			if ( fileName.toUpperCase().contains("STLD") || fileName.toUpperCase().contains("DETAILEDSOS") ) {
				parts = value.toString().split("\t");
				
				if ( parts.length >= 8 ) {
					includeFile = true;
				}
			}
		
			
//			if ( includeFile ) {
//					
//				includeListKey = new IncludeListKey(parts[DaaSConstants.XML_REC_TERR_CD_POS],parts[DaaSConstants.XML_REC_LGCY_LCL_RFR_DEF_CD_POS]);
//				
//				if ( includeListMap.containsKey(includeListKey.toString()) ) {
//					if ( !includeListMap.get(includeListKey.toString()).isDateBetween(parts[DaaSConstants.XML_REC_POS_BUSN_DT_POS]) ) {
//						includeFile = false;
//					}
//				} else {
//					includeFile = false;
//				}
//			}
			
			if ( includeFile ) {
				terrCd = String.format("%03d", Integer.parseInt(parts[DaaSConstants.XML_REC_TERR_CD_POS]));
				posBusnDt = formatDateAsTsDtOnly(parts[DaaSConstants.XML_REC_POS_BUSN_DT_POS]);
				mcdGbalLcatIdNu = parts[DaaSConstants.XML_REC_MCD_GBAL_LCAT_ID_NU_POS];

				
				if ( parts[DaaSConstants.XML_REC_FILE_TYPE_POS].equalsIgnoreCase("STLD") ) {
			    	getOrderDataStld(parts[DaaSConstants.XML_REC_XML_TEXT_POS],context);

				} else if ( parts[DaaSConstants.XML_REC_FILE_TYPE_POS].equalsIgnoreCase("DETAILEDSOS") ) {
					getOrderDataDetailedSos(parts[DaaSConstants.XML_REC_XML_TEXT_POS],context);
				}
			}
			
		} catch (Exception ex) {
			System.err.println("Error occured in OffersReportFormatMapper.Map:");
			ex.printStackTrace(System.err);
			System.exit(8);
		}
		
	}
	
	private void getOrderDataStld(String xmlText
			                     ,Context context) {
		

		Element eleRoot;
		
		try {
			try {
				strReader  = new StringReader(xmlText);
				xmlSource = new InputSource(strReader);
				doc = docBuilder.parse(xmlSource);
			} catch (Exception ex1) {
				strReader  = new StringReader(xmlText.replaceAll("&#x1F" , "_"));
				xmlSource = new InputSource(strReader);
				doc = docBuilder.parse(xmlSource);
			}

			eleRoot = (Element) doc.getFirstChild();

			if ( eleRoot.getNodeName().equals("TLD") ) {
				posRestId = eleRoot.getAttribute("gdwLgcyLclRfrDefCd");

				processNode(eleRoot.getChildNodes(),context);
			}
		} catch (Exception ex) {
			System.err.println("Error occured in OffersReportFormatMapper.getOrderData:");
			ex.printStackTrace(System.err);
			System.exit(8); 
		}
		
	}

	private void processNode(NodeList nlNode
			                ,Context context) {

		Element eleNode;
		
		if (nlNode != null && nlNode.getLength() > 0 ) {
			for (int idxNode=0; idxNode < nlNode.getLength(); idxNode++ ) {
				if ( nlNode.item(idxNode).getNodeType() == Node.ELEMENT_NODE ) {  
					eleNode = (Element)nlNode.item(idxNode);
					if ( eleNode.getNodeName().equals("Node") ) {
						posDvceId = eleNode.getAttribute("id");
						
						processEvent(eleNode.getChildNodes(),context);
					}
				}
			}
		}
		
	}

	private void processEvent(NodeList nlEvent
			                 ,Context context) {

		Element eleEvent;
		
		if (nlEvent != null && nlEvent.getLength() > 0 ) {
			for (int idxEvent=0; idxEvent < nlEvent.getLength(); idxEvent++ ) {
				if ( nlEvent.item(idxEvent).getNodeType() == Node.ELEMENT_NODE ) {  
					eleEvent = (Element)nlEvent.item(idxEvent);
					if ( eleEvent.getNodeName().equals("Event") &&
					     (eleEvent.getAttribute("Type").equalsIgnoreCase("TRX_Sale") ||
					    		 eleEvent.getAttribute("Type").equalsIgnoreCase("TRX_Refund") ||
					    		 eleEvent.getAttribute("Type").equalsIgnoreCase("TRX_Overring") ||
					    		 eleEvent.getAttribute("Type").equalsIgnoreCase("TRX_Waste") )) {
						processTrxSale(eleEvent,eleEvent.getChildNodes(),context);
					}
				}
			}
		}
		
	}
	private void processTrxSale(Element eleEvent,NodeList nlTrxSale
			                   ,Context context) {

		Element eleTrxSale; 
		
		if (nlTrxSale != null && nlTrxSale.getLength() > 0 ) {
			for (int idxTrxSale=0; idxTrxSale < nlTrxSale.getLength(); idxTrxSale++ ) {
				if ( nlTrxSale.item(idxTrxSale).getNodeType() == Node.ELEMENT_NODE ) {
					eleTrxSale = (Element)nlTrxSale.item(idxTrxSale);

					if ( !eleEvent.getAttribute("Type").equalsIgnoreCase("TRX_Sale") ||
							(eleEvent.getAttribute("Type").equalsIgnoreCase("TRX_Sale") && eleTrxSale.getAttribute("status").equals("Paid")) ) {
						posAreaTypShrtDs = eleTrxSale.getAttribute("POD");
						
						processOrder(eleTrxSale.getChildNodes(),context);
					}
				}
			}
		}

	}

	private void processOrder(NodeList nlOrder
			                 ,Context context) {

		Element eleOrder;
		
		try {
			if (nlOrder != null && nlOrder.getLength() > 0 ) {
				for (int idxOrder=0; idxOrder < nlOrder.getLength(); idxOrder++ ) {
					if ( nlOrder.item(idxOrder).getNodeType() == Node.ELEMENT_NODE ) {
						eleOrder = (Element)nlOrder.item(idxOrder);
						
						posOrdKey = getValue(eleOrder,"key");
						
//						if(!posOrdKey.equalsIgnoreCase("POS0001:1001435871")) 
//							return;
//						
						
						posTrnStrtTs = formatAsTs(eleOrder.getAttribute("Timestamp"));

						getDaypart();
						
						itmOfferAppdFoundFl = false;
						
						posTrnTypCd = getValue(eleOrder,"kind");
						posMfySideCd = getValue(eleOrder,"side");
						posPrdDlvrMethCd = getValue(eleOrder,"saleType");
						posTotNetTrnAm = getValue(eleOrder,"totalAmount");
						posTotNprdNetTrnAm = getValue(eleOrder,"nonProductAmount");
						posTotTaxAm = getValue(eleOrder,"totalTax");
						posTotNprdTaxAm = getValue(eleOrder,"nonProductTax");
						posOrdUniqId = getValue(eleOrder,"uniqueId");
						posOrdStrtDt = formatDateAsTs(eleOrder.getAttribute("startSaleDate"));
						posOrdStrtTm = formatTimeAsTs(eleOrder.getAttribute("startSaleTime"),posOrdStrtDt);
						posOrdEndDt = formatDateAsTs(eleOrder.getAttribute("endSaleDate"));
						posOrdEndTm = formatTimeAsTs(eleOrder.getAttribute("endSaleTime"),posOrdEndDt);

					

//						context.getCounter("COUNT","POS_TRN_ITM_UNIQUE").increment(1);

						
						orderInfo.setLength(0);
						orderInfo.append(posBusnDt).append(SEPARATOR_CHARACTER);
						orderInfo.append(posOrdKey).append(SEPARATOR_CHARACTER);//@TODO verify
						orderInfo.append(posRestId).append(SEPARATOR_CHARACTER);
						orderInfo.append(mcdGbalLcatIdNu).append(SEPARATOR_CHARACTER);
						orderInfo.append(terrCd).append(SEPARATOR_CHARACTER);
						orderInfo.append(posAreaTypShrtDs).append(SEPARATOR_CHARACTER);
						orderInfo.append(posTrnStrtTs).append(SEPARATOR_CHARACTER);
						orderInfo.append(posOrdUniqId).append(SEPARATOR_CHARACTER);
						orderInfo.append(posTrnTypCd).append(SEPARATOR_CHARACTER);
						orderInfo.append(posOrdKey).append(SEPARATOR_CHARACTER);
						orderInfo.append(posMfySideCd).append(SEPARATOR_CHARACTER);
						orderInfo.append(posPrdDlvrMethCd).append(SEPARATOR_CHARACTER);
						orderInfo.append(posTotNetTrnAm).append(SEPARATOR_CHARACTER);
						orderInfo.append(posTotNprdNetTrnAm).append(SEPARATOR_CHARACTER);
						orderInfo.append(posTotTaxAm).append(SEPARATOR_CHARACTER);
						orderInfo.append(posTotNprdTaxAm).append(SEPARATOR_CHARACTER);
						orderInfo.append(posOrdStrtDt).append(SEPARATOR_CHARACTER);
						orderInfo.append(posOrdStrtTm).append(SEPARATOR_CHARACTER);
						orderInfo.append(posOrdEndDt).append(SEPARATOR_CHARACTER);
						orderInfo.append(posOrdEndTm);

						
						allInfo.setLength(0);
						
//						System.out.println ( "orderInfo :" + orderInfo.toString());
						
						processOrderItems(eleOrder.getChildNodes(),context);
						processItem(eleOrder.getChildNodes(),context);
						
					}
				}
			}
		} catch (Exception ex) {
			System.err.println("Error occured in OffersReportFormatMapper.processOrder:");
			ex.printStackTrace(System.err);
			System.exit(8);
		}

	}
	
	private void processOrderItems(NodeList nlOrderItems
			                      ,Context context) {

		Element eleOrderItems;
		
		posTotItmQt = "";
		posPaidForOrdTs = "";
		posTotKeyPrssTs = "";
		posOrdStrInSysTs = "";
		offrCustId = "";
		ordOffrApplFl = "0";
		
		String offrOverride;
		String offrApplied;
		String tmpOffrCustId = "";
		String promoId = "";

		promoIdMap.clear();
		
		try {
			

			orderCustomerInfo.setLength(0);
			orderCustomerInfo.append("");
			
			orderPOSTimingsInfo.setLength(0);
			orderPOSTimingsInfo.append("").append(SEPARATOR_CHARACTER);//itemsCount
			orderPOSTimingsInfo.append("").append(SEPARATOR_CHARACTER);//until pay
			orderPOSTimingsInfo.append("").append(SEPARATOR_CHARACTER);//until total
			orderPOSTimingsInfo.append("");//until store
			
			orderCustomInfoInfo.setLength(0);
			
			orderCustomInfoInfo.append("").append(SEPARATOR_CHARACTER);
			orderCustomInfoInfo.append("").append(SEPARATOR_CHARACTER);
			orderCustomInfoInfo.append("").append(SEPARATOR_CHARACTER);
			orderCustomInfoInfo.append("");
			
			orderOfferInfo.setLength(0);
			orderOfferInfo.append("").append(SEPARATOR_CHARACTER);
			orderOfferInfo.append("").append(SEPARATOR_CHARACTER);
			orderOfferInfo.append("").append(SEPARATOR_CHARACTER);
			orderOfferInfo.append("").append(SEPARATOR_CHARACTER);
			orderOfferInfo.append("").append(SEPARATOR_CHARACTER);
			orderOfferInfo.append("");
			
			orderPromotionsInfo.setLength(0);
			orderPromotionsInfo.append("").append(SEPARATOR_CHARACTER);
			orderPromotionsInfo.append("").append(SEPARATOR_CHARACTER);
			orderPromotionsInfo.append("").append(SEPARATOR_CHARACTER);
			orderPromotionsInfo.append("").append(SEPARATOR_CHARACTER);
			orderPromotionsInfo.append("").append(SEPARATOR_CHARACTER);
			orderPromotionsInfo.append("").append(SEPARATOR_CHARACTER);
			orderPromotionsInfo.append("").append(SEPARATOR_CHARACTER);
			orderPromotionsInfo.append("");
			
			tenderInfo.setLength(0);
		    tenderInfo.append("").append(SEPARATOR_CHARACTER);//TenderId
            tenderInfo.append("").append(SEPARATOR_CHARACTER);//TenderKind
            tenderInfo.append("").append(SEPARATOR_CHARACTER);//TenderName
            tenderInfo.append("").append(SEPARATOR_CHARACTER);//TenderQuantity
            tenderInfo.append("").append(SEPARATOR_CHARACTER);//FaceValue
            tenderInfo.append("").append(SEPARATOR_CHARACTER);//TenderAmount
            tenderInfo.append("").append(SEPARATOR_CHARACTER);//cardproviderid
            tenderInfo.append("").append(SEPARATOR_CHARACTER);//cashlessdata
    
            orderReductionInfo.setLength(0);
            orderReductionInfo.append("").append(SEPARATOR_CHARACTER);
            orderReductionInfo.append("").append(SEPARATOR_CHARACTER);
            orderReductionInfo.append("").append(SEPARATOR_CHARACTER);
            orderReductionInfo.append("").append(SEPARATOR_CHARACTER);
            orderReductionInfo.append("").append(SEPARATOR_CHARACTER);
            orderReductionInfo.append("").append(SEPARATOR_CHARACTER);
            orderReductionInfo.append("");
			
			
	           
			for(int i=1;i<=5;i++){   
		           tendersInfoMap.put(i, tenderInfo.toString());
		            
			}
			
			
			
			if (nlOrderItems != null && nlOrderItems.getLength() > 0 ) {
				for (int idxOrderItems=0; idxOrderItems < nlOrderItems.getLength(); idxOrderItems++ ) {
					if ( nlOrderItems.item(idxOrderItems).getNodeType() == Node.ELEMENT_NODE ) {
						eleOrderItems = (Element)nlOrderItems.item(idxOrderItems);
					
						
						if ( eleOrderItems.getNodeName().equals("Customer") ) {
							
							orderCustomerFoundF1 = true;
							offrCustId = getValue(eleOrderItems,"id");
							
							orderCustomerInfo.setLength(0);
							orderCustomerInfo.append(offrCustId);
						}else
						if ( eleOrderItems.getNodeName().equals("POSTimings") ) {
							posTotItmQt = getValue(eleOrderItems,"itemsCount");
							posPaidForOrdTs = formatAsTs(eleOrderItems.getAttribute("untilPay"));
							posTotKeyPrssTs = formatAsTs(eleOrderItems.getAttribute("untilTotal"));
							posOrdStrInSysTs = formatAsTs(eleOrderItems.getAttribute("untilStore"));
							
							
							orderPOSTimingsInfo.setLength(0);
							orderPOSTimingsInfo.append(posTotItmQt).append(SEPARATOR_CHARACTER);//itemsCount
							orderPOSTimingsInfo.append(posPaidForOrdTs).append(SEPARATOR_CHARACTER);//until pay
							orderPOSTimingsInfo.append(posTotKeyPrssTs).append(SEPARATOR_CHARACTER);//until total
							orderPOSTimingsInfo.append(posOrdStrInSysTs);//until store
							
						}
						
						
						else if ( eleOrderItems.getNodeName().equals("CustomInfo") ) {
							
							orderCustomInfoInfo.setLength(0);
							
							orderCustomInfoInfo.append(eleOrderItems.getAttribute("orderId")).append(SEPARATOR_CHARACTER);
							orderCustomInfoInfo.append(eleOrderItems.getAttribute("customerId")).append(SEPARATOR_CHARACTER);
							orderCustomInfoInfo.append(eleOrderItems.getAttribute("IsPaidMobileOrder")).append(SEPARATOR_CHARACTER);
							orderCustomInfoInfo.append(eleOrderItems.getAttribute("checkInData"));
							
							
						}
						
						else if ( eleOrderItems.getNodeName().equals("Offers") ) {
							
							orderOfferFoundF1 = true;
							ordOffrApplFl = "1";
							
							orderOfferInfo.setLength(0);
							orderOfferInfo.append(eleOrderItems.getAttribute("tagId")).append(SEPARATOR_CHARACTER);
							orderOfferInfo.append(eleOrderItems.getAttribute("offerId")).append(SEPARATOR_CHARACTER);
							orderOfferInfo.append(eleOrderItems.getAttribute("override")).append(SEPARATOR_CHARACTER);
							orderOfferInfo.append(eleOrderItems.getAttribute("applied")).append(SEPARATOR_CHARACTER);
							orderOfferInfo.append(eleOrderItems.getAttribute("clearAfterOverride")).append(SEPARATOR_CHARACTER);
							orderOfferInfo.append(eleOrderItems.getAttribute("promotionId"));
							
							
							
							promoId = getValue(eleOrderItems,"promotionId");
							if ( promoId.length() > 0 ) {
								if ( promoIdMap.containsKey(promoId) ) {
									promoIdMap.put(promoId, promoIdMap.get(promoId) + 1);
								} else {
									promoIdMap.put(promoId, 1);
								}
							}
						}else if(eleOrderItems.getNodeName().equals("Promotions") ){
							
							Element promotion = (Element)(eleOrderItems.getElementsByTagName("Promotion").item(0));
							
							orderPromotionsInfo.setLength(0);
							orderPromotionsInfo.append(promotion.getAttribute("promotionId")).append(SEPARATOR_CHARACTER);
							orderPromotionsInfo.append(promotion.getAttribute("promotionCounter")).append(SEPARATOR_CHARACTER);
							orderPromotionsInfo.append(promotion.getAttribute("discountType")).append(SEPARATOR_CHARACTER);
							orderPromotionsInfo.append(promotion.getAttribute("discountAmount")).append(SEPARATOR_CHARACTER);
							orderPromotionsInfo.append(promotion.getAttribute("offerId")).append(SEPARATOR_CHARACTER);
							orderPromotionsInfo.append(promotion.getAttribute("exclusive")).append(SEPARATOR_CHARACTER);
							orderPromotionsInfo.append(promotion.getAttribute("promotionOnTender")).append(SEPARATOR_CHARACTER);
							orderPromotionsInfo.append(promotion.getAttribute("returnedValue"));
							
							
						}else if ( eleOrderItems.getNodeName().equals("Reduction") ) {
							orderReductionInfo.setLength(0);
							
							 NodeList nlReductions = eleOrderItems.getChildNodes();
							 Element eleTender;
							
							 Node textNode;
							 String textNodeName;
							 Element eleReduction;
							
							 
							 if ( nlReductions != null && nlReductions.getLength() > 0 ) {
					              for (int idxReductions = 0; idxReductions < nlReductions.getLength(); idxReductions++ ) {
					                if ( nlReductions.item(idxReductions).getNodeType() == Node.ELEMENT_NODE ) {
					                	eleReduction = (Element)nlReductions.item(idxReductions);

					                  textNodeName  = eleReduction.getNodeName();
					                  textNode 		= eleReduction.getFirstChild();
					 
					                  String qty = "";
					                  String afterTotal = "";
					                  String beforeTotal = "";
					                  String amount ="";
					                  String amountAfterTotal = "";
					                  String amountBeforeTotal = "";
					                  
					                  if ( textNodeName.equalsIgnoreCase("Qty") && textNode != null && textNode.getNodeType() == Node.TEXT_NODE ) {
					                	  qty = textNode.getNodeValue();
						               }else if ( textNodeName.equalsIgnoreCase("AfterTotal") && textNode != null && textNode.getNodeType() == Node.TEXT_NODE ) {
						            	   afterTotal = textNode.getNodeValue();
							           }else if ( textNodeName.equalsIgnoreCase("BeforeTotal") && textNode != null && textNode.getNodeType() == Node.TEXT_NODE ) {
							        	   beforeTotal = textNode.getNodeValue();
							           }else if ( textNodeName.equalsIgnoreCase("Amount") && textNode != null && textNode.getNodeType() == Node.TEXT_NODE ) {
							        	   amount = textNode.getNodeValue();
							           }else if ( textNodeName.equalsIgnoreCase("AmountAfterTotal") && textNode != null && textNode.getNodeType() == Node.TEXT_NODE ) {
							        	   amountAfterTotal = textNode.getNodeValue();
							           }else if ( textNodeName.equalsIgnoreCase("AmountBeforeTotal") && textNode != null && textNode.getNodeType() == Node.TEXT_NODE ) {
							        	   amountBeforeTotal = textNode.getNodeValue();
							           }
					               
					                  orderReductionInfo.setLength(0);
						              orderReductionInfo.append("1").append(SEPARATOR_CHARACTER);
						              orderReductionInfo.append(qty).append(SEPARATOR_CHARACTER);
						              orderReductionInfo.append(afterTotal).append(SEPARATOR_CHARACTER);
						              orderReductionInfo.append(beforeTotal).append(SEPARATOR_CHARACTER);
						              orderReductionInfo.append(amount).append(SEPARATOR_CHARACTER);
						              orderReductionInfo.append(amountAfterTotal).append(SEPARATOR_CHARACTER);
						              orderReductionInfo.append(amountBeforeTotal);
						              
						              break;
					                
					                }
					             
					              
					                 
					              }
					              
					              
					              
							 }
							
						}
						
						else if(eleOrderItems.getNodeName().equals("Tenders") ){
							
							 NodeList nlTenders = eleOrderItems.getChildNodes();
							 Element eleTender;
							 NodeList nlTenderSubItems;
							 Node textNode;
							 String textNodeName;
							 Element eleTenderSubItem;
							 
							 int numtenders = 0;
							 
						      if ( nlTenders !=null && nlTenders.getLength() > 0 ) {
						        for (int idxTenders=0; idxTenders < nlTenders.getLength(); idxTenders++ ) {
						          if ( nlTenders.item(idxTenders).getNodeType() == Node.ELEMENT_NODE ) {
						        	  
						        	  numtenders ++;
						        	  
						            eleTender = (Element)nlTenders.item(idxTenders);

						            nlTenderSubItems = eleTender.getChildNodes();
						            String tenderId  = "";
						            String tenderKind = "@@@@";
						            String tenderName = "@@@@";
						            String tenderAmount = "0.00";
						            String tenderCardProvider = "@@@@";
						            String tenderPaymentName ="";
						            String tenderQuantity = "";
						            String faceValue = "";
						            String cashlessData = "";
						                   
						            tenderInfo.setLength(0);
						            int numberTenders = 0;
						            
						            if ( nlTenderSubItems != null && nlTenderSubItems.getLength() > 0 ) {
						              for (int idxTenderSubItems = 0; idxTenderSubItems < nlTenderSubItems.getLength(); idxTenderSubItems++ ) {
						                if ( nlTenderSubItems.item(idxTenderSubItems).getNodeType() == Node.ELEMENT_NODE ) {
						                  eleTenderSubItem = (Element)nlTenderSubItems.item(idxTenderSubItems);

						                  textNodeName = eleTenderSubItem.getNodeName();
						                  textNode = eleTenderSubItem.getFirstChild();
						 
						                  if ( textNodeName.equalsIgnoreCase("TenderId") && textNode != null && textNode.getNodeType() == Node.TEXT_NODE ) {
						                	  tenderId = textNode.getNodeValue();
							                  }
						                  
						                  else if ( textNodeName.equalsIgnoreCase("TenderKind") && textNode != null && textNode.getNodeType() == Node.TEXT_NODE ) {
						                    tenderKind = textNode.getNodeValue();
						                  }

						                  else if ( textNodeName.equalsIgnoreCase("TenderName") && textNode != null && textNode.getNodeType() == Node.TEXT_NODE ) {
						                    tenderName = textNode.getNodeValue();
						                  }

						                  else if ( textNodeName.equalsIgnoreCase("TenderAmount") && textNode != null && textNode.getNodeType() == Node.TEXT_NODE ) {
						                    tenderAmount = textNode.getNodeValue();
						                  }
						                  
						                  else if ( textNodeName.equalsIgnoreCase("CardProviderID") && textNode != null && textNode.getNodeType() == Node.TEXT_NODE && 
						                		  textNode.getNodeValue() != null && !textNode.getNodeValue().trim().isEmpty()) {
						                    tenderCardProvider = textNode.getNodeValue();
						                  }
						                  
						                  else if ( textNodeName.equalsIgnoreCase("TenderQuantity") && textNode != null && textNode.getNodeType() == Node.TEXT_NODE ) {
						                	  tenderQuantity = textNode.getNodeValue();
							                  }
						                  else if ( textNodeName.equalsIgnoreCase("FaceValue") && textNode != null && textNode.getNodeType() == Node.TEXT_NODE ) {
						                	  faceValue = textNode.getNodeValue();
							                  }
						                  else if ( textNodeName.equalsIgnoreCase("CashlessData") && textNode != null && textNode.getNodeType() == Node.TEXT_NODE ) {
						                	  cashlessData = textNode.getNodeValue();
							                  }
						                  
						                }
						              }

						              if ( ! tenderName.equals("@@@@") && ( tenderKind.equals("0") || tenderKind.equals("1") || tenderKind.equals("2") || tenderKind.equals("3") || tenderKind.equals("4") || tenderKind.equals("5") || tenderKind.equals("8") ) ) { 
						                if ( tenderCardProvider.equals("@@@@") ) {
						                  tenderPaymentName = tenderName;
						                } else {
						                  tenderPaymentName = "Cashless-" + tenderCardProvider;
						                }

						               
						              }
						            }
						            
						           
						            tenderInfo.setLength(0);
						            
						            tenderInfo.append(tenderId).append(SEPARATOR_CHARACTER);//TenderId
						            tenderInfo.append(tenderKind).append(SEPARATOR_CHARACTER);//TenderKind
						            tenderInfo.append(tenderPaymentName).append(SEPARATOR_CHARACTER);//TenderName
						            tenderInfo.append(tenderQuantity).append(SEPARATOR_CHARACTER);//TenderQuantity
						            tenderInfo.append(faceValue).append(SEPARATOR_CHARACTER);//FaceValue
						            tenderInfo.append(tenderAmount).append(SEPARATOR_CHARACTER);//TenderAmount
						            tenderInfo.append(tenderCardProvider).append(SEPARATOR_CHARACTER);
						            tenderInfo.append(cashlessData).append(SEPARATOR_CHARACTER);
						            
						            tendersInfoMap.put(numtenders, tenderInfo.toString());
						            
						            numberTenders++;
						          }//tenders
						        }
						      }
						      
						}
					}
				}
			}
		} catch (Exception ex) {
			System.err.println("Error occured in OffersReportFormatMapper.processOrderItems:");
			ex.printStackTrace(System.err);
			System.exit(8);
		}

	}

	private void processItem(NodeList nlItem
                            ,Context context) {
		
		Element eleItem;
		@SuppressWarnings("unused")
		int qty;
		
		try {
			if (nlItem != null && nlItem.getLength() > 0 ) {
				for (int idxItem=0; idxItem < nlItem.getLength(); idxItem++ ) {
					if ( nlItem.item(idxItem).getNodeType() == Node.ELEMENT_NODE ) {
						eleItem = (Element)nlItem.item(idxItem);
						if ( eleItem.getNodeName().equals("Item") ) {

							posTrnItmSeqNu = getValue(eleItem,"id");
							posItmLvlNu = getValue(eleItem,"level");
							sldMenuItmId = getValue(eleItem,"code");
							posPrdTypCd = getValue(eleItem,"type");
							posItmActnCd = getValue(eleItem,"action");
							posItmTotQt = getValue(eleItem,"qty");
							posItmGrllQt = getValue(eleItem,"grillQty");
							posItmGrllModCd = getValue(eleItem,"grillModifer");
							posItmPrmoQt = getValue(eleItem,"qtyPromo");
							posChgAftTotCd = getValue(eleItem,"chgAfterTotal");
							posItmNetUntPrcB4PrmoAm = getValue(eleItem,"BPPrice");
							posItmTaxB4PrmoAm = getValue(eleItem,"BPTax");
							posItmNetUntPrcB4DiscAm = getValue(eleItem,"BDPrice");
							posItmTaxB4DiscAm = getValue(eleItem,"BDTax");
							posItmActUntPrcAm = getValue(eleItem,"totalPrice");
							posItmActTaxAm = getValue(eleItem,"totalTax");
							posItmCatCd = getValue(eleItem,"category");
							posItmFmlyGrpCd = getValue(eleItem,"familyGroup");
							posItmVoidQt = getValue(eleItem,"qtyVoided");
							posItmTaxRateAm = getValue(eleItem,"unitPrice");
							posItmTaxBasAm = getValue(eleItem,"unitTax");
							itmOffrAppdFl = "0";

							try {
								qty = Integer.parseInt(posItmTotQt);
							} catch (Exception ex) {
								posItmTotQt = "0";
							}

							try {
								qty = Integer.parseInt(posItmGrllQt);
							} catch (Exception ex) {
								posItmGrllQt = "0";
							}

							try {
								qty = Integer.parseInt(posItmPrmoQt);
							} catch (Exception ex) {
								posItmPrmoQt = "0";
							}

							
							itemInfo.setLength(0);
							itemInfo.append(sldMenuItmId).append(SEPARATOR_CHARACTER);//code
							itemInfo.append(posPrdTypCd).append(SEPARATOR_CHARACTER);//type
							itemInfo.append(posItmActnCd).append(SEPARATOR_CHARACTER);//action
							itemInfo.append(posItmLvlNu).append(SEPARATOR_CHARACTER);//level
							itemInfo.append(posTrnItmSeqNu).append(SEPARATOR_CHARACTER);//id
							itemInfo.append(posItmTotQt).append(SEPARATOR_CHARACTER);//qty
							itemInfo.append(posItmGrllQt).append(SEPARATOR_CHARACTER);//grillQty
							itemInfo.append(posItmGrllModCd).append(SEPARATOR_CHARACTER);//grillModifier
							itemInfo.append(posItmPrmoQt).append(SEPARATOR_CHARACTER);//qtyPromo
							itemInfo.append(posChgAftTotCd).append(SEPARATOR_CHARACTER);//chgAfterTotal
							itemInfo.append(posItmNetUntPrcB4PrmoAm).append(SEPARATOR_CHARACTER);//bpPrice
							itemInfo.append(posItmTaxB4PrmoAm).append(SEPARATOR_CHARACTER);//bpTax
							itemInfo.append(posItmNetUntPrcB4DiscAm).append(SEPARATOR_CHARACTER);//bdPrice
							itemInfo.append(posItmTaxB4DiscAm).append(SEPARATOR_CHARACTER);//bdTax
							itemInfo.append(posItmActUntPrcAm).append(SEPARATOR_CHARACTER);//totalPrice
							itemInfo.append(posItmActTaxAm).append(SEPARATOR_CHARACTER);//totalTax
							itemInfo.append(posItmCatCd).append(SEPARATOR_CHARACTER);//category
							itemInfo.append(posItmFmlyGrpCd).append(SEPARATOR_CHARACTER);//familyGroup
							itemInfo.append(posItmVoidQt).append(SEPARATOR_CHARACTER);//qtyVoided
							itemInfo.append(posItmTaxRateAm).append(SEPARATOR_CHARACTER);//unitPrice
							itemInfo.append(posItmTaxBasAm).append(SEPARATOR_CHARACTER);//unitTax
											

							processItemOffers(eleItem.getChildNodes(),context);
							
							itemInfo.append(itmOfferAppdFoundFl);			
							
//							System.out.println ( "orderInfo in processItem :" + orderInfo.toString());
//							System.out.println ( "itemInfo in processItem :" + itemInfo.toString());
//							System.out.println ( "itemPromotionAppliedInfo in processItem :" + itemPromotionAppliedInfo.toString());
//							System.out.println ( "orderPromotionsInfo in processItem :" + orderPromotionsInfo.toString());
//							System.out.println ( "orderOfferInfo in processItem :" + orderOfferInfo.toString());
//							System.out.println ( "tenderInfo in processItem :" + tenderInfo.toString());
//							System.out.println ( "orderPOSTimingsInfo in processItem :" + orderPOSTimingsInfo.toString());
//							System.out.println ( "orderCustomInfoInfo in processItem :" + orderCustomInfoInfo.toString());
							
							allInfo.setLength(0);
							allInfo.append("1").append(SEPARATOR_CHARACTER);
							allInfo.append(orderInfo).append(SEPARATOR_CHARACTER);
							allInfo.append(itemInfo).append(SEPARATOR_CHARACTER);
							allInfo.append(itemPromotionAppliedInfo).append(SEPARATOR_CHARACTER);
							allInfo.append(orderPromotionsInfo).append(SEPARATOR_CHARACTER);
							allInfo.append(orderOfferInfo).append(SEPARATOR_CHARACTER);
							
							for(int i=1;i<=5;i++){
								
								allInfo.append(tendersInfoMap.get(i));
							
							}
							
							allInfo.append(orderPOSTimingsInfo).append(SEPARATOR_CHARACTER);
							allInfo.append(orderReductionInfo).append(SEPARATOR_CHARACTER);
							allInfo.append(orderCustomerInfo).append(SEPARATOR_CHARACTER);
							
							if(orderOfferFoundF1 || orderCustomerFoundF1 || isItemOfferFl){
								allInfo.append("1").append(SEPARATOR_CHARACTER);
							}else{
								allInfo.append("").append(SEPARATOR_CHARACTER);
							}
							
							allInfo.append(dyptIdNu).append(SEPARATOR_CHARACTER);
							allInfo.append(orderCustomInfoInfo);
							
							
							outputKey.setLength(0);						
							outputKey.append(REC_POSTRNITM);
							outputKey.append(terrCd);
							outputKey.append(posBusnDt);
							outputKey.append(posOrdKey);
							outputKey.append(SEPARATOR_CHARACTER);
							outputKey.append(mcdGbalLcatIdNu);
							
							mapKey.clear();
							mapKey.set(outputKey.toString());
							
							mapValue.clear();
							mapValue.set(allInfo.toString());
							
							context.write(mapKey,mapValue);
							
//							System.out.println( "orderInfo ************************************* " + orderInfo.toString().split("\t",-1).length);
//							System.out.println( "itemInfo ************************************* " + itemInfo.toString().split("\t",-1).length);
//							System.out.println( "itemPromotionAppliedInfo ************************************* " + itemPromotionAppliedInfo.toString().split("\t",-1).length);
//							System.out.println( "orderPromotionsInfo ************************************* " + orderPromotionsInfo.toString().split("\t",-1).length);
//							System.out.println( "orderOfferInfo ************************************* " + orderOfferInfo.toString().split("\t",-1).length);
//							for(int i=1;i<=5;i++){
//								System.out.println( "tendersInfoMap ************************************* " + tendersInfoMap.get(i).toString().split("\t",-1).length);
//								
//							
//							}
//							System.out.println( "orderPOSTimingsInfo ************************************* " + orderPOSTimingsInfo.toString().split("\t",-1).length);
//							System.out.println( "orderCustomInfoInfo ************************************* " + orderCustomInfoInfo.toString().split("\t",-1).length);
							
							context.getCounter("COUNT","POS_TRN_ITM").increment(1);
							processItem(eleItem.getChildNodes(),context);
							
							
							
						}
					}
				}
				
				
				
			}
			
		} catch (Exception ex) {
			System.err.println("Error occured in OffersReportFormatMapper.processItem:");
			ex.printStackTrace(System.err);
			System.exit(8);
		}
		
	}

	private void processItemOffers(NodeList nlItemOffers,
			                       Context context) {

		Element eleItemOffers;
		String promoId = "";
	
	    isItemOfferFl = false;
		isPromoAppledNode = false;
		
		try {
			itemPromotionAppliedInfo.setLength(0);
			
			if (nlItemOffers != null && nlItemOffers.getLength() > 0 ) {
				for (int idxItemOffers=0; idxItemOffers < nlItemOffers.getLength(); idxItemOffers++ ) {
					if ( nlItemOffers.item(idxItemOffers).getNodeType() == Node.ELEMENT_NODE ) {
						eleItemOffers = (Element)nlItemOffers.item(idxItemOffers);
						
						isItemOfferFl = false;
						isPromoAppledNode = false; 
						
						if ( eleItemOffers.getNodeName().equals("PromotionApplied") ) {
							promoId = getValue(eleItemOffers,"promotionId");
							if ( promoId.length() > 0 && promoIdMap.containsKey(promoId) ) {
								isItemOfferFl = true;
								isPromoAppledNode = true;
							}
							} else if ( eleItemOffers.getNodeName().equals("Offers") ) {
								isItemOfferFl = true;
							}
							
						if ( isItemOfferFl ) { 
							
							itmOffrAppdFl = "1";
							itmOfferAppdFoundFl = true;
							
							itemPromotionAppliedInfo.append(promoId).append(SEPARATOR_CHARACTER);//promotionId
							itemPromotionAppliedInfo.append(eleItemOffers.getAttribute("promotionCounter")).append(SEPARATOR_CHARACTER);
							itemPromotionAppliedInfo.append(eleItemOffers.getAttribute("eligible")).append(SEPARATOR_CHARACTER);
							itemPromotionAppliedInfo.append(eleItemOffers.getAttribute("originalPrice")).append(SEPARATOR_CHARACTER);
							itemPromotionAppliedInfo.append(eleItemOffers.getAttribute("discountAmount")).append(SEPARATOR_CHARACTER);
							itemPromotionAppliedInfo.append(eleItemOffers.getAttribute("discountType")).append(SEPARATOR_CHARACTER);
							itemPromotionAppliedInfo.append(eleItemOffers.getAttribute("originalItemPromoQty")).append(SEPARATOR_CHARACTER);
							itemPromotionAppliedInfo.append(eleItemOffers.getAttribute("originalProductCode"));
							break;//assuming only one promotionApplied for a given item
						} 
						
							
					}
				}
			}
			if(!isPromoAppledNode){
				itemPromotionAppliedInfo.append("").append(SEPARATOR_CHARACTER);//promotionId
				itemPromotionAppliedInfo.append("").append(SEPARATOR_CHARACTER);
				itemPromotionAppliedInfo.append("").append(SEPARATOR_CHARACTER);
				itemPromotionAppliedInfo.append("").append(SEPARATOR_CHARACTER);
				itemPromotionAppliedInfo.append("").append(SEPARATOR_CHARACTER);
				itemPromotionAppliedInfo.append("").append(SEPARATOR_CHARACTER);
				itemPromotionAppliedInfo.append("").append(SEPARATOR_CHARACTER);
				itemPromotionAppliedInfo.append("");
			}
		} catch (Exception ex) {
			System.err.println("Error occured in OffersReportFormatMapper.processItemOffers:");
			ex.printStackTrace(System.err);
			System.exit(8);
		}
		
	}
	
	private void getOrderDataDetailedSos(String xmlText
                                        ,Context context) {


		Element eleRoot;

		try {
			strReader  = new StringReader(xmlText);
			xmlSource = new InputSource(strReader);
			doc = docBuilder.parse(xmlSource);

			eleRoot = (Element) doc.getFirstChild();

			if ( eleRoot.getNodeName().equals("DetailedSOS") ) {
				posRestId = eleRoot.getAttribute("gdwLgcyLclRfrDefCd");

				processStoreTotals(eleRoot.getChildNodes(),context);
			}
		} catch (Exception ex) {
			System.err.println("Error occured in OffersReportFormatMapper.getOrderDataDetailedSos:");
			ex.printStackTrace(System.err);
			System.exit(8);
		}

	}
	
	private void processStoreTotals(NodeList nlStoreTotals
                                   ,Context context) {

		Element eleStoreTotals;

		if (nlStoreTotals != null && nlStoreTotals.getLength() > 0 ) {
			for (int idxStoreTotals=0; idxStoreTotals < nlStoreTotals.getLength(); idxStoreTotals++ ) {
				if ( nlStoreTotals.item(idxStoreTotals).getNodeType() == Node.ELEMENT_NODE ) {  
					eleStoreTotals = (Element)nlStoreTotals.item(idxStoreTotals);
					if ( eleStoreTotals.getNodeName().equals("StoreTotals") && (eleStoreTotals.getAttribute("productionNodeId").equalsIgnoreCase("DT") || eleStoreTotals.getAttribute("productionNodeId").equalsIgnoreCase("FC")) ) {

						processServiceTime(eleStoreTotals.getChildNodes(),context);
					}
				}
			}
		}

	}
	
	private void processServiceTime(NodeList nlServiceTime
                                   ,Context context) {

		Element eleServiceTime;

		untlTotKeyPrssScQt = "";
		untlStrInSysScQt = "";
		untlOrdRcllScQt = "";
		untlDrwrClseScQt = "";
		untlPaidScQt = "";
		untlSrvScQt = "";
		totOrdTmScQt = "";
		abovPsntTmTrgtFl = "";
		abovTotTmTrgtFl = "";
		abovTotMfyTrgtTmTmFl = "";
		abovTotFrntCterTrgtTmFl = "";
		abovTotDrvTrgtTmFl = "";
		abov50ScFl = "";
		bel25ScFl = "";
		heldTmScQt = "";
		ordHeldFl = "";

		try {
			
			if (nlServiceTime != null && nlServiceTime.getLength() > 0 ) {
				for (int idxServiceTime=0; idxServiceTime < nlServiceTime.getLength(); idxServiceTime++ ) {
					if ( nlServiceTime.item(idxServiceTime).getNodeType() == Node.ELEMENT_NODE ) {  
						eleServiceTime = (Element)nlServiceTime.item(idxServiceTime);
						if ( eleServiceTime.getNodeName().equals("ServiceTime") ) {
							
							untlTotKeyPrssScQt = "";
							untlStrInSysScQt = "";
							untlOrdRcllScQt = "";
							untlDrwrClseScQt = "";
							untlPaidScQt = "";
							untlSrvScQt = "";
							totOrdTmScQt = "";
							abovPsntTmTrgtFl = "";
							abovTotTmTrgtFl = "";
							abovTotMfyTrgtTmTmFl = "";
							abovTotFrntCterTrgtTmFl = "";
							abovTotDrvTrgtTmFl = "";
							abov50ScFl = "";
							bel25ScFl = "";
							heldTmScQt = "";
							ordHeldFl = "";
							
							posOrdKey = eleServiceTime.getAttribute("orderKey");
							
							

							untlTotKeyPrssScQt = getScQt(eleServiceTime.getAttribute("untilTotal"));
							
//							System.out.println(" until Total  "+  eleServiceTime.getAttribute("untilTotal") + " untlTotKeyPrssScQt " + untlTotKeyPrssScQt);
							
							untlStrInSysScQt = getScQt(eleServiceTime.getAttribute("untilStore"));
							untlOrdRcllScQt = getScQt(eleServiceTime.getAttribute("untilRecall"));
							untlDrwrClseScQt = getScQt(eleServiceTime.getAttribute("untilCloseDrawer"));
							untlPaidScQt = getScQt(eleServiceTime.getAttribute("untilPay"));
							untlSrvScQt = getScQt(eleServiceTime.getAttribute("untilServe"));
							totOrdTmScQt = getScQt(eleServiceTime.getAttribute("totalTime"));
							
							abovPsntTmTrgtFl = getValue(eleServiceTime,"tcOverPresentationPreset");
							abovTotTmTrgtFl = getValue(eleServiceTime,"tcOverTotalPreset");
							abovTotMfyTrgtTmTmFl = getValue(eleServiceTime,"tcOverTotalMFY");
							abovTotFrntCterTrgtTmFl = getValue(eleServiceTime,"tcOverTotalFC");
							abovTotDrvTrgtTmFl = getValue(eleServiceTime,"tcOverTotalDT");
							
							processProductionTime(eleServiceTime.getChildNodes(),context);

							outputKey.setLength(0);						
							outputKey.append(REC_POSTRN);
							outputKey.append(terrCd);
							outputKey.append(posBusnDt);
							outputKey.append(posOrdKey);
							outputKey.append(SEPARATOR_CHARACTER);
							outputKey.append(mcdGbalLcatIdNu);
							
					    	mapKey.clear();
							mapKey.set(outputKey.toString());

							outputTextValue.setLength(0);
							outputTextValue.append("2");
							outputTextValue.append(SEPARATOR_CHARACTER);
							outputTextValue.append(untlTotKeyPrssScQt); 
							outputTextValue.append(SEPARATOR_CHARACTER);
							outputTextValue.append(untlStrInSysScQt); 
							outputTextValue.append(SEPARATOR_CHARACTER);
							outputTextValue.append(untlOrdRcllScQt); 
							outputTextValue.append(SEPARATOR_CHARACTER);
							outputTextValue.append(untlDrwrClseScQt);
							outputTextValue.append(SEPARATOR_CHARACTER);
							outputTextValue.append(untlPaidScQt);
							outputTextValue.append(SEPARATOR_CHARACTER);
							outputTextValue.append(untlSrvScQt); 
							outputTextValue.append(SEPARATOR_CHARACTER);
							outputTextValue.append(totOrdTmScQt);
							outputTextValue.append(SEPARATOR_CHARACTER);
							outputTextValue.append(abovPsntTmTrgtFl);
							outputTextValue.append(SEPARATOR_CHARACTER);
							outputTextValue.append(abovTotTmTrgtFl);
							outputTextValue.append(SEPARATOR_CHARACTER);
							outputTextValue.append(abovTotMfyTrgtTmTmFl);
							outputTextValue.append(SEPARATOR_CHARACTER);
							outputTextValue.append(abovTotFrntCterTrgtTmFl);
							outputTextValue.append(SEPARATOR_CHARACTER);
							outputTextValue.append(abovTotDrvTrgtTmFl);
							outputTextValue.append(SEPARATOR_CHARACTER);
							outputTextValue.append(abov50ScFl);
							outputTextValue.append(SEPARATOR_CHARACTER);
							outputTextValue.append(bel25ScFl);
							outputTextValue.append(SEPARATOR_CHARACTER);
							outputTextValue.append(heldTmScQt);
							outputTextValue.append(SEPARATOR_CHARACTER);
							outputTextValue.append(ordHeldFl);
							
							mapValue.clear();
							mapValue.set(outputTextValue.toString());
							
//							System.out.println(" sos " + outputTextValue.toString());
							
//							context.write(mapKey, mapValue);	
//							context.getCounter("COUNT","POS_TRN_DetailedSOS").increment(1);

							outputKey.setLength(0);						
							outputKey.append(REC_POSTRNITM);
							outputKey.append(terrCd);
							outputKey.append(posBusnDt);
							outputKey.append(posOrdKey);
							outputKey.append(SEPARATOR_CHARACTER);
							outputKey.append(mcdGbalLcatIdNu);
							
					    	mapKey.clear();
							mapKey.set(outputKey.toString());
							context.write(mapKey, mapValue);	
							context.getCounter("COUNT","POS_TRN_ITM_DetailedSOS").increment(1);

						}
					}
				}
			}
		} catch (Exception ex) {
			System.err.println("Error occured in OffersReportFormatMapper.processServiceTime:");
			ex.printStackTrace(System.err);
			System.exit(8);
		}
	}
	
	private void processProductionTime(NodeList nlProductionTime
                                      ,Context context) {

		Element eleProductionTime;

		if (nlProductionTime != null && nlProductionTime.getLength() > 0 ) {
			for (int idxProductionTime=0; idxProductionTime < nlProductionTime.getLength(); idxProductionTime++ ) {
				if ( nlProductionTime.item(idxProductionTime).getNodeType() == Node.ELEMENT_NODE ) {  
					eleProductionTime = (Element)nlProductionTime.item(idxProductionTime);
					if ( eleProductionTime.getNodeName().equals("ProductionTime") ) {
						abov50ScFl = getValue(eleProductionTime,"tcOver50");
						bel25ScFl = getValue(eleProductionTime,"tcUnder25");
						heldTmScQt = getScQt(eleProductionTime.getAttribute("heldTime"));
						ordHeldFl = getValue(eleProductionTime,"tcHeld");
					}
				}
			}
		}

	}

	private void getDaypart() {
		
		String dayPartKey;
		String hour;
		String minute;
		int minuteInt;
		
		dyptIdNu = "";
		dyptDs = "";
		dyOfCalWkDs = "";
		
		if(posTrnStrtTs == null || posTrnStrtTs.isEmpty()) return;
		
		if(posTrnStrtTs.trim().length() < 4){
			System.out.println(" posTrnStrtTs " +posTrnStrtTs  + "XXX" );
		}
		
		cal.set(Integer.parseInt(posTrnStrtTs.substring(0, 4)), Integer.parseInt(posTrnStrtTs.substring(5, 7))-1, Integer.parseInt(posTrnStrtTs.substring(8, 10)));
		hour = posTrnStrtTs.substring(11, 13);
		hrIdNu = String.valueOf(hour);
		minuteInt = Integer.parseInt(posTrnStrtTs.substring(14, 16));
		
		if ( minuteInt < 15 ) {
			minute = "00";
		} else if ( minuteInt < 30 ) {
			minute = "15";
		} else if ( minuteInt < 45 ) {
			minute = "30";
		} else {
			minute = "45";
		}
		
		switch ( cal.get(Calendar.DAY_OF_WEEK) ) {
			case 1: 
				dyOfCalWkDs = "Sunday";
			    break;
			
			case 2: 
				dyOfCalWkDs = "Monday";
			    break;
			
			case 3: 
				dyOfCalWkDs = "Tuesday";
			    break;
			
			case 4: 
				dyOfCalWkDs = "Wednesday";
			    break;
			
			case 5: 
				dyOfCalWkDs = "Thursday";
			    break;
			
			case 6: 
				dyOfCalWkDs = "Friday";
			    break;
			
			case 7: 
				dyOfCalWkDs = "Saturday";
			    break;
			
			default: 
				dyOfCalWkDs = "**";
			    break;
		}

		dayPartKey = terrCd + SEPARATOR_CHARACTER + String.valueOf(cal.get(Calendar.DAY_OF_WEEK)) + SEPARATOR_CHARACTER + hour + minute;
		
		if ( dayPartMap.containsKey(dayPartKey) ) {
			parts = dayPartMap.get(dayPartKey).split("\t",-1);
			dyptIdNu = parts[0];
			dyptDs = parts[1];
		}
		
	}
	
	private String formatAsTs(String in) {
		
		String retTs = "";
		
		if ( in.length() >= 14 ) {
			retTs = in.substring(0, 4) + "-" + in.substring(4, 6) + "-" + in.substring(6, 8) + " " + in.substring(8, 10) + ":" + in.substring(10, 12) + ":" + in.substring(12, 14);
		}

		return(retTs);
	}
	
	private String formatDateAsTsDtOnly(String in) {

		String retTs = "";
		
		if ( in.length() >= 8 ) {
			retTs = in.substring(0, 4) + "-" + in.substring(4, 6) + "-" + in.substring(6, 8);
		}

		return(retTs);
		
	}
	
	private String formatDateAsTs(String in) {

		String retTs = "";
		
		if ( in.length() >= 8 ) {
			retTs = in.substring(0, 4) + "-" + in.substring(4, 6) + "-" + in.substring(6, 8) + " 00:00:00";
		}

		return(retTs);
		
	}
	
	private String formatTimeAsTs(String in
			                     ,String inDate) {

		String retTs = "";
		
		if ( in.length() >= 6 ) {
			retTs = inDate.substring(0, 10) + " " + in.substring(0, 2) + ":" + in.substring(3, 4) + ":" + in.substring(4, 6);
		}

		return(retTs);
		
	}
	
	private String getScQt(String time) {
		
		String retScQt = "";
		
		Double tmpScQt;
		
		try {
			tmpScQt = Double.parseDouble(time) / 1000.0;
			retScQt = String.valueOf(tmpScQt);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return(retScQt);
		
	}
	
	private String getValue(Element ele
			               ,String attribute) {
		
		String retValue = "";

		try {
			retValue = ele.getAttribute(attribute);
			
		} catch (Exception ex) {
		}
		
		return(retValue.trim());
	}
	
}