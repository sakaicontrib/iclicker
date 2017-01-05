/**
 * Copyright (c) 2009 i>clicker (R) <http://www.iclicker.com/dnn/>
 *
 * This file is part of i>clicker Sakai integrate.
 *
 * i>clicker Sakai integrate is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * i>clicker Sakai integrate is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with i>clicker Sakai integrate.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sakaiproject.iclicker.tool;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.sakaiproject.entitybus.util.http.HttpAuth;
import org.sakaiproject.entitybus.util.http.HttpRESTUtils;
import org.sakaiproject.iclicker.exception.ClickerIdInvalidException;
import org.sakaiproject.iclicker.exception.ClickerRegisteredException;
import org.sakaiproject.iclicker.logic.IClickerLogic;
import org.sakaiproject.iclicker.model.Course;
import org.sakaiproject.iclicker.model.Gradebook;
import org.sakaiproject.iclicker.model.GradebookItem;
import org.sakaiproject.iclicker.model.Student;
import org.sakaiproject.iclicker.model.dao.ClickerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This servlet will basically take the place of entitybroker in versions of Sakai that do not have it <br/>
 * More help info at: <br/>
 * http://localhost:8080/iclicker/rest
 */
public class RestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(RestServlet.class);

    private static final String PASSWORD = "_password";
    private static final String LOGIN = "_login";
    public static final String SESSION_ID = "_sessionId";
    public static final String SSO_KEY = "_key";
    public static final char SEPARATOR = '/';
    public static final char PERIOD = '.';
    public static final String COMPENSATE_METHOD = "_method";
    public static final String XML_DATA = "_xml";
    public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n";

    protected transient IClickerLogic logic;

    public IClickerLogic getLogic() {
        if (this.logic == null) {
            this.logic = IClickerLogic.getInstance();
        }

        return this.logic;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        log.info("INIT");
        super.init(config);
        // get the services
        getLogic();
        log.debug("IClickerLogic: {}:", logic);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // DEFAULT: POST for PUT or POST
        String method = "POST";
        String _method = req.getParameter(COMPENSATE_METHOD);

        if (StringUtils.isNotBlank(_method)) {
            // Allows override to GET or DELETE
            _method = _method.toUpperCase().trim();

            if (StringUtils.equals("GET", _method)) {
                method = "GET";
            } else if (StringUtils.equals("DELETE", _method)) {
                method = "DELETE";
            }
        }

        handle(req, resp, method);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // treat PUT as POST
        doPost(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handle(req, resp, "GET");
    }

    @SuppressWarnings("unchecked")
    protected void handle(HttpServletRequest req, HttpServletResponse res, String method) throws ServletException, IOException {
        // force all response encoding to UTF-8 / XML by default
        res.setCharacterEncoding("UTF-8");
        // get the path
        String path = req.getPathInfo();

        if (path == null) {
            path = "";
        }

        String[] segments = HttpRESTUtils.getPathSegments(path);

        // init the vars to success
        boolean valid = true;
        int status = HttpServletResponse.SC_OK;
        String output = "";

        // check to see if this is one of the paths we understand
        if (StringUtils.isBlank(path) || segments.length == 0) {
            valid = false;
            output = "Unknown path (" + path + ") specified";
            status = HttpServletResponse.SC_NOT_FOUND;
        }

        // check the method is allowed
        if (valid && !StringUtils.equals("POST", method) && !StringUtils.equals("GET", method)) {
            valid = false;
            output = "Only POST and GET methods are supported";
            status = HttpServletResponse.SC_METHOD_NOT_ALLOWED;
        }

        // attempt to handle the request
        if (valid) {
            // check against the ones we know and process
            String pathSeg0 = HttpRESTUtils.getPathSegment(path, 0);
            String pathSeg1 = HttpRESTUtils.getPathSegment(path, 1);

            boolean restDebug = false;

            if (req.getParameter("_debug") != null || logic.forceRestDebugging) {
                restDebug = true;
                StringBuilder sb = new StringBuilder();
                sb.append("[");

                for (Map.Entry<String, String[]> entry : (Set<Map.Entry<String, String[]>>) req.getParameterMap()
                                .entrySet()) {
                    sb.append(entry.getKey()).append("=").append(Arrays.toString(entry.getValue())).append(", ");
                }

                if (sb.length() > 2) {
                    sb.setLength(sb.length() - 2); // Removes the last comma
                }

                sb.append("]");
                log.info("iclicker REST debugging: req: {} {}, params={}", method, path, sb);
            }

            try {
                if (StringUtils.equals("verifykey", pathSeg0)) {
                    // SPECIAL case handling (no authn handling)
                    String ssoKey = req.getParameter(SSO_KEY);

                    if (logic.verifyKey(ssoKey)) {
                        status = HttpServletResponse.SC_OK;
                        output = "Verified";
                    } else {
                        status = HttpServletResponse.SC_NOT_IMPLEMENTED;
                        output = "Disabled";
                    }

                    if (restDebug) {
                        log.info("iclicker REST debugging: verifykey (s={}, o={})", status, output);
                    }

                    res.setContentType("text/plain");
                    res.setContentLength(output.length());
                    res.getWriter().print(output);
                    res.setStatus(status);

                    return;
                } else {
                    // NORMAL case handling
                    // handle the request authn
                    handleAuthN(req, res);
                    // process the REQUEST
                    if (StringUtils.equals("GET", method)) {
                        if (StringUtils.equals("courses", pathSeg0)) {
                            // handle retrieving the list of courses for an instructor
                            String userId = getAndCheckCurrentUser("access instructor courses listings");
                            String courseId = pathSeg1;

                            if (restDebug) {
                                log.info("iclicker REST debugging: courses (u={}, c={})", userId, courseId);
                            }

                            List<Course> courses = logic.getCoursesForInstructorWithStudents(courseId);

                            if (courses.isEmpty()) {
                                throw new SecurityException("No courses found, only instructors can access instructor courses listings");
                            }

                            output = logic.encodeCourses(userId, courses);
                        } else if (StringUtils.equals("students", pathSeg0)) {
                            // handle retrieval of the list of students
                            String courseId = pathSeg1;

                            if (courseId == null) {
                                throw new IllegalArgumentException("valid courseId must be included in the URL /students/{courseId}");
                            }

                            if (restDebug) {
                                log.info("iclicker REST debugging: students (c={})", courseId);
                            }

                            getAndCheckCurrentUser("access student enrollment listings");
                            List<Student> students = logic.getStudentsForCourseWithClickerReg(courseId);
                            Course course = new Course(courseId, courseId);
                            course.setStudents(students);
                            output = logic.encodeEnrollments(course);
                        } else {
                            // UNKNOWN
                            valid = false;
                            output = "Unknown path (" + path + ") specified";
                            status = HttpServletResponse.SC_NOT_FOUND;
                        }
                    } else {
                        // POST
                        if (StringUtils.equals("gradebook", pathSeg0)) {
                            // handle retrieval of the list of students
                            String courseId = pathSeg1;

                            if (courseId == null) {
                                throw new IllegalArgumentException(
                                                "valid courseId must be included in the URL /gradebook/{courseId}");
                            }

                            getAndCheckCurrentUser("upload grades into the gradebook");
                            String xml = getXMLData(req);

                            if (restDebug) {
                                log.info("iclicker REST debugging: gradebook (c={}, xml={})", courseId, xml);
                            }

                            try {
                                Gradebook gradebook = logic.decodeGradebookXML(xml);
                                // process gradebook data
                                List<GradebookItem> results = logic.saveGradebook(gradebook);
                                // generate the output
                                output = logic.encodeSaveGradebookResults(courseId, results);

                                if (output == null) {
                                    // special return, non-XML, no failures in save
                                    res.setStatus(HttpServletResponse.SC_OK);
                                    res.setContentType("text/plain");
                                    output = "True";
                                    res.setContentLength(output.length());
                                    res.getWriter().print(output);

                                    return;
                                } else {
                                    // failures occurred during save
                                    status = HttpServletResponse.SC_OK;
                                }
                            } catch (IllegalArgumentException e) {
                                // invalid XML
                                valid = false;
                                output = "Invalid gradebook XML in request, unable to process: " + xml;
                                log.warn("i>clicker: {}", output, e);
                                status = HttpServletResponse.SC_BAD_REQUEST;
                            }

                        } else if (StringUtils.equals("authenticate", pathSeg0)) {
                            if (restDebug) {
                                log.info("iclicker REST debugging: authenticate");
                            }

                            getAndCheckCurrentUser("authenticate via iclicker");
                            // special return, non-XML
                            res.setStatus(HttpServletResponse.SC_NO_CONTENT);
                            return;

                        } else if (StringUtils.equals("register", pathSeg0)) {
                            getAndCheckCurrentUser("upload registrations data");
                            String xml = getXMLData(req);

                            if (restDebug) {
                                log.info("iclicker REST debugging: register (xml={})", xml);
                            }

                            ClickerRegistration cr = logic.decodeClickerRegistration(xml);
                            String ownerId = cr.getOwnerId();
                            Locale locale = req.getLocale();
                            String message;
                            boolean regStatus = false;

                            try {
                                logic.createRegistration(cr.getClickerId(), ownerId);
                                // valid registration
                                message = logic.getMessage("reg.registered.below.success", locale, cr.getClickerId());
                                regStatus = true;
                            } catch (ClickerIdInvalidException e) {
                                // invalid clicker id;
                                message = logic.getMessage("reg.registered.clickerId.invalid", locale,
                                                cr.getClickerId());
                            } catch (IllegalArgumentException e) {
                                // invalid user id
                                message = "Student not found in the CMS";
                            } catch (ClickerRegisteredException e) {
                                // already registered
                                String key;

                                if (StringUtils.equals(e.ownerId, e.registeredOwnerId)) {
                                    // already registered to this user
                                    key = "reg.registered.below.duplicate";
                                } else {
                                    // already registered to another user
                                    key = "reg.registered.clickerId.duplicate.notowned";
                                }

                                message = logic.getMessage(key, locale, cr.getClickerId());
                            }
                            List<ClickerRegistration> registrations = logic.getClickerRegistrationsForUser(ownerId,
                                            true);
                            output = logic.encodeClickerRegistrationResult(registrations, regStatus, message);

                            if (regStatus) {
                                status = HttpServletResponse.SC_OK;
                            } else {
                                status = HttpServletResponse.SC_BAD_REQUEST;
                            }

                        } else {
                            // UNKNOWN
                            valid = false;
                            output = "Unknown path (" + path + ") specified";
                            status = HttpServletResponse.SC_NOT_FOUND;
                        }
                    }
                }
            } catch (SecurityException e) {
                valid = false;
                String currentUserId = currentUser();

                if (currentUserId == null) {
                    output = "User must be logged in to perform this action: " + e;
                    status = HttpServletResponse.SC_UNAUTHORIZED;
                } else {
                    output = "User (" + currentUserId + ") is not allowed to perform this action: " + e;
                    status = HttpServletResponse.SC_FORBIDDEN;
                }
            } catch (IllegalArgumentException e) {
                valid = false;
                output = "Invalid request: " + e;
                log.warn("i>clicker: {}", output, e);
                status = HttpServletResponse.SC_BAD_REQUEST;
            } catch (Exception e) {
                valid = false;
                output = "Failure occurred: " + e;
                log.warn("i>clicker: {}", output, e);
                status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }

            if (restDebug) {
                String extra = "";

                if (!valid) {
                    extra = ", o=" + output;
                }

                log.info("iclicker REST debugging: DONE (s={}, v={}{})", status, valid, extra);
            }
        }
        if (valid) {
            // send the response
            res.setStatus(HttpServletResponse.SC_OK);
            res.setContentType("application/xml");
            output = XML_HEADER + output;
            res.setContentLength(output.length());
            res.getWriter().print(output);
        } else {
            // error with info about how to do it right
            res.setStatus(status);
            res.setContentType("text/plain");
            // add helpful info to the output
            String msg = "ERROR " + status + ": Invalid request (" + req.getMethod() + " " + req.getContextPath() + req
                            .getServletPath() + path + ")" + "\n\n=INFO========================================================================================\n" + output + "\n\n-HELP----------------------------------------------------------------------------------------\n" + "Valid request paths include the following (without the servlet prefix: " + req
                                            .getContextPath() + req
                                                            .getServletPath() + "):\n" + "POST /authenticate             - authenticate by sending credentials (" + LOGIN + "," + PASSWORD + ") \n" + "                                 return status 204 (valid login) \n" + "POST /verifykey                - check the encoded key is valid and matches the shared key \n" + "                                 return 200 if valid OR 501 if SSO not enabled OR 400/401 if key is bad \n" + "POST /register                 - Add a new clicker registration, return 200 for success or 400 with \n" + "                                 registration response (XML) for failure \n" + "GET  /courses                  - returns the list of courses for the current user (XML) \n" + "GET  /students/{courseId}      - returns the list of student enrollments for the given course (XML) \n" + "                                 or 403 if user is not an instructor in the specified course \n" + "POST /gradebook/{courseId}     - send the gradebook data into the system, returns errors on failure (XML) \n" + "                                 or 'True' if no errors, 400 if the xml is missing or courseid is invalid, \n" + "                                 403 if user is not an instructor in the specified course \n" + "\n" + " - Authenticate by sending credentials (" + LOGIN + "," + PASSWORD + ") or by sending a valid session id (" + SESSION_ID + ") in the request parameters \n" + " -- SSO authentication requires an encoded key (" + SSO_KEY + ") in the request parameters \n" + " -- The response headers will include the sessionId when credentials are valid \n" + " -- Invalid credentials or sessionId will result in a 401 (invalid credentials) or 403 (not authorized) status \n" + " - Use " + COMPENSATE_METHOD + " to override the http method being used (e.g. POST /courses?" + COMPENSATE_METHOD + "=GET will force the method to be a GET despite sending as a POST) \n" + " - Send data as the http request BODY or as a form parameter called " + XML_DATA + " \n" + " - All endpoints return 403 if user is not an instructor \n";
            res.setContentLength(msg.length());
            res.getWriter().print(msg);
        }
    }

    /**
     * Extracts the XML data from the request
     * 
     * @param req the request
     * @return the XML data OR null if none can be found
     */
    private String getXMLData(HttpServletRequest req) {
        String xml = req.getParameter(XML_DATA);

        if (StringUtils.isBlank(xml)) {
            xml = HttpRESTUtils.getRequestBody(req);
        }

        return xml;
    }

    /**
     * Check that the current user is set and that they are an instructor or admin
     * 
     * @param msg the message about what this action is, like "upload grades"
     * @return the current user ID
     * @throws SecurityException is there is no current user or they are not allowed
     */
    private String getAndCheckCurrentUser(String msg) {
        String userId = currentUser();

        if (userId == null) {
            throw new SecurityException("Only logged in users can " + msg);
        }

        if (!isAdmin(userId) && !isInstructor(userId)) {
            throw new SecurityException("Only instructors can " + msg);
        }

        return userId;
    }

    private void handleAuthN(HttpServletRequest req, HttpServletResponse res) {
        HttpAuth auth = HttpRESTUtils.extractRequestAuth(req, LOGIN, PASSWORD); // support basic and params auth
        String sessionId = req.getParameter(SESSION_ID);

        if (auth != null && auth.getLoginName() != null) {
            String ssoKey = req.getParameter(SSO_KEY); // might return a null
            sessionId = getLogic().authenticate(auth.getLoginName(), auth.getPassword(), ssoKey);

            if (sessionId == null) {
                throw new SecurityException("Invalid login credentials (" + LOGIN + "," + PASSWORD + ") supplied");
            }
        } else if (sessionId != null && sessionId.length() > 1) {
            boolean valid = getLogic().getExternalLogic().validateSessionId(sessionId, true);

            if (!valid) {
                throw new SecurityException(
                                "Invalid " + SESSION_ID + " provided, session may have expired, send new login credentials");
            }
        }

        String currentUser = currentUser();

        if (currentUser != null) {
            res.setHeader(SESSION_ID, sessionId);
            res.setHeader("_userId", currentUser());
        }
    }

    private String currentUser() {
        return getLogic().getExternalLogic().getCurrentUserId();
    }

    private boolean isAdmin(String userId) {
        return getLogic().getExternalLogic().isUserAdmin(userId);
    }

    private boolean isInstructor(String userId) {
        return getLogic().getExternalLogic().isUserInstructor(userId);
    }

}
