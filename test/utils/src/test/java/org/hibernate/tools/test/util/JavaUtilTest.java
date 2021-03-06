/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 * 
 * Copyright 2017-2020 Red Hat, Inc.
 *
 * Licensed under the GNU Lesser General Public License (LGPL), 
 * version 2.1 or later (the "License").
 * You may not use this file except in compliance with the License.
 * You may read the licence in the 'lgpl.txt' file in the root folder of 
 * project or obtain a copy at
 *
 *     http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tools.test.util;

import java.io.File;
import java.io.FileWriter;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class JavaUtilTest {
	
	private static final String FOO_STRING = 
			"package org.hibernate.tool.test;"+
			"public class Foo {              "+
			"  public Bar bar;               "+
			"}                               ";
	
	private static final String BAR_STRING = 
			"package org.hibernate.tool.test;"+
			"public class Bar {              "+
			"  public Foo foo;               "+
			"}                               ";
	
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();
	
	@Test
	public void testCompile() throws Exception {
		File testFolder = temporaryFolder.getRoot();
		File packageFolder = new File(testFolder, "org/hibernate/tool/test");
		packageFolder.mkdirs();
		File fooFile = new File(packageFolder, "Foo.java");
		FileWriter fileWriter = new FileWriter(fooFile);
		fileWriter.write(FOO_STRING);
		fileWriter.close();
		File barFile = new File(packageFolder, "Bar.java");
		fileWriter = new FileWriter(barFile);
		fileWriter.write(BAR_STRING);
		fileWriter.close();
		Assert.assertFalse(new File(packageFolder, "Foo.class").exists());
		Assert.assertFalse(new File(packageFolder, "Bar.class").exists());
		JavaUtil.compile(testFolder);
		Assert.assertTrue(new File(packageFolder, "Foo.class").exists());
		Assert.assertTrue(new File(packageFolder, "Bar.class").exists());
	}

}
