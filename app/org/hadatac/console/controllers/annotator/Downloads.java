package org.hadatac.console.controllers.annotator;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Properties;

import play.Play;
import play.mvc.*;
import play.mvc.Http.*;
import play.mvc.Result;

import org.apache.commons.io.FileUtils;
import org.hadatac.console.models.CSVAnnotationHandler;
import org.hadatac.console.views.html.annotator.*;
import org.hadatac.utils.NameSpaces;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Downloads extends Controller {

	/* 
	 *  Download operations
	 */
	public static final String OPER_UPLOAD     = "Upload CCSV";
	public static final String OPER_CCSV       = "Download CCSV";
	public static final String OPER_PREAMBLE   = "Download Preamble";
	public static final String OPER_FINISH     = "Finish";
	
	/*
	 *  Preamble fragments
	 */

	public static final String FRAG_START_PREAMBLE          = "== START-PREAMBLE ==\n";
    public static final String FRAG_END_PREAMBLE            = "== END-PREAMBLE ==\n";

    public static final String FRAG_KB_PART1                = "<kb> a hadatac:KnowledgeBase; hadatac:hasHost \"";
    public static final String FRAG_KB_PART2                = "\"^^xsd:anyURI . \n\n";

    public static final String FRAG_DATASET                 = " a vstoi:Dataset; prov:wasGeneratedBy <";
    public static final String FRAG_HAS_MEASUREMENT_TYPE    = " hasMeasurementType ";
    public static final String FRAG_MT                      = "<mt";

    public static final String FRAG_MEASUREMENT_TYPE_PART1  = "> a hadatac:MeasurementType; hadatac:atColumn ";
    public static final String FRAG_MEASUREMENT_TYPE_PART2  = "; oboe:ofCharacteristic ";
    public static final String FRAG_MEASUREMENT_TYPE_PART3  = "; oboe:usesStandard ";

    public static Result postGenerate(String handler_json) {

    	String oper = "";
    	
    	RequestBody body = request().body();
    	if (body == null) {
            return ok(completeAnnotation.render("Error processing form: form appears to be empty."));       		    		
    	}
    	
    	String textBody = body.asText();
    	Properties p = new Properties();
    	try {
	    	p.load(new StringReader(textBody));
		} catch (Exception e) {
			e.printStackTrace();
            return ok(completeAnnotation.render("Error processing form: form appears to be empty."));       		
		}

    	System.out.println("Selection: " + p.getProperty("submitButton"));
    	if (p.getProperty("submitButton") != null)
    		oper = p.getProperty("submitButton");
    	
    	if (oper.equals(OPER_FINISH)) {
            return ok(completeAnnotation.render("Annotation operation finished."));       		
    	}
    	
    	NameSpaces ns = NameSpaces.getInstance();
    	String preamble = FRAG_START_PREAMBLE;
    	preamble += ns.printNameSpaceList();
    	preamble += "\n";

    	/* 
    	 * Insert KB
    	 */
    	
    	preamble += FRAG_KB_PART1;
    	preamble += Play.application().configuration().getString("hadatac.console.host") + "/hadatac/"; 
    	preamble += FRAG_KB_PART2;
    	
    	try {
			handler_json = URLDecoder.decode(handler_json, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
    	System.out.println(handler_json);
    	
    	ObjectMapper mapper = new ObjectMapper();    	
    	CSVAnnotationHandler handler = null;
    	try {
			handler = mapper.readValue(handler_json, CSVAnnotationHandler.class);

			  /* 
			   * Insert Data Set
			   */
			
			  preamble += "<DATASET URI>";
			  preamble += FRAG_DATASET;
			  preamble += "DATACOLLECTION_URI>; ";
			
			  int i = 0;
			  ArrayList<Integer> mt = new ArrayList<Integer>();
			  for (String str : handler.getFields()) {
		  		  //System.out.println(str);
	  		 	  //System.out.println("get " + i + "-characteristic: [" + p.getProperty(i + "-characteristic") + "]");
	  		 	  //System.out.println("get " + i + "-unit:           [" + p.getProperty(i + "-unit") + "]");
	  		 	  if ((p.getProperty(i + "-characteristic") != null) && 
	  		 		  (!p.getProperty(i + "-characteristic").equals("")) && 
		  		 	  (p.getProperty(i + "-unit") != null) && 
			  		  (!p.getProperty(i + "-unit").equals(""))) {
			  			  mt.add(i);
			  		  }
	  		 	  i++;
		      }
			  
			  preamble += FRAG_HAS_MEASUREMENT_TYPE;	
			  int aux = 0;
			  for (Integer mt_count : mt) {
				  preamble += FRAG_MT + aux++ + "> ";
			  }
			  preamble += ".\n\n";
			  
			  /*
			   * Insert measurement types
			   */
			  
			  aux = 0;
			  for (Integer mt_count : mt) {
				  preamble += FRAG_MT + aux;
				  preamble += FRAG_MEASUREMENT_TYPE_PART1;
				  preamble += mt_count;
				  preamble += FRAG_MEASUREMENT_TYPE_PART2;
				  preamble += p.getProperty(mt_count + "-characteristic"); 
				  preamble += FRAG_MEASUREMENT_TYPE_PART3;
				  preamble += p.getProperty(mt_count + "-unit"); 
				  preamble += " .\n";
			  }

			  if(textBody != null) {
			    //System.out.println("Got: [" + textBody + "]");
			  } else {
			    badRequest("Expecting text/plain request body");
			  }
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return ok (completeAnnotation.render("Error processing form. Please restart form."));
		} 
    	
    	preamble += FRAG_END_PREAMBLE;
    	
    	if (oper.equals(OPER_PREAMBLE)) {
    		return ok(preamble).as("text/turtle");
    	}
    	
    	if (oper.equals(OPER_CCSV)) {
		    File newFile = new File(handler.getDatasetName()); 
		    try {
				preamble += FileUtils.readFileToString(newFile, "UTF-8");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return ok (completeAnnotation.render("Error reading cached CSV file. Please restart form."));
			}
	        return ok(preamble).as("text/turtle");
    	}
    	
    	if (oper.equals(OPER_UPLOAD)) {
    		
    	}
    	
		return ok (completeAnnotation.render("Error processing form: unspecified download operation."));
    	
    }

}
