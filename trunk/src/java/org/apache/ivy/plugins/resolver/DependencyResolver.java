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
package org.apache.ivy.plugins.resolver;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

import org.apache.ivy.core.cache.RepositoryCacheManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.search.ModuleEntry;
import org.apache.ivy.core.search.OrganisationEntry;
import org.apache.ivy.core.search.RevisionEntry;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;

/**
 *
 */
public interface DependencyResolver {
    String getName();

    /**
     * Should only be used by configurator
     * 
     * @param name
     *            the new name of the resolver
     */
    void setName(String name);

    /**
     * Resolve a module by id, getting its module descriptor and resolving the revision if it's a
     * latest one (i.e. a revision uniquely identifying the revision of a module in the current
     * environment - If this revision is not able to identify uniquelely the revision of the module
     * outside of the current environment, then the resolved revision must begin by ##)
     * 
     * @throws ParseException
     */
    ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data)
            throws ParseException;
    
    /**
     * Finds the module descriptor for the specified <tt>DependencyDescriptor</tt>.
     * If this resolver can't find the module descriptor, <tt>null</tt> is returned.
     * 
     * @param dd the dependency descriptor
     * @param data the resolve data
     * @return the module descriptor, or <tt>null</tt>
     */
    ResolvedResource findIvyFileRef(DependencyDescriptor dd, ResolveData data);

    DownloadReport download(Artifact[] artifacts, DownloadOptions options);

    boolean exists(Artifact artifact);

    void publish(Artifact artifact, File src, boolean overwrite) throws IOException;
    
    void beginPublishTransaction(ModuleRevisionId module, boolean overwrite) throws IOException;
    void abortPublishTransaction() throws IOException;
    void commitPublishTransaction() throws IOException;

    /**
     * Reports last resolve failure as Messages
     */
    void reportFailure();

    /**
     * Reports last artifact download failure as Messages
     * 
     * @param art
     */
    void reportFailure(Artifact art);

    // listing methods, enable to know what is available from this resolver
    // the listing methods must only list entries directly
    // available from them, no recursion is needed as long as sub resolvers
    // are registered in ivy too.

    /**
     * List all the values the given token can take if other tokens are set as described in the
     * otherTokenValues map. For instance, if token = "revision" and the map contains
     * "organisation"->"foo" "module"->"bar" The results will be the list of revisions of the module
     * bar from the org foo.
     */
    String[] listTokenValues(String token, Map otherTokenValues);
    
    Map[] listTokenValues(String[] tokens, Map criteria);

    OrganisationEntry[] listOrganisations();

    ModuleEntry[] listModules(OrganisationEntry org);

    RevisionEntry[] listRevisions(ModuleEntry module);
    
    void dumpSettings();
    
    void setSettings(ResolverSettings settings);

    /**
     * Returns the {@link RepositoryCacheManager} used to manage the repository cache associated
     * with this dependency resolver.
     * 
     * @return the {@link RepositoryCacheManager} used to manage the repository cache associated
     *         with this dependency resolver.
     */
    RepositoryCacheManager getRepositoryCacheManager();
}
