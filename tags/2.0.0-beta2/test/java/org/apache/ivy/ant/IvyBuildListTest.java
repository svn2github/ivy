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

import junit.framework.TestCase;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;

public class IvyBuildListTest extends TestCase {

    /* 
     * Those tests use the ivy files A , B , C , D , E in test/buildlist
     * The dependencies are :
     * A -> C
     * B has no dependency
     * C -> B
     * D -> A , B
     * E has no dependency
     */

    //CheckStyle:MagicNumber| OFF   
    //The test very often use MagicNumber.  Using a constant is less expessif.

    
    public void testSimple() {
        Project p = new Project();

        IvyBuildList buildlist = new IvyBuildList();
        buildlist.setProject(p);

        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");
        buildlist.addFileset(fs);

        buildlist.setReference("ordered.build.files");

        buildlist.execute();

        Object o = p.getReference("ordered.build.files");
        assertNotNull(o);
        assertTrue(o instanceof Path);

        Path path = (Path) o;
        String[] files = path.list();
        assertNotNull(files);
        
        assertEquals(5, files.length);

        assertEquals(new File("test/buildlist/B/build.xml").getAbsolutePath(), new File(files[0])
                .getAbsolutePath());
        assertEquals(new File("test/buildlist/C/build.xml").getAbsolutePath(), new File(files[1])
                .getAbsolutePath());
        assertEquals(new File("test/buildlist/A/build.xml").getAbsolutePath(), new File(files[2])
                .getAbsolutePath());
        assertEquals(new File("test/buildlist/D/build.xml").getAbsolutePath(), new File(files[3])
                .getAbsolutePath());
        assertEquals(new File("test/buildlist/E/build.xml").getAbsolutePath(), new File(files[4])
                .getAbsolutePath());
    }

    public void testReverse() {
        Project p = new Project();

        IvyBuildList buildlist = new IvyBuildList();
        buildlist.setProject(p);
        buildlist.setReverse(true);

        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");
        buildlist.addFileset(fs);

        buildlist.setReference("reverse.ordered.build.files");

        buildlist.execute();

        Object o = p.getReference("reverse.ordered.build.files");
        assertNotNull(o);
        assertTrue(o instanceof Path);

        Path path = (Path) o;
        String[] files = path.list();
        assertNotNull(files);
        
        assertEquals(5, files.length);

        assertEquals(new File("test/buildlist/E/build.xml").getAbsolutePath(), new File(files[0])
                .getAbsolutePath());
        assertEquals(new File("test/buildlist/D/build.xml").getAbsolutePath(), new File(files[1])
                .getAbsolutePath());
        assertEquals(new File("test/buildlist/A/build.xml").getAbsolutePath(), new File(files[2])
                .getAbsolutePath());
        assertEquals(new File("test/buildlist/C/build.xml").getAbsolutePath(), new File(files[3])
                .getAbsolutePath());
        assertEquals(new File("test/buildlist/B/build.xml").getAbsolutePath(), new File(files[4])
                .getAbsolutePath());
        
    }

    public void testWithRoot() {
        Project p = new Project();

        IvyBuildList buildlist = new IvyBuildList();
        buildlist.setProject(p);
        buildlist.setRoot("C");

        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");
        buildlist.addFileset(fs);

        buildlist.setReference("ordered.build.files");

        buildlist.execute();

        Object o = p.getReference("ordered.build.files");
        assertNotNull(o);
        assertTrue(o instanceof Path);

        Path path = (Path) o;
        String[] files = path.list();
        assertNotNull(files);
        assertEquals(2, files.length); // A and D should be filtered out

        assertEquals(new File("test/buildlist/B/build.xml").getAbsolutePath(), new File(files[0])
                .getAbsolutePath());
        assertEquals(new File("test/buildlist/C/build.xml").getAbsolutePath(), new File(files[1])
                .getAbsolutePath());
    }

    public void testWithTwoRoots() {
        Project p = new Project();

        IvyBuildList buildlist = new IvyBuildList();
        buildlist.setProject(p);
        buildlist.setRoot("C,E");

        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");
        buildlist.addFileset(fs);

        buildlist.setReference("ordered.build.files");

        buildlist.execute();

        Object o = p.getReference("ordered.build.files");
        assertNotNull(o);
        assertTrue(o instanceof Path);

        Path path = (Path) o;
        String[] files = path.list();
        assertNotNull(files);
        
        assertEquals(3, files.length); // A and D should be filtered out
        

        assertEquals(new File("test/buildlist/B/build.xml").getAbsolutePath(), new File(files[0])
                .getAbsolutePath());
        assertEquals(new File("test/buildlist/C/build.xml").getAbsolutePath(), new File(files[1])
                .getAbsolutePath());
        assertEquals(new File("test/buildlist/E/build.xml").getAbsolutePath(), new File(files[2])
                .getAbsolutePath());
    }

    public void testWithRootExclude() {
        Project p = new Project();

        IvyBuildList buildlist = new IvyBuildList();
        buildlist.setProject(p);
        buildlist.setRoot("C");
        buildlist.setExcludeRoot(true);

        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");
        buildlist.addFileset(fs);

        buildlist.setReference("ordered.build.files");

        buildlist.execute();

        Object o = p.getReference("ordered.build.files");
        assertNotNull(o);
        assertTrue(o instanceof Path);

        Path path = (Path) o;
        String[] files = path.list();
        assertNotNull(files);
        assertEquals(1, files.length); // A, D and C should be filtered out

        assertEquals(new File("test/buildlist/B/build.xml").getAbsolutePath(), new File(files[0])
                .getAbsolutePath());
    }

    
    public void testWithRootAndOnlyDirectDep() {
        Project p = new Project();

        IvyBuildList buildlist = new IvyBuildList();
        buildlist.setProject(p);
        buildlist.setRoot("A");
        buildlist.setOnlydirectdep(true);

        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");
        buildlist.addFileset(fs);

        buildlist.setReference("ordered.build.files");

        buildlist.execute();

        Object o = p.getReference("ordered.build.files");
        assertNotNull(o);
        assertTrue(o instanceof Path);

        Path path = (Path) o;
        String[] files = path.list();
        assertNotNull(files);
        assertEquals(2, files.length); // We should have only A and C

        assertEquals(new File("test/buildlist/C/build.xml").getAbsolutePath(), new File(files[0])
                .getAbsolutePath());
        assertEquals(new File("test/buildlist/A/build.xml").getAbsolutePath(), new File(files[1])
                .getAbsolutePath());
    }

    
    public void testWithLeaf() {
        Project p = new Project();

        IvyBuildList buildlist = new IvyBuildList();
        buildlist.setProject(p);
        buildlist.setLeaf("C");

        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");
        buildlist.addFileset(fs);

        buildlist.setReference("ordered.build.files");

        buildlist.execute();

        Object o = p.getReference("ordered.build.files");
        assertNotNull(o);
        assertTrue(o instanceof Path);

        Path path = (Path) o;
        String[] files = path.list();
        assertNotNull(files);
        
        assertEquals(3, files.length); // B should be filtered out
        

        assertEquals(new File("test/buildlist/C/build.xml").getAbsolutePath(), new File(files[0])
                .getAbsolutePath());
        assertEquals(new File("test/buildlist/A/build.xml").getAbsolutePath(), new File(files[1])
                .getAbsolutePath());
        assertEquals(new File("test/buildlist/D/build.xml").getAbsolutePath(), new File(files[2])
                .getAbsolutePath());
    }

    public void testWithTwoLeafs() {
        Project p = new Project();

        IvyBuildList buildlist = new IvyBuildList();
        buildlist.setProject(p);
        buildlist.setLeaf("C,E");

        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");
        buildlist.addFileset(fs);

        buildlist.setReference("ordered.build.files");

        buildlist.execute();

        Object o = p.getReference("ordered.build.files");
        assertNotNull(o);
        assertTrue(o instanceof Path);

        Path path = (Path) o;
        String[] files = path.list();
        assertNotNull(files);
        
        assertEquals(4, files.length); // B should be filtered out

        assertEquals(new File("test/buildlist/C/build.xml").getAbsolutePath(), new File(files[0])
                .getAbsolutePath());
        assertEquals(new File("test/buildlist/A/build.xml").getAbsolutePath(), new File(files[1])
                .getAbsolutePath());
        assertEquals(new File("test/buildlist/D/build.xml").getAbsolutePath(), new File(files[2])
                .getAbsolutePath());
        assertEquals(new File("test/buildlist/E/build.xml").getAbsolutePath(), new File(files[3])
                .getAbsolutePath());
        
    }

    public void testWithLeafExclude() {
        Project p = new Project();

        IvyBuildList buildlist = new IvyBuildList();
        buildlist.setProject(p);
        buildlist.setLeaf("C");
        buildlist.setExcludeLeaf(true);

        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");
        buildlist.addFileset(fs);

        buildlist.setReference("ordered.build.files");

        buildlist.execute();

        Object o = p.getReference("ordered.build.files");
        assertNotNull(o);
        assertTrue(o instanceof Path);

        Path path = (Path) o;
        String[] files = path.list();
        assertNotNull(files);
        assertEquals(2, files.length); // B and C should be filtered out

        assertEquals(new File("test/buildlist/A/build.xml").getAbsolutePath(), new File(files[0])
                .getAbsolutePath());
        assertEquals(new File("test/buildlist/D/build.xml").getAbsolutePath(), new File(files[1])
                .getAbsolutePath());
    }

    
    public void testWithLeafAndOnlyDirectDep() {
        Project p = new Project();

        IvyBuildList buildlist = new IvyBuildList();
        buildlist.setProject(p);
        buildlist.setLeaf("C");
        buildlist.setOnlydirectdep(true);

        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");
        buildlist.addFileset(fs);

        buildlist.setReference("ordered.build.files");

        buildlist.execute();

        Object o = p.getReference("ordered.build.files");
        assertNotNull(o);
        assertTrue(o instanceof Path);

        Path path = (Path) o;
        String[] files = path.list();
        assertNotNull(files);
        assertEquals(2, files.length); // We must have only A and C

        assertEquals(new File("test/buildlist/C/build.xml").getAbsolutePath(), new File(files[0])
                .getAbsolutePath());
        assertEquals(new File("test/buildlist/A/build.xml").getAbsolutePath(), new File(files[1])
                .getAbsolutePath());
    }

    
    public void testRestartFrom() {
        Project p = new Project();

        IvyBuildList buildlist = new IvyBuildList();
        buildlist.setProject(p);
        buildlist.setRestartFrom("C");

        FileSet fs = new FileSet();
        fs.setDir(new File("test/buildlist"));
        fs.setIncludes("**/build.xml");
        buildlist.addFileset(fs);

        buildlist.setReference("ordered.build.files");

        buildlist.execute();

        Object o = p.getReference("ordered.build.files");
        assertNotNull(o);
        assertTrue(o instanceof Path);

        Path path = (Path) o;
        String[] files = path.list();
        assertNotNull(files);
        
        assertEquals(4, files.length);

        assertEquals(new File("test/buildlist/C/build.xml").getAbsolutePath(), new File(files[0])
                .getAbsolutePath());
        assertEquals(new File("test/buildlist/A/build.xml").getAbsolutePath(), new File(files[1])
                .getAbsolutePath());
        assertEquals(new File("test/buildlist/D/build.xml").getAbsolutePath(), new File(files[2])
                .getAbsolutePath());
        assertEquals(new File("test/buildlist/E/build.xml").getAbsolutePath(), new File(files[3])
                .getAbsolutePath());
    }


    
}
//CheckStyle:MagicNumber| ON