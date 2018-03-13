package org.hadatac.entity.pojo;

import java.util.List;
import java.util.ArrayList;
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
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.hadatac.utils.CollectionUtil;
import org.hadatac.utils.NameSpaces;
import org.hadatac.utils.FirstLabel;
import org.hadatac.metadata.loader.LabkeyDataHandler;
import org.hadatac.metadata.loader.URIUtils;
import org.labkey.remoteapi.CommandException;
import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import org.hadatac.console.controllers.AuthApplication;

public class DataAcquisitionSchemaEvent extends HADatAcThing {

	public static String INDENT1 = "     ";
	public static String INSERT_LINE1 = "INSERT DATA {  ";
	public static String DELETE_LINE1 = "DELETE WHERE {  ";
	public static String LINE3 = INDENT1 + "a         hasco:DASchemaEvent;  ";
	public static String DELETE_LINE3 = " ?p ?o . ";
	public static String LINE_LAST = "}  ";
	public static String PREFIX = "DASE-";

	private String uri;
	private String label;
	private String partOfSchema;
	private String entity;
	private String entityLabel;
	private String unit;
	private String unitLabel;

	public DataAcquisitionSchemaEvent(
	        String uri, 
			String label, 
			String partOfSchema, 
			String entity, 
			String unit) {
		this.uri = uri;
		this.label = label;
		this.partOfSchema = partOfSchema;
		this.setEntity(entity);
		this.setUnit(unit);
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getUriNamespace() {
		return URIUtils.replaceNameSpaceEx(uri);
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getPartOfSchema() {
		return partOfSchema;
	}

	public void setPartOfSchema(String partOfSchema) {
		this.partOfSchema = partOfSchema;
	}

	public String getEntity() {
		return entity;
	}

	public void setEntity(String entity) {
		this.entity = entity;
		if (entity == null || entity.equals("")) {
			this.entityLabel = "";
		} else {
			this.entityLabel = FirstLabel.getLabel(entity);
		}
	}

	public String getEntityNamespace() {
		return URIUtils.replaceNameSpaceEx(entity);
	}

	public String getEntityLabel() {
		if (entityLabel.equals("")) {
			return URIUtils.replaceNameSpaceEx(entity);
		}
		return entityLabel;
	}

	public String getUnit() {
		if (unit == null) {
			return "";
		} else {
			return unit;
		}
	}

	public String getUnitNamespace() {
		if (unit == "") {
			return "";
		}
		return URIUtils.replaceNameSpaceEx(unit);
	}

	public void setUnit(String unit) {
		this.unit = unit;
		if (unit == null || unit.equals("")) {
			this.unitLabel = "";
		} else {
			this.unitLabel = FirstLabel.getLabel(unit);
		}
	}

	public String getUnitLabel() {
		if (unitLabel.equals("")) {
			return URIUtils.replaceNameSpaceEx(unit);
		}
		return unitLabel;
	}

	public String getAnnotatedUnit() {
		String annotation;
		if (unitLabel.equals("")) {
			if (unit == null || unit.equals("")) {
				return "";
			}
			annotation = URIUtils.replaceNameSpaceEx(unit);
		} else {
			annotation = unitLabel;
		}
		if (!getUnitNamespace().equals("")) {
			annotation += " [" + getUnitNamespace() + "]";
		}
		return annotation;
	}


	public static DataAcquisitionSchemaEvent find(String uri) {
		DataAcquisitionSchemaEvent event = null;
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() + 
				"SELECT ?partOfSchema ?entity ?unit WHERE { " + 
				"   <" + uri + "> a hasco:DASchemaEvent . " + 
				"   <" + uri + "> hasco:partOfSchema ?partOfSchema. " +
				"   OPTIONAL { <" + uri + ">  hasco:hasEntity ?entity } ." + 
				"   OPTIONAL { <" + uri + "> hasco:hasUnit ?unit } ." + 
				"}";
		Query query = QueryFactory.create(queryString);

		QueryExecution qexec = QueryExecutionFactory.sparqlService(CollectionUtil.getCollectionsName(CollectionUtil.METADATA_SPARQL), query);
		ResultSet results = qexec.execSelect();
		ResultSetRewindable resultsrw = ResultSetFactory.copyResults(results);
		qexec.close();

		if (!resultsrw.hasNext()) {
			System.out.println("[WARNING] DataAcquisitionSchemaEvent. Could not find event for uri: " + uri);
			return event;
		}

		QuerySolution soln = resultsrw.next();
		String labelStr = "";
		String partOfSchemaStr = "";
		String entityStr = "";
		String unitStr = "";
		try {
			if (soln != null) {

				labelStr = FirstLabel.getLabel(uri);

				try {
					if (soln.getResource("partOfSchema") != null && soln.getResource("partOfSchema").getURI() != null) {
						partOfSchemaStr = soln.getResource("partOfSchema").getURI();
					}
				} catch (Exception e1) {
					partOfSchemaStr = "";
				}

				try {
					if (soln.getResource("entity") != null && soln.getResource("entity").getURI() != null) {
						entityStr = soln.getResource("entity").getURI();
					}
				} catch (Exception e1) {
					entityStr = "";
				}

				try {
					if (soln.getResource("unit") != null && soln.getResource("unit").getURI() != null) {
						unitStr = soln.getResource("unit").getURI();
					}
				} catch (Exception e1) {
					unitStr = "";
				}

				event = new DataAcquisitionSchemaEvent(uri,
						labelStr,
						partOfSchemaStr,
						entityStr,
						unitStr);
			}
		}  catch (Exception e) {
			System.out.println("[ERROR] DataAcquisitionSchemaEvent. uri: e.Message: " + e.getMessage());
		}

		return event;
	}

	public static List<DataAcquisitionSchemaEvent> findBySchema(String schemaUri) {
		List<DataAcquisitionSchemaEvent> events = new ArrayList<DataAcquisitionSchemaEvent>();
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() + 
				"SELECT ?uri WHERE { " + 
				"   ?uri a hasco:DASchemaEvent . " + 
				"   ?uri hasco:partOfSchema <" + schemaUri + "> .  " + 
				"}";
		Query query = QueryFactory.create(queryString);

		QueryExecution qexec = QueryExecutionFactory.sparqlService(CollectionUtil.getCollectionsName(CollectionUtil.METADATA_SPARQL), query);
		ResultSet results = qexec.execSelect();
		ResultSetRewindable resultsrw = ResultSetFactory.copyResults(results);
		qexec.close();

		if (!resultsrw.hasNext()) {
			System.out.println("[WARNING] DataAcquisitionSchemaEvent. Could not find events for schema: " + schemaUri);
			return events;
		}

		while (resultsrw.hasNext()) {
			QuerySolution soln = resultsrw.next();
			try {
				if (soln != null && soln.getResource("uri") != null && soln.getResource("uri").getURI() != null) {

					DataAcquisitionSchemaEvent obj = DataAcquisitionSchemaEvent.find(soln.getResource("uri").getURI());
					if (obj != null) {
						events.add(obj);
					}
				}
			}  catch (Exception e) {
				System.out.println("[ERROR] DataAcquisitionSchemaEvent. uri: e.Message: " + e.getMessage());
			}

		}
		return events;
	}

	@Override
	public int saveToLabKey(String user_name, String password) {
		LabkeyDataHandler loader = LabkeyDataHandler.createDefault(user_name, password);
		List< Map<String, Object> > rows = new ArrayList< Map<String, Object> >();
		Map<String, Object> row = new HashMap<String, Object>();
		row.put("hasURI", URIUtils.replaceNameSpaceEx(getUri()));
		row.put("a", "hasco:DASchemaEvent");
		row.put("rdfs:label", getLabel());
		row.put("rdfs:comment", getLabel());
		row.put("hasco:partOfSchema", URIUtils.replaceNameSpaceEx(getPartOfSchema()));
		row.put("hasco:hasEntity", this.getEntity());
		row.put("hasco:hasUnit", this.getUnit());
		row.put("hasco:isVirtual", "");
		row.put("hasco:isPIConfirmed", "false");
		rows.add(row);
		int totalChanged = 0;
		try {
			totalChanged = loader.insertRows("DASchemaEvent", rows);
		} catch (CommandException e) {
			try {
				totalChanged = loader.updateRows("DASchemaEvent", rows);
			} catch (CommandException e2) {
				System.out.println("[ERROR] Could not insert or update DASE(s)");
			}
		}
		return totalChanged;
	}

	@Restrict(@Group(AuthApplication.DATA_MANAGER_ROLE))
	@Override
	public int deleteFromLabKey(String user_name, String password) {
		LabkeyDataHandler loader = LabkeyDataHandler.createDefault(user_name, password);
		List< Map<String, Object> > rows = new ArrayList< Map<String, Object> >();
		Map<String, Object> row = new HashMap<String, Object>();
		row.put("hasURI", URIUtils.replaceNameSpaceEx(getUri().replace("<","").replace(">","")));
		rows.add(row);
		
		try {
            return loader.deleteRows("DASchemaEvent", rows);
        } catch (CommandException e) {
            System.out.println("[ERROR] Could not delete DASE(s)");
            e.printStackTrace();
            return 0;
        }
	}

    @Override
    public boolean saveToTripleStore() {
        if (uri == null || uri.equals("")) {
            System.out.println("[ERROR] Trying to save DASE without assigning an URI");
            return false;
        }
        if (partOfSchema == null || partOfSchema.equals("")) {
            System.out.println("[ERROR] Trying to save DASE without assigning DAS's URI");
            return false;
        }
        
        deleteFromTripleStore();
        
        String insert = "";
        insert += NameSpaces.getInstance().printSparqlNameSpaceList();
        insert += INSERT_LINE1;
        insert += this.getUri() + " a hasco:DASchemaEvent . ";
        insert += this.getUri() + " rdfs:label  \"" + label + "\" . ";
        if (partOfSchema.startsWith("http")) {
            insert += this.getUri() + " hasco:partOfSchema <" + partOfSchema + "> .  ";
        } else {
            insert += this.getUri() + " hasco:partOfSchema " + partOfSchema + " .  ";
        }
        if (!entity.equals("")) {
            insert += this.getUri() + " hasco:hasEntity "  + entity + " .  ";
        }
        if (!unit.equals("")) {
            insert += this.getUri() + " hasco:hasUnit " + unit + " .  ";
        }                                                   
        insert += LINE_LAST;
        System.out.println("DASE insert query (pojo's save): <" + insert + ">");
        UpdateRequest request = UpdateFactory.create(insert);
        UpdateProcessor processor = UpdateExecutionFactory.createRemote(
                request, CollectionUtil.getCollectionsName(CollectionUtil.METADATA_UPDATE));
        processor.execute();
        
        return true;
    }

    @Override
    public void deleteFromTripleStore() {
        String query = "";
        if (this.getUri() == null || this.getUri().equals("")) {
            return;
        }
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += DELETE_LINE1;
        if (this.getUri().startsWith("http")) {
            query += "<" + this.getUri() + ">";
        } else {
            query += this.getUri();
        }
        query += DELETE_LINE3;
        query += LINE_LAST;                                        
        UpdateRequest request = UpdateFactory.create(query);
        UpdateProcessor processor = UpdateExecutionFactory.createRemote(
                request, CollectionUtil.getCollectionsName(CollectionUtil.METADATA_UPDATE));
        processor.execute();
    }

    @Override
    public boolean saveToSolr() {
        return false;
    }

    @Override
    public int deleteFromSolr() {
        return 0;
    }
}
