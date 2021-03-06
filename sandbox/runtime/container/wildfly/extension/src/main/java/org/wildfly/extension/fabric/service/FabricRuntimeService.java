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
package org.wildfly.extension.fabric.service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.jboss.as.server.ServerEnvironment;
import org.jboss.gravia.Constants;
import org.wildfly.extension.gravia.service.RuntimeService;

/**
 * Service responsible for creating and managing the life-cycle of the gravia subsystem.
 *
 * @since 19-Apr-2013
 */
public final class FabricRuntimeService extends RuntimeService {

    @Override
    protected Properties initialProperties() {
        Properties properties = super.initialProperties();
        
        // Fabric8 integration properties
        ServerEnvironment serverEnv = getServerEnvironment();
        File configurationDir = serverEnv.getServerConfigurationDir();
        Path configsPath = Paths.get(configurationDir.toURI()).resolve(Paths.get("fabric8", "etc"));
        properties.setProperty(Constants.RUNTIME_CONFIGURATIONS_DIR, configsPath.toString());
        
        // [TODO] Derive port from wildfly config
        // https://issues.jboss.org/browse/FABRIC-762
        properties.setProperty("org.osgi.service.http.port", "8080");
        
        return properties;
    }
}
