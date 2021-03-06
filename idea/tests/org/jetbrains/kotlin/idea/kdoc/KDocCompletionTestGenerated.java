/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.kdoc;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.InnerTestClasses;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.test.JetTestUtils;
import org.jetbrains.kotlin.completion.AbstractJvmBasicCompletionTest;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.TestsPackage}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("idea/testData/kdoc/completion")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class KDocCompletionTestGenerated extends AbstractJvmBasicCompletionTest {
    public void testAllFilesPresentInCompletion() throws Exception {
        JetTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("idea/testData/kdoc/completion"), Pattern.compile("^(.+)\\.kt$"), true);
    }

    @TestMetadata("Link.kt")
    public void testLink() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/kdoc/completion/Link.kt");
        doTest(fileName);
    }

    @TestMetadata("ParamTag.kt")
    public void testParamTag() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/kdoc/completion/ParamTag.kt");
        doTest(fileName);
    }

    @TestMetadata("SkipExistingParamTag.kt")
    public void testSkipExistingParamTag() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/kdoc/completion/SkipExistingParamTag.kt");
        doTest(fileName);
    }

    @TestMetadata("TagName.kt")
    public void testTagName() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/kdoc/completion/TagName.kt");
        doTest(fileName);
    }

    @TestMetadata("TagNameAfterAt.kt")
    public void testTagNameAfterAt() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/kdoc/completion/TagNameAfterAt.kt");
        doTest(fileName);
    }

    @TestMetadata("TagNameMiddle.kt")
    public void testTagNameMiddle() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/kdoc/completion/TagNameMiddle.kt");
        doTest(fileName);
    }

    @TestMetadata("TagNameStart.kt")
    public void testTagNameStart() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/kdoc/completion/TagNameStart.kt");
        doTest(fileName);
    }
}
