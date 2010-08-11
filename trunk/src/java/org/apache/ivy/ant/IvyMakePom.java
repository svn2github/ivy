/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.ant;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.plugins.parser.m2.PomModuleDescriptorWriter;
import org.apache.ivy.plugins.parser.m2.PomWriterOptions;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParser;
import org.apache.ivy.util.FileUtil;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Convert an ivy file to a pom
 */
public class IvyMakePom extends IvyTask {
    public class Mapping {
        private String conf;
        private String scope;
        
        public String getConf() {
            return conf;
        }
        public void setConf(String conf) {
            this.conf = conf;
        }
        public String getScope() {
            return scope;
        }
        public void setScope(String scope) {
            this.scope = scope;
        }
    }

    public class Dependency {
        private String group = null;
        private String artifact = null;
        private String version = null;
        private String scope = null;
        private boolean optional = false;
        
        public String getGroup() {
            return group;
        }
        public void setGroup(String group) {
            this.group = group;
        }
        public String getArtifact() {
            return artifact;
        }
        public void setArtifact(String artifact) {
            this.artifact = artifact;
        }
        public String getVersion() {
            return version;
        }
        public void setVersion(String version) {
            this.version = version;
        }
        public String getScope() {
            return scope;
        }
        public void setScope(String scope) {
            this.scope = scope;
        }
        public boolean getOptional() {
            return optional;
        }
        public void setOptional(boolean optional) {
            this.optional = optional;
        }      
    }
            
    private String artifactName;
            
    private String artifactPackaging;

    private File pomFile = null;

    private File headerFile = null;
    
    private boolean printIvyInfo = true;

    private String conf;
   
    private File ivyFile = null;

    private Collection mappings = new ArrayList();
    
    private Collection dependencies = new ArrayList();

    public File getPomFile() {
        return pomFile;
    }

    public void setPomFile(File file) {
        pomFile = file;
    }

    public File getIvyFile() {
        return ivyFile;
    }

    public void setIvyFile(File ivyFile) {
        this.ivyFile = ivyFile;
    }

    public File getHeaderFile() {
        return headerFile;
    }

    public void setHeaderFile(File headerFile) {
        this.headerFile = headerFile;
    }
    
    public boolean isPrintIvyInfo() {
        return printIvyInfo;
    }

    public void setPrintIvyInfo(boolean printIvyInfo) {
        this.printIvyInfo = printIvyInfo;
    }

    public String getConf() {
        return conf;
    }
    
    public void setConf(String conf) {
        this.conf = conf;
    }
    
    public String getArtifactName() {
        return artifactName;
    }

    public void setArtifactName(String artifactName) {
        this.artifactName = artifactName;
    }

    public String getArtifactPackaging() {
        return artifactPackaging;
    }

    public void setArtifactPackaging(String artifactPackaging) {
        this.artifactPackaging = artifactPackaging;
    }

    public Mapping createMapping() {
        Mapping mapping = new Mapping();
        this.mappings.add(mapping);
        return mapping;
    }
    
    public Dependency createDependency() {
        Dependency dependency = new Dependency();
        this.dependencies.add(dependency);
        return dependency;
    }
    
    public void doExecute() throws BuildException {
        try {
            if (ivyFile == null) {
                throw new BuildException("source ivy file is required for makepom task");
            }
            if (pomFile == null) {
                throw new BuildException("destination pom file is required for makepom task");
            }
            ModuleDescriptor md = XmlModuleDescriptorParser.getInstance().parseDescriptor(
                getSettings(), ivyFile.toURI().toURL(), false);
            PomModuleDescriptorWriter.write(md, pomFile, getPomWriterOptions());
        } catch (MalformedURLException e) {
            throw new BuildException("unable to convert given ivy file to url: " + ivyFile + ": "
                    + e, e);
        } catch (ParseException e) {
            log(e.getMessage(), Project.MSG_ERR);
            throw new BuildException("syntax errors in ivy file " + ivyFile + ": " + e, e);
        } catch (Exception e) {
            throw new BuildException("impossible convert given ivy file to pom file: " + e
                    + " from=" + ivyFile + " to=" + pomFile, e);
        }
    }
    
    private PomWriterOptions getPomWriterOptions() throws IOException {
        PomWriterOptions options = new PomWriterOptions();
        options.setConfs(splitConfs(conf))
               .setArtifactName(getArtifactName())
               .setArtifactPackaging(getArtifactPackaging())
               .setPrintIvyInfo(isPrintIvyInfo())
               .setExtraDependencies(getDependencies());
        
        if (!mappings.isEmpty()) {
            options.setMapping(new PomWriterOptions.ConfigurationScopeMapping(getMappingsMap()));
        }
        
        if (headerFile != null) {
            options.setLicenseHeader(FileUtil.readEntirely(getHeaderFile()));
        }
        
        return options;
    }

    private Map getMappingsMap() {
        Map mappingsMap = new HashMap();
        for (Iterator iter = mappings.iterator(); iter.hasNext();) {
            Mapping mapping = (Mapping) iter.next();
            mappingsMap.put(mapping.getConf(), mapping.getScope());
        }
        return mappingsMap;
    }
    
    private List getDependencies() {
        List result = new ArrayList();
        for (Iterator iter = dependencies.iterator(); iter.hasNext();) {
            Dependency dependency = (Dependency) iter.next();
            result.add(new PomWriterOptions.ExtraDependency(dependency.getGroup(),
                    dependency.getArtifact(), dependency.getVersion(), dependency.getScope(),
                    dependency.getOptional()));
        }
        return result;
    }
}
