package org.wescheme.servlet;

import java.io.IOException;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.output.XMLOutputter;
import org.wescheme.project.DriveProgram;
import org.wescheme.project.Program;
import org.wescheme.user.Session;
import org.wescheme.user.SessionManager;
import org.wescheme.util.CacheHelpers;
import org.wescheme.util.PMF;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

/**
 * Deletes a program by setting its isDeleted flag.
 * 
 * @author dyoo
 *
 */

public class Delete extends BaseServlet{
	private static final Logger log = Logger.getLogger(Share.class.getName());
	private static final long serialVersionUID = -5765142296681571504L;

	public void doPost(HttpServletRequest req, HttpServletResponse resp)	throws IOException 
	{
		PersistenceManager pm = PMF.get().getPersistenceManager();
		Session userSession;
		SessionManager sm = new SessionManager();
		try {
	        String pid = req.getParameter("pid");
	        
			userSession = sm.authenticate(req, resp);
			if( null != userSession ) {
				CacheHelpers.notifyUserProgramsDirtied(userSession.getName());
				
//				Program prog = pm.getObjectById(Program.class,
//						Long.parseLong(req.getParameter("pid")));		
//				if (prog.getOwner().equals(userSession.getName())) {
//					prog.setIsDeleted(true);
//					XMLOutputter outputter = new XMLOutputter();
//					resp.getWriter().print(outputter.outputString(prog.toXML(pm)));
//				} else {
//					log.warning(userSession.getName() + " does not own project " + req.getParameter("pid"));
//					throw new RuntimeException("Doesn't own Project");
//				}
				
				
				
				//The Delete Part
				
				Drive service = getDriveService(req, resp);
				log.severe(pid);
		        service.files().delete(pid).execute();
		        log.severe("done deleting the program.");
				
				
			} else {
				log.warning("Unauthorized users may not delete a project.");
				resp.sendError(403);
			}
		} finally {
			pm.close();
		}
	}	
}