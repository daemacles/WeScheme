package org.wescheme.servlet;

import java.io.IOException;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.wescheme.project.NameGenerator;
import org.wescheme.project.Program;
import org.wescheme.user.Session;
import org.wescheme.user.SessionManager;
import org.wescheme.util.PMF;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;


public class SaveProjectServlet extends HttpServlet{
	private static final Logger log = Logger.getLogger(SaveProjectServlet.class.getName());
	private static final long serialVersionUID = 4038563388689831368L;

	public void doPost(HttpServletRequest req, HttpServletResponse resp)	throws IOException 
	{
		PersistenceManager pm = PMF.get().getPersistenceManager();
		SessionManager sm = new SessionManager();
		
		if( !sm.isIntentional(req, resp) ){
			resp.sendError(401);
			return;
		}
		String title = req.getParameter("title");
		String code = req.getParameter("code");
		String pid = req.getParameter("pid");
		try {
			Session userSession = sm.authenticate(req, resp);
			if( null != userSession ){			
				if (pid == null) {
					saveNewProgram(pm, userSession, resp, title, code);
				} else {
					saveExistingProgram(pm, userSession, resp, pid, title, code);
					}
			} else {
				log.warning("User does not own project " + req.getParameter("pid"));
				resp.sendError(401);
				return;
			}
		} finally {
			pm.close();
		}		
	}

	private void saveNewProgram(PersistenceManager pm, Session userSession,
			HttpServletResponse resp,
			String title, String code) throws IOException {
		Program prog = new Program(code, userSession.getName());
		prog.updateTitle(title);
		prog.setPublicId(NameGenerator.getInstance(getServletContext()).generateUniqueName(pm));
//		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
//		prog.setId(service.allocateIds("Program", 1).getStart());

		prog = pm.makePersistent(prog);
		System.out.println("saved program, with id: " + prog.getId());
		resp.setContentType("text/plain"); 
		resp.getWriter().println(KeyFactory.keyToString(prog.getId()));					
	}
	
	

	


	private void saveExistingProgram(PersistenceManager pm, Session userSession,
			HttpServletResponse resp,
			String pid, String title, String code) throws IOException {
		// Preconditions: the program is owned by the user, and has not been published yet.
		Key id = KeyFactory.stringToKey(pid);
		Program prog = pm.getObjectById(Program.class, id);
		if (prog.getOwner().equals(userSession.getName()) && !prog.isPublished()) {
			prog.updateTitle(title);
			prog.updateSource(code);

			if (prog.getPublicId() == null) {
				prog.setPublicId(NameGenerator.getInstance(getServletContext()).generateUniqueName(pm));
			}
		
			resp.setContentType("text/plain");
			resp.getWriter().println(KeyFactory.keyToString(prog.getId()));					
		} else {
			// FIXME: throw an error that the client can recognize!
			throw new RuntimeException("Cannot save program: either not owner, or program published");
		}
	}
}