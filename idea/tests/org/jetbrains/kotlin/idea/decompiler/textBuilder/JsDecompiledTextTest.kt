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

package org.jetbrains.kotlin.idea.decompiler.textBuilder

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.UsefulTestCase
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.JdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.idea.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.JetLightProjectDescriptor
import org.jetbrains.kotlin.idea.PluginTestCaseBase
import org.jetbrains.kotlin.idea.decompiler.JetClsFile
import org.jetbrains.kotlin.idea.decompiler.KotlinJavascriptMetaFile
import org.jetbrains.kotlin.idea.decompiler.navigation.NavigateToDecompiledLibraryTest
import org.jetbrains.kotlin.psi.JetPsiUtil
import org.jetbrains.kotlin.test.JetTestUtils
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.Assert

public class JsDecompiledTextTest : JetLightCodeInsightFixtureTestCase() {

    private val TEST_DATA_PATH = PluginTestCaseBase.getTestDataPathBase() + "/decompiler/decompiledText"

    TestMetadata("Annotations")
    throws(javaClass<Exception>())
    public fun testAnnotations() {
        val fileName = JetTestUtils.navigationMetadata("idea/testData/decompiler/decompiledText/Annotations/")
        doTest(fileName)
    }

    TestMetadata("AnnotationsOnPrimaryCtr")
    throws(javaClass<Exception>())
    public fun testAnnotationsOnPrimaryCtr() {
        val fileName = JetTestUtils.navigationMetadata("idea/testData/decompiler/decompiledText/AnnotationsOnPrimaryCtr/")
        doTest(fileName)
    }

    TestMetadata("ClassWithClassObject")
    throws(javaClass<Exception>())
    public fun testClassWithClassObject() {
        val fileName = JetTestUtils.navigationMetadata("idea/testData/decompiler/decompiledText/ClassWithClassObject/")
        doTest(fileName)
    }

    TestMetadata("DependencyOnNestedClasses")
    throws(javaClass<Exception>())
    public fun testDependencyOnNestedClasses() {
        val fileName = JetTestUtils.navigationMetadata("idea/testData/decompiler/decompiledText/DependencyOnNestedClasses/")
        doTest(fileName)
    }

    TestMetadata("Enum")
    throws(javaClass<Exception>())
    public fun testEnum() {
        val fileName = JetTestUtils.navigationMetadata("idea/testData/decompiler/decompiledText/Enum/")
        doTest(fileName)
    }

    TestMetadata("FlexibleTypes")
    throws(javaClass<Exception>())
    public fun testFlexibleTypes() {
        val fileName = JetTestUtils.navigationMetadata("idea/testData/decompiler/decompiledText/FlexibleTypes/")
        doTest(fileName)
    }

    TestMetadata("FunctionTypes")
    throws(javaClass<Exception>())
    public fun testFunctionTypes() {
        val fileName = JetTestUtils.navigationMetadata("idea/testData/decompiler/decompiledText/FunctionTypes/")
        doTest(fileName)
    }

    TestMetadata("NestedClasses")
    throws(javaClass<Exception>())
    public fun testNestedClasses() {
        val fileName = JetTestUtils.navigationMetadata("idea/testData/decompiler/decompiledText/NestedClasses/")
        doTest(fileName)
    }

    TestMetadata("Object")
    throws(javaClass<Exception>())
    public fun testObject() {
        val fileName = JetTestUtils.navigationMetadata("idea/testData/decompiler/decompiledText/Object/")
        doTest(fileName)
    }

    TestMetadata("SecondaryConstructors")
    throws(javaClass<Exception>())
    public fun testSecondaryConstructors() {
        val fileName = JetTestUtils.navigationMetadata("idea/testData/decompiler/decompiledText/SecondaryConstructors/")
        doTest(fileName)
    }

    TestMetadata("SimpleClass")
    throws(javaClass<Exception>())
    public fun testSimpleClass() {
        val fileName = JetTestUtils.navigationMetadata("idea/testData/decompiler/decompiledText/SimpleClass/")
        doTest(fileName)
    }

    TestMetadata("TestPackage")
    throws(javaClass<Exception>())
    public fun testTestPackage() {
        val fileName = JetTestUtils.navigationMetadata("idea/testData/decompiler/decompiledText/TestPackage/")
        doTest(fileName)
    }

    public fun doTest(path: String) {
        val classFile = getJsMetaFile("test", getTestName(false), myModule!!)
        val clsFile = PsiManager.getInstance(getProject()!!).findFile(classFile)
        Assert.assertTrue("Expecting decompiled kotlin javascript file, was: " + clsFile!!.javaClass, clsFile is KotlinJavascriptMetaFile)
        UsefulTestCase.assertSameLinesWithFile(path.substring(0, path.length - 1) + ".expected.kt", clsFile.getText())
        checkThatFileWasParsedCorrectly(clsFile)
    }

    private fun getJsMetaFile(packageName: String, className: String, module: Module): VirtualFile? {
        val root = NavigateToDecompiledLibraryTest.findTestLibraryRoot(module)
        TestCase.assertNotNull(root)
        val moduleDirName = JdkAndMockLibraryProjectDescriptor.LIBRARY_NAME
        val packageDir = root.findFileByRelativePath(moduleDirName + "/" + packageName.replace(".", "/"))
        TestCase.assertNotNull(packageDir)
        val metaFile = packageDir.findChild(className + ".meta")
        TestCase.assertNotNull(metaFile)
        return metaFile
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        if (isAllFilesPresentInTest()) {
            return JetLightProjectDescriptor.INSTANCE
        }
        return JdkAndMockLibraryProjectDescriptor(TEST_DATA_PATH + "/" + getTestName(false), false, true)
    }

    private fun checkThatFileWasParsedCorrectly(clsFile: PsiFile) {
        clsFile.accept(object : PsiRecursiveElementVisitor() {
            override fun visitErrorElement(element: PsiErrorElement) {
                Assert.fail("Decompiled file should not contain error elements!\n${JetPsiUtil.getElementTextWithContext(element)}")
            }
        })
    }
}
