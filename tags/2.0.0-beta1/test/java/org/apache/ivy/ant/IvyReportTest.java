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
import java.util.Locale;

import junit.framework.TestCase;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;

public class IvyReportTest extends TestCase {
    private File cache;

    private IvyReport report;

    private Project project;

    protected void setUp() throws Exception {
        createCache();
        project = new Project();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");

        report = new IvyReport();
        report.setTaskName("report");
        report.setProject(project);
        report.setCache(cache);
    }

    private void createCache() {
        cache = new File("build/cache");
        cache.mkdirs();
    }

    protected void tearDown() throws Exception {
        cleanCache();
    }

    private void cleanCache() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(cache);
        del.execute();
    }
    
    public void testSimple() throws Exception {
        Locale oldLocale = Locale.getDefault();
        
        try {
            // set the locale to UK as workaround for SUN bug 6240963
            Locale.setDefault(Locale.UK);

            IvyResolve res = new IvyResolve();
            res.setProject(project);
            res.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
            res.setCache(cache);
            res.execute();
    
            report.setTodir(new File(cache, "report"));
            report.execute();
            
            assertTrue(new File(cache, "report/apache-resolve-simple-default.html").exists());
            assertTrue(new File(cache, "report/apache-resolve-simple-default.graphml").exists());
        } finally {
            Locale.setDefault(oldLocale);
        }
    }

    public void testMultipleConfigurations() throws Exception {
        Locale oldLocale = Locale.getDefault();
        
        try {
            // set the locale to UK as workaround for SUN bug 6240963
            Locale.setDefault(Locale.UK);

            IvyResolve res = new IvyResolve();
            res.setProject(project);
            res.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
            res.setCache(cache);
            res.execute();
    
            report.setTodir(new File(cache, "report"));
            report.execute();
            
            assertTrue(new File(cache, "report/apache-resolve-simple-default.html").exists());
            assertTrue(new File(cache, "report/apache-resolve-simple-default.graphml").exists());
            assertTrue(new File(cache, "report/apache-resolve-simple-compile.html").exists());
            assertTrue(new File(cache, "report/apache-resolve-simple-compile.graphml").exists());
        } finally {
            Locale.setDefault(oldLocale);
        }
    }

    public void testRegularCircular() throws Exception {
        project.setProperty("ivy.dep.file", "test/repositories/2/mod11.1/ivy-1.0.xml");
        IvyResolve res = new IvyResolve();
        res.setProject(project);
        res.execute();

        report.setTodir(new File(cache, "report"));
        report.setXml(true);

        // do not test any xsl transformation here, because of problems of build in our continuous
        // integration server
        report.setXsl(false);
        report.setGraph(false);

        report.execute();

        assertTrue(new File(cache, "report/org11-mod11.1-compile.xml").exists());
    }
}
