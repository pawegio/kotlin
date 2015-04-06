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

package org.jetbrains.kotlin.idea.decompiler.stubBuilder

import com.google.protobuf.ExtensionRegistryLite
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.compiled.ClsStubBuilder
import com.intellij.psi.impl.compiled.ClassFileStubBuilder
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.util.cls.ClsFormatException
import com.intellij.util.indexing.FileContent
import org.jetbrains.kotlin.builtins.BuiltInsSerializationUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.idea.decompiler.navigation
import org.jetbrains.kotlin.idea.decompiler.navigation.*
import org.jetbrains.kotlin.idea.decompiler.textBuilder.*
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.load.kotlin.header.isCompatibleClassKind
import org.jetbrains.kotlin.load.kotlin.header.isCompatiblePackageFacadeKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.stubs.elements.JetFileStubBuilder
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.serialization.ClassData
import org.jetbrains.kotlin.serialization.PackageData
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.js.JsProtoBuf
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil
import java.io.ByteArrayInputStream

public open class KotlinJavascriptStubBuilder : ClsStubBuilder() {
    override fun getStubVersion() = ClassFileStubBuilder.STUB_VERSION + 1

    override fun buildFileStub(content: FileContent): PsiFileStub<*>? {
        val file = content.getFile()

        val project = content.getProject()
        return doBuildFileStub(file, project)
    }

    throws(javaClass<ClsFormatException>())
    fun doBuildFileStub(file: VirtualFile, project: Project): PsiFileStub<JetFile>? {

        // TODO remove this temporarily hack
        val decompiledTextObj = buildDecompiledTextFromJsMetadata(file)
        val fileWithDecompiledText = psi.JetPsiFactory(project).createFile(decompiledTextObj.text)
        val stubTreeFromDecompiledText = JetFileStubBuilder().buildStubTree(fileWithDecompiledText)
        if (stubTreeFromDecompiledText is PsiFileStub) return (stubTreeFromDecompiledText as PsiFileStub<JetFile>)
        return null

        /*
        val relPath = JsMetaFileUtils.getRelativeToRootPath(file)
        val packageFqName = JsMetaFileUtils.getPackageFqName(relPath)

        val bytes = file.contentsToByteArray(false)
        val isPackageHeader = JsMetaFileUtils.isPackageHeader(relPath)

        val packageDirectory = file.getParent()!!
        val stringsFile = packageDirectory.findChild(BuiltInsSerializationUtil.STRING_TABLE_FILE_NAME)
        assert(stringsFile != null)
        val nameResolver = NameResolver.read(ByteArrayInputStream(stringsFile!!.contentsToByteArray(false)))

        val components = createStubBuilderComponents(file, packageFqName, nameResolver)

        val registry = ExtensionRegistryLite.newInstance()
        JsProtoBuf.registerAllExtensions(registry)

        if (isPackageHeader) {
            val packageProto = ProtoBuf.Package.parseFrom(ByteArrayInputStream(bytes), registry)
            val packageData = PackageData(nameResolver, packageProto)
            val context = components.createContext(packageData.getNameResolver(), packageFqName)
            return createPackageFacadeFileStub(packageData.getPackageProto(), packageFqName, context)
        }
        else {
            val classProto = ProtoBuf.Class.parseFrom(ByteArrayInputStream(bytes), registry)
            val classData =  ClassData(nameResolver, classProto)
            val context = components.createContext(classData.getNameResolver(), packageFqName)
            val classId = JsMetaFileUtils.getClassId(relPath)
            return createTopLevelClassStub(classId, classData.getClassProto(), context)
        }
        */
    }

    private fun createStubBuilderComponents(file: VirtualFile, packageFqName: FqName, nameResolver: NameResolver): ClsStubBuilderComponents {
        val classFinder = DirectoryBasedClassFinder(file.getParent()!!, packageFqName)
        val classFinderJS = DirectoryBasedClassFinderJS(file.getParent()!!, packageFqName, nameResolver)
        val classDataFinder = DirectoryBasedDataFinderJS(classFinderJS, LOG)
        val annotationLoader = AnnotationLoaderForStubBuilder(classFinder, LoggingErrorReporter(LOG))
        return ClsStubBuilderComponents(classDataFinder, annotationLoader)
    }

    companion object {
        val LOG = Logger.getInstance(javaClass<KotlinClsStubBuilder>())
    }
}

