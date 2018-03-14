package org.hadatac.entity.pojo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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
import org.hadatac.utils.CollectionUtil;
import org.hadatac.utils.NameSpaces;

import com.typesafe.config.ConfigFactory;

public class PlatformType extends HADatAcClass implements Comparable<PlatformType> {

    static String className = "vstoi:Platform";

    public PlatformType () {
        super(className);
    }

    public static List<PlatformType> find() {
        List<PlatformType> platformTypes = new ArrayList<PlatformType>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?uri rdfs:subClassOf* " + className + " . " + 
                "} ";

        Query query = QueryFactory.create(queryString);

        QueryExecution qexec = QueryExecutionFactory.sparqlService(CollectionUtil.getCollectionsName(CollectionUtil.METADATA_SPARQL), query);
        ResultSet results = qexec.execSelect();
        ResultSetRewindable resultsrw = ResultSetFactory.copyResults(results);
        qexec.close();

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            PlatformType platformType = find(soln.getResource("uri").getURI());
            platformTypes.add(platformType);
        }			

        java.util.Collections.sort((List<PlatformType>) platformTypes);
        return platformTypes;   
    }

    public static Map<String,String> getMap() {
        List<PlatformType> list = find();
        Map<String,String> map = new HashMap<String,String>();
        for (PlatformType typ: list) 
            map.put(typ.getUri(),typ.getLabel());
        return map;
    }

    public static PlatformType find(String uri) {
        PlatformType platformType = null;
        Model model;
        Statement statement;
        RDFNode object;

        String queryString = "DESCRIBE <" + uri + ">";
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService(
                ConfigFactory.load().getString("hadatac.solr.triplestore") 
                + CollectionUtil.METADATA_SPARQL, query);
        model = qexec.execDescribe();

        platformType = new PlatformType();
        StmtIterator stmtIterator = model.listStatements();

        while (stmtIterator.hasNext()) {
            statement = stmtIterator.next();
            object = statement.getObject();
            if (statement.getPredicate().getURI().equals("http://www.w3.org/2000/01/rdf-schema#label")) {
                platformType.setLabel(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals("http://www.w3.org/2000/01/rdf-schema#subClassOf")) {
                platformType.setSuperUri(object.asResource().getURI());
            }
        }

        platformType.setUri(uri);
        platformType.setLocalName(uri.substring(uri.indexOf('#') + 1));

        return platformType;
    }

    @Override
    public int compareTo(PlatformType another) {
        if (this.getLabel() != null && another.getLabel() != null) {
            return this.getLabel().compareTo(another.getLabel());
        }
        return this.getLocalName().compareTo(another.getLocalName());
    }
}
