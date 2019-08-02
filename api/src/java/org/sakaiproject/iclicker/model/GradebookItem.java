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
package org.sakaiproject.iclicker.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This represents an item in a gradebook and the associated scores.
 */
@Data
@NoArgsConstructor
public class GradebookItem {
    private String id;
    private String gradebookId;
    private String name;
    private Double pointsPossible;
    private Date dueDate;
    private String type = "internal"; // this is the externalAppName or "internal"
    private boolean released = false;
    private List<GradebookItemScore> scores = new ArrayList<>();

    /**
     * map of score id -> error_key,
     * these are recorded when this item is saved
     * (errors also recorded in the scores themselves).
     */
    private Map<String, String> scoreErrors;

    /**
     * Constructor.
     *
     * @param gradebookId the gradebook ID
     * @param name the name
     */
    public GradebookItem(String gradebookId, String name) {
        this(gradebookId, name, null, null, null, false);
    }

    /**
     * Constructor.
     *
     * @param gradebookId the gradebook ID
     * @param name the name
     * @param pointsPossible the points possible
     * @param dueDate the due date
     * @param type the type
     * @param released is this a released item?
     */
    public GradebookItem(
            String gradebookId, String name, Double pointsPossible, Date dueDate, String type, boolean released) {
        if (StringUtils.isBlank(gradebookId)) {
            throw new IllegalArgumentException("gradebookId must be set");
        }

        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("name must be set");
        }

        this.gradebookId = gradebookId;
        this.name = name;

        if (pointsPossible != null && pointsPossible > 0d) {
            this.pointsPossible = Double.valueOf(pointsPossible.doubleValue());
        }

        if (dueDate != null) {
            this.dueDate = new Date(dueDate.getTime());
        }

        if (StringUtils.isNotBlank(type)) {
            this.type = type;
        }

        this.released = released;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((gradebookId == null) ? 0 : gradebookId.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        GradebookItem other = (GradebookItem) obj;

        if (gradebookId == null) {
            if (other.gradebookId != null) {
                return false;
            }
        } else if (!StringUtils.equals(gradebookId, other.gradebookId)) {
            return false;
        }

        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!StringUtils.equals(name, other.name)) {
            return false;
        }

        return true;
    }

}
