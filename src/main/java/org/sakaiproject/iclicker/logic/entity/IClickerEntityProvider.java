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

package org.sakaiproject.iclicker.logic.entity;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.azeckoski.reflectutils.ConversionUtils;
import org.azeckoski.reflectutils.transcoders.JSONTranscoder;
import org.azeckoski.reflectutils.transcoders.XMLTranscoder;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.EntityView.Method;
import org.sakaiproject.entitybroker.entityprovider.EntityProvider;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.CollectionResolvable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Createable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Inputable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Outputable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Redirectable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.RequestAware;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Resolvable;
import org.sakaiproject.entitybroker.entityprovider.extension.ActionReturn;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.entityprovider.extension.RequestGetter;
import org.sakaiproject.entitybroker.entityprovider.search.Restriction;
import org.sakaiproject.entitybroker.entityprovider.search.Search;
import org.sakaiproject.entitybroker.exception.EntityException;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;
import org.sakaiproject.iclicker.exception.ClickerIdInvalidException;
import org.sakaiproject.iclicker.logic.ExternalLogic;
import org.sakaiproject.iclicker.logic.IClickerLogic;
import org.sakaiproject.iclicker.model.Course;
import org.sakaiproject.iclicker.model.Gradebook;
import org.sakaiproject.iclicker.model.GradebookItem;
import org.sakaiproject.iclicker.model.GradebookItemScore;
import org.sakaiproject.iclicker.model.Student;
import org.sakaiproject.iclicker.model.dao.ClickerRegistration;

import lombok.Setter;

/**
 * iClicker REST handler <br/>
 * This handles all the RESTful endpoint and data feed generation for the application
 */
public class IClickerEntityProvider extends AbstractEntityProvider
                implements EntityProvider, Createable, Resolvable, CollectionResolvable, Outputable, Inputable,
                Describeable, ActionsExecutable, Redirectable, RequestAware {

    public static final String PREFIX = "iclicker";
    public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n";

    @Setter private IClickerLogic logic;
    @Setter private ExternalLogic externalLogic;
    @Setter protected RequestGetter requestGetter;

    // custom actions

    @EntityCustomAction(action = "courses", viewKey = EntityView.VIEW_LIST)
    public ActionReturn getInstructorCourses(EntityView view) {
        String userId = externalLogic.getCurrentUserId();

        if (userId == null) {
            throw new SecurityException("Only logged in users can access instructor courses listings");
        }

        String courseId = view.getPathSegment(2);
        List<Course> courses = logic.getCoursesForInstructorWithStudents(courseId);

        if (courses.isEmpty()) {
            throw new SecurityException("Only instructors can access instructor courses listings");
        }

        Object toEncode;

        if (courseId != null) {
            // get a single course and the students
            toEncode = courses.get(0);
        } else {
            toEncode = courses;
        }

        // do the encoding manually
        ActionReturn ar;

        if (Formats.JSON.equals(view.getFormat())) {
            String out = JSONTranscoder.makeJSON(toEncode, null, false, false, false, 6, null);
            ar = new ActionReturn(out);
            ar.encoding = Formats.JSON_MIME_TYPE;
        } else {
            // using the iclicker specific XML format
            String out = XML_HEADER + logic.encodeCourses(userId, courses);
            ar = new ActionReturn(out);
            ar.encoding = Formats.XML_MIME_TYPE;
        }

        return ar;
    }

    @EntityCustomAction(action = "students", viewKey = EntityView.VIEW_LIST)
    public ActionReturn getCourseStudents(EntityView view) {
        String courseId = view.getPathSegment(2);

        if (courseId == null) {
            throw new IllegalArgumentException(
                            "valid courseId must be included in the URL /iclicker/students/{courseId}");
        }

        String userId = externalLogic.getCurrentUserId();

        if (userId == null) {
            throw new SecurityException("Only logged in users can access student enrollment listings");
        }

        if (!externalLogic.isUserAdmin(userId) && !externalLogic.isUserInstructor(userId)) {
            throw new SecurityException("Only instructors can access course students listing");
        }

        List<Student> students = logic.getStudentsForCourseWithClickerReg(courseId);
        // do the encoding manually
        ActionReturn ar;

        if (StringUtils.equals(Formats.JSON, view.getFormat())) {
            String out = JSONTranscoder.makeJSON(students, null, false, false, false, 4, null);
            ar = new ActionReturn(out);
            ar.encoding = Formats.JSON_MIME_TYPE;
        } else {
            // use the iclicker XML format
            Course course = new Course(courseId, courseId);
            course.setStudents(students);
            String out = XML_HEADER + logic.encodeEnrollments(course);
            ar = new ActionReturn(out);
            ar.encoding = Formats.XML_MIME_TYPE;
        }

        return ar;
    }

    @EntityCustomAction(action = "gradebook", viewKey = EntityView.VIEW_LIST)
    public ActionReturn getCourseGradebook(EntityView view) {
        String courseId = view.getPathSegment(2);

        if (courseId == null) {
            throw new IllegalArgumentException(
                            "valid courseId must be included in the URL /iclicker/gradebook/{courseId}");
        }

        String userId = externalLogic.getCurrentUserId();

        if (userId == null) {
            throw new SecurityException("Only logged in users can access instructor courses listings");
        }

        if (!externalLogic.isUserAdmin(userId) && !externalLogic.isUserInstructor(userId)) {
            throw new SecurityException("Only instructors can access course gradebook");
        }

        Gradebook gradebook = logic.getCourseGradebook(courseId, null);
        // do the encoding manually
        ActionReturn ar;

        if (StringUtils.equals(Formats.JSON, view.getFormat())) {
            String out = JSONTranscoder.makeJSON(gradebook, null, false, false, false, 6, null);
            ar = new ActionReturn(out);
            ar.encoding = Formats.JSON_MIME_TYPE;
        } else {
            // use the iclicker XML format
            String out = XML_HEADER + logic.encodeGradebook(gradebook);
            ar = new ActionReturn(out);
            ar.encoding = Formats.XML_MIME_TYPE;
        }

        return ar;
    }

    @EntityCustomAction(action = "validate", viewKey = EntityView.VIEW_NEW)
    public ActionReturn validateClickerId(EntityView view) {
        String clickerId = view.getPathSegment(2);

        if (clickerId == null) {
            throw new IllegalArgumentException(
                            "valid clickerId must be included in the URL /iclicker/validate/{clickerId}");
        }

        HashMap<String, Object> m = new HashMap<>();

        try {
            clickerId = logic.validateClickerId(clickerId);
            m.put("valid", true);
        } catch (ClickerIdInvalidException e) {
            m.put("failure", e.failure.name());
            m.put("valid", false);
        }

        m.put("clickerId", clickerId);
        // do the encoding manually
        ActionReturn ar;

        if (StringUtils.equals(Formats.JSON, view.getFormat())) {
            String out = JSONTranscoder.makeJSON(m, null, false, false, false, 2, null);
            ar = new ActionReturn(out);
            ar.encoding = Formats.JSON_MIME_TYPE;
        } else {
            String out = XML_HEADER + XMLTranscoder.makeXML(m, "validate", null, false, false, false, false, 2, null);
            ar = new ActionReturn(out);
            ar.encoding = Formats.XML_MIME_TYPE;
        }

        if (m.containsKey("failure")) {
            // failure also indicated with a 404
            ar.responseCode = HttpServletResponse.SC_BAD_REQUEST;
        }

        return ar;
    }

    @EntityCustomAction(action = "activate", viewKey = EntityView.VIEW_EDIT)
    public int activateClicker(EntityView view, EntityReference ref) {
        ClickerRegistration cr = findClickerRegistration(ref.getId());

        if (cr == null) {
            throw new EntityException("Could not find clicker by id (" + ref.getId() + ")", "/activate",
                            HttpServletResponse.SC_BAD_REQUEST);
        }

        logic.setRegistrationActive(cr.getId(), true);

        return HttpServletResponse.SC_NO_CONTENT;
    }

    @EntityCustomAction(action = "deactivate", viewKey = EntityView.VIEW_EDIT)
    public int deactivateClicker(EntityView view, EntityReference ref) {
        ClickerRegistration cr = findClickerRegistration(ref.getId());

        if (cr == null) {
            throw new EntityException("Could not find clicker by id (" + ref.getId() + ")", "/deactivate",
                            HttpServletResponse.SC_BAD_REQUEST);
        }

        logic.setRegistrationActive(cr.getId(), false);

        return HttpServletResponse.SC_NO_CONTENT;
    }

    @EntityCustomAction(action = "gradeitem", viewKey = "")
    @SuppressWarnings("unchecked")
    public ActionReturn handleGradeItem(EntityView view) {
        String courseId = view.getPathSegment(2);

        if (courseId == null) {
            throw new IllegalArgumentException(
                            "valid courseId must be included in the URL /iclicker/gradeitem/{courseId}");
        }

        String userId = externalLogic.getCurrentUserId();

        if (userId == null) {
            throw new SecurityException("Only logged in users can access instructor courses listings");
        }

        if (!externalLogic.isUserAdmin(userId) && !externalLogic.isUserInstructor(userId)) {
            throw new SecurityException("Only instructors can access course gradebook");
        }

        GradebookItem gbItemOut;

        if (StringUtils.equalsIgnoreCase(Method.GET.toString(), view.getMethod())) {
            String gradeItemName = view.getPathSegment(3);

            if (gradeItemName == null) {
                throw new IllegalArgumentException(
                                "valid gbItemName must be included in the URL /iclicker/gradeitem/{courseId}/{gradeItemName}");
            }

            Gradebook gb = logic.getCourseGradebook(courseId, gradeItemName);
            gbItemOut = gb.getItems().get(0);
        } else if (StringUtils.equalsIgnoreCase(Method.POST.toString(), view.getMethod()) || StringUtils
                        .equalsIgnoreCase(Method.PUT.toString(), view.getMethod())) {
            ServletRequest request = requestGetter.getRequest();

            if (request == null) {
                throw new IllegalStateException("Cannot get request to read data from");
            }

            String inputData;

            try {
                inputData = readerToString(request.getReader());
            } catch (IOException e) {
                throw new RuntimeException("Failed to read the data from the request: " + e);
            }

            if (StringUtils.isBlank(inputData)) {
                throw new IllegalStateException("Must include the grade item and grades data for input (sent nothing)");
            }

            Map<String, Object> input;

            if (StringUtils.equals(Formats.JSON, view.getFormat())) {
                input = new JSONTranscoder().decode(inputData);
            } else {
                input = new XMLTranscoder().decode(inputData);
            }

            // loop through and get the data out and put it into a gradeitem
            ConversionUtils cvu = ConversionUtils.getInstance();
            String gbItemName = (String) input.get("name");
            GradebookItem gbItemIn = new GradebookItem(courseId, gbItemName);
            gbItemIn.setPointsPossible(cvu.convert(input.get("pointsPossible"), Double.class));
            gbItemIn.setDueDate(cvu.convert(input.get("dueDate"), Date.class));
            List<Object> scores = cvu.convert(input.get("scores"), List.class);

            if (scores != null) {
                for (Object o : scores) {
                    GradebookItemScore score = cvu.convert(o, GradebookItemScore.class);
                    gbItemIn.getScores().add(score);
                }
            }

            gbItemOut = logic.saveGradebookItem(gbItemIn);
        } else {
            throw new EntityException("Method (" + view.getMethod() + ") not supported", "iclicker/gradeitem",
                            HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }

        // do the encoding manually
        ActionReturn ar;
        if (StringUtils.equals(Formats.JSON, view.getFormat())) {
            String out = JSONTranscoder.makeJSON(gbItemOut, null, false, false, false, 6, null);
            ar = new ActionReturn(out);
            ar.encoding = Formats.JSON_MIME_TYPE;
        } else {
            String out = XML_HEADER + XMLTranscoder.makeXML(gbItemOut, "gradeitem", null, false, false, false, false, 6,
                            null);
            ar = new ActionReturn(out);
            ar.encoding = Formats.XML_MIME_TYPE;
        }

        return ar;
    }

    // standard methods

    public String getEntityPrefix() {
        return PREFIX;
    }

    public Object getEntity(EntityReference ref) {
        if (ref.getId() == null) {
            return new ClickerRegistration();
        }

        ClickerRegistration entity;
        entity = findClickerRegistration(ref.getId());

        if (entity != null) {
            return entity;
        }

        throw new IllegalArgumentException("Invalid id:" + ref.getId());
    }

    /**
     * Find the clicker by either the internal id or the clicker id
     * 
     * @param id the internal id or the clicker id
     * @return the {@link ClickerRegistration} OR null if none can be found
     */
    private ClickerRegistration findClickerRegistration(String id) {
        ClickerRegistration entity;
        Long lid = Long.getLong(id);

        if (lid == null || lid > 999999) {
            // lookup by clickerId
            entity = logic.getItemByClickerId(id);
        } else {
            // lookup by lid
            entity = logic.getItemById(lid);
        }

        return entity;
    }

    public List<?> getEntities(EntityReference ref, Search search) {
        String locationId = null;
        String userId = getCurrentUser();

        if (search != null) {
            Restriction restriction = search.getRestrictionByProperty("locationId");

            if (restriction != null) {
                locationId = restriction.property;
            } else {
                restriction = search.getRestrictionByProperty("siteId");
                if (restriction != null) {
                    locationId = restriction.property;
                }
            }

            if (externalLogic.isUserAdmin(getCurrentUser())) {
                // allowed to lookup for other users
                Restriction r = search.getRestrictionByProperty("userId");

                if (r != null) {
                    userId = r.property;
                }
            }
        }

        return logic.getAllVisibleItems(userId, locationId);
    }

    public String createEntity(EntityReference ref, Object entity) {
        return createEntity(ref, entity, null);
    }

    public String createEntity(EntityReference ref, Object entity, Map<String, Object> params) {
        String clickerId = ref.getId();

        if (clickerId == null) {
            clickerId = (String) params.get("clickerId");
        }

        return registerClicker(clickerId);
    }

    private String registerClicker(String clickerId) {
        try {
            ClickerRegistration cr = logic.createRegistration(clickerId);
            return cr.getClickerId();
        } catch (ClickerIdInvalidException e) {
            throw new EntityException("Invalid clickerId (" + clickerId + "): " + e, "/register",
                            HttpServletResponse.SC_BAD_REQUEST);
        } catch (IllegalStateException e) {
            throw new EntityException("ClickerId is already registered (" + clickerId + "): " + e, "/register",
                            HttpServletResponse.SC_CONFLICT);
        }
    }

    public Object getSampleEntity() {
        return new ClickerRegistration();
    }

    private String getCurrentUser() {
        return externalLogic.getCurrentUserId();
    }

    public String[] getHandledOutputFormats() {
        return new String[] {Formats.XML, Formats.JSON};
    }

    public String[] getHandledInputFormats() {
        return new String[] {Formats.HTML, Formats.XML, Formats.JSON};
    }

    public static String readerToString(BufferedReader br) {
        StringBuilder sb = new StringBuilder();
        String line;

        try {
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to get data from stream: " + e.getMessage(), e);
        } finally {
            try {
                br.close();
            } catch (IOException e) {
            }
        }

        return sb.toString();
    }

}
