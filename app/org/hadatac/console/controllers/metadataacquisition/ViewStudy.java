package org.hadatac.console.controllers.metadataacquisition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import play.mvc.Controller;
import play.mvc.Result;

import org.hadatac.console.views.html.metadataacquisition.*;
import org.hadatac.entity.pojo.Measurement;
import org.hadatac.metadata.loader.URIUtils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;
import org.hadatac.console.controllers.AuthApplication;
import org.hadatac.console.controllers.triplestore.UserManagement;
import org.hadatac.console.models.SysUser;
import org.hadatac.utils.Collections;
import org.hadatac.utils.NameSpaces;

import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;

public class ViewStudy extends Controller {
	
	public static Map<String, List<String>> findStudyIndicators(String study_uri) {
		
		String das_uri = study_uri.replace("STD", "DAS");
		String indicatorQuery = "";
		indicatorQuery += NameSpaces.getInstance().printSparqlNameSpaceList();
		indicatorQuery += "SELECT ?subIndicator ?label ?comment WHERE { "
				+ "?subIndicator	rdfs:subClassOf	<http://hadatac.org/ont/hasco/Indicator> . "
				+ "?subIndicator	rdfs:label ?label . "
				+ "?subIndicator	rdfs:comment ?comment . "
				+ "}";
		Map<String, String> indicatorMap = new HashMap<String, String>();
		String indicatorLabel = "";
		try {
			QueryExecution qexecInd = QueryExecutionFactory.sparqlService(
					Collections.getCollectionsName(Collections.METADATA_SPARQL), indicatorQuery);
			ResultSet indicatorResults = qexecInd.execSelect();
			ResultSetRewindable resultsrwIndc = ResultSetFactory.copyResults(indicatorResults);
			qexecInd.close();
			while (resultsrwIndc.hasNext()) {
				QuerySolution soln = resultsrwIndc.next();
				indicatorLabel = soln.get("label").toString();
				indicatorMap.put(soln.get("subIndicator").toString(), indicatorLabel);		
			}
		} catch (QueryExceptionHTTP e) {
			e.printStackTrace();
		}
		Map<String, String> indicatorMapSorted = new TreeMap<String, String>(indicatorMap);
		Map<String, List<String>> indicatorValues = new HashMap<String, List<String>>();
		
		for(Map.Entry<String, String> entry : indicatorMapSorted.entrySet()){
		    String parentIndicatorUri = entry.getKey();
			String indvIndicatorQuery = "";
			indvIndicatorQuery += NameSpaces.getInstance().printSparqlNameSpaceList();
			indvIndicatorQuery += "SELECT DISTINCT ?label ?answer WHERE { "
					+ "?answer rdf:type	<http://hadatac.org/ont/hasco/DASchemaAttribute> . "
					+ "?answer <http://hadatac.org/ont/hasco/partOfSchema> " + das_uri + " . "
					+ "<http://hadatac.org/ont/hasco/DASchemaAttribute> rdfs:subClassOf	<" + parentIndicatorUri + "> . "
					+ "?answer rdfs:label ?label . "
					+ "}";
			
			try {
				QueryExecution qexecIndvInd = QueryExecutionFactory.sparqlService(
						Collections.getCollectionsName(Collections.METADATA_SPARQL), indvIndicatorQuery);
				ResultSet indvIndResults = qexecIndvInd.execSelect();
				ResultSetRewindable resultsrwIndvInd = ResultSetFactory.copyResults(indvIndResults);
				qexecIndvInd.close();
				List<String> indvIndicatorList = new ArrayList<String>();
				while (resultsrwIndvInd.hasNext()) {
					QuerySolution soln = resultsrwIndvInd.next();
					System.out.println("ViewStudy Solution: " + soln);
					indvIndicatorList.add(soln.get("label").toString());
				}
				indicatorValues.put(parentIndicatorUri, indvIndicatorList);
				System.out.println(indvIndicatorList.toString());
			} catch (QueryExceptionHTTP e) {
				e.printStackTrace();
			}
		}
		
		return indicatorValues;
	}
	
	public static Map<String, String> findStudyIndicatorsUri(String study_uri) {
		String indicatorQuery = ""; 
		indicatorQuery += NameSpaces.getInstance().printSparqlNameSpaceList();
		indicatorQuery += "SELECT ?subIndicator ?label ?comment WHERE { "
				+ "?subIndicator	rdfs:subClassOf	<http://hadatac.org/ont/hasco/Indicator> . "
				+ "?subIndicator	rdfs:label ?label . "
				+ "?subIndicator	rdfs:comment ?comment . "
				+ "}";
		Map<String, String> indicatorMap = new HashMap<String, String>();
		String indicatorLabel = "";
		try {
			QueryExecution qexecInd = QueryExecutionFactory.sparqlService(
					Collections.getCollectionsName(Collections.METADATA_SPARQL), indicatorQuery);
			ResultSet indicatorResults = qexecInd.execSelect();
			ResultSetRewindable resultsrwIndc = ResultSetFactory.copyResults(indicatorResults);
			qexecInd.close();
			while (resultsrwIndc.hasNext()) {
				QuerySolution soln = resultsrwIndc.next();
				indicatorLabel = soln.get("label").toString();
				indicatorMap.put(soln.get("subIndicator").toString(),indicatorLabel);		
			}
		} catch (QueryExceptionHTTP e) {
			e.printStackTrace();
		}
		Map<String, String> indicatorMapSorted = new TreeMap<String, String>(indicatorMap);
		Map<String, String> indicatorUris = new HashMap<String, String>();
		
		for(Map.Entry<String, String> entry : indicatorMapSorted.entrySet()){
			String parentIndicatorUri = entry.getKey();
			String indvIndicatorQuery = "";
			indvIndicatorQuery += NameSpaces.getInstance().printSparqlNameSpaceList();
			indvIndicatorQuery += "SELECT DISTINCT ?label ?answer WHERE { "
					+ "?answer rdf:type	<http://hadatac.org/ont/hasco/DASchemaAttribute> . "
					+ "<http://hadatac.org/ont/hasco/DASchemaAttribute> rdfs:subClassOf	<" + parentIndicatorUri + "> . "
					+ "?answer rdfs:label ?label . "
					+ "}";
			
			try {
				QueryExecution qexecIndvInd = QueryExecutionFactory.sparqlService(
						Collections.getCollectionsName(Collections.METADATA_SPARQL), indvIndicatorQuery);
				ResultSet indvIndResults = qexecIndvInd.execSelect();
				ResultSetRewindable resultsrwIndvInd = ResultSetFactory.copyResults(indvIndResults);
				qexecIndvInd.close();
				
				while (resultsrwIndvInd.hasNext()) {
					QuerySolution soln = resultsrwIndvInd.next();
//					System.out.println("ViewStudy Solution: " + soln);
					indicatorUris.put(soln.get("label").toString(), soln.get("answer").toString());
					System.out.println(indicatorUris.keySet().toString());					
				}
			} catch (QueryExceptionHTTP e) {
				e.printStackTrace();
			}
		}
		
		return indicatorUris;
	}
	
	public static Map<String, List<String>> findBasic(String study_uri) {
		String basicQueryString = "";
		basicQueryString += NameSpaces.getInstance().printSparqlNameSpaceList();
		basicQueryString += "SELECT ?studyUri ?studyLabel ?proj ?studyDef ?studyComment ?agentName ?institutionName WHERE { "
				+ "?subUri rdfs:subClassOf* hasco:Study . "
				+ "?studyUri a ?subUri . "
				+ "?studyUri rdfs:label ?studyLabel . "
				+ "FILTER ( ?studyUri = " + study_uri + " ) . "
				+ "OPTIONAL { ?studyUri hasco:hasProject ?proj} . "
				+ "OPTIONAL { ?studyUri skos:definition ?studyDef } . "
				+ "OPTIONAL { ?studyUri rdfs:comment ?studyComment } . "
				+ "OPTIONAL { ?studyUri hasco:hasAgent ?agent . "
				+ "			  ?agent foaf:name ?agentName } . "
				+ "OPTIONAL { ?studyUri hasco:hasInstitution ?institution . "
				+ "			  ?institution foaf:name ?institutionName } . "
				+ "} " ;
		Map<String, List<String>> poResult = new HashMap<String, List<String>>();
		List<String> values = new ArrayList<String>();
		try {
			Query basicQuery = QueryFactory.create(basicQueryString);
			QueryExecution qexec = QueryExecutionFactory.sparqlService(
					Collections.getCollectionsName(Collections.METADATA_SPARQL), basicQuery);
			ResultSet results = qexec.execSelect();
			ResultSetRewindable resultsrw = ResultSetFactory.copyResults(results);
			qexec.close();
			
			while (resultsrw.hasNext()) {
				QuerySolution soln = resultsrw.next();
				values = new ArrayList<String>();
				if(soln.contains("studyLabel")){
					values.add("Label: " + soln.get("studyLabel").toString());
				}
				if(soln.contains("studyDef")){
					values.add("Title: " + soln.get("studyDef").toString());
				}
				if(soln.contains("proj")){
					values.add("Project: " + URIUtils.replaceNameSpaceEx(soln.get("proj").toString()));
				}
				if(soln.contains("studyComment")){
					values.add("Comment: " + soln.get("studyComment").toString());
				}
				if(soln.contains("agentName")){
					values.add("Agent(s): " + soln.get("agentName").toString());
				}
				if(soln.contains("institutionName")){
					values.add("Institution: " + soln.get("institutionName").toString());
				}
				poResult.put(study_uri, values);
			}
		} catch (QueryExceptionHTTP e) {
			e.printStackTrace();
		}
		
		return poResult;
	}
	
	public static Map<String, List<String>> findSubject(String study_uri) {
		String subjectQueryString = "";
    	        subjectQueryString += NameSpaces.getInstance().printSparqlNameSpaceList(); 
    	        subjectQueryString += "SELECT ?subjectUri ?subjectType ?subjectLabel ?cohortLabel ?studyLabel WHERE { \n"
    			+ "?subjectUri hasco:isMemberOf ?cohort . \n"
    			+ "?cohort hasco:isMemberOf " + study_uri + " . \n"
    			+ "?subjectUri rdf:type	sio:Human . \n"
    			+ "?cohort rdfs:label ?cohortLabel . \n"
    			+ "OPTIONAL { ?subjectUri rdfs:label ?subjectLabel } . \n"
    			+ "OPTIONAL { ?subjectUri a ?subjectType } . \n"
    			+ "} \n"
                + "ORDER BY ?subjectUri \n";
    	Map<String, List<String>> subjectResult = new HashMap<String, List<String>>();
		List<String> values = new ArrayList<String>();
		try {
			Query subjectQuery = QueryFactory.create(subjectQueryString);
			QueryExecution qexec2 = QueryExecutionFactory.sparqlService(
					Collections.getCollectionsName(Collections.METADATA_SPARQL), subjectQuery);
			ResultSet results = qexec2.execSelect();
			ResultSetRewindable resultsrw = ResultSetFactory.copyResults(results);
			qexec2.close();
			
			while (resultsrw.hasNext()) {
				QuerySolution soln = resultsrw.next();
				values = new ArrayList<String>();
				values.add("Label: " + soln.get("subjectLabel").toString());
				subjectResult.put(URIUtils.replaceNameSpaceEx(soln.get("subjectUri").toString()) ,values);
			}
		} catch (QueryExceptionHTTP e) {
			e.printStackTrace();
		}
		
		return subjectResult;
	}
	
	public static List<String> findSampleCollection(String study_uri) {
		String scQueryString = "";
		scQueryString += NameSpaces.getInstance().printSparqlNameSpaceList(); 
		scQueryString += "SELECT ?sc ?si WHERE { "
    			+ "?sc	<http://hadatac.org/ont/hasco/isMemberOf> " + study_uri + " . "
    			+ "?sc	rdf:type	<http://hadatac.org/ont/hasco/SampleCollection> . "
    			+ "?si	<http://hadatac.org/ont/hasco/isMemberOf>	?sc . "
    			+ "}";
		List<String> values = new ArrayList<String>();
		try {
			Query sampleQuery = QueryFactory.create(scQueryString);
			QueryExecution qexec3 = QueryExecutionFactory.sparqlService(
					Collections.getCollectionsName(Collections.METADATA_SPARQL), sampleQuery);
			ResultSet results = qexec3.execSelect();
			ResultSetRewindable resultsrw = ResultSetFactory.copyResults(results);
			qexec3.close();
			
			while (resultsrw.hasNext()) {
				QuerySolution soln = resultsrw.next();
				values.add(URIUtils.replaceNameSpaceEx(soln.get("si").toString()));	
			}
		} catch (QueryExceptionHTTP e) {
			e.printStackTrace();
		}
		
		return values;
	}
	
	@Restrict(@Group(AuthApplication.DATA_OWNER_ROLE))
    public Result index(String study_uri) {
		Map<String, List<String>> indicatorValues = findStudyIndicators(study_uri);
		Map<String, String> indicatorUris = findStudyIndicatorsUri(study_uri);
		
		Map<String, List<String>> poResult = findBasic(study_uri);
		Map<String, List<String>> subjectResult = findSubject(study_uri);
		List<String> scResult = findSampleCollection(study_uri);
		Map<String, String> showValues = new HashMap<String, String>();
		showValues.put("study", study_uri);
		showValues.put("user", UserManagement.getCurrentUserUri());
        
    	return ok(viewStudy.render(poResult, subjectResult, indicatorValues, indicatorUris, showValues, scResult));
    }

	@Restrict(@Group(AuthApplication.DATA_OWNER_ROLE))
    public Result postIndex(String study_uri) {
		return index(study_uri);
	}
}
