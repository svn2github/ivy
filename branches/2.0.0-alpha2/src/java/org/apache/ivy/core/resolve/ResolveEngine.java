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
package org.apache.ivy.core.resolve;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.Set;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.event.download.PrepareDownloadEvent;
import org.apache.ivy.core.event.resolve.EndResolveEvent;
import org.apache.ivy.core.event.resolve.StartResolveEvent;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ConfigurationResolveReport;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.IvyNodeEviction.EvictionData;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.sort.SortEngine;
import org.apache.ivy.plugins.conflict.ConflictManager;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.apache.ivy.plugins.resolver.CacheResolver;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.filter.Filter;

/**
 * The resolve engine which is the core of the dependency resolution mechanism used in Ivy. It
 * features several resolve methods, some very simple, like {@link #resolve(File)} and
 * {@link #resolve(URL)} which allow to simply resolve dependencies of a single module descriptor,
 * or more complete one, like the {@link #resolve(ModuleDescriptor, ResolveOptions)} which allows to
 * provide options to the resolution engine.
 * 
 * @see ResolveOptions
 */
public class ResolveEngine {
    private IvySettings settings;

    private EventManager eventManager;

    private SortEngine sortEngine;

    private Set fetchedSet = new HashSet();

    private DependencyResolver dictatorResolver;

    /**
     * Constructs a ResolveEngine.
     * 
     * @param settings
     *            the settings to use to configure the engine. Must not be null.
     * @param eventManager
     *            the event manager to use to send events about the resolution process. Must not be
     *            null.
     * @param sortEngine
     *            the sort engine to use to sort modules before producing the dependency resolution
     *            report. Must not be null.
     */
    public ResolveEngine(IvySettings settings, EventManager eventManager, SortEngine sortEngine) {
        this.settings = settings;
        this.eventManager = eventManager;
        this.sortEngine = sortEngine;
    }

    /**
     * Returns the currently configured dictator resolver, which when non null is used in place of
     * any specified resolver in the {@link IvySettings}
     * 
     * @return the currently configured dictator resolver, may be null.
     */
    public DependencyResolver getDictatorResolver() {
        return dictatorResolver;
    }

    /**
     * Sets a dictator resolver, which is used in place of regular dependency resolver for
     * subsequent dependency resolution by this engine.
     * 
     * @param dictatorResolver
     *            the dictator resolver to use in this engine, null if regular settings should used
     */
    public void setDictatorResolver(DependencyResolver dictatorResolver) {
        this.dictatorResolver = dictatorResolver;
        settings.setDictatorResolver(dictatorResolver);
    }

    public ResolveReport resolve(File ivySource) throws ParseException, IOException {
        return resolve(ivySource.toURL());
    }

    public ResolveReport resolve(URL ivySource) throws ParseException, IOException {
        return resolve(ivySource, new ResolveOptions());
    }

    /**
     * Resolves the module identified by the given mrid with its dependencies if transitive is set
     * to true.
     */
    public ResolveReport resolve(final ModuleRevisionId mrid, ResolveOptions options,
            boolean changing) throws ParseException, IOException {
        DefaultModuleDescriptor md;

        String[] confs = options.getConfs();
        if (confs.length == 1 && confs[0].equals("*")) {
            // create new resolve options because this is a different resolve than the real resolve
            // (which will be a resolve of a newCallerInstance module)
            ResolvedModuleRevision rmr = findModule(mrid, new ResolveOptions(options));
            if (rmr == null) {
                md = DefaultModuleDescriptor.newCallerInstance(mrid, confs, options.isTransitive(),
                    changing);
                return new ResolveReport(md, options.getResolveId()) {
                    public boolean hasError() {
                        return true;
                    }

                    public List getProblemMessages() {
                        return Arrays.asList(new String[] {"module not found: " + mrid});
                    }
                };
            } else {
                confs = rmr.getDescriptor().getConfigurationsNames();
                md = DefaultModuleDescriptor.newCallerInstance(ModuleRevisionId.newInstance(mrid,
                    rmr.getId().getRevision()), confs, options.isTransitive(), changing);
            }
        } else {
            md = DefaultModuleDescriptor.newCallerInstance(mrid, confs, options.isTransitive(),
                changing);
        }

        return resolve(md, options);
    }

    /**
     * Resolve dependencies of a module described by an ivy file. Note: the method signature is way
     * too long, we should use a class to store the settings of the resolve.
     */
    public ResolveReport resolve(URL ivySource, ResolveOptions options) throws ParseException,
            IOException {
        URLResource res = new URLResource(ivySource);
        ModuleDescriptorParser parser = ModuleDescriptorParserRegistry.getInstance().getParser(res);
        Message.verbose("using " + parser + " to parse " + ivySource);
        ModuleDescriptor md = parser.parseDescriptor(settings, ivySource, options.isValidate());
        String revision = options.getRevision();
        if (revision == null && md.getResolvedModuleRevisionId().getRevision() == null) {
            revision = Ivy.getWorkingRevision();
        }
        if (revision != null) {
            md.setResolvedModuleRevisionId(ModuleRevisionId.newInstance(md.getModuleRevisionId(),
                revision));
        }

        return resolve(md, options);
    }

    /**
     * Resolve dependencies of a module described by a module descriptor Note: the method signature
     * is way too long, we should use a class to store the settings of the resolve.
     */
    public ResolveReport resolve(ModuleDescriptor md, ResolveOptions options)
            throws ParseException, IOException {
        DependencyResolver oldDictator = getDictatorResolver();
        if (options.isUseCacheOnly()) {
            setDictatorResolver(new CacheResolver(settings));
        }
        try {
            CacheManager cacheManager = options.getCache();
            if (cacheManager == null) { // ensure that a cache is configured
                cacheManager = IvyContext.getContext().getCacheManager();
                options.setCache(cacheManager);
            } else {
                IvyContext.getContext().setCache(cacheManager.getCache());
            }

            String[] confs = options.getConfs();
            if (confs.length == 1 && confs[0].equals("*")) {
                confs = md.getConfigurationsNames();
            }
            options.setConfs(confs);

            if (options.getResolveId() == null) {
                options.setResolveId(ResolveOptions.getDefaultResolveId(md));
            }

            eventManager.fireIvyEvent(new StartResolveEvent(md, confs));

            long start = System.currentTimeMillis();
            Message.info(":: resolving dependencies :: " + md.getResolvedModuleRevisionId()
                    + (options.isTransitive() ? "" : " [not transitive]"));
            Message.info("\tconfs: " + Arrays.asList(confs));
            Message.verbose("\tvalidate = " + options.isValidate());
            ResolveReport report = new ResolveReport(md, options.getResolveId());

            // resolve dependencies
            IvyNode[] dependencies = getDependencies(md, options, report);
            report.setDependencies(Arrays.asList(dependencies), options.getArtifactFilter());

            // produce resolved ivy file and ivy properties in cache
            File ivyFileInCache = cacheManager.getResolvedIvyFileInCache(md
                    .getResolvedModuleRevisionId());
            md.toIvyFile(ivyFileInCache);

            // we store the resolved dependencies revisions and statuses per asked dependency
            // revision id,
            // for direct dependencies only.
            // this is used by the deliver task to resolve dynamic revisions to static ones
            File ivyPropertiesInCache = cacheManager.getResolvedIvyPropertiesInCache(md
                    .getResolvedModuleRevisionId());
            Properties props = new Properties();
            if (dependencies.length > 0) {
                IvyNode root = dependencies[0].getRoot();
                for (int i = 0; i < dependencies.length; i++) {
                    if (!dependencies[i].isCompletelyEvicted() && !dependencies[i].hasProblem()) {
                        DependencyDescriptor dd = dependencies[i].getDependencyDescriptor(root);
                        if (dd != null) {
                            String rev = dependencies[i].getResolvedId().getRevision();
                            String status = dependencies[i].getDescriptor().getStatus();
                            props.put(dd.getDependencyRevisionId().encodeToString(), rev + " "
                                    + status);
                        }
                    }
                }
            }
            FileOutputStream out = new FileOutputStream(ivyPropertiesInCache);
            props.store(out, md.getResolvedModuleRevisionId() + " resolved revisions");
            out.close();
            Message.verbose("\tresolved ivy file produced in " + ivyFileInCache);

            report.setResolveTime(System.currentTimeMillis() - start);

            if (options.isDownload()) {
                Message.verbose(":: downloading artifacts ::");

                downloadArtifacts(report, cacheManager, options.isUseOrigin(), options
                        .getArtifactFilter());
            }

            if (options.isOutputReport()) {
                outputReport(report, cacheManager.getCache());
            }

            eventManager.fireIvyEvent(new EndResolveEvent(md, confs, report));
            return report;
        } finally {
            setDictatorResolver(oldDictator);
        }
    }

    public void outputReport(ResolveReport report, File cache) {
        Message.info(":: resolution report ::");
        report.setProblemMessages(Message.getProblems());
        // output report
        report.output(settings.getReportOutputters(), cache);

        Message.verbose("\tresolve done (" + report.getResolveTime() + "ms resolve - "
                + report.getDownloadTime() + "ms download)");
        Message.sumupProblems();
    }

    public void downloadArtifacts(ResolveReport report, CacheManager cacheManager,
            boolean useOrigin, Filter artifactFilter) {
        long start = System.currentTimeMillis();
        IvyNode[] dependencies = (IvyNode[]) report.getDependencies().toArray(
            new IvyNode[report.getDependencies().size()]);

        eventManager.fireIvyEvent(new PrepareDownloadEvent((Artifact[]) report.getArtifacts()
                .toArray(new Artifact[report.getArtifacts().size()])));

        for (int i = 0; i < dependencies.length; i++) {
            checkInterrupted();
            // download artifacts required in all asked configurations
            if (!dependencies[i].isCompletelyEvicted() && !dependencies[i].hasProblem()) {
                DependencyResolver resolver = dependencies[i].getModuleRevision()
                        .getArtifactResolver();
                Artifact[] selectedArtifacts = dependencies[i].getSelectedArtifacts(artifactFilter);
                DownloadReport dReport = resolver.download(selectedArtifacts, new DownloadOptions(
                        settings, cacheManager, eventManager, useOrigin));
                ArtifactDownloadReport[] adrs = dReport.getArtifactsReports();
                for (int j = 0; j < adrs.length; j++) {
                    if (adrs[j].getDownloadStatus() == DownloadStatus.FAILED) {
                        Message.warn("\t[NOT FOUND  ] " + adrs[j].getArtifact());
                        resolver.reportFailure(adrs[j].getArtifact());
                    }
                }
                // update concerned reports
                String[] dconfs = dependencies[i].getRootModuleConfigurations();
                for (int j = 0; j < dconfs.length; j++) {
                    // the report itself is responsible to take into account only
                    // artifacts required in its corresponding configuration
                    // (as described by the Dependency object)
                    if (dependencies[i].isEvicted(dconfs[j])) {
                        report.getConfigurationReport(dconfs[j]).addDependency(dependencies[i]);
                    } else {
                        report.getConfigurationReport(dconfs[j]).addDependency(dependencies[i],
                            dReport);
                    }
                }
            }
        }
        report.setDownloadTime(System.currentTimeMillis() - start);
    }

    /**
     * Download an artifact to the cache. Not used internally, useful especially for IDE plugins
     * needing to download artifact one by one (for source or javadoc artifact, for instance).
     * Downloaded artifact file can be accessed using getArchiveFileInCache method. It is possible
     * to track the progression of the download using classical ivy progress monitoring feature (see
     * addTransferListener).
     * 
     * @param artifact
     *            the artifact to download
     * @param cacheManager
     *            the cacheManager to use.
     * @return a report concerning the download
     */
    public ArtifactDownloadReport download(Artifact artifact, CacheManager cacheManager,
            boolean useOrigin) {
        DependencyResolver resolver = settings.getResolver(artifact.getModuleRevisionId()
                .getModuleId());
        DownloadReport r = resolver.download(new Artifact[] {artifact}, new DownloadOptions(
                settings, cacheManager, eventManager, useOrigin));
        return r.getArtifactReport(artifact);
    }

    /**
     * Resolve the dependencies of a module without downloading corresponding artifacts. The module
     * to resolve is given by its ivy file URL. This method requires appropriate configuration of
     * the ivy instance, especially resolvers.
     * 
     * @param ivySource
     *            url of the ivy file to use for dependency resolving
     * @param confs
     *            an array of configuration names to resolve - must not be null nor empty
     * @param cache
     *            the cache to use - default cache is used if null
     * @param date
     *            the date to which resolution must be done - may be null
     * @return an array of the resolved dependencies
     * @throws ParseException
     *             if a parsing problem occured in the ivy file
     * @throws IOException
     *             if an IO problem was raised during ivy file parsing
     */
    public IvyNode[] getDependencies(URL ivySource, ResolveOptions options) throws ParseException,
            IOException {
        return getDependencies(ModuleDescriptorParserRegistry.getInstance().parseDescriptor(
            settings, ivySource, options.isValidate()), options, null);
    }

    /**
     * Resolve the dependencies of a module without downloading corresponding artifacts. The module
     * to resolve is given by its module descriptor.This method requires appropriate configuration
     * of the ivy instance, especially resolvers.
     * 
     * @param md
     *            the descriptor of the module for which we want to get dependencies - must not be
     *            null
     * @param options
     *            the resolve options to use to resolve the dependencies
     * @param report
     *            a resolve report to fill during resolution - may be null
     * @return an array of the resolved Dependencies
     */
    public IvyNode[] getDependencies(ModuleDescriptor md, ResolveOptions options,
            ResolveReport report) {
        if (md == null) {
            throw new NullPointerException("module descriptor must not be null");
        }
        CacheManager cacheManager = options.getCache();
        if (cacheManager == null) { // ensure that a cache is configured
            cacheManager = IvyContext.getContext().getCacheManager();
            options.setCache(cacheManager);
        } else {
            IvyContext.getContext().setCache(cacheManager.getCache());
        }

        String[] confs = options.getConfs();
        if (confs.length == 1 && confs[0].equals("*")) {
            confs = md.getConfigurationsNames();
        }
        options.setConfs(confs);

        Date reportDate = new Date();
        ResolveData data = new ResolveData(this, options);
        IvyNode rootNode = new IvyNode(data, md);

        for (int i = 0; i < confs.length; i++) {
            if (confs[i] == null) {
                throw new NullPointerException("null conf not allowed: confs where: "
                        + Arrays.asList(confs));
            }

            Message.verbose("resolving dependencies for configuration '" + confs[i] + "'");
            // for each configuration we clear the cache of what's been fetched
            fetchedSet.clear();

            Configuration configuration = md.getConfiguration(confs[i]);
            if (configuration == null) {
                Message.error("asked configuration not found in " + md.getModuleRevisionId() + ": "
                        + confs[i]);
            } else {
                ConfigurationResolveReport confReport = null;
                if (report != null) {
                    confReport = report.getConfigurationReport(confs[i]);
                    if (confReport == null) {
                        confReport = new ConfigurationResolveReport(this, md, confs[i], reportDate,
                                options);
                        report.addReport(confs[i], confReport);
                    }
                }
                // we reuse the same resolve data with a new report for each conf
                data.setReport(confReport);

                // update the root module conf we are about to fetch
                VisitNode root = new VisitNode(data, rootNode, null, confs[i], null);
                root.setRequestedConf(confs[i]);
                rootNode.updateConfsToFetch(Collections.singleton(confs[i]));

                // go fetch !
                fetchDependencies(root, confs[i], false);

                // clean data
                for (Iterator iter = data.getNodes().iterator(); iter.hasNext();) {
                    IvyNode dep = (IvyNode) iter.next();
                    dep.clean();
                }
            }
        }

        // prune and reverse sort fectched dependencies
        Collection dependencies = new LinkedHashSet(data.getNodes().size()); // use a Set to
        // avoids duplicates
        for (Iterator iter = data.getNodes().iterator(); iter.hasNext();) {
            IvyNode dep = (IvyNode) iter.next();
            if (dep != null) {
                dependencies.add(dep);
            }
        }
        List sortedDependencies = sortEngine.sortNodes(dependencies);
        Collections.reverse(sortedDependencies);

        // handle transitive eviction now:
        // if a module has been evicted then all its dependencies required
        // only by it should be evicted too. Since nodes are now sorted from the more dependent to
        // the less one, we can traverse the list and check only the direct parent and not all
        // the ancestors
        for (ListIterator iter = sortedDependencies.listIterator(); iter.hasNext();) {
            IvyNode node = (IvyNode) iter.next();
            if (!node.isCompletelyEvicted()) {
                for (int i = 0; i < confs.length; i++) {
                    IvyNodeCallers.Caller[] callers = node.getCallers(confs[i]);
                    if (settings.debugConflictResolution()) {
                        Message.debug("checking if " + node.getId()
                                + " is transitively evicted in " + confs[i]);
                    }
                    boolean allEvicted = callers.length > 0;
                    for (int j = 0; j < callers.length; j++) {
                        if (callers[j].getModuleRevisionId().equals(md.getModuleRevisionId())) {
                            // the caller is the root module itself, it can't be evicted
                            allEvicted = false;
                            break;
                        } else {
                            IvyNode callerNode = data.getNode(callers[j].getModuleRevisionId());
                            if (callerNode == null) {
                                Message.warn("ivy internal error: no node found for "
                                        + callers[j].getModuleRevisionId() + ": looked in "
                                        + data.getNodeIds() + " and root module id was "
                                        + md.getModuleRevisionId());
                            } else if (!callerNode.isEvicted(confs[i])) {
                                allEvicted = false;
                                break;
                            } else {
                                if (settings.debugConflictResolution()) {
                                    Message.debug("caller " + callerNode.getId() + " of "
                                            + node.getId() + " is evicted");
                                }
                            }
                        }
                    }
                    if (allEvicted) {
                        Message.verbose("all callers are evicted for " + node + ": evicting too");
                        node.markEvicted(confs[i], null, null, null);
                    } else {
                        if (settings.debugConflictResolution()) {
                            Message.debug(node.getId()
                                  + " isn't transitively evicted, at least one caller was" 
                                  + " not evicted");
                        }
                    }
                }
            }
        }

        return (IvyNode[]) dependencies.toArray(new IvyNode[dependencies.size()]);
    }

    private void fetchDependencies(VisitNode node, String conf, boolean shouldBePublic) {
        checkInterrupted();
        long start = System.currentTimeMillis();
        if (node.getParent() != null) {
            Message.verbose("== resolving dependencies " + node.getParent().getId() + "->"
                    + node.getId() + " [" + node.getParentConf() + "->" + conf + "]");
        } else {
            Message.verbose("== resolving dependencies for " + node.getId() + " [" + conf + "]");
        }
        resolveConflict(node);

        if (node.loadData(conf, shouldBePublic)) {
            resolveConflict(node);
            if (!node.isEvicted()) {
                String[] confs = node.getRealConfs(conf);
                for (int i = 0; i < confs.length; i++) {
                    doFetchDependencies(node, confs[i]);
                }
            }
        } else if (!node.hasProblem()) {
            // the node has not been loaded but hasn't problem: it was already loaded
            // => we just have to update its dependencies data
            if (!node.isEvicted()) {
                String[] confs = node.getRealConfs(conf);
                for (int i = 0; i < confs.length; i++) {
                    doFetchDependencies(node, confs[i]);
                }
            }
        }
        if (node.isEvicted()) {
            // update selected nodes with confs asked in evicted one
            EvictionData ed = node.getEvictedData();
            if (ed.getSelected() != null) {
                for (Iterator iter = ed.getSelected().iterator(); iter.hasNext();) {
                    IvyNode selected = (IvyNode) iter.next();
                    if (!selected.isLoaded()) {
                        // the node is not yet loaded, we can simply update its set of
                        // configurations to fetch
                        selected.updateConfsToFetch(Collections.singleton(conf));
                    } else {
                        // the node has already been loaded, we must fetch its dependencies in the
                        // required conf
                        fetchDependencies(node.gotoNode(selected), conf, true);
                    }
                }
            }
        }
        if (settings.debugConflictResolution()) {
            Message.debug(node.getId() + " => dependencies resolved in " + conf + " ("
                    + (System.currentTimeMillis() - start) + "ms)");
        }
    }

    private void doFetchDependencies(VisitNode node, String conf) {
        Configuration c = node.getConfiguration(conf);
        if (c == null) {
            Message.warn("configuration not found '" + conf + "' in " + node.getResolvedId()
                    + ": ignoring");
            if (node.getParent() != null) {
                Message.warn("it was required from " + node.getParent().getResolvedId());
            }
            return;
        }
        // we handle the case where the asked configuration extends others:
        // we have to first fetch the extended configurations

        // first we check if this is the actual requested conf (not an extended one)
        boolean requestedConfSet = false;
        if (node.getRequestedConf() == null) {
            node.setRequestedConf(conf);
            requestedConfSet = true;
        }
        // now let's recurse in extended confs
        String[] extendedConfs = c.getExtends();
        if (extendedConfs.length > 0) {
            node.updateConfsToFetch(Arrays.asList(extendedConfs));
        }
        for (int i = 0; i < extendedConfs.length; i++) {
            fetchDependencies(node, extendedConfs[i], false);
        }

        // now we can actually resolve this configuration dependencies
        DependencyDescriptor dd = node.getDependencyDescriptor();
        if (!isDependenciesFetched(node.getNode(), conf) && (dd == null || node.isTransitive())) {
            Collection dependencies = node.getDependencies(conf);
            for (Iterator iter = dependencies.iterator(); iter.hasNext();) {
                VisitNode dep = (VisitNode) iter.next();
                dep.useRealNode(); // the node may have been resolved to another real one while
                // resolving other deps
                if (dep.isCircular()) {
                    continue;
                }
                String[] confs = dep.getRequiredConfigurations(node, conf);
                for (int i = 0; i < confs.length; i++) {
                    fetchDependencies(dep, confs[i], true);
                }
                // if there are still confs to fetch (usually because they have
                // been updated when evicting another module), we fetch them now
                confs = dep.getConfsToFetch();
                for (int i = 0; i < confs.length; i++) {
                    fetchDependencies(dep, confs[i], true);
                }
            }
        }
        // we have finiched with this configuration, if it was the original requested conf
        // we can clean it now
        if (requestedConfSet) {
            node.setRequestedConf(null);
        }

    }

    /**
     * Returns true if we've already fetched the dependencies for this node and configuration
     * 
     * @param node
     *            node to check
     * @param conf
     *            configuration to check
     * @return true if we've already fetched this dependency
     */
    private boolean isDependenciesFetched(IvyNode node, String conf) {
        ModuleId moduleId = node.getModuleId();
        ModuleRevisionId moduleRevisionId = node.getResolvedId();
        String key = moduleId.getOrganisation() + "|" + moduleId.getName() + "|"
                + moduleRevisionId.getRevision() + "|" + conf;
        if (fetchedSet.contains(key)) {
            return true;
        }
        fetchedSet.add(key);
        return false;
    }

    private void resolveConflict(VisitNode node) {
        resolveConflict(node, node.getParent(), Collections.EMPTY_SET);
    }

    /**
     * Resolves conflict for the given node in the given ancestor. This method do conflict
     * resolution in ancestor parents recursively, unless not necessary.
     * 
     * @param node
     *            the node for which conflict resolution should be done
     * @param ancestor
     *            the ancestor in which the conflict resolution should be done
     * @param toevict
     *            a collection of IvyNode to evict (as computed by conflict resolution in
     *            descendants of ancestor)
     * @return true if conflict resolution has been done, false it can't be done yet
     */
    private boolean resolveConflict(VisitNode node, VisitNode ancestor, Collection toevict) {
        if (ancestor == null || node == ancestor) {
            return true;
        }
        // check if job is not already done
        if (checkConflictSolvedEvicted(node, ancestor)) {
            // job is done and node is evicted, nothing to do
            return true;
        }
        if (checkConflictSolvedSelected(node, ancestor)) {
            // job is done and node is selected, nothing to do for this ancestor, but we still have
            // to check higher levels, for which conflict resolution might have been impossible
            // before
            if (resolveConflict(node, ancestor.getParent(), toevict)) {
                // now that conflict resolution is ok in ancestors
                // we just have to check if the node wasn't previously evicted in root ancestor
                EvictionData evictionData = node.getEvictionDataInRoot(node.getRootModuleConf(),
                    ancestor);
                if (evictionData != null) {
                    // node has been previously evicted in an ancestor: we mark it as evicted
                    if (settings.debugConflictResolution()) {
                        Message.debug(node + " was previously evicted in root module conf "
                                + node.getRootModuleConf());
                    }

                    node.markEvicted(evictionData);
                    if (settings.debugConflictResolution()) {
                        Message.debug("evicting " + node + " by " + evictionData);
                    }
                }
                return true;
            } else {
                return false;
            }
        }

        // compute conflicts
        Collection resolvedNodes = new HashSet(ancestor.getNode().getResolvedNodes(
            node.getModuleId(), node.getRootModuleConf()));
        resolvedNodes.addAll(ancestor.getNode().getPendingConflicts(node.getRootModuleConf(),
            node.getModuleId()));
        Collection conflicts = computeConflicts(node, ancestor, toevict, resolvedNodes);

        if (settings.debugConflictResolution()) {
            Message.debug("found conflicting revisions for " + node + " in " + ancestor + ": "
                    + conflicts);
        }

        ConflictManager conflictManager = ancestor.getNode().getConflictManager(node.getModuleId());
        Collection resolved = conflictManager.resolveConflicts(ancestor.getNode(), conflicts);

        if (resolved == null) {
            if (settings.debugConflictResolution()) {
                Message.debug("impossible to resolve conflicts for " + node + " in " + ancestor
                        + " yet");
                Message.debug("setting all nodes as pending conflicts for later conflict" 
                    + " resolution: " + conflicts);
            }
            ancestor.getNode().setPendingConflicts(node.getModuleId(), node.getRootModuleConf(),
                conflicts);
            return false;
        }

        if (settings.debugConflictResolution()) {
            Message.debug("selected revisions for " + node + " in " + ancestor + ": " + resolved);
        }
        if (resolved.contains(node.getNode())) {
            // node has been selected for the current parent

            // handle previously selected nodes that are now evicted by this new node
            toevict = resolvedNodes;
            toevict.removeAll(resolved);

            for (Iterator iter = toevict.iterator(); iter.hasNext();) {
                IvyNode te = (IvyNode) iter.next();
                te.markEvicted(node.getRootModuleConf(), ancestor.getNode(), conflictManager,
                    resolved);

                if (settings.debugConflictResolution()) {
                    Message.debug("evicting " + te + " by "
                            + te.getEvictedData(node.getRootModuleConf()));
                }
            }

            // it's very important to update resolved and evicted nodes BEFORE recompute parent call
            // to allow it to recompute its resolved collection with correct data
            // if necessary
            ancestor.getNode().setResolvedNodes(node.getModuleId(), node.getRootModuleConf(),
                resolved);

            Collection evicted = new HashSet(ancestor.getNode().getEvictedNodes(node.getModuleId(),
                node.getRootModuleConf()));
            evicted.removeAll(resolved);
            evicted.addAll(toevict);
            ancestor.getNode().setEvictedNodes(node.getModuleId(), node.getRootModuleConf(),
                evicted);
            ancestor.getNode().setPendingConflicts(node.getModuleId(), node.getRootModuleConf(),
                Collections.EMPTY_SET);

            return resolveConflict(node, ancestor.getParent(), toevict);
        } else {
            // node has been evicted for the current parent
            if (resolved.isEmpty()) {
                if (settings.debugConflictResolution()) {
                    Message.verbose("conflict manager '" + conflictManager
                            + "' evicted all revisions among " + conflicts);
                }
            }

            // it's time to update parent resolved and evicted with what was found

            Collection evicted = new HashSet(ancestor.getNode().getEvictedNodes(node.getModuleId(),
                node.getRootModuleConf()));
            toevict.removeAll(resolved);
            evicted.removeAll(resolved);
            evicted.addAll(toevict);
            evicted.add(node.getNode());
            ancestor.getNode().setEvictedNodes(node.getModuleId(), node.getRootModuleConf(),
                evicted);
            ancestor.getNode().setPendingConflicts(node.getModuleId(), node.getRootModuleConf(),
                Collections.EMPTY_SET);

            node.markEvicted(ancestor, conflictManager, resolved);
            if (settings.debugConflictResolution()) {
                Message.debug("evicting " + node + " by " + node.getEvictedData());
            }

            // if resolved changed we have to go up in the graph
            Collection prevResolved = ancestor.getNode().getResolvedNodes(node.getModuleId(),
                node.getRootModuleConf());
            boolean solved = true;
            if (!prevResolved.equals(resolved)) {
                ancestor.getNode().setResolvedNodes(node.getModuleId(), node.getRootModuleConf(),
                    resolved);
                for (Iterator iter = resolved.iterator(); iter.hasNext();) {
                    IvyNode sel = (IvyNode) iter.next();
                    if (!prevResolved.contains(sel)) {
                        solved &= resolveConflict(node.gotoNode(sel), ancestor.getParent(),
                            toevict);
                    }
                }
            }
            return solved;
        }
    }

    private Collection computeConflicts(VisitNode node, VisitNode ancestor, Collection toevict,
            Collection resolvedNodes) {
        Collection conflicts = new HashSet();
        conflicts.add(node.getNode());
        if (resolvedNodes.removeAll(toevict)) {
            // parent.resolved(node.mid) is not up to date:
            // recompute resolved from all sub nodes
            Collection deps = ancestor.getNode().getDependencies(node.getRootModuleConf(),
                ancestor.getRequiredConfigurations());
            for (Iterator iter = deps.iterator(); iter.hasNext();) {
                IvyNode dep = (IvyNode) iter.next();
                if (dep.getModuleId().equals(node.getModuleId())) {
                    conflicts.add(dep);
                }
                conflicts
                        .addAll(dep.getResolvedNodes(node.getModuleId(), node.getRootModuleConf()));
            }
        } else if (resolvedNodes.isEmpty() && node.getParent() != ancestor) {
            DependencyDescriptor[] dds = ancestor.getDescriptor().getDependencies();
            for (int i = 0; i < dds.length; i++) {
                if (dds[i].getDependencyId().equals(node.getModuleId())) {
                    IvyNode n = node.getNode().findNode(dds[i].getDependencyRevisionId());
                    if (n != null) {
                        conflicts.add(n);
                        break;
                    }
                }
            }
        } else {
            conflicts.addAll(resolvedNodes);
        }
        return conflicts;
    }

    private boolean checkConflictSolvedSelected(VisitNode node, VisitNode ancestor) {
        if (ancestor.getResolvedRevisions(node.getModuleId()).contains(node.getResolvedId())) {
            // resolve conflict has already be done with node with the same id
            if (settings.debugConflictResolution()) {
                Message.debug("conflict resolution already done for " + node + " in " + ancestor);
            }
            return true;
        }
        return false;
    }

    private boolean checkConflictSolvedEvicted(VisitNode node, VisitNode ancestor) {
        if (ancestor.getEvictedRevisions(node.getModuleId()).contains(node.getResolvedId())) {
            // resolve conflict has already be done with node with the same id
            if (settings.debugConflictResolution()) {
                Message.debug("conflict resolution already done for " + node + " in " + ancestor);
            }
            return true;
        }
        return false;
    }

    public ResolvedModuleRevision findModule(ModuleRevisionId id, ResolveOptions options) {
        DependencyResolver r = settings.getResolver(id.getModuleId());
        if (r == null) {
            throw new IllegalStateException("no resolver found for " + id.getModuleId());
        }
        DefaultModuleDescriptor md = DefaultModuleDescriptor.newCallerInstance(id,
            new String[] {"*"}, false, false);

        if (options.getResolveId() == null) {
            options.setResolveId(ResolveOptions.getDefaultResolveId(md));
        }

        try {
            return r.getDependency(new DefaultDependencyDescriptor(id, true), new ResolveData(this,
                    options, new ConfigurationResolveReport(this, md, "default", null, options)));
        } catch (ParseException e) {
            throw new RuntimeException("problem while parsing repository module descriptor for "
                    + id + ": " + e, e);
        }
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public IvySettings getSettings() {
        return settings;
    }

    public SortEngine getSortEngine() {
        return sortEngine;
    }

    private void checkInterrupted() {
        IvyContext.getContext().getIvy().checkInterrupted();
    }

}
