/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.tooling.archetype;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.xml.transform.stream.StreamSource;

import io.fabric8.utils.JMXUtils;
import io.fabric8.utils.Strings;
import io.fabric8.tooling.archetype.catalog.Archetype;
import io.fabric8.tooling.archetype.catalog.Archetypes;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "io.fabric8.tooling.archetype.ArchetypeService", label = "Fabric8 Archetype Service",
    description = "Generates projects from Maven Archetypes.",
    policy = ConfigurationPolicy.OPTIONAL, immediate = true, metatype = true)
@Service(ArchetypeService.class)
public class ArchetypeServiceImpl implements ArchetypeService {

    private static final transient Logger LOG = LoggerFactory.getLogger(ArchetypeServiceImpl.class);
    public static ObjectName OBJECT_NAME;

    static {
        try {
            OBJECT_NAME = new ObjectName("io.fabric8:type=ArchetypeService");
        } catch (MalformedObjectNameException e) {
            // ignore
        }
    }

    // we want the archetypes sorted
    private Map<String, Archetype> archetypes = new TreeMap<>();

    @Reference(referenceInterface = MBeanServer.class, bind = "bindMBeanServer", unbind = "unbindMBeanServer")
    private MBeanServer mbeanServer;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Activate
    void activate(Map<String, ?> configuration, ComponentContext componentContext) throws Exception {
        if (mbeanServer != null) {
            JMXUtils.registerMBean(this, mbeanServer, OBJECT_NAME);
        }

        URL catalog = componentContext.getBundleContext().getBundle().getResource("archetype-catalog.xml");
        Archetypes archetypes = (Archetypes) Archetypes.newUnmarshaller().unmarshal(new StreamSource(catalog.openStream()));
        for (Archetype arch : archetypes.getArchetypes()) {
            this.archetypes.put(String.format("%s:%s:%s", arch.groupId, arch.artifactId, arch.version), arch);
        }
    }

    @Deactivate
    void deactivate() throws Exception {
        if (mbeanServer != null) {
            JMXUtils.unregisterMBean(mbeanServer, OBJECT_NAME);
        }
    }

    @Override
    public List<String[]> listArchetypeGAVs() {
        assertValid();
        List<String[]> answer = new ArrayList<String[]>(this.archetypes.size());
        for (Archetype a : this.archetypes.values()) {
            answer.add(new String[] { a.groupId, a.artifactId, a.version });
        }

        return answer;
    }

    private void assertValid() {
        // TODO - no longer using core fabric DS component...
    }

    @Override
    public Collection<Archetype> listArchetypes() {
        assertValid();
        return this.archetypes.values();
    }

    @Override
    public List<Archetype> listArchetypes(String filter, boolean artifactIdOnly) {
        List<Archetype> answer = new ArrayList<Archetype>();

        if (Strings.isNullOrBlank(filter)) {
            answer.addAll(listArchetypes());
            return answer;
        }

        filter = filter.toLowerCase();

        for (Archetype archetype : archetypes.values()) {
            if (artifactIdOnly && archetype.artifactId.toLowerCase().contains(filter)) {
                answer.add(archetype);
            } else {
                if (archetype.groupId.toLowerCase().contains(filter)
                    || archetype.artifactId.toLowerCase().contains(filter)
                    || archetype.version.toLowerCase().contains(filter)) {
                    answer.add(archetype);
                }
            }
        }
        return answer;
    }

    @Override
    public Archetype getArchetype(String gav) {
        return this.archetypes.get(gav);
    }

    @Override
    public Archetype getArchetypeByArtifact(String artifactId) {
        for (Archetype archetype : archetypes.values()) {
            if (archetype.artifactId.equals(artifactId)) {
                return archetype;
            }
        }
        return null;
    }

    void bindMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

    void unbindMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = null;
    }

}
