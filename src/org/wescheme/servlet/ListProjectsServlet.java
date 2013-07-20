package org.wescheme.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.wescheme.project.DriveProgram;
import org.wescheme.project.Program;
import org.wescheme.project.ProgramDigest;
import org.wescheme.user.Session;
import org.wescheme.user.SessionManager;
import org.wescheme.user.UnauthorizedUserException;
import org.wescheme.util.CacheHelpers;
import org.wescheme.util.PMF;
import org.wescheme.util.Queries;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

/**
 * Produces a list of projects of the logged-in user, in descending order of modification time.
 * 
 * This list is presented as a sequence of ProgramDigest XML elements.
 * 
 * @author dyoo
 *
 */
public class ListProjectsServlet extends BaseServlet {
	
    private static final long serialVersionUID = 6291188410939739681L;
    private static final Logger log = Logger.getLogger(ListProjectsServlet.class.getName());

    /**
     * Caching mechanism.  We listen for user program dirty notices and clear the cache accordingly.
     */
    static {
        CacheHelpers.addUserProgramsDirtiedListener(new CacheHelpers.UserProgramsDirtiedListener() {
                public void onUserProgramsDirtied(String userName) {
                    Cache c = CacheHelpers.getCache();
                    if (c != null) {
                        c.remove(CacheHelpers.getUserProgramsCacheKey(userName));
                    }
                }
            });
    }
		
	
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Authentication
	
        try {
            Session userSession = authenticate(req, resp);		
            //String outputString = getOutputString(userSession);
            String outputString = getDriveOutputString(req, resp); 
            resp.setContentType("text/xml");
            PrintWriter w = resp.getWriter();
            w.write(outputString);
            w.close();
        } catch (IOException e) {
            log.severe("IO expection in ListProjectsServlet!" + e.toString());
            e.printStackTrace();
            resp.sendError(500);
        } catch (UnauthorizedUserException e) {
            log.warning("UnauthorizedUserException: user appears to be logged out");
            e.printStackTrace();
            resp.sendError(401);
        }
    }

    // authenticate: -> Session
    // Returns the Session of the currently logged-in user.
    // If the user isn't logged in, throws UnauthorizedUserException
    public Session authenticate(HttpServletRequest req, HttpServletResponse resp) throws UnauthorizedUserException {
        SessionManager sm = new SessionManager();
        Session userSession = sm.authenticate(req, resp);
        if( userSession == null ){
            throw new UnauthorizedUserException();
        }
        return userSession;
    }


    // @SuppressWarnings("unchecked")
	private String getOutputString(Session userSession) throws IOException {
        // Cache c = CacheHelpers.getCache();
        // if (c != null) {
        //     if (! c.containsKey(CacheHelpers.getUserProgramsCacheKey(userSession.getName()))) {
        //         String outputString = getFromDatabase(userSession);
        //         c.put(CacheHelpers.getUserProgramsCacheKey(userSession.getName()),
        //               outputString);
        //         return outputString;
        //     } else {
        //         String result = (String) c.get(CacheHelpers.getUserProgramsCacheKey(userSession.getName()));
        //         if (result != null) {
        //             return result;
        //         }
        //     }			
        // }
        
        // Turning off the cache for the moment.  Something is wrong, as I'm seeing stale data.

        return getFromDatabase(userSession);
    }
	
	private String getDriveOutputString(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Drive service = getDriveService(req, resp);
        FileList driveFiles = service.files().list().execute();
        Element elt = new Element("ProgramDigests");
        for (File f : driveFiles.getItems())
        {
        	log.info("displaying program: " + f.getTitle());
        	elt.addContent(DriveProgram.getEmptyProgram(f).toXmlProgramDigest());
        }

        XMLOutputter outputter = new XMLOutputter();
        String outputString = outputter.outputString(elt); 			
        return outputString;				
	}
	
    /**
     * Gets the set of ProgramDigests for the user with the given session.
     * @param userSession
     * @return
     * @throws IOException
     */
    private String getFromDatabase(Session userSession) throws IOException {
        PersistenceManager pm = PMF.get().getPersistenceManager();

        try {

            List<Program> pl = (List<Program>) 
                Queries.getUserPrograms(pm, userSession.getName());
            Element elt = new Element("ProgramDigests");
            for( Program p : pl ){
                if (! p.getIsDeleted())
                    elt.addContent(new ProgramDigest(p).toXML(pm));
            }
            XMLOutputter outputter = new XMLOutputter();
            String outputString = outputter.outputString(elt); 			
            return outputString;				
        } finally {
            pm.close();
        }
    }
}
