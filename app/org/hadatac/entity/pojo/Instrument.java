package org.hadatac.entity.pojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;
import org.hadatac.console.models.Facet;
import org.hadatac.console.models.FacetHandler;
import org.hadatac.utils.Collections;
import org.hadatac.utils.NameSpaces;

import com.typesafe.config.ConfigFactory;

public class Instrument extends HADatAcThing implements Comparable<Instrument> {

	private String serialNumber;
	
	public String getSerialNumber() {
		return serialNumber;
	}
	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}
	
	@Override
	public boolean equals(Object o) {
		if((o instanceof Instrument) && (((Instrument)o).getUri().equals(this.getUri()))) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return getUri().hashCode();
	}
	
	public Map<HADatAcThing, List<HADatAcThing>> getTargetFacets(
			Facet facet, FacetHandler facetHandler) {
		String valueConstraint = "";
		if (!facet.getFacetValuesByField("platform_uri_str").isEmpty()) {
			valueConstraint += " VALUES ?platformUri { " + stringify(
					facet.getFacetValuesByField("platform_uri_str"), true) + " } \n ";
		}
		
		String query = "";
		query += NameSpaces.getInstance().printSparqlNameSpaceList();
		query += "SELECT ?platformUri ?instrumentUri ?dataAcquisitionUri ?instrumentLabel ?dataAcquisitionLabel WHERE { \n"
				+ valueConstraint
				+ " ?dataAcquisitionUri hasco:hasDeployment ?deploymentUri . \n"
				+ " ?deploymentUri vstoi:hasPlatform ?platformUri . \n"
				+ " ?deploymentUri hasco:hasInstrument ?instrumentUri . \n"
				+ " ?instrumentUri rdfs:label ?instrumentLabel . \n"
				+ " ?dataAcquisitionUri rdfs:label ?dataAcquisitionLabel . \n"
				+ " } \n";

		//System.out.println("Instrument getTargetFacets query: " + query);
		
		facet.clearFieldValues("acquisition_uri_str");
		
		Map<HADatAcThing, List<HADatAcThing>> results = new HashMap<HADatAcThing, List<HADatAcThing>>();
		try {
			QueryExecution qe = QueryExecutionFactory.sparqlService(
					Collections.getCollectionsName(Collections.METADATA_SPARQL), query);
			ResultSet resultSet = qe.execSelect();
			ResultSetRewindable resultsrw = ResultSetFactory.copyResults(resultSet);
			qe.close();
			while (resultsrw.hasNext()) {
				QuerySolution soln = resultsrw.next();
				Instrument instrument = new Instrument();
				instrument.setUri(soln.get("instrumentUri").toString());
				instrument.setLabel(soln.get("instrumentLabel").toString());
				instrument.setField("instrument_uri_str");
				
				DataAcquisition da = new DataAcquisition();
				da.setUri(soln.get("dataAcquisitionUri").toString());
				da.setLabel(soln.get("dataAcquisitionLabel").toString());
				da.setField("acquisition_uri_str");
				
				if (!results.containsKey(instrument)) {
					List<HADatAcThing> facets = new ArrayList<HADatAcThing>();
					results.put(instrument, facets);
				}
				if (!results.get(instrument).contains(da)) {
					results.get(instrument).add(da);
				}
				
				Facet subFacet = facet.getChildById(instrument.getUri());
				subFacet.putFacet("instrument_uri_str", instrument.getUri());
				subFacet.putFacet("acquisition_uri_str", da.getUri());
			}
		} catch (QueryExceptionHTTP e) {
			e.printStackTrace();
		}

		return results;
	}
	
	public static List<Instrument> find() {
		List<Instrument> instruments = new ArrayList<Instrument>();
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
		    " SELECT ?uri WHERE { " +
		    " ?instModel rdfs:subClassOf+ vstoi:Instrument . " + 
		    " ?uri a ?instModel ." + 
		    "} ";
		
		Query query = QueryFactory.create(queryString);
		
		QueryExecution qexec = QueryExecutionFactory.sparqlService(Collections.getCollectionsName(Collections.METADATA_SPARQL), query);
		ResultSet results = qexec.execSelect();
		ResultSetRewindable resultsrw = ResultSetFactory.copyResults(results);
		qexec.close();
			
		while (resultsrw.hasNext()) {
		    QuerySolution soln = resultsrw.next();
		    Instrument instrument = find(soln.getResource("uri").getURI());
		    instruments.add(instrument);
		}			
		
		java.util.Collections.sort((List<Instrument>) instruments);
		return instruments;
		
	}
	
	public static List<Instrument> findAvailable() {
		List<Instrument> instruments = new ArrayList<Instrument>();
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
		    " SELECT ?uri WHERE { " +
		    "   { ?instModel rdfs:subClassOf+ vstoi:Instrument . " + 
		    "     ?uri a ?instModel ." + 
		    "   } MINUS { " + 
		    "     ?dep_uri a vstoi:Deployment . " + 
		    "     ?dep_uri hasco:hasInstrument ?uri .  " +
		    "     FILTER NOT EXISTS { ?dep_uri prov:endedAtTime ?enddatetime . } " + 
		    "    } " + 
		    "} " + 
		    "ORDER BY DESC(?datetime) ";
		
		Query query = QueryFactory.create(queryString);
		
		QueryExecution qexec = QueryExecutionFactory.sparqlService(Collections.getCollectionsName(Collections.METADATA_SPARQL), query);
		ResultSet results = qexec.execSelect();
		ResultSetRewindable resultsrw = ResultSetFactory.copyResults(results);
		qexec.close();
		
		while (resultsrw.hasNext()) {
		    QuerySolution soln = resultsrw.next();
		    Instrument instrument = find(soln.getResource("uri").getURI());
			instruments.add(instrument);
		}			
		
		java.util.Collections.sort((List<Instrument>) instruments);
		return instruments;
	}
	
	public static List<Instrument> findDeployed() {
	    //System.out.println("Inside Lits<Instrument> findAvailable()");
		List<Instrument> instruments = new ArrayList<Instrument>();
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
		    " SELECT ?uri WHERE { " +
		    "   ?instModel rdfs:subClassOf+ vstoi:Instrument . " + 
		    "   ?uri a ?instModel ." + 
		    "   ?dep_uri a vstoi:Deployment . " + 
		    "   ?dep_uri hasco:hasInstrument ?uri .  " +
		    "   FILTER NOT EXISTS { ?dep_uri prov:endedAtTime ?enddatetime . } " + 
		    "} " + 
		    "ORDER BY DESC(?datetime) ";
		
		Query query = QueryFactory.create(queryString);
			
		QueryExecution qexec = QueryExecutionFactory.sparqlService(Collections.getCollectionsName(Collections.METADATA_SPARQL), query);
		ResultSet results = qexec.execSelect();
		ResultSetRewindable resultsrw = ResultSetFactory.copyResults(results);
		qexec.close();
		
		while (resultsrw.hasNext()) {
		    QuerySolution soln = resultsrw.next();
		    Instrument instrument = find(soln.getResource("uri").getURI());
		    instruments.add(instrument);
		}			

		java.util.Collections.sort((List<Instrument>) instruments);
		return instruments;
	}
	
	public static Instrument find(String uri) {
	    Instrument instrument = null;
	    Model model;
	    Statement statement;
	    RDFNode object;
	    
	    String queryString = "DESCRIBE <" + uri + ">";
		Query query = QueryFactory.create(queryString);
		QueryExecution qexec = QueryExecutionFactory.sparqlService(
				ConfigFactory.load().getString("hadatac.solr.triplestore") + 
				Collections.METADATA_SPARQL, query);
		model = qexec.execDescribe();
		
		instrument = new Instrument();
		StmtIterator stmtIterator = model.listStatements();
		
		while (stmtIterator.hasNext()) {
		    statement = stmtIterator.next();
		    object = statement.getObject();
		    if (statement.getPredicate().getURI().equals("http://www.w3.org/2000/01/rdf-schema#label")) {
			instrument.setLabel(object.asLiteral().getString());
		    } else if (statement.getPredicate().getURI().equals("http://hadatac.org/ont/vstoi#hasSerialNumber")) {
			instrument.setSerialNumber(object.asLiteral().getString());
		    } else if (statement.getPredicate().getURI().equals("http://www.w3.org/2000/01/rdf-schema#comment")) {
			instrument.setComment(object.asLiteral().getString());
		    }
		}
		
		instrument.setUri(uri);
		
		return instrument;
	}
	
	public static Instrument find(HADataC hadatac) {
		Instrument instrument = null;
		
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() 
		    + "SELECT ?instrument ?label WHERE {\n"
		    + "  <" + hadatac.getDeploymentUri() + "> hasco:hasInstrument ?instrument .\n"
		    + "  OPTIONAL { ?instrument rdfs:label ?label . }\n"
		    + "  OPTIONAL { ?instrument rdfs:comment ?comment . }\n"
		    + "}";
		
		Query query = QueryFactory.create(queryString);
		
		QueryExecution qexec = QueryExecutionFactory.sparqlService(hadatac.getStaticMetadataSparqlURL(), query);
		ResultSet results = qexec.execSelect();
		ResultSetRewindable resultsrw = ResultSetFactory.copyResults(results);
		qexec.close();
		
		if (resultsrw.size() >= 1) {
		    QuerySolution soln = resultsrw.next();
		    instrument = new Instrument();
		    instrument.setUri(soln.getResource("instrument").getURI());
		    if (soln.getLiteral("label") != null) { 
			instrument.setLabel(soln.getLiteral("label").getString()); 
		    } else if (soln.getLiteral("comment") != null) { 
			instrument.setComment(soln.getLiteral("comment").getString()); 
		    } 
		}
		
		return instrument;
	}

    @Override
    public int compareTo(Instrument another) {
        return this.getLabel().compareTo(another.getLabel());
    }
	
}
