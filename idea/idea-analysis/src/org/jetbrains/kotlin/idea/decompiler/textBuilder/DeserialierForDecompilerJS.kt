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

import com.google.protobuf.ExtensionRegistryLite
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.builtins.BuiltInsAnnotationAndConstantLoader
import org.jetbrains.kotlin.builtins.BuiltInsSerializationUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.MutablePackageFragmentDescriptor
import org.jetbrains.kotlin.idea.decompiler.navigation.JsMetaFileUtils
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.PlatformToKotlinClassMap
import org.jetbrains.kotlin.serialization.PackageData
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.DeserializationComponents
import org.jetbrains.kotlin.serialization.deserialization.FlexibleTypeCapabilitiesDeserializer
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.serialization.js.JsProtoBuf
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import java.io.ByteArrayInputStream
import java.util.Collections

public fun DeserializerForDecompilerJS(classFile: VirtualFile): DeserializerForDecompilerJS {
    val relPath = JsMetaFileUtils.getRelativeToRootPath(classFile)
    val packageFqName = JsMetaFileUtils.getPackageFqName(relPath)

    return DeserializerForDecompilerJS(classFile.getParent()!!, packageFqName)
}

public class DeserializerForDecompilerJS(val packageDirectory: VirtualFile, val directoryPackageFqName: FqName) : ResolverForDecompiler {

    private val moduleDescriptor =
            ModuleDescriptorImpl(Name.special("<module for building decompiled sources>"), listOf(), PlatformToKotlinClassMap.EMPTY)

    private fun createDummyModule(name: String) = ModuleDescriptorImpl(Name.special("<$name>"), listOf(), PlatformToKotlinClassMap.EMPTY)

    override fun resolveTopLevelClass(classId: ClassId) = deserializationComponents.deserializeClass(classId)

    private val nameResolver: NameResolver = run {
        val stringsFile = packageDirectory.findChild(BuiltInsSerializationUtil.STRING_TABLE_FILE_NAME)
        assert(stringsFile != null)
        NameResolver.read(ByteArrayInputStream(stringsFile!!.contentsToByteArray(false)))
    }

    override fun resolveDeclarationsInPackage(packageFqName: FqName): Collection<DeclarationDescriptor> {
        assert(packageFqName == directoryPackageFqName, "Was called for $packageFqName but only $directoryPackageFqName is expected.")
        val file = classFinder.findKotlinJavascriptMetaInfo(PackageClassUtils.getPackageClassId(packageFqName))
        if (file == null) {
            LOG.error("Could not read data for $packageFqName")
            return Collections.emptyList()
        }

        val bytes = file.contentsToByteArray(false)

        val registry = ExtensionRegistryLite.newInstance()
        JsProtoBuf.registerAllExtensions(registry)

        val packageProto = ProtoBuf.Package.parseFrom(ByteArrayInputStream(bytes), registry)

        val packageData = PackageData(nameResolver, packageProto)

        val membersScope = DeserializedPackageMemberScope(
                createDummyPackageFragment(packageFqName),
                packageData.getPackageProto(),
                packageData.getNameResolver(),
                deserializationComponents
        ) { listOf() }
        return membersScope.getDescriptors()
    }

    private val classFinder = DirectoryBasedClassFinderJS(packageDirectory, directoryPackageFqName, nameResolver)
    private val classDataFinder = DirectoryBasedDataFinderJS(classFinder, LOG)

    private val storageManager = LockBasedStorageManager.NO_LOCKS

    private val annotationAndConstantLoader =
            BuiltInsAnnotationAndConstantLoader(moduleDescriptor)

    private val packageFragmentProvider = object : PackageFragmentProvider {
        override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
            return listOf(createDummyPackageFragment(fqName))
        }

        override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
            throw UnsupportedOperationException("This method is not supposed to be called.")
        }
    }

    init {
        moduleDescriptor.initialize(packageFragmentProvider)
        moduleDescriptor.addDependencyOnModule(moduleDescriptor)
        moduleDescriptor.addDependencyOnModule(KotlinBuiltIns.getInstance().getBuiltInsModule())
        val moduleContainingMissingDependencies = createDummyModule("module containing missing dependencies for decompiled sources")
        moduleContainingMissingDependencies.addDependencyOnModule(moduleContainingMissingDependencies)
        moduleContainingMissingDependencies.initialize(
                PackageFragmentProviderForMissingDependencies(moduleContainingMissingDependencies)
        )
        moduleDescriptor.addDependencyOnModule(moduleContainingMissingDependencies)
        moduleDescriptor.seal()
        moduleContainingMissingDependencies.seal()
    }

    private val deserializationComponents = DeserializationComponents(
            storageManager, moduleDescriptor, classDataFinder, annotationAndConstantLoader, packageFragmentProvider,
            ResolveEverythingToKotlinAnyLocalClassResolver, FlexibleTypeCapabilitiesDeserializer.Dynamic
    )

    private fun createDummyPackageFragment(fqName: FqName): MutablePackageFragmentDescriptor {
        return MutablePackageFragmentDescriptor(moduleDescriptor, fqName)
    }

    companion object {
        private val LOG = Logger.getInstance(javaClass<DeserializerForDecompilerJS>())
    }
}
