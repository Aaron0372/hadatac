package org.hadatac.console.controllers.triplestore;

import java.io.*;
import java.util.*;

//import java.lang.reflect.Field;
import java.lang.Object;
/*
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hadatac.console.controllers.AuthApplication;
import org.hadatac.console.views.html.dataacquisitionmanagement.dataAcquisitionManagement;
import org.hadatac.console.views.html.triplestore.*;
import org.hadatac.entity.pojo.DataAcquisition;
import org.hadatac.console.models.LabKeyLoginForm;
import org.hadatac.console.models.SysUser;
import org.hadatac.metadata.loader.MetadataContext;
import org.hadatac.metadata.loader.SpreadsheetProcessing;
import org.hadatac.metadata.loader.TripleProcessing;
import org.hadatac.utils.Feedback;
import org.hadatac.utils.NameSpaces;
import org.hadatac.utils.State;
*/
import org.hadatac.console.views.html.triplestore.*;
import org.hadatac.entity.pojo.DataAcquisition;
import org.hadatac.metadata.loader.TripleProcessing;
import org.hadatac.utils.NameSpaces;
import org.hadatac.utils.State;
import org.labkey.remoteapi.query.*;
import org.hadatac.console.controllers.AuthApplication;
import org.hadatac.console.controllers.metadataacquisition.ViewStudy;
import org.hadatac.console.models.LabKeyLoginForm;
import org.hadatac.console.models.SysUser;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;

import play.Play;
import play.data.Form;
/*
import play.Play;
import play.data.Form;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
*/
import play.mvc.Controller;
import play.mvc.Result;

public class DiffTool extends Controller {

	public static List<String> diffLoadLists(List<String> list_names) {
    	System.out.println("Loading List Info for Diff Tool\n");
    	List<String> final_names = new LinkedList<String>();
    	if (list_names.size() == 2){
	        for(String name : list_names){
        		final_names.add(name);
	        }
        }
        else {
        	System.out.println("Please Select Exactly 2 Folders\n");
        }   
    	return final_names;
    }
	
	//Used to pull the name and description from the lists
    private static ArrayList<String> Convert_str(String s, String type){
        ArrayList<String>dataArray = new ArrayList<String>() ;
        if(type.equals("Name") || type.equals("Description")){
            StringTokenizer st = new StringTokenizer(s,"\"");
            int i = 0;
            while(st.hasMoreElements()){
                if(i == 3) { dataArray.add(st.nextElement().toString()); }
                else { st.nextElement(); }
                i++;
            }
        }
        return dataArray;
    }

    //used to pull just the name of each list
    private static String Get_Name(String s, String type){
        String item = "";
        if(type.equals("Name")){
            StringTokenizer st = new StringTokenizer(s,"\"");
            int i = 0;
            while(st.hasMoreElements()){
                if(i == 3) { item = st.nextElement().toString(); }
                else { st.nextElement(); }
                i++;
            }
        }
        return item;
    }


    //Retrieves both the name and description of each list
    private static Collection<ArrayList<String>> ConnectToLabkey_List_Manager(String url_name, Connection cn) throws Exception{
        
        SelectRowsCommand cmd = new SelectRowsCommand("ListManager", "ListManager");
        cmd.setRequiredVersion(9.1);
        cmd.setColumns(Arrays.asList("Name", "Description"));
        cmd.setSorts(Collections.singletonList(new Sort("Name")));

        SelectRowsResponse response = cmd.execute(cn, url_name);
        ////System.out.println("Number of rows: " + response.getRowCount());

        List<Map<String,Object>> rows = response.getRows();
        int i = 0;
        Collection<ArrayList<String>> dataArrayM = new ArrayList<ArrayList<String>>() ;

        Collection <String> dataNameArray = new ArrayList<String>() ;

        for (Map<String, Object> row : rows) {

            ArrayList<String>dataArray1 = Convert_str(rows.get(i).get("Name").toString(), "Name");
            ArrayList<String>dataArray2 = Convert_str(rows.get(i).get("Description").toString(), "Description");
            if(!dataArray2.isEmpty()){
                dataArray1.add(dataArray2.get(0));
            }
            dataArrayM.add(dataArray1);
            dataNameArray.add(Get_Name(rows.get(i).get("Name").toString(), "Name"));
            i++;
        }

        ////System.out.println(dataArrayM);
        ////System.out.println("\n\n The collection names:");
        ////System.out.println(dataNameArray);

        return dataArrayM;
    }

    //Built for getting the headers of the sub-list
    private static Collection<String> ConnectToLabkey_List(String url_name, String list_name, Connection cn) throws Exception{
        
        SelectRowsCommand cmd = new SelectRowsCommand("lists", list_name);
        cmd.setRequiredVersion(9.1);

        SelectRowsResponse response = cmd.execute(cn, url_name);

        //get the header
        List<Map<String,Object>> rows = response.getColumnModel();
        Collection<String> dataArrayH = new ArrayList<String>() ;

        for (Map<String, Object> row : rows) {
            dataArrayH.add(row.get("dataIndex").toString());
        }
        return dataArrayH;
    }

    private static ArrayList<String> SortAlpha_FromCollection(Collection<String> inputCol){
        ArrayList<String> list = new ArrayList<String>();
        for (Iterator<String> iter_name = inputCol.iterator(); iter_name.hasNext(); ) {
            list.add(iter_name.next());
        }
        Collections.sort(list);
        return list;
    }
    private static void print_ArrayList(ArrayList<String> inputArr, PrintWriter writer){
        //writer.print("\n");
        for(int i = 0; i < inputArr.size(); i++){
            if(i==0){ writer.print(inputArr.get(i)); }
            else{ writer.print(", " + inputArr.get(i)); }
        }
        writer.print("\n");
        return;
    }

    //function creates a collection of the lists that appear in both projects
    private static ArrayList<String> CombineHeaders(Collection<String> H1, Collection<String> H2, PrintWriter writer, String Schema1, String Schema2){
        Collection<String> similar = new HashSet<String>( H1 );
        Collection<String> different = new HashSet<String>();

        different.addAll( H1 );
        different.addAll( H2 );

        similar.retainAll( H2 );
        different.removeAll( similar );

        //sort the array
        ArrayList<String> list = SortAlpha_FromCollection(similar);

        writer.println("<p>------------------------------------</p>");
        writer.print("<h3>Similar list names (does not account difference in description):</h3>\n");
        if(list.isEmpty()){
            System.out.printf("<p>There are no Similarities between the two files</p>\n");
        }
        else{
            print_ArrayList(list, writer);
        }
        writer.print("\n");
        CompareHeaders(H1, H2, writer, Schema1, Schema2);
        writer.println("<p>------------------------------------</p>");

        return list;
    }


    //For comparing the list headers
    private static void CompareListCollections(Collection<ArrayList<String>> line1Array, Collection<ArrayList<String>> line2Array, PrintWriter writer, String Schema1, String Schema2){
        //Lists With similar names, but different descriptions
        writer.print("<h3>List of values that have the same name, but different descriptions:</h3>\n<table style=\"width:80%\">\n");
        writer.print("<tr><th>" + Schema1 + "</th><th></th><th>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</th><th>" + Schema2 + "</th><th></th></tr>\n");
        writer.print("<tr><th>Name</th><th>Description</th><th></th><th>Name</th><th>Description</th></tr>\n");
        for (Iterator<ArrayList<String>> iter1 = line1Array.iterator(); iter1.hasNext(); ) {
            ArrayList<String> arr_iter1 = iter1.next();
            for (Iterator<ArrayList<String>> iter2 = line2Array.iterator(); iter2.hasNext(); ) {
                ArrayList<String> arr_iter2 = iter2.next();
                if(arr_iter1.size() == 1 || arr_iter2.size() == 1 ){
                	if(arr_iter1.size() == 1){
	                    if(arr_iter1.get(0).equals(arr_iter2.get(0)) && arr_iter1.size() != arr_iter2.size()) {
	                        writer.println("<tr><td>"+ arr_iter1.get(0) + "</td><td> </td><td></td><td>" + arr_iter2.get(0) + "</td><td>" + arr_iter2.get(1) + "</td></tr>\n");
	                    }
                	} else if(arr_iter2.size() == 1) {
                		if(arr_iter1.get(0).equals(arr_iter2.get(0)) && arr_iter1.size() != arr_iter2.size()) {
	                        writer.println("<tr><td>"+ arr_iter1.get(0) + "</td><td>" + arr_iter1.get(1) + "</td><td></td><td>" + arr_iter2.get(0) + "</td><td> </td></tr>\n");
	                    }
                	}
                }
                else if( (arr_iter1.get(0).equals(arr_iter2.get(0))) && (!arr_iter1.get(1).equals(arr_iter2.get(1))) ) {
                   writer.println("<tr><td>"+ arr_iter1.get(0) + "</td><td>"+ arr_iter1.get(1) + "</td><td></td><td>" + arr_iter2.get(0) + "</td><td>" + arr_iter2.get(1) + "</td></tr>\n");
                }
            }
        }
        writer.print("</table>");
        //Functionality to implement:
            // Determine which have different descriptions and put it in a separate list
    }

    private static void CompareHeaders(Collection<String> line1Array, Collection<String> line2Array, PrintWriter writer, String Schema1, String Schema2){
        Collection<String> similar_ = new HashSet<String>( line1Array );
        Collection<String> different_ = new HashSet<String>();
        Collection<String> inMainNotSecond_ = new HashSet<String>();
        Collection<String> inSecondNotMain_ = new HashSet<String>();
        different_.addAll( line1Array );
        different_.addAll( line2Array );

        similar_.retainAll( line2Array );
        different_.removeAll( similar_ );

        inMainNotSecond_.addAll( line1Array );
        inMainNotSecond_.removeAll( line2Array);
        inSecondNotMain_.addAll( line2Array );
        inSecondNotMain_.removeAll( line1Array);

        //sort the array
        ArrayList<String> diff_list = SortAlpha_FromCollection(different_);
        ArrayList<String> main_notSecond_list = SortAlpha_FromCollection(inMainNotSecond_);
        ArrayList<String> second_notMain_list = SortAlpha_FromCollection(inSecondNotMain_);

        writer.print("<h4>Differences:</h4>\n");
        if(diff_list.isEmpty()){
            writer.print("\t<p>There are no differences between the two headers</p>\n");
        }
        else{
            print_ArrayList(diff_list, writer);

            writer.print("\n<h4>All Lines in " + Schema1 + ", but not in " + Schema2 + ":</h4><p>\n");
            if(main_notSecond_list.isEmpty()){
                writer.println("NONE");
            }
            else{
                print_ArrayList(main_notSecond_list, writer);
            }
            writer.print("</p>");
            writer.print("\n<h4>All Lines in " + Schema2 + ", but not in " + Schema1 + ":</h4><p>\n");
            if(second_notMain_list.isEmpty()){
                writer.println("NONE");
            }
            else{
                print_ArrayList(second_notMain_list, writer);
            }
            writer.print("</p>");
        }
    }

    public static void runDiffTool(LabKeyLoginForm auth, List<String> list) throws Exception {
    	
        /*if(args.length != 4){
            System.out.println("ERROR: Incorrect number of arguments");
            System.out.println(args[0]);
            System.out.println("Usage: Schema1, Schema2, Username, Password");
            return;
        }
*/
    	String Schema1 = "";
       	String Schema2 = "";
		if (list.size()==2) {
        	Schema1 = list.get(0);//"CHEAR Development";
        	Schema2 = list.get(1);//"CHEAR Production";
		} else {
			Schema1 = "CHEAR Development";
        	Schema2 = "CHEAR Production";
		}	
		String username = auth.user_name;
        String password = auth.password;
        Connection cn = new Connection("http://chear.tw.rpi.edu/labkey/", username, password);

        PrintWriter writer = new PrintWriter("./app/org/hadatac/console/views/triplestore/diff_results.scala.html", "UTF-8");
        writer.print("@()\n@import helper._\n@import org.hadatac.console.views.html._\n@import org.hadatac.console.controllers.triplestore._\n@import org.hadatac.data.loader._\n@import org.hadatac.metadata.loader._\n@import org.hadatac.utils._\n@import java.net._\n@import play._\n\n@main(\"Diff Tool Results\") {\n<div class=\"container-fluid\">\n<h1>Diff Tool Results</h1>\n");
        //Map<String, Object> chear_dev_map = connect_chear_dev();
        Collection<ArrayList<String>> dataArrayMain = ConnectToLabkey_List_Manager(Schema1, cn);
        Collection<ArrayList<String>> dataArraySecond = ConnectToLabkey_List_Manager(Schema2, cn);
        CompareListCollections(dataArrayMain, dataArraySecond,  writer, Schema1, Schema2);

//        Collection<String> dataArrayH1 = ConnectToLabkey_Retrieve_List_Names(Schema1, cn);
//        Collection<String> dataArrayH2 = ConnectToLabkey_Retrieve_List_Names(Schema2, cn);

        Collection<String> dataArrayH1 =  new ArrayList<String>();
        Collection<String> dataArrayH2 =  new ArrayList<String>();

        for (Iterator<ArrayList<String>> iter1 = dataArrayMain.iterator(); iter1.hasNext(); ) {
            ArrayList<String> arr_iter1 = iter1.next();
            dataArrayH1.add(arr_iter1.get(0));
        }
        for (Iterator<ArrayList<String>> iter2 = dataArraySecond.iterator(); iter2.hasNext(); ) {
            ArrayList<String> arr_iter2 = iter2.next();
            dataArrayH2.add(arr_iter2.get(0));
        }
        
        Collection<String> dataArrayCombined = CombineHeaders(dataArrayH1, dataArrayH2, writer, Schema1, Schema2);

        for (Iterator<String> iter = dataArrayCombined.iterator(); iter.hasNext(); ) {
            String sub_list_name = iter.next();
            writer.print("<h2>List Name: " + sub_list_name + "</h2>\n\n");
            Collection<String> dataArrayH11 = ConnectToLabkey_List(Schema1,sub_list_name, cn);
            Collection<String> dataArrayH22 = ConnectToLabkey_List(Schema2,sub_list_name, cn);

            CompareHeaders(dataArrayH11, dataArrayH22, writer, Schema1, Schema2);

            writer.println("<p>---------------------------</p>");
        }
        writer.print("\n</div>\n}");
        writer.close();

    }
    
    //public static void updateForm(String alias, List<String> selectedTerms) {
    public static void updateForm(String alias) {
    	System.out.println("Alias: " + alias);
    }
    
    public static Result index(LabKeyLoginForm auth, List<String> list_names) throws Exception {
    	List<String> lists = diffLoadLists(list_names);
    	runDiffTool(auth,lists);
        return ok(diff_results.render());
        
    }
    
    public static Result postIndex(LabKeyLoginForm auth, List<String> list_names) throws Exception {
        
        return index(auth, list_names);
        
    }
    
}
