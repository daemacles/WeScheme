package org.wescheme.dropbox;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.wescheme.project.Program;
import org.wescheme.user.Session;
import org.wescheme.user.SessionManager;
import org.wescheme.user.UnauthorizedUserException;
import org.wescheme.util.PMF;

public class AddProjectServlet extends HttpServlet {

	private static final long serialVersionUID = 1927887337443757869L;

	public void doPost(HttpServletRequest req, HttpServletResponse resp){
		PersistenceManager pm = PMF.get().getPersistenceManager();
		
		try {
			

			Session userSession;
			SessionManager sm = new SessionManager();
			userSession = sm.authenticate(req, resp);
			String userName = userSession.getName();
			Long dbID = new Long(req.getParameter("dbID"));
			String binID = req.getParameter("binID");
				
			Dropbox db = Dropbox.getDropbox(pm, dbID);
			
			
			String[] programs = req.getParameterValues("program");
			
			for(String s : programs){
				
				
				Long progID = new Long(s);
				System.out.println(progID);
				Program prog = (Program) pm.getObjectById(new javax.jdo.identity.LongIdentity(Program.class, progID));
			
				if( !userSession.getName().equals(prog.getOwner()) ){
					throw new UnauthorizedUserException();
				}
				Program p = prog.clone(db.owner());
				pm.makePersistent(p);
				Long pid = p.getId();
				Entry entry = new Entry(db.getId(), binID, pid);
				pm.makePersistent(entry);
			}
			pm.close();
					
		} catch (Exception e){
			System.err.println("Error while adding project");
			e.printStackTrace();
			//TODO make error handling robust.
		}
	
	}
}