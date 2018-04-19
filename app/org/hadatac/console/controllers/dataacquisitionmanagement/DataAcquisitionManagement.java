package org.hadatac.console.controllers.dataacquisitionmanagement;

import org.hadatac.console.controllers.AuthApplication;
import org.hadatac.console.controllers.dataacquisitionmanagement.routes;
import org.hadatac.console.controllers.triplestore.UserManagement;
import org.hadatac.console.models.DataAcquisitionForm;
import org.hadatac.console.models.SysUser;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.data.FormFactory;

import org.hadatac.console.views.html.dataacquisitionmanagement.*;
import org.hadatac.entity.pojo.ObjectAccessSpec;
import org.hadatac.entity.pojo.DataAcquisitionSchema;
import org.hadatac.entity.pojo.Deployment;
import org.hadatac.entity.pojo.TriggeringEvent;
import org.hadatac.entity.pojo.User;
import org.hadatac.entity.pojo.UserGroup;
import org.hadatac.metadata.loader.URIUtils;
import org.hadatac.utils.State;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;

public class DataAcquisitionManagement extends Controller {
	
	@Inject
	private FormFactory formFactory;

	@Restrict(@Group(AuthApplication.DATA_OWNER_ROLE))
	public Result index(int stateId) {
		List<ObjectAccessSpec> results = null;
		State state = new State(stateId);
		final SysUser user = AuthApplication.getLocalUser(session());
		if (user.isDataManager()) {
			results = ObjectAccessSpec.findAll(state);
		}
		else {
			String ownerUri = UserManagement.getUriByEmail(user.getEmail());
			results = ObjectAccessSpec.find(ownerUri, state);
		}

		for (ObjectAccessSpec dataAcquisition : results) {
			dataAcquisition.setSchemaUri(URIUtils.replaceNameSpaceEx(
					dataAcquisition.getSchemaUri()));
		}
		results.sort(new Comparator<ObjectAccessSpec>() {
			@Override
			public int compare(ObjectAccessSpec lhs, ObjectAccessSpec rhs) {
				return lhs.getUri().compareTo(rhs.getUri());
			}
		});

		return ok(dataAcquisitionManagement.render(state, results, user.isDataManager()));
	}

	@Restrict(@Group(AuthApplication.DATA_OWNER_ROLE))
	public Result postIndex(int stateId) {
		return index(stateId);
	}

	@Restrict(@Group(AuthApplication.DATA_OWNER_ROLE))
	public Result newDataAcquisition() {
		if (session().get("LabKeyUserName") == null && session().get("LabKeyPassword") == null) {
			return redirect(org.hadatac.console.controllers.triplestore.routes.LoadKB.logInLabkey(
					routes.DataAcquisitionManagement.newDataAcquisition().url()));
		}

		final SysUser sysUser = AuthApplication.getLocalUser(session());

		List<DataAcquisitionSchema> schemas = DataAcquisitionSchema.findAll();
		for (DataAcquisitionSchema schema : schemas) {
			schema.setUri(URIUtils.replaceNameSpaceEx(schema.getUri()));
		}

		Map<String, String> nameList = new HashMap<String, String>();
		List<User> groups = UserGroup.find();
		for (User group : groups) {
			nameList.put(group.getUri(), group.getName());
		}

		return ok(newDataAcquisition.render(Deployment.find(new State(State.ACTIVE)),
				schemas, nameList, User.getUserURIs(), sysUser.isDataManager()));
	}

	@Restrict(@Group(AuthApplication.DATA_OWNER_ROLE))
	public Result postNewDataAcquisition() {
		return newDataAcquisition();
	}

	@Restrict(@Group(AuthApplication.DATA_OWNER_ROLE))
	public Result processForm() {
		final SysUser sysUser = AuthApplication.getLocalUser(session());

		Form<DataAcquisitionForm> form = formFactory.form(DataAcquisitionForm.class).bindFromRequest();
		DataAcquisitionForm data = form.get();

		if (form.hasErrors()) {
			return badRequest("The submitted form has errors!");
		}
		if (null != ObjectAccessSpec.findByUri(data.getNewDataAcquisitionUri())) {
			return badRequest("Data acquisition with this uri already exists!");
		}

		ObjectAccessSpec da = new ObjectAccessSpec();
		da.setUri(data.getNewDataAcquisitionUri());
		da.setNumberDataPoints(0);
		da.setSchemaUri(data.getNewSchema());
		da.setTriggeringEvent(TriggeringEvent.INITIAL_DEPLOYMENT);
		da.setParameter(data.getNewParameter());
		if (sysUser.isDataManager()) {
			da.setOwnerUri(data.getNewOwner());
		}
		da.setPermissionUri(data.getNewPermission());
		DateTimeFormatter isoFormat = DateTimeFormat.forPattern("MM/dd/yyyy HH:mm a");
		da.setStartedAt(isoFormat.parseDateTime(data.getNewStartDate()));
		da.save();

		return redirect(routes.DataAcquisitionManagement.index(State.ACTIVE));
	}
}

