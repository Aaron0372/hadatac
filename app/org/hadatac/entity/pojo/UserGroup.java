package org.hadatac.entity.pojo;

import java.util.ArrayList;
import java.util.List;

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

import org.hadatac.utils.Collections;

public class UserGroup extends User {
	
	public static User find(String uri) {	
		User user = new User();
		String queryString = "DESCRIBE <" + uri + ">";
		Query query = QueryFactory.create(queryString);
		
		Model modelPublic;
		Model modelPrivate;
		Statement statement;
		RDFNode object;
		
		QueryExecution qexecPrivate = QueryExecutionFactory.sparqlService(
				Collections.getCollectionsName(Collections.PERMISSIONS_SPARQL), query);
		modelPrivate = qexecPrivate.execDescribe();

		StmtIterator stmtIteratorPrivate = modelPrivate.listStatements();
		
		while (stmtIteratorPrivate.hasNext()) {
			statement = stmtIteratorPrivate.next();
			object = statement.getObject();
			if (statement.getPredicate().getURI().equals("http://www.w3.org/2000/01/rdf-schema#comment")) {
				user.setComment(object.asLiteral().getString());
				System.out.println("comment: " + object.asLiteral().getString());
		    }
			else if (statement.getPredicate().getURI().equals("http://hadatac.org/ont/hadatac#isMemberOfGroup")) {
				if(object.toString().equals("Public") || object.toString().equals("")){
					user.setImmediateGroupUri("Public");
				}
				else{
					user.setImmediateGroupUri(object.asResource().toString());
					System.out.println("memberOfUri: " + object.asResource().toString());
				}
			}
			else if (statement.getPredicate().getURI().equals("http://xmlns.com/foaf/0.1/givenName")) {
				user.setGivenName(object.asLiteral().getString());
				System.out.println("given_name: " + object.asLiteral().getString());
			}
			else if (statement.getPredicate().getURI().equals("http://xmlns.com/foaf/0.1/familyName")) {
				user.setFamilyName(object.asLiteral().getString());
				System.out.println("family_name: " + object.asLiteral().getString());
			}
			else if (statement.getPredicate().getURI().equals("http://xmlns.com/foaf/0.1/name")) {
				user.setName(object.asLiteral().getString());
				System.out.println("name: " + object.asLiteral().getString());
			}
			else if (statement.getPredicate().getURI().equals("http://xmlns.com/foaf/0.1/mbox")) {
				user.setEmail(object.asLiteral().getString());
				System.out.println("mbox: " + object.asLiteral().getString());
			}
			else if (statement.getPredicate().getURI().equals("http://xmlns.com/foaf/0.1/homepage")) {
				String homepage = object.asLiteral().getString();
				if(homepage.startsWith("<") && homepage.endsWith(">")){
					System.out.println("homepage: " + homepage);
					homepage = homepage.replace("<", "");
					homepage = homepage.replace(">", "");
				}
				user.setHomepage(homepage);
				System.out.println("homepage: " + homepage);
		    }
		}
		
		QueryExecution qexecPublic = QueryExecutionFactory.sparqlService(Collections.getCollectionsName(Collections.METADATA_SPARQL), query);
		modelPublic = qexecPublic.execDescribe();
		
		StmtIterator stmtIteratorPublic = modelPublic.listStatements();
		while (stmtIteratorPublic.hasNext()) {
			statement = stmtIteratorPublic.next();
			object = statement.getObject();
			if (statement.getPredicate().getURI().equals("http://xmlns.com/foaf/0.1/name")) {
				user.setName(object.asLiteral().getString());
				System.out.println("name: " + object.asLiteral().getString());
			} else if (statement.getPredicate().getURI().equals("http://xmlns.com/foaf/0.1/mbox")) {
				user.setEmail(object.asLiteral().getString());
				System.out.println("mbox: " + object.asLiteral().getString());
			}
		}
		
		user.setUri(uri);
		return user;
	}
	
	public static List<User> find() {
		List<User> users = new ArrayList<User>();
		String queryString = 
				"PREFIX prov: <http://www.w3.org/ns/prov#>  " +
        		"PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
				"SELECT ?uri WHERE { " +
				"  ?uri a prov:Group . " +
				"} ";
		
		Query query = QueryFactory.create(queryString);
		
		QueryExecution qexec = QueryExecutionFactory.sparqlService(Collections.getCollectionsName(Collections.PERMISSIONS_SPARQL), query);
		ResultSet results = qexec.execSelect();
		ResultSetRewindable resultsrw = ResultSetFactory.copyResults(results);
		qexec.close();
		
		while (resultsrw.hasNext()) {
			QuerySolution soln = resultsrw.next();
			System.out.println("URI from main query: " + soln.getResource("uri").getURI());
			User user = find(soln.getResource("uri").getURI());
			if(null != user){
				users.add(user);
			}
		}			

		java.util.Collections.sort((List<User>) users);
		return users;
	}
	
	public static List<User> findMembers(String group_uri) {
		System.out.println("findMembers(" + group_uri + ") is called.");
		List<User> users = new ArrayList<User>();
		String queryString = 
				"PREFIX prov: <http://www.w3.org/ns/prov#>  " +
        		"PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
        		"PREFIX hadatac: <http://hadatac.org/ont/hadatac#> " + 
				"SELECT ?uri WHERE { " +
				"  ?uri a foaf:Person . " +
				"  ?uri hadatac:isMemberOfGroup <" + group_uri + "> . " +
				"} ";
		
		Query query = QueryFactory.create(queryString);
		
		QueryExecution qexec = QueryExecutionFactory.sparqlService(Collections.getCollectionsName(Collections.PERMISSIONS_SPARQL), query);
		ResultSet results = qexec.execSelect();
		ResultSetRewindable resultsrw = ResultSetFactory.copyResults(results);
		qexec.close();
		
		while (resultsrw.hasNext()) {
			QuerySolution soln = resultsrw.next();
			System.out.println("URI from main query: " + soln.getResource("uri").getURI());
			User user = find(soln.getResource("uri").getURI());
			users.add(user);
		}		
		System.out.println(users.size());
		
		java.util.Collections.sort((List<User>) users);
		return users;
	}
}
