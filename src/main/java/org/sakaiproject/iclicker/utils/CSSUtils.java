/**********************************************************************************
 * $URL: https://source.sakaiproject.org/svn/portal/trunk/portal-util/util/src/java/org/sakaiproject/portal/util/CSSUtils.java $
 * $Id: CSSUtils.java 127916 2013-07-29 17:48:03Z csev@umich.edu $
 ***********************************************************************************
 *
 * Copyright (c) 2013 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.opensource.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.iclicker.utils;

import org.sakaiproject.site.api.Site;
import org.apache.commons.lang.StringUtils;
import org.sakaiproject.component.cover.ServerConfigurationService;

/* NOTE: copied from portal/portal-util/util/src/java/org/sakaiproject/portal/util/CSSUtils.java
 * until Sakai 10 is the oldest thing we need to support
 *      <dependency>
            <groupId>org.sakaiproject.portal</groupId>
            <artifactId>sakai-portal-util</artifactId>
            <version>10</version>
        </dependency>
 */
public final class CSSUtils {

    private static final String PORTAL_SKIN_DEFAULT_PROPERTY = "skin.default";
    private static final String PORTAL_SKIN_MORPHEUS_PREFIX_DEFAULT = "morpheus";

    /**
     * Returns a URL for the tool_base.css suitable for putting in an href= field.
     *
     * @return <code>cssToolBase</code> URL for the tool_base.css
     */
    public static String getCssToolBase() {
        String skinRepo = ServerConfigurationService.getString("skin.repo");
        String cssToolBase = skinRepo + "/tool_base.css";

        return cssToolBase;
    }

    /**
     * Captures the (yes) overly complex rules for the skin folder naming convention
     *
     * @param <code>skinFolder</code> The folder where the skins are to be found.
     * @return <code>skinFolder</code> The adjusted folder where the skins can be found.
     */
    public static String adjustCssSkinFolder(String skinFolder) {
        if (skinFolder == null) {
            skinFolder = ServerConfigurationService.getString(PORTAL_SKIN_DEFAULT_PROPERTY);

            if (skinFolder == null) {
                skinFolder = ""; // Not likely - not good if it happens
            }
        }

        String prefix = ServerConfigurationService.getString(PORTAL_SKIN_DEFAULT_PROPERTY, "");

        if (prefix == null) {
            prefix = "";
        }

        String templates = ServerConfigurationService.getString("portal.templates", PORTAL_SKIN_MORPHEUS_PREFIX_DEFAULT);

        if (StringUtils.equals(PORTAL_SKIN_MORPHEUS_PREFIX_DEFAULT, templates) && !StringUtils.startsWith(skinFolder, prefix)) {
            skinFolder = prefix + skinFolder;
        }

        return skinFolder;
    }

    /**
     * Returns a URL for the tool.css suitable for putting in an href= field.
     *
     * @param <code>skinFolder</code> where the tool.css skin lives for this site.
     * @return <code>cssToolSkin</code> URL for the tool.css
     */
    public static String getCssToolSkin(String skinFolder) {
        skinFolder = adjustCssSkinFolder(skinFolder);
        String skinRepo = ServerConfigurationService.getString("skin.repo");
        String cssToolSkin = skinRepo + "/" + skinFolder + "/tool.css";

        return cssToolSkin;
    }

    /**
     * Returns a URL for the tool.css suitable for putting in an href= field.
     *
     * @param <code>site</code> The site for this tool.
     * @return <code>cssToolSkin</code> URL for the tool.css
     */
    public static String getCssToolSkin(Site site) {
        String skinFolder = site.getSkin();
        return getCssToolSkin(skinFolder);
    }

    private CSSUtils() {
    }

}
