package org.hadatac.console.controllers.metadataacquisition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import play.mvc.Controller;
import play.mvc.Result;

import org.hadatac.console.views.html.metadataacquisition.*;
import org.hadatac.metadata.loader.ValueCellProcessing;
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
		String indicatorQuery = ""; 
		indicatorQuery += NameSpaces.getInstance().printSparqlNameSpaceList();
		indicatorQuery += "SELECT ?studyIndicator ?label ?comment WHERE { "
				+ "?studyIndicator rdfs:subClassOf chear:StudyIndicator . "
				+ "?studyIndicator rdfs:label ?label . "
				+ "?studyIndicator rdfs:comment ?comment . "
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
				indicatorMap.put(soln.get("studyIndicator").toString(), indicatorLabel);		
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
			indvIndicatorQuery += "SELECT DISTINCT ?studyUri ?label ?uri WHERE { "
					+ "?schemaUri hasco:isSchemaOf " + study_uri + " . "
					+ "?schemaAttribute hasneto:partOfSchema ?schemaUri . "
					+ "?schemaAttribute hasneto:hasAttribute ?uri . " 
					+ "?uri rdfs:subClassOf* <" + parentIndicatorUri + "> . "
					+ "?uri rdfs:label ?label . "
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
					System.out.println("Solution: " + soln);
					indvIndicatorList.add(soln.get("label").toString());
				}
				indicatorValues.put(entry.getValue().toString(), indvIndicatorList);
			} catch (QueryExceptionHTTP e) {
				e.printStackTrace();
			}
		}
		
		return indicatorValues;
	}
	
	public static Map<String, String> findStudyIndicatorsUri(String study_uri) {
		String indicatorQuery = ""; 
		indicatorQuery += NameSpaces.getInstance().printSparqlNameSpaceList();
		indicatorQuery += "SELECT ?studyIndicator ?label ?comment WHERE { "
				+ "?studyIndicator rdfs:subClassOf chear:StudyIndicator . "
				+ "?studyIndicator rdfs:label ?label . "
				+ "?studyIndicator rdfs:comment ?comment . "
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
				indicatorMap.put(soln.get("studyIndicator").toString(),indicatorLabel);		
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
			indvIndicatorQuery += "SELECT DISTINCT ?studyUri ?label ?uri WHERE { "
					+ "?schemaUri hasco:isSchemaOf " + study_uri + " . "
					+ "?schemaAttribute hasneto:partOfSchema ?schemaUri . "
					+ "?schemaAttribute hasneto:hasAttribute ?uri . "
					+ "?uri rdfs:subClassOf* <" + parentIndicatorUri + "> . " 
					+ "?uri rdfs:label ?label . "
					+ "}";
			try {
				QueryExecution qexecIndvInd = QueryExecutionFactory.sparqlService(
						Collections.getCollectionsName(Collections.METADATA_SPARQL), indvIndicatorQuery);
				ResultSet indvIndResults = qexecIndvInd.execSelect();
				ResultSetRewindable resultsrwIndvInd = ResultSetFactory.copyResults(indvIndResults);
				qexecIndvInd.close();
				
				while (resultsrwIndvInd.hasNext()) {
					QuerySolution soln = resultsrwIndvInd.next();
					System.out.println("Solution: " + soln);
					indicatorUris.put(soln.get("label").toString(), soln.get("uri").toString());
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
				+ "?subUri rdfs:subClassOf hasco:Study . "
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
				+ "}" ;
		Map<String, List<String>> poResult = new HashMap<String, List<String>>();
		List<String> values = new ArrayList<String>();
		try {
			Query basicQuery = QueryFactory.create(basicQueryString);
			QueryExecution qexec = QueryExecutionFactory.sparqlService(
					Collections.getCollectionsName(Collections.METADATA_SPARQL), basicQuery);
			ResultSet results = qexec.execSelect();
			ResultSetRewindable resultsrw = ResultSetFactory.copyResults(results);
			qexec.close();
			
			ValueCellProcessing cellProc = new ValueCellProcessing();
			while (resultsrw.hasNext()) {
				QuerySolution soln = resultsrw.next();
				values = new ArrayList<String>();
				values.add("Label: " + soln.get("studyLabel").toString());
				values.add("Title: " + soln.get("studyDef").toString());
				values.add("Project: " + cellProc.replaceNameSpaceEx(soln.get("proj").toString()));
				values.add("Comment: " + soln.get("studyComment").toString());
				values.add("Agent(s): " + soln.get("agentName").toString());
				values.add("Institution: " + soln.get("institutionName").toString());
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
    	subjectQueryString += "SELECT ?subjectUri ?subjectType ?subjectLabel ?cohortLabel ?studyLabel WHERE { "
    			+ "?subjectUri hasco:isSubjectOf* ?cohort . "
    			+ "?cohort hasco:isCohortOf " + study_uri + " . "
    			+ study_uri + " rdfs:label ?studyLabel . "
    			+ "cohort rdfs:label ?cohortLabel . "
    			+ "OPTIONAL { ?subjectUri rdfs:label ?subjectLabel } . "
    			+ "OPTIONAL { ?subjectUri a ?subjectType } . "
    			+ "}";		
    	Map<String, List<String>> subjectResult = new HashMap<String, List<String>>();
		List<String> values = new ArrayList<String>();
		try {
			Query subjectQuery = QueryFactory.create(subjectQueryString);
			QueryExecution qexec2 = QueryExecutionFactory.sparqlService(Collections.getCollectionsName(Collections.METADATA_SPARQL), subjectQuery);
			ResultSet results = qexec2.execSelect();
			ResultSetRewindable resultsrw = ResultSetFactory.copyResults(results);
			qexec2.close();
			
			ValueCellProcessing cellProc = new ValueCellProcessing();
			while (resultsrw.hasNext()) {
				QuerySolution soln = resultsrw.next();
				values = new ArrayList<String>();
				values.add("Label: " + soln.get("subjectLabel").toString());
				values.add("Type: " + cellProc.replaceNameSpaceEx(soln.get("subjectType").toString()));
				values.add("Cohort: " + soln.get("cohortLabel").toString());
				values.add("Study: " + soln.get("studyLabel").toString());
				subjectResult.put(cellProc.replaceNameSpaceEx(soln.get("subjectUri").toString()) ,values);		
			}
		} catch (QueryExceptionHTTP e) {
			e.printStackTrace();
		}
		
		return subjectResult;
	}

	public static String findUser() {
		String results = null;
	    final SysUser user = AuthApplication.getLocalUser(session());
	    if(null == user){
	        results = null;
	    }
	    else{
	    	results = UserManagement.getUriByEmail(user.email);
	    }
	    System.out.println("This is the current user's uri:" + results);
	    
	    return results;
	}
	
	@Restrict(@Group(AuthApplication.DATA_OWNER_ROLE))
    public static Result index(String study_uri) {
		Map<String, List<String>> indicatorValues = findStudyIndicators(study_uri);
		Map<String, String> indicatorUris = findStudyIndicatorsUri(study_uri);
		
		Map<String, List<String>> poResult = findBasic(study_uri);
		Map<String, List<String>> subjectResult = findSubject(study_uri);
		Map<String, String> showValues = new HashMap<String, String>();
		showValues.put("study", study_uri);
		showValues.put("user", findUser());
        
    	return ok(viewStudy.render(poResult, subjectResult, indicatorValues, indicatorUris, showValues));   
    }

	@Restrict(@Group(AuthApplication.DATA_OWNER_ROLE))
    public static Result postIndex(String study_uri) {
		return index(study_uri);
	}
}
