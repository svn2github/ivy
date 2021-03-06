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
package org.apache.ivy.plugins.parser.m2;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.AbstractModuleDescriptorParserTester;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParserTest;
import org.apache.ivy.plugins.repository.url.URLResource;

public class PomModuleDescriptorParserTest extends AbstractModuleDescriptorParserTester {
    // junit test -- DO NOT REMOVE used by ant to know it's a junit test

    public void testAccept() throws Exception {
        assertTrue(PomModuleDescriptorParser.getInstance().accept(
            new URLResource(getClass().getResource("test-simple.pom"))));
        assertFalse(PomModuleDescriptorParser.getInstance().accept(
            new URLResource(XmlModuleDescriptorParserTest.class.getResource("test.xml"))));
    }

    public void testSimple() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), getClass().getResource("test-simple.pom"), false);
        assertNotNull(md);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "test", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertNotNull(md.getConfigurations());
        assertEquals(Arrays.asList(PomModuleDescriptorParser.MAVEN2_CONFIGURATIONS), Arrays
                .asList(md.getConfigurations()));

        Artifact[] artifact = md.getArtifacts("master");
        assertEquals(1, artifact.length);
        assertEquals(mrid, artifact[0].getModuleRevisionId());
        assertEquals("test", artifact[0].getName());
        assertEquals("jar", artifact[0].getExt());
        assertEquals("jar", artifact[0].getType());
    }

    public void testPackaging() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), getClass().getResource("test-packaging.pom"), false);
        assertNotNull(md);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "test", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertNotNull(md.getConfigurations());
        assertEquals(Arrays.asList(PomModuleDescriptorParser.MAVEN2_CONFIGURATIONS), Arrays
                .asList(md.getConfigurations()));

        Artifact[] artifact = md.getArtifacts("master");
        assertEquals(1, artifact.length);
        assertEquals(mrid, artifact[0].getModuleRevisionId());
        assertEquals("test", artifact[0].getName());
        assertEquals("war", artifact[0].getExt());
        assertEquals("war", artifact[0].getType());
    }

    public void testParent() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), getClass().getResource("test-parent.pom"), false);
        assertNotNull(md);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "test", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertNotNull(md.getConfigurations());
        assertEquals(Arrays.asList(PomModuleDescriptorParser.MAVEN2_CONFIGURATIONS), Arrays
                .asList(md.getConfigurations()));

        Artifact[] artifact = md.getArtifacts("master");
        assertEquals(1, artifact.length);
        assertEquals(mrid, artifact[0].getModuleRevisionId());
        assertEquals("test", artifact[0].getName());
    }

    public void testParent2() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), getClass().getResource("test-parent2.pom"), false);
        assertNotNull(md);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "test", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertNotNull(md.getConfigurations());
        assertEquals(Arrays.asList(PomModuleDescriptorParser.MAVEN2_CONFIGURATIONS), Arrays
                .asList(md.getConfigurations()));

        Artifact[] artifact = md.getArtifacts("master");
        assertEquals(1, artifact.length);
        assertEquals(mrid, artifact[0].getModuleRevisionId());
        assertEquals("test", artifact[0].getName());
    }

    public void testParentVersion() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), getClass().getResource("test-parent.version.pom"), false);
        assertNotNull(md);

        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "test", "1.0");
        assertEquals(mrid, md.getModuleRevisionId());

        assertNotNull(md.getConfigurations());
        assertEquals(Arrays.asList(PomModuleDescriptorParser.MAVEN2_CONFIGURATIONS), Arrays
                .asList(md.getConfigurations()));

        Artifact[] artifact = md.getArtifacts("master");
        assertEquals(1, artifact.length);
        assertEquals(mrid, artifact[0].getModuleRevisionId());
        assertEquals("test", artifact[0].getName());
    }

    public void testDependencies() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), getClass().getResource("test-dependencies.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.apache", "test", "1.0"), md
                .getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"),
            dds[0].getDependencyRevisionId());
    }

    public void testDependenciesWithClassifier() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), getClass().getResource("test-dependencies-with-classifier.pom"),
            false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.apache", "test", "1.0"), md
                .getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"),
            dds[0].getDependencyRevisionId());
        Map extraAtt = new HashMap();
        extraAtt.put("classifier", "asl");
        assertEquals(1, dds[0].getAllDependencyArtifacts().length);
        assertEquals(extraAtt, dds[0].getAllDependencyArtifacts()[0].getExtraAttributes());
    }

    public void testWithVersionProperty() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), getClass().getResource("test-version.pom"), false);
        assertNotNull(md);

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("org.apache", "test-other", "1.0"), dds[0]
                .getDependencyRevisionId());
    }

    // IVY-392
    public void testDependenciesWithProfile() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), getClass().getResource("test-dependencies-with-profile.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.apache", "test", "1.0"), md
                .getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"),
            dds[0].getDependencyRevisionId());
    }

    public void testWithoutVersion() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), getClass().getResource("test-without-version.pom"), false);
        assertNotNull(md);

        assertEquals(new ModuleId("org.apache", "test"), md.getModuleRevisionId().getModuleId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"),
            dds[0].getDependencyRevisionId());
    }

    public void testProperties() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), getClass().getResource("test-properties.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("drools", "drools-smf", "2.0-beta-18"), md
                .getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("drools", "drools-core", "2.0-beta-18"), dds[0]
                .getDependencyRevisionId());
    }

    public void testReal() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), getClass().getResource("commons-lang-1.0.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("commons-lang", "commons-lang", "1.0"), md
                .getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(1, dds.length);
        assertEquals(ModuleRevisionId.newInstance("junit", "junit", "3.7"), dds[0]
                .getDependencyRevisionId());
    }

    public void testReal2() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), getClass().getResource("wicket-1.3-incubating-SNAPSHOT.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.apache.wicket", "wicket",
            "1.3-incubating-SNAPSHOT"), md.getModuleRevisionId());
    }

    public void testVariables() throws Exception {
        // test case for IVY-425
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), getClass().getResource("spring-hibernate3-2.0.2.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.springframework", "spring-hibernate3",
            "2.0.2"), md.getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(11, dds.length);
        assertEquals(ModuleRevisionId.newInstance("org.springframework", "spring-web", "2.0.2"),
            dds[10].getDependencyRevisionId());
    }

    public void testDependenciesInProfile() throws Exception {
        // test case for IVY-423
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), getClass().getResource("mule-module-builders-1.3.3.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.mule.modules", "mule-module-builders",
            "1.3.3"), md.getModuleRevisionId());
    }

    public void testIVY424() throws Exception {
        // test case for IVY-424
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), getClass().getResource("shale-tiger-1.1.0-SNAPSHOT.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.apache.shale", "shale-tiger",
            "1.1.0-SNAPSHOT"), md.getModuleRevisionId());
    }

    public void testOptional() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), getClass().getResource("test-optional.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.apache", "test", "1.0"), md
                .getModuleRevisionId());
        assertTrue(Arrays.asList(md.getConfigurationsNames()).contains("optional"));

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(2, dds.length);
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"),
            dds[0].getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"optional"})), new HashSet(Arrays
                .asList(dds[0].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile(*)", "runtime(*)",
                "master(*)"})), new HashSet(Arrays.asList(dds[0]
                .getDependencyConfigurations("optional"))));

        assertEquals(ModuleRevisionId.newInstance("cglib", "cglib", "2.0.2"), dds[1]
                .getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile", "runtime"})), new HashSet(
                Arrays.asList(dds[1].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"master(*)", "compile(*)"})),
            new HashSet(Arrays.asList(dds[1].getDependencyConfigurations("compile"))));
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime(*)"})), new HashSet(Arrays
                .asList(dds[1].getDependencyConfigurations("runtime"))));
    }

    public void testDependenciesWithScope() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), getClass().getResource("test-dependencies-with-scope.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.apache", "test", "1.0"), md
                .getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(3, dds.length);
        assertEquals(ModuleRevisionId.newInstance("odmg", "odmg", "3.0"), dds[0]
                .getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime"})), new HashSet(Arrays
                .asList(dds[0].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile(*)", "runtime(*)",
                "master(*)"})), new HashSet(Arrays.asList(dds[0]
                .getDependencyConfigurations("runtime"))));

        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"),
            dds[1].getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile", "runtime"})), new HashSet(
                Arrays.asList(dds[1].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"master(*)", "compile(*)"})),
            new HashSet(Arrays.asList(dds[1].getDependencyConfigurations("compile"))));
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime(*)"})), new HashSet(Arrays
                .asList(dds[1].getDependencyConfigurations("runtime"))));

        assertEquals(ModuleRevisionId.newInstance("cglib", "cglib", "2.0.2"), dds[2]
                .getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile", "runtime"})), new HashSet(
                Arrays.asList(dds[2].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"master(*)", "compile(*)"})),
            new HashSet(Arrays.asList(dds[2].getDependencyConfigurations("compile"))));
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime(*)"})), new HashSet(Arrays
                .asList(dds[2].getDependencyConfigurations("runtime"))));
    }

    public void testExclusion() throws Exception {
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), getClass().getResource("test-exclusion.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.apache", "test", "1.0"), md
                .getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(3, dds.length);
        assertEquals(ModuleRevisionId.newInstance("commons-logging", "commons-logging", "1.0.4"),
            dds[0].getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile", "runtime"})), new HashSet(
                Arrays.asList(dds[0].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"master(*)", "compile(*)"})),
            new HashSet(Arrays.asList(dds[0].getDependencyConfigurations("compile"))));
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime(*)"})), new HashSet(Arrays
                .asList(dds[0].getDependencyConfigurations("runtime"))));
        assertEquals(0, dds[0].getAllExcludeRules().length);

        assertEquals(ModuleRevisionId.newInstance("dom4j", "dom4j", "1.6"), dds[1]
                .getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile", "runtime"})), new HashSet(
                Arrays.asList(dds[1].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"master(*)", "compile(*)"})),
            new HashSet(Arrays.asList(dds[1].getDependencyConfigurations("compile"))));
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime(*)"})), new HashSet(Arrays
                .asList(dds[1].getDependencyConfigurations("runtime"))));
        assertDependencyModulesExcludes(dds[1], new String[] {"compile"}, new String[] {
                "jaxme-api", "jaxen"});
        assertDependencyModulesExcludes(dds[1], new String[] {"runtime"}, new String[] {
                "jaxme-api", "jaxen"});

        assertEquals(ModuleRevisionId.newInstance("cglib", "cglib", "2.0.2"), dds[2]
                .getDependencyRevisionId());
        assertEquals(new HashSet(Arrays.asList(new String[] {"compile", "runtime"})), new HashSet(
                Arrays.asList(dds[2].getModuleConfigurations())));
        assertEquals(new HashSet(Arrays.asList(new String[] {"master(*)", "compile(*)"})),
            new HashSet(Arrays.asList(dds[2].getDependencyConfigurations("compile"))));
        assertEquals(new HashSet(Arrays.asList(new String[] {"runtime(*)"})), new HashSet(Arrays
                .asList(dds[2].getDependencyConfigurations("runtime"))));
        assertEquals(0, dds[2].getAllExcludeRules().length);
    }

    public void testWithPlugins() throws Exception {
        // test case for IVY-417
        ModuleDescriptor md = PomModuleDescriptorParser.getInstance().parseDescriptor(
            new IvySettings(), getClass().getResource("mule-1.3.3.pom"), false);
        assertNotNull(md);

        assertEquals(ModuleRevisionId.newInstance("org.mule", "mule", "1.3.3"), md
                .getModuleRevisionId());

        DependencyDescriptor[] dds = md.getDependencies();
        assertNotNull(dds);
        assertEquals(0, dds.length);
    }

}
