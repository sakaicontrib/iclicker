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
package org.sakaiproject.iclicker.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.authz.api.FunctionManager;
import org.sakaiproject.authz.api.Member;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.email.api.EmailService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.iclicker.api.logic.BigRunner;
import org.sakaiproject.iclicker.impl.logic.BigRunnerImpl;
import org.sakaiproject.iclicker.model.Course;
import org.sakaiproject.iclicker.model.Gradebook;
import org.sakaiproject.iclicker.model.GradebookItem;
import org.sakaiproject.iclicker.model.GradebookItemScore;
import org.sakaiproject.iclicker.model.Student;
import org.sakaiproject.javax.PagingPosition;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.service.gradebook.shared.CommentDefinition;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SitePage;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.site.api.SiteService.SelectionType;
import org.sakaiproject.site.api.SiteService.SortType;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.Tool;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.util.ResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Setter;

/**
 * This is the common parts of the logic which is external to our app logic, this provides isolation of the Sakai system from the app so that the integration can be adjusted for future versions or
 * even other systems without requiring rewriting large parts of the code
 */
public abstract class AbstractExternalLogic {

    public static final String SCORE_UPDATE_ERRORS = "ScoreUpdateErrors";
    public static final String POINTS_POSSIBLE_UPDATE_ERRORS = "PointsPossibleUpdateErrors";
    public static final String USER_DOES_NOT_EXIST_ERROR = "UserDoesNotExistError";
    public static final String GENERAL_ERRORS = "GeneralErrors";

    public String serverId = "UNKNOWN_SERVER_ID";

    public static final String NO_LOCATION = "noLocationAvailable";

    private static final Logger log = LoggerFactory.getLogger(AbstractExternalLogic.class);

    @Setter protected AuthzGroupService authzGroupService;

    @Setter private EmailService emailService;
    @Setter protected FunctionManager functionManager;
    @Setter protected GradebookService gradebookService;
    @Setter protected ToolManager toolManager;
    @Setter protected SecurityService securityService;
    @Setter protected ServerConfigurationService serverConfigurationService;
    @Setter protected SessionManager sessionManager;
    @Setter protected SiteService siteService;
    @Setter protected UserDirectoryService userDirectoryService;

    public void init() {
        serverId = getConfigurationSetting(AbstractExternalLogic.SETTING_SERVER_ID, serverId);
    }

    /**
     * @return the current location id of the current user
     */
    public String getCurrentLocationId() {
        String location = null;

        try {
            String context = toolManager.getCurrentPlacement().getContext();
            location = context;
        } catch (Exception e) {
            // sakai failed to get us a location so we can assume we are not inside the portal
            return NO_LOCATION;
        }

        if (location == null) {
            location = NO_LOCATION;
        }

        return location;
    }

    /**
     * @param locationId a unique id which represents the current location of the user (entity reference)
     * @return the title for the context or "--------" (8 hyphens) if none found
     */
    public String getLocationTitle(String locationId) {
        String title = null;

        try {
            Site site = siteService.getSite(locationId);
            title = site.getTitle();
        } catch (IdUnusedException e) {
            log.warn("Cannot get the info about locationId: {}", locationId);
            title = "----------";
        }

        return title;
    }

    /**
     * Attempt to authenticate a user given a login name and password
     * 
     * @param loginname the login name for the user
     * @param password the password for the user
     * @param createSession if true then a session is established for the user and the session ID is returned, otherwise the session is not created
     * @return the user ID if the user was authenticated OR session ID if authenticated and createSession is true OR null if the auth params are invalid
     */
    public String authenticateUser(String loginname, String password, boolean createSession) {
        if (StringUtils.isBlank(loginname)) {
            throw new IllegalArgumentException("loginname cannot be blank");
        }
        if (password == null) {
            password = "";
        }

        User u = userDirectoryService.authenticate(loginname, password);

        if (u == null) {
            // auth failed
            return null;
        } else {
            // auth succeeded
            if (createSession) {
                return makeSession(u);
            } else {
                return u.getId();
            }
        }
    }

    /**
     * Get the user id from the user login name
     * 
     * @param loginname the eid for the user
     * @return the id IF the user exists OR null if they do not
     */
    public String getUserIdFromLoginName(String loginname) {
        String userId;

        try {
            User u = userDirectoryService.getUserByEid(loginname);
            userId = u.getId();
        } catch (UserNotDefinedException e) {
            userId = null;
        }

        return userId;
    }

    /**
     * Start a session for the user, assumption is that the user has already be authenticated in some way
     * 
     * @param loginname the login name for the user
     * @return the new session ID for this user
     */
    public String startUserSession(String loginname) {
        if (StringUtils.isBlank(loginname)) {
            throw new IllegalArgumentException("loginname cannot be blank");
        }

        User u;

        try {
            u = userDirectoryService.getUserByEid(loginname);
        } catch (UserNotDefinedException e) {
            throw new IllegalArgumentException("loginname (" + loginname + ") is invalid, user not found: " + e);
        }

        return makeSession(u);
    }

    /**
     * Start a session for the user, assumption is that the user has already be authenticated in some way
     * 
     * @param userId the internal Sakai id for the user
     * @return the new session ID for this user
     */
    public String startUserSessionById(String userId) {
        if (StringUtils.isBlank(userId)) {
            throw new IllegalArgumentException("userId cannot be blank");
        }

        User u;

        try {
            u = userDirectoryService.getUser(userId);
        } catch (UserNotDefinedException e) {
            throw new IllegalArgumentException("userId (" + userId + ") is invalid, user not found: " + e);
        }

        return makeSession(u);
    }

    protected String makeSession(User u) {
        Session s = sessionManager.startSession();
        s.setUserId(u.getId());
        s.setUserEid(u.getEid());
        s.setActive();
        sessionManager.setCurrentSession(s);
        authzGroupService.refreshUser(u.getId());

        return s.getId();
    }

    /**
     * Validate the session id given and optionally make it the current one
     * 
     * @param sessionId a sakai session id
     * @param makeCurrent if true and the session id is valid then it is made the current one
     * @return true if the session id is valid OR false if not
     */
    public boolean validateSessionId(String sessionId, boolean makeCurrent) {
        try {
            // this also protects us from null pointer where session service is not set or working
            Session s = sessionManager.getSession(sessionId);

            if (s != null && s.getUserId() != null) {
                if (makeCurrent) {
                    s.setActive();
                    sessionManager.setCurrentSession(s);
                    authzGroupService.refreshUser(s.getUserId());
                }
            } else {
                return false;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(
                            "Failure attempting to set sakai session id (" + sessionId + "): " + e.getMessage());
        }

        return true;
    }

    /**
     * @return the current sakai user session id OR null if none
     */
    public String getCurrentSessionId() {
        String sessionId = null;
        Session s = sessionManager.getCurrentSession();

        if (s != null) {
            sessionId = s.getId();
        }

        return sessionId;
    }

    /**
     * @return the current sakai user id (not username)
     */
    public String getCurrentUserId() {
        return sessionManager.getCurrentSessionUserId();
    }

    /**
     * @return the current Locale as Sakai understands it
     */
    public Locale getCurrentLocale() {
        return new ResourceLoader().getLocale();
    }

    /**
     * Get the display name for a user by their unique id
     * 
     * @param userId the current sakai user id (not username)
     * @return display name (probably firstname lastname) or "----------" (10 hyphens) if none found
     */
    public String getUserDisplayName(String userId) {
        String name = null;

        try {
            name = userDirectoryService.getUser(userId).getDisplayName();
        } catch (UserNotDefinedException e) {
            log.warn("Cannot get user displayname for id: {}", userId);
            name = "--------";
        }

        return name;
    }

    public org.sakaiproject.iclicker.model.User getUser(String userId) {
        org.sakaiproject.iclicker.model.User user = null;
        User u = null;

        try {
            u = userDirectoryService.getUser(userId);
        } catch (UserNotDefinedException e) {
            try {
                u = userDirectoryService.getUserByEid(userId);
            } catch (UserNotDefinedException e1) {
                log.warn("Cannot get user for id: {}", userId);
            }
        }
        if (u != null) {
            user = new org.sakaiproject.iclicker.model.User(u.getId(), u.getEid(), u.getDisplayName(), u.getSortName(),
                            u.getEmail());
            user.setFname(u.getFirstName());
            user.setLname(u.getLastName());
        }

        return user;
    }

    /**
     * @return the system email address or null if none available
     */
    public String getNotificationEmail() {
        // attempt to get the email address, if it is not there then we will not send an email
        String emailAddr = serverConfigurationService.getString("portal.error.email",
                        serverConfigurationService.getString("mail.support"));

        if (StringUtils.isBlank(emailAddr)) {
            emailAddr = null;
        }

        return emailAddr;
    }

    /**
     * Sends an email to a group of email addresses
     * 
     * @param fromEmail [OPTIONAL] from email
     * @param toEmails array of emails to send to, must not be null or empty
     * @param subject the email subject
     * @param body the body (content) of the email message
     */
    public void sendEmails(String fromEmail, String[] toEmails, String subject, String body) {
        if (toEmails == null || toEmails.length == 0) {
            throw new IllegalArgumentException("toEmails must be set");
        }
        if (StringUtils.isBlank(fromEmail)) {
            fromEmail = "\"<no-reply@" + serverConfigurationService.getServerName() + ">";
        }

        for (String emailAddr : toEmails) {
            try {
                emailService.send(fromEmail, emailAddr, subject, body, emailAddr, null, null);
            } catch (Exception e) {
                log.warn("Failed to send email to {} ({})", emailAddr, subject, e);
            }
        }
    }

    /**
     * Check if this user has super admin access
     * 
     * @param userId the internal user id (not username)
     * @return true if the user has admin access, false otherwise
     */
    public boolean isUserAdmin(String userId) {
        return securityService.isSuperUser(userId);
    }

    /**
     * Check if a user has a specified permission within a context, primarily a convenience method and passthrough
     * 
     * @param userId the internal user id (not username)
     * @param permission a permission string constant
     * @param locationId a unique id which represents the current location of the user (entity reference)
     * @return true if allowed, false otherwise
     */
    public boolean isUserAllowedInLocation(String userId, String permission, String locationId) {
        return securityService.unlock(userId, permission, locationId);
    }

    /**
     * Get all the courses for the current user, note that this needs to be limited from outside this method for security
     * 
     * @param siteId [OPTIONAL] limit the return to just this one site
     * @param max [OPTIONAL] limit the number of sites returned (default 100)
     * @return the sites (up to max) which the user has instructor access in
     */
    public List<Course> getCoursesForInstructor(String siteId, int max) {
        if (max <= 0) {
            max = 100;
        }

        List<Course> courses = new Vector<>();

        if (StringUtils.isBlank(siteId)) {
            List<Site> sites = getInstructorSites(0, max);

            for (Site site : sites) {
                courses.add(makeCourseFromSite(site));
            }
        } else {
            // return a single site and enrollments
            if (siteService.siteExists(siteId)) {
                if (siteService.allowUpdateSite(siteId) || siteService.allowViewRoster(siteId)) {
                    Site site;

                    try {
                        site = siteService.getSite(siteId);
                        Course c = makeCourseFromSite(site);
                        courses.add(c);
                    } catch (IdUnusedException e) {
                        site = null;
                    }
                }
            }
        }

        return courses;
    }

    private Course makeCourseFromSite(Site site) {
        long createdTime = System.currentTimeMillis() / 1000;

        if (site.getCreatedDate() != null) {
            createdTime = site.getCreatedDate().getTime() / 1000;
        }

        Course c = new Course(site.getId(), site.getTitle(), site.getShortDescription(), createdTime,
                        site.isPublished());

        return c;
    }

    /**
     * @param start the number of the first site to start on (default 1)
     * @param max the maximum number of sites to return (default 100)
     * @return a list of sites for the current instructor
     */
    private List<Site> getInstructorSites(int start, int max) {
        if (max <= 0) {
            // return a max of 100 sites
            max = 100;
        }
        if (start <= 0 || start > max) {
            start = 1;
        }

        List<Site> instSites = new ArrayList<>();
        List<Site> sites = siteService.getSites(SelectionType.UPDATE, null, null, null, SortType.TITLE_ASC, new PagingPosition(start, max));
        for (Site site : sites) {
            // filter out admin sites
            String sid = site.getId();

            if (StringUtils.startsWith(sid, "!") || StringUtils.endsWith(sid, "Admin") || StringUtils.equals(sid, "mercury")) {
                log.debug("Skipping site ({}) for current user in instructor courses", sid);
                continue;
            }

            instSites.add(site);
        }

        return instSites;
    }

    /**
     * Get the listing of students from the site gradebook, uses GB security so safe to call
     * 
     * @param siteId the id of the site to get students from
     * @return the list of Students
     */
    public List<Student> getStudentsForCourse(String siteId) {
        List<Student> students = new ArrayList<>();
        Site site;

        try {
            site = siteService.getSite(siteId);
        } catch (IdUnusedException e1) {
            throw new IllegalArgumentException("No course found with id (" + siteId + ")");
        }

        String siteRef = site.getReference();
        // use the method gradebook uses internally
        List<User> studentUsers = securityService.unlockUsers("section.role.student", siteRef);

        for (User user : studentUsers) {
            Student s = new Student(user.getId(), user.getEid(), user.getDisplayName(), user.getSortName(),
                            user.getEmail());
            s.setFname(user.getFirstName());
            s.setLname(user.getLastName());
            students.add(s);
        }

        return students;
    }

    /**
     * @param userId the current sakai user id (not username)
     * @return true if the user has update access in any sites
     */
    public boolean isUserInstructor(String userId) {
        boolean inst = false;

        // admin never counts as in instructor
        if (!isUserAdmin(userId)) {
            int count = siteService.countSites(SelectionType.UPDATE, null, null, null);
            inst = (count > 0);
        }

        return inst;
    }

    /**
     * Check if the current user in an instructor for the given user id, this will return the first course found in alpha order, will only check the first 100 courses
     * 
     * @param studentUserId the Sakai user id for the student
     * @return the course ID of the course they are an instructor for the student OR null if they are not
     */
    public String isInstructorOfUser(String studentUserId) {
        if (StringUtils.isBlank(studentUserId)) {
            throw new IllegalArgumentException("studentUserId must be set");
        }

        String courseId = null;
        List<Site> sites = getInstructorSites(0, 0);

        if (sites != null && !sites.isEmpty()) {
            if (sites.size() >= 99) {
                // if instructor of 99 or more sites then auto-approved
                courseId = sites.get(0).getId();
            } else {
                for (Site site : sites) {
                    Member member = site.getMember(studentUserId);

                    if (member != null) {
                        courseId = site.getId();
                        break;
                    }
                }
            }
        }

        return courseId;
    }

    /**
     * Gets the gradebook data for a given site, this uses the gradebook security so it is safe for anyone to call
     * 
     * @param siteId a sakai siteId (cannot be group Id)
     * @param gbItemName [OPTIONAL] an item name to fetch from this gradebook (limit to this item only), if null then all items are returned
     */
    // @SuppressWarnings("unchecked")
    public Gradebook getCourseGradebook(String siteId, String gbItemName) {
        // The gradebookUID is the siteId, the gradebookID is a long
        String gbID = siteId;

        if (!gradebookService.isGradebookDefined(gbID)) {
            throw new IllegalArgumentException("No gradebook found for site: " + siteId);
        }

        // verify permissions
        String userId = getCurrentUserId();

        if (userId == null || !siteService.allowUpdateSite(siteId) || !siteService.allowViewRoster(siteId)) {
            throw new SecurityException("User (" + userId + ") cannot access gradebook in site (" + siteId + ")");
        }

        Gradebook gb = new Gradebook(gbID);
        gb.setStudents(getStudentsForCourse(siteId));
        Map<String, String> studentUserIds = new ConcurrentHashMap<>();

        for (Student student : gb.getStudents()) {
            studentUserIds.put(student.getUserId(), student.getUsername());
        }

        ArrayList<String> studentIds = new ArrayList<>(studentUserIds.keySet());

        List<Assignment> gbItems = gradebookService.getAssignments(gbID);
        if (gbItemName == null) {
            for (Assignment assignment : gbItems) {
                GradebookItem gbItem = makeGradebookItemFromAssignment(gbID, assignment, studentUserIds, studentIds);
                gb.getItems().add(gbItem);
            }
        } else {
            Assignment assignment = null;

            for (Assignment gbItem : gbItems) {
                if (StringUtils.equals(gbItemName, gbItem.getName())) {
                    assignment = gbItem;
                    break;
                }
            }

            if (assignment != null) {
                GradebookItem gbItem = makeGradebookItemFromAssignment(gbID, assignment, studentUserIds, studentIds);
                gb.getItems().add(gbItem);
            } else {
                throw new IllegalArgumentException("Invalid gradebook item name (" + gbItemName + "), no item with this name found in course (" + siteId + ")");
            }
        }

        return gb;
    }

    private GradebookItem makeGradebookItemFromAssignment(String gbID, Assignment assignment,
                    Map<String, String> studentUserIds, ArrayList<String> studentIds) {
        // build up the items listing
        GradebookItem gbItem = new GradebookItem(gbID, assignment.getName(), assignment.getPoints(),
                        assignment.getDueDate(), assignment.getExternalAppName(), assignment.isReleased());
        gbItem.setId(assignment.getId().toString());

        for (String studentId : studentIds) {
            String grade = gradebookService.getAssignmentScoreString(gbID, assignment.getId(), studentId);

            if (grade != null) {
                GradebookItemScore score = new GradebookItemScore(assignment.getId().toString(), studentId, grade);
                score.setUsername(studentUserIds.get(studentId));
                CommentDefinition cd = gradebookService.getAssignmentScoreComment(gbID, assignment.getId(), studentId);

                if (cd != null) {
                    score.setComment(cd.getCommentText());
                    score.setRecorded(cd.getDateRecorded());
                    score.setGraderUserId(cd.getGraderUid());
                }

                gbItem.getScores().add(score);
            }
        }

        return gbItem;
    }

    /**
     * Save a gradebook item and optionally the scores within <br/>
     * Scores must have at least the studentId or username AND the grade set
     * 
     * @param gbItem the gradebook item to save, must have at least the gradebookId and name set
     * @return the updated gradebook item and scores, contains any errors that occurred
     * @throws IllegalArgumentException if the assignment is invalid and cannot be saved
     * @throws SecurityException if the current user does not have permissions to save
     */
    public GradebookItem saveGradebookItem(GradebookItem gbItem) {
        if (gbItem == null) {
            throw new IllegalArgumentException("gbItem cannot be null");
        }
        if (StringUtils.isBlank(gbItem.getGradebookId())) {
            throw new IllegalArgumentException("gbItem must have the gradebookId set");
        }
        if (StringUtils.isBlank(gbItem.getName())) {
            throw new IllegalArgumentException("gbItem must have the name set");
        }

        String gradebookUid = gbItem.getGradebookId();
        Assignment assignment = null;

        // find by name
        if (gradebookService.isAssignmentDefined(gradebookUid, gbItem.getName())) {
            List<Assignment> gbItems = gradebookService.getAssignments(gradebookUid);

            for (Assignment gbAssignment : gbItems) {
                if (StringUtils.equals(gbAssignment.getName(), gbItem.getName())) {
                    assignment = gbAssignment;
                    break;
                }
            }
        }

        // now we have the item if it exists
        try {
            // try to save or update it
            if (assignment == null) {
                // no item so create one
                assignment = new Assignment();
                assignment.setExternallyMaintained(false); // cannot modify it later if true
                // assign values
                assignment.setDueDate(gbItem.getDueDate());
                assignment.setExternalAppName(gbItem.getType());
                assignment.setName(gbItem.getName());
                assignment.setPoints(gbItem.getPointsPossible());
                assignment.setReleased(gbItem.isReleased());
                assignment.setId(gradebookService.addAssignment(gradebookUid, assignment));
            } else {
                assignment.setExternallyMaintained(false); // cannot modify it later if true

                // assign new values to existing assignment
                if (gbItem.getDueDate() != null) {
                    assignment.setDueDate(gbItem.getDueDate());
                }

                if (gbItem.getType() != null) {
                    assignment.setExternalAppName(gbItem.getType());
                }

                if (gbItem.getPointsPossible() != null && gbItem.getPointsPossible() >= 0d) {
                    assignment.setPoints(gbItem.getPointsPossible());
                }

                gradebookService.updateAssignment(gradebookUid, assignment.getId(), assignment);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid assignment (" + assignment + "): cannot create: ", e);
        }

        gbItem.setId(assignment.getId() + ""); // avoid NPE
        int errorsCount = 0;

        if (gbItem.getScores() != null && !gbItem.getScores().isEmpty()) {
            // now update scores if there are any to update,
            // this will not remove scores and will only add new ones
            for (GradebookItemScore score : gbItem.getScores()) {
                if (StringUtils.isBlank(score.getUsername()) && StringUtils.isBlank(score.getUserId())) {
                    score.setError(USER_DOES_NOT_EXIST_ERROR); // "USER_MISSING_ERROR";
                    continue;
                }

                String studentId = score.getUserId();

                if (StringUtils.isBlank(studentId)) {
                    // convert student EID to ID
                    try {
                        studentId = userDirectoryService.getUserId(score.getUsername());
                        score.setUserId(studentId);
                    } catch (UserNotDefinedException e) {
                        score.setError(USER_DOES_NOT_EXIST_ERROR);
                        errorsCount++;
                        continue;
                    }
                } else {
                    // validate the student ID
                    try {
                        score.setUsername(userDirectoryService.getUserEid(studentId));
                    } catch (UserNotDefinedException e) {
                        score.setError(USER_DOES_NOT_EXIST_ERROR);
                        errorsCount++;
                        continue;
                    }
                }
                score.assignId(gbItem.getId(), studentId);

                // null/blank scores are not allowed
                if (StringUtils.isBlank(score.getGrade())) {
                    score.setError("NO_SCORE_ERROR");
                    errorsCount++;
                    continue;
                }

                Double dScore;

                try {
                    dScore = Double.valueOf(score.getGrade());
                } catch (NumberFormatException e) {
                    score.setError("SCORE_INVALID");
                    errorsCount++;
                    continue;
                }

                // Student Score should not be greater than the total points possible
                if (dScore > assignment.getPoints()) {
                    score.setError(POINTS_POSSIBLE_UPDATE_ERRORS);
                    errorsCount++;
                    continue;
                }

                try {
                    // check against existing score
                    String currentScore = gradebookService.getAssignmentScoreString(gradebookUid, assignment.getId(), studentId);

                    if (currentScore != null) {
                        Double currentScoreDouble = Double.valueOf(currentScore);

                        if (dScore < currentScoreDouble) {
                            score.setError(SCORE_UPDATE_ERRORS);
                            errorsCount++;
                            continue;
                        }
                    }
                    // null grade deletes the score
                    gradebookService.setAssignmentScoreString(gradebookUid, assignment.getId(), studentId,
                                    Double.toString(dScore), "i>clicker");

                    if (StringUtils.isNotBlank(score.getComment())) {
                        gradebookService.setAssignmentScoreComment(gradebookUid, assignment.getId(), studentId,
                                        score.getComment());
                    }
                } catch (Exception e) {
                    // General errors, caused while performing updates (Tag: generalerrors)
                    log.warn("Failure saving score ({}): ", score, e);
                    score.setError(GENERAL_ERRORS);
                    errorsCount++;
                }
            }

            // put the errors in the item
            if (errorsCount > 0) {
                gbItem.setScoreErrors(new HashMap<String, String>());

                for (GradebookItemScore score : gbItem.getScores()) {
                    gbItem.getScoreErrors().put(score.getId(), score.getError());
                }
            }
        }
        return gbItem;
    }

    /**
     * String type: gets the printable name of this server
     */
    public static final String SETTING_SERVER_NAME = "server.name";

    /**
     * String type: gets the unique id of this server (safe for clustering if used)
     */
    public static final String SETTING_SERVER_ID = "server.cluster.id";

    /**
     * String type: gets the URL to this server
     */
    public static final String SETTING_SERVER_URL = "server.main.URL";

    /**
     * String type: gets the URL to the portal on this server (or just returns the server URL if no portal in use)
     */
    public static final String SETTING_PORTAL_URL = "server.portal.URL";

    /**
     * Boolean type: if true then there will be data preloads and DDL creation, if false then data preloads are disabled (and will cause exceptions if preload data is missing)
     */
    public static final String SETTING_AUTO_DDL = "auto.ddl";

    /**
     * Retrieves settings from the configuration service (sakai.properties)
     * 
     * @param settingName the name of the setting to retrieve, Should be a string name: e.g. auto.ddl, mystuff.config, etc. OR one of the SETTING constants (e.g {@link #SETTING_AUTO_DDL})
     * @param defaultValue a specified default value to return if this setting cannot be found, <b>NOTE:</b> You can set the default value to null but you must specify the class type in parens
     * @return the value of the configuration setting OR the default value if none can be found
     */
    @SuppressWarnings("unchecked")
    public <T> T getConfigurationSetting(String settingName, T defaultValue) {
        T returnValue = defaultValue;

        if (SETTING_SERVER_NAME.equals(settingName)) {
            returnValue = (T) serverConfigurationService.getServerName();
        } else if (SETTING_SERVER_URL.equals(settingName)) {
            returnValue = (T) serverConfigurationService.getServerUrl();
        } else if (SETTING_PORTAL_URL.equals(settingName)) {
            returnValue = (T) serverConfigurationService.getPortalUrl();
        } else if (SETTING_SERVER_ID.equals(settingName)) {
            returnValue = (T) serverConfigurationService.getServerIdInstance();
        } else {
            if (defaultValue == null) {
                returnValue = (T) serverConfigurationService.getString(settingName);
                if ("".equals(returnValue)) {
                    returnValue = null;
                }
            } else {
                if (defaultValue instanceof Number) {
                    int num = ((Number) defaultValue).intValue();
                    int value = serverConfigurationService.getInt(settingName, num);
                    returnValue = (T) Integer.valueOf(value);
                } else if (defaultValue instanceof Boolean) {
                    boolean bool = ((Boolean) defaultValue).booleanValue();
                    boolean value = serverConfigurationService.getBoolean(settingName, bool);
                    returnValue = (T) Boolean.valueOf(value);
                } else if (defaultValue instanceof String) {
                    returnValue = (T) serverConfigurationService.getString(settingName, (String) defaultValue);
                }
            }
        }

        return returnValue;
    }

    // METHODS TO ADD TOOL TO MY WORKSPACES

    static final String[] SPECIAL_USERS = {"admin", "postmaster"};

    /**
     * Set a current user for the current thread, create session if needed
     * 
     * @param userId the userId to set
     */
    public void setCurrentUser(String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        Session currentSession = sessionManager.getCurrentSession();

        if (currentSession == null) {
            // start a session if none is around
            currentSession = sessionManager.startSession(userId);
        }

        currentSession.setUserId(userId);
        currentSession.setActive();
        sessionManager.setCurrentSession(currentSession);
        authzGroupService.refreshUser(userId);
    }

    /**
     * Create runner to add a tool to all My Workspace sites <br/>
     * NOTE: take steps to ensure this cannot be run more than once
     *
     * @param toolId the id of the tool you want to add (ie sakai.iclicker)
     * @param pageTitle the title of the page shown in the site navigation
     * @param toolTitle [OPTIONAL] the title of the tool shown in the main portlet OR null to use default
     * @return an object that can be used to track the process
     * @throws SecurityException if non-admin tries to use this
     * @throws IllegalArgumentException if the toolId is invalid
     * @throws RuntimeException if there is a failure
     */
    public BigRunner makeAddToolToWorkspacesRunner(final String toolId, final String pageTitle,
                    final String toolTitle) {
        // check for admin first
        final String currentUserId = getCurrentUserId();

        if (currentUserId == null || !isUserAdmin(currentUserId)) {
            throw new SecurityException("current user (" + currentUserId + ") cannot push tool into worksites, only the admin can perform this operation");
        }

        // get the tool
        final Tool tool = toolManager.getTool(toolId);

        if (tool == null) {
            throw new IllegalArgumentException("toolId (" + toolId + ") is invalid, could not find tool");
        }

        BigRunner r = new BigRunnerImpl(BigRunner.RUNNER_TYPE_ADD) {
            public void run() {
                try {
                    // force current thread to current admin user
                    setCurrentUser(currentUserId);

                    // Get all user Ids
                    List<String> allUserIds = new ArrayList<>();
                    List<User> users = userDirectoryService.getUsers();

                    for (Iterator<User> i = users.iterator(); i.hasNext();) {
                        User user = (User) i.next();
                        allUserIds.add(user.getId());
                    }

                    // remove special users
                    List<String> specialUserIds = Arrays.asList(SPECIAL_USERS);
                    allUserIds.removeAll(specialUserIds);
                    this.setTotal(allUserIds.size());

                    // now add a page to each site, and the tool to that page
                    for (String userId : allUserIds) {
                        String myWorkspaceId = siteService.getUserSiteId(userId);
                        Site siteEdit;

                        try {
                            siteEdit = siteService.getSite(myWorkspaceId);
                        } catch (IdUnusedException e) {
                            // no workspace for user, this is ok
                            this.incrementCompleted();
                            continue;
                        }

                        // check if we already have the tool
                        ToolConfiguration tc = siteEdit.getToolForCommonId(toolId);

                        if (tc == null) {
                            // no tool so add the page and the tool
                            SitePage sitePageEdit = siteEdit.addPage();
                            sitePageEdit.setTitle(pageTitle);
                            sitePageEdit.setLayout(0);
                            int numPages = siteEdit.getPages().size();
                            sitePageEdit.setPosition(numPages - 1);
                            sitePageEdit.setPopup(false);

                            tc = sitePageEdit.addTool();
                            tc.setTool(toolId, tool);
                            tc.setTitle(toolTitle);

                            siteService.save(siteEdit);
                            log.info("Tool ({}) added to site ({}) for user ({})", toolId, siteEdit.getId(), userId);
                        }

                        this.incrementCompleted();
                    }
                } catch (Exception e) {
                    log.error("Failed trying to add ({}) to my workspaces: ", toolId, e);
                    throw new RuntimeException("Failed trying to add (" + toolId + ") to my workspaces: " + e, e);
                } finally {
                    log.info("Completed long running process: {}", this);
                    setComplete();
                }
            }
        };

        return r;
    }

    /**
     * Create runner to remove the tool from all my workspaces <br/>
     * NOTE: take steps to ensure this cannot be run more than once
     * 
     * @param toolId the id of the tool you want to add (ie sakai.iclicker)
     * @return an object that can be used to track the process
     * @throws SecurityException if non-admin tries to use this
     * @throws IllegalArgumentException if the toolId is invalid
     * @throws RuntimeException if there is a failure
     */
    public BigRunner makeRemoveToolFromWorkspacesRunner(final String toolId) {
        // check for admin first
        final String currentUserId = getCurrentUserId();

        if (currentUserId == null || !isUserAdmin(currentUserId)) {
            throw new SecurityException(
                            "current user (" + currentUserId + ") cannot push tool into worksites, only the admin can perform this operation");
        }

        // get the tool
        final Tool tool = toolManager.getTool(toolId);

        if (tool == null) {
            throw new IllegalArgumentException("toolId (" + toolId + ") is invalid, could not find tool");
        }

        BigRunner r = new BigRunnerImpl(BigRunner.RUNNER_TYPE_REMOVE) {
            public void run() {
                try {
                    // force current thread to current admin user
                    setCurrentUser(currentUserId);

                    // Get all user Ids
                    List<String> allUserIds = new ArrayList<String>();
                    List<User> users = userDirectoryService.getUsers();

                    for (Iterator<User> i = users.iterator(); i.hasNext();) {
                        User user = (User) i.next();
                        allUserIds.add(user.getId());
                    }

                    // remove special users
                    List<String> specialUserIds = Arrays.asList(SPECIAL_USERS);
                    allUserIds.removeAll(specialUserIds);
                    this.setTotal(allUserIds.size());

                    // now add a page to each site, and the tool to that page
                    for (String userId : allUserIds) {
                        String myWorkspaceId = siteService.getUserSiteId(userId);
                        Site siteEdit;

                        try {
                            siteEdit = siteService.getSite(myWorkspaceId);
                        } catch (IdUnusedException e) {
                            // no workspace for user, this is ok
                            this.incrementCompleted();
                            continue;
                        }

                        // check if we have the tool
                        ToolConfiguration tc = siteEdit.getToolForCommonId(toolId);

                        if (tc != null) {
                            // remove it
                            siteEdit.removePage(tc.getContainingPage());
                            siteService.save(siteEdit);
                            log.info("Tool ({}) removed from site ({}) for user ({})", toolId, siteEdit.getId(),
                                            userId);
                        }

                        this.incrementCompleted();
                    }
                } catch (Exception e) {
                    log.error("Failed trying to remove ({}) from my workspaces: ", toolId, e);
                    setFailure(e);
                    throw new RuntimeException("Failed trying to remove (" + toolId + ") from my workspaces: " + e, e);
                } finally {
                    log.info("Completed long running process: {}", this);
                    setComplete();
                }
            }
        };

        return r;
    }

}
